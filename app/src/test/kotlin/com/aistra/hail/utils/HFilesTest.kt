package com.aistra.hail.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HFilesTest {
    private val tempDir = File(java.lang.System.getProperty("java.io.tmpdir"), "hailtest_${System.currentTimeMillis()}")

    @Test
    fun `exists returns false for missing file`() {
        assertFalse(HFiles.exists("$tempDir/nonexistent.txt"))
    }

    @Test
    fun `createDirectories creates the directory`() {
        val dir = "$tempDir/new/sub/dir"
        assertTrue(HFiles.createDirectories(dir))
        assertTrue(File(dir).exists())
    }

    @Test
    fun `write and read roundtrip`() {
        tempDir.mkdirs()
        val file = File(tempDir, "roundtrip.txt")
        assertTrue(HFiles.write(file.absolutePath, "hello world"))
        assertEquals("hello world", HFiles.read(file.absolutePath))
    }

    @Test
    fun `read returns null for missing file`() {
        assertEquals(null, HFiles.read("$tempDir/missing.txt"))
    }

    @Test
    fun `write returns false when path is invalid`() {
        assertFalse(HFiles.write("/proc/1/nonexistent_file_xyz", "data"))
    }
}
