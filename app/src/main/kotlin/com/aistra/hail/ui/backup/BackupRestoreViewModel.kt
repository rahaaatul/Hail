package com.aistra.hail.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupEntry
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import com.aistra.hail.backup.canStartBackup
import com.aistra.hail.utils.backup
import com.aistra.hail.utils.restore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

data class BackupUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

data class RestoreUiState(
    val isWorking: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val entries: List<BackupEntry> = emptyList(),
    val fileName: String? = null,
    val fileSize: Long = 0,
    val isCancelable: Boolean = true,
)

class BackupRestoreViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    companion object {
        const val KEY_STAGED_ARCHIVE_URI = "staged_archive_uri"
    }

    private val appContext = application

    // URI of the archive chosen for restore, persisted across process death.
    private val _stagedArchiveUri = savedStateHandle.getLiveData<Uri?>(KEY_STAGED_ARCHIVE_URI)
    val stagedArchiveUri: LiveData<Uri?> = _stagedArchiveUri

    private val _backupState = MutableStateFlow(BackupUiState())
    val backupState: StateFlow<BackupUiState> = _backupState

    private val _restoreState = MutableStateFlow(RestoreUiState())
    val restoreState: StateFlow<RestoreUiState> = _restoreState

    private val _backupOptions = MutableStateFlow(BackupOptions())
    val backupOptions: StateFlow<BackupOptions> = _backupOptions

    private val _restoreOptions = MutableStateFlow(RestoreOptions())
    val restoreOptions: StateFlow<RestoreOptions> = _restoreOptions

    private var restoreJob: Job? = null

    fun updateBackupOption(
        category: BackupCategory,
        enabled: Boolean,
    ) {
        _backupOptions.update { current ->
            when (category) {
                BackupCategory.APPS -> current.copy(apps = enabled)
                BackupCategory.TAGS -> current.copy(tags = enabled)
                BackupCategory.ACTIONS -> current.copy(actions = enabled)
                BackupCategory.SETTINGS -> current.copy(settings = enabled)
                BackupCategory.WHITELIST -> current.copy(whitelist = enabled)
            }
        }
    }

    fun updateRestoreOption(
        category: BackupCategory,
        enabled: Boolean,
    ) {
        _restoreOptions.update { current ->
            when (category) {
                BackupCategory.APPS -> current.copy(apps = enabled)
                BackupCategory.TAGS -> current.copy(tags = enabled)
                BackupCategory.ACTIONS -> current.copy(actions = enabled)
                BackupCategory.SETTINGS -> current.copy(settings = enabled)
                BackupCategory.WHITELIST -> current.copy(whitelist = enabled)
            }
        }
    }

    fun performBackup(destinationUri: Uri) {
        val options = _backupOptions.value
        if (!canStartBackup(options)) return
        viewModelScope.launch {
            _backupState.value = BackupUiState(isWorking = true, message = "Creating backup...")
            val tempFile = File(appContext.cacheDir, "backup-${System.currentTimeMillis()}.zip")
            val result = backup(appContext, tempFile, options)
            result.onFailure { exception ->
                _backupState.value =
                    BackupUiState(
                        isWorking = false,
                        error = exception.message ?: "Backup failed",
                    )
                return@launch
            }
            // Copy temp file to destination URI on the IO dispatcher.
            var copySucceeded = false
            try {
                copySucceeded =
                    withContext(Dispatchers.IO) {
                        appContext.contentResolver.openOutputStream(destinationUri)?.use { output ->
                            FileInputStream(tempFile).use { input ->
                                input.copyTo(output)
                            }
                            true
                        } ?: false
                    }
            } catch (e: IOException) {
                _backupState.value =
                    BackupUiState(
                        isWorking = false,
                        error = e.message ?: "Failed to save backup",
                    )
            } finally {
                tempFile.delete()
            }
            if (copySucceeded) {
                _backupState.value = BackupUiState(isWorking = false, message = "Backup saved")
            } else {
                _backupState.value =
                    BackupUiState(
                        isWorking = false,
                        error = "Failed to open destination URI for writing",
                    )
            }
        }
    }

    fun onArchivePicked(uri: Uri) {
        // Persist the URI so it survives process death.
        _stagedArchiveUri.postValue(uri)
        viewModelScope.launch {
            _restoreState.value = RestoreUiState(isWorking = true, message = "Reading archive...")
            // Read the archive off the main thread to avoid ANRs.
            val readResult: Result<Pair<File, List<BackupEntry>>> =
                withContext(Dispatchers.IO) {
                    val tempFile = File(appContext.cacheDir, "restore-${System.currentTimeMillis()}.zip")
                    val inputStream =
                        appContext.contentResolver.openInputStream(uri) ?: run {
                            tempFile.delete()
                            return@withContext Result.failure(IOException("Failed to open URI"))
                        }
                    try {
                        inputStream.use { input ->
                            FileOutputStream(tempFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    } catch (e: IOException) {
                        tempFile.delete()
                        return@withContext Result.failure(e)
                    }
                    val entries =
                        com.aistra.hail.backup
                            .entriesOf(tempFile)
                    Result.success(tempFile to entries)
                }

            val (tempFile, entries) =
                readResult.getOrNull() ?: run {
                    val error =
                        if (readResult.isFailure) {
                            readResult.exceptionOrNull()?.message ?: "Failed to read archive"
                        } else {
                            "Failed to read archive"
                        }
                    _restoreState.value =
                        RestoreUiState(
                            isWorking = false,
                            error = error,
                        )
                    _stagedArchiveUri.postValue(null)
                    return@launch
                }
            // URI persisted above in SavedStateHandle; the temp file was only for preview — delete it.
            tempFile.delete()
            _restoreState.value =
                RestoreUiState(
                    isWorking = false,
                    entries = entries,
                    fileName = "archive.zip",
                    fileSize = readResult.getOrNull()?.first?.length() ?: 0L,
                )
        }
    }

    fun onRestoreConfirmed() {
        val archiveUri = stagedArchiveUri.value ?: return
        val options = _restoreOptions.value
        restoreJob =
            viewModelScope.launch {
                _restoreState.value =
                    RestoreUiState(
                        isWorking = true,
                        message = "Restoring...",
                        entries = restoreState.value.entries,
                        fileName = "archive.zip",
                        fileSize = 0L,
                    )
                val tempFile = copyArchiveToTemp(archiveUri)
                if (tempFile == null) {
                    _restoreState.value =
                        RestoreUiState(
                            isWorking = false,
                            error = "Failed to open archive",
                            entries = restoreState.value.entries,
                        )
                    return@launch
                }
                val result = restore(appContext, tempFile, options)
                result.onFailure { exception ->
                    _restoreState.value =
                        RestoreUiState(
                            isWorking = false,
                            error = exception.message ?: "Restore failed",
                            entries = restoreState.value.entries,
                        )
                    tempFile.delete()
                    return@launch
                }
                _restoreState.value =
                    RestoreUiState(
                        isWorking = false,
                        message = "Restore complete",
                        entries = restoreState.value.entries,
                    )
                _stagedArchiveUri.postValue(null)
                tempFile.delete()
            }
    }

    fun cancelRestore() {
        restoreJob?.cancel()
        restoreJob = null
        _restoreState.value =
            RestoreUiState(
                isWorking = false,
                message = "Restore cancelled",
                entries = restoreState.value.entries,
            )
    }

    fun clearStagedArchive() {
        restoreJob?.cancel()
        restoreJob = null
        _stagedArchiveUri.postValue(null)
        _restoreState.value = RestoreUiState()
    }

    private suspend fun copyArchiveToTemp(archiveUri: Uri): File? =
        withContext(Dispatchers.IO) {
            val file = File(appContext.cacheDir, "restore-${System.currentTimeMillis()}.zip")
            appContext.contentResolver.openInputStream(archiveUri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            } ?: run {
                file.delete()
                return@withContext null
            }
            file
        }

    override fun onCleared() {
        super.onCleared()
        clearStagedArchive()
    }
}
