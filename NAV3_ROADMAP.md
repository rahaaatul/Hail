# Navigation 3 Migration Roadmap

> **This document is the single source of truth for migrating Hail from Navigation 2 (Fragment-based) to Navigation 3 (Compose-based).**
> 
> **Read this entire document before starting any migration work. An AI agent or developer must verify all green-light conditions are met before proceeding.**

---

## 1. Current State Assessment

### 1.1 Navigation Stack
| Component | Current Implementation |
|-----------|------------------------|
| **Activity** | `MainActivity` extends `AppCompatActivity` |
| **Host** | `NavHostFragment` in `activity_main.xml` |
| **Destinations** | 5 Fragments: `HomeFragment`, `ActionsFragment`, `AppsFragment`, `SettingsFragment`, `AboutFragment` |
| **Navigation Graph** | `res/navigation/mobile_navigation.xml` (Navigation 2 XML) |
| **Bottom Nav / Nav Rail** | Material `BottomNavigationView` / `NavigationRailView` wired via `setupWithNavController` |
| **Dependencies** | `navigation-fragment-ktx` 2.9.8 + `navigation-ui-ktx` 2.9.8 |
| **Target SDK** | 36 |
| **compileSdk** | 37 |
| **minSdk** | 23 |

### 1.2 Compose Usage Today
- **SettingsFragment** embeds Compose via `ComposeView` (uses `me.zhanghai.compose.preference`)
- **AboutFragment** embeds Compose via `ComposeView` (Material 3)
- **PagerFragment** embeds Compose via `ComposeView` (tri-state tag dialog)
- **HomeFragment**, **AppsFragment**, **ActionsFragment** are pure View-based
- `buildFeatures.compose = true` is already enabled
- Compose BOM is `2026.08.00`

