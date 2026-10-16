package com.aistra.hail.ui.home

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.core.view.isVisible
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.ui.theme.AppTheme
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout

internal class PagerTagActions(private val fragment: PagerFragment) {
    fun showTagDialog(list: List<AppInfo>? = null) {
        val tabLayout = fragment.tabs ?: return
        val homeAdapter = fragment.adapter ?: return
        val binding = DialogInputBinding.inflate(fragment.layoutInflater)
        binding.inputLayout.setHint(R.string.tag)
        if (list == null) binding.editText.setText(fragment.tag?.first ?: "")
        MaterialAlertDialogBuilder(fragment.requireActivity())
            .setTitle(if (list != null) R.string.action_tag_add else R.string.action_tag_set)
            .setView(binding.root).setPositiveButton(android.R.string.ok) { _, _ ->
                saveTag(list, binding.editText.text.toString(), tabLayout, homeAdapter)
            }.apply {
                val position = tabLayout.selectedTabPosition
                if (list != null || position == 0) return@apply
                setNeutralButton(R.string.action_tag_remove) { _, _ ->
                    removeTag(position, tabLayout, homeAdapter)
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    fun showAppTagDialog(info: AppInfo) {
        val checkedItems = BooleanArray(HailData.tags.size) { index ->
            HailData.tags[index].second in info.tagIdList
        }
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(R.string.action_tag_set)
            .setMultiChoiceItems(HailData.tags.map { it.first }.toTypedArray(), checkedItems) { _, index, isChecked ->
                checkedItems[index] = isChecked
            }.setPositiveButton(android.R.string.ok) { _, _ ->
                info.tagIdList.clear()
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) info.tagIdList.add(HailData.tags[index].second)
                }
                if (info.tagIdList.isEmpty()) fragment.removeCheckedApp(info.packageName, false)
                HailData.saveApps()
                fragment.updateCurrentList()
            }.setNeutralButton(R.string.action_tag_add) { _, _ ->
                showTagDialog(listOf(info))
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    fun showSelectedTagDialog() {
        val selectedList = fragment.selectedList
        val initialStates = Array(HailData.tags.size) { index ->
            val tagId = HailData.tags[index].second
            when (selectedList.count { tagId in it.tagIdList }) {
                selectedList.size -> ToggleableState.On
                0 -> ToggleableState.Off
                else -> ToggleableState.Indeterminate
            }
        }
        val states = mutableStateListOf<ToggleableState>().apply {
            initialStates.forEach { add(it) }
        }
        MaterialAlertDialogBuilder(fragment.requireActivity()).setTitle(R.string.action_tag_set)
            .setView(ComposeView(fragment.requireActivity()).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    AppTheme {
                        TriStateTagList(HailData.tags.map { it.first }, initialStates, states)
                    }
                }
            }).setPositiveButton(android.R.string.ok) { _, _ ->
                selectedList.forEach {
                    states.forEachIndexed { index, state ->
                        val tagId = HailData.tags[index].second
                        when (state) {
                            ToggleableState.On -> {
                                if (tagId !in it.tagIdList) it.tagIdList.add(tagId)
                            }

                            ToggleableState.Off -> it.tagIdList.remove(tagId)
                            ToggleableState.Indeterminate -> Unit
                        }
                    }
                    if (it.tagIdList.isEmpty()) fragment.removeCheckedApp(it.packageName, false)
                }
                HailData.saveApps()
                fragment.selectionActions.deselect()
            }.setNeutralButton(R.string.action_tag_add) { _, _ ->
                showTagDialog(selectedList)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun saveTag(
        list: List<AppInfo>?,
        tagName: String,
        tabLayout: TabLayout,
        homeAdapter: HomeAdapter
    ) {
        val tagId = tagName.hashCode()
        if (HailData.tags.any { it.first == tagName || it.second == tagId }) return
        if (list == null) {
            renameTag(tagName, tagId, tabLayout, homeAdapter)
        } else {
            HailData.tags.add(tagName to tagId)
            homeAdapter.notifyItemInserted(homeAdapter.itemCount - 1)
            if (fragment.query.isEmpty() && tabLayout.tabCount == 2) tabLayout.isVisible = true
            if (list == fragment.selectedList) showSelectedTagDialog() else showAppTagDialog(list.first())
        }
        HailData.saveTags()
    }

    private fun renameTag(tagName: String, tagId: Int, tabLayout: TabLayout, homeAdapter: HomeAdapter) {
        val position = tabLayout.selectedTabPosition
        val isDefaultTab = position == 0
        val oldTagId = HailData.tags[position].second
        HailData.tags[position] = tagName to if (isDefaultTab) 0 else tagId
        if (!isDefaultTab) {
            val affectedApps = HailData.checkedList.toList().filter { oldTagId in it.tagIdList }
            affectedApps.forEach { app ->
                app.tagIdList.replaceAll { if (it == oldTagId) tagId else it }
            }
            HailData.saveApps()
        }
        homeAdapter.notifyItemChanged(position)
    }

    private fun removeTag(position: Int, tabLayout: TabLayout, homeAdapter: HomeAdapter) {
        val tagIdToRemove = HailData.tags[position].second
        val packagesToRemove = HailData.checkedList.toList().mapNotNull { app ->
            if (app.tagIdList.remove(tagIdToRemove) && app.tagIdList.isEmpty()) app.packageName else null
        }
        packagesToRemove.forEach { fragment.removeCheckedApp(it, false) }
        HailData.tags.removeAt(position)
        homeAdapter.notifyItemRemoved(position)
        if (tabLayout.tabCount == 1) tabLayout.isVisible = false
        HailData.saveApps()
        HailData.saveTags()
    }
}
