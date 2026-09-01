# Material 3 Expressive UI — Comparative Analysis & Recommendations

**Analysis Date:** 2026-09-01
**Reference Repos:** InstallerX-Revived, AZenith, Keyguard
**Google Spec:** Material Design 3 Expressive (material3 1.5.0-alpha27)

## Executive Summary

Three production Android apps implementing Material 3 Expressive were analyzed against Google's official guidelines. All three repos successfully implement the expressive visual language, but all three overengineer connected shapes with custom layouts instead of using Google's built-in `ListItemDefaults.segmentedShapes()` API. Only Keyguard uses `MaterialTheme.motionScheme` for animations. Only AZenith uses haze blur (not a Google pattern). None of the repos fully adhere to Google's corner radius spec.

## Comparison Matrix

| Pattern | InstallerX-Revived | AZenith | Keyguard | Google Spec |
|---------|-------------------|---------|----------|-------------|
| **Theme** | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` | `MaterialExpressiveTheme` |
| **Motion scheme** | `MotionScheme.expressive()` | `MotionScheme.expressive()` | (default) | `MotionScheme.expressive()` |
| **Light color** | `dynamicColorScheme` | `expressiveLightColorScheme()` | (custom) | `expressiveLightColorScheme()` |
| **Dark color** | `dynamicColorScheme` | `darkColorScheme()` | (custom) | `darkColorScheme()` |
| **Connected shapes** | Custom `SegmentedColumn` | `ExpressiveList` | `surfaceShape()` | `ListItemDefaults.segmentedShapes()` |
| **Inner corner** | 5dp | 4dp | 4dp | **8dp** |
| **Outer corner** | 16dp | 26dp | 16dp (large) | Size-dependent |
| **Top bar** | `LargeFlexibleTopAppBar` | `LargeFlexibleTopAppBar` | `ScaffoldLazyColumn` | `LargeFlexibleTopAppBar` |
| **Motion** | Custom springs | Custom springs | `motionScheme` | `motionScheme` |
| **Emphasized type** | ✅ 7 files | ✅ 5 files | ❌ | ✅ Recommended |
| **Blur** | ❌ | `haze` library | ❌ | ❌ Not in spec |
| **Wavy progress** | ❌ | ✅ | ❌ | ✅ Available |
| **Runtime toggle** | ❌ | ❌ | `LocalExpressive` | ❌ Not in spec |
| **Custom dep** | `materialkolor` | `materialkolor`, `haze` | None | None needed |

## What Each Repo Does Best

### InstallerX-Revived
- **Clean component hierarchy:** `BaseWidget` → specialized widgets (`SwitchWidget`, `RadioButtonWidget`, etc.)
- **Expressive navigation:** `ShortNavigationBar` / `WideNavigationRail` for adaptive navigation
- **Menu shapes:** Correct use of `MenuDefaults.groupShape()` / `itemShape()`

### AZenith
- **Compressive component library:** Every list item variant covered (switch, dropdown, radio, checkbox, slider)
- **Connected button groups:** Correct use of `ButtonGroupDefaults.connectedLeadingButtonShapes()`
- **Dialog system:** `RootDialogsProvider` composition local for root-level dialogs

### Keyguard
- **Motion scheme:** Only repo using `MaterialTheme.motionScheme` correctly
- **Component interface:** `SettingPaneComponents` interface for clean contracts
- **Icon shapes:** Correct use of `MaterialShapes.SoftBurst` / `.Square`
- **Minimalist approach:** Least custom code, most reliance on built-in APIs

## What Google Recommends (and Repos Deviate)

### 1. Connected Shapes

**Google's API:**
```kotlin
ListItemDefaults.segmentedShapes(index = i, count = n)
ListItemDefaults.segmentedColors()
ListItemDefaults.SegmentedGap  // 2dp
```

**What repos do:** Build custom layouts with manual corner radii.

**Impact:** 300-767 lines of custom code vs one API call. Visual difference is negligible. Custom implementations deviate from Google's 8dp inner corner spec.

**Recommendation for Hail:** Use `ListItemDefaults.segmentedShapes()`. Do not build custom layouts.

### 2. Motion

**Google's API:**
```kotlin
MaterialTheme.motionScheme.defaultEffectsSpec()
MaterialTheme.motionScheme.fastEffectsSpec()
MaterialTheme.motionScheme.fastSpatialSpec<T>()
```

**What repos do:** Hand-roll custom `spring(dampingRatio, stiffness)` values.

**Impact:** Custom springs work but risk inconsistency. Google's pre-tuned specs are research-backed.

**Recommendation for Hail:** Use `MaterialTheme.motionScheme` specs. Only Keyguard does this correctly.

### 3. Corner Radii

**Google's spec:** 8dp inner corners for connected shapes, with outer corners varying by size (4/8/8/16/20dp).

**What repos do:** InstallerX uses 5dp inner, AZenith uses 4dp inner, Keyguard uses 4dp inner.

**Impact:** Minor visual deviation, but using `segmentedShapes()` eliminates the issue entirely.

**Recommendation for Hail:** Use `segmentedShapes()` and let Google handle the values.

### 4. Emphasized Typography

**Google's API:**
```kotlin
MaterialTheme.typography.titleMediumEmphasized
MaterialTheme.typography.bodySmallEmphasized
MaterialTheme.typography.labelMediumEmphasized
```

**What repos do:** InstallerX and AZenith use emphasized variants. Keyguard does not.

**Impact:** Emphasized typography is a key part of the expressive identity. Keyguard misses this.

**Recommendation for Hail:** Use emphasized variants for section headers and key information.

## Patterns to Skip (Not in Google's Spec)

| Pattern | Used By | Recommendation |
|---------|---------|----------------|
| Haze blur / glassmorphism | AZenith | Skip — third-party dependency, not in M3 Expressive |
| Runtime expressive toggle | Keyguard | Skip — not a Google pattern, unnecessary complexity |
| Wavy progress indicators | AZenith | Skip — Hail has almost no loading states |
| `materialkolor` dynamic color | InstallerX, AZenith | Skip — Hail already has Monet on Android 12+ |
| Custom connected-shape Layout | All three | Skip — use `segmentedShapes()` instead |

## Recommended Implementation Order for Hail

### Phase 1: Connected Shapes (High Impact, Low Effort)
- Wrap settings groups with `ListItemDefaults.segmentedShapes()`
- Apply `ListItemDefaults.segmentedColors()` for surface container background
- Add `@OptIn(ExperimentalMaterial3ExpressiveApi::class)`

### Phase 2: Expressive Top Bar (High Impact, Low Effort)
- Replace static top bar with `LargeFlexibleTopAppBar`
- Add `TopAppBarDefaults.exitUntilCollapsedScrollBehavior()`
- Connect via `nestedScroll` modifier

### Phase 3: Motion System (Medium Impact, Low Effort)
- Replace custom `spring()` calls with `MaterialTheme.motionScheme` specs
- Update `animateDpAsState`, `animateFloatAsState`, `animateContentSize`

### Phase 4: Emphasized Typography (Medium Impact, Low Effort)
- Apply `titleMediumEmphasized` to section headers
- Use `primary` color for emphasis

### Phase 5: Polish (Low Impact, Medium Effort)
- `ContainedLoadingIndicator` where loading states exist
- `MaterialShapes` for decorative icon shapes
- `MenuDefaults.groupShape()` for dropdown menus

## Key Takeaways

1. **Google's built-in expressive APIs cover 90% of the visual effect.** The repos that build custom components are working harder than necessary.

2. **Connected shapes are the #1 expressive indicator.** All three repos implement them, all three overengineer them. Use `segmentedShapes()`.

3. **Motion consistency matters.** Only Keyguard uses `motionScheme`. Custom springs work but aren't research-backed.

4. **Emphasized typography is underused.** Keyguard doesn't use it at all. It's a free win for visual hierarchy.

5. **Skip non-Google patterns.** Haze blur, wavy indicators, and runtime toggles add complexity without spec backing.

6. **The minimalist approach (Keyguard) is closest to Google's intent.** Use built-in APIs, add custom code only where the API doesn't cover the use case.

## File Reference

- `case-study-installerx.md` — Full analysis of InstallerX-Revived
- `case-study-azenith.md` — Full analysis of AZenith
- `case-study-keyguard.md` — Full analysis of Keyguard
- `../settings-expressive-migration-plan.md` — Hail's implementation plan
