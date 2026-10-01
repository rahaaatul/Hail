# Task 2b fix round 4 — entry rows must be disabled during Working, not hidden

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `26af70d` and CI
is green on it (`36904773588`). Every push runs CI: `gh pr checks 121`, `gh run view <id> --log-failed`.
Report only run ids that `gh` shows for this branch.

Step 7's other six items are good and stay: the entry names, the copy, the de-duplicated disabled
reason, the option-row lock, the `entriesOf` tidy, the dead-parameter cleanup. This round is one
defect.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies.

## What is wrong

In `app/src/main/kotlin/com/aistra/hail/ui/backup/BackupRestoreScreen.kt`, the four restore entry rows
were moved **inside** the `Phase.Idle` arm of the `when (loadedState.phase)` block, at lines 180-195.
The spec's "Empty and disabled states" table says the opposite:

> Working | Both sections' controls **disabled**; the system back button intercepted.

And the "Working" section says only *"The section's button is replaced in place by a row"* — the
button, not the entry list. Hiding the list is a deviation, and it also breaks two things the spec
depends on:

1. **During `Working` the list disappears**, so the user loses sight of what is being restored.
2. **After `Done` or `Failed` the list and the Restore button are both gone**, so a failed restore
   cannot be retried and the selection cannot be changed without re-picking the archive. That is a
   functional regression, and it is the more serious half.

The rows were inside the `when` in the first place because step 7 needed them locked during `Working`
and hiding was the easy way to do it. Locking is what was asked for.

## The fix

1. **Hoist the entry rows out of the `when` block** so they render in every phase, at the same place
   they used to sit: after the `RestoreFileSlotLoaded` item and before the action area. Leave the
   `when (loadedState.phase)` doing only what the spec gives it — the `Idle` arm renders the Restore
   button, `Working` the progress row, `Done`/`Failed` the result line.
2. **Lock the rows during `Working`.** Give `RestoreEntryRow` an `enabled` parameter meaning "not
   busy" and pass it `loadedState.phase != Phase.Working`. It already has `entryPresent` from step 7
   for the other meaning; a row is interactive only when it is present **and** not busy. Route both
   through `Modifier.toggleable` and the `Checkbox`, the way you did for `BackupOptionRow` in step 7 —
   match that, so the two row types behave identically.
3. **Disable the archive "Change" button during `Working` too.** It lives in `RestoreFileSlotLoaded`
   and is currently always tappable. Swapping the archive out from under a running restore is not
   recoverable from the UI, and "both sections' controls disabled" covers it. Add the parameter and
   pass `loadedState.phase != Phase.Working`.

Everything else about those composables — the headline from item 1, the `label_not_in_archive`
supporting line, the disabled colour — is already correct and stays.

## Do not undo the rest of step 7

In particular, leave the explicit `as RestoreState.Loaded` cast at line 167. Step 7 removed it, CI run
`36904284884` failed on the smart cast over the delegated property, and the cast was put back
deliberately. It is not an oversight and it is not in scope here.

## Finish the round only when CI is green

Push, read the run, and keep going on anything the compiler reports. One commit. No amend, no rebase,
no force-push. Do not open or merge a PR, do not post on GitHub, push only from `worktree-prB`.

## Report

Append a fix-round 4 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md`. Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHA and subject
- **CI:** run id and conclusion for the head, confirmed on this branch
- Where the entry rows now sit, with `file:line`
- How the row's two conditions combine, and how you named them
- What you did about the Change button, with `file:line`
- Confirmation that the rows now render in `Idle`, `Working`, `Done` and `Failed`, and that the
  `Idle` arm still renders the Restore button
- Confirmation that the cast at line 167 is still there
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.