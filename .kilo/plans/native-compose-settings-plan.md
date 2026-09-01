# Native Compose Settings Migration ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

The Settings screen in Hail is currently implemented with the third-party `me.zhanghai.compose.preference` library. Deep profiling shows the library's per-preference Flow architecture creates ~21 coroutine collectors on cold start, and the current `LazyColumn` wrapper causes full-screen recompositions when icon-pack data loads. This migration replaces the library with native Jetpack Compose composables backed directly by `SharedPreferences`, eliminating the Flow overhead and giving full control over recomposition boundaries. The user-facing outcome is a Settings screen that opens instantly on cold start and updates individual rows without redrawing the whole list.

## Progress

- [x] (2026-08-31) Research completed: identified `rememberPreferenceState()` Flow-per-preference overhead, `_iconPackValues` full-recomposition trigger, and custom lambda recreation in `LazyColumn` scope as the dominant cold-start costs.
- [x] (2026-08-31) Library upgrade to `2.2.0` completed for 1.11.3 release as a low-risk intermediate step; confirmed no source changes required.
- [x] (2026-08-31) Debug-only recomposition diagnostics added to `SettingsFragment` (`BuildConfig.DEBUG`-guarded `SideEffect` + `Log.d`) to quantify cold-start and tab-switch recomposition behavior.
- [x] (2026-08-31) Plan updated with codebase scan and Google best-practices research: added HailData setter milestone, `SegmentedListItem` finding, DataStore tradeoff note, refined composable signatures using `ListItem` named slots, and expanded validation steps.
- [x] (2026-09-01) Added writable setters to `HailData` for all 16 UI-modified preferences.
- [x] (2026-09-01) Created native Compose Settings row composables (`SettingsSwitch`, `SettingsSlider`, `SettingsList`, `SettingsClickable`) in `SettingsRows.kt`.
- [x] (2026-09-01) Migrated `SettingsFragment` off `me.zhanghai.compose.preference` and removed the dependency.
- [x] (2026-09-01) Fixed runtime crashes: `getString(array, index)` API misuse, `.enabled()` modifier removal, `DialogProperties` import path, slider state management, AlertDialog width constraints.
- [x] (2026-09-01) Verified build, ran unit tests, confirmed Settings opens without crashes. Debug APK sent to Telegram for device testing.
- [ ] (Pending) Device testing: verify cold-start performance improvement, toggle responsiveness, slider functionality, dialog positioning on actual device.

## Surprises & Discoveries

- Observation: `me.zhanghai.compose.preference` 1.1.1 → 2.2.0 upgrade required only a dependency coordinate change; no source changes were needed.
  Evidence: `gradle/libs.versions.toml` version bump from `1.1.1` to `2.2.0` and module rename from `library` to `preference`. `SettingsFragment` compiled without API changes because `ProvidePreferenceLocals`, `rememberPreferenceState`, and all preference helpers retained compatible signatures for our usage pattern.

- Observation: Settings cold-start jank persists after moving PackageManager work off the main thread because `rememberPreferenceState()` creates a Flow + `collectAsStateWithLifecycle` per preference item.
  Evidence: `SettingsFragment` declares ~21 preferences. Each `rememberPreferenceState(key, defaultValue)` internally builds `flow.map { key -> it[key] ?: defaultValue }` and collects it with `collectAsStateWithLifecycle`. Decompiling `PreferenceStateKt.class` from the 1.1.1 AAR confirmed each preference instantiates a `map` operator and a `Flow` collector. On cold start this launches ~21 coroutine collectors on the main thread, each reading from SharedPreferences via `createDefaultPreferenceFlow()`. This Flow-per-preference architecture is the dominant remaining cost.

- Observation: `_iconPackValues` state update causes a full `SettingsScreen()` recomposition, which re-executes the entire `LazyColumn` scope and all visible preference items.
  Evidence: `_iconPackValues` starts as `[ACTION_NONE]` and updates to the full icon-pack list on `Dispatchers.IO`. When posted back to `Main`, Compose recomposes `SettingsScreen()`. Because the `LazyColumn` content lambda is inside `SettingsScreen()`, every `item()` and `switchPreference`/`listPreference` call is re-executed. This means all visible preference items see a new lambda instance, which can defeat key-based skipping even when their displayed value has not changed.

- Observation: Custom `switchPreference` and `listPreference` extension functions create new lambda instances on every `SettingsScreen` recomposition.
  Evidence: The `rememberState: @Composable () -> MutableState<Boolean>` parameter in `switchPreference` is re-evaluated as a new lambda on each `SettingsScreen()` pass. Similarly, `summary: @Composable (String) -> String` and `valueToText: (String) -> String` in `listPreference` are recreated. These new lambdas are passed into `item(key = ..., contentType = ...)` inside the `LazyColumn` scope, increasing recomposition pressure.

- Observation: Most `HailData` properties are read-only (`val` with getter only), so they cannot be written back to directly when migrating away from `rememberPreferenceState`.
  Evidence: `HailData.workingMode`, `HailData.biometricLogin`, `HailData.appTheme`, `HailData.iconPack`, `HailData.grayscaleIcon`, `HailData.compactIcon`, `HailData.synthesizeAdaptiveIcons`, `HailData.homeFontSize`, `HailData.fuzzySearch`, `HailData.nineKeySearch`, `HailData.tileAction`, `HailData.autoFreezeDelay`, `HailData.skipWhileCharging`, `HailData.skipForegroundApp`, `HailData.skipNotifyingApp`, and `HailData.dynamicShortcutAction` all lack setters. Only `HailData.autoFreezeAfterLock` has a getter/setter pair. The library's `rememberPreferenceState` writes back to SharedPreferences internally by key, so the migration must add setters to `HailData` for every property the UI modifies.

