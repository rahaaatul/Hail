# Task 2b fix round 5a — ViewModel hardening

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `958e631`, CI green
(`36906244890`). Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed`.

**One file only: `app/src/main/kotlin/com/aistra/hail/ui/backup/BackupRestoreViewModel.kt`.** Do not edit
anything else. Four fixes, all in that file. No new tests are possible here — these are Android-coupled
paths with no JVM seam — so CI proving it compiles and the existing suite still passes is the whole
verification, and you should say so plainly in your report rather than implying test coverage.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path.

## 1. Stop deleting the staged archive out from under the state that references it

`onRestoreConfirmed` calls `deleteStagedFile()` and then publishes a `RestoreState.Loaded` that still
holds that file — lines 174-176 on success, 178-180 on failure. The state therefore describes a file
that no longer exists: `BackupRestoreScreen` calls `state.file.lastModified()` for that row, so it
shows `0` bytes and a 1970 date, and a second tap on Restore calls `HBackup.restore` on a missing path.

**Remove `deleteStagedFile()` from both arms of `onRestoreConfirmed`.** Let the file survive the restore
so the result line can still describe what was restored. It stays deleted at the two points where the
state stops referencing it — `onChangeArchiveClick` (line 141) and `onCleared` (line 193) — which
already call it. Nothing else needs to change for this item.

Accept deliberately: one file, bounded, in `cacheDir`, alive until the user picks another archive or
leaves. Do not compensate by clearing the state after deleting; losing the result message is worse.

## 2. The backup cache file leaks when the coroutine is cancelled

`cacheFile.delete()` is the last statement of the `performBackup` coroutine body (line 87).
`HBackup.backup(...)` at line 66 returns a `Result` rather than throwing, so it is not covered by any
`runCatching` — a cancellation during it ends the coroutine there and line 87 never runs. The file
stays in `cacheDir` permanently, which is what happens when the user navigates away mid-backup, because
`viewModelScope` is cancelled in `onCleared`.

Wrap the body so the delete happens on every exit path: `try`/`finally`, with `cacheFile.delete()` in
the `finally`. Two requirements beyond the obvious:

- the `finally` must not throw and mask the real outcome;
- a failed delete must **not** be turned into a user-visible backup failure.

Then re-read items 1-3 together and confirm you have not reintroduced an early `return` inside the
`try` that would skip the state update the UI depends on.

## 3. `runCatching` is swallowing `CancellationException`

Two sites: line 68 (copying to the chosen destination) and line 99 (copying the picked archive in).
`runCatching` catches `Throwable`, so cancellation inside those blocks is caught and converted into an
ordinary failure — at line 68 into a user-visible "operation failed", at line 99 into "Not a Hail
backup". A cancelled screen must not claim the user's file is corrupt, and swallowing it breaks
structured concurrency: the coroutine keeps running instead of unwinding, so anything relying on
unwinding never happens.

Re-throw cancellation before treating a failure as real, at both sites. Prefer explicit
`try`/`catch`/`catch` over `runCatching` where it makes the intent clearer. Note that `IOException` is
what these blocks actually throw, so keep catching it — you are adding a case, not replacing one.

Check which `CancellationException` the codebase and this file's imports use. If it resolves to
`kotlinx.coroutines.CancellationException` rather than `java.util.concurrent.CancellationException` on
this Kotlin version, match whatever the rest of the project uses and be consistent with it.

## 4. The success message names the wrong file

Line 79 sets `message = context.getString(R.string.msg_exported, cacheFile.name)`. `cacheFile` is the
app's own staging file in `cacheDir`, named from a timestamp taken here. The user picked a name in the
SAF picker and may have renamed it, so the result line can claim they exported to a filename they
never chose.

Report the destination the user actually picked. This file already has `getDisplayName(uri, context)`
at line 131, which queries `DISPLAY_NAME` for a URI — call it with the destination URI. Its fallback
when a URI has no display name is acceptable; the staging filename is not.

## Finish

One commit. Push, read the run, and keep going on anything the compiler reports. The round is done when
CI is green. No amend, no rebase, no force-push. Do not open or merge a PR, do not post on GitHub,
push only from `worktree-prB`.

## Report

Append a "fix round 5a" section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md`. Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHA and subject
- **CI:** run id and conclusion for the head, confirmed on this branch
- Each of the four items: done or not, plus anything you did differently
- The exact `CancellationException` type and import you used, and why that one
- How you kept a failing delete from becoming a user-visible backup failure
- An explicit statement that these paths have no JVM test coverage, and what CI did and did not prove
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.