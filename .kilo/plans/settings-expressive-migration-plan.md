# Hail Settings: Material 3 Expressive UI Implementation Plan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

Hail's Settings screen has been migrated from `me.zhanghai.compose.preference` to native Compose composables, and the theme has been upgraded to `MaterialExpressiveTheme`. This plan covers the **gradual implementation of Material 3 Expressive UI patterns** — connected shapes, expressive top bars, motion, and typography — following Google's official M3 Expressive guidelines and informed by analysis of three reference repos (InstallerX-Revived, AZenith, Keyguard).

The user-facing outcome is a Settings screen that looks and feels like a modern Material 3 Expressive app: grouped list items with connected corners, a collapsible top app bar, spring-based motion, and emphasized typography — all using Google's built-in APIs with minimal custom code.

## Implementation Guidelines

These rules govern every change in this plan. They are derived from Google's official M3 Expressive guidelines and analysis of three production repos.

### 1. Use Built-In Expressive APIs, Not Custom Implementations

Google provides first-party APIs that cover 90% of expressive patterns. Building custom alternatives (as all three reference repos do) adds hundreds of lines of code for marginal visual difference.

**Use this:**
```kotlin
ListItemDefaults.segmentedShapes(index = i, count = items.size)
ListItemDefaults.segmentedColors()
ListItemDefaults.SegmentedGap
```

**Not this:**
- Custom `Layout` with dynamic corner radii (InstallerX: 325 lines)
- Manual `topShape`/`middleShape`/`bottomShape` definitions (AZenith: 767 lines)
- Custom `surfaceShape()` with `ShapeState` enum (Keyguard)

### 2. Follow Google's Corner Radius Spec

Google specifies **8dp inner corners** for connected shapes, with outer corners varying by size (4/8/8/16/20dp). The `segmentedShapes()` API handles this automatically.

Do NOT use custom values like 4dp inner (AZenith/Keyguard) or 5dp inner (InstallerX) — they deviate from spec.

### 3. Use `MaterialTheme.motionScheme` for Animations

Google provides pre-tuned spring specs via `MotionScheme.expressive()`:
```kotlin
MaterialTheme.motionScheme.defaultEffectsSpec()
MaterialTheme.motionScheme.fastEffectsSpec()
MaterialTheme.motionScheme.fastSpatialSpec()
```

Do NOT hand-roll custom `spring(dampingRatio, stiffness)` values (as InstallerX and AZenith do) — they risk feeling inconsistent.

### 4. Use `LargeFlexibleTopAppBar` for Expressive Top Bars

Google's expressive top bar API:
```kotlin
LargeFlexibleTopAppBar(
    title = { ... },
    scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
)
```

This is the standard expressive pattern. All three repos use it. Hail should too.

### 5. Apply Emphasized Typography Sparingly

Google's expressive type scale includes emphasized variants:
- `titleMediumEmphasized` — for key actions and status
- `bodySmallEmphasized` — for important supporting text
- `labelMediumEmphasized` — for badges and labels

Use for section headers and key information. Do NOT emphasize everything — emphasis works through contrast.

### 6. Skip Patterns Google Doesn't Recommend

