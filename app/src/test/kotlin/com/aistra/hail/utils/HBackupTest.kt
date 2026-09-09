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

    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
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
        every { HailData.saveApps() } returns Unit
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
                assertEquals("com.example.app1", jsonArray.getJSONObject(0).getString("packageName"))
                assertEquals("com.example.app2", jsonArray.getJSONObject(1).getString("packageName"))
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
                assertEquals("com.example.app2", jsonArray.getJSONObject(0).getString("packageName"))
                assertEquals("com.example.app3", jsonArray.getJSONObject(1).getString("packageName"))
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
        val jsonString = """[{"packageName":"com.example.app1"},{"packageName":"com.example.app2"}]"""
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

        val mockSp = mockk<SharedPreferences>(relaxed = true)
        every { mockSp.all } returns mapOf()

        val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
        val options = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
    }
}