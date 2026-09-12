# Task 2: Add Backup/Restore Menu Items to PagerFragment

## Goal

Add backup/restore UI to `PagerFragment.kt` and `menu_home.xml`. Backup shows a checkbox dialog first, then opens the folder picker. Restore opens the folder picker first, then shows a checkbox dialog.

## Files

- **Modify:** `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`
- **Modify:** `app/src/main/res/menu/menu_home.xml`

## Requirements

### PagerFragment.kt

**Add imports after `import kotlinx.coroutines.withContext`:**
```kotlin
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import com.aistra.hail.utils.BackupOptions
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.RestoreOptions
import java.io.File
```

**Add member variables after `private var query: String = String()`:**
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

**Add menu item handlers in `onMenuItemSelected` after `R.id.action_export_all -> exportToClipboard(HailData.checkedList)`:**
```kotlin
            R.id.action_backup -> showBackupDialog()
            R.id.action_restore -> restoreLauncher.launch(arrayOf("application/zip"))
```

**Add backup/restore dialog functions after `exportToClipboard` function:**
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

### menu_home.xml

Add before the closing `</menu>` tag:
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

## Constraints

- Follow existing patterns: `MaterialAlertDialogBuilder`, `registerForActivityResult`, `lifecycleScope.launch`
- Backup flow: dialog first, then folder picker
- Restore flow: folder picker first, then dialog
- Use `HFiles.copy` for file operations
- Use `HUI.showToast` for user feedback
- No new dependencies

## Deliverable

Modify both files, run `./gradlew :app:assembleDebug`, commit with message `feat: add backup/restore menu items and dialogs to PagerFragment`.
