# Hail: XML-to-Compose Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate Hail's UI layer from XML Views + Fragments to Jetpack Compose, ending with a single `ComponentActivity` hosting Navigation 3 `NavDisplay`.

**Architecture:** Per-screen migration following Google's 10-step XML-to-Compose process. Create new Compose files alongside old Fragment/XML, validate, then delete old files. One git branch per screen. Data layer (Room, HailData, AppManager, AppMetaCache, Shizuku, Xposed) remains untouched.

**Tech Stack:** Jetpack Compose, Navigation 3 (1.2.0-alpha05 → alpha07 available, upgrade deferred), Material 3 Expressive (1.5.0-alpha27), material-kolor 5.0.1, Kotlin 2.4.10

**Spec:** docs/superpowers/specs/2026-09-02-compose-migration-design.md

## Global Constraints

- **Repo:** `rahaaatul/Hail` (never `aistra0528/Hail`)
- **minSdk:** 24 (Navigation 3 requires 23+, compatible)
- **compileSdk:** 37
- **Kotlin:** 2.4.10
- **Compose BOM:** 2026.08.00
- **Material 3:** 1.5.0-alpha27 (Material 3 Expressive)
- **material-kolor:** 5.0.1
- **AGENTS.md rules:** run `./gradlew :app:compileDebugKotlin` after every change, conventional commits, PR title `[Hail] <description>`, pre-push gate `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`
- **Branch naming:** `feature/migrate-<screen>-to-compose` for each phase
- **Code style:** 4 spaces, no tabs, K&R braces, `val` over `var`, PascalCase for Composable functions returning Unit
- **Testing:** Compose UI tests via `createAndroidComposeRule` in `app/src/androidTest/`
- **New dependency approval:** Navigation 3 + kotlinx-serialization require user approval (see Phase 0 Task 1-2)
- **No Hilt** — existing project doesn't use Hilt; use `AndroidViewModel` + `viewModel()` for Compose ViewModels

### Navigation 3 Version Decision: alpha05 → alpha07

**Finding:** `1.2.0-alpha07` is available (released July 29, 2026). We are currently on `alpha05` (July 1, 2026). Intermediate `alpha06` (July 15) also exists.

**alpha06 breaking changes (deep link APIs):**
- Removed `DeepLinkRequest.fromAction`, `fromMimeType`, `fromIntent`, `fromUri`, `fromUriString`
- Replaced with new constructors + `extras` field
- `DeepLinkMatcher.Filter` is now a functional interface (constructor field removed)

**alpha07 new features:**
- `BackStackMatcher` + `DeepLinkMatcher.withBackStack` for back stack-aware deep linking
- `DeepLinkSerializer` abstract class for non-primitive deep link arguments
- `DeepLinkMatcher` second type parameter `out R : MatchResult` (less casting)
- `WrappedMatchResult` class for layered match results
- `ResultEffect` uses `rememberUpdatedState`
- `EntryProvider` prioritizes key instances over key types
- `SceneState` previous scene calculation fix

**Decision: Defer upgrade to alpha07.**
- We are not using deep links in Phase 0–2 (no `DeepLinkMatcher` yet)
- Upgrading during active migration adds regression risk without immediate benefit
- Upgrade after Phase 3 (first real screen) or when deep linking is needed
- If upgrading, test deep link paths and `EntryProvider` key matching behavior

---

## Image Loading Strategy

**Finding:** No image loading library (Coil, Glide, Picasso, Accompanist) was in the project. `AppIconCache.loadIconBitmapAsync()` takes an `ImageView` — incompatible with Compose.

**Decision: Use Coil 2.7.0 (Google-recommended for Compose).**

Per Google/Android recommendations, Coil is the recommended image loading library for Jetpack Compose. It provides:
- `AsyncImage` composable for declarative image loading
- Built-in caching, placeholder/error handling
- `ImageRequest.Builder` for customization
- Lifecycle-aware loading

```kotlin
AsyncImage(
    model = ImageRequest.Builder(context)
        .data(applicationInfo)
        .size(64)
        .crossfade(false)
        .build(),
    contentDescription = null,
    modifier = modifier.size(64.dp),
    colorFilter = if (grayscale) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) else null,
    placeholder = painterResource(R.drawable.ic_round_apps),
    error = painterResource(R.drawable.ic_round_apps),
)
```

**Why Coil over custom bridge:**
- Google-recommended standard for Compose image loading
- Eliminates custom bitmap management code
- Handles lifecycle, caching, and memory automatically
- Version 2.7.0 is latest stable (verified via Maven Central)

---

## Compose State Management Patterns

**Existing pattern:** Local `remember { mutableStateOf(HailData.xxx) }` with write-back callbacks (used in SettingsFragment, AboutFragment).

**New pattern for screen ViewModels:**

```kotlin
class PagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    init {
        viewModelScope.launch {
            AppMetaCache.revision.collect { loadApps() }
        }
    }

    private fun loadApps() {
        _apps.value = HailData.checkedList.filter { /* tag + search filter */ }
    }
}

@Composable
fun PagerScreen(viewModel: PagerViewModel = viewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    // ...
}
```

**Key rule:** Use `collectAsStateWithLifecycle()` (from `androidx.lifecycle:lifecycle-runtime-compose`) for Flow collection — backed by `repeatOnLifecycle(STARTED)`. Never use `collect` directly in Compose.

---

## Existing Compose Foundation Audit

| Layer | Status | Key Files |
|-------|--------|-----------|
| **Theme** | ✅ Wired | `Theme.kt` — `HailTheme`, `MaterialExpressiveTheme`, `MotionScheme.expressive()`, `AppTypography`, `expressiveShapes` |
| **Navigation 3** | ✅ Scaffold | `HailNavHost.kt` — `rememberNavBackStack`, `NavDisplay`, `entryProvider` DSL with 5 `@Serializable` routes |
| **Bottom nav** | ✅ Production | `ExpressiveNavigationBar.kt` — Traditional/FloatingPill variants, `graphicsLayer` press animation, `semantics` |
| **Settings** | ✅ Migrated | `SettingsFragment` + `SettingsRows.kt` — full settings UI in Compose via `ComposeView` root |
| **About** | ✅ Migrated | `AboutFragment` — full Compose screen |
| **API Activity** | ✅ Hybrid | `ApiActivity` — `ComponentActivity` using `setContent` for dialogs/sheets |
| **Pager** | 🔶 Partial | `PagerFragment` — mostly ViewPager2/ViewBinding, but embeds `ComposeView` for tag picker dialog |
| **State management** | 🔶 Local only | Heavy use of `remember { mutableStateOf }`; no `ViewModel` + `StateFlow` observation in Compose yet |
| **AndroidView interop** | ❌ None | `AndroidView` not used; only `ComposeView` for Fragment embedding |

