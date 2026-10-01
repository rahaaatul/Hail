# PR Review: Tests and Resources

## Verdict: CHANGES_REQUESTED

---

## Blocking Issues

### Tests

| File:Line | Issue | Why It Matters |
|-----------|-------|----------------|
| `BackupPreviewTest.kt` (no test) | **Duplicate-entry handling not tested** — the implementation at `BackupPreview.kt:67` uses `entries[category] = entry.name to size` (last wins), and the doc comment promises "last one encountered is the one reported". No test creates a zip with multiple `apps.json` (or other) entries to verify this. Mutation: change line 67 to `entries[category] = entries[category] ?: (entry.name to size)` (first wins) — all tests still pass. | Silent contract violation if a backup tool or user produces a zip with duplicate entries; restore path reads sequentially so preview must match. |
| `BackupPreviewTest.kt` (no test) | **`ZipEntry.getSize() == -1` clamp not exercised** — `BackupPreview.kt:66` clamps `entry.size < 0` to `0L`. The test helper `writeZip` uses `ZipOutputStream` which always writes known sizes. No fixture produces a central-directory entry with size `-1` (unknown size, valid in ZIP spec). Mutation: remove the clamp (`val size = entry.size`) — all tests still pass. | Unknown-size entries (streamed/zip64) would report negative sizes to UI, breaking display logic. |
| `BackupPreviewTest.kt` (no test) | **Unreadable/truncated zip not tested** — the function declares `@throws ZipException` but no test passes a corrupt file. Mutation: wrap body in `try { … } catch (e: ZipException) { return emptyList() }` — all tests still pass. | Users selecting damaged files get a crash instead of a handled error state. |

### Resources

| File:Line | Issue | Why It Matters |
|-----------|-------|----------------|
| `strings.xml:125` | **`label_in_this_archive` added but never referenced** — grep finds only the definition. | Dead resource inflates APK, confuses translators, signals incomplete feature. |

---

## Nits

| File:Line | Issue | Why It Matters |
|-----------|-------|----------------|
| `BackupPreviewTest.kt:188-204` | Zero-byte entry test exists but is the only size edge case; the `-1` (unknown) case is distinct and untested (see blocking above). | Incomplete boundary coverage. |
| `BackupPreviewTest.kt:36-69` | `entryCategory` tests are thorough for the helper but don't exercise the integration path through `entriesOf` (e.g., nested `subdir/apps.json` returns null — verified in isolation but not via `entriesOf`). | Minor; integration tests cover the main flows. |

---

## Tests That Would Pass Against a Deliberately Broken Implementation

| Broken Mutation | Tests That Still Pass | Why |
|-----------------|----------------------|-----|
| `entries[category] = entries[category] ?: (entry.name to size)` (first-wins instead of last-wins) | All 18 `entriesOf` tests | No test creates duplicate entries in the zip. |
| `val size = entry.size` (drop `-1` clamp) | All 18 `entriesOf` tests | No test fixture produces `entry.size == -1`. |
| `try { … } catch (e: ZipException) { return emptyList() }` (swallow corrupt zip) | All 18 `entriesOf` tests + all `canStartRestore` tests | No test passes a corrupt/unreadable zip file. |

---

## Strings Added by This Change (with values and reference confirmation)

| String Name | Value | Referenced? | Location(s) |
|-------------|-------|-------------|-------------|
| `msg_restored` | `Restored: %s` | ✅ | `BackupRestoreViewModel.kt:176` |
| `title_backup_restore` | `Backup & restore` | ✅ | `mobile_navigation.xml:41`, `SettingsFragment.kt:322` |
| `summary_backup_apps` | `Pinned and selected apps` | ✅ | `BackupRestoreScreen.kt:84` |
| `summary_backup_whitelist` | `Apps excluded from freezing` | ✅ | `BackupRestoreScreen.kt:93` |
| `summary_backup_actions` | `Freeze and unfreeze recipes` | ✅ | `BackupRestoreScreen.kt:102` |
| `summary_backup_settings` | `Preferences and app config` | ✅ | `BackupRestoreScreen.kt:111` |
| `action_create_backup` | `Create backup` | ✅ | `BackupRestoreScreen.kt:123` |
| `action_choose_archive` | `Choose backup file…` | ✅ | `BackupRestoreScreen.kt:342` |
| `action_change_archive` | `Change` | ✅ | `BackupRestoreScreen.kt:381` |
| `action_restore_selected` | `Restore selected` | ✅ | `BackupRestoreFragment.kt:95`, `BackupRestoreScreen.kt:201` |
| `summary_choose_archive` | `Pick a .zip this app wrote. Nothing is read until you choose one.` | ✅ | `BackupRestoreScreen.kt:352` |
| `label_in_this_archive` | `In this archive` | ❌ **UNUSED** | — |
| `label_not_in_archive` | `Not in this archive` | ✅ | `BackupRestoreScreen.kt:399` |
| `msg_exporting` | `Exporting %1$s…` | ✅ | `BackupRestoreScreen.kt:133` |
| `msg_importing` | `Restoring %1$s…` | ✅ | `BackupRestoreScreen.kt:211` |
| `msg_restore_confirm_title` | `Restore this backup?` | ✅ | `BackupRestoreFragment.kt:93` |
| `msg_restore_confirm_body` | `%1$s will be replaced on this device. This cannot be undone.` | ✅ | `BackupRestoreFragment.kt:94` |
| `msg_wait_for_operation` | `Wait for the current operation to finish` | ✅ | `BackupRestoreFragment.kt:49` |
| `msg_not_a_backup` | `Not a Hail backup` | ✅ | `BackupRestoreViewModel.kt:126` |

**Removed string**: `no_items_to_select` ("No items to select") — was **not referenced** anywhere (all usages point to `msg_no_items_to_select` which remains with updated text "Select at least one item"). Clean removal.

**Modified string**: `msg_no_items_to_select` — changed from "No items to select" to "Select at least one item"; all 4 references (3 in backup UI, 1 in `PagerFragment.kt`) remain valid.

---

## Hardcoded User-Visible Text Check

- `BackupRestoreScreen.kt`: **All** user-visible text uses `stringResource(R.string.xxx)` — no hardcoded literals.
- `BackupRestoreFragment.kt`: **All** user-visible text uses `getString(R.string.xxx)` or `R.string.xxx` — no hardcoded literals.
- `SettingsFragment.kt:322`: Uses `stringResource(R.string.title_backup_restore)` — correct.

---

## Navigation Entry

- `mobile_navigation.xml:37-41`: `<fragment android:id="@+id/nav_backup" android:name="com.aistra.hail.ui.backup.BackupRestoreFragment" android:label="@string/title_backup_restore" … />` — destination exists, label string exists and is referenced.

---

## Could Not Check Without Building

- Actual APK string resource table to confirm no duplicate string keys or missing entries at compile time.
- Runtime behavior of `ZipFile.entries()` iteration order with duplicate names (JVM implementation detail) — the "last wins" claim depends on `ZipFile.entries()` returning duplicates in central-directory order, which is typical but not explicitly specified.
- Whether `label_in_this_archive` is referenced via generated `R.string` lookup from a dynamic string name (e.g., `getString(resources.getIdentifier(...))`) — grep would miss this, but codebase shows no such pattern.