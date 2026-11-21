package com.aistra.hail.utils

import android.content.pm.ApplicationInfo
import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
        val installed: Boolean,
        val state: AppInfo.State,
        val sourceSignature: String,
    )

    private val cache = ConcurrentHashMap<String, Entry>()
    private val installedApplications = ConcurrentHashMap<String, ApplicationInfo>()
    private val packageLocks = ConcurrentHashMap<String, Mutex>()

    // loadLabel() loads each app's APK resources while holding the process-wide
    // ResourcesManager lock. Fanning out one coroutine per app (100+) starves the main
    // thread on a configuration change and triggers ANRs, so cap how many load at once.
    private const val LABEL_LOAD_CONCURRENCY = 4
    private val labelLoadPermits = Semaphore(LABEL_LOAD_CONCURRENCY)

    // A cache-ready gate that never completes must not hang prefetch indefinitely.
    private const val CACHE_READY_TIMEOUT_MS = 5_000L
    private val database by lazy {
        Room
            .databaseBuilder(HailApp.app, AppMetadataDatabase::class.java, "app_metadata.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
            .build()
    }

    internal fun database(): AppMetadataDatabase = database

    private val scope = HailApp.app.applicationScope
    private val revisionState = MutableStateFlow(0L)
    val revision: StateFlow<Long> = revisionState

    private val cacheReadyDeferred = CompletableDeferred<Unit>()

    /**
     * Completes once Room has been read into the in-memory cache. The UI awaits this before the
     * first render so it paints from cache instead of falling back to PackageManager.
     */
    val cacheReady: Deferred<Unit> = cacheReadyDeferred

    fun get(packageName: String): Entry? = cache[packageName]

    fun cachedApplications(): List<ApplicationInfo> = installedApplications.values.toList()

    private suspend fun loadDatabaseIntoCache() {
        runCatching {
            database.appMetadataDao().loadAll().forEach { entity ->
                cache[entity.packageName] = entity.toEntry()
            }
            revisionState.value++
        }
    }

    fun warmUp(): Job =
        scope.launch {
            loadDatabaseIntoCache()
            cacheReadyDeferred.complete(Unit)
            prefetch(HPackages.getInstalledApplications()).join()
        }

    fun prefetch(applicationInfo: Collection<ApplicationInfo>): Job =
        scope.launch {
            // A timeout means the cache is not ready yet: proceed to load from PackageManager.
            withTimeoutOrNull(CACHE_READY_TIMEOUT_MS) { cacheReady.await() }
            val newPackageMap = applicationInfo.associateBy { it.packageName }
            newPackageMap.forEach { (packageName, info) ->
                installedApplications[packageName] = info
            }
            val toRemove = installedApplications.keys.filterNot { it in newPackageMap.keys }
            toRemove.forEach { installedApplications.remove(it) }
            applicationInfo
                .map { info ->
                    async { labelLoadPermits.withPermit { loadIfStale(info.packageName, info) } }
                }.awaitAll()
            val toRemoveLocks = packageLocks.keys.filterNot { it in newPackageMap.keys }
            toRemoveLocks.forEach { packageLocks.remove(it) }
            val removed = cache.keys.filterNot { it in newPackageMap.keys }
            removed.forEach { packageName ->
                cache.computeIfPresent(packageName) { _, entry ->
                    entry.copy(installed = false, state = AppInfo.State.NOT_FOUND)
                }
            }
            if (removed.isNotEmpty()) revisionState.value++
            persist()
            AppIconCache.prefetch(HailApp.app, applicationInfo)
        }

    /**
     * Lazily hydrate a single app's metadata, called from the grid as rows become visible.
     *
     * Keeps the first paint cheap: rows already served from the Room cache resolve immediately,
     * while rows whose cache entry is missing load their label off the main thread. Callers sit on
     * the main thread, so the work is forced onto IO where loadLabel() and lastModified() belong.
     * The ApplicationInfo is deliberately not taken from the caller: cache-derived lists carry a
     * synthetic instance, and deriving state from it would misreport hidden and disabled apps.
     */
    suspend fun ensureLoaded(packageName: String) {
        withContext(Dispatchers.IO) {
            labelLoadPermits.withPermit { loadIfStale(packageName) }
        }
    }

    suspend fun getInstalledApplicationsCacheFirst(forceRefresh: Boolean = false): List<ApplicationInfo> {
        cachedApplications().takeIf { it.isNotEmpty() && !forceRefresh }?.let { return it }
        val refreshed = withContext(Dispatchers.IO) { HPackages.getInstalledApplications() }
        // State is re-derived asynchronously; callers render UNKNOWN until the revision bumps.
        prefetch(refreshed)
        return refreshed
    }

    fun prefetchPackages(packageNames: Collection<String>): Job =
        scope.launch {
            packageNames
                .map { packageName ->
                    async { labelLoadPermits.withPermit { loadIfStale(packageName) } }
                }.awaitAll()
            persist()
        }

    fun clearAndRebuild(): Job =
        scope.launch {
            cache.clear()
            packageLocks.clear()
            database.appMetadataDao().deleteAll()
            revisionState.value++
            val applicationInfo = HPackages.getInstalledApplications()
            val installedPackages = applicationInfo.mapTo(HashSet()) { it.packageName }
            applicationInfo
                .map { info ->
                    async { labelLoadPermits.withPermit { loadIfStale(info.packageName, info) } }
                }.awaitAll()
            val toRemove = packageLocks.keys.filterNot { it in installedPackages }
            toRemove.forEach { packageLocks.remove(it) }
            persist()
            AppIconCache.prefetch(HailApp.app, applicationInfo)
        }

    /**
     * Re-derives the cached and persisted state of [packageNames] from PackageManager.
     *
     * Runs on [scope] because the binder queries must not block the caller, and writes the delta to
     * Room so the corrected state survives a process death.
     */
    fun invalidateState(packageNames: Collection<String>) {
        scope.launch {
            val changed =
                withContext(Dispatchers.IO) {
                    packageNames.mapNotNull { packageName ->
                        val info = HPackages.getApplicationInfoOrNull(packageName)
                        cache.computeIfPresent(packageName) { _, entry ->
                            entry.copy(
                                installed = installedFrom(info),
                                state = stateFrom(info),
                            )
                        }
                    }
                }
            if (changed.isNotEmpty()) runCatching { database.appMetadataDao().upsertAll(changed.map { it.toEntity() }) }
            revisionState.value++
        }
    }

    /**
     * Targeted refresh for a single package after a package-change event (add/remove/enable/
     * disable/suspend/hide). Updates the installed-apps map and metadata cache, then bumps the
     * revision so the list re-filters. Unlike [invalidateState] this also re-derives the state
     * (including UNINSTALLED) and keeps [installedApplications] in sync.
     */
    fun invalidatePackage(packageName: String): Job =
        scope.launch {
            withContext(Dispatchers.IO) {
                val info = HPackages.getApplicationInfoOrNull(packageName)
                if (info == null) {
                    installedApplications.remove(packageName)
                } else {
                    installedApplications[packageName] = info
                }
                labelLoadPermits.withPermit { loadIfStale(packageName, info) }
                // Persist the single re-derived row: a process death must not resurrect the stale
                // Room entry for a package that was uninstalled or changed while we were stopped.
                cache[packageName]?.let { entry ->
                    runCatching { database.appMetadataDao().upsertAll(listOf(entry.toEntity())) }
                }
            }
        }

    private suspend fun loadIfStale(
        packageName: String,
        knownInfo: ApplicationInfo? = null,
    ) {
        val lock = packageLocks.getOrPut(packageName) { Mutex() }
        lock.withLock {
            val info = knownInfo ?: HPackages.getApplicationInfoOrNull(packageName)
            val packageInfo = HPackages.getUnhiddenPackageInfoOrNull(packageName)
            val sourceSignature =
                listOf(
                    info?.sourceDir,
                    info?.sourceDir?.let { File(it).lastModified() },
                    packageInfo?.lastUpdateTime,
                ).joinToString(":")
            val current = cache[packageName]
            if (info != null && current?.sourceSignature == sourceSignature) {
                val newState = stateFrom(info)
                val newInstalled = installedFrom(info)
                if (current.state != newState || current.installed != newInstalled) {
                    cache[packageName] = current.copy(state = newState, installed = newInstalled)
                    revisionState.value++
                }
                return
            }

            val entry =
                if (info == null) {
                    current?.copy(installed = false, state = AppInfo.State.NOT_FOUND)
                } else {
                    Entry(
                        packageName = packageName,
                        name = info.loadLabel(HailApp.app.packageManager).toString(),
                        isSystemApp = info.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                        firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                        lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                        flags = info.flags,
                        enabled = info.enabled,
                        installed = installedFrom(info),
                        state = stateFrom(info),
                        sourceSignature = sourceSignature,
                    )
                }
            if (entry == null) cache.remove(packageName) else cache[packageName] = entry
            revisionState.value++
        }
    }

    internal fun installedFrom(info: ApplicationInfo?): Boolean = info != null && (info.flags and ApplicationInfo.FLAG_INSTALLED) != 0

    internal fun stateFrom(info: ApplicationInfo?): AppInfo.State {
        if (info == null) return AppInfo.State.NOT_FOUND
        return when {
            info.flags and ApplicationInfo.FLAG_INSTALLED == 0 -> AppInfo.State.UNINSTALLED
            isSuspended(info) -> AppInfo.State.SUSPENDED
            isHidden(info) -> AppInfo.State.HIDDEN
            isStopped(info) -> AppInfo.State.STOPPED
            isDisabled(info) -> AppInfo.State.DISABLED
            else -> AppInfo.State.ENABLED
        }
    }

    // HPackages.isApp* re-query PackageManager for each flag; derive the same checks from a single
    // already-fetched ApplicationInfo so one lookup serves the whole state.
    private fun isSuspended(info: ApplicationInfo): Boolean = HTarget.N && info.flags and ApplicationInfo.FLAG_SUSPENDED == ApplicationInfo.FLAG_SUSPENDED

    // privateFlags is @hide and absent on some ApplicationInfo instances (test doubles, future
    // platforms); a missing field means the hidden bit cannot be read, so treat it as not hidden.
    private fun isHidden(info: ApplicationInfo): Boolean =
        runCatching {
            (ApplicationInfo::class.java.getField("privateFlags").get(info) as Int) and 1 == 1
        }.getOrDefault(false)

    private fun isStopped(info: ApplicationInfo): Boolean = info.flags and ApplicationInfo.FLAG_STOPPED == ApplicationInfo.FLAG_STOPPED

    private fun isDisabled(info: ApplicationInfo): Boolean = info.enabled.not()

    private suspend fun persist() {
        runCatching {
            database.appMetadataDao().upsertAll(cache.values.map { it.toEntity() })
        }
    }

    private fun AppMetadataEntity.toEntry() =
        Entry(
            packageName = packageName,
            name = name,
            isSystemApp = systemApp,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            flags = flags,
            enabled = enabled,
            installed = installed,
            state = AppInfo.State.values()[frozen.coerceIn(0, AppInfo.State.values().size - 1)],
            sourceSignature = sourceSignature,
        )

    private fun Entry.toEntity() =
        AppMetadataEntity(
            packageName = packageName,
            name = name,
            systemApp = isSystemApp,
            firstInstallTime = firstInstallTime,
            lastUpdateTime = lastUpdateTime,
            flags = flags,
            enabled = enabled,
            installed = installed,
            frozen = state.ordinal,
            sourceSignature = sourceSignature,
        )

    private val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE app_metadata ADD COLUMN installed INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS actions (id TEXT NOT NULL, launchPackage TEXT NOT NULL, PRIMARY KEY(id))",
                )
                connection.execSQL(
                    "CREATE TABLE IF NOT EXISTS action_dependencies (actionId TEXT NOT NULL, packageName TEXT NOT NULL, ordering INTEGER NOT NULL, PRIMARY KEY(actionId, packageName), FOREIGN KEY(actionId) REFERENCES actions(id) ON UPDATE NO ACTION ON DELETE CASCADE)",
                )
            }
        }

    private val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("DROP INDEX IF EXISTS index_action_dependencies_actionId")
            }
        }

    private val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE app_metadata ADD COLUMN frozen INTEGER NOT NULL DEFAULT 0")
            }
        }

    private val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override suspend fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "CREATE TABLE app_metadata_new (" +
                        "packageName TEXT NOT NULL, " +
                        "name TEXT NOT NULL, " +
                        "systemApp INTEGER NOT NULL, " +
                        "firstInstallTime INTEGER NOT NULL, " +
                        "lastUpdateTime INTEGER NOT NULL, " +
                        "flags INTEGER NOT NULL, " +
                        "enabled INTEGER NOT NULL, " +
                        "installed INTEGER NOT NULL, " +
                        "frozen INTEGER NOT NULL DEFAULT 0, " +
                        "sourceSignature TEXT NOT NULL, " +
                        "PRIMARY KEY(packageName))",
                )
                connection.execSQL(
                    "INSERT INTO app_metadata_new " +
                        "SELECT packageName, COALESCE(name, ''), systemApp, firstInstallTime, " +
                        "lastUpdateTime, flags, enabled, installed, frozen, sourceSignature " +
                        "FROM app_metadata",
                )
                connection.execSQL(
                    "UPDATE app_metadata_new SET frozen = CASE frozen WHEN 1 THEN 2 ELSE 1 END",
                )
                connection.execSQL("DROP TABLE app_metadata")
                connection.execSQL("ALTER TABLE app_metadata_new RENAME TO app_metadata")
            }
        }
}
