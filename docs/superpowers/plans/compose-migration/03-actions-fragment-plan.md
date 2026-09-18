# Hail App Compose Migration Plan: ActionsFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate ActionsFragment from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyColumn, preserving all functionality: action items list, headers, switches, dialogs (add/edit action, app picker), and icon loading via Coil.

**Architecture:** 
- Replace `fragment_actions.xml` with a `ComposeView` in `ActionsFragment.onCreateView`
- Create `@Composable ActionsScreen` that hosts the UI
- Break down into smaller composables: `ActionItem`, `ActionHeader`, `AddActionFab`, `ActionDialog`, `AppPickerDialog`
- Use `rememberSaveable` for UI state (expanded sections, dialog visibility)
- Use `StateFlow` from ViewModel collected with `collectAsState()` for actions list
- Replace `ActionsAdapter` with `LazyColumn` items
- Replace `AppIconCache` with `AppIcon` composable from theme
- Preserve `ActionsViewModel` (no changes needed) but expose `StateFlow` for UI

**Tech Stack:**
- Jetpack Compose, Material3
- Coil for image loading (via AppIcon composable)
- Accompanist Material3 MaterialDialogs (or Material3 built-in alerts/dialogs)
- ViewModel with StateFlow (unchanged)

## File Changes

### 1. Update ActionsFragment to Use ComposeView
#### Task 1: Update ActionsFragment.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/actions/ActionsFragment.kt
- [ ] Replace ViewBinding inflation with ComposeView in onCreateView
- [ ] Set ViewCompositionStrategy to DisposeOnViewTreeLifecycleDestroyed
- [ ] Set content to HailTheme { ActionsScreen(viewModel = viewModel, ...) }

### 2. Create ActionsScreen Composable
#### Task 2: Create ActionsScreen.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ActionsScreen.kt
- [ ] Implement @Composable fun ActionsScreen(viewModel: ActionsViewModel, modifier: Modifier = Modifier)
- [ ] Collect viewModel.uiState.collectAsStateWithLifecycle() and individual StateFlow properties
- [ ] Implement Column layout with fillMaxSize()
- [ ] Add ActionHeader, loading indicator (if needed), LazyColumn for actions list, and ActionDialogs

### 3. Create Supporting Composables
#### Task 3: Create ActionHeader.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ActionHeader.kt
- [ ] Implement @Composable fun ActionHeader(title: String, onAddClicked: () -> Unit, modifier: Modifier = Modifier)
- [ ] Use Row layout with fillMaxWidth(), padding(16.dp), height(56.dp)
- [ ] Add Text with title, style=titleMedium, verticalAlignment=Alignment.CenterVertically
- [ ] Add Spacer with weight(1f)
- [ ] Add IconButton with onClick = onAddClicked and Icons.Default.Add

#### Task 4: Create ActionItem.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ActionItem.kt
- [ ] Implement @Composable fun ActionItem(action: ActionItemData, onClicked: () -> Unit, onEdited: () -> Unit, onDeleted: () -> Unit, onToggleChanged: (Boolean) -> Unit, modifier: Modifier = Modifier)
- [ ] Use MutableInteractionSource() and collectIsPressedAsState() for pressed state
- [ ] Use Row modifier with fillMaxWidth(), background based on pressed state, clickable, ripple, padding(16.dp), padding(horizontal=8.dp)
- [ ] Add AppIcon with request = AppIconRequest(packageName = action.appPackageName, userId = action.userId), contentDescription = "${action.label} icon", modifier = Modifier.size(40.dp)
- [ ] Add Spacer(width=12.dp)
- [ ] Add Column with weight(1f) containing Text(action.label, style=bodyLarge) and Text(action.description, style=bodySmall, color=onSurfaceVariant)
- [ ] Add Spacer(width=8.dp)
- [ ] Add Switch with checked = action.enabled, onCheckedChange = onToggleChanged, colors = SwitchDefaults.colors(thumbColor=primary, trackColor=onSurface)
- [ ] Add Spacer(width=8.dp)
- [ ] Add IconButton with onClick = onEdited and Icons.Default.Edit
- [ ] Add Spacer(width=4.dp)
- [ ] Add IconButton with onClick = onDeleted and Icons.Default.Delete

