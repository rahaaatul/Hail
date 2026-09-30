# Design: Backup & restore settings screen

Status: design, not implemented. This is the visual and behavioural half of the
[implementation plan](2026-09-29-backup-restore-compose.md), which decides *where* the work goes
and in how many PRs; this document decides what the screen looks like, what state it holds, what
it says, and what it does with a picked file. Where the two disagree, this one is the later word
and the plan gets amended.

It supersedes the "How it should look" sketch in the plan, which is kept there as the first
draft.

Branch for all of it: not cut yet. It branches off `main` after PR #109 lands.

## What the user is doing

Two jobs that share one vocabulary. A backup writes four things — the pinned apps, the
whitelist, the action recipes, the preferences — to a file the user names. A restore reads such
a file back and replaces some or all of them.

Today both jobs are a four-checkbox `MaterialAlertDialogBuilder` opened from a Settings row
(`SettingsFragment.kt:757` `showBackupDialog`, `:787` `showRestoreDialog`), and the restore dialog
is reached *after* the file is already picked. The consequences of that shape are the whole
reason for this design:

1. The restore dialog offers the same four checkboxes whatever the file contains, and
   `HBackup.restore` ignores an entry that is not there without a word. Checking "Actions" on an
   archive with no `actions.json` is a silent no-op.
2. The only feedback is a toast, and the backup direction's toast was **lying** until PR #109
   fixed the discarded `Result` — the flow had no way to show a failure next to the button that
   caused it.
3. Progress is a modal spinner carrying the app slogan (`R.layout.dialog_progress`), which blocks
   the screen and names the step nowhere.
4. Nothing survives rotation: the staged archive lives in a field, the options live in a field.

## Decisions

| Question | Decision | Why |
|---|---|---|
| Where does it live? | One Settings row → one new destination `nav_backup`, `ui/backup/` | Four states (choose, name a file, work, report) do not belong in a modal, and `SettingsFragment.kt` is already 867 lines. Matches `ui/about`, `ui/apps`, `ui/actions` — one package per destination. |
| One row or two? | **One** row, "Backup & restore" | The two directions share the same four categories. Two rows make the user pick a direction *before* they know what the file holds, which is the thing the preview exists to fix. |
| One screen or a picker between two? | One screen, two sections | Two tabs for two short forms adds a mode switch and hides the other side's state. Sections keep both flows legible at once; each has exactly one primary action. |
| Feedback | In place, never a toast | A toast disappears, and the one that mattered most was the one that said "Exported" for a failed backup. Result and failure render where the button was. |
| Destructive? | Restore confirms, and its button is the only `errorContainer` action on the screen | Restore overwrites live settings and app state. It is the one action here that is not undoable. |
| Backup history? | **Not in scope** | The app has no storage for it; today the user names a destination with SAF. Inventing a history store is a feature, not a refactor. |
| Cloud / encryption / scheduling? | **Not in scope** | Same. |

## The screen

One `LazyColumn`, the activity's existing toolbar as the only top bar — no `Scaffold`, no
`TopAppBar`, exactly as `AboutFragment` does it. `nav_backup` label is the screen title, so the
toolbar shows **Backup & restore** and the up arrow comes free from
`setupActionBarWithNavController`.

### 1. Idle

```
  ←  Backup & restore
  ┌───────────────────────────────────────────────┐
  │  Backup                                       │   section header
  │  ┌─────────────────────────────────────────┐  │
  │  │ ☑  Apps                                  │  │   ListItem, leading Checkbox,
  │  │    Pinned and selected apps              │  │   supporting text, whole row toggles
  │  ├─────────────────────────────────────────┤  │
  │  │ ☑  Whitelist                             │  │
  │  │    Apps excluded from freezing           │  │
  │  ├─────────────────────────────────────────┤  │
  │  │ ☑  Actions                               │  │
  │  │    Freeze and unfreeze recipes           │  │
  │  ├─────────────────────────────────────────┤  │
  │  │ ☑  Settings                              │  │
  │  │    Preferences and app config             │  │
  │  └─────────────────────────────────────────┘  │
  │                                               │
  │  ┌─────────────────────────────────────────┐  │
  │  │            Create backup                 │  │   filled Button, full width
  │  └─────────────────────────────────────────┘  │
  ├───────────────────────────────────────────────┤   horizontalDivider()
  │  Restore                                      │
  │  ┌─────────────────────────────────────────┐  │
  │  │ Choose backup file…                      │  │   outlined Button, full width
  │  └─────────────────────────────────────────┘  │
  │  Pick a .zip this app wrote. Nothing is read  │   supporting text
  │  until you choose one.                        │
  └───────────────────────────────────────────────┘
```

