# Task 2b step 7 — screen conformance, entry names, `entriesOf` tidy

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `3abfbd9` and CI
is green on it. Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed` for the log.
Report only run ids that `gh` shows for this branch.

Read the binding spec before you start: `../plans/2026-09-30-backup-restore-ui-design.md` — the
"Restore, archive loaded" row table (around line 86), "Empty and disabled states" (around line 134),
and the composable table (around line 221).

Seven items. Six are mechanical. The first one is the substantive change and it is test-driven, so do
it first and in the order written here.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies.

---

## 1. `BackupEntry` needs the entry name — TDD, tests first

`data class BackupEntry` (in `app/src/main/kotlin/com/aistra/hail/backup/BackupPreview.kt`, currently
lines 11-15) has `category`, `present`, `sizeBytes` and no name, which is why the screen cannot show
what the spec requires. The spec's row table shows the *file* name as the headline — `apps.json`,
`whitelist.json`, `actions.json`, `settings.json` — and it shows it even for an entry that is **not in
the archive**, where the name still reads `settings.json`.

So add a `name: String` field carrying the archive's own entry name, and fall back to the canonical
name for that category when the category is absent. Those four names are the ones `HBackup` writes:

```
FILE_APPS = "apps.json"  FILE_WHITELIST = "whitelist.json"
FILE_ACTIONS = "actions.json"  FILE_SETTINGS = "settings.json"
```

They are `private const val` in `HBackup.kt` (lines 39-42), and the same four literals are the arms
of `entryCategory` in `BackupPreview.kt` (lines 32-38). Do not widen `HBackup`'s visibility to reuse
its constants, and do not add a second copy of the literals as public constants unless you decide
that is genuinely better than duplicating them — either way, say which you chose and why in the
report. Keeping one place that maps name to category is worth more than avoiding four literals.

**Tests first, in this order:**

1. Write the new assertions in `BackupPreviewTest.kt` — that a present entry reports the archive's own
   entry name, and that an absent category still reports its canonical name with `present == false`.
   Also assert the four-entry fixed order still holds, since the constructor gained a parameter.
2. Commit and push **the tests only**. Read the CI run for that push and confirm it **fails** on the
   new assertions. Paste the failing test names into your report. If the run is green, stop and say so
   — a green run there means your assertions do not test what you think they test.
3. Then implement, and finish only when CI is green.

That red-then-green sequence is the deliverable, not a formality. An earlier round in this task was
falsely reported as green, so report exactly what `gh` printed.

## 2. Restore row headlines show the entry name, not the category label

`RestoreEntryRow` in `BackupRestoreScreen.kt` computes a `categoryName` from a `when` over
`entry.category` (lines 407-412) and puts it in `headlineContent` (line 433). That is the wrong text
and the `when` becomes dead code once the headline reads `entry.name`. Delete the `when` and the
`categoryName` val; the headline is the name from item 1.

The supporting line is already right and stays: formatted size when present, `label_not_in_archive`
when not.

## 3. `msg_no_items_to_select` says the wrong thing

The spec (lines 138 and 140) requires the supporting text under a disabled button to read **"Select at
least one item"**, in the place the old code used to fire a toast. The current value is "No items to
select", which is a statement about the archive rather than an instruction to the user. Change the
string's value to "Select at least one item". One line, no new string.

## 4. The disabled reason renders twice

`BackupActionButton` and `RestoreActionButton` each render `DisabledReasonText` themselves when
disabled with a reason (`BackupActionButton` lines 280-282, `RestoreActionButton` lines 464-466).
The call sites then render a second one: lines 131-135 for backup and 211-215 for restore, each
guarding on `!canRestore && loadedState.message != null` — that guard is the same condition the button
already used for its own `disabledReason`. So the user sees the reason twice.

Delete the call-site duplicates. The `disabledReason` parameter the buttons already take is the
single place that text belongs. After deleting, `backupState.message` / `loadedState.message` are no
longer read in the `Phase.Idle` branch — do not go looking for another use for them, the `Working`,
`Done` and `Failed` branches are where messages are shown.

## 5. Controls stay live during `Working` — the spec says they must not

Spec line 142: "Working | Both sections' controls disabled; the system back button intercepted."

- `BackupOptionRow` (lines 238-264) takes no `enabled` parameter and is not disabled in any phase, so
  the four backup checkboxes stay tappable while a backup is running. Give it an `enabled` parameter,
  pass it through to `Modifier.toggleable` and to the `Checkbox`, and pass
  `enabled = backupState.phase != Phase.Working` from the four call sites (lines 83-118).
- `RestoreEntryRow` already takes `enabled`, but it currently means only "is this entry in the
  archive" (`enabled = entry.present`, line 194). That has to become the conjunction of present and not
  working: an entry in the archive must still be untappable while a restore is running. Rename the
  parameter or introduce a second one so the two meanings are not conflated at the call site — a
  reader should not have to know which of the two `enabled` senses a given call site meant.
- Make the restore button and the archive "Change" button consistent with that: while the restore is
  `Working`, the section shows only the progress row, so this mostly falls out — but confirm it, and
  confirm the back interception in `BackupRestoreFragment.kt` still covers both directions.

The supporting text for a disabled row stays `label_not_in_archive` when the entry is absent. Do not
invent a second "busy" string; the progress row is the busy indicator and the spec does not ask for
one.

## 6. `entriesOf`'s presence map is redundant — tidy it

In `entriesOf` (lines 54-76) the map value is `Pair<Boolean, Long>` and every write is
`true to size`. Presence is therefore just "is this category a key in the map", and the `Boolean` half
carries no information. Collapse the value to `Long`, drop the `(false to 0L)` default for the lookup,
and derive `present` from map membership. While you are there, populate the new `name` field from
`entry.name` for present categories and from the canonical name otherwise.

Behaviour that the existing tests already pin must not change: four entries, fixed order APPS /
WHITELIST / ACTIONS / SETTINGS, unknown ZIP sizes clamped to `0` rather than surfacing `-1`, and the
duplicate-entry and unreadable-zip handling already documented in that file's KDoc. Read the KDoc and
keep it accurate — if your change alters what a reader would need to know, update it.

## 7. Three bits of dead weight in `BackupRestoreScreen.kt`

- `BackupOptionRow`'s `category: BackupCategory` parameter (line 241) is never used in the body. The
  four call sites pass it. Delete the parameter and the four arguments.
- Line 174, `val loadedState = restoreState as RestoreState.Loaded`, is inside an `is RestoreState.Loaded`
  branch where the smart cast already applies. Drop the redundant cast.
- `RestoreFileSlotLoaded` (lines 368-397) wraps its single `ListItem` in a `Column` with one child and
  one spacing value. Drop the `Column` and return the `ListItem`.

## Finish the round only when CI is green

Push, read the run, and keep going on anything the compiler reports that this brief did not name. The
round is done when the build compiles and the unit tests pass, with the red-then-green sequence from
item 1 in the report as evidence.

Two commits are expected and acceptable — the tests-only commit from item 1, then the implementation.
No amend, no rebase, no force-push. Do not open or merge a PR, do not post on GitHub, push only from
`worktree-prB`.

## Report

Append a step-7 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md`. Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHAs and subjects
- **CI:** every run id and conclusion, confirmed on this branch. Include the **red** run from item 1
  and the failing test names it reported, then the final green run.
- Your choice on where the four canonical names live, and why
- Each of the seven items: done or not, and anything you did differently from this brief
- For item 5: the two senses of `enabled` and how you named them
- Confirmation that the four-entry fixed order, the size clamp, and the duplicate/unreadable-zip
  handling still hold
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.