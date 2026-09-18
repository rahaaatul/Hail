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

## File Changes

### 1. Update AppsFragment to Use ComposeView
#### Task 1: Update AppsFragment.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsFragment.kt
- [ ] Change constructor to use Fragment(R.layout.fragment_apps) (placeholder until XML removed)
- [ ] Replace onCreateView implementation to return ComposeView(requireContext()).apply {
    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    setContent { HailTheme { AppsScreen(viewModel = viewModel, ...) } }
  }
- [ ] Remove ViewBinding related code (binding variable, _binding references)

### 2. Create AppsScreen Composable
#### Task 2: Create AppsScreen.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppsScreen.kt
- [ ] Implement @Composable fun AppsScreen(viewModel: AppsViewModel, modifier: Modifier = Modifier)
- [ ] Collect viewModel.uiState.collectAsStateWithLifecycle() and individual StateFlow properties
- [ ] Implement Column layout with padding(16.dp) and fillMaxSize()
- [ ] Add AppSearchBar, Spacer(height=8.dp), AppFilterChipRow, Spacer(height=8.dp), AppGrid

### 3. Create Supporting Composables
#### Task 3: Create AppSearchBar.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppSearchBar.kt
- [ ] Implement @Composable fun AppSearchBar(value: String, onQueryChanged: (String) -> Unit, isMultiSelect: Boolean, modifier: Modifier = Modifier)
- [ ] Use Row layout with Icons.Default.Search, Spacer(width=8.dp), OutlinedTextField
- [ ] Show IconButton with Icons.Default.Close when isMultiSelect is true
- [ ] Set OutlinedTextField isEnabled = !isMultiSelect

#### Task 4: Create AppFilterChipRow.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppFilterChipRow.kt
- [ ] Implement @Composable fun AppFilterChipRow(filters: List<AppFilter>, selectedFilter: AppFilter, onFilterSelected: (AppFilter) -> Unit, modifier: Modifier = Modifier)
- [ ] Use HorizontalScrollView with Row containing FilterChip items
- [ ] Use Arrangement.spacedBy(8.dp) and padding(vertical = 4.dp) on Row
- [ ] Implement FilterChip with selected = (filter == selectedFilter) and onClick = { onFilterSelected(filter) }

#### Task 5: Create AppGrid.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt
- [ ] Implement @Composable fun AppGrid(apps: List<AppInfo>, onAppClicked: (AppInfo) -> Unit, onAppLongClicked: (AppInfo) -> Unit, isMultiSelect: Boolean, selectedApps: Set<String>, modifier: Modifier = Modifier)
- [ ] Use LazyVerticalGrid with columns = GridCells.Fixed(3) and modifier.fillMaxWidth().padding(8.dp)
- [ ] Use items(apps) { app -> AppItem(...) } with appropriate parameters

#### Task 6: Create AppItem.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppItem.kt
- [ ] Implement @Composable fun AppItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isSelected: Boolean, isMultiSelect: Boolean, modifier: Modifier = Modifier)
- [ ] Use MutableInteractionSource() and collectIsPressedAsState() for pressed state
- [ ] Use Column modifier with size(72.dp), background based on selected/pressed state, clickable, longClickable, interactionSource, padding(8.dp), align(Alignment.Center)
- [ ] Add AppIcon with request = AppIconRequest(packageName = app.packageName, userId = app.userId), contentDescription = app.label, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
- [ ] Add Spacer(height=4.dp) and Text with app.label, maxLines=1, overflow=TextOverflow.Ellipsis, style=bodyMedium, align=Alignment.CenterHorizontally
- [ ] When isMultiSelect, add Box(align=Alignment.TopEnd, size=20.dp) with Checkbox(checked=isSelected, onChange={/* handled by toggleable */})

### 4. Update AppsViewModel to Expose StateFlow
#### Task 7: Update AppsViewModel.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow for uiState, apps, query, selectedFilter, isMultiSelect, selectedApps
- [ ] Expose StateFlow properties via _uiState.map { it.property } or direct flows
- [ ] Implement AppsUiState data class with apps, query, selectedFilter, isMultiSelect, selectedApps fields
- [ ] Update all methods to update _uiState instead of individual LiveData objects
- [ ] Ensure proper initialization and state update logic

### 5. Remove XML Layout and Adapter
#### Task 8: Remove fragment_apps.xml
- [ ] Delete app/src/main/res/layout/fragment_apps.xml

#### Task 9: Remove AppsAdapter.kt
- [ ] Delete app/src/main/java/com/aistra/hail/ui/apps/AppsAdapter.kt
- [ ] Verify no remaining references through compiler errors

## Validation Checklist

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