package com.aistra.hail.app

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.SettingsBackup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * All user settings, mirrored from HailData constants.
 * This is the in-memory representation backed by Proto DataStore.
 */
data class SettingsData(
    val sortBy: String = HailData.SORT_NAME,
    val filterUserApps: Boolean = true,
    val filterSystemApps: Boolean = false,
    val filterAllApps: Boolean = true,
    val filterFrozenApps: Boolean = true,
    val filterUnfrozenApps: Boolean = true,
    val workingMode: String = HailData.MODE_DEFAULT,
    val biometricLogin: Boolean = false,
    val appTheme: String = HailData.FOLLOW_SYSTEM,
    val iconPack: String = HailData.ACTION_NONE,
    val grayscaleIcon: Boolean = true,
    val compactIcon: Boolean = false,
    val synthesizeAdaptiveIcons: Boolean = false,
    val homeFontSize: Float = 14f,
    val fuzzySearch: Boolean = false,
    val nineKeySearch: Boolean = false,
    val tileAction: String = HailData.AUTO_FREEZE_AFTER_LOCK,
    val autoFreezeAfterLock: Boolean = false,
    val autoFreezeDelay: Float = 0f,
    val skipWhileCharging: Boolean = false,
    val skipForegroundApp: Boolean = false,
    val skipNotifyingApp: Boolean = false,
    val dynamicShortcutAction: String = HailData.ACTION_NONE,
)

/**
 * Returns [value] when the proto field is present ([hasValue]); otherwise [default].
 *
 * Proto3 optional fields use [SettingsBackup.hasXxx] to distinguish "explicitly backed up"
 * from "default", so each field must be checked before being copied into [SettingsData].
 */
private fun <T> valueOrDefault(
    hasValue: Boolean,
    value: T,
    default: T,
): T = if (hasValue) value else default

/**
 * Converts [SettingsBackup] (protobuf wire format) to [SettingsData].
 */
fun SettingsBackup.toSettingsData(): SettingsData =
    SettingsData(
        sortBy = valueOrDefault(hasSortBy(), sortBy, HailData.SORT_NAME),
        filterUserApps = valueOrDefault(hasFilterUserApps(), filterUserApps, true),
        filterSystemApps = valueOrDefault(hasFilterSystemApps(), filterSystemApps, false),
        filterAllApps = valueOrDefault(hasFilterAllApps(), filterAllApps, true),
        filterFrozenApps = valueOrDefault(hasFilterFrozenApps(), filterFrozenApps, true),
        filterUnfrozenApps = valueOrDefault(hasFilterUnfrozenApps(), filterUnfrozenApps, true),
        workingMode = valueOrDefault(hasWorkingMode(), workingMode, HailData.MODE_DEFAULT),
        biometricLogin = valueOrDefault(hasBiometricLogin(), biometricLogin, false),
        appTheme = valueOrDefault(hasAppTheme(), appTheme, HailData.FOLLOW_SYSTEM),
        iconPack = valueOrDefault(hasIconPack(), iconPack, HailData.ACTION_NONE),
        grayscaleIcon = valueOrDefault(hasGrayscaleIcon(), grayscaleIcon, true),
        compactIcon = valueOrDefault(hasCompactIcon(), compactIcon, false),
        synthesizeAdaptiveIcons = valueOrDefault(hasSynthesizeAdaptiveIcons(), synthesizeAdaptiveIcons, false),
        homeFontSize = valueOrDefault(hasHomeFontSize(), homeFontSize, default = 14f),
        fuzzySearch = valueOrDefault(hasFuzzySearch(), fuzzySearch, false),
        nineKeySearch = valueOrDefault(hasNineKeySearch(), nineKeySearch, false),
        tileAction = valueOrDefault(hasTileAction(), tileAction, HailData.AUTO_FREEZE_AFTER_LOCK),
        autoFreezeAfterLock = valueOrDefault(hasAutoFreezeAfterLock(), autoFreezeAfterLock, false),
        autoFreezeDelay = valueOrDefault(hasAutoFreezeDelay(), autoFreezeDelay, 0f),
        skipWhileCharging = valueOrDefault(hasSkipWhileCharging(), skipWhileCharging, false),
        skipForegroundApp = valueOrDefault(hasSkipForegroundApp(), skipForegroundApp, false),
        skipNotifyingApp = valueOrDefault(hasSkipNotifyingApp(), skipNotifyingApp, false),
        dynamicShortcutAction = valueOrDefault(hasDynamicShortcutAction(), dynamicShortcutAction, HailData.ACTION_NONE),
    )

