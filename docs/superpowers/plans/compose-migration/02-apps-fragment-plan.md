# Hail App Compose Migration Plan: AppsFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate AppsFragment from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyVerticalGrid, preserving all functionality: app grid display, search, filtering, context menu, multi-select, and icon loading via Coil.

**Architecture:** 
- Replace `fragment_apps.xml` with a `ComposeView` in `AppsFragment.onCreateView`
- Create `@Composable AppsScreen` that hosts the UI
- Break down into smaller composables: `AppSearchBar`, `AppFilterChipRow`, `AppGrid`, `AppContextMenu`, `AppMultiSelectToolbar`
- Use `rememberSaveable` for UI state (search text, selected filter, grid columns, multi-select state)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for app list and filtering results
- Replace `AppsAdapter` with `LazyVerticalGrid` items
- Replace `AppIconCache` with `AppIcon` composable from theme
- Preserve `AppsViewModel` (no changes needed) but expose `StateFlow` for UI

**Tech Stack:**
- Jetpack Compose, Material3
- Coil for image loading (via AppIcon composable)
- Accompanist Material3 Ripple (if needed) or Material3 built-in
- ViewModel with StateFlow (unchanged)

**Spec:** This plan is based on comprehensive codebase analysis of the AppsFragment and related components.

## Global Constraints
- Jetpack Compose BOM 2026.08.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- Navigation Component 2.10.0 (Fragment-based)
- Coil 2.6.0 for image loading
- AppsViewModel exposed as StateFlow for Compose integration

---
### Task 1: Update AppsFragment to Use ComposeView
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsFragment.kt`

**Steps:**
- [ ] Change constructor to use `Fragment(R.layout.fragment_apps)` (temporary until XML removed)
- [ ] Replace `onCreateView` implementation to return `ComposeView(requireContext()).apply {`
    `setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)`
    `setContent { HailTheme { AppsScreen(viewModel = viewModel, ...) } }`
  `}`
- [ ] Remove ViewBinding related code (binding variable, `_binding` references)

### Task 2: Create AppsScreen Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppsScreen.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppsScreen(viewModel: AppsViewModel, modifier: Modifier = Modifier)`
- [ ] Collect `viewModel.uiState.collectAsStateWithLifecycle()` and individual StateFlow properties
- [ ] Implement Column layout with `padding(16.dp)` and `fillMaxSize()`
- [ ] Add `AppSearchBar`, `Spacer(height=8.dp)`, `AppFilterChipRow`, `Spacer(height=8.dp)`, `AppGrid`

### Task 3: Create AppSearchBar Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppSearchBar.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppSearchBar(value: String, onQueryChanged: (String) -> Unit, isMultiSelect: Boolean, modifier: Modifier = Modifier)`
- [ ] Use `Row` layout with `Icons.Default.Search`, `Spacer(width=8.dp)`, `OutlinedTextField`
- [ ] Set `OutlinedTextField` label to `{ Text("Search apps") }`
- [ ] Set `OutlinedTextField isEnabled = !isMultiSelect`
- [ ] Show `IconButton` with `Icons.Default.Close` when `isMultiSelect` is true
- [ ] Add `onClick` to the close button to cancel multi-select (implementation detail)

### Task 4: Create AppFilterChipRow Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppFilterChipRow.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppFilterChipRow(filters: List<AppFilter>, selectedFilter: AppFilter, onFilterSelected: (AppFilter) -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `HorizontalScrollView` with `Row` containing `FilterChip` items
- [ ] Use `Arrangement.spacedBy(8.dp)` and `padding(vertical = 4.dp)` on the `Row`
- [ ] Implement `FilterChip` with `selected = (filter == selectedFilter)` and `onClick = { onFilterSelected(filter) }`
- [ ] Set `FilterChip` label to `{ Text(filter.label) }`

### Task 5: Create AppGrid Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppGrid(apps: List<AppInfo>, onAppClicked: (AppInfo) -> Unit, onAppLongClicked: (AppInfo) -> Unit, isMultiSelect: Boolean, selectedApps: Set<String>, modifier: Modifier = Modifier)`
- [ ] Use `LazyVerticalGrid` with `columns = GridCells.Fixed(3)`
- [ ] Use `modifier.fillMaxWidth().padding(8.dp)`
- [ ] Use `items(apps) { app -> AppItem(...) }` with appropriate parameters

