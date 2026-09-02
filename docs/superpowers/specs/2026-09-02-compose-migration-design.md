# Hail: Full XML-to-Compose Migration Design

**Date:** 2026-09-02
**Status:** Draft
**Author:** Kilo (brainstorming skill)
**Target branch:** `dev`
**Working repo:** `rahaaatul/Hail` (never `aistra0528/Hail`)

---

## 1. Summary

Migrate Hail's UI layer from XML Views + Fragments to Jetpack Compose, ending with a single `ComponentActivity` hosting a Navigation 3 `NavDisplay`. The data layer, service layer, and theme system remain unchanged. The migration follows Google's recommended per-screen approach (one screen per branch, create-new-then-delete-old).

## 2. Motivation

The app is already ~40% Compose (Settings, About, ApiActivity, bottom nav, theme). Remaining XML screens (Home/Pager, Apps, Actions) create maintenance burden: two UI paradigms, ViewBinding boilerplate, RecyclerView adapter code, XML/View interop friction. Completing the migration unlocks:
- Single source of truth for UI (one paradigm)
- Compose UI tests for all screens
- Navigation 3 type-safe routes
- Removal of `appcompat`, `constraintlayout`, `swiperefreshlayout`, `insetter` dependencies
- ~35% reduction in UI code (per Google's published metrics)

## 3. Non-Goals

- **No data layer changes** — `HailData`, `AppMetaCache`, `AppManager`, Room, WorkManager, Shizuku, Xposed hooks remain untouched
- **No theme changes** — `HailTheme` + `MaterialExpressiveTheme` already in place
- **No new features** — this is a structural refactor only
- **No Room schema changes**
- **No manifest changes** beyond removing now-unused Activity references

## 4. Target Architecture

### 4.1 End State

```
MainActivity (ComponentActivity)
  └── setContent { HailTheme { HailRootScreen() } }
        ├── HailNavHost (Navigation 3 NavDisplay)
        │     ├── HomeRoute → HomeScreen
        │     │     └── PrimaryTabRow + HorizontalPager
        │     │           └── PagerScreen (per tag, LazyVerticalGrid)
        │     ├── AppsRoute → AppsScreen (LazyVerticalGrid)
        │     ├── ActionsRoute → ActionsScreen (LazyColumn)
        │     ├── SettingsRoute → SettingsScreen (existing, rehomed)
        │     └── AboutRoute → AboutScreen (existing, rehomed)
        ├── HailTopAppBar (TopAppBar with title + actions)
        ├── HailBottomNav (ExpressiveNavigationBar, existing)
        └── HailFab (ExtendedFloatingActionButton, per-destination)
```

### 4.2 Package Structure (after migration)

```
app/src/main/kotlin/com/aistra/hail/ui/
  MainActivity.kt                    — single activity, NavHost root
  nav/
    HailNavHost.kt                   — Navigation 3 entryProvider
    Routes.kt                        — @Serializable route data classes
    HailNavState.kt                  — back stack + scene strategy
    NavActions.kt                    — type-safe navigation callbacks
  screens/
    home/
      HomeScreen.kt                  — tab pager
      PagerScreen.kt                 — app grid per tag
      HomeAppItem.kt                 — single app cell composable
    apps/
      AppsScreen.kt                  — all-apps list
      AppsViewModel.kt               — already exists, rehome
      AppListItem.kt                 — single app row composable
    actions/
      ActionsScreen.kt               — actions list
      ActionItem.kt                  — single action row
      ActionEditorDialog.kt          — create/edit dialog
      AppPickerDialog.kt             — app picker in editor
    settings/
      SettingsScreen.kt              — already Compose, rehome
      ... (existing sub-screens)
    about/
      AboutScreen.kt                 — already Compose, rehome
    shared/
      HailTopAppBar.kt               — top app bar composable
      HailBottomNav.kt               — from ExpressiveNavigationBar
      HailFab.kt                     — FAB composable
      HailScaffold.kt                — Scaffold wrapper with insets
      EmptyState.kt                  — empty list placeholder
  theme/                             — unchanged
```

### 4.3 Dependencies Removed (end state)

- `androidx.appcompat:appcompat`
- `androidx.constraintlayout:constraintlayout`
- `androidx.swiperefreshlayout:swiperefreshlayout`
- `dev.chrisbanes.insetter:insetter`
- `com.google.android.material:material` (Material XML components)

### 4.4 Dependencies Added (end state)

- `androidx.navigation3:navigation3-runtime`
- `androidx.navigation3:navigation3-compose`
- `org.jetbrains.kotlinx:kotlinx-serialization-json` (for type-safe routes)

**Note:** Adding new dependencies requires user approval per AGENTS.md "Ask first" rule.

## 5. Navigation 3 Design

### 5.1 Routes (type-safe, `@Serializable`)

```kotlin
@Serializable data object HomeRoute
@Serializable data object AppsRoute
@Serializable data object ActionsRoute
@Serializable data object SettingsRoute
@Serializable data object AboutRoute
```

### 5.2 NavDisplay with Multiple Back Stacks

Uses Navigation 3's "Multiple back stacks" recipe (per the `navigation-3` skill). Each top-level destination (Home, Actions, Settings) maintains its own back stack. The bottom nav switches between stacks. State is retained across config changes and process death.

### 5.3 Edge-to-Edge

- `enableEdgeToEdge()` called in `MainActivity.onCreate()` (replaces current `setDecorFitsSystemWindows(false)`)
- `android:windowSoftInputMode="adjustResize"` added to manifest
- `window.isNavigationBarContrastEnforced = false` preserved
- System bar icon colors delegated to `enableEdgeToEdge()` (removes manual `window.statusBarColor` / `window.navigationBarColor` setting in `Theme.kt`)
- All screens use `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` pattern per `edge-to-edge` skill

## 6. Per-Screen Migration Plan

Each screen follows the `migrate-xml-views-to-jetpack-compose` skill's 10-step process. **Pattern: create new file, validate, delete old files.** No in-place editing of existing Fragments.

### 6.1 Migration Order

| # | Screen | Branch | Current Files | New File | Complexity |
|---|--------|--------|---------------|----------|------------|
| 1 | PagerFragment | `feature/migrate-pager-to-compose` | `PagerFragment.kt`, `fragment_pager.xml`, `PagerAdapter.kt`, `item_home.xml` | `PagerScreen.kt`, `HomeAppItem.kt` | **High** — multiselect, context menus, tri-state tag dialog (already Compose), SwipeRefreshLayout, SearchView, icon loading |
| 2 | AppsFragment | `feature/migrate-apps-to-compose` | `AppsFragment.kt`, `fragment_apps.xml`, `AppsAdapter.kt`, `item_apps.xml`, `HRecyclerView.kt` | `AppsScreen.kt`, `AppListItem.kt` | **High** — GridLayoutManager, SwipeRefreshLayout, search/sort menus, context menus, APK export ActivityResult |
| 3 | ActionsFragment | `feature/migrate-actions-to-compose` | `ActionsFragment.kt`, `fragment_actions.xml`, `ActionsAdapter.kt`, `item_action.xml`, `AppPickerAdapter.kt`, `item_action_picker.xml`, `dialog_input.xml` | `ActionsScreen.kt`, `ActionItem.kt`, `ActionEditorDialog.kt`, `AppPickerDialog.kt` | **Medium** — dialog-heavy, app picker grid |
| 4 | HomeFragment | `feature/migrate-home-to-compose` | `HomeFragment.kt`, `fragment_home.xml`, `HomeAdapter.kt` | `HomeScreen.kt` | **Medium** — TabLayout+ViewPager2 → PrimaryTabRow+HorizontalPager |
| 5 | MainActivity | `feature/migrate-mainactivity-to-compose` | `MainActivity.kt`, `activity_main.xml`, `app_bar_main.xml`, `content_main.xml` | `MainActivity.kt` (rewrite), `HailRootScreen.kt`, `HailScaffold.kt`, `HailTopAppBar.kt`, `HailFab.kt` | **Medium** — biometric auth, FAB per-destination, edge-to-edge, Navigation 3 host |

### 6.2 Per-Screen Deliverables

Per the 10-step skill, each screen migration produces:
1. `@Composable` screen function in `ui/screens/<screen>/<Screen>Screen.kt`
2. `@Preview` composable for visual verification
3. Compose UI test in `app/src/androidTest/kotlin/com/aistra/hail/ui/screens/<screen>/` using `createAndroidComposeRule`
4. Old files deleted in a separate commit: `Fragment.kt`, `layout.xml`, `Adapter.kt`, `item_*.xml`
5. PR with: `[Hail] Migrate <Screen> to Compose`

### 6.3 Per-Screen Validation

- `./gradlew :app:compileDebugKotlin` passes
- `./gradlew :app:testDebugUnitTest` passes
- `./gradlew :app:connectedDebugAndroidTest` passes for new Compose UI tests
- Visual parity verified on emulator/device (screenshot comparison with baseline)
- All existing behavior preserved (multiselect, context menus, search, freeze/unfreeze workflows)

## 7. Per-Screen Migration Pattern (10-Step)

For each screen (e.g., PagerFragment):

1. **Identify** — `fragment_pager.xml` is the target
2. **Analyze** — read fragment, adapter, item layout, ViewModel
3. **Plan** — decompose UI into composables, state hoisting, list of components needed
4. **Capture** — take baseline screenshot of current screen
5. **Setup deps** — Compose deps already present
6. **Setup theming** — `HailTheme` already in place
7. **Migrate** — create `PagerScreen.kt` + `HomeAppItem.kt` alongside old files
8. **Validate** — visual parity, compile, tests
9. **Replace** — wire new screen into Fragment (temporary) or directly into nav graph; old files still present
10. **Cleanup** — delete old `PagerFragment.kt`, `fragment_pager.xml`, `PagerAdapter.kt`, `item_home.xml`; Fragment deleted if no longer needed

## 8. MainActivity Migration (Final Step)

The last migration is the most impactful:

### 8.1 Before
```kotlin
class MainActivity : AppCompatActivity() {
    // XML layout with FragmentContainerView(NavHostFragment)
    // ComposeView for bottom nav
    // AppBarLayout + MaterialToolbar
    // ExtendedFloatingActionButton
}
```

### 8.2 After
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HailTheme {
                HailRootScreen()
            }
        }
    }
}
```

### 8.3 HailRootScreen
```kotlin
@Composable
fun HailRootScreen() {
    val navState = rememberHailNavState()
    val currentRoute by navState.currentRouteAsState()
    HailScaffold(
        topBar = { HailTopAppBar(currentRoute) },
        bottomBar = { HailBottomNav(navState) },
        fab = { HailFab(currentRoute, navState) }
    ) { padding ->
        HailNavHost(navState, modifier = Modifier.padding(padding))
    }
}
```

## 9. Edge-to-Edge Migration

Per the `edge-to-edge` skill:

- **Add `enableEdgeToEdge()`** to `MainActivity.onCreate()` (replaces `setDecorFitsSystemWindows(false)`)
- **Add `android:windowSoftInputMode="adjustResize"`** to manifest
- **Remove manual `window.statusBarColor` / `window.navigationBarColor`** from `Theme.kt` — `enableEdgeToEdge()` handles these
- **Remove `WindowCompat.getInsetsController().isAppearanceLight*`** from `Theme.kt` — `enableEdgeToEdge()` handles icon colors
- **All screens** use `Scaffold(contentWindowInsets = WindowInsets.safeDrawing)` with `consumeWindowInsets`
- **Lists** use `contentPadding` not `Modifier.padding` for inset handling
- **FABs** positioned via Scaffold's `floatingActionButton` slot

## 10. Testing Strategy

### 10.1 Per-Screen Compose UI Tests

Each migrated screen gets a Compose UI test:

```kotlin
@RunWith(AndroidJUnit4::class)
class PagerScreenTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun pagedScreen_displaysAppGrid() {
        composeTestRule.setContent {
            HailTheme { PagerScreen(/* test state */) }
        }
        composeTestRule.onNodeWithTag("app_grid").assertIsDisplayed()
    }
}
```

### 10.2 Existing Tests

- `app/src/test/` unit tests continue to work (no changes to data layer)
- `app/src/androidTest/` instrumented tests (DAO, ActionExecutor) continue to work

## 11. Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Visual regression in migrated screens | User-visible bugs | Screenshot comparison + device testing per screen |
| Navigation 3 API changes (alpha) | Build breaks | Pin specific version; Navigation Compose fallback option |
| Context menu behavior differences | UX regression | Use Compose `DropdownMenu` with anchored positioning |
| SwipeRefreshLayout replacement | Different pull-to-refresh feel | Use Material 3 `PullToRefreshBox` |
| SearchView replacement | Different search UX | Use Material 3 `SearchBar` or `DockedSearchBar` |
| Biometric auth integration | Auth flow breaks | Keep BiometricPrompt API call in `MainActivity`, pass result to Compose via `LaunchedEffect` |

## 12. Rollout

### 12.1 Branch Strategy

- One feature branch per screen: `feature/migrate-<screen>-to-compose`
- Merge to `dev` after validation
- `dev` is the integration branch
- Final cleanup branch: `feature/remove-fragments-and-xml` (deletes all remaining Fragments and XML, removes AppCompat)

### 12.2 Commit Strategy

Per AGENTS.md, conventional commits. Each screen migration = 2-3 commits:
1. `feat(ui): migrate <Screen> to Compose` (add new files)
2. `refactor(ui): remove old <Screen> Fragment and XML` (delete old files)

### 12.3 PR Strategy

One PR per screen. Squash merge per AGENTS.md.

## 13. Resolved Decisions (Google Recommendations)

1. **Navigation library** — Navigation 3. Per Google: "The recommended end goal for a Compose-first architecture is to replace Fragments entirely with screen-level composables managed by Navigation Compose/Navigation 3." Alpha is acceptable for new Compose development.

2. **Compose BOM compatibility** — Navigation 3 is independent of Compose BOM. Current `2026.08.00` BOM is fully compatible. No BOM change needed.

3. **PullToRefreshBox** — Use Material 3's `PullToRefreshBox` (available in `material3 1.3.0+`, included in `1.5.0-alpha27`). This is Google's recommended replacement for `SwipeRefreshLayout` in Compose.

4. **Biometric flow** — Keep `BiometricPrompt` call in `MainActivity.onCreate()` (it requires `FragmentActivity` context). Expose authentication state to Compose via `mutableStateOf` in the Activity, read by Compose. This is Google's recommended pattern for biometric auth in Compose.

5. **minSdk compatibility** — Navigation 3 requires `minSdk 23+` per Google's migration guide. Hail's current `minSdk = 24` (Android 7.0 Nougat) is fully compatible. No minSdk change needed. Android 7.0+ covers 99.2% of Play Store users per Google's January 2026 data.

6. **compileSdk** — Navigation 3 requires `compileSdk 36+`. Hail's current `compileSdk = 37` exceeds the requirement. No change needed.

7. **Material 3 Expressive animations** — Already in use via `MotionScheme.expressive()` in `Theme.kt`. Google's Material 3 Expressive provides built-in motion specs (`fastEffectsSpec()`, `slowEffectsSpec()`, `emphasizedDecelerateEasing()`, etc.) for all UI animations. No additional dependencies or setup needed — animations are built into `material3 1.5.0-alpha27`. Use `MaterialTheme.motionScheme` throughout for consistent Expressive motion.

## 14. Success Criteria

- [ ] Zero XML layouts remain (except those needed by other modules)
- [ ] Zero Fragments remain
- [ ] Zero Adapters remain
- [ ] Single `MainActivity` (ComponentActivity, not AppCompat)
- [ ] Navigation 3 `NavDisplay` hosts all screens
- [ ] All screens are `@Composable` functions
- [ ] `./gradlew :app:compileDebugKotlin` passes
- [ ] `./gradlew :app:testDebugUnitTest` passes
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes
- [ ] `./gradlew :app:assembleDebug` produces working APK
- [ ] Visual parity verified for each migrated screen
- [ ] Dependencies removed: appcompat, constraintlayout, swiperefreshlayout, insetter, material XML
