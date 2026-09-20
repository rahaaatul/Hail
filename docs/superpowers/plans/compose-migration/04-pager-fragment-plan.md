# Hail App Compose Migration Plan: PagerFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate PagerFragment (which is actually a Fragment used as a page in HomeFragment's ViewPager2) from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyVerticalGrid, preserving all functionality: app grid with tabs, multi-select, tag management, and icon loading via AppIconLoader, and create a PagerViewModel to manage UI state.

**Architecture:** 
- Note: PagerFragment is not a top-level navigation destination but a child fragment of HomeFragment that displays app grids for each tab (All, Frequent, Recent, Custom tabs)
- Replace `fragment_pager.xml` with a `ComposeView` in `PagerFragment.onCreateView`
- Create `@Composable PagerScreen` that hosts the UI for a single tab
- Break down into smaller composables: `AppGridItem`, `TagChip`, `MultiSelectToolbar`, `TagEditDialog`
- Use `rememberSaveable` for UI state (selected apps, tag edit mode)
- Create `PagerViewModel` that exposes `StateFlow` for apps list, tags, query, multi-select state, etc.
- Use `StateFlow` from `PagerViewModel` collected with `collectAsState()` for UI state
- Replace `PagerAdapter` (which is actually a RecyclerView.Adapter) with `LazyVerticalGrid` items
- Replace `AppIconCache` with `AppIcon` composable from theme (which uses AppIconLoader)
- Note: The tab switching logic remains in HomeFragment (which will migrate separately)

**Tech Stack:**
- Jetpack Compose, Material3
- AppIconLoader 1.5.0 for image loading (via AppIcon composable)
- Material3 built-in alerts/dialogs
- ViewModel with StateFlow (unchanged)

**Spec:** This plan is based on comprehensive codebase analysis of the PagerFragment and related components.

## Global Constraints
- Jetpack Compose BOM 2026.09.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- Navigation Component 2.10.1 (Fragment-based)
- AppIconLoader 1.5.0 for image loading
- PagerViewModel exposed as StateFlow for Compose integration

---
## Tasks

### Task 1: Update PagerFragment to Use ComposeView
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/PagerFragment.kt`

**Steps:**
- [ ] Replace ViewBinding inflation with `ComposeView` in `onCreateView`
- [ ] Set `ViewCompositionStrategy` to `DisposeOnViewTreeLifecycleDestroyed`
- [ ] Set content to `HailTheme { PagerScreen(viewModel = viewModel, tabType = arguments?.getString("tabType") ?: "all", ...) }`

### Task 2: Create PagerScreen Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/PagerScreen.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun PagerScreen(viewModel: PagerViewModel, tabType: String, modifier: Modifier = Modifier)`
- [ ] Collect `viewModel.uiState.collectAsStateWithLifecycle()` and individual StateFlow properties
- [ ] Implement `Column` layout with `fillMaxSize()`
- [ ] Add tab-specific header (`PagerHeader`) with conditional tag management
- [ ] Add loading indicator when `uiState.isLoading` is true
- [ ] Add main content: `AppGrid` with appropriate callbacks
- [ ] Add multi-select toolbar when `isMultiSelect` is true
- [ ] Add tag edit dialog when `showTagEditDialog` is true and `tabType` is custom

### Task 3: Create PagerHeader Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/PagerHeader.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun PagerHeader(title: String, onEditText: (), onEditTagsClicked: () -> Unit = {}, canEditTags: Boolean = false, modifier: Modifier = Modifier)`
- [ ] Use `Row` layout with `fillMaxWidth()`, `padding(16.dp)`, `height(56.dp)`
- [ ] Add `Text` with `title`, `style=titleMedium`, `verticalAlignment=Alignment.CenterVertically`
- [ ] Add `Spacer` with `weight(1f)`
- [ ] Conditionally add `IconButton` with `onClick = onEditTagsClicked` and `Icons.Default.Edit` when `canEditTags` is true

