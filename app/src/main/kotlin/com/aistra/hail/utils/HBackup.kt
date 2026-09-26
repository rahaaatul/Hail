package com.aistra.hail.utils

import android.content.Context
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
import java.io.FileOutputStream
import java.io.FileInputStream
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

object HBackup {

    data class BackupOptions(
        val apps: Boolean = false,
        val whitelist: Boolean = false,
        val actions: Boolean = false,
        val settings: Boolean = false
    )

    data class RestoreOptions(
        val apps: Boolean = false,
        val whitelist: Boolean = false,
        val actions: Boolean = false,
        val settings: Boolean = false
    )

    private const val FILE_APPS = "apps.json"
    private const val FILE_WHITELIST = "whitelist.json"
    private const val FILE_ACTIONS = "actions.json"
    private const val FILE_SETTINGS = "settings.json"

    private val INT_AS_LONG_RANGE = Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()

    // 2^53, where a Double stops naming every integer: a Double carries 53 bits of
    // significand, so an integral value at least this large cannot be carried by one, and a
    // Long preference reaching the reader by that route has already lost its low digits.
    private const val DOUBLE_EXACT_INTEGER_LIMIT = 9.007199254740992E15

    // The preference keys the app declares to be a Float. Both are written by
    // sliderPreference, so the type belongs to the key rather than to whatever value happens
    // to be stored under it, and a key listed here is a Float whatever an earlier build
    // managed to put there. HailData.getFloat names these same two keys and no others, so
    // this set is the whole of the app's Float surface and cannot fall behind it silently.
    private val FLOAT_PREFERENCE_KEYS = setOf(HailData.HOME_FONT_SIZE, HailData.AUTO_FREEZE_DELAY)

    suspend fun backup(
        context: Context,
        outputFile: File,
        options: BackupOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // File.mkdirs() returns false when the directory already exists, so only
            // treat a path that is neither an existing directory nor creatable as a failure.
            // The trailing isDirectory re-check covers another process creating the parent
            // between the check above and mkdirs().
            val parent = outputFile.parentFile
            if (parent != null && !parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
                throw IllegalStateException("Failed to create directory: $parent")
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

    suspend fun restore(
        context: Context,
        inputFile: File,
        options: RestoreOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
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
                    .put("unfreezePackages", JSONArray(action.unfreezePackages))
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
                else -> HLog.w("HBackup", "Unsupported preference type for key '$key': ${value?.javaClass?.simpleName}, skipping")
            }
        }
        writeEntry(zipOutputStream, FILE_SETTINGS, jsonObject.toString())
    }

    private fun writeEntry(zipOutputStream: ZipOutputStream, name: String, content: String) {
        zipOutputStream.putNextEntry(ZipEntry(name))
        zipOutputStream.write(content.toByteArray())
        zipOutputStream.closeEntry()
    }

    private fun readJsonString(bufferedStream: BufferedInputStream): String {
        val stringBuilder = StringBuilder()
        val buffer = ByteArray(8192)
        var bytesRead: Int
        while (bufferedStream.read(buffer).also { bytesRead = it } != -1) {
            stringBuilder.append(String(buffer, 0, bytesRead, Charset.forName("UTF-8")))
        }
        return stringBuilder.toString()
    }

