# Plan: Backup & restore as a Compose screen

Status: proposal, not started. Branch not yet cut.

**The screen design now lives in [2026-09-30-backup-restore-ui-design.md](2026-09-30-backup-restore-ui-design.md)** — wireframes, state model, behaviour rules, copy, and the phasing. The "How it should look" sketch below is the first draft and is kept for history; the design document supersedes it wherever they differ.

**Defect 1 of three is already fixed.** #92 (the discarded backup `Result`) is fixed by PR #109, along with #83 and #91. Defects 2 (the leaked staged restore file) and 3 (`pendingBackupOptions`) are untouched, and they are what the first PR below is for.

## Premise correction — read this first

**Backup and restore are already in Settings, not the Home tab.** On `main`
(ca1d49f) the only entry points are in `SettingsFragment.kt`:

- `SettingsFragment.kt:391-403` — the `backup` preference category, with
  `backup_preference` and `restore` rows, inside the Compose `LazyColumn` that
  makes up the whole Settings screen (`SettingsFragment.kt:195-404`).
- `app/src/main/res/menu/menu_home.xml` has no backup item. A repo-wide grep for
  `backup|restore` in `app/src/main/kotlin` hits only `SettingsFragment.kt`,
  `HBackup.kt`, and an unrelated Android DPM call in `HPolicy.kt:45`.

So there is nothing to move out of Home. What *is* true, and is what this plan
does: the two dialogs that implement the flow are **View** dialogs
(`MaterialAlertDialogBuilder`) sitting inside an otherwise-Compose screen, and
the flow they drive has three real defects. That is the work.

## What is there now

Both flows are four-checkbox multi-choice dialogs.

**Backup** — `showBackupDialog` (757-785): dialog → options → `pendingBackupOptions`
field (148) → `backupLauncher` (`CreateDocument("application/zip")`, 87-117) →
`HBackup.backup` → copy the cache file to the chosen URI → toast.

**Restore** — `restoreLauncher` (`OpenDocument`, 118-147) copies the picked
document into the cache first, *then* `showRestoreDialog(file)` (787-842) asks
for options and runs. The order is inverted relative to backup.

A fourth checkbox label is built from `R.string.backup_*`. Progress is a View
spinner, `R.layout.dialog_progress`, which also displays the app slogan.

## Defects this replaces

Three are real and confirmed against `main`. Two of them are already filed.

1. **The backup `Result` is discarded — issue #92. FIXED IN PR #109.** `backupLauncher` wraps the
   call in `runCatching { HBackup.backup(ctx, file, ...) }`, but `HBackup.backup`
   returns `Result<Unit>` and never throws. A `Result.failure` therefore falls
   straight through, the copy to the user's URI still runs, `onSuccess` fires,
   and the user is told `Exported: backup-….zip` for a backup that failed. This
   was how the `mkdirs()` bug (#83) stayed invisible for so long. PR #109 resolves the result and deletes the staged cache file on failure, so the copy no longer runs and no success toast appears for a failed backup.
2. **The staged restore file leaks.** `showRestoreDialog` sets
   `restoreStarted = true` at 801, then returns early at 810 when nothing is
   checked. The dismiss listener at 835 only deletes the file when
   `restoreStarted` is false, so that early return leaves the copied archive in
   `cacheDir` forever.
3. **`pendingBackupOptions` is never cleared.** It is set at 780 and read at 98;
   a second backup launched without re-picking options silently reuses the old
   ones.

Plus one design smell worth fixing while the code is open: the restore success
path walks `parentFragmentManager.fragments` looking for `HomeFragment` and then
its child `PagerFragment`s (818-825) to refresh the app list. Fragment-tree
reflection to signal another screen is a maintenance trap.

## Where it should live

**A dedicated destination, `ui/backup`, reached from one Settings row.**

Reasoning: this is not a dialog's worth of work. Choosing what to include, choosing
a file, doing the work, and reporting the result are four states, and three of them
survive process death poorly in a dialog. A screen holds them, survives rotation
through a ViewModel, and can show progress in place rather than behind a modal
spinner. `SettingsFragment.kt` is already 867 lines; this is not more code to pile
onto it.

- New package `app/src/main/kotlin/com/aistra/hail/ui/backup/`, matching the
  project's one-package-per-destination convention (`ui/home`, `ui/apps`,
  `ui/actions`, `ui/about`). It introduces no new convention — `theme/` is the
  only non-destination package and stays that way.
- New destination `nav_backup` in `res/navigation/mobile_navigation.xml`, next
  to the existing five. The top bar is the activity's existing `MaterialToolbar`;
  no new `Scaffold`/`TopAppBar`, matching `AboutFragment`.
- `SettingsFragment` keeps exactly one row, `backup_preference`, retitled to
  cover both directions, navigating to it. The `restore` row and both
  `MaterialAlertDialogBuilder` dialogs are deleted from it, along with
  `showBackupDialog`, `showRestoreDialog`, `showTerminalDialog`'s neighbours being
  left alone.

Rejected alternative: keep both rows and swap the dialogs for Compose dialogs.
Smaller, but it keeps a four-step flow inside a modal and leaves the state
problem in place.

## How it should look

```
  ┌─────────────────────────────────────┐
  │  ←   Backup & restore               │   activity toolbar, no new top bar
  ├─────────────────────────────────────┤
  │  What to include                    │   section header
  │  ┌─────────────────────────────────┐│
  │  │ ☑  Apps                        ││   ListItem + Checkbox
  │  │    Pinned and selected apps     ││   supporting text
  │  ├─────────────────────────────────┤│
  │  │ ☑  Whitelist                   ││
  │  │    Apps excluded from freezing  ││
  │  ├─────────────────────────────────┤│
  │  │ ☑  Actions                     ││
  │  │    Freeze and unfreeze recipes  ││
  │  ├─────────────────────────────────┤│
  │  │ ☑  Settings                    ││
  │  │    Preferences and app config   ││
  │  └─────────────────────────────────┘│
  │                                     │
  │  [ Create backup        ]  [ Restore ]│   filled / tonal, side by side
  └─────────────────────────────────────┘
```

