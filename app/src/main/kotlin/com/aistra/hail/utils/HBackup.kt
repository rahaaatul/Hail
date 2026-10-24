package com.aistra.hail.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.app.HailData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private const val BUFFER_SIZE = 8192

private fun writeEntry(zipOutputStream: ZipOutputStream, name: String, content: String) {
    zipOutputStream.putNextEntry(ZipEntry(name))
    zipOutputStream.write(content.toByteArray())
    zipOutputStream.closeEntry()
}

private fun readJsonString(bufferedStream: BufferedInputStream): String {
    val stringBuilder = StringBuilder()
    val buffer = ByteArray(BUFFER_SIZE)
    var bytesRead: Int
    while (bufferedStream.read(buffer).also { bytesRead = it } != -1) {
        stringBuilder.append(String(buffer, 0, bytesRead, Charset.forName("UTF-8")))
    }
    return stringBuilder.toString()
}

private fun warnNotStorable(key: String, value: Any?, target: String) {
    HLog.w("HBackup", "Value '$value' for key '$key' cannot be stored as $target, skipping")
}

private fun Any?.toNumberOrNull(): Number? = when (this) {
    is Number -> this
    is String -> toDoubleOrNull()
    else -> null
}

private fun String.toFloatTagOrNull(): Float? = toFloatOrNull()?.takeIf { it.toString() == this && it.isFinite() }

object HBackup {

    data class BackupOptions(
        val apps: Boolean = false,
        val whitelist: Boolean = false,
        val actions: Boolean = false,
        val settings: Boolean = false,
    )

    data class RestoreOptions(
        val apps: Boolean = false,
        val whitelist: Boolean = false,
        val actions: Boolean = false,
        val settings: Boolean = false,
    )

    private const val FILE_APPS = "apps.json"
    private const val FILE_WHITELIST = "whitelist.json"
    private const val FILE_ACTIONS = "actions.json"
    private const val FILE_SETTINGS = "settings.json"

    private val INT_AS_LONG_RANGE = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()

    // 2^53, where a Double stops naming every integer. A Double carries 53 bits of
    // significand, so from here upward it cannot name every integer in the range: 2^53 is
    // itself held exactly, and 2^53+1 is the same Double. Every double between 2^53 and
    // the top of the Long range is still a whole number, so this is not about
    // unrepresentability. It is that a value at
    // or past 2^53 may already have been rounded by the parser that produced it, and nothing
    // left here can tell "the file wrote 2^53" from "the file wrote 2^53+1 and it rounded
    // down". The cut therefore includes 2^53 itself, which putLong could hold: the round
    // trip is the thing that cannot be undone, so it is the point where restoring is
    // refused rather than guessed.
    private const val DOUBLE_EXACT_INTEGER_LIMIT = 9.007199254740992E15

    // The preference keys the app declares to be a Float, with the range each allows.
    // Both are written by sliderPreference, so the type belongs to the key rather than to
    // whatever value happens to be stored under it, and a key listed here is a Float
    // whatever an earlier build managed to put there. The declaration lives in HailData
    // because the settings screen and the app's own getters read their ranges and their
    // defaults out of the same map: a Float preference cannot be declared on either side
    // of it without it, so this reader's idea of the Float surface is the app's.
    private val FLOAT_PREFERENCES = HailData.FLOAT_PREFERENCES

    private val FLOAT_PREFERENCE_KEYS = FLOAT_PREFERENCES.keys

