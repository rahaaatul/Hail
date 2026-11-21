package com.aistra.hail.ui.apps

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HSystem
import com.aistra.hail.utils.HUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Filter bottom sheet content, rendered inside a Material 3 [ModalBottomSheet].
 *
 * Type section uses shared-edge segment buttons (System|User, multi-select, at least one
 * selected). Status section uses multi-select filter chips (Running|Disabled|Suspended|
 * Hidden|Stopped); the last selected filter in each section cannot be unselected.
 * Tapping any filter re-filters the already-loaded app list immediately; the caller
 * re-filters via [onFilterChanged].
 */
@Composable
fun FilterBottomSheetContent(onFilterChanged: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = dimensionResource(R.dimen.space300))) {
        SortSection(onFilterChanged = onFilterChanged)
        SectionDivider()
        TypeSection(onFilterChanged = onFilterChanged)
        SectionDivider()
        StatusSection(onFilterChanged = onFilterChanged)
    }
}

@Composable
private fun SortSection(onFilterChanged: () -> Unit) {
    var sortBy by remember { mutableStateOf(HailData.sortBy) }
    var ascending by remember { mutableStateOf(HailData.sortAscending(HailData.sortBy)) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun selectSort(sort: String) {
        if (sort == sortBy) {
            ascending = !ascending
            HailData.changeSort(sort, ascending)
            onFilterChanged()
            return
        }
        // The sort itself is a local, synchronous operation: apply it immediately so a
        // quick dismiss or rapid taps cannot lose or race the selection.
        sortBy = sort
        ascending = HailData.sortAscending(sort)
        HailData.changeSort(sort, ascending)
        onFilterChanged()
        if (sort == HailData.SORT_LAST_USED || sort == HailData.SORT_MOST_USED) {
            // checkOpUsageStats is a binder call, keep it off the main thread; the sort
            // above stays applied even if access is missing.
            scope.launch {
                val granted = withContext(Dispatchers.IO) { HSystem.checkOpUsageStats(context) }
                if (!granted) {
                    HUI.showToast(R.string.msg_usage_access_required)
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            }
        }
    }

    SectionTitle(
        text = stringResource(R.string.sort_by),
        icon = if (ascending) MoreUp else MoreDown,
    )

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = dimensionResource(R.dimen.space300), vertical = dimensionResource(R.dimen.space50)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space100)),
    ) {
        SortItem(
            icon = SortByAlpha,
            label = stringResource(R.string.sort_name),
            selected = sortBy == HailData.SORT_NAME,
            onClick = { selectSort(HailData.SORT_NAME) },
        )
        SortItem(
            icon = ApkInstall,
            label = stringResource(R.string.sort_install),
            selected = sortBy == HailData.SORT_INSTALL,
            onClick = { selectSort(HailData.SORT_INSTALL) },
        )
        SortItem(
            icon = Upgrade,
            label = stringResource(R.string.sort_update),
            selected = sortBy == HailData.SORT_UPDATE,
            onClick = { selectSort(HailData.SORT_UPDATE) },
        )
        SortItem(
            icon = Package2,
            label = stringResource(R.string.sort_size),
            selected = sortBy == HailData.SORT_SIZE,
            onClick = { selectSort(HailData.SORT_SIZE) },
        )
        SortItem(
            icon = Today,
            label = stringResource(R.string.sort_last_used),
            selected = sortBy == HailData.SORT_LAST_USED,
            onClick = { selectSort(HailData.SORT_LAST_USED) },
        )
        SortItem(
            icon = TrendingUp,
            label = stringResource(R.string.sort_most_used),
            selected = sortBy == HailData.SORT_MOST_USED,
            onClick = { selectSort(HailData.SORT_MOST_USED) },
        )
    }
}

