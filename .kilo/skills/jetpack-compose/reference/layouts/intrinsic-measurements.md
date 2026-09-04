[Video](https://www.youtube.com/watch?v=l6rAoph5UgI)

One of the rules of Compose is that you should only measure your children once;
measuring children twice throws a runtime exception. However, there are times
when you need some information about your children before measuring them.

**Intrinsics lets you query children before they're actually measured.**

To a composable, you can ask for its `IntrinsicSize.Min` or `IntrinsicSize.Max`:

- `Modifier.width(IntrinsicSize.Min)` - What's the minimum width you need to display your content properly?
- `Modifier.width(IntrinsicSize.Max)` - What's the maximum width you need to display your content properly?
- `Modifier.height(IntrinsicSize.Min)` - What's the minimum height you need to display your content properly?
- `Modifier.height(IntrinsicSize.Max)` - What's the maximum height you need to display your content properly?

For example, if you ask the `minIntrinsicHeight` of a `Text` with infinite
`width` constraints in a custom layout, it returns the `height` of the `Text`
with the text drawn in a single line.

> [!NOTE]
> **Note:** Asking for intrinsics measurements doesn't measure the children twice. Children are queried for their intrinsic measurements before they're measured, and then based on that information, the parent calculates the constraints to measure its children with.

## Intrinsics in action

You can create a composable that displays two texts on the screen separated by a
divider:

![Two text elements side by side, with a vertical divider between them](https://developer.android.com/static/develop/ui/compose/images/layout-text-with-divider.png)

To do this, use a `Row` with two `Text` composables that fill the available
space, and a `Divider` in the middle. The `Divider` should be as tall as the
tallest `Text`, and it should be thin (`width = 1.dp`).


```kotlin
@Composable
fun TwoTexts(modifier: Modifier = Modifier, text1: String, text2: String) {
    Row(modifier = modifier) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .wrapContentWidth(Alignment.Start),
            text = text1
        )
        VerticalDivider(
            color = Color.Black,
            modifier = Modifier.fillMaxHeight().width(1.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .wrapContentWidth(Alignment.End),

            text = text2
        )
    }
}
```

<br />

The `Divider` expands to the whole screen, which isn't the desired behavior:

![Two text elements side by side, with a divider between them, but the divider stretches down below the bottom of the text](https://developer.android.com/static/develop/ui/compose/images/layout-text-with-divider-too-long.png)

This happens because `Row` measures each child individually, and the height of
`Text` cannot be used to constrain the `Divider`.

To have the `Divider` fill the available space with a given height instead,
use the `height(IntrinsicSize.Min)` modifier.

`height(IntrinsicSize.Min)` sizes its children to be as tall as their minimum
intrinsic height. Because this modifier is recursive, it queries the
`minIntrinsicHeight` of the `Row` and its children.

Applying this modifier to your code makes it work as expected:


```kotlin
@Composable
fun TwoTexts(modifier: Modifier = Modifier, text1: String, text2: String) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
                .wrapContentWidth(Alignment.Start),
            text = text1
        )
        VerticalDivider(
            color = Color.Black,
            modifier = Modifier.fillMaxHeight().width(1.dp)
        )
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
                .wrapContentWidth(Alignment.End),

            text = text2
        )
    }
}

// @Preview
@Composable
fun TwoTextsPreview() {
    MaterialTheme {
        Surface {
            TwoTexts(text1 = "Hi", text2 = "there")
        }
    }
}
```

<br />

With preview:

![Two text elements side by side, with a vertical divider between them](https://developer.android.com/static/develop/ui/compose/images/layout-text-with-divider.png)

The `Row`'s height is determined as follows:

- The `Row` composable's `minIntrinsicHeight` is the maximum `minIntrinsicHeight` of its children.
- The `Divider` element's `minIntrinsicHeight` is 0, as it doesn't occupy space if no constraints are given.
- The `Text` `minIntrinsicHeight` is that of the text for a specific `width`.
- Therefore, the `Row` element's `height` constraint becomes the maximum `minIntrinsicHeight` of the `Text`s.
- The `Divider` then expands its `height` to the `height` constraint given by the `Row`.

### Intrinsics in your custom layouts

When creating a custom `Layout` or `layout` modifier, intrinsic measurements
are calculated automatically based on approximations. Therefore, the
calculations might not be correct for all layouts. These APIs offer options
to override these defaults.

To specify the intrinsics measurements of your custom `Layout`, override the
`minIntrinsicWidth`, `minIntrinsicHeight`, `maxIntrinsicWidth`, and
`maxIntrinsicHeight` of the [`MeasurePolicy`](https://developer.android.com/reference/kotlin/androidx/compose/ui/layout/MeasurePolicy) interface when creating it.


```kotlin
@Composable
fun MyCustomComposable(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Layout(
        content = content,
        modifier = modifier,
        measurePolicy = object : MeasurePolicy {
            override fun MeasureScope.measure(
                measurables: List<Measurable>,
                constraints: Constraints
            ): MeasureResult {
                // Measure and layout here
                // ...
            }

            override fun IntrinsicMeasureScope.minIntrinsicWidth(
                measurables: List<IntrinsicMeasurable>,
                height: Int
            ): Int {
                // Logic here
                // ...
            }

            // Other intrinsics related methods have a default value,
            // you can override only the methods that you need.
        }
    )
}
```

<br />

When creating your custom `layout` modifier, override the related methods
in the `LayoutModifier` interface.


```kotlin
fun Modifier.myCustomModifier(/* ... */) = this then object : LayoutModifier {

    override fun MeasureScope.measure(
        measurable: Measurable,
        constraints: Constraints
    ): MeasureResult {
        // Measure and layout here
        // ...
    }

    override fun IntrinsicMeasureScope.minIntrinsicWidth(
        measurable: IntrinsicMeasurable,
        height: Int
    ): Int {
        // Logic here
        // ...
    }

    // Other intrinsics related methods have a default value,
    // you can override only the methods that you need.
}
```

<br />

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Custom layouts {:#custom-layouts}](https://developer.android.com/develop/ui/compose/layouts/custom)
- [Alignment lines in Jetpack Compose](https://developer.android.com/develop/ui/compose/layouts/alignment-lines)
- [Jetpack Compose Phases](https://developer.android.com/develop/ui/compose/phases)