# Correctness Review: Backup/Restore ViewModel & Preview

## Verdict: CHANGES_REQUESTED

---

## Blocking Issues

**BackupRestoreViewModel.kt:65-88** — Backup cache file leaked on coroutine cancellation  
`cacheFile` is created at line 65 but only deleted at line 87. If the coroutine is cancelled between `HBackup.backup` (line 66) and line 87 (e.g., ViewModel cleared, process killed, user navigates away), the temporary `backup-<ts>.zip` remains in `cacheDir` indefinitely. No `finally` block, no `onCleared` tracking.

**BackupRestoreViewModel.kt:165-182** — Restore staged file leaked on coroutine cancellation during restore  
`onRestoreConfirmed` launches a coroutine that calls `HBackup.restore` then `deleteStagedFile()` in both success/failure handlers. No `finally` block. If cancelled during `HBackup.restore` (line 166), the staged file is never deleted. `onCleared` only runs when ViewModel is cleared, not on coroutine cancellation.

**BackupRestoreViewModel.kt:174-180** — `RestoreState.Loaded` retains reference to deleted file after restore  
Both success (line 174) and failure (line 178) paths call `deleteStagedFile()` *then* update state with `current.file` (captured at line 161). The `Loaded.file` field points to a `File` that no longer exists on disk. Subsequent UI actions (e.g., reading `file.name` for display, or re-attempting restore) operate on a deleted file.

**BackupRestoreViewModel.kt:97-127** — Staged file leaked if `onArchivePicked` coroutine cancelled *after* copy succeeds but *before* state update  
If cancellation occurs between line 108 (copy complete) and line 113 (state update), the `runCatching` completes successfully but `.onSuccess` never runs. The staged file exists and `stagedFile` holds it, but state remains `Empty`. `onCleared` will clean it up *only if* ViewModel is cleared; otherwise it leaks until next `onArchivePicked` (line 97 calls `deleteStagedFile()` first) or `onChangeArchiveClick`.

---

## Nits

**BackupPreview.kt:51-53** — KDoc claims "multiple entries with the same name" but code keys by category  
The map key is `BackupCategory` (line 60), not entry name. Two entries with different names mapping to the same category (theoretically possible if `entryCategory` recognised more names) would have the last win. The KDoc describes a different deduplication strategy than implemented.

**BackupPreview.kt:51-53** — KDoc claims last entry wins per `ZipFile.entries()` order, but iteration order is unspecified  
`ZipFile.entries()` returns an `Enumeration` whose order is not guaranteed by the ZIP spec. The "last encountered" is implementation-dependent (typically central directory order, but not mandated). The claim of deterministic "last wins" is not guaranteed by the code.

**BackupPreview.kt:55** — KDoc declares only `ZipException` thrown, but `IOException` (file not found, permission denied) also possible  
`ZipFile(File)` constructor throws `IOException` for missing/unreadable files. Caller (`onArchivePicked:110`) wraps in `runCatching` so it's handled, but the KDoc is incomplete.

**BackupRestoreViewModel.kt:68-74** — `runCatching` for destination copy swallows `CancellationException` as failure  
`runCatching` catches `Throwable`. If coroutine cancelled during copy, `CancellationException` is caught, treated as copy failure, and reported as "operation_failed". Cancellation should propagate, not be reported as a user error.

**BackupRestoreViewModel.kt:99-127** — `runCatching` for SAF copy swallows `CancellationException` as "Not a Hail backup"  
Same issue: cancellation during `contentResolver.openInputStream` or copy is caught and mapped to `msg_not_a_backup` (line 126), misleading the user.

**HBackup.kt:83-100** — Backup writes directly to output file; partial write on failure leaves a valid-but-incomplete zip  
If exception occurs after some entries written (e.g., `writeActionsJson` fails), `zipOutputStream.close()` in `finally` finalises a zip containing only the entries written so far. Since `performBackup` writes to a cache file first and only copies on full success, the user's destination is protected — but the cache file (if not deleted due to cancellation leak above) would be a valid zip with a subset of categories, which a later `entriesOf` would read as a seemingly valid backup.

**HBackup.kt:110-128** — Restore has no transaction semantics; partial restore on mid-stream failure  
If `readAppsJson` succeeds but `readWhitelistJson` throws, apps are already restored. No rollback. This is a design limitation, not a bug per se, but worth noting.

