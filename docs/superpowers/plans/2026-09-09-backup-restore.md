# Backup/Restore Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add backup/restore functionality to Hail that exports/imports apps, actions, settings, and whitelist via a ZIP file

**Architecture:** New `HBackup` utility class handles data serialization to/from ZIP. `PagerFragment` gets backup/restore menu items with checkbox dialogs. Backup shows dialog first then folder picker; restore shows folder picker first then dialog.

**Tech Stack:** Kotlin, `java.util.zip`, SharedPreferences, Room (via ActionsRepository), JSON, Storage Access Framework (`CreateDocument`/`OpenDocument`)

**Spec:** Conversation-derived design: ZIP with `apps.json`, `actions.json`, `settings.json`, `whitelist.json` at root level. Package names only, no state/metadata, no icons. settings.json includes all SharedPreferences.

## Global Constraints

- Min SDK 23, Target SDK 36, Compile SDK 37
- Use `java.util.zip` (no new dependencies)
- Follow existing patterns: `MaterialAlertDialogBuilder`, `registerForActivityResult`, `lifecycleScope.launch`, `HFiles` utility
- Tests use JUnit 4 + MockK + `runTest`
- Backup flow: Dialog → Folder picker; Restore flow: Folder picker → Dialog

---

### Task 1: Create HBackup Utility Class

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt`
- Test: `app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt`

**Interfaces:**
- Consumes: `HailData.checkedList`, `ActionsRepository.loadAll()`, `PreferenceManager.getDefaultSharedPreferences()`
- Produces: `suspend fun HBackup.backup(outputFile: File, options: BackupOptions): Result<Unit>` and `suspend fun HBackup.restore(inputFile: File, options: RestoreOptions): Result<Unit>`

- [ ] **Step 1: Write the failing test**

`app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt`:
```kotlin
package com.aistra.hail.utils

