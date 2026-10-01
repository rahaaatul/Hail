# Task 2b fix round 5 — hardening pass from the three-way review

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `958e631` and CI
is green on it (`36906244890`). Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed`.
Report only run ids that `gh` shows for this branch.

Eight items, all verified by hand before this brief was written. Work through them in order; items 1-3
are the substantive ones and they are related, so get them right together.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies.

---

## 1. Stop deleting the staged archive out from under the state that references it

`BackupRestoreViewModel.onRestoreConfirmed` calls `deleteStagedFile()` and then publishes a
`RestoreState.Loaded` that still holds that file (lines 174-176 and 178-180). After a restore — success
*or* failure — the state describes a file that no longer exists:

- `BackupRestoreScreen` calls `Formatter.formatFileSize(context, state.sizeBytes)` and
  `state.file.lastModified()` to build the archive slot's supporting line, so it reads a deleted file
  and shows `0` bytes and a 1970 date.
- A second tap on Restore calls `HBackup.restore` on a path that is gone.
- The entry rows and their sizes stay on screen, describing a file that was deleted underneath them.

This is worse than the leak it looks like, and the fix is to stop creating the inconsistency rather
than to paper over it: **the staged file lives exactly as long as the state that points at it.**

Delete `deleteStagedFile()` from both the `onSuccess` and `onFailure` arms of `onRestoreConfirmed`, so
the file survives the restore and the result line can keep showing what was restored. It is then
deleted at the two points where the state stops referencing it — `onChangeArchiveClick` (line 141) and
`onCleared` (line 193) — which already call it.

Consequences to accept deliberately, and to state in your report: after a restore the archive stays in
`cacheDir` until the user picks a different one or leaves. That is one file, bounded, in a directory
the OS already reclaims, and it is the only arrangement in which the state stays truthful. Do not
delete it eagerly and then blank the state to compensate — losing the result message is worse.

## 2. The backup cache file leaks if the coroutine is cancelled

In `performBackup`, `cacheFile.delete()` is the last statement of the coroutine body (line 87).
`HBackup.backup(...)` at line 66 is not wrapped in `runCatching` — it returns a `Result` — so a
cancellation while it is running kills the coroutine at that point and line 87 never executes. The
file stays in `cacheDir` forever. Navigating away mid-backup is exactly this case, because
`viewModelScope` is cancelled in `onCleared`.

Wrap the body so the delete happens on every exit. A `try`/`finally` around the work, with
`cacheFile.delete()` in the `finally`, is the shape to use. Make sure the `finally` cannot itself
throw and mask the original outcome, and that a delete failure is not reported to the user as a
backup failure.

## 3. `runCatching` is swallowing `CancellationException`

Two sites: line 68 (the destination copy) and line 99 (the archive copy in `onArchivePicked`).
`runCatching` catches `Throwable`, so a cancellation inside those blocks is caught and turned into an
ordinary failure. At line 68 that becomes a user-visible "operation failed"; at line 99 it becomes
"Not a Hail backup". Both are wrong: a cancelled screen should not tell the user their file is
corrupt, and swallowing the exception breaks structured concurrency — the coroutine keeps running
instead of unwinding, and any cleanup that depends on unwinding will not happen.

Re-throw cancellation before treating a failure as real. For each of the two sites, catch explicitly
and re-throw `CancellationException` (and be careful: if the project targets a Kotlin where
`CancellationException` is under `java.util.concurrent`, match the import the file already uses —
check what it imports and be consistent with the rest of the codebase).

After this change, re-check item 2: whether the backup cache delete now needs the `finally` depends on
where exactly you placed it.

## 4. The success message names the wrong file

`performBackup` sets `message = context.getString(R.string.msg_exported, cacheFile.name)` (line 79).
`cacheFile` is the app's own staging file in `cacheDir`, named from a timestamp taken here. The user
chose a name and a location in the SAF picker, and may have renamed it there. So the result line can
tell the user they exported to a filename they never chose.

Report the destination the user actually picked, not the staging file. There is already a
`getDisplayName(uri, context)` helper in this file (line 131) that queries `DISPLAY_NAME` for a URI —
use it against the destination URI. If it returns a fallback for a URI with no name, that is
acceptable; the staging filename is not.

## 5. Add the divider between the two sections

The spec's tree diagram marks `horizontalDivider()` between the Backup section and the Restore section
(line 77 of the design spec). The screen uses only vertical spacing. Use Compose's
`HorizontalDivider` from material3 (not the deprecated `Divider`) inside the `LazyColumn`, between the
backup action area and the Restore header. Read the spec around line 77 to confirm the placement is
once, not once per section.

## 6. Label the restore entry list with the string already added for it

`strings.xml:125` defines `label_in_this_archive` ("In this archive"), the design spec's copy table
lists it at line 258, and the spec's row table shows it as a heading above the four entry rows — and
nothing in the app references it. Three independent reviews found this. Add it as an item before
`loadedState.entries.forEach`, styled like the other section headings in this screen rather than
inventing a new look, and use the existing string. Do not add another string with the same text.

## 7. Pin the three `entriesOf` paths that no test covers

Three mutations to `BackupPreview.kt` currently pass the entire suite, so those behaviours are
documented but not enforced:

- **Dropping the `-1` size clamp.** `ZipEntry.getSize()` returns `-1` for a streamed entry. A test
  must assert an unknown size is reported as `0`.
- **Flipping duplicate handling from last-wins to first-wins.** A test must assert what the code
  actually does for two entries mapping to the same category.
- **Swallowing the exception on a corrupt or truncated zip.** A test must assert `entriesOf` throws
  for a file that is not a readable zip.

Existing fixtures cover a zero-byte entry, extra unrelated files, and directory entries, but those are
different cases from the three above — a zero-byte entry is a real entry of length zero, not an entry
whose size is unknown.

**Tests first, in this order, because the plan requires it:** add the three assertions, commit and push
**the tests only**, read the CI run, and confirm it **fails** — that is the proof each test bites. Then
implement or adjust whatever is needed and finish green. If a test turns out to pass against the
current code, that is the point: it pins existing behaviour, so say so plainly in your report rather
than claiming a red run you did not get. Paste the failing test names from the red run.

## 8. Correct the KDoc that no longer matches the code

`BackupPreview.kt` around lines 40-53 makes three claims the code does not support:

- it says entries are deduplicated by **name**; the code keys the map by **category**;
- it says "the last one encountered is the one reported" as though that were deterministic;
  `ZipFile.entries()` order is not specified, so it is not guaranteed;
- it documents only `ZipException` as thrown, but a missing or unreadable file raises `IOException`.

State what the code actually does. If duplicate-entry order genuinely is unspecified, either make it
deterministic (prefer that — a preview that reports a different size depending on zip layout is a bad
preview) or say plainly in the KDoc that it is whichever entry the archive happens to list last, and
make the test pin the current behaviour rather than a promise the code does not keep. Choose, and
justify the choice in your report. Do not leave a comment that claims more than the code guarantees.

## Finish the round only when CI is green

Push, read the run, and keep going on anything the compiler reports that this brief did not name.

Two commits are expected and acceptable — the tests-only commit from item 7, then the implementation.
No amend, no rebase, no force-push. Do not open or merge a PR, do not post on GitHub, push only from
`worktree-prB`.

## Report

Append a fix-round 5 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md`. Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHAs and subjects
- **CI:** every run id and conclusion for this branch, including item 7's red run with its failing test
  names, then the final green run
- Each of the eight items: done or not, plus anything you did differently and why
- For item 1: confirm the staged file now survives a completed restore, and that it is still deleted
  on change-archive and on `onCleared`
- For item 3: the exact `CancellationException` type and import you used at each site
- For item 8: your choice on duplicate-entry determinism and the justification
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.