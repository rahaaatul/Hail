package com.aistra.hail.ui.actions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.R
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.ActionExecutor
import com.aistra.hail.utils.ActionsRepository
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.LaunchAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActionsFragment : MainFragment() {
    private val ui = ActionsUiState()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    val callbacks = remember { actionsCallbacks() }
                    ActionsScreen(ui = ui, callbacks = callbacks)
                }
            }
        }

    override fun onResume() {
        super.onResume()
        loadActions()
    }

    private fun loadActions() =
        viewLifecycleOwner.lifecycleScope.launch {
            ui.actions = ActionsRepository.loadAll()
            ui.pickerApps =
                withContext(Dispatchers.IO) {
                    AppMetaCache.getInstalledApplicationsCacheFirst()
                }
        }

    private fun execute(action: LaunchAction) =
        viewLifecycleOwner.lifecycleScope.launch {
            ActionExecutor
                .prepare(action)
                .onSuccess { startActivity(it) }
                .onFailure { HUI.showToast(it.message ?: getString(R.string.action_unavailable), true) }
        }

    private fun actionsCallbacks(): ActionsCallbacks =
        ActionsCallbacks(
            onExecute = { action -> execute(action) },
            onEdit = { action -> showEditor(action) },
            onDuplicate = { action ->
                viewLifecycleOwner.lifecycleScope.launch {
                    ActionsRepository.duplicate(action)
                    loadActions()
                }
            },
            onDelete = { action ->
                viewLifecycleOwner.lifecycleScope.launch {
                    ActionsRepository.delete(action.id)
                    loadActions()
                }
            },
            onCreateShortcut = { action -> HShortcuts.addActionShortcut(action) },
            onSaveAction = { existingId, launchPackage, unfreezePackages ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val saved =
                        ActionsRepository.save(
                            existingId ?: java.util.UUID
                                .randomUUID()
                                .toString(),
                            launchPackage,
                            unfreezePackages,
                        )
                    HShortcuts.updateActionShortcut(saved)
                    loadActions()
                }
            },
        )

    fun showEditor(existing: LaunchAction?) {
        if (existing != null) {
            ui.editorLaunch = existing.launchPackage
            ui.editorUnfreeze = existing.unfreezePackages
        }
        ui.editorTarget = existing?.let { EditorTarget.Edit(it) } ?: EditorTarget.New
    }
}
