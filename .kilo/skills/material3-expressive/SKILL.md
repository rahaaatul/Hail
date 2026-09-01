---
name: material3-expressive
description: Implement Material 3 Expressive UI in Jetpack Compose — seven pillars (color, shape, type, motion, layout, components, icons), MotionScheme.expressive(), LoadingIndicator, FloatingToolbar, FlexibleBottomAppBar, ButtonGroup, SplitButton, morphing shapes, 48-role color system, full-radius, fixed accent colors, background blur, haptics, Material Symbols fill toggle, Android 16+ edge-to-edge and predictive back. Use when building or modernizing Android UIs in this repo (Hail uses material3 1.5.0-alpha27 + compose-bom 2026.08.00).
---

# Material 3 Expressive for Jetpack Compose

This skill consolidates the four Material 3 Expressive sources (`material-design-3-master-guide`, `material-3-expressive`, `android-material-3-expressive-design`, `material-3-expressive-ui`) into one concrete, project-aligned reference. Two of the four sources were fetched directly; the other two were reconstructed from the broader M3 Expressive design system documentation (m3.material.io, Android Developers, supercharge.design). The working spec for any Compose UI work in Hail.

Hail already ships `androidx.compose.material3:1.5.0-alpha27` and `compose-bom:2026.08.00` — every API below is available. Opt in once per file with `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

## Core principle: seven pillars, one theme

A Material 3 Expressive surface is the product of seven coordinated systems. Touching one without the others produces a generic-looking app.

1. **Color** — the expanded 48-role color system, not the legacy 26 roles. Hero elements get tonal-surfaced container colors; supporting surfaces stay neutral. **Fixed accent colors** stay constant across light and dark themes for branding.
2. **Typography** — emphasis-scale-aware: prefer `displayLarge/Medium/Small` for hero moments, `headline*` for section titles, `title*` for in-list hierarchy, `body*` for content, `label*` for affordances. **Emphasized** variants for editorial feel.
3. **Shape** — 35 shapes across 5 size buckets (extraSmall → extraLarge) with **morphing** transitions between pressed/checked/selected states. **Full-radius** corners (100% of component size) for hero elements.
4. **Motion** — `MotionScheme.expressive()` for hero moments, `MotionScheme.standard()` for utilitarian UI. Spring-based physics (stiffness, damping, velocity), not linear easing. Pair with **haptics** for tactile feedback.
5. **Layout** — spacing tokens, density scales, **background blur** for overlays/navigation, **adaptive** WindowSizeClass.
6. **Components** — expressive variants of FAB, button, app bar, nav, progress, slider; new ButtonGroup, SplitButton, FloatingToolbar, FAB menu, LoadingIndicator, FlexibleBottomAppBar.
7. **Icons** — Material Symbols (variable icon fonts) with **fill toggle** (filled vs outlined) for selected/unselected nav state.

```kotlin
MaterialTheme(
    colorScheme = colorScheme,
    typography = typography,
    shapes = shapes,
    motionScheme = MotionScheme.expressive(),
) { content() }
```

## Quick reference — pick the right component

| Need | Expressive component | Replaces |
|---|---|---|
| Short wait (<5s) | `LoadingIndicator` | `CircularProgressIndicator` |
| Wait with container | `ContainedLoadingIndicator` | `CircularProgressIndicator` + card |
| Bottom action bar | `DockedToolbar` | `BottomAppBar` |
| Contextual floating actions | `FloatingToolbar` (horizontal/vertical) | bespoke FAB row |
| Scroll-reactive bottom bar | `FlexibleBottomAppBar` | custom animated bar |
| Multi-select actions | `ButtonGroup` (connected, morphing) | `Row` of buttons |
| Primary + dropdown action | `SplitButton` | FAB + menu |
| Tonal progress | `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` | linear/circular |
| Range value | `RangeSlider` (expressive variant) | `RangeSlider` |
| Scrolling top app bar | `LargeFlexibleTopAppBar` / `MediumFlexibleTopAppBar` / `CenterAlignedTopAppBar` | `TopAppBar` |
| Connected list items | `SegmentedListItem` (morphing leading shape) | `ListItem` + manual padding |

## Required opt-ins

```kotlin
@file:OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalMaterial3Api::class,
)

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
```

## Build sequence — pillars in order

Set up the seven pillars in this order. Each one builds on the last; skipping ahead produces inconsistent surfaces.

1. **Color** — pick a brand seed; generate the 48-role scheme (or rely on dynamic color on Android 12+).
2. **Shape** — define the 5-bucket shape scale; choose full-radius for hero elements.
3. **Typography** — load the emphasized type scale; map roles to display/headline/title/body/label.
4. **Icons** — wire Material Symbols with `fill` axis for selected/unselected state.
5. **Components** — adopt expressive variants: `FloatingToolbar`, `DockedToolbar`, `ButtonGroup`, `SplitButton`, `FAB` menu, `LoadingIndicator`, `FlexibleBottomAppBar`, `LargeFlexibleTopAppBar`.
6. **Motion** — set `MotionScheme.expressive()`; route hero interactions through expressive specs, utilitarian through standard.
7. **Layout** — apply spacing tokens; add background blur to overlays; adapt to WindowSizeClass.

Expressive enhancements (overlay on top of the base sequence): emphasized typography, decorative shapes, expressive motion for hero moments, fixed accent colors, contrast levels, background blur.

## Component pillar mapping

Every expressive component draws from several pillars. When styling a component, touch all of these — partial styling is the most common source of "this looks off".

| Component | Color | Shape | Type | Motion | Layout | Icons |
|---|---|---|---|---|---|---|
| **Button** | primary/secondary/tertiary + state layers | full radius, morphing on press | label-large | fastSpatialSpec on press | 48dp min touch target | optional leading icon |
| **ButtonGroup** | primaryContainer | connected corners morphing between selected items | label-large | defaultSpatialSpec on selection | horizontal row, equal width | optional per-item icon |
| **SplitButton** | primary (leading) + tonal (trailing) | split corner radii | label-large | defaultSpatialSpec on expand | leading + trailing slot | leading icon + dropdown |
| **FAB / FAB menu** | primaryContainer / secondary | full (round) | label-large for extended | defaultSpatialSpec on expand | bottom-end anchor, 16dp gutter | hero icon |
| **FloatingToolbar** | surfaceContainer | floating radius (large) | label-large | slowSpatialSpec enter/exit | horizontal or vertical, content padding | row of IconButtons |
| **DockedToolbar** | surfaceContainer | docked radius | label-large | defaultEffectsSpec on selection | docked to bottom edge | IconButton row |
| **FlexibleBottomAppBar** | surfaceContainer | bottom radius | label-medium | defaultSpatialSpec on scroll | scroll-reactive height | NavigationBarItem icons |
| **LargeFlexibleTopAppBar** | surface | top radius | headline-medium (collapsed) → headline-large (expanded) | defaultSpatialSpec on scroll | scroll-collapsible | title + actions |
| **LoadingIndicator** | primary | morphing arc polygons | — | fastEffectsSpec | centered or inline | — |
| **Card (elevated)** | surfaceContainerHigh | medium (12dp) | title-medium + body-medium | defaultEffectsSpec on hover | 16dp padding | optional |
| **ListItem** | onSurface | segmented per-section | title + body | defaultEffectsSpec on ripple | leading + content + trailing | leading/trailing icons |
| **TextField** | surfaceVariant + outline | extraSmall (4dp) | body-large | defaultEffectsSpec on focus | 56dp height, 16dp gutter | optional trailing icon |
| **Chip** | surface / surfaceContainer | extraSmall | label-large | fastSpatialSpec on press | height 32dp | optional leading icon |
| **Tab** | primary on selected | full pill indicator | title-small | defaultSpatialSpec on indicator | equal-width row | optional icon |
| **Snackbar** | inverseSurface | extraSmall | body-medium | slowSpatialSpec enter | bottom, 16dp gutter | — |
| **Dialog** | surfaceContainerHigh | extraLarge (28dp) | headline + body | slowEffectsSpec on enter | centered, max-width 560dp | — |
| **NavigationBar item** | onSurface + primaryContainer on selected | full pill (selected) | label-medium | fastEffectsSpec | 80dp height | fill-toggle icon (selected/unselected) |

## Icons — Material Symbols

Use Material Symbols, not legacy Material Icons. They are variable icon fonts that support four axes: `wght` (weight), `GRAD` (grade), `opsz` (optical size), `FILL` (fill).

```kotlin
// In Compose
Icon(
    imageVector = Icons.Filled.Add,  // or Filled/Outlined/Rounded/TwoTone/Sharp
    contentDescription = null,
    modifier = Modifier.size(24.dp),
)
```

For **navigation selected/unselected** state, toggle between Filled (selected) and Outlined (unselected) variants. Never animate the color alone — the fill change is the signal.

The `material-icons-extended` dependency (already in Hail's `libs.versions.toml`) ships Compose vector variants; pair with `material-symbols` only if you need the variable font with custom axes.

## MotionScheme — the heart of expressive

`MotionScheme` was added in `1.5.0-alpha27`. It returns `FiniteAnimationSpec<T>` for six motion slots. Use them via `animateDpAsState`/`animate*AsState` with the scheme specs, not hand-tuned springs.

```kotlin
val motion = MaterialTheme.motionScheme
val spatial by animateDpAsState(
    targetValue = if (expanded) 240.dp else 56.dp,
    animationSpec = motion.defaultSpatialSpec(),   // shape-changing transitions
    label = "size",
)
val tint by animateColorAsState(
    targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
    animationSpec = motion.fastEffectsSpec(),      // color/alpha — non-spatial
    label = "tint",
)
```

Slot usage:

| Spec | Use for |
|---|---|
| `defaultSpatialSpec` | size, shape, position changes (affects bounds) |
| `fastSpatialSpec` | quick morphs (button press, toggle) |
| `slowSpatialSpec` | long transitions (sheet expand, hero enter) |
| `defaultEffectsSpec` | color, alpha, content (no bound change) |
| `fastEffectsSpec` | hover/ripple-like tints |
| `slowEffectsSpec` | long crossfades |

`expressive()` returns visually engaging specs (spring-based, higher amplitude). `standard()` returns linear-feeling specs for utilitarian work. The expressive scheme is meant for hero interactions and prominent UI; do not use it for every list row.

## Component patterns

### LoadingIndicator

Replace any `CircularProgressIndicator` used for short waits. It morphs shape between a circle and a square arch based on progress.

```kotlin
LoadingIndicator(
    modifier = Modifier.size(48.dp),
    color = MaterialTheme.colorScheme.primary,
    polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.take(4),
)
```

For progress with a container:

```kotlin
ContainedLoadingIndicator(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
    indicatorColor = MaterialTheme.colorScheme.primary,
)
```

### DockedToolbar (replaces BottomAppBar)

```kotlin
DockedToolbar(
    modifier = Modifier.fillMaxWidth(),
    floatingActionButton = {
        FloatingActionButton(onClick = { /* primary action */ }) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    },
) {
    IconButton(onClick = {}) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) }
    Spacer(Modifier.weight(1f))
    IconButton(onClick = {}) { Icon(Icons.Default.Search, null) }
    IconButton(onClick = {}) { Icon(Icons.Default.MoreVert, null) }
}
```

### FlexibleBottomAppBar — scroll-reactive

```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    bottomBar = {
        FlexibleBottomAppBar(
            scrollBehavior = scrollBehavior,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            bottomDestinations.forEach { dest ->
                NavigationBarItem(
                    selected = dest == selected,
                    onClick = { selected = dest },
                    icon = { Icon(dest.icon, null) },
                    label = { Text(dest.label) },
                )
            }
        }
    },
) { padding -> /* content */ }
```

### FloatingToolbar — contextual, horizontal or vertical

```kotlin
val expanded by remember { mutableStateOf(false) }
Box {
    FloatingActionButton(onClick = { expanded = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = null)
    }
    FloatingToolbar(
        expanded = expanded,
        onDismiss = { expanded = false },
        content = {
            IconButton(onClick = {}) { Icon(Icons.Default.Edit, null) }
            IconButton(onClick = {}) { Icon(Icons.Default.Share, null) }
            IconButton(onClick = {}) { Icon(Icons.Default.Delete, null) }
        },
    )
}
```

Set `orientation = FloatingToolbarOrientation.Vertical` for a vertical toolbar.

### ButtonGroup — connected, morphing

```kotlin
SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
    options.forEachIndexed { index, label ->
        SegmentedButton(
            selected = selected == index,
            onClick = { selected = index },
            shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = options.size,
            ),
        ) { Text(label) }
    }
}
```

For expressive multi-select, use `MultiChoiceSegmentedButtonRow`. Shapes morph at the joints as selection changes.

### LargeFlexibleTopAppBar — scroll-reactive hero

```kotlin
val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
Scaffold(
    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
        LargeFlexibleTopAppBar(
            title = { Text("Hail") },
            scrollBehavior = scrollBehavior,
        )
    },
) { padding -> /* content */ }
```

Use `MediumFlexibleTopAppBar` for less dominant scroll states; `CenterAlignedTopAppBar` for utility screens.

### SegmentedListItem / connected list rows

```kotlin
ListItem(
    headlineContent = { Text("Frozen") },
    leadingContent = { Icon(Icons.Default.AcUnit, null) },
    colors = ListItemDefaults.colors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
)
```

For grouped/connected lists, use `Modifier.clip(RoundedCornerShape(...))` per section and `Modifier.divider()` between — never hairline `Divider()` for expressive layouts.

## Theme — the 48-role color system

Material 3 Expressive extends the color scheme from 26 to **48 roles**. The new roles support hero elements and tonal-surfaced containers.

### Dynamic color (Android 12+)

```kotlin
val scheme = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val ctx = LocalContext.current
        if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    }
    isSystemInDarkTheme() -> darkColorScheme()
    else -> lightColorScheme()
}
```

Android 16 adds predictive wallpaper-derived schemes; respect `dynamicDarkColorScheme(LocalContext.current)` — do not roll your own.

### Tonal elevation overlay — no shadows

Expressive UI uses **tonal color overlays** for elevation, not drop shadows. `surfaceContainer`, `surfaceContainerHigh`, `surfaceContainerHighest` are the new semantic roles:

```kotlin
Card(
    colors = CardDefaults.elevatedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
) { /* content */ }
```

Never combine `Modifier.shadow(...)` with these surfaces — the shadow contradicts the tonal-overlay model.

### Emphasized typography (preferred)

`EmphasizedTypography` is the recommended typography in Expressive. It tightens line height and slightly bumps weight for editorial feel.

```kotlin
val emphasis = Typography(
    displayLarge = Typography().displayLarge.copy(fontWeight = FontWeight.W400),
    displayMedium = Typography().displayMedium.copy(fontWeight = FontWeight.W400),
    displaySmall = Typography().displaySmall.copy(fontWeight = FontWeight.W400),
    headlineLarge = Typography().headlineLarge.copy(fontWeight = FontWeight.W400),
    headlineMedium = Typography().headlineMedium.copy(fontWeight = FontWeight.W400),
    headlineSmall = Typography().headlineSmall.copy(fontWeight = FontWeight.W400),
)
```

Use `headline*` for hero section titles, `title*` for in-list hierarchy, `body*` for content, `label*` for affordances only.

## Shape morphing — five buckets, 35 shapes

```kotlin
val shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)
```

Morphing transitions happen automatically in components like `ButtonGroup`, `SegmentedButton`, and `SplitButton` when selection state changes. The animation is driven by `motionScheme.defaultSpatialSpec()`.

For custom morphing, animate `RoundedCornerShape` via `animateValueAsState` with a `TwoWayConverter<CornerBasedShape, AnimationVector4C>`.

## Android 16+ requirements

### Edge-to-edge (required)

`enableEdgeToEdge()` must be called in `onCreate` of every Activity. Apply via the activity:

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
}
```

