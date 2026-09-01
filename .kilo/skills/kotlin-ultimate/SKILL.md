# Kotlin & Android Ultimate Skill

This is the definitive reference for idiomatic Kotlin, Coroutines, Flow, Jetpack Compose, Ktor, and Kotlin Multiplatform (KMP) development. It synthesizes official documentation, 2026 best practices, and production patterns.

## Scope

Use this skill when:
- Writing or reviewing Kotlin code for Android, KMP, or Ktor backends
- Designing asynchronous architectures with Coroutines and Flow
- Structuring multiplatform shared code
- Implementing server-side Ktor applications
- Migrating legacy Java or callback-based code to Kotlin

---

## 1. Coroutine Fundamentals

### 1.1 Structured Concurrency

Coroutines form a tree hierarchy. A parent waits for all children. If a parent fails or is cancelled, all children are recursively cancelled.

```kotlin
// ✅ CORRECT: Structured concurrency with coroutineScope
suspend fun fetchAllData(): CombinedData = coroutineScope {
    val user = async { repository.getUser() }
    val orders = async { repository.getOrders() }
    CombinedData(user.await(), orders.await())
}

// ❌ WRONG: GlobalScope breaks structured concurrency
fun loadData(): Job = GlobalScope.launch {
    // Orphaned coroutine — no lifecycle management
}
```

### 1.2 Scope Selection Guide

| Context | Scope | Cancellation Trigger |
|---------|-------|---------------------|
| ViewModel | `viewModelScope` | `ViewModel.onCleared()` |
| Activity / Fragment / LifecycleOwner | `lifecycleScope` | `Lifecycle.State.DESTROYED` |
| Suspend function (fail-together) | `coroutineScope { }` | First child failure cancels all siblings |
| Suspend function (independent failures) | `supervisorScope { }` | One child failure does not cancel siblings |
| Tests | `runTest` | Virtual time, deterministic |
| Entry points only (`main()`) | `runBlocking` | Blocks current thread |
| **NEVER in production** | `GlobalScope` | No lifecycle — causes leaks |

**Decision tree:**
```
Are you in a ViewModel?
├── Yes → viewModelScope
└── No
    ├── Are you in a Fragment/Activity/LifecycleOwner?
    │   ├── Yes → lifecycleScope (+ repeatOnLifecycle for Flow collection)
    │   └── No
    │       ├── Is this test code?
    │       │   ├── Yes → runTest { }
    │       │   └── No
    │       │       ├── Failures should cancel siblings?
    │       │       │   ├── Yes → coroutineScope { }
    │       │       │   └── No → supervisorScope { }
```

### 1.3 Coroutine Builders

| Builder | Returns | Use When |
|---------|---------|----------|
| `launch` | `Job` | Fire-and-forget work; no result needed |
| `async` | `Deferred<T>` | Parallel computation with a result; call `.await()` |
| `coroutineScope` | Result of block | Suspend function that needs child coroutines |
| `supervisorScope` | Result of block | Like `coroutineScope` but child failures are independent |
| `withContext` | Result of block | Switch dispatcher for a section of code |
| `runBlocking` | Result of block | Bridging sync/async code in `main()` or tests only |

```kotlin
// Parallel decomposition
suspend fun loadDashboard(): Dashboard = supervisorScope {
    val profile = async { api.getProfile() }
    val feed = async { api.getFeed() }
    val notifications = async { api.getNotifications() }
    Dashboard(profile.await(), feed.await(), notifications.await())
}
```

### 1.4 Dispatchers

| Dispatcher | Use For | Thread Pool |
|------------|---------|-------------|
| `Dispatchers.Main` | UI updates, Compose state mutations | Main/UI thread |
| `Dispatchers.Main.immediate` | Immediate UI work when already on main | Main/UI thread |
| `Dispatchers.IO` | Network, file I/O, database queries | Expanded thread pool |
| `Dispatchers.Default` | CPU-intensive work, data processing | Fixed to CPU core count |
| `Dispatchers.Unconfined` | Not recommended for production | No thread confinement |

**Rule:** Inject dispatchers rather than hardcoding them. This enables testability with `TestDispatcher`.

```kotlin
// ✅ CORRECT: Injected dispatchers
class UserRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getUser(): User = withContext(ioDispatcher) {
        api.fetchUser()
    }
}

// ❌ WRONG: Hardcoded dispatcher
class UserRepository {
    suspend fun getUser(): User = withContext(Dispatchers.IO) {
        api.fetchUser()  // Not testable
    }
}
```

### 1.5 Cancellation

