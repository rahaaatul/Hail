[Material Design 3](https://m3.material.io/) is the next evolution of Material Design. It includes
updated theming, components, and Material You personalization features like
dynamic color. It's an update to [Material Design 2](https://material.io/) and is cohesive with the
new visual style and system UI on Android 12 and higher.

This guide focuses on migrating from the [Compose Material
(androidx.compose.material) Jetpack library](https://developer.android.com/jetpack/androidx/releases/compose-material) to the [Compose Material 3
(androidx.compose.material3) Jetpack library](https://developer.android.com/jetpack/androidx/releases/compose-material3).

> [!NOTE]
> **Note:** This guide uses abbreviation "M3" to refer to the interchangeable terms: "Material 3", "Material Design 3", "Material You", and the Compose Material 3 (androidx.compose.material3) Jetpack library. The abbreviation "M2" is used to refer to the interchangeable terms: "Material 2", "Material Design 2", and the Compose Material (`androidx.compose.material`) Jetpack library.

## Approaches

In general, **you should not use both M2 and M3 in a single app long-term**.
This is because the two design systems and respective libraries differ
significantly in terms of their UX/UI designs and Compose implementations.

Your app may use a design system, such as one created using Figma. In such
cases, we also highly recommend that you or your design team migrate it from M2
to M3 *before* starting the Compose migration. It doesn't make sense to migrate
an app to M3 if its UX/UI design is based on M2.

Furthermore, your approach to migration should take your app's
size, complexity, and UX/UI design into account. Doing so helps you to
minimize the impact on your codebase. Take a phased approach to
migration.

> [!NOTE]
> **Note:** If you're starting a new app instead of migrating an existing app from M2 to M3, we recommend that you use M3 from the beginning.

### When to migrate

You should ideally start the migration as soon as possible. However, it's
important to consider whether your app is in a realistic position to
**fully** migrate from M2 to M3. There are some *blocker* scenarios to
consider investigating before you start:

| Scenario | Recommended approach |
|---|---|
| Your app has no significant blockers. | Begin phased migration. |
| Your app uses a component from M2 that isn't available in M3 yet. See the [Components and layouts](https://developer.android.com/develop/ui/compose/designsystems/material2-material3#components-and) section. | Begin phased migration. |
| You or your design team haven't migrated the app's design system from M2 to M3. | Migrate the design system from M2 to M3, then begin phased migration. |

Even if you're affected by the preceding scenarios, you should take a phased
approach to migration before committing and releasing an app update. In these
cases, you would use M2 and M3 side-by-side, and gradually phase out M2 while
migrating to M3.

### Phased approach

The general steps to a phased migration are as follows:

1. Add the M3 dependency alongside the M2 dependency.
2. Add the M3 versions of your app's themes alongside the M2 versions of your app's themes.
3. Migrate individual modules, screens, or composables to M3, depending on the size and complexity of your app (see the following sections for details).
4. Once fully migrated, remove the M2 versions of your app's themes.
5. Remove the M2 dependency.

## Dependencies

M3 has a separate package and version from M2:

### M2

    implementation "androidx.compose.material:material:$m2-version"

### M3

    implementation "androidx.compose.material3:material3:$m3-version"

See the latest M3 versions on the [Compose Material 3 releases page](https://developer.android.com/jetpack/androidx/releases/compose-material3).

Other Material dependencies outside of the main M2 and M3 libraries have not
changed. They use a mix of the M2 and M3 packages and versions, but this has no
impact on migration. They can be used as-is with M3:

| Library | Package and version |
|---|---|
| [Compose Material Icons](https://developer.android.com/reference/kotlin/androidx/compose/material/icons/package-summary) | `androidx.compose.material:material-icons-*:$m2-version` |
| [Compose Material Ripple](https://developer.android.com/reference/kotlin/androidx/compose/material/ripple/package-summary) | `androidx.compose.material:material-ripple:$m2-version` |

## Experimental APIs

Some M3 APIs are considered experimental. In such cases, you need to opt
in at the function or file level using the
[`ExperimentalMaterial3Api`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ExperimentalMaterial3Api) annotation:

    import androidx.compose.material3.ExperimentalMaterial3Api

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun AppComposable() {
        // M3 composables
    }

## Theming

In both M2 and M3, the theme composable is named `MaterialTheme` but the import
packages and parameters differ:

### M2

    import androidx.compose.material.MaterialTheme

    MaterialTheme(
        colors = AppColors,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        // M2 content
    }

### M3

    import androidx.compose.material3.MaterialTheme

    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        // M3 content
    }

> [!NOTE]
> **Note:** The parameters are different for the two types of `MaterialTheme`. This results in errors after the change. See the [Color](https://developer.android.com/develop/ui/compose/designsystems/material2-material3#color), [Typography](https://developer.android.com/develop/ui/compose/designsystems/material2-material3#typography) and [Shape](https://developer.android.com/develop/ui/compose/designsystems/material2-material3#shape) sections on how to resolve this.

### Color

![Comparison of the M2 to M3 color systems](https://developer.android.com/static/develop/ui/compose/images/migration-color-update.png) **Figure 1**. M2 color system (left) versus M3 color system (right).

The [color system](https://m3.material.io/styles/color/overview) in M3 is significantly different to M2. The
number of color parameters has increased, they have different names, and they
map differently to M3 components. In Compose, this applies to the M2
[`Colors`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors) class, the M3 [`ColorScheme`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ColorScheme) class, and related functions:

### M2

    import androidx.compose.material.lightColors
    import androidx.compose.material.darkColors

    val AppLightColors = lightColors(
        // M2 light Color parameters
    )
    val AppDarkColors = darkColors(
        // M2 dark Color parameters
    )
    val AppColors = if (darkTheme) {
        AppDarkColors
    } else {
        AppLightColors
    }

### M3

    import androidx.compose.material3.lightColorScheme
    import androidx.compose.material3.darkColorScheme

    val AppLightColorScheme = lightColorScheme(
        // M3 light Color parameters
    )
    val AppDarkColorScheme = darkColorScheme(
        // M3 dark Color parameters
    )
    val AppColorScheme = if (darkTheme) {
        AppDarkColorScheme
    } else {
        AppLightColorScheme
    }

Given the significant differences between the M2 and M3 color systems, there's
no reasonable mapping for [`Color`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors#isLight()) parameters. Instead, use the [Material
Theme Builder tool](https://m3.material.io/theme-builder) to generate an M3 color scheme. Use the M2
colors as *core* source colors in the tool, which the tool expands into tonal
palettes used by the M3 color scheme. The following mappings are recommended as
a starting point:

| M2 | Material Theme Builder |
|---|---|
| `primary` | Primary |
| `primaryVariant` | Secondary |
| `secondary` | Tertiary |
| `surface` or `background` | Neutral |

> [!NOTE]
> **Note:** Some M2 and M3 colors are named the same, such as primary and secondary. However, their hex code values may not be the same. This is because Material Theme Builder uses an algorithm to generate tonal palettes from the M2 colors, which the tools use in the M3 color scheme.

![M2 colors used in Material Theme Builder to generate an M3 color scheme](https://developer.android.com/static/develop/ui/compose/images/migration-colorscheme-update.png) **Figure 2**. Jetchat's M2 colors used in Material Theme Builder to generate an M3 color scheme.

You can copy the color hex code values for light and dark themes from the tool
and use them to implement an M3 ColorScheme instance. Alternatively, Material
Theme Builder can export Compose code.

#### `isLight`

Unlike the M2 `Colors` class, the M3 `ColorScheme` class doesn't include an
[`isLight`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors#isLight()) parameter. In general, you should try and model whatever needs
this information at the theme level. For example:

### M2

    import androidx.compose.material.lightColors
    import androidx.compose.material.darkColors
    import androidx.compose.material.MaterialTheme

    @Composable
    private fun AppTheme(
      darkTheme: Boolean = isSystemInDarkTheme(),
      content: @Composable () -> Unit
    ) {
      val colors = if (darkTheme) darkColors(...) else lightColors(...)
      MaterialTheme(
          colors = colors,
          content = content
      )
    }

    @Composable
    fun AppComposable() {
        AppTheme {
            val cardElevation = if (MaterialTheme.colors.isLight) 0.dp else 4.dp
            ...
        }
    }

### M3

    import androidx.compose.material3.lightColorScheme
    import androidx.compose.material3.darkColorScheme
    import androidx.compose.material3.MaterialTheme

    val LocalCardElevation = staticCompositionLocalOf { Dp.Unspecified }
    @Composable
    private fun AppTheme(
       darkTheme: Boolean = isSystemInDarkTheme(),
        content: @Composable () -> Unit
    ) {
       val cardElevation = if (darkTheme) 4.dp else 0.dp
        CompositionLocalProvider(LocalCardElevation provides cardElevation) {
            val colorScheme = if (darkTheme) darkColorScheme(...) else lightColorScheme(...)
            MaterialTheme(
                colorScheme = colorScheme,
                content = content
            )
        }
    }

    @Composable
    fun AppComposable() {
        AppTheme {
            val cardElevation = LocalCardElevation.current
            ...
        }
    }

See the [Custom design systems in Compose guide](https://developer.android.com/develop/ui/compose/designsystems/custom) for more information.

#### Dynamic color

A new feature in M3 is [dynamic color](https://m3.material.io/styles/color/dynamic-color/overview). Instead of using custom
colors, an M3 `ColorScheme` can make use of device wallpaper colors on Android
12 and higher, using the following functions:

- [`dynamicLightColorScheme`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#dynamiclightcolorscheme)
- [`dynamicDarkColorScheme`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#dynamicdarkcolorscheme)

> [!NOTE]
> **Note:** Even if you choose to use a dynamic color scheme, you almost always need a fallback custom color scheme given the Android 12 and higher restriction. See [Jetchat](https://github.com/android/compose-samples/blob/main/Jetchat/app/src/main/java/com/example/compose/jetchat/theme/Themes.kt#L91) as an example.

### Typography

![Comparison of M2 and M3 typography systems](https://developer.android.com/static/develop/ui/compose/images/migration-typography-update.png) **Figure 3**. M3 typography system (left) versus M2 typography system (right)

The [typography system](https://m3.material.io/styles/typography/overview) in M3 is different to M2. The number of
typography parameters is roughly the same, but they have different names and
they map differently to M3 components. In Compose, this applies to the M2
[`Typography`](https://developer.android.com/reference/kotlin/androidx/compose/material/Typography) class and the M3 [`Typography`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Typography) class:

### M2

    import androidx.compose.material.Typography

    val AppTypography = Typography(
        // M2 TextStyle parameters
    )

### M3

    import androidx.compose.material3.Typography

    val AppTypography = Typography(
        // M3 TextStyle parameters
    )

The following [`TextStyle`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextStyle) parameter mappings are recommended as a starting
point:

| M2 | M3 |
|---|---|
| `h1` | `displayLarge` |
| `h2` | `displayMedium` |
| `h3` | `displaySmall` |
| N/A | `headlineLarge` |
| `h4` | `headlineMedium` |
| `h5` | `headlineSmall` |
| `h6` | `titleLarge` |
| `subtitle1` | `titleMedium` |
| `subtitle2` | `titleSmall` |
| `body1` | `bodyLarge` |
| `body2` | `bodyMedium` |
| `caption` | `bodySmall` |
| `button` | `labelLarge` |
| N/A | `labelMedium` |
| `overline` | `labelSmall` |

> [!NOTE]
> **Note:** Unlike the M2 `Typography` class, the M3 `Typography` class doesn't include a `defaultFontFamily` parameter. You'll need to use the `fontFamily` parameter in each of the individual `TextStyles` instead.

### Shape

![Comparison of M2 and M3 shape systems](https://developer.android.com/static/develop/ui/compose/images/migration-shape-update.png) **Figure 4**. M2 shape system (left) versus M3 shape system (right).

The [shape system](https://m3.material.io/styles/shape/overview) in M3 is different to M2. The number of shape
parameters has increased, they're named differently and they map differently to
M3 components. In Compose, this applies to the M2 [`Shapes`](https://developer.android.com/reference/kotlin/androidx/compose/material/Shapes) class and the
M3 [`Shapes`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Shapes) class:

### M2

    import androidx.compose.material.Shapes

    val AppShapes = Shapes(
        // M2 Shape parameters
    )

### M3

    import androidx.compose.material3.Shapes

    val AppShapes = Shapes(
        // M3 Shape parameters
    )

The following [`Shape`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Shape) parameter mappings are recommended as a starting
point:

| M2 | M3 |
|---|---|
| N/A | `extraSmall` |
| `small` | `small` |
| `medium` | `medium` |
| `large` | `large` |
| N/A | `extraLarge` |

> [!NOTE]
> **Note:** The M3 shape system also defines `none` and `full` styles which are constant and not part of the `Shapes` class. Use [`RectangleShape`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary#RectangleShape()) and [`CircleShape`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/package-summary#CircleShape()) respectively for these.

## Components and layouts

Most components and layouts from M2 are available in M3. There are, however,
some missing as well as new ones that didn't exist in M2. Furthermore, some M3
components have more variations than their equivalents in M2. In general, the M3
API surfaces are designed to be as similar as possible to their closest
equivalents in M2.

Given the updated color, typography and shape systems, M3 components tend to map
differently to the new theming values. It's a good idea to check out the [tokens
directory](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/tokens/) in the Compose Material 3 source code as a source of
truth for these mappings.

While some components require special considerations, the following function
mappings are recommended as a starting point:

**Missing APIs**:

| M2 | M3 |
|---|---|
| [`androidx.compose.material.swipeable`](https://developer.android.com/reference/kotlin/androidx/compose/material/swipeable.modifier) | Not available yet |

**Replaced APIs**:

| M2 | M3 |
|---|---|
| [`androidx.compose.material.BackdropScaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material/BackdropScaffold.composable) | No M3 equivalent, migrate to [`Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable) or [`BottomSheetScaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomSheetScaffold.composable) instead |
| [`androidx.compose.material.BottomDrawer`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomDrawer.composable) | No M3 equivalent, migrate to [`ModalBottomSheet`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable) instead |

**Renamed APIs**:

| M2 | M3 |
|---|---|
| [`androidx.compose.material.BottomNavigation`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomNavigation.composable) | [`androidx.compose.material3.NavigationBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationBar.composable) |
| [`androidx.compose.material.BottomNavigationItem`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomNavigationItem.composable) | [`androidx.compose.material3.NavigationBarItem`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationBarItem.composable) |
| [`androidx.compose.material.Chip`](https://developer.android.com/reference/kotlin/androidx/compose/material/Chip.composable) | [`androidx.compose.material3.AssistChip`](https://developer.android.com/reference/kotlin/androidx/compose/material3/AssistChip.composable) or [`androidx.compose.material3.SuggestionChip`](https://developer.android.com/reference/kotlin/androidx/compose/material3/SuggestionChip.composable) |
| [`androidx.compose.material.ModalBottomSheetLayout`](https://developer.android.com/reference/kotlin/androidx/compose/material/ModalBottomSheetLayout.composable) | [`androidx.compose.material3.ModalBottomSheet`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalBottomSheet.composable) |
| [`androidx.compose.material.ModalDrawer`](https://developer.android.com/reference/kotlin/androidx/compose/material/ModalDrawer.composable) | [`androidx.compose.material3.ModalNavigationDrawer`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalNavigationDrawer.composable) |

**All other APIs**:

| M2 | M3 |
|---|---|
| [`androidx.compose.material.AlertDialog`](https://developer.android.com/reference/kotlin/androidx/compose/material/AlertDialog.composable) | [`androidx.compose.material3.AlertDialog`](https://developer.android.com/reference/kotlin/androidx/compose/material3/AlertDialog.composable) |
| [`androidx.compose.material.Badge`](https://developer.android.com/reference/kotlin/androidx/compose/material/Badge.composable) | [`androidx.compose.material3.Badge`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Badge.composable) |
| [`androidx.compose.material.BadgedBox`](https://developer.android.com/reference/kotlin/androidx/compose/material/BadgedBox.composable) | [`androidx.compose.material3.BadgedBox`](https://developer.android.com/reference/kotlin/androidx/compose/material3/BadgedBox.composable) |
| [`androidx.compose.material.BottomAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomAppBar.composable) | [`androidx.compose.material3.BottomAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomAppBar.composable) |
| [`androidx.compose.material.BottomSheetScaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomSheetScaffold.composable) | [`androidx.compose.material3.BottomSheetScaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material3/BottomSheetScaffold.composable) |
| [`androidx.compose.material.Button`](https://developer.android.com/reference/kotlin/androidx/compose/material/Button.composable) | [`androidx.compose.material3.Button`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Button.composable) |
| [`androidx.compose.material.Card`](https://developer.android.com/reference/kotlin/androidx/compose/material/Card.composable) | [`androidx.compose.material3.Card`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Card.composable) |
| [`androidx.compose.material.Checkbox`](https://developer.android.com/reference/kotlin/androidx/compose/material/Checkbox.composable) | [`androidx.compose.material3.Checkbox`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Checkbox.composable) |
| [`androidx.compose.material.CircularProgressIndicator`](https://developer.android.com/reference/kotlin/androidx/compose/material/CircularProgressIndicator.composable) | [`androidx.compose.material3.CircularProgressIndicator`](https://developer.android.com/reference/kotlin/androidx/compose/material3/CircularProgressIndicator.composable) |
| [`androidx.compose.material.Divider`](https://developer.android.com/reference/kotlin/androidx/compose/material/Divider.composable) | [`androidx.compose.material3.Divider`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Divider.composable) |
| [`androidx.compose.material.DropdownMenu`](https://developer.android.com/reference/kotlin/androidx/compose/material/DropdownMenu.composable) | [`androidx.compose.material3.DropdownMenu`](https://developer.android.com/reference/kotlin/androidx/compose/material3/DropdownMenu.composable) |
| [`androidx.compose.material.DropdownMenuItem`](https://developer.android.com/reference/kotlin/androidx/compose/material/DropdownMenuItem.composable) | [`androidx.compose.material3.DropdownMenuItem`](https://developer.android.com/reference/kotlin/androidx/compose/material3/DropdownMenuItem.composable) |
| [`androidx.compose.material.ExposedDropdownMenuBox`](https://developer.android.com/reference/kotlin/androidx/compose/material/ExposedDropdownMenuBox.composable) | [`androidx.compose.material3.ExposedDropdownMenuBox`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ExposedDropdownMenuBox.composable) |
| [`androidx.compose.material.ExtendedFloatingActionButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/ExtendedFloatingActionButton.composable) | [`androidx.compose.material3.ExtendedFloatingActionButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ExtendedFloatingActionButton.composable) |
| [`androidx.compose.material.FilterChip`](https://developer.android.com/reference/kotlin/androidx/compose/material/FilterChip.composable) | [`androidx.compose.material3.FilterChip`](https://developer.android.com/reference/kotlin/androidx/compose/material3/FilterChip.composable) |
| [`androidx.compose.material.FloatingActionButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/FloatingActionButton.composable) | [`androidx.compose.material3.FloatingActionButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/FloatingActionButton.composable) |
| [`androidx.compose.material.Icon`](https://developer.android.com/reference/kotlin/androidx/compose/material/Icon.composable) | [`androidx.compose.material3.Icon`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Icon.composable) |
| [`androidx.compose.material.IconButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/IconButton.composable) | [`androidx.compose.material3.IconButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconButton.composable) |
| [`androidx.compose.material.IconToggleButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/IconToggleButton.composable) | [`androidx.compose.material3.IconToggleButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/IconToggleButton.composable) |
| [`androidx.compose.material.LeadingIconTab`](https://developer.android.com/reference/kotlin/androidx/compose/material/LeadingIconTab.composable) | [`androidx.compose.material3.LeadingIconTab`](https://developer.android.com/reference/kotlin/androidx/compose/material3/LeadingIconTab.composable) |
| [`androidx.compose.material.LinearProgressIndicator`](https://developer.android.com/reference/kotlin/androidx/compose/material/LinearProgressIndicator.composable) | [`androidx.compose.material3.LinearProgressIndicator`](https://developer.android.com/reference/kotlin/androidx/compose/material3/LinearProgressIndicator.composable) |
| [`androidx.compose.material.ListItem`](https://developer.android.com/reference/kotlin/androidx/compose/material/ListItem.composable) | [`androidx.compose.material3.ListItem`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ListItem.composable) |
| [`androidx.compose.material.NavigationRail`](https://developer.android.com/reference/kotlin/androidx/compose/material/NavigationRail.composable) | [`androidx.compose.material3.NavigationRail`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationRail.composable) |
| [`androidx.compose.material.NavigationRailItem`](https://developer.android.com/reference/kotlin/androidx/compose/material/NavigationRailItem.composable) | [`androidx.compose.material3.NavigationRailItem`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationRailItem.composable) |
| [`androidx.compose.material.OutlinedButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/OutlinedButton.composable) | [`androidx.compose.material3.OutlinedButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedButton.composable) |
| [`androidx.compose.material.OutlinedTextField`](https://developer.android.com/reference/kotlin/androidx/compose/material/OutlinedTextField.composable) | [`androidx.compose.material3.OutlinedTextField`](https://developer.android.com/reference/kotlin/androidx/compose/material3/OutlinedTextField.composable) |
| [`androidx.compose.material.RadioButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/RadioButton.composable) | [`androidx.compose.material3.RadioButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/RadioButton.composable) |
| [`androidx.compose.material.RangeSlider`](https://developer.android.com/reference/kotlin/androidx/compose/material/RangeSlider.composable) | [`androidx.compose.material3.RangeSlider`](https://developer.android.com/reference/kotlin/androidx/compose/material3/RangeSlider.composable) |
| [`androidx.compose.material.Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material/Scaffold.composable) | [`androidx.compose.material3.Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable) |
| [`androidx.compose.material.ScrollableTabRow`](https://developer.android.com/reference/kotlin/androidx/compose/material/ScrollableTabRow.composable) | [`androidx.compose.material3.ScrollableTabRow`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ScrollableTabRow.composable) |
| [`androidx.compose.material.Slider`](https://developer.android.com/reference/kotlin/androidx/compose/material/Slider.composable) | [`androidx.compose.material3.Slider`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Slider.composable) |
| [`androidx.compose.material.Snackbar`](https://developer.android.com/reference/kotlin/androidx/compose/material/Snackbar.composable) | [`androidx.compose.material3.Snackbar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Snackbar.composable) |
| [`androidx.compose.material.Switch`](https://developer.android.com/reference/kotlin/androidx/compose/material/Switch.composable) | [`androidx.compose.material3.Switch`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Switch.composable) |
| [`androidx.compose.material.Tab`](https://developer.android.com/reference/kotlin/androidx/compose/material/Tab.composable) | [`androidx.compose.material3.Tab`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Tab.composable) |
| [`androidx.compose.material.TabRow`](https://developer.android.com/reference/kotlin/androidx/compose/material/TabRow.composable) | [`androidx.compose.material3.TabRow`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TabRow.composable) |
| [`androidx.compose.material.Text`](https://developer.android.com/reference/kotlin/androidx/compose/material/Text.composable) | [`androidx.compose.material3.Text`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Text.composable) |
| [`androidx.compose.material.TextButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/TextButton.composable) | [`androidx.compose.material3.TextButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextButton.composable) |
| [`androidx.compose.material.TextField`](https://developer.android.com/reference/kotlin/androidx/compose/material/TextField.composable) | [`androidx.compose.material3.TextField`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TextField.composable) |
| [`androidx.compose.material.TopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material/TopAppBar.composable) | [`androidx.compose.material3.TopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TopAppBar.composable) |
| [`androidx.compose.material.TriStateCheckbox`](https://developer.android.com/reference/kotlin/androidx/compose/material/TriStateCheckbox.composable) | [`androidx.compose.material3.TriStateCheckbox`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TriStateCheckbox.composable) |

See the latest M3 components and layouts on the [Compose Material 3 API
reference overview](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#overview), and keep an eye out on the [releases page](https://developer.android.com/jetpack/androidx/releases/compose-material3) for new
and updated APIs.

### Scaffold, snackbars and navigation drawer

![Comparison of M2 and M3 scaffold with snackbar and navigation drawer](https://developer.android.com/static/develop/ui/compose/images/migration-scaffold-update.gif) **Figure 5**. M2 scaffold with snackbar and navigation drawer (left) versus M3 scaffold with snackbar and navigation drawer (right).

Scaffold in M3 is different to M2. In both M2 and M3, the main layout composable
is named `Scaffold` but the import packages and parameters differ:

### M2

    import androidx.compose.material.Scaffold

    Scaffold(
        // M2 scaffold parameters
    )

### M3

    import androidx.compose.material3.Scaffold

    Scaffold(
        // M3 scaffold parameters
    )

The M2 [`Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material/Scaffold.composable) contains a `backgroundColor` parameter is now named to
`containerColor` in the M3 [`Scaffold`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Scaffold.composable):

### M2

    import androidx.compose.material.Scaffold

    Scaffold(
        backgroundColor = ...,
        content = { ... }
    )

### M3

    import androidx.compose.material3.Scaffold

    Scaffold(
        containerColor = ...,
        content = { ... }
    )

The M2 [`ScaffoldState`](https://developer.android.com/reference/kotlin/androidx/compose/material/ScaffoldState) class no longer exists in M3 as it contains a
[`drawerState`](https://developer.android.com/reference/kotlin/androidx/compose/material/ScaffoldState#drawerState()) parameter which is no longer needed. To show snackbars with
the M3 `Scaffold`, use [`SnackbarHostState`](https://developer.android.com/reference/kotlin/androidx/compose/material/SnackbarHostState) instead:

### M2

    import androidx.compose.material.Scaffold
    import androidx.compose.material.rememberScaffoldState

    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        content = {
            ...
            scope.launch {
                scaffoldState.snackbarHostState.showSnackbar(...)
            }
        }
    )

### M3

    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.SnackbarHost
    import androidx.compose.material3.SnackbarHostState

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        content = {
            ...
            scope.launch {
                snackbarHostState.showSnackbar(...)
            }
        }
    )

> [!NOTE]
> **Note:** The [`SnackbarData`](https://developer.android.com/reference/kotlin/androidx/compose/material/SnackbarData) class in M2 has been split into [`SnackbarData`](https://developer.android.com/reference/kotlin/androidx/compose/material3/SnackbarData) and [`SnackbarVisuals`](https://developer.android.com/reference/kotlin/androidx/compose/material3/SnackbarVisuals) in M3.

All of the `drawer*` parameters from the M2 `Scaffold` have been removed from
the M3 `Scaffold`. These include parameters such as `drawerShape` and
`drawerContent`. To show a drawer with the M3 `Scaffold`, use a navigation
drawer composable, such as [`ModalNavigationDrawer`](https://developer.android.com/reference/kotlin/androidx/compose/material3/ModalNavigationDrawer.composable), instead:

### M2

    import androidx.compose.material.DrawerValue
    import
    import androidx.compose.material.Scaffold
    import androidx.compose.material.rememberDrawerState
    import androidx.compose.material.rememberScaffoldState

    val scaffoldState = rememberScaffoldState(
        drawerState = rememberDrawerState(DrawerValue.Closed)
    )
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = { ... },
        drawerGesturesEnabled = ...,
        drawerShape = ...,
        drawerElevation = ...,
        drawerBackgroundColor = ...,
        drawerContentColor = ...,
        drawerScrimColor = ...,
        content = {
            ...
            scope.launch {
                scaffoldState.drawerState.open()
            }
        }
    )

### M3

    import androidx.compose.material3.DrawerValue
    import androidx.compose.material3.ModalDrawerSheet
    import androidx.compose.material3.ModalNavigationDrawer
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.rememberDrawerState

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = ...,
                drawerTonalElevation = ...,
                drawerContainerColor = ...,
                drawerContentColor = ...,
                content = { ... }
            )
        },
        gesturesEnabled = ...,
        scrimColor = ...,
        content = {
            Scaffold(
                content = {
                    ...
                    scope.launch {
                        drawerState.open()
                    }
                }
            )
        }
    )

### Top app bar

![Comparison of M2 and M3 scaffold with top app bar and scrolled list](https://developer.android.com/static/develop/ui/compose/images/migration-topbar-update.png) **Figure 6**. M2 scaffold with top app bar and scrolled list (left) versus M3 scaffold with top app bar and scrolled list (right)

[Top app bars](https://m3.material.io/components/top-app-bar/overview) in M3 are different to those in M2. In both M2
and M3, the main top app bar composable is named `TopAppBar` but the import
packages and parameters differ:

### M2

    import androidx.compose.material.TopAppBar

    TopAppBar(...)

### M3

    import androidx.compose.material3.TopAppBar

    TopAppBar(...)

Consider using the M3 [`CenterAlignedTopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/CenterAlignedTopAppBar.composable) if you were previously
centering content within the M2 `TopAppBar`. It's good to be aware of the
[`MediumTopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/MediumTopAppBar.composable) and [`LargeTopAppBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/LargeTopAppBar.composable) as well.

M3 top app bars contain a new `scrollBehavior` parameter to provide different
functionality on scroll through the [`TopAppBarScrollBehavior`](https://developer.android.com/reference/kotlin/androidx/compose/material3/TopAppBarScrollBehavior) class, such
as changing elevation. This works in conjunction with scrolling content using
[`Modifier.nestedScroll`](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/nestedscroll/package-summary#nestedscroll). This was possible in the M2 `TopAppBar` by
manually changing the `elevation` parameter:

### M2

    import androidx.compose.material.AppBarDefaults
    import androidx.compose.material.Scaffold
    import androidx.compose.material.TopAppBar

    val state = rememberLazyListState()
    val isAtTop by remember {
        derivedStateOf {
            state.firstVisibleItemIndex == 0 && state.firstVisibleItemScrollOffset == 0
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                elevation = if (isAtTop) {
                    0.dp
                } else {
                    AppBarDefaults.TopAppBarElevation
                },
                ...
            )
        },
        content = {
            LazyColumn(state = state) { ... }
        }
    )

### M3

    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.TopAppBar
    import androidx.compose.material3.TopAppBarDefaults

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                ...
            )
        },
        content = {
            LazyColumn { ... }
        }
    )

### Bottom navigation and the navigation bar

![Comparison of M2 bottom navigation and M3 navigation bar](https://developer.android.com/static/develop/ui/compose/images/migration-bottomnav-update.png) **Figure 7**. M2 bottom navigation (left) versus M3 navigation bar (right).

Bottom navigation in M2 has been renamed to [navigation bar](https://m3.material.io/components/navigation-bar/overview) in
M3. In M2 there are the [`BottomNavigation`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomNavigation.composable) and
[`BottomNavigationItem`](https://developer.android.com/reference/kotlin/androidx/compose/material/BottomNavigationItem.composable) composables, while in M3 there are the
[`NavigationBar`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationBar.composable) and [`NavigationBarItem`](https://developer.android.com/reference/kotlin/androidx/compose/material3/NavigationBarItem.composable) composables:

### M2

    import androidx.compose.material.BottomNavigation
    import androidx.compose.material.BottomNavigationItem

    BottomNavigation {
        BottomNavigationItem(...)
        BottomNavigationItem(...)
        BottomNavigationItem(...)
    }

### M3

    import androidx.compose.material3.NavigationBar
    import androidx.compose.material3.NavigationBarItem

    NavigationBar {
        NavigationBarItem(...)
        NavigationBarItem(...)
        NavigationBarItem(...)
    }

### Buttons, icon buttons and FABs

![Comparison of M2 and M3 buttons](https://developer.android.com/static/develop/ui/compose/images/migration-buttons-update.png) **Figure 8**. M2 buttons (left) versus M3 buttons (right)

[Buttons, icon buttons and floating action buttons (FABs)](https://m3.material.io/components/all-buttons) in M3
are different to those in M2. M3 includes all of the M2 button composables:

### M2

    import androidx.compose.material.Button
    import androidx.compose.material.ExtendedFloatingActionButton
    import androidx.compose.material.FloatingActionButton
    import androidx.compose.material.IconButton
    import androidx.compose.material.IconToggleButton
    import androidx.compose.material.OutlinedButton
    import androidx.compose.material.TextButton

    // M2 buttons
    Button(...)
    OutlinedButton(...)
    TextButton(...)
    // M2 icon buttons
    IconButton(...)
    IconToggleButton(...)
    // M2 FABs
    FloatingActionButton(...)
    ExtendedFloatingActionButton(...)

### M3

    import androidx.compose.material3.Button
    import androidx.compose.material3.ExtendedFloatingActionButton
    import androidx.compose.material3.FloatingActionButton
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.IconToggleButton
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.TextButton

    // M3 buttons
    Button(...)
    OutlinedButton(...)
    TextButton(...)
    // M3 icon buttons
    IconButton(...)
    IconToggleButton(...)
    // M3 FABs
    FloatingActionButton(...)
    ExtendedFloatingActionButton(...)

M3 also includes new button variations. Check them out on the [Compose Material
3 API reference overview](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#components).

### Switch

![Comparison of M2 and M3 switches](https://developer.android.com/static/develop/ui/compose/images/migration-switch-update.png) **Figure 9**. M2 switch (left) versus M3 switch (right).

[Switch](https://m3.material.io/components/switch/overview) in M3 is different to M2. In both M2 and M3, the switch
composable is named `Switch` but the import packages differ:

### M2

    import androidx.compose.material.Switch

    Switch(...)

### M3

    import androidx.compose.material3.Switch

    Switch(...)

## Surfaces and elevation

![Comparison of M2 surface elevation and M3 surface elevation in light and dark themes](https://developer.android.com/static/develop/ui/compose/images/migration-elevation-update.png) **Figure 10**. M2 surface elevation versus M3 surface elevation in light theme (left) and dark theme (right).

The surface and elevation systems in M3 are different to M2. There are two types
of elevation in M3:

- Shadow elevation (casts a shadow, same as M2)
- Tonal elevation (overlays a color, new to M3)

In Compose, this applies to the M2 [`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material/Surface.composable) function and the M3
[`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material3/Surface.composable) function:

### M2

    import androidx.compose.material.Surface

    Surface(
        elevation = ...
    ) { ... }

### M3

    import androidx.compose.material3.Surface

    Surface(
        shadowElevation = ...,
        tonalElevation = ...
    ) { ... }

You can use the `elevation` [`Dp`](https://developer.android.com/reference/kotlin/androidx/compose/ui/unit/Dp) values in M2 for both `shadowElevation`
and/or `tonalElevation` in M3, depending on the UX/UI design preference.
`Surface` is the backing composable behind most components, so component
composables might also expose elevation parameters you must migrate in the same
way.

> [!NOTE]
> **Note:** The default color used by `tonalElevation` in M3 is `primary`. You can override this by using the `surfaceTint` parameter in the `ColorScheme` class.

Tonal elevation in M3 replaces the concept of elevation overlays in M2 dark
themes. As a result, [`ElevationOverlay`](https://developer.android.com/reference/kotlin/androidx/compose/material/ElevationOverlay) and [`LocalElevationOverlay`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalElevationOverlay())
don't exist in M3, and [`LocalAbsoluteElevation`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalAbsoluteElevation()) in M2 has changed to
[`LocalAbsoluteTonalElevation`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#LocalAbsoluteTonalElevation()) in M3.

## Emphasis and content alpha

![Comparison of M2 and M3 icon and text emphasis](https://developer.android.com/static/develop/ui/compose/images/migration-emphasis-update.png) **Figure 11**. M2 icon and text emphasis (left) versus M3 icon and text emphasis (right)

Emphasis in M3 is significantly different to M2. In M2, emphasis involved using
*on* colors with certain alpha values to differentiate content like text and
icons. In M3, there are now a couple different approaches:

- Using *on* colors alongside their *variant on* colors from the expanded M3 color system.
- Using different font weights for text.

As a result, [`ContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha) and [`LocalContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentAlpha()) don't exist in
M3 and need to be replaced.

> [!NOTE]
> **Note:** For disabled states in M3, it's still acceptable to use *on* colors with alpha values, like in M2. For more information, see [Color roles](https://m3.material.io/styles/color/roles#19e75989-7485-4f5b-a769-940c4e4364bc).

The following mappings are recommended as a starting point:

| M2 | M3 |
|---|---|
| `onSurface` with [`ContentAlpha.high`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#high()) | `onSurface` in general, [`FontWeight.Medium`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/FontWeight#Medium()) - [`FontWeight.Black`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/FontWeight#Black()) for text |
| `onSurface` with [`ContentAlpha.medium`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#medium()) | `onSurfaceVariant` in general, [`FontWeight.Thin`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/FontWeight#Thin()) - [`FontWeight.Normal`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/FontWeight#Normal()) for text |
| `onSurface` with [`ContentAlpha.disabled`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#high()) | `onSurface.copy(alpha = 0.38f)` |

Here's an example of icon emphasis in M2 versus M3:

### M2

    import androidx.compose.material.ContentAlpha
    import androidx.compose.material.LocalContentAlpha

    // High emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Icon(...)
    }
    // Medium emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Icon(...)
    }
    // Disabled emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
        Icon(...)
    }

### M3

    import androidx.compose.material3.LocalContentColor

    // High emphasis
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Icon(...)
    }
    // Medium emphasis
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
        Icon(...)
    }
    // Disabled emphasis
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) {
        Icon(...)
    }

Here are examples of text emphasis in M2 and M3:

### M2

    import androidx.compose.material.ContentAlpha
    import androidx.compose.material.LocalContentAlpha

    // High emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.high) {
        Text(...)
    }
    // Medium emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
        Text(...)
    }
    // Disabled emphasis
    CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
        Text(...)
    }

### M3

    import androidx.compose.material3.LocalContentColor

    // High emphasis
    Text(
        ...,
        fontWeight = FontWeight.Bold
    )
    // Medium emphasis
    Text(
        ...,
        fontWeight = FontWeight.Normal
    )
    // Disabled emphasis
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)) {
        Text(
            ...,
            fontWeight = FontWeight.Normal
        )
    }

## Backgrounds and containers

Backgrounds in M2 are named containers in M3. In general, you can replace
`background*` parameters in M2 with `container*` in M3, using the same values.
For example:

### M2

    Badge(
        backgroundColor = MaterialTheme.colors.primary
    ) { ... }

### M3

    Badge(
        containerColor = MaterialTheme.colorScheme.primary
    ) { ... }

## Useful links

To learn more about migrating from M2 to M3 in Compose, consult the following
additional resources.

### Docs

- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)

### Sample apps

- [Reply M3 sample app](https://github.com/android/compose-samples/tree/main/Reply)
- Jetchat sample app M2 to M3 migration
  - [Initial alpha01 migration](https://github.com/android/compose-samples/pull/690)
  - [Follow-up alpha10 update](https://github.com/android/compose-samples/pull/798)
- [Jetnews sample app M2 to M3 migration](https://github.com/android/compose-samples/pull/964)
- [Now in Android M3 hero app :core-designsystem module](https://github.com/android/nowinandroid/tree/main/core/designsystem)

### Videos

- [Implementing Material You Using Jetpack Compose](https://www.youtube.com/watch?v=jrfuHyMlehc&t=5s)

### API reference and source code

- [Compose Material 3 API reference](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary)
- [Compose Material 3 samples in source code](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/samples/src/main/java/androidx/compose/material3/samples/)

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Material Design 2 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)
- [Custom design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems/custom)