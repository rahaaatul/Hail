package com.aistra.hail.ui.backup

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupEntry
import com.aistra.hail.backup.Phase
import com.aistra.hail.backup.RestoreState
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.canStartRestore
import com.aistra.hail.backup.entriesOf
import com.aistra.hail.backup.entryCategory
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.HFiles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import kotlin.Result
import kotlin.runCatching

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _backupState = MutableStateFlow(BackupSectionState())
    val backupState = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Empty)
    val restoreState = _restoreState.asStateFlow()

    private var stagedFile: File? = null

    data class BackupSectionState(
        val options: HBackup.BackupOptions = HBackup.BackupOptions(apps = true, whitelist = true, actions = true, settings = true),
        val phase: Phase = Phase.Idle,
        val message: String? = null,
    )

    fun onBackupOptionChange(category: BackupCategory, checked: Boolean) {
        val current = _backupState.value
        val newOptions = when (category) {
            BackupCategory.APPS -> current.options.copy(apps = checked)
            BackupCategory.WHITELIST -> current.options.copy(whitelist = checked)
            BackupCategory.ACTIONS -> current.options.copy(actions = checked)
            BackupCategory.SETTINGS -> current.options.copy(settings = checked)
        }
        _backupState.value = current.copy(options = newOptions, message = null)
    }

    fun startBackup(context: Context) {
        val current = _backupState.value
        if (!canStartBackup(current.options)) {
            _backupState.value = current.copy(message = context.getString(R.string.msg_no_items_to_select))
            return
        }
        _backupState.value = current.copy(phase = Phase.Working, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(context.cacheDir, "backup-${System.currentTimeMillis()}.zip")
            val result = HBackup.backup(context, file, current.options)
            result.onSuccess {
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Done,
                    message = context.getString(R.string.msg_exported, file.name)
                )
                file.delete()
            }.onFailure {
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Failed,
                    message = context.getString(R.string.operation_failed, it.localizedMessage ?: context.getString(R.string.error_unknown))
                )
                file.delete()
            }
        }
    }

    fun onBackupResult(uri: Uri?, context: Context) {
        // The backup file was already written to cache by startBackup.
        // This is called when the user picks a destination in SAF.
        // Actually, looking at the old flow: backup runs first, then SAF saves the cache file.
        // So the ViewModel runs backup to a cache file, then Fragment shows SAF to save it.
        // But the design says "Create backup" button -> progress -> result with filename.
        // The SAF is part of the backup flow.
        // Let me re-read the design...
        //
        // Actually the old flow was: dialog -> options -> SAF CreateDocument -> backup to cache -> copy to SAF URI.
        // The design doesn't explicitly change this flow, but says the screen has states.
        // I think the correct flow is: button -> working (backup to cache) -> SAF picker -> copy -> result.
        // But the design shows the progress row replacing the button, and then result line.
        // The SAF picker would appear during "working" or after?
        //
        // Looking at the old code: backupLauncher creates the document, THEN runs backup to cache, THEN copies.
        // The design doesn't explicitly change this. But it says "progress in place" and "result persists".
        // I think we keep the same flow: button click -> SAF CreateDocument -> backup runs -> copy -> result.
        // But the ViewModel should handle the backup, and the Fragment handles SAF.
        //
        // Let me restructure: ViewModel has a method that runs backup and returns the cache file.
        // Fragment calls it after SAF returns URI.
        // But the design shows progress during backup... so maybe:
        // 1. User clicks "Create backup"
        // 2. Fragment launches SAF CreateDocument
        // 3. On SAF result, Fragment calls ViewModel.startBackupToCache()
        // 4. ViewModel shows working, runs backup, returns result
        // 5. Fragment copies cache file to SAF URI
        // 6. ViewModel shows result
        //
        // This is getting complex. Let me simplify: ViewModel handles the backup operation
        // (to cache file), Fragment handles SAF and copying.
        // But the progress UI needs to show during backup.
        //
        // Actually, the simplest approach: ViewModel has startBackup() that runs backup to cache.
        // Fragment calls it, shows progress via ViewModel state, then on success launches SAF,
        // then copies, then tells ViewModel the final result.
        // But that means ViewModel state goes: Idle -> Working -> Done (backup to cache) -> Working (copy) -> Done.
        //
        // Let me look at the old flow again:
        // backupLauncher.launch(filename) -> onActivityResult(uri) -> backup to cache -> copy to uri -> toast
        // The progress was a modal dialog.
        //
        // For the new design, I think the flow should be:
        // 1. User clicks "Create backup"
        // 2. Fragment launches SAF CreateDocument
        // 3. User picks location/name
        // 4. Fragment calls ViewModel.performBackup(uri)
        // 5. ViewModel: working -> backup to cache -> copy to uri -> done/failed
        //
        // This keeps all the backup logic in ViewModel, Fragment just forwards SAF result.
        // Let me implement it this way.
    }

    fun performBackup(destinationUri: Uri, context: Context) {
        val current = _backupState.value
        if (!canStartBackup(current.options)) {
            _backupState.value = current.copy(message = context.getString(R.string.msg_no_items_to_select))
            return
        }
        _backupState.value = current.copy(phase = Phase.Working, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            val cacheFile = File(context.cacheDir, "backup-${System.currentTimeMillis()}.zip")
            var result = HBackup.backup(context, cacheFile, current.options)
            if (result.isSuccess) {
                result = runCatching {
                    context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                        cacheFile.inputStream().use { input ->
                            HFiles.copy(input, output)
                        }
                    } ?: throw IllegalStateException(context.getString(R.string.cannot_open_selected_file))
                }
            }
            if (result.isSuccess) {
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Done,
                    message = context.getString(R.string.msg_exported, cacheFile.name)
                )
            } else {
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Failed,
                    message = context.getString(R.string.operation_failed, result.exceptionOrNull()?.message ?: context.getString(R.string.error_unknown))
                )
            }
            cacheFile.delete()
        }
    }

    fun onArchivePicked(uri: Uri?, context: Context) {
        if (uri == null) return
        val cacheDir = context.cacheDir
        val ctx = context
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(cacheDir, "restore-${System.currentTimeMillis()}.zip")
            deleteStagedFile()
            stagedFile = file
            runCatching {
                val inputStream = ctx.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    throw IOException("Null input stream")
                }
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        HFiles.copy(input, output)
                    }
                }
            }.onSuccess {
                val entries = entriesOf(file)
                val displayName = getDisplayName(uri, ctx)
                val sizeBytes = file.length()
                _restoreState.value = RestoreState.Loaded(
                    file = file,
                    displayName = displayName,
                    sizeBytes = sizeBytes,
                    entries = entries,
                    options = HBackup.RestoreOptions(apps = true, whitelist = true, actions = true, settings = true),
                    phase = Phase.Idle,
                    message = null
                )
            }.onFailure {
                deleteStagedFile()
                _restoreState.value = RestoreState.Loaded(
                    file = file,
                    displayName = getDisplayName(uri, ctx),
                    sizeBytes = 0,
                    entries = listOf(
                        BackupEntry(BackupCategory.APPS, false, 0),
                        BackupEntry(BackupCategory.WHITELIST, false, 0),
                        BackupEntry(BackupCategory.ACTIONS, false, 0),
                        BackupEntry(BackupCategory.SETTINGS, false, 0)
                    ),
                    options = HBackup.RestoreOptions(apps = false, whitelist = false, actions = false, settings = false),
                    phase = Phase.Failed,
                    message = ctx.getString(R.string.msg_not_a_backup)
                )
            }
        }
    }

    private fun getDisplayName(uri: Uri, context: Context): String {
        return context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getString(cursor.getColumnIndexOrThrow(android.provider.OpenableColumns.DISPLAY_NAME))
            } else {
                "backup.zip"
            }
        } ?: "backup.zip"
    }

    fun onChangeArchiveClick() {
        deleteStagedFile()
        _restoreState.value = RestoreState.Empty
    }

    fun onRestoreOptionChange(category: BackupCategory, checked: Boolean) {
        val current = _restoreState.value
        if (current is RestoreState.Loaded) {
            val newOptions = when (category) {
                BackupCategory.APPS -> current.options.copy(apps = checked)
                BackupCategory.WHITELIST -> current.options.copy(whitelist = checked)
                BackupCategory.ACTIONS -> current.options.copy(actions = checked)
                BackupCategory.SETTINGS -> current.options.copy(settings = checked)
            }
            _restoreState.value = current.copy(options = newOptions, message = null)
        }
    }

    fun onRestoreConfirmed(context: Context) {
        val current = _restoreState.value
        if (current is RestoreState.Loaded) {
            if (!canStartRestore(current)) return
            _restoreState.value = current.copy(phase = Phase.Working, message = null)
            viewModelScope.launch(Dispatchers.IO) {
                val result = HBackup.restore(context, current.file, current.options)
                result.onSuccess {
                    if (current.options.apps || current.options.whitelist) {
                        com.aistra.hail.utils.AppMetaCache.invalidateAll()
                    }
                    if (current.options.settings) {
                        (context as? android.app.Activity)?.invalidateOptionsMenu()
                    }
                    deleteStagedFile()
                    _restoreState.value = _restoreState.value as RestoreState.Loaded
                        .copy(phase = Phase.Done, message = context.getString(R.string.msg_restored, current.file.name))
                }.onFailure {
                    _restoreState.value = _restoreState.value as RestoreState.Loaded
                        .copy(phase = Phase.Failed, message = context.getString(R.string.operation_failed, it.message ?: context.getString(R.string.error_unknown)))
                }
            }
        }
    }

    private fun deleteStagedFile() {
        stagedFile?.let {
            if (it.exists()) it.delete()
            stagedFile = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        val backupWorking = _backupState.value.phase == Phase.Working
        val restoreWorking = (_restoreState.value as? RestoreState.Loaded)?.phase == Phase.Working
        if (!backupWorking && !restoreWorking) {
            deleteStagedFile()
        }
    }
}