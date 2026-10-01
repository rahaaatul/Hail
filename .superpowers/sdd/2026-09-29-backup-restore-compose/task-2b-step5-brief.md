# Task 2b step 5 — the host fragment, and making both directions actually work

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `5deec8d` and
CI is green on it. Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed` for the log.
Report only run ids that `gh` shows for this branch.

Read the binding spec first: `../plans/2026-09-30-backup-restore-ui-design.md`, sections "The screen",
"Behaviour rules" 1-4, "Component map" and "State model". The plan's argument is
`../plans/2026-09-29-backup-restore-compose.md`, "Where it should live" and "Compose, not Views".

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies — in particular do not add
`lifecycle-viewmodel-compose`; the project acquires ViewModels Fragment-side with `by viewModels()`.

## Why this step exists

An audit of the current code found both directions are non-functional, and one of them fails in the
exact way issue #92 did:

- `BackupRestoreScreen.kt` has two placeholder callbacks whose bodies are comments — "Fragment will
  handle SAF launch" and "confirmation dialog handled by Fragment". The fragment does not exist.
- `BackupRestoreViewModel.performBackup`, `.onArchivePicked` and `.onRestoreConfirmed` have **zero
  call sites**: the copy-to-destination, the entry preview, and the restore execution are all
  unreachable.
- `BackupRestoreViewModel.startBackup` is worse than unwired. It builds a zip in the cache, reports
  `Phase.Done`, and deletes the file — **without ever firing `CreateDocument`**. Wired to a button as
  it stands, the screen would tell the user "Exported: backup-….zip" for a backup that reached
  nowhere. That is the discarded-`Result` bug from #92, reintroduced one layer up.

The flow the design specifies, and the one the old Settings flow used, is: choose destination first,
then write. So `CreateDocument` fires, and only when a destination URI comes back does the archive get
built and copied.

## Work in two commits, in this order

A previous attempt at a large single-file write was cut off by an upstream timeout. Commit and push
after each of these two, so an interruption costs at most one.

### Commit 1 — the host fragment and the wiring

**Create `app/src/main/kotlin/com/aistra/hail/ui/backup/BackupRestoreFragment.kt`.** Read
`AboutFragment.kt` first and match it: the bare `ComposeView` in `onCreateView` with
`DisposeOnViewTreeLifecycleDestroyed`, `AppTheme` wrapping the content, no `Scaffold`, no ViewBinding,
no layout XML. The class name must be exactly the `android:name` already present for `nav_backup` in
`res/navigation/mobile_navigation.xml` — read that file and match it.

The fragment owns the two SAF launchers, because `registerForActivityResult` needs a Fragment:

- `CreateDocument("application/zip")` for the backup direction. Its callback passes the URI to
  `viewModel.performBackup(uri, context)` and does nothing when the URI is null.
- `OpenDocument()` restricted to `arrayOf("application/zip")` for the restore direction. Its callback
  passes the URI to `viewModel.onArchivePicked(uri, context)` and does nothing when null.

Obtain the ViewModel with `by viewModels()`, matching `AppsFragment`.

**Change `BackupRestoreScreen`'s signature so the host supplies the actions.** The screen cannot
launch SAF itself, so it takes the ViewModel plus a callback for each host-owned action. Keep the
existing parameter list otherwise; the ViewModel stays required with no default. Then replace the two
comment-placeholder callback bodies with the real ones:

- "Create backup" → the callback that fires `CreateDocument`.
- "Choose backup file…" → the callback that fires `OpenDocument`.
- "Restore selected" → the callback that shows the confirmation.

**Add the restore confirmation, the one View dialog the design keeps**: a
`MaterialAlertDialogBuilder` from the fragment, titled with `msg_restore_confirm_title`, whose body is
`msg_restore_confirm_body` filled with the names of the selected categories, cancelled on dismiss, and
on OK calling `viewModel.onRestoreConfirmed(context)`. Build the category list from the loaded
restore state, and read the design's copy table for the exact strings — `msg_restored` and
`msg_not_a_backup` already exist; you will need to add one new key for the button label, because the
design's "Restore selected" wording has no key in the copy table. Add it to `res/values/strings.xml`
in the file's existing style and note the key you chose in your report.

**Remove `startBackup` from the ViewModel**, and delete its call site in the screen. It exists only to
produce a false success, and its work is `performBackup`'s. Do not leave a deprecated wrapper.

**Intercept back while an operation is in flight**, per behaviour rule 1: while either section is
`Phase.Working`, the back press is consumed and a snackbar with `msg_wait_for_operation` is shown.
Register the callback in the fragment's `onCreate`, not inside the composable. When nothing is
running, back must navigate up normally.

### Commit 2 — the staged archive's teardown paths

Behaviour rule 2 requires one owner and one teardown path. The audit found three holes:

1. **A failed restore never deletes the staged archive.** Add the delete to the failure branch beside
   the success branch, so the file is released whether the restore succeeded or not.
2. **`onCleared` only deletes when nothing is `Working`.** A restore that ended in `Failed` leaks the
   file on process death. Re-read the current guard and make it delete in every case where no
   operation is actually in flight — the file is worthless once the ViewModel is gone, and the
   `Working` guard should only survive if you can justify it from the source; if you cannot, delete
   unconditionally and say so in your report.
3. **The failure path in `onArchivePicked` deletes the staged file and then builds a
   `RestoreState.Loaded` that still points at it.** A `Loaded` state must never reference a file that
   no longer exists. Make the not-a-backup case leave the restore section able to show the error line
   and keep the picker available, per the design's "Empty and disabled states" table.

Do not otherwise restructure the ViewModel. `performBackup`, `onArchivePicked` and
`onRestoreConfirmed` keep their names and signatures — the wiring you add in commit 1 calls them.

## Not in this step

The Settings deletion, the entry-row and disabled-state polish, and the `entriesOf` tidy are later
tasks. Leave `SettingsFragment.kt` alone. Do not touch `HBackup`. Do not delete
`R.layout.dialog_progress`.

## Report

Append a step-5 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md` (create it if absent).
Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- The two commits with short SHAs and subjects
- **CI:** run id and conclusion for the head, confirmed on this branch
- The new string key you added, and its English text
- How back interception is wired, with `file:line`
- What you deleted from `startBackup`'s call path
- Anything the design required that you could not do, and why
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.