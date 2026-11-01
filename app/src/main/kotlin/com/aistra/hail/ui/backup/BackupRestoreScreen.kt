package com.aistra.hail.ui.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.checked

@Composable
fun BackupDialog(
    onDismissRequest: () -> Unit,
    viewModel: BackupRestoreViewModel,
    onCreateBackupClick: () -> Unit,
) {
    val backupState by viewModel.backupState.collectAsState()
    val backupOptions by viewModel.backupOptions.collectAsState()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = { Text(text = stringResource(R.string.section_backup)) },
        text = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                backupCategoryList(
                    backupOptions = backupOptions,
                    onToggle = viewModel::updateBackupOption,
                )
            }
        },
        confirmButton = {
            if (backupState.isWorking) {
                CircularProgressIndicator()
            } else {
                TextButton(
                    onClick = onCreateBackupClick,
                    enabled = canStartBackup(backupOptions),
                ) {
                    Text(text = stringResource(R.string.action_create_backup))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
fun RestoreDialog(
    onDismissRequest: () -> Unit,
    viewModel: BackupRestoreViewModel,
    onRestoreConfirmed: () -> Unit,
) {
    val restoreState by viewModel.restoreState.collectAsState()
    val restoreOptions by viewModel.restoreOptions.collectAsState()
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        confirmRestoreDialog(
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                onRestoreConfirmed()
            },
        )
    } else if (!restoreState.isWorking) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = { Text(text = stringResource(R.string.restore_backup)) },
            text = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    restoreCategoryList(
                        restoreState = restoreState,
                        restoreOptions = restoreOptions,
                        onToggle = viewModel::updateRestoreOption,
                    )
                }
            },
            confirmButton = {
                if (restoreState.isWorking) {
                    CircularProgressIndicator()
                } else {
                    TextButton(
                        onClick = { showConfirmDialog = true },
                        enabled = restoreState.entries.any { it.present },
                    ) {
                        Text(text = stringResource(R.string.action_restore))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun backupCategoryList(
    backupOptions: BackupOptions,
    onToggle: (BackupCategory, Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        BackupCategory.values().sortedBy { it.name }.forEach { category ->
            val isChecked = backupOptions.checked(category)
            Surface(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onToggle(category, !isChecked) },
                color = MaterialTheme.colorScheme.surfaceContainer,
                shape = RoundedCornerShape(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { onToggle(category, it) },
                    )
                    Text(text = stringResource(category.stringResId), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun restoreCategoryList(
    restoreState: RestoreUiState,
    restoreOptions: RestoreOptions,
    onToggle: (BackupCategory, Boolean) -> Unit,
) {
    val entriesByCategory = restoreState.entries.groupBy { it.category }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        BackupCategory.values().sortedBy { it.name }.forEach { category ->
            val entries = entriesByCategory[category].orEmpty()
            val isEnabled = entries.any { it.present }
            restoreCategoryItem(
                category = category,
                isChecked = restoreOptions.checked(category),
                isEnabled = isEnabled,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun restoreCategoryItem(
    category: BackupCategory,
    isChecked: Boolean,
    isEnabled: Boolean,
    onToggle: (BackupCategory, Boolean) -> Unit,
) {
    Surface(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable { onToggle(category, !isChecked) },
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isChecked,
                enabled = isEnabled,
                onCheckedChange = { onToggle(category, it) },
            )
            Text(text = stringResource(category.stringResId), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun confirmRestoreDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.dialog_confirm_restore_title)) },
        text = { Text(text = stringResource(R.string.dialog_confirm_restore_message)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(R.string.action_restore))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

internal val BackupCategory.stringResId: Int
    get() =
        when (this) {
            BackupCategory.APPS -> R.string.backup_category_apps
            BackupCategory.TAGS -> R.string.backup_category_tags
            BackupCategory.ACTIONS -> R.string.backup_category_actions
            BackupCategory.SETTINGS -> R.string.backup_category_settings
            BackupCategory.WHITELIST -> R.string.backup_category_whitelist
        }
