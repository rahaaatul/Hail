@file:Suppress("MatchingDeclarationName", "LongMethod")

package com.aistra.hail.ui.actions

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.ui.apps.AppIcon
import com.aistra.hail.ui.components.HailBottomSheet
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.LaunchAction

private fun editorDraftValid(ui: ActionsUiState): Boolean =
    ui.editorLaunch.isNotEmpty() &&
        ui.editorUnfreeze.isNotEmpty() &&
        ui.editorUnfreeze.any { it != ui.editorLaunch }

@Immutable
data class ActionsCallbacks(
    val onExecute: (LaunchAction) -> Unit,
    val onEdit: (LaunchAction) -> Unit,
    val onDuplicate: (LaunchAction) -> Unit,
    val onDelete: (LaunchAction) -> Unit,
    val onCreateShortcut: (LaunchAction) -> Unit,
    val onSaveAction: (existingId: String?, launchPackage: String, unfreezePackages: List<String>) -> Unit,
)

@Suppress("LongMethod", "CyclomaticComplexMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ActionsScreen(
    ui: ActionsUiState,
    callbacks: ActionsCallbacks,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
    ) {
        if (ui.actions.isEmpty()) {
            Text(
                text = stringResource(R.string.nothing_here),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            ActionsList(
                actions = ui.actions,
                onExecute = callbacks.onExecute,
                onLongClick = { ui.menuTarget = it },
            )
        }
        ui.editorTarget?.let { target ->
            HailBottomSheet(
                onDismissRequest = {
                    when {
                        ui.pickerTarget != null -> {
                            ui.pickerTarget = null
                        }

                        else -> {
                            ui.editorLaunch = ""
                            ui.editorUnfreeze = emptyList()
                            ui.editorTarget = null
                        }
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                hideHandleOnExpand = ui.pickerTarget != null,
                fillMaxHeight = ui.pickerTarget != null,
            ) {
                val picker = ui.pickerTarget
                if (picker != null) {
                    AppPickerContent(
                        target = picker,
                        apps = ui.pickerApps,
                        onConfirm = { selected ->
                            when (picker) {
                                is PickerTarget.Single -> ui.editorLaunch = selected.firstOrNull() ?: ""
                                is PickerTarget.Multi -> ui.editorUnfreeze = selected.toList()
                            }
                            ui.pickerTarget = null
                        },
                    )
                } else {
                    ActionEditorContent(
                        target = target,
                        launchPackage = ui.editorLaunch,
                        unfreezePackages = ui.editorUnfreeze,
                        onSave = {
                            if (!editorDraftValid(ui)) {
                                HUI.showToast(R.string.action_required_unfreeze)
                            } else {
                                val id = target.existing?.id
                                callbacks.onSaveAction(id, ui.editorLaunch, ui.editorUnfreeze)
                                ui.editorLaunch = ""
                                ui.editorUnfreeze = emptyList()
                                ui.editorTarget = null
                            }
                        },
                        onDelete = {
                            val existing = target.existing
                            if (existing != null) callbacks.onDelete(existing)
                            ui.editorLaunch = ""
                            ui.editorUnfreeze = emptyList()
                            ui.editorTarget = null
                        },
                        onPickUnfreeze = {
                            ui.pickerTarget =
                                PickerTarget.Multi(
                                    selected = ui.editorUnfreeze.toSet(),
                                    exclude = ui.editorLaunch?.let { setOf(it) } ?: emptySet(),
                                )
                        },
                        onPickLaunch = {
                            ui.pickerTarget =
                                PickerTarget.Single(
                                    selected = if (ui.editorLaunch.isNotEmpty()) setOf(ui.editorLaunch) else emptySet(),
                                    exclude = ui.editorUnfreeze.toSet(),
                                )
                        },
                    )
                }
            }
        }
        ui.menuTarget?.let { target ->
            HailBottomSheet(onDismissRequest = { ui.menuTarget = null }) {
                Column(
                    modifier =
                        Modifier
                            .padding(
                                start = dimensionResource(R.dimen.space300),
                                top = 0.dp,
                                end = dimensionResource(R.dimen.space300),
                                bottom = dimensionResource(R.dimen.space300),
                            ).navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                ) {
                    SegmentedListItem(
                        onClick = {
                            ui.menuTarget = null
                            callbacks.onEdit(target)
                        },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 4),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        contentPadding =
                            PaddingValues(
                                start = dimensionResource(R.dimen.space200),
                                end = dimensionResource(R.dimen.space200),
                                top = dimensionResource(R.dimen.space200),
                                bottom = dimensionResource(R.dimen.space200),
                            ),
                        leadingContent = {
                            Icon(
                                EditFilledIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                            )
                        },
                        content = { Text(stringResource(R.string.action_edit_action), style = MaterialTheme.typography.bodyLarge) },
                    )
                    SegmentedListItem(
                        onClick = {
                            ui.menuTarget = null
                            callbacks.onCreateShortcut(target)
                        },
                        shapes = ListItemDefaults.segmentedShapes(index = 1, count = 4),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        contentPadding =
                            PaddingValues(
                                start = dimensionResource(R.dimen.space200),
                                end = dimensionResource(R.dimen.space200),
                                top = dimensionResource(R.dimen.space200),
                                bottom = dimensionResource(R.dimen.space200),
                            ),
                        leadingContent = {
                            Icon(
                                MobileArrowUpRightIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                            )
                        },
                        content = { Text(stringResource(R.string.action_create_shortcut), style = MaterialTheme.typography.bodyLarge) },
                    )
                    SegmentedListItem(
                        onClick = {
                            ui.menuTarget = null
                            callbacks.onDuplicate(target)
                        },
                        shapes = ListItemDefaults.segmentedShapes(index = 2, count = 4),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        contentPadding =
                            PaddingValues(
                                start = dimensionResource(R.dimen.space200),
                                end = dimensionResource(R.dimen.space200),
                                top = dimensionResource(R.dimen.space200),
                                bottom = dimensionResource(R.dimen.space200),
                            ),
                        leadingContent = {
                            Icon(
                                FileCopyIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                            )
                        },
                        content = { Text(stringResource(R.string.action_duplicate), style = MaterialTheme.typography.bodyLarge) },
                    )
                    SegmentedListItem(
                        onClick = {
                            ui.menuTarget = null
                            ui.deleteTarget = target
                        },
                        shapes = ListItemDefaults.segmentedShapes(index = 3, count = 4),
                        colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                        contentPadding =
                            PaddingValues(
                                start = dimensionResource(R.dimen.space200),
                                end = dimensionResource(R.dimen.space200),
                                top = dimensionResource(R.dimen.space200),
                                bottom = dimensionResource(R.dimen.space200),
                            ),
                        leadingContent = {
                            Icon(
                                DeleteForeverFilledIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                            )
                        },
                        content = { Text(stringResource(R.string.action_delete), style = MaterialTheme.typography.bodyLarge) },
                    )
                }
            }
        }
        ui.deleteTarget?.let { target ->
            HailBottomSheet(onDismissRequest = { ui.deleteTarget = null }, containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(
                    modifier =
                        Modifier
                            .padding(
                                start = dimensionResource(R.dimen.space300),
                                top = 0.dp,
                                end = dimensionResource(R.dimen.space300),
                                bottom = dimensionResource(R.dimen.space300),
                            ).navigationBarsPadding(),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.action_remove_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        Row {
                            FilledIconButton(
                                onClick = {
                                    ui.deleteTarget = null
                                    callbacks.onDelete(target)
                                },
                                shapes = IconButtonDefaults.shapes(),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                            ) { Icon(DeleteIcon, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(dimensionResource(R.dimen.space300))) }
                        }
                    }
                    Spacer(Modifier.height(dimensionResource(R.dimen.space100)))
                    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            contentPadding =
                                PaddingValues(
                                    start = dimensionResource(R.dimen.space200),
                                    end = dimensionResource(R.dimen.space200),
                                    top = dimensionResource(R.dimen.space200),
                                    bottom = dimensionResource(R.dimen.space200),
                                ),
                            content = {
                                if (target.unfreezePackages.isEmpty()) {
                                    Text(stringResource(R.string.action_no_dependencies), style = MaterialTheme.typography.bodyLarge)
                                } else {
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space100)), verticalArrangement = Arrangement.Center) {
                                        Text(stringResource(R.string.action_delete_prerequisites), style = MaterialTheme.typography.bodyLarge)
                                        target.unfreezePackages.forEach { pkg ->
                                            remember(pkg) { AppInfo(pkg).applicationInfo }?.let { info ->
                                                AppIcon(info = info, dimmed = false, size = dimensionResource(R.dimen.space400), internalPadding = 0.dp, contentDescription = AppInfo(pkg).name)
                                            }
                                        }
                                    }
                                }
                            },
                        )
                        SegmentedListItem(
                            shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                            colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            contentPadding =
                                PaddingValues(
                                    start = dimensionResource(R.dimen.space200),
                                    end = dimensionResource(R.dimen.space200),
                                    top = dimensionResource(R.dimen.space200),
                                    bottom = dimensionResource(R.dimen.space200),
                                ),
                            content = {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space100))) {
                                    Text(stringResource(R.string.action_delete_main), style = MaterialTheme.typography.bodyLarge)
                                    remember(target.launchPackage) { AppInfo(target.launchPackage).applicationInfo }?.let { info ->
                                        AppIcon(
                                            info = info,
                                            dimmed = false,
                                            size = dimensionResource(R.dimen.space400),
                                            internalPadding = 0.dp,
                                            contentDescription = AppInfo(target.launchPackage).name,
                                        )
                                    }
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionsList(
    actions: List<LaunchAction>,
    onExecute: (LaunchAction) -> Unit,
    onLongClick: (LaunchAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = dimensionResource(R.dimen.space200), vertical = dimensionResource(R.dimen.space100)),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        itemsIndexed(actions) { index, action ->
            val info = remember(action.launchPackage) { AppInfo(action.launchPackage) }
            val appInfo = remember(action.launchPackage) { info.applicationInfo }
            SegmentedListItem(
                onClick = { onExecute(action) },
                onLongClick = { onLongClick(action) },
                onLongClickLabel = stringResource(R.string.action_edit_action),
                shapes = ListItemDefaults.segmentedShapes(index = index, count = actions.size),
                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                contentPadding =
                    PaddingValues(
                        start = dimensionResource(R.dimen.space200),
                        end = dimensionResource(R.dimen.space200),
                        top = dimensionResource(R.dimen.space100),
                        bottom = dimensionResource(R.dimen.space100),
                    ),
                modifier = Modifier.heightIn(min = dimensionResource(R.dimen.space900)),
                leadingContent = {
                    appInfo?.let {
                        AppIcon(
                            info = it,
                            dimmed = false,
                            size = dimensionResource(R.dimen.space700),
                            internalPadding = dimensionResource(R.dimen.space100),
                            contentDescription = info.name,
                        )
                    }
                },
                content = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        Text(text = info.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = action.unfreezePackages.joinToString(", ") { AppInfo(it).name },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
fun ActionEditorContent(
    target: EditorTarget,
    launchPackage: String,
    unfreezePackages: List<String>,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPickUnfreeze: () -> Unit,
    onPickLaunch: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(
                    start = dimensionResource(R.dimen.space300),
                    top = 0.dp,
                    end = dimensionResource(R.dimen.space300),
                    bottom = dimensionResource(R.dimen.space300),
                ).imePadding()
                .navigationBarsPadding(),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(if (target.existing != null) R.string.action_edit_title else R.string.action_create_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Row {
                FilledIconButton(
                    onClick = onDelete,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                ) {
                    Icon(DeleteIcon, contentDescription = stringResource(R.string.action_delete), modifier = Modifier.size(dimensionResource(R.dimen.space300)))
                }
                FilledIconButton(
                    onClick = onSave,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                ) {
                    Icon(CheckIcon, contentDescription = stringResource(R.string.save), modifier = Modifier.size(dimensionResource(R.dimen.space300)))
                }
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.space200)))
        Column(
            modifier =
                Modifier
                    .padding(
                        bottom = dimensionResource(R.dimen.space300),
                    ),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            SegmentedListItem(
                onClick = onPickUnfreeze,
                shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                contentPadding =
                    PaddingValues(
                        start = dimensionResource(R.dimen.space200),
                        end = dimensionResource(R.dimen.space200),
                        top = dimensionResource(R.dimen.space200),
                        bottom = dimensionResource(R.dimen.space200),
                    ),
                content = {
                    Text(
                        text = stringResource(R.string.action_select_unfreeze),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    val count = unfreezePackages.size
                    if (count > 0) {
                        CountBadge(count)
                    } else {
                        Icon(
                            imageVector = ChevronRightIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                        )
                    }
                },
            )
            SegmentedListItem(
                onClick = onPickLaunch,
                shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                contentPadding =
                    PaddingValues(
                        start = dimensionResource(R.dimen.space200),
                        end = dimensionResource(R.dimen.space200),
                        top = dimensionResource(R.dimen.space200),
                        bottom = dimensionResource(R.dimen.space200),
                    ),
                content = {
                    Text(
                        text = stringResource(R.string.action_select_launch),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                trailingContent = {
                    val count = if (launchPackage.isNotEmpty()) 1 else 0
                    if (count > 0) {
                        CountBadge(count)
                    } else {
                        Icon(
                            imageVector = ChevronRightIcon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun CountBadge(count: Int) {
    Box(
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.space300))
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Suppress("LongParameterList")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerContent(
    target: PickerTarget,
    apps: List<ApplicationInfo>,
    onConfirm: (Set<String>) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    var searchExpanded by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val selected = remember(target) { mutableStateSetOf<String>().apply { addAll(target.selected) } }
    val filtered by remember(apps, query, target.exclude) {
        derivedStateOf {
            val q = query.trim().lowercase()
            apps.filter { info ->
                if (info.packageName in target.exclude) {
                    false
                } else if (q.isEmpty()) {
                    true
                } else {
                    val name = AppInfo(info.packageName).name.lowercase()
                    name.contains(q)
                }
            }
        }
    }
    Column(
        modifier =
            Modifier
                .padding(
                    start = dimensionResource(R.dimen.space150),
                    top = dimensionResource(R.dimen.space200),
                    end = dimensionResource(R.dimen.space150),
                    bottom = dimensionResource(R.dimen.space300),
                ).imePadding()
                .navigationBarsPadding()
                .fillMaxHeight(),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { searchExpanded = it.isFocused }
                        .height(dimensionResource(R.dimen.space700)),
                placeholder = { Text(stringResource(R.string.action_search_apps)) },
                leadingIcon = {
                    if (searchExpanded) {
                        IconButton(onClick = {
                            searchExpanded = false
                            focusManager.clearFocus()
                        }) {
                            Icon(
                                ChevronLeftIcon,
                                contentDescription = stringResource(R.string.action_back),
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)).padding(start = dimensionResource(R.dimen.space100)),
                            )
                        }
                    } else {
                        Icon(SearchIcon, contentDescription = null, modifier = Modifier.size(dimensionResource(R.dimen.space300)).padding(start = dimensionResource(R.dimen.space100)))
                    }
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                CloseIcon24,
                                contentDescription = stringResource(R.string.action_clear),
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)).padding(end = dimensionResource(R.dimen.space100)),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(dimensionResource(R.dimen.space400)),
                colors =
                    TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
            )
            if (!searchExpanded || selected.isNotEmpty()) {
                Spacer(Modifier.width(dimensionResource(R.dimen.space150)))
                FilledIconButton(
                    onClick = { onConfirm(selected.toSet()) },
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                    modifier = Modifier.size(dimensionResource(R.dimen.space700)),
                ) {
                    Icon(CheckIcon24, contentDescription = stringResource(R.string.action_confirm), modifier = Modifier.size(dimensionResource(R.dimen.space300)))
                }
            }
        }
        Spacer(Modifier.height(dimensionResource(R.dimen.space150)))
        val sorted =
            filtered.sortedWith(
                compareByDescending<ApplicationInfo> { it.packageName in target.selected }
                    .thenBy { AppInfo(it.packageName).name.lowercase() },
            )
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(top = 0.dp, bottom = dimensionResource(R.dimen.space100)),
            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        ) {
            itemsIndexed(sorted, key = { _, info -> info.packageName }) { index, info ->
                val isSelected = info.packageName in selected
                SegmentedListItem(
                    onClick = {
                        if (target.multi) {
                            if (isSelected) {
                                selected.remove(info.packageName)
                            } else {
                                selected.add(info.packageName)
                            }
                        } else {
                            selected.clear()
                            selected.add(info.packageName)
                        }
                    },
                    selected = isSelected,
                    shapes = ListItemDefaults.segmentedShapes(index = index, count = sorted.size),
                    colors =
                        ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    contentPadding =
                        PaddingValues(
                            start = dimensionResource(R.dimen.space200),
                            end = dimensionResource(R.dimen.space200),
                            top = dimensionResource(R.dimen.space100),
                            bottom = dimensionResource(R.dimen.space100),
                        ),
                    modifier = Modifier.heightIn(min = dimensionResource(R.dimen.space700)),
                    leadingContent = {
                        val appInfo = remember(info.packageName) { AppInfo(info.packageName).applicationInfo }
                        appInfo?.let {
                            AppIcon(
                                info = it,
                                dimmed = false,
                                size = dimensionResource(R.dimen.space600),
                                internalPadding = dimensionResource(R.dimen.space50),
                                contentDescription = AppInfo(info.packageName).name,
                            )
                        }
                    },
                    content = {
                        Text(
                            text = AppInfo(info.packageName).name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        )
                    },
                    trailingContent = {
                        if (isSelected) {
                            Icon(
                                CheckIcon24,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
                            )
                        }
                    },
                )
            }
        }
    }
}
