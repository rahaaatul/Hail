package com.aistra.hail.ui.home

internal const val INDEX_EXPORT_CLIPBOARD = 3
internal const val INDEX_REMOVE_HOME = 4
internal const val INDEX_UNFREEZE_REMOVE_HOME = 5

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.app.HailApi
import com.aistra.hail.ui.apps.AppsFragment
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.HIconCache
import com.aistra.hail.utils.HIsland
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.work.HWork
import kotlinx.coroutines.launch


internal fun mapActionIds(
    entries: List<String>,
): List<Int> {
    val launchStr = getString(R.string.action_launch)
    val freezeStr = getString(R.string.action_freeze)
    val unfreezeStr = getString(R.string.action_unfreeze)
    val deferredTaskStr = getString(R.string.action_deferred_task)
    val pinStr = getString(R.string.action_pin)
    val unpinStr = getString(R.string.action_unpin)
    val whitelistStr = getString(R.string.action_whitelist)
    val removeWhitelistStr = getString(R.string.action_remove_whitelist)
    val tagSetStr = getString(R.string.action_tag_set)
    val addPinShortcutStr = getString(R.string.action_add_pin_shortcut)
    val exportClipboardStr = getString(R.string.action_export_clipboard)
    val removeHomeStr = getString(R.string.action_remove_home)
    val unfreezeRemoveHomeStr = getString(R.string.action_unfreeze_remove_home)
    val mapping = mapOf(
        launchStr to R.string.action_launch,
        freezeStr to R.string.action_freeze,
        unfreezeStr to R.string.action_unfreeze,
        deferredTaskStr to R.string.action_deferred_task,
        pinStr to R.string.action_pin,
        unpinStr to R.string.action_unpin,
        whitelistStr to R.string.action_whitelist,
        removeWhitelistStr to R.string.action_remove_whitelist,
        tagSetStr to R.string.action_tag_set,
        addPinShortcutStr to R.string.action_add_pin_shortcut,
        exportClipboardStr to R.string.action_export_clipboard,
        removeHomeStr to R.string.action_remove_home,
        unfreezeRemoveHomeStr to R.string.action_unfreeze_remove_home,
    )
    return entries.map { entry ->
        mapping[entry] ?: check(false) { "Unhandled action entry: $entry" }
    }
}


internal fun tagDialog(info: AppInfo) {
    val checkedItems = BooleanArray(HailData.tags.size) { index ->
        HailData.tags[index].second in info.tagIdList
    }
    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setMultiChoiceItems(
        HailData.tags.map { it.first }.toTypedArray(),
        checkedItems,
    ) { _, index, isChecked ->
        checkedItems[index] = isChecked
    }.setPositiveButton(android.R.string.ok) { _, _ ->
        info.tagIdList.clear()
        checkedItems.forEachIndexed { index, checked ->
            if (checked) info.tagIdList.add(HailData.tags[index].second)
        }
        if (info.tagIdList.isEmpty()) {
            removeCheckedApp(info.packageName, false)
        }
        HailData.saveApps()
        updateCurrentList()
    }.setNeutralButton(R.string.action_tag_add) { _, _ ->
        showTagDialog(listOf(info))
    }.setNegativeButton(android.R.string.cancel, null).show()
}




internal fun onMultiselectLongClick() {
    val currentList = pagerAdapter.currentList
    if (currentList.isEmpty()) {
        HUI.showToast(R.string.no_items_to_select)
        return
    }

    val allSelected = selectedList.size == currentList.size &&
        currentList.all { it in selectedList }

    if (!multiselect || !allSelected) {
        multiselect = true
        selectedList.clear()
        selectedList.addAll(currentList)
        updateBarTitle()
        HUI.showToast(getString(R.string.msg_selected, selectedList.size.toString()))
    } else {
        deselect()
    }
    updateCurrentList()
    updateMultiselectButton()
}


internal fun updateMultiselectButton() {
    val multiselectItem = menu.findItem(R.id.action_multiselect)
    val button = multiselectItem?.actionView?.findViewById<ImageView>(R.id.multiselect_button)
    button?.let {
        it.setImageResource(
            if (multiselect) {
                R.drawable.ic_outline_check
            } else {
                R.drawable.ic_outline_select_all
            },
        )
        val colorAttr = if (multiselect) {
            androidx.appcompat.R.attr.colorPrimary
        } else {
            com.google.android.material.R.attr.colorOnSurface
        }
        val color = MaterialColors.getColor(activity.findViewById(R.id.toolbar), colorAttr)
        it.setColorFilter(color)
        it.contentDescription = if (multiselect) {
            getString(R.string.msg_selected, selectedList.size.toString())
        } else {
            getString(R.string.tap_to_select)
        }
    }
}