/**
 * Converts [SettingsData] to [SettingsBackup] (protobuf wire format) for backup/restore.
 */
fun SettingsData.toSettingsBackup(): SettingsBackup =
    SettingsBackup
        .newBuilder()
        .setSortBy(sortBy)
        .setWorkingMode(workingMode)
        .setAppTheme(appTheme)
        .setIconPack(iconPack)
        .setTileAction(tileAction)
        .setDynamicShortcutAction(dynamicShortcutAction)
        .setFilterUserApps(filterUserApps)
        .setFilterSystemApps(filterSystemApps)
        .setFilterAllApps(filterAllApps)
        .setFilterFrozenApps(filterFrozenApps)
        .setFilterUnfrozenApps(filterUnfrozenApps)
        .setBiometricLogin(biometricLogin)
        .setGrayscaleIcon(grayscaleIcon)
        .setCompactIcon(compactIcon)
        .setSynthesizeAdaptiveIcons(synthesizeAdaptiveIcons)
        .setFuzzySearch(fuzzySearch)
        .setNineKeySearch(nineKeySearch)
        .setAutoFreezeAfterLock(autoFreezeAfterLock)
        .setSkipWhileCharging(skipWhileCharging)
        .setSkipForegroundApp(skipForegroundApp)
        .setSkipNotifyingApp(skipNotifyingApp)
        .setHomeFontSize(homeFontSize)
        .setAutoFreezeDelay(autoFreezeDelay)
        .build()

/**
 * Protobuf Serializer for Proto DataStore.
 */
object SettingsSerializer : Serializer<SettingsBackup> {
    override val defaultValue: SettingsBackup = SettingsBackup.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): SettingsBackup =
        try {
            SettingsBackup.parseFrom(input)
        } catch (e: IOException) {
            throw CorruptionException("Unable to read SettingsBackup proto.", e)
        }

    override suspend fun writeTo(
        t: SettingsBackup,
        output: OutputStream,
    ) {
        t.writeTo(output)
    }
}

/**
 * Proto DataStore for user settings.
 */
object SettingsDataStore {
    private const val DATA_STORE_NAME = "hail_settings.pb"

    private val dataStore: DataStore<SettingsBackup> by lazy {
        DataStoreFactory.create(
            serializer = SettingsSerializer,
            produceFile = { settingsFile(app, DATA_STORE_NAME) },
            corruptionHandler = ReplaceFileCorruptionHandler { SettingsBackup.getDefaultInstance() },
        )
    }

    /**
     * Ensures the DataStore instance is created. Call this early in app startup to
     * initialize DataStore eagerly rather than on first read.
     */
    fun ensureInitialized() = dataStore

    val settingsFlow: kotlinx.coroutines.flow.Flow<SettingsData>
        get() = dataStore.data.map { it.toSettingsData() }

    /**
     * Atomically updates the DataStore value using [transform] on the current [SettingsBackup.Builder].
     */
    suspend fun updateSettings(transform: suspend SettingsBackup.Builder.() -> Unit) {
        dataStore.updateData { current ->
            current.toBuilder().apply { transform() }.build()
        }
    }

    /**
     * Reads current settings from DataStore (suspends until DataStore is ready).
     */
    suspend fun readSettings(): SettingsData = dataStore.data.first().toSettingsData()
}

/**
 * Returns the file DataStore will use for storage.
 */
private fun settingsFile(
    context: Context,
    name: String,
): File = File(context.filesDir, "datastore/$name")

/**
 * Migrates all settings from SharedPreferences to Proto DataStore.
 *
 * Should be called once at app startup (from [HailApp.onCreate]) before DataStore is first read.
 * Safe to call multiple times — DataStore writes are idempotent.
 */