Cancellation is **cooperative**. A coroutine is only cancelled when it suspends or checks for cancellation.

```kotlin
// ✅ CORRECT: Ensure cancellation in blocking loops
suspend fun readFiles(files: List<File>) = withContext(Dispatchers.IO) {
    for (file in files) {
        ensureActive() // Throws CancellationException if cancelled
        processFile(file)
    }
}

// ✅ CORRECT: All kotlinx.coroutines suspend functions are cancellable
suspend fun fetchData(): Data {
    delay(1000)  // Cancellable
    return withContext(Dispatchers.IO) {
        api.getData()  // If api uses suspend functions, this is cancellable
    }
}

// ❌ WRONG: Never swallow CancellationException
try {
    fetchData()
} catch (e: Exception) {  // Catches CancellationException — BAD
    // Cancellation won't propagate properly
}

// ✅ CORRECT: Rethrow CancellationException
try {
    fetchData()
} catch (e: CancellationException) {
    throw e  // Always rethrow
} catch (e: IOException) {
    // Handle actual error
}
```

---

## 2. Flow & Reactive Streams

### 2.1 Cold vs Hot Flows

| Type | Variant | Execution | Current Value | Use Case |
|------|---------|-----------|---------------|----------|
| Cold `Flow` | `flow { }` | Starts on each `collect` | None | One-shot streams, repository queries |
| Hot | `StateFlow` | Always active | Yes (replay=1) | UI state in ViewModel |
| Hot | `SharedFlow` | Always active | Configurable replay | One-shot events, notifications |

### 2.2 StateFlow Patterns

```kotlin
// ✅ CORRECT: Private mutable, public immutable
class NewsViewModel(
    private val repository: NewsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<NewsUiState>(NewsUiState.Loading)
    val uiState: StateFlow<NewsUiState> = _uiState.asStateFlow()

    fun loadNews() {
        viewModelScope.launch {
            repository.getNews()
                .catch { _uiState.value = NewsUiState.Error(it.message) }
                .collect { _uiState.value = NewsUiState.Success(it) }
        }
    }
}

// ✅ CORRECT: stateIn for cold-to-hot conversion
val newsState: StateFlow<List<NewsItem>> = repository.newsFlow()
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
```

### 2.3 SharedFlow Patterns

```kotlin
// ✅ CORRECT: SharedFlow for one-shot events (navigation, snackbar)
class EventViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,
        extraBufferCapacity = 10,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    fun onButtonClick() {
        viewModelScope.launch {
            _events.emit(UiEvent.NavigateToDetail)
        }
    }
}

// ✅ CORRECT: Collect in Compose with LaunchedEffect
@Composable
fun EventScreen(viewModel: EventViewModel) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateToDetail -> navigate(event.id)
                is UiEvent.ShowSnackbar -> showSnackbar(event.message)
            }
        }
    }
}
```

### 2.4 Essential Flow Operators

```kotlin
repository.getUsers()
    .map { users -> users.filter { it.isActive } }
    .distinctUntilChanged()
    .debounce(300)                      // Debounce user input
    .flatMapLatest { users ->           // Cancel previous on new emission
        fetchUserDetails(users)
    }
    .catch { e ->
        emit(emptyList())               // Provide fallback on error
    }
    .onEach { users ->
        analytics.logUserCount(users.size)
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
```

### 2.5 Flow Collection in UI

```kotlin
// ✅ CORRECT: Lifecycle-aware collection with repeatOnLifecycle
class MyFragment : Fragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }
}

// ✅ CORRECT: Compose with collectAsStateWithLifecycle
@Composable
fun MyScreen(viewModel: MyViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Automatically stops collecting when lifecycle is STOPPED
}
```

### 2.6 Flow Anti-Patterns

```kotlin
// ❌ WRONG: Blocking inside flow builder
val data = flow {
    Thread.sleep(1000)  // Blocks collector's thread!
    emit(repository.getData())
}

// ✅ CORRECT: Use flowOn or suspend functions
val data = flow {
    emit(repository.getData())
}.flowOn(Dispatchers.IO)

// ❌ WRONG: Using collectLatest when work must complete
searchQuery.collectLatest { query ->
    performExpensiveOperation(query)  // Cancelled on every new query
}

// ✅ CORRECT: Use collect when work must complete
searchQuery.collect { query ->
    performExpensiveOperation(query)  // Always completes
}
```

---

## 3. Error Handling

### 3.1 Exception Patterns

