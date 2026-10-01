package com.aistra.hail.backup

import com.aistra.hail.utils.HBackup
import java.io.File
import java.util.zip.ZipFile

enum class BackupCategory {
    APPS, WHITELIST, ACTIONS, SETTINGS
}

data class BackupEntry(
    val category: BackupCategory,
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
        val entries = mutableMapOf<BackupCategory, Pair<Boolean, Long>>()
        val zipEntries = zipFile.entries()
        while (zipEntries.hasMoreElements()) {
            val entry = zipEntries.nextElement()
            if (!entry.isDirectory) {
                entryCategory(entry.name)?.let { category ->
                    val size = if (entry.size >= 0) entry.size else 0L
                    entries[category] = true to size
                }
            }
        }
        return listOf(
            BackupCategory.APPS,
            BackupCategory.WHITELIST,
            BackupCategory.ACTIONS,
            BackupCategory.SETTINGS
        ).map { category ->
            val (present, size) = entries[category] ?: (false to 0L)
            BackupEntry(category, present, size)
        }
    } finally {
        zipFile.close()
    }
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