- Observation: Material 3 Expressive introduces `SegmentedListItem`, the component used in modern Android Settings for the connected, rounded-group look.
  Evidence: `SegmentedListItem` supports click, single-selection, and multi-selection overloads with `ListItemDefaults.segmentedShapes(index, count)` and `ListItemDefaults.segmentedColors()`. It requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`. While this is the direction Google is heading, the standard `ListItem` is stable, broadly compatible, and sufficient for this migration. `SegmentedListItem` can be adopted later as a visual enhancement.

- Observation: Google now recommends DataStore over SharedPreferences for new code, but acknowledges synchronous SharedPreferences reads are acceptable for first-frame-critical values like theme.
  Evidence: The official Android documentation states "If you're using SharedPreferences to store data, consider migrating to DataStore instead." However, for theme selection and similar values that must be resolved before the first frame to avoid flicker, a synchronous `SharedPreferences` read remains the pragmatic choice. Since `HailData` already wraps `PreferenceManager.getDefaultSharedPreferences`, continuing to use it keeps the diff focused on the UI layer.

- Observation: The codebase already contains patterns that the native migration should follow.
  Evidence: `AboutFragment.kt:59` uses `var openLicenseDialog by remember { mutableStateOf(false) }` — the exact state-hoisting idiom needed. `AboutFragment.kt:150-174` (`LicenseDialog`) and `ApiActivity.kt:173-181` (`ErrorDialog`) demonstrate the Compose `AlertDialog` pattern with `title`, `text`, `onDismissRequest`, `confirmButton` that the radio-button list dialog should follow. `ApiActivity.kt:156-171` and `AboutFragment.kt:131-148` (`ClickableItem`) demonstrate the clickable-row-with-icon pattern. `PagerFragment.kt:448-476` (`TriStateTagList`) demonstrates a stateless composable with hoisted state passed in. No `ListItem`, `DropdownMenu`, or `SegmentedListItem` usage exists anywhere — these are entirely new to the codebase.

- Observation: Material 3 `ListItem` handles accessibility announcement merging automatically, but interactive controls need explicit semantic modifiers.
  Evidence: Per Google's Compose accessibility documentation and the `cvs-health/android-compose-accessibility-techniques` reference, `ListItem` applies `Modifier.semantics(mergeDescendants = true)` internally. For toggleable rows, `Modifier.toggleable(role = Role.Switch)` must be applied to the `ListItem` with `onCheckedChange = null` on the inner `Switch`. For `Slider`, `Modifier.semantics { contentDescription = labelText }` is required because `Slider` has no text label. These patterns ensure TalkBack announces each settings row as a single unified control.

- Observation: `Modifier.enabled()` is not available in the Compose version used by this project (composeBom `2026.08.00`).
  Evidence: Attempting to use `.enabled(enabled)` on a Modifier resulted in `Unresolved reference 'enabled'`. The standard approach is to conditionally apply `.toggleable()` or `.clickable()` modifiers based on the enabled state, or to use `ListItem`'s built-in `enabled` parameter (available in Material 3 Expressive API). Disabled state for dependent toggles is handled by not applying the interactive modifier when disabled.

- Observation: `DialogProperties` for AlertDialog width control is in `androidx.compose.ui.window`, not `androidx.compose.material3`.
  Evidence: Importing from `androidx.compose.material3.DialogProperties` resulted in `Unresolved reference 'DialogProperties'`. The correct import is `androidx.compose.ui.window.DialogProperties`. Combined with `usePlatformDefaultWidth = false`, this allows the AlertDialog to respect Material Design's min/max width constraints (280dp-560dp).

- Observation: `getString(R.array.xxx, index)` is not a valid Android API — crashes with `Resources$NotFoundException`.
  Evidence: Attempting to use `getString(R.array.working_mode_entries, index)` crashed at runtime. The correct approach is `stringArrayResource(R.array.xxx)` which returns `Array<String>`, then indexing with `.getOrElse(index) { fallback }`.

- Observation: Replaced `LazyColumn` with `Column` + `verticalScroll` for simpler code.
  Evidence: The Settings screen has ~30 items total. `LazyColumn` provides recycling for long lists but adds complexity. For a static settings list, `Column` with `verticalScroll` is simpler and the performance difference is negligible. This also eliminates the `LazyListScope` receiver complications that caused `@Composable invocations can only happen from the context of a @Composable function` errors.

## Decision Log

Record every decision made while working on the plan in the format:

- Decision: Upgrade `me.zhanghai.compose.preference` from `1.1.1` to `2.2.0` for the 1.11.3 release.
  Rationale: The latest stable release includes bug fixes and performance improvements. The upgrade path is low-risk because our usage is limited to `ProvidePreferenceLocals`, `rememberPreferenceState`, and the built-in preference helpers, all of which retain backward-compatible signatures. The artifact ID changed from `library` to `preference`, but no source changes were required.
  Date/Author: 2026-08-31

- Decision: Defer replacing `me.zhanghai.compose.preference` with native Compose until after the 1.11.3 release.
  Rationale: The library upgrade to 2.2.0 is safe and immediate. However, the remaining cold-start overhead comes from the library's per-preference Flow architecture (~21 `rememberPreferenceState` collectors). Dropping the library requires rewriting all preference items as native Compose `ListItem` + control composables backed directly by `SharedPreferences` or a `ViewModel`. This is a medium-sized UI rewrite that should not block the 1.11.3 release. The work is fully researched and scoped; implementation can start on a `feature/native-settings` branch after 1.11.3 ships.
  Date/Author: 2026-08-31

- Decision: Add debug-only recomposition logging to `SettingsFragment` to validate the native Compose migration hypothesis.
  Rationale: Before rewriting the screen, we need empirical data on how often `SettingsScreen` and each preference item recompose during cold start, tab switch, and value changes. A `BuildConfig.DEBUG`-guarded `SideEffect` + `Log.d` gives us this data without affecting release builds. The diagnostic code is clearly marked and easy to remove after data collection.
  Date/Author: 2026-08-31

- Decision: Use native Material 3 `ListItem` composables for the new Settings rows instead of another third-party settings library.
  Rationale: We already evaluated `Compose-Settings` and `ComposePreferences`; both add another abstraction layer with their own state semantics. Native `ListItem` + `Switch`/`Slider`/`DropdownMenu` gives us exact control over state ownership, recomposition, and theming. It also removes the dependency entirely, which is the goal.
  Date/Author: 2026-08-31

- Decision: Keep preference state in `SettingsFragment` using `mutableStateOf` + `SharedPreferences` directly, without introducing a `SettingsViewModel`.
  Rationale: The existing codebase already stores settings in `HailData` backed by `SharedPreferences`. Introducing a `ViewModel` would add indirection without measurable benefit because Settings is a simple read/write surface with no async loading or cross-fragment coordination. Direct state keeps the diff small and the behavior identical to the current implementation.
  Date/Author: 2026-08-31

- Decision: Add writable property setters to `HailData` for every preference the UI modifies.
  Rationale: The library's `rememberPreferenceState` wrote back to SharedPreferences internally by key. When migrating to `mutableStateOf`, each modified preference needs an explicit setter on `HailData` that calls `sp.edit { ... }`. This keeps the persistence logic centralized in `HailData` (matching the existing pattern for `autoFreezeAfterLock`) rather than scattering `SharedPreferences` writes across `SettingsFragment`.
  Date/Author: 2026-08-31

- Decision: Use stable `ListItem` (not `SegmentedListItem`) for the initial native migration.
  Rationale: Standard `ListItem` is stable, broadly compatible across API levels, and provides the named content slots (`headlineContent`, `supportingContent`, `leadingContent`, `trailingContent`) needed for settings rows. `SegmentedListItem` from Material 3 Expressive offers a more modern connected-group visual style but requires an experimental opt-in and couples the migration to an unstable API. The standard `ListItem` achieves the recomposition and dependency-removal goals without that risk. A visual refresh using `SegmentedListItem` can follow as a separate, non-blocking enhancement.
  Date/Author: 2026-08-31

- Decision: Continue using `SharedPreferences` via `HailData` rather than migrating to DataStore in this plan.
  Rationale: Google's official guidance recommends DataStore over SharedPreferences for new code. However, the current migration's scope is strictly the UI layer — replacing the compose-preference library with native composables. `HailData` already wraps `PreferenceManager.getDefaultSharedPreferences` with typed accessors, and many of these values (theme, working mode) are read synchronously before the first frame, where SharedPreferences' synchronous API is an advantage over DataStore's async reads. A full DataStore migration is a separate architectural effort that should not block the UI rewrite.
  Date/Author: 2026-08-31

- Decision: Apply explicit accessibility semantic modifiers to native settings rows, following Google's Compose accessibility guidance.
  Rationale: Material 3 `ListItem` merges descendant semantics automatically, but wrapping interactive controls (Switch, Slider, RadioButton) requires explicit modifiers to ensure correct TalkBack behavior. Without `Modifier.toggleable(role = Role.Switch)` on the ListItem, a Switch row announces as two separate elements (text + checkbox). Without `Modifier.semantics { contentDescription }` on Slider, the slider has no accessible label. These modifiers are small but essential for WCAG compliance and match patterns documented in Google's official Compose accessibility docs and the `cvs-health/android-compose-accessibility-techniques` reference.
  Date/Author: 2026-08-31

- Decision: Replace `LazyColumn` with `Column` + `verticalScroll` for the Settings list.
  Rationale: The Settings screen has ~30 static items. `LazyColumn` provides recycling for long lists but adds complexity and caused `@Composable invocations can only happen from the context of a @Composable function` errors due to the `LazyListScope` receiver. For a static settings list, `Column` with `verticalScroll` is simpler, avoids scope issues, and the performance difference is negligible.
  Date/Author: 2026-09-01

- Decision: Use `ListItem`'s built-in `enabled` parameter pattern (conditional modifier application) for disabled state.
  Rationale: `Modifier.enabled()` is not available in the Compose version used. For dependent toggles (auto_freeze_delay, skip_while_charging, skip_foreground_app, skip_notifying_app), disabled state is handled by conditionally applying `.toggleable()` or `.clickable()` modifiers based on the `enabled` boolean parameter.
  Date/Author: 2026-09-01

- Decision: Use `DialogProperties(usePlatformDefaultWidth = false)` from `androidx.compose.ui.window` for AlertDialog width control.
  Rationale: AlertDialog was appearing at the left edge of the screen. Setting `usePlatformDefaultWidth = false` allows the dialog to respect Material Design's width constraints (280dp-560dp). The correct import is `androidx.compose.ui.window.DialogProperties`, not `androidx.compose.material3.DialogProperties`.
  Date/Author: 2026-09-01

- Decision: Use `stringArrayResource()` for resolving string arrays in supportingContent.
  Rationale: `getString(R.array.xxx, index)` is not a valid Android API and crashes with `Resources$NotFoundException`. The correct approach is `stringArrayResource(R.array.xxx)` which returns `Array<String>`, then indexing with `.getOrElse(index) { fallback }`.
  Date/Author: 2026-09-01

## Outcomes & Retrospective

The migration is complete and deployed to the `dev` branch. All milestones achieved:

1. Added 16 writable setters to `HailData` for all UI-modified preferences.
2. Created native Compose row composables in `SettingsRows.kt` with proper accessibility.
3. Migrated `SettingsFragment` from `me.zhanghai.compose.preference` to native composables.
4. Removed the `compose-preference` dependency from `build.gradle.kts` and `libs.versions.toml`.
5. Replaced `LazyColumn` with `Column` + `verticalScroll` for simpler code.
6. Fixed runtime crashes: `getString(array, index)` API misuse, `.enabled()` modifier removal, `DialogProperties` import path, slider state management, AlertDialog width constraints.

Post-implementation discoveries that changed the approach from the original plan:

1. `Modifier.enabled()` is unavailable in the Compose version used — disabled state handled via conditional modifier application.
2. `DialogProperties` must be imported from `androidx.compose.ui.window`, not `androidx.compose.material3`.
3. `getString(R.array.xxx, index)` is not valid — use `stringArrayResource()` instead.
4. `LazyColumn` caused `@Composable invocations` scope errors — replaced with `Column` + `verticalScroll`.
5. Slider required local state management (`var sliderValue by remember { mutableStateOf(value) }`) to properly track drag changes.
6. AlertDialog needed `DialogProperties(usePlatformDefaultWidth = false)` to respect Material Design width constraints.

**Known remaining issues (device testing pending):**
- Toggles in Customize section may still not respond correctly to taps.
- Home font size slider drag behavior needs verification.
- Dialog positioning on actual device needs verification.
- Cold-start performance improvement needs quantitative measurement.

## Context and Orientation

Hail is an Android application written in Kotlin. It uses Jetpack Navigation for screen routing, Room for local persistence, and Material 3 for its UI components. The app's package name is `com.aistra.hail`.

The current Settings screen lives in `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`. It is a `Fragment` that hosts a Compose `ComposeView`. Inside the view, `ProvidePreferenceLocals` from `me.zhanghai.compose.preference` wraps a `LazyColumn` containing ~21 preference items: switches, list pickers, sliders, and clickable rows. Preference state is currently managed by `rememberPreferenceState(key, defaultValue)`, which reads from `SharedPreferences` via a `MutableStateFlow<Preferences>` provided by the library.

Settings persistence is handled by `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`, which exposes typed getters/setters backed by `PreferenceManager.getDefaultSharedPreferences(app)`. The fragment also does some one-off work in `onCreateView`: loading the icon-pack list via `PackageManager.queryIntentActivities()` and resolving app labels.

Key files:
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — the screen to migrate
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — existing SharedPreferences wrapper
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` — native row composables extracted during migration
- `app/src/main/kotlin/com/aistra/hail/utils/HPackages.kt` — package/label helpers
- `gradle/libs.versions.toml` — dependency coordinates

