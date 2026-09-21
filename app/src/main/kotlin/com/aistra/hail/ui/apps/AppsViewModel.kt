package com.aistra.hail.ui.apps

import android.app.Application
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppsViewModel(application: Application) : AndroidViewModel(application) {
    private val _apps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val apps: StateFlow<List<ApplicationInfo>> = _apps.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _displayApps = MutableStateFlow<List<ApplicationInfo>>(emptyList())
    val displayApps: StateFlow<List<ApplicationInfo>> = _displayApps.asStateFlow()

    init {
        viewModelScope.launch {
            AppMetaCache.installedApplicationsReady.first { it }
            updateAppList()
        }
        updateAppList()
    }

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null
    private var lastUpdateTime: Long = 0
    private var appListRefreshJob: Job? = null

    private fun postRefreshState(state: Boolean, delayTime: Long = 200L) {
        if (!state) {
            refreshStateJob?.cancel()
            _isRefreshing.value = false
        } else if (refreshStateJob == null || refreshStateJob!!.isCompleted) {
            refreshStateJob = viewModelScope.launch {
                delay(delayTime)
                _isRefreshing.value = true
            }
        }
    }

    fun postQuery(text: String, delayTime: Long = 300L) {
        refreshJob?.cancel()
        if (delayTime == 0L)
            _query.value = text
        else {
            refreshJob = viewModelScope.launch {
                delay(delayTime)
                _query.value = text
            }
        }
    }

    fun updateAppList(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastUpdateTime < 1000) return
        lastUpdateTime = now
        if (forceRefresh) {
            appListRefreshJob?.cancel()
            postRefreshState(true)
        }
        viewModelScope.launch {
            val appList = withContext(Dispatchers.IO) {
                AppMetaCache.getInstalledApplicationsCacheFirst(forceRefresh)
            }
            if (appList.isNotEmpty()) {
                _apps.value = appList
                updateDisplayAppList()
            }
            if (forceRefresh) {
                postRefreshState(false)
            } else if (appList.isNotEmpty()) {
                appListRefreshJob = viewModelScope.launch {
                    withContext(Dispatchers.IO) { HPackages.getInstalledApplications() }.let { refreshed ->
                        val currentPackages = _apps.value?.map { it.packageName }?.toSet() ?: emptySet()
                        val newPackages = refreshed.map { it.packageName }.toSet()
                        if (currentPackages != newPackages) {
                            _apps.value = refreshed
                            updateDisplayAppList()
                        }
                        AppMetaCache.prefetch(refreshed)
                        AppIconCache.prefetch(getApplication(), refreshed)
                    }
                }
            }
        }
    }

    fun updateDisplayAppList() {
        _apps.value?.let {
            viewModelScope.launch {
                _displayApps.value = filterList(it, _query.value)
            }
        }
    }

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?
    ): List<ApplicationInfo> {
        return withContext(Dispatchers.Default) {
            return@withContext appList.filter {
                val metadata = AppMetaCache.get(it.packageName)
                val isSystemApp = metadata?.isSystemApp ?: it.isSystemApp
                val name = metadata?.name ?: it.packageName
                val frozen = metadata?.state == AppInfo.State.FROZEN
                (HailData.filterAllApps
                        || (HailData.filterUserApps && !isSystemApp)
                        || (HailData.filterSystemApps && isSystemApp))

                        && ((HailData.filterFrozenApps && frozen)
                        || (HailData.filterUnfrozenApps && !frozen))
                        && ((HailData.nineKeySearch
                        && (NineKeySearch.search(query, it.packageName, name)))
                        || FuzzySearch.search(it.packageName, query)
                        || FuzzySearch.search(name, query)
                        || PinyinSearch.searchPinyinAll(name, query))
            }.run {
                when (HailData.sortBy) {
                    HailData.SORT_INSTALL -> sortedBy {
                        AppMetaCache.get(it.packageName)?.firstInstallTime ?: 0
                    }

                    HailData.SORT_UPDATE -> sortedByDescending {
                        AppMetaCache.get(it.packageName)?.lastUpdateTime ?: 0
                    }

                    else -> sortedWith(NameComparator)
                }
            }
        }
    }
}