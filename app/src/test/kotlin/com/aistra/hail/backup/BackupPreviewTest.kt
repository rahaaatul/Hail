package com.aistra.hail.backup

import com.aistra.hail.utils.HBackup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupPreviewTest {

    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private fun writeZip(vararg entries: Pair<String, String>): File {
        val zipFile = temporaryFolder.newFile("test.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zipOutputStream ->
            entries.forEach { (name, content) ->
                zipOutputStream.putNextEntry(ZipEntry(name))
                zipOutputStream.write(content.toByteArray())
                zipOutputStream.closeEntry()
            }
        }
        return zipFile
    }

    // ===== entryCategory tests =====

    @Test
    fun `entryCategory maps apps_json to APPS`() {
        assertEquals(BackupCategory.APPS, entryCategory("apps.json"))
    }

    @Test
    fun `entryCategory maps whitelist_json to WHITELIST`() {
        assertEquals(BackupCategory.WHITELIST, entryCategory("whitelist.json"))
    }

    @Test
    fun `entryCategory maps actions_json to ACTIONS`() {
        assertEquals(BackupCategory.ACTIONS, entryCategory("actions.json"))
    }

    @Test
    fun `entryCategory maps settings_json to SETTINGS`() {
        assertEquals(BackupCategory.SETTINGS, entryCategory("settings.json"))
    }

    @Test
    fun `entryCategory returns null for unknown entry`() {
        assertNull(entryCategory("unknown.json"))
    }

    @Test
    fun `entryCategory returns null for directory entry`() {
        assertNull(entryCategory("some/directory/"))
    }

    @Test
    fun `entryCategory returns null for nested file`() {
        assertNull(entryCategory("subdir/apps.json"))
    }

    // ===== entriesOf tests =====

    @Test
    fun `entriesOf returns all four categories when zip has all entries`() {
        val zipFile = writeZip(
            "apps.json" to """["pkg1","pkg2"]""",
            "whitelist.json" to """["pkg3"]""",
            "actions.json" to """[]""",
            "settings.json" to """{"key":"value"}"""
        )

        val entries = entriesOf(zipFile)

        assertEquals(4, entries.size)
        assertEquals(BackupCategory.APPS, entries[0].category)
        assertTrue(entries[0].present)
        assertTrue(entries[0].sizeBytes > 0)
        assertEquals(BackupCategory.WHITELIST, entries[1].category)
        assertTrue(entries[1].present)
        assertTrue(entries[1].sizeBytes > 0)
        assertEquals(BackupCategory.ACTIONS, entries[2].category)
        assertTrue(entries[2].present)
        assertTrue(entries[2].sizeBytes > 0)
        assertEquals(BackupCategory.SETTINGS, entries[3].category)
        assertTrue(entries[3].present)
        assertTrue(entries[3].sizeBytes > 0)
    }

    @Test
    fun `entriesOf returns present false and size 0 for missing settings.json`() {
        val zipFile = writeZip(
            "apps.json" to """["pkg1"]""",
            "whitelist.json" to """[]""",
            "actions.json" to """[]"""
        )

        val entries = entriesOf(zipFile)

        assertEquals(4, entries.size)
        assertTrue(entries[0].present)
        assertTrue(entries[1].present)
        assertTrue(entries[2].present)
        assertEquals(BackupCategory.SETTINGS, entries[3].category)
        assertFalse(entries[3].present)
        assertEquals(0L, entries[3].sizeBytes)
    }

    @Test
    fun `entriesOf ignores unrelated extra file in zip`() {
        val zipFile = writeZip(
            "apps.json" to """["pkg1"]""",
            "unrelated.txt" to "ignored",
            "whitelist.json" to """[]"""
        )

        val entries = entriesOf(zipFile)

        assertEquals(4, entries.size)
        assertTrue(entries[0].present)
        assertTrue(entries[1].present)
        assertFalse(entries[2].present)
        assertEquals(0L, entries[2].sizeBytes)
        assertFalse(entries[3].present)
        assertEquals(0L, entries[3].sizeBytes)
    }

    @Test
    fun `entriesOf returns four absent entries when zip has none of the four`() {
        val zipFile = writeZip(
            "readme.txt" to "not a backup",
            "other.json" to "{}"
        )

        val entries = entriesOf(zipFile)

        assertEquals(4, entries.size)
        entries.forEach { entry ->
            assertFalse(entry.present)
            assertEquals(0L, entry.sizeBytes)
        }
    }

    @Test
    fun `entriesOf returns fixed order APPS WHITELIST ACTIONS SETTINGS`() {
        val zipFile = writeZip(
            "settings.json" to "{}",
            "apps.json" to "[]",
            "actions.json" to "[]",
            "whitelist.json" to "[]"
        )

        val entries = entriesOf(zipFile)

        assertEquals(BackupCategory.APPS, entries[0].category)
        assertEquals(BackupCategory.WHITELIST, entries[1].category)
        assertEquals(BackupCategory.ACTIONS, entries[2].category)
        assertEquals(BackupCategory.SETTINGS, entries[3].category)
    }

    // ===== canStartBackup tests =====

    @Test
    fun `canStartBackup returns false when nothing selected`() {
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = false)
        assertFalse(canStartBackup(options))
    }

    @Test
    fun `canStartBackup returns true when only apps selected`() {
        val options = HBackup.BackupOptions(apps = true, whitelist = false, actions = false, settings = false)
        assertTrue(canStartBackup(options))
    }

    @Test
    fun `canStartBackup returns true when only whitelist selected`() {
        val options = HBackup.BackupOptions(apps = false, whitelist = true, actions = false, settings = false)
        assertTrue(canStartBackup(options))
    }

    @Test
    fun `canStartBackup returns true when only actions selected`() {
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = true, settings = false)
        assertTrue(canStartBackup(options))
    }

    @Test
    fun `canStartBackup returns true when only settings selected`() {
        val options = HBackup.BackupOptions(apps = false, whitelist = false, actions = false, settings = true)
        assertTrue(canStartBackup(options))
    }

    @Test
    fun `canStartBackup returns true when all selected`() {
        val options = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true)
        assertTrue(canStartBackup(options))
    }

    // ===== canStartRestore tests =====

    @Test
    fun `canStartRestore returns false for Empty state`() {
        assertFalse(canStartRestore(RestoreState.Empty))
    }

    @Test
    fun `canStartRestore returns false for Loaded state with nothing selected`() {
        val zipFile = writeZip("apps.json" to "[]")
        val entries = entriesOf(zipFile)
        val state = RestoreState.Loaded(
            file = zipFile,
            displayName = "test.zip",
            sizeBytes = zipFile.length(),
            entries = entries,
            options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = false),
            phase = Phase.Idle,
            message = null
        )
        assertFalse(canStartRestore(state))
    }

    @Test
    fun `canStartRestore returns true for Loaded state with apps selected`() {
        val zipFile = writeZip("apps.json" to "[]")
        val entries = entriesOf(zipFile)
        val state = RestoreState.Loaded(
            file = zipFile,
            displayName = "test.zip",
            sizeBytes = zipFile.length(),
            entries = entries,
            options = HBackup.RestoreOptions(apps = true, whitelist = false, actions = false, settings = false),
            phase = Phase.Idle,
            message = null
        )
        assertTrue(canStartRestore(state))
    }

    @Test
    fun `canStartRestore returns true for Loaded state with all selected`() {
        val zipFile = writeZip(
            "apps.json" to "[]",
            "whitelist.json" to "[]",
            "actions.json" to "[]",
            "settings.json" to "{}"
        )
        val entries = entriesOf(zipFile)
        val state = RestoreState.Loaded(
            file = zipFile,
            displayName = "test.zip",
            sizeBytes = zipFile.length(),
            entries = entries,
            options = HBackup.RestoreOptions(apps = true, whitelist = true, actions = true, settings = true),
            phase = Phase.Idle,
            message = null
        )
        assertTrue(canStartRestore(state))
    }

    @Test
    fun `canStartRestore returns false for Loaded state in Working phase`() {
        val zipFile = writeZip("apps.json" to "[]")
        val entries = entriesOf(zipFile)
        val state = RestoreState.Loaded(
            file = zipFile,
            displayName = "test.zip",
            sizeBytes = zipFile.length(),
            entries = entries,
            options = HBackup.RestoreOptions(apps = true, whitelist = false, actions = false, settings = false),
            phase = Phase.Working,
            message = null
        )
        assertFalse(canStartRestore(state))
    }

    @Test
    fun `canStartRestore returns false for Loaded state in Done phase`() {
        val zipFile = writeZip("apps.json" to "[]")
        val entries = entriesOf(zipFile)
        val state = RestoreState.Loaded(
            file = zipFile,
            displayName = "test.zip",
            sizeBytes = zipFile.length(),
            entries = entries,
            options = HBackup.RestoreOptions(apps = true, whitelist = false, actions = false, settings = false),
            phase = Phase.Done,
            message = null
        )
        assertFalse(canStartRestore(state))
    }
}