# Hail App Compose Migration Plan: ActionsFragment Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate ActionsFragment from XML layout + ViewBinding + RecyclerView.Adapter to Jetpack Compose using LazyColumn, preserving all functionality: action items list (icon, name, dependencies), long-press menu (Edit Action, Create Shortcut, Duplicate, Delete), direct execution on item click, and app picker dialog for selecting unfreeze and launch apps.

**Architecture:**
- Replace `fragment_actions.xml` with a `ComposeView` in `ActionsFragment.onCreateView`
- Create `@Composable ActionsScreen` that hosts the UI and manages state (since no ViewModel exists)
- Break down into smaller composables: `ActionItem`, `ActionDialog`, `AppPickerDialog`
- Use `remember` and `LaunchedEffect` for loading actions from ActionsRepository and handling UI state
- Replace `ActionsAdapter` with `LazyColumn` items
- Use `AppIcon` composable from theme for icon loading (or async image loading if needed)
- Preserve the existing long-press menu functionality and app picker dialogs in Compose

**Tech Stack:**
- Jetpack Compose, Material3
- Coroutines for async repository calls
- Material3 built-in alerts/dialogs

**Spec:** This plan is based on the current ActionsFragment implementation as of the codebase analysis.

## Global Constraints
- Jetpack Compose BOM 2026.09.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- Navigation Component 2.10.1 (Fragment-based)
- No ViewModel or ActionsViewModel exists; state managed in composable

---
### Task 1: Update ActionsFragment to Use ComposeView
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ui/actions/ActionsFragment.kt`

**Steps:**
- [ ] Remove ViewBinding inflation and replace with `ComposeView` in `onCreateView`
- [ ] Set `ViewCompositionStrategy` to `DisposeOnViewTreeLifecycleDestroyed`
- [ ] Set content to `HailTheme { ActionsScreen(/* state holder */) }`
- [ ] Remove unused imports and ViewBinding reference

### Task 2: Create ActionsScreen Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ActionsScreen.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun ActionsScreen(onNavigateToHome: () -> Unit)` (if needed for navigation, but fragment is already in navigation graph)
- [ ] Inside ActionsScreen, manage state for:
    - `actions` list: `val actions by remember { mutableStateOf(emptyList<LaunchAction>()) }`
    - `isLoading` Boolean: `val isLoading by remember { mutableStateOf(false) }`
    - `showDialog` Boolean: `val showDialog by remember { mutableStateOf(false) }`
    - `editingActionId`: `val editingActionId by remember { mutableStateOf<String?>(null) }`
    - `selectedUnfreezePackages`: `val selectedUnfreezePackages by remember { mutableStateOf(setOf<String>()) }`
    - `selectedLaunchPackage`: `val selectedLaunchPackage by remember { mutableStateOf<String?>(null) }`
- [ ] Load actions in `LaunchedEffect(Unit)` when entering the screen, setting `isLoading` true, calling `ActionsRepository.loadAll()`, updating `actions` and setting `isLoading` false
- [ ] Implement UI with:
    - If `isLoading`, show a circular progress indicator
    - Else, show a `LazyColumn` with `items(actions)` each as an `ActionItem`
    - The `ActionItem` should handle click (to execute action) and long-click (to show edit menu)
