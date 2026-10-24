package com.aistra.hail.ui.home

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


internal fun filterActionEntries(
    entries: Array<String>, frozen: Boolean, info: AppInfo,
): List<String> {
    val freezeStr = getString(R.string.action_freeze)
    val unfreezeStr = getString(R.string.action_unfreeze)
    val pinStr = getString(R.string.action_pin)
    val unpinStr = getString(R.string.action_unpin)
    val whitelistStr = getString(R.string.action_whitelist)
    val removeWhitelistStr = getString(R.string.action_remove_whitelist)
    val unfreezeRemoveHomeStr = getString(R.string.action_unfreeze_remove_home)
    return entries.filter {
        (it != freezeStr || !frozen) && (it != unfreezeStr || frozen) && (it != pinStr || !info.pinned) &&
            (it != unpinStr || info.pinned) && (it != whitelistStr || !info.whitelisted) &&
            (it != removeWhitelistStr || info.whitelisted) && (it != unfreezeRemoveHomeStr || frozen)
    }
}


internal fun importFromClipboard() = runCatching {
    val str = HUI.pasteText() ?: throw IllegalArgumentException("No text on clipboard")
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


internal fun PagerFragment.updateBarTitle() {
if (!isAdded) return
activity.supportActionBar?.title =
    if (multiselect) {
        getString(R.string.msg_selected, selectedList.size.toString())
    } else {
        getString(R.string.app_name)
    }
}


internal fun PagerFragment.deselect(update: Boolean = true) {
selectedList.clear()
if (!update) return
updateCurrentList()
updateBarTitle()
updateMultiselectButton()
}


internal fun PagerFragment.onMultiselectClick() {
multiselect = !multiselect
if (multiselect) {
    updateBarTitle()
    HUI.showToast(R.string.tap_to_select)
} else {
    deselect()
}
updateMultiselectButton()
}


internal fun PagerFragment.launchApp(packageName: String) {
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


internal fun PagerFragment.setListFrozen(
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


internal fun updateCheckedAppsWithOldTagId(oldTagId: Int, tagId: Int) {
val checkedSnapshot = HailData.checkedList.toList()
val toUpdate = checkedSnapshot.filter { oldTagId in it.tagIdList }
toUpdate.forEach { it.tagIdList.replaceAll { if (it == oldTagId) tagId else it } }
}

@Composable

internal fun TriStateTagList(initialStates: Array<ToggleableState>, states: MutableList<ToggleableState>) = Column(
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


internal fun PagerFragment.setupTagDialogInput(binding: DialogInputBinding, list: List<AppInfo>?) {
binding.inputLayout.setHint(R.string.tag)
list ?: binding.editText.setText(tag?.first ?: "")
}


internal fun PagerFragment.exportToClipboard(list: List<AppInfo>) {
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


