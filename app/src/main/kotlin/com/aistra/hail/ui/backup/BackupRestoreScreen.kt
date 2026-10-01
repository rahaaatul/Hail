package com.aistra.hail.ui.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistra.hail.R
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupEntry
import com.aistra.hail.backup.BackupPreview.canStartBackup
import com.aistra.hail.backup.BackupPreview.canStartRestore
import com.aistra.hail.backup.Phase
import com.aistra.hail.backup.RestoreState
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.ui.theme.MaterialTheme
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.HUI
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()

    val canBackup = canStartBackup(backupState.options)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = dimensionResource(R.dimen.padding_medium),
            vertical = dimensionResource(R.dimen.padding_medium)
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_medium))
    ) {
        item {
            Text(
                text = stringResource(R.string.title_backup),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Backup option rows
        item {
            BackupOptionRow(
                category = BackupCategory.APPS,
                title = stringResource(R.string.backup_apps),
                summary = stringResource(R.string.summary_backup_apps),
                checked = backupState.options.apps,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.APPS, it) }
            )
        }
        item {
            BackupOptionRow(
                category = BackupCategory.WHITELIST,
                title = stringResource(R.string.backup_whitelist),
                summary = stringResource(R.string.summary_backup_whitelist),
                checked = backupState.options.whitelist,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.WHITELIST, it) }
            )
        }
        item {
            BackupOptionRow(
                category = BackupCategory.ACTIONS,
                title = stringResource(R.string.backup_actions),
                summary = stringResource(R.string.summary_backup_actions),
                checked = backupState.options.actions,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.ACTIONS, it) }
            )
        }
        item {
            BackupOptionRow(
                category = BackupCategory.SETTINGS,
                title = stringResource(R.string.backup_settings),
                summary = stringResource(R.string.summary_backup_settings),
                checked = backupState.options.settings,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.SETTINGS, it) }
            )
        }

        // Backup section action area: button / progress / result / disabled reason
        when (backupState.phase) {
            Phase.Idle -> {
                item {
                    BackupActionButton(
                        text = stringResource(R.string.action_create_backup),
                        enabled = canBackup,
                        onClick = { viewModel.startBackup(context) },
                        disabledReason = if (!canBackup) stringResource(R.string.msg_no_items_to_select) else null
                    )
                }
                if (!canBackup && backupState.message != null) {
                    item {
                        DisabledReasonText(text = backupState.message!!)
                    }
                }
            }
            Phase.Working -> {
                item {
                    ProgressRow(
                        label = stringResource(R.string.msg_exporting, backupState.message ?: "")
                    )
                }
            }
            Phase.Done, Phase.Failed -> {
                item {
                    ResultLine(
                        phase = backupState.phase,
                        message = backupState.message ?: ""
                    )
                }
            }
        }

        // Restore section header
        item {
            Text(
                text = stringResource(R.string.action_restore),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Restore section content
        when (restoreState) {
            is RestoreState.Empty -> {
                item {
                    RestoreFileSlotEmpty(
                        onClick = { viewModel.onArchivePicked(null, context) } // Fragment will handle the actual SAF launch
                    )
                }
            }
            is RestoreState.Loaded -> {
                item {
                    RestoreFileSlotLoaded(
                        state = restoreState,
                        onChangeClick = { viewModel.onChangeArchiveClick() }
                    )
                }
                // Restore entry rows
                restoreState.entries.forEach { entry ->
                    item {
                        RestoreEntryRow(
                            entry = entry,
                            checked = when (entry.category) {
                                BackupCategory.APPS -> restoreState.options.apps
                                BackupCategory.WHITELIST -> restoreState.options.whitelist
                                BackupCategory.ACTIONS -> restoreState.options.actions
                                BackupCategory.SETTINGS -> restoreState.options.settings
                            },
                            onCheckedChange = { viewModel.onRestoreOptionChange(entry.category, it) },
                            enabled = entry.present
                        )
                    }
                }
                // Restore section action area
                val canRestore = canStartRestore(restoreState)
                when (restoreState.phase) {
                    Phase.Idle -> {
                        item {
                            RestoreActionButton(
                                text = stringResource(R.string.action_restore_selected),
                                enabled = canRestore,
                                onClick = { /* confirmation dialog handled by Fragment */ },
                                disabledReason = if (!canRestore) stringResource(R.string.msg_no_items_to_select) else null
                            )
                        }
                        if (!canRestore && restoreState.message != null) {
                            item {
                                DisabledReasonText(text = restoreState.message!!)
                            }
                        }
                    }
                    Phase.Working -> {
                        item {
                            ProgressRow(
                                label = stringResource(R.string.msg_importing, restoreState.displayName)
                            )
                        }
                    }
                    Phase.Done, Phase.Failed -> {
                        item {
                            ResultLine(
                                phase = restoreState.phase,
                                message = restoreState.message ?: ""
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupOptionRow(
    category: BackupCategory,
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = summary) },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.padding_medium))
            )
        }
    )
}

@Composable
private fun BackupActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    disabledReason: String? = null
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled
    ) {
        Text(text = text)
    }
    if (!enabled && disabledReason != null) {
        DisabledReasonText(text = disabledReason)
    }
}