**Conventions to preserve:**
1. Wrap every screen in `HailTheme(state = HailThemeState()) { ... }`
2. Use expressive components (`ShortNavigationBar`, `PrimaryTabRow`, etc.) per the material3-expressive skill
3. `modifier` as first optional parameter; chained modifiers with leading dots on continuation lines
4. For Fragment embedding: `ComposeView` + `DisposeOnViewTreeLifecycleDestroyed` strategy
5. Settings-style state: local `remember { mutableStateOf(HailData.xxx) }` with write-back callbacks

---

## Google Recommendations for Jetpack Compose (Consolidated)

Source: Android Developer Docs (Context7 `/websites/developer_android` + `/websites/developer_android_develop_ui_compose` + `/websites/developer_android_develop_ui_compose_migrate`), verified against Hail's Compose BOM `2026.08.00` and Material 3 `1.5.0-alpha27`.

### Image Loading
- **Use Coil** (`io.coil-kt:coil-compose`) as the recommended image loading library for Compose.
- Use `AsyncImage` for declarative image loading with built-in caching, placeholder/error handling.
- Use `ImageRequest.Builder` for customization (`size()`, `crossfade()`, etc.).
- **Do not** use custom `produceState` + `Bitmap` bridges for standard image loading.

### State Management
- **ViewModel:** Expose `StateFlow` / `SharedFlow` from `AndroidViewModel`. Never expose `mutableStateOf` or `mutableStateListOf` from a ViewModel — those are Compose runtime types.
- **Compose:** Collect flows with `collectAsStateWithLifecycle()` from `androidx.lifecycle:lifecycle-runtime-compose`. This is backed by `repeatOnLifecycle(STARTED)` and avoids collecting when the screen is off.
- **UI state:** Use `sealed interface` for UI state (`Loading`, `Success`, `Error`) to make `when` exhaustive.
- **Fallible operations:** Prefer `runCatching { }.getOrNull()` / `.getOrDefault()` over try/catch when no built-in safe API exists.

### Lists & Grids
- **LazyColumn/LazyVerticalGrid:** Use for scrollable lists. Provide `key` and `contentType` to `items()` for better composition reuse and scroll restoration.
- **Grids:** Use `LazyVerticalGrid(columns = GridCells.Fixed(count))` for fixed column grids. `GridCells.Fixed` is standard in Compose Foundation.
- **Avoid:** Reading `LazyListState` scroll metrics in a way that triggers recomposition on every scroll event.

### Theming
- Wrap every screen in `HailTheme(state = HailThemeState()) { ... }` to receive animated `ColorScheme`, expressive typography, motion scheme, and system bar coloring.
- Use `MaterialTheme.colorScheme.primary/secondary/surface/onSurface` etc. — never hardcode colors.
- Use `MaterialTheme.typography.bodyLarge/bodyMedium/titleMediumEmphasized` etc.
- Use `MaterialTheme.motionScheme.fastEffectsSpec()` / `slowEffectsSpec()` for animated transitions.
- Use `CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)` for tonal elevation.
- Avoid `Modifier.shadow()` — use tonal color overlays for elevation.

### Interop
- **Compose in Views:** Use `ComposeView` + `setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)` for embedding Compose in Fragments.
- **Views in Compose:** Use `AndroidView` or `AndroidViewBinding` for embedding legacy Views in Compose.
- **Activity migration:** Use `ComponentActivity` + `setContent` + `enableEdgeToEdge()`.

### Side Effects & Lifecycle
- **LaunchedEffect:** Use for suspend work — cancelled when keys change or composable leaves.
- **DisposableEffect:** Use for lifecycle observers — requires `onDispose { }` cleanup.
- **rememberUpdatedState:** Use for lambdas that need to reference updated values without restarting the effect.
- **BackHandler:** Use `BackHandler` composable for back press handling in Compose (replaces `OnBackPressedCallback`).

### Navigation 3
- Use `rememberNavBackStack(vararg keys: NavKey)` for persistent back stack.
- Use `NavDisplay(backStack, onBack, entryProvider)` from `androidx.navigation3.ui`.
- Use `entryProvider { entry<Route> { ... } }` DSL.
- For multiple back stacks (bottom nav): use `NavigationState` + `Navigator` pattern with `rememberNavigationState()`.
- For ViewModel persistence across navigation: use `rememberViewModelStoreNavEntryDecorator()`.
- For deep linking: use `DeepLinkPattern` + `DeepLinkMatcher` (alpha06+ API).

### Accessibility
- 48dp minimum touch targets.
- `contentDescription = null` for decorative icons.
- Use `Modifier.semantics { ... }` for custom gestures.
- Prefer `onSurface` / `onSurfaceVariant` for text/icons; avoid hardcoded `Color.Black`/`Color.White`.

### Code Style
- `modifier` as first optional parameter in public composables.
- `@Composable` functions returning `Unit` use `PascalCase` noun names.
- `@File:OptIn(ExperimentalMaterial3ExpressiveApi::class)` for Expressive APIs.
- 4 spaces, no tabs, K&R braces, `val` over `var`.

---

## Material 3 Expressive Reference (1.5.0-alpha27)

Source: Context7 `/websites/developer_android_develop_ui_compose` + `.kilo/skills/material3-expressive/SKILL.md`

### Seven Pillars
Material 3 Expressive coordinates **Color**, **Typography**, **Shape**, **Motion**, **Layout**, **Components**, and **Icons**. Partial styling produces generic-looking apps.

### 1. Color — 48-Role System
Expressive uses an expanded **48-role color scheme** with hero elements receiving tonal-surfaced container colors (`surfaceContainerHigh`) and supporting surfaces staying neutral. Use `withContrastLevel(ContrastLevel.High)` for AA contrast. Fixed accent colors remain constant across light/dark themes.

### 2. Typography — Emphasized Scale
Use `EmphasizedTypography` (tighter line height, bumped weight). Map roles: `display*` for hero, `headline*` for sections, `title*` for in-list, `body*` for content, `label*` for affordances.

