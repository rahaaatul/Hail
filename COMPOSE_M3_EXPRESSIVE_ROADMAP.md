# Compose + Material 3 Expressive Migration Roadmap

> **Single source of truth for migrating Hail from View-based Fragments to 100% Compose with Material 3 Expressive UI.**
> Read this entire document before starting any migration work.

---

## 1. Current State Assessment

### 1.1 Architecture Overview
| Layer | Current State | Compose Status |
|-------|---------------|----------------|
| Activities | 2: MainActivity (AppCompatActivity), ApiActivity (ComponentActivity) | ApiActivity ✅ Compose | MainActivity ❌ Fragment-based |
| Navigation | Navigation 2 + 5 Fragments + XML nav graph | 0% Compose |
| Screens | 6 Fragments | 2 partially Compose, 4 pure View |
| Layouts | 15 XML layout files | 0% Compose |
| Theming | Material 3 dynamic colors via XML + Compose theme | 50% Compose |
| Lists | Recycler + ViewPager2 + GridManager + FragmentStateAdapter | 0% Compose |
| Dialogs | MaterialAlertDialogBuilder (View) | 30% Compose |
| Menus | MenuProvider + XML menus + SearchView | 0% Compose |
| Dependencies | Mixed View + Compose libraries | Partial |

### 1.2 Existing Compose Assets (Reuse These)
| Asset | Location | Reusability |
|-------|----------|-------------|
| AppTheme | ui/theme/Theme.kt | ✅ Keep, extend for M3 Expressive |
| Color schemes | ui/theme/Color.kt | ✅ Keep, add Expressive tokens |
| Typography | ui/theme/Type.kt | ⚠️ Default — needs M3 Expressive type scale |
| Settings composable | SettingsFragment.kt SettingsScreen() | ✅ Extract to standalone |
| About composable | AboutFragment.kt AboutScreen() | ✅ Extract to standalone |
| Pager dialog composable | PagerFragment.kt TriStateTagList() | ✅ Extract to standalone |
| Compose BOM | 2026.08.00 | ⚠️ Upgrade to latest stable |
| compose-preference | me.zhanghai.compose.preference | ⚠️ Keep or replace with M3 Expressive |

### 1.3 All View-Based Files To Migrate

**Activities (1):**
- app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt

**Fragments (5):**
- app/src/main/kotlin/com/aistra/hail/ui/main/MainFragment.kt
- app/src/main/kotlin/com/aistra/hail/ui/home/HomeFragment.kt
- app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt
- app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt
- app/src/main/kotlin/com/aistra/hail/ui/actions/ActionsFragment.kt

**Adapters (3):**
- app/src/main/kotlin/com/aistra/hail/ui/apps/AppsAdapter.kt
- app/src/main/kotlin/com/aistra/hail/ui/home/HomeAdapter.kt
- app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt

**Layout XMLs (15):**
- res/layout/activity_main.xml
- res/layout/app_bar_main.xml
- res/layout/content_main.xml
- res/layout/fragment_home.xml
- res/layout/fragment_pager.xml
- res/layout/fragment_apps.xml
- res/layout/fragment_actions.xml
- res/layout/item_apps.xml
- res/layout/item_home.xml
- res/layout/item_action.xml
- res/layout/item_action_picker.xml
- res/layout/multiselect_action_view.xml
- res/layout/dialog_progress.xml
- res/layout/dialog_input.xml

**Menus (5):**
- res/menu/menu_home.xml
- res/menu/menu_apps.xml
- res/menu/menu_apps_action.xml
- res/menu/menu_settings.xml
- res/menu/nav_main.xml

**Navigation (1):**
- res/navigation/mobile_navigation.xml

**Drawables (26):** All vector drawables can be kept as VectorAssets or converted to Painter resources.


---

## 2. Library Strategy

### 2.1 Libraries to ADD
| Library | Version | Purpose |
|---------|---------|---------|
| androidx.navigation:navigation-compose | 2.9.8+ | Compose routing (interim before Nav3) |
| androidx.lifecycle:lifecycle-runtime-compose | 2.8.7+ | collectAsStateWithLifecycle |
| io.coil-kt:coil-compose | 2.7.0+ | Async image loading (replaces AppIconCache bitmap loading) |
| com.google.accompanist:accompanist-pager | 0.36.0+ | ViewPager2 replacement |
| com.google.accompanist:accompanist-systemuicontroller | 0.37.0+ | System bars color control |
| androidx.compose.material3:material3-expressive | 1.1.0+ | Material 3 Expressive components |

