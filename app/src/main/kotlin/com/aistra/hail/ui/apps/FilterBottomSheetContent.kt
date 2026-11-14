package com.aistra.hail.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.HailData

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
        TypeSection(onFilterChanged = onFilterChanged)
        SectionDivider()
        StatusSection(onFilterChanged = onFilterChanged)
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun TypeSection(onFilterChanged: () -> Unit) {
    var system by remember { mutableStateOf(HailData.filterSystemApps) }
    var user by remember { mutableStateOf(HailData.filterUserApps) }
    val selectedCount = (if (system) 1 else 0) + (if (user) 1 else 0)

    SectionTitle(text = stringResource(R.string.filter_apps_type), icon = Icons.Rounded.Apps)

    MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
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

    SectionTitle(text = stringResource(R.string.filter_freeze_state), icon = Icons.Rounded.CheckCircle)

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
    Row(
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(16.dp),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
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