suspend fun migrateSettingsFromSharedPreferences() {
    val sp = app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val builder = SettingsBackup.newBuilder()

    sp.all.forEach { key, value ->
        when (value) {
            is String -> {
                setStringField(builder, key, value)
            }

            is Boolean -> {
                setBooleanField(builder, key, value)
            }

            is Float -> {
                setFloatField(builder, key, value)
            }

            // Other SharedPreferences value types are not backed up by HailData.
            else -> { }
        }
    }

    SettingsDataStore.updateSettings {
        mergeFrom(builder.build())
    }
}

private val stringSetters: Map<String, (SettingsBackup.Builder, String) -> Unit> =
    mapOf(
        HailData.SORT_BY to { builder, value -> builder.setSortBy(value) },
        HailData.WORKING_MODE to { builder, value -> builder.setWorkingMode(value) },
        HailData.APP_THEME to { builder, value -> builder.setAppTheme(value) },
        HailData.ICON_PACK to { builder, value -> builder.setIconPack(value) },
        HailData.TILE_ACTION to { builder, value -> builder.setTileAction(value) },
        HailData.DYNAMIC_SHORTCUT_ACTION to { builder, value -> builder.setDynamicShortcutAction(value) },
    )

private val booleanSetters: Map<String, (SettingsBackup.Builder, Boolean) -> Unit> =
    mapOf(
        HailData.FILTER_USER_APPS to { builder, value -> builder.setFilterUserApps(value) },
        HailData.FILTER_SYSTEM_APPS to { builder, value -> builder.setFilterSystemApps(value) },
        HailData.FILTER_ALL_APPS to { builder, value -> builder.setFilterAllApps(value) },
        HailData.FILTER_FROZEN_APPS to { builder, value -> builder.setFilterFrozenApps(value) },
        HailData.FILTER_UNFROZEN_APPS to { builder, value -> builder.setFilterUnfrozenApps(value) },
        HailData.BIOMETRIC_LOGIN to { builder, value -> builder.setBiometricLogin(value) },
        HailData.GRAYSCALE_ICON to { builder, value -> builder.setGrayscaleIcon(value) },
        HailData.COMPACT_ICON to { builder, value -> builder.setCompactIcon(value) },
        HailData.SYNTHESIZE_ADAPTIVE_ICONS to { builder, value -> builder.setSynthesizeAdaptiveIcons(value) },
        HailData.FUZZY_SEARCH to { builder, value -> builder.setFuzzySearch(value) },
        HailData.NINE_KEY_SEARCH to { builder, value -> builder.setNineKeySearch(value) },
        HailData.AUTO_FREEZE_AFTER_LOCK to { builder, value -> builder.setAutoFreezeAfterLock(value) },
        HailData.SKIP_WHILE_CHARGING to { builder, value -> builder.setSkipWhileCharging(value) },
        HailData.SKIP_FOREGROUND_APP to { builder, value -> builder.setSkipForegroundApp(value) },
        HailData.SKIP_NOTIFYING_APP to { builder, value -> builder.setSkipNotifyingApp(value) },
    )

private val floatSetters: Map<String, (SettingsBackup.Builder, Float) -> Unit> =
    mapOf(
        HailData.HOME_FONT_SIZE to { builder, value -> builder.setHomeFontSize(value) },
        HailData.AUTO_FREEZE_DELAY to { builder, value -> builder.setAutoFreezeDelay(value) },
    )

private fun setStringField(
    builder: SettingsBackup.Builder,
    key: String,
    value: String,
) {
    stringSetters[key]?.invoke(builder, value)
}

private fun setBooleanField(
    builder: SettingsBackup.Builder,
    key: String,
    value: Boolean,
) {
    booleanSetters[key]?.invoke(builder, value)
}

private fun setFloatField(
    builder: SettingsBackup.Builder,
    key: String,
    value: Float,
) {
    floatSetters[key]?.invoke(builder, value)
}

/**
 * SharedPreferences file name used by HailData prior to DataStore migration.
 */
private const val PREFS_NAME = "hail_preferences"
