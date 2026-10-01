# Spec Compliance Review: BackupRestoreScreen

## Verdict: CHANGES_REQUESTED

## Blocking Issues

**BackupRestoreScreen.kt:70** — Missing horizontal divider between Backup and Restore sections — Spec line 77 explicitly shows `horizontalDivider()` between the two sections; the LazyColumn uses only `Arrangement.spacedBy(padding_medium)` which produces spacing, not a divider.

**BackupRestoreScreen.kt:369** — Restore file slot loaded supporting text omits source location — Spec line 92 shows supporting text as "54.9 MB · 29 Sep 2026, 12:00 · from Downloads" (three parts, 2 lines max); code at line 369 produces only "size · date" (two parts), missing the "from <source>" segment.

**BackupRestoreScreen.kt:176-194** — Missing "In this archive" label before restore entry rows — Spec line 95 shows a label "In this archive" above the entry list when an archive is loaded; code renders entries directly with no such label. The string `label_in_this_archive` is defined in spec line 258 but unused.

## Nits

**BackupRestoreScreen.kt:399** — Absent entry supporting text uses reduced alpha — Spec line 222 says absent entry supporting text uses `onSurfaceVariant`; code line 405 applies `.copy(alpha = 0.6f)`. Not forbidden by spec but deviates from the stated token.

**BackupRestoreScreen.kt:49** — Snackbar anchored to `activity.fab` which may not exist — Fragment uses `Snackbar.make(activity.fab, ...)`; if the hosting activity lacks a FAB this will crash. Spec line 146 only requires the snackbar text, not the anchor.

**BackupRestoreScreen.kt:74,150** — Section header strings not explicitly verified — Code uses `R.string.title_backup` and `R.string.action_restore` for "Backup" and "Restore" section headers; spec ASCII art shows plain "Backup"/"Restore" but copy table does not name these keys. Cannot confirm exact text match without string resources.

**BackupRestoreScreen.kt:133,211** — Progress labels depend on ViewModel message content — Spec lines 117,121 show "Exporting <filename>…" and "Restoring…"; code uses `msg_exporting`/`msg_importing` with `backupState.message` and `loadedState.displayName`. Correctness depends on ViewModel populating these fields as the spec expects (filename for backup, filename for restore). Not verifiable without ViewModel.

## Spec Sections Checked

- Idle state row tables (spec lines 56–85)
- Restore, archive loaded (spec lines 87–103)
- Working state (spec lines 110–123)
- Result rendering (spec lines 125–132)
- Empty and disabled states table (spec lines 134–143)
- Component map table (spec lines 218–229)
- Behaviour rules 1, 4 (spec lines 144–160) — fragment back interception and restore confirmation
- Copy table (spec lines 247–268) — string key usage
- Removed section (spec lines 275–281) — not applicable to new screen code

## Could Not Verify Without Building

- Exact string resource values for section headers (`title_backup`, `action_restore`, `backup_apps`, `backup_whitelist`, `backup_actions`, `backup_settings`) — spec copy table omits headline keys.
- ViewModel population of `backupState.message` during Working phase (must be filename for "Exporting <file>…" to match spec).
- Staged archive lifecycle (teardown on success/failure/Change/destroy) — ViewModel responsibility.
- `AppMetaCache.invalidateAll()` and `invalidateOptionsMenu()` calls after restore — ViewModel responsibility.
- `canStartBackup` / `canStartRestore` pure function logic — imported from `com.aistra.hail.backup`, not in reviewed files.
- `BackupEntry.entriesOf(zipFile)` zip parsing — not in reviewed files.