```kotlin
// ✅ CORRECT: Catch specific exceptions, never CancellationException
viewModelScope.launch {
    try {
        val data = repository.fetchData()
        _uiState.value = UiState.Success(data)
    } catch (e: CancellationException) {
        throw e  // Always rethrow to preserve cancellation
    } catch (e: IOException) {
        _uiState.value = UiState.Error("Network error")
    } catch (e: HttpException) {
        _uiState.value = UiState.Error("Server error: ${e.code()}")
    }
}
```

### 3.2 CoroutineExceptionHandler

```kotlin
// ✅ CORRECT: Top-level exception handler with SupervisorJob
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + CoroutineExceptionHandler { _, throwable ->
    // Log to crash reporting
    FirebaseCrashlytics.getInstance().recordException(throwable)
})

// Note: ExceptionHandler only works on top-level coroutines launched with launch
// It does NOT work with async — those surface exceptions on .await()
```

### 3.3 Result Wrapper Pattern

```kotlin
// ✅ CORALLEL: Sealed result type for explicit error handling
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    data object Loading : Result<Nothing>()
}

suspend fun fetchUser(): Result<User> = runCatching {
    api.getUser()
}.fold(
    onSuccess = { Result.Success(it) },
    onFailure = { Result.Error(it) }
)
```

---

## 4. Testing Coroutines & Flow

### 4.1 TestDispatcher Injection

```kotlin
// ✅ CORRECT: Inject TestDispatcher for testing
class UserViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `loadUser success updates state to Success`() = runTest(testDispatcher) {
        // Given
        val repository = mockk<UserRepository>()
        coEvery { repository.getUser() } returns User("test")
        val viewModel = UserViewModel(repository, testDispatcher)

        // When
        viewModel.loadUser()

        // Then
        viewModel.uiState.test {
            assertThat(awaitItem()).isInstanceOf(UiState.Success::class.java)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
```

### 4.2 Turbine for Flow Testing

```kotlin
// ✅ CORRECT: Use Turbine to test Flow emissions
@Test
fun `newsFlow emits items`() = runTest {
    val repository = mockk<NewsRepository>()
    every { repository.newsFlow() } returns flowOf(
        NewsItem("1"),
        NewsItem("2")
    )

    val viewModel = NewsViewModel(repository)

    viewModel.newsState.test {
        assertThat(awaitItem()).isEmpty()  // initial
        assertThat(awaitItem()).containsExactly(NewsItem("1"), NewsItem("2"))
        cancelAndIgnoreRemainingEvents()
    }
}
```

### 4.3 runTest Rules

- `runTest` uses virtual time — `delay()` is skipped automatically
- All TestDispatchers share one scheduler for deterministic tests
- Use `advanceUntilIdle()` to process all pending coroutines
- Never use `runBlocking` in tests — use `runTest` instead

---

## 5. Jetpack Compose Integration

### 5.1 State in Compose

```kotlin
// ✅ CORRECT: ViewModel creates coroutines, exposes StateFlow
class OrderViewModel(
    private val repository: OrderRepository
) : ViewModel() {

    val uiState: StateFlow<OrderUiState> = repository.getOrders()
        .map { OrderUiState.Success(it) }
        .catch { OrderUiState.Error(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OrderUiState.Loading)

    fun placeOrder(item: OrderItem) {
        viewModelScope.launch {
            repository.placeOrder(item)
        }
    }
}

// ✅ CORRECT: Compose collects with lifecycle awareness
@Composable
fun OrderScreen(viewModel: OrderViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = state) {
        is OrderUiState.Loading -> LoadingIndicator()
        is OrderUiState.Success -> OrderList(s.orders)
        is OrderUiState.Error -> ErrorMessage(s.exception)
    }
}
```

### 5.2 rememberCoroutineScope

```kotlin
// ✅ CORRECT: Use for event-driven coroutines tied to composable lifecycle
@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            viewModel.onSearchClicked()
        }
    }) {
        Text("Search")
    }
}
```

---

## 6. Interop Patterns

### 6.1 Callback to Suspend

```kotlin
// ✅ CORRECT: suspendCancellableCoroutine for cancellable bridges
suspend fun fetchFromLegacyApi(id: String): Data =
    suspendCancellableCoroutine { continuation ->
        val callback = object : LegacyCallback {
            override fun onSuccess(data: Data) {
                continuation.resume(data)
            }
            override fun onError(error: Throwable) {
                continuation.resumeWithException(error)
            }
        }
        legacyApi.fetch(id, callback)

        continuation.invokeOnCancellation {
            legacyApi.cancel(id)
        }
    }

// ✅ CORRECT: callbackFlow for listener-based APIs
fun observeDatabaseChanges(): Flow<List<Item>> = callbackFlow {
    val observer = object : DatabaseObserver {
        override fun onChange(items: List<Item>) {
            trySend(items)
        }
    }
    database.registerObserver(observer)

    awaitClose {
        database.unregisterObserver(observer)
    }
}
```

