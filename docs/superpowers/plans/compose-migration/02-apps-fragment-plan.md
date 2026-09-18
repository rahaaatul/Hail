# Hail App Compose Migration Plan: AppsFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate AppsFragment from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyVerticalGrid, preserving all functionality: app grid display, search, filtering, sort, select all, context menu, multi-select, swipe-to-refresh, and icon loading via Coil.

**Architecture:** 
- Replace `fragment_apps.xml` with a `ComposeView` in `AppsFragment.onCreateView`
- Create `@Composable AppsScreen` that hosts the UI
- Break down into smaller composables: `AppSearchBar`, `AppFilterChipRow`, `AppGrid`, `AppContextMenu`, `AppMultiSelectToolbar`, `AppSortMenu`
- Use `rememberSaveable` for UI state (search text, selected filter, grid columns, multi-select state)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for app list and filtering results (but note: selectedFilter, isMultiSelect, selectedApps are managed via rememberSaveable and HailData, not ViewModel)
- Replace `AppsAdapter` with `LazyVerticalGrid` items
- Replace `AppIconCache` with `AppIcon` composable from theme
- Preserve `AppsViewModel` (no changes needed) but expose `StateFlow` for app list and query only

**Tech Stack:**
- Jetpack Compose, Material3
- Coil for image loading (via AppIcon composable)
- Material3 Ripple (or Material3 built-in)
- Material3 SwipeToRefresh (for swipe-to-refresh)
- ViewModel with StateFlow (for app list and query)

**Spec:** This plan is based on comprehensive codebase analysis of the AppsFragment and related components, including sort functionality (options menu), select all action, context menu (long-press), and swipe-to-refresh.

## Global Constraints
- Jetpack Compose BOM 2026.08.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- Navigation Component 2.10.0 (Fragment-based)
- Coil 2.6.0 for image loading
- AppsViewModel exposed as StateFlow for Compose integration (app list and query only)

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

### Task 8: Create AppSortMenu Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppSortMenu.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppSortMenu(sortBy: SortBy, onSortByChanged: (SortBy) -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `DropdownMenu` with `ExpandedBox` containing radio items for each sort option (e.g., Alphabetical, Installation Date, Usage Frequency)
- [ ] Each radio item uses `RadioButton` and `Text` to display the sort option label
- [ ] Set `selected` based on `sortBy` parameter and call `onSortByChanged` when selected
- [ ] Integrate with AppsScreen to show in options menu (or toolbar menu) via `IconButton` with `Icons.Default.SortByAlpha` or similar

### Task 9: Update AppMultiSelectToolbar to Include Select All Action
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppMultiSelectToolbar.kt` (create if doesn't exist)

**Steps:**
- [ ] Create the file if not exists with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppMultiSelectToolbar(selectedCount: Int, totalCount: Int, onSelectAllClicked: () -> Unit, onClearSelectionClicked: () -> Unit, onCloseClicked: () -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `Row` with `Arrangement.spaceBetween` containing:
    - Text showing "{selectedCount} of {totalCount} selected"
    - `Row` with `Spacing` of 8.dp containing:
        - `IconButton` with `Icons.Default.SelectAll` and `onClick = onSelectAllClicked` (enabled when selectedCount < totalCount)
        - `IconButton` with `Icons.Default.ClearAll` and `onClick = onClearSelectionClicked`
        - `IconButton` with `Icons.Default.Close` and `onClick = onCloseClicked`
- [ ] Update AppsScreen to pass the new callbacks and state

### Task 10: Implement Context Menu (Long-Press on Items)
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppItem.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppContextMenu.kt`

**Steps:**
- [ ] Modify `AppItem` to accept an additional parameter: `onContextMenuClicked: (AppInfo) -> Unit`
- [ ] In `AppItem`, when `isMultiSelect` is false, show a context menu (e.g., `Popup` or `DropdownMenu`) on long press (already handled by `onLongClick` parameter)
- [ ] Create `AppContextMenu` composable that takes an `app: AppInfo` and `onDismiss: () -> Unit`
- [ ] Use `Popup` or `DropdownMenu` with options like "Open", "App info", "Uninstall", etc. (based on existing XML context menu)
- [ ] Update `AppItem` to call `onContextMenuClicked` when the context menu item is selected (or handle within AppItem)
- [ ] Update `AppGrid` to pass the context menu callback and handle it (e.g., show the popup)

### Task 11: Add Swipe-to-Refresh to App Grid
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppsScreen.kt`

**Steps:**
- [ ] Modify `AppGrid` to accept an `onRefresh: () -> Unit` and `isRefreshing: Boolean` parameter
- [ ] Wrap the `LazyVerticalGrid` in `androidx.compose.material3.SwipeToRefresh` with the `onRefresh` and `isRefreshing` parameters
- [ ] Update `AppsScreen` to collect a `refreshState` from ViewModel (or use rememberSaveable) and pass it to `AppGrid`
- [ ] Update `AppsViewModel` to expose a `refresh` function that triggers a reload of apps (if not already present) and update `isRefreshing` state
- [ ] Note: Since we corrected ViewModel strategy, we may need to add a `refresh` function in ViewModel that updates the apps StateFlow

### Task 12: Update AppsViewModel to Expose StateFlow for App List and Query Only
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt`