### 3. Shape — Five Buckets, Morphing
35 shapes across 5 size buckets with automatic **morphing** transitions in `ButtonGroup`, `SegmentedButton`, and `SplitButton` driven by `motionScheme.defaultSpatialSpec()`. **Full-radius** corners (`RoundedCornerShape(percent = 100)`) reserved for one hero element per screen.

### 4. Motion — MotionScheme
`MotionScheme` returns specs for six slots. Route hero interactions through `MotionScheme.expressive()` and utilitarian UI through `MotionScheme.standard()`.

| Spec | Use for |
|---|---|
| `defaultSpatialSpec` | size/shape/position changes |
| `fastSpatialSpec` | button press, toggle morphs |
| `slowSpatialSpec` | sheet expand, hero enter |
| `defaultEffectsSpec` | color/alpha/content |
| `fastEffectsSpec` | hover/ripple tints |
| `slowEffectsSpec` | long crossfades |

Use with `animateDpAsState`/`animateColorAsState`, not hand-rolled springs.

### 5. Layout — Adaptive + Background Blur
Use `WindowSizeClass` for adaptive layouts. Apply **background blur** to overlays/navigation via translucent containers, pairing with tonal elevation (not drop shadows).

### 6. Components — What's New vs Standard M3

| Need | Expressive Component | Replaces |
|---|---|---|
| Short wait (<5s) | `LoadingIndicator` | `CircularProgressIndicator` |
| Bottom action bar | `DockedToolbar` | `BottomAppBar` |
| Contextual floating actions | `FloatingToolbar` (horizontal/vertical) | bespoke FAB row |
| Scroll-reactive bottom bar | `FlexibleBottomAppBar` | custom animated bar |
| Multi-select actions | `ButtonGroup` / `MultiChoiceSegmentedButtonRow` | `Row` of buttons |
| Primary + dropdown | `SplitButton` | FAB + menu |
| Hero top bar | `LargeFlexibleTopAppBar` / `MediumFlexibleTopAppBar` | `TopAppBar` |
| Tonal progress | `LinearWavyProgressIndicator` / `CircularWavyProgressIndicator` | linear/circular |

### 7. Icons — Material Symbols
Use Material Symbols (variable icon fonts) with **fill toggle** (Filled for selected, Outlined for unselected) in navigation. Never animate color alone — the fill change is the signal.

### Android 16+ Requirements
- **Edge-to-edge:** call `enableEdgeToEdge()` in every Activity's `onCreate`
- **Predictive back gesture:** supported out-of-the-box in `BottomSheet`, `NavigationDrawer`, `ModalBottomSheet`, `DockedToolbar`

### Accessibility Non-Negotiables
- **Contrast:** 4.5:1 minimum (3:1 for large text)
- **Touch targets:** 48dp minimum
- **Reduced motion:** fall back to `MotionScheme.standard()` when accessibility manager recommends shorter timeouts
- **TalkBack:** every `IconButton` needs `contentDescription`

### Key Deprecations → Replacements
| Deprecated | Replacement |
|---|---|
| `BottomAppBar` | `DockedToolbar` / `FlexibleBottomAppBar` |
| `CircularProgressIndicator` (short waits) | `LoadingIndicator` |
| `TopAppBar` | `LargeFlexibleTopAppBar` / `MediumFlexibleTopAppBar` / `CenterAlignedTopAppBar` |
| `ExtendedFloatingActionButton` | `FloatingActionButtonMenu` |
| Manual rounded `ListItem` | `surfaceContainer` + segmented shape |
| Only `MaterialTheme.shapes` | Set both `shapes` and `motionScheme` |

---

## Navigation 3 Reference (1.2.0-alpha05)

Source: Context7 `/websites/developer_android_develop_ui_compose` + nav3-recipes source + bytecode inspection

### Core Architecture
Navigation 3 gives you **full control** over the back stack — you own it as a Compose-backed `List` and `NavDisplay` simply observes it.

| Layer | Component | Package | Role |
|-------|-----------|---------|------|
| **State** | `NavKey` (interface) | `androidx.navigation3.runtime` | Marker interface: all back stack keys must implement this + `@Serializable` |
| **State** | `NavBackStack<T>` | `androidx.navigation3.runtime` | Mutable back stack of `NavKey` elements; integrates with Compose snapshot state |
| **State** | `rememberNavBackStack()` | `androidx.navigation3.runtime` | Remembers a `NavBackStack` across config changes & process death |
| **Resolution** | `NavEntry<T>` | `androidx.navigation3.runtime` | Wraps a key + its composable content + metadata + `contentKey` |
| **Resolution** | `entryProvider { }` DSL | `androidx.navigation3.runtime` | Lambda that maps back stack keys → `NavEntry` objects |
| **Resolution** | `EntryProviderScope.entry<T>()` | `androidx.navigation3.runtime` | Typed entry registration within the DSL |
| **Resolution** | `rememberDecoratedNavEntries()` | `androidx.navigation3.runtime` | Decorates `NavEntry` list with `NavEntryDecorator`s |
| **Display** | `NavDisplay()` | `androidx.navigation3.ui` | Observes back stack; renders matching `NavEntry` content with animations |
| **Display** | `SceneStrategy<T>` | `androidx.navigation3.scene` | Pluggable layout strategy (single pane, dialog, bottom sheet, list-detail, etc.) |
| **Display** | `NavEntryDecorator<T>` | `androidx.navigation3.runtime` | Adds cross-cutting behavior to entries (state saving, ViewModel scoping, result buses) |

### "You Own the Back Stack"
```kotlin
// Push (navigate forward)
backStack.add(RouteB("123"))
// Pop (navigate back)
backStack.removeLastOrNull()
```

### `NavKey` Requirements
Every key must:
1. **Implement `NavKey`** — marker interface signaling the key is saveable
2. **Be annotated with `@Serializable`** — for process-death persistence via `kotlinx.serialization`

```kotlin
@Serializable data object Home : NavKey
@Serializable data class Details(val id: String) : NavKey
```

### `NavDisplay` API
```kotlin
NavDisplay(
    backStack = backStack,
    modifier = Modifier,
    contentAlignment = Alignment.TopStart,
    onBack = { backStack.removeLastOrNull() },
    entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
    sceneStrategies = listOf(SinglePaneSceneStrategy()),
    entryProvider = entryProvider { ... }
)
```