> Note: material3-expressive is a separate artifact from core material3. It provides the expressive variant of M3 components with updated motion, shapes, and component behaviors.

### 2.2 Libraries to REMOVE
| Library | Reason |
|---------|--------|
| androidx.appcompat:appcompat | Replaced by ComponentActivity + Material 3 |
| androidx.constraintlayout:constraintlayout | Replaced by Compose layout DSL |
| androidx.swiperefreshlayout:swiperefreshlayout | Replaced by PullRefresh in Compose |
| androidx.preference:preference-ktx | Replaced by compose-preference or M3 Expressive preferences |
| com.google.android.material:material | Replaced by material3-expressive |
| androidx.navigation:navigation-fragment-ktx | Replaced by navigation-compose |
| androidx.navigation:navigation-ui-ktx | Replaced by Compose navigation UI |

### 2.3 Libraries to KEEP
| Library | Reason |
|---------|--------|
| androidx.activity:activity-compose | Needed for ComponentActivity + setContent |
| androidx.core:core-ktx | Utility extensions |
| androidx.lifecycle:lifecycle-livedata-ktx | ViewModel support |
| androidx.work:work-runtime | Background work |
| androidx.room:room3-runtime | Database |
| kotlinx-coroutines-android | Coroutines |
| compose-preference | Settings preferences (optional replacement) |
| All other utility libs | Not UI-related |


---

## 3. Theme & Design System Migration

### 3.1 Current Theme State
The app already has a solid Compose theme foundation:
- Material 3 dynamic colors (Android 12+)
- Light/dark color schemes with full token coverage
- AppTheme composable wrapper
- XML theme parent: Theme.Material3.DynamicColors.DayNight.NoActionBar

### 3.2 What Changes with Material 3 Expressive
Material 3 Expressive (released 2025, stable 2026) introduces:

| Token Category | Change | Impact |
|----------------|--------|--------|
| Shapes | Updated corner sizes: Small=8dp (was 4dp), Medium=12dp (was 8dp), Large=16dp (was 16dp) | Cards, dialogs, buttons |
| Motion | Emphasized easing curves, longer standard durations (200ms to 300ms) | All transitions |
| Component specs | Updated FAB, BottomNav, Card, ListItem specs | Component replacements |
| Typography | Updated type scale with more weight contrast | AppTypography rewrite |
| Color | Enhanced tonal palettes, new surface roles | Mostly backward-compatible |

### 3.3 Theme Migration Steps

**Step 3.3.1: Upgrade Compose BOM**
```toml
# gradle/libs.versions.toml
composeBom = "2026.08.00"  # Verify latest stable
```

**Step 3.3.2: Add Material 3 Expressive Dependency**
```toml
[libraries]
androidx-compose-material3-expressive = { module = "androidx.compose.material3:material3-expressive" }
```

**Step 3.3.3: Update Theme.kt**
```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        dynamicColor && HTarget.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    // M3 Expressive uses updated shapes
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(16.dp),
        large = RoundedCornerShape(24.dp),
        extraLarge = RoundedCornerShape(32.dp)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = shapes,
        content = content
    )
}
```

**Step 3.3.4: Remove XML Theme Overrides**
Gradually remove XML theme attributes that are now handled in Compose:
```xml
<!-- REMOVE from themes.xml -->
<item name="android:statusBarColor">@android:color/transparent</item>
<item name="android:navigationBarColor">@android:color/transparent</item>
<!-- These are now handled by accompanist-systemuicontroller or Compose -->
```

**Step 3.3.5: Update Typography for Expressive Scale**
```kotlin
// ui/theme/Type.kt
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = defaultFont,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = defaultFont,
        fontWeight = FontWeight.Normal,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    // Continue with full M3 Expressive type scale
    // headlineLarge through labelSmall with updated sizes
)
```

> Pitfall: If you use custom fonts, verify they support the new weight range (400-800). M3 Expressive uses more weight contrast.


---

## 4. Migration Phases (Detailed)

### Phase 0: Foundation (2-3 days) [COMPLETED]
**Goal: Add libraries, upgrade dependencies, create Compose infrastructure**
**Status: DONE — Dependencies added, shared components created, navigation graph defined**