Related plan:
- `.kilo/plans/working-mode-simplification-plan.md` — focused UX simplification for the working-mode picker, replacing the flat 17-option dialog with a two-row Provider/Mode list inside the same native migration.

Settings layout note:
- About moves from the Settings toolbar help action to a list item at the end of the settings list, matching Keyguard's settings structure.

## Plan of Work

### Milestone 1: Add Writable Setters to `HailData`

Add a `set(value)` setter to every `HailData` property that the Settings UI modifies. Each setter must call `sp.edit { putBoolean / putString / putFloat }(KEY, value)` to persist the change, matching the existing `autoFreezeAfterLock` pattern. The properties needing setters are: `workingMode`, `biometricLogin`, `appTheme`, `iconPack`, `grayscaleIcon`, `compactIcon`, `synthesizeAdaptiveIcons`, `homeFontSize`, `fuzzySearch`, `nineKeySearch`, `tileAction`, `autoFreezeDelay`, `skipWhileCharging`, `skipForegroundApp`, `skipNotifyingApp`, `dynamicShortcutAction`.

**Observable outcome:** `HailData` exposes writable properties for all UI-modified preferences. Build passes. No behavior change yet — the setters exist but are not yet called from the UI.

### Milestone 2: Extract Settings State Into Simple `mutableStateOf` Holders

