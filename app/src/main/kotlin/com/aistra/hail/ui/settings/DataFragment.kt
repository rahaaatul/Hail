package com.aistra.hail.ui.settings

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.aistra.hail.R
import com.aistra.hail.ui.backup.BackupDialog
import com.aistra.hail.ui.backup.BackupRestoreViewModel
import com.aistra.hail.ui.backup.BackupUiState
import com.aistra.hail.ui.backup.RestoreDialog
import com.aistra.hail.ui.backup.RestoreUiState
import com.aistra.hail.ui.theme.AppTheme
import me.zhanghai.compose.preference.ProvidePreferenceLocals

class DataFragment : Fragment() {
    private val viewModel: BackupRestoreViewModel by viewModels()
    private lateinit var composeView: ComposeView

    private val createBackupLauncher =
        registerForActivityResult(CreateDocument("application/zip")) { uri ->
            uri?.let { destinationUri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    destinationUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                viewModel.performBackup(destinationUri)
            }
        }

    private val chooseArchiveLauncher =
        registerForActivityResult(OpenDocument()) { uri ->
            uri?.let { archiveUri ->
                requireContext().contentResolver.takePersistableUriPermission(
                    archiveUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                viewModel.onArchivePicked(archiveUri)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        composeView =
            ComposeView(requireContext()).apply {
                setViewCompositionStrategy(
                    androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed,
                )
                setContent {
                    AppTheme {
                        ProvidePreferenceLocals {
                            DataScreen(
                                viewModel = viewModel,
                                onCreateBackupClick = {
                                    val appName = requireContext().getString(R.string.app_name).lowercase()
                                    val versionName =
                                        requireContext()
                                            .packageManager
                                            .getPackageInfo(requireContext().packageName, 0)
                                            .versionName
                                            ?.substringBefore("-g")
                                            ?: "unknown"
                                    createBackupLauncher.launch("$appName-$versionName-${System.currentTimeMillis()}.hail")
                                },
                                onChooseArchive = { chooseArchiveLauncher.launch(arrayOf("application/zip")) },
                            )
                        }
                    }
                }
            }
        return composeView
    }

    @Composable
    private fun DataScreen(
        viewModel: BackupRestoreViewModel,
        onCreateBackupClick: () -> Unit,
        onChooseArchive: () -> Unit,
    ) {
        var showBackupDialog by remember { mutableStateOf(false) }
        val backupState by viewModel.backupState.collectAsState()
        val restoreState by viewModel.restoreState.collectAsState()

        observeBackupToast(backupState)

        // Auto-dismiss the backup dialog once the backup completes successfully.
        LaunchedEffect(backupState) {
            if (!backupState.isWorking && backupState.message != null) {
                showBackupDialog = false
            }
        }

        observeRestoreToast(restoreState)

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        ) {
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable(onClick = { showBackupDialog = true }),
                leadingContent = { androidx.compose.material3.Icon(imageVector = Icons.Default.Save, contentDescription = null) },
                content = { Text(text = stringResource(R.string.action_create_backup)) },
                supportingContent = { Text(text = stringResource(R.string.summary_backup), style = MaterialTheme.typography.bodySmall) },
            )
            ListItem(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onChooseArchive),
                leadingContent = { androidx.compose.material3.Icon(imageVector = Icons.Default.History, contentDescription = null) },
                content = { Text(text = stringResource(R.string.action_restore)) },
                supportingContent = { Text(text = stringResource(R.string.summary_restore), style = MaterialTheme.typography.bodySmall) },
            )
        }
        if (showBackupDialog) {
            BackupDialog(
                onDismissRequest = { showBackupDialog = false },
                viewModel = viewModel,
                onCreateBackupClick = onCreateBackupClick,
            )
        }
        if (restoreState.fileName != null) {
            RestoreDialog(
                onDismissRequest = viewModel::clearStagedArchive,
                viewModel = viewModel,
                onRestoreConfirmed = viewModel::onRestoreConfirmed,
            )
        }
    }

    @Composable
    private fun observeBackupToast(state: BackupUiState) {
        LaunchedEffect(state) {
            if (!state.isWorking) {
                state.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                state.message?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
            }
        }
    }

    @Composable
    private fun observeRestoreToast(state: RestoreUiState) {
        LaunchedEffect(state) {
            if (!state.isWorking) {
                state.error?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
                state.message?.let { Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show() }
            }
        }
    }
}
