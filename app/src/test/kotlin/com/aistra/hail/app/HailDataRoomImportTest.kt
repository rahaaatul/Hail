package com.aistra.hail.app

import android.content.Context
import androidx.room3.Room
import com.aistra.hail.HailApp
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.AppMetadataDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class HailDataRoomImportTest {
    private lateinit var context: Context
    private lateinit var db: AppMetadataDatabase
    private lateinit var dir: File

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.getApplication() as HailApp
        HailApp.setAppForTest(app)
        context = app
        db =
            Room
                .inMemoryDatabaseBuilder(context, AppMetadataDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        AppMetaCache.useDatabaseForTest(db)
        HailDataStore.resetForTest()
        dir = File(context.filesDir, "v1")
        dir.mkdirs()
    }

    @After
    fun tearDown() {
        AppMetaCache.useDatabaseForTest(null)
        db.close()
    }

    @Test
    fun `imports apps and tags from json into room once`() {
        writeJson(
            apps = """[{"package":"com.example.one","pinned":true,"whitelisted":true,"tags":[1,3]}]""",
            tags = """[{"tag":"Work","id":1},{"tag":"Play","id":3}]""",
        )

        val checked = HailData.checkedList
        assertEquals(1, checked.size)
        assertEquals("com.example.one", checked[0].packageName)
        assertTrue(checked[0].pinned)
        assertTrue(checked[0].whitelisted)
        assertEquals(listOf(1, 3), checked[0].tagIdList)

        assertEquals(listOf("Work" to 1, "Play" to 3), HailData.tags)

        val apps = db.checkedAppDao().loadAll()
        assertEquals(1, apps.size)
        assertEquals("1,3", apps[0].tagIds)
        assertEquals(listOf(1, 3), db.tagDao().loadAll().map { it.id })
        assertEquals(1, db.stateFlagDao().get("json_imported"))
    }

    @Test
    fun `does not re-import json once the marker is set`() {
        writeJson(
            apps = """[{"package":"com.example.one","pinned":true,"whitelisted":true,"tags":[1,3]}]""",
            tags = """[{"tag":"Work","id":1},{"tag":"Play","id":3}]""",
        )
        assertEquals(1, HailData.checkedList.size)

        // Change the JSON, then drop only the in-memory snapshot: the marker must prevent re-import.
        writeJsonFiles(
            apps = """[{"package":"com.example.two","pinned":false,"whitelisted":false,"tags":[9]}]""",
            tags = """[{"tag":"Other","id":9}]""",
        )
        HailDataStore.resetSnapshotForTest()

        assertEquals("com.example.one", HailData.checkedList.single().packageName)
        assertEquals(
            "1,3",
            db
                .checkedAppDao()
                .loadAll()
                .single()
                .tagIds,
        )
    }

    @Test
    fun `deduplicates apps and tags during the import`() {
        writeJson(
            apps =
                """[
                {"package":"com.example.one","pinned":false,"whitelisted":false,"tags":[1]},
                {"package":"com.example.one","pinned":true,"whitelisted":false,"tags":[1]}
                ]""",
            tags = """[{"tag":"Work","id":1},{"tag":"Work","id":1}]""",
        )

        assertEquals(1, HailData.checkedList.size)
        assertEquals(1, HailData.tags.size)
        assertEquals(1, db.checkedAppDao().loadAll().size)
        assertEquals(1, db.tagDao().loadAll().size)
    }

    @Test
    fun `missing json files fall back to the default tag and no apps`() {
        File(dir, "apps.json").delete()
        File(dir, "tags.json").delete()

        assertTrue(HailData.checkedList.isEmpty())
        assertEquals(listOf("Default" to 0), HailData.tags)
    }

    @Test
    fun `a malformed tag entry falls back to the default tag`() {
        writeJson(
            apps = "[]",
            tags = """[{"tag":"Work","id":1},{"tag":"Broken"}]""",
        )

        // The whole tags.json parse is guarded: a bad element must not crash the snapshot.
        assertEquals(listOf("Default" to 0), HailData.tags)
        assertTrue(HailData.checkedList.isEmpty())
        assertEquals(listOf("Default"), db.tagDao().loadAll().map { it.name })
    }

    @Test
    fun `a malformed app entry is skipped`() {
        writeJson(
            apps = """[{"package":"com.example.good","pinned":true,"whitelisted":false,"tags":[1]},{"pinned":true}]""",
            tags = """[{"tag":"Work","id":1}]""",
        )

        assertEquals(1, HailData.checkedList.size)
        assertEquals("com.example.good", HailData.checkedList.single().packageName)
        assertEquals(listOf("com.example.good"), db.checkedAppDao().loadAll().map { it.packageName })
        assertEquals(listOf("Work" to 1), HailData.tags)
    }

    private fun writeJson(
        apps: String,
        tags: String,
    ) {
        writeJsonFiles(apps, tags)
        HailDataStore.resetForTest()
    }

    private fun writeJsonFiles(
        apps: String,
        tags: String,
    ) {
        File(dir, "apps.json").writeText(apps)
        File(dir, "tags.json").writeText(tags)
    }
}
