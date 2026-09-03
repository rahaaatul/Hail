This page provides a high-level overview of the architectural layers that make
up Jetpack Compose, and the core principles that inform this design.

Jetpack Compose is not a single monolithic project; it is created from a number
of modules which are assembled together to form a complete stack. Understanding
the different modules that make up Jetpack Compose enables you to:

- Use the appropriate level of abstraction to build your app or library
- Understand when you can 'drop down' to a lower level for more control or customization
- Minimize your dependencies

## Layers

The major layers of Jetpack Compose are:

![](https://developer.android.com/static/develop/ui/compose/images/layering-major-layers.svg)

**Figure 1.** The major layers of Jetpack Compose.

Each layer is built upon the lower levels, combining functionality to create
higher level components. Each layer builds on public APIs of the lower layers
to verify the module boundaries and enable you to replace any layer should you
need to. Let's examine these layers from the bottom up.

[Runtime](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary)
:   This module provides the fundamentals of the Compose runtime such as
    [`remember`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/remember.composable#remember(kotlin.Function0)),
    [`mutableStateOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#mutableStateOf(kotlin.Any,androidx.compose.runtime.SnapshotMutationPolicy)),
    the
    [`@Composable`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/Composable)
    annotation and
    [`SideEffect`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/SideEffect.composable#SideEffect(kotlin.Function0)).
    You might consider building directly upon this layer if you only need
    Compose's tree management abilities, not its UI.

[UI](https://developer.android.com/reference/kotlin/androidx/compose/ui/package-summary)
:   The UI layer is made up of multiple modules (
    [`ui-text`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/package-summary),
    [`ui-graphics`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/package-summary),
    [`ui-tooling`](https://developer.android.com/reference/kotlin/androidx/compose/ui/tooling/package-summary),
    etc.). These modules implement the fundamentals of the UI toolkit, such as
    `LayoutNode`,
    [`Modifier`](https://developer.android.com/reference/kotlin/androidx/compose/ui/Modifier), input handlers,
    custom layouts, and drawing. You might consider building upon this layer if
    you only need fundamental concepts of a UI toolkit.

[Foundation](https://developer.android.com/reference/kotlin/androidx/compose/foundation/package-summary)
:   This module provides design system agnostic building blocks for Compose UI,
    like
    [`Row`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Row.composable#Row(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1))
    and
    [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)),
    [`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable),
    recognition of particular gestures, etc. You might consider building upon the
    foundation layer to create your own design system.

[Material](https://developer.android.com/reference/kotlin/androidx/compose/material/package-summary)
:   This module provides an implementation of the Material Design system for
    Compose UI, providing a theming system, styled components, ripple
    indications, icons. Build upon this layer when using Material Design in your
    app.

## Design principles

A guiding principle for Jetpack Compose is to provide small, focused pieces of
functionality that can be assembled (or composed) together, rather than a few
monolithic components. This approach has a number of advantages.

### Control

Higher level components tend to do more for you, but limit the amount of direct
control that you have. If you need more control, you can "drop down" to use a
lower level component.

For example, if you want to animate the color of a component you might use the
[`animateColorAsState`](https://developer.android.com/reference/kotlin/androidx/compose/animation/package-summary#animateColorAsState(androidx.compose.ui.graphics.Color,androidx.compose.animation.core.AnimationSpec,kotlin.Function1))
API:


```kotlin
val color = animateColorAsState(if (condition) Color.Green else Color.Red)
```

<br />

However, if you needed the component to always start out grey, you cannot
do it with this API. Instead, you can drop down to use the lower level
[`Animatable`](https://developer.android.com/reference/kotlin/androidx/compose/animation/core/Animatable)
API:


```kotlin
val color = remember { Animatable(Color.Gray) }
LaunchedEffect(condition) {
    color.animateTo(if (condition) Color.Green else Color.Red)
}
```

<br />

The higher level `animateColorAsState` API is itself built upon the lower level
`Animatable` API. Using the lower level API is more complex but offers more
control. Choose the level of abstraction that best suits your needs.

### Customization

Assembling higher level components from smaller building blocks makes it far
easier to customize components should you need to. For example, consider the
[implementation](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/material/material/src/commonMain/kotlin/androidx/compose/material/Button.kt)
of
[`Button`](https://developer.android.com/reference/kotlin/androidx/compose/material/Button.composable#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.ButtonElevation,androidx.compose.ui.graphics.Shape,androidx.compose.foundation.BorderStroke,androidx.compose.material.ButtonColors,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1))
provided by the Material layer:


```kotlin
@Composable
fun Button(
    // ...
    content: @Composable RowScope.() -> Unit
) {
    Surface(/* ... */) {
        CompositionLocalProvider(/* ... */) { // set LocalContentAlpha
            ProvideTextStyle(MaterialTheme.typography.button) {
                Row(
                    // ...
                    content = content
                )
            }
        }
    }
}
```

<br />

A `Button` is assembled from 4 components:

1. A material
   [`Surface`](https://developer.android.com/reference/kotlin/androidx/compose/material/Surface.composable)
   providing the background, shape, click handling, etc.

2. A
   [`CompositionLocalProvider`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocalProvider.composable#CompositionLocalProvider(kotlin.Array,kotlin.Function0))
   which changes the content's alpha when the button is enabled or disabled

3. A
   [`ProvideTextStyle`](https://developer.android.com/reference/kotlin/androidx/compose/material/ProvideTextStyle.composable#ProvideTextStyle(androidx.compose.ui.text.TextStyle,kotlin.Function0))
   sets the default text style to use

4. A `Row` provides the default layout policy for the button's content

We have omitted some parameters and comments to make the structure clearer, but
the entire component is only around 40 lines of code because it simply
assembles these 4 components to implement the button. Components like `Button`
are opinionated about which parameters they expose, balancing enabling common
customizations against an explosion of parameters that can make a component
harder to use. Material components, for example, offer customizations specified
in the Material Design system, making it easy to follow material design
principles.

If, however, you wish to make a customization beyond a component's parameters,
then you can "drop down" a level and fork a component. For example, Material
Design specifies that buttons should have a solid colored background. If you
need a gradient background, this option is not supported by the `Button`
parameters. In this case you can use the Material `Button` implementation as a
reference and build your own component:


```kotlin
@Composable
fun GradientButton(
    // ...
    background: List<Color>,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        // ...
        modifier = modifier
            .clickable(onClick = {})
            .background(
                Brush.horizontalGradient(background)
            )
    ) {
        CompositionLocalProvider(/* ... */) { // set material LocalContentAlpha
            ProvideTextStyle(MaterialTheme.typography.button) {
                content()
            }
        }
    }
}
```

<br />

The above implementation continues to use components from the Material layer,
such as Material's concepts of
[current content alpha](https://developer.android.com/develop/ui/compose/designsystems/material3#emphasis)
and the current text style. However, it replaces the material `Surface` with a
`Row` and styles it to achieve the desired appearance.

> [!CAUTION]
> **Caution:** When dropping down to a lower layer to customize a component, ensure that you do not degrade any functionality by, for example, neglecting accessibility support. Use the component you are forking as a guide.

If you do not want to use Material concepts at all, for example if building your
own bespoke design system, then you can drop down to purely using foundation
layer components:


```kotlin
@Composable
fun BespokeButton(
    // ...
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        // ...
        modifier = modifier
            .clickable(onClick = {})
            .background(backgroundColor)
    ) {
        // No Material components used
        content()
    }
}
```

<br />

Jetpack Compose reserves the simplest names for the highest level components. For example,
[`androidx.compose.material.Text`](https://developer.android.com/reference/kotlin/androidx/compose/material/Text.composable)
is built upon
[`androidx.compose.foundation.text.BasicText`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/text/BasicText.composable).
This makes it possible to provide your own implementation with the most
discoverable name if you wish to replace higher levels.

> [!CAUTION]
> **Caution:** Forking a component means that you will not benefit from any future additions or bug fixes from the upstream component.

### Picking the right abstraction

Compose's philosophy of building layered, reusable components means that you
should not always reach for the lower level building blocks. Many higher level
components not only offer more functionality but often implement best practices
such as supporting accessibility.

For example, if you wanted to add gesture support to your custom component, you
could build this from scratch using
[`Modifier.pointerInput`](https://developer.android.com/reference/kotlin/androidx/compose/ui/input/pointer/package-summary#(androidx.compose.ui.Modifier).pointerInput(kotlin.Any,kotlin.coroutines.SuspendFunction1))
but there are other, higher level components built on top of this which may
offer a better starting point, for example
[`Modifier.draggable`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/draggable.modifier#(androidx.compose.ui.Modifier).draggable(androidx.compose.foundation.gestures.DraggableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Boolean,kotlin.coroutines.SuspendFunction2,kotlin.coroutines.SuspendFunction2,kotlin.Boolean)),
[`Modifier.scrollable`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/gestures/scrollable.modifier#(androidx.compose.ui.Modifier).scrollable(androidx.compose.foundation.gestures.ScrollableState,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,kotlin.Boolean,androidx.compose.foundation.gestures.FlingBehavior,androidx.compose.foundation.interaction.MutableInteractionSource))
or [`Modifier.swipeable`](https://developer.android.com/reference/kotlin/androidx/compose/material/swipeable.modifier#(androidx.compose.ui.Modifier).swipeable(androidx.compose.material.SwipeableState,kotlin.collections.Map,androidx.compose.foundation.gestures.Orientation,kotlin.Boolean,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function2,androidx.compose.material.ResistanceConfig,androidx.compose.ui.unit.Dp)).

As a rule, prefer building on the *highest-level* component which offers the
functionality you need in order to benefit from the best practices they
include.

### Learn more

See the
[Jetsnack sample](https://github.com/android/compose-samples/tree/main/Jetsnack)
for an example of building a custom design system.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Kotlin for Jetpack Compose](https://developer.android.com/develop/ui/compose/kotlin)
- [Lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)