In Compose, use `WindowInsets.systemBars`, `WindowInsets.navigationBars`, and `WindowInsets.statusBars` — do not consume insets via `Modifier.statusBarsPadding()` without checking edge-to-edge is enabled.

### Predictive back gesture

Material 3 components support predictive back out of the box on Android 16+ — `BottomSheet`, `NavigationDrawer`, `ModalBottomSheet`, and `DockedToolbar` all animate the back gesture preview.

Add to `AndroidManifest.xml`:

```xml
<application
    android:enableOnBackInvokedCallback="true"
    ...>
```

For custom back handling, implement `OnBackPressedCallback` in the Activity or use the `BackHandler` composable in Compose.

## Adaptive layouts (compact / medium / expanded)

Use `WindowSizeClass` from `androidx.compose.material3:material3-window-size-class`:

```kotlin
val windowSizeClass = calculateWindowSizeClass(activity)

when (windowSizeClass.widthSizeClass) {
    WindowWidthSizeClass.Compact -> /* single-pane, bottom bar */
    WindowWidthSizeClass.Medium -> /* two-pane adaptive */
    WindowWidthSizeClass.Expanded -> /* nav rail + multi-pane */
    else -> /* single-pane */
}
```

For Hail-style content, `Compact` is the default. Reserve rail layouts for tablets/foldables; use `WindowInsets` to size the navigation drawer.