### Two Ways to Define `entryProvider`
**Option A: Lambda with `when` (manual)**
```kotlin
entryProvider = { key ->
    when (key) {
        is HomeRoute -> NavEntry(key) { HomeScreen(...) }
        is DetailsRoute -> NavEntry(key) { DetailsScreen(...) }
        else -> NavEntry(key) { Text("Unknown") }
    }
}
```

**Option B: DSL with `entry<T>()` (recommended)**
```kotlin
entryProvider = entryProvider {
    entry<HomeRoute> { HomeScreen(...) }
    entry<DetailsRoute> { key -> DetailsScreen(key.id) }
}
```

### Scene Strategies
| Strategy | Purpose |
|----------|---------|
| `SinglePaneSceneStrategy<T>` | Default. Shows only topmost entry. |
| `DialogSceneStrategy<T>` | Shows entry as a dialog overlay. |
| `BottomSheetSceneStrategy<T>` | Shows entry as a modal bottom sheet. |
| `ListDetailSceneStrategy<T>` | Adaptive 1/2/3 pane layout. |
| `SupportingPaneSceneStrategy<T>` | Main + supporting pane layout. |

### `NavEntryDecorator`s
| Decorator | Purpose |
|-----------|---------|
| `rememberSaveableStateHolderNavEntryDecorator()` | Default. Saves/restores entry state. |
| `rememberViewModelStoreNavEntryDecorator()` | Provides `ViewModelStoreOwner` per `NavEntry`, enabling `viewModel()` scoped to individual entries. Requires `androidx.lifecycle:lifecycle-viewmodel-navigation3`. |
| `rememberResultEventBusNavEntryDecorator()` | Provides `ResultEventBus` for state-based result passing. |

### Multiple Back Stacks Pattern
For bottom navigation with per-tab back stacks:
```kotlin
class NavigationState(
    val startRoute: NavKey,
    topLevelRoute: MutableState<NavKey>,
    val backStacks: Map<NavKey, NavBackStack<NavKey>>
)

class Navigator(val state: NavigationState) {
    fun navigate(route: NavKey) {
        if (route in state.backStacks.keys) {
            state.topLevelRoute = route
        } else {
            state.backStacks[state.topLevelRoute]?.add(route)
        }
    }
}
```

### Animations
Three transition specs on `NavDisplay`:
| Parameter | Purpose |
|-----------|---------|
| `transitionSpec` | Forward navigation (push) animation |
| `popTransitionSpec` | Back navigation (pop) animation |
| `predictivePopTransitionSpec` | Predictive back gesture animation |

### ViewModel Integration
Use `rememberViewModelStoreNavEntryDecorator()` + `hiltViewModel()` or `viewModel()` inside entries. Requires `androidx.lifecycle:lifecycle-viewmodel-navigation3`.

### Navigation 2 → 3 Migration
| Nav 2 | Nav 3 |
|-------|-------|
| `NavController` | Custom back stack (you own it) |
| `NavHost` + `composable<T>` | `NavDisplay` + `entryProvider { entry<T>() {} }` |
| `navController.navigate(route)` | `backStack.add(route)` |
| `backStackEntry.toRoute<Route>()` | Lambda parameter `key` in `entry<T> { key -> }` |
| `hiltViewModel()` | `hiltViewModel()` (NavEntry scoped via decorator) + `creationCallback` for args |

---

## Pager / ViewPager2 Migration Reference

Source: Context7 `/websites/developer_android_develop_ui_compose`

### Core APIs
- `rememberPagerState(pageCount = { ... })` — manages page count, current page, scroll position
- `HorizontalPager(state = pagerState) { page -> ... }` — equivalent of `ViewPager2`
- `VerticalPager(state = pagerState) { page -> ... }` — vertical swipe
- `animateScrollToPage(page)` — programmatic navigation (launch in `CoroutineScope`)

### Tab APIs
- `PrimaryTabRow(selectedTabIndex = ...) { ... }` — replaces `TabLayout` for top tabs
- `Tab(selected = ..., onClick = ...) { ... }` — individual tab
- `Modifier.tabIndicatorOffset(currentTabPosition)` — indicator positioning

### Migration Pattern: ViewPager2 + TabLayout → HorizontalPager + PrimaryTabRow
```kotlin
val pagerState = rememberPagerState(pageCount = { tabTitles.size })
val coroutineScope = rememberCoroutineScope()

Column {
    PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                text = { Text(title) }
            )
        }
    }
    HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
        /* page content */
    }
}
```

### Best Practices
- Use `rememberPagerState { pageCount }` with lazy lambda when page count is dynamic
- Always launch `animateScrollToPage` in a `CoroutineScope`
- Pass `contentPadding` from `Scaffold` to the pager's modifier
- Keep `selectedTabIndex` in `PrimaryTabRow` == `pagerState.currentPage`
- Customize via `pagerSnapDistance` and `flingBehavior`

---

## Compose Interop & Migration Strategy Reference

Source: Context7 `/websites/developer_android_develop_ui_compose_migrate`

### Three-Step Migration Strategy
1. **Build new screens using Compose** — New features or screens are written entirely in Compose from the start.
2. **Identify and extract reusable elements** — Extract common UI components into a shared library of composables.
3. **Gradually replace existing features one screen at a time** — Convert existing Feature/Fragment screens incrementally.

### ComposeView in Fragment (Transitional)
```kotlin
class ExampleFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { MaterialTheme { Text("Hello Compose!") } }
        }
    }
}
```

**ViewCompositionStrategy options:**
- `DisposeOnViewTreeLifecycleDestroyed` — **Recommended for Fragments.** Ties composition lifetime to view tree's LifecycleOwner.
- `DisposeOnLifecycleDestroyed` — Legacy option.
- `DisposeOnDetachedFromWindow` — Legacy option for standard View hierarchies.

### AndroidView (Views in Compose)
```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context -> MyView(context).apply { /* setup */ } },
    update = { view -> view.selectedItem = selectedItem }
)
```

### AbstractComposeView (Custom Compose-wrapper View)
Exposes a composable as a reusable Android View that can be placed in XML layouts.

### Migration Scenarios
| Existing | Compose Equivalent |
|----------|-------------------|
| `LinearLayoutManager` (vertical) | `LazyColumn` |
| `LinearLayoutManager` (horizontal) | `LazyRow` |
| `GridLayoutManager` | `LazyVerticalGrid` / `LazyHorizontalGrid` |
| `StaggeredGridLayoutManager` | `LazyVerticalStaggeredGrid` / `LazyHorizontalStaggeredGrid` |
| `ViewPager2` | `HorizontalPager` |
| `TabLayout` | `PrimaryTabRow` + `Tab` |
| `SwipeRefreshLayout` | `PullToRefreshBox` ( accompanist ) or `Modifier.pullRefresh` |
| `RecyclerView.Adapter` | `LazyColumn`/`LazyVerticalGrid` items |