### Task 4: Create AppGrid Composable (with tag badges)
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppGrid(apps: List<AppInfo>, onAppClicked: (AppInfo) -> Unit, onAppLongClicked: (AppInfo) -> Unit, isMultiSelect: Boolean, selectedApps: Set<String>, showTagBadge: Boolean = false, modifier: Modifier = Modifier)`
- [ ] Use `LazyVerticalGrid` with `columns = GridCells.Fixed(3)`
- [ ] Use `modifier.fillMaxWidth().padding(8.dp)`
- [ ] Use `items(apps) { app -> AppGridItem(...) }` with appropriate parameters

### Task 5: Create AppGridItem Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppGridItem.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppGridItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isSelected: Boolean, isMultiSelect: Boolean, tags: List<Tag>, showTagBadge: Boolean, modifier: Modifier = Modifier)`
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
- [ ] When `showTagBadge` and `tags.isNotEmpty()`, add:
    - `TagChip` with:
        - `text = tags.first().label`
        - `onDelete = { /* handle tag removal from app */ }`
        - `modifier = Modifier.align(Alignment.TopEnd)`
- [ ] Add `Spacer(height=4.dp)`
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

### Task 6: Create TagChip Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/TagChip.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun TagChip(text: String, onDelete: () -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `Chip` with:
    - `onDeleteRequest = onDelete`
    - `modifier = modifier`
- [ ] Add `Text(text)`

### Task 7: Create MultiSelectToolbar Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/MultiSelectToolbar.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun MultiSelectToolbar(selectedCount: Int, onTagSelected: (String) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `Surface` with `modifier.fillMaxWidth().height(56.dp)`
- [ ] Use `Row` with `fillMaxWidth()` and `padding(horizontal = 16.dp)`
- [ ] Add `Text` with:
    - `text = "$selectedCount selected"`
    - `style = bodyLarge`
    - `verticalAlignment = Alignment.CenterVertically`
- [ ] Add `Spacer` with `weight(1f)`
- [ ] Add `Button` with:
    - `onClick = { /* open tag picker */ }`
    - `enabled = selectedCount > 0`
    - `text = "Add to tag"`
- [ ] Add `Spacer` with `width(8.dp)`
- [ ] Add `IconButton` with:
    - `onClick = onCancel`
    - `Icons.Default.Close`

### Task 8: Create TagEditDialog Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/TagEditDialog.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun TagEditDialog(currentTagName: String, onDismissed: () -> Unit, onSaved: (String) -> Unit, onDeleted: () -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `rememberDialogState()` and `rememberCoroutineScope()`
- [ ] Use `rememberSaveable` for `tagName` initialized to `currentTagName`
- [ ] Implement `AlertDialog` with:
    - `onDismissRequest = { onDismissed(); dialogState.dismissDialog() }`
    - `title = { Text("Edit Tag") }`
    - `text = {`
        `TextField(`
            `value = tagName,`
            `onValueChange = { tagName = it },`
            `label = { Text("Tag name") },`
            `isError = tagName.isBlank(),`
            `errorMessage = if (tagName.isBlank()) { Text("Tag name cannot be empty") } else null`
        `)`
    `}`
- [ ] Add `confirmButton` with:
    - `TextButton` with:
        - `onClick = {`
            `if (tagName.isNotBlank()) {`
                `onSaved(tagName)`
                `onDismissed()`
                `dialogState.dismissDialog()`
            `}`
        `}`
        - `Text("Save")`
    `}`
- [ ] Add `dismissButton` with:
    - `TextButton` with:
        - `onClick = { onDismissed(); dialogState.dismissDialog() }`
        - `Text("Cancel")`
    `}`
- [ ] Conditionally show delete button for custom tabs (implementation detail)