After **Create backup** is tapped, the same screen with the two buttons replaced
in place by a progress row — label plus an indeterminate `LinearProgressIndicator`
— and the option checkboxes disabled. No modal. On finish the row becomes a
result line: the file name and `Exported`, or the failure and its message, with
the buttons back. Nothing about the screen moves.

For **Restore**, the archive is picked first (matching the current code order),
then the screen shows what the zip actually contains, so options are chosen
against reality rather than assumption:

```
  │  Restoring backup-1727510000.zip    │
  │  ┌─────────────────────────────────┐│
  │  │ ✓ apps.json       412 apps      ││   only entries present are listed
  │  │ ✓ settings.json                 ││   and only those can be selected
  │  │ ○ actions.json    not in file   ││
  │  └─────────────────────────────────┘│
```

That is the substantive win over the current dialog: the current restore path
cannot tell the user the archive contains no `actions.json` before they check
"Actions" and get a silent no-op, because `HBackup.restore` ignores unknown and
missing entries without complaint.

Disabled states to get right: both buttons disabled when nothing is selected
(replacing the `msg_no_items_to_select` toast with a visible reason), and Restore
additionally disabled while no archive is loaded.

## Compose, not Views

- All UI in `ui/backup/BackupRestoreScreen.kt`, plus a `BackupRestoreViewModel`
  holding options, archive metadata, and the in-flight state.
- Wrap in `AppTheme` at the fragment, exactly as `SettingsFragment.kt:177` and
  `AboutFragment.kt:47` do. `dimensionResource(R.dimen.padding_medium)` for
  spacing. `Icons.Outlined.*` with `contentDescription = null`, per the codebase.
- `stringResource` for every user-visible string. The only hardcoded literals in
  the Compose codebase today are two `"%.0f".format(it)` slider formatters, and
  that is not a precedent worth following.
- **Removed:** `MaterialAlertDialogBuilder` in both flows, and the
  `showBackupDialog` / `showRestoreDialog` functions.
- **Kept:** `R.layout.dialog_progress` and the `app_slogan` it carries — **verified,
  not assumed:** `AppsFragment.kt:63` still opens it while the app list is loading.
  The slogan moves into the About screen only if that call goes too.
- **Stays View-based, unavoidably:** the SAF launchers. `CreateDocument` and
  `OpenDocument` must be registered with `registerForActivityResult` on a
  Fragment or ComponentActivity, so `BackupRestoreFragment` remains the host and
  simply hands results to its ViewModel. The fragment's `onCreateView` returns a
  bare `ComposeView` with `DisposeOnViewTreeLifecycleDestroyed`, same as the
  Settings and About fragments. No ViewBinding, no layout XML for this screen.
- `HBackup` is untouched by this plan. Its API stays
  `suspend fun backup(context, outputFile, options): Result<Unit>`.

## Sequencing

Three PRs, each mergeable on its own.

**PR 1 — fix the two remaining defects, no UI change.** Delete the staged file on
the all-unchecked restore path, and clear `pendingBackupOptions` after use (or drop
the field, which the design does). Small, and it stops the leak while the UI is
still the old one. #92 came in with PR #109, so this closes nothing new.

**PR 2 — the Compose screen and destination.** The bulk; the design document is
the specification. The post-restore refresh replaces the
`parentFragmentManager.fragments` walk with `AppMetaCache.invalidateAll()`, whose
`revision` `StateFlow` `PagerFragment.kt:121` already collects — an existing
signal, not a new one.

**PR 3 — tests.** See below. Kept separate so PR 2 is not held up on adding a
test dependency.

## Testing

There is **no UI test infrastructure in this repository** — no
`createAndroidComposeRule`, no `ui-test-junit4` dependency, and Espresso is
declared in `androidTestImplementation` but never used. Adding Compose UI test
infrastructure would be a larger change than the refactor it guards, so it is not
proposed.

What is worth doing, and cheap:

- Extract the checkbox-selection → `BackupOptions`/`RestoreOptions` mapping out of
  the dialog into a pure function and JVM-test it. No Android, no new dependency.
- Extract the "which entries does this archive contain" parse out of
  `BackupRestoreViewModel` into a pure function over the zip and JVM-test it the
  same way `HBackupTest` already does, with real `java.util.zip` and a
  `TemporaryFolder`.
- `test.sh` already runs `:app:testDebugUnitTest` on every PR, and `pr.yml` runs
  that on every push to a PR, so these execute in CI with no pipeline change.

The UI wiring itself — button enablement, progress, result rendering — would be
uncovered. That is the honest cost of not introducing Compose test infrastructure,
and it is worth stating in the PR body rather than implying full coverage.

## Risks

- **Archive preview is new behaviour.** Reading the picked zip's entry list means
  opening the user's file before restoring. It is read-only and local, but it is
  something the current flow never does, so it deserves a line in the PR body.
- **Navigation change.** A sixth destination touches `mobile_navigation.xml` and
  the toolbar's back behaviour. Small, but it is the only part of this plan that
  can affect unrelated screens.
- **Progress in place of a modal** changes behaviour for users who tap restore
  and then navigate away. The View spinner blocked that; a non-modal screen will
  not. Decide deliberately whether to lock navigation during the operation.
