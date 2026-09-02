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

Source: Android Developer Docs (Context7 `/websites/developer_android` + `/websites/developer_android_develop_ui_compose`), verified against Hail's Compose BOM `2026.08.00` and Material 3 `1.5.0-alpha27`.

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

### Task 1: Add Navigation 3 + serialization dependencies

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
