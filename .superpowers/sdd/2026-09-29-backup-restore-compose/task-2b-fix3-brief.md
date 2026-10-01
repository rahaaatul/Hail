# Task 2b fix round 3 — the fragment does not compile

Branch `feat/backup-restore-screen`, worktree `worktree-prB`, draft PR #121. Head is `bf40cca`.
CI on every push: `gh pr checks 121`, `gh run view <id> --log-failed` for the log. Report only run
ids that `gh` shows for this branch.

Step 5's structure is right and stays: the fragment hosts the SAF launchers, the screen takes the
ViewModel plus three callbacks, the confirmation is a `MaterialAlertDialogBuilder`, and back is
intercepted while an operation runs. This round fixes compilation only. **Do not restructure the
wiring, do not touch `SettingsFragment.kt`, do not delete anything you just added.**

**Do NOT run `./gradlew`, `test.sh`, or any build/test/lint command.** No JDK and no Android SDK is
installed. PR CI is the verification path. No new dependencies.

Run `36883234116` on `bf40cca` reports six errors, all in
`app/src/main/kotlin/com/aistra/hail/ui/backup/BackupRestoreFragment.kt`:

### 1 and 2. Lines 45 and 17 — `OnBackPressedCallback` imported from the wrong artifact

`import androidx.fragment.app.OnBackPressedCallback` does not exist. The class is
`androidx.activity.OnBackPressedCallback`. Change the import.

Line 52's `isEnabled = false` is reported as unresolved only because that import failed, so the
anonymous object's type is unknown. Once the import is right, `isEnabled = false` followed by
`onBackPressed()` is the correct way to hand the press back to the default handler — keep that
sequence as it is.

### 3. Line 39 — `OpenDocument` takes no constructor arguments

`registerForActivityResult(OpenDocument(arrayOf("application/zip")))` is wrong:
`ActivityResultContracts.OpenDocument` has a no-argument constructor, and the MIME type filter is
passed to `launch`. Line 72 already launches it correctly with
`restoreLauncher.launch(arrayOf("application/zip"))`. So the constructor becomes `OpenDocument()`
and nothing else about that launcher changes.

### 4. Line 50 — there is no `HUI.showSnackbar`

`HUI` has no snackbar helper; its only feedback method is `showToast`. This app shows snackbars
directly with the Material `Snackbar` class — read
`app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt` around lines 52 and 176 for the
pattern the project uses, including which view it anchors to and which `LENGTH` it passes. Rewrite
line 50 to use `Snackbar.make(...)` with `R.string.msg_wait_for_operation`, anchored to a view that
exists at that moment. Note: the callback can fire before the view is laid out, so check what
`PagerFragment` does about that before you decide the anchor, and say in your report what you chose.

### 5. Line 68 — another invalid fully-qualified `Modifier`

`androidx.compose.foundation.layout.Modifier` is not a package path; `Modifier` lives in
`androidx.compose.ui`. Use the imported `Modifier`:

```
                    Surface(modifier = Modifier.fillMaxSize()) {
```

with `import androidx.compose.ui.Modifier` added. Then grep the whole file for any other
fully-qualified reference that is not a real package path and reduce it the same way.

### 6. Lines 81-94 — delete the pointless forwarding composable

The private `@Composable fun BackupRestoreScreen(...)` whose entire body calls
`com.aistra.hail.ui.backup.BackupRestoreScreen(...)` with the same arguments is indirection with no
purpose, and it exists only to confuse the name lookup. Delete it and call the top-level
`BackupRestoreScreen` directly from `setContent`. While you are there, delete any import that this
deletion leaves unused — `MaterialTheme` is a likely candidate, since the content is wrapped in
`AppTheme` and `Surface` rather than reading the theme directly. Verify before deleting.

## Finish the round only when CI is green

Push, read the run, and keep going on anything the compiler reports that this brief did not name —
the remaining cascade errors in the screen and ViewModel are part of the same breakage. The round is
done when the build compiles and the unit tests pass.

One commit. No amend, no rebase, no force-push. Do not open or merge a PR, do not post on GitHub,
push only from `worktree-prB`.

## Report

Append a fix-round 3 section to
`../.superpowers/sdd/2026-09-29-backup-restore-compose/task-2b-report.md`. Reply with ONLY:

- **Status:** DONE | DONE_WITH_CONCERNS | BLOCKED | NEEDS_CONTEXT
- Commit short SHA and subject
- **CI:** run id and conclusion, confirmed on this branch
- Each of the six items: fixed or not, and why
- What view the snackbar is anchored to, and what you did about the not-yet-laid-out case
- Any further errors CI reported that this brief did not name
- Concerns, if any

You do not dispatch subagents. Do all of this yourself.