#### 0.1 Upgrade Dependencies
```toml
# gradle/libs.versions.toml
[versions]
composeBom = "2026.08.00"  # Verify latest stable
navigationCompose = "2.9.8"
lifecycleRuntimeCompose = "2.8.7"
coilCompose = "2.7.0"
accompanistPager = "0.36.0"
accompanistSystemUiController = "0.37.0"
material3Expressive = "1.1.0"

[libraries]
# ADD
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
androidx-lifecycle-runtime-compose = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycleRuntimeCompose" }
coil-compose = { module = "io.coil-kt:coil-compose", version.ref = "coilCompose" }
accompanist-pager = { module = "com.google.accompanist:accompanist-pager", version.ref = "accompanistPager" }
accompanist-system-ui-controller = { module = "com.google.accompanist:accompanist-systemuicontroller", version.ref = "accompanistSystemUiController" }
androidx-compose-material3-expressive = { module = "androidx.compose.material3:material3-expressive", version.ref = "material3Expressive" }
```

#### 0.2 Create Compose Infrastructure
Create these new files:

```
ui/
├── components/
│   ├── AppImage.kt          # Coil-based async image loader (replaces AppIconCache)
│   ├── AppScaffold.kt       # M3 Expressive Scaffold with TopAppBar, FAB, BottomNav
│   ├── AppTopAppBar.kt      # M3 Expressive TopAppBar with navigation/actions
│   ├── AppBottomNav.kt      # M3 Expressive NavigationBar
│   ├── AppNavigationRail.kt # M3 Expressive NavigationRail
│   ├── AppFab.kt            # M3 Expressive FloatingActionButton
│   ├── AppSearchBar.kt      # M3 Expressive SearchBar
│   ├── AppPullRefresh.kt    # PullRefresh wrapper
│   ├── AppDialog.kt         # M3 Expressive AlertDialog wrapper
│   └── EmptyState.kt        # Reusable empty state composable
├── navigation/
│   └── NavGraph.kt          # Compose navigation routes + graph
└── theme/
    ├── Color.kt             # Already exists
    ├── Theme.kt             # Already exists
    └── Type.kt              # Already exists, needs update
```

#### 0.3 Create Navigation Infrastructure
```kotlin
// ui/navigation/NavGraph.kt
sealed interface HailRoute {
    @Serializable data object Home : HailRoute
    @Serializable data object Actions : HailRoute
    @Serializable data object Apps : HailRoute
    @Serializable data object Settings : HailRoute
    @Serializable data object About : HailRoute
}

@Composable
fun HailNavHost(navController: NavHostController, startDestination: HailRoute = HailRoute.Home) {
    NavHost(navController, startDestination = startDestination.serializer()) {
        composable<HailRoute.Home> { HomeScreen() }
        composable<HailRoute.Actions> { ActionsScreen() }
        composable<HailRoute.Apps> { AppsScreen() }
        composable<HailRoute.Settings> { SettingsScreen() }
        composable<HailRoute.About> { AboutScreen() }
    }
}
```

> Note: This uses Compose Navigation as an interim step. The final architecture will upgrade to Nav3 (see NAV3_ROADMAP.md). The route definitions are forward-compatible.


---

### Phase 1: Extract Existing Compose Screens (1 day) [NEXT]
**Goal: Convert SettingsFragment and AboutFragment from Fragment wrappers to pure composables**

#### 1.1 SettingsFragment to SettingsScreen
**Current state**: SettingsFragment wraps SettingsScreen() in a ComposeView

**Changes**:
1. Move SettingsScreen() and all helper composables (switchPreference, listPreference, horizontalDivider) to ui/settings/SettingsScreen.kt
2. Replace Fragment-specific APIs:
   - registerForActivityResult(ActivityResultContracts.RequestPermission()) to rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())
   - requireActivity() to LocalContext.current
   - viewLifecycleOwner.lifecycleScope to LaunchedEffect or rememberCoroutineScope()
   - findNavController().navigate(R.id.nav_about) to navController.navigate(HailRoute.About)
3. Remove SettingsFragment.kt entirely
4. Update navigation graph to use composable<HailRoute.Settings> { SettingsScreen() }

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsScreen.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt

#### 1.2 AboutFragment to AboutScreen
**Current state**: AboutFragment wraps AboutScreen() in a ComposeView

**Changes**:
1. Move AboutScreen(), ClickableItem(), LicenseDialog() to ui/about/AboutScreen.kt
2. Replace Fragment-specific APIs:
   - requireActivity() to LocalContext.current
   - startActivity(intent) to LocalContext.current.startActivity(intent)
3. Remove AboutFragment.kt entirely
4. Update navigation graph

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/about/AboutScreen.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/about/AboutFragment.kt

#### 1.3 Extract PagerFragment Dialog Composable
**Current state**: TriStateTagList() is nested inside PagerFragment

