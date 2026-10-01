package com.aistra.hail.app

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

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
    fun `no value HailData initializes names the application`() {
        // The invariant behind `dir` being resolved on first use (HailData.kt:233), and the
        // only thing in this file that keeps it that way. An ordinary `val` is resolved while
        // the object initializes, so it captures whatever `app` was at the moment something
        // first touched HailData - which in the app is onCreate, and in a JVM test is whichever
        // mock that test class happened to install. Reverting `dir` to a plain `val` restores
        // exactly that cross-class ordering dependency, and nothing else in the suite notices:
        // the suites that would suffer all mock the calls that would reach the path.
        //
        // Stated over every eager initializer rather than over the declarations this branch
        // happens to have touched, so it also covers the next value somebody adds that needs
        // the application, and does not care how that value is spelled: a type annotation, any
        // modifier, an annotation on the same line as the keyword, `var`, any indentation, and a
        // value continued over several lines either by a trailing operator or a leading one, or
        // by a head with no value behind it yet. A check that recognises one spelling of an
        // eager initializer is a check on the spelling.
        //
        // A local inside a function is not looked at, and the difference is not tidiness: it
        // runs when the function runs, so `val path = app.filesDir` inside `saveAppsLocked` is
        // correct code, and reporting it would be reporting a rule the invariant does not have.
        // An `init` block is the other kind of block that is not a function body, and its values
        // are eager by definition, so it stays in scope.
        val source = hailDataSource()
        val declared = withoutFunctionBodies(withoutComments(source.readText()))
        // A declaration inside a parameter list is a parameter, not an initializer - and it
        // sits at eight spaces, so the pattern cannot tell it from a member by indentation
        // alone. The header it sits under can: a parameter list belongs to a `fun` or a class
        // header, and nothing else. "Inside some parenthesis" would be the wrong test twice
        // over - it is true of a member of an object expression passed as an argument, which
        // is exactly the thing to catch, and false of nothing that matters.
        val parameterised = parameters(code(declared))
        val eager = EAGER_PROPERTY.findAll(declared).mapNotNull { declaration ->
            if (parameterised[declaration.range.first]) return@mapNotNull null
            // A computed property runs its body on every access and captures nothing at
            // initialization, so `val sp: SharedPreferences get() = …` is not a hit. The
            // annotation does not change that, and both spellings are excluded here.
            if (GETTER.containsMatchIn(declaration.value)) return@mapNotNull null
            val initializer = initializerAfter(declared, declaration.range.last + 1)
            // A reference is either code or a template hole. A word inside a string's own text
            // is neither, so it takes both a strings-blanked and a strings-kept reading to
            // tell `"${'$'}{app.filesDir.path}/v1"` from "the app is frozen".
            val reference = APPLICATION.containsMatchIn(code(initializer)) ||
                    TEMPLATE_REFERENCE.containsMatchIn(initializer)
            if (reference) {
                "${source.name}:${lineOf(declared, declaration.range.first)} " +
                    "${declaration.value.trim()} ${initializer.trim()}"
            } else null
        }.toList()
        assertTrue(
            "these values capture the application while HailData initializes, so they keep " +
                "whatever was installed at that moment for every later test in the JVM: $eager",
            eager.isEmpty()
        )
    }

    /**
     * The initializer beginning at [index], which is just after an `=`, read over as many lines
     * as it takes: this file writes initializers one per line often enough that a
     * single-line reading is a hole rather than a simplification, and a hole in *this* check
     * is a `dir` that has gone back to being eager.
     */
    private fun initializerAfter(text: String, index: Int): String {
        var depth = 0
        var end = index
        while (end < text.length) {
            // A literal is stepped over as a unit, because a raw string spans lines and its
            // interior newlines are not the end of anything.
            val literal = literalEndingAt(text, end)
            if (literal >= 0) {
                end = literal
                continue
            }
            when (text[end]) {
                '(', '[', '{' -> depth++
                ')', ']', '}' -> depth--
                // A line ends the value unless the value is still open, has not started, or
                // the next line leads with an operator - which is this codebase's other way of
                // writing a long value, and the one a trailing-character rule cannot see.
                '\n' -> if (depth <= 0 && !awaitsRest(text.substring(index, end), text, end + 1)) {
                    return text.substring(index, end)
                }
            }
            end++
        }
        return text.substring(index)
    }

    /**
     * Whether the value read so far is finished, which is three ways of not being finished:
     * it ends in something a value cannot end in, the next line leads with an operator, or it
     * ends on a word that cannot end an expression - a head with no value behind it yet, or an
     * `else` whose branch has not arrived. All three are spellings this codebase uses for a
     * long initializer, and a check that misses one reports a `dir` as clean.
     *
     * The patterns run over the value with its literals blanked, so the text of a `String`
     * cannot decide where a value ends; only the final character is read raw, because a
     * literal that ends on a quote ends there.
     */
    private fun awaitsRest(value: String, text: String, nextLine: Int): Boolean {
        val last = value.trimEnd().lastOrNull() ?: return true
        if (last in UNFINISHED || leadsWithOperator(text, nextLine)) return true
        // A line that ends on a word which cannot end an expression - `else` with its branch
        // still to come - and a branch opened with no `else` seen yet, are the same fact.
        if (DANGLING_KEYWORD.containsMatchIn(code(value))) return true
        // A head with no value behind it yet: `if (x)` on its own line is a decision waiting
        // for its branches, and only `if` is closed by an `else` - a brace-less `when`, a
        // `try` and a `do` are closed by something else or by nothing at all, so pairing
        // them with `else` sent the read to the end of the file.
        return AWAITING_HEAD.containsMatchIn(code(value))
    }

    /**
     * Whether the next line to say anything continues the value, by starting with an operator
     * or an `else` - `context` on one line and `.getSharedPreferences(app.packageName, 0)` on
     * the next is one expression, and reading only the first line is how a real reference gets
     * missed by a check whose whole job is not to miss it.
     */
    private fun leadsWithOperator(text: String, from: Int): Boolean {
        var index = from
        while (index < text.length && text[index].isWhitespace()) index++
        if (index >= text.length) return false
        return text[index] in LEADING || text.startsWith(ELSE, index)
    }

    /**
     * [text] with the body of every function replaced by spaces, same length throughout.
     *
     * What is left is everything that can be resolved while the object initializes: its own
     * members, those of a nested object or class, and those of an `init` block.
     */
    private fun withoutFunctionBodies(text: String): String {
        val code = code(text)
        val blanked = StringBuilder(text)
        for (function in FUNCTIONS.findAll(code)) {
            val parameters = code.indexOf('(', function.range.last)
            val close = if (parameters < 0) null else matchingBracket(code, parameters)
            val open = code.indexOf(BRACE, function.range.last)
            if (open < 0 || close == null) continue
            // `fun f(x: Int): T = expression` has no brace of its own: the first `{` after the
            // keyword belongs to the expression, and blanking from there would hide a
            // declaration this test exists to see. The `=` after the parameter list says so,
            // and after the list rather than inside it, so a default argument does not.
            val assigned = code.indexOf('=', close)
            if (assigned in (close + 1) until open) continue
            val end = matchingBracket(code, open) ?: continue
            for (index in open..end) if (blanked[index] != '\n') blanked.setCharAt(index, ' ')
        }
        return blanked.toString()
    }

    /** The offset of the bracket closing the one at [open], or null when there is none. */
    private fun matchingBracket(text: String, open: Int): Int? {
        val close = when (text[open]) {
            '(' -> ')'
            '[' -> ']'
            else -> '}'
        }
        var depth = 0
        for (index in open until text.length) {
            when (text[index]) {
                text[open] -> depth++
                close -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        return null
    }

    /** For each character of [text], whether it is inside a function's or class's parameters. */
    private fun parameters(text: String): BooleanArray {
        val inside = BooleanArray(text.length)
        var depth = 0
        val opening = ArrayDeque<Int>()
        for (index in text.indices) {
            when (text[index]) {
                '(', '[' -> {
                    opening.addLast(index)
                    depth++
                }
                ')', ']' -> {
                    opening.removeLastOrNull()
                    depth--
                }
            }
            inside[index] = depth > 0 && opening.lastOrNull()?.let { isHeader(text, it) } == true
        }
        return inside
    }

    /**
     * Whether the parenthesis at [open] opens a parameter list, which is to say whether a
     * `fun` or a class header is what it belongs to - `fun range(from: Float, to: Float)`
     * and `private data class Slider(key: String, value: Float = 0f)`, but not the arguments
     * of `Config(`.
     */
    private fun isHeader(text: String, open: Int): Boolean {
        // The last HEADER_START match ending strictly before `open`: the start of the
        // header the `(` at `open` belongs to. Searching forward from `open - 1` used to
        // find the first match at or after it - almost always a `}` or `{` below the
        // declaration being tested - which put `start` past `open` and threw from
        // `text.substring(start, open)`.
        //
        // With no match at all before `open` the whole prefix is the candidate: `HEADER`
        // is anchored at the end, so only the tail decides, and a header that starts at
        // the very top of the file is then still recognised rather than its parameters
        // being reported as eager initializers.
        val start = HEADER_START.findAll(text)
            .takeWhile { it.range.last < open }
            .lastOrNull()?.range?.last?.plus(1) ?: 0
        return HEADER.containsMatchIn(text.substring(start, open))
    }

    /** The 1-based line [offset] falls on. */
    private fun lineOf(text: String, offset: Int): Int = text.take(offset).count { it == '\n' } + 1

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

    /**
     * The text after `name = ` on its first line, for a message that has to be readable.
     *
     * Read through [code] like every other search on this call, so a comment placed before
     * the argument cannot supply the quoted value and hide the one that actually failed.
     *
     * The masking erases a string literal, and an erased value is indistinguishable from an
     * absent one - so when the masked read comes back empty the raw text is quoted instead.
     * Without that, a value that *is* a literal would be reported as missing, which is the one
     * thing a failure message must not do. The residual is that a comment can be quoted in
     * exactly the case where there is no real value to quote, which is also the case the
     * message already says has no value.
     */
    private fun Call.valueOf(name: String): String {
        val afterName = Regex("\\b${Regex.escape(name)}\\s*=\\s*([^\\n]*)")
        val masked = afterName.find(code(arguments))?.groupValues?.get(1)?.trim()
        if (!masked.isNullOrEmpty()) return masked
        val raw = afterName.find(arguments)?.groupValues?.get(1)?.trim()
        return if (raw.isNullOrEmpty()) "no $name" else raw
    }

    /**
     * [text] with every comment replaced by spaces and every string literal kept, which is what
     * a search for a *name in a value* needs: `val dir = "${app.filesDir.path}/v1"` says `app`
     * only inside a string literal, so blanking strings erases the very thing being looked for.
     * A URL that happens to contain `app` is not a reference, which is what [APPLICATION] is
     * for. Only the bracket-counting paths may not use this.
     */
    private fun withoutComments(text: String): String = blank(text, keepStrings = true)

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
    private fun code(text: String): String = blank(text, keepStrings = false)

    /**
     * [text] with the regions named by the mode replaced by spaces, same length throughout and
     * with the newlines left in place so a location is still counted in them.
     *
     * A literal is stepped over as a unit in both modes - `keepStrings` decides whether its
     * text survives, not whether its contents are read as code. That is the difference between
     * masking a URL and losing everything after the two slashes in its scheme, and the flag
     * has to mean what it says: a kept literal that contains what would otherwise open a
     * comment is still a literal.
     */
    private fun blank(text: String, keepStrings: Boolean): String {
        val out = StringBuilder(text)
        var index = 0
        while (index < text.length) {
            val literal = literalEndingAt(text, index)
            if (literal >= 0) {
                if (!keepStrings) blankOut(out, index, literal)
                index = literal
                continue
            }
            val comment = commentEndingAt(text, index)
            if (comment >= 0) {
                blankOut(out, index, comment)
                index = comment
                continue
            }
            index++
        }
        return out.toString()
    }

    /** The end of the string, char or raw-string literal starting at [index], or -1. */
    private fun literalEndingAt(text: String, index: Int): Int = when {
        text.startsWith(TRIPLE_QUOTE, index) -> text.indexOf(TRIPLE_QUOTE, index + 3).orEndAt(text.length, 3)
        text[index] == '"' -> text.endOfLiteral(index, '"')
        text[index] == '\'' -> text.endOfLiteral(index, '\'')
        else -> -1
    }

    /** The end of the line or block comment starting at [index], or -1. */
    private fun commentEndingAt(text: String, index: Int): Int = when {
        text.startsWith("//", index) -> text.indexOf('\n', index).orEndAt(text.length)
        text.startsWith("/*", index) -> text.indexOf(END_COMMENT, index + 2).orEndAt(text.length, 2)
        else -> -1
    }

    private fun blankOut(out: StringBuilder, from: Int, to: Int) {
        for (index in from until minOf(to, out.length)) {
            if (out[index] != '\n') out[index] = ' '
        }
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
        private const val HAIL_DATA_PATH = "com/aistra/hail/app/HailData.kt"
        private const val HAIL_DATA_FILE_NAME = "HailData.kt"
        private const val GET_FLOAT = "getFloat("
        private const val FLOAT_DEFAULT = "floatDefault("
        private const val DECLARED_FLOAT = "declaredFloat("
        private const val SLIDER_PREFERENCE = "sliderPreference("
        private const val TRIPLE_QUOTE = "\"\"\""
        private const val END_COMMENT = "*/"
        private val FUNCTION_DECLARATION = Regex("\\bfun\\s+$")
        // Any indentation, because a nested object or class inside the file has its properties
        // deeper and a parameter list is told apart by its parentheses rather than by depth.
        // The type annotation cannot cross a line: bounded to one, a declaration with no `=` on
        // its own line simply does not match, instead of reaching down the file to whatever `=`
        // comes next and reporting a span that has nothing to do with it.
        private val EAGER_PROPERTY =
            Regex("^[ \\t]+(?:@[\\w.:]+(?:\\([^()]*\\))?[ \\t]+)*(?:\\w+ )*(?:val|var) \\w+(?:[ \\t]*:[^=\\n]*?)?[ \\t]*=(?!=)", RegexOption.MULTILINE)
        private val GETTER = Regex("\\bget\\s*\\(")
        // A reference to the application from inside a string template, which masking erases.
        private val TEMPLATE_REFERENCE = Regex("\\$\\{[^}]*?(?<![\\w/])app(?![\\w])")
        private val UNFINISHED = charArrayOf('(', '[', '{', ',', '=', '+', '-', '*', '/', '<', '>', '?', ':', '&', '|', '.', '\\')
        private val LEADING = charArrayOf('(', '[', '.', '+', '-', '*', '/', '%', '?', ':', '&', '|', '=', '<', '>', '!')
        private const val ELSE = "else"
        private val DANGLING_KEYWORD = Regex("\\b(?:else|if|when|try|do|return|throw|in|by)\\s*$")
        private const val BRACE = '{'
        private val FUNCTIONS = Regex("\\bfun\\b")
        private val AWAITING_HEAD = Regex("\\b(?:if|when|while)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*$")
        private val HEADER = Regex("(?:fun|class|interface|object|constructor)\\s+[\\w<>,.?: ]*$")
        private val HEADER_START = Regex("(?m)^[ \\t]*(?:@\\w[\\w.:]*(?:\\([^)\\n]*\\))?[ \\t]*)*$|[;{}]")
        // A name, not a substring: `app` in a URL or a longer identifier is not a reference.
        private val APPLICATION = Regex("(?<![\\w/])app(?![\\w])")
        private val FIRST_ARGUMENT = Regex("\\A\\s*([\\w.]+)\\s*(?:,|$)")
    }
}