### 2. Restore, archive loaded

```
  │  Restore                                      │
  │  backup-1727510000.zip                        │   headline
  │  54.9 MB · 29 Sep 2026, 12:00 · from Downloads │   supporting, 2 lines max
  │                              Change            │   TextButton
  │  ┌─────────────────────────────────────────┐  │
  │  │ ☑  apps.json          1.2 MB             │  │   only what the zip lists is
  │  │ ☑  whitelist.json     4.1 KB             │  │   enabled; each shows its
  │  │ ☑  actions.json       6.0 KB             │  │   uncompressed size
  │  │ ○  settings.json      not in this archive │  │   disabled + reason
  │  └─────────────────────────────────────────┘  │
  │  ┌─────────────────────────────────────────┐  │
  │  │          Restore selected                │  │   errorContainer, full width
  │  └─────────────────────────────────────────┘  │
```

The entry list is read with `java.util.zip.ZipFile` and nothing else: entry names and sizes are
in the central directory, so a preview costs one open and no parsing. A per-category **count**
("412 apps") would mean parsing `apps.json`, which is the thing worth not doing in a UI that only
needs to say "this file has these four things in it". Sizes are already accurate and free.

### 3. Working

Neither direction gets a modal. The section's button is replaced in place by a row, so nothing
on the screen moves and the options stay visible next to the progress:

```
  │  ┌─────────────────────────────────────────┐  │
  │  │ ⟳  Exporting backup-1727510000.zip…      │  │   20dp CircularProgressIndicator
  │  └─────────────────────────────────────────┘  │   + label, replaces the button
```

Restore reads `⟳  Restoring… 3 of 4` — `HBackup.restore` has no progress callback, so the count
is a lie unless it is added. It is not added here; the label is the file name. If a progress
callback is wanted later, it belongs in `HBackup` as a suspend parameter, not in a guess.

### 4. Result

The row persists until the next action in that section (no toast, no auto-dismiss):

```
  │  ✓  Exported: backup-1727510000.zip            │   onSurfaceVariant + check icon
  │  ✕  Backup failed: File not found              │   colorScheme.error, up to 3 lines
```

### 5. Empty and disabled states

| State | What the user sees |
|---|---|
| Nothing selected in Backup | Button disabled; supporting text **"Select at least one item"** where the toast used to be. A disabled control with no stated reason is the defect the old code had. |
| No archive chosen | Restore button absent, not disabled — the "Choose backup file…" button is the action. |
| Archive loaded, nothing selected | Restore button disabled with **"Select at least one item"**. |
| Picked file is not a zip, or not one of ours | Error line **"Not a Hail backup"** under the file slot, with "Choose backup file…" still available. No crash, no silent no-op. |
| Working | Both sections' controls disabled; the system back button intercepted. |

## Behaviour rules

1. **Back is blocked while an operation is in flight**, with a snackbar "Wait for the current
   operation to finish". A half-copied backup and a half-restored install are both states the
   user cannot reason about, and the old modal did the blocking implicitly. This is a deliberate
   change from the current behaviour and the only place the screen is more restrictive.
2. **The staged archive has one owner and one teardown path.** The ViewModel copies the picked
   document into `cacheDir/restore-<ts>.zip`, and deletes it on: success, failure, "Change",
   a new file being picked, and the screen being destroyed while nothing is running. That is
   defect 2 from the plan — the leak happens today when the options dialog is dismissed with
   nothing checked, because `restoreStarted` is set after the early return and the dismiss
   listener only deletes when it is false.
3. **No `pendingBackupOptions`.** The options live in the ViewModel and are read at the moment
   the SAF launcher is fired, so there is no field to leak across launches. That is defect 3,
   and it disappears with the field rather than being cleared.
4. **Restore confirms before it runs**, naming the categories the user selected: "Apps,
   whitelist, actions and settings will be replaced on this device. This cannot be undone."
5. **After a restore that included apps or whitelist**, the app list refreshes through
   `AppMetaCache.invalidateAll()` — the existing `revision` `StateFlow` that `PagerFragment`
   already collects. That replaces the `parentFragmentManager.fragments` walk in
   `showRestoreDialog`, which reaches into `HomeFragment`'s children to call
   `updateCurrentList()` and breaks the moment a `Pager` is not there.
