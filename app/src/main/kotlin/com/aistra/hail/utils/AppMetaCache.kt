package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import androidx.room.Room
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

    private val cache = ConcurrentHashMap<String, Entry>()
    private val packageLocks = ConcurrentHashMap<String, Mutex>()
    private val database by lazy {
        Room.databaseBuilder(HailApp.app, AppMetadataDatabase::class.java, "app_metadata.db").build()
    }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _revision = MutableStateFlow(0L)
    val revision: StateFlow<Long> = _revision

    fun get(packageName: String): Entry? = cache[packageName]

    fun seedFromDatabase(): Job = scope.launch {
        runCatching {
            database.appMetadataDao().loadAll().forEach { entity ->
                cache[entity.packageName] = entity.toEntry()
            }
            _revision.value++
        }
    }

    fun prefetch(applicationInfo: Collection<ApplicationInfo>): Job = scope.launch {
        val installedPackages = applicationInfo.mapTo(HashSet()) { it.packageName }
        applicationInfo.map { info ->
            async { loadIfStale(info.packageName, info) }
        }.awaitAll()
        persist(installedPackages)
    }

    fun prefetchPackages(packageNames: Collection<String>): Job = scope.launch {
        packageNames.map { packageName ->
            async { loadIfStale(packageName) }
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

    private suspend fun persist(installedPackages: Set<String>? = null) {
        runCatching {
            val dao = database.appMetadataDao()
            if (installedPackages != null) {
                cache.keys.removeIf { it !in installedPackages }
                dao.replaceAll(cache.values.map { it.toEntity() })
            } else {
                dao.upsertAll(cache.values.map { it.toEntity() })
            }
        }
    }

    private fun AppMetadataEntity.toEntry() = Entry(
        packageName = packageName,
        name = name,
        isSystemApp = systemApp,
        firstInstallTime = firstInstallTime,
        lastUpdateTime = lastUpdateTime,
        flags = flags,
        enabled = enabled,
        state = AppInfo.State.UNFROZEN,
        sourceSignature = sourceSignature
    )

    private fun Entry.toEntity() = AppMetadataEntity().also {
        it.packageName = packageName
        it.name = name
        it.systemApp = isSystemApp
        it.firstInstallTime = firstInstallTime
        it.lastUpdateTime = lastUpdateTime
        it.flags = flags
        it.enabled = enabled
        it.sourceSignature = sourceSignature
    }
}