These are used by reference repos but have no Google endorsement:
- Haze blur / glassmorphism (AZenith) — third-party dependency, not in M3 Expressive
- Runtime expressive toggle (Keyguard's `LocalExpressive`) — not a Google pattern
- Wavy progress indicators (AZenith) — Hail has almost no loading states
- `materialkolor` dynamic color (InstallerX/AZenith) — Hail already has Monet

### 7. Surface Container for Grouped Content

Use `MaterialTheme.colorScheme.surfaceContainer` as the background for grouped list items. This is Google's recommended color for visually containing related content. The `segmentedColors()` API applies this automatically.

## Progress

- [x] (2026-08-31) Research completed: identified `rememberPreferenceState()` Flow-per-preference overhead.
- [x] (2026-08-31) Library upgrade to `2.2.0` completed for 1.11.3 release.
- [x] (2026-09-01) Added writable setters to `HailData` for all 16 UI-modified preferences.
- [x] (2026-09-01) Created native Compose Settings row composables in `SettingsRows.kt`.
- [x] (2026-09-01) Migrated `SettingsFragment` off `me.zhanghai.compose.preference`.
- [x] (2026-09-01) Fixed runtime crashes and critical UI bugs.
- [x] (2026-09-01) Implemented 3-level navigation: Main Settings → Working Mode → Provider/Mode Selection.
- [x] (2026-09-01) Restored all missing settings.
- [x] (2026-09-01) Updated material3 to 1.5.0-alpha27 and minSdk to 24.
- [x] (2026-09-01) Migrated Theme.kt from `MaterialTheme` to `MaterialExpressiveTheme`.
- [x] (2026-09-01) Added `expressiveLightColorScheme()`, `MotionScheme.expressive()`, `expressiveShapes`, `AppTypography`.
- [x] (2026-09-01) Verified Kotlin compilation and unit tests pass.
- [x] (2026-09-01) Analyzed 3 reference repos (InstallerX-Revived, AZenith, Keyguard) for expressive patterns.
- [x] (2026-09-01) Compared repo patterns against Google M3 Expressive guidelines.
- [ ] (Pending) Milestone 7: Connected shapes for settings groups.
- [ ] (Pending) Milestone 8: `LargeFlexibleTopAppBar` with scroll collapse.
- [ ] (Pending) Milestone 9: Replace custom springs with `MaterialTheme.motionScheme`.
- [ ] (Pending) Milestone 10: Emphasized typography for section headers.
- [ ] (Pending) Milestone 11: `ContainedLoadingIndicator` where applicable.
- [ ] (Pending) Milestone 12: Device testing and verification.
- [ ] (Pending) Milestone 13: Migrate row composables from `ListItem` to `SegmentedListItem` for proper connected shapes.
- [ ] (Pending) Milestone 14: Fix Terminal visibility to include DHIZUKU provider.
- [ ] (Pending) Milestone 15: Clean up unused count variables and hardcoded values.

## Surprises & Discoveries

- Observation: All three reference repos build custom connected-shape systems instead of using `ListItemDefaults.segmentedShapes()`. This is unnecessary — Google's built-in API handles corner radii, colors, and gaps automatically.

- Observation: Only Keyguard uses `MaterialTheme.motionScheme` for animations. InstallerX and AZenith hand-roll custom spring specs, which risks inconsistency.

- Observation: Google's expressive spec calls for 8dp inner corners on connected shapes. No reference repo matches this exactly (InstallerX: 5dp, AZenith: 4dp, Keyguard: 4dp).

- Observation: `expressiveDarkColorScheme()` does not exist in the Material 3 library. Google's official sample pairs `expressiveLightColorScheme()` with `darkColorScheme()` for dark mode.

- Observation: Material 3 Expressive APIs require material3 1.5.0-alpha27+ and minSdk 24.

- Observation: APK assembly crashes the Gradle daemon due to memory constraints in this environment. Kotlin compilation and unit tests pass.

- Observation: Reading from `HailData.xxx` directly in composables does NOT trigger recomposition. All switches/sliders/lists must use local `mutableStateOf` state holders.

- Observation: `ListItemDefaults.segmentedShapes()` requires `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

- **Discovery (2026-09-01):** `ListItemDefaults.segmentedShapes()` returns `ListItemShapes`, NOT `Shape`. It only works with `SegmentedListItem`, NOT `ListItem`. The initial implementation incorrectly used `ListItem` with shape/colors parameters that don't exist in material3 1.5.0-alpha27. This caused compilation errors and the connected shapes were never visually applied.

- **Discovery (2026-09-01):** `SegmentedListItem` has 4 overloads: non-interactive, click (onClick), single-selection (selected + onClick), and multi-selection (checked + onCheckedChange). All accept `shapes: ListItemShapes` and `colors: ListItemColors` parameters.

- **Discovery (2026-09-01):** `LargeFlexibleTopAppBar` is correctly implemented per Google's official sample. The `nestedScroll` on `Scaffold` properly receives scroll events from the inner `Column` with `verticalScroll`.

- **Bug found (2026-09-01):** Terminal visibility condition in the settings list missing `HailData.DHIZUKU` — Dhizuku users couldn't access the terminal from settings.

## Decision Log

- Decision: Use `ListItemDefaults.segmentedShapes()` for connected shapes instead of custom Layout.
  Rationale: Google's built-in API provides spec-correct corner radii (8dp inner), proper colors, and 2dp gaps with one function call. Reference repos build 300-767 line custom components for the same effect.
  Date/Author: 2026-09-01

- Decision: Use `MaterialTheme.motionScheme` specs instead of custom spring definitions.
  Rationale: Google provides pre-tuned spring specs via `MotionScheme.expressive()`. Custom springs risk feeling inconsistent across the app.
  Date/Author: 2026-09-01

- Decision: Use `LargeFlexibleTopAppBar` with `exitUntilCollapsedScrollBehavior` for the expressive top bar.
  Rationale: This is Google's standard expressive top bar API. All three reference repos use it. Provides collapsible header with minimal code.
  Date/Author: 2026-09-01

- Decision: Skip haze blur, wavy progress indicators, and runtime expressive toggle.
  Rationale: These patterns have no Google endorsement in M3 Expressive. They add complexity and dependencies without aligning with the official design system.
  Date/Author: 2026-09-01

- Decision: Continue using `SharedPreferences` via `HailData` rather than migrating to DataStore.
  Rationale: Settings values like theme and working mode are read synchronously before the first frame, where SharedPreferences' synchronous API is an advantage over DataStore's async reads.
  Date/Author: 2026-08-31

- Decision: Keep preference state in `SettingsFragment` using `mutableStateOf` + `SharedPreferences` directly.
  Rationale: Settings is a simple read/write surface with no async loading or cross-fragment coordination. A ViewModel would add indirection without benefit.
  Date/Author: 2026-08-31

- Decision: Migrate all row composables from `ListItem` to `SegmentedListItem` for proper connected shapes.
  Rationale: `ListItemDefaults.segmentedShapes()` returns `ListItemShapes` which only works with `SegmentedListItem`. Using it with `ListItem` has no visual effect. This is Google's official API for expressive connected groups.
  Date/Author: 2026-09-01

- Decision: Add `HailData.DHIZUKU` to Terminal visibility condition.
  Rationale: Dhizuku is a root-like provider that should grant terminal access, consistent with `onWorkingModeChange()` which handles DHIZUKU as a first-class provider.
  Date/Author: 2026-09-01

- Decision: Keep Security section as a single standalone item (not merged).
  Rationale: A single-item group doesn't benefit from segmented styling. Keeping it separate maintains clear visual hierarchy. `SegmentedListItem` with count=1 renders identically to a plain item.
  Date/Author: 2026-09-01

## Plan of Work

### Milestone 7: Connected Shapes for Settings Groups

Replace the current `Column`-based settings list with connected-shape groups using Google's built-in expressive APIs.

**What changes:**
- Wrap each settings group in a `Column` with `Arrangement.spacedBy(ListItemDefaults.SegmentedGap)`
- Use `SegmentedListItem` instead of `ListItem` for all row composables
- Apply `ListItemDefaults.segmentedShapes(index, count)` to each item
- Apply `ListItemDefaults.segmentedColors()` for proper surface container background
- Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)` to the file

**Important:** `segmentedShapes()` returns `ListItemShapes` which only works with `SegmentedListItem`. Using `ListItem` has no visual effect. This is why Milestone 13 (migration) is required.

**Files to modify:**
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`

**Before (current):**
```kotlin
Column(modifier = Modifier.verticalScroll(scrollState)) {
    // Flat list of items with no visual grouping
    SettingsSwitch(...)
    SettingsSwitch(...)
    SettingsClickable(...)
}
```

**After (expressive):**
```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
Column(modifier = Modifier.verticalScroll(scrollState)) {
    // Group 1: Appearance
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        val appearanceItems = listOf("Theme", "Icon Pack", "Font Size")
        appearanceItems.forEachIndexed { index, _ ->
            SettingsSwitch(
                shapes = ListItemDefaults.segmentedShapes(index, appearanceItems.size),
                colors = ListItemDefaults.segmentedColors(),
                ...
            )
        }
    }
    SettingsHorizontalDivider()
    // Group 2: Behavior
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        ...
    }
}
```

**Observable outcome:** Settings items visually group into cards with connected corners, matching Google's expressive spec.

### Milestone 8: `LargeFlexibleTopAppBar` with Scroll Collapse

Replace the current static top bar with Google's expressive collapsible top bar.

**What changes:**
- Add `LargeFlexibleTopAppBar` with `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`
- Connect scroll behavior to the `Column` via `nestedScroll`
- Move title and actions from current top bar to the new API

**Files to modify:**
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`

