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

## File Changes

### 1. Create Compose Resource Object
#### Task 1: Create ComposeResources.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ComposeResources.kt
- [ ] Implement package com.aistra.hail.ui.theme
- [ ] Create object ComposeResources
- [ ] Add string resource delegates: val app_name by remember { stringResource(R.string.app_name) }, etc.
- [ ] Add dimension resource delegates: val fab_size by remember { Dp(dimenResource(R.dimen.fab_size)) }, etc.
- [ ] Add text style delegates if not using Material3 typography: val titleTextStyle by remember { TextStyle(fontSize = sp(resource = R.dimen.title_text_size), fontWeight = FontWeight.Bold) }, etc.

### 2. Create Material3 Color Scheme Extensions
#### Task 2: Create ColorExtensions.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ColorExtensions.kt
- [ ] Implement package com.aistra.hail.ui.theme
- [ ] Add private val ActivityContext: Context by remember { LocalContext.current }
- [ ] Add ColorScheme extensions:
    - val ColorScheme.primaryLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.primary))
    - val ColorScheme.secondaryLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.secondary))
    - val ColorScheme.backgroundLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.background))
    - val ColorScheme.surfaceLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.surface))
    - val ColorScheme.errorLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.error))
    - val ColorScheme.onPrimaryLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_primary))
    - val ColorScheme.onSecondaryLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_secondary))
    - val ColorScheme.onBackgroundLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_background))
    - val ColorScheme.onSurfaceLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_surface))
    - val ColorScheme.onErrorLegacy: Color get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_error))

### 3. Refactor ViewModels to Use StateFlow
#### Task 3: Refactor AppsViewModel
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow:
    - private val _uiState = MutableStateFlow(AppsUiState())
    - val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()
    - val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }
    - val query: StateFlow<String> = _uiState.map { it.query }
    - val selectedFilter: StateFlow<AppFilter> = _uiState.map { it.selectedFilter }
    - val isMultiSelect: StateFlow<Boolean> = _uiState.map { it.isMultiSelect }
    - val selectedApps: StateFlow<Set<String>> = _uiState.map { it.selectedApps }
- [ ] Implement AppsUiState data class with apps, query, selectedFilter, isMultiSelect, selectedApps fields
- [ ] Update init { loadApps() } and all methods (updateQuery, selectFilter, etc.) to update _uiState instead of individual LiveData
- [ ] Implement private fun loadApps() and private fun filterApps() with proper state updates

#### Task 4: Refactor ActionsViewModel
- [ ] Apply same StateFlow refactoring pattern to ActionsViewModel
- [ ] Create ActionsUiState data class
- [ ] Expose individual StateFlow properties for actions list and UI states
- [ ] Update all state modification methods

#### Task 5: Refactor PagerViewModel
- [ ] Apply same StateFlow refactoring pattern to PagerViewModel
- [ ] Create PagerUiState data class
- [ ] Expose individual StateFlow properties for apps, tags, and UI states
- [ ] Update all state modification methods

#### Task 6: Refactor HomeViewModel
- [ ] Apply same StateFlow refactoring pattern to HomeViewModel
- [ ] Create HomeUiState data class
- [ ] Expose individual StateFlow properties for tab titles, visibility, etc.
- [ ] Update all state modification methods

### 4. Update Hilt Modules (if needed)
#### Task 7: Update ViewModelModule.kt
- [ ] Verify ViewModels have @Inject constructors in ViewModelModule
- [ ] Ensure proper scoping (@ViewModelScoped) for ViewModel providers
- [ ] Add any missing ViewModel providers if needed
- [ ] Example: @Provides @ViewModelScoped fun provideAppsViewModel(repo: AppsRepository, hailData: HailData): AppsViewModel = AppsViewModel(repo, hailData)

### 5. Remove Unused XML Resources (After Verification)
#### Task 8: Remove unused strings.xml resources
- [ ] After confirming all Compose screens work:
- [ ] Review strings.xml for strings only used in XML layouts (not referenced in Compose code)
- [ ] Safely remove unused strings while preserving those used in AndroidManifest, etc.

#### Task 9: Remove unused colors.xml resources
- [ ] Review colors.xml for colors only used in XML layouts
- [ ] Safely remove unused colors if using Material3 extensions exclusively

#### Task 10: Remove unused dimens.xml resources
- [ ] Review dimens.xml for dimensions only used in XML layouts
- [ ] Safely remove unused dimensions if using Compose resources exclusively

## Validation Checklist

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