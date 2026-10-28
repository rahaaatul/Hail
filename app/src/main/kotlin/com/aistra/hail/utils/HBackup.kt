package com.aistra.hail.utils

import android.content.Context
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.app.SettingsDataStore
import com.aistra.hail.app.toSettingsBackup
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import com.aistra.hail.backup.RestorePlan
import com.aistra.hail.backup.SettingsBackup
import com.aistra.hail.utils.ActionsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

const val BACKUP_FORMAT = "hail-backup"
const val BACKUP_VERSION = 1
const val MANIFEST_ENTRY = "manifest.json"
const val APPS_ENTRY = "apps.json"
const val TAGS_ENTRY = "tags.json"
const val ACTIONS_ENTRY = "actions.json"
const val SETTINGS_ENTRY = "settings.bin"
const val WHITELIST_ENTRY = "whitelist.json"

private const val KEY_PACKAGE = "package"
private const val KEY_PINNED = "pinned"
private const val KEY_WHITELISTED = "whitelisted"
private const val KEY_TAGS = "tags"
private const val KEY_ID = "id"
private const val KEY_LAUNCH_PACKAGE = "launchPackage"
private const val KEY_UNFREEZE_PACKAGES = "unfreezePackages"

private const val MAX_SAFE_INTEGER = 9.007199254740992E15

// Zip-bomb protection: refuse to restore archives that are likely decompression bombs.
private const val MAX_ZIP_ENTRY_COUNT = 1000
private const val MAX_ZIP_TOTAL_UNCOMPRESSED = 100L * 1024 * 1024 // 100 MiB
private const val MAX_ZIP_COMPRESSION_RATIO = 100.0

suspend fun backup(
    context: Context,
    outputFile: File,
    options: BackupOptions,
): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            val tempFile = File(context.cacheDir, "backup-${System.currentTimeMillis()}.zip.tmp")
            try {
                ZipOutputStream(FileOutputStream(tempFile)).use { zos ->
                    zos.writeManifestEntry()
                    if (options.apps) zos.writeAppsEntry()
                    if (options.tags) zos.writeTagsEntry()
                    if (options.actions) zos.writeActionsEntry()
                    if (options.settings) zos.writeSettingsEntry()
                    if (options.whitelist) zos.writeWhitelistEntry()
                }

                // Atomic move to final location
                if (outputFile.exists()) outputFile.delete()
                Files.move(tempFile.toPath(), outputFile.toPath(), StandardCopyOption.ATOMIC_MOVE)
                Unit
            } catch (e: IOException) {
                tempFile.delete()
                throw e
            } finally {
                tempFile.delete()
            }
        }
    }

suspend fun restore(
    context: Context,
    inputFile: File,
    options: RestoreOptions,
): Result<Unit> =
    withContext(Dispatchers.IO) {
        runCatching {
            // Phase 0: Zip-bomb protection — refuse archives that are likely
            // decompression bombs before touching any entry data.
            validateArchiveSize(inputFile)

            // Phase 1: Validate manifest
            validateManifest(inputFile)

            // Phase 2: Build RestorePlan by reading all entries
            val plan = buildRestorePlan(inputFile)

            // Phase 3: Cross-entry validation
            validatePlan(plan)

            // Phase 4: Apply atomically
            applyPlan(context, plan, options)
        }
    }

private fun validateArchiveSize(inputFile: File) {
    ZipFile(inputFile).use { zip ->
        val entries = zip.entries()
        var entryCount = 0
        var totalUncompressed = 0L
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            entryCount++
            if (entryCount > MAX_ZIP_ENTRY_COUNT) {
                error("Too many entries in archive: $entryCount > $MAX_ZIP_ENTRY_COUNT")
            }
            val size = entry.size
            if (size < 0) {
                // ZIP64 / unknown size — treat conservatively.
                error("Archive entry with unknown uncompressed size: ${entry.name}")
            }
            totalUncompressed += size
            if (totalUncompressed > MAX_ZIP_TOTAL_UNCOMPRESSED) {
                error(
                    "Archive too large: $totalUncompressed bytes uncompressed > $MAX_ZIP_TOTAL_UNCOMPRESSED",
                )
            }
            val compressed = entry.compressedSize
            if (compressed > 0 && size.toDouble() / compressed > MAX_ZIP_COMPRESSION_RATIO) {
                error(
                    "Suspicious compression ratio for ${entry.name}: " +
                        "$size uncompressed / $compressed compressed",
                )
            }
        }
    }
}

