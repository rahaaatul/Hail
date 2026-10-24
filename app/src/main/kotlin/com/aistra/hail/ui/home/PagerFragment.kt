package com.aistra.hail.ui.home

import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailApi.addTag
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.databinding.FragmentPagerBinding
import com.aistra.hail.extensions.applyDefaultInsetter
import com.aistra.hail.extensions.isLandscape
import com.aistra.hail.extensions.isRtl
import com.aistra.hail.extensions.marginRelative
import com.aistra.hail.extensions.paddingRelative
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
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

class PagerFragment :
    MainFragment(),
    PagerAdapter.OnItemClickListener,
    PagerAdapter.OnItemLongClickListener,
    MenuProvider {
    private var query: String = String()
    private var _binding: FragmentPagerBinding? = null
    private val binding get() = _binding!!
    private lateinit var pagerAdapter: PagerAdapter
    private var _menu: Menu? = null
    private val menu get() = _menu!!
    private var multiselect: Boolean
        set(value) {
            (parentFragment as HomeFragment).multiselect = value
        }
        get() = (parentFragment as HomeFragment).multiselect
    private val selectedList get() = (parentFragment as HomeFragment).selectedList
    private val tabs: TabLayout? get() = (parentFragment as? HomeFragment)?.binding?.tabs
    private val adapter: HomeAdapter? get() = (parentFragment as? HomeFragment)?.binding?.pager?.adapter as? HomeAdapter
    private val tag: Pair<String, Int>? get() = tabs?.let { HailData.tags.getOrNull(it.selectedTabPosition) }

    private const val INDEX_EXPORT_CLIPBOARD = 3
    private const val INDEX_REMOVE_HOME = 4
    private const val INDEX_UNFREEZE_REMOVE_HOME = 5

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (multiselect) {
                        deselect()
                    } else {
                        isEnabled = false
                        requireActivity().onBackPressedDispatcher.onBackPressed()
                    }
                }
            },
        )
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        pagerAdapter = PagerAdapter(selectedList).apply {
            onItemClickListener = this@PagerFragment
            onItemLongClickListener = this@PagerFragment
        }
        binding.recyclerView.run {
            layoutManager = GridLayoutManager(
                activity,
                resources.getInteger(
                    if (HailData.compactIcon) R.integer.home_span_compact else R.integer.home_span,
                ),
            )
            adapter = pagerAdapter
            applyDefaultInsetter { paddingRelative(isRtl, bottom = isLandscape) }
        }

        binding.refresh.apply {
            setOnRefreshListener {
                updateCurrentList()
                binding.refresh.isRefreshing = false
            }
            applyDefaultInsetter { marginRelative(isRtl, start = !isLandscape, end = true) }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                AppMetaCache.revision.collect { updateCurrentList() }
            }
        }
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        AppMetaCache.invalidateState(HailData.checkedList.map { it.packageName })
        AppMetaCache.prefetchPackages(HailData.checkedList.map { it.packageName })
        updateCurrentList()
        updateBarTitle()
        activity.appbar.setLiftOnScrollTargetView(binding.recyclerView)
        tabs?.let { tabLayout ->
            tabLayout.getTabAt(tabLayout.selectedTabPosition)?.view?.setOnLongClickListener {
                if (isResumed) showTagDialog()
                true
            }
        }
        activity.fab.setOnLongClickListener(null)
    }

    internal fun updateCurrentList() {
        val binding = _binding ?: return
        HailData.checkedList.filter { it.isInstalled }.filter {
            if (query.isEmpty()) {
                tag?.second?.let { tagId -> tagId in it.tagIdList } ?: false
            } else {
                (
                    (
                        HailData.nineKeySearch && NineKeySearch.search(
                            query,
                            it.packageName,
                            it.name,
                        )
                        ) || FuzzySearch.search(it.packageName, query) || FuzzySearch.search(
                        it.name,
                        query,
                    ) || PinyinSearch.searchPinyinAll(it.name, query)
                    )
            }
        }.sortedWith(NameComparator).let {
            binding.empty.isVisible = it.isEmpty()
            pagerAdapter.submitList(it)
            app.setAutoFreezeService()
        }
    }


    override fun onItemClick(info: AppInfo) {
        if (isAdded) {
            if (multiselect) {
                if (info in selectedList) {
                    selectedList.remove(info)
                } else {
                    selectedList.add(info)
                }
                updateCurrentList()
                updateBarTitle()
            } else if (info.applicationInfo == null) {
                Snackbar.make(activity.fab, R.string.app_not_installed, Snackbar.LENGTH_LONG)
                    .setAction(R.string.action_remove_home) { removeCheckedApp(info.packageName) }.show()
            } else {
                launchApp(info.packageName)
            }
        }
    }

    override fun onItemLongClick(info: AppInfo): Boolean {
        if (info.applicationInfo == null && (!multiselect || info !in selectedList)) {
            exportToClipboard(listOf(info))
        } else if (info in selectedList) {
            onMultiSelect()
        } else {
        showAppActionDialog(info)
    }
    return true
}

    }

    private fun filterActionEntries(
        entries: Array<String>, frozen: Boolean, info: AppInfo,
        freezeStr: String, unfreezeStr: String, pinStr: String, unpinStr: String,
        whitelistStr: String, removeWhitelistStr: String, unfreezeRemoveHomeStr: String,
    ): List<String> = entries.filter {
        (it != freezeStr || !frozen) && (it != unfreezeStr || frozen) && (it != pinStr || !info.pinned) &&
            (it != unpinStr || info.pinned) && (it != whitelistStr || !info.whitelisted) &&
            (it != removeWhitelistStr || info.whitelisted) && (it != unfreezeRemoveHomeStr || frozen)
    }

    private fun mapActionIds(
        entries: List<String>, launchStr: String, freezeStr: String, unfreezeStr: String,
        deferredTaskStr: String, pinStr: String, unpinStr: String, whitelistStr: String,
        removeWhitelistStr: String, tagSetStr: String, addPinShortcutStr: String,
        exportClipboardStr: String, removeHomeStr: String, unfreezeRemoveHomeStr: String,
    ): List<Int> {
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

    private fun tagDialog(info: AppInfo) {
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



    private fun onMultiselectLongClick() {
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

    private fun updateMultiselectButton() {
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

    private fun onMultiSelect() {
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

    private fun triStateTagDialog() {
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




    private fun showTagDialog(list: List<AppInfo>? = null) {
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


    private fun onTagPositiveClicked(
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

    private fun addTag(
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

    private fun renameTag(
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


    private fun onTagNeutralClicked(
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


    private fun importFromClipboard() = runCatching {
        val str = HUI.pasteText() ?: throw IllegalArgumentException()
        val json = if (str.contains('[')) {
            JSONArray(
                str.substring(
                    str.indexOf('[')..str.indexOf(']', str.indexOf('[')),
                ),
            )
        } else {
            JSONArray().put(str)
        }
        var i = 0
        for (index in 0 until json.length()) {
            val pkg = json.getString(index)
            if (HPackages.getApplicationInfoOrNull(pkg) != null && !HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, tag?.second ?: 0, false)
                i++
            }
        }
        if (i > 0) {
            HailData.saveApps()
            updateCurrentList()
        }
        HUI.showToast(getString(R.string.msg_imported, i.toString()))
    }

    private suspend fun importFrozenApp() = withContext(Dispatchers.IO) {
        HPackages.getInstalledApplications().map { it.packageName }
            .filter { AppManager.isAppFrozen(it) && !HailData.isChecked(it) }
            .onEach { HailData.addCheckedApp(it, tag?.second ?: 0, false) }.size
    }


    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_freeze_current -> setListFrozen(true, pagerAdapter.currentList.filterNot { it.whitelisted })

            R.id.action_unfreeze_current -> setListFrozen(false, pagerAdapter.currentList)

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

            R.id.action_export_current -> exportToClipboard(pagerAdapter.currentList)

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
                } else {
                    inited = true
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })

        val multiselectItem = menu.findItem(R.id.action_multiselect)
        val actionView = multiselectItem.actionView
        val button = actionView?.findViewById<ImageView>(R.id.multiselect_button)

        button?.setOnClickListener { onMultiselectClick() }
        button?.setOnLongClickListener {
            onMultiselectLongClick()
            true
        }

        updateMultiselectButton()
    }

    override fun onDestroyView() {
        _menu = null
        pagerAdapter.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}

private fun PagerFragment.updateBarTitle() {
    if (!isAdded) return
    activity.supportActionBar?.title =
        if (multiselect) {
            getString(R.string.msg_selected, selectedList.size.toString())
        } else {
            getString(R.string.app_name)
        }
}

private fun PagerFragment.deselect(update: Boolean = true) {
    selectedList.clear()
    if (!update) return
    updateCurrentList()
    updateBarTitle()
    updateMultiselectButton()
}

private fun PagerFragment.onMultiselectClick() {
    multiselect = !multiselect
    if (multiselect) {
        updateBarTitle()
        HUI.showToast(R.string.tap_to_select)
    } else {
        deselect()
    }
    updateMultiselectButton()
}

private fun PagerFragment.launchApp(packageName: String) {
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

private fun PagerFragment.setListFrozen(
    frozen: Boolean,
    list: List<AppInfo> = HailData.checkedList,
    updateList: Boolean = true,
): Job {
    val earlyJob: Job? = if (HailData.workingMode == HailData.MODE_DEFAULT) {
        MaterialAlertDialogBuilder(activity).setMessage(R.string.msg_guide)
            .setPositiveButton(android.R.string.ok, null).show()
        viewLifecycleOwner.lifecycleScope.launch { }
    } else if (HailData.workingMode == HailData.MODE_SHIZUKU_HIDE) {
        var hideJob: Job? = null
        runCatching { HShizuku.isRoot }.onSuccess {
            if (!it) {
                MaterialAlertDialogBuilder(activity).setMessage(R.string.shizuku_hide_adb)
                    .setPositiveButton(android.R.string.ok, null).show()
                hideJob = viewLifecycleOwner.lifecycleScope.launch { }
            }
        }
        hideJob
    } else null
    return earlyJob ?: run {
        val filtered = list.filter { it.isInstalled && AppManager.isAppFrozen(it.packageName) != frozen }
        viewLifecycleOwner.lifecycleScope.launch {
            AppActions.freezePackages(frozen, filtered.map { it.packageName }).onSuccess {
                AppMetaCache.invalidateState(filtered.map { it.packageName })
                if (updateList) updateCurrentList()
                pagerAdapter.refreshVisualState()
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze,
                    filtered.size.toString(),
                )
            }.onFailure {
                HUI.showToast(R.string.permission_denied)
            }
        }
    }
}

private fun updateCheckedAppsWithOldTagId(oldTagId: Int, tagId: Int) {
    val checkedSnapshot = HailData.checkedList.toList()
    val toUpdate = checkedSnapshot.filter { oldTagId in it.tagIdList }
    toUpdate.forEach { it.tagIdList.replaceAll { if (it == oldTagId) tagId else it } }
}

@Composable
private fun TriStateTagList(initialStates: Array<ToggleableState>, states: MutableList<ToggleableState>) = Column(
    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
) {
    Spacer(modifier = Modifier.height(16.dp))
    HailData.tags.forEachIndexed { index, tag ->
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                states[index] = if (initialStates[index] == ToggleableState.Indeterminate) {
                    when (states[index]) {
                        ToggleableState.On -> ToggleableState.Off
                        ToggleableState.Off -> ToggleableState.Indeterminate
                        ToggleableState.Indeterminate -> ToggleableState.On
                    }
                } else if (states[index] == ToggleableState.On) {
                    ToggleableState.Off
                } else {
                    ToggleableState.On
                }
            }.padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TriStateCheckbox(
                state = states[index],
                onClick = null,
                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary),
            )
            Spacer(modifier = Modifier.width(24.dp))
            Text(
                text = tag.first,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

private fun PagerFragment.setupTagDialogInput(binding: DialogInputBinding, list: List<AppInfo>?) {
    binding.inputLayout.setHint(R.string.tag)
    list ?: binding.editText.setText(tag?.first ?: "")
}

private fun PagerFragment.exportToClipboard(list: List<AppInfo>) {
    if (list.isEmpty()) return
    HUI.copyText(
        if (list.size > 1) {
            JSONArray().run {
                list.forEach { put(it.packageName) }
                toString()
            }
        } else {
            list[0].packageName
        },
    )
    HUI.showToast(
        R.string.msg_exported,
        if (list.size > 1) list.size.toString() else list[0].name,
    )
}

private fun PagerFragment.removeCheckedApp(packageName: String, saveApps: Boolean = true) {
    HailData.removeCheckedApp(packageName, saveApps)
    if (saveApps) updateCurrentList()
}

private fun PagerFragment.showAppActionDialog(info: AppInfo) {
    val pkg = info.packageName
    val frozen = AppManager.isAppFrozen(pkg)
    val freezeStr = getString(R.string.action_freeze)
    val unfreezeStr = getString(R.string.action_unfreeze)
    val pinStr = getString(R.string.action_pin)
    val unpinStr = getString(R.string.action_unpin)
    val whitelistStr = getString(R.string.action_whitelist)
    val removeWhitelistStr = getString(R.string.action_remove_whitelist)
    val unfreezeRemoveHomeStr = getString(R.string.action_unfreeze_remove_home)
    val launchStr = getString(R.string.action_launch)
    val deferredTaskStr = getString(R.string.action_deferred_task)
    val tagSetStr = getString(R.string.action_tag_set)
    val addPinShortcutStr = getString(R.string.action_add_pin_shortcut)
    val exportClipboardStr = getString(R.string.action_export_clipboard)
    val removeHomeStr = getString(R.string.action_remove_home)
    val filteredEntries = filterActionEntries(
        resources.getStringArray(R.array.home_action_entries), frozen, info,
        freezeStr, unfreezeStr, pinStr, unpinStr, whitelistStr, removeWhitelistStr, unfreezeRemoveHomeStr,
    )
    val actionIds = mapActionIds(
        filteredEntries, launchStr, freezeStr, unfreezeStr, deferredTaskStr, pinStr, unpinStr,
        whitelistStr, removeWhitelistStr, tagSetStr, addPinShortcutStr, exportClipboardStr, removeHomeStr,
        unfreezeRemoveHomeStr,
    )
    MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(filteredEntries.toTypedArray()) { _, which ->
        handleActionSelection(info, pkg, frozen, actionIds[which])
    }.setNeutralButton(R.string.action_details) { _, _ ->
        HUI.startActivity(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            HPackages.packageUri(pkg),
        )
    }.setNegativeButton(android.R.string.cancel, null).show()
}

private fun PagerFragment.handleActionSelection(info: AppInfo, pkg: String, frozen: Boolean, actionId: Int) {
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

private fun PagerFragment.showDeferredTaskDialog(info: AppInfo, pkg: String, frozen: Boolean) {
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
                resources.getQuantityString(R.plurals.msg_deferred_task, values[i], values[i], deferredAction, info.name),
                Snackbar.LENGTH_INDEFINITE,
            ).setAction(R.string.action_undo) { HWork.cancelWork(pkg) }.show()
        }.setNegativeButton(android.R.string.cancel, null).show()
}

private fun PagerFragment.addPinShortcut(info: AppInfo, pkg: String) {
    tabs?.takeIf { it.tabCount > 1 }?.let {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
            .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                HShortcuts.addPinShortcut(info, pkg, info.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg).addTag(HailData.tags[index].first))
            }.setPositiveButton(R.string.action_skip) { _, _ ->
                HShortcuts.addPinShortcut(info, pkg, info.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg))
            }.setNegativeButton(android.R.string.cancel, null).show()
    } ?: HShortcuts.addPinShortcut(info, pkg, info.name, HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, pkg))
}

private fun PagerFragment.unfreezeAndRemoveHome(info: AppInfo, pkg: String) {
    val job = setListFrozen(false, listOf(info), false)
    lifecycleScope.launch {
        job.join()
        if (!AppManager.isAppFrozen(pkg)) removeCheckedApp(pkg)
    }
}
