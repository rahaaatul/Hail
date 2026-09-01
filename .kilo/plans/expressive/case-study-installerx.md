# Case Study: InstallerX-Revived — Material 3 Expressive UI Analysis

**Repository:** [github.com/InstallerX-Revived/InstallerX-Revived](https://github.com/InstallerX-Revived/InstallerX-Revived)
**M3 Version:** 1.5.0-alpha27
**Analysis Date:** 2026-09-01
**Clone Location:** `/tmp/installerx`

## Overview

InstallerX-Revived is an Android app installer with a comprehensive Material 3 Expressive implementation. It uses `MaterialExpressiveTheme`, `MotionScheme.expressive()`, and a custom `SegmentedColumn` layout for connected shapes. It also integrates `materialkolor` for dynamic color generation with expressive palette styles.

## Theme Setup

**File:** `app/src/main/java/com/rosan/installer/ui/theme/InstallerTheme.kt`

The theme wrapper uses `MaterialExpressiveTheme` with `MotionScheme.expressive()` and a custom typography:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun InstallerMaterialExpressiveTheme(
    colorScheme: ColorScheme,
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = InstallerTypography,
        content = content
    )
}
```

Color scheme is generated via `materialkolor`'s `dynamicColorScheme()` with `ColorSpec.SpecVersion.SPEC_2025` and `PaletteStyle.Expressive`. A custom `ColorScheme.animateAsState()` extension animates all 40+ color fields individually using `spring()` for smooth theme transitions.

## Connected Shapes Implementation

**File:** `app/src/main/java/com/rosan/installer/ui/page/main/widget/setting/SegmentedColumn.kt` (325 lines)

This is the most significant custom component — a `Layout` that dynamically calculates corner radii for each child:

- **Outer corners:** 16dp (`CornerRadius`)
- **Inner corners:** 5dp (`ConnectionRadius`)
- **Animation:** `animateDpAsState` with bouncy spring (damping 0.5, stiffness 800) on Android 13+

The layout uses `LocalSegmentedItemShape` composition local to pass computed shapes down to child widgets. Each item is wrapped in a `Box` with a custom `Shape` outline using `graphicsLayer` for clip + alpha animations.

**Verdict:** Overengineered. Google's `ListItemDefaults.segmentedShapes()` provides the same visual effect with one function call. The custom layout adds 325 lines of code for a 5dp deviation from Google's 8dp inner corner spec.

## Component Library

All under `app/src/main/java/com/rosan/installer/ui/page/main/widget/`:

| Component | Pattern |
|-----------|---------|
| `BaseWidget` | Wraps `ListItem` with `ListItemDefaults.colors/shapes`, handles click/haptic/alpha/disabled |
| `SwitchWidget` | `BaseWidget` + `Switch` with check/close thumb icons |
| `NavigationItemWidget` | `BaseWidget` + trailing arrow icon |
| `RadioButtonWidget` | `BaseWidget` + `RadioButton` with shared `interactionSource` |
| `DropDownMenuWidget` | `BaseWidget` + `GroupedDropdownMenuPopup` |
| `ExpressiveBackButton` | `IconButton` with `CircleShape`, semi-transparent container |
| `LabelWidget` | Section label with `titleSmall` + `primary` color |

**Verdict:** Well-structured component hierarchy. The `BaseWidget` pattern is clean and reusable. However, most of these could be replaced by Google's built-in expressive list items with less code.

## Top App Bar

Uses `LargeFlexibleTopAppBar` with `TopAppBarDefaults.exitUntilCollapsedScrollBehavior` across all 15+ settings pages. This matches Google's expressive spec exactly.

## Motion and Animation

- **Predictive back navigation:** Custom `NavTransitionGeometry` with scale/translate animations
- **Damped drag animation:** Custom spring specs for value, velocity, press progress, and scale
- **Theme transitions:** `animateColorAsState` with `spring()` on all 40+ color fields

**Verdict:** Custom springs deviate from Google's `MaterialTheme.motionScheme` specs. The damped drag animation is well-implemented but hand-tuned.

## Emphasized Typography

Used across 7 files:
- `titleMediumEmphasized` — status card titles
- `bodySmallEmphasized` — status card descriptions
- `bodyMediumEmphasized` — tip cards
- `labelMediumEmphasized` — badges
- `headlineMediumEmphasized` — dialog titles

**Verdict:** Correct usage of Google's expressive type scale.

## Expressive APIs Used

| API | Status |
|-----|--------|
| `MaterialExpressiveTheme` | ✅ Correct |
| `MotionScheme.expressive()` | ✅ Correct |
| `LargeFlexibleTopAppBar` | ✅ Correct |
| `ListItemDefaults.shapes` | ✅ Correct |
| `ListItemDefaults.segmentedShapes` | ❌ Not used (custom implementation) |
| `ListItemDefaults.segmentedColors` | ❌ Not used (custom implementation) |
| `ContainedLoadingIndicator` | ✅ Correct |
| `ShortNavigationBar` / `WideNavigationRail` | ✅ Correct |
| `MenuDefaults.groupShape` / `itemShape` | ✅ Correct |
| `MaterialTheme.motionScheme` | ❌ Not used (custom springs) |

## Key Takeaways for Hail

**Adopt:**
- `BaseWidget` pattern for consistent row styling
- `ExpressiveBackButton` with `CircleShape` + semi-transparent container
- Emphasized typography for section headers and key information
- `MenuDefaults.groupShape()` for dropdown menus

**Skip:**
- Custom `SegmentedColumn` — use `ListItemDefaults.segmentedShapes()` instead
- Custom spring animations — use `MaterialTheme.motionScheme` instead
- `materialkolor` dependency — Hail already has Monet
- Predictive back navigation — overkill for Hail's settings flow

**Lessons:**
1. Building custom connected-shape layouts is unnecessary when Google provides the API
2. Custom springs work but risk inconsistency — prefer `motionScheme` specs
3. A clean component hierarchy (`BaseWidget` → specialized widgets) is valuable for maintainability