**Changes**:
1. Move TriStateTagList() to ui/home/TriStateTagList.kt
2. Make it a public composable that accepts state parameters

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/home/TriStateTagList.kt


---

### Phase 2: Action & Home Screens (Medium Complexity) (3-4 days)
**Goal: Migrate ActionsFragment and HomeFragment/PagerFragment to Compose**

#### 2.1 ActionsFragment to ActionsScreen
**Current state**: View-based fragment with RecyclerView + heavy dialog usage

**Changes**:
1. Replace RecyclerView +LinearLayoutManager with LazyColumn
2. Convert ActionsAdapter to LazyListScope items
3. Replace all MaterialAlertDialogBuilder with M3 Expressive AlertDialog:
   - showActionMenu() to DropdownMenu on long-press
   - showEditor() to Dialog composable
   - showAppPicker() to Dialog with LazyVerticalGrid
4. Replace registerForContextMenu with DropdownMenu
5. Replace viewLifecycleOwner.lifecycleScope with rememberCoroutineScope()

**New composable structure**:
```kotlin
@Composable
fun ActionsScreen(
    viewModel: ActionsViewModel = viewModel()
) {
    var actions by remember { mutableStateOf<List<LaunchAction>>(emptyList()) }
    var showEditor by remember { mutableStateOf<LaunchAction?>(null) }
    var showAppPicker by remember { mutableStateOf<ShowAppPickerState?>(null) }

    Scaffold(
        topBar = { AppTopAppBar(title = stringResource(R.string.title_actions)) }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(actions) { action ->
                ActionItem(
                    action = action,
                    onClick = { /* execute */ },
                    onLongClick = { /* show menu */ }
                )
            }
        }
    }

    // Dialogs
    showEditor?.let { ActionEditorDialog(it, onDismiss = { showEditor = null }) }
    showAppPicker?.let { AppPickerDialog(it, onDismiss = { showAppPicker = null }) }
}
```

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/actions/ActionsScreen.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/actions/ActionsFragment.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/actions/ActionsAdapter.kt

#### 2.2 HomeFragment + PagerFragment to HomeScreen
**Current state**: Fragment + child Fragment with ViewPager2 + TabLayout + RecyclerView + GridManager

**Changes**:
1. Replace ViewPager2 + TabLayoutMediator with HorizontalPager (Accompanist) + TabRow
2. Replace PagerFragment content with HomePagerScreen() composable
3. Replace RecyclerView + GridManager with LazyVerticalGrid
4. Replace PagerAdapter with Compose pager page composable
5. Replace HomeAdapter (FragmentStateAdapter) with pager page count + content
6. Replace OnBackPressedCallback with BackHandler
7. Replace registerForContextMenu with DropdownMenu
8. Replace MenuProvider with TopAppBar actions

**New composable structure**:
```kotlin
@OptIn(ExperimentalPagerApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val tabs = remember { HailData.tags }
    val pagerState = rememberPagerState { tabs.size }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = stringResource(R.string.app_name),
                actions = { /* search, multiselect */ }
            )
        },
        floatingActionButton = { /* FAB */ }
    ) { padding ->
        Column {
            if (tabs.size > 1) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = { /* handled by pager */ },
                            text = { Text(tab.first) }
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                HomePagerScreen(tagId = tabs[page].second)
            }
        }
    }
}

@Composable
fun HomePagerScreen(tagId: Int) {
    var query by remember { mutableStateOf("") }
    var multiselect by remember { mutableStateOf(false) }
    val selectedList = remember { mutableStateListOf<AppInfo>() }

    // PullRefresh + LazyVerticalGrid
    Box(modifier = Modifier.fillMaxSize()) {
        val gridState = rememberLazyGridState()

        LazyVerticalGrid(
            columns = GridCells.Adaptive(128.dp),
            state = gridState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(apps) { app ->
                HomeAppItem(
                    app = app,
                    selected = app in selectedList,
                    multiselect = multiselect,
                    onClick = { /* launch or select */ },
                    onLongClick = { /* show menu */ }
                )
            }
        }

        if (apps.isEmpty()) {
            EmptyState(text = stringResource(R.string.nothing_here))
        }
    }

    BackHandler(enabled = multiselect) {
        multiselect = false
        selectedList.clear()
    }
}
```

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/home/HomeScreen.kt
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/home/HomePagerScreen.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/home/HomeFragment.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/home/HomeAdapter.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt


---

