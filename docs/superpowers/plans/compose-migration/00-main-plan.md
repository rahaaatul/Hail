# Hail App Jetpack Compose Migration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate the Hail app from XML layouts and ViewBinding to Jetpack Compose while preserving the existing Navigation Component structure.

**Architecture:** Incremental screen-by-screen migration using ComposeView inside existing Fragments, keeping the Navigation Component XML nav graph intact. Each screen becomes a @Composable function rendered in a ComposeView within its Fragment container. Shared state (ViewModels, HailData, AppManager) remains unchanged.

**Tech Stack:**
- Jetpack Compose BOM 2026.08.00 (stable) / Material3 1.4.0
- Kotlin 2.4.20, AGP 9.4.0, compileSdk 37
- Navigation Component 2.10.0 (Fragment-based)
- Coil 2.6.0 for async image loading
- Room 2.6.0 (kept, DAOs extended with Flow queries)
- me.zhanghai.compose.preference 2.3.0 (already used in SettingsFragment)

**Spec:** This plan is based on comprehensive codebase analysis of all 50+ Kotlin files, 14 layout XMLs, 5 menu XMLs, and resource files.

## Global Constraints

- Preserve existing navigation: `mobile_navigation.xml` with 5 destinations (nav_home, nav_actions, nav_apps, nav_settings, nav_about) stays unchanged
- Preserve MainActivity: AppCompatActivity with BottomNavigationView, NavigationRailView, FAB, biometric auth - stays as-is
- Preserve Fragment containers: Each destination Fragment remains but its `onCreateView` returns a `ComposeView` instead of inflating XML
- Preserve business logic: HailData, AppManager, AppMetaCache, ActionsRepository, WorkManager services unchanged
- Material3 only: Use stable Material3 components (no Expressive/Material3 Expressive alpha)
- Coil for images: Replace custom AppIconCache with Coil's AsyncImage
- No breaking changes: Each screen migration must be independently testable; app must build and run after each screen

## Phase 1: AppIcon/Compose Theme Infrastructure

### Task 1: Add Coil Dependencies
- [ ] Update gradle/libs.versions.toml with coil and coil-compose versions
- [ ] Add implementation(libs.coil) and implementation(libs.coilCompose) to app/build.gradle.kts

### Task 2: Create AppIconRequest Data Class
- [ ] Create app/src/main/kotlin/com/aistra/hail/utils/AppIconRequest.kt
- [ ] Implement data class with packageName, userId, size, grayscale, synthesizeAdaptive, iconPack fields
- [ ] Make it implement ImageRequest.Data for Coil integration

### Task 3: Create AppIconDecoder
- [ ] Create AppIconDecoder class that extends ImageDecoder<AppIconRequest>
- [ ] Implement decode() method to reuse AppIconLoader logic
- [ ] Add SimpleImagePool.Closeable inner class for bitmap management

### Task 4: Configure ImageLoader in HailApp
- [ ] Modify app/src/main/kotlin/com/aistra/hail/HailApp.kt
- [ ] Add lazy-initialized imageLoader with AppIconDecoder.Factory()
- [ ] Configure memory and disk cache settings

### Task 5: Create Reusable AppIcon Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppIcon.kt
- [ ] Implement @Composable AppIcon function using rememberImagePainter
- [ ] Add Canvas wrapper with CircleShape for adaptive icons
- [ ] Handle contentDescription for accessibility

### Task 6: Update Theme.kt with Typography and Shapes
- [ ] Modify app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt
- [ ] Define DarkColorScheme and LightColorScheme with Material3 colors
- [ ] Update HailTheme composable to use MaterialTheme with typography and shapes
- [ ] Add Typography and Shapes constants with appropriate values

### Task 7: Remove AppIconCache.kt
- [ ] Delete app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt
- [ ] Verify no remaining references in codebase

### Task 8: Validate Infrastructure Changes
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Verify AppIcon composable displays icons correctly in preview
- [ ] Check theme colors and typography applied in light/dark mode
- [ ] Confirm AppIconCache.kt removal doesn't break existing functionality

## Phase 2: ApiActivity Migration

### Task 9: Update ApiActivity to Use Compose
- [ ] Modify app/src/main/java/com/aistra/hail/ApiActivity.kt
- [ ] Replace setContentView with setContent { HailTheme { ApiScreen() } }
- [ ] Remove activity_api.xml layout reference

