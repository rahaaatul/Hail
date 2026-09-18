# Hail App Compose Migration Plan: PagerFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate PagerFragment (which is actually a Fragment used as a page in HomeFragment's ViewPager2) from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyVerticalGrid, preserving all functionality: app grid with tabs, multi-select, tag management, and icon loading via Coil.

**Architecture:** 
- Note: PagerFragment is not a top-level navigation destination but a child fragment of HomeFragment that displays app grids for each tab (All, Frequent, Recent, Custom tabs)
- Replace `fragment_pager.xml` with a `ComposeView` in `PagerFragment.onCreateView`
- Create `@Composable PagerScreen` that hosts the UI for a single tab
- Break down into smaller composables: `AppGridItem`, `TagChip`, `MultiSelectToolbar`, `TagEditDialog`
- Use `rememberSaveable` for UI state (selected apps, tag edit mode)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for apps list and tags
- Replace `PagerAdapter` (which is actually a RecyclerView.Adapter) with `LazyVerticalGrid` items
- Replace `AppIconCache` with `AppIcon` composable from theme
- Preserve `PagerViewModel` (no changes needed) but expose `StateFlow` for UI
- Note: The tab switching logic remains in HomeFragment (which will migrate separately)

**Tech Stack:**
- Jetpack Compose, Material3
- Coil for image loading (via AppIcon composable)
- Accompanist Material3 MaterialDialogs (or Material3 built-in alerts/dialogs)
- ViewModel with StateFlow (unchanged)

## File Changes

### 1. Update PagerFragment to Use ComposeView
#### Task 1: Update PagerFragment.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/PagerFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView in onCreateView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { PagerScreen(viewModel = viewModel, tabType = arguments?.getString("tabType") ?: "all", ...) }

### 2. Create PagerScreen Composable
#### Task 2: Create PagerScreen.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/PagerScreen.kt
- [ ] Implement @Composable fun PagerScreen(viewModel: PagerViewModel, tabType: String, modifier: Modifier = Modifier)
- [ ] Collect viewModel.uiState.collectAsStateWithLifecycle() and individual StateFlow properties
- [ ] Implement Column layout with fillMaxSize()
- [ ] Add tab-specific header (PagerHeader) with conditional tag management
- [ ] Add loading indicator when uiState.isLoading is true
- [ ] Add main content: AppGrid with appropriate callbacks
- [ ] Add multi-select toolbar when isMultiSelect is true
- [ ] Add tag edit dialog when showTagEditDialog is true and tabType is custom

### 3. Create Supporting Composables
#### Task 3: Create PagerHeader.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/PagerHeader.kt
- [ ] Implement @Composable fun PagerHeader(title: String, onEditText: (), onEditTagsClicked: () -> Unit = {}, canEditTags: Boolean = false, modifier: Modifier = Modifier)
- [ ] Use Row layout with fillMaxWidth(), padding(16.dp), height(56.dp)
- [ ] Add Text with title, style=titleMedium, verticalAlignment=Alignment.CenterVertically
- [ ] Add Spacer with weight(1f)
- [ ] Conditionally add IconButton with onClick = onEditTagsClicked and Icons.Default.Edit when canEditTags is true

#### Task 4: Create AppGrid.kt (with tag badges)
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppGrid.kt
- [ ] Implement @Composable fun AppGrid(apps: List<AppInfo>, onAppClicked: (AppInfo) -> Unit, onAppLongClicked: (AppInfo) -> Unit, isMultiSelect: Boolean, selectedApps: Set<String>, showTagBadge: Boolean = false, modifier: Modifier = Modifier)
- [ ] Use LazyVerticalGrid with columns = GridCells.Fixed(3) and modifier.fillMaxWidth().padding(8.dp)
- [ ] Use items(apps) { app -> AppGridItem(...) } with appropriate parameters