### Phase 3: Apps Screen (High Complexity) (3-4 days)
**Goal: Migrate AppsFragment to Compose with Grid + Menus + Activity Results**

**Current state**: GridManager + SwipeRefresh + MenuProvider + registerForActivityResult + context menus

**Changes**:
1. Replace SwipeRefresh with PullRefresh (Accompanist or Compose-foundation)
2. Replace GridManager + RecyclerView with LazyVerticalGrid
3. Replace AppsAdapter with LazyVerticalGrid items
4. Replace MenuProvider + SearchView with TopAppBar + SearchBar/SearchActive
5. Replace overflow menu with DropdownMenu
6. Replace registerForActivityResult(CreateDocument) with rememberLauncherForActivityResult
7. Replace registerForContextMenu with DropdownMenu
8. Replace Snackbar with SnackbarHost in Scaffold

**New composable structure**:
```kotlin
@Composable
fun AppsScreen(
    viewModel: AppsViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { uri -> /* handle export */ }

    Scaffold(
        topBar = {
            AppTopAppBar(
                title = stringResource(R.string.title_apps),
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.postQuery(it) },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Outlined.Sort, contentDescription = null)
                    }
                    DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                        // sort options
                    }
                }
            )
        },
        floatingActionButton = { /* FAB */ }
    ) { padding ->
        PullRefresh(
            refreshing = viewModel.isRefreshing,
            onRefresh = { viewModel.updateAppList(true) }
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                contentPadding = padding,
                modifier = Modifier.fillMaxSize()
            ) {
                items(viewModel.displayApps) { app ->
                    AppsItem(
                        app = app,
                        onClick = { /* handle click */ },
                        onLongClick = { /* show context menu */ },
                        checked = viewModel.isChecked(app.packageName),
                        onCheckedChange = { /* handle check */ }
                    )
                }
            }
        }
    }
}
```

**Files to modify**:
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/apps/AppsScreen.kt
- CREATE: app/src/main/kotlin/com/aistra/hail/ui/apps/AppsItem.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt
- DELETE: app/src/main/kotlin/com/aistra/hail/ui/apps/AppsAdapter.kt


---

### Phase 4: Activity-Level Migration (2-3 days)
**Goal: Replace MainActivity with ComponentActivity + Compose UI**

#### 4.1 MainActivity to ComponentActivity
**Changes**:
1. Change base class from AppCompatActivity to ComponentActivity
2. Replace setContentView with setContent { }
3. Move all UI to Compose:
   - activity_main.xml to MainScreen() composable
   - app_bar_main.xml to AppScaffold() composable
   - content_main.xml to NavHost area
4. ReplaceNavController + NavHostFragment with Compose NavHost
5. Replace Bottom NavigationView with NavigationBar (M3 Expressive)
6. Replace NavigationRailView with NavigationRail (M3 Expressive)
7. Replace ExtendedFloatingActionButton with ExtendedFloatingActionButton from M3 Expressive
8. Move biometric prompt to Compose side
9. Handle window insets with Compose (Modifier.systemBarsPadding())

**New structure**:
```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val useNavRail = WindowInsetsNavigationBars.asPaddingValues().calculateEndPadding(
        LocalLayoutDirection.current
    ) > 320.dp

    // Biometric prompt
    var showBiometricPrompt by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            if (!useNavRail) {
                AppBottomNav(navController = navController)
            }
        },
        floatingActionButton = { AppFab(navController = navController) }
    ) { padding ->
        Row {
            if (useNavRail) {
                AppNavigationRail(navController = navController)
            }
            Box(modifier = Modifier.padding(padding)) {
                HailNavHost(navController = navController)
            }
        }
    }
}
```

**Files to modify**:
- MODIFY: app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt
- DELETE: app/src/main/res/layout/activity_main.xml
- DELETE: app/src/main/res/layout/app_bar_main.xml
- DELETE: app/src/main/res/layout/content_main.xml

#### 4.2 Update Navigation
Replace XML navigation with Compose navigation:
```kotlin
// ui/navigation/NavGraph.kt
@Composable
fun HailNavHost(navController: NavHostController) {
    NavHost(navController, startDestination = HailRoute.Home) {
        composable<HailRoute.Home> { HomeScreen() }
        composable<HailRoute.Actions> { ActionsScreen() }
        composable<HailRoute.Apps> { AppsScreen() }
        composable<HailRoute.Settings> { SettingsScreen() }
        composable<HailRoute.About> { AboutScreen() }
    }
}
```

**Files to modify**:
- DELETE: app/src/main/res/navigation/mobile_navigation.xml


