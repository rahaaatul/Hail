package com.aistra.hail.utils

import android.content.Context
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
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
class HBackupRoundtripTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        val app = RuntimeEnvironment.application as HailApp
        HailApp.setAppForTest(app)
        context = app
        // Seed empty app/tag files so the lazy collections initialize without touching
        // app resources (unavailable under Robolectric), then reset in-memory state.
        val dir = File(context.filesDir.path, "v1")
        dir.mkdirs()
        File(dir, "apps.json").writeText("[]")
        File(dir, "tags.json").writeText("[]")
        HailData.checkedList.clear()
        HailData.tags.clear()
    }

    @Test
    fun `backup and restore roundtrip preserves apps and tags`() =
        runTest {
            HailData.checkedList.add(AppInfo("com.example.one", tagIdList = mutableListOf(1)))
            HailData.checkedList.add(
                AppInfo(
                    packageName = "com.example.two",
                    pinned = true,
                    whitelisted = true,
                    tagIdList = mutableListOf(3),
                ),
            )
            HailData.tags.add("Work" to 1)
            HailData.tags.add("Personal" to 3)

            val outputFile = File(context.cacheDir, "roundtrip-${System.currentTimeMillis()}.hail")
            assertTrue(
                backup(
                    context,
                    outputFile,
                    BackupOptions(apps = true, tags = true, actions = true, settings = true, whitelist = true),
                ).isSuccess,
            )
            assertTrue(outputFile.exists())

            HailData.checkedList.clear()
            HailData.tags.clear()

            assertTrue(
                restore(
                    context,
                    outputFile,
                    RestoreOptions(apps = true, tags = true, actions = true, settings = true, whitelist = true),
                ).isSuccess,
            )

            assertEquals(2, HailData.checkedList.size)
            assertTrue(HailData.checkedList.any { it.packageName == "com.example.one" })
            val restoredTwo = HailData.checkedList.first { it.packageName == "com.example.two" }
            assertTrue(restoredTwo.pinned)
            assertTrue(restoredTwo.whitelisted)
            assertEquals(2, HailData.tags.size)
            assertTrue(HailData.tags.any { it.first == "Work" && it.second == 1 })
            assertTrue(HailData.tags.any { it.first == "Personal" && it.second == 3 })

            outputFile.delete()
        }

    @Test
    fun `restore fails on archive referencing an unknown tag id`() {
        runBlocking {
            HailData.checkedList.add(AppInfo("com.example.one", tagIdList = mutableListOf(99)))
            HailData.tags.add("Work" to 1)

            val outputFile = File(context.cacheDir, "badtags-${System.currentTimeMillis()}.hail")
            assertTrue(
                backup(
                    context,
                    outputFile,
                    BackupOptions(apps = true, tags = true, actions = true, settings = true, whitelist = true),
                ).isSuccess,
            )

            HailData.checkedList.clear()
            HailData.tags.clear()

            assertFalse(restore(context, outputFile, RestoreOptions(apps = true, tags = true)).isSuccess)

            outputFile.delete()
        }
    }
}
