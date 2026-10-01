package com.aistra.hail.ui.backup

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aistra.hail.R
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupEntry
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.canStartRestore
import com.aistra.hail.backup.Phase
import com.aistra.hail.backup.RestoreState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel,
    onCreateBackup: () -> Unit,
    onChooseArchive: () -> Unit,
    onRestoreConfirm: () -> Unit
) {
    val context = LocalContext.current
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()
    val restoreError by viewModel.restoreError.collectAsStateWithLifecycle()

    val canBackup = canStartBackup(backupState.options)

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
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
                title = stringResource(R.string.backup_apps),
                summary = stringResource(R.string.summary_backup_apps),
                checked = backupState.options.apps,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.APPS, it) },
                enabled = backupState.phase != Phase.Working
            )
        }
        item {
            BackupOptionRow(
                title = stringResource(R.string.backup_whitelist),
                summary = stringResource(R.string.summary_backup_whitelist),
                checked = backupState.options.whitelist,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.WHITELIST, it) },
                enabled = backupState.phase != Phase.Working
            )
        }
        item {
            BackupOptionRow(
                title = stringResource(R.string.backup_actions),
                summary = stringResource(R.string.summary_backup_actions),
                checked = backupState.options.actions,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.ACTIONS, it) },
                enabled = backupState.phase != Phase.Working
            )
        }
        item {
            BackupOptionRow(
                title = stringResource(R.string.backup_settings),
                summary = stringResource(R.string.summary_backup_settings),
                checked = backupState.options.settings,
                onCheckedChange = { viewModel.onBackupOptionChange(BackupCategory.SETTINGS, it) },
                enabled = backupState.phase != Phase.Working
            )
        }

        // Backup section action area: button / progress / result / disabled reason
        when (backupState.phase) {
            Phase.Idle -> {
                item {
                    BackupActionButton(
                        text = stringResource(R.string.action_create_backup),
                        enabled = canBackup,
                        onClick = onCreateBackup,
                        disabledReason = if (!canBackup) stringResource(R.string.msg_no_items_to_select) else null
                    )
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
                        onClick = onChooseArchive,
                        errorMessage = restoreError
                    )
                }
            }
            is RestoreState.Loaded -> {
                val loadedState = restoreState
                item {
                    RestoreFileSlotLoaded(
                        state = loadedState,
                        onChangeClick = { viewModel.onChangeArchiveClick() },
                        context = context
                    )
                }
                // Restore section action area
                val canRestore = canStartRestore(loadedState)
                when (loadedState.phase) {
                    Phase.Idle -> {
                        // Restore entry rows
                        loadedState.entries.forEach { entry ->
                            item {
                                RestoreEntryRow(
                                    entry = entry,
                                    checked = when (entry.category) {
                                        BackupCategory.APPS -> loadedState.options.apps
                                        BackupCategory.WHITELIST -> loadedState.options.whitelist
                                        BackupCategory.ACTIONS -> loadedState.options.actions
                                        BackupCategory.SETTINGS -> loadedState.options.settings
                                    },
                                    onCheckedChange = { viewModel.onRestoreOptionChange(entry.category, it) },
                                    entryPresent = entry.present,
                                    context = context
                                )
                            }
                        }
                        item {
                            RestoreActionButton(
                                text = stringResource(R.string.action_restore_selected),
                                enabled = canRestore,
                                onClick = onRestoreConfirm,
                                disabledReason = if (!canRestore) stringResource(R.string.msg_no_items_to_select) else null
                            )
                        }
                    }
                    Phase.Working -> {
                        item {
                            ProgressRow(
                                label = stringResource(R.string.msg_importing, loadedState.displayName)
                            )
                        }
                    }
                    Phase.Done, Phase.Failed -> {
                        item {
                            ResultLine(
                                phase = loadedState.phase,
                                message = loadedState.message ?: ""
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
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = if (enabled) onCheckedChange else { _ -> }
            ),
        headlineContent = { Text(text = title) },
        supportingContent = { Text(text = summary) },
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
        Spacer(modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_medium)))
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
        Spacer(modifier = Modifier.padding(start = dimensionResource(R.dimen.padding_medium)))
        Text(
            text = message,
            color = if (isSuccess) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun RestoreFileSlotEmpty(
    onClick: () -> Unit,
    errorMessage: String? = null
) {
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
        if (errorMessage != null) {
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        } else {
            Text(
                text = stringResource(R.string.summary_choose_archive),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RestoreFileSlotLoaded(
    state: RestoreState.Loaded,
    onChangeClick: () -> Unit,
    context: android.content.Context
) {
    val sizeText = Formatter.formatFileSize(context, state.sizeBytes)
    val dateText = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(state.file.lastModified())
    val summaryText = "$sizeText · $dateText"
    
    ListItem(
        modifier = Modifier.fillMaxWidth(),
        headlineContent = {
            Text(text = state.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(text = summaryText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        trailingContent = {
            TextButton(onClick = onChangeClick) {
                Text(text = stringResource(R.string.action_change_archive))
            }
        }
    )
}

@Composable
private fun RestoreEntryRow(
    entry: BackupEntry,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    entryPresent: Boolean,
    context: android.content.Context
) {
    val supportingText = if (entryPresent) {
        Formatter.formatFileSize(context, entry.sizeBytes)
    } else {
        stringResource(R.string.label_not_in_archive)
    }
    
    val supportingColor = if (entryPresent) 
        MaterialTheme.colorScheme.onSurfaceVariant 
    else 
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = if (entryPresent) onCheckedChange else { _ -> }
            ),
        headlineContent = { Text(text = entry.name) },
        supportingContent = { Text(text = supportingText, color = supportingColor) },
        leadingContent = {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.padding(end = dimensionResource(R.dimen.padding_medium)),
                enabled = entryPresent
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
        colors = ButtonDefaults.buttonColors(
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