### Task 10: Create ApiScreen Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ApiScreen.kt
- [ ] Implement scrolling text content using LazyColumn or VerticalScroller
- [ ] Use rememberSaveable for scroll position state
- [ ] Display formatted API documentation from string resources
- [ ] Preserve translucent theme and window flags

### Task 11: Validate ApiActivity Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test ApiActivity renders correctly in both light/dark theme
- [ ] Verify scrolling functionality works properly
- [ ] Confirm translucent background and window flags are preserved

## Phase 3: AppsFragment Migration

### Task 12: Update AppsFragment to Use ComposeView
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { AppsScreen(viewModel) }

### Task 13: Create AppsScreen Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppsScreen.kt
- [ ] Implement Column layout with AppSearchBar, AppFilterChipRow, and AppGrid
- [ ] Collect viewModel.uiState and individual StateFlow properties
- [ ] Pass appropriate callbacks for user interactions

### Task 14: Create AppSearchBar Composable
- [ ] Implement search text field with Icons.Default.Search
- [ ] Handle enabled state based on multi-select mode
- [ ] Show cancel icon during multi-select mode

### Task 15: Create AppFilterChipRow Composable
- [ ] Implement HorizontalScrollView with FilterChip items
- [ ] Map filter list to chips with selection callbacks
- [ ] Show selected filter with appropriate styling

### Task 16: Create AppGrid Composable
- [ ] Implement LazyVerticalGrid with GridCells.Fixed(3)
- [ ] Create AppGridItem composable for each app
- [ ] Handle click/long-click for selection and details navigation

### Task 17: Create AppItem Composable
- [ ] Implement app item with AppIcon, label, and selection indicators
- [ ] Use toggleable modifier for multi-select checkboxes
- [ ] Show checkbox overlay in top-right during multi-select
- [ ] Handle visual feedback for pressed/selected states

### Task 18: Update AppsViewModel to Expose StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt
- [ ] Replace LiveData with StateFlow and MutableStateFlow
- [ ] Expose individual StateFlow properties for fine-grained recomposition
- [ ] Ensure UI state updates trigger appropriate filtering

### Task 19: Remove AppsAdapter and XML Layout
- [ ] Delete app/src/main/res/layout/fragment_apps.xml
- [ ] Delete app/src/main/java/com/aistra/hail/ui/apps/AppsAdapter.kt
- [ ] Verify no remaining references in codebase

### Task 20: Validate AppsFragment Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test app grid displays correctly with proper spacing
- [ ] Verify search functionality filters apps in real-time
- [ ] Confirm filter chips work and persist selection
- [ ] Check app icons load via Coil with proper placeholders
- [ ] Test long press enters multi-select mode correctly
- [ ] Verify single tap opens app details (navigation preserved)
- [ ] Ensure state survives configuration changes
- [ ] Confirm TalkBack accessibility works for all elements

## Phase 4: ActionsFragment Migration

### Task 21: Update ActionsFragment to Use ComposeView
- [ ] Modify app/src/main/java/com/aistra/hail/ui/actions/ActionsFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { ActionsScreen(viewModel) }

### Task 22: Create ActionsScreen Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ActionsScreen.kt
- [ ] Implement Column layout with ActionHeader, LazyColumn, and ActionDialogs
- [ ] Collect viewModel.uiState and individual StateFlow properties
- [ ] Handle add/edit action dialog visibility states

### Task 23: Create ActionHeader Composable
- [ ] Implement Row with title text and Add Action IconButton
- [ ] Use IconButton with Icons.Default.Add for FAB alternative
- [ ] Apply proper padding and height specifications

### Task 24: Create ActionItem Composable
- [ ] Implement Row layout with app icon, details, and toggle switch
- [ ] Use RippleIndicator for click feedback
- [ ] Show enabled/disabled state with Switch composable
- [ ] Add edit/delete IconButton actions at end
- [ ] Handle visual feedback for pressed states

### Task 25: Create AddActionFab Composable
- [ ] Implement FloatingActionButton with Icons.Default.Add
- [ ] Position at bottom end with appropriate padding
- [ ] Handle click callback to show add action dialog

### Task 26: Create ActionDialog Composable
- [ ] Implement AlertDialog for add/edit action forms
- [ ] Use rememberSaveable for form field state (label, description, enabled)
- [ ] Include app picker button to launch separate dialog
- [ ] Validate form before saving (non-empty label required)
- [ ] Handle save/cancel actions with ViewModel updates

