# Hail App Compose Migration Plan: Resource and ViewModel Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate remaining XML resources (strings, colors, dimensions) to Compose-friendly formats and convert ViewModels to use StateFlow/UIState patterns for seamless Compose integration.

**Architecture:** 
- Convert string resources to typed constants in an object or use rememberUpdatedState
- Migrate color resources to Material3 color scheme extensions
- Convert dimension resources to Dp values in a companion object
- Refactor ViewModels to expose UIState as StateFlow and individual properties as StateFlow for fine-grained recomposition
- Replace LiveData with StateFlow in ViewModels (where appropriate for Compose)
- Ensure all ViewModels are Hilt-compatible for easy mocking in tests
- Preserve existing business logic while improving Compose integration

**Tech Stack:**
- Kotlin data classes for UIState
- StateFlow and MutableStateFlow
- Hilt for ViewModel injection
- Compose State APIs (rememberUpdatedState, etc.)

**Spec:** This plan is based on standard Android resource management and ViewModel architecture patterns applied to Jetpack Compose migration.

## Global Constraints
- Jetpack Compose BOM 2026.08.00 (stable)
- Kotlin 2.4.20
- Material3 1.4.0 (stable)
- Hilt for dependency injection
- All ViewModels must be compatible with StateFlow for Compose integration

---
### Task 1: Create Compose Resource Object
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ComposeResources.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Create `object ComposeResources`
- [ ] Add string resource delegates:
    - `val app_name by remember { stringResource(R.string.app_name) }`
    - `val menu_home by remember { stringResource(R.string.menu_home) }`
    - `val menu_actions by remember { stringResource(R.string.menu_actions) }`
    - `val menu_apps by remember { stringResource(R.string.menu_apps) }`
    - `val menu_settings by remember { stringResource(R.string.menu_settings) }`
    - `val menu_about by remember { stringResource(R.string.menu_about) }`
    - `val title_add_action by remember { stringResource(R.string.title_add_action) }`
    - `val title_edit_action by remember { stringResource(R.string.title_edit_action) }`
    - `val label_enabled by remember { stringResource(R.string.label_enabled) }`
    - [Add all string resources needed in Compose]
- [ ] Add dimension resource delegates:
    - `val fab_size by remember { Dp(dimenResource(R.dimen.fab_size)) }`
    - `val app_icon_size by remember { Dp(dimenResource(R.dimen.app_icon_size)) }`
    - `val list_item_height by remember { Dp(dimenResource(R.dimen.list_item_height)) }`
    - `val horizontal_padding by remember { Dp(dimenResource(R.dimen.horizontal_padding)) }`
    - `val vertical_padding by remember { Dp(dimenResource(R.dimen.vertical_padding)) }`
    - `val button_height by remember { Dp(dimenResource(R.dimen.button_height)) }`
    - [Add all dimension resources needed in Compose]
- [ ] Add text style delegates if not using Material3 typography:
    - `val titleTextStyle by remember { TextStyle(fontSize = sp(resource = R.dimen.title_text_size), fontWeight = FontWeight.Bold) }`
    - [Add other text styles as needed]

### Task 2: Create Material3 Color Scheme Extensions
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ColorExtensions.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Add `private val ActivityContext: Context by remember { LocalContext.current }`
- [ ] Add ColorScheme extensions:
    - `val ColorScheme.primaryLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.primary))`
    - `val ColorScheme.secondaryLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.secondary))`
    - `val ColorScheme.backgroundLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.background))`
    - `val ColorScheme.surfaceLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.surface))`
    - `val ColorScheme.errorLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.error))`
    - `val ColorScheme.onPrimaryLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_primary))`
    - `val ColorScheme.onSecondaryLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_secondary))`
    - `val ColorScheme.onBackgroundLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_background))`
    - `val ColorScheme.onSurfaceLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_surface))`
    - `val ColorScheme.onErrorLegacy: Color`
        `get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_error))`

### Task 3: Refactor AppsViewModel to Use StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt`

**Steps:**
- [ ] Replace `MutableLiveData` with `MutableStateFlow`:
    - `private val _uiState = MutableStateFlow(AppsUiState())`
    - `val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()`
    - `val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }`
    - `val query: StateFlow<String> = _uiState.map { it.query }`
    - `val selectedFilter: StateFlow<AppFilter> = _uiState.map { it.selectedFilter }`
    - `val isMultiSelect: StateFlow<Boolean> = _uiState.map { it.isMultiSelect }`
    - `val selectedApps: StateFlow<Set<String>> = _uiState.map { it.selectedApps }`
