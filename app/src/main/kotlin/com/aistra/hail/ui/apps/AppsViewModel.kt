package com.aistra.hail.ui.apps

import android.app.Application
import android.app.usage.UsageStatsManager
import android.content.pm.ApplicationInfo
import androidx.core.content.getSystemService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.FuzzySearch
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.NameComparator
import com.aistra.hail.utils.NineKeySearch
import com.aistra.hail.utils.PinyinSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

class AppsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    val apps = MutableLiveData<List<ApplicationInfo>>()
    val isRefreshing = MutableLiveData(false)
    val query = MutableLiveData("")
    val displayApps = MutableLiveData<List<ApplicationInfo>>()

    init {
        viewModelScope.launch {
            withTimeoutOrNull(CACHE_READY_TIMEOUT_MS) { AppMetaCache.cacheReady.await() }
            updateAppList()
        }
    }

    private var refreshJob: Job? = null
    private var refreshStateJob: Job? = null
    private var lastUpdateTime: Long = 0
    private var appListRefreshJob: Job? = null

    /**
     * Delaying changes to the refreshing state prevents the progress bar from flickering.
     * */
    private fun postRefreshState(
        state: Boolean,
        delayTime: Long = 200L,
    ) {
        if (!state) {
            refreshStateJob?.cancel()
            isRefreshing.postValue(false)
        } else if (refreshStateJob == null || refreshStateJob!!.isCompleted) {
            refreshStateJob =
                viewModelScope.launch {
                    delay(delayTime)
                    isRefreshing.postValue(true)
                }
        }
    }

    fun postQuery(
        text: String,
        delayTime: Long = 300L,
    ) {
        refreshJob?.cancel()
        if (delayTime == 0L) {
            query.postValue(text)
        } else {
            refreshJob =
                viewModelScope.launch {
                    delay(delayTime)
                    query.postValue(text)
                }
        }
    }

    /**
     * This method is only used to refresh all the applications that the user has installed
     * and has no filtering or sorting effect.
     *
     * The loaded list is always published (even when empty) so the UI can reflect the result
     * and show an empty-state / informational toast instead of staying stuck on a blank screen.
     * */
    fun updateAppList(forceRefresh: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && now - lastUpdateTime < 1000) return
        lastUpdateTime = now
        if (forceRefresh) {
            appListRefreshJob?.cancel()
            postRefreshState(true)
        }
        viewModelScope.launch {
            val appList =
                runCatching {
                    withContext(Dispatchers.IO) {
                        AppMetaCache.getInstalledApplicationsCacheFirst(forceRefresh)
                    }
                }.getOrDefault(emptyList())
            // Always publish — even an empty list — so the UI reflects the load result.
            apps.postValue(appList)
            updateDisplayAppList()
            if (forceRefresh) {
                postRefreshState(false)
            } else if (appList.isNotEmpty()) {
                appListRefreshJob =
                    viewModelScope.launch {
                        withContext(Dispatchers.IO) { HPackages.getInstalledApplications() }.let { refreshed ->
                            // Reinstall/update keeps the package set identical but swaps the
                            // ApplicationInfo (new sourceDir/APK modification time); the publish
                            // is gated on that APK identity so the list rebinds and the icon
                            // reloads. A changed identity makes this converge — it cannot loop on
                            // an unchanged install, and a full ApplicationInfo compare would
                            // never settle because label/resource ids differ across PM queries.
                            val currentPackages = apps.value?.map { it.packageName }?.toSet() ?: emptySet()
                            val newPackages = refreshed.map { it.packageName }.toSet()
                            val stalePackageInfo =
                                refreshed.any { fresh ->
                                    apps.value
                                        ?.firstOrNull { it.packageName == fresh.packageName }
                                        ?.let { current ->
                                            AppIconCache.apkSignature(fresh) != AppIconCache.apkSignature(current)
                                        } ?: false
                                }
                            if (currentPackages != newPackages || stalePackageInfo) {
                                apps.postValue(refreshed)
                                updateDisplayAppList()
                            }
                            AppMetaCache.prefetch(refreshed)
                            AppIconCache.prefetch(getApplication(), refreshed)
                        }
                    }
            }
        }
    }

    /**
     * The list that the user actually sees.
     *
     * This method is different from `updateAppList()` in that it filters and rearranges the data
     * from `apps` and places it in `displayApps`.
     * */
    fun updateDisplayAppList() {
        apps.value?.let {
            viewModelScope.launch {
                displayApps.postValue(filterList(it, query.value))
            }
        }
    }

    private val ApplicationInfo.isSystemApp: Boolean
        get() = flags and ApplicationInfo.FLAG_SYSTEM == ApplicationInfo.FLAG_SYSTEM

    private suspend fun filterList(
        appList: List<ApplicationInfo>,
        query: String?,
    ): List<ApplicationInfo> {
        return withContext(Dispatchers.Default) {
            val statusFilter = currentStatusFilter()
            return@withContext appList
                .filter {
                    val metadata = AppMetaCache.get(it.packageName)
                    val isSystemApp = metadata?.isSystemApp ?: it.isSystemApp
                    // Before metadata loads, hide apps that are not really installed (data-only or
                    // broken records whose APK is gone). A user app is hidden unconditionally, a
                    // system app only while the Uninstalled filter is off; installed apps stay.
                    if (metadata == null) {
                        val installed = AppMetaCache.installedFrom(it)
                        if (!installed && (!isSystemApp || !HailData.filterUninstalledApps)) {
                            return@filter false
                        }
                    }
                    val name = AppMetaCache.displayName(it.packageName)
                    (
                        (HailData.filterUserApps && !isSystemApp) ||
                            (HailData.filterSystemApps && isSystemApp)
                    ) &&
                        matchesStatusFilter(metadata?.state, metadata?.enabled, isSystemApp, statusFilter) &&
                        // Search apps
                        (
                            (
                                HailData.nineKeySearch &&
                                    (NineKeySearch.search(query, it.packageName, name))
                            ) ||
                                FuzzySearch.search(it.packageName, query) ||
                                FuzzySearch.search(name, query) ||
                                PinyinSearch.searchPinyinAll(name, query)
                        )
                }.run {
                    val ascending = HailData.sortAscending(HailData.sortBy)
                    val usageStats =
                        if (HailData.sortBy == HailData.SORT_LAST_USED || HailData.sortBy == HailData.SORT_MOST_USED) {
                            withContext(Dispatchers.IO) { loadUsageStats() }
                        } else {
                            emptyMap()
                        }
                    val sizes =
                        if (HailData.sortBy == HailData.SORT_SIZE) {
                            withContext(Dispatchers.IO) {
                                associate { it.packageName to (it.sourceDir?.let { dir -> File(dir).length() } ?: 0L) }
                            }
                        } else {
                            emptyMap()
                        }
                    val comparator: Comparator<ApplicationInfo> =
                        when (HailData.sortBy) {
                            HailData.SORT_INSTALL -> compareBy { AppMetaCache.get(it.packageName)?.firstInstallTime ?: 0L }
                            HailData.SORT_UPDATE -> compareBy { AppMetaCache.get(it.packageName)?.lastUpdateTime ?: 0L }
                            HailData.SORT_SIZE -> compareBy { sizes[it.packageName] ?: 0L }
                            HailData.SORT_LAST_USED -> compareBy { usageStats[it.packageName]?.lastTimeUsed ?: 0L }
                            HailData.SORT_MOST_USED -> compareBy { usageStats[it.packageName]?.totalTimeInForeground ?: 0L }
                            else -> Comparator { a, b -> NameComparator.compare(a, b) }
                        }
                    if (ascending) sortedWith(comparator) else sortedWith(comparator.reversed())
                }
        }
    }

    /**
     * The current status-filter selection, read once per list pass so every row is judged against
     * the same snapshot even if the settings change mid-filter.
     */
    private fun currentStatusFilter() =
        StatusFilter(
            enabled = HailData.filterEnabledApps,
            disabled = HailData.filterDisabledApps,
            suspended = HailData.filterSuspendedApps,
            hidden = HailData.filterHiddenApps,
            stopped = HailData.filterStoppedApps,
            uninstalled = HailData.filterUninstalledApps,
        )

    private fun loadUsageStats(): Map<String, android.app.usage.UsageStats> {
        val usageStatsManager = HailApp.app.getSystemService<UsageStatsManager>()!!
        return usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_BEST, 0, System.currentTimeMillis())
            .associateBy { it.packageName }
    }

    companion object {
        // Bound the wait so a slow first Room read can never block the initial list forever.
        private const val CACHE_READY_TIMEOUT_MS = 5_000L
    }
}
