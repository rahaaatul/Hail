# Hail App - Compose Migration Plan: Resource and ViewModel Migration

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
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/ComposeResources.kt
package com.aistra.hail.ui.theme

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.dimenResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.aistra.hail.R

object ComposeResources {
    // String resources
    val app_name by remember { stringResource(R.string.app_name) }
    val menu_home by remember { stringResource(R.string.menu_home) }
    val menu_actions by remember { stringResource(R.string.menu_actions) }
    val menu_apps by remember { stringResource(R.string.menu_apps) }
    val menu_settings by remember { stringResource(R.string.menu_settings) }
    val menu_about by remember { stringResource(R.string.menu_about) }
    val title_add_action by remember { stringResource(R.string.title_add_action) }
    val title_edit_action by remember { stringResource(R.string.title_edit_action) }
    val label_enabled by remember { stringResource(R.string.label_enabled) }
    // ... add all string resources needed in Compose

    // Dimension resources
    val fab_size by remember { Dp(dimenResource(R.dimen.fab_size)) }
    val app_icon_size by remember { Dp(dimenResource(R.dimen.app_icon_size)) }
    val list_item_height by remember { Dp(dimenResource(R.dimen.list_item_height)) }
    val horizontal_padding by remember { Dp(dimenResource(R.dimen.horizontal_padding)) }
    val vertical_padding by remember { Dp(dimenResource(R.dimen.vertical_padding)) }
    val button_height by remember { Dp(dimenResource(R.dimen.button_height)) }
    // ... add all dimension resources needed in Compose

    // Text styles (if not using Material3 typography)
    val titleTextStyle by remember {
        androidx.compose.ui.text.style.TextStyle(
            fontSize = sp(resource = R.dimen.title_text_size),
            fontWeight = FontWeight.Bold
        )
    }
    // ... add other text styles as needed
}
```

### 2. Create Material3 Color Scheme Extensions
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/ColorExtensions.kt
package com.aistra.hail.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.aistra.hail.R

/**
 * Extensions to get legacy color resources as Material3 colors
 * Useful for gradual migration
 */
private val ActivityContext: Context by remember {
    LocalContext.current
}

val ColorScheme.primaryLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.primary))

val ColorScheme.secondaryLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.secondary))

val ColorScheme.backgroundLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.background))

val ColorScheme.surfaceLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.surface))

val ColorScheme.errorLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.error))

val ColorScheme.onPrimaryLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_primary))

val ColorScheme.onSecondaryLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_secondary))

val ColorScheme.onBackgroundLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_background))

val ColorScheme.onSurfaceLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_surface))

val ColorScheme.onErrorLegacy: Color
    get() = Color(ContextCompat.getColor(ActivityContext, R.color.on_error))
```

### 3. Refactor ViewModels to Use StateFlow
```kotlin
// Before: AppsViewModel with LiveData
class AppsViewModel(...) : ViewModel() {
    private val _apps = MutableLiveData<List<AppInfo>>()
    val apps: LiveData<List<AppInfo>> = _apps
    private val _query = MutableLiveData<String>()
    val query: LiveData<String> = _query
    // ... etc.
}

// After: AppsViewModel with StateFlow
class AppsViewModel(
    private val repo: AppsRepository,
    private val hailData: HailData
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppsUiState())
    val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()

    // Individual state flows for fine-grained recomposition
    val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }
    val query: StateFlow<String> = _uiState.map { it.query }
    val selectedFilter: StateFlow<AppFilter> = _uiState.map { it.selectedFilter }
    val isMultiSelect: StateFlow<Boolean> = _uiState.map { it.isMultiSelect }
    val selectedApps: StateFlow<Set<String>> = _uiState.map { it.selectedApps }

    init {
        // Load initial state
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            try {
                val apps = repo.getApps()
                _uiState.update { it.copy(apps = apps) }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateQuery(query: String) {
        _uiState.update { it.copy(query = query) }
        // Trigger filtering
        filterApps()
    }

    fun selectFilter(filter: AppFilter) {
        _uiState.update { it.copy(selectedFilter = filter) }
        filterApps()
    }

    private fun filterApps() {
        val state = _uiState.value
        val filtered = state.apps.filter { app ->
            // Apply query and filter logic
            matchesQuery(app, state.query) && matchesFilter(app, state.selectedFilter)
        }
        _uiState.update { it.copy(apps = filtered) }
    }

    // ... other methods update _uiState accordingly
}

// Data class for UI state
data class AppsUiState(
    val apps: List<AppInfo> = emptyList(),
    val query: String = "",
    val selectedFilter: AppFilter = AllAppsFilter.INSTANCE,
    val isMultiSelect: Boolean = false,
    val selectedApps: Set<String> = emptySet()
)
```

### 4. Update All ViewFollow the Same Pattern
Apply the same StateFlow refactoring to:
- ActionsViewModel
- PagerViewModel
- HomeViewModel
- SettingsViewModel (if exists)
- Any other ViewModels in the app

### 5. Update Hilt Modules (if needed)
Ensure ViewModels are properly constructed with @Inject constructors
```kotlin
// app/src/main/java/com/aistra/hail/di/ViewModelModule.kt
@Module
@InstallIn(SingletonComponent::class)
object ViewModelModule {

    @Provides
    @ViewModelScoped
    fun provideAppsViewModel(
        repo: AppsRepository,
        hailData: HailData
    ): AppsViewModel = AppsViewModel(repo, hailData)

    // ... other ViewModel providers
}
```

### 6. Remove Unused XML Resources (After Verification)
After confirming all Compose screens work:
```diff
// app/src/main/res/values/strings.xml
- <!-- Strings only used in XML layouts can be removed -->
- <!-- But keep those used in AndroidManifest, etc. -->

// app/src/main/res/values/colors.xml
- <!-- Colors only used in XML layouts can be removed if using Material3 -->

// app/src/main/res/values/dimens.xml
- <!-- Dimensions only used in XML layouts can be removed -->
```

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