# Native Compose Settings Migration ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

The Settings screen in Hail is currently implemented with the third-party `me.zhanghai.compose.preference` library. Deep profiling shows the library's per-preference Flow architecture creates ~21 coroutine collectors on cold start, and the current `LazyColumn` wrapper causes full-screen recompositions when icon-pack data loads. This migration replaces the library with native Jetpack Compose composables backed directly by `SharedPreferences`, eliminating the Flow overhead and giving full control over recomposition boundaries. The user-facing outcome is a Settings screen that opens instantly on cold start and updates individual rows without redrawing the whole list.

## Progress

- [x] (2026-08-31) Research completed: identified `rememberPreferenceState()` Flow-per-preference overhead, `_iconPackValues` full-recomposition trigger, and custom lambda recreation in `LazyColumn` scope as the dominant cold-start costs.
- [x] (2026-08-31) Library upgrade to `2.2.0` completed for 1.11.3 release as a low-risk intermediate step; confirmed no source changes required.
- [x] (2026-08-31) Debug-only recomposition diagnostics added to `SettingsFragment` (`BuildConfig.DEBUG`-guarded `SideEffect` + `Log.d`) to quantify cold-start and tab-switch recomposition behavior.
- [ ] Create native Compose Settings row composables (`SettingsSwitch`, `SettingsSlider`, `SettingsList`, `SettingsClickable`) using Material 3 `ListItem` + trailing controls.
- [ ] Migrate `SettingsFragment` off `me.zhanghai.compose.preference` and remove the dependency.
- [ ] Verify build, run unit tests, and confirm Settings opens without recomposition spikes in debug logs.

## Surprises & Discoveries

- Observation: `me.zhanghai.compose.preference` 1.1.1 → 2.2.0 upgrade required only a dependency coordinate change; no source changes were needed.
  Evidence: `gradle/libs.versions.toml` version bump from `1.1.1` to `2.2.0` and module rename from `library` to `preference`. `SettingsFragment` compiled without API changes because `ProvidePreferenceLocals`, `rememberPreferenceState`, and all preference helpers retained compatible signatures for our usage pattern.

- Observation: Settings cold-start jank persists after moving PackageManager work off the main thread because `rememberPreferenceState()` creates a Flow + `collectAsStateWithLifecycle` per preference item.
  Evidence: `SettingsFragment` declares ~21 preferences. Each `rememberPreferenceState(key, defaultValue)` internally builds `flow.map { key -> it[key] ?: defaultValue }` and collects it with `collectAsStateWithLifecycle`. Decompiling `PreferenceStateKt.class` from the 1.1.1 AAR confirmed each preference instantiates a `map` operator and a `Flow` collector. On cold start this launches ~21 coroutine collectors on the main thread, each reading from SharedPreferences via `createDefaultPreferenceFlow()`. This Flow-per-preference architecture is the dominant remaining cost.

- Observation: `_iconPackValues` state update causes a full `SettingsScreen()` recomposition, which re-executes the entire `LazyColumn` scope and all visible preference items.
  Evidence: `_iconPackValues` starts as `[ACTION_NONE]` and updates to the full icon-pack list on `Dispatchers.IO`. When posted back to `Main`, Compose recomposes `SettingsScreen()`. Because the `LazyColumn` content lambda is inside `SettingsScreen()`, every `item()` and `switchPreference`/`listPreference` call is re-executed. This means all visible preference items see a new lambda instance, which can defeat key-based skipping even when their displayed value has not changed.

- Observation: Custom `switchPreference` and `listPreference` extension functions create new lambda instances on every `SettingsScreen` recomposition.
  Evidence: The `rememberState: @Composable () -> MutableState<Boolean>` parameter in `switchPreference` is re-evaluated as a new lambda on each `SettingsScreen()` pass. Similarly, `summary: @Composable (String) -> String` and `valueToText: (String) -> String` in `listPreference` are recreated. These new lambdas are passed into `item(key = ..., contentType = ...)` inside the `LazyColumn` scope, increasing recomposition pressure.

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

## Outcomes & Retrospective

The library upgrade to 2.2.0 shipped in 1.11.3 without source changes. Post-upgrade profiling confirmed the remaining bottleneck is architectural: `rememberPreferenceState()` creates a Flow collector per preference, and the current `LazyColumn` wrapper triggers full-screen recompositions on async data updates. The debug recomposition logger is in place to collect quantitative data before the native rewrite begins.

## Context and Orientation

Hail is an Android application written in Kotlin. It uses Jetpack Navigation for screen routing, Room for local persistence, and Material 3 for its UI components. The app's package name is `com.aistra.hail`.

The current Settings screen lives in `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`. It is a `Fragment` that hosts a Compose `ComposeView`. Inside the view, `ProvidePreferenceLocals` from `me.zhanghai.compose.preference` wraps a `LazyColumn` containing ~21 preference items: switches, list pickers, sliders, and clickable rows. Preference state is currently managed by `rememberPreferenceState(key, defaultValue)`, which reads from `SharedPreferences` via a `MutableStateFlow<Preferences>` provided by the library.

Settings persistence is handled by `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`, which exposes typed getters/setters backed by `PreferenceManager.getDefaultSharedPreferences(app)`. The fragment also does some one-off work in `onCreateView`: loading the icon-pack list via `PackageManager.queryIntentActivities()` and resolving app labels.

Key files:
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — the screen to migrate
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — existing SharedPreferences wrapper
- `app/src/main/kotlin/com/aistra/hail/utils/HPackages.kt` — package/label helpers
- `gradle/libs.versions.toml` — dependency coordinates

## Plan of Work

### Milestone 1: Extract Settings State Into Simple `mutableStateOf` Holders

