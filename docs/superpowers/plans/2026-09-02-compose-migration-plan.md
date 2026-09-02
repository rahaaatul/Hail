# Hail: XML-to-Compose Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate Hail's UI layer from XML Views + Fragments to Jetpack Compose, ending with a single `ComponentActivity` hosting Navigation 3 `NavDisplay`.

**Architecture:** Per-screen migration following Google's 10-step XML-to-Compose process. Create new Compose files alongside old Fragment/XML, validate, then delete old files. One git branch per screen. Data layer (Room, HailData, AppManager, AppMetaCache, Shizuku, Xposed) remains untouched.

**Tech Stack:** Jetpack Compose, Navigation 3 (1.2.0-alpha05), Material 3 Expressive (1.5.0-alpha27), material-kolor 5.0.1, Kotlin 2.4.10

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

---

## Phase 0: Navigation 3 Setup

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

Read: `PagerFragment.kt`, `fragment_pager.xml`, `PagerAdapter.kt`, `item_home.xml`, `AppsFragment.kt` (for shared state patterns)

Document: all callbacks, state, and data sources the PagerScreen needs. Output summary as commit message.

### Task 4: Create HomeAppItem composable (item_home.xml equivalent)

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/HomeAppItem.kt`
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/HomeAppItemTest.kt`

**Interfaces:**
- Consumes: `AppInfo` (`com.aistra.hail.app.AppInfo`), `AppIconCache`, `Icons.Filled.Home`
- Produces: `HomeAppItem(appInfo, isSelected, multiselectMode, onClick, onLongClick, modifier)`

Key mappings from item_home.xml:
- `ImageView` (app icon) → `AsyncImage` (Coil) or fallback `Icon`
- `MaterialCheckBox` (multiselect) → `Checkbox`
- App name `TextView` → `Text`
- Frozen state: `AppInfo.state` + `AppInfo.isInstalled`
- `isSelectable` → `Modifier.clickable(onLongClick)`

### Task 5: Create PagerScreen composable

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerScreen.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/screens/home/PagerViewModel.kt` (extends `AndroidViewModel`)
- Create: `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/home/PagerScreenTest.kt`

**Interfaces:**
- Consumes: `HomeAppItem` (Task 4), `TriStateTagList` (existing Compose in PagerFragment), `AppsViewModel` flow
- Produces: `PagerScreen(tagId: Long, onAppClick, onAppLongClick, onFabClick, modifier)`

Key mappings from fragment_pager.xml:
- `RecyclerView` (grid) → `LazyVerticalGrid(GridCells.FixedSpan(HailData.iconColumns))`
- `SwipeRefreshLayout` → `PullToRefreshBox`
- `SearchView` (menu) → `SearchBar` or `TextField` in top bar
- Context menu → `DropdownMenu` anchored on long-click
- `OnBackPressedCallback` → `BackHandler`
- Multiselect action mode → inline `Checkbox` selection + action bar

PagerViewModel wraps existing `AppsViewModel` logic:
```kotlin
class PagerViewModel(application: Application) : AndroidViewModel(application) {
    // Exposes Flow<List<AppInfo>> filtered by tag
    val appsForTag = flow { ... }
    // Reuses existing AppMetaCache, HPackages, filtering logic
}
```

### Task 6: Validate PagerScreen on existing fragment

Wire `PagerScreen` into the existing `PagerFragment` via `ComposeView` temporarily:
```kotlin
binding.root.addView(ComposeView(requireContext()).apply {
    setContent {
        HailTheme(state = themeState) {
            PagerScreen(tagId = tagId, onAppClick = { ... }, ...)
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