### 1.3 What Must Be Migrated
1. **MainActivity** — Replace `AppCompatActivity` + `NavHostFragment` with `ComponentActivity` + `NavDisplay`
2. **All 5 Fragments** — Convert to Compose composables (heavy lifting)
3. **Navigation Graph** — Delete XML, replace with `entryProvider` DSL
4. **Bottom Nav / Nav Rail** — Replace `setupWithNavController` with Nav3-aware state-driven selection
5. **All Navigation calls** — Replace `findNavController().navigate()` with back-stack `add()`/`removeLastOrNull()`
6. **ViewModel wiring** — Replace `by viewModels()` in Fragments with `viewModel()` in composables (or `lifecycle-viewmodel-navigation3`)
7. **Activity Result APIs** — Replace Fragment `registerForActivityResult` with Compose `rememberLauncherForActivityResult`
8. **Menus / Options Menus** — Replace `MenuProvider`/`onCreateOptionsMenu` with Compose `TopAppBar` actions
9. **Context menus / long-press** — Replace `registerForContextMenu` with Compose `DropdownMenu` or custom dialog
10. **Biometric prompt** — Move from `MainActivity.onCreate` to Compose-side invocation
11. **API Activity (`ApiActivity`)** — Keep as-is (it's a standalone `ComponentActivity` that uses deep-link intents, NOT part of the NavGraph)

---

## 2. Prerequisites (Must ALL be green before starting)

### 2.1 Hard Blockers — Must Resolve First

| # | Blocker | Why | Status |
|---|---------|-----|--------|
| P1 | **All destinations must become composables** | Nav3 is Compose-only. There is no Fragment interop. | ❌ Not started |
| P2 | **Type-safe routes** | Routes must implement `NavKey` (`@Serializable` data classes). String routes are not supported by `rememberNavBackStack`. | ❌ Not started |
| P3 | **Atomic migration** | Navigation 3 does NOT support running Nav2 + Nav3 side-by-side. You delete `NavController` and replace with `NavDisplay` in a single change. | ❌ Not started |
| P4 | **Deep links** | Nav3 has **no built-in deep link support**. If the app handles `hail://` URIs or notification intents via the NavGraph, you must implement a manual intent parser and push routes onto the back stack yourself. | ❌ Not started |
| P5 | **`compileSdk >= 36`** | Required by Nav3. | ✅ Already 37 |

### 2.2 Strong Recommendations

| # | Recommendation | Why | Status |
|---|----------------|-----|--------|
| R1 | **Test coverage for navigation** | Migration resets back-stack behavior. Tests catch regressions. | Unknown |
| R2 | **Do NOT migrate `ApiActivity`** | It handles intents, not in-app navigation. Keep it as a `ComponentActivity` launched via explicit intent. | N/A |
| R3 | **Budget 1–2 weeks for full migration** | Real-world reports: ~40 screens took ~1 week for a team familiar with Compose. This app has 5 main screens but heavy Fragment-to-Compose conversion. | N/A |

---

## 3. Migration Steps (Detailed)

### Step 0: Prepare Dependencies
**Add Nav3 artifacts, remove Nav2 artifacts.**

```toml
# gradle/libs.versions.toml
[versions]
nav3 = "1.1.6"  # stable as of 2026-08
lifecycleViewmodelNav3 = "2.11.0"  # stable — DO NOT use alpha versions

[libraries]
androidx-navigation3-runtime = { module = "androidx.navigation3:navigation3-runtime", version.ref = "nav3" }
androidx-navigation3-ui = { module = "androidx.navigation3:navigation3-ui", version.ref = "nav3" }
androidx-lifecycle-viewmodel-navigation3 = { module = "androidx.lifecycle:lifecycle-viewmodel-navigation3", version.ref = "lifecycleViewmodelNav3" }
```

```kotlin
// app/build.gradle.kts — REMOVE
implementation(libs.androidx.navigation.fragment.ktx)
implementation(libs.androidx.navigation.ui.ktx)

// app/build.gradle.kts — ADD
implementation(libs.androidx.navigation3.runtime)
implementation(libs.androidx.navigation3.ui)
implementation(libs.androidx.lifecycle.viewmodel.navigation3)
```

⚠️ **Pitfall**: The `get-started` page sample pins `lifecycle-viewmodel-navigation3` to `2.12.0-alpha01`. Use `2.11.0` (stable). Shipping an alpha lifecycle dependency to production is a silent trap.

### Step 1: Define Type-Safe Routes (NavKey)
Every route must be a `@Serializable` data class implementing `NavKey`.

```kotlin
@Serializable
data object HomeRoute : NavKey

@Serializable
data object ActionsRoute : NavKey

@Serializable
data object AppsRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data object AboutRoute : NavKey
```

> Note: This app has no route arguments today (no `nav_apps/{id}` patterns). If arguments are added later, they become properties on the data class.

### Step 2: Create NavigationState
Create `NavigationState.kt` — holds top-level routes and per-tab back stacks.

```kotlin
// Key fields:
// - topLevelRoute: State<NavKey>  — the currently selected tab
// - backStacks: Map<NavKey, SnapshotStateList<NavKey>> — per-tab back stacks
// - currentBackStack: SnapshotStateList<NavKey> — active stack
```

Use `rememberSerializable` for `topLevelRoute` (NOT `rememberSaveable`). The guide explicitly warns AI agents about this — `rememberSaveable` silently corrupts here.

### Step 3: Create Navigator
Create `Navigator.kt` — replaces all `NavController` navigation methods.

```kotlin
// Required methods to implement:
fun navigate(route: NavKey)
fun goBack()
// Optional but needed:
fun popUpTo(route: NavKey, inclusive: Boolean = false)  // for logout flows
```

⚠️ **Pitfall**: `NavDisplay` requires a non-empty back stack (`require(backStack.isNotEmpty())`). If your logout flow clears the entire stack before pushing login, you crash. Ensure the start destination is always present.

### Step 4: Replace All Fragment Navigation Calls

| Old (Fragment) | New (Composable) |
|----------------|------------------|
| `findNavController().navigate(R.id.nav_apps)` | `navigator.navigate(AppsRoute)` |
| `findNavController().navigateUp()` | `navigator.goBack()` |
| `navController.currentBackStackEntry?.arguments` | Direct property on route data class: `route.appId` |
| `navController.addOnDestinationChangedListener` | Derive UI state from `navigationState.topLevelRoute` |

**Files to update:**
- `MainActivity.kt` lines 77, 114, 116–143 — all `NavController` references
- `SettingsFragment.kt` line 642 — `findNavController().navigate(R.id.nav_about)`
- `PagerFragment.kt` line 32 — import removal

### Step 5: Convert Fragments to Composables

Convert each Fragment into a top-level `@Composable` entry. This is the bulk of the work.

#### 5.1 HomeFragment → `HomeScreen`
- **Layout**: `fragment_home.xml` → Jetpack Compose `TabRow` + `HorizontalPager` (Accompanist or official Pager)
- **Complexity**: Medium. Uses `TabLayoutMediator` with ViewPager2. Replace with `Accompanist Pager` or `androidx.compose.foundation.pager`.
- **State**: `multiselect`, `selectedList` → `remember { mutableStateListOf() }`, `remember { mutableStateOf(false) }`
- **Nested fragment**: `PagerFragment` is a child fragment — its content becomes a page composable inside `HomeScreen`

#### 5.2 PagerFragment → `HomePagerScreen`
- **Layout**: `fragment_pager.xml` → Compose `LazyVerticalGrid`
- **Complexity**: High. Heavy RecyclerView + GridLayoutManager + MenuProvider + OnBackPressedCallback + context menus
- **State**: `query`, `multiselect`, `_menu` → Compose equivalents
- **Context menus**: Replace `registerForContextMenu` with `DropdownMenu` on long-press
- **OnBackPressedCallback**: Replace with `BackHandler` composable
- **SearchView**: Replace with `OutlinedTextField` in TopAppBar

#### 5.3 AppsFragment → `AppsScreen`
- **Layout**: `fragment_apps.xml` → Compose `LazyVerticalGrid` + `PullRefresh`
- **Complexity**: High. `GridLayoutManager` + `MenuProvider` + `registerForActivityResult` + context menus + `Snackbar`
- **Activity results**: `exportApk` → `rememberLauncherForActivityResult(CreateDocument(...))`
- **Menus**: Replace `SearchView` + overflow menu with `TopAppBar` actions + `DropdownMenu`
- **Context menus**: Same as PagerFragment

#### 5.4 ActionsFragment → `ActionsScreen`
- **Layout**: `fragment_actions.xml` → Compose `LazyColumn`
- **Complexity**: Medium. `LinearLayoutManager` + dialog-heavy UI (`showEditor`, `showAppPicker`)
- **Dialogs**: `MaterialAlertDialogBuilder` → Compose `AlertDialog`
- **Custom dialogs**: `showEditor` (LinearLayout with buttons) → Compose `AlertDialog` with custom content

#### 5.5 SettingsFragment → `SettingsScreen`
- **Layout**: Already Compose via `ComposeView`! Just extract the `@Composable` function.
- **Complexity**: Low. The composable content already exists; just move it out of the Fragment wrapper.
- **Permission requests**: `requestPermissionLauncher` → `rememberLauncherForActivityResult(RequestPermission())`
- **Navigation**: `findNavController().navigate(R.id.nav_about)` → `navigator.navigate(AboutRoute)`

#### 5.6 AboutFragment → `AboutScreen`
- **Layout**: Already Compose via `ComposeView`. Extract the `@Composable` function.
- **Complexity**: Very low.
- **Dialogs**: `MaterialAlertDialogBuilder` for donate → Compose `AlertDialog`

### Step 6: Create entryProvider
Define the `entryProvider` DSL that maps routes to composable entries.

```kotlin
val entryProvider = entryProvider {
    entry<HomeRoute> { HomeScreen() }
    entry<ActionsRoute> { ActionsScreen() }
    entry<AppsRoute> { AppsScreen() }
    entry<SettingsRoute> { SettingsScreen() }
    entry<AboutRoute> { AboutScreen() }
}
```

> Note: There are no nested `<navigation>` graphs in the current XML, so no nested back-stack logic is needed. Each top-level route has its own back stack.

### Step 7: Replace NavHost with NavDisplay
Replace the `NavHostFragment` in `activity_main.xml` with a Compose host.

**New `MainActivity` structure:**
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { MainScreen() } }
    }
}

