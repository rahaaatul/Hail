package com.aistra.hail.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.AppShortcut
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material.icons.outlined.ManageSearch
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.ScreenLockPortrait
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shortcut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HBackup
import com.aistra.hail.utils.HBackup.BackupOptions

import com.aistra.hail.utils.HFiles

import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.HPolicy

import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HSystem
import com.aistra.hail.utils.HTarget
import com.aistra.hail.utils.HUI

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.zhanghai.compose.preference.Icons
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.listPreference
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.rememberPreferenceState
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.switchPreference

import java.io.File

class SettingsFragment :
    MainFragment(),
    MenuProvider {
    internal var islandPermissionRequest: CompletableDeferred<Boolean>? = null
        private set
    internal val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            islandPermissionRequest?.complete(isGranted)
        }
        val cacheDir = context?.cacheDir ?: return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        if (DocumentsContract.isDocumentUri(ctx, uri) && DocumentFile.fromSingleUri(ctx, uri)?.isDirectory == true) {
            HUI.showToast(R.string.pick_file_not_folder)
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val file = File(cacheDir, "restore-${System.currentTimeMillis()}.zip")
            runCatching {
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            HFiles.copy(input, output)
                        }
                    }
                } catch (e: java.io.FileNotFoundException) {
                    HLog.d("SettingsFragment", "Settings file not found: ${e.message}")
                    file.delete()
                    HUI.showToast(R.string.operation_failed, app.getString(R.string.file_not_found), true)
                    return@launch
                }
            }.onSuccess {
                showRestoreDialog(file)
            }.onFailure {
                file.delete()
                HUI.showToast(
                    R.string.operation_failed,
                    it.localizedMessage ?: app.getString(R.string.error_unknown),
                    true,
                )
            }
        }
    }
    private val _iconPackValues = mutableStateOf(listOf(HailData.ACTION_NONE))
    private val _iconPackNames = mutableStateOf(mapOf<String, String>())


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        if (_iconPackValues.value.size == 1) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val values = mutableListOf(HailData.ACTION_NONE).apply {
                    addAll(
                        Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME").let {
                            if (HTarget.T) {
                                app.packageManager.queryIntentActivities(
                                    it,
                                    PackageManager.ResolveInfoFlags.of(0),
                                )
                            } else {
                                app.packageManager.queryIntentActivities(it, 0)
                            }
                        }.map { it.activityInfo.packageName },
                    )
                }
                val names = values.associateWith { pack ->
                    if (pack == HailData.ACTION_NONE) {
                        app.getString(R.string.action_none)
                    } else {
                        HPackages.getApplicationInfoOrNull(pack)?.loadLabel(app.packageManager)?.toString() ?: pack
                    }
                }
                withContext(Dispatchers.Main) {
                    _iconPackValues.value = values
                    _iconPackNames.value = names
                }
            }
        }
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    ProvidePreferenceLocals {
                        SettingsScreen()
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        if (BuildConfig.DEBUG) {
            SideEffect {
                Log.d("SettingsRecompose", "SettingsScreen recomposed")
            }
        }
        val iconPackValues by _iconPackValues
        val autoFreezeAfterLock = rememberPreferenceState(HailData.AUTO_FREEZE_AFTER_LOCK, false)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            workingModeSection(::onWorkingModeChange, ::resetDynamicShortcuts)
            customizeSection(iconPackValues) { iconPackName(it) }
            iconSettingsSection()
            searchSection()
            autoFreezeSection(autoFreezeAfterLock)
            skipOptionsSection(autoFreezeAfterLock)
            shortcutsSection(
                onAddPinShortcut = { addPinShortcut() },
                onResetDynamicShortcuts = ::resetDynamicShortcuts,
            )
            cacheSection(
                onConfirmRebuildCache = { confirmRebuildCache() },
                onRequestBackgroundActivity = ::requestBackgroundActivity,
            )
            backupSection(
                onShowBackupDialog = { showBackupDialog() },
                onRestore = { restoreLauncher.launch(arrayOf("application/zip")) },
            )
        }
    }

    fun onWorkingModeChange(rememberState: MutableState<String>, mode: String): Boolean {
        // Show/hide terminal menu.
        activity.invalidateOptionsMenu()
        return when {
            mode.startsWith(HailData.OWNER) -> handleOwnerMode()
            mode.startsWith(HailData.DHIZUKU) -> handleDhizukuMode(rememberState, mode)
            mode.startsWith(HailData.SU) -> handleSuMode()
            mode.startsWith(HailData.SHIZUKU) -> handleShizukuMode(rememberState, mode)
            mode.startsWith(HailData.ISLAND) -> handleIslandMode(rememberState, mode)
            mode.startsWith(HailData.PRIVAPP) -> handlePrivappMode()
            else -> true
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_terminal -> showTerminalDialog()
            R.id.action_remove_owner -> (requireActivity() as MainActivity).ownerRemoveDialog()
            R.id.action_help -> findNavController().navigate(R.id.nav_about)
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (HailData.workingMode.startsWith(HailData.SU) || HailData.workingMode.startsWith(
                HailData.SHIZUKU,
            )
        ) {
            menu.findItem(R.id.action_terminal).isVisible = true
        } else if (HPolicy.isDeviceOwnerActive) {
            menu.findItem(R.id.action_remove_owner).isVisible = true
        }
    }

    private fun resetDynamicShortcuts() {
        HShortcuts.removeAllDynamicShortcuts()
        HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
    }

    private fun requestBackgroundActivity() {
        val powerManager = requireContext().getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            },
        )
    }

    private var restoreLauncher = registerForActivityResult(OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        val cacheDir = context?.cacheDir ?: return@registerForActivityResult
        val ctx = context ?: return@registerForActivityResult
        if (DocumentsContract.isDocumentUri(ctx, uri) && DocumentFile.fromSingleUri(ctx, uri)?.isDirectory == true) {
            HUI.showToast(R.string.pick_file_not_folder)
            return@registerForActivityResult
        }
        lifecycleScope.launch {
            val file = File(cacheDir, "restore-${System.currentTimeMillis()}.zip")
            runCatching {
                try {
                    ctx.contentResolver.openInputStream(uri)?.use { input ->
                        file.outputStream().use { output ->
                            HFiles.copy(input, output)
                        }
                    }
                } catch (e: java.io.FileNotFoundException) {
                    HLog.d("SettingsFragment", "Settings file not found: ${e.message}")
                    file.delete()
                    HUI.showToast(R.string.operation_failed, app.getString(R.string.file_not_found), true)
                    return@launch
                }
            }.onSuccess {
                showRestoreDialog(file)
            }.onFailure {
                file.delete()
                HUI.showToast(
                    R.string.operation_failed,
                    it.localizedMessage ?: app.getString(R.string.error_unknown),
                    true,
                )
            }
        }
    }


}