## Accessibility — non-negotiable rules

1. **Contrast** — every foreground/background pair must meet WCAG 4.5:1 (3:1 for large text). Material 3 roles are pre-validated; never override `onPrimary` without re-checking.
2. **Touch targets** — 48dp minimum. Expressive buttons are large by default; do not shrink below 40dp without a `minimumInteractiveComponentSize` modifier.
3. **Talkback** — every `IconButton` needs a `contentDescription`; every custom gesture needs `Modifier.semantics { ... }`.
4. **Reduced motion** — guard expressive springs:

```kotlin
val motionScheme = if (
    LocalAccessibilityManager.current.calculateRecommendedTimeoutMillis(...) < 100
) MaterialTheme.motionScheme.standard() else MaterialTheme.motionScheme.expressive()
```

5. **High-contrast text** — prefer `onSurface` and `onSurfaceVariant`; avoid hard-coded `Color.Black`/`Color.White`.

## Expressive-only enhancements (overlay on the base system)

These features are unique to M3 Expressive and should be added after the base build sequence completes. They are not required for an accessible baseline — they exist to create emotional resonance at hero moments.

### Full-radius shapes

For hero elements (primary FAB, primary button, large thumbnails, expanded top-app-bar leading icon), set the corner shape to `RoundedCornerShape(percent = 100)`. The legacy "50%" pill is now `full` in the shape token system; use the keyword to stay correct across the type scale.