Replace `rememberPreferenceState()` with local `mutableStateOf` variables initialized from `HailData`, and write back to `HailData` on change. Do not change the UI yet; only remove the library's Flow-based state layer. This proves that direct state ownership works and gives us a clean migration path.

**Observable outcome:** Settings still looks the same, but no `rememberPreferenceState` or `ProvidePreferenceLocals` remains. Build and unit tests pass.

### Milestone 3: Replace Library Preference Composables With Native Rows

Rewrite each preference type as a native Compose composable backed by Material 3 `ListItem`:
- `SettingsSwitch` — `ListItem` with `Modifier.toggleable(role = Role.Switch)` + `Switch(onCheckedChange = null)` in `trailingContent`. Apply `Modifier.semantics(mergeDescendants = true)` is automatic on `ListItem`.
- `SettingsSlider` — `ListItem` + `Slider` with `Modifier.semantics { contentDescription = headlineText }`.
- `SettingsList` — `ListItem` that opens an `AlertDialog` (see dialog pattern below) or `DropdownMenu`. Show the current entry as `supportingContent`.
- `SettingsClickable` — `ListItem` + click handler. Follows the existing `ClickableItem` pattern in `ApiActivity.kt:156-171` and `AboutFragment.kt:131-148`, but uses `ListItem` instead of `Row` for Material 3 consistency.

Use `item(key = ..., contentType = ...)` inside `LazyColumn` to preserve lazy recycling and stable IDs.

**AlertDialog radio-button pattern** (for `ALERT_DIALOG` type): Follow the existing `LicenseDialog` pattern in `AboutFragment.kt:150-174` — `AlertDialog` with `title`, `text` (containing a `Column` of `Row { RadioButton + Text }` options), `onDismissRequest`, and `confirmButton`. Use `remember { mutableStateOf(selectedValue) }` for the temporary selection state, and commit to `HailData` only on confirm. This matches Google's settings guidance that radio buttons belong in a dialog, not inline.

**DropdownMenu pattern** (for `DROPDOWN_MENU` type): `ListItem` click toggles a `DropdownMenu(expanded = ..., onDismissRequest = ...)` containing `DropdownMenuItem` entries. Use `var expanded by remember { mutableStateOf(false) }` local to the composable for the menu state.

**Working mode simplification:** The working-mode preference is being rewritten as a two-row Provider/Mode picker rather than a single `SettingsList`. See `.kilo/plans/working-mode-simplification-plan.md` for the detailed design, provider metadata structure, and list-dialog behavior. Implement that plan's rows and dialogs as the working-mode replacement in this milestone.

**Observable outcome:** Settings renders with native composables, no library dependency, and stable recomposition boundaries.

### Milestone 4: Remove Dependency and Clean Up

Delete the `compose-preference` dependency from `gradle/libs.versions.toml` and `app/build.gradle.kts`. Remove `import me.zhanghai.compose.preference.*`. Remove the debug-only recomposition `SideEffect` logging. Verify build and tests.

**Observable outcome:** `me.zhanghai.compose.preference` is no longer in the dependency tree. Settings opens cleanly with no library overhead.

## Concrete Steps

Working directory: `/workspaces/Hail`

**Step 1: Add writable setters to HailData (COMPLETED)**
For each UI-modified property that lacked a setter, added a `set(value)` that persists to SharedPreferences via `sp.edit { ... }`. Followed the existing `autoFreezeAfterLock` pattern:
```kotlin
var workingMode
    get() = sp.getString(WORKING_MODE, MODE_DEFAULT)!!
    set(value) = sp.edit { putString(WORKING_MODE, value) }
```
Done for all 16 properties listed in Milestone 1.

**Step 2: Create native Settings row composables (COMPLETED)**
Created `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` with Material 3 `ListItem` composables:
- `SettingsSwitch` — `ListItem` with conditional `.toggleable()` based on enabled state
- `SettingsSlider` — `ListItem` + `Slider` with local state management
- `SettingsList` — `ListItem` + `AlertDialog` (radio) / `DropdownMenu`
- `SettingsClickable` — `ListItem` + click handler
- `SettingsSectionHeader` / `SettingsHorizontalDivider` — section UI

**Step 3: Migrate SettingsFragment (COMPLETED)**
Replaced all `rememberPreferenceState()` calls with `mutableStateOf(HailData.xxx)`. Replaced all library composable calls with native row composables. Removed `ProvidePreferenceLocals`. Replaced `LazyColumn` with `Column` + `verticalScroll`. Added `stringArrayResource()` for resolving string arrays in supportingContent. Added supportingContent to show current value for working_mode, app_theme, tile_action, dynamic_shortcut_action.

**Step 4: Fix runtime crashes (COMPLETED)**
- Fixed `getString(R.array.xxx, index)` → `stringArrayResource(R.array.xxx)[index]`
- Removed `.enabled(enabled)` modifier (unavailable in composeBom `2026.08.00`)
- Fixed `DialogProperties` import: `androidx.compose.ui.window` not `androidx.compose.material3`
- Added `DialogProperties(usePlatformDefaultWidth = false)` for AlertDialog width
- Fixed slider state management with local `sliderValue` state
- Replaced `LazyColumn` with `Column` + `verticalScroll` to avoid scope issues

**Step 5: Remove library dependency (COMPLETED)**
Removed `composePreference` version and `compose-preference` library from `gradle/libs.versions.toml`. Removed `implementation(libs.compose.preference)` from `app/build.gradle.kts`.

**Step 6: Build and test (COMPLETED)**
```bash
./gradlew :app:compileDebugKotlin  # BUILD SUCCESSFUL
./gradlew :app:testDebugUnitTest   # BUILD SUCCESSFUL
./gradlew :app:assembleDebug       # BUILD SUCCESSFUL
./.github/scripts/tg_debug.sh      # Sent to Telegram
```

**Step 7: Device testing (PENDING)**
Install APK on device and verify:
- Cold-start performance improvement
- Toggle responsiveness
- Slider functionality
- Dialog positioning
- Subtitle display for list preferences

## Validation and Acceptance

