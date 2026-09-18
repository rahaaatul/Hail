# Hail App - Compose Migration Plan: PagerFragment (Inside HomeFragment)

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
```kotlin
// app/src/main/java/com/aistra/hail/ui/home/PagerFragment.kt
package com.aistra.hail.ui.home

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
import com.aistra.hail.ui.theme.PagerScreen
import com.aistra.hail.ui.theme.HailTheme

class PagerFragment : Fragment(R.layout.fragment_pager) {

    private val viewModel: PagerViewModel by viewModels { factory }

    // ... existing factory initialization

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HailTheme {
                    PagerScreen(
                        viewModel = viewModel,
                        tabType = arguments?.getString("tabType") ?: "all",
                        // ... pass any necessary callbacks
                    )
                }
            }
        }
    }

    // ... remove existing XML-related code (binding, etc.)
}
```

### 2. Create PagerScreen Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/PagerScreen.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.home.PagerViewModel
import com.aistra.hail.ui.theme.AppIcon
import com.aistra.hail.ui.theme.AppGridItem
import com.aistra.hail.ui.theme.TagChip
import com.aistra.hail.ui.theme.MultiSelectToolbar
import com.aistra.hail.ui.theme.TagEditDialog
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PagerScreen(
    viewModel: PagerViewModel,
    tabType: String, // "all", "frequent", "recent", or custom tab name
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val apps by uiState.apps.collectAsStateWithLifecycle(emptyList())
    val tags by uiState.tags.collectAsStateWithLifecycle(emptyList())
    val isMultiSelect by uiState.isMultiSelect.collectAsStateWithLifecycle(false)
    val selectedApps by uiState.selectedApps.collectAsStateWithLifecycle(emptySet())
    val editingTagId by uiState.editingTagId.collectAsStateWithLifecycle<String?>(null)
    val showTagEditDialog by uiState.showTagEditDialog.collectAsStateWithLifecycle(false)

    Column(modifier
        .fillMaxSize()
    ) {
        // Tab-specific header (could show tag management for custom tabs)
        when (tabType) {
            "all", "frequent", "recent" -> {
                // These tabs don't have tag management, just show title
                PagerHeader(
                    title = when (tabType) {
                        "all" -> "All Apps"
                        "frequent" -> "Frequent"
                        "recent" -> "Recent"
                        else -> tabType
                    },
                    canEditTags = false
                )
            }
            else -> {
                // Custom tabs can have tag management
                PagerHeader(
                    title = tabType,
                    onEditTagsClicked = { viewModel.showTagEditDialog(true) },
                    canEditTags = true
                )
            }
        }
        
        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(24.dp)
            )
        } else {
            // Main content: App grid
            AppGrid(
                apps = apps,
                onAppClicked = { app -> 
                    if (isMultiSelect) {
                        viewModel.toggleAppSelection(app)
                    } else {
                        viewModel.openAppDetails(app)
                    }
                },
                onAppLongClicked = { 
                    if (!isMultiSelect) viewModel.enableMultiSelect()
                    viewModel.toggleAppSelection(app)
                },
                isMultiSelect = isMultiSelect,
                selectedApps = selectedApps,
                showTagBadge = tabType !in listOf("all", "frequent", "recent") // Show tags for custom tabs
            )
            
            // Multi-select toolbar (appears at bottom when in multi-select mode)
            if (isMultiSelect) {
                MultiSelectToolbar(
                    selectedCount = selectedApps.size,
                    onTagSelected = { tagId -> 
                        viewModel.assignTagToSelectedApps(tagId)
                        viewModel.disableMultiSelect()
                    },
                    onCancel = { viewModel.disableMultiSelect() }
                )
            }
        }
        
        // Tag edit dialog (for custom tabs)
        if (showTagEditDialog && tabType !in listOf("all", "frequent", "recent")) {
            TagEditDialog(
                currentTagName = tabType,
                onDismissed = { viewModel.showTagEditDialog(false) },
                onSaved = { newName -> 
                    viewModel.renameTab(tabType, newName)
                    viewModel.showTagEditDialog(false)
                },
                onDeleted = { 
                    viewModel.deleteTab(tabType)
                    // Navigation would need to handle this - perhaps pop back stack
                }
            )
        }
    }
}
```

### 3. Create Supporting Composables
```kotlin
// PagerHeader.kt
@Composable
fun PagerHeader(
    title: String,
    onEditText,
    onEditTagsClicked: () -> Unit = {},
    canEditTags: Boolean = false,
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
        if (canEditTags) {
            IconButton(
                onClick = onEditTagsClicked
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit tag"
                )
            }
        }
    }
}

