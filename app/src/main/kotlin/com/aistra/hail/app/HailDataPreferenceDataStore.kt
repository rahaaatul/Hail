package com.aistra.hail.app

import androidx.preference.PreferenceDataStore
import com.aistra.hail.HailApp.Companion.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * [PreferenceDataStore] backed by HailData, which in turn is backed by Proto DataStore.
 *
 * Reads are synchronous (from HailData's cached StateFlow). Writes are async (launched on a
 * coroutine scope) to avoid blocking the UI thread while DataStore persists to disk.
 */
class HailDataPreferenceDataStore : PreferenceDataStore() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Boolean ──

    // Boolean settings resolved lazily so reads always reflect the current
    // DataStore-cached value rather than a snapshot captured at construction.
    private val booleanGetters: Map<String, () -> Boolean> =
        mapOf(
            HailData.FILTER_USER_APPS to { HailData.filterUserApps },
            HailData.FILTER_SYSTEM_APPS to { HailData.filterSystemApps },
            HailData.FILTER_ALL_APPS to { HailData.filterAllApps },
            HailData.FILTER_FROZEN_APPS to { HailData.filterFrozenApps },
            HailData.FILTER_UNFROZEN_APPS to { HailData.filterUnfrozenApps },
            HailData.BIOMETRIC_LOGIN to { HailData.biometricLogin },
            HailData.GRAYSCALE_ICON to { HailData.grayscaleIcon },
            HailData.COMPACT_ICON to { HailData.compactIcon },
            HailData.SYNTHESIZE_ADAPTIVE_ICONS to { HailData.synthesizeAdaptiveIcons },
            HailData.FUZZY_SEARCH to { HailData.fuzzySearch },
            HailData.NINE_KEY_SEARCH to { HailData.nineKeySearch },
            HailData.AUTO_FREEZE_AFTER_LOCK to { HailData.autoFreezeAfterLock },
            HailData.SKIP_WHILE_CHARGING to { HailData.skipWhileCharging },
            HailData.SKIP_FOREGROUND_APP to { HailData.skipForegroundApp },
            HailData.SKIP_NOTIFYING_APP to { HailData.skipNotifyingApp },
        )

    // Boolean setters resolved lazily so writes always target the current
    // DataStore-cached state rather than a snapshot captured at construction.
    private val booleanSetters: Map<String, suspend (Boolean) -> Unit> =
        mapOf(
            HailData.FILTER_USER_APPS to { value -> SettingsDataStore.updateSettings { setFilterUserApps(value) } },
            HailData.FILTER_SYSTEM_APPS to { value -> SettingsDataStore.updateSettings { setFilterSystemApps(value) } },
            HailData.FILTER_ALL_APPS to { value -> SettingsDataStore.updateSettings { setFilterAllApps(value) } },
            HailData.FILTER_FROZEN_APPS to { value -> SettingsDataStore.updateSettings { setFilterFrozenApps(value) } },
            HailData.FILTER_UNFROZEN_APPS to { value -> SettingsDataStore.updateSettings { setFilterUnfrozenApps(value) } },
            HailData.BIOMETRIC_LOGIN to { value -> SettingsDataStore.updateSettings { setBiometricLogin(value) } },
            HailData.GRAYSCALE_ICON to { value -> SettingsDataStore.updateSettings { setGrayscaleIcon(value) } },
            HailData.COMPACT_ICON to { value -> SettingsDataStore.updateSettings { setCompactIcon(value) } },
            HailData.SYNTHESIZE_ADAPTIVE_ICONS to { value -> SettingsDataStore.updateSettings { setSynthesizeAdaptiveIcons(value) } },
            HailData.FUZZY_SEARCH to { value -> SettingsDataStore.updateSettings { setFuzzySearch(value) } },
            HailData.NINE_KEY_SEARCH to { value -> SettingsDataStore.updateSettings { setNineKeySearch(value) } },
            HailData.AUTO_FREEZE_AFTER_LOCK to { value -> SettingsDataStore.updateSettings { setAutoFreezeAfterLock(value) } },
            HailData.SKIP_WHILE_CHARGING to { value -> SettingsDataStore.updateSettings { setSkipWhileCharging(value) } },
            HailData.SKIP_FOREGROUND_APP to { value -> SettingsDataStore.updateSettings { setSkipForegroundApp(value) } },
            HailData.SKIP_NOTIFYING_APP to { value -> SettingsDataStore.updateSettings { setSkipNotifyingApp(value) } },
        )

    override fun getBoolean(
        key: String,
        defaultValue: Boolean,
    ): Boolean = booleanGetters[key]?.invoke() ?: defaultValue

    override fun putBoolean(
        key: String,
        value: Boolean,
    ) {
        scope.launch {
            booleanSetters[key]?.invoke(value)
        }
    }

    // ── String ──

    override fun getString(
        key: String,
        defaultValue: String?,
    ): String? =
        when (key) {
            HailData.SORT_BY -> HailData.sortBy
            HailData.WORKING_MODE -> HailData.workingMode
            HailData.APP_THEME -> HailData.appTheme
            HailData.ICON_PACK -> HailData.iconPack
            HailData.TILE_ACTION -> HailData.tileAction
            HailData.DYNAMIC_SHORTCUT_ACTION -> HailData.dynamicShortcutAction
            else -> defaultValue
        }

    override fun putString(
        key: String,
        value: String?,
    ) {
        value?.let {
            scope.launch {
                when (key) {
                    HailData.SORT_BY -> {
                        SettingsDataStore.updateSettings { setSortBy(it) }
                    }

                    HailData.WORKING_MODE -> {
                        SettingsDataStore.updateSettings { setWorkingMode(it) }
                    }

                    HailData.APP_THEME -> {
                        SettingsDataStore.updateSettings { setAppTheme(it) }
                    }

                    HailData.ICON_PACK -> {
                        SettingsDataStore.updateSettings { setIconPack(it) }
                    }

                    HailData.TILE_ACTION -> {
                        SettingsDataStore.updateSettings { setTileAction(it) }
                    }

                    HailData.DYNAMIC_SHORTCUT_ACTION -> {
                        SettingsDataStore.updateSettings { setDynamicShortcutAction(it) }
                    }
                }
            }
        }
    }

    // ── Float ──

    override fun getFloat(
        key: String,
        defaultValue: Float,
    ): Float =
        when (key) {
            HailData.HOME_FONT_SIZE -> HailData.homeFontSize
            HailData.AUTO_FREEZE_DELAY -> HailData.autoFreezeDelay
            else -> defaultValue
        }

    override fun putFloat(
        key: String,
        value: Float,
    ) {
        scope.launch {
            SettingsDataStore.updateSettings {
                when (key) {
                    HailData.HOME_FONT_SIZE -> setHomeFontSize(value)
                    HailData.AUTO_FREEZE_DELAY -> setAutoFreezeDelay(value)
                }
            }
        }
    }

    // ── Int ──

    override fun getInt(
        key: String,
        defaultValue: Int,
    ): Int = defaultValue

    override fun putInt(
        key: String,
        value: Int,
    ) {
        // No Int settings currently. No-op.
    }

    // ── Long ──

    override fun getLong(
        key: String,
        defaultValue: Long,
    ): Long = defaultValue

    override fun putLong(
        key: String,
        value: Long,
    ) {
        // No Long settings currently. No-op.
    }

    // ── StringSet ──

    override fun getStringSet(
        key: String,
        defaultValue: Set<String>?,
    ): Set<String>? = defaultValue

    override fun putStringSet(
        key: String,
        value: Set<String>?,
    ) {
        // No StringSet settings currently. No-op.
    }
}