```kotlin
FloatingActionButton(
    onClick = { },
    shape = RoundedCornerShape(percent = 100),  // hero shape
) { Icon(Icons.Default.Add, null) }
```

Reserve full-radius for one element per screen. If everything is full, nothing is.

### Fixed accent colors

A small set of colors that remain constant across light and dark themes — used for brand identity, not for state.

```kotlin
val fixedAccent = Color(0xFFFF6F61)  // brand coral, identical in light + dark
val scheme = lightColorScheme(...).copy(
    tertiary = fixedAccent,
    onTertiary = Color.White,
) // explicit override of dynamic-color tertiary
```

For dynamic-color users: keep one or two fixed accents in your `colorScheme` and let the rest come from the wallpaper. Do not force the entire scheme fixed.

### Contrast levels

Three standardized contrast modes: **standard**, **medium**, **high**. Surface roles (`surfaceContainerLow/.../Highest`) shift in luminance to maintain AA contrast against text at each level.

```kotlin
val scheme = lightColorScheme(
    /* base roles */
).withContrastLevel(ContrastLevel.High)  // Compose M3 Expressive API
```

Expose this as a user preference in your accessibility settings. The default is `Standard`.

### Background blur

For overlays, navigation rail, and modal surfaces, use a translucent container with a blur backdrop. Compose Material 3 provides this via `BackdropBlur` (or `Modifier.blur` in `androidx.compose.ui`).

