# Working Mode Settings Simplification ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

If PLANS.md file is checked into the repo, reference the path to that file here from the repository root and note that this document must be maintained in accordance with PLANS.md.

## Relationship to Native Compose Settings Migration

This plan is a focused UX simplification that sits on top of the existing native Compose settings migration in `.kilo/plans/native-compose-settings-plan.md`. The working-mode preference is one of the settings being rewritten as native Compose rows. Do not treat this as a separate UI rewrite; treat it as a targeted replacement of the working-mode picker with a two-row Provider/Mode list while the rest of the settings migration proceeds as planned. All milestones below assume the native migration is in progress or complete for the working-mode row.

## Purpose / Big Picture

The current Working mode setting presents all 17 available modes in a single alert dialog. Users must scan the full list to find their preferred mode, and the flat list makes it hard to understand which modes belong to which system capability. This simplification replaces the flat picker with a two-row list-driven selection: a **Provider** row and a **Mode** row, each opening a Keyguard-style list dialog. The user-visible outcome is a settings screen where a user first chooses a provider such as Shizuku, Root, Dhizuku, Device Owner, Island/Insular, System App, or Idle, and then selects the specific action for that provider. The change follows Material 3 settings guidance, the native Compose migration direction, and the list-dialog pattern used by Keyguard.

## Progress

- [x] (2026-08-31) Research completed: confirmed current flat 17-option dialog in SettingsFragment, Google Settings guidance threshold of 15, existing mode constants and downstream parsing by prefix/suffix, existing dialog patterns in the codebase, and Material 3 Compose list/selection/dialog patterns from Android Developers documentation.
- [x] (2026-08-31) Approach selected: two-row Provider/Mode list picker with list dialogs, preserving all existing mode constants and routing all selections through the existing `onWorkingModeChange` business logic, implemented with native list rows as specified in the native Compose migration plan.
- [x] (2026-08-31) Help button approach selected: add a dedicated Settings toolbar action for Working mode docs that opens an external URL, keeping the existing About help action unchanged.
- [ ] (2026-08-31) Add provider metadata to `HailData` as additive data structures alongside existing constants.
- [ ] (2026-08-31) Replace the flat `ListPreference` in `SettingsFragment` with two native list rows: Provider and Mode.
- [ ] (2026-08-31) Implement provider-selection list dialog and mode-selection list dialog, with Mode disabled when the selected provider has no separate modes.
- [ ] (2026-08-31) Add Working mode help toolbar action and wire it to the docs URL.
- [ ] (2026-08-31) Update resources to remove the flat `working_mode_entries` array, verify build, tests, and manual flow on a device or emulator.

## Surprises & Discoveries

- Observation: The existing `working_mode_entries` array already contains the exact provider-prefixed labels needed for the two-level UI. Each entry is of the form "Provider - Action" (for example "Shizuku - Hide", "Root - Disable").
  Evidence: `app/src/main/res/values/arrays.xml` lines 32-50 contain 17 items that all follow the pattern `mode_<provider>_<action>`, and the corresponding strings in `app/src/main/res/values/strings.xml` lines 63-76 and 158-165 all use the hyphenated format.

- Observation: The current `HailData.WORKING_MODE_VALUES` list is flat and ordered to match the flat entries array. Changing to a two-row model requires a provider-indexed mapping, but the existing mode string constants can remain unchanged because the rest of the app selects behavior by parsing the prefix and suffix of the mode string.
  Evidence: `app/src/main/kotlin/com/aistra/hail/app/AppManager.kt` lines 55-67 and 93-105 use `startsWith(HailData.SHIZUKU)` and `endsWith(HailData.HIDE)` respectively. As long as the mode string format is preserved, no downstream changes are required.

- Observation: The current `onWorkingModeChange` method is already designed to accept any mode string and run the appropriate permission/side-effect flow for that mode. This means the two-row UI can commit the final mode string through the exact same callback without duplicating business logic.
  Evidence: `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` lines 509-612 contain the full async permission handling for Shizuku, Dhizuku, Root, Island, and Device Owner inside `onWorkingModeChange`.

- Observation: Google's Settings guidance explicitly says "For 15 or more settings, group related settings under a subscreen." The current 17-mode flat dialog exceeds this threshold, which makes the simplification a guideline-driven improvement rather than a stylistic preference.
  Evidence: https://developer.android.com/design/ui/mobile/guides/patterns/settings retrieved 2026-08-31.

- Observation: Material 3 list guidance explicitly supports single-action list items and single-select lists, and `AlertDialog` is the standard container for selection dialogs. Android Developers Compose docs show `AlertDialog` with `title`, `text`, `onDismissRequest`, `confirmButton`, and `dismissButton` as the preferred dialog API.
  Evidence: https://m3.material.io/components/lists/guidelines and https://developer.android.com/develop/ui/compose/components/dialog retrieved 2026-08-31.

