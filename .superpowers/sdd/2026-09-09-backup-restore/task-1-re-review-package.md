# Task 1 Fix Round 1 Re-Review Package

## Plan Reference
`docs/superpowers/plans/2026-09-09-backup-restore.md`

## Task Brief
`.superpowers/sdd/2026-09-09-backup-restore/task-1-brief.md`

## Report
`.superpowers/sdd/2026-09-09-backup-restore/task-1-report.md`

## Open Findings to Verify

1. **Critical — Spec deviation (JSON format):** `apps.json` and `whitelist.json` must be simple JSON arrays of package name strings, like `["com.example.app1","com.example.app2"]`. The current implementation uses objects with `"packageName"` keys: `[{"packageName":"com.example.app1"},...]`. Fix `HBackup.kt` lines:
   - `writeAppsJson` (line ~127): change `jsonArray.put(JSONObject().put("packageName", appInfo.packageName))` to `jsonArray.put(appInfo.packageName)`
   - `writeWhitelistJson` (line ~137): same change
   - `readAppsJson` (line ~183): change `jsonArray.getJSONObject(i).getString("packageName")` to `jsonArray.getString(i)`
   - `readWhitelistJson` (line ~195): same change

2. **Important — Unused mock:** In `HBackupTest.kt`, the `backup handles empty checked list` test creates `mockSp` but never injects it into `PreferenceManager.getDefaultSharedPreferences()`. Either remove the unused mock or inject it properly via `mockkObject` + `mockkStatic` / `mockkConstructor` on `PreferenceManager`.

3. **Minor — Transcription artifact:** In the review package at `HBackupTest.kt:286`, there's `System.currentTimeTimeMillis()` (double "Time"). Fix to `System.currentTimeMillis()` if present in the actual file.

## Diff (bfb422e..4767e13)

```diff
diff --git a/app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt b/app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt
index e6c8a7d..5a5ec6d 100644
--- a/app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt
+++ b/app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt
@@ -93,7 +93,7 @@ object HBackup {
     private fun writeAppsJson(zipOutputStream: ZipOutputStream) {
         val jsonArray = JSONArray()
         HailData.checkedList.forEach { appInfo ->
-            jsonArray.put(JSONObject().put("packageName", appInfo.packageName))
+            jsonArray.put(appInfo.packageName)
         }
         writeEntry(zipOutputStream, FILE_APPS, jsonArray.toString())
     }
@@ -103,7 +103,7 @@ object HBackup {
         HailData.checkedList
             .filter { it.whitelisted }
             .forEach { appInfo ->
-                jsonArray.put(JSONObject().put("packageName", appInfo.packageName))
+                jsonArray.put(appInfo.packageName)
             }
         writeEntry(zipOutputStream, FILE_WHITELIST, jsonArray.toString())
     }
@@ -149,7 +149,7 @@ object HBackup {
         val jsonString = zipInputStream.readAllBytes().toString(StandardCharsets.UTF_8)
         val jsonArray = JSONArray(jsonString)
         for (i in 0 until jsonArray.length()) {
-            val pkg = jsonArray.getJSONObject(i).getString("packageName")
+            val pkg = jsonArray.getString(i)
             if (!HailData.isChecked(pkg)) {
                 HailData.addCheckedApp(pkg, 0, false)
             }
         }
         HailData.saveApps()
     }
@@ -161,7 +161,7 @@ object HBackup {
         val jsonString = zipInputStream.readAllBytes().toString(StandardCharsets.UTF_8)
         val jsonArray = JSONArray(jsonString)
         for (i in 0 until jsonArray.length()) {
-            val pkg = jsonArray.getJSONObject(i).getString("packageName")
+            val pkg = jsonArray.getString(i)
             HailData.checkedList.firstOrNull { it.packageName == pkg }?.let {
                 it.whitelisted = true
             }
         }
         HailData.saveApps()
     }
diff --git a/app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt b/app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt
index 65dbdf2..c39b5b5 100644
--- a/app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt
+++ b/app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt
@@ -1,7 +1,6 @@
 package com.aistra.hail.utils
 
 import android.content.Context
-import android.content.SharedPreferences
 import com.aistra.hail.HailApp
 import com.aistra.hail.app.HailData
 import com.aistra.hail.app.AppInfo
@@ -79,8 +78,8 @@ class HBackupTest {
                 val jsonString = String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                 val jsonArray = JSONArray(jsonString)
                 assertEquals(2, jsonArray.length())
-                assertEquals("com.example.app1", jsonArray.getJSONObject(0).getString("packageName"))
-                assertEquals("com.example.app2", jsonArray.getJSONObject(1).getString("packageName"))
+                assertEquals("com.example.app1", jsonArray.getString(0))
+                assertEquals("com.example.app2", jsonArray.getString(1))
             }
             zipInputStream.closeEntry()
             entry = zipInputStream.nextEntry
         }
         zipInputStream.close()
         assertTrue(foundAppsJson)
     }
@@ -113,8 +112,8 @@ class HBackupTest {
                 val jsonString = String(zipInputStream.readAllBytes(), StandardCharsets.UTF_8)
                 val jsonArray = JSONArray(jsonString)
                 assertEquals(2, jsonArray.length())
-                assertEquals("com.example.app2", jsonArray.getJSONObject(0).getString("packageName"))
-                assertEquals("com.example.app3", jsonArray.getJSONObject(1).getString("packageName"))
+                assertEquals("com.example.app2", jsonArray.getString(0))
+                assertEquals("com.example.app3", jsonArray.getString(1))
             }
             zipInputStream.closeEntry()
             entry = zipInputStream.nextEntry
         }
         zipInputStream.close()
         assertTrue(foundWhitelistJson)
     }
@@ -128,7 +127,7 @@ class HBackupTest {
         // Create a temp ZIP with apps.json
         val zipFile = File(System.getProperty("java.io.tmpdir"), "restore-test-${System.currentTimeMillis()}.zip")
         val zipOutputStream = ZipOutputStream(FileOutputStream(zipFile))
-        val jsonString = """[{"packageName":"com.example.app1"},{"packageName":"com.example.app2"}]"""
+        val jsonString = """["com.example.app1","com.example.app2"]"""
         zipOutputStream.putNextEntry(ZipEntry("apps.json"))
         zipOutputStream.write(jsonString.toByteArray(StandardCharsets.UTF_8))
         zipOutputStream.closeEntry()
         zipOutputStream.close()
@@ -166,9 +165,6 @@ class HBackupTest {
     fun `backup handles empty checked list`() = runTest {
         every { HailData.checkedList } returns mutableListOf()
 
-        val mockSp = mockk<SharedPreferences>(relaxed = true)
-        every { mockSp.all } returns mapOf()
-
         val outputFile = File(System.getProperty("java.io.tmpdir"), "backup-test-${System.currentTimeMillis()}.zip")
         val options = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true)
 
         val result = HBackup.backup(HailApp.app, outputFile, options)
 
         assertTrue(result.isSuccess)
     }
```
