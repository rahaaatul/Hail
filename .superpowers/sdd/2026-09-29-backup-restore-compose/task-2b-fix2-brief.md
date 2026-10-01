# Task 2b fix round 2 — the last compile errors

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `60342b2`.
CI on every push: `gh pr checks 121`, `gh run view <id> --log-failed` for the log. Report only run
ids that `gh` shows for this branch.

**Scope: make the branch compile. Touch nothing else.** Do not start step 5, do not restructure the
screen, do not delete the ViewModel's functions — an audit found that `performBackup`,
`onArchivePicked` and `onRestoreConfirmed` currently have no call sites, but that is expected: the
hosting fragment does not exist yet and wiring them is step 5's job. Step 5 needs those functions.
Leave them exactly as they are.

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK or Android SDK here.
PR CI is the verification path.

## Four fixes, each verified against the source

### 1. `BackupRestoreScreen.kt:42` — delete the `viewModel()` import

`import androidx.lifecycle.viewmodel.compose.viewModel` cannot resolve:
`androidx.lifecycle:lifecycle-viewmodel-compose` is **not** a dependency of the `:app` module. The
only lifecycle artifact declared is `libs.androidx.lifecycle.livedata.ktx`
(`app/build.gradle.kts`), so there is no Compose `viewModel()` helper available.

**Do not add the dependency.** This branch adds no dependencies. The project's pattern is
Fragment-side acquisition — `private val model: AppsViewModel by viewModels()` in `AppsFragment.kt` —
and the plan puts the SAF launchers in the fragment anyway.

### 2. `BackupRestoreScreen.kt:46-47` — wrong import for two top-level functions

`BackupPreview.kt` declares `canStartBackup` and `canStartRestore` as **top-level functions**; there
is no `BackupPreview` object or class, only a file of that name. So the qualified form
`com.aistra.hail.backup.BackupPreview.canStartBackup` does not resolve. Use:

```
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.canStartRestore
```

### 3. `BackupRestoreScreen.kt:57` — the `viewModel()` default argument

`viewModel: BackupRestoreViewModel = viewModel()` must lose its default. Make the parameter required:

```
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    ...
```

Step 5 will have the fragment pass it. Read the whole signature and keep every other parameter
exactly as it is.

### 4. `BackupRestoreViewModel.kt:266` and `:269` — cast precedence

Both statements read `_restoreState.value as RestoreState.Loaded.copy(...)`. `.` binds tighter than
`as`, so this parses as a qualified call on the *type* and fails to resolve. Parenthesise the cast:

```
_restoreState.value = (_restoreState.value as RestoreState.Loaded)
    .copy(phase = Phase.Done, message = …)
```

and the same for the `Phase.Failed` line. Change nothing else in those statements.

## Also remove these unused imports

An audit found them unreferenced in the whole module. Verify each is unreferenced yourself before
deleting, and skip any that turns out to be used:

- `BackupRestoreScreen.kt`: `androidx.compose.material.icons.outlined.CheckBoxOutlineBlank`,
  `com.aistra.hail.ui.theme.AppTheme`
- `BackupRestoreViewModel.kt`: `com.aistra.hail.app.HailData`,
  `com.aistra.hail.backup.entryCategory`, `kotlin.Result`

## Finish the round only when CI is green

Push, read the run. If CI reports errors in code this round did not touch, fix them too — they are
part of the same cascade. The round is done when the build compiles and the unit tests pass.

One commit. No amend, no rebase, no force-push. Do not open or merge a PR, do not post on GitHub,
push only from `worktree-prB`.

## Report

Append a fix-round 2 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md` (create it if the earlier
ones are gone). Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHA and subject
- **CI:** run id and conclusion, confirmed on this branch
- Each of the four fixes: done or not done, and why
- Any further errors CI reported that this brief did not name
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.