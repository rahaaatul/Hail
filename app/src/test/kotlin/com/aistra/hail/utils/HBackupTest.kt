package com.aistra.hail.utils

import android.content.Context
import android.content.SharedPreferences
import com.aistra.hail.HailApp
import com.aistra.hail.app.HailData
import com.aistra.hail.app.AppInfo
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class HBackupTest {

    private lateinit var mockApp: HailApp

    @Before
    fun setUp() {
        mockApp = mockk<HailApp>(relaxed = true)
        val tmpDir = File(System.getProperty("java.io.tmpdir"), "hail-test-${System.currentTimeMillis()}")
        tmpDir.mkdirs()
        every { mockApp.filesDir } returns tmpDir
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        mockkObject(HailData)
        mockkObject(ActionsRepository)
        coEvery { ActionsRepository.loadAll() } returns emptyList()
        coEvery { ActionsRepository.save(any(), any(), any()) } returns LaunchAction("test-id", "test-pkg", emptyList())
        every { HailData.isChecked(any()) } returns false
        every { HailData.addCheckedApp(any(), any(), any()) } returns Unit
        every { HailData.saveApps() } returns true
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `backup writes apps json when apps enabled`() = runTest {
        val appInfo1 = AppInfo("com.example.app1", pinned = false, whitelisted = false, tagIdList = mutableListOf(0))
        val appInfo2 = AppInfo("com.example.app2", pinned = true, whitelisted = true, tagIdList = mutableListOf(1))
        every { HailData.checkedList } returns mutableListOf(appInfo1, appInfo2)

        val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
        val options = HBackup.BackupOptions(apps = true, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)

        // Verify ZIP contains apps.json with correct package names
        val zipInputStream = ZipInputStream(FileInputStream(outputFile))
        var entry = zipInputStream.nextEntry
        var foundAppsJson = false
        while (entry != null) {
            if (entry.name == "apps.json") {
                foundAppsJson = true
                val jsonString = String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                val jsonArray = JSONArray(jsonString)
                assertEquals(2, jsonArray.length())
                assertEquals("com.example.app1", jsonArray.getString(0))
                assertEquals("com.example.app2", jsonArray.getString(1))
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        assertTrue(foundAppsJson)
    }

    @Test
    fun `backup writes whitelist json when whitelist enabled`() = runTest {
        val appInfo1 = AppInfo("com.example.app1", pinned = false, whitelisted = false, tagIdList = mutableListOf(0))
        val appInfo2 = AppInfo("com.example.app2", pinned = true, whitelisted = true, tagIdList = mutableListOf(1))
        val appInfo3 = AppInfo("com.example.app3", pinned = false, whitelisted = true, tagIdList = mutableListOf(0))
        every { HailData.checkedList } returns mutableListOf(appInfo1, appInfo2, appInfo3)

        val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = true, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)

        // Verify ZIP contains whitelist.json with only whitelisted packages
        val zipInputStream = ZipInputStream(FileInputStream(outputFile))
        var entry = zipInputStream.nextEntry
        var foundWhitelistJson = false
        while (entry != null) {
            if (entry.name == "whitelist.json") {
                foundWhitelistJson = true
                val jsonString = String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                val jsonArray = JSONArray(jsonString)
                assertEquals(2, jsonArray.length())
                assertEquals("com.example.app2", jsonArray.getString(0))
                assertEquals("com.example.app3", jsonArray.getString(1))
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        assertTrue(foundWhitelistJson)
    }

    @Test
    fun `restore reads apps json and adds to checked list`() = runTest {
        // Create a temp ZIP with apps.json
        val zipFile = File(System.getProperty("java.io.tmpdir"), "restore-test-${System.currentTimeMillis()}.zip")
        val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
        val jsonString = """["com.example.app1","com.example.app2"]"""
        zipOutputStream.putNextEntry(ZipEntry("apps.json"))
        zipOutputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
        zipOutputStream.closeEntry()
        zipOutputStream.close()

        val options = HBackup.RestoreOptions(apps = true, whitelist = false, actions = false, settings = false)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 1) { HailData.addCheckedApp("com.example.app1", 0, false) }
        verify(exactly = 1) { HailData.addCheckedApp("com.example.app2", 0, false) }
        verify(exactly = 1) { HailData.saveApps() }
    }

    @Test
    fun `restore ignores missing files and succeeds`() = runTest {
        // Create a temp ZIP with a non-existent file (nonexistent.json)
        val zipFile = File(System.getProperty("java.io.tmpdir"), "restore-test-${System.currentTimeMillis()}.zip")
        val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
        val jsonString = """{"key":"value"}"""
        zipOutputStream.putNextEntry(ZipEntry("nonexistent.json"))
        zipOutputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
        zipOutputStream.closeEntry()
        zipOutputStream.close()

        val options = HBackup.RestoreOptions(apps = true, whitelist = true, actions = true, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `backup handles empty checked list`() = runTest {
        every { HailData.checkedList } returns mutableListOf()

        val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
        val options = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `backup writes a file name without a parent directory`() = runTest {
        // A bare file name has no parent, so there is no directory to create. The file is
        // resolved against the working directory of the test run, hence the cleanup below.
        val outputFile = File("backup-test-${System.currentTimeMillis()}.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
        assertTrue(outputFile.exists())
        outputFile.delete()
    }

    @Test
    fun `backup creates a missing parent directory`() = runTest {
        val parent = File(System.getProperty("java.io.tmpdir"), "hail-test-dir-${System.currentTimeMillis()}")
        val outputFile = File(parent, "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
        assertTrue(parent.isDirectory)
        assertTrue(outputFile.exists())
    }

    @Test
    fun `backup fails when the parent path is a file`() = runTest {
        val parent = File(System.getProperty("java.io.tmpdir"), "hail-test-file-${System.currentTimeMillis()}")
        parent.writeText("not a directory")
        val outputFile = File(parent, "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `backup fails when the parent directory cannot be created`() = runTest {
        // A file in the middle of the path makes mkdirs() fail
        val blocker = File(System.getProperty("java.io.tmpdir"), "hail-test-blocker-${System.currentTimeMillis()}")
        blocker.writeText("not a directory")
        val outputFile = File(File(blocker, "hail-test-dir"), "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `backup and restore round trip float preferences`() = runTest {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        // Whole numbers would be indistinguishable from Int preferences once serialized,
        // so use fractional values to exercise the Float to JSON number mapping
        every { sharedPreferences.all } returns mutableMapOf(
            HailData.HOME_FONT_SIZE to 14.5f,
            HailData.AUTO_FREEZE_DELAY to 7.5f
        )
        every { sharedPreferences.edit() } returns editor
        every { mockApp.packageName } returns "com.aistra.hail"
        every { mockApp.getSharedPreferences(any(), any()) } returns sharedPreferences

        val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
        val backupOptions = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = true)

        val backupResult = HBackup.backup(HailApp.app, outputFile, backupOptions)

        assertTrue(backupResult.isSuccess)

        // Verify ZIP contains settings.json with the font sizes as JSON numbers
        val zipInputStream = ZipInputStream(FileInputStream(outputFile))
        var entry = zipInputStream.nextEntry
        var foundSettingsJson = false
        while (entry != null) {
            if (entry.name == "settings.json") {
                foundSettingsJson = true
                val jsonString = String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                val jsonObject = JSONObject(jsonString)
                assertEquals(14.5, jsonObject.getDouble(HailData.HOME_FONT_SIZE), 0.0)
                assertEquals(7.5, jsonObject.getDouble(HailData.AUTO_FREEZE_DELAY), 0.0)
            }
            zipInputStream.closeEntry()
            entry = zipInputStream.nextEntry
        }
        zipInputStream.close()
        assertTrue(foundSettingsJson)

        val restoreOptions = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val restoreResult = HBackup.restore(HailApp.app, outputFile, restoreOptions)

        assertTrue(restoreResult.isSuccess)
        // JSONObject never yields a Float, the values must still land as Float preferences
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 14.5f) }
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 7.5f) }
    }
}