Replace `rememberPreferenceState()` with local `mutableStateOf` variables initialized from `HailData`, and write back to `HailData` on change. Do not change the UI yet; only remove the library's Flow-based state layer. This proves that direct state ownership works and gives us a clean migration path.

**Observable outcome:** Settings still looks the same, but no `rememberPreferenceState` or `ProvidePreferenceLocals` remains. Build and unit tests pass.

### Milestone 2: Replace Library Preference Composables With Native Rows

Rewrite each preference type as a native Compose composable backed by Material 3 `ListItem`:
- `SettingsSwitch` — `ListItem` + `Switch`
- `SettingsSlider` — `ListItem` + `Slider`
- `SettingsList` — `ListItem` + click handler + `DropdownMenu` or `AlertDialog`
- `SettingsClickable` — `ListItem` + click handler

Use `item(key = ..., contentType = ...)` inside `LazyColumn` to preserve lazy recycling and stable IDs.

**Observable outcome:** Settings renders with native composables, no library dependency, and stable recomposition boundaries.

### Milestone 3: Remove Dependency and Clean Up

Delete the `compose-preference` dependency from `gradle/libs.versions.toml` and `app/build.gradle.kts`. Remove `import me.zhanghai.compose.preference.*`. Remove the debug-only recomposition `SideEffect` logging. Verify build and tests.

**Observable outcome:** `me.zhanghai.compose.preference` is no longer in the dependency tree. Settings opens cleanly with no library overhead.

## Concrete Steps

Working directory: `/workspaces/Hail`

**Step 1: Inspect current SettingsFragment**
```bash
cat app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt
```

**Step 2: Add native Settings row composables**
Create the row composables in `SettingsFragment.kt` or a new `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt`. Start with `SettingsSwitch`, then `SettingsSlider`, then `SettingsList`, then `SettingsClickable`.

**Step 3: Migrate state to `mutableStateOf`**
Replace each `rememberPreferenceState(key, defaultValue)` with:
```kotlin
var workingMode by mutableStateOf(HailData.workingMode)
```
and write back on change:
```kotlin
HailData.workingMode = newValue
```

**Step 4: Swap UI to native rows**
Replace each `switchPreference(...)` / `listPreference(...)` / `sliderPreference(...)` / `preference(...)` call with the corresponding native row composable inside the same `LazyColumn`.

**Step 5: Remove library dependency**
```bash
# gradle/libs.versions.toml
# Remove or comment out:
# composePreference = "2.2.0"
# compose-preference = { module = "me.zhanghai.compose.preference:preference", version.ref = "composePreference" }

# app/build.gradle.kts
# Remove:
# implementation(libs.compose.preference)
```

**Step 6: Build and test**
```bash
./gradlew :app:compileDebugKotlin :app:processDebugResources
./gradlew :app:testDebugUnitTest
```

**Step 7: Remove debug recomposition logging**
Remove the `BuildConfig.DEBUG` + `SideEffect` + `Log.d` blocks from `SettingsFragment.kt`.

## Validation and Acceptance

1. Run `./gradlew :app:assembleDebug` and confirm the build succeeds.
2. Run `./gradlew :app:testDebugUnitTest` and confirm all tests pass.
3. Install the debug APK on a device or emulator.
4. Open Settings from a cold start. Observe that the screen renders without the ~21 Flow collectors visible in the debug recomposition logs.
5. Toggle a switch, move the slider, and change a list preference. Observe that only the affected row recomposes, not the entire screen.
6. Verify the debug logs no longer show `SettingsRecompose` spam after the diagnostic logging is removed.

## Idempotence and Recovery

- Each milestone is independently verifiable. If a native row composable does not behave correctly, you can revert only that composable's code without affecting the others.
- The `compose-preference` dependency can be restored by reverting `gradle/libs.versions.toml` and `app/build.gradle.kts` if the migration proves riskier than expected.
- The debug recomposition logging is guarded by `BuildConfig.DEBUG` and can be removed at any time without changing behavior.

## Artifacts and Notes

Key diagnostics collected during research:

- `gradle/libs.versions.toml` current dependency:
  ```toml
  composePreference = "2.2.0"
  compose-preference = { module = "me.zhanghai.compose.preference:preference", version.ref = "composePreference" }
  ```

- Decompiled `PreferenceStateKt.class` from the 1.1.1 AAR shows each `rememberPreferenceState` creates a `Flow.map` operator and collects it via `collectAsStateWithLifecycle`.

- Debug recomposition logger pattern (to be removed after data collection):
  ```kotlin
  if (BuildConfig.DEBUG) {
      SideEffect {
          Log.d("SettingsRecompose", "switchPreference(${app.getString(titleId)}) recomposed")
      }
  }
  ```

## Interfaces and Dependencies

- Remove: `me.zhanghai.compose.preference` library
- Keep: `androidx.compose.material3` (`ListItem`, `Switch`, `Slider`, `DropdownMenu`, `AlertDialog`)
- Keep: `androidx.compose.material` icons
- Keep: `com.aistra.hail.app.HailData` for SharedPreferences access
- New internal composables (all in `SettingsFragment.kt` or `SettingsRows.kt`):
  - `@Composable SettingsSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, icon: ImageVector?, enabled: Boolean)`
  - `@Composable SettingsSlider(title: String, value: Float, onValueChange: (Float) -> Unit, valueRange: ClosedFloatingPointRange<Float>, valueSteps: Int, valueText: String, icon: ImageVector?, enabled: Boolean)`
  - `@Composable SettingsList(title: String, value: String, onValueChange: (String) -> Unit, values: List<String>, entries: List<String>, icon: ImageVector?, enabled: Boolean, dropdown: Boolean = true)`
  - `@Composable SettingsClickable(title: String, onClick: () -> Unit, icon: ImageVector?, enabled: Boolean)`
