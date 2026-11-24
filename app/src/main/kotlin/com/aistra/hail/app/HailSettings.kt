package com.aistra.hail.app

import com.aistra.hail.backup.SettingsBackup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Settings facade backed by Proto DataStore.
 *
 * Reads are served from an in-memory snapshot. Every setter updates that snapshot synchronously —
 * so the UI reflects the change immediately — and persists the same change to [SettingsDataStore]
 * in the background. The snapshot is kept in sync with DataStore by a collector that is started
 * only after [seedSettings], so a pre-migration emission can never overwrite the seeded value.
 *
 * Settings keys stay on [HailData] (`HailData.WORKING_MODE`, `HailData.FILTER_USER_APPS`, ...) so
 * the persisted preference names and the public constants do not change.
 */
object HailSettings {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _settingsData = MutableStateFlow(SettingsData())

    /** Hot snapshot of the settings, updated synchronously by setters and by the DataStore collector. */
    val settingsData: StateFlow<SettingsData> = _settingsData.asStateFlow()

    @Volatile
    private var collectorJob: Job? = null

    // ── Synchronous getters (read from the snapshot) ──

    val sortBy get() = settingsData.value.sortBy
    val sortDirections: Map<String, Boolean> get() = settingsData.value.sortDirections

    fun sortAscending(sort: String): Boolean = sortDirections[sort] ?: (sort == HailData.SORT_NAME)

    val filterUserApps get() = settingsData.value.filterUserApps
    val filterSystemApps get() = settingsData.value.filterSystemApps
    val filterEnabledApps get() = settingsData.value.filterEnabledApps
    val filterDisabledApps get() = settingsData.value.filterDisabledApps
    val filterSuspendedApps get() = settingsData.value.filterSuspendedApps
    val filterHiddenApps get() = settingsData.value.filterHiddenApps
    val filterStoppedApps get() = settingsData.value.filterStoppedApps
    val filterUninstalledApps get() = settingsData.value.filterUninstalledApps

    // ── Settings with optimistic setters ──
    // Every setter updates the in-memory snapshot synchronously so the UI reflects the change
    // immediately, then persists it to DataStore in the background.

    var workingMode: String
        get() = settingsData.value.workingMode
        set(value) {
            updateOptimistically({ copy(workingMode = value) }, { setWorkingMode(value) })
        }

    var biometricLogin: Boolean
        get() = settingsData.value.biometricLogin
        set(value) {
            updateOptimistically({ copy(biometricLogin = value) }, { setBiometricLogin(value) })
        }

    var appTheme: String
        get() = settingsData.value.appTheme
        set(value) {
            updateOptimistically({ copy(appTheme = value) }, { setAppTheme(value) })
        }

    var iconPack: String
        get() = settingsData.value.iconPack
        set(value) {
            updateOptimistically({ copy(iconPack = value) }, { setIconPack(value) })
        }

    var grayscaleIcon: Boolean
        get() = settingsData.value.grayscaleIcon
        set(value) {
            updateOptimistically({ copy(grayscaleIcon = value) }, { setGrayscaleIcon(value) })
        }

    var compactIcon: Boolean
        get() = settingsData.value.compactIcon
        set(value) {
            updateOptimistically({ copy(compactIcon = value) }, { setCompactIcon(value) })
        }

    var synthesizeAdaptiveIcons: Boolean
        get() = settingsData.value.synthesizeAdaptiveIcons
        set(value) {
            updateOptimistically({ copy(synthesizeAdaptiveIcons = value) }, { setSynthesizeAdaptiveIcons(value) })
        }

    var homeFontSize: Float
        get() = settingsData.value.homeFontSize
        set(value) {
            updateOptimistically({ copy(homeFontSize = value) }, { setHomeFontSize(value) })
        }

    var fuzzySearch: Boolean
        get() = settingsData.value.fuzzySearch
        set(value) {
            updateOptimistically({ copy(fuzzySearch = value) }, { setFuzzySearch(value) })
        }

    var nineKeySearch: Boolean
        get() = settingsData.value.nineKeySearch
        set(value) {
            updateOptimistically({ copy(nineKeySearch = value) }, { setNineKeySearch(value) })
        }

    var tileAction: String
        get() = settingsData.value.tileAction
        set(value) {
            updateOptimistically({ copy(tileAction = value) }, { setTileAction(value) })
        }

    var autoFreezeAfterLock: Boolean
        get() = settingsData.value.autoFreezeAfterLock
        set(value) {
            updateOptimistically({ copy(autoFreezeAfterLock = value) }, { setAutoFreezeAfterLock(value) })
        }

    var autoFreezeDelay: Float
        get() = settingsData.value.autoFreezeDelay
        set(value) {
            updateOptimistically({ copy(autoFreezeDelay = value) }, { setAutoFreezeDelay(value) })
        }