### Task 27: Update ActionsViewModel to Expose StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/actions/ActionsViewModel.kt
- [ ] Replace LiveData with StateFlow and MutableStateFlow
- [ ] Expose individual StateFlow properties for actions list and UI states
- [ ] Ensure proper state updates for loading, dialogs, and editing states

### Task 28: Remove ActionsAdapter and XML Layout
- [ ] Delete app/src/main/res/layout/fragment_actions.xml
- [ ] Delete app/src/main/java/com/aistra/hail/ui/actions/ActionsAdapter.kt
- [ ] Verify no remaining references in codebase

### Task 29: Validate ActionsFragment Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test action list displays correctly with icons and descriptions
- [ ] Verify toggle switch updates action enabled state via ViewModel
- [ ] Confirm FAB opens add action dialog properly
- [ ] Check add/edit dialogs capture all required fields
- [ ] Test save button creates/updates action via ViewModel
- [ ] Verify delete button removes action via ViewModel
- [ ] Ensure dialogs animate in/out correctly
- [ ] Confirm state survives configuration changes
- [ ] Validate TalkBack accessibility for all action items and controls

## Phase 5: PagerFragment Migration

### Task 30: Update PagerFragment to Use ComposeView
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/PagerFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { PagerScreen(viewModel, tabType) }

### Task 31: Create PagerScreen Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/PagerScreen.kt
- [ ] Implement Column layout with PagerHeader, AppGrid, and dialogs
- [ ] Handle tab-specific header actions (edit tags for custom tabs)
- [ ] Show loading indicator when appropriate
- [ ] Implement multi-select toolbar appearance logic

### Task 32: Create PagerHeader Composable
- [ ] Implement Row with title text and conditional edit tag IconButton
- [ ] Show edit tag button only for custom tabs (not all/frequent/recent)
- [ ] Apply proper padding and height specifications

### Task 33: Create AppGrid Composable (with tag badges)
- [ ] Implement LazyVerticalGrid with GridCells.Fixed(3)
- [ ] Create AppGridItem composable with tag badge support
- [ ] Handle click/long-click for selection and details navigation
- [ ] Show tag badges for apps in custom tabs only

### Task 34: Create AppGridItem Composable
- [ ] Implement app item with AppIcon, label, and conditional tag badge
- [ ] Use Box with Alignment.TopEnd for tag badge positioning
- [ ] Show checkbox overlay in top-right during multi-select mode
- [ ] Handle visual feedback for pressed/selected states

### Task 35: Create TagChip Composable
- [ ] Implement FilterChip or plain Chip with delete functionality
- [ ] Show tag label with optional delete action
- [ ] Handle tag removal from app via callback
- [ ] Apply appropriate styling for small chip appearance

### Task 36: Create MultiSelectToolbar Composable
- [ ] Implement Surface with selected count and action buttons
- [ ] Show "$selectedCount selected" text with bodyLarge style
- [ ] Implement "Add to tag" button that opens tag selection dialog
- [ ] Show cancel IconButton to exit multi-select mode
- [ ] Disable actions when no items selected

### Task 37: Create TagEditDialog Composable
- [ ] Implement AlertDialog for renaming/deleting custom tabs
- [ ] Use rememberSaveable for tag name input state
- [ ] Validate input (non-empty) before allowing save
- [ ] Show error message for empty tag name
- [ ] Handle save/delete actions with ViewModel updates
- [ ] Only show delete button for custom tabs

### Task 38: Update PagerViewModel to Expose StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt
- [ ] Replace LiveData with StateFlow and MutableStateFlow
- [ ] Expose individual StateFlow properties for apps, tags, and UI states
- [ ] Ensure proper state updates for loading, multi-select, and dialog states

### Task 39: Remove PagerAdapter and XML Layout
- [ ] Delete app/src/main/res/layout/fragment_pager.xml
- [ ] Delete app/src/main/java/com/aistra/hail/ui/home/PagerAdapter.kt
- [ ] Verify no remaining references in codebase

### Task 40: Validate PagerFragment Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test each tab type (all, frequent, recent, custom) displays correctly
- [ ] Verify all apps tab shows all applications
- [ ] Check frequent tab shows most used apps (based on usage stats)
- [ ] Verify recent tab shows recently used apps
- [ ] Confirm custom tabs show apps assigned to that tag
- [ ] Test app icons load via Coil with proper placeholders
- [ ] Verify long press enters multi-select mode correctly
- [ ] Check multi-select toolbar appears with "Add to tag" action
- [ ] Confirm tag chips show assigned tags for apps in custom tabs
- [ ] Test tag edit dialog allows renaming and deleting custom tabs
- [ ] Verify header shows appropriate title for each tab
- [ ] Ensure state survives configuration changes
- [ ] Confirm TalkBack accessibility reads app names, tag info, and selection state
- [ ] Verify smooth scrolling performance with 100+ apps
- [ ] Ensure tab switching in HomeFragment continues to work

