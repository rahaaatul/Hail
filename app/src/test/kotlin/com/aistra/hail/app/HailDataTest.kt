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
        // #91 under a different key. A getter that called sp.getFloat itself could
        // reintroduce exactly that without changing anything the type system can see, so
        // the design is one accessor and this is what holds it there.
        //
        // Checking that the line mentions floatDefault is not enough. A read that pairs a
        // new key with another key's default compiles, passes every restore test, and loses
        // that key on the next restore. So the read is pinned two ways: there is exactly
        // one of them, it lives in HailData.kt, and the key it reads is the key the default
        // is asked about - which is the property that makes the accessor's default the right
        // one for whatever key a caller passed it.
        val reads = callSites(GET_FLOAT)
        assertTrue("expected at least one SharedPreferences Float read to check", reads.isNotEmpty())
        assertEquals(
            "these SharedPreferences Float reads bypass HailData.declaredFloat, so their " +
                "default can come from somewhere other than the declaration: $reads",
            1,
            reads.size
        )
        val read = reads.single()
        assertEquals(
            "the one SharedPreferences Float read belongs in HailData.kt: ${read.location}",
            HAIL_DATA_FILE_NAME,
            read.file.name
        )
        assertEquals(
            "a Float read must take its default from the declaration of the same key: ${read.location}",
            argumentOf(read.text, GET_FLOAT),
            argumentOf(read.text, FLOAT_DEFAULT)
        )
    }

    @Test
    fun `every declared float preference is read through the declared accessor`() {
        // The other direction of the same drift, and the one the map cannot see: a key
        // declared as a Float that nothing reads. The reader bounds such a key to the
        // declared range and refuses anything outside it, so the declaration has to be a
        // fact about the app rather than a fact about this file. Keys are matched by the
        // string they resolve to, so renaming a constant does not quietly undeclare it.
        val constants = constantStringsIn(hailDataSource())
        val accessorArguments = callSites(DECLARED_FLOAT)
            .map { argumentOf(it.text, DECLARED_FLOAT)?.substringAfterLast('.') }
            .toSet()
        for (key in HailData.FLOAT_PREFERENCES.keys) {
            val identifier = constants.entries.firstOrNull { it.value == key }?.key
            assertTrue(
                "no const val in $HAIL_DATA_FILE_NAME declares the string '$key', so this " +
                    "test cannot tell whether the preference behind it is still read",
                identifier != null
            )
            assertTrue(
                "'$key' is declared a Float preference but nothing reads it through " +
                    "declaredFloat($identifier), so the range the reader enforces bounds no " +
                    "setting the app has: $accessorArguments",
                identifier in accessorArguments
            )
        }
    }

    @Test
    fun `every float slider takes its bounds and default from the declaration`() {
        // The screen side of the same declaration, and the failure it prevents is silent
        // rather than wrong: a slider left with literal bounds narrower than the map's lets
        // a user pick a value the reader then refuses, so every later restore of that
        // setting quietly leaves it alone. Nothing in the type system relates a literal
        // range to the map, so it is checked here instead.
        //
        // Every sliderPreference in the app is a Float slider today, which is what lets
        // this be a blanket rule; the first Int or Long slider is the day this test has to
        // be told about the difference rather than left to fail.
        val sliders = callArguments(SLIDER_PREFERENCE)
        assertTrue("expected at least one slider to check", sliders.isNotEmpty())
        for ((location, arguments) in sliders) {
            val key = named(arguments, "key")?.substringAfterLast('.')
            assertTrue("$location declares no key", key != null)
            assertEquals(
                "$location must take its bounds from the declaration, not a literal",
                "HailData.floatRange(HailData.$key)",
                named(arguments, "valueRange")
            )
            assertEquals(
                "$location must take its default from the declaration, not a literal",
                "HailData.floatDefault(HailData.$key)",
                named(arguments, "defaultValue")
            )
        }
    }

    /** Every line in the app's Kotlin sources that contains [needle]. */
    private fun callSites(needle: String): List<CallSite> =
        kotlinSources().flatMap { file ->
            file.readLines().withIndex()
                .filter { (_, line) -> line.contains(needle) }
                .map { (index, line) -> CallSite(file, index + 1, line.trim()) }
        }

    /**
     * The argument text of every call to [needle], balanced over newlines so a call written
     * one argument per line is read whole, paired with a location a failure can name.
     */
    private fun callArguments(needle: String): List<Pair<String, String>> =
        kotlinSources().flatMap { file ->
            val text = file.readText()
            val sites = mutableListOf<Pair<String, String>>()
            var from = 0
            while (true) {
                val start = text.indexOf(needle, from)
                if (start < 0) break
                from = start + needle.length
                var depth = 1
                var index = from
                while (index < text.length && depth > 0) {
                    when (text[index]) {
                        '(' -> depth++
                        ')' -> depth--
                    }
                    index++
                }
                sites += "${file.name}:${text.take(start).count { it == '\n' } + 1}" to
                        text.substring(from, index - 1)
            }
            sites
        }

    /** The first argument after [needle] in [text], qualified names and all. */
    private fun argumentOf(text: String, needle: String): String? {
        val after = text.substringAfter(needle, "")
        if (after.isEmpty()) return null
        return FIRST_ARGUMENT.find(after)?.groupValues?.get(1)
    }

    /** The value of `name = …` inside a call's argument text. */
    private fun named(arguments: String, name: String): String? =
        Regex("$name\\s*=\\s*([\\w.]+(?:\\([^()]*\\))?)").find(arguments)?.groupValues?.get(1)

    /** `const val NAME = "value"` pairs, which is how a preference key reaches the map. */
    private fun constantStringsIn(file: File): Map<String, String> =
        Regex("const val (\\w+) = \"([^\"]*)\"").findAll(file.readText())
            .associate { it.groupValues[1] to it.groupValues[2] }

    private fun hailDataSource(): File =
        kotlinSources().single { it.name == HAIL_DATA_FILE_NAME }

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

    private data class CallSite(val file: File, val line: Int, val text: String) {
        val location: String get() = "${file.name}:$line"
    }

    private companion object {
        const val HAIL_DATA_PATH = "com/aistra/hail/app/HailData.kt"
        const val HAIL_DATA_FILE_NAME = "HailData.kt"
        const val GET_FLOAT = "getFloat("
        const val FLOAT_DEFAULT = "floatDefault("
        const val DECLARED_FLOAT = "declaredFloat("
        const val SLIDER_PREFERENCE = "sliderPreference("
        val FIRST_ARGUMENT = Regex("^\\s*([\\w.]+)\\s*[,)]")
    }
}
