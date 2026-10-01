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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupPreview.canStartBackup
import com.aistra.hail.backup.Phase
import com.aistra.hail.ui.theme.AppTheme

@Composable
fun BackupRestoreScreen(
    viewModel: BackupRestoreViewModel = viewModel()
) {
    val context = LocalContext.current
    val backupState by viewModel.backupState.collectAsStateWithLifecycle()
    val restoreState by viewModel.restoreState.collectAsStateWithLifecycle()

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
                style = HailData.MaterialTheme.typography.titleSmall,
                color = HailData.MaterialTheme.colorScheme.primary
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

        // Restore section header
        item {
            Text(
                text = stringResource(R.string.title_restore),
                style = HailData.MaterialTheme.typography.titleSmall,
                color = HailData.MaterialTheme.colorScheme.primary
            )
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