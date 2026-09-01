# Case Study: AZenith — Material 3 Expressive UI Analysis

**Repository:** [github.com/AZenith-Team/AZenith](https://github.com/AZenith-Team/AZenith)
**M3 Version:** 1.5.0-alpha23
**Analysis Date:** 2026-09-01
**Clone Location:** `/tmp/azenith`

## Overview

AZenith is a system utility app with the most comprehensive expressive component library found in the analysis. It features a 767-line `ExpressiveListComponent.kt`, haze blur glassmorphism, wavy progress indicators, and a full custom dialog system. It represents the "maximalist" approach to M3 Expressive.

## Theme Setup

**File:** `manager/app/src/main/java/zx/azenith/ui/theme/Theme.kt`

Uses `MaterialExpressiveTheme` with `MotionScheme.expressive()`:

```kotlin
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AZenithTheme(
    content: @Composable () -> Unit
) {
    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = AppTypography,
        content = content
    )
}
```

Color scheme uses `expressiveLightColorScheme()` as fallback for pre-Android-12 devices, with `materialkolor`'s `rememberDynamicColorScheme` for dynamic color. A custom `animateColorSchemeAsState` animates all 22 color fields with `tween(400)`.

**Shapes:** Custom `ExpressiveShapes` with generous corners:
- `extraSmall = 8.dp`, `small = 12.dp`, `medium = 20.dp`, `large = 28.dp`, `extraLarge = 32.dp`

## Connected Shapes Implementation

**File:** `manager/app/src/main/java/zx/azenith/ui/component/ExpressiveListComponent.kt` (767 lines)

The core expressive component library. Defines manual connected shape definitions:

```kotlin
val largeCorner = 26.dp
val smallCorner = 4.dp

val topShape = RoundedCornerShape(
    topStart = largeCorner, topEnd = largeCorner,
    bottomStart = smallCorner, bottomEnd = smallCorner
)
val middleShape = RoundedCornerShape(smallCorner)  // 4dp all corners
val bottomShape = RoundedCornerShape(
    topStart = smallCorner, topEnd = smallCorner,
    bottomStart = largeCorner, bottomEnd = largeCorner
)
val singleShape = RoundedCornerShape(largeCorner)
```

**Verdict:** Significantly deviates from Google's spec. Uses 26dp outer corners (Google: 16dp) and 4dp inner corners (Google: 8dp). The 767-line file is the largest custom component in any of the three repos. Google's `ListItemDefaults.segmentedShapes()` would replace all of this.

## Component Library

The `ExpressiveListComponent.kt` file defines:

| Component | Description |
|-----------|-------------|
| `ExpressiveList` | Static list container with connected shapes |
| `ExpressiveLazyList<T>` | Lazy variant with `animateItem` placement |
| `ExpressiveListItem` | Base row with headline/supporting/leading/trailing slots |
| `ExpressiveListItemHighlight` | Variant with explicit container color |
| `ExpressiveInfoCard` | Info card with full-width text |
| `ExpressiveSwitchItem` | Switch row with thumb icons |
| `ExpressiveDropdownItem` | Dropdown with embedded `DropdownMenu` |
| `ExpressiveRadioItem` | Radio button row |
| `ExpressiveCheckboxItem` | Checkbox row |
| `ExpressiveColumn` | Non-lazy variant of `ExpressiveList` |
| `LeadingIcon` | Icon in 36x36 rounded box (12dp corners) |
| `ExpressiveSliderItem` | Slider with gradient progress bar |

**Verdict:** Comprehensive but overengineered. The `ExpressiveListItem` base pattern is clean, but the file is 767 lines when Google's built-in APIs would handle most of this. The `ExpressiveSliderItem` with gradient progress bar is a nice touch but non-standard.

## Top App Bar

Uses `LargeFlexibleTopAppBar` with `exitUntilCollapsedScrollBehavior` inside a transparent gradient `Box`:

```kotlin
Box(modifier = Modifier.background(Brush.verticalGradient(...))) {
    LargeFlexibleTopAppBar(
        title = { ... },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = colorScheme.surface,
        )
    )
}
```

**Verdict:** Correct usage of Google's expressive top bar API. The gradient scrim is a nice polish detail.

## Motion and Animation

- **Content size:** `Spring.DampingRatioLowBouncy` + `Spring.StiffnessLow` for `animateContentSize`
- **Expand/collapse:** `expandVertically` + `fadeIn` / `shrinkVertically` + `fadeOut`
- **Value transitions:** `AnimatedContent` with scale/fade
- **Progress indicators:** `CircularWavyProgressIndicator` and `LinearWavyProgressIndicator`
- **Sliders:** `animateFloatAsState` with spring

**Verdict:** Uses custom springs instead of `MaterialTheme.motionScheme`. The wavy progress indicators are expressive APIs but Hail has almost no loading states where they'd be relevant.

## Glassmorphism / Blur

**File:** `manager/app/src/main/java/zx/azenith/ui/component/DialogComponent.kt`

Uses the `haze` library for blur effects:
```kotlin
hazeEffect(
    state = LocalAppHazeState.current,
    style = HazeDefaults.style(blurRadius = 24.dp)
)
```

Applied to dialogs, bottom sheets, and the bottom navigation bar. Toggleable via `expressive_blur_ui` preference.

**Verdict:** Not part of Google's M3 Expressive spec. Third-party dependency. Adds visual polish but also complexity and potential performance cost. Not recommended for Hail.

## Emphasized Typography

- `titleLarge` with `FontWeight.Bold` in top app bars and section headers
- Section headers use `labelSmall` or `labelLarge` with `primary` color
- App names use `headlineSmall` with `FontWeight.Bold`

**Verdict:** Correct usage, though could leverage more emphasized variants from the type scale.

## Connected Button Groups

**File:** `manager/app/src/main/java/zx/azenith/ui/subscreen/CustomThemeScreen.kt`

Uses Google's expressive button group APIs:
```kotlin
ButtonGroupDefaults.connectedLeadingButtonShapes()
ButtonGroupDefaults.connectedTrailingButtonShapes()
ButtonGroupDefaults.connectedMiddleButtonShapes()
ButtonGroupDefaults.ConnectedSpaceBetween
```

**Verdict:** Correct usage of Google's expressive APIs. This is the right way to implement connected button groups.

## Expressive APIs Used

| API | Status |
|-----|--------|
| `MaterialExpressiveTheme` | ✅ Correct |
| `MotionScheme.expressive()` | ✅ Correct |
| `expressiveLightColorScheme()` | ✅ Correct |
| `LargeFlexibleTopAppBar` | ✅ Correct |
| `ListItemDefaults.segmentedShapes` | ❌ Not used (custom implementation) |
| `ButtonGroupDefaults.connectedLeadingButtonShapes` | ✅ Correct |
| `CircularWavyProgressIndicator` | ✅ Correct (but not needed for Hail) |
| `LinearWavyProgressIndicator` | ✅ Correct (but not needed for Hail) |
| `MaterialTheme.motionScheme` | ❌ Not used (custom springs) |
| `haze` library | ⚠️ Third-party, not Google spec |

## Key Takeaways for Hail

**Adopt:**
- `ExpressiveListItem` base pattern (simplified) for consistent row styling
- `ButtonGroupDefaults.connectedLeadingButtonShapes()` for connected button groups
- Gradient scrim behind top app bar for polish
- `ExpressiveSliderItem` pattern (simplified) for slider rows

**Skip:**
- 767-line `ExpressiveListComponent.kt` — use `ListItemDefaults.segmentedShapes()` instead
- 26dp outer corners — use Google's spec values
- Haze blur — not in Google's M3 Expressive spec
- Wavy progress indicators — Hail has minimal loading states
- `materialkolor` dependency — Hail already has Monet

**Lessons:**
1. The maximalist approach (custom everything) creates maintenance burden without proportional visual improvement
2. Google's `ButtonGroupDefaults` APIs are well-implemented and should be used directly
3. Glassmorphism is a stylistic choice, not an expressive requirement
4. A 767-line component file is a sign that built-in APIs are being reinvented
