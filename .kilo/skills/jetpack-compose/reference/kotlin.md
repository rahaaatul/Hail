Jetpack Compose is built around Kotlin. In some cases, Kotlin provides special
idioms that make it easier to write good Compose code. If you think in another
programming language and mentally translate that language to Kotlin, you're
likely to miss out on some of the strength of Compose, and you might find it
difficult to understand idiomatically-written Kotlin code. Gaining more
familiarity with Kotlin's style can help you avoid those pitfalls.

## Default arguments

When you write a Kotlin function, you can specify *[default values for function
arguments](https://kotlinlang.org/docs/reference/functions.html#default-arguments)*, used if the caller doesn't explicitly pass
those values. This feature reduces the need for overloaded functions.

For example, suppose you want to write a function that draws a square. That
function might have a single required parameter, **sideLength** , specifying the length
of each side. It might have several optional parameters, like **thickness** ,
**edgeColor** and so on; if the caller doesn't specify those, the
function uses default values. In other languages, you might expect to write
several functions:

```java
// We don't need to do this in Kotlin!
void drawSquare(int sideLength) { }

void drawSquare(int sideLength, int thickness) { }

void drawSquare(int sideLength, int thickness, Color edgeColor) { }
```

In Kotlin, you can write a single function and specify the default values for
the arguments:


```kotlin
fun drawSquare(
    sideLength: Int,
    thickness: Int = 2,
    edgeColor: Color = Color.Black
) {
}
```

<br />

Besides saving you from having to write multiple redundant functions, this
feature makes your code much clearer to read. If the caller doesn't specify a
value for an argument, that indicates that they're willing to use the default
value. In addition, the named parameters make it much easier to see what's going
on. If you look at the code and see a function call like this, you might not
know what the parameters mean without checking the `drawSquare()` code:

```java
drawSquare(30, 5, Color.Red);
```

By contrast, this code is self-documenting:


```kotlin
drawSquare(sideLength = 30, thickness = 5, edgeColor = Color.Red)
```

<br />

Most Compose libraries use default arguments, and it's a good practice to do the
same for the composable functions that you write. This practice makes your
composables customizable, but still makes the default behavior simple to invoke.
So, for example, you might create a simple text element like this:


```kotlin
Text(text = "Hello, Android!")
```

<br />

That code has the same effect as the following, much more verbose code, in which
more of the [`Text`](https://developer.android.com/reference/kotlin/androidx/compose/material/Text.composable) parameters are set explicitly:


```kotlin
Text(
    text = "Hello, Android!",
    color = Color.Unspecified,
    fontSize = TextUnit.Unspecified,
    letterSpacing = TextUnit.Unspecified,
    overflow = TextOverflow.Clip
)
```

<br />

Not only is the first code snippet much simpler and easier to read, it's also
self-documenting. By specifying only the `text` parameter, you document that for
all the other parameters, you want to use the default values. By contrast, the
second snippet implies that you want to explicitly set the values for those
other parameters, though the values you set happen to be the default values for
the function.

## Higher-order functions and lambda expressions

Kotlin supports [*higher-order functions*](https://kotlinlang.org/docs/reference/lambdas.html), functions that
receive other functions as parameters. Compose builds upon this approach. For
example, the [`Button`](https://developer.android.com/reference/kotlin/androidx/compose/material/Button.composable#Button(kotlin.Function0,androidx.compose.ui.Modifier,kotlin.Boolean,androidx.compose.foundation.interaction.MutableInteractionSource,androidx.compose.material.ButtonElevation,androidx.compose.ui.graphics.Shape,androidx.compose.foundation.BorderStroke,androidx.compose.material.ButtonColors,androidx.compose.foundation.layout.PaddingValues,kotlin.Function1)) composable function provides an `onClick`
lambda parameter. The value of that parameter is a function, which the button
calls when the user clicks it:


```kotlin
Button(
    // ...
    onClick = myClickFunction
)
// ...
```

<br />

Higher-order functions pair naturally with *lambda expressions* , expressions
which evaluate to a function. If you only need the function once, you don't
have to define it elsewhere to pass it to the higher-order function. Instead,
you can just define the function right there with a lambda expression.
The previous example assumes that `myClickFunction()` is defined elsewhere.
But if you only use that function here, it's simpler to just define the function
inline with a lambda expression:


```kotlin
Button(
    // ...
    onClick = {
        // do something
        // do something else
    }
) { /* ... */ }
```

<br />

### Trailing lambdas

Kotlin offers a special syntax for calling higher-order functions whose *last*
parameter is a lambda. If you want to pass a lambda expression as that
parameter, you can use [*trailing lambda syntax*](https://kotlinlang.org/docs/lambdas.html#passing-trailing-lambdas).
Instead of putting the lambda expression within the parentheses, you put it
afterwards. This is a common situation in Compose, so you need to be familiar
with how the code looks.

For example, the last parameter to all layouts, such as the
[`Column()`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/layout/Column.composable#Column(androidx.compose.ui.Modifier,androidx.compose.foundation.layout.Arrangement.Vertical,androidx.compose.ui.Alignment.Horizontal,kotlin.Function1)) composable function, is `content`, a function which
emits the child UI elements. Suppose you wanted to create a column containing
three text elements, and you need to apply some formatting. This code would
work, but it's very cumbersome:


```kotlin
Column(
    modifier = Modifier.padding(16.dp),
    content = {
        Text("Some text")
        Text("Some more text")
        Text("Last text")
    }
)
```

<br />

Because the `content` parameter is the last one in the function signature, and
we're passing its value as a lambda expression, we can pull it out of the
parentheses:


```kotlin
Column(modifier = Modifier.padding(16.dp)) {
    Text("Some text")
    Text("Some more text")
    Text("Last text")
}
```

<br />

The two examples have exactly the same meaning. The braces define the lambda
expression that is passed to the `content` parameter.

In fact, if the *only* parameter you're passing is that trailing lambda---that is,
if the final parameter is a lambda, and you aren't passing any other
parameters---you can omit the parentheses altogether. So, for example, suppose you
didn't need to pass a modifier to the `Column`. You could write the code like
this:


```kotlin
Column {
    Text("Some text")
    Text("Some more text")
    Text("Last text")
}
```

<br />

This syntax is quite common in Compose, especially for layout elements like
`Column`. The last parameter is a lambda expression defining the element's
children, and those children are specified in braces after the function call.

## Scopes and receivers

Some methods and properties are only available in a certain scope. The limited
scope lets you offer functionality where it's needed and avoid accidentally
using that functionality where it isn't appropriate.

Consider an example used in Compose. When you call the `Row` layout
composable, your content lambda is automatically invoked within a `RowScope`.
This enables `Row` to expose functionality which is only valid within a `Row`.
The following example demonstrates how `Row` has exposed a row-specific value
for the `align` modifier:


```kotlin
Row {
    Text(
        text = "Hello world",
        // This Text is inside a RowScope so it has access to
        // Alignment.CenterVertically but not to
        // Alignment.CenterHorizontally, which would be available
        // in a ColumnScope.
        modifier = Modifier.align(Alignment.CenterVertically)
    )
}
```

<br />

Some APIs accept lambdas which are called in *receiver scope*. Those lambdas
have access to properties and functions that are defined elsewhere, based on the
parameter declaration:


```kotlin
Box(
    modifier = Modifier.drawBehind {
        // This method accepts a lambda of type DrawScope.() -> Unit
        // therefore in this lambda we can access properties and functions
        // available from DrawScope, such as the `drawRectangle` function.
        drawRect(
            /*...*/
            /* ...
        )
    }
)
```

<br />

For more information, see [function literals with receiver](https://kotlinlang.org/docs/reference/lambdas.html#function-literals-with-receiver) in
the Kotlin documentation.

## Delegated properties

Kotlin supports [delegated properties](https://kotlinlang.org/docs/reference/delegated-properties.html). These properties
are called as if they were fields, but their value is determined dynamically by
evaluating an expression. You can recognize these properties by their use of
the `by` syntax:


```kotlin
class DelegatingClass {
    var name: String by nameGetterFunction()

    // ...
}
```

<br />

Other code can access the property with code like this:


```kotlin
val myDC = DelegatingClass()
println("The name property is: " + myDC.name)
```

<br />

When `println()` executes, `nameGetterFunction()` is called to return the value
of the string.

These delegated properties are particularly useful when you're working with
state-backed properties:


```kotlin
var showDialog by remember { mutableStateOf(false) }

// Updating the var automatically triggers a state change
showDialog = true
```

<br />

> [!NOTE]
> **Note:** Learn more about `remember` and `mutableStateOf` in the [State in Jetpack Compose documentation](https://developer.android.com/develop/ui/compose/state).

## Destructuring data classes

If you define a [data class](https://kotlinlang.org/docs/reference/data-classes.html), you can access the data with a
[destructuring declaration](https://kotlinlang.org/docs/reference/multi-declarations.html). For example, suppose you
define a `Person` class:


```kotlin
data class Person(val name: String, val age: Int)
```

<br />

If you have an object of that type, you can access its values with code like
this:


```kotlin
val mary = Person(name = "Mary", age = 35)

// ...

val (name, age) = mary
```

<br />

You'll often see that kind of code in Compose functions:


```kotlin
Row {

    val (image, title, subtitle) = createRefs()

    // The `createRefs` function returns a data object;
    // the first three components are extracted into the
    // image, title, and subtitle variables.

    // ...
}
```

<br />

Data classes provide a lot of other useful functionality. For example, when you
define a data class, the compiler automatically defines useful functions like
`equals()` and `copy()`. You can find more information in the
[data classes](https://kotlinlang.org/docs/reference/data-classes.html) documentation.

## Singleton objects

Kotlin lets you declare *singletons* , classes which always have one and only one
instance. These singletons are declared with the [`object` keyword](https://kotlinlang.org/docs/reference/object-declarations.html#object-declarations).
Compose often makes use of such objects. For example,
[`MaterialTheme`](https://developer.android.com/reference/kotlin/androidx/compose/material/MaterialTheme) is defined as a singleton object; the
`MaterialTheme.colors`, `shapes`, and `typography` properties all contain the
values for the current theme.

## Type-safe builders and DSLs

Kotlin allows creating [domain-specific languages (DSLs)](https://wikipedia.org/wiki/Domain-specific_language)
with type-safe builders. DSLs allow building complex hierarchical data
structures in a more maintainable and readable way.

Jetpack Compose uses DSLs for some APIs such as [`LazyRow`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyRow.composable) and
[`LazyColumn`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/lazy/LazyColumn.composable).


```kotlin
@Composable
fun MessageList(messages: List<Message>) {
    LazyColumn {
        // Add a single item as a header
        item {
            Text("Message List")
        }

        // Add list of messages
        items(messages) { message ->
            Message(message)
        }
    }
}
```

<br />

Kotlin enables type-safe builders using
[function literals with receiver](https://kotlinlang.org/docs/lambdas.html#function-literals-with-receiver).
If we take the [`Canvas`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/Canvas.composable#Canvas(androidx.compose.ui.Modifier,kotlin.Function1)) composable as an example, it takes as a
parameter a function with [`DrawScope`](https://developer.android.com/reference/kotlin/androidx/compose/ui/graphics/drawscope/DrawScope) as the receiver,
`onDraw: DrawScope.() -> Unit`, allowing the block of code to call member
functions defined in `DrawScope`.


```kotlin
Canvas(Modifier.size(120.dp)) {
    // Draw grey background, drawRect function is provided by the receiver
    drawRect(color = Color.Gray)

    // Inset content by 10 pixels on the left/right sides
    // and 12 by the top/bottom
    inset(10.0f, 12.0f) {
        val quadrantSize = size / 2.0f

        // Draw a rectangle within the inset bounds
        drawRect(
            size = quadrantSize,
            color = Color.Red
        )

        rotate(45.0f) {
            drawRect(size = quadrantSize, color = Color.Blue)
        }
    }
}
```

<br />

Learn more about type-safe builders and DSLs in
[Kotlin's documentation](https://kotlinlang.org/docs/type-safe-builders.html).

## Kotlin coroutines

Coroutines offer asynchronous programming support at the language level in
Kotlin. Coroutines can *suspend* execution without blocking threads. A
responsive UI is inherently asynchronous, and Jetpack Compose solves this by
embracing coroutines at the API level instead of using callbacks.

Jetpack Compose offers APIs that make using coroutines safe within the UI layer.
The [`rememberCoroutineScope`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/rememberCoroutineScope.composable#rememberCoroutineScope(kotlin.Function0)) function returns
a [`CoroutineScope`](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/-coroutine-scope/) with which you can create coroutines
in event handlers and call Compose suspend APIs. See the following example
using the [`ScrollState`](https://developer.android.com/reference/kotlin/androidx/compose/foundation/ScrollState)'s `animateScrollTo` API.


```kotlin
// Create a CoroutineScope that follows this composable's lifecycle
val composableScope = rememberCoroutineScope()
Button(
    // ...
    onClick = {
        // Create a new coroutine that scrolls to the top of the list
        // and call the ViewModel to load data
        composableScope.launch {
            scrollState.animateScrollTo(0) // This is a suspend function
            viewModel.loadData()
        }
    }
) { /* ... */ }
```

<br />

Coroutines execute the block of code *sequentially* by default. A running
coroutine that calls a suspend function *suspends* its execution until the
suspend function returns. This is true even if the suspend function moves the
execution to a different `CoroutineDispatcher`. In the previous example,
`loadData` won't be executed until the suspend function `animateScrollTo`
returns.

To execute code concurrently, new coroutines need to be created. In the example
above, to parallelize scrolling to the top of the screen and loading data from
`viewModel`, two coroutines are needed.


```kotlin
// Create a CoroutineScope that follows this composable's lifecycle
val composableScope = rememberCoroutineScope()
Button( // ...
    onClick = {
        // Scroll to the top and load data in parallel by creating a new
        // coroutine per independent work to do
        composableScope.launch {
            scrollState.animateScrollTo(0)
        }
        composableScope.launch {
            viewModel.loadData()
        }
    }
) { /* ... */ }
```

<br />

Coroutines make it easier to combine asynchronous APIs. In the following
example, we combine the `pointerInput` modifier with the animation APIs to
animate the position of an element when the user taps on the screen.


```kotlin
@Composable
fun MoveBoxWhereTapped() {
    // Creates an `Animatable` to animate Offset and `remember` it.
    val animatedOffset = remember {
        Animatable(Offset(0f, 0f), Offset.VectorConverter)
    }

    Box(
        // The pointerInput modifier takes a suspend block of code
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // Create a new CoroutineScope to be able to create new
                // coroutines inside a suspend function
                coroutineScope {
                    while (true) {
                        // Wait for the user to tap on the screen and animate
                        // in the same block
                        awaitPointerEventScope {
                            val offset = awaitFirstDown().position

                            // Launch a new coroutine to asynchronously animate to
                            // where the user tapped on the screen
                            launch {
                                // Animate to the pressed position
                                animatedOffset.animateTo(offset)
                            }
                        }
                    }
                }
            }
    ) {
        Text("Tap anywhere", Modifier.align(Alignment.Center))
        Box(
            Modifier
                .offset {
                    // Use the animated offset as the offset of this Box
                    IntOffset(
                        animatedOffset.value.x.roundToInt(),
                        animatedOffset.value.y.roundToInt()
                    )
                }
                .size(40.dp)
                .background(Color(0xff3c1361), CircleShape)
        )
    }
```

<br />

To learn more about Coroutines, check out the
[Kotlin coroutines on Android](https://developer.android.com/kotlin/coroutines) guide.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Material Components and layouts](https://developer.android.com/develop/ui/compose/layouts/material)
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
- [Compose layout basics](https://developer.android.com/develop/ui/compose/layouts/basics)