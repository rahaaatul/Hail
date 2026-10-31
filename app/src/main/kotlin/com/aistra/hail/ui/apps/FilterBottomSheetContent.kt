package com.aistra.hail.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistra.hail.app.HailData

/**
 * Standalone Compose filter bottom sheet content.
 *
 * Hosted today in [BottomSheetDialog] + [ComposeView] from [AppsFragment].
 * Drops directly into a pure [ModalBottomSheet] later when [AppsFragment] migrates
 * to Compose — zero UI rewrite needed.
 *
 * Design follows the Vector app's settings sheet pattern:
 * - Rounded surface container (Material 3 bottom sheet)
 * - Drag handle at top
 * - SheetHeading-style section titles (titleSmall, SemiBold, primary color)
 * - HorizontalDivider between sections
 * - Type: shared-edge segmented buttons (multi-select System|User)
 * - Status: multi-select filter chips (tap to toggle each; cannot unselect last)
 *
 * Filter changes call [onFilterChanged] immediately; the caller re-filters
 * the already-loaded app list (no PackageManager reload).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterBottomSheetContent(
    onFilterChanged: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    // ── Type: System | User (multi-select, can't unselect last) ──
    var typeSystem by remember { mutableStateOf(HailData.filterSystemApps) }
    var typeUser by remember { mutableStateOf(HailData.filterUserApps) }

    // ── Status: Enabled | Disabled | Suspended | Hidden | Stopped (multi-select) ──
    var statusEnabled by remember { mutableStateOf(HailData.filterEnabledApps) }
    var statusDisabled by remember { mutableStateOf(HailData.filterDisabledApps) }
    var statusSuspended by remember { mutableStateOf(HailData.filterSuspendedApps) }
    var statusHidden by remember { mutableStateOf(HailData.filterHiddenApps) }
    var statusStopped by remember { mutableStateOf(HailData.filterStoppedApps) }

    val typeSelectedCount = (if (typeSystem) 1 else 0) + (if (typeUser) 1 else 0)
    val statusSelectedCount =
        (if (statusEnabled) 1 else 0) +
            (if (statusDisabled) 1 else 0) +
            (if (statusSuspended) 1 else 0) +
            (if (statusHidden) 1 else 0) +
            (if (statusStopped) 1 else 0)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            // ── Drag handle ──
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp, 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)),
                )
            }

            // ── Type section ──
            Text(
                text = "Type",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                TypeSegment(
                    label = "System",
                    selected = typeSystem,
                    onClick = {
                        // Cannot unselect the last type chip.
                        if (typeSystem || typeSelectedCount > 1) {
                            typeSystem = true
                            HailData.changeAppsFilter(HailData.FILTER_SYSTEM_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
                TypeSegment(
                    label = "User",
                    selected = typeUser,
                    onClick = {
                        // Cannot unselect the last type chip.
                        if (typeUser || typeSelectedCount > 1) {
                            typeUser = true
                            HailData.changeAppsFilter(HailData.FILTER_USER_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // ── Status section ──
            Text(
                text = "Status",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            StatusChipRow(
                modifier = Modifier.fillMaxWidth(),
            ) {
                StatusChip(
                    label = "Enabled",
                    selected = statusEnabled,
                    onClick = {
                        if (statusEnabled) {
                            if (statusSelectedCount > 1) {
                                statusEnabled = false
                                HailData.changeAppsFilter(HailData.FILTER_ENABLED_APPS, false)
                                onFilterChanged()
                            }
                        } else {
                            statusEnabled = true
                            HailData.changeAppsFilter(HailData.FILTER_ENABLED_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
                StatusChip(
                    label = "Disabled",
                    selected = statusDisabled,
                    onClick = {
                        if (statusDisabled) {
                            if (statusSelectedCount > 1) {
                                statusDisabled = false
                                HailData.changeAppsFilter(HailData.FILTER_DISABLED_APPS, false)
                                onFilterChanged()
                            }
                        } else {
                            statusDisabled = true
                            HailData.changeAppsFilter(HailData.FILTER_DISABLED_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
                StatusChip(
                    label = "Suspended",
                    selected = statusSuspended,
                    onClick = {
                        if (statusSuspended) {
                            if (statusSelectedCount > 1) {
                                statusSuspended = false
                                HailData.changeAppsFilter(HailData.FILTER_SUSPENDED_APPS, false)
                                onFilterChanged()
                            }
                        } else {
                            statusSuspended = true
                            HailData.changeAppsFilter(HailData.FILTER_SUSPENDED_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
                StatusChip(
                    label = "Hidden",
                    selected = statusHidden,
                    onClick = {
                        if (statusHidden) {
                            if (statusSelectedCount > 1) {
                                statusHidden = false
                                HailData.changeAppsFilter(HailData.FILTER_HIDDEN_APPS, false)
                                onFilterChanged()
                            }
                        } else {
                            statusHidden = true
                            HailData.changeAppsFilter(HailData.FILTER_HIDDEN_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
                StatusChip(
                    label = "Stopped",
                    selected = statusStopped,
                    onClick = {
                        if (statusStopped) {
                            if (statusSelectedCount > 1) {
                                statusStopped = false
                                HailData.changeAppsFilter(HailData.FILTER_STOPPED_APPS, false)
                                onFilterChanged()
                            }
                        } else {
                            statusStopped = true
                            HailData.changeAppsFilter(HailData.FILTER_STOPPED_APPS, true)
                            onFilterChanged()
                        }
                    },
                )
            }
        }
    }
}

/**
 * Shared-edge segment pill for Type selection (System | User).
 * Uses [Button] styled as a segment — adjacent segments share edges via zero spacing.
 */
@Composable
private fun TypeSegment(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Button(
        modifier = Modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainer,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text = label)
    }
}

/**
 * Wrapper for status filter chips that supports wrapping.
 */
@Composable
private fun StatusChipRow(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
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
    Button(
        modifier = Modifier,
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else Color.Transparent,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text = label)
    }
}
