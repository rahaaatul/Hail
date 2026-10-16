package com.aistra.hail.ui.home

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.widget.SearchView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppActions
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HShizuku
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HUI
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import com.aistra.hail.work.HWork
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PagerFragment : MainFragment(), MenuProvider {
    internal var query: String = String()
    internal var currentList by mutableStateOf(emptyList<AppInfo>(), neverEqualPolicy())
        private set
    internal var selectedPackages by mutableStateOf(emptySet<String>())
        private set
    private val clipboardActions = PagerClipboardActions(this)
    private val tagActions = PagerTagActions(this)
    internal val selectionActions = PagerSelectionActions(this, clipboardActions, tagActions)
    private val itemActions = PagerItemActions(this, clipboardActions, tagActions)
    private var _menu: Menu? = null
    internal val menu get() = _menu!!
    internal var multiselect: Boolean
        set(value) {
            (parentFragment as HomeFragment).multiselect = value
        }
        get() = (parentFragment as HomeFragment).multiselect
    internal val selectedList get() = (parentFragment as HomeFragment).selectedList
    internal val tabs: TabLayout? get() = (parentFragment as? HomeFragment)?.binding?.tabs
    internal val adapter: HomeAdapter? get() = (parentFragment as? HomeFragment)?.binding?.pager?.adapter as? HomeAdapter
    internal val tag: Pair<String, Int>? get() = tabs?.let { HailData.tags.getOrNull(it.selectedTabPosition) }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (multiselect) {
                    selectionActions.deselect()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        val columns = resources.getInteger(
            if (HailData.compactIcon) R.integer.home_span_compact else R.integer.home_span
        )
        val content = ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    PagerScreen(
                        apps = currentList,
                        selectedPackages = selectedPackages,
                        grayscaleIcon = HailData.grayscaleIcon,
                        homeFontSize = HailData.homeFontSize,
                        columns = columns,
                        isLandscape = activity.isLandscape,
                        onRefresh = ::updateCurrentList,
                        onItemClick = ::onItemClick,
                        onItemLongClick = { onItemLongClick(it) },
                        onScrolled = { activity.appbar.setLifted(it) },
                    )
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AppMetaCache.revision.collect { updateCurrentList() }
            }
        }
        return content
    }

    override fun onResume() {
        super.onResume()
        AppMetaCache.invalidateState(HailData.checkedList.map { it.packageName })
        AppMetaCache.prefetchPackages(HailData.checkedList.map { it.packageName })
        updateCurrentList()
        updateBarTitle()
        tabs?.let { tabLayout ->
            tabLayout.getTabAt(tabLayout.selectedTabPosition)?.view?.setOnLongClickListener {
                if (isResumed) tagActions.showTagDialog()
                true
            }
        }
        activity.fab.setOnLongClickListener(null)
    }

    internal fun updateCurrentList() {
        HailData.checkedList.filter { it.isInstalled }.filter {
            if (query.isEmpty()) tag?.second?.let { tagId -> tagId in it.tagIdList } ?: false
            else ((HailData.nineKeySearch && NineKeySearch.search(
                query, it.packageName, it.name
            )) || FuzzySearch.search(it.packageName, query) || FuzzySearch.search(
                it.name, query
            ) || PinyinSearch.searchPinyinAll(it.name, query))
        }.sortedWith(NameComparator).let {
            currentList = it
            selectedPackages = selectedList.mapTo(mutableSetOf()) { info -> info.packageName }
            app.setAutoFreezeService()
        }
    }

    internal fun updateBarTitle() {
        if (!isAdded) return
        activity.supportActionBar?.title =
            if (multiselect) getString(R.string.msg_selected, selectedList.size.toString())
            else getString(R.string.app_name)
    }

    fun onItemClick(info: AppInfo) {
        if (!isAdded) return
        when {
            multiselect -> {
                if (info in selectedList) selectedList.remove(info)
                else selectedList.add(info)
                updateCurrentList()
                updateBarTitle()
            }

            info.applicationInfo == null -> Snackbar.make(
                activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG
            ).setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }.show()

            else -> launchApp(info.packageName)
        }
    }

    fun onItemLongClick(info: AppInfo): Boolean {
        return when {
            info.applicationInfo == null && (!multiselect || info !in selectedList) -> {
                clipboardActions.export(listOf(info))
                true
            }

            info in selectedList -> {
                selectionActions.showMultiSelectActions()
                true
            }

            else -> {
                itemActions.show(info)
                true
            }
        }
    }

    internal fun launchApp(packageName: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (AppManager.isAppFrozen(packageName)) {
                AppActions.ensureUnfrozen(packageName).onSuccess {
                    updateCurrentList()
                }
            }
            AppActions.getLaunchIntent(packageName).onSuccess { intent ->
                HShortcuts.addDynamicShortcut(packageName)
                startActivity(intent)
            }.onFailure {
                HUI.showToast(R.string.activity_not_found)
            }
        }
    }

    internal fun setListFrozen(
        frozen: Boolean, list: List<AppInfo> = HailData.checkedList, updateList: Boolean = true
    ): Job {
        if (showWorkingModeGuidance()) {
            return viewLifecycleOwner.lifecycleScope.launch { }
        }
        val filtered = list.filter { it.isInstalled && AppManager.isAppFrozen(it.packageName) != frozen }
        return viewLifecycleOwner.lifecycleScope.launch {
            AppActions.freezePackages(frozen, filtered.map { it.packageName }).onSuccess {
                AppMetaCache.invalidateState(filtered.map { it.packageName })
                if (updateList) updateCurrentList()
                else currentList = currentList.toList()
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, filtered.size.toString()
                )
            }.onFailure {
                HUI.showToast(R.string.permission_denied)
            }
        }
    }

    internal fun showWorkingModeGuidance(): Boolean {
        val message = when {
            HailData.workingMode == HailData.MODE_DEFAULT -> R.string.msg_guide
            HailData.workingMode == HailData.MODE_SHIZUKU_HIDE &&
                runCatching { HShizuku.isRoot }.getOrNull() == false -> R.string.shizuku_hide_adb
            else -> null
        }
        message?.let {
            MaterialAlertDialogBuilder(activity).setMessage(it)
                .setPositiveButton(android.R.string.ok, null).show()
        }
        return message != null
    }

    private fun showTagDialog(list: List<AppInfo>? = null) {
        val tabLayout = tabs ?: return
        val homeAdapter = adapter ?: return

        val binding = DialogInputBinding.inflate(layoutInflater)
        binding.inputLayout.setHint(R.string.tag)
        list ?: binding.editText.setText(tag?.first ?: "")
        MaterialAlertDialogBuilder(activity)
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
            if (query.isEmpty() && tabLayout.tabCount == 2) tabLayout.isVisible = true
            if (list == selectedList) tagActions.showSelectedTagDialog()
            else tagActions.showAppTagDialog(list.first())
        }
        HailData.saveTags()
    }

    private fun renameTag(
        tagName: String,
        tagId: Int,
        tabLayout: TabLayout,
        homeAdapter: HomeAdapter
    ) {
        val position = tabLayout.selectedTabPosition
        val isDefaultTab = position == 0
        val oldTagId = HailData.tags[position].second
        HailData.tags[position] = tagName to if (isDefaultTab) 0 else tagId
        if (!isDefaultTab) {
            val checkedSnapshot = HailData.checkedList.toList()
            val affectedApps = checkedSnapshot.filter { oldTagId in it.tagIdList }
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
            if (app.tagIdList.remove(tagIdToRemove) && app.tagIdList.isEmpty()) {
                app.packageName
            } else {
                null
            }
        }
        packagesToRemove.forEach { removeCheckedApp(it, false) }
        HailData.tags.removeAt(position)
        homeAdapter.notifyItemRemoved(position)
        if (tabLayout.tabCount == 1) tabLayout.isVisible = false
        HailData.saveApps()
        HailData.saveTags()
    }

    private fun exportToClipboard(list: List<AppInfo>) {
        if (list.isEmpty()) return
        HUI.copyText(if (list.size > 1) JSONArray().run {
            list.forEach { put(it.packageName) }
            toString()
        } else list[0].packageName)
        HUI.showToast(
            R.string.msg_exported, if (list.size > 1) list.size.toString() else list[0].name
        )
    }

    private fun importFromClipboard() {
        val str = HUI.pasteText()
        if (str == null) {
            HUI.showToast(R.string.operation_failed, "Clipboard is empty", true)
            return
        }
        runCatching {
            val json = if (str.contains('[')) JSONArray(
                str.substring(
                    str.indexOf('[')..str.indexOf(']', str.indexOf('['))
                )
            ) else JSONArray().put(str)
            var importedCount = 0
            for (index in 0 until json.length()) {
                val packageName = json.getString(index)
                if (HPackages.getApplicationInfoOrNull(packageName) != null &&
                    !HailData.isChecked(packageName)
                ) {
                    HailData.addCheckedApp(packageName, tag?.second ?: 0, false)
                    importedCount++
                }
            }
            if (importedCount > 0) {
                HailData.saveApps()
                updateCurrentList()
            }
            HUI.showToast(getString(R.string.msg_imported, importedCount.toString()))
        }.onFailure {
            HUI.showToast(
                R.string.operation_failed, it.message ?: it.javaClass.simpleName, true
            )
        }
    }

    private suspend fun importFrozenApp() = withContext(Dispatchers.IO) {
        HPackages.getInstalledApplications().map { it.packageName }
            .filter { AppManager.isAppFrozen(it) && !HailData.isChecked(it) }
            .onEach { HailData.addCheckedApp(it, tag?.second ?: 0, false) }.size
    }

    internal fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) updateCurrentList()
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_freeze_current -> setListFrozen(true, currentList.filterNot { it.whitelisted })

            R.id.action_unfreeze_current -> setListFrozen(false, currentList)
            R.id.action_freeze_all -> setListFrozen(true)
            R.id.action_unfreeze_all -> setListFrozen(false)
            R.id.action_freeze_non_whitelisted -> setListFrozen(true, HailData.checkedList.filterNot { it.whitelisted })

            R.id.action_import_clipboard -> importFromClipboard()
            R.id.action_import_frozen -> lifecycleScope.launch {
                val size = importFrozenApp()
                if (size > 0) {
                    HailData.saveApps()
                    updateCurrentList()
                }
                HUI.showToast(getString(R.string.msg_imported, size.toString()))
            }

            R.id.action_export_current -> exportToClipboard(currentList)
            R.id.action_export_all -> exportToClipboard(HailData.checkedList)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        _menu = menu
        inflater.inflate(R.menu.menu_home, menu)
        val searchView = menu.findItem(R.id.action_search).actionView as SearchView
        if (HailData.nineKeySearch) {
            val editText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
            editText.inputType = InputType.TYPE_CLASS_PHONE
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            private var inited = false
            override fun onQueryTextChange(newText: String): Boolean {
                if (inited) {
                    query = newText
                    tabs?.run {
                        isVisible = query.isEmpty() && tabCount > 1
                    }
                    updateCurrentList()
                } else inited = true
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })

        val multiselectItem = menu.findItem(R.id.action_multiselect)
        val actionView = multiselectItem.actionView
        val button = actionView?.findViewById<ImageView>(R.id.multiselect_button)

        button?.setOnClickListener { selectionActions.onMultiselectClick() }
        button?.setOnLongClickListener { selectionActions.onMultiselectLongClick(); true }

        selectionActions.updateMultiselectButton()
    }

    override fun onDestroyView() {
        _menu = null
        super.onDestroyView()
    }
}