## Phase 6: HomeFragment Migration

### Task 41: Update HomeFragment to Use ComposeView
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/HomeFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { HomeScreen(viewModel) }

### Task 42: Create HomeScreen Composable
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/HomeScreen.kt
- [ ] Implement Column layout with TabRow and HorizontalPager
- [ ] Collect tab titles and visibility from viewModel StateFlow
- [ ] Map each tab to a PagerScreen composable (or inline logic)
- [ ] Add FAB for adding apps to custom tab
- [ ] Handle navigation to other destinations via Navigation Component

### Task 43: Create TabRow Implementation
- [ ] Implement TabRow with Tab components for each tab
- [ ] Use selected tab index from viewModel StateFlow
- [ ] Handle tab selection callbacks to update viewModel
- [ ] Apply proper styling and indicators for selected tab

### Task 44: Create HorizontalPager Implementation
- [ ] Implement HorizontalPager from Accompanist Material3 0.37.2
- [ ] Set page count to match tab count
- [ ] Handle page change callbacks to sync with TabRow selection
- [ ] Each page contains a PagerScreen for the corresponding tab type
- [ ] Apply proper paging behavior and indicators

### Task 45: Create FAB for Custom Tab Creation
- [ ] Implement FloatingActionButton with Icons.Default.Add
- [ ] Position at bottom end with appropriate padding
- [ ] Handle click callback to show custom tab creation dialog
- [ ] Only show FAB when appropriate (based on viewModel state)

### Task 46: Update HomeViewModel to Expose StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/HomeViewModel.kt
- [ ] Replace LiveData with StateFlow and MutableStateFlow
- [ ] Expose individual StateFlow properties for tab titles and visibility
- [ ] Ensure proper state updates for tab management operations

### Task 47: Validate HomeFragment Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Test tab navigation works correctly via TabRow taps
- [ ] Verify horizontal paging works via swipe gestures
- [ ] Check TabRow and HorizontalPager stay in sync
- [ ] Confirm each tab displays correct content via PagerScreen
- [ ] Test FAB opens custom tab creation dialog properly
- [ ] Verify navigation to other destinations (Apps, Actions, etc.) works
- [ ] Ensure state survives configuration changes
- [ ] Confirm TalkBack accessibility works for all tabs and controls
- [ ] Test performance with multiple tabs containing many apps

## Phase 7: Testing and CI/CD Infrastructure

### Task 48: Add Compose Testing Dependencies
- [ ] Update gradle/libs.versions.toml with composeUiTest, androidXTestCore, etc.
- [ ] Add debugImplementation and androidTestImplementation lines to app/build.gradle.kts
- [ ] Add androidTestImplementation("androidx.test.ext:junit") for JUnit runner

### Task 49: Update Build Configuration for Compose Tests
- [ ] Modify app/build.gradle.kts android { buildFeatures { compose = true } }
- [ ] Set composeOptions { kotlinCompilerExtensionVersion = composeVersions.composeCompiler.toString() }
- [ ] Add testOptions { unitTests { includeAndroidResources = true } }

### Task 50: Create Compose Test Base Class
- [ ] Create src/androidTest/java/com/aistra/hail/ui/theme/ComposeTest.kt
- [ ] Implement abstract class with @get:Rule composeTestRule = createComposeRule()
- [ ] Use AndroidJUnit4 test runner

### Task 51: Create Example Compose Test for AppsScreen
- [ ] Create src/androidTest/java/com/aistra/hail/ui/theme/AppsScreenTest.kt
- [ ] Use @HiltAndroidTest and @UninstallModules for ViewModel mocking
- [ ] Implement test methods to verify app display and search filtering
- [ ] Use composeTestRule.onNodeWithText().assertIsDisplayed() assertions
- [ ] Test multi-select mode toggling and selection persistence

### Task 52: Update CI/CD Workflow (GitHub Actions)
- [ ] Modify .github/workflows/android-ci.yml
- [ ] Add instrumented tests step using reactivecircus/android-emulator-runner@v2
- [ ] Configure emulator with API 33, google_apis target, x86_64 arch
- [ ] Set up script to run ./gradlew connectedAndroidTest