@Composable
private fun DisabledReasonText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = dimensionResource(R.dimen.padding_small))
    )
}

@Composable
private fun ProgressRow(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.Modifier.padding(start = dimensionResource(R.dimen.padding_medium)))
        Text(text = label)
    }
}

@Composable
private fun ResultLine(
    phase: Phase,
    message: String
) {
    val isSuccess = phase == Phase.Done
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (isSuccess) Icons.Outlined.CheckCircle else Icons.Outlined.ErrorOutline,
            contentDescription = null,
            tint = if (isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
        )
        androidx.compose.foundation.layout.Spacer(modifier = androidx.compose.foundation.layout.Modifier.padding(start = dimensionResource(R.dimen.padding_medium)))
        Text(
            text = message,
            color = if (isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RestoreFileSlotEmpty(onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onClick
        ) {
            Text(text = stringResource(R.string.action_choose_archive))
        }
        Text(
            text = stringResource(R.string.summary_choose_archive),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun RestoreFileSlotLoaded(
    state: RestoreState.Loaded,
    onChangeClick: () -> Unit
) {
    val sizeText = HUI.formatFileSize(state.sizeBytes)
    val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(state.file.lastModified())
    val summaryText = "$sizeText · $dateText"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.padding_small))
    ) {
        ListItem(
            modifier = Modifier.fillMaxWidth(),
            headlineContent = {
                Text(text = state.displayName, maxLines = 1, overflow = androidx.compose.ui.text.TextOverflow.Ellipsis)
            },
            supportingContent = {
                Text(text = summaryText, maxLines = 2, overflow = androidx.compose.ui.text.TextOverflow.Ellipsis)
            },
            trailingContent = {
                TextButton(onClick = onChangeClick) {
                    Text(text = stringResource(R.string.action_change_archive))
                }
            }
        )
    }
}

@Composable
private fun RestoreEntryRow(
    entry: BackupEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean
) {
    val categoryName = when (entry.category) {
        BackupCategory.APPS -> stringResource(R.string.backup_apps)
        BackupCategory.WHITELIST -> stringResource(R.string.backup_whitelist)
        BackupCategory.ACTIONS -> stringResource(R.string.backup_actions)
        BackupCategory.SETTINGS -> stringResource(R.string.backup_settings)
    }
    
    val supportingText = if (enabled) {
        HUI.formatFileSize(entry.sizeBytes)
    } else {
        stringResource(R.string.label_not_in_archive)
    }
    
    val supportingColor = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = if (enabled) onCheckedChange else null
            ),
        enabled = enabled,
        headlineContent = { Text(text = categoryName) },
        supportingContent = { Text(text = supportingText, color = supportingColor) },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.padding_medium)),
                enabled = enabled
            )
        }
    )
}

@Composable
private fun RestoreActionButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    disabledReason: String? = null
) {
    Button(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Text(text = text)
    }
    if (!enabled && disabledReason != null) {
        DisabledReasonText(text = disabledReason)
    }
}