- Observation: Keyguard's settings system is built with Compose + Material 3 Expressive and uses a dynamic settings UI with list rows that open selection dialogs or child screens. The pattern is list-driven navigation rather than one giant radio-button dialog.
  Evidence: https://deepwiki.com/AChep/keyguard-app/7.1-settings-system and https://keyguard.dev/docs/ssh-agent/ retrieved 2026-08-31.

- Observation: Hail's native migration already introduced reusable row composables in `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt`, including `SettingsClickable`, `SettingsList`, and section headers. The working-mode simplification can reuse these patterns instead of introducing new row types.
  Evidence: `SettingsRows.kt` search results showing `SettingsClickable` and `SettingsList` implementations with `ListItem` and `clickable` modifiers.

## Decision Log

- Decision: Use a two-row Provider/Mode list picker instead of a single flat dialog.
  Rationale: Google's Settings guidance recommends subscreens for 15 or more settings. The 17-mode flat dialog is the only setting in Hail that exceeds this threshold, and provider grouping matches the README documentation structure that users already see. A two-row list also matches the Keyguard-style settings pattern.
  Date/Author: 2026-08-31

- Decision: Keep all existing `MODE_*` string constants and the `WORKING_MODE_VALUES` list unchanged during the migration, adding only a provider lookup structure.
  Rationale: Downstream code in `AppManager`, `HailApp`, and elsewhere parses mode strings by prefix and suffix. Changing the constants would require touching every consumer. A provider lookup layer on top of the existing constants achieves the UI simplification with zero downstream risk.
  Date/Author: 2026-08-31

- Decision: Implement provider and mode selection as list dialogs, not radio-button dialogs.
  Rationale: Keyguard uses list-driven selection dialogs rather than radio-button dialogs. Material 3 list guidance supports single-action list items and single-select lists. `AlertDialog` with a custom list content is the standard Android/Material pattern for selection dialogs. This also matches the native Compose migration's preference for `ListItem`-based rows.
  Date/Author: 2026-08-31

- Decision: Route all mode selections through the existing `onWorkingModeChange` method, passing only the final chosen mode string.
  Rationale: The method already contains all permission-request and side-effect logic. Duplicating that logic in a new callback would create two sources of truth and increase regression risk.
  Date/Author: 2026-08-31

- Decision: Define providers and their modes in `HailData` as data structures rather than hardcoding them in the fragment.
  Rationale: `HailData` already owns all working-mode constants and the values list. Adding provider metadata there keeps the fragment as a pure view layer and makes the provider list testable without Compose.
  Date/Author: 2026-08-31

- Decision: Grey out the Mode row when the selected provider has only one mode.
  Rationale: When a provider has only one mode, such as Idle or Device Owner, there is no mode choice to make. Disabling the row makes the UI state explicit and prevents the user from opening an unnecessary dialog. This matches Material 3 guidance for disabled list items.
  Date/Author: 2026-08-31

- Decision: Keep Idle as a normal provider with one mode, shown first in the provider list.
  Rationale: Treating Idle as a first-class provider keeps the UI consistent. It appears alongside Shizuku, Root, and others. Selecting it commits `MODE_DEFAULT` immediately because it has no sub-modes, and the Mode row becomes disabled.
  Date/Author: 2026-08-31

- Decision: Use `MaterialAlertDialogBuilder` with a custom list view for selection dialogs, matching the current imperative dialog patterns in `SettingsFragment`.
  Rationale: The current codebase already uses `MaterialAlertDialogBuilder` for cache rebuild, pin shortcuts, Device Owner setup, terminal input, and terminal results. Staying with the same dialog API keeps the change local and consistent with the current migration stage. If the broader settings migration switches to Compose `AlertDialog` first, migrate these dialogs in a follow-up change.
  Date/Author: 2026-08-31

## Outcomes & Retrospective

The plan is in research phase. Outcomes will be recorded after each milestone. The expected end state is a Settings screen where the Working mode section shows two rows: Provider and Mode. Tapping Provider opens a list dialog with 7 providers. Tapping a provider with multiple modes opens a second list dialog with only that provider's available modes. Selecting Idle or any single-mode provider commits immediately and disables the Mode row. The user can accomplish the same mode changes as before, but with fewer items to scan and clearer visual grouping.

The Settings screen layout also changes: About moves from the toolbar help action to a list item at the end of the settings list, matching Keyguard's settings structure. The toolbar help action becomes dynamic, showing context-sensitive help based on the current settings section.

## Context and Orientation

Hail is an Android application written in Kotlin. It uses Jetpack Navigation for screen routing, Room 3.0 for persistence, Material 3 for UI, and Jetpack Compose inside `Fragment` screens via `ComposeView`. The package name is `com.aistra.hail`.