6. **Settings restored → the options menu is invalidated** (`activity.invalidateOptionsMenu()`),
   same as today, because `HailData`'s getters are read at menu time.
7. **`HBackup` is not touched.** Its API stays
   `suspend fun backup(context, file, options): Result<Unit>` and
   `suspend fun restore(context, file, options): Result<Unit>`. The result handling that PR #109
   added (`getOrThrow()` plus the `operation_failed` string) is what the screen renders, not
   replaces.

## State model

```kotlin
data class BackupRestoreUiState(
    val backup: SectionState = SectionState(),
    val restore: RestoreState = RestoreState.Empty,
)

data class SectionState(
    val options: BackupOptions = BackupOptions(apps = true, whitelist = true, actions = true, settings = true),
    val phase: Phase = Phase.Idle,
    val message: String? = null,   // result or failure, rendered under the button
)

sealed interface RestoreState {
    data object Empty : RestoreState
    data class Loaded(
        val file: File,
        val displayName: String,
        val sizeBytes: Long,
        val entries: List<BackupEntry>,
        val options: RestoreOptions,
        val phase: Phase,
        val message: String?,
    ) : RestoreState
}

data class BackupEntry(
    val category: BackupCategory,   // APPS, WHITELIST, ACTIONS, SETTINGS
    val present: Boolean,
    val sizeBytes: Long,            // 0 when absent
)

enum class Phase { Idle, Working, Done, Failed }
```

The ViewModel owns the SAF results, the staged file, and the coroutine; the fragment is a host
that registers `CreateDocument` / `OpenDocument` and forwards results, because
`registerForActivityResult` needs a `ComponentActivity` or a `Fragment` and the screen is not
one. Rotation survives because the state is in the ViewModel, which is the reason this is a screen
and not a dialog.

## Component map

| Element | Component | Notes |
|---|---|---|
| Section header | `Text` with `MaterialTheme.typography.titleSmall`, `colorScheme.primary` | Same rhythm as the Settings `preferenceCategory` |
| Option row | `ListItem(headlineContent, supportingContent, leadingContent = { Checkbox(checked, onCheckedChange = null) })` inside `Modifier.toggleable(value, role = Role.Checkbox)` | One touch target, not two. The `Checkbox` is decorative; the row is the control. |
| Absent entry | Same row, `enabled = false`, supporting text in `onSurfaceVariant` | Says why: "not in this archive" |
| Primary button | `Button`, `fillMaxWidth` | Create backup |
| Destructive button | `Button` with `containerColor = colorScheme.errorContainer`, `contentColor = onErrorContainer`, `fillMaxWidth` | Restore |
| File slot | `OutlinedButton` when empty; `ListItem` + `TextButton` when loaded | No new component |
| Progress row | `Row` + `CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)` + `Text` | Sized so the row does not reflow the button's slot |
| Result line | `Row` + `Icons.Outlined.CheckCircle` / `Icons.Outlined.ErrorOutline`, `contentDescription = null` | Icon + text, so the meaning is not carried by colour |
| Confirmation | `MaterialAlertDialogBuilder` (the one View dialog worth keeping) | Fire-and-forget, no state to lose |

Spacing: `dimensionResource(R.dimen.padding_medium)` for the screen's horizontal padding and
between sections, `padding_small` under headers. All of it `dimensionResource`, per
`AboutFragment`. No hardcoded `dp` except the progress indicator's own size.

Theming: `AppTheme` at the fragment, exactly as `AboutFragment.kt:47` and
`SettingsFragment.kt:184` do. Colour roles only — no literals — so dark theme and the
user's accent follow automatically. Every user-visible string is a `stringResource`.

Accessibility: rows are 48dp+ through `ListItem`, toggles carry `Role.Checkbox`, the icon on a
result line is `contentDescription = null` with the text beside it, and the error line is
announced by being text rather than a colour. The `LazyColumn` scrolls, so nothing breaks at
font scale 2.

## Copy

New English strings; Weblate covers the rest, so nothing else is touched here.

