[`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal) is a tool for
passing data down through the Composition implicitly. In this page, you'll
learn what a `CompositionLocal` is in more detail, how to create your own
`CompositionLocal`, and know if a `CompositionLocal` is a good solution for
your use case.

## Introduction to `CompositionLocal`

Usually in Compose, [data flows down](https://developer.android.com/develop/ui/compose/architecture) through
the UI tree as parameters to each composable function. This makes a composable's
dependencies explicit. However, this can be cumbersome for data that is very
frequently and widely used, such as colors or type styles. See the following
example:


```kotlin
@Composable
fun MyApp() {
    // Theme information tends to be defined near the root of the application
    val colors = colors()
}

// Some composable deep in the hierarchy
@Composable
fun SomeTextLabel(labelText: String) {
    Text(
        text = labelText,
        color = colors.onPrimary // ← need to access colors here
    )
}
```

<br />

To support not needing to pass the colors as an explicit parameter dependency to
most composables, **Compose offers [`CompositionLocal`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal), which allows you
to create tree-scoped named objects that can be used as an implicit way to have
data flow through the UI tree.**

`CompositionLocal` elements are usually provided with a value in a certain node
of the UI tree. That value can be used by its composable descendants without
declaring the `CompositionLocal` as a parameter in the composable function.

Key terms: In this guide, we use the terms **Composition** ,
**UI tree** , and **UI hierarchy**. Although they might be used
interchangeably in other guides, they have different meanings:

- **The Composition** is the record of the call graph of composable functions.
- The **UI tree** or **UI hierarchy** is the tree of [`LayoutNode`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/ui/ui/src/commonMain/kotlin/androidx/compose/ui/node/LayoutNode.kt) constructed, updated, and maintained by the composition process.

`CompositionLocal` is what the Material theme uses under the hood.
[`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material3/MaterialTheme) is
an object that provides three `CompositionLocal` instances: `colorScheme`,
`typography` and `shapes`, allowing you to retrieve them later in any descendant
part of the Composition.
Specifically, these are the `LocalColorScheme`, `LocalShapes`, and
`LocalTypography` properties that you can access through the `MaterialTheme`
`colorScheme`, `shapes`, and `typography` attributes.


```kotlin
@Composable
fun MyApp() {
    // Provides a Theme whose values are propagated down its `content`
    MaterialTheme {
        // New values for colorScheme, typography, and shapes are available
        // in MaterialTheme's content lambda.

        // ... content here ...
    }
}

// Some composable deep in the hierarchy of MaterialTheme
@Composable
fun SomeTextLabel(labelText: String) {
    Text(
        text = labelText,
        // `primary` is obtained from MaterialTheme's
        // LocalColors CompositionLocal
        color = MaterialTheme.colorScheme.primary
    )
}
```

<br />

