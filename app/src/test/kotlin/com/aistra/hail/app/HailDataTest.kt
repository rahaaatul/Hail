package com.aistra.hail.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HailDataTest {
    @Test
    fun `working mode default is correct`() {
        assertEquals("default", HailData.MODE_DEFAULT)
    }

    @Test
    fun `working mode shizuku hide is correct`() {
        assertEquals("shizuku_hide", HailData.MODE_SHIZUKU_HIDE)
    }

    @Test
    fun `working mode island hide is correct`() {
        assertEquals("island_hide", HailData.MODE_ISLAND_HIDE)
    }

    @Test
    fun `every float read in the app goes through the declared default`() {
        // HailData.FLOAT_PREFERENCES is the app's only statement of which keys are Floats
        // and what each falls back to, and the backup reader treats a key it does not list
        // as undeclared: the value is restored with whatever type the file happens to carry
        // and no range check, so a bare whole number goes in as an Int and stays there -
        // which is #91 under a different key. A getter that called sp.getFloat itself
        // would reintroduce exactly that without changing anything the type system can
        // see, so the design is one accessor and this test is what holds it there.
        //
        // The scan is over the source tree rather than over a set of call sites, because
        // there is no other way to see a getter that has not been written yet. It fails
        // loudly if it cannot find the tree at all, and it fails if it finds no reads
        // either, so it cannot pass by scanning nothing.
        val reads = kotlinSources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> line.contains("getFloat(") }
                .map { (index, line) -> "${file.name}:${index + 1} ${line.trim()}" }
        }
        assertTrue("expected at least one SharedPreferences Float read to check", reads.isNotEmpty())

        val unguarded = reads.filterNot { it.contains("floatDefault(") }
        assertTrue(
            "these reads take their default from a literal rather than from " +
                "HailData.FLOAT_PREFERENCES, so the backup reader will not recognise the " +
                "key: $unguarded",
            unguarded.isEmpty()
        )
    }

    /**
     * The app module's Kotlin sources, found by walking up from the test's working
     * directory (Gradle runs it from the module root, but the test is not entitled to
     * assume that) until the tree that holds [HailData] appears.
     */
    private fun kotlinSources(): List<File> {
        var directory: File? = File("").absoluteFile
        while (directory != null) {
            val sources = File(directory, "src/main/kotlin")
            if (File(sources, HAIL_DATA_PATH).isFile) {
                return sources.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
            }
            directory = directory.parentFile
        }
        throw AssertionError(
            "could not find src/main/kotlin from ${File("").absolutePath}; this test " +
                "checks the app's Float reads and cannot do that without the sources"
        )
    }

    private companion object {
        const val HAIL_DATA_PATH = "com/aistra/hail/app/HailData.kt"
    }
}
