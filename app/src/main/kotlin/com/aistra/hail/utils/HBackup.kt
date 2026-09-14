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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipInputStream

object HBackup {

    data class BackupOptions(
        val apps: Boolean = true,
        val whitelist: Boolean = true,
        val actions: Boolean = true,
        val settings: Boolean = true
    )

    data class RestoreOptions(
        val apps: Boolean = true,
        val whitelist: Boolean = true,
        val actions: Boolean = true,
        val settings: Boolean = true
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
        try {
            val parent = outputFile.parentFile
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw IOException("Failed to create parent directories: ${parent.absolutePath}")
            }
            ZipOutputStream(FileOutputStream(outputFile)).use { zipOutputStream ->
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
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
    }

    suspend fun restore(
        context: Context,
        inputFile: File,
        options: RestoreOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ZipInputStream(FileInputStream(inputFile)).use { zipInputStream ->
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
            }
            Result.success(Unit)
        } catch (e: Throwable) {
            Result.failure(e)
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
                is Float -> jsonObject.put(key, value)
                is Boolean -> jsonObject.put(key, value)
                // SharedPreferences only stores Set<String>; non-String elements are
                // serialized via toString() and restored as Strings on readSettingsJson.
                is Set<*> -> jsonObject.put(key, JSONArray(value.map { it.toString() }))
                else -> jsonObject.put(key, value.toString())
            }
        }
        writeEntry(zipOutputStream, FILE_SETTINGS, jsonObject.toString())
    }

    private fun writeEntry(zipOutputStream: ZipOutputStream, name: String, content: String) {
        zipOutputStream.putNextEntry(ZipEntry(name))
        zipOutputStream.write(content.toByteArray())
        zipOutputStream.closeEntry()
    }

    private fun readAllBytes(inputStream: InputStream): ByteArray {
        val buffer = ByteArrayOutputStream()
        val data = ByteArray(1024)
        var count = inputStream.read(data)
        while (count != -1) {
            buffer.write(data, 0, count)
            count = inputStream.read(data)
        }
        return buffer.toByteArray()
    }

    private fun readAppsJson(zipInputStream: ZipInputStream) {
        val jsonString = readAllBytes(zipInputStream).toString(StandardCharsets.UTF_8)
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val pkg = jsonArray.getString(i)
            if (!HailData.isChecked(pkg)) {
                HailData.addCheckedApp(pkg, 0, false)
            }
        }
        HailData.saveApps()
    }

    private fun readWhitelistJson(zipInputStream: ZipInputStream) {
        val jsonString = readAllBytes(zipInputStream).toString(StandardCharsets.UTF_8)
        val jsonArray = JSONArray(jsonString)
        for (i in 0 until jsonArray.length()) {
            val pkg = jsonArray.getString(i)
            HailData.checkedList.firstOrNull { it.packageName == pkg }?.let {
                it.whitelisted = true
            }
        }
        HailData.saveApps()
    }

    private suspend fun readActionsJson(zipInputStream: ZipInputStream) {
        val jsonString = readAllBytes(zipInputStream).toString(StandardCharsets.UTF_8)
        val jsonArray = JSONArray(jsonString)
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
        val jsonString = readAllBytes(zipInputStream).toString(StandardCharsets.UTF_8)
        val jsonObject = JSONObject(jsonString)
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        val unsupportedKeys = mutableListOf<String>()
        sp.edit {
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val value = jsonObject.get(key)
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Boolean -> putBoolean(key, value)
                    is JSONArray -> {
                        val stringSet = mutableSetOf<String>()
                        for (i in 0 until value.length()) {
                            stringSet.add(value.getString(i))
                        }
                        putStringSet(key, stringSet)
                    }
                    else -> unsupportedKeys.add(key)
                }
            }
        }
        if (unsupportedKeys.isNotEmpty()) {
            throw IOException("Unsupported preference type for keys: ${unsupportedKeys.joinToString()}")
        }
    }
}