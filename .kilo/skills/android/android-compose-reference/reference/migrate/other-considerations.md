While migrating from Views to Compose is purely UI-related, there are a lot of
things to take into account to perform a safe and incremental migration. This
page contains some considerations while migrating your
View-based app to Compose.

## Migrating your app's theme

Material Design is the recommended design system for theming Android apps.

For View-based apps, there are three versions of Material available:

- Material Design 1 using the [AppCompat](https://developer.android.com/jetpack/androidx/releases/appcompat) library (i.e. `Theme.AppCompat.*`)
- Material Design 2 using the [MDC-Android](https://github.com/material-components/material-components-android) library (i.e. `Theme.MaterialComponents.*`)
- Material Design 3 using the [MDC-Android](https://github.com/material-components/material-components-android) library (i.e. `Theme.Material3.*`)

For Compose apps, there are two versions of Material available:

- Material Design 2 using the [Compose Material](https://developer.android.com/jetpack/androidx/releases/compose-material) library (i.e. `androidx.compose.material.MaterialTheme`)
- Material Design 3 using the [Compose Material 3](https://developer.android.com/jetpack/androidx/releases/compose-material3) library (i.e. `androidx.compose.material3.MaterialTheme`)

We recommend using the latest version (Material 3) if your app's design system
is in a position to do so. There are migration guides available for both Views
and Compose:

- [Material 1 to Material 2 in Views](https://material.io/blog/migrate-android-material-components)
- [Material 2 to Material 3 in Views](https://material.io/blog/migrating-material-3)
- [Material 2 to Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material2-material3)

When creating new screens in Compose, regardless of which version of Material
Design you're using, ensure that you apply a `MaterialTheme` before any
composables that emit UI from the Compose Material libraries. The Material
components (`Button`, `Text`, etc.) depend on a `MaterialTheme` being in place
and their behaviour is undefined without it.

All
[Jetpack Compose samples](https://github.com/android/compose-samples)
use a custom Compose theme built on top of `MaterialTheme`.

See [Design systems in Compose](https://developer.android.com/develop/ui/compose/designsystems) and [Migrating XML themes to Compose](https://developer.android.com/develop/ui/compose/designsystems/views-to-compose) to learn more.

## Navigation

If you use the [Navigation component](https://developer.android.com/guide/navigation/get-started) in your app, see
the [Navigate from Compose in a Fragment-based app](https://developer.android.com/guide/navigation/use-graph/navigate#compose-interop) and
[Migrate Jetpack Navigation to Navigation Compose](https://developer.android.com/develop/ui/compose/migrate/migration-scenarios/navigation) for more information.

## Test your mixed Compose/Views UI

After migrating parts of your app to Compose, testing is critical to make sure
you haven't broken anything.

When an activity or fragment uses Compose, you need to use
[`createAndroidComposeRule`](https://developer.android.com/reference/kotlin/androidx/compose/ui/test/junit4/package-summary#createAndroidComposeRule())
instead of using `ActivityScenarioRule`. `createAndroidComposeRule` integrates
`ActivityScenarioRule` with a `ComposeTestRule` that lets you test Compose and
View code at the same time.


```kotlin
class MyActivityTest {
    @Rule
    @JvmField
    val composeTestRule = createAndroidComposeRule<MyActivity>()

    @Test
    fun testGreeting() {
        val greeting = InstrumentationRegistry.getInstrumentation()
            .targetContext.resources.getString(R.string.greeting)

        composeTestRule.onNodeWithText(greeting).assertIsDisplayed()
    }
}
```

<br />

See [Testing your Compose layout](https://developer.android.com/develop/ui/compose/testing) to learn more about testing. For
interoperability with UI testing frameworks, see
[interoperability with Espresso](https://developer.android.com/develop/ui/compose/testing#espresso-interop) and
[interoperability with UiAutomator](https://developer.android.com/develop/ui/compose/testing#uiautomator-interop).

## Integrating Compose with your existing app architecture

[Unidirectional Data Flow](https://developer.android.com/develop/ui/compose/architecture#udf) (UDF) architecture
patterns work seamlessly with Compose. If the app uses other types of
architecture patterns instead, like Model View Presenter (MVP), we recommend you
migrate that part of the UI to UDF before or whilst adopting Compose.

### Using a `ViewModel` in Compose

If you use the [Architecture Components
`ViewModel`](https://developer.android.com/topic/libraries/architecture/viewmodel) library, you can access a
[`ViewModel`](https://developer.android.com/reference/androidx/lifecycle/ViewModel) from any composable by
calling the
[`viewModel()`](https://developer.android.com/reference/kotlin/androidx/lifecycle/viewmodel/compose/package-summary#viewmodel)
function, as explained in [Compose and other libraries](https://developer.android.com/develop/ui/compose/libraries).

> [!NOTE]
> **Note:** Due to their [lifecycle and scoping](https://developer.android.com/topic/libraries/architecture/viewmodel#lifecycle), *you should access and call `ViewModel` instances at screen-level
> composables* ---that is, close to a root composable called from an activity, fragment, or destination of a Navigation graph. *You should never pass down
> `ViewModel` instances to other composables*. Instead, pass only the data they need and functions that perform the required logic as parameters.

When adopting Compose, be careful about using the same `ViewModel` type in
different composables as `ViewModel` elements follow View-lifecycle scopes. The
scope will be either the host activity, fragment, or the navigation graph if the
Navigation library is used.

> [!NOTE]
> **Note:** The same instance of a `ViewModel` type is used in all composables unless the composable is a destination of the navigation graph or different activity or fragment instances.

For example, if the composables are hosted in an activity, `viewModel()` always
returns the same instance that is only cleared when the activity finishes.
In the following example, the same user ("user1") is greeted twice because
the same `GreetingViewModel` instance is reused in all composables under the
host activity. The first `ViewModel` instance created is reused in other
composables.


```kotlin
class GreetingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column {
                    GreetingScreen("user1")
                    GreetingScreen("user2")
                }
            }
        }
    }
}

@Composable
fun GreetingScreen(
    userId: String,
    viewModel: GreetingViewModel = viewModel(  
        factory = GreetingViewModelFactory(userId)  
    )
) {
    val messageUser by viewModel.message.observeAsState("")
    Text(messageUser)
}

class GreetingViewModel(private val userId: String) : ViewModel() {
    private val _message = MutableLiveData("Hi $userId")
    val message: LiveData<String> = _message
}

class GreetingViewModelFactory(private val userId: String) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return GreetingViewModel(userId) as T
    }
}
```

<br />

As navigation graphs also scope `ViewModel` elements, composables that are a
destination in a navigation graph have a different instance of the `ViewModel`.
In this case, the `ViewModel` is scoped to the lifecycle of the destination, and
it is cleared when the destination is removed from the backstack. In the
following example, when the user navigates to the *Profile* screen, a new
instance of `GreetingViewModel` is created.


```kotlin
@Composable
fun MyApp() {
    NavHost(rememberNavController(), startDestination = "profile/{userId}") {
        /* ... */
        composable("profile/{userId}") { backStackEntry ->
            GreetingScreen(backStackEntry.arguments?.getString("userId") ?: "")
        }
    }
}
```

<br />

### State source of truth

When you adopt Compose in one part of the UI, it's possible that Compose and
the View system code need to share data. When possible, we recommend you
encapsulate that shared state in another class that follows UDF best practices
used by both platforms; for example, in a `ViewModel` that exposes a stream of the
shared data to emit data updates.

However, that's not always possible if the data to be shared is mutable or is
tightly bound to a UI element. In that case, one system must be the source of
truth, and that system needs to share any data updates to the other system. As a
general rule of thumb, the source of truth should be owned by whichever element
is closer to the root of the UI hierarchy.

### Compose as the source of truth

Use the
[`SideEffect`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/SideEffect.composable#SideEffect(kotlin.Function0))
composable to publish Compose state to non-Compose code. In this case, the
source of truth is kept in a composable, which sends state updates.

As an example, your analytics library might allow you to segment your user
population by attaching custom metadata (*user properties* in this example)
to all subsequent analytics events. To communicate the user type of the
current user to your analytics library, use `SideEffect` to update its value.


```kotlin
@Composable
fun rememberFirebaseAnalytics(user: User): FirebaseAnalytics {
    val analytics: FirebaseAnalytics = remember {
        FirebaseAnalytics()
    }

    // On every successful composition, update FirebaseAnalytics with
    // the userType from the current User, ensuring that future analytics
    // events have this metadata attached
    SideEffect {
        analytics.setUserProperty("userType", user.userType)
    }
    return analytics
}
```

<br />

For more information, see [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects).

### View system as the source of truth

If the View system owns the state and shares it with Compose, we recommend that
you wrap the state in `mutableStateOf` objects to make it thread-safe for
Compose. If you use this approach, composable functions are simplified because
they no longer have the source of truth, but the View system needs to update the
mutable state and the Views that use that state.

In the following example, a `CustomViewGroup` contains a `TextView` and a
`ComposeView` with a `TextField` composable inside. The `TextView` needs to show
the content of what the user types in the `TextField`.


```kotlin
class CustomViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    // Source of truth in the View system as mutableStateOf
    // to make it thread-safe for Compose
    private var text by mutableStateOf("")

    private val textView: TextView

    init {
        orientation = VERTICAL

        textView = TextView(context)
        val composeView = ComposeView(context).apply {
            setContent {
                MaterialTheme {
                    TextField(value = text, onValueChange = { updateState(it) })
                }
            }
        }

        addView(textView)
        addView(composeView)
    }

    // Update both the source of truth and the TextView
    private fun updateState(newValue: String) {
        text = newValue
        textView.text = newValue
    }
}
```

<br />

## Migrating shared UI

If you are migrating gradually to Compose, you might need to use shared UI
elements in both Compose and the View system. For example, if your app has a
custom `CallToActionButton` component, you might need to use it in both Compose
and View-based screens.

In Compose, shared UI elements become composables that can be reused across the
app, regardless of the element being styled using XML or being a custom view. For
example, you'd create a `CallToActionButton` composable for your custom call to
action `Button` component.

To use the composable in View-based screens, create a custom view wrapper that
extends from `AbstractComposeView`. In its overridden `Content` composable,
place the composable you created wrapped in your Compose theme, as shown in the
example below:


```kotlin
@Composable
fun CallToActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        ),
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(text)
    }
}

class CallToActionViewButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : AbstractComposeView(context, attrs, defStyle) {

    var text by mutableStateOf("")
    var onClick by mutableStateOf({})

    @Composable
    override fun Content() {
        YourAppTheme {
            CallToActionButton(text, onClick)
        }
    }
}
```

<br />

Notice that the composable parameters become mutable variables inside the custom
view. This makes the custom `CallToActionViewButton` view inflatable and usable,
like a traditional view. See an example of this with [View Binding](https://developer.android.com/topic/libraries/view-binding)
below:


```kotlin
class ViewBindingActivity : ComponentActivity() {

    private lateinit var binding: ActivityExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.callToAction.apply {
            text = getString(R.string.greeting)
            onClick = { /* Do something */ }
        }
    }
}
```

<br />

If the custom component contains mutable state, see [State source of
truth](https://developer.android.com/develop/ui/compose/migrate/other-considerations#state-source-of-truth).

## Prioritize splitting state from presentation

Traditionally, a `View` is stateful. A `View` manages fields that
describe *what* to display, in addition to *how* to display it. When you
convert a `View` to Compose, look to separate the data being rendered to
achieve a unidirectional data flow, as explained further in [state hoisting](https://developer.android.com/develop/ui/compose/state#state-hoisting).

For example, a `View` has a `visibility` property that describes if it is
visible, invisible, or gone. This is an inherent property of the `View`. While
other pieces of code may change the visibility of a `View`, only the `View`
itself really knows what its current visibility is. The logic for ensuring that
a `View` is visible can be error prone, and is often tied to the `View`
itself.

By contrast, Compose makes it easy to display entirely different composables
using conditional logic in Kotlin:


```kotlin
@Composable
fun MyComposable(showCautionIcon: Boolean) {
    if (showCautionIcon) {
        CautionIcon(/* ... */)
    }
}
```

<br />

By design, `CautionIcon` doesn't need to know or care why it is being displayed,
and there is no concept of `visibility`: it either is in the Composition, or it
isn't.

By cleanly separating state management and presentation logic, you can more
freely change how you display content as a conversion of state to UI. Being
able to hoist state when needed also makes composables more reusable, since
state ownership is more flexible.

## Promote encapsulated and reusable components

`View` elements often have some idea of where they live: inside an `Activity`, a
`Dialog`, a `Fragment` or somewhere inside another `View` hierarchy. Because
they are often inflated from static layout files, the overall structure of a
`View` tends to be very rigid. This results in tighter coupling, and makes it
harder for a `View` to be changed or reused.

For example, a custom `View` might assume that it has a child view of a certain
type with a certain id, and change its properties directly in response to some
action. This tightly couples those `View` elements together: the custom `View`
may crash or be broken if it can't find the child, and the child likely can't
be reused without the custom `View` parent.

This is less of a problem in Compose with reusable composables. Parents can
easily specify state and callbacks, so you can write reusable composables
without having to know the exact place where they'll be used.


```kotlin
@Composable
fun AScreen() {
    var isEnabled by rememberSaveable { mutableStateOf(false) }

    Column {
        ImageWithEnabledOverlay(isEnabled)
        ControlPanelWithToggle(
            isEnabled = isEnabled,
            onEnabledChanged = { isEnabled = it }
        )
    }
}
```

<br />

In the example above, all three parts are more encapsulated and less coupled:

- `ImageWithEnabledOverlay` only needs to know what the current `isEnabled`
  state is. It doesn't need to know that `ControlPanelWithToggle` exists, or
  even how it is controllable.

- `ControlPanelWithToggle` doesn't know that `ImageWithEnabledOverlay` exists.
  There could be zero, one, or more ways that `isEnabled` is displayed, and
  `ControlPanelWithToggle` wouldn't have to change.

- To the parent, it doesn't matter how deeply nested `ImageWithEnabledOverlay`
  or `ControlPanelWithToggle` are. Those children could be animating changes,
  swapping out content, or passing content on to other children.

This pattern is known as the *inversion of control* , which you can read more
about in the [`CompositionLocal` documentation](https://developer.android.com/develop/ui/compose/compositionlocal#inversion-of-control).

## Handling screen size changes

Having different resources for different window sizes is one of the main ways to
create responsive `View` layouts. While qualified resources are still an option
for screen-level layout decisions, Compose makes it much easier to change
layouts entirely in code with normal conditional logic. See [Use window size
classes](https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes) to learn more.

Additionally, refer to [Support different display sizes](https://developer.android.com/develop/ui/compose/layouts/adaptive/support-different-display-sizes)
to learn about the techniques Compose offers to build adaptive UIs.

## Nested scrolling with Views

For more information on how to enable nested scrolling interop between
scrollable View elements and scrollable composables, nested in both directions,
read through
[Nested scrolling interop](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/scroll#nested-scrolling-interop).

## Compose in `RecyclerView`

Composables in `RecyclerView` are performant since `RecyclerView` version
1.3.0-alpha02. Make sure you are on at least version 1.3.0-alpha02 of
`RecyclerView` to see those benefits.

## `WindowInsets` interop with Views

You may need to override default insets when your screen has both Views and
Compose code in the same hierarchy. In this case, you need to be explicit in
which one should consume the insets, and which one should ignore them.

For example, if your outermost layout is an Android View layout, you should
consume the insets in the View system and ignore them for Compose.
Alternatively, if your outermost layout is a composable, you should consume the
insets in Compose, and pad the `AndroidView` composables accordingly.

By default, each `ComposeView` consumes all insets at the
`WindowInsetsCompat` level of consumption. To change this default behavior, set
[`ComposeView.consumeWindowInsets`](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/ComposeView#(androidx.compose.ui.platform.ComposeView).consumeWindowInsets())
to `false`.

For more information, read the [`WindowInsets` in Compose](https://developer.android.com/develop/ui/compose/system/insets) documentation.

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Display emoji](https://developer.android.com/develop/ui/compose/text/emoji)
- [Material Design 2 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material)
- [Window insets in Compose](https://developer.android.com/develop/ui/compose/system/insets)