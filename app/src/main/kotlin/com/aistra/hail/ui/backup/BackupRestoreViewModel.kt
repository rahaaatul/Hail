package com.aistra.hail.ui.backup

import android.app.Application
import android.app.Activity
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistra.hail.R
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.Phase
import com.aistra.hail.backup.RestoreState
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.backup.canStartRestore
import com.aistra.hail.backup.entriesOf
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.HFiles
import com.aistra.hail.utils.HLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val _backupState = MutableStateFlow(BackupSectionState())
    val backupState = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<RestoreState>(RestoreState.Empty)
    val restoreState = _restoreState.asStateFlow()

    private val _restoreError = MutableStateFlow<String?>(null)
    val restoreError = _restoreError.asStateFlow()

    private var stagedFile: File? = null

    data class BackupSectionState(
        val options: HBackup.BackupOptions = HBackup.BackupOptions(
            apps = true,
            whitelist = true,
            actions = true,
            settings = true
        ),
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

    fun performBackup(destinationUri: Uri, context: Context) {
        val current = _backupState.value
        if (!canStartBackup(current.options)) {
            _backupState.value = current.copy(message = context.getString(R.string.msg_no_items_to_select))
            return
        }
        _backupState.value = current.copy(phase = Phase.Working, message = null)
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val cacheFile = File(appContext.cacheDir, "backup-${System.currentTimeMillis()}.zip")
            val result = try {
                HBackup.backup(appContext, cacheFile, current.options).getOrThrow()
                appContext.contentResolver.openOutputStream(destinationUri)?.use { output ->
                    cacheFile.inputStream().use { input ->
                        HFiles.copy(input, output)
                    }
                } ?: throw IllegalStateException(appContext.getString(R.string.cannot_open_selected_file))
                Result.success(Unit)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Result.failure(exception)
            } finally {
                cacheFile.delete()
            }
            result.onSuccess {
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Done,
                    message = appContext.getString(R.string.msg_exported, cacheFile.name)
                )
            }.onFailure { exception ->
                _backupState.value = _backupState.value.copy(
                    phase = Phase.Failed,
                    message = appContext.getString(
                        R.string.operation_failed,
                        exception.message ?: appContext.getString(R.string.error_unknown)
                    )
                )
            }
        }
    }

    fun onArchivePicked(uri: Uri?, context: Context) {
        if (uri == null) return
        val appContext = context.applicationContext
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(appContext.cacheDir, "restore-${System.currentTimeMillis()}.zip")
            deleteStagedFile()
            stagedFile = file
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    throw IOException("Null input stream")
                }
                inputStream.use { input ->
                    file.outputStream().use { output ->
                        HFiles.copy(input, output)
                    }
                }
                val entries = entriesOf(file)
                val displayName = getDisplayName(uri, appContext)
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
                _restoreError.value = null
            } catch (exception: CancellationException) {
                deleteStagedFile()
                throw exception
            } catch (exception: Exception) {
                deleteStagedFile()
                _restoreState.value = RestoreState.Empty
                _restoreError.value = appContext.getString(R.string.msg_not_a_backup)
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
        _restoreError.value = null
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
            val appContext = context.applicationContext
            val activity = context as? Activity
            viewModelScope.launch(Dispatchers.IO) {
                val result = try {
                    HBackup.restore(appContext, current.file, current.options)
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Result.failure(exception)
                }
                result.onSuccess {
                    if (current.options.apps || current.options.whitelist) {
                        com.aistra.hail.utils.AppMetaCache.invalidateAll()
                    }
                    if (current.options.settings) {
                        withContext(Dispatchers.Main) {
                            if (activity?.isFinishing == false && activity.isDestroyed.not()) {
                                activity.invalidateOptionsMenu()
                            }
                        }
                    }
                    _restoreState.value = (_restoreState.value as RestoreState.Loaded)
                        .copy(
                            phase = Phase.Done,
                            message = appContext.getString(R.string.msg_restored, current.file.name)
                        )
                }.onFailure { exception ->
                    _restoreState.value = (_restoreState.value as RestoreState.Loaded)
                        .copy(
                            phase = Phase.Failed,
                            message = appContext.getString(
                                R.string.operation_failed,
                                exception.message ?: appContext.getString(R.string.error_unknown)
                            )
                        )
                }
            }
        }
    }

    private fun deleteStagedFile() {
        stagedFile?.let {
            if (it.exists() && !it.delete()) {
                HLog.w("BackupRestore", "Unable to delete staged backup file: ${it.name}")
            }
            stagedFile = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Staged file is worthless once ViewModel is cleared; delete unconditionally.
        deleteStagedFile()
    }
}