package com.aistra.hail.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class PagerUiState(
    val apps: List<AppInfo> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
)

data class Tag(val label: String, val id: Int)

class PagerViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PagerUiState())
    val uiState: StateFlow<PagerUiState> = _uiState.asStateFlow()

    val apps: StateFlow<List<AppInfo>> = _uiState.map { it.apps }
    val isLoading: StateFlow<Boolean> = _uiState.map { it.isLoading }
    val isRefreshing: StateFlow<Boolean> = _uiState.map { it.isRefreshing }

    private val _tags = MutableStateFlow<List<Tag>>(emptyList())
    val tags: StateFlow<List<Tag>> = _tags.asStateFlow()

    private var query: String = ""
    private var tagId: Int = 0
    private var refreshJob: Job? = null
    private var refreshSeq = 0

    init {
        viewModelScope.launch {
            _tags.value = HailData.tags.map { Tag(it.first, it.second) }
            HailData.tagsFlow.collect {
                try {
                    updateTags()
                } catch (e: Exception) {
                    HLog.e("Failed to update tags: ${e.message}", e)
                }
            }
        }
    }

    fun updateTags() {
        _tags.value = HailData.tags.map { Tag(it.first, it.second) }
    }

    fun setTabType(tabType: String) {
        tagId = HailData.tags.find { it.first == tabType }?.second ?: 0
    }

    fun setQuery(newQuery: String) {
        query = newQuery
        refreshApps()
    }

    fun refresh() {
        val seq = ++refreshSeq
        _uiState.value = _uiState.value.copy(isRefreshing = true)
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            try {
                AppMetaCache.invalidateState(HailData.checkedList.map { it.packageName })
                refreshApps()
            } finally {
                if (seq == refreshSeq) {
                    _uiState.value = _uiState.value.copy(isRefreshing = false)
                }
            }
        }
    }

    fun refreshApps() {
        val filtered = HailData.checkedList.filter { it.isInstalled }.filter {
            if (query.isEmpty()) tagId in it.tagIdList
            else ((HailData.nineKeySearch && NineKeySearch.search(query, it.packageName, it.name))
                    || FuzzySearch.search(it.packageName, query)
                    || FuzzySearch.search(it.name, query)
                    || PinyinSearch.searchPinyinAll(it.name, query))
        }.sortedWith(NameComparator)
        _uiState.value = _uiState.value.copy(apps = filtered, isLoading = false)
    }
}