- [ ] Implement `AppsUiState` data class with:
    - `apps: List<AppInfo> = emptyList()`
    - `query: String = ""`
    - `selectedFilter: AppFilter = AllAppsFilter.INSTANCE`
    - `isMultiSelect: Boolean = false`
    - `selectedApps: Set<String> = emptySet()`
- [ ] Update `init { loadApps() }` and all methods (`updateQuery`, `selectFilter`, etc.) to update `_uiState` instead of individual `LiveData`
- [ ] Implement `private fun loadApps()` and `private fun filterApps()` with proper state updates

### Task 4: Refactor ActionsViewModel to Use StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/actions/ActionsViewModel.kt`

**Steps:**
- [ ] Apply same StateFlow refactoring pattern to ActionsViewModel
- [ ] Create `ActionsUiState` data class
- [ ] Expose individual StateFlow properties for actions list and UI states
- [ ] Update all state modification methods

### Task 5: Refactor PagerViewModel to Use StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt`

**Steps:**
- [ ] Apply same StateFlow refactoring pattern to PagerViewModel
- [ ] Create `PagerUiState` data class
- [ ] Expose individual StateFlow properties for apps, tags, and UI states
- [ ] Update all state modification methods

### Task 6: Refactor HomeViewModel to Use StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/HomeViewModel.kt`

**Steps:**
- [ ] Apply same StateFlow refactoring pattern to HomeViewModel
- [ ] Create `HomeUiState` data class
- [ ] Expose individual StateFlow properties for tab titles, visibility, etc.
- [ ] Update all state modification methods

### Task 7: Update Hilt Modules (if needed)
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/di/ViewModelModule.kt` (or equivalent)

**Steps:**
- [ ] Verify ViewModels have `@Inject` constructors in `ViewModelModule`
- [ ] Ensure proper scoping (`@ViewModelScoped`) for ViewModel providers
- [ ] Add any missing ViewModel providers if needed
- [ ] Example: `@Provides @ViewModelScoped fun provideAppsViewModel(repo: AppsRepository, hailData: HailData): AppsViewModel = AppsViewModel(repo, hailData)`

### Task 8: Remove Unused XML Resources (After Verification)
**Files:**
- Delete: Unused resources in `app/src/main/res/values/` (strings.xml, colors.xml, dimens.xml)

**Steps:**
- [ ] After confirming all Compose screens work:
- [ ] Review `strings.xml` for strings only used in XML layouts (not referenced in Compose code)
- [ ] Safely remove unused strings while preserving those used in AndroidManifest, etc.
- [ ] Review `colors.xml` for colors only used in XML layouts
- [ ] Safely remove unused colors if using Material3 extensions exclusively
- [ ] Review `dimens.xml` for dimensions only used in XML layouts
- [ ] Safely remove unused dimensions if using Compose resources exclusively

## Validation Checklist

After completing all tasks above:
- [ ] App builds successfully with new resource objects
- [ ] Compose screens use ComposeResources for strings/dimensions
- [ ] Material3 color scheme extensions provide correct colors
- [ ] All ViewModels expose StateFlow for UI state
- [ ] Individual StateFlow properties enable fine-grained recomposition
- [ ] ViewModel initialization works with Hilt
- [ ] No LiveData remains in ViewModels (optional but recommended)
- [ ] Existing unit tests pass with StateFlow ViewModels
- [ ] UI tests collect StateFlow values correctly with collectAsStateWithLifecycle
- [ ] Memory usage: No leaks from StateFlow or ViewModels
- [ ] Configuration changes: State survives via ViewModel scoping
- [ ] Strings and dimensions: Values match original resources
- [ ] Colors: Material3 extensions match original color resources
- [ ] No hardcoded strings/dimensions/colors in Compose screens
- [ ] Lint passes with new resource usage
- [ ] APK size: No significant increase from resource refactoring

## References
- [StateFlow in ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel#stateflow)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Hilt ViewModel Injection](https://developer.android.com/training/dependency-injection/hilt-android#viewmodel-injection)
- [Material3 Color Scheme](https://m3.material.io/styles/color/the-color-system/color-system-role)
- [Android Resource Migration Guide](https://developer.android.com/guide/topics/resources/providing-resources#AlternativeResources)