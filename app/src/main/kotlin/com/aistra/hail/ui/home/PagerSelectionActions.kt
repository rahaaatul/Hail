package com.aistra.hail.ui.home

import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HUI
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

internal class PagerSelectionActions(
    private val fragment: PagerFragment,
    private val clipboardActions: PagerClipboardActions,
    private val tagActions: PagerTagActions
) {
    fun deselect(update: Boolean = true) {
        fragment.selectedList.clear()
        if (!update) return
        fragment.updateCurrentList()
        fragment.updateBarTitle()
        updateMultiselectButton()
    }

    fun onMultiselectClick() {
        fragment.multiselect = !fragment.multiselect
        if (fragment.multiselect) {
            fragment.updateBarTitle()
            HUI.showToast(R.string.tap_to_select)
        } else {
            deselect()
        }
        updateMultiselectButton()
    }

    fun onMultiselectLongClick() {
        val currentList = fragment.currentList
        if (currentList.isEmpty()) {
            HUI.showToast(R.string.no_items_to_select)
            return
        }
        val selectedList = fragment.selectedList
        val allSelected = selectedList.size == currentList.size &&
            currentList.all { it in selectedList }
        if (!fragment.multiselect || !allSelected) {
            fragment.multiselect = true
            selectedList.clear()
            selectedList.addAll(currentList)
            fragment.updateBarTitle()
            HUI.showToast(fragment.getString(R.string.msg_selected, selectedList.size.toString()))
        } else {
            deselect()
        }
        fragment.updateCurrentList()
        updateMultiselectButton()
    }

    fun showMultiSelectActions() {
        if (!fragment.isAdded) return
        val selectedList = fragment.selectedList
        val actions = listOf(
            R.string.action_freeze,
            R.string.action_unfreeze,
            R.string.action_tag_set,
            R.string.action_export_clipboard,
            R.string.action_remove_home,
            R.string.action_unfreeze_remove_home
        )
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(
            fragment.getString(R.string.msg_selected, selectedList.size.toString())
        ).setItems(actions.map(fragment::getString).toTypedArray()) { _, which ->
            when (actions[which]) {
                R.string.action_freeze -> {
                    fragment.setListFrozen(true, selectedList, false)
                    deselect()
                }

                R.string.action_unfreeze -> {
                    fragment.setListFrozen(false, selectedList, false)
                    deselect()
                }

                R.string.action_tag_set -> tagActions.showSelectedTagDialog()

                R.string.action_export_clipboard -> {
                    clipboardActions.export(selectedList)
                    deselect()
                }

                R.string.action_remove_home -> {
                    selectedList.forEach { fragment.removeCheckedApp(it.packageName, false) }
                    HailData.saveApps()
                    deselect()
                }

                R.string.action_unfreeze_remove_home -> {
                    val job = fragment.setListFrozen(false, selectedList, false)
                    fragment.lifecycleScope.launch {
                        job.join()
                        selectedList.forEach {
                            if (!AppManager.isAppFrozen(it.packageName)) {
                                fragment.removeCheckedApp(it.packageName, false)
                            }
                        }
                        HailData.saveApps()
                        deselect()
                    }
                }
            }
        }.setNegativeButton(R.string.action_deselect) { _, _ ->
            deselect()
        }.setNeutralButton(R.string.action_select_all) { _, _ ->
            val toAdd = fragment.currentList.filterNot { it in selectedList }
            selectedList.addAll(toAdd)
            fragment.updateCurrentList()
            fragment.updateBarTitle()
            if (toAdd.isNotEmpty()) showMultiSelectActions()
        }.show()
    }

    fun updateMultiselectButton() {
        val button = fragment.menu.findItem(R.id.action_multiselect)
            ?.actionView?.findViewById<ImageView>(R.id.multiselect_button)
        button?.let {
            it.setImageResource(
                if (fragment.multiselect) R.drawable.ic_outline_check else R.drawable.ic_outline_select_all
            )
            val colorAttr = if (fragment.multiselect) androidx.appcompat.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOnSurface
            val toolbar = fragment.requireActivity().findViewById<android.view.View>(R.id.toolbar)
            it.setColorFilter(MaterialColors.getColor(toolbar, colorAttr))
            it.contentDescription = if (fragment.multiselect) {
                fragment.getString(R.string.msg_selected, fragment.selectedList.size.toString())
            } else {
                fragment.getString(R.string.tap_to_select)
            }
        }
    }
}
