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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HSystem
import com.aistra.hail.utils.HUI

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
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
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

    fun selectSort(sort: String) {
        if (sort == sortBy) {
            ascending = !ascending
            HailData.changeSort(sort, ascending)
        } else {
            if ((sort == HailData.SORT_LAST_USED || sort == HailData.SORT_MOST_USED) &&
                !HSystem.checkOpUsageStats(context)
            ) {
                HUI.showToast(R.string.msg_usage_access_required)
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
            sortBy = sort
            ascending = HailData.sortAscending(sort)
            HailData.changeSort(sort, ascending)
        }
        onFilterChanged()
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
                .padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                .width(72.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false),
                    onClick = onClick,
                ).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier =
                Modifier
                    .size(40.dp)
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
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun TypeSection(onFilterChanged: () -> Unit) {
    var system by remember { mutableStateOf(HailData.filterSystemApps) }
    var user by remember { mutableStateOf(HailData.filterUserApps) }
    val selectedCount = (if (system) 1 else 0) + (if (user) 1 else 0)

    SectionTitle(text = stringResource(R.string.filter_apps_type), icon = Widgets)

    MultiChoiceSegmentedButtonRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        SegmentedButton(
            checked = system,
            onCheckedChange = {
                toggleFilter(system, selectedCount, HailData.FILTER_SYSTEM_APPS, onFilterChanged) { system = it }
            },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            icon = {},
            label = { Text(text = stringResource(R.string.filter_system_apps)) },
            colors = segmentedColors(),
        )
        SegmentedButton(
            checked = user,
            onCheckedChange = {
                toggleFilter(user, selectedCount, HailData.FILTER_USER_APPS, onFilterChanged) { user = it }
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
    var enabled by remember { mutableStateOf(HailData.filterEnabledApps) }
    var disabled by remember { mutableStateOf(HailData.filterDisabledApps) }
    var suspended by remember { mutableStateOf(HailData.filterSuspendedApps) }
    var hidden by remember { mutableStateOf(HailData.filterHiddenApps) }
    var stopped by remember { mutableStateOf(HailData.filterStoppedApps) }
    val selectedCount =
        (if (enabled) 1 else 0) +
            (if (disabled) 1 else 0) +
            (if (suspended) 1 else 0) +
            (if (hidden) 1 else 0) +
            (if (stopped) 1 else 0)

    SectionTitle(text = stringResource(R.string.filter_freeze_state), icon = CheckCircle)

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatusChip(
            label = stringResource(R.string.state_running),
            selected = enabled,
            onClick = {
                toggleFilter(enabled, selectedCount, HailData.FILTER_ENABLED_APPS, onFilterChanged) { enabled = it }
            },
        )
        StatusChip(
            label = stringResource(R.string.state_disabled),
            selected = disabled,
            onClick = {
                toggleFilter(disabled, selectedCount, HailData.FILTER_DISABLED_APPS, onFilterChanged) { disabled = it }
            },
        )
        StatusChip(
            label = stringResource(R.string.state_suspended),
            selected = suspended,
            onClick = {
                toggleFilter(suspended, selectedCount, HailData.FILTER_SUSPENDED_APPS, onFilterChanged) { suspended = it }
            },
        )
        StatusChip(
            label = stringResource(R.string.state_hidden),
            selected = hidden,
            onClick = {
                toggleFilter(hidden, selectedCount, HailData.FILTER_HIDDEN_APPS, onFilterChanged) { hidden = it }
            },
        )
        StatusChip(
            label = stringResource(R.string.state_stopped),
            selected = stopped,
            onClick = {
                toggleFilter(stopped, selectedCount, HailData.FILTER_STOPPED_APPS, onFilterChanged) { stopped = it }
            },
        )
    }
}

@Composable
private fun SectionTitle(
    text: String,
    icon: ImageVector,
) {
    val textStyle = MaterialTheme.typography.titleSmall
    val symbolSize = textStyle.fontSize.value.dp
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(symbolSize),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = textStyle,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/**
 * Toggles a filter and persists it. The last selected filter in a section cannot be
 * unselected, so a group never becomes empty.
 */
private fun toggleFilter(
    current: Boolean,
    selectedCount: Int,
    filter: String,
    onFilterChanged: () -> Unit,
    onStateChange: (Boolean) -> Unit,
) {
    val newSelected = !current
    if (newSelected || selectedCount > 1) {
        onStateChange(newSelected)
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
        label = { Text(text = label, fontWeight = FontWeight.Normal) },
        colors =
            FilterChipDefaults.filterChipColors(
                containerColor = Color.Transparent,
                labelColor = MaterialTheme.colorScheme.onSurface,
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
    )
}