A **`CompositionLocal` instance is scoped to a part of the Composition** so you
can provide different values at different levels of the tree. The [`current`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal#current()) value
of a `CompositionLocal` corresponds to the closest value provided by an
ancestor in that part of the Composition.

**To provide a new value to a `CompositionLocal`, use the
[`CompositionLocalProvider`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocalProvider.composable#CompositionLocalProvider(kotlin.Array,kotlin.Function0))**
and its [`provides`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/ProvidableCompositionLocal#provides(kotlin.Any))
infix function that associates a `CompositionLocal` key to a `value`. The
`content` lambda of the `CompositionLocalProvider` will get the provided
value when accessing the `current` property of the `CompositionLocal`. When a
new value is provided, Compose recomposes parts of the Composition that read
the `CompositionLocal`.

As an example of this, the [`LocalContentColor`](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#LocalContentColor()) `CompositionLocal` contains
the preferred content color used for text and
iconography to ensure it contrasts against the current background color. In the
following example, `CompositionLocalProvider` is used to provide different
values for different parts of the Composition.


```kotlin
@Composable
fun CompositionLocalExample() {
    MaterialTheme {
        // Surface provides contentColorFor(MaterialTheme.colorScheme.surface) by default
        // This is to automatically make text and other content contrast to the background
        // correctly.
        Surface {
            Column {
                Text("Uses Surface's provided content color")
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.primary) {
                    Text("Primary color provided by LocalContentColor")
                    Text("This Text also uses primary as textColor")
                    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                        DescendantExample()
                    }
                }
            }
        }
    }
}

@Composable
fun DescendantExample() {
    // CompositionLocalProviders also work across composable functions
    Text("This Text uses the error color now")
}
```

<br />

![Preview of the CompositionLocalExample composable.](https://developer.android.com/static/develop/ui/compose/images/compositionlocal-color.png) **Figure 1.** Preview of the `CompositionLocalExample` composable.

In the last example, the `CompositionLocal` instances were internally used
by Material composables. To access the current value of a `CompositionLocal`,
use its [`current`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal#current())
property. In the following example, the current [`Context`](https://developer.android.com/reference/android/content/Context) value of the
[`LocalContext`](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/package-summary#LocalContext()) `CompositionLocal` that is commonly used in Android apps is
used to format the text:


```kotlin
@Composable
fun FruitText(fruitSize: Int) {
    // Get `resources` from the current value of LocalContext
    val resources = LocalContext.current.resources
    val fruitText = remember(resources, fruitSize) {
        resources.getQuantityString(R.plurals.fruit_title, fruitSize)
    }
    Text(text = fruitText)
}
```

<br />

> [!NOTE]
> **Note:** `CompositionLocal` objects or constants are usually prefixed with `Local` to allow better discoverability with auto-complete in the IDE.

## Create your own `CompositionLocal`

`CompositionLocal` is a tool for passing data down through the Composition
implicitly.

Another key signal for using `CompositionLocal` is when the parameter is
cross-cutting and intermediate layers of implementation shouldn't be aware
it exists, because making those intermediate layers aware would limit the
utility of the composable. For example, querying for Android permissions is
afforded by a `CompositionLocal` under the hood. A media picker composable
can add new functionality to access permission-protected content on the
device without changing its API and requiring callers of the media picker to
be aware of this added context used from the environment.

However, `CompositionLocal` is not always the best solution. We
discourage *overusing* `CompositionLocal` as it comes with some downsides:

**`CompositionLocal` makes a composable's behavior harder to reason about** . As
they create implicit dependencies, callers of composables that use them need
to make sure that a value for every `CompositionLocal` is satisfied.

Furthermore, there might be no clear source of truth for this dependency as it
can mutate in any part of the Composition. Thus, **debugging the app when a
problem occurs can be more challenging** as you need to navigate up the
Composition to see where the `current` value was provided. Tools such as *Find
usages* in the IDE or the [Compose layout inspector](https://developer.android.com/develop/ui/compose/tooling#layout-inspector) provide enough
information to mitigate this issue.

> [!NOTE]
> **Note:** `CompositionLocal` works well for foundational architecture and Jetpack Compose makes heavy use of it.

### Decide whether to use `CompositionLocal`

There are certain conditions that can make `CompositionLocal` a good solution
for your use case:

A **`CompositionLocal` should have a good default value** . If there's no default
value, you must guarantee that it is exceedingly difficult for a developer to
get into a situation where a value for the `CompositionLocal` isn't provided.
Not providing a default value can cause problems and frustration when creating
tests or previewing a composable that uses that `CompositionLocal` will always
require it to be explicitly provided.

**Avoid `CompositionLocal` for concepts that aren't thought as *tree-scoped or
sub-hierarchy scoped*** . A `CompositionLocal` makes sense when it can be
potentially used by any descendant, not by a few of them.

If your use case doesn't meet these requirements, check out the
[Alternatives to consider](https://developer.android.com/develop/ui/compose/compositionlocal#alternatives) section before creating a
`CompositionLocal`.

An example of a bad practice is creating a `CompositionLocal` that holds the
`ViewModel` of a particular screen so that all composables in that screen can
get a reference to the `ViewModel` to perform some logic. This is a bad practice
because not all composables below a particular UI tree need to know about a
`ViewModel`. The good practice is to pass to composables only the information
that they need following the pattern that
[state flows down and events flow up](https://developer.android.com/develop/ui/compose/architecture).
This approach will make your composables more reusable and easier to test.

### Create a `CompositionLocal`

There are two APIs to create a `CompositionLocal`:

- [`compositionLocalOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#compositionLocalOf(androidx.compose.runtime.SnapshotMutationPolicy,kotlin.Function0)):
  Changing the value provided during recomposition invalidates *only*
  the content that reads its
  [`current`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocal#current()) value.

- [`staticCompositionLocalOf`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#staticCompositionLocalOf(kotlin.Function0)):
  Unlike `compositionLocalOf`, reads of a `staticCompositionLocalOf` are not
  tracked by Compose. Changing the value causes the entirety of the `content`
  lambda where the `CompositionLocal` is provided to be recomposed, instead of
  just the places where the `current` value is read in the Composition.

If the value provided to the `CompositionLocal` is highly unlikely to change or
will never change, use `staticCompositionLocalOf` to get performance benefits.

For example, an app's design system might be opinionated in the way composables
are elevated using a shadow for the UI component. Since the different
elevations for the app should propagate throughout the UI tree, we use a
`CompositionLocal`. As the `CompositionLocal` value is derived conditionally
based on the system theme, we use the `compositionLocalOf` API:


```kotlin
// LocalElevations.kt file

data class Elevations(val card: Dp = 0.dp, val default: Dp = 0.dp)

// Define a CompositionLocal global object with a default
// This instance can be accessed by all composables in the app
val LocalElevations = compositionLocalOf { Elevations() }
```

<br />

### Provide values to a `CompositionLocal`

**The [`CompositionLocalProvider`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/CompositionLocalProvider.composable#CompositionLocalProvider(kotlin.Array,kotlin.Function0))
composable binds values to `CompositionLocal` instances for the given
hierarchy** . To provide a new value to a `CompositionLocal`, use the
[`provides`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/ProvidableCompositionLocal#provides(kotlin.Any))
infix function that associates a `CompositionLocal` key to a `value` as follows:


```kotlin
// MyActivity.kt file

class MyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Calculate elevations based on the system theme
            val elevations = if (isSystemInDarkTheme()) {
                Elevations(card = 1.dp, default = 1.dp)
            } else {
                Elevations(card = 0.dp, default = 0.dp)
            }

            // Bind elevation as the value for LocalElevations
            CompositionLocalProvider(LocalElevations provides elevations) {
                // ... Content goes here ...
                // This part of Composition will see the `elevations` instance
                // when accessing LocalElevations.current
            }
        }
    }
}
```

<br />

### Consuming the `CompositionLocal`

[`CompositionLocal.current`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/ProvidableCompositionLocal#current()) returns the value provided by the nearest
`CompositionLocalProvider` that provides a value to that `CompositionLocal`:


```kotlin
@Composable
fun SomeComposable() {
    // Access the globally defined LocalElevations variable to get the
    // current Elevations in this part of the Composition
    MyCard(elevation = LocalElevations.current.card) {
        // Content
    }
}
```

<br />

## Alternatives to consider

A `CompositionLocal` might be an excessive solution for some use cases. If your
use case doesn't meet the criteria specified in the [Deciding whether to use
CompositionLocal](https://developer.android.com/develop/ui/compose/compositionlocal#deciding) section, another solution might likely be better
suited for your use case.

### Pass explicit parameters

Being explicit about composable's dependencies is a good habit. We recommend
that you **pass composables *only* what they need**. To encourage decoupling
and reuse of composables, each composable should hold the least amount of
information possible.


```kotlin
@Composable
fun MyComposable(myViewModel: MyViewModel = viewModel()) {
    // ...
    MyDescendant(myViewModel.data)
}

// Don't pass the whole object! Just what the descendant needs.
// Also, don't  pass the ViewModel as an implicit dependency using
// a CompositionLocal.
@Composable
fun MyDescendant(myViewModel: MyViewModel) { /* ... */ }

// Pass only what the descendant needs
@Composable
fun MyDescendant(data: DataToDisplay) {
    // Display data
}
```

<br />

### Inversion of control

Another way to avoid passing unnecessary dependencies to a composable is
using *inversion of control*. Instead of the descendant taking in a
dependency to execute some logic, the parent does that instead.

See the following example where a descendant needs to trigger the request to
load some data:


```kotlin
@Composable
fun MyComposable(myViewModel: MyViewModel = viewModel()) {
    // ...
    MyDescendant(myViewModel)
}

@Composable
fun MyDescendant(myViewModel: MyViewModel) {
    Button(onClick = { myViewModel.loadData() }) {
        Text("Load data")
    }
}
```

<br />

Depending on the case, `MyDescendant` might have a lot of responsibility. Also,
passing `MyViewModel` as a dependency makes `MyDescendant` less reusable since
they're now coupled together. Consider the alternative that doesn't pass the
dependency into the descendant and uses inversion of control principles that
makes the ancestor responsible for executing the logic:


```kotlin
@Composable
fun MyComposable(myViewModel: MyViewModel = viewModel()) {
    // ...
    ReusableLoadDataButton(
        onLoadClick = {
            myViewModel.loadData()
        }
    )
}

@Composable
fun ReusableLoadDataButton(onLoadClick: () -> Unit) {
    Button(onClick = onLoadClick) {
        Text("Load data")
    }
}
```

<br />

This approach can be better suited for some use cases as it **decouples the
child from its immediate ancestors**. Ancestor composables tend to become more
complex in favor of having more flexible lower-level composables.

Similarly, `@Composable` content lambdas can be used in the same way to get
the same benefits:


```kotlin
@Composable
fun MyComposable(myViewModel: MyViewModel = viewModel()) {
    // ...
    ReusablePartOfTheScreen(
        content = {
            Button(
                onClick = {
                    myViewModel.loadData()
                }
            ) {
                Text("Confirm")
            }
        }
    )
}

@Composable
fun ReusablePartOfTheScreen(content: @Composable () -> Unit) {
    Column {
        // ...
        content()
    }
}
```

<br />

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Anatomy of a theme in Compose](https://developer.android.com/develop/ui/compose/designsystems/anatomy)
- [Using Views in Compose](https://developer.android.com/develop/ui/compose/migrate/interoperability-apis/views-in-compose)
- [Kotlin for Jetpack Compose](https://developer.android.com/develop/ui/compose/kotlin)