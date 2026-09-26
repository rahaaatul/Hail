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
                // string keeps the "." that marks the value as a Float.
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
        // bare number parses back as an Integer. The type already recorded in
        // SharedPreferences is authoritative, and using it also restores a backup taken by an
        // older build, whose untagged whole numbers still hit the type the key really has.
        val recordedValues = sp.all
        sp.edit {
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                val recorded = recordedValues[key]
                val number = if (recorded is Number) value.toNumberOrNull() else null
                when {
                    number != null -> when (recorded) {
                        is Float -> putFloat(key, number.toFloat())
                        is Int -> putInt(key, number.toInt())
                        else -> putLong(key, number.toLong())
                    }

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
                    // org.json returns a BigDecimal for decimal notation and a BigInteger for
                    // an integer wider than 64 bits. Only a Float preference ever wrote
                    // decimal notation, so an integral value is the BigInteger case.
                    value is Number -> if (value is BigInteger) {
                        putLong(key, value.toLong())
                    } else {
                        putFloat(key, value.toFloat())
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
}