    private fun readAppsJson(zipInputStream: ZipInputStream) {
        val bufferedStream = BufferedInputStream(zipInputStream, 8192)
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
        val bufferedStream = BufferedInputStream(zipInputStream, 8192)
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
        val bufferedStream = BufferedInputStream(zipInputStream, 8192)
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
        val bufferedStream = BufferedInputStream(zipInputStream, 8192)
        val jsonObject = JSONObject(readJsonString(bufferedStream))
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        // A numeric preference cannot be recognized from the JSON alone: JSONObject has no
        // Float, and it shaves the "." off a whole number, so a Float of 15.0f written as a
        // bare number parses back as an Integer. Three things can say what a key is, in
        // increasing order of authority:
        //
        //   1. the spelling writeSettingsJson gives a Float, which is a fixed point;
        //   2. the key itself, for the two keys the app declares as a Float;
        //   3. the type already recorded in SharedPreferences.
        //
        // The order matters, and (3) is deliberately last. A Float key that an earlier broken
        // restore stored as an Int is the state every #91 reporter is in today: the recorded
        // Int is not a fact about the preference, it is the damage, so a rule that dispatches
        // on it first believes the damage and reproduces it on every later restore. A repair
        // keyed off the recorded type can therefore never bootstrap - the evidence it would
        // have to trust is the very thing it has to repair. (3) still earns its place, because
        // a bare untagged whole number really is ambiguous between an Int and a Float, and for
        // a key that is neither declared a Float nor already recorded as one it is the only
        // thing left to consult.
        val recordedValues = sp.all
        sp.edit {
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                val recorded = recordedValues[key]
                val floatTag = (value as? String)?.toFloatTagOrNull()
                val declaredFloat = recorded is Float || key in FLOAT_PREFERENCE_KEYS
                val number = if (recorded is Number && !declaredFloat) value.toNumberOrNull() else null
                when {
                    // A Float, and deliberately reached ahead of the recorded-type lookup below.
                    // The recorded type is evidence, not authority: where it and the file
                    // disagree in a way a consistent writer could not have produced, the file
                    // wins, because the spelling is the only part of this that cannot be
                    // damage. The one recorded type that outranks the file is a String, which
                    // keeps its own type even when its content happens to be spelled like a
                    // Float - a genuine String preference is reported as itself by the very
                    // act of being one, and this build would have written the same text for
                    // it. That exception stops at a declared Float key: nothing in the app can
                    // store a String under one, so there it is damage too, and the pre-fix
                    // reader created exactly that by claiming the tag with putString.
                    declaredFloat || (floatTag != null && recorded !is String) -> {
                        val asFloat = floatTag ?: value.toNumberOrNull()?.toFloat()
                        if (asFloat != null && asFloat.isFinite()) {
                            putFloat(key, asFloat)
                        } else {
                            warnNotStorable(key, value, "Float")
                        }
                    }

                    // An untagged number for a key whose type the install already states. One
                    // range check for both numeric targets, rather than letting toInt()/toLong()
                    // decide: those saturate at the type's limits and truncate toward zero, so
                    // a value no preference can hold would be stored as a different but
                    // plausible looking one, with nothing logged. A skipped key leaves the
                    // stored value alone, which is the only outcome that cannot invent a
                    // preference the user never chose.
                    number != null -> {
                        val asLong = number.toExactLongOrNull()
                        when {
                            recorded is Int && asLong != null && asLong in INT_AS_LONG_RANGE ->
                                putInt(key, asLong.toInt())
                            recorded is Int -> warnNotStorable(key, value, "Int")
                            asLong != null -> putLong(key, asLong)
                            else -> warnNotStorable(key, value, "Long")
                        }
                    }

                    // Nothing recorded, and the value is not a Float, so the shape of the JSON
                    // value has to decide. A String this build wrote for a Float is a fixed
                    // point and was claimed above; a String keeps its own content here, and a
                    // bare "15" or "1e20" is a number JSONObject should have parsed, not a Float
                    // this build wrote.
                    value is String -> putString(key, value)
                    value is Boolean -> putBoolean(key, value)
                    value is JSONArray -> {
                        val stringSet = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            stringSet.add(value.getString(i))
                        }
                        putStringSet(key, stringSet)
                    }
                    // JSONObject hands back no Float, but keep these arms in step with
                    // writeSettingsJson so the two stay mirrored and a type is never dropped.
                    value is Float -> putFloat(key, value)
                    value is Int -> putInt(key, value)
                    value is Long -> putLong(key, value)
                    // A number for a key nothing is known about, so the file's own claim has
                    // to be taken at face value. Which Java class org.json hands back for a
                    // given literal differs between the two implementations this project can
                    // load - see isTooWideInteger - so nothing here may branch on the class.
                    // The two outcomes are decided by magnitude instead: an integer no
                    // preference can hold is skipped and logged rather than stored as a Float
                    // of the right order but the wrong digits, and anything else is a Float,
                    // because only a Float preference ever wrote decimal notation.
                    value is Number -> {
                        val asLong = value.toExactLongOrNull()
                        val asFloat = value.toFloat()
                        when {
                            value.isTooWideInteger() && asLong == null ->
                                warnNotStorable(key, value, "Long")
                            asLong != null -> putLong(key, asLong)
                            asFloat.isFinite() -> putFloat(key, asFloat)
                            else -> warnNotStorable(key, value, "Float")
                        }
                    }

                    else -> HLog.w("HBackup", "Unsupported preference type for key '$key': ${value?.javaClass?.simpleName}")
                }
            }
        }
    }

    private fun Any?.toNumberOrNull(): Number? = when (this) {
        is Number -> this
        is String -> toDoubleOrNull()
        else -> null
    }

    /**
     * This string as the [Float] it spells, or null when it does not spell one. Only
     * writeSettingsJson produces a number as a String, and it produces it as
     * [Float.toString], whose spelling is a fixed point: parsing it and printing it again
     * gives the same text. A String preference keeps its own content instead, so a value
     * that fails the round trip is a String, as is "15" and "1e20" - spellings this build
     * never writes for a Float, because a Float of 15.0f prints as "15.0" and 1.0E20f
     * prints as "1.0E20".
     */
    private fun String.toFloatTagOrNull(): Float? =
        toFloatOrNull()?.takeIf { it.toString() == this && it.isFinite() }

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
     * Whether this number is an integer too wide for any preference type, decided from its
     * magnitude rather than from its class. The class is not available as a discriminator
     * because the two org.json implementations this project can load disagree about it: a
     * BigInteger is only ever produced by the org.json:json artifact on the unit test
     * classpath, while AOSP's parser has answered an overflowing integer literal with a
     * Double in every release up to the one that adopted the BigDecimal/BigInteger parser.
     * Branching on the class is what let the JVM tests pin a path a device cannot take while
     * the same file silently stored a Float of a meaningless magnitude on one. The outcome
     * has to be the same either way, so it is keyed off the number instead: past 2^53 a
     * Double can no longer name every integer, so the digits are already gone and no
     * preference can hold what the file wrote.
     */
    private fun Number.isTooWideInteger(): Boolean = when (this) {
        is BigInteger -> true
        // A scale of zero or less is an integer literal; a positive scale is a fraction, which
        // is never too wide and is the only shape a Float ever wrote as a bare number.
        is BigDecimal -> scale() <= 0
        else -> {
            val asDouble = toDouble()
            asDouble.isFinite() &&
                (asDouble >= DOUBLE_EXACT_INTEGER_LIMIT || asDouble <= -DOUBLE_EXACT_INTEGER_LIMIT)
        }
    }

    private fun warnNotStorable(key: String, value: Any?, target: String) {
        HLog.w("HBackup", "Value '$value' for key '$key' cannot be stored as $target, skipping")
    }
}