**Key code:**
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
        LargeFlexibleTopAppBar(
            title = { Text("Settings") },
            navigationIcon = { ExpressiveBackButton { navigator.pop() } },
            actions = { /* About, Search */ },
            scrollBehavior = scrollBehavior,
        )
    }
) { innerPadding ->
    Column(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(innerPadding)
    ) { ... }
}
```

**Observable outcome:** Top bar collapses on scroll, expanding back when scrolling up. Matches expressive pattern from all three reference repos.

### Milestone 9: Replace Custom Springs with `motionScheme`

Replace any hand-rolled `spring()` calls with Google's pre-tuned motion specs.

**What changes:**
- Replace `spring(stiffness = Spring.StiffnessMediumLow)` with `MaterialTheme.motionScheme.fastSpatialSpec()`
- Replace `animateDpAsState` custom springs with `motionScheme.defaultEffectsSpec()`

**Files to modify:**
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt`
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt`

**Observable outcome:** Animations use Google's pre-tuned expressive spring physics. Consistent feel across all transitions.

### Milestone 10: Emphasized Typography for Section Headers

Apply expressive typography to section headers using emphasized variants.

**What changes:**
- Replace `titleSmall` with `titleMediumEmphasized` for section headers
- Use `primary` color for emphasis (already in `AppTypography`)

**Files to modify:**
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` (`settingsSectionHeader` composable)