import android.content.Context
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class HBackupTest {
    @Before
    fun setUp() {
        val mockApp = mockk<HailApp>(relaxed = true)
        every { mockApp.filesDir } returns File(System.getProperty("java.io.tmpdir"), "hail-test-${System.currentTimeMillis()}")
        every { mockApp.packageManager } returns mockk(relaxed = true)
        HailApp.setAppForTest(mockApp)

        mockkObject(HailData)
        mockkObject(ActionsRepository)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `backup writes apps json when apps enabled`() = runTest {
        every { HailData.checkedList } returns mutableListOf(
            AppInfo("com.example.app1", pinned = false, whitelisted = false, tagIdList = mutableListOf(0)),
            AppInfo("com.example.app2", pinned = true, whitelisted = true, tagIdList = mutableListOf(1))
        )

        val outputFile = File.createTempFile("backup", ".zip")
        val result = HBackup.backup(
            context = mockk(relaxed = true),
            outputFile = outputFile,
            options = BackupOptions(apps = true, whitelist = false, actions = false, settings = false)
        )

        assertTrue(result.isSuccess)
        assertTrue(outputFile.exists())
        val content = String(java.util.zip.ZipInputStream(FileInputStream(outputFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "apps.json") {
                    String(zip.readBytes())
                } else {
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            ""
        })
        val json = org.json.JSONArray(content)
        assertEquals(2, json.length())
        assertEquals("com.example.app1", json.getString(0))
        assertEquals("com.example.app2", json.getString(1))
    }

    @Test
    fun `backup writes whitelist json when whitelist enabled`() = runTest {
        every { HailData.checkedList } returns mutableListOf(
            AppInfo("com.example.app1", pinned = false, whitelisted = true, tagIdList = mutableListOf(0)),
            AppInfo("com.example.app2", pinned = true, whitelisted = false, tagIdList = mutableListOf(1))
        )

        val outputFile = File.createTempFile("backup", ".zip")
        val result = HBackup.backup(
            context = mockk(relaxed = true),
            outputFile = outputFile,
            options = BackupOptions(apps = false, whitelist = true, actions = false, settings = false)
        )

        assertTrue(result.isSuccess)
        val content = String(java.util.zip.ZipInputStream(FileInputStream(outputFile)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "whitelist.json") {
                    String(zip.readBytes())
                } else {
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            ""
        })
        val json = org.json.JSONArray(content)
        assertEquals(1, json.length())
        assertEquals("com.example.app1", json.getString(0))
    }

    @Test
    fun `restore reads apps json and adds to checked list`() = runTest {
        val inputFile = File.createTempFile("backup", ".zip")
        java.util.zip.ZipOutputStream(FileOutputStream(inputFile)).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("apps.json"))
            zip.write(org.json.JSONArray().apply {
                put("com.example.restore1")
                put("com.example.restore2")
            }.toString().toByteArray())
            zip.closeEntry()
        }

        var addedApps = mutableListOf<String>()
        every { HailData.isChecked(any()) } returns false
        every { HailData.addCheckedApp(any(), 0, false) } answers {
            addedApps.add(firstArg())
        }
        every { HailData.saveApps() } returns Unit

        val result = HBackup.restore(
            context = mockk(relaxed = true),
            inputFile = inputFile,
            options = RestoreOptions(apps = true, whitelist = false, actions = false, settings = false)
        )

        assertTrue(result.isSuccess)
        assertEquals(2, addedApps.size)
        assertTrue(addedApps.contains("com.example.restore1"))
        assertTrue(addedApps.contains("com.example.restore2"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aistra.hail.utils.HBackupTest" -i`
Expected: FAIL with "Unresolved reference: HBackup" and missing imports

- [ ] **Step 3: Write minimal implementation**

`app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt`:
```kotlin
package com.aistra.hail.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.aistra.hail.HailApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

object HBackup {
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
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
                if (options.apps) {
                    val apps = HailData.checkedList.map { it.packageName }
                    zip.putNextEntry(ZipEntry(FILE_APPS))
                    zip.write(JSONArray(apps).toString().toByteArray())
                    zip.closeEntry()
                }
                if (options.whitelist) {
                    val whitelist = HailData.checkedList.filter { it.whitelisted }.map { it.packageName }
                    zip.putNextEntry(ZipEntry(FILE_WHITELIST))
                    zip.write(JSONArray(whitelist).toString().toByteArray())
                    zip.closeEntry()
                }
                if (options.actions) {
                    val actions = ActionsRepository.loadAll()
                    val json = JSONArray().apply {
                        actions.forEach { action ->
                            put(JSONObject().apply {
                                put("id", action.id)
                                put("launchPackage", action.launchPackage)
                                put("unfreezePackages", JSONArray(action.unfreezePackages))
                            })
                        }
                    }
                    zip.putNextEntry(ZipEntry(FILE_ACTIONS))
                    zip.write(json.toString().toByteArray())
                    zip.closeEntry()
                }
                if (options.settings) {
                    val settings = JSONObject()
                    sp.all.forEach { (key, value) ->
                        when (value) {
                            is String -> settings.put(key, value)
                            is Int -> settings.put(key, value)
                            is Boolean -> settings.put(key, value)
                            is Float -> settings.put(key, value)
                            is Long -> settings.put(key, value)
                            is Set<*> -> settings.put(key, JSONArray(value.filterIsInstance<String>()))
                        }
                    }
                    zip.putNextEntry(ZipEntry(FILE_SETTINGS))
                    zip.write(settings.toString().toByteArray())
                    zip.closeEntry()
                }
            }
        }
    }

    suspend fun restore(
        context: Context,
        inputFile: File,
        options: RestoreOptions
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val sp = PreferenceManager.getDefaultSharedPreferences(context)
            ZipInputStream(FileInputStream(inputFile)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        FILE_APPS -> if (options.apps) {
                            val apps = JSONArray(String(zip.readBytes()))
                            for (i in 0 until apps.length()) {
                                val pkg = apps.getString(i)
                                if (!HailData.isChecked(pkg)) {
                                    HailData.addCheckedApp(pkg, 0, false)
                                }
                            }
                        }
                        FILE_WHITELIST -> if (options.whitelist) {
                            val whitelist = JSONArray(String(zip.readBytes()))
                            for (i in 0 until whitelist.length()) {
                                val pkg = whitelist.getString(i)
                                val appInfo = HailData.checkedList.find { it.packageName == pkg }
                                if (appInfo != null) appInfo.whitelisted = true
                            }
                        }
                        FILE_ACTIONS -> if (options.actions) {
                            val actions = JSONArray(String(zip.readBytes()))
                            for (i in 0 until actions.length()) {
                                val actionObj = actions.getJSONObject(i)
                                val id = actionObj.getString("id")
                                val launchPackage = actionObj.getString("launchPackage")
                                val unfreezePackages = actionObj.getJSONArray("unfreezePackages").let { arr ->
                                    List(arr.length()) { arr.getString(it) }
                                }
                                ActionsRepository.save(id, launchPackage, unfreezePackages)
                            }
                        }
                        FILE_SETTINGS -> if (options.settings) {
                            val settings = JSONObject(String(zip.readBytes()))
                            val editor = sp.edit()
                            settings.keys().forEach { key ->
                                when (val value = settings.get(key)) {
                                    is String -> editor.putString(key, value)
                                    is Int -> editor.putInt(key, value)
                                    is Boolean -> editor.putBoolean(key, value)
                                    is Float -> editor.putFloat(key, value)
                                    is Long -> editor.putLong(key, value)
                                }
                            }
                            editor.apply()
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            if (options.apps) HailData.saveApps()
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aistra.hail.utils.HBackupTest" -i`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt
git commit -m "feat: add HBackup utility for backup/restore via ZIP"
```

---

### Task 2: Add Backup/Restore Menu Items to PagerFragment

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`
- Modify: `app/src/main/res/menu/menu_home.xml`

**Interfaces:**
- Consumes: `HBackup.backup()`, `HBackup.restore()`, `HUI.showToast()`
- Produces: `backupLauncher`, `restoreLauncher`, menu item handlers, backup/restore dialogs

- [ ] **Step 1: Add imports and member variables**

In `PagerFragment.kt`, add after line 56 (`import kotlinx.coroutines.withContext`):
```kotlin
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import com.aistra.hail.utils.BackupOptions
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.RestoreOptions
import java.io.File
```

Add after line 61 (`private var query: String = String()`):
```kotlin
    private var backupLauncher = registerForActivityResult(CreateDocument("application/zip")) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val options = pendingBackupOptions ?: return@launch
            pendingBackupOptions = null
            val file = File(context?.cacheDir, "backup-${System.currentTimeMillis()}.zip")
            runCatching {
                context?.contentResolver?.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input ->
                        HFiles.copy(input, output)
                    }
                }
            }.onSuccess {
                HUI.showToast(R.string.msg_exported, file.name)
            }.onFailure {
                HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
            }
        }
    }

    private var restoreLauncher = registerForActivityResult(OpenDocument(arrayOf("application/zip"))) { uri ->
        if (uri == null) return@registerForActivityResult
        lifecycleScope.launch {
            val file = File(context?.cacheDir, "restore-${System.currentTimeMillis()}.zip")
            runCatching {
                context?.contentResolver?.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        HFiles.copy(input, output)
                    }
                }
            }.onSuccess {
                showRestoreDialog(file)
            }.onFailure {
                HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
            }
        }
    }

    private var pendingBackupOptions: BackupOptions? = null
```

- [ ] **Step 2: Add menu items to menu_home.xml**

In `menu_home.xml`, add before the closing `</menu>` tag:
```xml
    <item
            android:title="@string/action_backup"
            app:showAsAction="never">
        <menu>
            <item
                    android:id="@+id/action_backup"
                    android:title="@string/action_backup"
                    app:showAsAction="never"/>
            <item
                    android:id="@+id/action_restore"
                    android:title="@string/action_restore"
                    app:showAsAction="never"/>
        </menu>
    </item>
```

- [ ] **Step 3: Add menu item handler in onMenuItemSelected**

In `PagerFragment.kt`, add after line 648 (`R.id.action_export_all -> exportToClipboard(HailData.checkedList)`):
```kotlin
            R.id.action_backup -> showBackupDialog()
            R.id.action_restore -> restoreLauncher.launch(arrayOf("application/zip"))
```

- [ ] **Step 4: Add backup/restore dialog functions**

Add after `exportToClipboard` function (after line 585):
```kotlin
    private fun showBackupDialog() {
        val checkedItems = booleanArrayOf(true, true, true, true)
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_backup)
            .setMultiChoiceItems(
                arrayOf(
                    getString(R.string.backup_apps),
                    getString(R.string.backup_whitelist),
                    getString(R.string.backup_actions),
                    getString(R.string.backup_settings)
                ),
                checkedItems
            ) { _, _, _ -> }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val options = BackupOptions(
                    apps = checkedItems[0],
                    whitelist = checkedItems[1],
                    actions = checkedItems[2],
                    settings = checkedItems[3]
                )
                if (!options.apps && !options.whitelist && !options.actions && !options.settings) {
                    HUI.showToast(R.string.msg_no_items_to_select)
                    return@setPositiveButton
                }
                pendingBackupOptions = options
                backupLauncher.launch("hail-backup-${System.currentTimeMillis()}.zip")
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showRestoreDialog(file: File) {
        val checkedItems = booleanArrayOf(true, true, true, true)
        MaterialAlertDialogBuilder(activity).setTitle(R.string.action_restore)
            .setMultiChoiceItems(
                arrayOf(
                    getString(R.string.backup_apps),
                    getString(R.string.backup_whitelist),
                    getString(R.string.backup_actions),
                    getString(R.string.backup_settings)
                ),
                checkedItems
            ) { _, _, _ -> }
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val options = RestoreOptions(
                    apps = checkedItems[0],
                    whitelist = checkedItems[1],
                    actions = checkedItems[2],
                    settings = checkedItems[3]
                )
                if (!options.apps && !options.whitelist && !options.actions && !options.settings) {
                    HUI.showToast(R.string.msg_no_items_to_select)
                    return@setPositiveButton
                }
                lifecycleScope.launch {
                    val dialog = MaterialAlertDialogBuilder(activity).setView(R.layout.dialog_progress).setCancelable(false).show()
                    val result = HBackup.restore(requireContext(), file, options)
                    dialog.dismiss()
                    result.onSuccess {
                        if (options.apps) {
                            updateCurrentList()
                        }
                        if (options.settings) {
                            activity.invalidateOptionsMenu()
                        }
                        HUI.showToast(R.string.msg_imported)
                    }.onFailure {
                        HUI.showToast(R.string.operation_failed, it.localizedMessage ?: "Unknown", true)
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt app/src/main/res/menu/menu_home.xml
git commit -m "feat: add backup/restore menu items and dialogs to PagerFragment"
```

---

### Task 3: Add String Resources

**Files:**
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1: Add string resources**

Add to `strings.xml` before the closing `</resources>` tag:
```xml
    <string name="action_backup">Backup</string>
    <string name="action_restore">Restore</string>
    <string name="backup_apps">Apps</string>
    <string name="backup_whitelist">Whitelist</string>
    <string name="backup_actions">Actions</string>
    <string name="backup_settings">Settings</string>
```

- [ ] **Step 2: Commit**

```bash
git add app/src/main/res/values/strings.xml
git commit -m "feat: add backup/restore string resources"
```

---

### Task 4: Write Additional Edge Case Tests

**Files:**
- Modify: `app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt`

- [ ] **Step 1: Write failing tests for edge cases**

Add to `HBackupTest.kt`:
```kotlin
    @Test
    fun `restore ignores missing files and succeeds`() = runTest {
        val inputFile = File.createTempFile("backup", ".zip")
        java.util.zip.ZipOutputStream(FileOutputStream(inputFile)).use { zip ->
            zip.putNextEntry(java.util.zip.ZipEntry("nonexistent.json"))
            zip.write("[]".toByteArray())
            zip.closeEntry()
        }

        every { HailData.isChecked(any()) } returns false
        every { HailData.addCheckedApp(any(), 0, false) } returns Unit
        every { HailData.saveApps() } returns Unit

        val result = HBackup.restore(
            context = mockk(relaxed = true),
            inputFile = inputFile,
            options = RestoreOptions(apps = true, whitelist = true, actions = true, settings = true)
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun `backup handles empty checked list`() = runTest {
        every { HailData.checkedList } returns mutableListOf()
        every { ActionsRepository.loadAll() } returns emptyList()

        val sp = mockk<SharedPreferences>(relaxed = true)
        every { sp.all } returns emptyMap()
        val context = mockk<Context>(relaxed = true)
        every { PreferenceManager.getDefaultSharedPreferences(context) } returns sp

        val outputFile = File.createTempFile("backup", ".zip")
        val result = HBackup.backup(
            context = context,
            outputFile = outputFile,
            options = BackupOptions(apps = true, whitelist = true, actions = true, settings = true)
        )

        assertTrue(result.isSuccess)
    }
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `./gradlew :app:testDebugUnitTest --tests "com.aistra.hail.utils.HBackupTest" -i`
Expected: PASS

- [ ] **Step 3: Commit**

```bash
git add app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt
git commit -m "test: add HBackup edge case tests"
```

---

### Task 5: Verify Build and Lint

- [ ] **Step 1: Run unit tests**

Run: `./gradlew :app:testDebugUnitTest`
Expected: All tests pass

- [ ] **Step 2: Run lint**

Run: `./gradlew :app:lintDebug --continue`
Expected: No new lint errors

- [ ] **Step 3: Commit any remaining changes**

```bash
git add .
git commit -m "chore: verify backup/restore implementation"
```