### Fragment Navigation → Navigation Compose
1. Extract screen content from Fragment's `setContent { }` into standalone `@Composable`
2. Register as composable destination in `NavHost`
3. Remove Fragment once all screens are migrated

### Recommended End State
- **Single-Activity structure** using Navigation Compose
- Move away from Fragments in favor of composable destinations
- `ComposeView` in Fragments is a transitional bridge — remove once all screens are fully Compose

---

## Material 3 Theming Reference

Source: Context7 `/websites/developer_android_develop_ui_compose`

### Three Primary Subsystems
| Subsystem | Type | Access | Purpose |
|-----------|------|--------|---------|
| **Color Scheme** | `ColorScheme` | `MaterialTheme.colorScheme` | Theme-defined colors |
| **Typography** | `Typography` | `MaterialTheme.typography` | Text styles |
| **Shapes** | `Shapes` | `MaterialTheme.shapes` | Component corner geometry |

### Color Schemes
- Use `lightColorScheme()` / `darkColorScheme()` with full 48-role system
- Dynamic color (Android 12+): `dynamicLightColorScheme(context)` / `dynamicDarkColorScheme(context)`
- Gate dynamic color on `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`
- For crossfading between light/dark: `animateColorScheme(target = if (darkTheme) DarkColorScheme else LightColorScheme)`
- For individual color transitions: `animateColorAsState`

### Typography
20 styles: `displayLarge/Medium/Small`, `headlineLarge/Medium/Small`, `titleLarge/Medium/Small`, `bodyLarge/Medium/Small`, `labelLarge/Medium/Small`. Use `EmphasizedTypography` for Material 3 Expressive.

### Shapes
Five buckets: `extraSmall` (4dp), `small` (8dp), `medium` (12dp), `large` (16dp), `extraLarge` (24dp). Use `RoundedCornerShape`, `CutCornerShape`, `CircleShape`.

### Production Theme Structure
```kotlin
@Composable
fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = when {
        dynamicColor && darkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor && !darkTheme -> dynamicLightColorScheme(LocalContext.current)
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    MaterialTheme(colorScheme = colorScheme, typography = typography, shapes = shapes, content = content)
}
```

---

## Phase 1: PagerScreen Migration

### Task 3: Analyze PagerFragment

**Read:** `PagerFragment.kt` (690 lines), `fragment_pager.xml`, `PagerAdapter.kt` (106 lines), `item_home.xml`, `HomeFragment.kt`, `HailData.tags`, `AppMetaCache`, `AppManager`, `AppActions`

**Document:** all callbacks, state, and data sources the PagerScreen needs.

**Actual complexity findings (from code analysis):**
- `PagerFragment` extends `MainFragment` and couples tightly to `HomeFragment` via `parentFragment` casts (multiselect, selectedList, tabs, pager adapter)
- State: `query`, `multiselect`, `selectedList` (mutable), `_menu`
- Data source: `AppMetaCache.revision` collected via `repeatOnLifecycle`
- Filtering: tag-based (`tag?.second`) + search (`query`) with `NineKeySearch`, `FuzzySearch`, `PinyinSearch`
- Actions: launch, freeze/unfreeze (single + batch), tag management (add/rename/remove/tri-state), import/export clipboard, deferred tasks, pin/unpin, whitelist, remove from home
- Menu: `menu_home.xml` with search (`SearchView`), multiselect button, freeze/unfreeze all, import/export actions
- Dialogs: tag dialog (`DialogInputBinding`), tri-state tag dialog (embeds ComposeView), action picker dialog
- Back handling: `OnBackPressedCallback` — deselects in multiselect mode
- Icon loading: `AppIconCache.loadIconBitmapAsync(context, info, userId, imageView)` — ImageView-bound

### Task 4: Create HomeAppItem composable (item_home.xml equivalent)

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/HomeAppItem.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/HomeAppItemTest.kt`

**Interfaces:**
- Consumes: `AppInfo` (`com.aistra.hail.app.AppInfo`), `AppIconCache`
- Produces: `HomeAppItem(appInfo, isSelected, multiselectMode, onClick, onLongClick, modifier)`

Key mappings from `item_home.xml`:
- `ImageView` (app icon, 64dp) → custom `AppIcon` composable using `AppIconCache.getOrLoadBitmap()` + `Image(bitmap.asImageBitmap())`
- App name `TextView` → `Text` with `overflow = TextOverflow.Ellipsis`, `maxLines = 1`
- Frozen state: `❄️` prefix + `grayscaleIcon` color filter
- Whitelisted: `🔒` suffix
- Selected state: `color = MaterialTheme.colorScheme.primary`
- Not found state: `color = MaterialTheme.colorScheme.error`
- Default: `MaterialTheme.typography.bodyMedium`, `fontSize = 14.sp`
- Background: `Modifier.clickable` with `selectableItemBackgroundBorderless`

### Task 5: Create PagerScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerScreen.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerViewModel.kt` (extends `AndroidViewModel`)
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/PagerScreenTest.kt`

**Interfaces:**
- Consumes: `HomeAppItem`, `HailData.tags`, `AppMetaCache.revision`, `AppManager`, `AppActions`
- Produces: `PagerScreen(tagId: Long, onFabClick, modifier)`

Key mappings from `fragment_pager.xml` + `PagerFragment.kt`:
- `SwipeRefreshLayout` → `PullToRefreshBox` (from `foundation-pager` or custom `PullRefresh` if not in BOM)
- `RecyclerView` (grid, `HailData.iconColumns` cols) → `LazyVerticalGrid(columns = Fixed(HailData.iconColumns))`
- Search (`SearchView` in menu) → `SearchBar` in `LargeFlexibleTopAppBar` or inline `TextField`
- Context menu (long-press) → `DropdownMenu` anchored on long-click
- `OnBackPressedCallback` → `BackHandler` with `multiselect` check
- Multiselect mode → inline checkbox UI + `TopAppBar` title update
- Tag tabs (`TabLayout`) → `ScrollableTabRow` or `PrimaryTabRow` (Expressive)

**PagerViewModel:**
```kotlin
class PagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    var query by mutableStateOf("")
    var multiselect by mutableStateOf(false)
    val selectedList = mutableStateListOf<AppInfo>()

    init {
        viewModelScope.launch {
            AppMetaCache.revision.collect { loadApps() }
        }
    }

    fun loadApps(tagId: Long) { /* filter + search logic from PagerFragment */ }
    fun setListFrozen(frozen: Boolean) { /* AppActions.freezePackages + invalidate */ }
    // ... other action handlers
}
```

### Task 6: Validate PagerScreen on existing fragment

Wire `PagerScreen` into the existing `PagerFragment` via `ComposeView` temporarily:
```kotlin
binding.root.addView(ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        HailTheme(state = HailThemeState()) {
            PagerScreen(tagId = tag?.second ?: 0, onFabClick = { /* ... */ })
        }
    }
})
```
Hide old RecyclerView. Validate visual parity. Run tests.

### Task 7: Delete PagerFragment + XML + Adapter

**Files to delete:**
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`
- `app/src/main/res/layout/fragment_pager.xml`
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt`
- `app/src/main/res/layout/item_home.xml`

Wire `PagerScreen` directly into the NavHost in Phase 5.

---

## Phase 2: AppsFragment Migration

### Task 8: Analyze AppsFragment

Read: `AppsFragment.kt`, `fragment_apps.xml`, `AppsAdapter.kt`, `item_apps.xml`, `AppsViewModel.kt`, `HRecyclerView.kt`

### Task 9: Create AppListItem composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/apps/AppListItem.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/apps/AppListItemTest.kt`