- [ ] Handle the edit menu options by setting appropriate state and opening dialogs (we'll create dialog composables for edit and app picker)
- [ ] Note: There is no FAB or header in the current UI

### Task 3: Create ActionItem Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ActionItem.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun ActionItem(action: LaunchAction, onClick: () -> Unit, onLongClick: () -> Unit, modifier: Modifier = Modifier)`
- [ ] Use `Row` layout with:
    - `fillMaxWidth()`
    - `padding(16.dp)`
    - `clickable(onClick = onClick)`
    - `background` with optional ripple or indication (we can use `clickable` with `indication` parameter)
    - For long press, we can use `pointerInput` to detect long press and call `onLongClick`, or use `detectTapGestures` (but simpler: we can use `clickable` for click and add `longClickable` for long click? Actually, Compose has `clickable` and we can add `longClickable` as a separate modifier? We can use `combinedClickable` or we can use `pointerInput`. Let's use `pointerInput` for long press and `clickable` for regular click? Alternatively, we can use `Modifier.pointerInput` to detect both. However, for simplicity, we can make the item clickable for execution and long-clickable for the menu. We'll use:
        - `modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
            .padding(16.dp)
        `
    - Or we can use two separate modifiers: `clickable` and `longClickable` but note that longClickable will also call the click action after the long click? We want to avoid that. So we'll use `pointerInput` as above.
- [ ] Inside the Row:
    - Add `AppIcon` composable (we need to create or use existing one) with:
        - `request = AppIconRequest(packageName = action.launchPackage, userId = HPackages.myUserId)` (assuming we have HPackages object)
        - `contentDescription = "${AppInfo(action.launchPackage).name} icon"`
        - `modifier = Modifier.size(40.dp)`
    - Add `Spacer(width=12.dp)`
    - Add `Column` with `weight(1f)` containing:
        - `Text(appName, style=bodyLarge)` where appName is loaded from AppInfo(action.launchPackage).name
        - `Text(dependenciesText, style=bodySmall, color=onSurfaceVariant)` where dependenciesText is action.unfreezePackages.joinToString(", ") { AppInfo(it).name }
    - Add `Spacer(width=8.dp)` (optional, for alignment)
- [ ] Note: No toggle switch, no labels, no descriptions as per current implementation

### Task 4: Create ActionDialog Composable (for editing/creating action)
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ActionDialog.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun ActionDialog(isVisible: Boolean, onDismiss: () -> Unit, onSave: (LaunchAction) -> Unit, editingAction: LaunchAction?, modifier: Modifier = Modifier)`
- [ ] Use `if (isVisible) { AlertDialog ... }` or `ModalBottomSheet` or `Dialog` - we'll use `AlertDialog` for consistency with existing dialogs.
- [ ] Inside the dialog:
    - Title: "Edit Action" if editingAction != null else "Create Action"
    - Text field area:
        - Two buttons: "Select Unfreeze Apps" and "Select Launch App"
        - The buttons should show the current selection as text (e.g., comma-separated list of unfreeze app names, or the launch app name)
    - We'll manage the state for selected unfreeze and launch packages within the dialog (or we can lift state up to ActionsScreen and pass down)
    - Since the dialog is used for both edit and create, we need to initialize the selections from the editingAction if present.
- [ ] We'll create a state inside the dialog for:
    - `selectedUnfreeze` (mutableStateOf(setOf<String>()))
    - `selectedLaunch` (mutableStateOf<String?>())
- [ ] Initialize these from editingAction if present, else empty.
- [ ] The "Select Unfreeze Apps" button opens an app picker dialog in multi-select mode.
- [ ] The "Select Launch App" button opens an app picker dialog in single-select mode.
- [ ] When the user selects apps in the picker, we update the respective state.
- [ ] The dialog has a Save button and a Cancel button.
- [ ] On Save, we validate: at least one unfreeze app and one launch app selected, and the launch app is not in the unfreeze list (as per repository logic). Then we create a LaunchAction object and call onSave.
- [ ] On Cancel or outside touch, we call onDismiss.

### Task 5: Create AppPickerDialog Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppPickerDialog.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppPickerDialog(isVisible: Boolean, onDismiss: () -> Unit, onSelected: (Set<String>) -> Unit, multiSelect: Boolean, preSelected: Set<String> = emptySet(), modifier: Modifier = Modifier)`
- [ ] This dialog will replicate the functionality of showAppPicker in the fragment but in Compose.
- [ ] We'll need to load the list of installed apps (similar to the fragment) and filter by search text.
- [ ] We'll use:
    - An `TextField` for search
    - A `LazyVerticalGrid` (or `LazyColumn` with grid) to display app icons and names
    - An adapter-like state to hold the selected apps (for multi-select) or single selection (for single-select)
- [ ] We'll load the apps list in a `LaunchedEffect` or `remember` using `AppMetaCache.getInstalledApplicationsCacheFirst()` (same as fragment)
- [ ] We'll filter the apps based on the search query.
- [ ] Each grid item will show the app icon and name, and a checkmark if selected (in multi-select) or a radio button (in single-select). We can use `Checkbox` or `RadioButton` or just change the background color.
- [ ] The dialog has OK and Cancel buttons.
- [ ] On OK, we call onSelected with the current selected set.
- [ ] On Cancel, we call onDismiss.

### Task 6: Integrate Dialogs in ActionsScreen
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/ActionsScreen.kt` (from Task 2)

**Steps:**
- [ ] In ActionsScreen, manage state for showing the action dialog and app picker dialog.
- [ ] When the user long-clicks on an ActionItem, we set the editingActionId to that action's id, and set the dialog state to show the ActionDialog (with editingAction loaded from ActionsRepository.loadById(editingActionId)).
- [ ] When the user clicks on an ActionItem, we execute the action (using ActionExecutor.prepare(action) and startActivity).
- [ ] We'll need to load the editingAction when the dialog is about to show, or we can pass the action directly to the dialog.
- [ ] Alternatively, we can pass the action object to the ActionDialog instead of the id, to avoid an extra repository call.
- [ ] We'll also manage the state for the app picker dialog when launched from the ActionDialog.

### Task 7: Remove ActionsAdapter and XML Layout
**Files:**
- Delete: `app/src/main/res/layout/fragment_actions.xml`
- Delete: `app/src/main/java/com/aistra/hail/ui/actions/ActionsAdapter.kt`

**Steps:**
- [ ] Delete the XML layout file
- [ ] Delete the Adapter file
- [ ] Verify no remaining references through compiler errors or IDE search

### Task 8: Update ActionsRepository (Optional - if we decide to expose StateFlow)
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/utils/ActionsRepository.kt` (only if we choose to expose a StateFlow for better Compose integration)

**Steps:**
- [ ] This task is optional. If we decide to keep state management in the composable, we can skip this.
- [ ] If we want to expose a StateFlow from the repository, we would:
    - Add a private MutableStateFlow<List<LaunchAction>> to hold the actions
    - Expose it as a StateFlow
    - Update it whenever loadAll, save, delete, duplicate are called
- [ ] However, to keep the migration focused and not change the repository unless necessary, we can skip this task and manage state in the composable.

## Validation Checklist
After completing all tasks above:
- [ ] ActionsFragment builds and displays action list correctly
- [ ] Action items show icon (from launch package), name (from launch package), and dependencies list (unfreeze package names as comma-separated text)
- [ ] Clicking an action item executes the action (launches the launch app)
- [ ] Long-pressing an action item shows a menu with Edit Action, Create Shortcut, Duplicate, Delete options
- [ ] Selecting Edit Action opens the ActionDialog with the current action's data pre-filled in the app picker buttons
- [ ] The ActionDialog allows selecting unfreeze apps (multi-select) and launch app (single-select) via the app picker dialog
- [ ] Validation in ActionDialog requires at least one unfreeze app and one launch app (and launch app not in unfreeze list)
- [ ] Saving the action updates the list (via ActionsRepository.save) and refreshes the UI
- [ ] Duplicate and Delete options work as expected
- [ ] Create Shortcut option works as expected
- [ ] The app picker dialog functions correctly (search, selection, etc.)
- [ ] State survives configuration changes (using remember)
- [ ] Accessibility: TalkBack reads action details correctly
- [ ] Performance: Smooth scrolling with 100+ actions
- [ ] No memory leaks from ComposeView or coroutines
- [ ] No references to ActionsAdapter or fragment_actions.xml remain

## References
- [Compose LazyLists](https://developer.android.com/jetpack/compose/lists/lists)
- [Material3 Dialogs](https://m3.material.io/components/dialogs/usage)
- [Compose State Handling](https://developer.android.com/jetpack/compose/state)
- [AppIcon Coil Integration](https://developer.android.com/jetpack/compose/images#loading-images-from-internet) (but we are using local app icons, so we can use AsyncImage or existing AppIconCache)
- [Coroutine Scope in Compose](https://developer.android.com/jetpack/compose/side-effects#launched-effect)