### Task 9: Update PagerViewModel to Expose StateFlow
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt`

**Steps:**
- [ ] Replace `MutableLiveData` with `MutableStateFlow` for:
    - `uiState` → `private val _uiState = MutableStateFlow(PagerUiState())`
    - `apps` → `val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }`
    - `tags` → `val tags: StateFlow<List<Tag>> = _uiState.map { it.tags }`
    - `isMultiSelect` → `val isMultiSelect: StateFlow<Boolean> = _uiState.map { it.isMultiSelect }`
    - `selectedApps` → `val selectedApps: StateFlow<Set<String>> = _uiState.map { it.selectedApps }`
    - `editingTagId` → `val editingTagId: StateFlow<String?> = _uiState.map { it.editingTagId }`
    - `showTagEditDialog` → `val showTagEditDialog: StateFlow<Boolean> = _uiState.map { it.showTagEditDialog }`
    - `isLoading` → `val isLoading: StateFlow<Boolean> = _uiState.map { it.isLoading }`
- [ ] Expose `uiState` as `val uiState: StateFlow<PagerUiState> = _uiState.asStateFlow()`
- [ ] Implement `PagerUiState` data class with appropriate fields
- [ ] Update all methods to update `_uiState` instead of individual `LiveData` objects
- [ ] Ensure proper initialization and state update logic

### Task 10: Remove PagerAdapter and XML Layout
**Files:**
- Delete: `app/src/main/res/layout/fragment_pager.xml`
- Delete: `app/src/main/java/com/aistra/hail/ui/home/PagerAdapter.kt`

**Steps:**
- [ ] Delete the XML layout file
- [ ] Delete the Adapter file
- [ ] Verify no remaining references through compiler errors or IDE search

### Task 11: Add Swipe-to-Refresh to App Grid
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/PagerScreen.kt`
- Modify: `app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt`

**Steps:**
- [ ] Modify `AppGrid` to accept an `onRefresh: () -> Unit` and `isRefreshing: Boolean` parameter
- [ ] Wrap the `LazyVerticalGrid` in `androidx.compose.material3.SwipeToRefresh` with the `onRefresh` and `isRefreshing` parameters
- [ ] Update `PagerScreen` to collect a `refreshState` from ViewModel (or use rememberSaveable) and pass it to `AppGrid`
- [ ] Update `PagerViewModel` to expose a `refresh` function that triggers a reload of apps (if not already present) and update `isRefreshing` state
- [ ] Note: Since we corrected ViewModel strategy, we may need to add a `refresh` function in ViewModel that updates the apps StateFlow

## Validation Checklist

After completing all tasks above:
- [ ] PagerFragment builds and displays app grid correctly for each tab type
- [ ] All apps tab shows all applications
- [ ] Frequent tab shows most used apps (based on usage stats)
- [ ] Recent tab shows recently used apps
- [ ] Custom tabs show apps assigned to that tag
- [ ] App icons load via Coil with proper placeholders
- [ ] Long press enters multi-select mode
- [ ] Multi-select toolbar appears with "Add to tag" action
- [ ] Tag chip shows assigned tags for apps in custom tabs
- [ ] Tag edit dialog allows renaming and deleting custom tabs
- [ ] Header shows appropriate title for each tab
- [ ] State survives configuration changes (rememberSaveable)
- [ ] Accessibility: TalkBack reads app names, tag info, and selection state
- [ ] Performance: Smooth scrolling with 100+ apps
- [ ] Swipe-to-refresh reloads app list
- [ ] No memory leaks from ComposeView or ViewModel
- [ ] Existing unit tests for PagerViewModel still pass
- [ ] No references to PagerAdapter or fragment_pager.xml remain
- [ ] Tab switching in HomeFragment continues to work (will migrate separately)

## Mitigation Strategies

See Mitigation Strategies in `00-master-plan.md`.

## References
- [Compose LazyGrids](https://developer.android.com/jetpack/compose/lists/grids)
- [Material3 Chips](https://m3.material.io/components/chips/usage)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Material3 Dialogs](https://m3.material.io/components/dialogs/usage)
- [Compose Multi-Selection Patterns](https://developer.android.com/jetpack/compose/gestures#selection)
- [Material3 SwipeToRefresh](https://developer.android.com/jetpack/compose/material3#swipe-to-refresh)