Key mappings from item_apps.xml:
- `ImageView` (app icon) → `AsyncImage`
- App name/desc `TextView` → `Text`
- `MaterialCheckBox` → `Checkbox`
- Context menu → `DropdownMenu`

### Task 10: Create AppsScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/apps/AppsScreen.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/apps/AppsScreenTest.kt`

Key mappings from fragment_apps.xml:
- `HRecyclerView` + `GridLayoutManager` → `LazyVerticalGrid(GridCells.Adaptive(128.dp))`
- `SwipeRefreshLayout` → `PullToRefreshBox`
- `SearchView` (menu) → `SearchBar`
- Context menu → `DropdownMenu`

Uses existing `AppsViewModel` directly via `viewModel()` (no new ViewModel needed).

---

## Phase 3: ActionsFragment Migration

### Task 11: Analyze ActionsFragment

Read: `ActionsFragment.kt`, `fragment_actions.xml`, `ActionsAdapter.kt`, `item_action.xml`, `AppPickerAdapter.kt`, `item_action_picker.xml`, `dialog_input.xml`

### Task 12: Create ActionItem and ActionEditor composables

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionItem.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionEditorDialog.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/AppPickerDialog.kt`

### Task 13: Create ActionsScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionsScreen.kt`

Key mappings:
- `RecyclerView` → `LazyColumn`
- Programmatically built `AlertDialog` → Compose `AlertDialog`
- `EditText` in dialog → `TextField`

---

## Phase 4: HomeFragment Migration

### Task 14: Create HomeScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/HomeScreen.kt`

Key mappings from fragment_home.xml (TabLayout + ViewPager2):
- `TabLayout` + `ViewPager2` + `HomeAdapter` → `PrimaryTabRow` + `HorizontalPager` from `androidx.compose.foundation.pager`

### Task 15: Delete HomeFragment + XML + Adapter

Delete: `HomeFragment.kt`, `fragment_home.xml`, `HomeAdapter.kt`

---

## Phase 5: MainActivity + NavHost integration

### Task 16: Wire NavHost into MainActivity

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/HailRootScreen.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailScaffold.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailTopAppBar.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailFab.kt`

MainActivity becomes:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HailTheme { HailRootScreen() }
        }
    }
}
```

HailRootScreen provides:
- TopAppBar with title (per destination)
- BottomNavigation (existing `ExpressiveNavigationBar`)
- FAB (per destination: Home→add, Actions→create action, else hidden)
- Handles NavOptions for back stack restoration

### Task 17: Remove XML layouts + old deps

Delete:
- `activity_main.xml`, `activity_main.xml` (land), `app_bar_main.xml` (both variants), `content_main.xml`
- `bottomNav` and `navRail` ComposeView references in XML (gone since no XML)
- Remove `android:theme="@style/Theme.Hail"` → `@style/Theme.Hail` stays (window animations)
- Remove dependencies: `appcompat`, `constraintlayout`, `swiperefreshlayout`, `insetter`, Material XML

### Task 18: Update manifest

**Files:** Modify `app/src/main/AndroidManifest.xml`
- Remove `android:theme="@style/Theme.Hail.AppBarOverlay"` references (gone)
- Add `android:windowSoftInputMode="adjustResize"` to `MainActivity`
- Remove `tools:context=".ui.main.MainActivity"` if not needed

---

## Phase 6: Cleanup + Validation

### Task 19: Remove legacy adapters and view utilities

Delete any remaining adapters or view utilities that are no longer used:
- `PagerAdapter.kt`
- `HomeAdapter.kt`
- `AppsAdapter.kt`
- `ActionsAdapter.kt`
- `AppPickerAdapter.kt`
- `HRecyclerView.kt`
- `InsetsExtensions.kt` (no longer needed if no XML views)

### Task 20: Final validation

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:connectedDebugAndroidTest`
- `./gradlew :app:assembleDebug`
- Visual parity check on device for all migrated screens

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Interfaces:**
- Consumes: nothing
- Produces: `navigation3-runtime`, `navigation3-compose`, `kotlinx-serialization-json` available

- [ ] **Step 1: Add versions to `libs.versions.toml`**

```toml
[versions]
navigation3 = "1.2.0-alpha05"

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-compose = { module = "androidx.navigation3:navigation3-compose", version.ref = "navigation3" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version = "1.8.1" }
```

- [ ] **Step 2: Add to `app/build.gradle.kts`**

```kotlin
implementation(libs.androidx.navigation3.runtime)
implementation(libs.androidx.navigation3.compose)
implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: Sync**

Run: `./gradlew :app:dependencies --configuration debugRuntimeClasspath | grep navigation3`

---