**Observable outcome:** Section headers have bolder weight, creating clearer visual hierarchy.

### Milestone 11: `ContainedLoadingIndicator`

Replace any `CircularProgressIndicator` with the expressive `ContainedLoadingIndicator` where loading states exist.

**What changes:**
- Replace `CircularProgressIndicator` with `ContainedLoadingIndicator` in any loading states
- Wrap in `Surface` with `MaterialTheme.shapes.extraLarge` if needed

**Files to modify:**
- Any file with loading states (currently minimal in Hail settings)

**Observable outcome:** Loading indicators match expressive visual style.

### Milestone 12: Device Testing and Verification

- Verify cold-start performance improvement on device
- Verify toggle responsiveness and slider functionality
- Verify dialog positioning on actual device
- Verify connected shapes render correctly
- Verify top bar collapse/expand behavior
- Verify animations feel natural

**Observable outcome:** All expressive patterns work correctly on device.

## Concrete Steps

Working directory: `/workspaces/Hail`

**Step 1: Add writable setters to HailData (COMPLETED)**

**Step 2: Create native Settings row composables (COMPLETED)**

**Step 3: Migrate SettingsFragment (COMPLETED)**

**Step 4: Implement 3-level navigation (COMPLETED)**

**Step 5: Fix runtime crashes (COMPLETED)**

**Step 6: Remove library dependency (COMPLETED)**

**Step 7: Migrate to Material 3 Expressive theme (COMPLETED)**

**Step 8: Implement connected shapes (PENDING)**
```kotlin
// In SettingsFragment.kt, wrap each group:
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
fun <T> ExpressiveSettingsGroup(items: List<T>, content: @Composable (Int, T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        items.forEachIndexed { index, item ->
            content(index, item)
        }
    }
}

// Usage:
ExpressiveSettingsGroup(items = listOf(
    SwitchItem("Theme", HailData.appTheme, { HailData.appTheme = it }),
    SwitchItem("Icon Pack", HailData.iconPack, { HailData.iconPack = it }),
    SwitchItem("Font Size", HailData.homeFontSize, { HailData.homeFontSize = it }),
)) { index, item ->
    SettingsSwitch(
        ...,
        shapes = ListItemDefaults.segmentedShapes(index, 3),
        colors = ListItemDefaults.segmentedColors(),
    )
}
```