---

### Phase 5: Shared Components & Polish (2-3 days)
**Goal: Create reusable M3 Expressive components, remove all XML layouts**

#### 5.1 Create Shared Components
These replace View-based layouts and adapters:

| Component | Replaces | File |
|-----------|----------|------|
| AppImage | AppIconCache bitmap loading + ImageView | ui/components/AppImage.kt |
| AppListItem | item_apps.xml | ui/components/AppListItem.kt |
| HomeAppItem | item_home.xml | ui/components/HomeAppItem.kt |
| ActionItem | item_action.xml + ActionsAdapter | ui/components/ActionItem.kt |
| AppPickerItem | item_action_picker.xml | ui/components/AppPickerItem.kt |
| EmptyState | fragment_pager.xml empty view | ui/components/EmptyState.kt |
|ProgressDialog | dialog_progress.xml | ui/components/ProgressDialog.kt |
| InputDialog | dialog_input.xml | ui/components/InputDialog.kt |

#### 5.2 Image Loading with Coil
Replace AppIconCache.loadIconBitmapAsync():
```kotlin
@Composable
fun AppImage(
    info: ApplicationInfo,
    grayscale: Boolean = false,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(info)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        transform = if (grayscale) {
            arrayOf(ColorFilterMatrixTransformer(
                floatArrayOf(
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0.33f, 0.33f, 0.33f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            ))
        } else emptyArray()
    )
}
```

#### 5.3 Remove Remaining XML Layouts
After Phase 4, delete ALL remaining XML layouts:
```
res/layout/
├── activity_main.xml        DELETED
├── app_bar_main.xml         DELETED
├── content_main.xml         DELETED
├── fragment_home.xml        DELETED
├── fragment_pager.xml       DELETED
├── fragment_apps.xml        DELETED
├── fragment_actions.xml     DELETED
├── item_apps.xml            DELETED
├── item_home.xml            DELETED
├── item_action.xml          DELETED
├── item_action_picker.xml   DELETED
├── multiselect_action_view.xml DELETED
├── dialog_progress.xml      DELETED
└── dialog_input.xml         DELETED
```


---

### Phase 6: Material 3 Expressive Polish (2-3 days)
**Goal: Apply M3 Expressive-specific enhancements**

#### 6.1 Component Updates
| Component | M3 Expressive Change |
|-----------|----------------------|
| Buttons | Use ButtonDefaults.elevatedButtonElevation() with increased default elevation |
| Cards | Updated default shape (16dp corners), updated elevation |
| FAB | Larger default size (56dp to 64dp for extended), updated shape |
| BottomNav | Updated to NavigationBar with new indicator animation |
| TopAppBar | Updated to TopAppBar with new scroll behavior |
| Dialogs | Updated shape and padding |
| Lists | Use ListItem with updated padding and shape |
| SearchBar | Use SearchBar/SearchActive with updated specs |
| Snackbar | Updated shape and behavior |

#### 6.2 Motion & Animation
Add M3 Expressive motion patterns:
```kotlin
// Updated easing
val EasingEmphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
val EasingStandard = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

// Animated visibility with M3 Expressive easing
AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(300, easing = EasingEmphasized)) +
            expandVertically(tween(300, easing = EasingEmphasized)),
    exit = fadeOut(tween(200, easing = EasingStandard)) +
           shrinkVertically(tween(200, easing = EasingStandard))
)
```

#### 6.3 Haptic Feedback
Add haptic feedback per M3 Expressive guidelines:
```kotlin
val hapticFeedback = LocalHapticFeedback.current

IconButton(onClick = {
    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
}) {
    Icon(Icons.Outlined.Sort, contentDescription = null)
}
```

#### 6.4 Dynamic Color Enhancements
Leverage M3 Expressive dynamic color:
```kotlin
val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val context = LocalContext.current
    DynamicThemeColorScheme(context, darkTheme)
} else {
    if (darkTheme) darkScheme else lightScheme
}

// M3 Expressive adds new surface roles
MaterialTheme(
    colorScheme = colorScheme,
    shapes = Shapes(...),
    typography = Typography(...)
)
```


---

## 5. Detailed File Migration Map

