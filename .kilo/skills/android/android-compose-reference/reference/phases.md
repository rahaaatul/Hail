[Video](https://www.youtube.com/watch?v=0yK7KoruhSM)

Like most other UI toolkits, Compose renders a frame through several distinct
*phases* . For example, the Android View system has three main phases: measure,
layout, and drawing. Compose is very similar but has an important additional
phase called *composition* at the start.

The Compose documentation describes composition in [Thinking in Compose](https://developer.android.com/develop/ui/compose/mental-model) and
[State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state).

## The three phases of a frame

Compose has three main phases:

1. **Composition** : *What* UI to show. Compose runs composable functions and creates a description of your UI.
2. **Layout** : *Where* to place UI. This phase consists of two steps: measurement and placement. Layout elements measure and place themselves and any child elements in 2D coordinates, for each node in the layout tree.
3. **Drawing** : *How* it renders. UI elements draw into a Canvas, usually a device screen.

![The three phases in which Compose transforms data into UI (in order, data, composition, layout, drawing, UI).](https://developer.android.com/static/develop/ui/compose/images/compose-phases.png) **Figure 1.** The three phases in which Compose transforms data into UI.

The order of these phases is generally the same, allowing data to flow in one
direction from composition to layout to drawing to produce a frame (also known
as [unidirectional data flow](https://developer.android.com/develop/ui/compose/architecture#udf)). [`BoxWithConstraints`](https://developer.android.com/develop/ui/compose/layouts/basics#constraints), [`LazyColumn`](https://developer.android.com/develop/ui/compose/lists#lazy),
and [`LazyRow`](https://developer.android.com/develop/ui/compose/lists#lazy) are notable exceptions, where the composition of its children
depends on the parent's layout phase.

Conceptually, each of these phases happens for every frame; however to optimize
performance, Compose avoids repeating work that would compute the same results
from the same inputs in all of these phases. Compose [skips](https://developer.android.com/develop/ui/compose/mental-model#skips) running a
composable function if it can reuse a former result, and Compose UI doesn't
re-layout or re-draw the entire tree if it doesn't have to. Compose performs
only the minimum amount of work required to update the UI. This optimization is
possible because Compose tracks state reads within the different phases.

### Understand the phases

This section describes how the three Compose phases are executed for composables
in greater detail.

#### Composition

In the composition phase, the Compose runtime executes composable functions and
outputs a tree structure that represents your UI. This UI tree consists of
layout nodes that contain all the information needed for the next phases, as
shown in the following video:

**Figure 2.** The tree representing your UI that is created in the composition
phase.

A subsection of the code and UI tree looks like the following:
![A code snippet with five composables and the resulting UI tree, with child nodes branching from their parent nodes.](https://developer.android.com/static/develop/ui/compose/images/code-subsection.png) **Figure 3.** A subsection of a UI tree with the corresponding code.

In these examples, each composable function in the code maps to a single layout
node in the UI tree. In more complex examples, composables can contain logic and
control flow, and produce a different tree given different states.

#### Layout

In the layout phase, Compose uses the UI tree produced in the composition phase
as input. The collection of layout nodes contain all the information needed to
decide on each node's size and location in 2D space.

**Figure 4.** The measurement and placement of each layout node in the UI tree
during the layout phase.

During the layout phase, the tree is traversed using the following three-step
algorithm:

1. **Measure children**: A node measures its children if any exist.
2. **Decide own size**: Based on these measurements, a node decides on its own size.
3. **Place children**: Each child node is placed relative to a node's own position.

At the end of this phase, each layout node has:

- An assigned **width** and **height**
- An **x, y coordinate** where it should be drawn

Recall the UI tree from the previous section:

![A code snippet with five composables and the resulting UI tree, with child nodes branching from their parent nodes](https://developer.android.com/static/develop/ui/compose/images/code-subsection.png)

For this tree, the algorithm works as follows:

1. The `Row` measures its children, `Image` and `Column`.
2. The `Image` is measured. It doesn't have any children, so it decides its own size and reports the size back to the `Row`.
3. The `Column` is measured next. It measures its own children (two `Text` composables) first.
4. The first `Text` is measured. It doesn't have any children so it decides its own size and reports its size back to the `Column`.
   1. The second `Text` is measured. It doesn't have any children so it decides its own size and reports it back to the `Column`.
5. The `Column` uses the child measurements to decide its own size. It uses the maximum child width and the sum of the height of its children.
6. The `Column` places its children relative to itself, putting them beneath each other vertically.
7. The `Row` uses the child measurements to decide its own size. It uses the maximum child height and the sum of the widths of its children. It then places its children.

Note that each node was visited only once. The Compose runtime requires only one
pass through the UI tree to measure and place all the nodes, which improves
performance. When the number of nodes in the tree increases, the time spent
traversing it increases in a linear fashion. In contrast, if each node was
visited multiple times, the traversal time increases exponentially.

#### Drawing

In the drawing phase, the tree is traversed again from top to bottom, and each
node draws itself on the screen in turn.

**Figure 5.** The drawing phase draws the pixels on the screen.

Using the previous example, the tree content is drawn in the following way:

1. The `Row` draws any content it might have, such as a background color.
2. The `Image` draws itself.
3. The `Column` draws itself.
4. The first and second `Text` draw themselves, respectively.

**Figure 6.** A UI tree and its drawn representation.

## State reads

When you read the `value` of a [`snapshot state`](https://developer.android.com/develop/ui/compose/state) during one of the phases
listed previously, Compose automatically tracks what it was doing when it read
the `value`. This tracking allows Compose to re-execute the reader when the
state's `value` changes, and is the basis of state observability in Compose.

You commonly create state using `mutableStateOf()` and then access it through
one of two ways: by directly accessing the `value` property, or by
using a Kotlin property delegate. You can read more about them in [State in
composables](https://developer.android.com/develop/ui/compose/state#state-in-composables). For the purposes of this guide, a "state read" refers to either
of those equivalent access methods.


```kotlin
// State read without property delegate.
val paddingState: MutableState<Dp> = remember { mutableStateOf(8.dp) }
Text(
    text = "Hello",
    modifier = Modifier.padding(paddingState.value)
)
```

<br />


```kotlin
// State read with property delegate.
var padding: Dp by remember { mutableStateOf(8.dp) }
Text(
    text = "Hello",
    modifier = Modifier.padding(padding)
)
```

<br />

Under the hood of the [property delegate](https://kotlinlang.org/docs/delegated-properties.html), "getter" and "setter"
functions are used to access and update the State's `value`. These getter and
setter functions are only invoked when you reference the property as a value,
and not when it is created, which is why the two ways described previously are
equivalent.

Each block of code that can be re-executed when a read state changes is a
*restart scope* . Compose keeps track of state `value` changes and restart scopes
in different phases.

## Phased state reads

As mentioned previously, there are three main phases in Compose, and Compose
tracks what state is read within each of them. This allows Compose to notify
only the specific phases that need to perform work for each affected element of
your UI.

> [!NOTE]
> **Note:** Where a state instance is created and stored has little bearing on the phases, it only matters when and where a state's `value` is **read**.

The following sections describe each phase and describe what happens when a
state value is read within it.

### Phase 1: Composition

State reads within a `@Composable` function or lambda block affect composition
and potentially the subsequent phases. When the state's `value` changes, the
recomposer schedules reruns of all the composable functions which read that
state's `value`. Note that the runtime may decide to skip some or all of the
composable functions if the inputs haven't changed. See [Skipping if the inputs
haven't changed](https://developer.android.com/develop/ui/compose/lifecycle#skipping) for more information.

Depending on the result of composition, Compose UI runs the layout and drawing
phases. It might skip these phases if the content remains the same and the size
and the layout won't change.


```kotlin
var padding by remember { mutableStateOf(8.dp) }
Text(
    text = "Hello",
    // The `padding` state is read in the composition phase
    // when the modifier is constructed.
    // Changes in `padding` will invoke recomposition.
    modifier = Modifier.padding(padding)
)
```

<br />

### Phase 2: Layout

The layout phase consists of two steps: *measurement* and *placement* . The
measurement step runs the measure lambda passed to the `Layout` composable, the
`MeasureScope.measure` method of the `LayoutModifier` interface, among others.
The placement step runs the placement block of the `layout` function, the lambda
block of `Modifier.offset { ... }`, and similar functions.

State reads during each of these steps affect the layout and potentially the
drawing phase. When the state's `value` changes, Compose UI schedules the layout
phase. It also runs the drawing phase if size or position has changed.

> [!IMPORTANT]
> **Key Point:** The measurement step and the placement step have separate restart scopes, meaning that state reads in the placement step don't re-invoke the measurement step before that. However, these two steps are often intertwined, so a state read in the placement step can affect other restart scopes that belong to the measurement step.


```kotlin
var offsetX by remember { mutableStateOf(8.dp) }
Text(
    text = "Hello",
    modifier = Modifier.offset {
        // The `offsetX` state is read in the placement step
        // of the layout phase when the offset is calculated.
        // Changes in `offsetX` restart the layout.
        IntOffset(offsetX.roundToPx(), 0)
    }
)
```

<br />

### Phase 3: Drawing

State reads during drawing code affect the drawing phase. Common examples
include `Canvas()`, `Modifier.drawBehind`, and `Modifier.drawWithContent`. When
the state's `value` changes, Compose UI runs only the draw phase.


```kotlin
var color by remember { mutableStateOf(Color.Red) }
Canvas(modifier = modifier) {
    // The `color` state is read in the drawing phase
    // when the canvas is rendered.
    // Changes in `color` restart the drawing.
    drawRect(color)
}
```

<br />

![Diagram showing that a state read during the draw phase only triggers the draw phase to run again.](https://developer.android.com/static/develop/ui/compose/images/phases-state-read-draw.svg)

## Optimize state reads

Because Compose performs localized state read tracking, you can minimize the
amount of work performed by reading each state in an appropriate phase.

Consider the following example. This example has an `Image()` which uses the
offset modifier to offset its final layout position, resulting in a parallax
effect as the user scrolls.


```kotlin
Box {
    val listState = rememberLazyListState()

    Image(
        // ...
        // Non-optimal implementation!
        Modifier.offset(
            with(LocalDensity.current) {
                // State read of firstVisibleItemScrollOffset in composition
                (listState.firstVisibleItemScrollOffset / 2).toDp()
            }
        )
    )

    LazyColumn(state = listState) {
        // ...
    }
}
```

<br />

This code works, but results in suboptimal performance. As written, the code
reads the `value` of the `firstVisibleItemScrollOffset` state and passes it to
the [`Modifier.offset(offset: Dp)`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/offset.modifier#(androidx.compose.ui.Modifier).offset(androidx.compose.ui.unit.Dp,androidx.compose.ui.unit.Dp)) function. As the user scrolls, the
`firstVisibleItemScrollOffset`'s `value` will change. As you've learned, Compose
tracks any state reads so that it can restart (re-invoke) the reading code,
which in this example is the content of the `Box`.

This is an example of reading a state within the **composition** phase. This is
not necessarily a bad thing, and in fact is the basis of recomposition,
allowing data changes to emit new UI.

Key point: This example is suboptimal because every scroll event results in the
entire composable content being reevaluated, measured, laid out, and finally
drawn. You trigger the Compose phase on every scroll even though the content
shown hasn't changed, only **its position**. You can optimize the state read to
only re-trigger the layout phase.

### Offset with lambda

There is another version of the offset modifier available:
[`Modifier.offset(offset: Density.() -> IntOffset)`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/offset.modifier#(androidx.compose.ui.Modifier).offset(kotlin.Function1)).

This version takes a lambda parameter, where the resulting offset is returned by
the lambda block. Update the code to use it:


```kotlin
Box {
    val listState = rememberLazyListState()

    Image(
        // ...
        Modifier.offset {
            // State read of firstVisibleItemScrollOffset in Layout
            IntOffset(x = 0, y = listState.firstVisibleItemScrollOffset / 2)
        }
    )

    LazyColumn(state = listState) {
        // ...
    }
}
```

<br />

So why is this more performant? The lambda block you provide to the modifier is
invoked during the **layout** phase (specifically, during the layout phase's
placement step), meaning that the `firstVisibleItemScrollOffset` state is no
longer read during composition. Because Compose tracks when state is read,
this change means that if the `firstVisibleItemScrollOffset`'s `value` changes,
Compose only has to restart the layout and drawing phases.

> [!NOTE]
> **Note:** You might wonder if taking a lambda parameter might add extra cost compared to taking a value. It does. However, the benefit of limiting the state read to the layout phase outweighs the cost in this case. The `value` of `firstVisibleItemScrollOffset` changes every frame during scroll, and by deferring the state read to the layout phase, you can avoid recompositions all along.

Of course, it is often absolutely necessary to read states in the composition
phase. Even so, there are cases where you can minimize the number of
recompositions by filtering state changes. For more information about this,
see [`derivedStateOf`: convert one or multiple state objects into another
state](https://developer.android.com/develop/ui/compose/side-effects#derivedstateof).

## Recomposition loop (cyclic phase dependency)

This guide previously mentioned that the phases of Compose are always invoked in
the same order, and that there is no way to go backwards while in the same
frame. However, that doesn't prohibit apps getting into composition loops
across *different* frames. Consider this example:


```kotlin
Box {
    var imageHeightPx by remember { mutableIntStateOf(0) }

    Image(
        painter = painterResource(R.drawable.rectangle),
        contentDescription = "I'm above the text",
        modifier = Modifier
            .fillMaxWidth()
            .onSizeChanged { size ->
                // Don't do this
                imageHeightPx = size.height
            }
    )

    Text(
        text = "I'm below the image",
        modifier = Modifier.padding(
            top = with(LocalDensity.current) { imageHeightPx.toDp() }
        )
    )
}
```

<br />

This example implements a vertical column, with the image at the top, and then
the text below it. It uses `Modifier.onSizeChanged()` to get the resolved size
of the image, and then uses `Modifier.padding()` on the text to shift it down.
The unnatural conversion from `Px` back to `Dp` already indicates that the code
has some issue.

The issue with this example is that the code doesn't arrive at the "final"
layout within a single frame. The code relies on multiple frames happening,
which performs unnecessary work, and results in UI jumping around on screen for
the user.

### First frame composition

During the composition phase of the first frame, `imageHeightPx` is initially
`0`. Consequently, the code provides the text with `Modifier.padding(top = 0)`.
The subsequent layout phase invokes the `onSizeChanged` modifier's callback,
which updates `imageHeightPx` to the image's actual height. Compose then
schedules a recomposition for the next frame. However, during the current
drawing phase, the text renders with a padding of `0`, as the updated
`imageHeightPx` value is not yet reflected.

### Second frame composition

Compose initiates the second frame, triggered by the change in `imageHeightPx`'s
value. In this frame's composition phase, the state is read within the `Box`
content block. The text is now provided with padding that accurately matches the
image's height. During the layout phase, `imageHeightPx` is set again; however,
no further recomposition is scheduled because the value remains consistent.

> [!IMPORTANT]
> **Key Point:** In the end, you get the chosen padding on the text, but it is suboptimal to spend an extra frame to pass the padding value back to a different phase and will result in producing a frame with overlapping content.

![Diagram showing a recomposition loop where a size change in the layout phase triggers a recomposition, which then causes the layout to happen again.](https://developer.android.com/static/develop/ui/compose/images/phases-recomp-loop.svg)

This example may seem contrived, but be careful of this general pattern:

- `Modifier.onSizeChanged()`, `onGloballyPositioned()`, or some other layout operations
- Update some state
- Use that state as input to a layout modifier (`padding()`, `height()`, or similar)
- Potentially repeat

The fix for the preceding sample is to use the proper layout primitives. The
preceding example can be implemented with a `Column()`, but you may have a more
complex example which requires something custom, which will require writing a
custom layout. See the [Custom layouts](https://developer.android.com/develop/ui/compose/layouts/custom) guide for more information.

The general principle here is to have a single source of truth for multiple UI
elements that should be measured and placed with regards to one another. Using
a proper layout primitive or creating a custom layout means that the minimal
shared parent serves as the source of truth that can coordinate the relation
between multiple elements. Introducing a dynamic state breaks this principle.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
- [Lists and grids](https://developer.android.com/develop/ui/compose/lists)
- [Kotlin for Jetpack Compose](https://developer.android.com/develop/ui/compose/kotlin)