### Task 2: Create Navigation 3 scaffolding

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/nav/Routes.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/nav/HailNavHost.kt`

**Interfaces:**
- Consumes: Navigation 3 APIs (Task 1)
- Produces: `HailNavHost()`, route objects

**Routes.kt:**
```kotlin
package com.aistra.hail.ui.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute : NavKey
@Serializable data object AppsRoute : NavKey
@Serializable data object ActionsRoute : NavKey
@Serializable data object SettingsRoute : NavKey
@Serializable data object AboutRoute : NavKey
```

**HailNavHost.kt:**
```kotlin
package com.aistra.hail.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.aistra.hail.ui.theme.HailTheme
import com.aistra.hail.ui.theme.HailThemeState

@Composable
fun HailNavHost(
    modifier: Modifier = Modifier,
    themeState: HailThemeState = HailThemeState(),
) {
    HailTheme(state = themeState) {
        val backStack = rememberNavBackStack(HomeRoute)
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> { Text("Home") }
                entry<AppsRoute> { Text("Apps") }
                entry<ActionsRoute> { Text("Actions") }
                entry<SettingsRoute> { Text("Settings") }
                entry<AboutRoute> { Text("About") }
            }
        )
    }
}
```

**Verified API (navigation3 1.2.0-alpha05 + nav3-recipes source + bytecode inspection):**
- `NavDisplay` is in `androidx.navigation3.ui` (NOT `androidx.navigation3.compose`)
- `entryProvider` is in `androidx.navigation3.runtime`
- `NavDisplay` accepts named params: `backStack`, `onBack`, `entryProvider`
- `rememberNavBackStack(vararg keys: NavKey)` creates persistent back stack
- `NavBackStack<T>` implements `List<T>` with `add(T)`, `removeLastOrNull()`
- `HailNavState` wrapper eliminated (over-engineered, per simplifier review — `onNavigate` was dead code, `onBack` was trivial passthrough)
- Per-tab back stacks will be added when wiring bottom nav bar (Phase 5), following `NavigationState` + `Navigator` pattern from multiplestacks recipe
- `NavBackStack<T>` implements `List<T>` with `add(T)`, `removeLastOrNull()`

**HailNavHost.kt:**
```kotlin
package com.aistra.hail.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.aistra.hail.ui.theme.HailTheme
import com.aistra.hail.ui.theme.HailThemeState

@Composable
fun HailNavHost(
    modifier: Modifier = Modifier,
    themeState: HailThemeState = HailThemeState(),
) {
    HailTheme(state = themeState) {
        val backStack = rememberNavBackStack(HomeRoute)
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> { Text("Home") }
                entry<AppsRoute> { Text("Apps") }
                entry<ActionsRoute> { Text("Actions") }
                entry<SettingsRoute> { Text("Settings") }
                entry<AboutRoute> { Text("About") }
            }
        )
    }
}
```

**Key corrections from actual API (verified against navigation3 1.2.0-alpha05 + nav3-recipes source):**
- `NavDisplay` is in `androidx.navigation3.ui` (NOT `androidx.navigation3.compose`)
- `NavDisplay` accepts `backStack`, `onBack`, `entryProvider` named parameters
- `HailTheme` requires `state: HailThemeState` parameter
- `rememberNavBackStack(vararg keys: NavKey)` takes vararg keys
- `NavBackStack<T>` implements `List<T>` with `add(T)`, `remove(T)` methods

---

## Phase 1: PagerScreen Migration

### Task 3: Analyze PagerFragment

**Read:** `PagerFragment.kt` (690 lines), `fragment_pager.xml`, `PagerAdapter.kt` (106 lines), `item_home.xml`, `HomeFragment.kt`, `HailData.tags`, `AppMetaCache`, `AppManager`, `AppActions`

**Document:** all callbacks, state, and data sources the PagerScreen needs.

**Actual complexity findings (from code analysis):**
- `PagerFragment` extends `MainFragment` and couples tightly to `HomeFragment` via `parentFragment` casts (multiselect, selectedList, tabs, pager adapter)
- State: `query`, `multiselect`, `selectedList` (mutable), `_menu`
- Data source: `AppMetaCache.revision` collected via `repeatOnLifecycle`
- Filtering: tag-based (`tag?.second`) + search (`query`) with `NineKeySearch`, `FuzzySearch`, `PinyinSearch`
- Actions: launch, freeze/unfreeze (single + batch), tag management (add/rename/remove/tri-state), import/export clipboard, deferred tasks, pin/unpin, whitelist, remove from home
- Menu: `menu_home.xml` with search (`SearchView`), multiselect button, freeze/unfreeze all, import/export actions
- Dialogs: tag dialog (`DialogInputBinding`), tri-state tag dialog (embeds ComposeView), action picker dialog
- Back handling: `OnBackPressedCallback` — deselects in multiselect mode
- Icon loading: `AppIconCache.loadIconBitmapAsync(context, info, userId, imageView)` — ImageView-bound

### Task 4: Create HomeAppItem composable (item_home.xml equivalent)

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/HomeAppItem.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/HomeAppItemTest.kt`

**Interfaces:**
- Consumes: `AppInfo` (`com.aistra.hail.app.AppInfo`), `AppIconCache`
- Produces: `HomeAppItem(appInfo, isSelected, multiselectMode, onClick, onLongClick, modifier)`

Key mappings from `item_home.xml`:
- `ImageView` (app icon, 64dp) → custom `AppIcon` composable using `AppIconCache.getOrLoadBitmap()` + `Image(bitmap.asImageBitmap())`
- App name `TextView` → `Text` with `overflow = TextOverflow.Ellipsis`, `maxLines = 1`
- Frozen state: `❄️` prefix + `grayscaleIcon` color filter
- Whitelisted: `🔒` suffix
- Selected state: `color = MaterialTheme.colorScheme.primary`
- Not found state: `color = MaterialTheme.colorScheme.error`
- Default: `MaterialTheme.typography.bodyMedium`, `fontSize = 14.sp`
- Background: `Modifier.clickable` with `selectableItemBackgroundBorderless`

