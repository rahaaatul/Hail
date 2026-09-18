# Hail App - Compose Migration Plan: AppsFragment

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
```kotlin
// app/src/main/java/com/aistra/hail/ui/apps/AppsFragment.kt
package com.aistra.hail.ui.apps

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
import com.aistra.hail.ui.theme.AppsScreen
import com.aistra.hail.ui.theme.HailTheme

class AppsFragment : Fragment(R.layout.fragment_apps) {

    private val viewModel: AppsViewModel by viewModels { factory }

    // ... existing factory initialization

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                HailTheme {
                    AppsScreen(
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

### 2. Create AppsScreen Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/AppsScreen.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.apps.AppsViewModel
import com.aistra.hail.ui.theme.AppIcon
import com.aistra.hail.ui.theme.AppSearchBar
import com.aistra.hail.ui.theme.AppFilterChipRow
import kotlinx.coroutines.flow.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen(
    viewModel: AppsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val apps by uiState.apps.collectAsStateWithLifecycle(emptyList())
    val query by uiState.query.collectAsStateWithLifecycle("")
    val selectedFilter by uiState.selectedFilter.collectAsStateWithLifecycle(AllAppsFilter.INSTANCE)
    val isMultiSelect by uiState.isMultiSelect.collectAsStateWithLifecycle(false)
    val selectedApps by uiState.selectedApps.collectAsStateWithLifecycle(emptySet())

    Column(modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        AppSearchBar(
            value = query,
            onQueryChanged = { viewModel.updateQuery(it) },
            isMultiSelect = isMultiSelect
        )
        Spacer(modifier = Modifier.height(8.dp))
        AppFilterChipRow(
            filters = viewModel.filters,
            selectedFilter = selectedFilter,
            onFilterSelected = { viewModel.selectFilter(it) }
        )
        Spacer(modifier = Modifier.height(8.dp))
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
                viewModel.toggleAppSelection(it)
            },
            isMultiSelect = isMultiSelect,
            selectedApps = selectedApps
        )
    }
}
```

### 3. Create Supporting Composables
```kotlin
// AppSearchBar.kt
@Composable
fun AppSearchBar(
    value: String,
    onQueryChanged: (String) -> Unit,
    isMultiSelect: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onQueryChanged,
            label = { Text("Search apps") },
            isEnabled = !isMultiSelect,
            modifier = Modifier
                .fillMaxWidth()
        )
        if (isMultiSelect) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { /* cancel multi-select */ }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel selection"
                )
            }
        }
    }
}

// AppFilterChipRow.kt
@Composable
fun AppFilterChipRow(
    filters: List<AppFilter>,
    selectedFilter: AppFilter,
    onFilterSelected: (AppFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalScrollView(modifier = modifier.fillMaxWidth()) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .padding(vertical = 4.dp)
        ) {
            filters.forEach { filter ->
                FilterChip(
                    selected = filter == selectedFilter,
                    onClick = { onFilterSelected(filter) },
                    label = { Text(filter.label) }
                )
            }
        }
    }
}

// AppGrid.kt
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    onAppClicked: (AppInfo) -> Unit,
    onAppLongClicked: (AppInfo) -> Unit,
    isMultiSelect: Boolean,
    selectedApps: Set<String>,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        items(apps) { app ->
            AppItem(
                app = app,
                onClick = { onAppClicked(app) },
                onLongClick = { onAppLongClicked(app) },
                isSelected = selectedApps.contains(app.packageName),
                isMultiSelect = isMultiSelect,
                modifier = Modifier
                    .clickable { /* handled in AppItem */ }
                    .toggleable(
                        value = selectedApps.contains(app.packageName),
                        onValueChange = { /* handled in AppItem */ },
                        role = if (isMultiSelect) Role.Checkbox else null
                    )
            )
        }
    }
}

// AppItem.kt
@Composable
fun AppItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isMultiSelect: Boolean,
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
```

### 4. Update AppsViewModel to Expose StateFlow
```kotlin
// app/src/main/java/com/aistra/hail/ui/apps/AppsViewModel.kt
// No UI changes needed, but ensure these are exposed as StateFlow:
class AppsViewModel(...) : ViewModel() {
    val uiState: MutableStateFlow<AppsUiState> = MutableStateFlow(AppsUiState())
    val apps: MutableStateFlow<List<AppInfo>> = MutableStateFlow(emptyList())
    val query: MutableStateFlow<String> = MutableStateFlow("")
    val selectedFilter: MutableStateFlow<AppFilter> = MutableStateFlow(AllAppsFilter.INSTANCE)
    val isMultiSelect: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val selectedApps: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    // ... existing logic updates these flows
}
```

### 5. Remove XML Layout and Adapter
```diff
// app/src/main/res/layout/fragment_apps.xml
- /* ENTIRE FILE REMOVED */

// app/src/main/java/com/aistra/hail/ui/apps/AppsAdapter.kt
- /* ENTIRE FILE REMOVED */
```

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