internal fun onMultiSelect() {
    if (!isAdded) return
    MaterialAlertDialogBuilder(activity).setTitle(
        getString(
            R.string.msg_selected,
            selectedList.size.toString(),
        ),
    ).setItems(
        intArrayOf(
            R.string.action_freeze,
            R.string.action_unfreeze,
            R.string.action_tag_set,
            R.string.action_export_clipboard,
            R.string.action_remove_home,
            R.string.action_unfreeze_remove_home,
        ).map { getString(it) }.toTypedArray(),
    ) { _, which ->
        when (which) {
            0 -> {
                setListFrozen(true, selectedList, false)
                deselect()
            }

            1 -> {
                setListFrozen(false, selectedList, false)
                deselect()
            }

            2 -> triStateTagDialog()

            INDEX_EXPORT_CLIPBOARD -> {
                exportToClipboard(selectedList)
                deselect()
            }

            INDEX_REMOVE_HOME -> {
                selectedList.forEach { removeCheckedApp(it.packageName, false) }
                HailData.saveApps()
                deselect()
            }

            INDEX_UNFREEZE_REMOVE_HOME -> {
                val job = setListFrozen(false, selectedList, false)
                lifecycleScope.launch {
                    job.join()
                    selectedList.forEach {
                        if (!AppManager.isAppFrozen(it.packageName)) removeCheckedApp(it.packageName, false)
                    }
                    HailData.saveApps()
                    deselect()
                }
            }
        }
    }.setNegativeButton(R.string.action_deselect) { _, _ ->
        deselect()
    }.setNeutralButton(R.string.action_select_all) { _, _ ->
        val toAdd = pagerAdapter.currentList.filterNot { it in selectedList }
        selectedList.addAll(toAdd)
        updateCurrentList()
        updateBarTitle()
        if (toAdd.isNotEmpty()) onMultiSelect()
    }.show()
}


internal fun triStateTagDialog() {
    val initialStates = Array(HailData.tags.size) { index ->
        val tagId = HailData.tags[index].second
        when (selectedList.count { tagId in it.tagIdList }) {
            selectedList.size -> ToggleableState.On
            0 -> ToggleableState.Off
            else -> ToggleableState.Indeterminate
        }
    }
    val states = initialStates.toMutableList()
    MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setView(
        ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { TriStateTagList(initialStates, states) } }
        },
    ).setPositiveButton(android.R.string.ok) { _, _ ->
        selectedList.forEach {
            states.forEachIndexed { index, state ->
                val tagId = HailData.tags[index].second
                when (state) {
                    ToggleableState.On -> {
                        if (tagId !in it.tagIdList) it.tagIdList.add(tagId)
                    }

                    ToggleableState.Off -> it.tagIdList.remove(tagId)

                    ToggleableState.Indeterminate -> {}
                }
            }
            if (it.tagIdList.isEmpty()) removeCheckedApp(it.packageName, false)
        }
        HailData.saveApps()
        deselect()
    }.setNeutralButton(R.string.action_tag_add) { _, _ ->
        showTagDialog(selectedList)
    }.setNegativeButton(android.R.string.cancel, null).show()
}