// AppGrid.kt (similar to AppsFragment but with tag badges)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit,
    isMultiSelect: Boolean,
    selectedApps: Set<String>,
    showTagBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        items(apps) { app ->
            AppGridItem(
                app = app,
                onClick = { onAppClicked(app) },
                onLongClick = { onAppLongClicked(app) },
                isSelected = selectedApps.contains(app.packageName),
                isMultiSelect = isMultiSelect,
                tags = emptyList(), // Would come from app-tag mapping in real implementation
                showTagBadge = showTagBadge,
                modifier = Modifier
            )
        }
    }
}

// AppGridItem.kt
@Composable
fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    tags: List<Tag>,
    showTagBadge: Boolean,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Column(
        modifier = modifier
            .size(72.dp)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else if (isPressed) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .longClickable(onLongClick = onLongClick)
            .interactionSource(interactionSource)
            .padding(8.dp)
            .align(Alignment.Center)
    ) {
        AppIcon(
            request = AppIconRequest(
                packageName = app.packageName,
                userId = app.userId
            ),
            contentDescription = app.label,
            modifier = Modifier
                .size(48.dp)
                .align(Alignment.CenterHorizontally)
        )
        
        // Tag badge (if applicable)
        if (showTagBadge && tags.isNotEmpty()) {
            // In a real implementation, we'd show the first tag or a count
            TagChip(
                text = tags.first().label,
                onDelete = { /* handle tag removal from app */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
        )
        
        if (isMultiSelect) {
            // Checkbox overlay in top-right
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
            ) {
                Checkbox(
                    checked = isSelected,
                    onChange = { /* handled by toggleable */ },
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

// TagChip.kt
@Composable
fun TagChip(
    text: String,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onDeleteRequest = onDelete,
        modifier = modifier
    ) {
        Text(text)
    }
}

// MultiSelectToolbar.kt
@Composable
fun MultiSelectToolbar(
    selectedCount: Int,
    onTagSelected: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "$selectedCount selected",
                style = MaterialTheme.typography.bodyLarge,
                verticalAlignment = Alignment.CenterVertically
            )
            Spacer(modifier = Modifier.weight(1f))
            // In a real implementation, this would be a dropdown or button to select tag
            Button(
                onClick = { /* open tag picker */ },
                enabled = selectedCount > 0
            ) {
                Text("Add to tag")
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onCancel
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel selection"
                )
            }
        }
    }
}

// TagEditDialog.kt
@Composable
fun TagEditDialog(
    currentTagName: String,
    onDismissed: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dialogState = rememberDialogState()
    val scope = rememberCoroutineScope()
    val tagName by rememberSaveable { mutableStateOf(currentTagName) }
    
    AlertDialog(
        onDismissRequest = {
            onDismissed()
            dialogState.dismissDialog()
        },
        title = { Text("Edit Tag") },
        text = {
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag name") },
                isError = tagName.isBlank(),
                errorMessage = if (tagName.isBlank()) { Text("Tag name cannot be empty") } else null
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (tagName.isNotBlank()) {
                        onSaved(tagName)
                        onDismissed()
                        dialogState.dismissDialog()
                    }
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
        },
        // Delete button only shown for custom tabs
        // In a real implementation, we'd check if this is a custom tab
        // For now, we'll show it conditionally based on a parameter
        // But since we don't have that here, we'll omit for brevity
    )
}
```

### 4. Update PagerViewModel to Expose StateFlow
```kotlin
// app/src/main/java/com/aistra/hail/ui/home/PagerViewModel.kt
// No UI changes needed, but ensure these are exposed as StateFlow:
class PagerViewModel(...) : ViewModel() {
    val uiState: MutableStateFlow<PagerUiState> = MutableStateFlow(PagerUiState())
    val apps: MutableStateFlow<List<AppInfo>> = MutableStateFlow(emptyList())
    val tags: MutableStateFlow<List<Tag>> = MutableStateFlow(emptyList())
    val isMultiSelect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val selectedApps: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val editingTagId: MutableStateFlow<String?> = MutableStateFlow(null)
    val showTagEditDialog: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val isLoading: MutableStateFlow<Boolean> = MutableStateFlow(false)
    // ... existing logic updates these flows
}
```

### 5. Remove XML Layout and Adapter
```diff
// app/src/main/res/layout/fragment_pager.xml
- /* ENTIRE FILE REMOVED */

// app/src/main/java/com/aistra/hail/ui/home/PagerAdapter.kt
- /* This is actually the RecyclerView.Adapter - REMOVED */
```

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