@Composable
private fun MainScreen() {
    val navigationState = remember { NavigationState(/* initial routes */) }
    val navigator = remember { Navigator(navigationState) }
    val entryProvider = remember { /* entries from Step 6 */ }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        sceneStrategies = listOf(DialogSceneStrategy.dialog()), // if any dialogs
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberHiltViewModelStoreNavEntryDecorator() // if using Hilt
        )
    )
}
```

**⚠️ CRITICAL: `entryDecorators` order matters.** The default `NavDisplay` includes `rememberSaveableStateHolderNavEntryDecorator()`. When you pass `entryDecorators` yourself, you **overwrite** that default. You must re-add the saveable-state decorator explicitly, and it **must come first**. Missing this silently breaks `rememberSaveable`.

### Step 8: Replace Bottom Navigation / Nav Rail
Replace `BottomNavigationView.setupWithNavController(navController)` with state-driven selection.

```kotlin
BottomNavigation(
    selectedItemId = navigationState.topLevelRoute, // or a derived index
    onItemSelected = { index ->
        navigator.navigate(topLevelRoutes[index])
    }
) {
    topLevelRoutes.forEach { route ->
        item(
            selected = route == navigationState.topLevelRoute,
            onClick = { navigator.navigate(route) },
            icon = { /* icon */ },
            label = { /* label */ }
        )
    }
}
```

### Step 9: Remove All Navigation 2 Dependencies
Delete:
- `res/navigation/mobile_navigation.xml`
- All `androidx.navigation.*` imports
- `navigation-fragment-ktx` and `navigation-ui-ktx` from `build.gradle.kts`
- `R.id.nav_home`, etc. from any remaining code
- `NavHostFragment` references
- `FragmentExtensions.kt` navigation helpers (if any)

### Step 10: Handle Deep Links Manually
**This app has deep links** (`hail://` scheme in `ApiActivity` + potentially `AndroidManifest.xml` intent filters). Nav3 has NO built-in deep link support.

