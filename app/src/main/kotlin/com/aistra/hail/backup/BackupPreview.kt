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

fun entriesOf(file: File): List<BackupEntry> {
    val zipFile = ZipFile(file)
    try {
        val entries = mutableMapOf<BackupCategory, Long>()
        val enum = zipFile.entries()
        while (enum.hasMoreElements()) {
            val entry = enum.nextElement()
            if (!entry.isDirectory) {
                entryCategory(entry.name)?.let { category ->
                    entries[category] = entry.size
                }
            }
        }
        return listOf(
            BackupCategory.APPS,
            BackupCategory.WHITELIST,
            BackupCategory.ACTIONS,
            BackupCategory.SETTINGS
        ).map { category ->
            val size = entries[category] ?: 0L
            BackupEntry(category, size > 0, size)
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