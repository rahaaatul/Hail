# SDD ledger — plan: docs/superpowers/plans/2026-09-09-backup-restore.md

## Preflight scan

| Tasks sharing files/interfaces | Finding | Ruling |
|---|---|---|
| Task 1 produces `HBackup.kt`/`HBackupTest.kt`; Task 2 consumes `HBackup` in `PagerFragment.kt` | Interface contract matches: `backup(Context, File, BackupOptions)` and `restore(Context, File, RestoreOptions)` | Proceed as planned |
| Task 1 tests mock `PreferenceManager.getDefaultSharedPreferences`; Task 2 uses real context | No conflict — Task 1 tests are unit tests with mocks, Task 2 is integration | Proceed as planned |
| Task 2 modifies `menu_home.xml`; no other task touches it | No conflict | Proceed as planned |
| Task 3 modifies `strings.xml`; no other task touches it | No conflict | Proceed as planned |
| Task 4 extends `HBackupTest.kt` from Task 1 | Test file created in Task 1, extended in Task 4 — correct dependency | Proceed as planned |

**Scan result: Clean. No conflicts.**

## Task progress

### Task 1: Create HBackup Utility Class
Status: complete (commits b75aedd..4767e13, review clean)

### Task 2: Add Backup/Restore Menu Items to PagerFragment
Status: pending

### Task 3: Add String Resources
Status: pending

### Task 4: Write Additional Edge Case Tests
Status: pending

### Task 5: Verify Build and Lint
Status: pending