```kotlin
val scheme = /* base color scheme */
ModalNavigationDrawer(
    drawerContainerColor = scheme.surfaceContainer.copy(alpha = 0.75f),
    drawerContent = { /* rail content */ },
) { /* content */ }
```

Pair blur with tonal elevation, not with drop shadows — the two contradict each other.

### Haptics

Coordinated vibration that mirrors spring animations. Compose doesn't expose haptics through Material 3 directly; use `LocalHapticFeedback.current` from `androidx.compose.ui.hapticfeedback`:

```kotlin
val haptics = LocalHapticFeedback.current
Button(onClick = {
    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    /* action */
}) { /* label */ }
```

Match haptic intensity to motion amplitude: light taps for `fastEffectsSpec`, medium for `defaultSpatialSpec`, strong for `slowSpatialSpec` hero transitions. Always respect `Settings.System.HAPTIC_FEEDBACK_ENABLED`.

## Deprecated → replacement cheat sheet

| Deprecated | Expressive replacement |
|---|---|
| `BottomAppBar` | `DockedToolbar` / `FlexibleBottomAppBar` |
| `CircularProgressIndicator` (short waits) | `LoadingIndicator` |
| `LinearProgressIndicator` | `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` |
| `TopAppBar` | `LargeFlexibleTopAppBar` / `MediumFlexibleTopAppBar` / `CenterAlignedTopAppBar` |
| `ExtendedFloatingActionButton` w/o morph | `FloatingActionButtonMenu` for multi-action contexts |
| Single-action `Row` of buttons | `ButtonGroup` / `SplitButton` |
| `ListItem` with manual rounded corners | `ListItem` with `surfaceContainer` + segmented shape |
| `MaterialTheme.shapes` only | `MaterialTheme.shapes` + `MaterialTheme.motionScheme` both set |