#### Task 5: Create AddActionFab.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AddActionFab.kt
- [ ] Implement @Composable fun AddActionFab(onClicked: () -> Unit, modifier: Modifier = Modifier)
- [ ] Use FloatingActionButton with onClick = onClicked and modifier = modifier.align(Alignment.BottomEnd).padding(16.dp)
- [ ] Add Icon with imageVector = Icons.Default.Add and contentDescription = "Add action"

#### Task 6: Create ActionDialog.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/ActionDialog.kt
- [ ] Implement @Composable fun ActionDialog(action: ActionItemData?, onDismissed: () -> Unit, onSaved: (ActionItemData) -> Unit, modifier: Modifier = Modifier)
- [ ] Use rememberDialogState() and rememberCoroutineScope()
- [ ] Use rememberSaveable for form fields (label, description, elevated) initialized from action or defaults
- [ ] Implement AlertDialog with onDismissRequest, title, text fields (TextField for label and description, Row with Text and Switch for enabled), and confirm/dismiss buttons
- [ ] Add app picker Button that launches separate dialog (implementation detail)
- [ ] Handle save action: create ActionItemData, call onSaved, then onDismissed and dialogState.dismissDialog()

### 4. Update ActionsViewModel to Expose StateFlow
#### Task 7: Update ActionsViewModel.kt
- [ ] Modify app/src/main/java/com/aistra/hail/ui/actions/ActionsViewModel.kt
- [ ] Replace MutableLiveData with MutableStateFlow for uiState, actions, isLoading, showAddDialog, editingActionId
- [ ] Expose StateFlow properties via _uiState.map { it.property } or direct flows
- [ ] Implement ActionsUiState data class with appropriate fields
- [ ] Update all methods to update _uiState instead of individual LiveData objects
- [ ] Ensure proper initialization and state update logic

### 5. Remove XML Layout and Adapter
#### Task 8: Remove fragment_actions.xml
- [ ] Delete app/src/main/res/layout/fragment_actions.xml

#### Task 9: Remove ActionsAdapter.kt
- [ ] Delete app/src/main/java/com/aistra/hail/ui/actions/ActionsAdapter.kt
- [ ] Verify no remaining references through compiler errors

## Validation Checklist

- [ ] ActionsFragment builds and displays action list correctly
- [ ] Action items show icon, label, description, and toggle switch
- [ ] Toggle switch updates action enabled state via ViewModel
- [ ] Floating Action Button opens add action dialog
- [ ] Add action dialog captures label, description, enabled state, and launches app picker
- [ ] Edit action dialog pre-fills with existing action data
- [ ] Save button creates/updates action via ViewModel
- [ ] Delete button removes action via ViewModel
- [ ] App picker dialog (to be implemented) allows selecting an app for the action
- [ ] Dialogs animate in/out correctly
- [ ] State survives configuration changes (rememberSaveable)
- [ ] Accessibility: TalkBack reads action details and switch states
- [ ] Performance: Smooth scrolling with 100+ actions
- [ ] No memory leaks from ComposeView or ViewModel
- [ ] Existing unit tests for ActionsViewModel still pass
- [ ] No references to ActionsAdapter or fragment_actions.xml remain

## References

- [Compose LazyLists](https://developer.android.com/jetpack/compose/lists/lists)
- [Material3 Dialogs](https://m3.material.io/components/dialogs/usage)
- [Compose StateFlow Integration](https://developer.android.com/jetpack/compose/stateflow)
- [Material3 Switch](https://m3.material.io/components/switches/usage)
- [Compose Dialog State Handling](https://developer.android.com/jetpack/compose/state#dialogs)