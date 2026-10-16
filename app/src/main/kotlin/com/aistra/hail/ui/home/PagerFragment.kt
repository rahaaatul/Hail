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
import com.google.android.material.color.MaterialColors
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray

private const val TAG_LIST_TOP_SPACING_DP = 16
private const val TAG_ROW_HORIZONTAL_PADDING_DP = 24
private const val TAG_ROW_VERTICAL_PADDING_DP = 12
private const val TAG_CHECKBOX_TEXT_SPACING_DP = 24

@Suppress("LargeClass", "TooManyFunctions")
class PagerFragment : MainFragment(), PagerAdapter.OnItemClickListener, PagerAdapter.OnItemLongClickListener,
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

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (multiselect) {
                    deselect()
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
        _binding = FragmentPagerBinding.inflate(inflater, container, false)
        pagerAdapter = PagerAdapter(selectedList).apply {
            onItemClickListener = this@PagerFragment
            onItemLongClickListener = this@PagerFragment
        }
        binding.recyclerView.run {
            layoutManager = GridLayoutManager(
                activity, resources.getInteger(
                    if (HailData.compactIcon) R.integer.home_span_compact else R.integer.home_span
                )
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
            if (query.isEmpty()) tag?.second?.let { tagId -> tagId in it.tagIdList } ?: false
            else ((HailData.nineKeySearch && NineKeySearch.search(
                query, it.packageName, it.name
            )) || FuzzySearch.search(it.packageName, query) || FuzzySearch.search(
                it.name, query
            ) || PinyinSearch.searchPinyinAll(it.name, query))
        }.sortedWith(NameComparator).let {
            binding.empty.isVisible = it.isEmpty()
            pagerAdapter.submitList(it)
            app.setAutoFreezeService()
        }
    }

    private fun updateBarTitle() {
        if (!isAdded) return
        activity.supportActionBar?.title =
            if (multiselect) getString(R.string.msg_selected, selectedList.size.toString())
            else getString(R.string.app_name)
    }

    override fun onItemClick(info: AppInfo) {
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

    override fun onItemLongClick(info: AppInfo): Boolean {
        return when {
            info.applicationInfo == null && (!multiselect || info !in selectedList) -> {
                exportToClipboard(listOf(info))
                true
            }

            info in selectedList -> {
                onMultiSelect()
                true
            }

            else -> {
                showItemActionDialog(info)
                true
            }
        }
    }

    private fun showItemActionDialog(info: AppInfo) {
        val pkg = info.packageName
        val frozen = AppManager.isAppFrozen(pkg)
        val actions = createItemActionMenu(info, frozen)
        MaterialAlertDialogBuilder(activity).setTitle(info.name).setItems(
            actions.map { it.first }.toTypedArray()
        ) { _, index ->
            handleItemAction(info, frozen, actions[index].second)
        }.setNeutralButton(R.string.action_details) { _, _ ->
            HUI.startActivity(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS, HPackages.packageUri(pkg)
            )
        }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun createItemActionMenu(info: AppInfo, frozen: Boolean): List<Pair<String, Int>> {
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
        ).associateBy(::getString)
        val hiddenActions = buildSet {
            add(if (frozen) R.string.action_freeze else R.string.action_unfreeze)
            add(if (info.pinned) R.string.action_pin else R.string.action_unpin)
            add(if (info.whitelisted) R.string.action_whitelist else R.string.action_remove_whitelist)
            if (!frozen) add(R.string.action_unfreeze_remove_home)
        }
        return resources.getStringArray(R.array.home_action_entries).map { entry ->
            entry to (actionIds[entry] ?: error("Unhandled action entry: $entry"))
        }.filterNot { (_, actionId) -> actionId in hiddenActions }
    }

    private fun handleItemAction(info: AppInfo, frozen: Boolean, actionId: Int) {
        when (actionId) {
            R.string.action_launch -> launchApp(info.packageName)
            R.string.action_freeze, R.string.action_unfreeze -> setListFrozen(!frozen, listOf(info))
            R.string.action_deferred_task -> showDeferredTaskDialog(info, frozen)
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
            R.string.action_add_pin_shortcut -> addPinShortcut(info)
            R.string.action_export_clipboard -> exportToClipboard(listOf(info))
            R.string.action_remove_home -> removeCheckedApp(info.packageName)
            R.string.action_unfreeze_remove_home -> unfreezeAndRemoveHome(info)
            else -> error("Unhandled action id: $actionId")
        }
    }

    private fun showDeferredTaskDialog(info: AppInfo, frozen: Boolean) {
        val values = resources.getIntArray(R.array.deferred_task_values)
        val entries = values.map {
            resources.getQuantityString(R.plurals.deferred_task_entry, it, it)
        }.toTypedArray()
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_deferred_task)
            .setItems(entries) { _, index ->
                val minutes = values[index]
                HWork.setDeferredFrozen(info.packageName, !frozen, minutes.toLong())
                val deferredAction = getString(
                    if (!frozen) R.string.action_freeze else R.string.action_unfreeze
                )
                Snackbar.make(
                    activity.fab,
                    resources.getQuantityString(
                        R.plurals.msg_deferred_task, minutes, minutes, deferredAction, info.name
                    ),
                    Snackbar.LENGTH_INDEFINITE
                ).setAction(R.string.action_undo) { HWork.cancelWork(info.packageName) }.show()
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun addPinShortcut(info: AppInfo) {
        val packageName = info.packageName
        val launchIntent = HailApi.getIntentForPackage(HailApi.ACTION_LAUNCH, packageName)
        val tabLayout = tabs
        if (tabLayout == null || tabLayout.tabCount <= 1) {
            HShortcuts.addPinShortcut(info, packageName, info.name, launchIntent)
            return
        }
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
            .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                HShortcuts.addPinShortcut(
                    info, packageName, info.name, launchIntent.addTag(HailData.tags[index].first)
                )
            }.setPositiveButton(R.string.action_skip) { _, _ ->
                HShortcuts.addPinShortcut(info, packageName, info.name, launchIntent)
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun unfreezeAndRemoveHome(info: AppInfo) {
        val job = setListFrozen(false, listOf(info), false)
        lifecycleScope.launch {
            job.join()
            if (!AppManager.isAppFrozen(info.packageName)) removeCheckedApp(info.packageName)
        }
    }

    private fun tagDialog(info: AppInfo) {
        val checkedItems = BooleanArray(HailData.tags.size) { index ->
            HailData.tags[index].second in info.tagIdList
        }
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setMultiChoiceItems(
            HailData.tags.map { it.first }.toTypedArray(), checkedItems
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

    private fun deselect(update: Boolean = true) {
        selectedList.clear()
        if (!update) return
        updateCurrentList()
        updateBarTitle()
        updateMultiselectButton()
    }

    private fun onMultiselectClick() {
        multiselect = !multiselect
        if (multiselect) {
            updateBarTitle()
            HUI.showToast(R.string.tap_to_select)
        } else {
            deselect()
        }
        updateMultiselectButton()
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
            it.setImageResource(if (multiselect) R.drawable.ic_outline_check
            else R.drawable.ic_outline_select_all)
            val colorAttr = if (multiselect) androidx.appcompat.R.attr.colorPrimary
            else com.google.android.material.R.attr.colorOnSurface
            val color = MaterialColors.getColor(activity.findViewById(R.id.toolbar), colorAttr)
            it.setColorFilter(color)
            it.contentDescription = if (multiselect)
                getString(R.string.msg_selected, selectedList.size.toString())
            else getString(R.string.tap_to_select)
        }
    }

    private fun onMultiSelect() {
        if (!isAdded) return
        val actions = listOf(
            R.string.action_freeze,
            R.string.action_unfreeze,
            R.string.action_tag_set,
            R.string.action_export_clipboard,
            R.string.action_remove_home,
            R.string.action_unfreeze_remove_home
        )
        MaterialAlertDialogBuilder(activity).setTitle(
            getString(
                R.string.msg_selected, selectedList.size.toString()
            )
        ).setItems(actions.map(::getString).toTypedArray()) { _, which ->
            when (actions[which]) {
                R.string.action_freeze -> {
                    setListFrozen(true, selectedList, false)
                    deselect()
                }

                R.string.action_unfreeze -> {
                    setListFrozen(false, selectedList, false)
                    deselect()
                }

                R.string.action_tag_set -> triStateTagDialog()

                R.string.action_export_clipboard -> {
                    exportToClipboard(selectedList)
                    deselect()
                }

                R.string.action_remove_home -> {
                    selectedList.forEach { removeCheckedApp(it.packageName, false) }
                    HailData.saveApps()
                    deselect()
                }

                R.string.action_unfreeze_remove_home -> {
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
        val states = mutableStateListOf<ToggleableState>().apply {
            initialStates.forEach { add(it) }
        }
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_tag_set).setView(ComposeView(activity).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { AppTheme { TriStateTagList(initialStates, states) } }
        }).setPositiveButton(android.R.string.ok) { _, _ ->
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

    @Suppress("FunctionNaming")
    @Composable
    private fun TriStateTagList(
        initialStates: Array<ToggleableState>,
        states: MutableList<ToggleableState>
    ) = Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
        Spacer(modifier = Modifier.height(TAG_LIST_TOP_SPACING_DP.dp))
        HailData.tags.forEachIndexed { index, tag ->
            Row(modifier = Modifier.fillMaxWidth().clickable {
                states[index] = if (initialStates[index] == ToggleableState.Indeterminate) when (states[index]) {
                    ToggleableState.On -> ToggleableState.Off
                    ToggleableState.Off -> ToggleableState.Indeterminate
                    ToggleableState.Indeterminate -> ToggleableState.On
                }
                else if (states[index] == ToggleableState.On) ToggleableState.Off
                else ToggleableState.On
            }.padding(
                horizontal = TAG_ROW_HORIZONTAL_PADDING_DP.dp,
                vertical = TAG_ROW_VERTICAL_PADDING_DP.dp
            ), verticalAlignment = Alignment.CenterVertically) {
                TriStateCheckbox(
                    state = states[index],
                    onClick = null,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(TAG_CHECKBOX_TEXT_SPACING_DP.dp))
                Text(
                    text = tag.first,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }

    private fun launchApp(packageName: String) {
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

    private fun setListFrozen(
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
                pagerAdapter.refreshVisualState()
                HUI.showToast(
                    if (frozen) R.string.msg_freeze else R.string.msg_unfreeze, filtered.size.toString()
                )
            }.onFailure {
                HUI.showToast(R.string.permission_denied)
            }
        }
    }

    private fun showWorkingModeGuidance(): Boolean {
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
            if (list == selectedList) triStateTagDialog() else tagDialog(list.first())
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

    private fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) updateCurrentList()
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
                } else inited = true
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean = true
        })

        val multiselectItem = menu.findItem(R.id.action_multiselect)
        val actionView = multiselectItem.actionView
        val button = actionView?.findViewById<ImageView>(R.id.multiselect_button)

        button?.setOnClickListener { onMultiselectClick() }
        button?.setOnLongClickListener { onMultiselectLongClick(); true }

        updateMultiselectButton()
    }

    override fun onDestroyView() {
        _menu = null
        pagerAdapter.onDestroy()
        super.onDestroyView()
        _binding = null
    }
}