- Keep `ApiActivity` as-is for external intent handling (it already works independently)
- If the app uses intent filters on `MainActivity`, create a `DeepLinkHandler` composable that parses incoming intents and pushes the appropriate route:

```kotlin
@Composable
fun DeepLinkHandler(
    intent: Intent?,
    navigator: Navigator
) {
    LaunchedEffect(intent) {
        intent?.data?.let { uri ->
            when (uri.host) {
                "launch" -> navigator.navigate(AppsRoute) // example
                // ... map hail:// URIs to routes
            }
        }
    }
}
```

---

## 4. Features NOT Covered by Official Nav3 Migration Guide

| Feature | Nav3 Status | Your App Impact |
|---------|-------------|-----------------|
| **Deep links** | ❌ Unsupported — manual implementation only | **HIGH** — app has `hail://` scheme |
| **Bottom sheets** | ⚠️ Recipe only — `BottomSheetSceneStrategy` is NOT in core library | Medium — `ApiActivity` uses `ModalBottomSheet`; keep it separate |
| **Shared ViewModels across screens** | ⚠️ No parent graph scoping | Low — app uses per-screen ViewModels (`AppsViewModel`) |
| **Returning results between screens** | ⚠️ No built-in `savedStateHandle` equivalent | Low — app doesn't use this pattern |
| **Custom destination types** | ❌ Unsupported | None |
| **More than one level of nested navigation** | ❌ Unsupported | None — current graph is flat |

### 4.1 Bottom Sheet Handling
The app's `ApiActivity` uses `ModalBottomSheet` for redirects. **This does NOT need to migrate to Nav3 scene strategy** — it's a separate activity launched via explicit intent. Keep it as-is.

### 4.2 Hilt / DI
If the app uses Hilt, the wiring changes:
- In Nav2: Hilt automatically injected navigation arguments into `SavedStateHandle`
- In Nav3: You must pass the route key to ViewModel manually
- Use `rememberHiltViewModelStoreNavEntryDecorator()` in `entryDecorators`

---

## 5. Decision Matrix — Should We Proceed?

### 🟢 GREEN LIGHT if ALL of the following are true:
1. The team has bandwidth for a **1–2 week focused migration** (not a 1-day task)
2. The app does NOT need deep link framework support (manual implementation is acceptable)
3. All screens are ready to be converted to Compose (or the team accepts the conversion cost)
4. `minSdk` can stay at 23 (it can — Nav3 supports 23)
5. `compileSdk` is 36+ (it is — 37)
6. The team accepts that this is an **atomic, all-or-nothing migration** — no incremental rollout

