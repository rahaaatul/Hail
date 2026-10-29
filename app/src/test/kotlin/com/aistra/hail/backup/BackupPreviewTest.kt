package com.aistra.hail.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupPreviewTest {
    private fun createTempZip(
        file: File,
        entries: Map<String, String>,
    ) {
        ZipOutputStream(java.io.FileOutputStream(file)).use { zos ->
            entries.forEach { name, content ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content.toByteArray())
                zos.closeEntry()
            }
        }
    }

    @Test
    fun `entriesOf returns all entries in ZIP`() {
        val tempDir = File(java.lang.System.getProperty("java.io.tmpdir"), "backuppreview_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val zipFile = File(tempDir, "test.zip")
        createTempZip(
            zipFile,
            mapOf(
                "apps.json" to "[{\"package\":\"com.example.app\"}]",
                "tags.json" to "[{\"tag\":\"Work\",\"id\":1}]",
                "manifest.json" to "{\"format\":\"hail-backup\",\"version\":1}",
            ),
        )

        val entries = entriesOf(zipFile)
        val appEntry = entries.find { it.category == BackupCategory.APPS }
        val tagEntry = entries.find { it.category == BackupCategory.TAGS }

        assertTrue(appEntry?.present == true)
        assertEquals("apps.json", appEntry?.name)
        assertTrue(appEntry?.sizeBytes ?: -1 > 0)

        assertTrue(tagEntry?.present == true)
        assertEquals("tags.json", tagEntry?.name)

        tempDir.deleteRecursively()
    }

    @Test
    fun `entriesOf marks missing categories as not present`() {
        val tempDir = File(java.lang.System.getProperty("java.io.tmpdir"), "backuppreview_${System.currentTimeMillis()}")
        tempDir.mkdirs()
        val zipFile = File(tempDir, "test.zip")
        createTempZip(
            zipFile,
            mapOf(
                "apps.json" to "[]",
                "manifest.json" to "{\"format\":\"hail-backup\",\"version\":1}",
            ),
        )

        val entries = entriesOf(zipFile)
        val settingsEntry = entries.find { it.category == BackupCategory.SETTINGS }

        assertFalse(settingsEntry?.present == true)
        assertEquals("settings.bin", settingsEntry?.name)

        tempDir.deleteRecursively()
    }

    @Test
    fun `canStartBackup returns true when any option is enabled`() {
        assertTrue(canStartBackup(BackupOptions(apps = true)))
        assertTrue(canStartBackup(BackupOptions(tags = true)))
        assertTrue(canStartBackup(BackupOptions(actions = true)))
        assertTrue(canStartBackup(BackupOptions(settings = true)))
        assertTrue(canStartBackup(BackupOptions(whitelist = true)))
        assertTrue(canStartBackup(BackupOptions(apps = true, tags = true)))
    }

    @Test
    fun `canStartBackup returns false when no options are enabled`() {
        assertFalse(canStartBackup(BackupOptions(apps = false, tags = false, actions = false, settings = false, whitelist = false)))
    }

    @Test
    fun `canStartRestore returns false for Empty state`() {
        assertFalse(canStartRestore(RestoreState.Empty))
    }

    @Test
    fun `canStartRestore returns false for Loaded state with no entries`() {
        val state =
            RestoreState.Loaded(
                file = File("/tmp/test.zip"),
                displayName = "test.zip",
                sizeBytes = 0,
                entries = emptyList(),
                options = RestoreOptions(),
                phase = Phase.Idle,
                message = null,
            )
        assertFalse(canStartRestore(state))
    }

    @Test
    fun `canStartRestore returns true for Loaded state with present entries`() {
        val state =
            RestoreState.Loaded(
                file = File("/tmp/test.zip"),
                displayName = "test.zip",
                sizeBytes = 0,
                entries =
                    listOf(
                        BackupEntry(
                            category = BackupCategory.APPS,
                            name = "apps.json",
                            present = true,
                            sizeBytes = 100,
                        ),
                    ),
                options = RestoreOptions(),
                phase = Phase.Idle,
                message = null,
            )
        assertTrue(canStartRestore(state))
    }

    @Test
    fun `backup options checked returns the matching option for every category`() {
        assertTrue(BackupOptions(apps = true).checked(BackupCategory.APPS))
        assertFalse(BackupOptions(apps = false).checked(BackupCategory.APPS))
        assertTrue(BackupOptions(tags = true).checked(BackupCategory.TAGS))
        assertFalse(BackupOptions(tags = false).checked(BackupCategory.TAGS))
        assertTrue(BackupOptions(actions = true).checked(BackupCategory.ACTIONS))
        assertFalse(BackupOptions(actions = false).checked(BackupCategory.ACTIONS))
        assertTrue(BackupOptions(settings = true).checked(BackupCategory.SETTINGS))
        assertFalse(BackupOptions(settings = false).checked(BackupCategory.SETTINGS))
        assertTrue(BackupOptions(whitelist = true).checked(BackupCategory.WHITELIST))
        assertFalse(BackupOptions(whitelist = false).checked(BackupCategory.WHITELIST))
    }

    @Test
    fun `restore options checked returns the matching option for every category`() {
        assertTrue(RestoreOptions(apps = true).checked(BackupCategory.APPS))
        assertFalse(RestoreOptions(apps = false).checked(BackupCategory.APPS))
        assertTrue(RestoreOptions(tags = true).checked(BackupCategory.TAGS))
        assertFalse(RestoreOptions(tags = false).checked(BackupCategory.TAGS))
        assertTrue(RestoreOptions(actions = true).checked(BackupCategory.ACTIONS))
        assertFalse(RestoreOptions(actions = false).checked(BackupCategory.ACTIONS))
        assertTrue(RestoreOptions(settings = true).checked(BackupCategory.SETTINGS))
        assertFalse(RestoreOptions(settings = false).checked(BackupCategory.SETTINGS))
        assertTrue(RestoreOptions(whitelist = true).checked(BackupCategory.WHITELIST))
        assertFalse(RestoreOptions(whitelist = false).checked(BackupCategory.WHITELIST))
    }

    @Test
    fun `categoryOf maps each known entry name to its category`() {
        assertEquals(BackupCategory.APPS, categoryOf("apps.json"))
        assertEquals(BackupCategory.TAGS, categoryOf("tags.json"))
        assertEquals(BackupCategory.ACTIONS, categoryOf("actions.json"))
        assertEquals(BackupCategory.SETTINGS, categoryOf("settings.bin"))
        assertEquals(BackupCategory.WHITELIST, categoryOf("whitelist.json"))
    }

    @Test
    fun `categoryOf returns null for unknown entry names`() {
        assertNull(categoryOf("manifest.json"))
        assertNull(categoryOf("README.txt"))
        assertNull(categoryOf("nested/dir/apps.json"))
        assertNull(categoryOf(""))
    }

    @Test
    fun `fileNameOf maps each category to its entry name`() {
        assertEquals("apps.json", fileNameOf(BackupCategory.APPS))
        assertEquals("tags.json", fileNameOf(BackupCategory.TAGS))
        assertEquals("actions.json", fileNameOf(BackupCategory.ACTIONS))
        assertEquals("settings.bin", fileNameOf(BackupCategory.SETTINGS))
        assertEquals("whitelist.json", fileNameOf(BackupCategory.WHITELIST))
    }
}
