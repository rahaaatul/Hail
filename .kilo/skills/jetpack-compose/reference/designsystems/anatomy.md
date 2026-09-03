Themes in Jetpack Compose are made up of a number of lower-level constructs
and related APIs. These can be seen in the
[source code](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/MaterialTheme.kt)
of `MaterialTheme` and can also be applied in custom design systems.

## Theme system classes

A theme is typically made up of a number of subsystems that group common visual and
behavioral concepts. These systems can be modeled with classes which have
theming values.

For example, `MaterialTheme` includes
[`ColorScheme`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ColorScheme.kt)
(color system),
[`Typography`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Typography.kt)
(typography system), and
[`Shapes`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/Shapes.kt)
(shape system).

> [!NOTE]
> **Note:** Classes should be annotated with [`Stable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Stable) or [`@Immutable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Immutable) to provide information to the Compose compiler. To learn more, check out the [Lifecycle of composables guide](https://developer.android.com/develop/ui/compose/lifecycle#skipping).


```kotlin
@Immutable
data class ColorSystem(
    val color: Color,
    val gradient: List<Color>
    /* ... */
)

@Immutable
data class TypographySystem(
    val fontFamily: FontFamily,
    val textStyle: TextStyle
)
/* ... */

@Immutable
data class CustomSystem(
    val value1: Int,
    val value2: String
    /* ... */
)

/* ... */
```

<br />

## Theme system composition locals

Theme system classes are implicitly provided to the composition tree as
[`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal)
instances. This allows theming values to be statically referenced in composable
functions.

To learn more about `CompositionLocal`, check out the
[Locally scoped data with CompositionLocal guide](https://developer.android.com/develop/ui/compose/compositionlocal).

> [!NOTE]
> **Note:** You can create a class's `CompositionLocal` with [`compositionLocalOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#compositionlocalof) or [`staticCompositionLocalOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#staticcompositionlocalof). These functions have a `defaultFactory` trailing lambda to provide fallback values of the same type that they're providing. It's a good idea to use reasonable defaults like `Color.Unspecified`, `TextStyle.Default`, etc.


```kotlin
val LocalColorSystem = staticCompositionLocalOf {
    ColorSystem(
        color = Color.Unspecified,
        gradient = emptyList()
    )
}

val LocalTypographySystem = staticCompositionLocalOf {
    TypographySystem(
        fontFamily = FontFamily.Default,
        textStyle = TextStyle.Default
    )
}

val LocalCustomSystem = staticCompositionLocalOf {
    CustomSystem(
        value1 = 0,
        value2 = ""
    )
}

/* ... */
```

<br />

## Theme function

The theme function is the entry point and primary API. It constructs instances
of the theme system `CompositionLocal`s --- using real values and any logic
required --- that are provided to the composition tree with
[`CompositionLocalProvider`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#compositionlocalprovider).
The `content` parameter allows nested composables to access theming values
relative to the hierarchy.


```kotlin
@Composable
fun Theme(
    /* ... */
    content: @Composable () -> Unit
) {
    val colorSystem = ColorSystem(
        color = Color(0xFF3DDC84),
        gradient = listOf(Color.White, Color(0xFFD7EFFF))
    )
    val typographySystem = TypographySystem(
        fontFamily = FontFamily.Monospace,
        textStyle = TextStyle(fontSize = 18.sp)
    )
    val customSystem = CustomSystem(
        value1 = 1000,
        value2 = "Custom system"
    )
    /* ... */
    CompositionLocalProvider(
        LocalColorSystem provides colorSystem,
        LocalTypographySystem provides typographySystem,
        LocalCustomSystem provides customSystem,
        /* ... */
        content = content
    )
}
```

<br />

## Theme object

Accessing theme systems is done using an object with convenience properties. For
consistency, the object tends to be named the same as the theme function. The
properties simply get the current composition local.


```kotlin
// Use with eg. Theme.colorSystem.color
object Theme {
    val colorSystem: ColorSystem
        @Composable
        get() = LocalColorSystem.current
    val typographySystem: TypographySystem
        @Composable
        get() = LocalTypographySystem.current
    val customSystem: CustomSystem
        @Composable
        get() = LocalCustomSystem.current
    /* ... */
}
```

<br />

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Locally scoped data with CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal)
- [Custom design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems/custom)
- [Material Design 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3)