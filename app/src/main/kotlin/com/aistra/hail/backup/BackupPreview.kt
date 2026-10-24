package com.aistra.hail.backup

import com.aistra.hail.utils.HBackup
import java.io.File
import java.util.zip.ZipFile

enum class BackupCategory {
    APPS, WHITELIST, ACTIONS, SETTINGS
}

data class BackupEntry(
    val category: BackupCategory,
    val name: String,
    val present: Boolean,
    val sizeBytes: Long
)

sealed interface RestoreState {
    data object Empty : RestoreState
    data class Loaded(
        val file: File,
        val displayName: String,
        val sizeBytes: Long,
        val entries: List<BackupEntry>,
        val options: HBackup.RestoreOptions,
        val phase: Phase,
        val message: String?,
    ) : RestoreState
}

enum class Phase { Idle, Working, Done, Failed }

fun entryCategory(name: String): BackupCategory? = when (name) {
    "apps.json" -> BackupCategory.APPS
    "whitelist.json" -> BackupCategory.WHITELIST
    "actions.json" -> BackupCategory.ACTIONS
    "settings.json" -> BackupCategory.SETTINGS
    else -> null
}

/**
 * Reads the central directory of a zip file and returns one [BackupEntry] per category.
 * The returned list always contains exactly four entries in fixed order: APPS, WHITELIST,
 * ACTIONS, SETTINGS. Each entry's [BackupEntry.present] is true when an entry with the
 * corresponding name exists in the zip, regardless of its size (including zero-byte entries).
 * [BackupEntry.sizeBytes] is the uncompressed size from the central directory, or 0 when
 * the size is unknown (-1 in the ZIP format) or the category is absent.
 * [BackupEntry.name] is the archive's own entry name when present, otherwise the canonical
 * name for that category (apps.json, whitelist.json, actions.json, settings.json).
 *
 * If the zip contains multiple entries with the same name, the last one encountered
 * (the one `ZipFile.entries()` yields last) is the one reported, matching the behaviour
 * of the restore path which reads entries sequentially.
 *
 * @throws java.util.zip.ZipException if the file is not a valid zip archive or cannot be read.
 */
fun entriesOf(file: File): List<BackupEntry> {
    val zipFile = ZipFile(file)
    try {
        val entries = mutableMapOf<BackupCategory, Pair<String, Long>>()
        val zipEntries = zipFile.entries()
        while (zipEntries.hasMoreElements()) {
            val entry = zipEntries.nextElement()
            if (!entry.isDirectory) {
                entryCategory(entry.name)?.let { category ->
                    val size = if (entry.size >= 0) entry.size else 0L
                    entries[category] = entry.name to size
                }
            }
        }
        return listOf(
            BackupCategory.APPS,
            BackupCategory.WHITELIST,
            BackupCategory.ACTIONS,
            BackupCategory.SETTINGS
        ).map { category ->
            val (name, size) = entries[category] ?: (canonicalName(category) to 0L)
            val present = category in entries
            BackupEntry(category, name, present, size)
        }
    } finally {
        zipFile.close()
    }
}

private fun canonicalName(category: BackupCategory): String = when (category) {
    BackupCategory.APPS -> "apps.json"
    BackupCategory.WHITELIST -> "whitelist.json"
    BackupCategory.ACTIONS -> "actions.json"
    BackupCategory.SETTINGS -> "settings.json"
}

fun canStartBackup(options: HBackup.BackupOptions): Boolean =
    options.apps || options.whitelist || options.actions || options.settings

fun canStartRestore(state: RestoreState): Boolean = when (state) {
    is RestoreState.Empty -> false
    is RestoreState.Loaded -> {
        if (state.phase != Phase.Idle) return false
        state.options.apps || state.options.whitelist || state.options.actions || state.options.settings
    }
}