### 5.1 Files to CREATE (New)
```
ui/
├── components/
│   ├── AppImage.kt
│   ├── AppScaffold.kt
│   ├── AppTopAppBar.kt
│   ├── AppBottomNav.kt
│   ├── AppNavigationRail.kt
│   ├── AppFab.kt
│   ├── AppSearchBar.kt
│   ├── AppPullRefresh.kt
│   ├── AppDialog.kt
│   ├── EmptyState.kt
│   ├── AppListItem.kt
│   ├── HomeAppItem.kt
│   ├── ActionItem.kt
│   ├── AppPickerItem.kt
│   ├──ProgressDialog.kt
│   └── InputDialog.kt
├── navigation/
│   ├── NavGraph.kt
│   └── DeepLinkHandler.kt
├── settings/
│   └── SettingsScreen.kt
├── about/
│   └── AboutScreen.kt
├── home/
│   ├── HomeScreen.kt
│   ├── HomePagerScreen.kt
│   └── TriStateTagList.kt
├── apps/
│   ├── AppsScreen.kt
│   └── AppsItem.kt
└── actions/
    └── ActionsScreen.kt
```

### 5.2 Files to DELETE (Migrated)
```
app/src/main/kotlin/com/aistra/hail/ui/
├── main/
│   └── MainActivity.kt            # Migrated to ComponentActivity
├── main/
│   └── MainFragment.kt           # Base class no longer needed
├── home/
│   ├── HomeFragment.kt           # Migrated to HomeScreen
│   ├── PagerFragment.kt         # Migrated to HomePagerScreen
│   ├── HomeAdapter.kt           # FragmentStateAdapter no longer needed
│   └── PagerAdapter.kt          # FragmentStateAdapter no longer needed
├── apps/
│   ├── AppsFragment.kt          # Migrated to AppsScreen
│   └── AppsAdapter.kt           # ListAdapter replaced by LazyVerticalGrid
└── actions/
    ├── ActionsFragment.kt       # Migrated to ActionsScreen
    └── ActionsAdapter.kt        # Adapter replaced by LazyColumn
```

### 5.3 XML Files to DELETE
```
app/src/main/res/
├── layout/
│   ├── activity_main.xml
│   ├── app_bar_main.xml
│   ├── content_main.xml
│   ├── fragment_home.xml
│   ├── fragment_pager.xml
│   ├── fragment_apps.xml
│   ├── fragment_actions.xml
│   ├── item_apps.xml
│   ├── item_home.xml
│   ├── item_action.xml
│   ├── item_action_picker.xml
│   ├── multiselect_action_view.xml
│   ├── dialog_progress.xml
│   └── dialog_input.xml
├── menu/
│   ├── menu_home.xml
│   ├── menu_apps.xml
│   ├── menu_apps_action.xml
│   ├── menu_settings.xml
│   └── nav_main.xml
└── navigation/
    └── mobile_navigation.xml
```

### 5.4 Files to MODIFY
```
app/
├── build.gradle.kts                # Add/remove dependencies
├── gradle/libs.versions.toml       # Add/remove library versions
├── src/main/kotlin/com/aistra/hail/
│   ├── ui/
│   │   ├── theme/
│   │   │   ├── Theme.kt          # Add M3 Expressive shapes
│   │   │   └── Type.kt           # Update typography scale
│   │   └── main/
│   │       └── MainActivity.kt    # Convert to ComponentActivity
│   └── app/
│       └── HailData.kt           # May need ViewModel integration
```


---

## 6. Decision Matrix — Should We Proceed?

### GREEN LIGHT if ALL of the following are true:
1. The team has bandwidth for a 2-3 week focused migration (not a 1-day task)
2. The app does NOT need deep link framework support (manual implementation is acceptable)
3. All screens are ready to be converted to Compose (or the team accepts the conversion cost)
4. minSdk can stay at 23 (it can — Compose supports 23)
5. compileSdk is 36+ (it is — 37)
6. The team accepts that this is an atomic, all-or-nothing migration — no incremental rollout
7. The team is comfortable with the Compose + M3 Expressive learning curve
8. The app does not depend on third-party Views that cannot be embedded in Compose

### STOP / RETHINK if ANY of the following are true:
1. Deep links are critical and must be framework-managed — Nav3 has no answer for this yet
2. The app heavily relies on nested graph scoped ViewModels — this is the roughest edge in Nav3
3. Fragments/Views are used for non-negotiable reasons (e.g., third-party SDK views that cannot be embedded in Compose)
4. The team is mid-sprint with hard deadlines — this will break the build for days
5. Shared destinations across back stacks are needed — not supported
6. The app uses custom View subclasses that cannot be easily wrapped in AndroidView

---

## 7. Recommended Order of Work