internal fun showTagDialog(list: List<AppInfo>? = null) {
    val tabLayout = tabs ?: return // The view has been destroyed; return directly.
    val homeAdapter = adapter ?: return

    val binding = DialogInputBinding.inflate(layoutInflater)
    setupTagDialogInput(binding, list)

    val title = if (list != null) R.string.action_tag_add else R.string.action_tag_set
    MaterialAlertDialogBuilder(activity).setTitle(title)
        .setView(binding.root)
        .setPositiveButton(android.R.string.ok) { _, _ ->
            onTagPositiveClicked(binding, list, homeAdapter, tabLayout)
        }
        .apply {
            val position = tabLayout.selectedTabPosition
            if (list != null || position == 0) return@apply
            setNeutralButton(R.string.action_tag_remove) { _, _ ->
                onTagNeutralClicked(position, tabLayout, homeAdapter)
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
}



internal fun onTagPositiveClicked(
    binding: DialogInputBinding,
    list: List<AppInfo>?,
    homeAdapter: HomeAdapter,
    tabLayout: TabLayout,
) {
    val tagName = binding.editText.text.toString()
    val tagId = tagName.hashCode()
    if (HailData.tags.any { it.first == tagName || it.second == tagId }) return
    if (list != null) {
        addTag(tagName, tagId, list, homeAdapter, tabLayout)
    } else {
        renameTag(tagName, tagId, tabLayout, homeAdapter)
    }
    HailData.saveTags()
}


internal fun addTag(
    tagName: String,
    tagId: Int,
    list: List<AppInfo>,
    homeAdapter: HomeAdapter,
    tabLayout: TabLayout,
) {
    HailData.tags.add(tagName to tagId)
    homeAdapter.notifyItemInserted(homeAdapter.itemCount - 1)
    if (query.isEmpty() && tabLayout.tabCount == 2) tabLayout.isVisible = true
    if (list == selectedList) triStateTagDialog() else tagDialog(list.first())
}


internal fun renameTag(
    tagName: String,
    tagId: Int,
    tabLayout: TabLayout,
    homeAdapter: HomeAdapter,
) {
    val position = tabLayout.selectedTabPosition
    val defaultTab = position == 0
    val oldTagId = HailData.tags[position].second
    HailData.tags[position] = tagName to if (defaultTab) 0 else tagId
    if (!defaultTab) {
        // Default tab (position 0) has tagId 0 meaning "no tag" and is
        // not renamed. Use snapshot to avoid
        // ConcurrentModificationException since tagIdList is shared
        // mutable state. This runs on the main thread (UI), so no
        // synchronization needed.
        updateCheckedAppsWithOldTagId(oldTagId, tagId)
        HailData.saveApps()
    }
    homeAdapter.notifyItemChanged(position)
}



internal fun onTagNeutralClicked(
    position: Int,
    tabLayout: TabLayout,
    homeAdapter: HomeAdapter,
) {
    val tagIdToRemove = HailData.tags[position].second
    val toRemove = mutableListOf<String>()
    val checkedSnapshot = HailData.checkedList.toList()
    checkedSnapshot.forEach {
        if (it.tagIdList.remove(tagIdToRemove) && it.tagIdList.isEmpty()) {
            toRemove.add(it.packageName)
        }
    }
    toRemove.forEach { removeCheckedApp(it, false) }
    HailData.tags.removeAt(position)
    homeAdapter.notifyItemRemoved(position)
    if (tabLayout.tabCount == 1) tabLayout.isVisible = false
    HailData.saveApps()
    HailData.saveTags()
}



internal fun PagerFragment.removeCheckedApp(packageName: String, saveApps: Boolean = true) {
HailData.removeCheckedApp(packageName, saveApps)
if (saveApps) updateCurrentList()
}


internal fun PagerFragment.showAppActionDialog(info: AppInfo) {
val pkg = info.packageName
val frozen = AppManager.isAppFrozen(pkg)
val filteredEntries = filterActionEntries(
    resources.getStringArray(R.array.home_action_entries), frozen, info,
)
val actionIds = mapActionIds(filteredEntries)
MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(filteredEntries.toTypedArray()) { _, which ->
    handleActionSelection(info, pkg, frozen, actionIds[which])
}.setNeutralButton(R.string.action_details) { _, _ ->
    HUI.startActivity(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        HPackages.packageUri(pkg),
    )
}.setNegativeButton(android.R.string.cancel, null).show()
}


internal fun PagerFragment.handleActionSelection(info: AppInfo, pkg: String, frozen: Boolean, actionId: Int) {
when (actionId) {
    R.string.action_launch -> launchApp(pkg)
    R.string.action_freeze, R.string.action_unfreeze -> setListFrozen(!frozen, listOf(info))
    R.string.action_deferred_task -> showDeferredTaskDialog(info, pkg, frozen)
    R.string.action_pin, R.string.action_unpin -> {
        info.pinned = !info.pinned
        HailData.saveApps()
        updateCurrentList()
    }
    R.string.action_whitelist, R.string.action_remove_whitelist -> {
        info.whitelisted = !info.whitelisted
        HailData.saveApps()
        updateCurrentList()
    }
    R.string.action_tag_set -> tagDialog(info)
    R.string.action_add_pin_shortcut -> addPinShortcut(info, pkg)
    R.string.action_export_clipboard -> exportToClipboard(listOf(info))
    R.string.action_remove_home -> removeCheckedApp(pkg)
    R.string.action_unfreeze_remove_home -> unfreezeAndRemoveHome(info, pkg)
}
}


internal fun PagerFragment.showDeferredTaskDialog(info: AppInfo, pkg: String, frozen: Boolean) {
val values = resources.getIntArray(R.array.deferred_task_values)
val entries = arrayOfNulls<String>(values.size)
values.forEachIndexed { i, value ->
    entries[i] = resources.getQuantityString(R.plurals.deferred_task_entry, value, value)
}
MaterialAlertDialogBuilder(activity).setTitle(R.string.action_deferred_task)
    .setItems(entries) { _, i ->
        HWork.setDeferredFrozen(pkg, !frozen, values[i].toLong())
        val deferredAction = getString(if (!frozen) R.string.action_freeze else R.string.action_unfreeze)
        Snackbar.make(
            activity.fab,
            resources.getQuantityString(
                R.plurals.msg_deferred_task,
                values[i],
                values[i],
                deferredAction,
                info.name,
            ),
            Snackbar.LENGTH_INDEFINITE,
        ).setAction(R.string.action_undo) { HWork.cancelWork(pkg) }.show()
    }.setNegativeButton(android.R.string.cancel, null).show()
}


internal fun PagerFragment.unfreezeAndRemoveHome(info: AppInfo, pkg: String) {
val job = setListFrozen(false, listOf(info), false)
lifecycleScope.launch {
    job.join()
    if (!AppManager.isAppFrozen(pkg)) removeCheckedApp(pkg)
}
}


