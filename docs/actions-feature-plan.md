# Actions Feature Plan

## Mandatory Implementation Log Rules

Any person or AI agent implementing this plan **MUST** maintain the tracker and log in this document. These rules are part of the implementation contract:

1. Before starting a work slice, update the tracker status to `In progress` and name the files/symbols being changed.
2. After each work slice, update the tracker with what was completed and the validation command/result.
3. If blocked, immediately set the affected item to `Blocked` and record the exact blocker, evidence, and the next attempted resolution. Do not silently skip or work around an unresolved blocker.
4. Keep the tracker truthful. Do not mark an item `Complete` until its acceptance criteria and focused validation have passed.
5. Add newly discovered work to the tracker instead of hiding it in prose.
6. Preserve previous entries; append notes or update the current status without deleting useful history.
7. Before handing work to another agent, record the current branch, commit, files changed, tests run, and remaining risks.

An agent that changes implementation files without updating this log is not following the plan.

## Implementation Tracker

| Item | Status | Owner/branch | Files or symbols | Validation/blocker |
| --- | --- | --- | --- | --- |
| Plan and UX specification | Complete | `plan/actions-feature` | This document | Reviewed and pushed |
| Room Actions schema and repository | Complete | `feature/actions` | `ActionEntity`, `ActionDependencyEntity`, `ActionDao`, `ActionsRepository`, `AppMetadataDatabase`, `AppMetaCache` | `./gradlew :app:compileDebugKotlin` passed |
| Action execution and API entry point | Not started | `feature/actions` | Action executor, `HailApi`, `ApiActivity` | Must unfreeze sequentially and launch only after verification |
| Actions navigation and Home/App FAB behavior | Not started | `feature/actions` | Navigation resources, `MainActivity`, Home, Apps | Home FAB position must remain unchanged |
| Actions list and Create/Edit action UI | Not started | `feature/actions` | Actions screen, row, dialog, app picker | One-line ellipsized summaries; Unfreeze is required |
| Long-press actions | Not started | `feature/actions` | Edit, shortcut, duplicate, delete menus | Delete confirmation and stable IDs required |
| Pinned action shortcuts | Not started | `feature/actions` | `HShortcuts`, shortcut intent handling | Launch app label/icon and action ID required |
| Tests and acceptance validation | Not started | `feature/actions` | Unit/UI/instrumentation tests | Must cover persistence, execution, navigation, and shortcuts |
| Backup/Restore | Backlog | Future branch | Future versioned import/export | Out of scope for initial Actions implementation |

### Log Entry Format

Each implementation update should append a dated entry using this format:

```text
### YYYY-MM-DD - agent/commit
- Status: In progress | Complete | Blocked
- Work: what changed and why
- Validation: exact command and result
- Blocker/next: blocker evidence or next slice
```

### 2026-08-27 - plan setup

- Status: Complete
- Work: Created `plan/actions-feature` from the main-based Actions plan, returned to `feature/actions`, and added mandatory tracker rules.
- Validation: Branch separation and plan push completed successfully.
- Blocker/next: Implement the Room schema first on `feature/actions`.

### 2026-08-27 - Room schema slice

- Status: Complete
- Work: Added the additive Actions schema and repository on `feature/actions`, including ordered dependencies and a 2-to-3 migration.
- Validation: Editor diagnostics report no errors in the changed Kotlin/Java files; `git diff --check` passes. `./gradlew :app:compileDebugKotlin` passed successfully.
- Blocker/next: No blocker remains for this slice. Continue with the execution/UI slices.

## Goal

Add reusable launch actions for apps that require one or more companion apps to be unfrozen first. An action unfreezes its configured apps, then launches one target app.

Example: `YouTube` launches after `MicroG` and `MicroG Services` are unfrozen.

The feature is intentionally separate from Home app selections, tags, whitelist state, and normal app shortcuts.

## Product Structure

Top-level navigation order:

```text
Home | Actions | Settings
```

- Home remains the place for normal app launching and freeze management.
- Actions contains saved launch actions.
- Apps is removed from bottom navigation and the navigation rail.
- Apps remains a navigable destination opened from Home.

### Home FAB

- Keep the Home FAB in its current bottom/end position, with its current margins, insets, and layout behavior.
- Change its purpose from the current freeze FAB to an Apps/navigation button.
- Use the existing FAB surface and position; do not move it into the toolbar or navigation bar.
- The button opens the Apps destination.
- Apps shows a toolbar back arrow and returns to Home through both toolbar back and system back.
- Do not show a second visible Apps tab.

### Actions FAB

- Show a plus FAB in the same bottom/end position used by the existing FAB.
- Tapping it opens the `Create action` dialog.
- The Actions FAB must not change the Home FAB's position or behavior.

## Action List

Display actions as a vertical list of slim Material 3 cards/list items with a small gap between rows.

Each row contains:

```text
[launch app icon]  Launch app name
                   App 1, App 2, App 3...
```

Rules:

- Use the launch app's actual icon.
- Primary text is the launch app's current display name.
- Secondary text contains only the configured unfreeze apps.
- Do not display the prefix `Unfreezes:`.
- Keep the secondary text to one line.
- End-truncate overflowing text with an ellipsis (`...`), never wrap it or resize the row.
- If no secondary text can be displayed, use a localized fallback such as `No apps to unfreeze`.
- At least one unfreeze app is required, so a valid saved action always has secondary content.
- If an app was removed or uninstalled, keep the action visible and show an error/invalid state so it can be edited or deleted.
- Tapping a row executes the action.

Use stable row dimensions and avoid nested cards. A low-elevation surface-container style is preferred over large elevated cards.

## Create and Edit Dialog

The creation dialog title is exactly:

```text
Create action
```

The edit flow uses the exact title:

```text
Edit action
```

Fields appear in this order:

1. `Unfreeze`
2. `Launch`

Both fields open app pickers rather than accepting package-name text.

### Unfreeze Picker

- Allows selecting one or more apps.
- At least one app is required.
- Preserve temporary selection state until Save.
- Do not remove an app from the Unfreeze selection immediately when it is selected as Launch.

### Launch Picker

- Allows selecting exactly one app.
- Launch is required.
- Show the selected app name and icon in the field summary.

### Dialog Rules

- Keep the dialog compact; do not render every selected app as a chip or a multi-line list.
- Display selected app names as a single-line summary with end ellipsizing.
- Save is disabled until both an Unfreeze app and a Launch app are selected.
- On Save only, remove the Launch package from the Unfreeze package list if it is present there.
- Deduplicate the final Unfreeze list while preserving its selection order.
- Cancel discards temporary changes.
- Buttons are exactly `Cancel` and `Save`.
- The saved action must contain at least one unfreeze package after overlap removal. If selecting the same app for both fields would leave the list empty, reject Save with a localized validation message.

The app picker may reuse the existing Apps data/filtering logic, but it must not mutate Hail's persistent Home app selection or whitelist state.

## Long-Press Menu

Long-pressing an action opens a menu containing:

- `Edit action`
- `Create shortcut`
- `Duplicate`
- `Delete`

### Edit

Open the same form with the action's current Unfreeze and Launch selections. Save applies the overlap-removal rule described above.

### Duplicate

Create a new action with a new stable ID and the same package selections. Do not overwrite or reuse the original ID.

### Delete

Show a confirmation dialog before deletion. The destructive action is `Delete`; the other button is `Cancel`.

Deleting an action must not crash if a launcher still owns a previously pinned shortcut for that action. Such a shortcut should show an `Action unavailable` error when used.

## Persistence: Extend the Existing Room Database

Room is already used for app metadata caching in the upstream/current metadata-cache work. The relevant existing surfaces are:

- `AppMetadataEntity`
- `AppMetadataDao`
- `AppMetadataDatabase`
- `AppMetaCache`

The checked-out `dev` tree may need to first integrate that metadata-cache work; implementation must inspect the branch being changed and build on the actual database version present there. Do not create a second Room database and do not add a parallel Actions database singleton.

Add Actions to the existing Room database:

- Add an `ActionEntity` with a stable string ID, launch package, and timestamps only if the existing conventions need them.
- Store unfreeze packages in an ordered child table such as `ActionDependencyEntity`, or use the existing project-approved list-storage approach if one already exists.
- Use a foreign key from dependencies to actions with cascading delete.
- Add DAO methods for observing all actions, inserting, updating, duplicating, and deleting.
- Keep package order stable so summaries and unfreeze execution are deterministic.
- Expose database access through a repository/use-case layer; fragments must not call DAOs directly.
- Increment the existing Room schema version and add an explicit migration.
- Preserve the existing app metadata tables and cache behavior during migration.
- Test the migration from the currently shipped metadata-cache schema and test action CRUD independently.

Existing app/tag configuration persistence through `HailData` JSON remains unchanged. Actions belong in Room because they are relational, mutable records with child dependencies and stable IDs; moving unrelated HailData JSON into Room is out of scope.

## Execution Flow

Create one shared action executor used by both the Actions screen and launcher shortcuts.

```text
load action
validate target and dependencies
unfreeze dependencies sequentially
verify each dependency is unfrozen
launch target
finish intermediary activity when invoked by a shortcut
```

Requirements:

- Run package operations off the main thread.
- Execute sequentially to avoid racing root/shell/package-manager operations.
- Do not show a progress indicator; preparation is expected to be quick.
- Do not launch the target if any required app cannot be unfrozen or verified.
- Show one concise failure message naming the unavailable app.
- Prevent duplicate execution while the same action is already running.
- Do not automatically refreeze dependencies in this feature. Existing auto-freeze behavior remains responsible for cleanup.
- Refresh auto-freeze service state after successful unfreezing, matching existing launch behavior.
- Preserve existing normal app launch and freeze behavior.

## Android Shortcuts

`Create shortcut` means create a pinned launcher shortcut for the saved action.

Use the existing `HShortcuts` utility and pinned-shortcut support.

Shortcut requirements:

- Label is the launch app's current display name.
- Icon is the launch app's current application icon, using Hail's existing icon-pack/icon-loader behavior where applicable.
- ID is the action's stable ID, not a package-name hash.
- Intent targets Hail's action execution entry point and contains the action ID.
- It must execute correctly when Hail is not already open.
- It must not use the ordinary single-app launch intent, because that would skip dependency preparation.
- If the launcher does not support pinned shortcuts, reuse the existing concise shortcut failure message.
- If an action is deleted or unavailable, the shortcut must fail gracefully with a localized `Action unavailable` message.
- When supported by Android/launcher APIs, refresh shortcut label and icon after editing the launch app.

Extend `HailApi` with a dedicated action intent constant and action-ID extra. Extend `ApiActivity` to load and execute an action through the shared executor, then finish. Existing API actions and normal app shortcuts must continue unchanged.

## Navigation and Ownership

Update the existing navigation resources and central activity behavior:

- Add an Actions destination immediately after Home.
- Keep Apps as a destination reachable from Home, but remove it from visible bottom navigation and the rail.
- Treat Home, Actions, and Settings as top-level destinations for toolbar navigation.
- Treat Apps as a child/temporary destination with a back arrow.
- Configure the FAB centrally in `MainActivity` by destination:
  - Home: Apps button
  - Actions: Add button
  - Apps: hidden
  - Settings: hidden
- Remove the Home fragment's direct freeze-FAB click handler without removing access to existing freeze operations. Preserve an existing menu/action path for freezing.

## Likely Files

- `app/src/main/res/navigation/mobile_navigation.xml`
- `app/src/main/res/menu/nav_main.xml`
- `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt`
- `app/src/main/kotlin/com/aistra/hail/ui/home/HomeFragment.kt`
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt`
- New `ActionsFragment.kt` and adapter/view holder
- New action row and create/edit dialog layouts or the repository's established Compose equivalent
- `app/src/main/kotlin/com/aistra/hail/app/HailApi.kt`
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` only where shared constants or migration coordination are required
- `app/src/main/kotlin/com/aistra/hail/utils/HShortcuts.kt`
- `app/src/main/kotlin/com/aistra/hail/ui/api/ApiActivity.kt`
- New Room entities, DAO, database, repository, and executor classes
- `app/src/main/res/values/strings.xml` and all maintained translations

## Acceptance Checklist

- Actions appears immediately after Home.
- Apps is absent from bottom navigation and the navigation rail.
- Home FAB remains in its current bottom/end position and opens Apps.
- Apps has a working toolbar back arrow and system back behavior.
- Actions FAB is a plus button in the same FAB position.
- `Create action` opens with fields ordered `Unfreeze`, then `Launch`.
- At least one Unfreeze app and one Launch app are required.
- App names in the dialog truncate on one line.
- Launch/Unfreeze overlap is removed only when Save is pressed.
- The final saved action never has an empty Unfreeze list.
- Buttons are exactly `Cancel` and `Save`.
- Cards show the launch icon/name and dependency names without `Unfreezes:`.
- Dependency text is one line and ellipsized with `...`.
- Tapping a card unfreezes dependencies before launching the target.
- No preparation progress indicator is displayed.
- Failure prevents launch and identifies the failing app.
- Long press offers Edit action, Create shortcut, Duplicate, and Delete.
- Edit action prepopulates and updates the existing action.
- Duplicate creates a new action ID.
- Delete requires confirmation.
- Pinned shortcuts use the launch app's name and icon.
- Pinned shortcuts execute actions even when Hail is closed.
- Deleted/invalid actions fail gracefully.
- Existing app launch, freeze, auto-freeze, and shortcut features still work.

## Suggested Test Coverage

- Room DAO insert/read/update/delete/duplicate tests.
- Save validation tests for empty Unfreeze, empty Launch, and Launch overlap.
- Action executor tests for already-unfrozen dependencies, sequential unfreezing, verification failure, and successful launch.
- Shortcut intent tests confirming action ID routing rather than ordinary package launch.
- UI tests for field order, truncation, Save/Cancel behavior, long-press menu, and Home/Apps navigation.

## Backlog: Backup and Restore

Out of scope for the initial Actions implementation, but reserve a future Backup/Restore feature for all user-created Hail configuration.

The future backup should export a versioned logical data format containing:

- Home app selections
- Tags
- Pin and whitelist state
- Saved Actions and their ordered Unfreeze package lists
- Relevant user preferences only if they are intentionally included in backup scope

The backup should not include Room app metadata, cached labels, cached icons, install state, or other device-generated data. That information is cache data and should be rebuilt on the destination device.

Future restore requirements:

- Validate the format version before changing local data.
- Support merge, replace, and cancel conflict policies.
- Preserve unavailable package names so users can repair actions after restoring.
- Preserve stable Action IDs when possible.
- Detect duplicate actions without silently overwriting unrelated records.
- Import HailData JSON and Room-backed Actions through one coordinated operation.
- Apply all changes transactionally where possible, with rollback or a clear failure state.
- Never overwrite existing data before validation succeeds.
- Provide a summary of imported, skipped, conflicting, and unavailable entries.

The export format should be independent of the raw Room schema so future Room migrations do not invalidate user backups. Add migration tests for backup format versions separately from Room schema migration tests.