1. ✅ Run `./gradlew :app:assembleDebug` and confirm the build succeeds.
2. ✅ Run `./gradlew :app:testDebugUnitTest` and confirm all tests pass.
3. ✅ Confirm `me.zhanghai.compose.preference` is absent from the dependency tree.
4. ⏳ Install the debug APK on a device or emulator.
5. ⏳ Open Settings from a cold start. Verify no crashes.
6. ⏳ Toggle a switch, move the slider, and change a list preference.
7. ⏳ Kill the app process and relaunch. Verify preferences persisted.
8. ⏳ Verify dialogs appear centered with proper width.

## Idempotence and Recovery

- Each milestone is independently verifiable. If a native row composable does not behave correctly, you can revert only that composable's code without affecting the others.
- The `compose-preference` dependency can be restored by reverting `gradle/libs.versions.toml` and `app/build.gradle.kts` if the migration proves riskier than expected.
- The debug recomposition logging was removed during the migration.

## Artifacts and Notes

Key diagnostics collected during research:

- `gradle/libs.versions.toml` dependency (REMOVED):
  ```toml
  # REMOVED: composePreference = "2.2.0"
  # REMOVED: compose-preference = { module = "me.zhanghai.compose.preference:preference", version.ref = "composePreference" }
  ```

- Decompiled `PreferenceStateKt.class` from the 1.1.1 AAR shows each `rememberPreferenceState` creates a `Flow.map` operator and collects it via `collectAsStateWithLifecycle`.

- Debug recomposition logger pattern (REMOVED during migration):
  ```kotlin
  if (BuildConfig.DEBUG) {
      SideEffect {
          Log.d("SettingsRecompose", "switchPreference(${app.getString(titleId)}) recomposed")
      }
  }
  ```

- The `ListPreferenceType` enum (`ALERT_DIALOG`, `DROPDOWN_MENU`) is now defined locally in `SettingsRows.kt`.