**Step 9: Add LargeFlexibleTopAppBar (PENDING)**
```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
        LargeFlexibleTopAppBar(
            title = { Text(stringResource(R.string.settings)) },
            navigationIcon = {
                IconButton(onClick = { /* back */ }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { /* about */ }) {
                    Icon(Icons.Filled.Info, contentDescription = "About")
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }
) { innerPadding ->
    SettingsContent(innerPadding)
}
```

**Step 10: Replace custom springs with motionScheme (PENDING)**
```kotlin
// Before:
animateDpAsState(targetValue = ..., spring(stiffness = Spring.StiffnessMediumLow))

// After:
animateDpAsState(targetValue = ..., animationSpec = MaterialTheme.motionScheme.fastSpatialSpec())
```

**Step 11: Apply emphasized typography (PENDING)**
```kotlin
// In settingsSectionHeader:
Text(
    text = title,
    style = MaterialTheme.typography.titleMediumEmphasized,
    color = MaterialTheme.colorScheme.primary,
)
```

**Step 12: Build and test**
```bash
./gradlew :app:compileDebugKotlin :app:processDebugResources :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

## Validation and Acceptance

1. ✅ Run `./gradlew :app:compileDebugKotlin` and confirm the build succeeds.
2. ✅ Run `./gradlew :app:testDebugUnitTest` and confirm all tests pass.
3. ⏳ Verify connected shapes render with correct corner radii (8dp inner, spec outer).
4. ⏳ Verify `LargeFlexibleTopAppBar` collapses on scroll and expands on scroll-up.
5. ⏳ Verify animations use `motionScheme` specs (no custom springs).
6. ⏳ Verify section headers use emphasized typography.
7. ⏳ Install the debug APK on a device or emulator and verify all patterns work.

## Idempotence and Recovery

- Each milestone is independently verifiable. Connected shapes, top bar, motion, and typography are separate changes.
- If `segmentedShapes()` causes issues, revert to the previous flat list — no other code depends on it.
- If `LargeFlexibleTopAppBar` doesn't work with the current scroll structure, revert to the previous static top bar.
- All changes are confined to `SettingsFragment.kt`, `SettingsRows.kt`, and theme files.

## Artifacts and Notes

### Reference Repo Analysis

| Pattern | InstallerX-Revived | AZenith | Keyguard | Google Spec |
|---------|-------------------|---------|----------|-------------|
| Theme | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` |
| Connected shapes | Custom `SegmentedColumn` (325 lines) | `ExpressiveList` (767 lines) | `surfaceShape()` | `ListItemDefaults.segmentedShapes()` |
| Inner corner | 5dp | 4dp | 4dp | **8dp** |
| Top bar | `LargeFlexibleTopAppBar` | `LargeFlexibleTopAppBar` | `ScaffoldLazyColumn` | `LargeFlexibleTopAppBar` |
| Motion | Custom springs | Custom springs | `motionScheme` | `motionScheme` |
| Emphasized type | ✅ | ✅ | ❌ | ✅ |
| Blur | ❌ | `haze` library | ❌ | ❌ |

### Key Material 3 Expressive APIs

```kotlin
// Theme
MaterialExpressiveTheme(
    colorScheme = expressiveLightColorScheme(),
    motionScheme = MotionScheme.expressive(),
    shapes = expressiveShapes,
    typography = AppTypography,
    content = { ... }
)

// Connected shapes
ListItemDefaults.segmentedShapes(index = i, count = n)
ListItemDefaults.segmentedColors()
ListItemDefaults.SegmentedGap  // 2dp

// Top bar
LargeFlexibleTopAppBar(
    title = { ... },
    scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
)

// Motion
MaterialTheme.motionScheme.defaultEffectsSpec()
MaterialTheme.motionScheme.fastEffectsSpec()
MaterialTheme.motionScheme.fastSpatialSpec<Float>()
MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()

// Typography
MaterialTheme.typography.titleMediumEmphasized
MaterialTheme.typography.bodySmallEmphasized
MaterialTheme.typography.labelMediumEmphasized

// Loading
ContainedLoadingIndicator()
```

### Key Files

- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — main settings screen
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsRows.kt` — row composables
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — data layer
- `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` — expressive theme
- `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt` — expressive typography
- `app/src/main/kotlin/com/aistra/hail/ui/theme/Shapes.kt` — expressive shapes
