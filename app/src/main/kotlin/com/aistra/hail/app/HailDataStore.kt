package com.aistra.hail.app

import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.CheckedAppEntity
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.StateFlagEntity
import com.aistra.hail.utils.TagEntity
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Room-backed store for the checked apps and tags that used to live in `apps.json` / `tags.json`.
 *
 * Room is canonical. The JSON files are read exactly once, guarded by a `state_flags` marker, and
 * then left on disk untouched as a read-only fallback. The public API keeps returning the same
 * mutable list instances so existing callers can mutate and re-read them without change.
 */
internal object HailDataStore {
    private const val JSON_IMPORTED_KEY = "json_imported"
    private const val JSON_IMPORTED = 1
    private const val JSON_NOT_IMPORTED = 0
    private const val DEFAULT_TAG_NAME = "Default"
    private const val DEFAULT_TAG_ID = 0

    private val dir get() = "${app.filesDir.path}/v1"
    private val appsPath get() = "$dir/apps.json"
    private val tagsPath get() = "$dir/tags.json"

    private val scope =
        CoroutineScope(
            SupervisorJob() +
                Dispatchers.IO +
                // A write is best-effort: log and drop a failed background save instead of letting
                // it escape as an uncaught exception (which would also fail unrelated runTest tests).
                CoroutineExceptionHandler { _, throwable -> HLog.e(throwable) },
        )
    private val writeMutex = Mutex()

    /** The most recent fire-and-forget save; [flush] joins it so the write is durable on return. */
    @Volatile
    private var pendingSave: Job? = null

    /**
     * The mutable lists handed out to callers. They are copy-on-write so the UI can mutate them
     * while [writeSnapshot] walks them on a background write without a concurrent modification.
     */
    private class Snapshot(
        val apps: MutableList<AppInfo> = CopyOnWriteArrayList(),
        val tags: MutableList<Pair<String, Int>> = CopyOnWriteArrayList(),
    )

    @Volatile
    private var snapshot: Snapshot? = null

    val checkedList: MutableList<AppInfo> get() = snapshot().apps

    val tags: MutableList<Pair<String, Int>> get() = snapshot().tags

    /** Loads the snapshot off the main thread; safe to call before the first access. */
    fun warmUp() {
        scope.launch { snapshot() }
    }

    fun save() {
        pendingSave = scope.launch { writeSnapshot() }
    }

    /** Writes both snapshots synchronously; callers that must know the rows are durable use this. */
    suspend fun flush() {
        withContext(Dispatchers.IO) {
            // Saves queue on writeMutex in launch order, so joining the newest also waits for any
            // earlier save that is still ahead of it.
            pendingSave?.join()
            writeSnapshot()
        }
    }

    internal fun resetForTest() {
        resetSnapshotForTest()
        val db = AppMetaCache.database()
        db.checkedAppDao().deleteAll()
        db.tagDao().deleteAll()
        db.stateFlagDao().set(StateFlagEntity(JSON_IMPORTED_KEY, JSON_NOT_IMPORTED))
    }

    /** Drops only the in-memory snapshot, keeping the Room rows and the import marker. */
    internal fun resetSnapshotForTest() {
        synchronized(this) { snapshot = null }
    }

    private fun snapshot(): Snapshot =
        snapshot ?: synchronized(this) {
            snapshot ?: runBlocking {
                withContext(Dispatchers.IO) {
                    importFromJsonOnce()
                    val db = AppMetaCache.database()
                    Snapshot(
                        apps = CopyOnWriteArrayList(db.checkedAppDao().loadAll().map { it.toAppInfo() }),
                        tags = CopyOnWriteArrayList(db.tagDao().loadAll().map { it.name to it.id }),
                    )
                }
            }.also { snapshot = it }
        }

    private suspend fun writeSnapshot() {
        writeMutex.withLock {
            val current = snapshot()
            // Copy first: the UI mutates the live copy-on-write lists while this write is running.
            val apps = current.apps.toList().map { it.toEntity() }
            val tags = current.tags.toList().map { TagEntity(id = it.second, name = it.first) }
            val db = AppMetaCache.database()
            db.checkedAppDao().replaceAll(apps)
            db.tagDao().replaceAll(tags)
        }
    }

    private fun importFromJsonOnce() {
        val db = AppMetaCache.database()
        if (db.stateFlagDao().get(JSON_IMPORTED_KEY) == JSON_IMPORTED) return
        db.tagDao().replaceAll(readTagsFromJson())
        db.checkedAppDao().replaceAll(readAppsFromJson())
        db.stateFlagDao().set(StateFlagEntity(JSON_IMPORTED_KEY, JSON_IMPORTED))
    }

    private fun readTagsFromJson(): List<TagEntity> {
        val raw = HFiles.read(tagsPath)?.takeIf { it.isNotBlank() }
        val json = raw?.let { runCatching { JSONArray(it) }.getOrNull() }
        val tags =
            json?.let { source ->
                runCatching {
                    val parsed = mutableListOf<TagEntity>()
                    for (i in 0 until source.length()) {
                        with(source.getJSONObject(i)) {
                            parsed.add(TagEntity(id = getInt(HailData.KEY_ID), name = getString(HailData.KEY_TAG)))
                        }
                    }
                    parsed
                }.getOrNull()
            }
        // A single malformed element must not take the whole tag list down; fall back to the
        // default tag exactly like a missing or unreadable file does.
        return tags?.distinctBy { it.id }
            ?: listOf(TagEntity(id = DEFAULT_TAG_ID, name = DEFAULT_TAG_NAME))
    }

    private fun readAppsFromJson(): List<CheckedAppEntity> {
        val raw = HFiles.read(appsPath)?.takeIf { it.isNotBlank() }
        val json = raw?.let { runCatching { JSONArray(it) }.getOrNull() }
        val apps =
            json?.let { source ->
                val parsed = mutableListOf<CheckedAppEntity>()
                for (i in 0 until source.length()) {
                    // Skip a malformed entry and keep the valid ones; one bad row must not lose the list.
                    val entity = runCatching { source.getJSONObject(i).toCheckedAppEntity() }.getOrNull()
                    if (entity != null) parsed.add(entity)
                }
                parsed
            }
        return apps?.distinctBy { it.packageName } ?: emptyList()
    }
}

private fun JSONObject.toCheckedAppEntity() =
    CheckedAppEntity(
        packageName = getString(HailData.KEY_PACKAGE),
        pinned = optBoolean(HailData.KEY_PINNED),
        whitelisted = optBoolean(HailData.KEY_WHITELISTED),
        tagIds =
            optJSONArray(HailData.KEY_TAGS)?.let { list ->
                MutableList(list.length()) { index -> list.getInt(index).toString() }.joinToString(",")
            } ?: optInt(HailData.KEY_TAG).toString(),
    )

private fun CheckedAppEntity.toAppInfo() =
    AppInfo(
        packageName = packageName,
        pinned = pinned,
        whitelisted = whitelisted,
        tagIdList = tagIds.split(",").mapNotNull { it.toIntOrNull() }.toMutableList(),
    )

private fun AppInfo.toEntity() =
    CheckedAppEntity(
        packageName = packageName,
        pinned = pinned,
        whitelisted = whitelisted,
        tagIds = tagIdList.joinToString(","),
    )