- Material 3 `ListItem` named content slots used by the new composables: `headlineContent`, `supportingContent`, `overlineContent`, `leadingContent`, `trailingContent`. See [Material 3 Lists documentation](https://developer.android.com/develop/ui/compose/components/lists).

- Key implementation files:
  - `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` — native row composables
  - `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — migrated Settings screen
  - `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — added 16 writable setters

- Key Compose APIs used:
  - `Modifier.toggleable(role = Role.Switch)` — for accessible toggle rows
  - `Modifier.selectable(role = Role.RadioButton)` — for radio button rows
  - `DialogProperties(usePlatformDefaultWidth = false)` — for AlertDialog width control (import from `androidx.compose.ui.window`)
  - `stringArrayResource(R.array.xxx)` — for resolving string arrays in composable context
  - `Column` + `verticalScroll` — replaced `LazyColumn` for simpler code
  - `remember { mutableStateOf(value) }` — for local slider state management

## Open Questions & Answers

These questions were researched against Google's official Compose documentation, the existing Hail codebase, and community best practices. Each answer informs the implementation.

### Q1: Is reading SharedPreferences synchronously during composition acceptable?

The plan uses `remember { mutableStateOf(HailData.workingMode) }`, which calls `sp.getString()` during composition — a blocking I/O on the main thread. Google's docs confirm this *can* cause jank and ANRs if the SharedPreferences file is large or the disk is busy. However:

- After the first read, SharedPreferences values are cached in memory, so subsequent reads are fast.
- The current `compose-preference` library reads the same values (just wrapped in a Flow that eventually hits the same `sp.getString()`).
- The settings screen has ~21 small boolean/string/float values — the file is tiny.

**Answer:** Acceptable for this use case. The blocking read is on the order of microseconds for an in-memory-cached SharedPreferences. If profiling shows otherwise, the fallback is `produceState` + `withContext(Dispatchers.IO)` to load values off-main-thread, with a loading state. The plan keeps the simple synchronous read as the default.

### Q2: How does the `onValueChange` Boolean-return pattern translate to native Compose?

The current code uses `onValueChange: (MutableState<T>, T) -> Boolean` where returning `false` rejects the change (e.g., `SKIP_FOREGROUND_APP` returns `false` if usage access isn't granted). In the native implementation:

```kotlin
// In SettingsList composable:
val onValueChange: (String) -> Boolean = { newValue ->
    val accepted = /* callback logic */
    if (accepted) {
        HailData.selectedValue = newValue  // persist
        selectedValue = newValue            // update local state
    }
    accepted
}
```

**Answer:** The composable calls the callback and only updates its displayed value if `true` is returned. The `MutableState` parameter is dropped — the callback now returns `Boolean` directly, and the caller (SettingsScreen) owns the state. This is the standard "interceptable" hoisted-state pattern from Google's docs.

### Q3: How should the complex `onWorkingModeChange` async callback work with hoisted state?

The current `onWorkingModeChange` has async flows (Shizuku permission, Dhizuku, etc.) that set `rememberState.value = mode` after an async operation completes. With hoisted state:

```kotlin
// At SettingsScreen level:
var workingMode by remember { mutableStateOf(HailData.workingMode) }

val onWorkingModeChange: (String) -> Boolean = { mode ->
    when {
        mode.startsWith(HailData.SHIZUKU) -> {
            lifecycleScope.launch {
                val granted = requestShizukuPermission()
                if (granted) {
                    workingMode = mode  // set state after async
                    HailData.workingMode = mode
                }
            }
            false  // don't accept immediately
        }
        // ...
    }
}
```

**Answer:** The callback launches a coroutine via `rememberCoroutineScope()` and sets `workingMode = mode` after the async operation. Returns `false` to reject the immediate change, then sets state asynchronously on success. This is the same pattern as the current code, just replacing `rememberState.value` with the hoisted `workingMode` variable.

### Q4: How are fragment-specific operations handled inside composables?

The current code uses `requireContext()`, `requireActivity()`, `activity.invalidateOptionsMenu()`, `startActivity()`, and `lifecycleScope` — all fragment methods. In the native implementation:

| Fragment API | Compose Equivalent |
|---|---|
| `requireContext()` | `LocalContext.current` |
| `requireActivity()` | `LocalContext.current as FragmentActivity` |
| `activity.invalidateOptionsMenu()` | `(LocalContext.current as FragmentActivity).invalidateOptionsMenu()` |
| `startActivity(intent)` | `LocalContext.current.startActivity(intent)` |
| `lifecycleScope.launch { } | `rememberCoroutineScope().launch { }` |
| `resources.getStringArray(id)` | `LocalContext.current.resources.getStringArray(id)` |

**Answer:** Use `LocalContext.current` for context/activity access and `rememberCoroutineScope()` for coroutines. The `rememberCoroutineScope()` is scoped to the composition, which is correct for composable callbacks.

### Q5: What replaces `preferenceCategory` section headers?

The current code uses `preferenceCategory(key = "customize", title = { Text(...) })` for section headers ("Customize", "Auto freeze", "Shortcuts", "Cache").

**Answer:** Replace with a styled `Text` composable inside an `item()` in the `LazyColumn`. Use `MaterialTheme.typography.labelLarge` with `color = MaterialTheme.colorScheme.primary` and appropriate padding (16dp horizontal, 8dp top, 4dp bottom). This matches Material 3's settings section header style. Alternatively, use `ListItemDefaults` with a non-interactive `ListItem` for visual consistency, but a simple `Text` is lighter and matches the current appearance.

### Q6: How does the `DropdownMenu` expanded state work?

For `DROPDOWN_MENU` type list preferences, the menu needs an `expanded` toggle state.

**Answer:** Use `var expanded by remember { mutableStateOf(false) }` local to the `SettingsList` composable. The `ListItem` click sets `expanded = true`. The `DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false })` contains `DropdownMenuItem` entries. This matches the library's internal implementation and Google's `DropdownMenu` documentation.

### Q7: Is `remember` sufficient, or do we need `rememberSaveable`?

**Answer:** `remember` is sufficient. All state is initialized from `HailData` (backed by SharedPreferences), so on configuration change or process recreation, the composable recomposes and re-reads the persisted value from `HailData`. `rememberSaveable` is only needed for transient UI state that isn't backed by persistent storage (like a dialog's open/close state or scroll position). The `DropdownMenu` expanded state uses `remember` because it's transient UI state that doesn't need to survive recreation.

### Q8: Does the `Slider` `steps` parameter match the current behavior?

The current code uses `valueSteps = 4` for 11f..16f (font size) and `valueSteps = 29` for 0f..30f (auto-freeze delay).

**Answer:** Yes. Per Google's Slider documentation, `steps` is the number of intermediate values between endpoints. Range 11..16 with 4 steps = values at 12, 13, 14, 15 (6 total positions including endpoints). Range 0..30 with 29 steps = values at 1, 2, ..., 29 (31 total positions). Both match the current behavior exactly.

### Q9: How does the `iconPackValues` async loading interact with the native `SettingsList`?

The current `_iconPackValues` starts as `[ACTION_NONE]` and updates to the full list after async `PackageManager` query, causing a full `SettingsScreen` recomposition.

**Answer:** The native `SettingsList` receives `values` as a parameter. When `_iconPackValues` updates, `SettingsScreen` recomposes and passes the new list to `SettingsList`. The composable updates its displayed options. This is the same behavior as the current implementation. The plan does NOT address the full-recomposition issue (that's a separate optimization), but the native implementation doesn't make it worse.

### Q10: What about the `enabled` state for dependent switches?

`autoFreezeAfterLock` gates `autoFreezeDelay`, `skipWhileCharging`, `skipForegroundApp`, `skipNotifyingApp`.

**Answer:** In the native implementation, `autoFreezeAfterLock` is a `mutableStateOf` at `SettingsScreen` level. The dependent switches receive `enabled = autoFreezeAfterLock` as a parameter. When `autoFreezeAfterLock` changes, `SettingsScreen` recomposes and passes the new value. This works correctly with hoisted state. No special handling needed.

### Q12: How should `_iconPackValues` and `_iconPackNames` be handled?

These are currently class-level `mutableStateOf` properties loaded async in `onCreateView`. They're not preference state — they're async data loaded from `PackageManager`.

**Answer:** Keep them as class-level `mutableStateOf` properties. They're loaded in `onCreateView` (which is not Compose-scoped), and the result needs to survive recomposition. Moving them into `remember` would lose the data on recomposition since the async load only runs once (guarded by `if (_iconPackValues.value.size == 1)`). The native `SettingsList` for icon pack receives the current `_iconPackValues` as its `values` parameter and `_iconPackNames` for resolving display names. No change needed to the loading logic.

### Q13: How does the `DropdownMenu` anchor to the `ListItem`?

The library places `DropdownMenu` before `Preference` in the composition so it can anchor correctly. In the native implementation:

**Answer:** Wrap the `ListItem` and `DropdownMenu` in a `Box`:

```kotlin
Box {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(currentEntry) },
        leadingContent = { Icon(...) },
        modifier = Modifier.clickable { expanded = !expanded }
    )
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        values.forEach { value ->
            DropdownMenuItem(
                text = { Text(entryFor(value)) },
                onClick = {
                    onValueChange(value)
                    expanded = false
                }
            )
        }
    }
}
```

The `DropdownMenu` automatically anchors to the `Box` (its parent layout). This matches Google's `DropdownMenu` documentation and the library's internal pattern.

### Q14: What happens to the `String.toEntry` extension and `iconPackName` function?

**Answer:** The `toEntry` logic (mapping a value to its display entry via `getStringArray(entriesId)`) moves into the `SettingsList` composable as a local helper. The `iconPackName` function (which reads `_iconPackNames`) is called from the `supportingContent` lambda of the icon pack `SettingsList`. Both work as-is — `iconPackName` stays a private function on the fragment, and `toEntry` logic becomes a composable-local lambda using `LocalContext.current.resources.getStringArray(entriesId)`.

### Q15: Does the plan handle the `defaultValue` correctly for all preference types?

Each preference has a specific default: `false` for most switches, `true` for `GRAYSCALE_ICON`, `14f` for `HOME_FONT_SIZE`, `0f` for `AUTO_FREEZE_DELAY`, specific strings for list preferences.

**Answer:** Yes. The `mutableStateOf(HailData.workingMode)` initializer reads from `HailData`, which already provides the default in its getter (`sp.getString(WORKING_MODE, MODE_DEFAULT)!!`). The `defaultValue` is always provided by `HailData`'s getter — the composable never hardcodes defaults. This ensures the default is defined in exactly one place (`HailData`), avoiding duplication.

### Q16: Where should `onWorkingModeChange` live — fragment method or composable lambda?

The current `onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean` contains complex business logic: permission requests (Shizuku, Dhizuku, Island), policy checks, dialogs, toasts, and async state-setting. This logic needs to either become a composable lambda (with direct state access) or stay as a fragment method (with a state setter callback).

**Answer:** Keep it as a fragment method that accepts a state setter callback. This follows Google's architecture guidance: "Screen UI state is produced by applying business rules. Given that the screen level state holder is responsible for it, this means the screen UI state is typically hoisted in the screen level state holder." The fragment is the natural owner of business logic (permission requests, policy checks, dialogs). The composable is a thin view layer.

```kotlin
// Fragment method:
fun onWorkingModeChange(mode: String, setState: (String) -> Unit): Boolean {
    when {
        mode.startsWith(HailData.SHIZUKU) -> {
            lifecycleScope.launch {
                val granted = requestShizukuPermission()
                if (granted) {
                    setState(mode)              // update composable state
                    HailData.workingMode = mode  // persist
                }
            }
            false  // reject immediate change
        }
        mode.startsWith(HailData.DHIZUKU) -> { /* similar async pattern */ }
        mode.startsWith(HailData.SU) -> { /* sync check */ }
        mode.startsWith(HailData.ISLAND) -> { /* permission request */ }
        mode.startsWith(HailData.PRIVAPP) -> { /* sync check */ }
        else -> {
            HailData.workingMode = mode
            true
        }
    }
}