### 6.2 CompletableFuture Interop

```kotlin
// ✅ CORRECT: Use await() extension from kotlinx-coroutines-jdk8
suspend fun fetchFromJavaLibrary(): Data {
    val future: CompletableFuture<Data> = javaLib.asyncFetch()
    return future.await()  // ✅ Non-blocking
}

// ❌ WRONG: get() blocks the thread
suspend fun fetchFromJavaLibrary(): Data {
    val future: CompletableFuture<Data> = javaLib.asyncFetch()
    return future.get()  // ❌ Blocks thread
}
```

---

## 7. Kotlin Multiplatform (KMP)

### 7.1 expect/actual Pattern

```kotlin
// Common module
expect class PlatformContext

expect fun getDeviceId(): String

expect fun getCurrentTimestamp(): Long

// Android actual
actual typealias PlatformContext = Context

actual fun getDeviceId(): String {
    return Settings.Secure.getString(
        appContext.contentResolver,
        Settings.Secure.ANDROID_ID
    )
}

// iOS actual
actual typealias PlatformContext = NSObject

actual fun getDeviceId(): String {
    return UIDevice.currentDevice.identifierForVendor?.UUIDString ?: "unknown"
}
```

### 7.2 KMP Project Structure

```
shared/
├── src/
│   ├── commonMain/          # Shared Kotlin code
│   │   └── kotlin/
│   │       ├── domain/      # Business logic
│   │       ├── data/        # Repositories
│   │       └── di/          # Shared DI
│   ├── androidMain/         # Android-specific
│   │   └── kotlin/
│   │       └── platform/
│   └── iosMain/             # iOS-specific
│       └── kotlin/
│           └── platform/
```

### 7.3 Compose Multiplatform

```kotlin
// CommonMain
@Composable
fun SharedApp() {
    val viewModel = getViewModel { AppViewModel() }
    val state by viewModel.state.collectAsState()
    MaterialTheme {
        when (state) {
            is AppState.Loading -> LoadingScreen()
            is AppState.Success -> SuccessScreen(state.data)
        }
    }
}

// Platform-specific UI where needed (camera, GPS, etc.)
// These are OUT of scope for Compose Multiplatform — use expect/actual
```

---

## 8. Ktor Server Patterns

### 8.1 Application Structure

```kotlin
fun Application.module() {
    // Configuration
    configureSerialization()
    configureMonitoring()
    configureHTTP()
    configureSecurity()
    configureRouting()
}

fun Application.configureHTTP() {
    install(DefaultHeaders)
    install(CallLogging)
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
    }
    install(ContentNegotiation) {
        json(Json { prettyPrint = true })
    }
}

fun Application.configureSecurity() {
    install(Sessions) {
        cookie<UserSession>("user_session") {
            cookie.path = "/"
            cookie.maxAgeInSeconds = 3600
        }
    }
    install(Authentication) {
        form("auth-form") {
            userParamName = "username"
            passwordParamName = "password"
            validate { credentials ->
                if (isValid(credentials)) UserIdPrincipal(credentials.name) else null
            }
        }
        session<UserSession>("auth-session") {
            validate { session -> if (session.isValid()) session else null }
            challenge { call.respondRedirect("/login") }
        }
    }
}
```

### 8.2 Routing DSL

```kotlin
fun Application.configureRouting() {
    routing {
        // Public routes
        get("/health") { call.respond(mapOf("status" to "ok")) }

        // Authenticated routes
        authenticate("auth-session") {
            route("/api") {
                get("/me") {
                    val session = call.principal<UserSession>()
                    call.respond(session)
                }
                post("/orders") {
                    val order = call.receive<OrderRequest>()
                    val created = orderService.create(order)
                    call.respond(HttpStatusCode.Created, created)
                }
            }
        }

        // Nested auth with required strategy
        authenticate("auth-session", strategy = AuthenticationStrategy.Required) {
            authenticate("auth-basic", strategy = AuthenticationStrategy.Required) {
                get("/admin") {
                    call.respondText("Admin panel")
                }
            }
        }
    }
}
```

### 8.3 Error Handling with StatusPages

```kotlin
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<ValidationException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error"))
        }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ErrorResponse("Not found"))
        }
    }
}
```

### 8.4 Dependency Injection with Koin