    suspend fun backup(context: Context, outputFile: File, options: BackupOptions): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                // File.mkdirs() returns false when the directory already exists, so only
                // treat a path that is neither an existing directory nor creatable as a failure.
                // The trailing isDirectory re-check covers another process creating the parent
                // between the check above and mkdirs().
                val parent = outputFile.parentFile
                if (parent != null) {
                    check(parent.isDirectory || parent.mkdirs()) { "Failed to create directory: $parent" }
                }
                val zipOutputStream = ZipOutputStream(FileOutputStream(outputFile))
                try {
                    if (options.apps) {
                        writeAppsJson(zipOutputStream)
                    }
                    if (options.whitelist) {
                        writeWhitelistJson(zipOutputStream)
                    }
                    if (options.actions) {
                        val actions = ActionsRepository.loadAll()
                        writeActionsJson(zipOutputStream, actions)
                    }
                    if (options.settings) {
                        writeSettingsJson(context, zipOutputStream)
                    }
                } finally {
                    zipOutputStream.close()
                }
            }
        }

    suspend fun restore(context: Context, inputFile: File, options: RestoreOptions): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val zipInputStream = ZipInputStream(FileInputStream(inputFile))
                try {
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        when (name) {
                            FILE_APPS -> if (options.apps) readAppsJson(zipInputStream)
                            FILE_WHITELIST -> if (options.whitelist) readWhitelistJson(zipInputStream)
                            FILE_ACTIONS -> if (options.actions) readActionsJson(zipInputStream)
                            FILE_SETTINGS -> if (options.settings) readSettingsJson(context, zipInputStream)
                        }
                        zipInputStream.closeEntry()
                        entry = zipInputStream.nextEntry
                    }
                } finally {
                    zipInputStream.close()
                }
            }
        }

    private fun writeAppsJson(zipOutputStream: ZipOutputStream) {
        val jsonArray = JSONArray()
        HailData.checkedList.forEach { appInfo ->
            jsonArray.put(appInfo.packageName)
        }
        writeEntry(zipOutputStream, FILE_APPS, jsonArray.toString())
    }

    private fun writeWhitelistJson(zipOutputStream: ZipOutputStream) {
        val jsonArray = JSONArray()
        HailData.checkedList
            .filter { it.whitelisted }
            .forEach { appInfo ->
                jsonArray.put(appInfo.packageName)
            }
        writeEntry(zipOutputStream, FILE_WHITELIST, jsonArray.toString())
    }

    private fun writeActionsJson(zipOutputStream: ZipOutputStream, actions: List<LaunchAction>) {
        val jsonArray = JSONArray()
        actions.forEach { action ->
            jsonArray.put(
                JSONObject()
                    .put("id", action.id)
                    .put("launchPackage", action.launchPackage)
                    .put("unfreezePackages", JSONArray(action.unfreezePackages)),
            )
        }
        writeEntry(zipOutputStream, FILE_ACTIONS, jsonArray.toString())
    }

    private fun writeSettingsJson(context: Context, zipOutputStream: ZipOutputStream) {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val jsonObject = JSONObject()
        sp.all.forEach { (key, value) ->
            when (value) {
                is String -> jsonObject.put(key, value)

                is Int -> jsonObject.put(key, value)

                is Long -> jsonObject.put(key, value)

                // JSONObject.numberToString shaves trailing zeros and then a trailing decimal
                // point, so 14.0f is written as 14 and comes back as an Integer. The decimal
                // string keeps the "." that marks the value as a Float, and it is the whole of
                // the format change: settings.json stays a flat name/value map, and a build
                // that predates this one restores a tagged value as a String under a Float key,
                // which the next restore on a fixed build repairs rather than compounding -
                // readSettingsJson resolves a declared Float key from the key, not from what
                // the install happens to have recorded for it. A {"value":..,"type":..}
                // envelope would make the file self-describing at the cost of widening the
                // format for a file no third party reads, and it would still not remove the
                // untagged branch, which every backup written before it has to keep parsing.
                is Float -> jsonObject.put(key, value.toString())

                is Boolean -> jsonObject.put(key, value)

                is Set<*> -> jsonObject.put(key, JSONArray(value.map { it.toString() }))

                else -> HLog.w(
                    "HBackup",
                    "Unsupported preference type for key '$key': ${value?.javaClass?.simpleName}, skipping",
                )
            }
        }
        writeEntry(zipOutputStream, FILE_SETTINGS, jsonObject.toString())
    }

    private fun readAppsJson(zipInputStream: ZipInputStream) {
        val bufferedStream = BufferedInputStream(zipInputStream, BUFFER_SIZE)
        val jsonArray = JSONArray(readJsonString(bufferedStream))
        for (i in 0 until jsonArray.length()) {
            val pkg = jsonArray.getString(i)
            if (!HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, 0, false)
            }
        }
        HailData.saveApps()
    }

    private fun readWhitelistJson(zipInputStream: ZipInputStream) {
        val bufferedStream = BufferedInputStream(zipInputStream, BUFFER_SIZE)
        val jsonArray = JSONArray(readJsonString(bufferedStream))
        for (i in 0 until jsonArray.length()) {
            val pkg = jsonArray.getString(i)
            if (!HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, 0, false)
            }
            HailData.checkedList.firstOrNull { it.packageName == pkg }?.whitelisted = true
        }
        HailData.saveApps()
    }

    private suspend fun readActionsJson(zipInputStream: ZipInputStream) {
        val bufferedStream = BufferedInputStream(zipInputStream, BUFFER_SIZE)
        val jsonArray = JSONArray(readJsonString(bufferedStream))
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val id = obj.getString("id")
            val launchPackage = obj.getString("launchPackage")
            val unfreezePackages = obj.getJSONArray("unfreezePackages").let {
                (0 until it.length()).map { idx -> it.getString(idx) }
            }
            ActionsRepository.save(id, launchPackage, unfreezePackages)
        }
    }

    private fun readSettingsJson(context: Context, zipInputStream: ZipInputStream) {
        val bufferedStream = BufferedInputStream(zipInputStream, BUFFER_SIZE)
        val jsonObject = JSONObject(readJsonString(bufferedStream))
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val recordedValues = sp.all
        sp.edit {
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                restorePreference(this, key, value, recordedValues)
            }
        }
    }

    private fun restorePreference(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?,
        recordedValues: Map<String, Any?>,
    ) {
        val recorded = recordedValues[key]
        val floatTag = (value as? String)?.toFloatTagOrNull()
        val declaredFloat = recorded is Float || key in FLOAT_PREFERENCE_KEYS
        val number = if (recorded is Number && !declaredFloat) value.toNumberOrNull() else null
        if (declaredFloat || (floatTag != null && recorded !is String)) {
            restoreAsFloat(editor, key, value, floatTag)
        } else if (number != null) {
            restoreAsRecordedNumber(editor, key, value, number, recorded)
        } else {
            restoreByType(editor, key, value)
        }
    }

    private fun restoreAsFloat(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?,
        floatTag: Float?,
    ) {
        val asFloat = floatTag ?: value.toNumberOrNull()?.toFloat()
        val range = FLOAT_PREFERENCES[key]?.range
        when {
            asFloat == null || !asFloat.isFinite() -> warnNotStorable(key, value, "Float")

            floatTag == null && isMagnitudeTooWide(asFloat.toDouble()) ->
                warnNotStorable(key, value, "Float")

            range != null && asFloat !in range ->
                warnNotStorable(key, value, "Float in ${range.start}..${range.endInclusive}")

            else -> editor.putFloat(key, asFloat)
        }
    }

    private fun restoreAsRecordedNumber(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?,
        number: Number,
        recorded: Any?,
    ) {
        val asLong = number.toExactLongOrNull()
        when {
            recorded is Int && asLong != null && asLong in INT_AS_LONG_RANGE ->
                editor.putInt(key, asLong.toInt())

            recorded is Int -> warnNotStorable(key, value, "Int")

            asLong != null -> editor.putLong(key, asLong)

            else -> warnNotStorable(key, value, "Long")
        }
    }

    private fun restoreByType(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?,
    ) {
        when (value) {
            is String -> editor.putString(key, value)

            is Boolean -> editor.putBoolean(key, value)

            is JSONArray -> restoreStringSet(editor, key, value)

            is Float -> editor.putFloat(key, value)

            is Int -> editor.putInt(key, value)

            is Long -> editor.putLong(key, value)

            is Number -> restoreAsUnknownNumber(editor, key, value)

            else -> HLog.w(
                "HBackup",
                "Unsupported preference type for key '$key': ${value.javaClass.simpleName}",
            )
        }
    }

    private fun restoreStringSet(
        editor: SharedPreferences.Editor,
        key: String,
        value: JSONArray,
    ) {
        val stringSet = mutableSetOf<String>()
        for (i in 0 until value.length()) {
            stringSet.add(value.getString(i))
        }
        editor.putStringSet(key, stringSet)
    }

    private fun restoreAsUnknownNumber(
        editor: SharedPreferences.Editor,
        key: String,
        value: Number,
    ) {
        val asLong = value.toExactLongOrNull()
        val asFloat = value.toFloat()
        when {
            value.isTooWideInteger() && asLong == null -> warnNotStorable(key, value, "Long")

            asLong != null -> editor.putLong(key, asLong)

            asFloat.isFinite() -> editor.putFloat(key, asFloat)

            else -> warnNotStorable(key, value, "Float")
        }
    }

    /**
     * This number as an exact [Long], or null when it is not one: a fraction, a NaN, an
     * infinity, or a magnitude a Long cannot hold. [Number.toLong] is unusable for the job
     * because it saturates at [Long.MIN_VALUE]/[Long.MAX_VALUE] and truncates toward zero, so
     * an unrepresentable value comes back looking like a legitimate one.
     */
    private fun Number.toExactLongOrNull(): Long? = when (this) {
        // Both throw ArithmeticException rather than truncating or saturating.
        is BigInteger -> try {
            longValueExact()
        } catch (_: ArithmeticException) {
            null
        }

        is BigDecimal -> try {
            longValueExact()
        } catch (_: ArithmeticException) {
            null
        }

        // An integral type needs no range check at all, because it is already a Long or
        // narrower and a Long preference can hold every Long there is. Routing one through
        // toDouble() is what made the top of the range unrestorable: Long.MAX_VALUE rounds up
        // to 2^63 as a Double, which is one past the end, so the value was rejected as too
        // wide after the round trip that was supposed to confirm it.
        is Long -> this

        is Int, is Short, is Byte -> toLong()

        // A Double is the one numeric type that can be in range and still be the wrong
        // number, because it carries 53 bits of significand. Past 2^53 it can no longer name
        // every integer, so an integral value that large is not necessarily the integer the
        // file wrote: 2^53+1 and 2^53 are the same Double, and by the time the value arrives
        // there is nothing here left to tell them apart. The digits are gone, so there is
        // nothing to restore, and writing the value that did survive would invent a number the
        // user never chose - so the value is skipped. The round trip below then rejects the
        // fractions, which is the only other thing a Double can be that a Long cannot.
        else -> {
            val asDouble = toDouble()
            when {
                !asDouble.isFinite() -> null

                asDouble <= -DOUBLE_EXACT_INTEGER_LIMIT || asDouble >= DOUBLE_EXACT_INTEGER_LIMIT -> null

                // Below 2^53 every Double is a Long exactly, so the Long range needs no check
                // of its own and the round trip is left to reject the fractions.
                else -> asDouble.toLong().takeIf { it.toDouble() == asDouble }
            }
        }
    }

    /**
     * Whether a magnitude has already lost digits, whatever produced it.
     *
     * A Double carries 53 bits of significand, so from 2^53 upward it can no longer
     * name every integer: 2^53+1 and 2^53 are the same Double. A value that has
     * reached this function at that magnitude is therefore no longer the number the
     * file wrote.
     *
     * A non-finite magnitude is too wide as well, because no preference can hold one and
     * [Number.toExactLongOrNull] already refuses it. That is reachable from a real file
     * rather than from a test: both parsers that read a literal as a number - the
     * org.json:json artifact and the AOSP parser that adopted BigDecimal/BigInteger - hand
     * 1E1000 back as a BigDecimal whose toDouble() is Infinity, so a guard that required a
     * finite value waved through the one magnitude nothing can hold, and the Float arm then
     * refused the file's integer under a "Float" target. AOSP releases older than that
     * parser do not produce a Double for 1E1000 at all - they guard against a non-finite
     * parse and hand the raw text back as a String - and a String is not a magnitude this
     * function can be asked about, so that difference belongs to the parser rather than to
     * the check.
     *
     * Internal rather than private so the unit tests can exercise it directly, which is
     * the only way to pin what a device's parser decides: org.json never hands a Double to
     * [Number.isTooWideInteger], so on the unit test classpath that answer is reachable
     * only by calling this.
     */
    internal fun isMagnitudeTooWide(asDouble: Double): Boolean = !asDouble.isFinite() ||
        asDouble >= DOUBLE_EXACT_INTEGER_LIMIT ||
        asDouble <= -DOUBLE_EXACT_INTEGER_LIMIT

    /**
     * Whether this number is an integer too wide for any preference type, decided from its
     * magnitude rather than from its class. The class is not available as a discriminator
     * because the two org.json implementations this project can load disagree about it: a
     * BigInteger is only ever produced by the org.json:json artifact on the unit test
     * classpath, while AOSP's parser has answered an overflowing integer literal with a
     * Double in every release up to the one that adopted the BigDecimal/BigInteger parser.
     * Branching on the class is what let the JVM tests pin a path a device cannot take while
     * the same file silently stored a Float of a meaningless magnitude on one. The outcome
     * has to be the same either way, so all three arms are keyed off the magnitude.
     *
     * The BigInteger arm is unconditionally true, and it is only correct in combination
     * with the call site's `asLong == null`: a BigInteger is an integer, so it is either
     * one a Long holds - which [Number.toExactLongOrNull] answers, and the caller then
     * stores - or wider than a Long, and nothing can hold it. Read on its own this arm
     * would call 10000000000000000 too wide, which is a Long the app stores verbatim. It
     * must not become a magnitude comparison either: 2^60 is holdable by a Long but past
     * the 2^53 cut, and routing it through [isMagnitudeTooWide] would refuse a value the
     * Long arm stores exactly.
     *
     * A BigDecimal is asked what a device's parser would have made of the same literal,
     * which is two questions and not one. Going through toDouble() is lossy, and the
     * direction it loses in decides the band that sits just below the cut:
     * 9007199254740991.5 is the midpoint between two adjacent doubles, so a device that
     * parsed the file as a Double holds 2^53 and refuses it, while comparing the BigDecimal
     * exactly says "below the cut" and lets it through to putFloat(9.007199E15f) - the
     * file's last digit dropped, with nothing logged. Asking the Double first closes that.
     * The same loss in the other direction is the overflow the Double cannot express:
     * 1E1000 is Infinity rather than a separable BigDecimal, and a check built on the
     * exact value stops discriminating exactly there.
     *
     * The second question is whether that Double still names the number the file wrote.
     * From 2^52 upward a Double names whole numbers only, so a literal that far out with a
     * fractional part is a number no Double can print: the device that parsed it has
     * already stored the whole number it rounded to, and the digits the file held are gone
     * here too. Below 2^52 there is room for the fraction - 1.5, 1048576.5 - and those have
     * to survive. Comparing the file's decimal against the decimal that Double.toString
     * prints answers both sides at once, and that is what keeps this arm the conservative
     * one everywhere rather than only in the band above the cut.
     *
     * The check is one-sided on purpose, because it can only be. A value that arrived as a
     * Double no longer carries the file's text, so on that side there is nothing left to
     * tell "the file wrote 4503599627370496" from "the file wrote 4503599627370496.5 and
     * the parser rounded it", and refusing every Double at that magnitude would refuse
     * every Long from 2^52 to Long.MAX_VALUE - including the one restore keeps working
     * here. What this side has instead is the text, so this side refuses; a device that
     * has already lost the digits cannot be argued with from here.
     */
    private fun Number.isTooWideInteger(): Boolean = when (this) {
        is BigInteger -> true

        is BigDecimal -> {
            val magnitude = abs()
            val asDouble = magnitude.toDouble()
            // isMagnitudeTooWide first: a non-finite magnitude is refused there, and
            // BigDecimal(Double.toString()) below would throw on an infinity.
            isMagnitudeTooWide(asDouble) ||
                // Double.toString is the shortest decimal that names that Double exactly,
                // so this compares the file's decimal with the number a device would hold
                // rather than with the binary expansion behind it. compareTo rather than
                // equals, because 1.0E20 and 1E20 are the same number spelled differently.
                BigDecimal(asDouble.toString()).compareTo(magnitude) != 0
        }

        else -> isMagnitudeTooWide(toDouble())
    }
}
