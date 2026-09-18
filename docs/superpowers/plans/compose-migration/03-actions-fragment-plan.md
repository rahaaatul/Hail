# Hail App - Compose Migration Plan: ActionsFragment

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
```kotlin
// app/src/main/java/com/aistra/hail/ui/actions/ActionsFragment.kt
package com.aistra.hail.ui.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.RememberObserver
import com.aistra.hail.R
import com.aistra.hail.ui.theme.ActionsScreen
import com.aistra.hail.ui.theme.HailTheme

class ActionsFragment : Fragment(R.layout.fragment_actions) {

    private val viewModel: ActionsViewModel by viewModels { factory }

    // ... existing factory initialization

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HailTheme {
                    ActionsScreen(
                        viewModel = viewModel,
                        // ... pass any necessary callbacks
                    )
                }
            }
        }
    }

    // ... remove existing XML-related code (binding, etc.)
}
```

### 2. Create ActionsScreen Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/ActionsScreen.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.actions.ActionsViewModel
import com.aistra.hail.ui.theme.AppIcon
import com.aistra.hail.ui.theme.ActionItem
import com.aistra.hail.ui.theme.ActionHeader
import com.aistra.hail.ui.theme.AddActionFab
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionsScreen(
    viewModel: ActionsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions by uiState.actions.collectAsStateWithLifecycle(emptyList())
    val isLoading by uiState.isLoading.collectAsStateWithLifecycle(false)
    val showAddDialog by uiState.showAddDialog.collectAsStateWithLifecycle(false)
    val editingActionId by uiState.editingActionId.collectAsStateWithLifecycle<String?>(null)

    Column(modifier
        .fillMaxSize()
    ) {
        // Header with title and add button
        ActionHeader(
            title = "Actions",
            onAddClicked = { viewModel.showAddDialog(true) }
        )
        
        // Loading indicator
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
            )
        } else {
            // Actions list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Group actions by type or show as flat list
                items(actions) { action ->
                    ActionItem(
                        action = action,
                        onClicked = { viewModel.onActionClicked(action) },
                        onEdited = { viewModel.startEditing(action.id) },
                        onDeleted = { viewModel.deleteAction(action.id) },
                        onToggleChanged = { enabled -> 
                            viewModel.updateActionEnabled(action.id, enabled)
                        }
                    )
                }
                
                // Add action button at the end
                item {
                    AddActionFab(
                        onClicked = { viewModel.showAddDialog(true) }
                    )
                }
            }
        }
        
        // Dialogs
        if (showAddDialog) {
            ActionDialog(
                action = null, // null indicates add mode
                onDismissed = { viewModel.showAddDialog(false) },
                onSaved = { actionData -> 
                    viewModel.saveAction(actionData)
                    viewModel.showAddDialog(false)
                }
            )
        }
        
        if (editingActionId != null) {
            val actionToEdit = actions.firstOrNull { it.id == editingActionId }
            actionToEdit?.let { action ->
                ActionDialog(
                    action = action,
                    onDismissed = { viewModel.editingActionId = null },
                    onSaved = { actionData -> 
                        viewModel.updateAction(actionData)
                        viewModel.editingActionId = null
                    }
                )
            }
        }
    }
}
```

### 3. Create Supporting Composables
```kotlin
// ActionHeader.kt
@Composable
fun ActionHeader(
    title: String,
    onAddClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(56.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            verticalAlignment = Alignment.CenterVertically
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(
            onClick = onAddClicked
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add action"
            )
        }
    }
}

// ActionItem.kt
@Composable
fun ActionItem(
    action: ActionItemData, // Assuming this is the data class
    onClicked: () -> Unit,
    onEdited: () -> Unit,
    onDeleted: () -> Unit,
    onToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                if (isPressed) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClicked)
            .ripple(bounded = false, interactionSource = interactionSource)
            .padding(16.dp)
            .padding(horizontal = 8.dp)
    ) {
        // Icon
        AppIcon(
            request = AppIconRequest(
                packageName = action.appPackageName,
                userId = action.userId
            ),
            contentDescription = "${action.label} icon",
            modifier = Modifier
                .size(40.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        
        // Details
        Column(
            modifier = Modifier
                .weight(1f)
        ) {
            Text(
                text = action.label,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = action.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Toggle switch
        Switch(
            checked = action.enabled,
            onCheckedChange = onToggleChanged,
            colors = SwitchDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface
            )
        )
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Edit button
        IconButton(
            onClick = onEdited
        ) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit action"
            )
        }
        
        Spacer(modifier = Modifier.width(4.dp))
        
        // Delete button
        IconButton(
            onClick = onDeleted
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete action"
            )
        }
    }
}

// AddActionFab.kt
@Composable
fun AddActionFab(
    onClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    FloatingActionButton(
        onClick = onClicked,
        modifier = modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add action"
        )
    }
}

// ActionDialog.kt
@Composable
fun ActionDialog(
    action: ActionItemData?, // null for add, non-null for edit
    onDismissed: () -> Unit,
    onSaved: (ActionItemData) -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogState = rememberDialogState()
    val scope = rememberCoroutineScope()
    
    // Form fields (would be lifted from dialog content)
    val label by rememberSaveable { mutableStateOf(action?.label ?: "") }
    val description by rememberSaveable { mutableStateOf(action?.description ?: "") }
    val enabled by rememberSaveable { mutableStateOf(action?.enabled ?: true) }
    
    AlertDialog(
        onDismissRequest = {
            onDismissed()
            dialogState.dismissDialog()
        },
        title = { Text(if (action == null) "Add Action" else "Edit Action") },
        text = {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    placeholder = { Text("Enter action label") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    placeholder = { Text("Enter description") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enabled")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }
                // App picker would be another dialog launched from here
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { /* open app picker dialog */ },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text("Select App")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val actionData = ActionItemData(
                        id = action?.id ?: java.util.UUID.randomUUID().toString(),
                        label = label,
                        description = description,
                        enabled = enabled,
                        // ... other required fields
                    )
                    onSaved(actionData)
                    onDismissed()
                    dialogState.dismissDialog()
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissed()
                    dialogState.dismissDialog()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}
```

### 4. Update ActionsViewModel to Expose StateFlow
```kotlin
// app/src/main/java/com/aistra/hail/ui/actions/ActionsViewModel.kt
// No UI changes needed, but ensure these are exposed as StateFlow:
class ActionsViewModel(...) : ViewModel() {
    val uiState: MutableStateFlow<ActionsUiState> = MutableStateFlow(ActionsUiState())
    val actions: MutableStateFlow<List<ActionItemData>> = MutableStateFlow(emptyList())
    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showAddDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val editingActionId: MutableStateFlow<String?> = MutableStateFlow(null)
    // ... existing logic updates these flows
}
```

### 5. Remove XML Layout and Adapter
```diff
// app/src/main/res/layout/fragment_actions.xml
- /* ENTIRE FILE REMOVED */

// app/src/main/java/com/aistra/hail/ui/actions/ActionsAdapter.kt
- /* ENTIRE FILE REMOVED */
```

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