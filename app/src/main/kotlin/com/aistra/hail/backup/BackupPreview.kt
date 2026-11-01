package com.aistra.hail.backup

import com.aistra.hail.utils.ACTIONS_ENTRY
import com.aistra.hail.utils.APPS_ENTRY
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.SETTINGS_ENTRY
import com.aistra.hail.utils.TAGS_ENTRY
import com.aistra.hail.utils.WHITELIST_ENTRY
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.zip.ZipFile

enum class BackupCategory {
    APPS,
    TAGS,
    ACTIONS,
    SETTINGS,
    WHITELIST,
}

data class BackupEntry(
    val category: BackupCategory,
    val name: String,
    val present: Boolean,
    val sizeBytes: Long,
    val itemCount: Int = 0,
)

data class Manifest(
    val format: String,
    val version: Int,
)

data class RestorePlan(
    val apps: List<org.json.JSONObject>,
    val tags: List<org.json.JSONObject>,
    val actions: List<org.json.JSONObject>,
    val settings: SettingsBackup?,
    val entryCounts: Map<BackupCategory, Int>,
)

sealed interface RestoreState {
    object Empty : RestoreState

    data class Loaded(
        val file: File,
        val displayName: String,
        val sizeBytes: Long,
        val entries: List<BackupEntry>,
        val options: RestoreOptions,
        val phase: Phase,
        val message: String?,
    ) : RestoreState
}

enum class Phase {
    Idle,
    Working,
    Done,
    Failed,
}

data class BackupOptions(
    val apps: Boolean = true,
    val tags: Boolean = true,
    val actions: Boolean = true,
    val settings: Boolean = true,
    val whitelist: Boolean = true,
)

fun BackupOptions.checked(category: BackupCategory): Boolean =
    when (category) {
        BackupCategory.APPS -> apps
        BackupCategory.TAGS -> tags
        BackupCategory.ACTIONS -> actions
        BackupCategory.SETTINGS -> settings
        BackupCategory.WHITELIST -> whitelist
    }

data class RestoreOptions(
    val apps: Boolean = true,
    val tags: Boolean = true,
    val actions: Boolean = true,
    val settings: Boolean = true,
    val whitelist: Boolean = true,
)

fun RestoreOptions.checked(category: BackupCategory): Boolean =
    when (category) {
        BackupCategory.APPS -> apps
        BackupCategory.TAGS -> tags
        BackupCategory.ACTIONS -> actions
        BackupCategory.SETTINGS -> settings
        BackupCategory.WHITELIST -> whitelist
    }

fun entriesOf(file: File): List<BackupEntry> =
    ZipFile(file).use { zip ->
        val entries = mutableListOf<BackupEntry>()
        val enumeration = zip.entries()
        while (enumeration.hasMoreElements()) {
            val entry = enumeration.nextElement()
            val category = categoryOf(entry.name)
            if (category != null) {
                entries.add(
                    BackupEntry(
                        category = category,
                        name = entry.name,
                        present = true,
                        sizeBytes = entry.size,
                        itemCount = countItems(zip, entry),
                    ),
                )
            }
        }
        BackupCategory.values().forEach { category ->
            if (entries.none { it.category == category }) {
                entries.add(
                    BackupEntry(
                        category = category,
                        name = fileNameOf(category),
                        present = false,
                        sizeBytes = 0,
                        itemCount = 0,
                    ),
                )
            }
        }
        entries.sortedBy { it.category.ordinal }
    }

@Suppress("TooGenericExceptionCaught", "SwallowedException")
internal fun countItems(
    zip: ZipFile,
    entry: java.util.zip.ZipEntry,
): Int =
    try {
        zip.getInputStream(entry).buffered().use { input ->
            countItemsIn(input.readBytes(), entry.name)
        }
    } catch (e: Exception) {
        0
    }

/**
 * Counts the items contained in a backup entry's raw bytes, based on its entry name.
 */
private fun countItemsIn(
    bytes: ByteArray,
    entryName: String,
): Int =
    when (entryName) {
        APPS_ENTRY, TAGS_ENTRY, ACTIONS_ENTRY, WHITELIST_ENTRY -> {
            JSONArray(String(bytes, StandardCharsets.UTF_8)).length()
        }

        SETTINGS_ENTRY -> {
            countSettingsItems(bytes)
        }

        else -> {
            0
        }
    }

/**
 * Counts the explicitly-set fields in a backed-up [SettingsBackup]. Proto3 optional fields
 * use [SettingsBackup.hasXxx] to distinguish "set" from default.
 */
private fun countSettingsItems(bytes: ByteArray): Int =
    try {
        SettingsBackup.parseFrom(bytes).let { settings ->
            listOf(
                settings.hasSortBy(),
                settings.hasWorkingMode(),
                settings.hasAppTheme(),
                settings.hasIconPack(),
                settings.hasTileAction(),
                settings.hasDynamicShortcutAction(),
                settings.hasFilterUserApps(),
                settings.hasFilterSystemApps(),
                settings.hasFilterAllApps(),
                settings.hasFilterFrozenApps(),
                settings.hasFilterUnfrozenApps(),
                settings.hasBiometricLogin(),
                settings.hasGrayscaleIcon(),
                settings.hasCompactIcon(),
                settings.hasSynthesizeAdaptiveIcons(),
                settings.hasFuzzySearch(),
                settings.hasNineKeySearch(),
                settings.hasAutoFreezeAfterLock(),
                settings.hasSkipWhileCharging(),
                settings.hasSkipForegroundApp(),
                settings.hasSkipNotifyingApp(),
                settings.hasHomeFontSize(),
                settings.hasAutoFreezeDelay(),
            ).count { it }
        }
    } catch (e: IOException) {
        HLog.e(e)
        0
    }

internal fun categoryOf(entryName: String): BackupCategory? =
    when (entryName) {
        "apps.json" -> BackupCategory.APPS
        "tags.json" -> BackupCategory.TAGS
        "actions.json" -> BackupCategory.ACTIONS
        "settings.bin" -> BackupCategory.SETTINGS
        "whitelist.json" -> BackupCategory.WHITELIST
        else -> null
    }

internal fun fileNameOf(category: BackupCategory): String =
    when (category) {
        BackupCategory.APPS -> "apps.json"
        BackupCategory.TAGS -> "tags.json"
        BackupCategory.ACTIONS -> "actions.json"
        BackupCategory.SETTINGS -> "settings.bin"
        BackupCategory.WHITELIST -> "whitelist.json"
    }

fun canStartBackup(options: BackupOptions): Boolean = options.apps || options.tags || options.actions || options.settings || options.whitelist

fun canStartRestore(state: RestoreState): Boolean {
    if (state !is RestoreState.Loaded) return false
    return state.entries.any { it.present }
}
