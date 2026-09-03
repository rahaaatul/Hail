You can use your favorite libraries in Compose. This section describes how to
incorporate a few of the most useful libraries.

## Activity

To use Compose in an activity, you must use
[`ComponentActivity`](https://developer.android.com/reference/kotlin/androidx/activity/ComponentActivity),
a subclass of `Activity` that provides the appropriate `LifecycleOwner` and
components to Compose. It also provides additional APIs that decouple your code
from overriding methods in your activity class.
[Activity Compose](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary)
exposes these APIs to composables such that overriding methods outside of your
composables or retrieving an explicit `Activity` instance is no longer required.
Moreover, these APIs ensure that they are only initialized once, survive
recomposition, and clean up properly if the composable is removed from the
composition.

### Activity Result

The
[`rememberLauncherForActivityResult()`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#rememberlauncherforactivityresult)
API allows you to
[get a result from an activity](https://developer.android.com/training/basics/intents/result)
in your composable:


```kotlin
@Composable
fun GetContentExample() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }
    Column {
        Button(onClick = { launcher.launch("image/*") }) {
            Text(text = "Load Image")
        }
        Image(
            painter = rememberAsyncImagePainter(imageUri),
            contentDescription = "My Image"
        )
    }
}
```

<br />

This example demonstrates a simple
[`GetContent()`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContracts.GetContent)
contract. Tapping the button launches the request. The trailing lambda for
[`rememberLauncherForActivityResult()`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#rememberlauncherforactivityresult)
is invoked once the user selects an image and returns to the launching activity.
This loads the selected image using Coil's `rememberImagePainter()`
function.

Any subclass of
[`ActivityResultContract`](https://developer.android.com/reference/androidx/activity/result/contract/ActivityResultContract)
can be used as the first argument to
[`rememberLauncherForActivityResult()`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#rememberlauncherforactivityresult).
This means that you can use this technique to request content from the framework
and in other common patterns. You can also create your own
[custom contracts](https://developer.android.com/training/basics/intents/result#custom) and use them with
this technique.

> [!CAUTION]
> **Caution:** This example shows how to launch a request from a user interaction. In this case from an `onClick()` listener. If you attempt to launch a request from inside the composable you'll get a runtime error because the [`ActivityResultLauncher`](https://developer.android.com/reference/androidx/activity/result/ActivityResultLauncher) has not been initialized at this point. If you need to trigger the launch following composition you'll need to call the [`launch()`](https://developer.android.com/reference/androidx/activity/result/ActivityResultLauncher#launch(I)) method from inside a [`LaunchedEffect`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/LaunchedEffect.composable#LaunchedEffect(kotlin.Any,kotlin.coroutines.SuspendFunction1)) or [`DisposableEffect`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/DisposableEffect.composable#DisposableEffect(kotlin.Any,kotlin.Function1)) block.

### Requesting runtime permissions

The same Activity Result API and
[`rememberLauncherForActivityResult()`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#rememberlauncherforactivityresult)
explained above can be used to
[request runtime permissions](https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code)
using the
[`RequestPermission`](https://developer.android.com/reference/kotlin/androidx/activity/result/contract/ActivityResultContracts.RequestPermission)
contract for a single permission or
[`RequestMultiplePermissions`](https://developer.android.com/reference/kotlin/androidx/activity/result/contract/ActivityResultContracts.RequestMultiplePermissions)
contract for multiple permissions.

The
[Accompanist Permissions library](https://google.github.io/accompanist/permissions/)
can also be used as a layer above those APIs to map the current granted state
for permissions into State that your Compose UI can use.

### Handling the system back button

To [provide custom back navigation](https://developer.android.com/guide/navigation/navigation-custom-back)
and override the default behavior of the system back button from within your
composable, your composable can use a
[`BackHandler`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#backhandler)
to intercept that event:


```kotlin
var backHandlingEnabled by remember { mutableStateOf(true) }
BackHandler(backHandlingEnabled) {
    // Handle back press
}
```

<br />

The first argument controls whether the
[`BackHandler`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#backhandler)
is currently enabled; you can use this argument to temporarily disable your handler
based on the state of your component. The trailing lambda will be invoked if the
user triggers a system back event, and the
[`BackHandler`](https://developer.android.com/reference/kotlin/androidx/activity/compose/package-summary#backhandler)
is currently enabled.

> [!CAUTION]
> **Caution:** While it is possible to add multiple back handlers to any composition, the system back event is handled and consumed by the innermost *enabled* `BackHandler`. In other words, at most one `BackHandler` is active at any time.

## `ViewModel`

If you use the [Architecture Components
ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel) library, you can access a
[`ViewModel`](https://developer.android.com/reference/androidx/lifecycle/ViewModel) from any composable by
calling the
[`viewModel()`](https://developer.android.com/reference/kotlin/androidx/lifecycle/viewmodel/compose/package-summary#viewmodel)
function. Add the following dependency to your Gradle file:

### Groovy

```groovy
dependencies {
    implementation 'androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0'
}
```

### Kotlin

```kotlin
dependencies {
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
}
```

You can then use the `viewModel()` function in your code.


```kotlin
class MyViewModel : ViewModel() { /*...*/ }

// import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) {
    // use viewModel here
}
```

<br />

`viewModel()` returns an existing `ViewModel` or creates a new one. By default,
the returned `ViewModel` is scoped to the enclosing activity, fragment or
navigation destination, and is retained as long as the scope is alive.

For example, if the composable is used in an activity, `viewModel()` returns the
same instance until the activity is finished or the process is killed.


```kotlin
class MyViewModel : ViewModel() { /*...*/ }
// import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun MyScreen(
    // Returns the same instance as long as the activity is alive,
    // just as if you grabbed the instance from an Activity or Fragment
    viewModel: MyViewModel = viewModel()
) { /* ... */ }

@Composable
fun MyScreen2(
    viewModel: MyViewModel = viewModel() // Same instance as in MyScreen
) { /* ... */ }
```

<br />

### Usage guidelines

You usually access `ViewModel` instances at *screen-level*
composables, that is, close to a root composable called from an activity,
fragment, or destination of a Navigation graph. This is because `ViewModel`s
are, by default, scoped to those *screen level* objects. Read more about a
`ViewModel`'s
[lifecycle and scope here](https://developer.android.com/topic/libraries/architecture/viewmodel#lifecycle).

Try to avoid passing down
`ViewModel` instances to other composables as this can make those composables
more difficult to test and can break
[previews](https://developer.android.com/develop/ui/compose/tooling/previews). Instead, pass only the data
and functions they need as parameters.

You *can* use `ViewModel` instances to
manage state for *sub screen-level* composables, however, be aware of the
`ViewModel`'s
[lifecycle and scope](https://developer.android.com/topic/libraries/architecture/viewmodel#lifecycle). If the
composable is self-contained, you may want to consider using [Hilt](https://developer.android.com/develop/ui/compose/libraries#hilt) to
inject the `ViewModel` to avoid having to pass dependencies from parent
composables.

If your `ViewModel` has dependencies, `viewModel()` takes an optional
[`ViewModelProvider.Factory`](https://developer.android.com/reference/androidx/lifecycle/ViewModelProvider.Factory)
as a parameter.

For more information about `ViewModel` in Compose and how instances are used
with the Navigation Compose library, or activities and fragments,
see the [Interoperability docs](https://developer.android.com/develop/ui/compose/interop#viewmodel).

## Streams of data

Compose comes with extensions for Android's most popular stream-based solutions.
Each of these extensions is provided by a different artifact:

- `https://developer.android.com/reference/kotlin/androidx/compose/runtime/livedata/package-summary#observeAsState(androidx.lifecycle.LiveData)` included in the `androidx.compose.runtime:runtime-livedata:$composeVersion` artifact.
- `https://developer.android.com/reference/kotlin/androidx/compose/runtime/package-summary#collectAsState(kotlinx.coroutines.flow.StateFlow,kotlin.coroutines.CoroutineContext)` doesn't require extra dependencies.
- `https://developer.android.com/reference/kotlin/androidx/compose/runtime/rxjava2/package-summary#subscribeAsState(io.reactivex.Observable,kotlin.Any)` included in the `androidx.compose.runtime:runtime-rxjava2:$composeVersion` or `androidx.compose.runtime:runtime-rxjava3:$composeVersion` artifact.

These artifacts register as a listener and represent the values as a
[`State`](https://developer.android.com/reference/kotlin/androidx/compose/runtime/State). Whenever a new value
is emitted, Compose recomposes those parts of the UI where that `state.value` is
used. For example, in this code, `ShowData` recomposes every time
`exampleLiveData` emits a new value.


```kotlin
// import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) {
    val dataExample = viewModel.exampleLiveData.observeAsState()

    // Because the state is read here,
    // MyScreen recomposes whenever dataExample changes.
    dataExample.value?.let {
        ShowData(dataExample)
    }
}
```

<br />

## Asynchronous operations in Compose

Jetpack Compose lets you execute asynchronous operations using coroutines from
within your composables.

See the `LaunchedEffect`, `produceState`, and `rememberCoroutineScope` APIs in
the [side effects documentation](https://developer.android.com/develop/ui/compose/side-effects) for more
information.

## Navigation

The [Navigation component](https://developer.android.com/guide/navigation/get-started) provides support for Jetpack Compose applications.
See [Navigating with Compose](https://developer.android.com/guide/navigation#framework-options) and
[Migrate Jetpack Navigation to Navigation Compose](https://developer.android.com/develop/ui/compose/migrate/migration-scenarios/navigation) for more information.

## Hilt

Hilt is the recommended solution for dependency injection in Android apps, and
works seamlessly with Compose.

The `viewModel()` function mentioned in the [ViewModel section](https://developer.android.com/develop/ui/compose/libraries#viewmodel)
automatically uses the ViewModel that Hilt constructs with the `@HiltViewModel`
annotation. We've provided documentation with information about [Hilt's ViewModel
integration](https://developer.android.com/training/dependency-injection/hilt-jetpack#viewmodels).


```kotlin
@HiltViewModel
class MyViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val repository: ExampleRepository
) : ViewModel() { /* ... */ }

// import androidx.lifecycle.viewmodel.compose.viewModel
@Composable
fun MyScreen(
    viewModel: MyViewModel = viewModel()
) { /* ... */ }
```

<br />

### Hilt and Navigation

Hilt also integrates with the Navigation Compose library. Add the following
additional dependencies to your Gradle file:

### Groovy

```groovy
dependencies {
    implementation 'androidx.hilt:hilt-navigation-compose:1.3.0'
}
```

### Kotlin

```kotlin
dependencies {
    implementation("androidx.hilt:hilt-navigation-compose:1.3.0")
}
```

When using Navigation Compose, always use the `hiltViewModel` composable
function to obtain an instance of your `@HiltViewModel` annotated `ViewModel`.
This works with fragments or activities that are annotated with
`@AndroidEntryPoint`.

For example, if `ExampleScreen` is a destination in a navigation graph,
call `hiltViewModel()` to get an instance of `ExampleViewModel` scoped
to the destination as shown in the code snippet below:


```kotlin
// import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MyApp() {
    val navController = rememberNavController()
    val startRoute = "example"
    NavHost(navController, startDestination = startRoute) {
        composable("example") { backStackEntry ->
            // Creates a ViewModel from the current BackStackEntry
            // Available in the androidx.hilt:hilt-navigation-compose artifact
            val viewModel = hiltViewModel<MyViewModel>()
            MyScreen(viewModel)
        }
        /* ... */
    }
}
```

<br />

If you need to retrieve the instance of a `ViewModel` scoped to
[navigation routes](https://developer.android.com/guide/navigation/design/nested-graphs#compose) or [the
navigation graph](https://developer.android.com/guide/navigation/use-graph/programmatic#share-ui-related-data-between-destinations-with-viewmodel)
instead, use the `hiltViewModel` composable function and pass the corresponding
`backStackEntry` as a parameter:


```kotlin
// import androidx.hilt.navigation.compose.hiltViewModel
// import androidx.navigation.compose.getBackStackEntry

@Composable
fun MyApp() {
    val navController = rememberNavController()
    val startRoute = "example"
    val innerStartRoute = "exampleWithRoute"
    NavHost(navController, startDestination = startRoute) {
        navigation(startDestination = innerStartRoute, route = "Parent") {
            // ...
            composable("exampleWithRoute") { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry("Parent")
                }
                val parentViewModel = hiltViewModel<ParentViewModel>(parentEntry)
                ExampleWithRouteScreen(parentViewModel)
            }
        }
    }
}
```

<br />

## Paging

The [Paging
library](https://developer.android.com/topic/libraries/architecture/paging)
makes it easier for you to load data gradually and it's supported in Compose.
The [Paging release
page](https://developer.android.com/jetpack/androidx/releases/paging) contains
information about the extra `paging-compose` dependency that needs to be added
to the project and its version.

Here's an example of the Paging library's Compose APIs:


```kotlin
@Composable
fun MyScreen(flow: Flow<PagingData<String>>) {
    val lazyPagingItems = flow.collectAsLazyPagingItems()
    LazyColumn {
        items(
            lazyPagingItems.itemCount,
            key = lazyPagingItems.itemKey { it }
        ) { index ->
            val item = lazyPagingItems[index]
            Text("Item is $item")
        }
    }
}
```

<br />

Check out the [Lists and grids documentation](https://developer.android.com/develop/ui/compose/lists) for more information
about using Paging in Compose.

## Maps

You can use the [Maps Compose](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
library to provide Google Maps in your app. Here's a usage example:


```kotlin
@Composable
fun MapsExample() {
    val singapore = LatLng(1.35, 103.87)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Marker(
            state = remember { MarkerState(position = singapore) },
            title = "Singapore",
            snippet = "Marker in Singapore"
        )
    }
}
```

<br />

## Recommended for you

- Note: link text is displayed when JavaScript is off
- [Side-effects in Compose](https://developer.android.com/develop/ui/compose/side-effects)
- [State and Jetpack Compose](https://developer.android.com/develop/ui/compose/state)
- [Save UI state in Compose](https://developer.android.com/develop/ui/compose/state-saving)