---

## Teardown Table: Staged File Deletion per Exit Path

| Exit Path | Staged File Deleted? |
|-----------|---------------------|
| `onArchivePicked` — copy succeeds | **No** (kept intentionally, referenced in `Loaded.state`; deleted later) |
| `onArchivePicked` — copy fails (IO error) | **Yes** (line 124) |
| `onArchivePicked` — coroutine cancelled *during* copy | **Yes** (caught by `runCatching` → `.onFailure`) |
| `onArchivePicked` — coroutine cancelled *after* copy, *before* state update | **No** (leak until next pick/change/clear) |
| `onRestoreConfirmed` — restore succeeds | **Yes** (line 174) |
| `onRestoreConfirmed` — restore fails | **Yes** (line 178) |
| `onRestoreConfirmed` — coroutine cancelled during restore | **No** (leak) |
| `onChangeArchiveClick` | **Yes** (line 142) |
| `onCleared` (ViewModel destroyed) | **Yes** (line 196) |
| Backup `cacheFile` — success | **Yes** (line 87) |
| Backup `cacheFile` — copy to destination fails | **Yes** (line 87) |
| Backup `cacheFile` — coroutine cancelled before line 87 | **No** (leak, not tracked by ViewModel) |

---

## False KDoc / Comment Claims

1. **BackupPreview.kt:43-44** — "The returned list always contains exactly four entries in fixed order: APPS, WHITELIST, ACTIONS, SETTINGS."  
   **Status: TRUE** — Code at lines 71-76 guarantees this.

2. **BackupPreview.kt:46-47** — "`sizeBytes` is the uncompressed size from the central directory, or 0 when the size is unknown (-1 in the ZIP format) or the category is absent."  
   **Status: TRUE** — Code at line 66 handles `entry.size == -1` → `0L`.

3. **BackupPreview.kt:51-53** — "If the zip contains multiple entries with the same name, the last one encountered (the one `ZipFile.entries()` yields last) is the one reported, matching the behaviour of the restore path which reads entries sequentially."  
   **Status: FALSE** — Code keys by `BackupCategory`, not by name. Restore path (`HBackup.restore` lines 112-123) processes entries sequentially and *last write wins per category*, which matches the category-keyed behaviour, but the KDoc's "same name" wording is wrong.

4. **BackupPreview.kt:55** — "@throws java.util.zip.ZipException if the file is not a valid zip archive or cannot be read."  
   **Status: FALSE** — Also throws `IOException` for missing/unreadable files.

5. **BackupRestoreViewModel.kt:195-196** — "Staged file is worthless once ViewModel is cleared; delete unconditionally."  
   **Status: MISLEADING** — Only deletes `stagedFile` (restore). Backup `cacheFile` is not tracked and leaks on cancellation.

---

## Cannot Verify Without Building

- **`HFiles.copy` behaviour on cancellation**: `HFiles.copy` uses `source.ktCopyTo(target)` inside `withContext(Dispatchers.IO)`. `ktCopyTo` is a blocking copy; cancellation behaviour depends on whether the streams are interruption-aware. Not verifiable without runtime test.

- **`ZipFile.entries()` iteration order on Android**: The claim that "last encountered" is deterministic relies on Android's `ZipFile` implementation returning entries in central directory order. This is true for current implementations but not spec-guaranteed.

- **`HBackup.backup`/`restore` actual exception types**: The `runCatching` wrappers catch all `Throwable`. Whether specific exceptions (e.g., `JSONException`, `ZipException`) are properly propagated to UI messages depends on `exceptionOrNull()?.message` which may be null for some exceptions.

- **Race between `onCleared` and in-flight coroutine**: If `onCleared` runs while `onRestoreConfirmed` coroutine is at `HBackup.restore`, `deleteStagedFile()` deletes the file out from under the restore. `HBackup.restore` opens a `FileInputStream` on that file — behaviour on deleted-but-open file is OS-dependent (works on Linux, may fail on some filesystems).

- **SAF `openInputStream` returning null**: Line 101-102 throws `IOException("Null input stream")`. Whether this actually occurs for valid URIs on all Android versions/storage providers is not verifiable statically.