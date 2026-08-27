package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.AppManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object AppMetaCache {
    data class Entry(
        val packageName: String,
        val name: String,
        val isSystemApp: Boolean,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
        val flags: Int,
        val enabled: Boolean,
        val state: AppInfo.State,
        val sourceSignature: String
    )

    private const val VERSION = 1
    private const val KEY_VERSION = "version"
    private const val KEY_ENTRIES = "entries"
    private const val KEY_PACKAGE = "package"
    private const val KEY_NAME = "name"
    private const val KEY_SYSTEM = "system"
    private const val KEY_FIRST_INSTALL = "firstInstall"
    private const val KEY_LAST_UPDATE = "lastUpdate"
    private const val KEY_FLAGS = "flags"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_SOURCE = "source"

    private val cache = ConcurrentHashMap<String, Entry>()
    private val packageLocks = ConcurrentHashMap<String, Mutex>()
    private val writeMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val metadataFile by lazy { File(HailApp.app.filesDir, "v1/app_meta.json") }
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    fun get(packageName: String): Entry? = cache[packageName]

    fun seedFromDisk() {
        runCatching {
            val root = JSONObject(metadataFile.readText())
            if (root.optInt(KEY_VERSION) != VERSION) return
            val entries = root.optJSONArray(KEY_ENTRIES) ?: return
            for (index in 0 until entries.length()) {
                val json = entries.optJSONObject(index) ?: continue
                val entry = Entry(
                    packageName = json.getString(KEY_PACKAGE),
                    name = json.getString(KEY_NAME),
                    isSystemApp = json.getBoolean(KEY_SYSTEM),
                    firstInstallTime = json.getLong(KEY_FIRST_INSTALL),
                    lastUpdateTime = json.getLong(KEY_LAST_UPDATE),
                    flags = json.getInt(KEY_FLAGS),
                    enabled = json.getBoolean(KEY_ENABLED),
                    state = AppInfo.State.UNFROZEN,
                    sourceSignature = json.getString(KEY_SOURCE)
                )
                cache[entry.packageName] = entry
            }
            _revision.value++
        }
    }

    fun prefetch(applicationInfo: Collection<ApplicationInfo>): Job = scope.launch {
        applicationInfo.map { info ->
            async {
                loadIfStale(info.packageName, info)
            }
        }.awaitAll()
        persist()
    }

    fun prefetchPackages(packageNames: Collection<String>): Job = scope.launch {
        packageNames.map { packageName ->
            async {
                loadIfStale(packageName)
            }
        }.awaitAll()
        persist()
    }

    fun invalidateState(packageNames: Collection<String> = cache.keys) {
        packageNames.forEach { packageName ->
            cache.computeIfPresent(packageName) { _, entry ->
                entry.copy(state = readState(packageName))
            }
        }
        _revision.value++
    }

    fun invalidateAll() {
        cache.clear()
        _revision.value++
    }

    private suspend fun loadIfStale(packageName: String, knownInfo: ApplicationInfo? = null) {
        val lock = packageLocks.getOrPut(packageName) { Mutex() }
        lock.withLock {
            val info = knownInfo ?: HPackages.getApplicationInfoOrNull(packageName)
            val packageInfo = HPackages.getUnhiddenPackageInfoOrNull(packageName)
            val sourceSignature = listOf(
                info?.sourceDir,
                info?.sourceDir?.let { File(it).lastModified() },
                packageInfo?.lastUpdateTime
            ).joinToString(":")
            val current = cache[packageName]
            if (info != null && current?.sourceSignature == sourceSignature) {
                cache[packageName] = current.copy(state = readState(packageName))
                return
            }

            val entry = if (info == null) {
                current?.copy(state = AppInfo.State.NOT_FOUND)
            } else {
                Entry(
                    packageName = packageName,
                    name = info.loadLabel(HailApp.app.packageManager).toString(),
                    isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                    firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                    flags = info.flags,
                    enabled = info.enabled,
                    state = readState(packageName),
                    sourceSignature = sourceSignature
                )
            }
            if (entry == null) cache.remove(packageName) else cache[packageName] = entry
            _revision.value++
        }
    }

    private fun readState(packageName: String): AppInfo.State = when {
        HPackages.getApplicationInfoOrNull(packageName) == null -> AppInfo.State.NOT_FOUND
        AppManager.isAppFrozen(packageName) -> AppInfo.State.FROZEN
        else -> AppInfo.State.UNFROZEN
    }

    private suspend fun persist() = writeMutex.withLock {
        runCatching {
            val directory = metadataFile.parentFile ?: return@runCatching
            directory.mkdirs()
            val root = JSONObject().put(KEY_VERSION, VERSION).put(KEY_ENTRIES, JSONArray().apply {
                cache.values.forEach { entry ->
                    put(JSONObject()
                        .put(KEY_PACKAGE, entry.packageName)
                        .put(KEY_NAME, entry.name)
                        .put(KEY_SYSTEM, entry.isSystemApp)
                        .put(KEY_FIRST_INSTALL, entry.firstInstallTime)
                        .put(KEY_LAST_UPDATE, entry.lastUpdateTime)
                        .put(KEY_FLAGS, entry.flags)
                        .put(KEY_ENABLED, entry.enabled)
                        .put(KEY_SOURCE, entry.sourceSignature))
                }
            })
            val temporary = File(metadataFile.path + ".tmp")
            temporary.writeText(root.toString())
            check(temporary.renameTo(metadataFile))
        }
    }
}