private fun ZipOutputStream.writeEntry(
    name: String,
    content: String,
) {
    putNextEntry(ZipEntry(name))
    write(content.toByteArray(StandardCharsets.UTF_8))
    closeEntry()
}

private fun ZipOutputStream.writeBinaryEntry(
    name: String,
    content: ByteArray,
) {
    putNextEntry(ZipEntry(name))
    write(content)
    closeEntry()
}

private suspend fun ZipOutputStream.writeManifestEntry() {
    val manifest =
        JSONObject().apply {
            put("format", BACKUP_FORMAT)
            put("version", BACKUP_VERSION)
        }
    writeEntry(MANIFEST_ENTRY, manifest.toString(2))
}

private suspend fun ZipOutputStream.writeAppsEntry() {
    val appsArray = JSONArray()
    HailData.checkedList.forEach { app ->
        appsArray.put(
            JSONObject().apply {
                put(KEY_PACKAGE, app.packageName)
                put(KEY_PINNED, app.pinned)
                put(KEY_WHITELISTED, app.whitelisted)
                put(KEY_TAGS, JSONArray(app.tagIdList))
            },
        )
    }
    writeEntry(APPS_ENTRY, appsArray.toString(2))
}

private suspend fun ZipOutputStream.writeTagsEntry() {
    val tagsArray = JSONArray()
    HailData.tags.forEach { (tagName, tagId) ->
        tagsArray.put(
            JSONObject().apply {
                put("tag", tagName)
                put("id", tagId)
            },
        )
    }
    writeEntry(TAGS_ENTRY, tagsArray.toString(2))
}

private suspend fun ZipOutputStream.writeActionsEntry() {
    val actions = ActionsRepository.loadAll()
    val actionsArray = JSONArray()
    actions.forEach { action ->
        actionsArray.put(
            JSONObject().apply {
                put(KEY_ID, action.id)
                put(KEY_LAUNCH_PACKAGE, action.launchPackage)
                put(KEY_UNFREEZE_PACKAGES, JSONArray(action.unfreezePackages))
            },
        )
    }
    writeEntry(ACTIONS_ENTRY, actionsArray.toString(2))
}

private suspend fun ZipOutputStream.writeSettingsEntry() {
    val settingsData = SettingsDataStore.readSettings()
    val settings = settingsData.toSettingsBackup()
    val byteArray = settings.toByteArray()
    writeBinaryEntry(SETTINGS_ENTRY, byteArray)
}

private suspend fun ZipOutputStream.writeWhitelistEntry() {
    val whitelistArray = JSONArray()
    HailData.checkedList.filter { it.whitelisted }.forEach { app ->
        whitelistArray.put(app.packageName)
    }
    writeEntry(WHITELIST_ENTRY, whitelistArray.toString(2))
}

private fun validateManifest(inputFile: File) {
    ZipFile(inputFile).use { zip ->
        val manifestEntry = zip.getEntry(MANIFEST_ENTRY) ?: error("Missing manifest.json")
        val inputStream = zip.getInputStream(manifestEntry)
        inputStream.buffered().use { input ->
            val bytes = input.readBytes()
            val manifestJson = JSONObject(String(bytes))
            val format = manifestJson.getString("format")
            val version = manifestJson.getInt("version")
            if (format != BACKUP_FORMAT) {
                error("Unsupported backup format: $format")
            }
            if (version > BACKUP_VERSION) {
                error("Backup version $version is newer than supported version $BACKUP_VERSION")
            }
        }
    }
}

