package com.aistra.hail.utils

import androidx.test.core.app.ApplicationProvider
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupEntry
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.canStartRestore
import com.aistra.hail.backup.categoryOf
import com.aistra.hail.backup.entriesOf
import com.aistra.hail.backup.fileNameOf
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class HBackupSuspendTest {
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        tempDir = File(java.lang.System.getProperty("java.io.tmpdir"), "hbackupsuspend_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        mockkObject(HailData)
    }

    @After
    fun tearDown() {
        unmockkAll()
        tempDir.deleteRecursively()
    }

    @Test
    fun `backup with no options creates manifest-only zip`() =
        runBlocking {
            val outputFile = File(tempDir, "backup_empty.zip")
            val result = backup(ApplicationProvider.getApplicationContext(), outputFile, BackupOptions(apps = false, tags = false, actions = false, settings = false, whitelist = false))

            assertTrue(result.isSuccess)
            assertTrue(outputFile.exists())

            ZipFile(outputFile).use { zip ->
                assertEquals(1, zip.size())
                assertNull(zip.getEntry("apps.json"))
                assertNull(zip.getEntry("tags.json"))
                assertNotNull(zip.getEntry("manifest.json"))
            }
        }

    @Test
    fun `restore rejects archive missing manifest`() =
        runBlocking {
            val zipFile = createZip(File(tempDir, "no_manifest.zip"), mapOf("apps.json" to "[]"))
            val result = restore(ApplicationProvider.getApplicationContext(), zipFile, RestoreOptions())

            assertFalse(result.isSuccess)
            assertNotNull(result.exceptionOrNull())
        }

    @Test
    fun `restore rejects unsupported format`() =
        runBlocking {
            val zipFile =
                createZip(
                    File(tempDir, "bad_format.zip"),
                    mapOf(
                        "manifest.json" to """{"format":"other-format","version":1}""",
                        "apps.json" to "[]",
                    ),
                )
            val result = restore(ApplicationProvider.getApplicationContext(), zipFile, RestoreOptions())

            assertFalse(result.isSuccess)
            assertNotNull(result.exceptionOrNull())
        }

    @Test
    fun `restore rejects newer version`() =
        runBlocking {
            val zipFile =
                createZip(
                    File(tempDir, "new_version.zip"),
                    mapOf(
                        "manifest.json" to """{"format":"hail-backup","version":999}""",
                        "apps.json" to "[]",
                    ),
                )
            val result = restore(ApplicationProvider.getApplicationContext(), zipFile, RestoreOptions())

            assertFalse(result.isSuccess)
            assertNotNull(result.exceptionOrNull())
        }

    @Test
    fun `canStartBackup returns true when any option enabled`() {
        assertTrue(canStartBackup(BackupOptions(apps = true)))
        assertTrue(canStartBackup(BackupOptions(tags = true)))
        assertTrue(canStartBackup(BackupOptions(actions = true)))
        assertTrue(canStartBackup(BackupOptions(settings = true)))
        assertTrue(canStartBackup(BackupOptions(whitelist = true)))
    }

    @Test
    fun `canStartBackup returns false when no options enabled`() {
        assertFalse(canStartBackup(BackupOptions(apps = false, tags = false, actions = false, settings = false, whitelist = false)))
    }

    @Test
    fun `canStartRestore returns false for Empty state`() {
        assertFalse(canStartRestore(com.aistra.hail.backup.RestoreState.Empty))
    }

    @Test
    fun `canStartRestore returns true for Loaded with present entries`() {
        val state =
            com.aistra.hail.backup.RestoreState.Loaded(
                file = File("/tmp/test.zip"),
                displayName = "test.zip",
                sizeBytes = 0,
                entries = listOf(BackupEntry(BackupCategory.APPS, "apps.json", true, 100)),
                options = RestoreOptions(),
                phase = com.aistra.hail.backup.Phase.Idle,
                message = null,
            )
        assertTrue(canStartRestore(state))
    }

    @Test
    fun `categoryOf maps entry names`() {
        assertEquals(BackupCategory.APPS, categoryOf("apps.json"))
        assertEquals(BackupCategory.TAGS, categoryOf("tags.json"))
        assertEquals(BackupCategory.ACTIONS, categoryOf("actions.json"))
        assertEquals(BackupCategory.SETTINGS, categoryOf("settings.bin"))
        assertEquals(BackupCategory.WHITELIST, categoryOf("whitelist.json"))
        assertNull(categoryOf("manifest.json"))
        assertNull(categoryOf("unknown.txt"))
    }

    @Test
    fun `fileNameOf maps categories`() {
        assertEquals("apps.json", fileNameOf(BackupCategory.APPS))
        assertEquals("tags.json", fileNameOf(BackupCategory.TAGS))
        assertEquals("actions.json", fileNameOf(BackupCategory.ACTIONS))
        assertEquals("settings.bin", fileNameOf(BackupCategory.SETTINGS))
        assertEquals("whitelist.json", fileNameOf(BackupCategory.WHITELIST))
    }

    private fun createZip(
        zipFile: File,
        entries: Map<String, String>,
    ): File {
        java.util.zip.ZipOutputStream(java.io.FileOutputStream(zipFile)).use { zos ->
            entries.forEach { name, content ->
                zos.putNextEntry(java.util.zip.ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
