package com.aistra.hail.ui.home

import android.provider.Settings
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

internal class PagerItemActions(
    private val fragment: PagerFragment,
    private val clipboardActions: PagerClipboardActions,
    private val tagActions: PagerTagActions
) {
    fun show(info: AppInfo) {
        val packageName = info.packageName
        val frozen = AppManager.isAppFrozen(packageName)
        val actions = createMenu(info, frozen)
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(info.name).setItems(
            actions.map { it.first }.toTypedArray()
        ) { _, index ->
            handle(info, frozen, actions[index].second)
        }.setNeutralButton(R.string.action_details) { _, _ ->
            HUI.startActivity(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                HPackages.packageUri(packageName),
            )
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    fun handle(info: AppInfo, frozen: Boolean, actionId: Int) {
        when (actionId) {
            R.string.action_launch -> fragment.launchApp(info.packageName)
            R.string.action_freeze, R.string.action_unfreeze ->
                fragment.setListFrozen(!frozen, listOf(info))
            R.string.action_deferred_task -> showDeferredTaskDialog(info, frozen)
            R.string.action_pin, R.string.action_unpin -> {
                info.pinned = !info.pinned
                HailData.saveApps()
                fragment.updateCurrentList()
            }

            R.string.action_whitelist, R.string.action_remove_whitelist -> {
                info.whitelisted = !info.whitelisted
                HailData.saveApps()
                fragment.updateCurrentList()
            }

            R.string.action_tag_set -> tagActions.showAppTagDialog(info)
            R.string.action_add_pin_shortcut -> addPinShortcut(info)
            R.string.action_export_clipboard -> clipboardActions.export(listOf(info))
            R.string.action_remove_home -> fragment.removeCheckedApp(info.packageName)
            R.string.action_unfreeze_remove_home -> unfreezeAndRemoveHome(info)
            else -> error("Unhandled action id: $actionId")
        }
    }

    private fun createMenu(info: AppInfo, frozen: Boolean): List<Pair<String, Int>> {
        val actionIds = listOf(
            R.string.action_launch,
            R.string.action_freeze,
            R.string.action_unfreeze,
            R.string.action_deferred_task,
            R.string.action_pin,
            R.string.action_unpin,
            R.string.action_whitelist,
            R.string.action_remove_whitelist,
            R.string.action_tag_set,
            R.string.action_add_pin_shortcut,
            R.string.action_export_clipboard,
            R.string.action_remove_home,
            R.string.action_unfreeze_remove_home
        ).associateBy(fragment::getString)
        val hiddenActions = buildSet {
            add(if (frozen) R.string.action_freeze else R.string.action_unfreeze)
            add(if (info.pinned) R.string.action_pin else R.string.action_unpin)
            add(if (info.whitelisted) R.string.action_whitelist else R.string.action_remove_whitelist)
            if (!frozen) add(R.string.action_unfreeze_remove_home)
        }
        return fragment.resources.getStringArray(R.array.home_action_entries).map { entry ->
            entry to (actionIds[entry] ?: error("Unhandled action entry: $entry"))
        }.filterNot { (_, actionId) -> actionId in hiddenActions }
    }

    private fun showDeferredTaskDialog(info: AppInfo, frozen: Boolean) {
        val values = fragment.resources.getIntArray(R.array.deferred_task_values)
        val entries = values.map {
            fragment.resources.getQuantityString(R.plurals.deferred_task_entry, it, it)
        }.toTypedArray()
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(R.string.action_deferred_task)
            .setItems(entries) { _, index ->
                val minutes = values[index]
                HWork.setDeferredFrozen(info.packageName, !frozen, minutes.toLong())
                val deferredAction = fragment.getString(
                    if (!frozen) R.string.action_freeze else R.string.action_unfreeze
                )
                val activity = fragment.requireActivity() as com.aistra.hail.ui.main.MainActivity
                Snackbar.make(
                    activity.fab,
                    fragment.resources.getQuantityString(
                        R.plurals.msg_deferred_task, minutes, minutes, deferredAction, info.name
                    ),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.action_undo) { HWork.cancelWork(info.packageName) }.show()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun addPinShortcut(info: AppInfo) {
        val packageName = info.packageName
        val launchIntent = HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, packageName)
        val tabLayout = fragment.tabs
        if (tabLayout == null || tabLayout.tabCount <= 1) {
            HShortcuts.addPinShortcut(info, packageName, info.name, launchIntent)
            return
        }
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(R.string.action_unfreeze_tag)
            .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                HShortcuts.addPinShortcut(
                    info, packageName, info.name, launchIntent.addTag(HailData.tags[index].first)
                )
            }.setPositiveButton(R.string.action_skip) { _, _ ->
                HShortcuts.addPinShortcut(info, packageName, info.name, launchIntent)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun unfreezeAndRemoveHome(info: AppInfo) {
        val job = fragment.setListFrozen(false, listOf(info), false)
        fragment.viewLifecycleOwner.lifecycleScope.launch {
            job.join()
            if (!AppManager.isAppFrozen(info.packageName)) fragment.removeCheckedApp(info.packageName)
        }
    }
}
