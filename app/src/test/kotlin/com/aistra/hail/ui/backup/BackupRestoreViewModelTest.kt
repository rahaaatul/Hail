package com.aistra.hail.ui.backup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.aistra.hail.HailApp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.backup.BackupCategory
import com.aistra.hail.backup.BackupOptions
import com.aistra.hail.backup.RestoreOptions
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HailApp::class)
class BackupRestoreViewModelTest {
    private lateinit var viewModel: BackupRestoreViewModel
    private lateinit var mockApplication: Application

    @Before
    fun setUp() {
        mockApplication = mockk(relaxed = true)
        mockkObject(HailData)
        every { HailData.checkedList } returns mutableListOf<AppInfo>()
        every { HailData.tags } returns mutableListOf<Pair<String, Int>>()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `updateBackupOption toggles each category`() {
        viewModel = BackupRestoreViewModel(mockApplication, SavedStateHandle())

        viewModel.updateBackupOption(BackupCategory.APPS, false)
        assertFalse(viewModel.backupOptions.value.apps)

        viewModel.updateBackupOption(BackupCategory.TAGS, true)
        assertTrue(viewModel.backupOptions.value.tags)

        viewModel.updateBackupOption(BackupCategory.ACTIONS, true)
        assertTrue(viewModel.backupOptions.value.actions)

        viewModel.updateBackupOption(BackupCategory.SETTINGS, false)
        assertFalse(viewModel.backupOptions.value.settings)

        viewModel.updateBackupOption(BackupCategory.WHITELIST, true)
        assertTrue(viewModel.backupOptions.value.whitelist)
    }

    @Test
    fun `updateRestoreOption toggles each category`() {
        viewModel = BackupRestoreViewModel(mockApplication, SavedStateHandle())

        viewModel.updateRestoreOption(BackupCategory.APPS, false)
        assertFalse(viewModel.restoreOptions.value.apps)

        viewModel.updateRestoreOption(BackupCategory.TAGS, true)
        assertTrue(viewModel.restoreOptions.value.tags)

        viewModel.updateRestoreOption(BackupCategory.ACTIONS, false)
        assertFalse(viewModel.restoreOptions.value.actions)

        viewModel.updateRestoreOption(BackupCategory.SETTINGS, true)
        assertTrue(viewModel.restoreOptions.value.settings)

        viewModel.updateRestoreOption(BackupCategory.WHITELIST, false)
        assertFalse(viewModel.restoreOptions.value.whitelist)
    }

    @Test
    fun `performBackup with no options does nothing`() =
        runBlocking {
            viewModel = BackupRestoreViewModel(mockApplication, SavedStateHandle())
            // Disable all options so canStartBackup returns false
            BackupCategory.values().forEach { category ->
                viewModel.updateBackupOption(category, false)
            }
            val uri = Uri.parse("file:///tmp/backup.zip")

            viewModel.performBackup(uri)

            assertFalse(viewModel.backupState.value.isWorking)
            assertNull(viewModel.backupState.value.error)
        }

    @Test
    fun `cancelRestore sets isWorking false and message`() =
        runBlocking {
            viewModel = BackupRestoreViewModel(mockApplication, SavedStateHandle())

            viewModel.cancelRestore()

            assertFalse(viewModel.restoreState.value.isWorking)
            assertEquals("Restore cancelled", viewModel.restoreState.value.message)
        }

    @Test
    fun `clearStagedArchive resets staged URI and restore state`() =
        runBlocking {
            viewModel = BackupRestoreViewModel(mockApplication, SavedStateHandle())
            viewModel.onArchivePicked(Uri.parse("file:///tmp/archive.zip"))

            viewModel.clearStagedArchive()

            assertNull(viewModel.stagedArchiveUri.value)
            assertFalse(viewModel.restoreState.value.isWorking)
            assertEquals(0, viewModel.restoreState.value.entries.size)
        }
}
