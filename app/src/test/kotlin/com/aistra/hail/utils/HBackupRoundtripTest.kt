package com.aistra.hail.utils

import android.content.Context
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.app.SettingsDataStore
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import com.aistra.hail.backup.SettingsBackup
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
import java.io.FileOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

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

    @Test
    fun `settings are backed up`() {
        runBlocking {
            SettingsDataStore.updateSettings {
                setSortBy("install")
                setFilterUserApps(false)
                setHomeFontSize(18f)
                setWorkingMode("shizuku_stop")
                setGrayscaleIcon(true)
            }

            val outputFile = File(context.cacheDir, "backup-test-${System.currentTimeMillis()}.hail")
            assertTrue(
                backup(
                    context,
                    outputFile,
                    BackupOptions(settings = true),
                ).isSuccess,
            )

            val zipFile = ZipFile(outputFile)
            val entry = zipFile.getEntry("settings.bin")
            require(entry != null) { "settings.bin not found in archive" }
            val input: InputStream = zipFile.getInputStream(entry)
            val bytes = input.readBytes()
            val settings = SettingsBackup.parseFrom(bytes)

            assertTrue(settings.hasSortBy())
            assertEquals("install", settings.sortBy)
            assertTrue(settings.hasFilterUserApps())
            assertFalse(settings.filterUserApps)
            assertTrue(settings.hasHomeFontSize())
            assertEquals(18f, settings.homeFontSize, 0.001f)
            assertTrue(settings.hasWorkingMode())
            assertEquals("shizuku_stop", settings.workingMode)
            assertTrue(settings.hasGrayscaleIcon())
            assertTrue(settings.grayscaleIcon)

            zipFile.close()
            outputFile.delete()
        }
    }

    @Test
    fun `settings are restored`() {
        runBlocking {
            // Pre-seed DataStore with different values, then back them up
            SettingsDataStore.updateSettings {
                setSortBy("name")
                setFilterUserApps(true)
                setHomeFontSize(20f)
                setWorkingMode("default_mode")
                setGrayscaleIcon(false)
            }

            val outputFile = File(context.cacheDir, "restore-test-${System.currentTimeMillis()}.hail")
            assertTrue(
                backup(
                    context,
                    outputFile,
                    BackupOptions(apps = true, tags = true, actions = true, settings = true, whitelist = true),
                ).isSuccess,
            )

            // Mutate DataStore to different values after backup
            SettingsDataStore.updateSettings {
                setSortBy("mutated")
                setFilterUserApps(false)
                setHomeFontSize(99f)
                setWorkingMode("mutated_mode")
                setGrayscaleIcon(true)
            }

            assertTrue(
                restore(
                    context,
                    outputFile,
                    RestoreOptions(settings = true),
                ).isSuccess,
            )

            // Should match backed-up values, not mutated ones
            val restored = SettingsDataStore.readSettings()
            assertEquals("name", restored.sortBy)
            assertTrue(restored.filterUserApps)
            assertEquals(20f, restored.homeFontSize, 0.001f)
            assertEquals("default_mode", restored.workingMode)
            assertFalse(restored.grayscaleIcon)

            outputFile.delete()
        }
    }

    @Test
    fun `restore preserves non-backed-up fields`() {
        runBlocking {
            // Pre-seed DataStore with non-default values for both fields
            SettingsDataStore.updateSettings {
                setSortBy("preserved")
                setFilterUserApps(true)
            }

            // Manually craft a backup zip whose settings.bin only has sortBy present
            // (filterUserApps absent → hasFilterUserApps() = false → not applied on restore)
            val outputFile = File(context.cacheDir, "preserve-test-${System.currentTimeMillis()}.hail")
            val settings = SettingsBackup.newBuilder().setSortBy("install").build()
            val byteArray = settings.toByteArray()

            ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
                zos.putNextEntry(ZipEntry("manifest.json"))
                zos.write("{\"format\":\"hail-backup\",\"version\":1}".toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("apps.json"))
                zos.write("[]".toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("tags.json"))
                zos.write("[]".toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("actions.json"))
                zos.write("[]".toByteArray(StandardCharsets.UTF_8))
                zos.closeEntry()
                zos.putNextEntry(ZipEntry("settings.bin"))
                zos.write(byteArray)
                zos.closeEntry()
            }

            assertTrue(
                restore(
                    context,
                    outputFile,
                    RestoreOptions(settings = true),
                ).isSuccess,
            )

            // sortBy was present in backup → applied; filterUserApps was absent → preserved
            val restored = SettingsDataStore.readSettings()
            assertEquals("install", restored.sortBy)
            assertTrue(restored.filterUserApps)

            outputFile.delete()
        }
    }
}