### 🔴 STOP / RETHINK if ANY of the following are true:
1. **Deep links are critical and must be framework-managed** — Nav3 has no answer for this yet
2. **The app heavily relies on nested graph scoped ViewModels** — this is the roughest edge in Nav3
3. **Fragments/Views are used for non-negotiable reasons** (e.g., third-party SDK views that can't be embedded in Compose)
4. **The team is mid-sprint with hard deadlines** — this will break the build for days
5. **Shared destinations across back stacks** are needed — not supported

---

## 6. Recommended Order of Work

```
Phase 0: Setup (1 day)
├── Add Nav3 dependencies
├── Define all route data classes
├── Create NavigationState + Navigator boilerplate
└── Verify project compiles (no Nav2 removed yet)

Phase 1: Easiest Screens First (2–3 days)
├── AboutFragment → AboutScreen (already Compose!)
├── SettingsFragment → SettingsScreen (already Compose!)
└── Verify bottom nav wiring works

Phase 2: Medium Screens (2–3 days)
├── ActionsFragment → ActionsScreen (dialog-heavy, but flat layout)
└── Verify all navigation actions work

Phase 3: Hard Screens (3–5 days)
├── HomeFragment + PagerFragment → HomeScreen + HomePagerScreen
│   ├── Replace ViewPager2 with Compose Pager
│   ├── Replace RecyclerView + GridLayoutManager with LazyVerticalGrid
│   ├── Replace MenuProvider with TopAppBar actions
│   ├── Replace context menus with DropdownMenu
│   └── Replace OnBackPressedCallback with BackHandler
└── AppsFragment → AppsScreen (same replacements)

Phase 4: Wiring + Polish (1–2 days)
├── Replace NavHost with NavDisplay
├── Remove ALL Nav2 dependencies
├── Implement manual deep link handler
├── Fix entryDecorators order (saveable state first!)
├── Test predictive back gestures
└── Full regression test
```

---

## 7. Checklist Before Merge

- [ ] All 5 screens render correctly as composables
- [ ] Bottom nav / nav rail selection state is correct
- [ ] Back button works on every screen (including nested back stacks per tab)
- [ ] Predictive back gesture animates correctly
- [ ] `rememberSaveable` works inside all screens (scroll positions persist)
- [ ] ViewModels are scoped correctly to entries
- [ ] All dialogs render correctly (not full-screen)
- [ ] Deep links are manually handled (or confirmed not needed)
- [ ] `ApiActivity` still works (unchanged)
- [ ] No `androidx.navigation` imports remain
- [ ] No `mobile_navigation.xml` or other Nav2 XML files remain
- [ ] No `Fragment`, `NavHostFragment`, `NavController` references in main source
- [ ] `lifecycle-viewmodel-navigation3` is at a stable version (not alpha)
- [ ] `entryDecorators` list includes `rememberSaveableStateHolderNavEntryDecorator()` FIRST

---

## 8. References

- [Official Nav3 Overview](https://developer.android.com/guide/navigation/navigation-3)
- [Official Migration Guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [Nav3 Recipes Repo](https://github.com/android/nav3-recipes)
- [Nav3 Release Notes](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Stable Announcement (Nov 2025)](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)
- [Real-World Migration Cost Analysis](https://medium.com/@sshameelakhtar/navigation-3-is-stable-heres-what-the-migration-actually-costs-6a6a297e81d4)

---

## 9. AI Agent Instructions

**Before doing ANY work on this migration:**

1. Read this entire document.
2. Verify all **Prerequisites (Section 2)** are met — every hard blocker must be resolved.
3. Check the **Decision Matrix (Section 5)** — if any 🟢 condition is false or any 🔴 condition is true, **STOP** and inform the user.
4. If all green, proceed in the **Recommended Order (Section 6)**, one phase at a time.
5. Do NOT skip the `entryDecorators` order requirement — it silently breaks `rememberSaveable`.
6. Do NOT use alpha versions of `lifecycle-viewmodel-navigation3`.
7. Do NOT attempt incremental migration — Nav3 requires an atomic change.
8. Keep `ApiActivity` unchanged — it is not part of in-app navigation.
