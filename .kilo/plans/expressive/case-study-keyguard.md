# Case Study: Keyguard — Material 3 Expressive UI Analysis

**Repository:** [github.com/AChep/Keyguard](https://github.com/AChep/Keyguard)
**M3 Version:** (uses `MaterialExpressiveTheme`)
**Analysis Date:** 2026-09-01
**Clone Location:** `/tmp/keyguard`

## Overview

Keyguard is a password manager with a unique approach to expressive UI: it implements a runtime toggle between expressive and non-expressive styles via `LocalExpressive` and `GlobalExpressive` composition locals. It uses `MaterialExpressiveTheme`, connected shapes via a `surfaceShape()` function, and `MaterialTheme.motionScheme` for animations. It represents the "minimalist" approach — using Google's built-in APIs wherever possible.

## Theme Setup

**File:** `common/src/commonMain/kotlin/com/artemchep/keyguard/ui/theme/Theme.kt`

Uses `MaterialExpressiveTheme` with a custom typography that supports font families:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun KeyguardTheme(
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = KeyguardTypography,
        content = content
    )
}
```

A `GlobalExpressive` composition local provides the global default (true), while `LocalExpressive` allows per-screen overrides.

## Expressive Toggle System

**File:** `common/src/commonMain/kotlin/com/artemchep/keyguard/ui/theme/Expressive.kt`

```kotlin
val LocalExpressive = staticCompositionLocalOf { false }
val GlobalExpressive = staticCompositionLocalOf { true }
```

This is Keyguard's own pattern — not a Google recommendation. It allows runtime switching between expressive and non-expressive styles.

**Verdict:** Useful for a password manager that needs to support older devices or user preference, but not a Google-recommended pattern. Hail doesn't need this complexity.

## Connected Shapes Implementation

**File:** `common/src/commonMain/kotlin/com/artemchep/keyguard/feature/home/vault/component/VaultListItem.kt`

The `surfaceShape()` function creates connected shapes:

```kotlin
fun surfaceShape(
    shapeState: ShapeState,
    innerCornerSize: CornerSize = CornerSize(4.dp)
): Shape {
    val baseShape = MaterialTheme.shapes.large
    return when (shapeState) {
        ShapeState.START -> RoundedCornerShape(
            topStart = baseShape.topStart,
            topEnd = baseShape.topEnd,
            bottomStart = innerCornerSize,
            bottomEnd = innerCornerSize
        )
        ShapeState.CENTER -> RoundedCornerShape(innerCornerSize)
        ShapeState.END -> RoundedCornerShape(
            topStart = innerCornerSize,
            topEnd = innerCornerSize,
            bottomStart = baseShape.bottomStart,
            bottomEnd = baseShape.bottomEnd
        )
        ShapeState.ALL -> baseShape
    }
}
```

Uses `MaterialTheme.shapes.large` as base with 4dp inner corners.

**Verdict:** Simpler than InstallerX and AZenith (no custom Layout), but still deviates from Google's 8dp inner corner spec. Google's `ListItemDefaults.segmentedShapes()` would replace this.

## Component Library

| Component | Description |
|-----------|-------------|
| `FlatItemSimpleExpressive` | Main list item with connected shape support |
| `FlatDropdownSimpleExpressive` | Dropdown item with connected shape |
| `FlatItemLayoutExpressive` | Base layout for expressive items |
| `KgSwitch` | Switch setting component |
| `KgAction` | Action setting component |
| `KgPicker` | Picker setting component |

The `SettingPaneComponents` interface provides a clean contract:

```kotlin
interface SettingPaneComponents {
    fun KgAction(...)
    fun KgPicker(...)
    fun KgSwitch(...)
    fun KgBlock(...)
}
```

**Verdict:** Clean, minimal interface. The `SettingPaneComponents` pattern is well-designed for testability and flexibility. Hail could adopt a similar (simplified) interface.

## Top App Bar

Uses `ScaffoldLazyColumn(expressive = true)` with a `LargeToolbar` and scroll behavior. This is functionally equivalent to Google's `LargeFlexibleTopAppBar` with `exitUntilCollapsedScrollBehavior`.

**Verdict:** Correct approach, matches expressive spec.

## Motion and Animation

Uses `MaterialTheme.motionScheme` throughout:

```kotlin
MaterialTheme.motionScheme.defaultEffectsSpec()
MaterialTheme.motionScheme.fastEffectsSpec()
MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
MaterialTheme.motionScheme.fastSpatialSpec<Float>()
```

Applied in `Scaffold.kt`, `SettingListScreen.kt`, `FavouriteToggleButton.kt`, and `AnimatedBadge.kt`.

**Verdict:** This is the correct approach. Keyguard is the only repo that uses Google's pre-tuned motion specs instead of custom springs.

## Icon Shapes

**File:** `common/src/commonMain/kotlin/com/artemchep/keyguard/feature/home/settings/SettingListScreen.kt`

Uses `MaterialShapes` for decorative icon shapes:

```kotlin
val premiumShape = MaterialShapes.SoftBurst
val optionsShape = MaterialShapes.Square
```

**Verdict:** Correct usage of Google's expressive shape library. These 35+ shapes are part of the M3 Expressive API and can add visual interest to icons and avatars.

## Emphasized Typography

Not used. Keyguard relies on standard typography with `FontWeight.Bold` for emphasis instead of the expressive emphasized variants.

**Verdict:** Missed opportunity. The emphasized type scale is a key part of M3 Expressive.

## Expressive APIs Used

| API | Status |
|-----|--------|
| `MaterialExpressiveTheme` | ✅ Correct |
| `MaterialTheme.motionScheme` | ✅ Correct (only repo that uses it) |
| `MaterialShapes.SoftBurst` / `.Square` | ✅ Correct |
| `LargeFlexibleTopAppBar` (equivalent) | ✅ Correct |
| `ListItemDefaults.segmentedShapes` | ❌ Not used (custom implementation) |
| Emphasized typography | ❌ Not used |
| `LocalExpressive` toggle | ⚠️ Custom pattern, not Google spec |

## Key Takeaways for Hail

**Adopt:**
- `MaterialTheme.motionScheme` for all animations (Keyguard does this correctly)
- `MaterialShapes` for decorative icon shapes where appropriate
- `SettingPaneComponents` interface pattern for clean component contracts
- `ScaffoldLazyColumn(expressive = true)` pattern for expressive scaffolds

**Skip:**
- `LocalExpressive` / `GlobalExpressive` toggle — not a Google pattern
- Custom `surfaceShape()` — use `ListItemDefaults.segmentedShapes()` instead
- 4dp inner corners — use Google's 8dp spec

**Lessons:**
1. Using `MaterialTheme.motionScheme` is the right approach — it's what Google provides
2. A clean interface for setting components (`SettingPaneComponents`) improves testability
3. The expressive toggle is clever but unnecessary for most apps
4. Keyguard proves that expressive UI doesn't require massive custom component libraries
