package com.aistra.hail.utils

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
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class HBackupTest {

    // Everything this class writes lives under here, and the rule removes the tree after each
    // test whether it passed or failed, so a red test cannot leave anything behind.
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private lateinit var mockApp: HailApp

    @Before
    fun setUp() {
        mockApp = mockk<HailApp>(relaxed = true)
        val filesDir = temporaryFolder.newFolder("files")
        every { mockApp.filesDir } returns filesDir
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

    /**
     * Points the default SharedPreferences at a mock holding [values] and returns the editor
     * that restore() writes through, so the overload used for each key can be verified.
     */
    private fun mockPreferences(vararg values: Pair<String, Any?>): SharedPreferences.Editor {
        val editor = mockk<SharedPreferences.Editor>(relaxed = true)
        val sharedPreferences = mockk<SharedPreferences>(relaxed = true)
        val stored: MutableMap<String, Any?> = mutableMapOf()
        values.forEach { (key, value) -> stored[key] = value }
        every { sharedPreferences.all } returns stored
        every { sharedPreferences.edit() } returns editor
        every { mockApp.packageName } returns "com.aistra.hail"
        every { mockApp.getSharedPreferences(any(), any()) } returns sharedPreferences
        return editor
    }

    private fun readEntry(zipFile: File, entryName: String): String {
        ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            while (entry != null) {
                if (entry.name == entryName) return String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
        throw AssertionError("no entry named '$entryName' in $zipFile")
    }

    /** Builds a backup by hand, so the parse path of a restore is not decided by the writer. */
    private fun zipWithSettings(content: String): File {
        val zipFile = temporaryFolder.newFile("hand-written-settings.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            zipOutputStream.putNextEntry(ZipEntry("settings.json"))
            zipOutputStream.write(content.toByteArray(StandardCharsets.UTF_8))
            zipOutputStream.closeEntry()
        }
        return zipFile
    }

    @Test
    fun `backup writes apps json when apps enabled`() = runTest {
        val appInfo1 = AppInfo("com.example.app1", pinned = false, whitelisted = false, tagIdList = mutableListOf(0))
        val appInfo2 = AppInfo("com.example.app2", pinned = true, whitelisted = true, tagIdList = mutableListOf(1))
        every { HailData.checkedList } returns mutableListOf(appInfo1, appInfo2)

        val outputFile = temporaryFolder.newFile("backup-apps.zip")
        val options = HBackup.BackupOptions(apps = true, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        if (result.isFailure) {
            result.exceptionOrNull()?.printStackTrace()
        }
        assertTrue(result.isSuccess)

        // Verify ZIP contains apps.json with correct package names
        val jsonArray = JSONArray(readEntry(outputFile, "apps.json"))
        assertEquals(2, jsonArray.length())
        assertEquals("com.example.app1", jsonArray.getString(0))
        assertEquals("com.example.app2", jsonArray.getString(1))
    }

    @Test
    fun `backup writes whitelist json when whitelist enabled`() = runTest {
        val appInfo1 = AppInfo("com.example.app1", pinned = false, whitelisted = false, tagIdList = mutableListOf(0))
        val appInfo2 = AppInfo("com.example.app2", pinned = true, whitelisted = true, tagIdList = mutableListOf(1))
        val appInfo3 = AppInfo("com.example.app3", pinned = false, whitelisted = true, tagIdList = mutableListOf(0))
        every { HailData.checkedList } returns mutableListOf(appInfo1, appInfo2, appInfo3)

        val outputFile = temporaryFolder.newFile("backup-whitelist.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = true, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)

        // Verify ZIP contains whitelist.json with only whitelisted packages
        val jsonArray = JSONArray(readEntry(outputFile, "whitelist.json"))
        assertEquals(2, jsonArray.length())
        assertEquals("com.example.app2", jsonArray.getString(0))
        assertEquals("com.example.app3", jsonArray.getString(1))
    }

    @Test
    fun `restore reads apps json and adds to checked list`() = runTest {
        // Create a temp ZIP with apps.json
        val zipFile = temporaryFolder.newFile("restore-apps.zip")
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
        val zipFile = temporaryFolder.newFile("restore-missing.zip")
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

        val outputFile = temporaryFolder.newFile("backup-empty.zip")
        val options = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
    }

    @Test
    fun `backup writes a file name without a parent directory`() = runTest {
        // A bare file name has no parent, so there is no directory to create. It resolves
        // against the working directory of the test run, which the rule cannot clean up, so
        // the delete is in a finally instead of after the assertions.
        val outputFile = File("backup-test-${System.currentTimeMillis()}.zip")
        try {
            val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

            val result = HBackup.backup(HailApp.app, outputFile, options)

            assertTrue(result.isSuccess)
            assertTrue(outputFile.exists())
        } finally {
            outputFile.delete()
        }
    }

    @Test
    fun `backup creates a missing parent directory`() = runTest {
        val parent = File(temporaryFolder.root, "missing-parent")
        val outputFile = File(parent, "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
        assertTrue(parent.isDirectory)
        assertTrue(outputFile.exists())
    }

    @Test
    fun `backup fails when the parent path is a file`() = runTest {
        val parent = temporaryFolder.newFile("parent-is-a-file")
        val outputFile = File(parent, "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `backup fails when the parent directory cannot be created`() = runTest {
        // A file in the middle of the path makes mkdirs() fail
        val blocker = temporaryFolder.newFile("blocker-is-a-file")
        val outputFile = File(File(blocker, "hail-test-dir"), "backup-test.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun `backup proceeds when the parent directory appears between the check and mkdirs`() = runTest {
        // The trailing isDirectory re-check and nothing else covers this: another process
        // creates the directory after the first check reported false, so mkdirs() returns
        // false for a directory that now exists. File, isDirectory and mkdirs are all
        // overridable, so the race is reproducible without threads.
        val realParent = temporaryFolder.newFolder("toctou")
        val racingParent = object : File(realParent.path) {
            private var reportedMissing = false
            override fun isDirectory(): Boolean {
                if (reportedMissing) return true
                reportedMissing = true
                return false
            }

            override fun mkdirs(): Boolean = false
        }
        val outputFile = object : File(realParent, "backup-test.zip") {
            override fun getParentFile(): File = racingParent
        }
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
        assertTrue(outputFile.exists())
    }

    @Test
    fun `backup writes float preferences as decimal strings`() = runTest {
        // A Float written as a JSON number loses its type: numberToString shaves the
        // trailing zero and the decimal point, so 15.0f becomes 15. The string keeps the
        // "." that readSettingsJson needs to recognize the value.
        mockPreferences(HailData.HOME_FONT_SIZE to 15f, HailData.AUTO_FREEZE_DELAY to 10f)
        val outputFile = temporaryFolder.newFile("backup-float-encoding.zip")
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.backup(HailApp.app, outputFile, options)

        assertTrue(result.isSuccess)
        val jsonObject = JSONObject(readEntry(outputFile, "settings.json"))
        assertEquals("15.0", jsonObject.getString(HailData.HOME_FONT_SIZE))
        assertEquals("10.0", jsonObject.getString(HailData.AUTO_FREEZE_DELAY))
    }

    @Test
    fun `backup and restore round trip whole number float preferences`() = runTest {
        // Both sliders can only produce whole numbers: home_font_size_f is 11f..16f with 4
        // steps and auto_freeze_delay_f is 0f..30f with 29 steps, so a fractional value here
        // would be a value no user can set.
        val editor = mockPreferences(HailData.HOME_FONT_SIZE to 15f, HailData.AUTO_FREEZE_DELAY to 10f)
        val outputFile = temporaryFolder.newFile("backup-round-trip.zip")
        val backupOptions = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = true)

        val backupResult = HBackup.backup(HailApp.app, outputFile, backupOptions)

        assertTrue(backupResult.isSuccess)
        val jsonObject = JSONObject(readEntry(outputFile, "settings.json"))
        assertEquals("15.0", jsonObject.getString(HailData.HOME_FONT_SIZE))
        assertEquals("10.0", jsonObject.getString(HailData.AUTO_FREEZE_DELAY))

        val restoreOptions = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val restoreResult = HBackup.restore(HailApp.app, outputFile, restoreOptions)

        assertTrue(restoreResult.isSuccess)
        // SharedPreferences.getFloat is "v is Float ? v : defValue", so an Int stored under a
        // Float key reads as absent and the user silently gets the default back.
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 15f) }
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 10f) }
        verify(exactly = 0) { editor.putInt(any(), any()) }
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    @Test
    fun `restore prefers the recorded preference type over the json number type`() = runTest {
        // A hand written settings.json in the shape an older build wrote: whole numbers with
        // no type marker at all. org.json parses 15 as an Integer, so without consulting the
        // type recorded in SharedPreferences the is Int arm claims it and the value is
        // stored as an Int under a Float key.
        val editor = mockPreferences(HailData.HOME_FONT_SIZE to 15f, HailData.AUTO_FREEZE_DELAY to 10f)
        val zipFile = zipWithSettings(
            """{"${HailData.HOME_FONT_SIZE}":15,"${HailData.AUTO_FREEZE_DELAY}":10}"""
        )
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 15f) }
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 10f) }
        verify(exactly = 0) { editor.putInt(any(), any()) }
    }

    @Test
    fun `restore handles a json number wider than 64 bits`() = runTest {
        // org.json returns a BigInteger for an integer wider than 64 bits, which matches no
        // Int or Long arm. Nothing is recorded for this key, so the JSON value alone decides,
        // and a Long is the widest type SharedPreferences can hold.
        val huge = "12345678901234567890123"
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":$huge}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putLong(HailData.HOME_FONT_SIZE, BigInteger(huge).toLong()) }
    }

    @Test
    fun `restore falls back to the json value type for keys that are not recorded`() = runTest {
        // A restore onto an install where the preferences are not set yet: there is no
        // recorded type to consult, so every other type still has to come back as itself.
        val editor = mockPreferences()
        val zipFile = zipWithSettings(
            """{"a_string":"hello","an_int":7,"a_long":4294967296,"a_bool":true,"a_set":["x","y"]}"""
        )
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putString("a_string", "hello") }
        verify { editor.putInt("an_int", 7) }
        verify { editor.putLong("a_long", 4294967296L) }
        verify { editor.putBoolean("a_bool", true) }
        verify { editor.putStringSet("a_set", setOf("x", "y")) }
    }
}