@Composable
private fun SortItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .width(dimensionResource(R.dimen.space900))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onClick,
                ).padding(vertical = dimensionResource(R.dimen.space50)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(dimensionResource(R.dimen.space500))
                    .background(
                        if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                        CircleShape,
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(dimensionResource(R.dimen.space300)),
            )
        }
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.space50)))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = dimensionResource(R.dimen.space300), vertical = dimensionResource(R.dimen.space100)),
        thickness = dimensionResource(R.dimen.divider_thickness),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun TypeSection(onFilterChanged: () -> Unit) {
    val filters =
        remember {
            mutableStateMapOf(
                HailData.FILTER_SYSTEM_APPS to HailData.filterSystemApps,
                HailData.FILTER_USER_APPS to HailData.filterUserApps,
            )
        }

    SectionTitle(text = stringResource(R.string.filter_apps_type), icon = Widgets)

    MultiChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(R.dimen.space300), vertical = dimensionResource(R.dimen.space50)),
    ) {
        SegmentedButton(
            checked = filters[HailData.FILTER_SYSTEM_APPS] ?: false,
            onCheckedChange = {
                toggleFilter(filters, HailData.FILTER_SYSTEM_APPS, onFilterChanged)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {},
            label = { Text(text = stringResource(R.string.filter_system_apps)) },
            colors = segmentedColors(),
        )
        SegmentedButton(
            checked = filters[HailData.FILTER_USER_APPS] ?: false,
            onCheckedChange = {
                toggleFilter(filters, HailData.FILTER_USER_APPS, onFilterChanged)
            },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            icon = {},
            label = { Text(text = stringResource(R.string.filter_user_apps)) },
            colors = segmentedColors(),
        )
    }
}

@Composable
private fun segmentedColors(): SegmentedButtonColors =
    SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
    )

@Composable
private fun StatusSection(onFilterChanged: () -> Unit) {
    val filters =
        remember {
            mutableStateMapOf(
                HailData.FILTER_ENABLED_APPS to HailData.filterEnabledApps,
                HailData.FILTER_DISABLED_APPS to HailData.filterDisabledApps,
                HailData.FILTER_SUSPENDED_APPS to HailData.filterSuspendedApps,
                HailData.FILTER_HIDDEN_APPS to HailData.filterHiddenApps,
                HailData.FILTER_STOPPED_APPS to HailData.filterStoppedApps,
                HailData.FILTER_UNINSTALLED_APPS to HailData.filterUninstalledApps,
            )
        }

    SectionTitle(text = stringResource(R.string.filter_freeze_state), icon = CheckCircle)

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.space300), vertical = dimensionResource(R.dimen.space50)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.space100)),
        ) {
            StatusFilters.forEach { (labelRes, filter) ->
                StatusChip(
                    label = stringResource(labelRes),
                    selected = filters[filter] ?: false,
                    onClick = { toggleFilter(filters, filter, onFilterChanged) },
                )
            }
        }
    }
}

private val StatusFilters =
    listOf(
        R.string.state_enabled to HailData.FILTER_ENABLED_APPS,
        R.string.state_disabled to HailData.FILTER_DISABLED_APPS,
        R.string.state_suspended to HailData.FILTER_SUSPENDED_APPS,
        R.string.state_hidden to HailData.FILTER_HIDDEN_APPS,
        R.string.state_stopped to HailData.FILTER_STOPPED_APPS,
        R.string.state_uninstalled to HailData.FILTER_UNINSTALLED_APPS,
    )

@Composable
private fun SectionTitle(
    text: String,
    icon: ImageVector,
) {
    val textStyle = MaterialTheme.typography.titleSmall
    val symbolSize = with(LocalDensity.current) { textStyle.fontSize.toDp() }
    Row(
        modifier =
            Modifier.padding(
                start = dimensionResource(R.dimen.space300),
                end = dimensionResource(R.dimen.space300),
                top = dimensionResource(R.dimen.space100),
                bottom = dimensionResource(R.dimen.space75),
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(symbolSize),
        )
        Spacer(modifier = Modifier.width(dimensionResource(R.dimen.space125)))
        Text(
            text = text,
            style = textStyle,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Toggles a filter and persists it. The last selected filter in a section cannot be
 * unselected, so a group never becomes empty. The selected count is read from the
 * current map state here, not captured at composition, so rapid taps are evaluated
 * against the live selection.
 */
private fun toggleFilter(
    filters: MutableMap<String, Boolean>,
    filter: String,
    onFilterChanged: () -> Unit,
) {
    val newSelected = !(filters[filter] ?: false)
    val selectedCount = filters.values.count { it }
    if (newSelected || selectedCount > 1) {
        filters[filter] = newSelected
        HailData.changeAppsFilter(filter, newSelected)
        onFilterChanged()
    }
}

/**
 * Individual status filter chip (multi-select toggle).
 * Tapping one chip toggles only that status; cannot unselect the last selected chip.
 */
@Composable
private fun StatusChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}