### Task 5: Create PagerScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerScreen.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerViewModel.kt` (extends `AndroidViewModel`)
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/PagerScreenTest.kt`

**Interfaces:**
- Consumes: `HomeAppItem`, `HailData.tags`, `AppMetaCache.revision`, `AppManager`, `AppActions`
- Produces: `PagerScreen(tagId: Long, onFabClick, modifier)`

Key mappings from `fragment_pager.xml` + `PagerFragment.kt`:
- `SwipeRefreshLayout` → `PullToRefreshBox` (from `foundation-pager` or custom `PullRefresh` if not in BOM)
- `RecyclerView` (grid, `HailData.iconColumns` cols) → `LazyVerticalGrid(columns = Fixed(HailData.iconColumns))`
- Search (`SearchView` in menu) → `SearchBar` in `LargeFlexibleTopAppBar` or inline `TextField`
- Context menu (long-press) → `DropdownMenu` anchored on long-click
- `OnBackPressedCallback` → `BackHandler` with `multiselect` check
- Multiselect mode → inline checkbox UI + `TopAppBar` title update
- Tag tabs (`TabLayout`) → `ScrollableTabRow` or `PrimaryTabRow` (Expressive)

**PagerViewModel:**
```kotlin
class PagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    var query by mutableStateOf("")
    var multiselect by mutableStateOf(false)
    val selectedList = mutableStateListOf<AppInfo>()

    init {
        viewModelScope.launch {
            AppMetaCache.revision.collect { loadApps() }
        }
    }

    fun loadApps(tagId: Long) { /* filter + search logic from PagerFragment */ }
    fun setListFrozen(frozen: Boolean) { /* AppActions.freezePackages + invalidate */ }
    // ... other action handlers
}
```

### Task 6: Validate PagerScreen on existing fragment

Wire `PagerScreen` into the existing `PagerFragment` via `ComposeView` temporarily:
```kotlin
binding.root.addView(ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent {
        HailTheme(state = HailThemeState()) {
            PagerScreen(tagId = tag?.second ?: 0, onFabClick = { /* ... */ })
        }
    }
})
```
Hide old RecyclerView. Validate visual parity. Run tests.

### Task 7: Delete PagerFragment + XML + Adapter

**Files to delete:**
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`
- `app/src/main/res/layout/fragment_pager.xml`
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt`
- `app/src/main/res/layout/item_home.xml`

Wire `PagerScreen` directly into the NavHost in Phase 5.

---

## Phase 2: AppsFragment Migration

### Task 8: Analyze AppsFragment

Read: `AppsFragment.kt`, `fragment_apps.xml`, `AppsAdapter.kt`, `item_apps.xml`, `AppsViewModel.kt`, `HRecyclerView.kt`

### Task 9: Create AppListItem composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/apps/AppListItem.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/apps/AppListItemTest.kt`

Key mappings from item_apps.xml:
- `ImageView` (app icon) → `AsyncImage`
- App name/desc `TextView` → `Text`
- `MaterialCheckBox` → `Checkbox`
- Context menu → `DropdownMenu`

### Task 10: Create AppsScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/apps/AppsScreen.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/apps/AppsScreenTest.kt`

Key mappings from fragment_apps.xml:
- `HRecyclerView` + `GridLayoutManager` → `LazyVerticalGrid(GridCells.Adaptive(128.dp))`
- `SwipeRefreshLayout` → `PullToRefreshBox`
- `SearchView` (menu) → `SearchBar`
- Context menu → `DropdownMenu`

Uses existing `AppsViewModel` directly via `viewModel()` (no new ViewModel needed).

---

## Phase 3: ActionsFragment Migration

### Task 11: Analyze ActionsFragment

Read: `ActionsFragment.kt`, `fragment_actions.xml`, `ActionsAdapter.kt`, `item_action.xml`, `AppPickerAdapter.kt`, `item_action_picker.xml`, `dialog_input.xml`

### Task 12: Create ActionItem and ActionEditor composables

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionItem.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionEditorDialog.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/AppPickerDialog.kt`

### Task 13: Create ActionsScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/actions/ActionsScreen.kt`

Key mappings:
- `RecyclerView` → `LazyColumn`
- Programmatically built `AlertDialog` → Compose `AlertDialog`
- `EditText` in dialog → `TextField`

---

## Phase 4: HomeFragment Migration

### Task 14: Create HomeScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/HomeScreen.kt`

Key mappings from fragment_home.xml (TabLayout + ViewPager2):
- `TabLayout` + `ViewPager2` + `HomeAdapter` → `PrimaryTabRow` + `HorizontalPager` from `accompanist/pager` or `androidx.compose.foundation.pager`

### Task 15: Delete HomeFragment + XML + Adapter

Delete: `HomeFragment.kt`, `fragment_home.xml`, `HomeAdapter.kt`

---

## Phase 5: MainActivity + NavHost integration

### Task 16: Wire NavHost into MainActivity

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/HailRootScreen.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailScaffold.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailTopAppBar.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/shared/HailFab.kt`

MainActivity becomes:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HailTheme { HailRootScreen() }
        }
    }
}
```

HailRootScreen provides:
- TopAppBar with title (per destination)
- BottomNavigation (existing `ExpressiveNavigationBar`)
- FAB (per destination: Home→add, Actions→create action, else hidden)
- Handles NavOptions for back stack restoration

### Task 17: Remove XML layouts + old deps

Delete:
- `activity_main.xml`, `activity_main.xml` (land), `app_bar_main.xml` (both variants), `content_main.xml`
- `bottomNav` and `navRail` ComposeView references in XML (gone since no XML)
- Remove `android:theme="@style/Theme.Hail"` → `@style/Theme.Hail` stays (window animations)
- Remove dependencies: `appcompat`, `constraintlayout`, `swiperefreshlayout`, `insetter`, Material XML

### Task 18: Update manifest

**Files:** Modify `app/src/main/AndroidManifest.xml`
- Remove `android:theme="@style/Theme.Hail.AppBarOverlay"` references (gone)
- Add `android:windowSoftInputMode="adjustResize"` to `MainActivity`
- Remove `tools:context=".ui.main.MainActivity"` if not needed

---

## Phase 6: Cleanup + Validation

### Task 19: Remove legacy adapters and view utilities

Delete any remaining adapters or view utilities that are no longer used:
- `PagerAdapter.kt`
- `HomeAdapter.kt`
- `AppsAdapter.kt`
- `ActionsAdapter.kt`
- `AppPickerAdapter.kt`
- `HRecyclerView.kt`
- `InsetsExtensions.kt` (no longer needed if no XML views)

### Task 20: Final validation

- `./gradlew :app:compileDebugKotlin`
- `./gradlew :app:testDebugUnitTest`
- `./gradlew :app:connectedDebugAndroidTest`
- `./gradlew :app:assembleDebug`
- Visual parity check on device for all migrated screens
