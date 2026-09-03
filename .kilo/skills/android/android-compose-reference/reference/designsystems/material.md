> [!CAUTION]
> **Caution:** Material 3 is the latest set of Material Components for Compose. New apps should use [Material 3](https://developer.android.com/develop/ui/compose/designsystems/material3) instead. This guidance is for Material 2. For information on how to migrate to Material 3, see the [migration
> guidance](https://developer.android.com/develop/ui/compose/designsystems/material2-material3).

Jetpack Compose offers an implementation of [Material Design](https://material.io/design/introduction), a
comprehensive design system for creating digital interfaces. The [Material
Design components](https://material.io/components) (buttons, cards, switches, and so on) are
built on top of [Material Theming](https://material.io/design/material-theming/), which is a systematic way to
customize Material Design to better reflect your product's brand. A Material
Theme contains [color](https://material.io/design/color/), [typography](https://material.io/design/typography/), and
[shape](https://material.io/design/shape/) attributes. When you customize these attributes, your
changes are automatically reflected in the components you use to build your app.

Jetpack Compose implements these concepts with the [`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/MaterialTheme.composable#MaterialTheme(androidx.compose.material.Colors,androidx.compose.material.Typography,androidx.compose.material.Shapes,kotlin.Function0))
composable:


```kotlin
MaterialTheme(
    colors = // ...
    typography = // ...
    shapes = // ...
) {
    // app content
}
```

<br />

Configure the parameters you pass to `MaterialTheme` to theme your application.
![Two contrasting screenshots. The first uses default MaterialTheme styling, the second screenshot uses modified styling.](https://developer.android.com/static/develop/ui/compose/images/theme-two-themes.png) **Figure 1.** The first screenshot shows an app that does not configure \`MaterialTheme\`, and so it uses default styling. The second screenshot shows an app that passes parameters to \`MaterialTheme\` to customize the styling.

## Color

Colors are modeled in Compose with the [`Color`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/Color) class, a data-holding class.


```kotlin
val Red = Color(0xffff0000)
val Blue = Color(red = 0f, green = 0f, blue = 1f)
```

<br />

While you can organize these however you like (as top-level constants, within a
singleton, or defined inline), we **strongly** recommend specifying colors in
your theme and retrieving the colors from there. This approach makes it possible
to support [dark theme](https://developer.android.com/guide/topics/ui/look-and-feel/darktheme) and nested themes.
![Example of theme's color palette](https://developer.android.com/static/develop/ui/compose/images/theme-colors.png) **Figure 2.** The Material color system.

Compose provides the [`Colors`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors) class to model the [Material color
system](https://material.io/design/color/). `Colors` provides builder functions to create sets of
[light](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#lightColors(androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color,%20androidx.compose.ui.graphics.Color)) or [dark](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#darkColors(androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color)) colors:


```kotlin
private val Yellow200 = Color(0xffffeb46)
private val Blue200 = Color(0xff91a4fc)
// ...

private val DarkColors = darkColors(
    primary = Yellow200,
    secondary = Blue200,
    // ...
)
private val LightColors = lightColors(
    primary = Yellow500,
    primaryVariant = Yellow400,
    secondary = Blue700,
    // ...
)
```

<br />

After you have defined your `Colors`, you can pass them to a `MaterialTheme`:


```kotlin
MaterialTheme(
    colors = if (darkTheme) DarkColors else LightColors
) {
    // app content
}
```

<br />

### Use theme colors

You can retrieve the `Colors` provided to the `MaterialTheme` composable by
using `MaterialTheme.colors`.


```kotlin
Text(
    text = "Hello theming",
    color = MaterialTheme.colors.primary
)
```

<br />

> [!NOTE]
> **Note:** If you have multiple nested `MaterialTheme` composables, then this retrieves the `Colors` instance for the `MaterialTheme` composable at the current point in the tree.

### Surface and content color

Many components accept a pair of color and content color:


```kotlin
Surface(
    color = MaterialTheme.colors.surface,
    contentColor = contentColorFor(color),
    // ...
) { /* ... */ }

TopAppBar(
    backgroundColor = MaterialTheme.colors.primarySurface,
    contentColor = contentColorFor(backgroundColor),
    // ...
) { /* ... */ }
```

<br />

This lets you not only set the color of a composable, but also provide a
default color for the *content,* the composables contained within it. Many
composables use this content color by default. For example, `Text` bases its
color on its parent's content color, and `Icon` uses that color to set its tint.
![Two examples of the same banner, with different colors](https://developer.android.com/static/develop/ui/compose/images/theme-contrast-styles.png) **Figure 3.** Setting different background colors produces different text and icon colors.

The [`contentColorFor()`](https://developer.android.com/reference/kotlin/androidx/compose/material/contentColorFor.composable#contentColorFor(androidx.compose.ui.graphics.Color)) method retrieves the appropriate "on" color for
any theme colors. For example, if you set a [`primary`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors#primary()) background color on
`Surface`, it uses this function to set [`onPrimary`](https://developer.android.com/reference/kotlin/androidx/compose/material/Colors#onPrimary()) as the content color.
If you set a non-theme background color, you should also specify an appropriate
content color. Use [`LocalContentColor`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentColor()) to retrieve the preferred content
color for the current background, at a given position in the hierarchy.

> [!NOTE]
> **Note:** If you need to set the background color of an element, prefer using a parent [`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material/Surface.composable#Surface(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.foundation.BorderStroke,androidx.compose.ui.unit.Dp,kotlin.Function0)) to do this, which sets an appropriate content color. Be wary of direct [`Modifier.background()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/background.modifier#(androidx.compose.ui.Modifier).background(androidx.compose.ui.graphics.Brush,androidx.compose.ui.graphics.Shape,kotlin.Float)) calls, which don't set an appropriate content color.

### Content alpha

Often you want to vary how much you emphasize content to communicate importance
and provide visual hierarchy. The [Material Design text legibility
recommendations](https://material.io/design/color/text-legibility.html) advise employing different levels of opacity
to convey different importance levels.

Jetpack Compose implements this using [`LocalContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentAlpha()). You can specify
a content alpha for a hierarchy by [providing a value](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocalProvider.composable#CompositionLocalProvider(kotlin.Array,kotlin.Function0)) for this
[`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal). Nested composables can use this value to apply the
alpha treatment to their content. For example, [`Text`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#Text(kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.font.FontStyle,androidx.compose.ui.text.font.FontWeight,androidx.compose.ui.text.font.FontFamily,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextDecoration,androidx.compose.ui.text.style.TextAlign,androidx.compose.ui.unit.TextUnit,androidx.compose.ui.text.style.TextOverflow,kotlin.Boolean,kotlin.Int,kotlin.Function1,androidx.compose.ui.text.TextStyle)) and [`Icon`](https://developer.android.com/reference/kotlin/androidx/compose/material/Icon.composable#Icon(androidx.compose.ui.graphics.painter.Painter,kotlin.String,androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Color)) by
default use the combination of [`LocalContentColor`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentColor()) adjusted to use
[`LocalContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentAlpha()). Material specifies some standard alpha values
([`high`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#high()), [`medium`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#medium()), [`disabled`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#disabled())) which are modeled by the
[`ContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha) object.


```kotlin
// By default, both Icon & Text use the combination of LocalContentColor &
// LocalContentAlpha. De-emphasize content by setting content alpha
CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
    Text(
        // ...
    )
}
CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.disabled) {
    Icon(
        // ...
    )
    Text(
        // ...
    )
}
```

<br />

> [!NOTE]
> **Note:** [`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/MaterialTheme.composable#MaterialTheme(androidx.compose.material.Colors,androidx.compose.material.Typography,androidx.compose.material.Shapes,kotlin.Function0)) defaults [`LocalContentAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalContentAlpha()) to [`ContentAlpha.high`](https://developer.android.com/reference/kotlin/androidx/compose/material/ContentAlpha#high()).

To learn more about `CompositionLocal`, see [Locally scoped data with
CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal).
![Screenshot of an article title, showing different levels of text emphasis](https://developer.android.com/static/develop/ui/compose/images/theme-emphasis.png) **Figure 4.** Apply different levels of emphasis to text to visually communicate the information hierarchy. The first line of text is the title and has the most important information, and thus uses `ContentAlpha.high`. The second line contains less-important metadata, and thus uses `ContentAlpha.medium`.

### Dark theme

In Compose, you implement light and dark themes by providing different sets of
`Colors` to the `MaterialTheme` composable:


```kotlin
@Composable
fun MyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = if (darkTheme) DarkColors else LightColors,
        /*...*/
        content = content
    )
}
```

<br />

In this example, `MaterialTheme` is wrapped in its own composable function,
which accepts a parameter that specifies whether to use a dark theme or not. In
this case, the function gets the default value for `darkTheme` by querying the
[device theme setting](https://developer.android.com/reference/kotlin/androidx/compose/foundation/isSystemInDarkTheme.composable#isSystemInDarkTheme()).

You can use code like this to check if the current `Colors` are light or dark:


```kotlin
val isLightTheme = MaterialTheme.colors.isLight
Icon(
    painterResource(
        id = if (isLightTheme) {
            R.drawable.ic_sun_24
        } else {
            R.drawable.ic_moon_24
        }
    ),
    contentDescription = "Theme"
)
```

<br />

#### Elevation overlays

In Material, surfaces in dark themes with higher elevations receive [elevation
overlays](https://material.io/design/color/dark-theme.html#properties), which lighten their background. The higher a
surface's elevation (raising it closer to an implied light source), the lighter
that surface becomes.

The `Surface` composable applies these overlays automatically when using
dark colors, and so does any other Material composable which uses a surface:


```kotlin
Surface(
    elevation = 2.dp,
    color = MaterialTheme.colors.surface, // color will be adjusted for elevation
    /*...*/
) { /*...*/ }
```

<br />

![Screenshot of an app, showing the subtly different colors used for elements at different elevation levels](https://developer.android.com/static/develop/ui/compose/images/theme-elevation.png) **Figure 5.** The cards and bottom navigation are both using the `surface` color as their background. Since the cards and bottom navigation are at different elevation levels above the background, they have slightly different colors--the cards are lighter than the background and the bottom navigation is lighter than the cards.

For custom scenarios that don't involve a `Surface`, use
[`LocalElevationOverlay`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#LocalElevationOverlay()), a `CompositionLocal` containing the
[`ElevationOverlay`](https://developer.android.com/reference/kotlin/androidx/compose/material/ElevationOverlay) used by [`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#surface) components:


```kotlin
// Elevation overlays
// Implemented in Surface (and any components that use it)
val color = MaterialTheme.colors.surface
val elevation = 4.dp
val overlaidColor = LocalElevationOverlay.current?.apply(
    color, elevation
)
```

<br />

To disable elevation overlays, provide `null` at the chosen point in a
composable hierarchy:


```kotlin
MyTheme {
    CompositionLocalProvider(LocalElevationOverlay provides null) {
        // Content without elevation overlays
    }
}
```

<br />

#### Limited color accents

Material recommends applying [limited color accents](https://material.io/design/color/dark-theme.html#anatomy) for dark
themes by preferring the use of the `surface` color over the `primary` color in
most cases. Material composables like `TopAppBar` and `BottomNavigation`
implement this behavior by default.
![Screenshot of a Material dark theme, showing the top app bar using surface color instead of primary color for limited color accents](https://developer.android.com/static/develop/ui/compose/images/themes-material-dark-theme.png) **Figure 6.** Material dark theme with limited color accents. The top app bar uses the primary color in light theme, and surface color in dark theme.

For custom scenarios, use the [`primarySurface`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#(androidx.compose.material.Colors).primarySurface()) extension property:


```kotlin
Surface(
    // Switches between primary in light theme and surface in dark theme
    color = MaterialTheme.colors.primarySurface,
    /*...*/
) { /*...*/ }
```

<br />

## Typography

Material defines a [type system](https://material.io/design/typography/the-type-system.html), encouraging you to use a small
number of semantically-named styles.
![Example of several different typefaces in various styles](https://developer.android.com/static/develop/ui/compose/images/theme-typefaces.png) **Figure 7.** The Material type system.

Compose implements the type system with the [`Typography`](https://developer.android.com/reference/kotlin/androidx/compose/material/Typography),
[`TextStyle`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/TextStyle), and [font-related](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/font/package-summary) classes. The `Typography` constructor
offers defaults for each style so you can omit any you don't want to customize:


```kotlin
val raleway = FontFamily(
    Font(R.font.raleway_regular),
    Font(R.font.raleway_medium, FontWeight.W500),
    Font(R.font.raleway_semibold, FontWeight.SemiBold)
)

val myTypography = Typography(
    h1 = TextStyle(
        fontFamily = raleway,
        fontWeight = FontWeight.W300,
        fontSize = 96.sp
    ),
    body1 = TextStyle(
        fontFamily = raleway,
        fontWeight = FontWeight.W600,
        fontSize = 16.sp
    )
    /*...*/
)
MaterialTheme(typography = myTypography, /*...*/) {
    /*...*/
}
```

<br />

If you want to use the same typeface throughout, specify the
[`defaultFontFamily`](https://developer.android.com/reference/kotlin/androidx/compose/material/Typography#public-constructors) parameter and omit the `fontFamily` of any
`TextStyle` elements:


```kotlin
val typography = Typography(defaultFontFamily = raleway)
MaterialTheme(typography = typography, /*...*/) {
    /*...*/
}
```

<br />

### Use text styles

`TextStyle` elements are accessed using `MaterialTheme.typography`. Retrieve
the `TextStyle` elements as follows:


```kotlin
Text(
    text = "Subtitle2 styled",
    style = MaterialTheme.typography.subtitle2
)
```

<br />

![Screenshot showing a mixture of different typefaces for different purposes](https://developer.android.com/static/develop/ui/compose/images/theme-typefaces-styles.png) **Figure 8.** Use a selection of typefaces and styles to express your brand.

## Shape

Material defines a [shape system](https://material.io/design/shape/about-shape.html), letting you define shapes for
large, medium, and small components.
![Shows a variety of Material Design shapes](https://developer.android.com/static/develop/ui/compose/images/theme-shapes.png) **Figure 9.** The Material shape system.

Compose implements the shape system with the [`Shapes`](https://developer.android.com/reference/kotlin/androidx/compose/material/Shapes) class, which lets
you specify a [`CornerBasedShape`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/shape/CornerBasedShape) for each size category:


```kotlin
val shapes = Shapes(
    small = RoundedCornerShape(percent = 50),
    medium = RoundedCornerShape(0f),
    large = CutCornerShape(
        topStart = 16.dp,
        topEnd = 0.dp,
        bottomEnd = 0.dp,
        bottomStart = 16.dp
    )
)

MaterialTheme(shapes = shapes, /*...*/) {
    /*...*/
}
```

<br />

Many components use these shapes by default. For example, [`Button`](https://developer.android.com/reference/kotlin/androidx/compose/material/Button.composable#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.ButtonElevation,androidx.compose.ui.graphics.Shape,androidx.compose.foundation.BorderStroke,androidx.compose.material.ButtonColors,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1)),
[`TextField`](https://developer.android.com/reference/kotlin/androidx/compose/material/TextField.composable#TextField(androidx.compose.foundation.text.input.TextFieldState,androidx.compose.ui.Modifier,kotlin.Boolean,kotlin.Boolean,androidx.compose.ui.text.TextStyle,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Function0,kotlin.Boolean,androidx.compose.foundation.text.input.InputTransformation,androidx.compose.foundation.text.input.OutputTransformation,androidx.compose.foundation.text.KeyboardOptions,androidx.compose.foundation.text.input.KeyboardActionHandler,androidx.compose.foundation.text.input.TextFieldLineLimits,androidx.compose.foundation.ScrollState,androidx.compose.ui.graphics.Shape,androidx.compose.material.TextFieldColors,androidx.compose.foundation.interaction.MutableInteractionSource)), and [`FloatingActionButton`](https://developer.android.com/reference/kotlin/androidx/compose/material/FloatingActionButton.composable#FloatingActionButton(kotlin.Function0,androidx.compose.ui.Modifier,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.material.FloatingActionButtonElevation,kotlin.Function0)) default to small,
[`AlertDialog`](https://developer.android.com/reference/kotlin/androidx/compose/material/AlertDialog.composable#AlertDialog(kotlin.Function0,kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Function0,kotlin.Function0,kotlin.Function0,androidx.compose.ui.graphics.Shape,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.window.DialogProperties)) defaults to medium, and [`ModalDrawer`](https://developer.android.com/reference/kotlin/androidx/compose/material/ModalDrawer.composable#ModalDrawer(kotlin.Function1,androidx.compose.ui.Modifier,androidx.compose.material.DrawerState,kotlin.Boolean,androidx.compose.ui.graphics.Shape,androidx.compose.ui.unit.Dp,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,androidx.compose.ui.graphics.Color,kotlin.Function0)) defaults to
large --- see the [shape scheme reference](https://material.io/design/shape/applying-shape-to-ui.html#shape-scheme) for the complete
mapping.

### Use shapes

`Shape` elements are accessed using `MaterialTheme.shapes`. Retrieve the
`Shape` elements with code like this:


```kotlin
Surface(
    shape = MaterialTheme.shapes.medium, /*...*/
) {
    /*...*/
}
```

<br />

![Screenshot of an app that uses Material shapes to convey what state an element is in](https://developer.android.com/static/develop/ui/compose/images/theme-shapes-example.png) **Figure 10.** Use shapes to express brand or state.

## Default styles

There is no equivalent concept in Compose of [default styles](https://developer.android.com/guide/topics/ui/look-and-feel/themes#Widgets) from Android
Views. You can provide similar functionality by creating your own `overload`
composable functions that wrap Material components. For example, to create a
style of button, wrap a button in your own composable function, directly setting
the parameters you want or need to alter and exposing others as parameters to
the containing composable.


```kotlin
@Composable
fun MyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            backgroundColor = MaterialTheme.colors.secondary
        ),
        onClick = onClick,
        modifier = modifier,
        content = content
    )
}
```

<br />

## Theme overlays

You can achieve the equivalent of [theme overlays](https://medium.com/androiddevelopers/android-styling-themes-overlay-1ffd57745207) from Android
Views in Compose by nesting [`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary#materialtheme) composables. Because
`MaterialTheme` defaults the colors, typography, and shapes to the current
theme value, all other parameters keep their default values when a theme only
sets one of those parameters.

Furthermore, when migrating View-based screens to Compose, watch out for usages
of the `android:theme` attribute. It's likely you need a new `MaterialTheme`
in that part of the Compose UI tree.

In this example, the details screen uses a `PinkTheme` for most of the screen,
and then a `BlueTheme` for the related section. The following screenshot and
code illustrate this concept:
![Screenshot of an app demonstrating nested themes, with a pink theme for the main screen and a blue theme for a related section](https://developer.android.com/static/develop/ui/compose/images/themes-material-overlays.png) **Figure 11.** Nested themes.


```kotlin
@Composable
fun DetailsScreen(/* ... */) {
    PinkTheme {
        // other content
        RelatedSection()
    }
}

@Composable
fun RelatedSection(/* ... */) {
    BlueTheme {
        // content
    }
}
```

<br />

## Component states

Material components that can be interacted with (clicked, toggled, etc.) can be
in different visual states. States include enabled, disabled, pressed, etc.

Composables often have an `enabled` parameter. Setting it to `false` prevents
interaction, and changes properties like color and elevation to visually convey
the component state.
![Screenshot of two buttons: one enabled, one disabled, showing their different visual states](https://developer.android.com/static/develop/ui/compose/images/themes-material-button-states.png) **Figure 12.** Button with `enabled = true` (left) and `enabled = false` (right).

In most cases you can rely on defaults for values like color and elevation. If
you need to configure values used in different states, there are classes
and convenience functions available. Consider the following button example:


```kotlin
Button(
    onClick = { /* ... */ },
    enabled = true,
    // Custom colors for different states
    colors = ButtonDefaults.buttonColors(
        backgroundColor = MaterialTheme.colors.secondary,
        disabledBackgroundColor = MaterialTheme.colors.onBackground
            .copy(alpha = 0.2f)
            .compositeOver(MaterialTheme.colors.background)
        // Also contentColor and disabledContentColor
    ),
    // Custom elevation for different states
    elevation = ButtonDefaults.elevation(
        defaultElevation = 8.dp,
        disabledElevation = 2.dp,
        // Also pressedElevation
    )
) { /* ... */ }
```

<br />

![Screenshot of two buttons with adjusted color and elevation for enabled and disabled states](https://developer.android.com/static/develop/ui/compose/images/themes-material-buttons-adjusted.png) **Figure 13.** Button with `enabled = true` (left) and `enabled = false` (right), with adjusted color and elevation values.

## Ripples

Material components use ripples to indicate they're being interacted with. If
you're using `MaterialTheme` in your hierarchy, a `Ripple` is used as the
default [`Indication`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/Indication) inside modifiers such as [`clickable`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/clickable.modifier#(androidx.compose.ui.Modifier).clickable(kotlin.Boolean,kotlin.String,androidx.compose.ui.semantics.Role,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function0)) and
[`indication`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/indication.modifier#(androidx.compose.ui.Modifier).indication(androidx.compose.foundation.interaction.InteractionSource,androidx.compose.foundation.Indication)).

In most cases you can rely on the default `Ripple`. If you need to
configure their appearance, you can use [`RippleTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/ripple/RippleTheme) to change properties
like color and alpha.

You can extend `RippleTheme` and make use of the [`defaultRippleColor`](https://developer.android.com/reference/kotlin/androidx/compose/material/ripple/RippleTheme#defaultRippleColor(androidx.compose.ui.graphics.Color,kotlin.Boolean)) and
[`defaultRippleAlpha`](https://developer.android.com/reference/kotlin/androidx/compose/material/ripple/RippleTheme#defaultRippleAlpha(androidx.compose.ui.graphics.Color,kotlin.Boolean)) utility functions. You can then provide your custom
ripple theme in your hierarchy using [`LocalRippleTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/ripple/package-summary#LocalRippleTheme()):


```kotlin
@Composable
fun MyApp() {
    MaterialTheme {
        CompositionLocalProvider(
            LocalRippleTheme provides SecondaryRippleTheme
        ) {
            // App content
        }
    }
}

@Immutable
private object SecondaryRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = MaterialTheme.colors.secondary,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = MaterialTheme.colors.secondary,
        lightTheme = MaterialTheme.colors.isLight
    )
}
```

<br />

![Animated GIF showing buttons with different ripple effects when tapped](https://developer.android.com/static/develop/ui/compose/images/themes-material-ripples.gif) **Figure 14.** Buttons with different ripple values provided using `RippleTheme`.

## Learn more

To learn more about Material Theming in Compose, consult the following
additional resources.

### Codelabs

- [Jetpack Compose theming](https://developer.android.com/codelabs/jetpack-compose-theming)

### Videos

- [Material You in Jetpack Compose](https://www.youtube.com/watch?v=jrfuHyMlehc)

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Custom design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems/custom)
- [Migrate from Material 2 to Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material2-material3)
- [Accessibility in Compose](https://developer.android.com/develop/ui/compose/accessibility)