| Key | English | Replaces |
|---|---|---|
| `title_backup_restore` | `Backup & restore` | nav label |
| `summary_backup_apps` | `Pinned and selected apps` | — |
| `summary_backup_whitelist` | `Apps excluded from freezing` | — |
| `summary_backup_actions` | `Freeze and unfreeze recipes` | — |
| `summary_backup_settings` | `Preferences and app config` | — |
| `action_create_backup` | `Create backup` | — |
| `action_choose_archive` | `Choose backup file…` | — |
| `action_change_archive` | `Change` | — |
| `summary_choose_archive` | `Pick a .zip this app wrote. Nothing is read until you choose one.` | — |
| `label_in_this_archive` | `In this archive` | — |
| `label_not_in_archive` | `Not in this archive` | — |
| `msg_exporting` | `Exporting %1$s…` | the slogan spinner |
| `msg_importing` | `Restoring %1$s…` | the slogan spinner |
| `msg_exported` | `Exported: %1$s` | reused as the result line |
| `msg_restored` | `Restored: %1$s` | `msg_imported` |
| `msg_restore_confirm_title` | `Restore this backup?` | — |
| `msg_restore_confirm_body` | `%1$s will be replaced on this device. This cannot be undone.` | — |
| `msg_wait_for_operation` | `Wait for the current operation to finish` | — |
| `msg_not_a_backup` | `Not a Hail backup` | — |

`no_items_to_select` and `msg_no_items_to_select` are the same string under two names; the new
screen uses one of them inline and the duplicate is deleted.

`R.layout.dialog_progress` **stays** — `AppsFragment.kt:63` still uses it. Only the backup and
restore dialogs go.

## Removed

- `showBackupDialog`, `showRestoreDialog`, `pendingBackupOptions`, `backupLauncher`,
  `restoreLauncher`, the `restore` Settings row, and both `MaterialAlertDialogBuilder` uses in
  this file.
- `dialog_progress` is *not* removed (see above).

## What is not covered by tests

There is no Compose test infrastructure in this repository and adding it would be a larger
change than the screen it guards. So the wiring — button enablement, progress, result rendering,
the back interception — ships unverified by automated means, and the PR body has to say so
rather than imply coverage.

What is testable on the JVM, and is therefore written as pure functions:

- `BackupEntry.entriesOf(file): List<BackupEntry>` — the zip read, over real `java.util.zip` and
  a `TemporaryFolder`, the way `HBackupTest` already does it. This is where "not in this
  archive" earns its keep.
- `entryCategory(name): BackupCategory?` — `"apps.json"` → `APPS`, an unknown entry → `null` and
  ignored, which is what `HBackup.restore` does with one.
- `canStartBackup(selected)` / `canStartRestore(state)` — the enablement rules, so "disabled with
  a stated reason" is a test rather than a habit.

`test.sh` already runs `:app:testDebugUnitTest` on every PR, so these execute in CI with no
pipeline change.

## Phasing

0. **PR #109 (done, in review).** #83, #91 and #92 — the discarded `Result`, the `Float` reader,
   the `mkdirs()` check. Of the three defects listed above, **#92 is already fixed there**, and
   the other two are untouched. Everything below assumes that.
1. **PR A — the two remaining defects, no UI change.** The staged-archive leak and
   `pendingBackupOptions`. Small, independent of any of this design, and it stops the leak while
   the old dialogs are still in place. Also the symmetric `openInputStream(uri)?.use` no-op noted
   in PR #109's body, if it fits.
2. **PR B — this screen.** New package, new destination, the Settings row change, the strings,
   the deletions above, the confirmation, the refresh through `AppMetaCache`.
3. **PR C — the JVM tests** for the three pure functions. Separate so PR B is not held on them.

## Rejected, and why

- **Two Settings rows, Compose dialogs in place of the View ones.** Smaller diff, and it keeps
  the four-step flow modal: no place for a result that outlives a toast, no preview, and both
  defects stay.
- **Tabs for Backup / Restore.** A mode switch for two short forms, and it hides the other
  side's state — including a file already picked and a backup already written, which is exactly
  the state a user comes back to.
- **A list of previous backups in app storage.** Nothing in the app stores them today, and the
  SAF destination is the existing contract. This is a feature with its own storage, retention and
  privacy questions.
- **The preview from `HBackup`, returning counts.** It would mean parsing `apps.json` in the UI
  path, and it would put a second entry-point list inside the module that owns the format. The
  preview is one `ZipFile` read in the UI layer.
- **Confirmation on backup.** A backup only writes a file the user just named. Asking twice
  would be the same reflex that put four checkboxes in a modal.
