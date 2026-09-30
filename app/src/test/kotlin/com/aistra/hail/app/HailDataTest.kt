package com.aistra.hail.app

import com.aistra.hail.installHailDataFilesDirForTests
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

class HailDataTest {

    companion object {
        /**
         * Before any test, and not in `@Before`: `HailData` is an object whose initializers
         * name `app.filesDir`, so the first class to touch it decides what the whole JVM
         * sees - both whether it initializes at all and which directory it froze onto. See
         * `installHailDataFilesDirForTests`, and the same call in `HBackupTest`, which is
         * just as capable of being first.
         */
        @BeforeClass
        @JvmStatic
        fun installFilesDir() {
            installHailDataFilesDirForTests()
        }
    }

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
        //
        // The calls are read whole and balanced over newlines, because a read spelled the
        // way the rest of this file spells long calls - one argument per line - otherwise
        // yields two empty extractions that compare equal to each other and check nothing.
        val reads = callArguments(GET_FLOAT)
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
        val defaults = callsIn(read.arguments, FLOAT_DEFAULT)
        assertEquals(
            "the one SharedPreferences Float read must ask the declaration for its default, " +
                "once: ${read.location}",
            1,
            defaults.size
        )
        val readKey = firstArgumentOf(read.arguments)
        val defaultKey = firstArgumentOf(defaults.single())
        assertNotNull("could not read the key the Float read uses: ${read.location}", readKey)
        assertNotNull("could not read the key the default is asked about: ${read.location}", defaultKey)
        assertEquals(
            "a Float read must take its default from the declaration of the same key: ${read.location}",
            readKey,
            defaultKey
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
        val accessorArguments = callArguments(DECLARED_FLOAT)
            .mapNotNull { firstArgumentOf(it.arguments)?.substringAfterLast('.') }
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
        for (slider in sliders) {
            val location = slider.location
            val key = slider.keyConstant()
            assertNotNull("$location must name a HailData constant as the key it edits", key)
            assertTrue(
                "$location must take its bounds from the declaration, not a literal: it has " +
                    "${slider.valueOf("valueRange")}",
                slider.takes("valueRange", "HailData.floatRange(HailData.$key)")
            )
            assertTrue(
                "$location must take its default from the declaration, not a literal: it has " +
                    "${slider.valueOf("defaultValue")}",
                slider.takes("defaultValue", "HailData.floatDefault(HailData.$key)")
            )
        }
    }

    /** One call to [needle] in the app's Kotlin sources, with its argument text and where it is. */
    private fun callArguments(needle: String): List<Call> =
        kotlinSources().flatMap { file ->
            val text = file.readText()
            argumentTexts(code(text), text, needle).map { (start, arguments) ->
                Call(file, text.take(start).count { it == '\n' } + 1, arguments)
            }
        }

    /** The argument text of every call to [needle] inside [text], for a call nested in another. */
    private fun callsIn(text: String, needle: String): List<String> =
        argumentTexts(code(text), text, needle).map { it.second }

    /**
     * `(offset of the call, its argument text)` for every call to [needle] in [masked], which
     * must be [original] with every comment and string blanked out. The offsets come from the
     * masked text so that a `(` in a comment is not a call, and the argument text comes from
     * the original so that a failure can be read. A declaration is not a call, so
     * `fun declaredFloat(key: String)` is not read as one - which matters here, because the
     * declaration's own "arguments" are a parameter list and would name no key at all.
     */
    private fun argumentTexts(masked: String, original: String, needle: String): List<Pair<Int, String>> {
        val calls = mutableListOf<Pair<Int, String>>()
        var from = 0
        while (true) {
            val start = masked.indexOf(needle, from)
            if (start < 0) break
            from = start + needle.length
            if (FUNCTION_DECLARATION.containsMatchIn(masked.substring(0, start))) continue
            var depth = 1
            var index = from
            while (index < masked.length && depth > 0) {
                when (masked[index]) {
                    '(' -> depth++
                    ')' -> depth--
                }
                index++
            }
            calls += start to original.substring(from, index - 1)
        }
        return calls
    }

    /**
     * The first argument of a call, if it is a bare name.
     *
     * A bare name is the point. The keys being compared are `key` on both sides of the tie,
     * and a first argument that is not a name - a call, a literal, a cast - is not what the
     * accessor is supposed to be reading, so it fails here with the location rather than
     * quietly comparing two expressions that happen to differ or match.
     */
    private fun firstArgumentOf(arguments: String): String? =
        FIRST_ARGUMENT.find(code(arguments))?.groupValues?.get(1)