### Task 53: Update Lint Rules for Compose
- [ ] Add Compose lint dependencies to app/build.gradle.kts
- [ ] implement("androidx.compose.compiler:compiler:1.6.8")
- [ ] debugImplement("androidx.compose.ui:ui-tooling:1.6.8")
- [ ] debugImplement("androidx.compose.ui:ui-tooling-preview:1.6.8")

### Task 54: Validate Testing Infrastructure
- [ ] Run ./gradlew assembleDebug to ensure successful build with test deps
- [ ] Execute ./gradlew test to verify unit tests still pass
- [ ] Run ./gradlew lint to check for Compose-related issues
- [ ] Execute connected Android tests on emulator/device
- [ ] Verify Compose UI tests compile and run successfully
- [ ] Check that existing unit tests show no regression
- [ ] Confirm CI pipeline runs connectedAndroidTest on PRs
- [ ] Ensure tests run in reasonable time (<5 minutes for UI tests)

## Phase 8: Resource and ViewModel Migration

### Task 55: Create Compose Resource Object
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ComposeResources.kt
- [ ] Implement object with stringResource and dimenResource delegates
- [ ] Add commonly used string resources (app_name, menu_*, title_*, label_*)
- [ ] Add commonly used dimension resources (fab_size, app_icon_size, etc.)
- [ ] Include text styles if not using Material3 typography

### Task 56: Create Material3 Color Scheme Extensions
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ColorExtensions.kt
- [ ] Implement private val ActivityContext: Context by remember { LocalContext.current }
- [ ] Add ColorScheme extensions for legacy colors (primary, secondary, etc.)
- [ ] Map each to ContextCompat.getColor(ActivityContext, R.color.*)

### Task 57: Refactor AppsViewModel to Use StateFlow
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow
- [ ] Expose StateFlow properties via _uiState.map { it.property }
- [ ] Implement AppsUiState data class for cohesive state object
- [ ] Update all methods to update _uiState instead of individual LiveData

### Task 58: Refactor ActionsViewModel to Use StateFlow
- [ ] Apply same StateFlow refactoring pattern to ActionsViewModel
- [ ] Create ActionsUiState data class
- [ ] Expose individual StateFlow properties for fine-grained recomposition
- [ ] Update all state modification methods

### Task 59: Refactor PagerViewModel to Use StateFlow
- [ ] Apply same StateFlow refactoring pattern to PagerViewModel
- [ ] Create PagerUiState data class
- [ ] Expose individual StateFlow properties
- [ ] Update all state modification methods

### Task 60: Refactor HomeViewModel to Use StateFlow
- [ ] Apply same StateFlow refactoring pattern to HomeViewModel
- [ ] Create HomeUiState data class
- [ ] Expose individual StateFlow properties
- [ ] Update all state modification methods

### Task 61: Update Hilt Modules (if needed)
- [ ] Verify ViewModels have @Inject constructors in ViewModelModule
- [ ] Ensure proper scoping (@ViewModelScoped) for ViewModel providers
- [ ] Add any missing ViewModel providers if needed

### Task 62: Remove Unused XML Resources (After Verification)
- [ ] After confirming all Compose screens work:
- [ ] Review strings.xml for strings only used in XML layouts
- [ ] Review colors.xml for colors only used in XML layouts
- [ ] Review dimens.xml for dimensions only used in XML layouts
- [ ] Safely remove unused resources while preserving AndroidManifest needs

### Task 63: Validate Resource and ViewModel Migration
- [ ] Run ./gradlew assembleDebug to ensure successful build
- [ ] Verify Compose screens use ComposeResources for strings/dimensions
- [ ] Check Material3 color scheme extensions provide correct colors
- [ ] Confirm all ViewModels expose StateFlow for UI state
- [ ] Verify individual StateFlow properties enable fine-grained recomposition
- [ ] Ensure ViewModel initialization works with Hilt
- [ ] Run existing unit tests to confirm they pass with StateFlow ViewModels
- [ ] Verify UI tests collect StateFlow values correctly with collectAsStateWithLifecycle
- [ ] Check for memory leaks from StateFlow or ViewModels
- [ ] Confirm state survives configuration changes via ViewModel scoping
- [ ] Validate strings, dimensions, and values match original resources
- [ ] Ensure Material3 extensions match original color resources
- [ ] Confirm no hardcoded strings/dimensions/colors remain in Compose screens
- [ ] Run lint to verify passes with new resource usage
- [ ] Check APK size for no significant increase from resource refactoring

---