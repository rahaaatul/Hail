package com.aistra.hail.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppActions
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PagerViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _multiselect = MutableStateFlow(false)
    val multiselect: StateFlow<Boolean> = _multiselect.asStateFlow()

    private val _selectedList = MutableStateFlow<List<AppInfo>>(emptyList())
    val selectedList: StateFlow<List<AppInfo>> = _selectedList.asStateFlow()

    var selectedTagId: Int = 0
        private set

    init {
        viewModelScope.launch {
            AppMetaCache.revision.collect { loadApps(selectedTagId) }
        }
    }

    fun loadApps(tagId: Int = selectedTagId) {
        val queryText = _query.value
        val filtered = HailData.checkedList.filter { it.isInstalled }
        val result = if (queryText.isEmpty()) {
            filtered.filter { it.tagIdList.contains(tagId) }
        } else {
            filtered.filter { app ->
                ((HailData.nineKeySearch && NineKeySearch.search(
                    queryText, app.packageName, app.name.toString()
                )) || FuzzySearch.search(app.packageName, queryText) || FuzzySearch.search(
                    app.name.toString(), queryText
                ) || PinyinSearch.searchPinyinAll(app.name.toString(), queryText))
            }
        }
        _apps.value = result.sortedWith(NameComparator)
    }

    fun setQuery(newQuery: String) {
        _query.value = newQuery
        loadApps(selectedTagId)
    }

    fun setMultiselect(enabled: Boolean) {
        _multiselect.value = enabled
        if (!enabled) {
            _selectedList.value = emptyList()
        }
    }

    fun toggleSelection(info: AppInfo) {
        val current = _selectedList.value
        _selectedList.value = if (info in current) current - info else current + info
    }

    fun selectAll() {
        _selectedList.value = _apps.value
    }

    fun deselect() {
        _selectedList.value = emptyList()
        _multiselect.value = false
    }

    fun setTagId(tagId: Int) {
        selectedTagId = tagId
        loadApps(tagId)
    }

    fun setListFrozen(
        frozen: Boolean,
        list: List<AppInfo> = _apps.value,
        onResult: (Boolean) -> Unit = {}
    ) {
        if (HailData.workingMode == HailData.MODE_DEFAULT) {
            onResult(false)
            return
        }
        val filtered = list.filter { AppManager.isAppFrozen(it.packageName) != frozen }
        viewModelScope.launch {
            AppActions.freezePackages(frozen, filtered.map { it.packageName }).onSuccess {
                AppMetaCache.invalidateState(filtered.map { it.packageName })
                loadApps(selectedTagId)
                onResult(true)
            }.onFailure {
                onResult(false)
            }
        }
    }

    fun launchApp(packageName: String, onLaunch: (android.content.Intent?) -> Unit = {}) {
        viewModelScope.launch {
            if (AppManager.isAppFrozen(packageName)) {
                AppActions.ensureUnfrozen(packageName).onSuccess {
                    launchApp(packageName, onLaunch)
                }.onFailure {
                    onLaunch(null)
                }
                return@launch
            }
            AppActions.getLaunchIntent(packageName).onSuccess { intent ->
                HShortcuts.addDynamicShortcut(packageName)
                onLaunch(intent)
            }.onFailure {
                onLaunch(null)
            }
        }
    }

    fun removeCheckedApp(packageName: String, saveApps: Boolean = true) {
        HailData.removeCheckedApp(packageName, saveApps)
        if (saveApps) loadApps(selectedTagId)
    }
}