## Hail-specific notes

- Hail already pins `material3 = "1.5.0-alpha27"` and `compose-bom = "2026.08.00"`. All APIs above are available. No new dependencies required.
- Add `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` to any file using expressive components. To silence project-wide, add the opt-in to a single `BuildKonfig`-style file or to the package's `build.gradle.kts`:

```kotlin
kotlinOptions {
    freeCompilerArgs += listOf(
        "-opt-in=androidx.compose.material3.ExperimentalMaterial3ExpressiveApi",
    )
}
```

- Hail uses `EdgeToEdge` semantics; if a fragment does not currently call `enableEdgeToEdge()`, the activity-level one in `MainActivity` covers it — confirm before removing per-fragment opt-in.
- The app already has a `MotionScheme` spec wired in (`MaterialTheme(..., motionScheme = MotionScheme.expressive())`). Do not pass a hand-rolled `SpringSpec` to `animate*AsState`; pass the scheme's `defaultSpatialSpec()` / `fastEffectsSpec()` etc. to stay consistent with theme changes.
- Existing connected-shape patterns (`HailShapes`, segmented list rows) should be migrated to `ButtonGroup` / `SingleChoiceSegmentedButtonRow` rather than custom drawables where possible.

## Verification commands

```bash
# Compile after theme change
./gradlew :app:compileDebugKotlin

# Lint check for banned APIs
./gradlew :app:lintDebug
```

## References

- https://m3.material.io/blog/building-with-m3-expressive
- https://developer.android.com/develop/ui/compose/designsystems/material3-expressive
- https://developer.android.com/reference/kotlin/androidx/compose/material3/MotionScheme
- https://developer.android.com/jetpack/androidx/releases/compose-material3
- Component catalog: https://m3.material.io/components