```kotlin
fun Application.configureDI() {
    install(Koin) {
        modules(appModule)
    }
}

val appModule = module {
    single<Database> { Database.connect(dataSource = get()) }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    single<OrderService> { OrderServiceImpl(get()) }
}

// In routes
fun Route.orderRoutes() {
    val orderService: OrderService by inject()

    post("/orders") {
        val request = call.receive<OrderRequest>()
        val order = orderService.create(request)
        call.respond(HttpStatusCode.Created, order)
    }
}
```

### 8.5 Testing Ktor

```kotlin
class OrderRoutesTest {
    @Test
    fun `create order returns 201`() = testApplication {
        application {
            configureRouting()
            configureDI()
        }

        client.post("/orders") {
            contentType(ContentType.Application.Json)
            setBody(OrderRequest(item = "test", quantity = 1))
        }.apply {
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun `unauthorized request returns 401`() = testApplication {
        application { configureRouting() }

        client.get("/api/me").apply {
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}
```

---

## 9. Android-Specific Patterns

### 9.1 Repository Pattern

```kotlin
interface UserRepository {
    fun observeUsers(): Flow<List<User>>       // Continuous stream
    suspend fun getUser(id: String): User      // One-shot
    suspend fun saveUser(user: User): Unit     // One-shot write
}

class UserRepositoryImpl(
    private val api: UserApi,
    private val dao: UserDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    override fun observeUsers(): Flow<List<User>> = dao.observeUsers()
        .flowOn(ioDispatcher)

    override suspend fun getUser(id: String): User = withContext(ioDispatcher) {
        dao.getUser(id) ?: api.fetchUser(id).also { dao.insert(it) }
    }

    override suspend fun saveUser(user: User): Unit = withContext(ioDispatcher) {
        api.saveUser(user)
        dao.insert(user)
    }
}
```

### 9.2 UseCase Pattern

```kotlin
// UseCases are suspend functions that do one thing
class GetUserProfileUseCase(
    private val userRepository: UserRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend operator fun invoke(userId: String): UserProfile = withContext(ioDispatcher) {
        userRepository.getUser(userId).toDomain()
    }
}
```

### 9.3 ViewModel Best Practices

```kotlin
// ✅ CORRECT: ViewModel creates coroutines
class ProfileViewModel(
    private val getUserProfile: GetUserProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(userId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            _uiState.value = try {
                ProfileUiState.Success(getUserProfile(userId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ProfileUiState.Error(e.message)
            }
        }
    }
}

// ❌ WRONG: Exposing suspend functions from ViewModel for business logic
class BadViewModel : ViewModel() {
    suspend fun loadData(): Data = repository.getData()  // Caller controls scope!
}
```

---

## 10. Common Anti-Patterns Checklist

- [ ] No `GlobalScope` usage — use framework or injected scopes
- [ ] `async` calls have corresponding `await` calls
- [ ] Structured concurrency maintained (children cancelled with parents)
- [ ] `awaitAll` in `coroutineScope`: first failure cancels others; use `supervisorScope` for independent failures
- [ ] `CancellationException` never caught or swallowed — always rethrown
- [ ] Suspend functions are main-safe (move blocking work to `Dispatchers.IO` internally)
- [ ] Dispatchers are injected, not hardcoded
- [ ] `MutableStateFlow`/`MutableSharedFlow` are private; expose read-only via `.asStateFlow()`/`.asSharedFlow()`
- [ ] Flow collection uses `repeatOnLifecycle(STARTED)` or `collectAsStateWithLifecycle()`
- [ ] No blocking calls inside `flow { }` — use `flowOn()` to switch context
- [ ] `collectLatest` only when cancelling in-flight work is acceptable
- [ ] Tests use `runTest` with virtual time, not `runBlocking`
- [ ] ViewModel creates coroutines, not the View
- [ ] Long-running guaranteed work uses WorkManager, not app scope
- [ ] Channels are properly closed; `consumeEach` only for single consumer

---

## 11. Key References

- **Official Coroutines Guide:** https://kotlinlang.org/docs/coroutines-guide.html
- **Coroutines Best Practices (Android):** https://developer.android.com/kotlin/coroutines/coroutines-best-practices
- **Flow Documentation:** https://kotlinlang.org/docs/flow.html
- **StateFlow & SharedFlow:** https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/
- **Ktor Documentation:** https://ktor.io/docs/
- **Kotlin Multiplatform:** https://kotlinlang.org/docs/multiplatform.html
- **Structured Concurrency:** https://kotlinlang.org/docs/coroutines-guide.html#structured-concurrency

---

*Last updated: 2026-09-01. Synthesized from official Kotlin docs, Context7, Android Developers, and production best practices from 15+ skill sources.*