```
Phase 0: Setup (2-3 days)
├── Add Nav3 + Compose + M3 Expressive dependencies
├── Define all route data classes
├── Create NavigationState + Navigator boilerplate
├── Create shared components (AppTopAppBar, AppBottomNav, etc.)
└── Verify project compiles (no Nav2 removed yet)

Phase 1: Easiest Screens First (1 day)
├── AboutFragment to AboutScreen (already Compose!)
├── SettingsFragment to SettingsScreen (already Compose!)
└── Extract TriStateTagList composable
├── Verify bottom nav wiring works

Phase 2: Medium Screens (3-4 days)
├── ActionsFragment to ActionsScreen (dialog-heavy, but flat layout)
└── Verify all navigation actions work

Phase 3: Hard Screens (3-5 days)
├── HomeFragment + PagerFragment to HomeScreen + HomePagerScreen
│   ├── Replace ViewPager2 with Compose Pager
│   ├── Replace RecyclerView + GridManager with LazyVerticalGrid
│   ├── Replace MenuProvider with TopAppBar actions
│   ├── Replace context menus with DropdownMenu
│   └── Replace OnBackPressedCallback with BackHandler
└── AppsFragment to AppsScreen (same replacements)

Phase 4: Activity + Polish (2-3 days)
├── Replace MainActivity with ComponentActivity
├── Remove ALL Nav2 dependencies
├── Implement manual deep link handler
├── Fix entryDecorators order (saveable state first!)
├── Test predictive back gestures
└── Full regression test

Phase 6: M3 Expressive Polish (2-3 days)
├── Apply updated shapes
├── Add expressive motion and animations
├── Add haptic feedback
└── Update all components to M3 Expressive specs
```

---

## 8. Checklist Before Merge

- [ ] All 5 screens render correctly as composables
- [ ] Bottom nav / nav rail selection state is correct
- [ ] Back button works on every screen (including nested back stacks per tab)
- [ ] Predictive back gesture animates correctly
- [ ] rememberSaveable works inside all screens (scroll positions persist)
- [ ] ViewModels are scoped correctly to entries
- [ ] All dialogs render correctly (not full-screen)
- [ ] Deep links are manually handled (or confirmed not needed)
- [ ] ApiActivity still works (unchanged)
- [ ] No androidx.navigation imports remain
- [ ] No mobile_navigation.xml or other Nav2 XML files remain
- [ ] No Fragment, NavHostFragment,NavController references in main source
- [ ] lifecycle-viewmodel-navigation3 is at a stable version (not alpha)
- [ ] entryDecorators list includes rememberSaveableStateHolderNavEntryDecorator() FIRST
- [ ] All XML layouts deleted
- [ ] All menus converted to Compose
- [ ] All View-based adapters removed
- [ ] Material 3 Expressive shapes applied
- [ ] Dynamic color theming works
- [ ] Coil image loading works correctly
- [ ] System bars colors set correctly
- [ ] No lint errors
- [ ] No typecheck errors

---

## 9. References

- [Official Nav3 Overview](https://developer.android.com/guide/navigation/navigation-3)
- [Official Migration Guide](https://developer.android.com/guide/navigation/navigation-3/migration-guide)
- [Nav3 Recipes Repo](https://github.com/android/nav3-recipes)
- [Nav3 Release Notes](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Stable Announcement (Nov 2025)](https://android-developers.googleblog.com/2025/11/jetpack-navigation-3-is-stable.html)
- [Real-World Migration Cost Analysis](https://medium.com/@sshameelakhtar/navigation-3-is-stable-heres-what-the-migration-actually-costs-6a6a297e81d4)
- [Material 3 Expressive Guide](https://developer.android.com/jetpack/androidx/releases/material3)
- [Coil Image Loading](https://coil-kt.github.io/coil/)
- [Accompanist Pager](https://github.com/google/accompanist)

---

## 10. AI Agent Instructions

**Before doing ANY work on this migration:**

1. Read this entire document.
2. Verify all conditions in the Decision Matrix (Section 6) are met — every hard blocker must be resolved.
3. Check the Recommended Order (Section 7) — if any condition is false, STOP and inform the user.
4. If all green, proceed in the Recommended Order, one phase at a time.
5. Do NOT skip the entryDecorators order requirement — it silently breaks rememberSaveable.
6. Do NOT use alpha versions of lifecycle-viewmodel-navigation3.
7. Do NOT attempt incremental migration — Nav3 requires an atomic change.
8. Keep ApiActivity unchanged — it is not part of in-app navigation.
9. Verify all Compose APIs used are available in the current Compose BOM version.
10. Run lint and typecheck after each phase.