    var skipWhileCharging: Boolean
        get() = settingsData.value.skipWhileCharging
        set(value) {
            updateOptimistically({ copy(skipWhileCharging = value) }, { setSkipWhileCharging(value) })
        }

    var skipForegroundApp: Boolean
        get() = settingsData.value.skipForegroundApp
        set(value) {
            updateOptimistically({ copy(skipForegroundApp = value) }, { setSkipForegroundApp(value) })
        }

    var skipNotifyingApp: Boolean
        get() = settingsData.value.skipNotifyingApp
        set(value) {
            updateOptimistically({ copy(skipNotifyingApp = value) }, { setSkipNotifyingApp(value) })
        }

    var dynamicShortcutAction: String
        get() = settingsData.value.dynamicShortcutAction
        set(value) {
            updateOptimistically({ copy(dynamicShortcutAction = value) }, { setDynamicShortcutAction(value) })
        }

    // ── Sorting and filtering ──

    fun changeSort(
        sort: String,
        ascending: Boolean,
    ) {
        updateOptimistically(
            update = { copy(sortBy = sort, sortDirections = sortDirections + (sort to ascending)) },
            persist = {
                setSortBy(sort)
                putSortDirections(sort, ascending)
            },
        )
    }

    fun changeAppsFilter(
        filter: String,
        enabled: Boolean,
    ) {
        updateOptimistically(
            update = { applyFilterToSettings(filter, enabled) },
            persist = { persistFilter(filter, enabled) },
        )
    }

    /**
     * Applies [update] to the synchronous snapshot and persists the same change to
     * [SettingsDataStore] in the background. Every optimistic setter goes through here.
     */
    private fun updateOptimistically(
        update: SettingsData.() -> SettingsData,
        persist: suspend SettingsBackup.Builder.() -> Unit,
    ) {
        _settingsData.value = _settingsData.value.update()
        scope.launch {
            SettingsDataStore.updateSettings { persist() }
        }
    }

    /**
     * Seeds the synchronous snapshot from DataStore. Called once at startup, after the
     * SharedPreferences migration and before [startSync].
     */
    internal fun seedSettings(settings: SettingsData) {
        _settingsData.value = settings
    }

    /**
     * Starts mirroring DataStore updates into the snapshot. Idempotent; must be called after
     * [seedSettings] so the first emission is already the post-migration value.
     */
    internal fun startSync() {
        if (collectorJob != null) return
        collectorJob =
            scope.launch {
                SettingsDataStore.settingsFlow.collect { _settingsData.value = it }
            }
    }

    /**
     * Cancels the DataStore collector and restores the default snapshot; tests only.
     *
     * The cancellation is joined so no in-flight emission can land after the snapshot is reset,
     * which keeps optimistic-setter assertions deterministic.
     */
    internal fun resetSettingsForTest() {
        runBlocking { collectorJob?.cancelAndJoin() }
        collectorJob = null
        _settingsData.value = SettingsData()
    }
}

private fun SettingsData.applyFilterToSettings(
    filter: String,
    enabled: Boolean,
): SettingsData =
    when (filter) {
        HailData.FILTER_USER_APPS -> copy(filterUserApps = enabled)
        HailData.FILTER_SYSTEM_APPS -> copy(filterSystemApps = enabled)
        HailData.FILTER_ENABLED_APPS -> copy(filterEnabledApps = enabled)
        HailData.FILTER_DISABLED_APPS -> copy(filterDisabledApps = enabled)
        HailData.FILTER_SUSPENDED_APPS -> copy(filterSuspendedApps = enabled)
        HailData.FILTER_HIDDEN_APPS -> copy(filterHiddenApps = enabled)
        HailData.FILTER_STOPPED_APPS -> copy(filterStoppedApps = enabled)
        HailData.FILTER_UNINSTALLED_APPS -> copy(filterUninstalledApps = enabled)
        else -> this
    }

private fun SettingsBackup.Builder.persistFilter(
    filter: String,
    enabled: Boolean,
) {
    when (filter) {
        HailData.FILTER_USER_APPS -> {
            setFilterUserApps(enabled)
        }

        HailData.FILTER_SYSTEM_APPS -> {
            setFilterSystemApps(enabled)
        }

        HailData.FILTER_ENABLED_APPS -> {
            setFilterEnabledApps(enabled)
        }

        HailData.FILTER_DISABLED_APPS -> {
            setFilterDisabledApps(enabled)
        }

        HailData.FILTER_SUSPENDED_APPS -> {
            setFilterSuspendedApps(enabled)
        }

        HailData.FILTER_HIDDEN_APPS -> {
            setFilterHiddenApps(enabled)
        }

        HailData.FILTER_STOPPED_APPS -> {
            setFilterStoppedApps(enabled)
        }

        HailData.FILTER_UNINSTALLED_APPS -> {
            setFilterUninstalledApps(enabled)
        }

        else -> { /* unknown filter key — ignore */ }
    }
}
