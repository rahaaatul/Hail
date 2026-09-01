# Kotlin & Android Ultimate Skill

Comprehensive reference for idiomatic Kotlin, coroutines, Flow, Ktor, and Android development. Combines best practices from official documentation, community patterns, and production-tested conventions current as of 2026.

**Use this skill when:**
- Writing or reviewing Kotlin code (Android, KMP, or server-side)
- Designing asynchronous architectures with coroutines and Flow
- Building Ktor backend services
- Implementing Kotlin Multiplatform (KMP) shared logic
- Reviewing code quality, testing strategies, or structured concurrency

---

## Table of Contents

1. [Kotlin Fundamentals](#1-kotlin-fundamentals)
2. [Coroutines & Structured Concurrency](#2-coroutines--structured-concurrency)
3. [Flow & Reactive Streams](#3-flow--reactive-streams)
4. [Dispatcher Selection](#4-dispatcher-selection)
5. [Error Handling & Cancellation](#5-error-handling--cancellation)
6. [Android Patterns](#6-android-patterns)
7. [Ktor Server Patterns](#7-ktor-server-patterns)
8. [Kotlin Multiplatform (KMP)](#8-kotlin-multiplatform-kmp)
9. [Testing](#9-testing)
10. [Detekt & Ktlint Standards](#10-detekt--ktlint-standards)
11. [Java-to-Kotlin Migration](#11-java-to-kotlin-migration)
12. [Anti-Patterns Checklist](#12-anti-patterns-checklist)

---

## 1. Kotlin Fundamentals

### Core Principles
- **Null safety**: Prefer nullable types (`T?`) over `@Nullable` annotations. Use safe calls (`?.`), Elvis (`?:`), and `let` over explicit null checks.
- **Immutability**: Prefer `val` over `var`, `List` over `MutableList`, `copy()` over mutation.
- **Expression-oriented**: Prefer `if`/`when` expressions over statements; use `runCatching { }` for safe fallible operations.
- **Scope functions**: Use `let` (transform + null-check), `apply` (configure + return self), `run` (compute + return result), `also` (side-effect + return self), `with` (operate on non-null receiver).
- **Sealed classes/interfaces** for state models — exhaustive `when` without `else`.

### Idiomatic Patterns
```kotlin
// Data class with copy()
data class User(val id: String, val name: val email: String)

// Sealed state model
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val cause: Throwable) : UiState<Nothing>
}

// Extension functions over utility classes
fun String.isEmailValid(): Boolean = contains("@") && contains(".")

// Type-safe builders / DSL
fun user(block: UserBuilder.() -> Unit): User = UserBuilder().apply(block).build()
```

### Collections
```kotlin
// Prefer read-only operations
val active = users.filter { it.isActive }.map { it.name }

// Use sequence() for chained operations on large collections
val result = largeList.asSequence()
    .filter { it.isValid }
    .map { it.transform() }
    .take(10)
    .toList()

// Destructuring
val (first, second) = pair
for ((key, value) in map) { }
```

---

## 2. Coroutines & Structured Concurrency

### The Golden Rule
Coroutines form a **tree hierarchy** of parent/child tasks with linked lifecycles. A parent waits for children; parent cancellation cancels all children recursively.

### Scope Selection Decision Table

| Context | Scope | Cancellation point |
|---------|-------|-------------------|
| ViewModel | `viewModelScope` | `onCleared()` |
| Fragment/Activity | `viewLifecycleOwner.lifecycleScope` | `Lifecycle.State.DESTROYED` |
| Suspend function (fail-together) | `coroutineScope { }` | First child failure cancels all |
| Suspend function (independent failures) | `supervisorScope { }` | One failure doesn't cancel siblings |
| Compose composable | `rememberCoroutineScope()` | Composition disposal |
| Tests | `runTest` | Virtual time, deterministic |
| App-wide background | Custom `CoroutineScope` (DI) | Managed by owner |
| **NEVER in production** | `GlobalScope` | No lifecycle → leaks |

### Structured Concurrency Patterns
```kotlin
// FAIL-TOGETHER: first exception cancels siblings
suspend fun loadDashboard(): Dashboard = coroutineScope {
    val user = async { repo.getUser() }
    val orders = async { repo.getOrders() }
    Dashboard(user.await(), orders.await())  // if user fails, orders cancelled
}

// INDEPENDENT FAILURES: each sibling isolated
suspend fun loadAll(): Combined = supervisorScope {
    val a = async { riskyCallA() }
    val b = async { riskyCallB() }
    Combined(a.awaitCatching().getOrNull(), b.awaitCatching().getOrNull())
}

// PARALLEL DECOMPOSITION with coroutineScope
suspend fun processItems(items: List<Item>): List<Result> = coroutineScope {
    items.map { async { process(it) } }.awaitAll()
}
```

### launch vs async

| Builder | Returns | Use when |
|---------|---------|----------|
| `launch` | `Job` | Fire-and-forget; no result needed |
| `async` | `Deferred<T>` | Parallel computation; result needed via `await()` |

**Rule**: Every `async` must have a corresponding `await`. Unawaited `async` silently drops exceptions.

---

## 3. Flow & Reactive Streams

### Cold vs Hot Decision Table

| Type | Has current value | Replays to new collectors | Use for |
|------|-------------------|---------------------------|---------|
| Cold `Flow` | No | No (re-executes) | One-shot data, repository queries |
| `StateFlow` | Yes | Yes (replay = 1) | UI state, ViewModel state |
| `SharedFlow` | No | Configurable | One-shot events, notifications |

### StateFlow Pattern (ViewModel)
```kotlin
class OrderViewModel(
    private val repo: OrderRepository
) : ViewModel() {

    // Private mutable backing property
    private val _uiState = MutableStateFlow<OrderUiState>(OrderUiState.Loading)
    // Public read-only exposure
    val uiState: StateFlow<OrderUiState> = _uiState.asStateFlow()

    // Cold Flow → hot StateFlow conversion
    val orders: StateFlow<List<Order>> = repo.ordersFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrderUiState.Loading
            repo.fetchOrders()
                .catch { _uiState.value = OrderUiState.Error(it) }
                .collect { _uiState.value = OrderUiState.Success(it) }
        }
    }
}
```

### SharedFlow Pattern (One-shot Events)
```kotlin
class EventViewModel : ViewModel() {
    private val _events = MutableSharedFlow<UiEvent>(
        replay = 0,  // Late collectors don't get past events
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

// Collecting one-shot events in Compose
@Composable
fun MyScreen(viewModel: EventViewModel) {
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.NavigateToDetail -> navigate(event.id)
                is UiEvent.ShowSnackbar -> snackbarHost.show(event.message)
            }
        }
    }
}
```

### Essential Flow Operators
```kotlin
flow
    .map { transform(it) }                    // Transform each value
    .filter { it.isValid }                     // Keep only matching
    .filterNot { it.isExpired }                // Remove matching
    .distinctUntilChanged()                    // Dedup consecutive duplicates
    .debounce(300)                             // Wait for pause (search input)
    .flatMapLatest { fetchDetails(it) }        // Cancel previous on new value
    .catch { emit(emptyList()) }               // Handle upstream errors
    .onEach { analytics.track(it) }             // Side effect without transforming
    .stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
```

### Flow Collection in UI (Android/Compose)

| Scenario | Pattern |
|----------|---------|
| Activity/Fragment collecting StateFlow | `repeatOnLifecycle(Lifecycle.State.STARTED) { flow.collect { } }` |
| Compose collecting StateFlow | `val state by flow.collectAsStateWithLifecycle()` |
| Compose collecting one-shot events | `LaunchedEffect(Unit) { flow.collect { handleEvent(it) } }` |

**collectAsStateWithLifecycle()** automatically starts/stops collection based on lifecycle, reducing recompositions when UI is not visible.

### Flow Anti-Patterns
```kotlin
// WRONG: blocking in flow builder
flow { emit blockingApiCall() }

// RIGHT: use flowOn to shift context
flow { suspendApiCall() }.flowOn(Dispatchers.IO)

// WRONG: cold Flow as shared state (each collector triggers new execution)
val users: Flow<List<User>> = flow { emit(api.fetchUsers()) }

// RIGHT: hot StateFlow for shared state
val users: StateFlow<List<User>> = repo.usersFlow().stateIn(viewModelScope, WhileSubscribed(5000), emptyList())
```

---

## 4. Dispatcher Selection

### Decision Table

| Dispatcher | Thread Pool | Use for |
|------------|-------------|---------|
| `Dispatchers.Main` | Single main thread | UI updates, Compose state mutations, lightweight work |
| `Dispatchers.Main.immediate` | Main (no dispatch if already on main) | When you must run synchronously on main |
| `Dispatchers.IO` | Elastic thread pool (64+ threads) | Network calls, file I/O, database queries, blocking APIs |
| `Dispatchers.Default` | CPU-core-sized pool | CPU-intensive computation, sorting large lists, JSON parsing |
| `Dispatchers.Unconfined` | No confinement | Rare; testing or `withContext` internals only |

### Dispatcher Injection for Testability
```kotlin
// DON'T hardcode dispatchers
class UserRepository {
    suspend fun getUser(): User = withContext(Dispatchers.IO) { api.fetchUser() }
}

// DO inject dispatchers via constructor
class UserRepository(
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getUser(): User = withContext(dispatcher) { api.fetchUser() }
}

// Test with UnconfinedTestDispatcher
@Test
fun getUser_returnsUser() = runTest {
    val repo = UserRepository(dispatcher = UnconfinedTestDispatcher(testScheduler))
    val user = repo.getUser()
    assertEquals(expected, user)
}
```

### Main-Safe Suspend Functions
All suspend functions should be safe to call from the main thread. The class doing the work (not the caller) is responsible for switching dispatchers:

```kotlin
// Repository owns the dispatcher switch
class AppRepository(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // Main-safe: internally switches to IO
    suspend fun fetchData(): Data = withContext(ioDispatcher) { blockingCall() }
}
```

---

## 5. Error Handling & Cancellation

### Exception Handling Patterns
```kotlin
// ViewModel with try/catch
fun loadData() {
    viewModelScope.launch {
        _uiState.value = UiState.Loading
        try {
            val data = repository.getData()
            _uiState.value = UiState.Success(data)
        } catch (e: CancellationException) {
            throw e  // ALWAYS rethrow CancellationException
        } catch (e: Exception) {
            _uiState.value = UiState.Error(e)
        }
    }
}

// runCatching for fallible operations
val result = runCatching { riskyCall() }.getOrDefault(defaultValue)

// Result<T> wrapper
suspend fun fetchUser(): Result<User> = runCatching { api.getUser() }
```

### Cancellation Rules
1. **Never swallow `CancellationException`** — always rethrow it if caught.
2. **Cooperative cancellation**: coroutines cancel only at suspension points. Check `ensureActive()` or `yield()` in long-running loops.
3. **`ensureActive()`**: explicitly checks cancellation in CPU-bound work.

```kotlin
// Cancellable loop
suspend fun processFiles(files: List<File>) = withContext(Dispatchers.IO) {
    for (file in files) {
        ensureActive()  // throws CancellationException if cancelled
        process(file)
    }
}
```

### CoroutineExceptionHandler
```kotlin
val handler = CoroutineExceptionHandler { _, throwable ->
    // Log to crash reporting; don't try to recover
    FirebaseCrashlytics.recordException(throwable)
}

// Attach to top-level scopes (viewModelScope already has this internally)
val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main + handler)
```

---

## 6. Android Patterns

### ViewModel Best Practices
```kotlin
@HiltViewModel
class ProductViewModel @Inject constructor(
    private val getProductsUseCase: GetProductsUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProductUiState>(ProductUiState.Loading)
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    // Survive process death with SavedStateHandle
    private val query: String = savedStateHandle["query"] ?: ""

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            getProductsUseCase(query)
                .onStart { _uiState.value = ProductUiState.Loading }
                .catch { _uiState.value = ProductUiState.Error(it) }
                .collect { _uiState.value = ProductUiState.Success(it) }
        }
    }

    // For one-shot actions (navigations, toasts)
    fun onProductClick(productId: String) {
        viewModelScope.launch {
            _events.emit(ProductEvent.NavigateToDetail(productId))
        }
    }
}
```

### Fragment Flow Collection
```kotlin
// Fragment
override fun onViewCreated(view: View, Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.uiState.collect { state ->
                render(state)
            }
        }
    }
}
```

### Compose Integration
```kotlin
@Composable
fun ProductScreen(viewModel: ProductViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is ProductUiState.Loading -> LoadingIndicator()
        is ProductUiState.Success -> ProductList(state.products)
        is ProductUiState.Error -> ErrorMessage(state.cause)
    }

    // One-shot events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProductEvent.NavigateToDetail -> navController.navigate(event.route)
            }
        }
    }
}
```

### Lifecycle-Aware Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        UI Layer (Compose/XML)                   │
│  collectAsStateWithLifecycle() / repeatOnLifecycle(STARTED)     │
└────────────────────────────────┬────────────────────────────────┘
                                 │ observes StateFlow
┌────────────────────────────────▼────────────────────────────────┐
│                      Presentation Layer                         │
│  ViewModel + viewModelScope + StateFlow + SharedFlow            │
│  StateIn(cold Flow → hot StateFlow, WhileSubscribed(5000))     │
└────────────────────────────────┬────────────────────────────────┘
                                 │ calls suspend functions
┌────────────────────────────────▼────────────────────────────────┐
│                        Domain Layer                             │
│  Use cases: suspend functions with coroutineScope/supervisorScope│
└────────────────────────────────┬────────────────────────────────┘
                                 │ calls suspend functions
┌────────────────────────────────▼────────────────────────────────┐
│                         Data Layer                              │
│  Repository: suspend functions + cold Flows                     │
│  Room DAOs: Flow return types                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## 7. Ktor Server Patterns

### Project Structure
```
src/main/kotlin/
├── Application.kt          # main(), plugin installation
├── config/
│   ├── DatabaseConfig.kt
│   └── AppConfig.kt
├── routes/
│   ├── UserRoutes.kt
│   └── OrderRoutes.kt
├── service/
│   ├── UserService.kt
│   └── OrderService.kt
├── repository/
│   ├── UserRepository.kt
│   └── OrderRepository.kt
├── model/
│   ├── User.kt
│   └── dto/
│       ├── CreateUserRequest.kt
│       └── UserResponse.kt
└── plugins/
    ├── Routing.kt
    ├── Security.kt
    ├── Serialization.kt
    └── DI.kt
```

### Essential Plugin Configuration
```kotlin
// Application.kt
fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureSerialization()
    configureMonitoring()
    configureHTTP()  // CORS, ContentNegotiation
    configureSecurity()  // JWT auth
    configureDI()  // Koin
    configureRouting()
    configureStatusPages()  // Global error handling
}
```

### Routing DSL
```kotlin
fun Application.configureRouting() {
    routing {
        route("/api/v1") {
            userRoutes()
            orderRoutes()

            // Authenticated routes
            authenticate("auth-jwt") {
                get("/profile") {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.getClaim("userId", String::class)
                    call.respond(service.getProfile(userId))
                }
            }
        }
    }
}

// UserRoutes.kt
fun Route.userRoutes() {
    route("/users") {
        post<CreateUserRequest> { dto ->
            val user = service.create(dto)
            call.respond(HttpStatusCode.Created, user)
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw BadRequestException("Missing id")
            val user = service.getById(id) ?: throw NotFoundException("User not found")
            call.respond(user)
        }
    }
}
```

### JWT Authentication
```kotlin
fun Application.configureSecurity() {
    val jwtConfig = environment.config.config("jwt")

    install(Authentication) {
        jwt("auth-jwt") {
            realm = jwtConfig.property("realm").getString()
            verifier(
                JWT.require(Algorithm.HMAC256(jwtConfig.property("secret").getString()))
                    .withAudience(jwtConfig.property("audience").getString())
                    .withIssuer(jwtConfig.property("issuer").getString())
                    .build()
            )
            validate { credential ->
                if (credential.payload.getClaim("userId").asString() != null) {
                    JWTPrincipal(credential.payload)
                } else null
            }
            challenge { _, _ ->
                call.respond(HttpStatusCode.Unauthorized, "Token is not valid")
            }
        }
    }
}
```

### Status Pages (Global Error Handling)
```kotlin
fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<BadRequestException> { call, cause ->
            call.respond(HttpStatusCode.BadRequest, ErrorResponse(cause.message ?: "Bad request"))
        }
        exception<NotFoundException> { call, cause ->
            call.respond(HttpStatusCode.NotFound, ErrorResponse(cause.message ?: "Not found"))
        }
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled exception", cause)
            call.respond(HttpStatusCode.InternalServerError, ErrorResponse("Internal error"))
        }
    }
}
```

### Koin Dependency Injection
```kotlin
fun Application.configureDI() {
    install(Koin) {
        modules(appModule)
    }
}

val appModule = module {
    single { DatabaseFactory(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<OrderRepository> { OrderRepositoryImpl(get()) }
    factory { UserService(get(), get()) }
}

// Usage in route: call.get<UserService>() or inject<KClass> { parametersOf(call) }
```

### Integration Testing
```kotlin
class ApplicationTest {
    @Test
    fun `get user returns 200`() = testApplication {
        application {
            module()
        }
        client.get("/api/v1/users/1").apply {
            assertEquals(HttpStatusCode.OK, status)
        }
    }

    @Test
    fun `unauthorized without token`() = testApplication {
        application { module() }
        client.get("/api/v1/profile").apply {
            assertEquals(HttpStatusCode.Unauthorized, status)
        }
    }
}
```

---

## 8. Kotlin Multiplatform (KMP)

### expect/actual Pattern
```kotlin
// Common (shared)
expect class PlatformContext

expect fun getPlatformName(): String

interface Repository {
    suspend fun getData(): String
}

// Android
actual typealias PlatformContext = Context

actual fun getPlatformName(): String = "Android ${Build.VERSION.RELEASE}"

class AndroidRepository : Repository {
    override suspend fun getData(): String = "Android data"
}

// iOS
actual class PlatformContext(val value: NSObject)

actual fun getPlatformName(): String = UIDevice.currentDevice.systemName()

class IOSRepository : Repository {
    override suspend fun getData(): String = "iOS data"
}
```

### KMP Project Structure
```
shared/
├── src/
│   ├── commonMain/kotlin/          # Shared business logic
│   │   ├── domain/
│   │   ├── data/
│   │   └── presentation/           # Shared ViewModels (optional)
│   ├── androidMain/kotlin/         # Android-specific
│   │   └── di/
│   ├── iosMain/kotlin/             # iOS-specific
│   │   └── di/
│   └── commonTest/kotlin/          # Shared tests
```

### Compose Multiplatform
```kotlin
// Shared composable
@Composable
fun SharedApp() {
    val viewModel = viewModel { SharedViewModel() }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Shared UI code
}

// Platform-specific integrations remain in platform modules
// - Permissions, Camera, GPS, Bluetooth → expect/actual or platform code
// - Navigation → often platform-specific
```

### Dispatchers in KMP
```kotlin
// commonMain
expect val ioDispatcher: CoroutineDispatcher

// androidMain
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

// iosMain
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.Default  // No IO dispatcher on iOS
```

---

## 9. Testing

### Coroutine Testing Patterns
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {

    @get:Rule
    val mainRule = MainDispatcherRule()  // Sets UnconfinedTestDispatcher as Main

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun `loadData updates state to success`() = runTest {
        // Given
        val repository = mockk<Repository> {
            coEvery { getData() } returns Result.success(data)
        }
        val viewModel = MyViewModel(repository, testDispatcher)

        // When
        viewModel.loadData()

        // Then
        val state = viewModel.uiState.value
        assertThat(state).isInstanceOf(UiState.Success::class.java)
    }

    @Test
    fun `loadData emits error on failure`() = runTest {
        val repository = mockk<Repository> {
            coEvery { getData() } throws IOException("Network error")
        }
        val viewModel = MyViewModel(repository, testDispatcher)

        viewModel.loadData()

        advanceUntilIdle()  // Let coroutines complete
        assertThat(viewModel.uiState.value).isInstanceOf(UiState.Error::class.java)
    }
}

// TestDispatcherRule
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(dispatcher)
    }
    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

### Flow Testing with Turbine
```kotlin
@Test
fun `uiState emits Loading then Success`() = runTest {
    viewModel.uiState.test {
        viewModel.loadData()

        assertThat(awaitItem()).isInstanceOf(UiState.Loading::class.java)
        assertThat(awaitItem()).isInstanceOf(UiState.Success::class.java)

        cancelAndIgnoreRemainingEvents()
    }
}
```

### Testing Suspended Functions
```kotlin
// runTest uses virtual time — delays are skipped automatically
@Test
fun `delayed emission`() = runTest {
    val flow = flow {
        delay(1000)  // Skipped in tests
        emit("value")
    }

    flow.test {
        assertThat(awaitItem()).isEqualTo("value")
        awaitComplete()
    }
}
```

### Parallel Testing
```kotlin
@Test
fun `parallel requests complete independently`() = runTest {
    val results = supervisorScope {
        val a = async { repo.fetchA() }
        val b = async { repo.fetchB() }
        val c = async { repo.fetchC() }
        listOf(a, b, c).map { it.awaitCatching().getOrNull() }
    }
    assertEquals(3, results.size)
}
```

---

## 10. Detekt & Ktlint Standards

### Key Rules
| Rule | Description |
|------|-------------|
| `TooManyFunctions` | Max 15 functions per class |
| `ComplexMethod` | Cyclomatic complexity ≤ 15 |
| `LongMethod` | Max 60 lines per function |
| `LargeClass` | Max 600 lines per class |
| `MagicNumber` | Named constants over literals |
| `ForbiddenComment` | No TODO/FIXME/HACK |
| `SwallowedException` | Don't catch and ignore |
| `ThrowingExceptionsWithoutMessageOrCause` | Always message or cause |

### detekt.yml Configuration
```yaml
complexity:
  active: true
  ComplexMethod:
    threshold: 15
    ignoreSimpleWhenEntries: true
  LargeClass:
    threshold: 600
  TooManyFunctions:
    thresholdInFiles: 15
    thresholdInClasses: 15

style:
  active: true
  MagicNumber:
    active: true
    ignorePropertyDeclaration: true
    ignoreAnnotation: true
    ignoreEnums: true
    ignoreNumbers:
      - '-1'
      - '0'
      - '1'
      - '2'

exceptions:
  active: true
  SwallowedException:
    active: true
    ignoredExceptionTypes:
      - InterruptedException
      - MalformedURLException
      - ParseException
```

---

## 11. Java-to-Kotlin Migration

### Automated Patterns
| Java | Kotlin |
|------|--------|
| `new Type(args)` | `Type(args)` |
| `field = value; return this;` | `field = value; return this` → `.apply { }` |
| `if (x != null) { ... }` | `x?.let { ... }` |
| `return condition ? a : b` | `return if (condition) a else b` |
| Anonymous classes | Lambdas or function references |
| Getters/setters | Properties (`val`/`var`) |
| `Optional<T>` | Nullable `T?` with `?.` |
| Builder pattern | Named + default args, `apply { }` |

### Manual Review Points
- Null-safety audits: identify nullable vs non-null types
- Check exception handling (Kotlin has no checked exceptions)
- Replace `Stream` with collection operators or `Flow`
- Convert callback interfaces to `suspend` functions or `Flow`
- Replace `CompletableDeferred`/`Future` with coroutines `async`/`suspend`

---

## 12. Anti-Patterns Checklist

### Coroutine Anti-Patterns
- [ ] No `GlobalScope` usage (use framework or injected scopes)
- [ ] Every `async` has a corresponding `await`
- [ ] Structured concurrency maintained (children cancelled with parents)
- [ ] `awaitAll` in `coroutineScope`: first failure cancels others — use `supervisorScope` for independence
- [ ] `CancellationException` never swallowed (always rethrown)
- [ ] No fire-and-forget `launch` inside repositories
- [ ] Dispatchers injected, not hardcoded (for testability)

### Flow Anti-Patterns
- [ ] No blocking calls in `flow { }` builder
- [ ] No cold `Flow` as shared state (use `StateFlow`)
- [ ] `collectLatest` only when cancellation of previous work is acceptable
- [ ] `MutableStateFlow`/`MutableSharedFlow` not exposed as public (use backing property + `asStateFlow()`)
- [ ] `flowOn()` applied correctly when emission context differs

### Android Anti-Patterns
- [ ] ViewModel creates coroutines (not exposed suspend functions for business logic)
- [ ] No direct coroutine launching in Composable functions (use `LaunchedEffect`/`rememberCoroutineScope`)
- [ ] Flow collection tied to lifecycle (`repeatOnLifecycle`/`collectAsStateWithLifecycle`)
- [ ] No `GlobalScope` for app-scoped work (use injected scope or `WorkManager`)
- [ ] Errors handled in ViewModel (not silently dropped)

### Testing Anti-Patterns
- [ ] `runBlocking` in tests → use `runTest` for virtual time
- [ ] `TestDispatcher` injected and shared across all test dependencies
- [ ] `advanceUntilIdle()` used to let coroutines complete
- [ ] `Turbine` used for Flow assertions (not manual collection)
- [ ] `Dispatchers.setMain(testDispatcher)` in test rule

### General Kotlin Anti-Patterns
- [ ] No `!!` (non-null assert) — use `?: return`, `?: throw`, `?.let`
- [ ] No empty catch blocks
- [ ] No catching `Throwable` or `Exception` broadly — catch specific types
- [ ] No mutable collections exposed publicly (use `toImmutableList()` or `List` interface)
- [ ] No platform types leaking into public APIs

---

## Quick Reference: Scope Decision Tree

```
Are you in a ViewModel?
├── Yes → viewModelScope
└── No
    Are you in a Fragment/Activity/LifecycleOwner?
    ├── Yes → viewLifecycleOwner.lifecycleScope (+ repeatOnLifecycle if collecting Flow)
    └── No
        Are you in Compose?
        ├── Yes → rememberCoroutineScope() / LaunchedEffect
        └── No
            Is this test code?
            ├── Yes → runTest { }
            └── No
                Should a failure in one child cancel others?
                ├── Yes → coroutineScope { }
                └── No (independent failures) → supervisorScope { }
```

## Quick Reference: Flow vs StateFlow vs SharedFlow

```
Do you need to share state across multiple collectors?
├── No → cold Flow
└── Yes
    Does the data represent current state (has a "last known value")?
    ├── Yes → StateFlow (replay = 1, always has current value)
    └── No (one-shot events, notifications)
        → SharedFlow (replay = 0, appropriate onBufferOverflow)
```

## Quick Reference: Dispatcher Decision Tree

```
Is the work I/O-bound (network, file, database)?
├── Yes → Dispatchers.IO
└── No
    Is the work CPU-bound (computation, parsing)?
    ├── Yes → Dispatchers.Default
    └── No (UI work)
        → Dispatchers.Main / Dispatchers.Main.immediate
```