private fun buildRestorePlan(inputFile: File): RestorePlan {
    val apps = mutableListOf<JSONObject>()
    val tags = mutableListOf<JSONObject>()
    val actions = mutableListOf<JSONObject>()

    var settings: SettingsBackup? = null

    ZipFile(inputFile).use { zip ->
        val appsArray = readJsonArray(zip, APPS_ENTRY)
        for (i in 0 until appsArray.length()) {
            apps.add(appsArray.getJSONObject(i))
        }
        val tagsArray = readJsonArray(zip, TAGS_ENTRY)
        for (i in 0 until tagsArray.length()) {
            tags.add(tagsArray.getJSONObject(i))
        }
        val actionsArray = readJsonArray(zip, ACTIONS_ENTRY)
        for (i in 0 until actionsArray.length()) {
            actions.add(actionsArray.getJSONObject(i))
        }
        settings = readSettingsEntry(zip)
    }

    return RestorePlan(
        apps = apps,
        tags = tags,
        actions = actions,
        settings = settings,
        entryCounts =
            mapOf(
                BackupCategory.APPS to apps.size,
                BackupCategory.TAGS to tags.size,
                BackupCategory.ACTIONS to actions.size,
                BackupCategory.SETTINGS to if (settings != null) 1 else 0,
            ),
    )
}

private fun readJsonArray(
    zip: ZipFile,
    entryName: String,
): JSONArray {
    val entry = zip.getEntry(entryName) ?: error("Missing $entryName")
    val inputStream = zip.getInputStream(entry)
    inputStream.buffered().use { input ->
        val bytes = input.readBytes()
        return JSONArray(String(bytes))
    }
    error("Failed to read $entryName")
}

private fun readSettingsEntry(zip: ZipFile): SettingsBackup? {
    val entry = zip.getEntry(SETTINGS_ENTRY) ?: return null
    val inputStream = zip.getInputStream(entry)
    return inputStream.buffered().use { input ->
        val bytes = input.readBytes()
        try {
            SettingsBackup.parseFrom(bytes)
        } catch (e: IOException) {
            HLog.e(e)
            null
        }
    }
}

private fun validatePlan(plan: RestorePlan) {
    val validTagIds = plan.tags.map { it.getInt("id") }.toSet()
    plan.apps.forEach { app ->
        val tagIds = app.getJSONArray(KEY_TAGS)
        for (i in 0 until tagIds.length()) {
            val tagId = tagIds.getInt(i)
            if (tagId !in validTagIds) {
                error("App references unknown tag id: $tagId")
            }
        }
    }
}

private suspend fun applyPlan(
    ignored: Context,
    plan: RestorePlan,
    options: RestoreOptions,
) {
    if (options.apps) applyApps(plan.apps)
    if (options.tags) applyTags(plan.tags)
    if (options.actions) applyActions(plan.actions)
    if (options.settings) applySettings(plan.settings)
    // Whitelisted flags are already restored as part of APPS entries
}

private suspend fun applyApps(apps: List<JSONObject>) {
    apps.forEach { app ->
        val packageName = app.getString(KEY_PACKAGE)
        val existing = HailData.checkedList.find { it.packageName == packageName }
        val tagIdList =
            app.optJSONArray(KEY_TAGS)?.let {
                MutableList(it.length()) { index -> it.getInt(index) }
            } ?: mutableListOf(0)
        if (existing != null) {
            // Update the app in place; don't drop tracked apps absent from the archive.
            existing.pinned = app.optBoolean(KEY_PINNED, false)
            existing.whitelisted = app.optBoolean(KEY_WHITELISTED, false)
            existing.tagIdList.clear()
            existing.tagIdList.addAll(tagIdList)
        } else {
            HailData.checkedList.add(
                AppInfo(
                    packageName = packageName,
                    pinned = app.optBoolean(KEY_PINNED, false),
                    whitelisted = app.optBoolean(KEY_WHITELISTED, false),
                    tagIdList = tagIdList,
                ),
            )
        }
    }
    HailData.saveApps()
}