**Steps:**
- [ ] Replace `MutableLiveData` with `MutableStateFlow` for:
    - `uiState` → `private val _uiState = MutableStateFlow(AppsUiState())` but only for apps and query (selectedFilter, isMultiSelect, selectedApps removed)
    - `apps` → `val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }`
    - `query` → `val query: StateFlow<String> = _uiState.map { it.query }`
- [ ] Expose `uiState` as `val uiState: StateFlow<AppsUiState> = _uiState.asStateFlow()` (but note: AppsUiState will only contain apps and query)
- [ ] Implement `AppsUiState` data class with:
    - `apps: List<AppInfo> = emptyList()`
    - `query: String = ""`
    - (Removed: selectedFilter, isMultiSelect, selectedApps)
- [ ] Update `init { loadApps() }` and `updateQuery` method to update `_uiState` with only apps and query.
- [ ] Implement `private fun loadApps()` and `private fun filterApps()` with proper state updates using `_uiState.update { it.copy(...) }` (only updating apps and query).
- [ ] Note: selectedFilter, isMultiSelect, selectedApps are managed via rememberSaveable in Compose and synchronized with HailData via events (not in ViewModel).

### Task 13: Remove AppsAdapter and XML Layout
**Files:**
- Delete: `app/src/main/res/layout/fragment_apps.xml`
- Delete: `app/src/main/java/com/aistra/hail/ui/apps/AppsAdapter.kt`

**Steps:**
- [ ] Delete the XML layout file
- [ ] Delete the Adapter file
- [ ] Verify no remaining references through compiler errors or IDE search

### Task 14: Handle Additional Functionality
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsFragment.kt`
- Modify: `app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppScreen.kt` (AppsScreen)
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppItem.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppSearchBar.kt`

**Steps:**
- [ ] Implement duplicate filtering optimization using `lastAppsHash` and `lastQuery` to avoid unnecessary reloads
- [ ] Enhance select all action logic to check/uncheck all apps based on current filter and selection state
- [ ] Add reinstall action (`action_reinstall`) handling in context menu and multi-select toolbar
- [ ] Implement complex uninstall logic: device owner, profile owner, admin active checks before uninstall
- [ ] Add extract APK action using Storage Access Framework (`CreateDocument`) to save APK to user-selected location
- [ ] Implement nine-key search feature: modify SearchView input type to support nine-key modal (if applicable)
- [ ] Add app bar lift-on-scroll behavior for scrolling UI
- [ ] Apply default insetter applications to refresh and recycler view equivalents (padding adjustments)
- [ ] Enhance HailData integration: persist sort/filter selections, sync checked apps state with ViewModel via events
- [ ] Adapt ViewModel methods (`updateAppList()`, `updateDisplayAppList()`, `postQuery()`) to work with Compose state flows and trigger UI updates

## Validation Checklist

After completing all tasks above:
- [ ] AppsFragment builds and displays app grid correctly
- [ ] Search functionality filters apps in real-time
- [ ] Sort functionality works via options menu
- [ ] Filter chips work and persist selection
- [ ] App icons load via Coil with proper placeholders
- [ ] Long press enters multi-select mode
- [ ] Multi-select toolbar appears with action icons (when implemented)
- [ ] Select all action works in multi-select mode
- [ ] Single tap opens app details (preserve existing navigation)
- [ ] Toolbar context menu (3-dot menu) works for individual apps
- [ ] Long-press context menu works for individual apps
- [ ] Grid adapts to orientation changes
- [ ] State survives process death (rememberSaveable)
- [ ] Accessibility: TalkBack reads app names and states
- [ ] Performance: Smooth scrolling with 100+ apps
- [ ] Swipe-to-refresh reloads app list
- [ ] Duplicate filtering optimization prevents unnecessary reloads
- [ ] Select all action correctly checks/unchecks all apps based on current filter
- [ ] Reinstall action works from context menu and multi-select toolbar
- [ ] Complex uninstall logic respects device/profile/admin restrictions
- [ ] Extract APK action saves APK to user-selected location via Storage Access Framework
- [ ] Nine-key search feature functional (if applicable)
- [ ] App bar lift-on-scroll behavior works
- [ ] Default insetter applications applied correctly
- [ ] HailData integration persists sort/filter selections and syncs checked apps state
- [ ] ViewModel methods correctly trigger UI updates via state flows
- [ ] No memory leaks from ComposeView or ViewModel
- [ ] Existing unit tests for AppsViewModel still pass
- [ ] No references to AppsAdapter or fragment_apps.xml remain

## References
- [Compose LazyGrids](https://developer.android.com/jetpack/compose/lists/grids)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Material3 Filter Chips](https://m3.material.io/components/chips/usage#filter-chips)
- [Compose Multi-Selection Patterns](https://developer.android.com/jetpack/compose/gestures#selection)
- [Material3 DropdownMenu](https://m3.material.io/components/menus/usage)
- [Material3 SwipeToRefresh](https://developer.android.com/jetpack/compose/material3#swipe-to-refresh)
- [Material3 Icons](https://m3.material.io/components/icons/overview)