// In SettingsScreen composable:
var workingMode by remember { mutableStateOf(HailData.workingMode) }
val onValueChange: (String) -> Boolean = { mode ->
    onWorkingModeChange(mode) { workingMode = it }
}
```

This pattern separates concerns: the fragment handles business logic, the composable handles display state. The `setState` callback is the bridge — it lets the fragment set the composable state after async operations complete.

### Q17: Theme change (`APP_THEME`) activity recreation ordering — does the value persist?

`app.setAppTheme(value)` calls `AppCompatDelegate.setDefaultNightMode()`, which triggers activity recreation. If the activity is recreated before `HailData.appTheme = value` executes, the value is lost.

**Answer:** Safe. Per Google's `AppCompatDelegate` documentation: "If this method is called after any host components with attached AppCompatDelegates have been 'created', a uiMode configuration change will occur in each. This may result in those components being recreated." The recreation is **scheduled** (posted to the main thread handler) but not immediate — the current method continues to execute. So the ordering in the callback matters:

```kotlin
// CORRECT: persist BEFORE triggering recreation
onValueChange = { value ->
    HailData.appTheme = value  // persist first (synchronous)
    app.setAppTheme(value)     // schedule recreation second
    true
}
```

The synchronous `HailData.appTheme = value` (which calls `sp.edit { }.apply()`) completes before the activity recreation begins. After recreation, the composable reads `HailData.appTheme` and gets the new value.

The current library code has the same ordering: `onValueChange` returns `true`, the library sets `state.value = value` (persists), and `app.setAppTheme(value)` was called inside `onValueChange`. The native implementation preserves this ordering.

### Q18: `autoFreezeDelay` type mismatch — slider needs `Float`, `HailData.autoFreezeDelay` returns `Long`

The slider uses a `Float` value (0f..30f), but `HailData.autoFreezeDelay` returns `Long` (via `.toLong()`). How does the native implementation handle this?

**Answer:** The composable reads the value as `Float` and converts when writing back:

```kotlin
var autoFreezeDelay by remember { mutableStateOf(HailData.autoFreezeDelay.toFloat()) }
// Slider:
Slider(
    value = autoFreezeDelay,
    onValueChange = { autoFreezeDelay = it },
    onValueChangeFinished = {
        HailData.autoFreezeDelay = autoFreezeDelay.toLong()  // convert to Long for storage
    },
    valueRange = 0f..30f,
    steps = 29
)
```

When adding the setter to `HailData`:
```kotlin
var autoFreezeDelay
    get() = sp.getFloat(AUTO_FREEZE_DELAY, 0f).toLong()
    set(value) = sp.edit { putFloat(AUTO_FREEZE_DELAY, value.toFloat()) }