private suspend fun applyTags(tags: List<JSONObject>) {
    HailData.tags.clear()
    tags.forEach { tag ->
        HailData.tags.add(tag.getString("tag") to tag.getInt("id"))
    }
    HailData.saveTags()
}

private suspend fun applyActions(actions: List<JSONObject>) {
    actions.forEach { action ->
        ActionsRepository.save(
            id = action.getString(KEY_ID),
            launchPackage = action.getString(KEY_LAUNCH_PACKAGE),
            unfreezePackages =
                action.getJSONArray(KEY_UNFREEZE_PACKAGES).let {
                    MutableList(it.length()) { index -> it.getString(index) }
                },
        )
    }
}

private suspend fun applySettings(settings: SettingsBackup?) {
    if (settings == null) return
    SettingsDataStore.updateSettings {
        // Restore only the settings that were backed up; never wipe fields
        // absent from the archive. Proto3 optional fields use hasXxx() to
        // distinguish "set" from "default".
        val appliedSettings =
            listOf(
                settings.hasSortBy() to { setSortBy(settings.sortBy) },
                settings.hasWorkingMode() to { setWorkingMode(settings.workingMode) },
                settings.hasAppTheme() to { setAppTheme(settings.appTheme) },
                settings.hasIconPack() to { setIconPack(settings.iconPack) },
                settings.hasTileAction() to { setTileAction(settings.tileAction) },
                settings.hasDynamicShortcutAction() to { setDynamicShortcutAction(settings.dynamicShortcutAction) },
                settings.hasFilterUserApps() to { setFilterUserApps(settings.filterUserApps) },
                settings.hasFilterSystemApps() to { setFilterSystemApps(settings.filterSystemApps) },
                settings.hasFilterAllApps() to { setFilterAllApps(settings.filterAllApps) },
                settings.hasFilterFrozenApps() to { setFilterFrozenApps(settings.filterFrozenApps) },
                settings.hasFilterUnfrozenApps() to { setFilterUnfrozenApps(settings.filterUnfrozenApps) },
                settings.hasBiometricLogin() to { setBiometricLogin(settings.biometricLogin) },
                settings.hasGrayscaleIcon() to { setGrayscaleIcon(settings.grayscaleIcon) },
                settings.hasCompactIcon() to { setCompactIcon(settings.compactIcon) },
                settings.hasSynthesizeAdaptiveIcons() to { setSynthesizeAdaptiveIcons(settings.synthesizeAdaptiveIcons) },
                settings.hasFuzzySearch() to { setFuzzySearch(settings.fuzzySearch) },
                settings.hasNineKeySearch() to { setNineKeySearch(settings.nineKeySearch) },
                settings.hasAutoFreezeAfterLock() to { setAutoFreezeAfterLock(settings.autoFreezeAfterLock) },
                settings.hasSkipWhileCharging() to { setSkipWhileCharging(settings.skipWhileCharging) },
                settings.hasSkipForegroundApp() to { setSkipForegroundApp(settings.skipForegroundApp) },
                settings.hasSkipNotifyingApp() to { setSkipNotifyingApp(settings.skipNotifyingApp) },
            )
        appliedSettings.forEach { (condition, apply) -> if (condition) apply() }
    }
}

// Float magnitude guards from prior work
fun isMagnitudeTooWide(value: Double): Boolean = value.isInfinite() || value.isNaN() || kotlin.math.abs(value) > MAX_SAFE_INTEGER

fun isTooWideInteger(value: Double): Boolean = value != value.toLong().toDouble() || value > MAX_SAFE_INTEGER || value < -MAX_SAFE_INTEGER

fun toExactLongOrNull(value: Double): Long? = if (isTooWideInteger(value)) null else value.toLong()