The current Settings screen is in `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`. It hosts a Compose `ComposeView`. In the current pre-migration state, it uses the third-party `me.zhanghai.compose.preference` library. During the native migration, reusable row composables were extracted to `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt`, including `SettingsClickable`, `SettingsList`, and section headers. The working mode preference is being rewritten as two native list rows inside the same `LazyColumn`. The target end state is a Provider row and a Mode row, each opening a `MaterialAlertDialogBuilder` list dialog.

Currently, About is accessed from the Settings toolbar via `action_help`. As part of this plan, About moves to a list item at the end of the settings list, matching Keyguard's settings structure. The toolbar help action becomes dynamic: default state opens general help, and when the user is interacting with Working mode provider/mode selection, it changes to Working mode documentation help.

All working mode constants and the flat values list are defined in `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` lines 43-89. The string labels for the 17 modes are in `app/src/main/res/values/arrays.xml` lines 32-50 and `app/src/main/res/values/strings.xml` lines 63-76 and 158-165.

When the user picks a mode, the row's click handler calls `onWorkingModeChange` (defined in `SettingsFragment.kt` lines 509-612). That method contains all permission requests, policy checks, dialogs, and async side effects. It writes the chosen mode back to state after permission is granted.

Downstream behavior selection happens in `app/src/main/kotlin/com/aistra/hail/app/AppManager.kt` lines 55-67 and 93-105. The app determines which system capability to use by checking `HailData.workingMode.startsWith(HailData.SHIZUKU)` and what action to perform by checking `endsWith(HailData.HIDE)`.

The native migration already established these patterns in `SettingsRows.kt`:
- `SettingsClickable` with `headlineContent`, `supportingContent`, `leadingContent`, and `Modifier.clickable`
- `SettingsList` with selected value, values, entries, and `ListPreferenceType`
- `settingsSectionHeader` for section titles