#### Task 5: Create AppGridItem.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppGridItem.kt
- [ ] Implement @Composable fun AppGridItem(app: AppInfo, onClick: () -> Unit, onLongClick: () -> Unit, isSelected: Boolean, isMultiSelect: Boolean, tags: List<Tag>, showTagBadge: Boolean, modifier: Modifier = Modifier)
- [ ] Use MutableInteractionSource() and collectIsPressedAsState() for pressed state
- [ ] Use Column modifier with size(72.dp), background based on selected/pressed state, clickable, longClickable, interactionSource, padding(8.dp), align(Alignment.Center)
- [ ] Add AppIcon with request = AppIconRequest(packageName = app.packageName, userId = app.userId), contentDescription = app.label, modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally)
- [ ] When showTagBadge and tags.isNotEmpty(), add TagChip with text = tags.first().label, onDelete = { /* handle tag removal from app */ }, modifier = Modifier.align(Alignment.TopEnd)
- [ ] Add Spacer(height=4.dp)
- [ ] Add Text with app.label, maxLines=1, overflow=TextOverflow.Ellipsis, style=bodyMedium, align=Alignment.CenterHorizontally
- [ ] When isMultiSelect, add Box(align=Alignment.TopEnd, size=20.dp) with Checkbox(checked=isSelected, onChange={/* handled by toggleable */})

#### Task 6: Create TagChip.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/TagChip.kt
- [ ] Implement @Composable fun TagChip(text: String, onDelete: () -> Unit, modifier: Modifier = Modifier)
- [ ] Use Chip with onDeleteRequest = onDelete and modifier = modifier
- [ ] Add Text(text)

#### Task 7: Create MultiSelectToolbar.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/MultiSelectToolbar.kt
- [ ] Implement @Composable fun MultiSelectToolbar(selectedCount: Int, onTagSelected: (String) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier)
- [ ] Use Surface with modifier.fillMaxWidth().height(56.dp)
- [ ] Use Row with fillMaxWidth() and padding(horizontal = 16.dp)
- [ ] add Text with text = "$selectedCount selected", style=bodyLarge, verticalAlignment=Alignment.CenterVertically
- [ ] add Spacer with weight(1f)
- [ ] add Button with onClick = { /* open tag picker */ }, enabled = selectedCount > 0 and text = "Add to tag"
- [ ] add Spacer with width(8.dp)
- [ ] add IconButton with onClick = onCancel and Icons.Default.Close

#### Task 8: Create TagEditDialog.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/TagEditDialog.kt
- [ ] Implement @Composable fun TagEditDialog(currentTagName: String, onDismissed: () -> Unit, onSaved: (String) -> Unit, onDeleted: () -> Unit, modifier: Modifier = Modifier)
- [ ] use rememberDialogState() and rememberCoroutineScope()
- [ ] use rememberSaveable for tagName initialized to currentTagName
- [ ] implement AlertDialog with onDismissRequest, title = Text("Edit Tag"), text = TextField with value = tagName, onValueChange = { tagName = it }, label = { Text("Tag name") }, isError = tagName.isBlank(), errorMessage = if (tagName.isBlank()) { Text("Tag name cannot be empty") } else null
- [ ] add confirm Button with onClick = { if (tagName.isNotBlank()) { onSaved(tagName); onDismissed(); dialogState.dismissDialog() } } and text = "Save"
- [ ] add dismiss Button with onClick = { onDismissed(); dialogState.dismissDialog() } and text = "Cancel"
- [ ] conditionally show delete button for custom tabs (implementation detail)

### 4. Update PagerViewModel to Expose StateFlow
#### Task 9: Update PagerViewModel.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow for uiState, apps, tags, isMultiSelect, selectedApps, editingTagId, showTagEditDialog, isLoading
- [ ] Expose StateFlow properties via _uiState.map { it.property } or direct flows
- [ ] Implement PagerUiState data class with appropriate fields
- [ ] Update all methods to update _uiState instead of individual LiveData objects
- [ ] Ensure proper initialization and state update logic

### 5. Remove XML Layout and Adapter
#### Task 10: Remove fragment_pager.xml
- [ ] Delete app/src/main/res/layout/fragment_pager.xml

#### Task 11: Remove PagerAdapter.kt
- [ ] Delete app/src/main/java/com/aistra/hail/ui/home/PagerAdapter.kt
- [ ] Verify no remaining references through compiler errors

## Validation Checklist

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
- [ ] No memory leaks from ComposeView or ViewModel
- [ ] Existing unit tests for PagerViewModel still pass
- [ ] No references to PagerAdapter or fragment_pager.xml remain
- [ ] Tab switching in HomeFragment continues to work (will migrate separately)

## References

- [Compose LazyGrids](https://developer.android.com/jetpack/compose/lists/grids)
- [Material3 Chips](https://m3.material.io/components/chips/usage)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Material3 Dialogs](https://m3.material.io/components/dialogs/usage)
- [Compose Multi-Selection Patterns](https://developer.android.com/jetpack/compose/gestures#selection)