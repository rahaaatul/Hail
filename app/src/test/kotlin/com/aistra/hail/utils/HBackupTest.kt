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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
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
        // A skipped preference is reported through HLog.w, and android.util.Log is not on a
        // unit test classpath, so the object has to be stubbed rather than reached through.
        mockkObject(HLog)
        every { HLog.w(any(), any()) } returns 0
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
    fun `restore reads an undeclared number as the type the file itself claims`() = runTest {
        // Nothing is recorded, so the JSON's own type has to decide, and which class org.json
        // hands back depends on the implementation. A fraction is a BigDecimal on the org.json
        // artifact the tests run against and a Double on AOSP, and only a Float preference ever
        // wrote decimal notation, so both are a Float; an integer too wide for a Long is
        // skipped instead, because no preference can hold it.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_fraction":7.5,"a_wide":12345678901234567890123}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat("a_fraction", 7.5f) }
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_wide") }) }
    }

    @Test
    fun `restore falls back to the json value type for keys that are not recorded`() = runTest {
        // A restore onto an install where the preferences are not set yet: there is no
        // recorded type to consult, so every other type still has to come back as itself.
        // "a_float" is the shape writeSettingsJson gives a Float, and the two number strings
        // next to it are spellings this build never writes: only Float.toString() is a fixed
        // point, so "15" and "1e20" stay the Strings they are.
        val editor = mockPreferences()
        val zipFile = zipWithSettings(
            """{"a_string":"hello","an_int":7,"a_long":4294967296,"a_bool":true,"a_set":["x","y"],"a_float":"15.0","a_whole_number_string":"15","an_exponent_string":"1e20"}"""
        )
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putString("a_string", "hello") }
        verify { editor.putInt("an_int", 7) }
        verify { editor.putLong("a_long", 4294967296L) }
        verify { editor.putBoolean("a_bool", true) }
        verify { editor.putStringSet("a_set", setOf("x", "y")) }
        verify { editor.putFloat("a_float", 15f) }
        verify { editor.putString("a_whole_number_string", "15") }
        verify { editor.putString("an_exponent_string", "1e20") }
    }

    @Test
    fun `restore reads an unrecorded float preference from its decimal string`() = runTest {
        // The clean restore the recorded-type lookup cannot serve: a fresh install, or app
        // data cleared, has nothing recorded for home_font_size_f, so the only evidence left
        // is the "15.0" writeSettingsJson produced. Stored as a String it would read back as
        // absent - getFloat is "v is Float ? v : defValue" - and the user would silently get
        // the default font size, which is the whole of #91.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":"15.0","${HailData.AUTO_FREEZE_DELAY}":"10.0"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 15f) }
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 10f) }
        verify(exactly = 0) { editor.putString(any(), any()) }
        verify(exactly = 0) { editor.putInt(any(), any()) }
    }

    @Test
    fun `restore reads an unrecorded float preference from a bare whole number`() = runTest {
        // A backup taken by a build that predates the tag: home_font_size_f is a bare 15, with
        // no type marker and nothing recorded to consult, which is the common upgrade path -
        // back up on the current release, install the fixed build, clear app data, restore.
        // Both sliders can only produce whole numbers, so this is the value the file really
        // holds; taking the JSON's Integer at face value stores it under a Float key as an Int
        // and the user gets the default back, silently and on every later restore too.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":15,"${HailData.AUTO_FREEZE_DELAY}":10}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 15f) }
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 10f) }
        verify(exactly = 0) { editor.putInt(any(), any()) }
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    @Test
    fun `restore repairs a float key an earlier build stored as an int`() = runTest {
        // The state every #91 reporter is in today: an earlier restore put an Int under a Float
        // key, and that Int is now the recorded type. Believing it reproduces the corruption
        // forever, and it is self-perpetuating - the next backup sees an Int in sp.all, writes a
        // bare 15, and the untagged form is what a clean install then stores as an Int again.
        // The recorded type is evidence, not authority: where the file's spelling and the
        // recorded type disagree, the file is the only one of the two that a consistent writer
        // could not have produced by accident.
        val editor = mockPreferences(HailData.HOME_FONT_SIZE to 15, "a_float" to 7)
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":"15.0","a_float":"7.0"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        // The declared key, and an undeclared one whose only evidence is the spelling.
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 15f) }
        verify { editor.putFloat("a_float", 7f) }
        verify(exactly = 0) { editor.putInt(any(), any()) }
    }

    @Test
    fun `restore repairs a float key an earlier build stored as a string`() = runTest {
        // The other shape the damage takes: before the tag was recognised, a Float read on a
        // clean install fell through to the is String arm and was written back as the string
        // "15.0". getFloat then reports the key as absent, so the repair has to see past a
        // recorded String too - but only for a key the app declares a Float, because a genuine
        // String preference is spelled the same way and must not be converted.
        val editor = mockPreferences(HailData.AUTO_FREEZE_DELAY to "10.0", "a_string" to "15.0")
        val zipFile = zipWithSettings("""{"${HailData.AUTO_FREEZE_DELAY}":"10.0","a_string":"15.0"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat(HailData.AUTO_FREEZE_DELAY, 10f) }
        verify { editor.putString("a_string", "15.0") }
    }

    @Test
    fun `restore keeps an undeclared int preference stored as an int`() = runTest {
        // The other half of the boundary the repair has to respect: an Int key recorded as an
        // Int with a bare whole number in the file is not damage, it is the ordinary case, and
        // letting the file's Float spelling override it would rewrite every Int preference the
        // app has. The recorded type is only overruled when the file actually says Float.
        val editor = mockPreferences("an_int" to 15)
        val zipFile = zipWithSettings("""{"an_int":15}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putInt("an_int", 15) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
    }

    @Test
    fun `restore keeps a recorded string preference that reads like a float as a string`() = runTest {
        // The inference is confined to a key with no recorded type. A String preference is
        // recorded as a String, so it keeps its type even when its content happens to be
        // spelled like a float - this is the whole boundary of the rule.
        val editor = mockPreferences("a_string" to "15.0")
        val zipFile = zipWithSettings("""{"a_string":"15.0"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putString("a_string", "15.0") }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
    }

    @Test
    fun `restore skips a value too large for the recorded Int type`() = runTest {
        // toInt() saturates here: it would store Int.MAX_VALUE, a value the file never
        // contained. Leaving the stored value alone is the only outcome that cannot hand the
        // user a number they did not choose.
        val editor = mockPreferences("an_int" to 1)
        val zipFile = zipWithSettings("""{"an_int":5000000000}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putInt(any(), any()) }
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains("an_int") && it.contains("5000000000") }) }
    }

    @Test
    fun `restore skips a value too large for the recorded Long type`() = runTest {
        // The value is a JSON string, so it arrives as the Double toNumberOrNull parses out of
        // it rather than as the BigDecimal a bare 1e30 would give; this is the path the hand
        // written Long bounds live on, and toLong() would saturate it to Long.MAX_VALUE. The
        // bare-number spelling of the same overflow is covered by the undeclared-number test
        // above, which reaches it through a class this classpath does not produce.
        val editor = mockPreferences("a_long" to 1L)
        val zipFile = zipWithSettings("""{"a_long":"1e30"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_long") && it.contains("Long") }) }
    }

    @Test
    fun `restore skips a value that is not representable as the recorded Float type`() = runTest {
        // toFloat() saturates to Infinity, and a Float preference holding Infinity is read
        // back as a font size of Infinity rather than as a font size.
        val editor = mockPreferences(HailData.AUTO_FREEZE_DELAY to 0f)
        val zipFile = zipWithSettings("""{"${HailData.AUTO_FREEZE_DELAY}":1e300}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains(HailData.AUTO_FREEZE_DELAY) && it.contains("Float") }) }
    }

    @Test
    fun `restore skips a fractional value for the recorded Int type instead of truncating it`() = runTest {
        // toInt() truncates 7.5 toward zero and stores 7, which is a different value from the
        // one in the file. A backup this build wrote cannot contain a fraction for an Int key,
        // so the file is the suspect one and the key is left alone.
        val editor = mockPreferences("an_int" to 1)
        val zipFile = zipWithSettings("""{"an_int":7.5}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putInt(any(), any()) }
        verify { HLog.w(any(), match { it.contains("an_int") && it.contains("7.5") }) }
    }

    @Test
    fun `restore keeps a Long at the top of the range instead of skipping it`() = runTest {
        // A Long is already exact: the check that a Long cannot survive is about the 53 bits a
        // Double carries, and routing an integral type through toDouble() applies it where it
        // does not belong. Long.MAX_VALUE rounds up to 2^63, which is one past the end, so the
        // value was rejected as too wide - along with the 1024 Longs below it, whose Double also
        // rounds to 2^63. toLong() on a Long returns it unchanged, so this used to restore and
        // must again: no preference holds anything wider, so nothing here is unrepresentable.
        val editor = mockPreferences("a_long" to 1L)
        val zipFile = zipWithSettings("""{"a_long":9223372036854775807}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putLong("a_long", Long.MAX_VALUE) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
    }

    @Test
    fun `restore keeps a Long just below the top of the range`() = runTest {
        // The value whose Double rounds up to 2^63 and was therefore skipped as though it were
        // out of range. A Long preference can hold it exactly, and it is a value a user can have.
        val editor = mockPreferences("a_long" to 1L)
        val zipFile = zipWithSettings("""{"a_long":9223372036854774784}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putLong("a_long", 9223372036854774784L) }
    }

    @Test
    fun `restore skips a long that reached the reader as a double past 2 to the 53`() = runTest {
        // The other side of the same rule, and the part that has to keep the range check. A
        // Double carries 53 bits, so an integral value this large has already been rounded off
        // somewhere upstream and the digits the file held are gone: 2^53+1 and 2^53 both arrive
        // as the same Double. Writing that back would invent 9007199254740992, a number the
        // file never contained, so it is skipped and logged instead.
        val editor = mockPreferences("a_long" to 1L)
        val zipFile = zipWithSettings("""{"a_long":"9007199254740993"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_long") && it.contains("Long") }) }
    }

    @Test
    fun `restore skips an integer too wide for any preference however org json reports it`() = runTest {
        // A bare integer wider than 64 bits reaches this code in different classes depending on
        // which org.json parsed the file: a BigInteger from the org.json:json artifact on the
        // unit test classpath, and a Double from AOSP's parser, whose Long.parseLong throws on
        // the overflow and whose Double.parseDouble then succeeds. Both name the same file, so
        // both have to give the same answer. Storing the magnitude as a Float - which is what
        // falls out of asking only "is this a finite Float" - writes 1.2345678E22f under a Long
        // key: right order of magnitude, digits nobody chose, and nothing logged.
        val huge = "12345678901234567890123"
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_long":$huge}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_long") && it.contains(huge) }) }
    }

    @Test
    fun `is magnitude too wide refuses both signs at and past 2 to the 53`() {
        // The predicate is internal so this can be asked directly. org.json on the unit
        // test classpath only ever hands isTooWideInteger a BigInteger or a BigDecimal, so
        // restore() cannot reach this predicate with a number a device's parser produced:
        // a literal like 9223372036854774784 arrives here as a Long, and a Long is stored
        // before the check is ever consulted. The declared-Float branch does reach the
        // predicate, but only with a value that is already a Float. So this test is what
        // pins the answer a device gives.
        assertTrue(HBackup.isMagnitudeTooWide(9.007199254740992E15))
        assertTrue(HBackup.isMagnitudeTooWide(-9.007199254740992E15))
        assertTrue(HBackup.isMagnitudeTooWide(1.2345678E22))
        assertTrue(HBackup.isMagnitudeTooWide(-1.2345678E22))

        // The whole reason the predicate measures Float and not Long. Long.MAX is an
        // exact Double and a perfectly good Long, so a restore that kept it as a long
        // loses nothing - but a Float would hold 9223372036854775808 and hand back a
        // different number. Refusing is the only honest answer.
        assertTrue(HBackup.isMagnitudeTooWide(9223372036854774784.0))
        assertTrue(HBackup.isMagnitudeTooWide(-9223372036854774784.0))

        // 2^53 itself is exactly representable, and so is every power of two above it,
        // yet the threshold refuses all of them. That margin is deliberate and it is not
        // free: on the declared-Float path there is no Long arm to fall back to, so
        // 2^53.toFloat() is refused by this and not by any range check. It is the right
        // trade anyway - a guard exists to be conservative, and the keys it can cost
        // anything to are the ones whose slider range is empty, which is none. Pinned so
        // a later reader does not "fix" the >= into a >.
        assertTrue(HBackup.isMagnitudeTooWide(9007199254740992.0))

        // Every value below 2^53 keeps its digits exactly, at either sign.
        assertFalse(HBackup.isMagnitudeTooWide(9.007199254740991E15))
        assertFalse(HBackup.isMagnitudeTooWide(-9.007199254740991E15))
        assertFalse(HBackup.isMagnitudeTooWide(0.0))
        assertFalse(HBackup.isMagnitudeTooWide(14f.toDouble()))
        assertFalse(HBackup.isMagnitudeTooWide(1024.5))
    }

    @Test
    fun `restore accepts a writer-emitted float tag past the magnitude limit`() = runTest {
        // "1.0E20" is a fixed point of Float.toString, so the text and the Float are the
        // same number and no parser rounded anything on the way. The magnitude guard is
        // for a value that arrived through a parser and lost digits; running it here too
        // would refuse a number this build itself wrote, which is the opposite of damage.
        // A declared key that has a slider range is still bounded by the check on the
        // next branch - the guard is dropped, not the bound.
        val editor = mockPreferences("a_float" to 15f)
        val zipFile = zipWithSettings("""{"a_float":"1.0E20"}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat("a_float", 1.0E20f) }
    }

    @Test
    fun `is magnitude too wide refuses a non-finite magnitude`() {
        // A non-finite value is too wide by definition: neither a Long nor a Float holds
        // one, and toExactLongOrNull already answers null for it. This is not a corner
        // case either - both parsers that read a literal as a number hand 1E1000 back as
        // a BigDecimal whose toDouble() is Infinity, and answering false for that is what
        // let this check pass a magnitude the Float arm then had to refuse under a "Float"
        // target, for what the file wrote as an integer.
        assertTrue(HBackup.isMagnitudeTooWide(Double.NaN))
        assertTrue(HBackup.isMagnitudeTooWide(Double.POSITIVE_INFINITY))
        assertTrue(HBackup.isMagnitudeTooWide(Double.NEGATIVE_INFINITY))
    }

    @Test
    fun `restore refuses a decimal too wide for a double, not just a finite one`() = runTest {
        // 1E1000 overflows Double to Infinity, so a magnitude check built on the exact
        // BigDecimal value stops discriminating exactly here - and a check that requires a
        // finite Double waves it through to putFloat, where it is refused under a "Float"
        // target for what the file wrote as an integer. Nothing is recorded for the key
        // here on purpose: with a recorded type the value takes the number arm instead and
        // isTooWideInteger is never consulted, so the assertion below would pass whether
        // or not this check existed.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_long":1E1000}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        // The target names what the file's number is, not what would have held it: no
        // preference can hold this, and the Long arm is the one that refused it.
        verify { HLog.w(any(), match { it.contains("a_long") && it.contains("Long") }) }
    }

    @Test
    fun `restore skips a fraction whose double has already rounded it`() = runTest {
        // Two fractions whose Double conversion is a different number, so the digits the
        // file held are gone before the reader sees them. 9007199254740991.5 is the
        // midpoint between two adjacent doubles and rounds up to 2^53, which the magnitude
        // check refuses. 4503599627370496.5 is below the cut but still rounds, because a
        // Double only names whole numbers from 2^52 up and this one has a half left to
        // place; a device that parsed this file stores the whole number it landed on, so
        // storing anything here would invent a value. Both are refused, and the second is
        // the case a magnitude comparison on the exact BigDecimal alone waves through to
        // putFloat(4.5035996E15f). The fraction just below it, 4503599627370495.5, is one a
        // Double does still name, which is what keeps this a question about the number
        // rather than a cut at 2^52.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_wide":9007199254740991.5,"a_fraction":4503599627370496.5}""")

        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_wide") }) }
        verify { HLog.w(any(), match { it.contains("a_fraction") }) }
    }

    @Test
    fun `restore skips a wide decimal for an undeclared key however org json reports it`() = runTest {
        // A fraction written at 2^53 magnitude. Its scale is 1, so a scale-based test
        // reads it as an ordinary fraction and lets it through, where the same digits
        // without the .5 are refused. It then cannot be a Long either, so the only
        // thing left is putFloat(9.007199E15f): a number nobody chose, stored silently.
        // A device hands the same file back as a Double, which the magnitude test refuses.
        // The two implementations must not disagree about one file.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_wide":9007199254740993.5}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putLong(any(), any()) }
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains("a_wide") && it.contains("Long") }) }
    }

    @Test
    fun `restore keeps a fraction a double names exactly however far out it is`() = runTest {
        // The other side of both questions, so neither can be read as "refuse every
        // decimal that is not a whole number" or "refuse everything past 2^52". A Double
        // carries 53 bits, so 2^20 + 0.5 is still named exactly that far out, and both
        // parsers hand this file back a number they can still print: 1.5, and
        // 1048576.5, which is a Float as well.
        val editor = mockPreferences()
        val zipFile = zipWithSettings("""{"a_decimal":1.5,"a_outer_decimal":1048576.5}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat("a_decimal", 1.5f) }
        verify { editor.putFloat("a_outer_decimal", 1048576.5f) }
        verify(exactly = 0) { HLog.w(any(), match { it.contains("a_") }) }
    }

    @Test
    fun `restore skips a value too wide for a declared float preference`() = runTest {
        // Being a declared Float key decides the type of the value, not its magnitude.
        // Without the guard this stores 1.2345678E22f under a slider whose range is
        // 11f..16f, and isFinite() accepts it happily, while the identical digits under
        // an undeclared key are refused. homeFontSize is passed straight to setTextSize.
        val editor = mockPreferences(HailData.HOME_FONT_SIZE to 14f)
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":12345678901234567890123}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains(HailData.HOME_FONT_SIZE) }) }
    }

    @Test
    fun `restore skips a float preference outside the range its own slider allows`() = runTest {
        // The second half of the same gap. -5 is a finite Float and a plausible-looking
        // number, so only the declared range refuses it. Before the reader was fixed the
        // same file left an Int under this key, which getFloat reports as absent, so the
        // value was inert; restoring it as a Float hands a negative delay straight to
        // setInitialDelay and moves the recency window start past now.
        val editor = mockPreferences(HailData.AUTO_FREEZE_DELAY to 0f)
        val zipFile = zipWithSettings("""{"${HailData.AUTO_FREEZE_DELAY}":-5.0}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify(exactly = 0) { editor.putFloat(any(), any()) }
        verify { HLog.w(any(), match { it.contains(HailData.AUTO_FREEZE_DELAY) }) }
    }

    @Test
    fun `restore keeps a float preference at the top of the range its slider allows`() = runTest {
        // The bound is the slider's, and it is inclusive. 16f is the last value the
        // settings screen can produce for this key, so the guard that refuses -5 must
        // not refuse it: an off-by-one here would lock every user out of the setting
        // they actually chose.
        val editor = mockPreferences(HailData.HOME_FONT_SIZE to 14f)
        val zipFile = zipWithSettings("""{"${HailData.HOME_FONT_SIZE}":16.0}""")
        val options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = true)

        val result = HBackup.restore(HailApp.app, zipFile, options)

        assertTrue(result.isSuccess)
        verify { editor.putFloat(HailData.HOME_FONT_SIZE, 16f) }
        verify(exactly = 0) { HLog.w(any(), match { it.contains(HailData.HOME_FONT_SIZE) }) }
    }

    @Test
    fun `every declared float preference range covers the default it is read with`() {
        // The map is the single declaration of the Float surface: the settings screen asks
        // it for each slider's bounds and default, the getters read their fallback from
        // it, and the reader refuses anything outside the bounds. So a default that fell
        // outside its own range would be rejected on the first restore of an untouched
        // install, and a key added to the map without a default to read it with would be
        // rejected as undeclared. Iterating the map itself is what makes both visible: a
        // copy of its keys in this test would pass whatever the map says, and a third key
        // would never be checked at all.
        for ((key, preference) in HailData.FLOAT_PREFERENCES) {
            assertTrue(
                "$key default ${preference.default} is outside ${preference.range}",
                preference.default in preference.range
            )
        }
    }

    @Test
    fun `declaring a float preference on one side of the map alone is rejected`() {
        // Both directions of the drift, because either one leaves the reader restoring a
        // Float with the type the app means for a key it treats as undeclared - or refusing
        // a value the slider itself can produce. The settings screen asks for a slider's
        // range and default, and the getters take their fallback from the same map, so a
        // Float preference cannot be declared on either side without it.
        for (undeclared in listOf("a_key_nobody_declared")) {
            assertTrue(
                "expected a missing range to fail loudly",
                runCatching { HailData.floatRange(undeclared) }.exceptionOrNull() is IllegalStateException
            )
            assertTrue(
                "expected a missing default to fail loudly",
                runCatching { HailData.floatDefault(undeclared) }.exceptionOrNull() is IllegalStateException
            )
        }
    }
}