### Task 6: Create AppItem Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppItem.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isSelected: Boolean, isMultiSelect: Boolean, modifier: Modifier = Modifier)`
- [ ] Use `MutableInteractionSource()` and `collectIsPressedAsState()` for pressed state
- [ ] Use `Column` modifier with:
    - `size(72.dp)`
    - `background` based on selected/pressed state (selected: primaryContainer, pressed: secondaryContainer, else: surfaceVariant)
    - `clickable(onClick = onClick)`
    - `longClickable(onLongClick = onLongClick)`
    - `interactionSource = interactionSource`
    - `padding(8.dp)`
    - `align(Alignment.Center)`
- [ ] Add `AppIcon` with:
    - `request = AppIconRequest(packageName = app.packageName, userId = app.userId)`
    - `contentDescription = app.label`
    - `modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)`
- [ ] Add `Spacer(modifier = Modifier.height(4.dp))`
- [ ] Add `Text` with:
    - `text = app.label`
    - `maxLines = 1`
    - `overflow = TextOverflow.Ellipsis`
    - `style = MaterialTheme.typography.bodyMedium`
    - `modifier = Modifier.align(Alignment.CenterHorizontally)`
- [ ] When `isMultiSelect`, add:
    - `Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp))`
    - `Checkbox` with:
        - `checked = isSelected`
        - `onChange = { /* handled by toggleable */ }`
        - `colors = CheckboxDefaults.colors(checkedColor = primary, uncheckedColor = onSurfaceVariant)`

### Task 7: Update AppsViewModel to Expose StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt`

**Steps:**
- [ ] Replace `MutableLiveData` with `MutableStateFlow` for:
    - `uiState` → `private val _uiState = MutableStateFlow(AppsUiState())`
    - `apps` → `val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }`
    - `query` → `val query: StateFlow<String> = _uiState.map { it.query }`
    - `selectedFilter` → `val selectedFilter: StateFlow<AppFilter> = _uiState.map { it.selectedFilter }`
    - `isMultiSelect` → `val isMultiSelect: StateFlow<Boolean> = _uiState.map { it.isMultiSelect }`
    - `selectedApps` → `val selectedApps: StateFlow<Set<String>> = _uiState.map { it.selectedApps }`
- [ ] Expose `uiState` as `val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()`
- [ ] Implement `AppsUiState` data class with:
    - `apps: List<AppInfo> = emptyList()`
    - `query: String = ""`
    - `selectedFilter: AppFilter = AllAppsFilter.INSTANCE`
    - `isMultiSelect: Boolean = false`
    - `selectedApps: Set<String> = emptySet()`
- [ ] Update `init { loadApps() }` and all methods (`updateQuery`, `selectFilter`, etc.) to update `_uiState` instead of individual `LiveData`
- [ ] Implement `private fun loadApps()` and `private fun filterApps()` with proper state updates using `_uiState.update { it.copy(...) }`

### Task 8: Remove AppsAdapter and XML Layout
**Files:**
- Delete: `app/src/main/res/layout/fragment_apps.xml`
- Delete: `app/src/main/java/com/aistra/hail/ui/apps/AppsAdapter.kt`

**Steps:**
- [ ] Delete the XML layout file
- [ ] Delete the Adapter file
- [ ] Verify no remaining references through compiler errors or IDE search

## Validation Checklist

After completing all tasks above:
- [ ] AppsFragment builds and displays app grid correctly
- [ ] Search functionality filters apps in real-time
- [ ] Filter chips work and persist selection
- [ ] App icons load via Coil with proper placeholders
- [ ] Long press enters multi-select mode
- [ ] Multi-select toolbar appears with action icons (when implemented)
- [ ] Single tap opens app details (preserve existing navigation)
- [ ] Context menu (3-dot menu) works for individual apps
- [ ] Grid adapts to orientation changes
- [ ] State survives process death (rememberSaveable)
- [ ] Accessibility: TalkBack reads app names and states
- [ ] Performance: Smooth scrolling with 100+ apps
- [ ] No memory leaks from ComposeView or ViewModel
- [ ] Existing unit tests for AppsViewModel still pass
- [ ] No references to AppsAdapter or fragment_apps.xml remain

## References
- [Compose LazyGrids](https://developer.android.com/jetpack/compose/lists/grids)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Material3 Filter Chips](https://m3.material.io/components/chips/usage#filter-chips)
- [Compose Multi-Selection Patterns](https://developer.android.com/jetpack/compose/gestures#selection)