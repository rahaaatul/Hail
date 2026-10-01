# Task 2b step 6 — retire the old Settings flow

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `cb90199` and
CI is green on it. Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed` for the
log. Report only run ids that `gh` shows for this branch.

Read the binding spec first: `../plans/2026-09-30-backup-restore-ui-design.md`, "Decisions" (the
"one row or two" row), "Removed", and the "Where it should live" section of
`../plans/2026-09-29-backup-restore-compose.md`.

This step removes the flow the new screen replaces, and points the one surviving row at it. It does
not change the new screen.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies.

## In `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`

Delete, in full:

- `backupLauncher` (currently line 87) and `restoreLauncher` (currently line 131) — both SAF
  launchers, with their whole callback bodies.
- the `pendingBackupOptions` field (currently line 161).
- `showBackupDialog` (currently line 770) and `showRestoreDialog` (currently line 800), with their
  whole bodies.
- the `restore` preference row (currently around line 415) — the one that launched `restoreLauncher`.

Then fix what the deletions leave behind, which is the part that needs judgement:

1. **The surviving row.** The `backup_preference` entry (currently around line 406) keeps its key and
   its place in the `backup` preference category, but its title must now cover both directions and its
   `onClick` must navigate to the new destination. Read `res/navigation/mobile_navigation.xml` for the
   `nav_backup` id and copy the navigation idiom the other rows in this `LazyColumn` already use — the
   file navigates somewhere else and should not need a new pattern.
2. **The row's title.** The design says the row is titled "Backup & restore". The new destination's
   nav label already carries that string (`title_backup_restore`), so reuse that resource rather than
   adding a second string with the same English text. Report which resource you used.
3. **Imports.** Deleting ~180 lines will orphan a lot: the two `ActivityResultContracts` imports, the
   `CreateDocument`/`OpenDocument` imports, `File`, `DocumentFile`, `DocumentsContract`, `BackupOptions`,
   `RestoreOptions`, `HBackup`, `HFiles`, `MaterialAlertDialogBuilder` if nothing else in the file uses
   it, and anything else the compiler then reports. Work from the compiler's list rather than guessing,
   and **do not remove an import that another surviving function still needs** — `showTerminalDialog`
   and the rest of the file are untouched and still compile today.
4. **What must survive.** `showTerminalDialog`, `islandPermissionRequest`, the icon-pack state, the
   `AppMetaCache`-driven refreshes, and every other screen behaviour in this file. Do not "tidy" anything
   you were not asked to remove. This file is large and its remaining contents are not your concern.

## Two things that are explicitly not removed

- **`R.layout.dialog_progress` stays in the repo**, because `AppsFragment` still uses it for the app
  list. Only the backup and restore flows go. If the compiler flags `dialog_progress` as unused *in
  SettingsFragment*, that is expected: remove the import from this file, not the layout file.
- `msg_imported` stays in `strings.xml`: `PagerFragment` uses it for importing an app list, which this
  branch does not touch. If a `msg_imported` reference remains in SettingsFragment after the deletions,
  it is inside code you were supposed to delete — find out why before assuming the string is unused.

## Also verify, do not assume

After the deletions, grep this file for `nav_backup` and confirm the row really navigates, and confirm
that no reference to any deleted symbol survives anywhere in the module:

```
grep -rn "showBackupDialog\|showRestoreDialog\|pendingBackupOptions" app/src/main/kotlin
```

That must come back empty. Report the command and its output.

## Report

Append a step-6 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md` (create it if absent).
Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHA and subject
- **CI:** run id and conclusion for the head, confirmed on this branch
- The resource you used for the row's title, and the navigation idiom you followed with `file:line`
- The grep command from the brief and its output
- Every import you removed, and confirmation that no surviving function lost one it needed
- Anything you deleted or kept that the brief did not name, and why
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.