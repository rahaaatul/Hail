package com.aistra.hail.ui.backup

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.R
import com.aistra.hail.backup.Phase
import com.aistra.hail.backup.RestoreState
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class BackupRestoreFragment : MainFragment() {

    private val viewModel: BackupRestoreViewModel by viewModels()

    private val backupLauncher =
        registerForActivityResult(CreateDocument("application/zip")) { uri: Uri? ->
            uri?.let { viewModel.performBackup(it, requireContext()) }
        }

    private val restoreLauncher =
        registerForActivityResult(OpenDocument()) { uri: Uri? ->
            uri?.let { viewModel.onArchivePicked(it, requireContext()) }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val backupWorking = viewModel.backupState.value.phase == Phase.Working
                val restoreWorking = (viewModel.restoreState.value as? RestoreState.Loaded)?.phase == Phase.Working
                if (backupWorking || restoreWorking) {
                    Snackbar.make(activity.fab, R.string.msg_wait_for_operation, Snackbar.LENGTH_LONG).show()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        BackupRestoreScreen(
                            viewModel = viewModel,
                            onCreateBackup = { backupLauncher.launch("backup-${System.currentTimeMillis()}.zip") },
                            onChooseArchive = { restoreLauncher.launch(arrayOf("application/zip")) },
                            onRestoreConfirm = { showRestoreConfirmation() }
                        )
                    }
                }
            }
        }
    }
    
    private fun showRestoreConfirmation() {
        val state = viewModel.restoreState.value
        if (state !is RestoreState.Loaded) return

        val selectedCategories = mutableListOf<String>()
        if (state.options.apps) selectedCategories.add(getString(R.string.backup_apps))
        if (state.options.whitelist) selectedCategories.add(getString(R.string.backup_whitelist))
        if (state.options.actions) selectedCategories.add(getString(R.string.backup_actions))
        if (state.options.settings) selectedCategories.add(getString(R.string.backup_settings))

        val categoryList = selectedCategories.joinToString(", ")

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.msg_restore_confirm_title)
            .setMessage(getString(R.string.msg_restore_confirm_body, categoryList))
            .setPositiveButton(R.string.action_restore_selected) { _, _ ->
                viewModel.onRestoreConfirmed(requireContext())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}