    /**
     * The constant a named argument names, if the argument is `name = HailData.CONSTANT`.
     *
     * Only the `key` argument is read this way, and only so that the other two assertions
     * can require the *same* constant: a slider whose bounds come from one preference and
     * whose default comes from another is the interesting failure, and it cannot be seen by
     * comparing either value on its own.
     */
    private fun Call.keyConstant(): String? =
        Regex("\\bkey\\s*=\\s*HailData\\.(\\w+)").find(code(arguments))?.groupValues?.get(1)

    /**
     * Whether a named argument's value is exactly [value], which is what the check is about.
     *
     * Matched as a pattern rather than extracted and compared, so no argument splitting is
     * involved and nothing has to know where a value ends: the name, the `=`, and the whole
     * value have to be adjacent, and the value has to be the argument - `+ 1f` after it
     * fails. A top-level generic argument is not a hazard here for the same reason; a
     * counter that split arguments on commas did not know `<...>` from a comparison, and
     * split `emptyMap<String, Int>()` in half.
     */
    private fun Call.takes(name: String, value: String): Boolean =
        Regex("\\b${Regex.escape(name)}\\s*=\\s*${Regex.escape(value)}(?=\\s*(?:,|$))")
            .containsMatchIn(code(arguments))

    /** The text after `name = ` on its first line, for a message that has to be readable. */
    private fun Call.valueOf(name: String): String =
        Regex("\\b${Regex.escape(name)}\\s*=\\s*([^\\n]*)").find(arguments)
            ?.groupValues?.get(1)?.trim()
            ?.takeIf { it.isNotEmpty() } ?: "no $name"

    /**
     * [text] with every comment and string literal replaced by spaces, so that indexes still
     * line up and a bracket inside one of them cannot move a counter.
     *
     * Counting brackets in raw text is the kind of thing that works until someone writes a
     * comment. A lone `)` - `"%d of %d)"`, or `// (see #91)` - used to close the argument
     * list early, and the failure then named the wrong argument on a call site that was
     * entirely correct. A lone `(` went the other way and swallowed the next call site's
     * arguments, which is worse: the assertion can be satisfied by a neighbouring slider.
     * This file is a scanner over a source tree that is written by hand, and this branch adds
     * a lot of prose inside argument lists, so the exposure is not hypothetical.
     */
    private fun code(text: String): String {
        val out = StringBuilder(text)
        var index = 0
        while (index < text.length) {
            val end = when {
                text.startsWith("//", index) -> text.indexOf('\n', index).orEndAt(text.length)
                text.startsWith("/*", index) -> text.indexOf(END_COMMENT, index + 2).orEndAt(text.length, 2)
                text.startsWith(TRIPLE_QUOTE, index) ->
                    text.indexOf(TRIPLE_QUOTE, index + 3).orEndAt(text.length, 3)
                text[index] == '"' -> text.endOfLiteral(index, '"')
                text[index] == '\'' -> text.endOfLiteral(index, '\'')
                else -> -1
            }
            if (end < 0) {
                index++
            } else {
                // Newlines stay, because a location is counted in them.
                for (blanked in index until end) {
                    if (out[blanked] != '\n') out[blanked] = ' '
                }
                index = end
            }
        }
        return out.toString()
    }

    /** The end of the string or char literal starting at [start], its closing quote included. */
    private fun String.endOfLiteral(start: Int, quote: Char): Int {
        var index = start + 1
        while (index < length && this[index] != quote) index += if (this[index] == '\\') 2 else 1
        return if (index < length) index + 1 else length
    }

    /**
     * [this] when it is a found index, otherwise [length] - a comment or literal that runs to
     * the end of the text. Clamped to [length] even when it was found: an unterminated block
     * comment or raw string has no closing delimiter to add [tail] to, and the alternative to
     * blanking the rest is an index past the end of the buffer being blanked, thrown from
     * inside the helper with no hint of which file was being scanned.
     */
    private fun Int.orEndAt(length: Int, tail: Int = 0): Int =
        (if (this < 0) length else this + tail).coerceAtMost(length)

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

    private data class Call(val file: File, val line: Int, val arguments: String) {
        val location: String get() = "${file.name}:$line"
    }

    private companion object {
        const val HAIL_DATA_PATH = "com/aistra/hail/app/HailData.kt"
        const val HAIL_DATA_FILE_NAME = "HailData.kt"
        const val GET_FLOAT = "getFloat("
        const val FLOAT_DEFAULT = "floatDefault("
        const val DECLARED_FLOAT = "declaredFloat("
        const val SLIDER_PREFERENCE = "sliderPreference("
        const val TRIPLE_QUOTE = "\"\"\""
        const val END_COMMENT = "*/"
        val FUNCTION_DECLARATION = Regex("\\bfun\\s+$")
        val FIRST_ARGUMENT = Regex("\\A\\s*([\\w.]+)\\s*(?:,|$)")
    }
}