Key files to read first:
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — the screen that renders working mode and owns the selection callback
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` — the native row composables introduced by the migration
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — the constants and SharedPreferences wrapper
- `app/src/main/kotlin/com/aistra/hail/app/AppManager.kt` — downstream consumers that parse mode strings
- `app/src/main/res/values/arrays.xml` — the flat `working_mode_entries` array
- `app/src/main/res/values/strings.xml` — the user-facing mode labels
- `.kilo/plans/native-compose-settings-plan.md` — the broader settings migration plan this work is nested inside

## Plan of Work

The work is broken into four milestones. Each milestone produces a working, testable state, and each builds on the assumption that the native Compose migration is either already complete for the working-mode row or will be completed in parallel.

### Milestone 1: Add Provider Metadata to `HailData`

Add a provider-to-modes map in `HailData` alongside the existing flat `WORKING_MODE_VALUES` list. This map will drive the two-row UI without changing any existing mode constants or downstream parsing logic. The map must include every mode currently in `WORKING_MODE_VALUES`, grouped by provider, and must expose helpers to resolve the human-readable provider name for a mode and the mode list for a provider key.

### Milestone 2: Replace the Flat `ListPreference` With Two Native List Rows

In `SettingsFragment`, replace the existing `listPreference` call for working mode with two native list rows inside the `LazyColumn`:

- **Provider row** — shows current provider in `supportingContent`; on click opens a list dialog with providers
- **Mode row** — shows current mode in `supportingContent`; on click opens a list dialog with modes for the selected provider; disabled when the provider has only one mode

Use `SettingsClickable` or a custom `ListItem` row for both. Use `MaterialAlertDialogBuilder` with a simple list view for the selection dialogs, matching the current imperative dialog patterns in the codebase.

### Milestone 2b: Add Dynamic Help Toolbar Action

The Settings toolbar already includes `action_help`, but it is currently static: it always navigates to About. Replace that static behavior with a context-sensitive help action that changes based on the current settings page or active selection flow.

Recommended behavior:
- Default state: `action_help` opens general Settings help/about
- When the user is interacting with Working mode provider/mode selection: `action_help` changes to a Working mode help action that opens documentation about providers and modes
- The change should happen in `onPrepareMenu()` or equivalent, using the same dynamic-menu pattern already used for `action_terminal` and `action_remove_owner`

Implementation outline:
- In `onPrepareMenu()`, detect whether the working-mode provider/mode flow is active
- Update the help menu item's `title` and/or `icon` to indicate context-sensitive help
- In `onMenuItemSelected()`, route `action_help` to the appropriate help destination:
  - General help: current About/docs behavior
  - Working mode help: open the docs URL or a dedicated help screen explaining providers and modes
- Keep the existing `action_help` item in `menu_settings.xml`; do not add a second static help button

This keeps one toolbar action, avoids UI clutter, and gives users contextual guidance when they need it most.

### Milestone 4: Implement Provider and Mode Selection Dialogs

For the provider dialog, show a list of providers: Idle, Shizuku, Root, Dhizuku, Device Owner, Island/Insular, System App. When the user selects a provider:
- If it has exactly one mode, commit immediately via `onWorkingModeChange`
- If it has multiple modes, close the provider dialog and open the mode dialog

For the mode dialog, show only the selected provider's available modes. When the user selects a mode, pass it to `onWorkingModeChange` and update the row supporting text on success.

If the broader native migration later switches to Compose `AlertDialog`, the same list-dialog structure can be migrated without changing the provider/mode data model.

### Milestone 4: Update Resources, Verify, and Clean Up

Remove `R.array.working_mode_entries` from `app/src/main/res/values/arrays.xml`. Verify the build, run unit tests, and confirm on a device that selecting each provider and mode still triggers the correct permission flows and persists the chosen mode.

## Concrete Steps

Working directory: `/workspaces/Hail`

**Step 1: Inspect current working mode implementation**

Run:
    cat app/src/main/kotlin/com/aistra/hail/app/HailData.kt | sed -n '43,90p'
    cat app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt | sed -n '118,127p'
    cat app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt | sed -n '509,612p'
    cat app/src/main/kotlin/com/aistra/hail/app/AppManager.kt | sed -n '55,105p'
    cat app/src/main/res/values/arrays.xml | sed -n '32,50p'
    cat app/src/main/res/values/strings.xml | sed -n '63,76p'
    cat app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt | sed -n '1,260p'

Expected output: you should see the flat `WORKING_MODE_VALUES` list, the single `listPreference` block for working mode, the long `onWorkingModeChange` method, the 17-entry flat array, and the native row composables in `SettingsRows.kt`.

**Step 2: Add provider metadata in `HailData`**

In `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`, after the existing `WORKING_MODE_VALUES` definition (around line 89), add:

    data class WorkingModeProvider(val key: String, val label: String, val modes: List<String>)

    val WORKING_MODE_PROVIDERS = listOf(
        WorkingModeProvider("idle", "Idle", listOf(MODE_DEFAULT)),
        WorkingModeProvider("shizuku", "Shizuku", listOf(MODE_SHIZUKU_STOP, MODE_SHIZUKU_DISABLE, MODE_SHIZUKU_HIDE, MODE_SHIZUKU_SUSPEND)),
        WorkingModeProvider("root", "Root", listOf(MODE_SU_STOP, MODE_SU_DISABLE, MODE_SU_HIDE, MODE_SU_SUSPEND)),
        WorkingModeProvider("dhizuku", "Dhizuku", listOf(MODE_DHIZUKU_HIDE, MODE_DHIZUKU_SUSPEND)),
        WorkingModeProvider("owner", "Device Owner", listOf(MODE_OWNER_HIDE, MODE_OWNER_SUSPEND)),
        WorkingModeProvider("island", "Island/Insular", listOf(MODE_ISLAND_HIDE, MODE_ISLAND_SUSPEND)),
        WorkingModeProvider("privapp", "System App", listOf(MODE_PRIVAPP_STOP, MODE_PRIVAPP_DISABLE))
    )

    fun providerForMode(mode: String): WorkingModeProvider? = when {
        mode == MODE_DEFAULT -> WORKING_MODE_PROVIDERS.first { it.key == "idle" }
        else -> WORKING_MODE_PROVIDERS.find { it.modes.any { m -> mode == m } }
    }

    fun modesForProvider(key: String): List<String> =
        WORKING_MODE_PROVIDERS.firstOrNull { it.key == key }?.modes.orEmpty()

Run:
    ./gradlew :app:compileDebugKotlin

Expected output: build succeeds, no compilation errors.

**Step 3: Replace the flat `ListPreference` with two native list rows**

In `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`, replace the existing working-mode `listPreference(...)` block with two custom items inside the `LazyColumn`.

Provider row shape:

    item(key = "working_mode_provider", contentType = "WorkingModeProvider") {
        var workingMode by remember { mutableStateOf(HailData.workingMode) }
        val provider = HailData.providerForMode(workingMode)
        SettingsClickable(
            headlineContent = { Text(text = stringResource(R.string.working_mode)) },
            supportingContent = { Text(text = provider?.label ?: stringResource(R.string.label_default)) },
            leadingContent = { Icon(imageVector = Icons.Outlined.Adb, contentDescription = null) },
            onClick = {
                val items = HailData.WORKING_MODE_PROVIDERS.map { it.label }.toTypedArray()
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.working_mode)
                    .setItems(items) { _, which ->
                        val chosen = HailData.WORKING_MODE_PROVIDERS[which]
                        val modes = chosen.modes
                        if (modes.size == 1) {
                            val accepted = onWorkingModeChange(modes.first()) { workingMode = it }
                            if (accepted) HailData.workingMode = modes.first()
                        } else {
                            workingMode = modes.first()
                            showModeDialog = true
                        }
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
    }

Mode row shape:

    item(key = "working_mode_mode", contentType = "WorkingModeMode") {
        var workingMode by remember { mutableStateOf(HailData.workingMode) }
        val provider = HailData.providerForMode(workingMode)
        val modes = provider?.modes.orEmpty()
        val modeEnabled = modes.size > 1
        val currentModeLabel = if (workingMode == HailData.MODE_DEFAULT) {
            stringResource(R.string.label_default)
        } else {
            stringResource(
                id = when (workingMode) {
                    HailData.MODE_SHIZUKU_STOP -> R.string.mode_shizuku_stop
                    HailData.MODE_SHIZUKU_DISABLE -> R.string.mode_shizuku_disable
                    HailData.MODE_SHIZUKU_HIDE -> R.string.mode_shizuku_hide
                    HailData.MODE_SHIZUKU_SUSPEND -> R.string.mode_shizuku_suspend
                    HailData.MODE_SU_STOP -> R.string.mode_su_stop
                    HailData.MODE_SU_DISABLE -> R.string.mode_su_disable
                    HailData.MODE_SU_HIDE -> R.string.mode_su_hide
                    HailData.MODE_SU_SUSPEND -> R.string.mode_su_suspend
                    HailData.MODE_DHIZUKU_HIDE -> R.string.mode_dhizuku_hide
                    HailData.MODE_DHIZUKU_SUSPEND -> R.string.mode_dhizuku_suspend
                    HailData.MODE_OWNER_HIDE -> R.string.mode_owner_hide
                    HailData.MODE_OWNER_SUSPEND -> R.string.mode_owner_suspend
                    HailData.MODE_ISLAND_HIDE -> R.string.mode_island_hide
                    HailData.MODE_ISLAND_SUSPEND -> R.string.mode_island_suspend
                    HailData.MODE_PRIVAPP_STOP -> R.string.mode_privapp_stop
                    HailData.MODE_PRIVAPP_DISABLE -> R.string.mode_privapp_disable
                    else -> R.string.label_default
                }
            )
        }
        SettingsClickable(
            headlineContent = { Text(text = stringResource(R.string.mode)) },
            supportingContent = { Text(text = currentModeLabel) },
            leadingContent = { Icon(imageVector = Icons.Outlined.Tune, contentDescription = null) },
            enabled = modeEnabled,
            onClick = {
                val modeItems = modes.map { mode ->
                    when (mode) {
                        HailData.MODE_DEFAULT -> stringResource(R.string.label_default)
                        HailData.MODE_SHIZUKU_STOP -> stringResource(R.string.mode_shizuku_stop)
                        HailData.MODE_SHIZUKU_DISABLE -> stringResource(R.string.mode_shizuku_disable)
                        HailData.MODE_SHIZUKU_HIDE -> stringResource(R.string.mode_shizuku_hide)
                        HailData.MODE_SHIZUKU_SUSPEND -> stringResource(R.string.mode_shizuku_suspend)
                        HailData.MODE_SU_STOP -> stringResource(R.string.mode_su_stop)
                        HailData.MODE_SU_DISABLE -> stringResource(R.string.mode_su_disable)
                        HailData.MODE_SU_HIDE -> stringResource(R.string.mode_su_hide)
                        HailData.MODE_SU_SUSPEND -> stringResource(R.string.mode_su_suspend)
                        HailData.MODE_DHIZUKU_HIDE -> stringResource(R.string.mode_dhizuku_hide)
                        HailData.MODE_DHIZUKU_SUSPEND -> stringResource(R.string.mode_dhizuku_suspend)
                        HailData.MODE_OWNER_HIDE -> stringResource(R.string.mode_owner_hide)
                        HailData.MODE_OWNER_SUSPEND -> stringResource(R.string.mode_owner_suspend)
                        HailData.MODE_ISLAND_HIDE -> stringResource(R.string.mode_island_hide)
                        HailData.MODE_ISLAND_SUSPEND -> stringResource(R.string.mode_island_suspend)
                        HailData.MODE_PRIVAPP_STOP -> stringResource(R.string.mode_privapp_stop)
                        HailData.MODE_PRIVAPP_DISABLE -> stringResource(R.string.mode_privapp_disable)
                        else -> mode
                    }
                }.toTypedArray()
                val checkedItem = modes.indexOf(workingMode).takeIf { it >= 0 } ?: 0
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.mode)
                    .setSingleChoiceItems(modeItems, checkedItem) { _, which ->
                        val chosenMode = modes[which]
                        val accepted = onWorkingModeChange(chosenMode) { workingMode = it }
                        if (accepted) HailData.workingMode = chosenMode
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        )
    }

Notes:
- `setSingleChoiceItems` gives a native Android single-choice list dialog with radio buttons, which matches the Material/M3 selection-dialog guidance without manually building radio rows.
- `SettingsClickable` already supports `enabled = false`, which provides the required grey-out behavior for single-mode providers.
- The current `working_mode` string resource is reused for the provider dialog title. Add a new `mode` string resource for the mode dialog title if it does not already exist.

**Step 4: Refactor `onWorkingModeChange` to accept a state setter**

Change the signature in `SettingsFragment.kt` from:

    fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean

to:

    fun onWorkingModeChange(mode: String, setState: (String) -> Unit): Boolean

Inside the method, replace every `rememberState.value = mode` with `setState(mode)`. Keep all existing permission logic, dialogs, toasts, and async flows unchanged. This is a mechanical rename that preserves behavior.

Call site change in the provider and mode rows:

    onWorkingModeChange(mode) { workingMode = it }

**Step 5: Update strings and arrays**

Remove `R.array.working_mode_entries` from `app/src/main/res/values/arrays.xml`. The flat entries array is no longer needed because the provider labels and mode labels are now drawn from `HailData.WORKING_MODE_PROVIDERS` and the existing `mode_*` string resources.

If `R.string.mode` does not exist, add it to `app/src/main/res/values/strings.xml`:

    <string name="mode">Mode</string>

Verify resources compile:
    ./gradlew :app:processDebugResources

Expected output: resource compilation succeeds with no missing-reference errors.

**Step 6: Verify build and tests**

    ./gradlew :app:compileDebugKotlin :app:processDebugResources :app:testDebugUnitTest

Expected output: build succeeds and all tests pass. The only existing tests are `HailDataTest`; after the change they should still pass because `MODE_*` constants are unchanged.

**Step 7: Move About to end of settings list**

Add an About list item at the end of the `LazyColumn` in `SettingsFragment`, using `SettingsClickable`:

    item(key = "about", contentType = "About") {
        SettingsClickable(
            headlineContent = { Text(text = stringResource(R.string.title_about)) },
            leadingContent = { Icon(imageVector = Icons.Outlined.Info, contentDescription = null) },
            onClick = {
                findNavController().navigate(R.id.nav_about)
            }
        )
    }

Remove the static `action_help` behavior from `onMenuItemSelected` or repurpose it for dynamic help. If the toolbar help action is retained as dynamic, it should no longer always navigate to About; instead it should show context-sensitive help.

**Step 8: Manual device verification**

1. Install the debug APK on a device or emulator.
2. Open Settings.
3. Tap Working mode. Expected: the provider row shows the current provider, and the mode row shows the current mode or `Idle`.
4. Tap the Provider row. Expected: an alert dialog with exactly 7 list options: Idle, Shizuku, Root, Dhizuku, Device Owner, Island/Insular, System App.
5. Tap "Shizuku". Expected: the provider dialog closes and the provider row updates to `Shizuku`.
6. Tap the Mode row. Expected: an alert dialog with 4 single-choice options: Force Stop, Disable, Hide, Suspend.
7. Select "Hide" and grant permission if prompted. Expected: the dialog closes, the Mode row updates to `Shizuku - Hide`, and the terminal menu visibility updates the same as before.
8. Return to the Settings screen. Expected: both rows show the chosen provider and mode.
9. Tap Provider, then select "Idle". Expected: provider commits immediately, Mode row becomes disabled, and mode shows `Idle`.
10. Repeat for Root, Dhizuku, Device Owner, Island/Insular, and System App. Expected: each provider shows only its supported modes, and selecting any mode triggers the same side effects as the current flat dialog.

## Validation and Acceptance

1. Run `./gradlew :app:assembleDebug` and confirm the build succeeds.
2. Run `./gradlew :app:testDebugUnitTest` and confirm all existing tests pass.
3. Run `./gradlew :app:processDebugResources` and confirm no resource errors.
4. Install the debug APK on a device or emulator.
5. Open Settings and tap Working mode. Expected: two list rows are shown: Provider and Mode.
6. Tap Provider. Expected: an alert dialog with exactly 7 list options.
7. Tap "Shizuku". Expected: the provider row updates to `Shizuku`.
8. Tap Mode. Expected: an alert dialog with 4 single-choice options.
9. Select "Hide" and grant permission if prompted. Expected: the dialog closes, the Mode row updates to `Shizuku - Hide`, and the terminal menu visibility updates the same as before.
10. Open Provider again, select "Idle", and confirm. Expected: provider commits immediately, Mode row becomes disabled, and mode shows `Idle`.
11. Repeat steps 6-10 for Root, Dhizuku, Device Owner, Island/Insular, and System App. Expected: each provider shows only its supported modes, and selecting any mode triggers the same side effects as the current flat dialog.
12. Kill the app and relaunch. Expected: the last chosen provider and mode are still displayed in the Settings rows.

## Idempotence and Recovery

- The provider metadata in `HailData` is additive. Adding the `WorkingModeProvider` data class and `WORKING_MODE_PROVIDERS` list does not modify existing constants or values, so it can be reverted by removing those lines without touching other code.
- The `onWorkingModeChange` signature change is mechanical. If the new signature causes issues, it can be reverted to the original `MutableState<String>` parameter without changing any internal logic.
- The old flat `R.array.working_mode_entries` is no longer referenced after Step 3. If rollback is needed, the old `listPreference` block can be restored from version control and the array re-added.
- All changes are confined to `HailData.kt`, `SettingsFragment.kt`, `arrays.xml`, and `strings.xml`. No changes to `AppManager` or other downstream consumers are required.
- If the broader native Compose migration is incomplete when this plan starts, `MaterialAlertDialogBuilder` is used for the dialogs. When the rest of Settings switches to Compose `AlertDialog`, these dialogs can be migrated in a follow-up change without altering the provider/mode data model.

## Artifacts and Notes

- Current working mode preference block in `SettingsFragment.kt` (lines 118-127):
    listPreference(
        key = HailData.WORKING_MODE,
        defaultValue = HailData.MODE_DEFAULT,
        onValueChange = ::onWorkingModeChange,
        values = HailData.WORKING_MODE_VALUES,
        entriesId = R.array.working_mode_entries,
        titleId = R.string.working_mode,
        icon = Icons.Outlined.Adb,
        type = ListPreferenceType.ALERT_DIALOG
    )

- Current `onWorkingModeChange` signature (line 509):
    fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean

- Current `working_mode_entries` array (`arrays.xml` lines 32-50):
    <string-array name="working_mode_entries">
        <item>@string/mode_default</item>
        <item>@string/mode_shizuku_stop</item>
        ...
        <item>@string/mode_privapp_disable</item>
    </string-array>

- Native settings row composables in `SettingsRows.kt`:
  - `SettingsClickable` for clickable list rows with `headlineContent`, `supportingContent`, `leadingContent`
  - `SettingsList` for selection rows with `selectedValue`, `values`, `entries`, `type`
  - `settingsSectionHeader` for section titles

- Android Developers dialog guidance (retrieved 2026-08-31): `AlertDialog` with `title`, `text`, `onDismissRequest`, `confirmButton`, `dismissButton` is the standard selection-dialog API. For simple single-choice lists, `setSingleChoiceItems` on `MaterialAlertDialogBuilder` provides native radio-button list behavior.

- Material 3 list guidance (retrieved 2026-08-31): lists support single-action list items, single-select lists with radio buttons, and disabled list-item states. This matches the Provider/Mode row design.

- Google Settings guidance excerpt (retrieved 2026-08-31): "For 15 or more settings, group related settings under a subscreen. Use subscreens to simplify multiple settings or extensive categories, helping users focus on fewer choices."

## Interfaces and Dependencies

No new external dependencies are required. The implementation uses existing Material 3 and Compose APIs already present in the project.

In `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`, add:

    data class WorkingModeProvider(val key: String, val label: String, val modes: List<String>)

    val WORKING_MODE_PROVIDERS: List<WorkingModeProvider>

    fun providerForMode(mode: String): WorkingModeProvider?
    fun modesForProvider(key: String): List<String>

In `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`, change:

    fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean

to:

    fun onWorkingModeChange(mode: String, setState: (String) -> Unit): Boolean

Add two custom list rows for working mode inside the `LazyColumn` in `SettingsScreen()`:
- Provider row using `SettingsClickable`, opening a `MaterialAlertDialogBuilder` list dialog
- Mode row using `SettingsClickable`, opening a `MaterialAlertDialogBuilder` single-choice list dialog, with `enabled = modes.size > 1`

Remove `R.array.working_mode_entries` from `app/src/main/res/values/arrays.xml` and any `entriesId` references from the old `listPreference` call.

Add `R.string.mode` to `app/src/main/res/values/strings.xml` if it does not already exist.

## Open Questions & Answers

These questions were researched against the current Hail codebase, Google's official documentation, Material Design guidance, and Keyguard's documented settings patterns. Each answer informs the implementation and is aligned with the native Compose migration plan.

### Q1: Should the selection dialogs use radio buttons, plain list items, or native single-choice dialogs?

Material 3 list guidance supports single-select lists with radio buttons, and Android's `AlertDialog` supports `setSingleChoiceItems` for exactly this case. Keyguard uses list-driven selection dialogs rather than raw radio-button grids. For Hail, the simplest implementation that matches both Material guidance and Keyguard style is `MaterialAlertDialogBuilder.setSingleChoiceItems(...)` for the mode dialog and `setItems(...)` for the provider dialog.

**Answer:** Use `MaterialAlertDialogBuilder.setItems(...)` for the provider dialog and `MaterialAlertDialogBuilder.setSingleChoiceItems(...)` for the mode dialog. This gives list-driven selection with native radio-button behavior without manually building radio rows.

### Q2: Should the Provider row commit immediately for single-mode providers?

Yes. Idle, Device Owner, Dhizuku, Island/Insular, and System App each have one or two modes. For providers with exactly one mode, there is no mode choice to make, so the provider dialog should commit immediately and skip the mode dialog. For providers with multiple modes, the provider dialog should close and the mode dialog should open.

**Answer:** Auto-commit only for providers with exactly one mode. For providers with multiple modes, always show the mode dialog. This keeps permission flows explicit and predictable.

### Q3: How should the Mode row behave when the selected provider has only one mode?

The Mode row should be disabled and greyed out. Material 3 list guidance explicitly supports disabled list items. The user should not be able to open a mode dialog when there is no mode choice.

**Answer:** Set `enabled = modes.size > 1` on the Mode row. When disabled, the row uses Material 3 disabled visuals and ignores clicks.

### Q4: How should Idle be displayed and handled?

Idle should be the first item in the provider list. When selected, it commits `MODE_DEFAULT` immediately because it has only one mode. The Mode row becomes disabled and its supporting text shows `Idle`.

**Answer:** Treat Idle as a normal provider with label `Idle` and mode list `[MODE_DEFAULT]`. Place it first in `WORKING_MODE_PROVIDERS`.

### Q5: What happens if the user cancels the mode dialog after opening it?

The temporary selection state is discarded. The row continues to show the previously persisted `workingMode`. No side effects run because `onWorkingModeChange` is only called on confirm.

**Answer:** Cancel dismisses the dialog without calling `onWorkingModeChange` and without writing to `HailData.workingMode`.

### Q6: Should `WORKING_MODE_VALUES` be kept after the migration?

Yes. It is still used elsewhere in the codebase for iteration and validation. Removing it would require auditing every consumer. The new provider map is additive and coexists with the flat list.

**Answer:** Keep `WORKING_MODE_VALUES` unchanged. Add `WORKING_MODE_PROVIDERS`, `providerForMode`, and `modesForProvider` as additions only.

### Q7: Do translations/localized strings need updates?

The flat `working_mode_entries` array is removed, but the individual `mode_*` strings remain and continue to be used as mode labels inside the mode dialog. Provider names are hardcoded in `WORKING_MODE_PROVIDERS.labels`; if localization is required, those labels should be moved to string resources. For the initial simplification, hardcoded English provider labels are acceptable, but they should be extracted to `strings.xml` if the app maintains translations.

**Answer:** Keep provider labels hardcoded for the initial change, but add a TODO comment to extract them to `strings.xml` if translations are maintained. Do not block the migration on localization.

### Q8: How does the `onWorkingModeChange` state-setter callback handle async permission grants?

The current implementation sets `rememberState.value = mode` after async permission grants. With the new signature, `setState(mode)` is called instead. Because `setState` is a lambda captured from the composable's `remember { mutableStateOf(...) }`, updating it from a coroutine launched in `lifecycleScope` still triggers recomposition correctly.

**Answer:** No behavioral change. Replace `rememberState.value = mode` with `setState(mode)` and the async flows continue to work.

### Q9: Should `invalidateOptionsMenu()` still be called?

Yes. `onWorkingModeChange` currently calls `activity.invalidateOptionsMenu()` to show/hide the Terminal menu item based on whether the mode is Root or Shizuku. That side effect must remain unchanged.

**Answer:** Keep `activity.invalidateOptionsMenu()` at the top of `onWorkingModeChange`.

### Q10: What if a provider gains or loses modes in the future?

The provider metadata is defined declaratively in `WORKING_MODE_PROVIDERS`. Adding or removing a mode for a provider only requires updating that list. The UI automatically reflects the new set because both rows and dialogs read from the same source.

**Answer:** Future mode changes are localized to `HailData.WORKING_MODE_PROVIDERS` and the corresponding `MODE_*` constants. No fragment changes needed.

### Q11: How does this plan interact with the broader native Compose settings migration?

The native migration plan already replaces the working-mode `listPreference` with a native row in Milestone 3. This simplification should be implemented as the concrete content of that replacement: the working-mode row becomes two list rows with list dialogs instead of a single flat dialog. The rest of the native migration proceeds unchanged.

**Answer:** Treat this plan as the working-mode-specific instantiation of native migration Milestone 3. Implement the two rows and dialogs as the replacement for `WORKING_MODE`. Do not block the rest of the settings migration on this change.