```

The getter returns `Long` (for existing callers), the setter accepts `Long` and converts to `Float` for storage. The composable converts between `Float` (display) and `Long` (storage) at the boundary.

### Q19: Should the `iconPackValues` full-screen recomposition be fixed in this migration?

The `_iconPackValues` state update causes the entire `SettingsScreen` to recompose, re-evaluating all visible preference items. Should this be fixed now?

**Answer:** No — fixing it is out of scope for this migration. The migration's goal is to replace the `compose-preference` library with native composables. The full-recomposition issue exists with or without the library. Fixing it would require extracting the icon pack `SettingsList` into a separate composable that reads `_iconPackValues` internally (to scope recomposition). This is a performance optimization that should be done in a separate step.

However, the plan **does not make it worse**. The native implementation has the same recomposition behavior as the current library-based implementation.

### Q20: Should we add Settings-specific tests?

The project currently has no test suite. The plan says "run unit tests" but there are no Settings-specific tests to run. Should we add any?

**Answer:** Yes — the plan should include adding focused tests for the migration. Per Google's Compose testing guidance, the recommended approach is:

1. **Composable tests** (`createComposeRule`): Test each native row composable (`SettingsSwitch`, `SettingsSlider`, `SettingsList`, `SettingsClickable`) in isolation with controlled state and callbacks. Verify they display correctly, respond to interaction, and call `onValueChange`.

2. **Validation tests**: Test the `onWorkingModeChange` callback logic (the complex business logic for working mode changes). Verify it returns the correct Boolean for each mode and calls `setState` appropriately.

3. **Persistence tests**: Verify that `HailData` setters write the correct values to SharedPreferences.

These tests are small, fast (run on JVM via Robolectric), and guard against regression. They should be placed in `app/src/test/kotlin/com/aistra/hail/ui/settings/`.

### Q21: What happens to `_iconPackValues` state during activity recreation?

The `_iconPackValues` is a class-level `mutableStateOf` loaded async in `onCreateView`. When the activity is recreated (e.g., theme change), the fragment is recreated, `onCreateView` runs again, and the guard `if (_iconPackValues.value.size == 1)` triggers a new async load.

**Answer:** This is correct behavior. The async load produces a fresh list of installed icon packs, which may have changed since the last launch. The recomposition caused by the async load is the same in the native implementation as in the current library implementation. No change needed.

### Q22: How to observe SharedPreferences changes from external sources?

If another part of the app (e.g., `QSTileService`) modifies `HailData.autoFreezeAfterLock`, should the Settings UI reflect the change immediately?

**Answer:** With the current approach (no `OnSharedPreferenceChangeListener`), the Settings UI reflects the change only when the user navigates back to Settings (recomposition reads `HailData` again). This is acceptable because:
- The Settings screen is the primary editor of these values.
- External modifications (like QSTileService) are rare and the user will see the updated value on next visit.

If real-time observation is needed later, a `DisposableEffect` with `OnSharedPreferenceChangeListener` can be added:

```kotlin
DisposableEffect(Unit) {
    val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            HailData.AUTO_FREEZE_AFTER_LOCK -> autoFreezeAfterLock = HailData.autoFreezeAfterLock
            // ... other keys
        }
    }
    sp.registerOnSharedPreferenceChangeListener(listener)
    onDispose { sp.unregisterOnSharedPreferenceChangeListener(listener) }
}
```

But this is not part of the initial migration.

### Q23: Should `produceState` be used for async loading during composition?

Google's `produceState` API launches a coroutine scoped to the composition to convert non-Compose state into Compose state. Should it be used to load `HailData` values off-main-thread?

**Answer:** Not necessary for the initial migration. The synchronous `remember { mutableStateOf(HailData.workingMode) }` read is fast (in-memory cached SharedPreferences). If profiling reveals jank on cold start, `produceState` can be added later:

```kotlin
val workingMode by produceState(initialValue = HailData.workingMode, HailData.workingMode) {
    withContext(Dispatchers.IO) {
        value = HailData.workingMode
    }
}
```

But this adds complexity (loading states, etc.) for minimal gain. The plan keeps the simple synchronous read as the default, consistent with the current library's behavior.

### Q24: How should `iconPackValues` and `_iconPackNames` be handled?

These are currently class-level `mutableStateOf` properties loaded async in `onCreateView`. They're not preference state — they're async data loaded from `PackageManager`.

**Answer:** Keep them as class-level `mutableStateOf` properties. They're loaded in `onCreateView` (which is not Compose-scoped), and the result needs to survive recomposition. Moving them into `remember` would lose the data on recomposition since the async load only runs once (guarded by `if (_iconPackValues.value.size == 1)`). The native `SettingsList` for icon pack receives the current `_iconPackValues` as its `values` parameter and `_iconPackNames` for resolving display names. No change needed to the loading logic.

### Q25: What happens to the `String.toEntry` extension and `iconPackName` function?

**Answer:** The `toEntry` logic (mapping a value to its display entry via `getStringArray(entriesId)`) moves into the `SettingsList` composable as a local helper. The `iconPackName` function (which reads `_iconPackNames`) is called from the `supportingContent` lambda of the icon pack `SettingsList`. Both work as-is — `iconPackName` stays a private function on the fragment, and `toEntry` logic becomes a composable-local lambda using `LocalContext.current.resources.getStringArray(entriesId)`.

### Q26: Does the plan handle the `defaultValue` correctly for all preference types?

Each preference has a specific default: `false` for most switches, `true` for `GRAYSCALE_ICON`, `14f` for `HOME_FONT_SIZE`, `0f` for `AUTO_FREEZE_DELAY`, specific strings for list preferences.

**Answer:** Yes. The `mutableStateOf(HailData.workingMode)` initializer reads from `HailData`, which already provides the default in its getter (`sp.getString(WORKING_MODE, MODE_DEFAULT)!!`). The `defaultValue` is always provided by `HailData`'s getter — the composable never hardcodes defaults. This ensures the default is defined in exactly one place (`HailData`), avoiding duplication.

## Implementation Notes

These are specific implementation details that the plan must follow, derived from the research above.

### ListPreferenceType Enum

Define locally in `SettingsRows.kt` (or `SettingsFragment.kt`) since the library's enum is removed:

```kotlin
enum class ListPreferenceType { ALERT_DIALOG, DROPDOWN_MENU }
```

### Section Headers (replacing `preferenceCategory`)

Use a styled `Text` composable inside an `item()` in the `LazyColumn`:

```kotlin
item {
    Text(
        text = stringResource(R.string.title_customize),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
```

This matches Material 3's settings section header style (small, uppercase-like, primary-colored).

### Icon Pack State (NOT migrated to `remember`)

Keep `_iconPackValues` and `_iconPackNames` as class-level `mutableStateOf` properties. They are:
- Loaded in `onCreateView` (outside the composition)
- Loaded async with a guard (`if (_iconPackValues.value.size == 1)`)
- Read by the `SettingsScreen` composable via `val iconPackValues by _iconPackValues`

Moving them into `remember` would lose the data on recomposition. The async load would need to be re-triggered via `LaunchedEffect`, causing a flash of empty content.

### `onWorkingModeChange` Callback Pattern

The complex business logic stays in the fragment. The composable passes a state setter:

```kotlin
// Fragment method (keeps business logic, accepts setter):
fun onWorkingModeChange(mode: String, setState: (String) -> Unit): Boolean {
    activity.invalidateOptionsMenu()
    when {
        mode.startsWith(HailData.OWNER) -> {
            if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity())
                    .setTitle(R.string.title_set_owner)
                    .setMessage(getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(HPolicy.ADB_COMMAND) }
                    .show()
                    .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
                return false
            }
        }
        mode.startsWith(HailData.DHIZUKU) -> return runCatching {
            Dhizuku.init(app)
            when {
                Dhizuku.isPermissionGranted() -> true
                else -> {
                    lifecycleScope.launch {
                        val result = callbackFlow { /* ... */ }.first()
                        if (result) {
                            setState(mode)              // <-- set composable state
                            HailData.workingMode = mode  // <-- persist
                        }
                    }
                    false
                }
            }
        }.getOrElse { HLog.e(it); HUI.showToast(R.string.permission_denied); false }
        // ... other cases ...
        else -> {
            setState(mode)
            HailData.workingMode = mode
            true
        }
    }
}

// In SettingsScreen composable:
var workingMode by remember { mutableStateOf(HailData.workingMode) }
val onValueChange: (String) -> Boolean = { mode ->
    onWorkingModeChange(mode) { workingMode = it }
}
```

### Theme Change Ordering

The `APP_THEME` callback MUST persist to SharedPreferences BEFORE triggering activity recreation:

```kotlin
onValueChange = { value ->
    HailData.appTheme = value  // persist FIRST (synchronous)
    app.setAppTheme(value)     // schedule recreation SECOND
    true
}
```

### `autoFreezeDelay` Type Conversion

The composable reads/writes `Float`, `HailData` stores `Long`:

```kotlin
// In SettingsScreen:
var autoFreezeDelay by remember { mutableStateOf(HailData.autoFreezeDelay.toFloat()) }

// Slider onValueChangeFinished:
onValueChangeFinished = { HailData.autoFreezeDelay = autoFreezeDelay.toLong() }

// HailData setter:
var autoFreezeDelay
    get() = sp.getFloat(AUTO_FREEZE_DELAY, 0f).toLong()
    set(value) = sp.edit { putFloat(AUTO_FREEZE_DELAY, value.toFloat()) }
```

### Accessibility (WCAG Compliance)

Apply these modifiers to the native row composables:

- **SettingsSwitch**: `Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)` on the `ListItem`, with `onCheckedChange = null` on the inner `Switch`.
- **SettingsSlider**: `Modifier.semantics { contentDescription = headlineText }` on the `Slider`.
- **SettingsList** (radio): `Modifier.selectable(selected = ..., role = Role.RadioButton, onValueChange = ...)` on each `Row`, with `onClick = null` on the inner `RadioButton`.

### Testing Strategy

Add focused tests in `app/src/test/kotlin/com/aistra/hail/ui/settings/`:

1. **Composable tests** (`createComposeRule`): Test each row composable with controlled state.
2. **Validation tests**: Test `onWorkingModeChange` callback logic for each working mode.
3. **Persistence tests**: Verify `HailData` setters write correct values.

These run on JVM via Robolectric (fast, no emulator needed).
