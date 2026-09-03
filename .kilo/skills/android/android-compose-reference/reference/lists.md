[Video](https://www.youtube.com/watch?v=1ANt65eoNhQ)

Many apps need to display collections of items. This document explains how you
can efficiently do this in Jetpack Compose.

If you know that your use case does not require any scrolling, you may wish to
use a simple [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) or [`Row`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Row.composable#Row(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Horizontal,androidx.compose.ui.Alignment.Vertical,kotlin.Function1)) (depending on the direction), and emit each item's content by
iterating over a list in the following way:


```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    Column {
        messages.forEach { message ->
            MessageRow(message)
        }
    }
}
```

<br />

We can make the [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) scrollable by using the `verticalScroll()` modifier.

## Lazy lists

If you need to display a large number of items (or a list of an unknown length),
using a layout such as [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) can cause performance issues, since all the items will be composed and laid out whether or not they are visible.

Compose provides a set of components which only compose and lay out items which
are visible in the component's viewport. These components include
[`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable)
and
[`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyRow.composable).

> [!NOTE]
> **Note:** If you've used the [`RecyclerView`
> widget](https://developer.android.com/guide/topics/ui/layout/recyclerview), these components follow the same set of principles.

As the name suggests, the difference between
[`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable)
and
[`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyRow.composable)
is the orientation in which they lay out their items and scroll. [`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable)
produces a vertically scrolling list, and [`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyRow.composable) produces a horizontally
scrolling list.

The Lazy components are different to most layouts in Compose. Instead of
accepting a `@Composable` content block parameter, allowing apps to directly
emit composables, the Lazy components provide a `LazyListScope.()` block. This
[`LazyListScope`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope)
block offers a DSL which allows apps to *describe* the item contents. The Lazy component is then responsible for adding each item's content as
required by the layout and scroll position.

> [!IMPORTANT]
> **Key Term:** *DSL* stands for **domain-specific language** . See the [Kotlin for
> Compose](https://developer.android.com/develop/ui/compose/kotlin#dsl) documentation for more information on how Compose defines DSLs for some APIs.

## `LazyListScope` DSL

The DSL of [`LazyListScope`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope) provides a number of functions for describing items
in the layout. At the most basic,
[`item()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope#item(kotlin.Any,kotlin.Any,kotlin.Function1))
adds a single item, and
[`items(Int)`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope#items(kotlin.Int,kotlin.Function1,kotlin.Function1,kotlin.Function2))
adds multiple items:


```kotlin
LazyColumn {
    // Add a single item
    item {
        Text(text = "First item")
    }

    // Add 5 items
    items(5) { index ->
        Text(text = "Item: $index")
    }

    // Add another single item
    item {
        Text(text = "Last item")
    }
}
```

<br />

There are also a number of extension functions which allow you to add
collections of items, such as a `List`. These extensions allow us to easily
migrate our [`Column`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) example from above:


```kotlin
/**
 * import androidx.compose.foundation.lazy.items
 */
LazyColumn {
    items(messages) { message ->
        MessageRow(message)
    }
}
```

<br />

There is also a variant of the
[`items()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope#items(kotlin.Int,kotlin.Function1,kotlin.Function1,kotlin.Function2))
extension function called
[`itemsIndexed()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary#(androidx.compose.foundation.lazy.LazyListScope).itemsIndexed(kotlin.collections.List,kotlin.Function2,kotlin.Function2,kotlin.Function3)),
which provides the index. Please see the
[`LazyListScope`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope)
reference for more details.

## Lazy grids

[Video](https://www.youtube.com/watch?v=qP-ieASbqMY)

The
[`LazyVerticalGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyVerticalGrid.composable)
and
[`LazyHorizontalGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyHorizontalGrid.composable)
composables provide support for displaying items in a grid. A Lazy vertical grid
will display its items in a vertically scrollable container, spanned across
multiple columns, while the Lazy horizontal grids will have the same behavior
on the horizontal axis.

Grids have the same powerful API capabilities as lists and they also use a
very similar DSL -
[`LazyGridScope.()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyGridScope)
for describing the content.

![Screenshot of a phone showing a grid of photos](https://developer.android.com/static/develop/ui/compose/images/lists-photogrid.png)

The `columns` parameter in
[`LazyVerticalGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyVerticalGrid.composable)
and `rows` parameter in
[`LazyHorizontalGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyHorizontalGrid.composable)
control how cells are formed into columns or rows. The following
example displays items in a grid, using
[`GridCells.Adaptive`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/GridCells.Adaptive)
to set each column to be at least `128.dp` wide:


```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 128.dp)
) {
    items(photos) { photo ->
        PhotoItem(photo)
    }
}
```

<br />

`LazyVerticalGrid` lets you specify a width for items, and then the grid will
fit as many columns as possible. Any remaining width is distributed equally
among the columns, after the number of columns is calculated.
This adaptive way of sizing is especially useful for displaying sets of items
across different screen sizes.

If you know the exact number of columns to be used, you can instead provide an
instance of
[`GridCells.Fixed`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/GridCells.Fixed)
containing the number of required columns.

If your design requires only certain items to have non-standard dimensions,
you can use the grid support for providing custom column spans for items.
Specify the column span with the `span` parameter of the
[`LazyGridScope DSL`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyGridScope)
`item` and `items` methods.
[`maxLineSpan`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/grid/LazyGridItemSpanScope#maxLineSpan()),
one of the span scope's values, is particularly useful when you're using
adaptive sizing, because the number of columns is not fixed.
This example shows how to provide a full row span:


```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(minSize = 30.dp)
) {
    item(span = {
        // LazyGridItemSpanScope:
        // maxLineSpan
        GridItemSpan(maxLineSpan)
    }) {
        CategoryCard("Fruits")
    }
    // ...
}
```

<br />

## Lazy staggered grid

[`LazyVerticalStaggeredGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/staggeredgrid/LazyVerticalStaggeredGrid.composable)
and
[`LazyHorizontalStaggeredGrid`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/staggeredgrid/LazyHorizontalStaggeredGrid.composable)
are composables that allow you to create a lazy-loaded, staggered grid of items.
A lazy vertical staggered grid displays its items in a vertically scrollable
container that spans across multiple columns and allows individual items to be
different heights. Lazy horizontal grids have the same behavior on the
horizontal axis with items of different widths.

> [!NOTE]
> **Note:** The difference between a lazy staggered grid and a lazy grid is that the former can display items of different size widths or heights.

The following snippet is a basic example of using `LazyVerticalStaggeredGrid`
with a `200.dp` width per item:


```kotlin
LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Adaptive(200.dp),
    verticalItemSpacing = 4.dp,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    content = {
        items(randomSizedPhotos) { photo ->
            AsyncImage(
                model = photo,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    },
    modifier = Modifier.fillMaxSize()
)
```

<br />

Figure 1. Example of lazy staggered vertical grid

To set a fixed number of columns, you can use
`StaggeredGridCells.Fixed(columns)` instead of `StaggeredGridCells.Adaptive`.
This divides the available width by the number of columns (or rows for a
horizontal grid), and has each item take up that width (or height for a
horizontal grid):


```kotlin
LazyVerticalStaggeredGrid(
    columns = StaggeredGridCells.Fixed(3),
    verticalItemSpacing = 4.dp,
    horizontalArrangement = Arrangement.spacedBy(4.dp),
    content = {
        items(randomSizedPhotos) { photo ->
            AsyncImage(
                model = photo,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            )
        }
    },
    modifier = Modifier.fillMaxSize()
)
```
![Lazy staggered grid of images in Compose](https://developer.android.com/static/develop/ui/compose/images/lists/staggered_grid_fixed.png) Figure 2. Example of lazy staggered vertical grid with fixed columns

<br />

## Content padding

Sometimes you'll need to add padding around the edges of the content. The lazy
components allow you to pass some
[`PaddingValues`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/PaddingValues)
to the `contentPadding` parameter to support this:


```kotlin
LazyColumn(
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    // ...
}
```

<br />

In this example, we add `16.dp` of padding to the horizontal edges (left and
right), and then `8.dp` to the top and bottom of the content.

Please note that this padding is applied to the *content* , not to the
`LazyColumn` itself. In the example above, the first item will add `8.dp`
padding to its top, the last item will add `8.dp` to its bottom, and all items
will have `16.dp` padding on the left and the right.

As another example, you can pass `Scaffold`'s `PaddingValues` into
`LazyColumn`'s `contentPadding`. See the
[edge-to-edge](https://developer.android.com/develop/ui/compose/layouts/insets#scaffold) guide.

## Content spacing

To add spacing in-between items, you can use
[`Arrangement.spacedBy()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Arrangement#spacedBy(androidx.compose.ui.unit.Dp)).
The example below adds `4.dp` of space in-between each item:


```kotlin
LazyColumn(
    verticalArrangement = Arrangement.spacedBy(4.dp),
) {
    // ...
}
```

<br />

Similarly for [`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyRow.composable):


```kotlin
LazyRow(
    horizontalArrangement = Arrangement.spacedBy(4.dp),
) {
    // ...
}
```

<br />

Grids, however, accept both vertical and horizontal arrangements:


```kotlin
LazyVerticalGrid(
    columns = GridCells.Fixed(2),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp)
) {
    items(photos) { item ->
        PhotoItem(item)
    }
}
```

<br />

## Item keys

By default, each item's state is keyed against the position of the item in the
list or grid. However, this can cause issues if the dataset changes, since
items which change position effectively lose any remembered state. If you
imagine the scenario of `LazyRow` within a `LazyColumn`, if the row changes
item position, the user would then lose their scroll position within the row.

> [!NOTE]
> **Note:** For more information on how Compose remembers state, see the [State](https://developer.android.com/develop/ui/compose/state) documentation

To combat this, you can provide a stable and unique key for each item, providing
a block to the `key` parameter. Providing a stable key enables item state to be
consistent across dataset changes:


```kotlin
LazyColumn {
    items(
        items = messages,
        key = { message ->
            // Return a stable + unique key for the item
            message.id
        }
    ) { message ->
        MessageRow(message)
    }
}
```

<br />

By providing keys, you help Compose to handle reorderings correctly.
For example, if your item contains remembered state, setting keys would allow
Compose to move this state together with the item, when its position changes.


```kotlin
LazyColumn {
    items(books, key = { it.id }) {
        val rememberedValue = remember {
            Random.nextInt()
        }
    }
}
```

<br />

However, there is one limitation on what types you can use as item keys.
The key's type must be supported by
[`Bundle`](https://developer.android.com/reference/android/os/Bundle), Android's mechanism for keeping the
states when the Activity is recreated. `Bundle` supports types like primitives,
enums or Parcelables.


```kotlin
LazyColumn {
    items(books, key = {
        // primitives, enums, Parcelable, etc.
    }) {
        // ...
    }
}
```

<br />

The key must be supported by `Bundle` so that the `rememberSaveable` inside
the item composable can be restored when the Activity is recreated, or even
when you scroll away from this item and scroll back.


```kotlin
LazyColumn {
    items(books, key = { it.id }) {
        val rememberedValue = rememberSaveable {
            Random.nextInt()
        }
    }
}
```

<br />

## Item animations

If you've used the RecyclerView widget, you'll know that it [animates item
changes](https://developer.android.com/guide/topics/ui/layout/recyclerview-custom#animations) automatically.
Lazy layouts provide the same functionality for item reorderings.
The API is simple - you just need to set the
[`animateItem`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyItemScope#(androidx.compose.ui.Modifier).animateItem(androidx.compose.animation.core.FiniteAnimationSpec,androidx.compose.animation.core.FiniteAnimationSpec,androidx.compose.animation.core.FiniteAnimationSpec))
modifier to the item content:


```kotlin
LazyColumn {
    // It is important to provide a key to each item to ensure animateItem() works as expected.
    items(books, key = { it.id }) {
        Row(Modifier.animateItem()) {
            // ...
        }
    }
}
```

<br />

You can even provide custom animation specification, if you need to:


```kotlin
LazyColumn {
    items(books, key = { it.id }) {
        Row(
            Modifier.animateItem(
                fadeInSpec = tween(durationMillis = 250),
                fadeOutSpec = tween(durationMillis = 100),
                placementSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioMediumBouncy)
            )
        ) {
            // ...
        }
    }
}
```

<br />

Make sure you provide keys for your items so it is possible to find the new
position for the moved element.

### Example: Animate items in lazy lists

With Compose, you can animate changes to items in lazy lists. When used
together, the following snippets implement animations when adding, removing, and
reordering lazy list items.

This snippet displays a list of strings with animated transitions when
items are added, removed, or reordered:


```kotlin
@Composable
fun ListAnimatedItems(
    items: List<String>,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier) {
        // Use a unique key per item, so that animations work as expected.
        items(items, key = { it }) {
            ListItem(
                headlineContent = { Text(it) },
                modifier = Modifier
                    .animateItem(
                        // Optionally add custom animation specs
                    )
                    .fillParentMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 0.dp),
            )
        }
    }
}
```

<br />

#### Key points about the code

- `ListAnimatedItems` displays a list of strings in a [`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable#LazyColumn(androidx.compose.ui.Modifier,androidx.compose.foundation.lazy.LazyListState,androidx.compose.foundation.layout.PaddingValues,kotlin.Boolean,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,androidx.compose.foundation.gestures.FlingBehavior,kotlin.Boolean,androidx.compose.foundation.OverscrollEffect,kotlin.Function1)) with animated transitions when items are modified.
- The `items` function assigns a unique key to each item in the list. Compose uses the keys to track the items and identify changes in their positions.
- `ListItem` defines the layout of each list item. It takes a `headlineContent` parameter, which defines the main content of the item.
- The [`animateItem`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyItemScope#(androidx.compose.ui.Modifier).animateItem(androidx.compose.animation.core.FiniteAnimationSpec,androidx.compose.animation.core.FiniteAnimationSpec,androidx.compose.animation.core.FiniteAnimationSpec)) modifier applies default animations to item additions, removals, and moves.

The following snippet presents a screen that incorporates controls for adding
and removing items, as well as sorting a predefined list:


```kotlin
@Composable
private fun ListAnimatedItemsExample(
    data: List<String>,
    modifier: Modifier = Modifier,
    onAddItem: () -> Unit = {},
    onRemoveItem: () -> Unit = {},
    resetOrder: () -> Unit = {},
    onSortAlphabetically: () -> Unit = {},
    onSortByLength: () -> Unit = {},
) {
    val canAddItem = data.size < 10
    val canRemoveItem = data.isNotEmpty()

    Scaffold(modifier) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            // Buttons that change the value of displayedItems.
            AddRemoveButtons(canAddItem, canRemoveItem, onAddItem, onRemoveItem)
            OrderButtons(resetOrder, onSortAlphabetically, onSortByLength)

            // List that displays the values of displayedItems.
            ListAnimatedItems(data)
        }
    }
}
```

<br />

#### Key points about the code

- `ListAnimatedItemsExample` presents a screen that incorporates controls for adding, removing, and sorting items.
  - `onAddItem` and `onRemoveItem` are lambda expressions that are passed to `AddRemoveButtons` to add and remove items from the list.
  - `resetOrder`, `onSortAlphabetically`, and `onSortByLength` are lambda expressions that are passed to `OrderButtons` to change the order of the items in the list.
- `AddRemoveButtons` displays the "Add" and "Remove" buttons. It enables/disables the buttons and handles button clicks.
- `OrderButtons` displays the buttons for reordering the list. It receives the lambda functions for resetting the order and sorting the list by length or alphabetically.
- `ListAnimatedItems` calls the `ListAnimatedItems` composable, passing the `data` list to display the animated list of strings. `data` is defined elsewhere.

This snippet creates a UI with **Add Item** and **Delete Item** buttons:


```kotlin
@Composable
private fun AddRemoveButtons(
    canAddItem: Boolean,
    canRemoveItem: Boolean,
    onAddItem: () -> Unit,
    onRemoveItem: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Button(enabled = canAddItem, onClick = onAddItem) {
            Text("Add Item")
        }
        Spacer(modifier = Modifier.padding(25.dp))
        Button(enabled = canRemoveItem, onClick = onRemoveItem) {
            Text("Delete Item")
        }
    }
}
```

<br />

#### Key points about the code

- `AddRemoveButtons` displays a row of buttons to perform add and remove operations on the list.
- The `canAddItem` and `canRemoveItem` parameters control the enabled state of the buttons. If `canAddItem` or `canRemoveItem` are false, then the corresponding button is disabled.
- The `onAddItem` and `onRemoveItem` parameters are lambdas that execute when the user clicks the corresponding button.

Finally, this snippet displays three buttons for sorting the list (**Reset,
Alphabetical** , and **Length**):


```kotlin
@Composable
private fun OrderButtons(
    resetOrder: () -> Unit,
    orderAlphabetically: () -> Unit,
    orderByLength: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        var selectedIndex by remember { mutableIntStateOf(0) }
        val options = listOf("Reset", "Alphabetical", "Length")

        SingleChoiceSegmentedButtonRow {
            options.forEachIndexed { index, label ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size
                    ),
                    onClick = {
                        Log.d("AnimatedOrderedList", "selectedIndex: $selectedIndex")
                        selectedIndex = index
                        when (options[selectedIndex]) {
                            "Reset" -> resetOrder()
                            "Alphabetical" -> orderAlphabetically()
                            "Length" -> orderByLength()
                        }
                    },
                    selected = index == selectedIndex
                ) {
                    Text(label)
                }
            }
        }
    }
}
```

<br />

#### Key points about the code

- `OrderButtons` displays a `SingleChoiceSegmentedButtonRow` to allow users to select a sorting method on the list or reset the list order. A [`SegmentedButton`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#(androidx.compose.material3.(androidx.compose.material3.MultiChoiceSegmentedButtonRowScope).SegmentedButton(kotlin.Boolean,kotlin.Function1,androidx.compose.ui.graphics.Shape,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.material3.SegmentedButtonColors,androidx.compose.foundation.BorderStroke,androidx.compose.foundation.layout.PaddingValues,androidx.compose.foundation.interaction.MutableInteractionSource,kotlin.Function0,kotlin.Function0)) component lets you select a single option from a list of options.
- `resetOrder`, `orderAlphabetically`, and `orderByLength` are lambda functions that are executed when the corresponding button is selected.
- The `selectedIndex` state variable keeps track of the selected option.

#### Result

This video shows the result of the preceding snippets when items are
reordered:

<br />

**Figure 1**. A list that animates item transitions when items are added, removed, or sorted.

<br />

## Sticky headers

The 'sticky header' pattern is helpful when displaying lists of grouped data.
Below you can see an example of a 'contacts list', grouped by each contact's
initial:

![Video of a phone scrolling up and down through a contacts list](https://developer.android.com/static/develop/ui/compose/images/lists-scrolling.gif)

To achieve a sticky header with `LazyColumn`, you can use the experimental
[`stickyHeader()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListScope#stickyHeader(kotlin.Any,kotlin.Any,kotlin.Function1))
function, providing the header content:


```kotlin
@Composable
fun ListWithHeader(items: List<Item>) {
    LazyColumn {
        stickyHeader {
            Header()
        }

        items(items) { item ->
            ItemRow(item)
        }
    }
}
```

<br />

To achieve a list with multiple headers, like the 'contacts list' example above,
you could do:


```kotlin
// This ideally would be done in the ViewModel
val grouped = contacts.groupBy { it.firstName[0] }

@Composable
fun ContactsList(grouped: Map<Char, List<Contact>>) {
    LazyColumn {
        grouped.forEach { (initial, contactsForInitial) ->
            stickyHeader {
                CharacterHeader(initial)
            }

            items(contactsForInitial) { contact ->
                ContactListItem(contact)
            }
        }
    }
}
```

<br />

## Reacting to scroll position

Many apps need to react and listen to scroll position and item layout changes.
The Lazy components support this use-case by hoisting the
[`LazyListState`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState):


```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    // Remember our own LazyListState
    val listState = rememberLazyListState()

    // Provide it to LazyColumn
    LazyColumn(state = listState) {
        // ...
    }
}
```

<br />

For simple use-cases, apps commonly only need to know information about the
first visible item. For this
[`LazyListState`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState)
provides the
[`firstVisibleItemIndex`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#firstVisibleItemIndex())
and
[`firstVisibleItemScrollOffset`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#firstVisibleItemScrollOffset())
properties.

If we use the example of showing and hiding a button based on if the user has
scrolled past the first item:


```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    Box {
        val listState = rememberLazyListState()

        LazyColumn(state = listState) {
            // ...
        }

        // Show the button if the first visible item is past
        // the first item. We use a remembered derived state to
        // minimize unnecessary compositions
        val showButton by remember {
            derivedStateOf {
                listState.firstVisibleItemIndex > 0
            }
        }

        AnimatedVisibility(visible = showButton) {
            ScrollToTopButton()
        }
    }
}
```

<br />

> [!NOTE]
> **Note:** The example above uses [`derivedStateOf()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#derivedStateOf(kotlin.Function0)) to minimize unnecessary compositions. For more information, see the [Side
> Effects](https://developer.android.com/develop/ui/compose/side-effects#derivedstateof) documentation.

Reading the state directly in composition is useful when you need to update
other UI composables, but there are also scenarios where the event does not need
to be handled in the same composition. A common example of this is sending an
analytics event once the user has scrolled past a certain point. To handle this
efficiently, we can use a
[`snapshotFlow()`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#snapshotFlow(kotlin.Function0)):


```kotlin
val listState = rememberLazyListState()

LazyColumn(state = listState) {
    // ...
}

LaunchedEffect(listState) {
    snapshotFlow { listState.firstVisibleItemIndex }
        .map { index -> index > 0 }
        .distinctUntilChanged()
        .filter { it }
        .collect {
            MyAnalyticsService.sendScrolledPastFirstItemEvent()
        }
}
```

<br />

`LazyListState` also provides information about all of the items currently being
displayed and their bounds on screen, via the
[`layoutInfo`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#layoutInfo())
property. See the
[`LazyListLayoutInfo`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListLayoutInfo)
class for more information.

## Controlling the scroll position

As well as reacting to scroll position, it's also useful for apps to be able to
control the scroll position too.
[`LazyListState`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState)
supports this via the [`scrollToItem()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#scrollToItem(kotlin.Int,kotlin.Int))
function, which 'immediately' snaps the
scroll position, and [`animateScrollToItem()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#animateScrollToItem(kotlin.Int,kotlin.Int))
which scrolls using an animation (also known as a smooth scroll):

> [!NOTE]
> **Note:** Both [`scrollToItem()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#scrollToItem(kotlin.Int,kotlin.Int)) and [`animateScrollToItem()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyListState#animateScrollToItem(kotlin.Int,kotlin.Int)) are suspending functions, which means that we need to invoke them in a coroutine. See our [coroutines documentation](https://developer.android.com/develop/ui/compose/kotlin#coroutines) for more information on how to do that in Compose.


```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    val listState = rememberLazyListState()
    // Remember a CoroutineScope to be able to launch
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(state = listState) {
        // ...
    }

    ScrollToTopButton(
        onClick = {
            coroutineScope.launch {
                // Animate scroll to the first item
                listState.animateScrollToItem(index = 0)
            }
        }
    )
}
```

<br />

## Large data-sets (paging)

The [Paging library](https://developer.android.com/topic/libraries/architecture/paging/v3-overview) enables apps to
support large lists of items, loading and displaying small chunks of the list as
necessary. Paging 3.0 and later provides Compose support through the
`androidx.paging:paging-compose` library.

> [!NOTE]
> **Note:** Compose support is provided only for Paging 3.0 and later. If you're using an earlier version of the Paging library, you need to [migrate to
> 3.0](https://developer.android.com/topic/libraries/architecture/paging/v3-migration) first.

To display a list of paged content, we can use the
[`collectAsLazyPagingItems()`](https://developer.android.com/reference/kotlin/androidx/paging/compose/package-summary#collectaslazypagingitems)
extension function, and then pass in the returned
[`LazyPagingItems`](https://developer.android.com/reference/kotlin/androidx/paging/compose/LazyPagingItems)
to `items()` in our `LazyColumn`. Similar to Paging support in views, you can
display placeholders while data loads by checking if the `item` is `null`:


```kotlin
@Composable
fun MessageList(pager: Pager<Int, Message>) {
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn {
        items(
            lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it.id }
        ) { index ->
            val message = lazyPagingItems[index]
            if (message != null) {
                MessageRow(message)
            } else {
                MessagePlaceholder()
            }
        }
    }
}
```

<br />

> [!WARNING]
> **Warning:** If you use a [`RemoteMediator`](https://developer.android.com/reference/kotlin/androidx/paging/RemoteMediator) to fetch data from a network service, make sure to provide realistically sized placeholder items. If you use a `RemoteMediator`, it will be repeatedly invoked to fetch new data, up until the screen has been filled with content. If small placeholders are provided (or no placeholder at all), the screen might never be filled, and your app will fetch many pages of data.

## Tips on using Lazy layouts

There are a few tips you can take into account to ensure your Lazy layouts work as intended.

### Avoid using 0-pixel sized items

This can happen in scenarios where, for example, you expect to asynchronously
retrieve some data like images, to fill your list's items at a later stage.
That would cause the Lazy layout to compose all of its items in the first
measurement, as their height is 0 pixels and it could fit them all in the
viewport. Once the items have loaded and their height expanded, Lazy layouts
would then discard all of the other items that have unnecessarily been composed
the first time around as they cannot in fact fit the viewport. To avoid this,
you should set default sizing to your items, so that the Lazy layout can do the
correct calculation of how many items can in fact fit in the viewport:


```kotlin
@Composable
fun Item(imageUrl: String) {
    AsyncImage(
        model = rememberAsyncImagePainter(model = imageUrl),
        modifier = Modifier.size(30.dp),
        contentDescription = null
        // ...
    )
}
```

<br />

When you know the approximate size of your items after the data is
asynchronously loaded, a good practice is to ensure your items' sizing remains
the same before and after loading, for example, by adding some placeholders.
This will help maintain the correct scroll position.

### Avoid nesting components scrollable in the same direction

This applies only to cases when nesting scrollable children without a predefined
size inside another same direction scrollable parent. For example, trying to
nest a child `LazyColumn` without a fixed height inside a vertically scrollable
`Column` parent:


```kotlin
// throws IllegalStateException
Column(
    modifier = Modifier.verticalScroll(state)
) {
    LazyColumn {
        // ...
    }
}
```

<br />

Instead, the same result can be achieved by wrapping all of your composables
inside one parent `LazyColumn` and using its DSL to pass in different types of
content. This enables emitting single items, as well as multiple list items,
all in one place:


```kotlin
LazyColumn {
    item {
        Header()
    }
    items(data) { item ->
        PhotoItem(item)
    }
    item {
        Footer()
    }
}
```

<br />

Keep in mind that cases where you're nesting different direction layouts,
for example, a scrollable parent `Row` and a child `LazyColumn`, are allowed:


```kotlin
Row(
    modifier = Modifier.horizontalScroll(scrollState)
) {
    LazyColumn {
        // ...
    }
}
```

<br />

As well as cases where you still use the same direction layouts, but also set
a fixed size to the nested children:


```kotlin
Column(
    modifier = Modifier.verticalScroll(scrollState)
) {
    LazyColumn(
        modifier = Modifier.height(200.dp)
    ) {
        // ...
    }
}
```

<br />

### Beware of putting multiple elements in one item

In this example, the second item lambda emits 2 items in one block:


```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(100.dp)
) {
    item { Item(0) }
    item {
        Item(1)
        Item(2)
    }
    item { Item(3) }
    // ...
}
```

<br />

Lazy layouts will handle this as expected - they will lay out elements one
after another as if they were different items. However, there are a couple
of problems with doing so.

When multiple elements are emitted as part of one item, they are handled as
one entity, meaning that they cannot be composed individually anymore. If one
element becomes visible on the screen, then all elements corresponding to the
item have to be composed and measured. This can hurt performance if used
excessively. In the extreme case of putting all elements in one item, it
completely defeats the purpose of using Lazy layouts. Apart from potential
performance issues, putting more elements in one item will also interfere
with `scrollToItem()` \& `animateScrollToItem()`.

However, there are valid use cases for putting multiple elements in one item,
like having dividers inside a list. You don't want dividers to change
scrolling indexes, as they shouldn't be considered independent elements.
Also, performance is not affected as dividers are small. A divider likely
needs to be visible when the item before it is visible, so it can be
part of the previous item:


```kotlin
LazyVerticalGrid(
    columns = GridCells.Adaptive(100.dp)
) {
    item { Item(0) }
    item {
        Item(1)
        Divider()
    }
    item { Item(2) }
    // ...
}
```

<br />

### Consider using custom arrangements

Usually Lazy lists have many items, and they occupy more than the size of the
scrolling container. However, when your list is populated with few items, your
design can have more specific requirements for how these should be positioned
in the viewport.

To achieve this, you can use custom vertical
[`Arrangement`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Arrangement)
and pass it to the `LazyColumn`. In the following example, the `TopWithFooter`
object only needs to implement the `arrange` method. Firstly, it will position
items one after another. Secondly, if the total used height is lower than the
viewport height, it will position the footer at the bottom:


```kotlin
object TopWithFooter : Arrangement.Vertical {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray
    ) {
        var y = 0
        sizes.forEachIndexed { index, size ->
            outPositions[index] = y
            y += size
        }
        if (y < totalSize) {
            val lastIndex = outPositions.lastIndex
            outPositions[lastIndex] = totalSize - sizes.last()
        }
    }
}
```

<br />

### Consider adding `contentType`

Starting with Compose 1.2, in order to maximize the performance of your Lazy
layout, consider adding
[`contentType`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary#extension-functions-summary)
to your lists or grids. This allows you to specify the content type for each
item of the layout, in cases where you're composing a list or a grid consisting
of multiple different types of items:


```kotlin
LazyColumn {
    items(elements, contentType = { it.type }) {
        // ...
    }
}
```

<br />

When you provide the
[`contentType`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/package-summary#extension-functions-summary),
Compose is able to reuse compositions only
between the items of the same type. As reusing is more efficient when you
compose items of similar structure, providing the content types ensures
Compose doesn't try to compose an item of type A on top of a completely
different item of type B. This helps maximize the benefits of composition
reusing and your Lazy layout performance.

### Measuring performance

You can only reliably measure the performance of a Lazy layout when running in
release mode and with R8 optimization enabled. On debug builds, Lazy layout
scrolling may appear slower. For more information on this, read through
[Compose performance](https://developer.android.com/develop/ui/compose/performance).

## Additional resources

- [Create a finite scrollable list](https://developer.android.com/develop/ui/compose/quick-guides/content/finite-scrolling-list)
- [Create a scrollable grid](https://developer.android.com/develop/ui/compose/quick-guides/content/create-scrollable-grid)
- [Display nested scrolling items in a list](https://developer.android.com/develop/ui/compose/quick-guides/content/display-nested-list)
- [Filter a list while typing](https://developer.android.com/develop/ui/compose/quick-guides/content/filter-list-while-typing)
- [Lazily load data with lists and Paging](https://developer.android.com/develop/ui/compose/quick-guides/content/lazily-load-list)
- [Build a list using multiple item types](https://developer.android.com/develop/ui/compose/quick-guides/content/build-list-multiple-item-types)
- [Video: Lists in Compose](https://developer.android.com/develop/ui/compose/quick-guides/content/video/lists-in-compose)

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Migrate `RecyclerView` to Lazy list](https://developer.android.com/develop/ui/compose/migrate/migration-scenarios/recycler-view)
- [Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving)
- [Kotlin for Jetpack Compose](https://developer.android.com/develop/ui/compose/kotlin)