In Compose the UI is immutable---there's no way to update it after it's been
drawn. What you can control is the state of your UI. Every time the state of the
UI changes, Compose [recreates the parts of the UI tree that have
changed](https://developer.android.com/develop/ui/compose/mental-model#recomposition). Composables can accept state and expose events---for example, a
`TextField` accepts a value and exposes a callback `onValueChange` that
requests the callback handler to change the value.


```kotlin
var name by remember { mutableStateOf("") }
OutlinedTextField(
    value = name,
    onValueChange = { name = it },
    label = { Text("Name") }
)
```

<br />

Because composables accept state and expose events, the unidirectional data flow
pattern fits well with Jetpack Compose. This guide focuses on how to implement
the unidirectional data flow pattern in Compose, how to implement events and
state holders, and how to work with ViewModels in Compose.

> [!NOTE]
> **Note:** The other layers of your app---the data layer and the business layer---are not affected by adopting Jetpack Compose. To learn more about architecting all layers of your app, check out the [guide to app architecture](https://developer.android.com/jetpack/guide).

## Unidirectional data flow

A *unidirectional data flow* (UDF) is a design pattern where state flows down
and events flow up. By following unidirectional data flow, you can decouple
composables that display state in the UI from the parts of your app that store
and change state.

The UI update loop for an app using unidirectional data flow looks like this:

1. **Event**: Part of the UI generates an event and passes it upward, such as a button click passed to the ViewModel to handle; or an event is passed from other layers of your app, such as indicating that the user session has expired.
2. **Update state**: An event handler might change the state.
3. **Display state**: The state holder passes down the state, and the UI displays it.

![Events flow up from the UI to a state holder, and state flows down from the state holder to the UI.](https://developer.android.com/static/develop/ui/compose/images/state-unidirectional-flow.png) **Figure 1.** Unidirectional data flow.

Following this pattern when using Jetpack Compose provides several advantages:

- **Testability**: Decoupling state from the UI that displays it makes it easier to test both in isolation.
- **State encapsulation**: Because state can only be updated in one place and there is only one source of truth for the state of a composable, it's less likely that you'll create bugs due to inconsistent states.
- **UI consistency** : All state updates are immediately reflected in the UI by the use of observable state holders, like `StateFlow` or `LiveData`.

### Unidirectional data flow in Jetpack Compose

Composables work based on state and events. For example, a `TextField` is only
updated when its `value` parameter is updated and it exposes an `onValueChange`
callback---an event that requests the value to be changed to a new one. Compose
defines the `State` object as a value holder, and changes to the state value
trigger a recomposition. You can hold the state in a
`remember { mutableStateOf(value) }` or a
`rememberSaveable { mutableStateOf(value) }` depending on how long you need to
remember the value for.

The type of the `TextField` composable's value is `String`, so this can come
from anywhere---from a hardcoded value, from a ViewModel, or passed in from the
parent composable. You don't have to hold it in a `State` object, but you need
to update the value when `onValueChange` is called.

> [!IMPORTANT]
> **Key Point:**

- `mutableStateOf(value)` creates a `MutableState`, which is an observable type in Compose. Any changes to its value schedule recomposition of any composable functions that read that value.
- `remember` stores objects in the composition, and forgets the object when the composable that called `remember` is removed from the composition.
- `rememberSaveable` retains the state across configuration changes by saving it in a `Bundle`.

> [!NOTE]
> **Note:** To learn more about state and state hoisting in Compose, see [State and
> Jetpack Compose](https://developer.android.com/develop/ui/compose/state).

### Define composable parameters

When defining the state parameters of a composable, keep the following
questions in mind:

- How reusable or flexible is the composable?
- How do the state parameters affect this composable's performance?

To promote decoupling and reuse, each composable should hold the least amount of
information possible. For example, when building a composable to hold the
header of a news article, prefer passing in only the information that needs to
be displayed, rather than the entire news article:


```kotlin
@Composable
fun Header(title: String, subtitle: String) {
    // Recomposes when title or subtitle have changed.
}

@Composable
fun Header(news: News) {
    // Recomposes when a new instance of News is passed in.
}
```

<br />

Sometimes, using individual parameters also improves performance---for example, if
`News` contains more information than just `title` and `subtitle`, whenever a
new instance of `News` is passed into `Header(news)`, the composable will
recompose, even if `title` and `subtitle` haven't changed.

Consider carefully the number of parameters you pass in. Having a function with
too many parameters decreases the ergonomics of the function, so in this case
grouping them up in a class is preferred.

## Events in Compose

Every input to your app should be represented as an event: taps, text changes,
and even timers or other updates. As these events change the state of your UI,
the `ViewModel` should handle them and update the UI state.

The UI layer should never change state outside of an event handler because this
can introduce inconsistencies and bugs in your application.

Prefer passing immutable values for state and event handler lambdas. This
approach has the following benefits:

- You improve reusability.
- You verify that your UI doesn't change the value of the state directly.
- You avoid concurrency issues because you make sure that the state isn't mutated from another thread.
- Often, you reduce code complexity.

For example, a composable that accepts a `String` and a lambda as parameters can
be called from many contexts and is highly reusable. Suppose that the top app
bar in your app always displays text and has a
back button. You can define a more generic `MyAppTopAppBar` composable
that receives the text and the back button handler as parameters:


```kotlin
@Composable
fun MyAppTopAppBar(topAppBarText: String, onBackPressed: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = topAppBarText,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = localizedString
                )
            }
        },
        // ...
    )
}
```

<br />

### ViewModels, states, and events: an example

By using `ViewModel` and `mutableStateOf`, you can also introduce unidirectional
data flow in your app if one of the following is true:

- The state of your UI is exposed using observable state holders, like `StateFlow` or `LiveData`.
- The `ViewModel` handles events coming from the UI or other layers of your app and updates the state holder based on the events.

For example, when implementing a sign-in screen, tapping on a **Sign in** button
should cause your app to display a progress spinner and a network call. If the
login was successful, then your app navigates to a different screen; in case of
an error the app shows a Snackbar. Here's how you would model the screen state
and the event:

The screen has four states:

- **Signed out**: when the user has not signed in yet.
- **In progress**: when your app is trying to sign the user in by performing a network call.
- **Error**: when an error occurred while signing in.
- **Signed in**: when the user is signed in.

You can model these states as a sealed class. The `ViewModel` exposes the state
as a `State`, sets the initial state, and updates the state as needed. The
`ViewModel` also handles the sign-in event by exposing an `onSignIn()` method.


```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = mutableStateOf<UiState>(UiState.SignedOut)
    val uiState: State<UiState>
        get() = _uiState

    // ...
}
```

<br />

In addition to the `mutableStateOf` API, Compose [provides
extensions](https://developer.android.com/develop/ui/compose/interop#streams) for `LiveData`, `Flow`, and `Observable` to register as a
listener and represent the value as a state.


```kotlin
class MyViewModel : ViewModel() {
    private val _uiState = MutableLiveData<UiState>(UiState.SignedOut)
    val uiState: LiveData<UiState>
        get() = _uiState

    // ...
}

@Composable
fun MyComposable(viewModel: MyViewModel) {
    val uiState = viewModel.uiState.observeAsState()
    // ...
}
```

<br />

## Learn more

To learn more about architecture in Jetpack Compose, consult the following
resources:

### Samples

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
- [Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving)
- [Handle user input](https://developer.android.com/develop/ui/compose/text/user-input)