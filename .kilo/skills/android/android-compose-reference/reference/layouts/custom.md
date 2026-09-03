[Video](https://www.youtube.com/watch?v=xcfEQO0k_gU)

In Compose, UI elements are represented by the composable functions that emit a
piece of UI when invoked, that is then added to a UI tree that gets rendered on
the screen. Each UI element has one parent and potentially many children. Each
element is also located within its parent, specified as an (x, y) position, and
a size, specified as a `width` and a `height`.

Parents define the constraints for their child elements. An element is asked to
define its size within those constraints. Constraints restrict the minimum and
maximum `width` and `height` of an element. If an element has child elements, it
may measure each child to help determine its size. Once an element determines
and reports its own size, it has an opportunity to define how to place its child
elements relative to itself, as described in detail in [Creating custom
layouts](https://developer.android.com/develop/ui/compose/layouts/custom#create-custom).

Laying out each node in the UI tree is a three step process. Each node must:

1. Measure any children
2. Decide its own size
3. Place its children

![Three steps of node layout: measure children, decide size, place children](https://developer.android.com/static/develop/ui/compose/images/layout-three-step-process.svg) **Figure 1.** The three steps of node layout are to measure children, decide size, and place children.

> [!NOTE]
> **Note:** Compose UI does not permit multi-pass measurement. This means that a layout element may not measure any of its children more than once in order to try different measurement configurations.

The use of scopes defines *when* you can measure and place your children.
Measuring a layout can only be done during the measurement and layout passes,
and a child can only be placed during the layout passes (and only after it has
been measured). Due to Compose scopes such as
[`MeasureScope`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/MeasureScope),
and [`PlacementScope`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/Placeable.PlacementScope),
this is enforced at compile time.

## Use the layout modifier

[Video](https://www.youtube.com/watch?v=l6rAoph5UgI)

You can use the `layout` modifier to modify how an element is measured and laid
out. `Layout` is a lambda; its parameters include the element you can measure,
passed as `measurable`, and that composable's incoming constraints, passed as
`constraints`. A custom layout modifier can look like this:


```kotlin
fun Modifier.customLayoutModifier() =
    layout { measurable, constraints ->
        // ...
    }
```

<br />

Display a `Text` on the screen and control the distance from the top to
the baseline of the first line of text. This is exactly what the
`paddingFromBaseline` modifier does; you're implementing it here as an example.
To do that, use the `layout` modifier to manually place the composable on the
screen. Here's the resulting behavior where the `Text` top padding is set to `24.dp`:
![Shows the difference between normal UI padding, which sets the space between elements, and text padding that sets the space from one baseline to the next](https://developer.android.com/static/develop/ui/compose/images/layout-padding-baseline.png) **Figure 2.** Text with `paddingFromBaseline` applied.

Here's the code to produce that spacing:


```kotlin
fun Modifier.firstBaselineToTop(
    firstBaselineToTop: Dp
) = layout { measurable, constraints ->
    // Measure the composable
    val placeable = measurable.measure(constraints)

    // Check the composable has a first baseline
    check(placeable[FirstBaseline] != AlignmentLine.Unspecified)
    val firstBaseline = placeable[FirstBaseline]

    // Height of the composable with padding - first baseline
    val placeableY = firstBaselineToTop.roundToPx() - firstBaseline
    val height = placeable.height + placeableY
    layout(placeable.width, height) {
        // Where the composable gets placed
        placeable.placeRelative(0, placeableY)
    }
}
```

<br />

Here's what's going on in that code:

1. In the `measurable` lambda parameter, you measure the `Text` represented by the measurable parameter by calling `measurable.measure(constraints)`.
2. You specify the size of the composable by calling the `layout(width, height)` method, which also gives a lambda used for placing the wrapped elements. In this case, it's the height between the last baseline and added top padding.
3. You position the wrapped elements on the screen by calling `placeable.place(x, y)`. If the wrapped elements aren't placed, they won't be visible. The `y` position corresponds to the top padding: the position of the first baseline of the text.

To verify this works as expected, use this modifier on a `Text`:


```kotlin
@Preview
@Composable
fun TextWithPaddingToBaselinePreview() {
    MyApplicationTheme {
        Text("Hi there!", Modifier.firstBaselineToTop(32.dp))
    }
}

@Preview
@Composable
fun TextWithNormalPaddingPreview() {
    MyApplicationTheme {
        Text("Hi there!", Modifier.padding(top = 32.dp))
    }
}
```

<br />

![Multiple previews of text elements; one shows ordinary padding between elements, the other shows padding from one baseline to the next.](https://developer.android.com/static/develop/ui/compose/images/layout-previews-showing-text-baseline-padding.png) **Figure 3.** Modifier applied to a `Text` composable and previewed.

## Create custom layouts

The `layout` modifier only changes the calling composable. To measure and layout
multiple composables, use the [`Layout`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/Layout.composable#Layout(kotlin.collections.List,androidx.compose.ui.Modifier,androidx.compose.ui.layout.MultiContentMeasurePolicy)) composable instead. This composable
lets you measure and lay out children manually. All higher-level layouts
like `Column` and `Row` are built with the `Layout` composable.

> [!NOTE]
> **Note:** In the View system, creating a custom layout required extending `ViewGroup` and implementing measure and layout functions. In Compose, you write a function using the `Layout` composable.

This example builds a very basic version of `Column`. Most custom layouts follow this
pattern:


```kotlin
@Composable
fun MyBasicColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        // measure and position children given constraints logic here
        // ...
    }
}
```

<br />

Similarly to the `layout` modifier, `measurables` is the list of children that
need to be measured and `constraints` are the constraints from the parent.
Following the same logic as before, `MyBasicColumn` can be implemented like
this:


```kotlin
@Composable
fun MyBasicColumn(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        modifier = modifier,
        content = content
    ) { measurables, constraints ->
        // Don't constrain child views further, measure them with given constraints
        // List of measured children
        val placeables = measurables.map { measurable ->
            // Measure each children
            measurable.measure(constraints)
        }

        // Set the size of the layout as big as it can
        layout(constraints.maxWidth, constraints.maxHeight) {
            // Track the y co-ord we have placed children up to
            var yPosition = 0

            // Place children in the parent layout
            placeables.forEach { placeable ->
                // Position item on the screen
                placeable.placeRelative(x = 0, y = yPosition)

                // Record the y co-ord placed up to
                yPosition += placeable.height
            }
        }
    }
}
```

<br />

The child composables are constrained by the `Layout` constraints (without the
`minHeight` constraints), and they're placed based on the `yPosition` of the
previous composable.

Here's how that custom composable would be used:


```kotlin
@Composable
fun CallingComposable(modifier: Modifier = Modifier) {
    MyBasicColumn(modifier.padding(8.dp)) {
        Text("MyBasicColumn")
        Text("places items")
        Text("vertically.")
        Text("We've done it by hand!")
    }
}
```

<br />

![Several text elements stacked one above the next in a column.](https://developer.android.com/static/develop/ui/compose/images/layout-complex-by-hand.png) **Figure 4.** Custom `Column` implementation.

## Layout direction

Change the layout direction of a composable by changing the
[`LocalLayoutDirection`](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary#LocalLayoutDirection()) composition local.

If you're placing composables manually on the screen, the `LayoutDirection` is
part of the `LayoutScope` of the `layout` modifier or `Layout` composable.

When using `layoutDirection`, place composables using [`place`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/Placeable.PlacementScope#place(androidx.compose.ui.layout.Placeable,kotlin.Int,kotlin.Int,kotlin.Float)). Unlike the
[`placeRelative`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/Placeable.PlacementScope#placeRelative(androidx.compose.ui.layout.Placeable,kotlin.Int,kotlin.Int,kotlin.Float))
method, `place` doesn't change based on the layout direction
(left-to-right versus right-to-left).

## Custom layouts in action

Learn more about layouts and modifiers in the
[Basic layouts in Compose](https://developer.android.com/codelabs/jetpack-compose-layouts),
and see custom layouts in action in the
[Compose samples that create custom layouts](https://github.com/android/compose-samples).

## Learn more

To learn more about custom layouts in Compose, consult the following additional
resources.

### Videos

- [A deep dive into Jetpack Compose Layouts](https://www.youtube.com/watch?v=zMKMwh9gZuI)

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Intrinsic measurements in Compose layouts](https://developer.android.com/develop/ui/compose/layouts/intrinsic-measurements)
- [Graphics in Compose](https://developer.android.com/develop/ui/compose/graphics/draw/overview)
- [Compose modifiers](https://developer.android.com/develop/ui/compose/modifiers)