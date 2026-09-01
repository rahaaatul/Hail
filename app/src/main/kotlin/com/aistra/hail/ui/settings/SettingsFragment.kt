package com.aistra.hail.ui.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ManageSearch
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.navigation.fragment.findNavController
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import com.aistra.hail.app.AppManager
import com.aistra.hail.app.HailApi
import com.aistra.hail.app.HailData
import com.aistra.hail.databinding.DialogInputBinding
import com.aistra.hail.ui.main.MainActivity
import com.aistra.hail.ui.main.MainFragment
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.utils.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.rosan.dhizuku.api.Dhizuku
import com.rosan.dhizuku.api.DhizukuRequestPermissionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku

// Navigation screens for settings
private object SettingsScreen {
    const val MAIN = 0
    const val WORKING_MODE = 1
    const val PROVIDER_SELECTION = 2
    const val MODE_SELECTION = 3
}

class SettingsFragment : MainFragment(), MenuProvider {
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    private val _iconPackValues = mutableStateOf(listOf(HailData.ACTION_NONE))
    private val _iconPackNames = mutableStateOf(mapOf<String, String>())

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val menuHost = requireActivity() as MenuHost
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        if (_iconPackValues.value.size == 1) {
            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val values = mutableListOf(HailData.ACTION_NONE).apply {
                    addAll(Intent(Intent.ACTION_MAIN).addCategory("com.anddoes.launcher.THEME").let {
                        if (HTarget.T) app.packageManager.queryIntentActivities(
                            it, PackageManager.ResolveInfoFlags.of(0)
                        ) else app.packageManager.queryIntentActivities(it, 0)
                    }.map { it.activityInfo.packageName })
                }
                val names = values.associateWith { pack ->
                    if (pack == HailData.ACTION_NONE) app.getString(R.string.action_none)
                    else HPackages.getApplicationInfoOrNull(pack)?.loadLabel(app.packageManager)?.toString() ?: pack
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
                    SettingsScreen()
                }
            }
        }
    }

    @Composable
    private fun SettingsScreen() {
        val context = LocalContext.current
        var currentScreen by remember { mutableStateOf(SettingsScreen.MAIN) }
        var workingMode by remember { mutableStateOf(HailData.workingMode) }

        when (currentScreen) {
            SettingsScreen.MAIN -> MainSettingsScreen(
                workingMode = workingMode,
                onWorkingModeChange = { workingMode = it; HailData.workingMode = it },
                onNavigateToWorkingMode = { currentScreen = SettingsScreen.WORKING_MODE }
            )
            SettingsScreen.WORKING_MODE -> WorkingModeScreen(
                workingMode = workingMode,
                onBack = { currentScreen = SettingsScreen.MAIN },
                onNavigateToProviderSelection = { currentScreen = SettingsScreen.PROVIDER_SELECTION },
                onNavigateToModeSelection = { currentScreen = SettingsScreen.MODE_SELECTION }
            )
            SettingsScreen.PROVIDER_SELECTION -> SelectionScreen(
                title = stringResource(R.string.working_mode),
                options = HailData.WORKING_MODE_PROVIDERS.map { getString(it.labelRes) },
                selectedOption = HailData.WORKING_MODE_PROVIDERS.indexOfFirst { it.modes.contains(workingMode) }.takeIf { it >= 0 } ?: 0,
                onSelect = { index ->
                    val chosen = HailData.WORKING_MODE_PROVIDERS[index]
                    if (chosen.modes.size == 1) {
                        val accepted = onWorkingModeChange(chosen.modes.first()) { workingMode = it }
                        if (accepted) HailData.workingMode = chosen.modes.first()
                        currentScreen = SettingsScreen.MAIN
                    } else {
                        workingMode = chosen.modes.first()
                        HailData.workingMode = chosen.modes.first()
                        currentScreen = SettingsScreen.MODE_SELECTION
                    }
                },
                onBack = { currentScreen = SettingsScreen.WORKING_MODE }
            )
            SettingsScreen.MODE_SELECTION -> {
                val provider = HailData.providerForMode(workingMode)
                SelectionScreen(
                    title = stringResource(R.string.mode),
                    options = (provider?.modes ?: emptyList()).map { getString(HailData.labelResForMode(it)) },
                    selectedOption = (provider?.modes ?: emptyList()).indexOf(workingMode).takeIf { it >= 0 } ?: 0,
                    onSelect = { index ->
                        val chosenMode = provider?.modes?.get(index) ?: return@SelectionScreen
                        val accepted = onWorkingModeChange(chosenMode) { workingMode = it }
                        if (accepted) HailData.workingMode = chosenMode
                        currentScreen = SettingsScreen.MAIN
                    },
                    onBack = { currentScreen = SettingsScreen.WORKING_MODE }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun MainSettingsScreen(
        workingMode: String,
        onWorkingModeChange: (String) -> Unit,
        onNavigateToWorkingMode: () -> Unit
    ) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()

        // Local state holders - trigger recomposition on change, persist to HailData
        var biometricLogin by remember { mutableStateOf(HailData.biometricLogin) }
        var appTheme by remember { mutableStateOf(HailData.appTheme) }
        var iconPack by remember { mutableStateOf(HailData.iconPack) }
        var grayscaleIcon by remember { mutableStateOf(HailData.grayscaleIcon) }
        var compactIcon by remember { mutableStateOf(HailData.compactIcon) }
        var synthesizeAdaptiveIcons by remember { mutableStateOf(HailData.synthesizeAdaptiveIcons) }
        var homeFontSize by remember { mutableStateOf(HailData.homeFontSize) }
        var fuzzySearch by remember { mutableStateOf(HailData.fuzzySearch) }
        var nineKeySearch by remember { mutableStateOf(HailData.nineKeySearch) }
        var tileAction by remember { mutableStateOf(HailData.tileAction) }
        var autoFreezeAfterLock by remember { mutableStateOf(HailData.autoFreezeAfterLock) }
        var autoFreezeDelay by remember { mutableStateOf(HailData.autoFreezeDelay) }
        var skipWhileCharging by remember { mutableStateOf(HailData.skipWhileCharging) }
        var skipForegroundApp by remember { mutableStateOf(HailData.skipForegroundApp) }
        var skipNotifyingApp by remember { mutableStateOf(HailData.skipNotifyingApp) }
        var dynamicShortcutAction by remember { mutableStateOf(HailData.dynamicShortcutAction) }

        val iconPackValues by _iconPackValues
        val workingModeEntries = stringArrayResource(R.array.working_mode_entries)
        val appThemeEntries = stringArrayResource(R.array.app_theme_entries)
        val tileActionEntries = stringArrayResource(R.array.tile_action_entries)
        val dynamicShortcutEntries = stringArrayResource(R.array.dynamic_shortcut_entries)

        Scaffold(
            topBar = {
                TopAppBar(title = { Text(stringResource(R.string.title_settings)) })
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // Working mode navigation row
                SettingsNavigationRow(
                    title = stringResource(R.string.working_mode),
                    subtitle = getString(HailData.providerForMode(workingMode)?.labelRes ?: R.string.label_default),
                    icon = Icons.Outlined.Adb,
                    onClick = onNavigateToWorkingMode
                )

                SettingsSwitch(
                    headlineContent = { Text(stringResource(R.string.action_biometric)) },
                    checked = biometricLogin,
                    onCheckedChange = { value ->
                        if (value) resetDynamicShortcuts()
                        biometricLogin = value
                        HailData.biometricLogin = value
                    },
                    leadingContent = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) }
                )
                SettingsHorizontalDivider()
                SettingsSectionHeader(stringResource(R.string.title_customize))
                SettingsList(
                    headlineContent = { Text(stringResource(R.string.app_theme)) },
                    selectedValue = appTheme,
                    onValueChange = { value ->
                        appTheme = value
                        HailData.appTheme = value
                        app.setAppTheme(value)
                        true
                    },
                    values = HailData.APP_THEME_VALUES,
                    entriesId = R.array.app_theme_entries,
                    leadingContent = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
                    supportingContent = {
                        val index = HailData.APP_THEME_VALUES.indexOf(appTheme)
                        Text(appThemeEntries.getOrElse(index) { appTheme })
                    }
                )
                // ... rest of settings
            }
        }
    }

    // ── New composables for 3-level navigation ──

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun WorkingModeScreen(
        workingMode: String,
        onBack: () -> Unit,
        onNavigateToProviderSelection: () -> Unit,
        onNavigateToModeSelection: () -> Unit
    ) {
        val currentProvider = HailData.providerForMode(workingMode)
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.working_mode)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                SettingsNavigationRow(
                    title = stringResource(R.string.working_mode),
                    subtitle = getString(currentProvider?.labelRes ?: R.string.label_default),
                    icon = Icons.Outlined.Adb,
                    onClick = onNavigateToProviderSelection
                )
                SettingsNavigationRow(
                    title = stringResource(R.string.mode),
                    subtitle = getString(HailData.labelResForMode(workingMode)),
                    icon = Icons.Outlined.Tune,
                    onClick = onNavigateToModeSelection,
                    enabled = (currentProvider?.modes?.size ?: 0) > 1
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SelectionScreen(
        title: String,
        options: List<String>,
        selectedOption: Int,
        onSelect: (Int) -> Unit,
        onBack: () -> Unit
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = index == selectedOption,
                                role = Role.RadioButton,
                                onClick = { onSelect(index) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = index == selectedOption,
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsNavigationRow(
        title: String,
        subtitle: String,
        icon: ImageVector,
        onClick: () -> Unit,
        enabled: Boolean = true
    ) = ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) }
    )

    private fun requestBackgroundActivity() {
        val powerManager = requireContext().getSystemService(PowerManager::class.java)
        if (powerManager.isIgnoringBatteryOptimizations(requireContext().packageName)) return
        startActivity(
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
        )
    }

    private fun confirmRebuildCache() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.action_rebuild_cache)
            .setMessage(R.string.msg_confirm_rebuild_cache)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.action_rebuild_cache) { _, _ ->
                AppIconCache.clear()
                AppMetaCache.clearAndRebuild()
            }
            .show()
    }

    private fun resetDynamicShortcuts() {
        HShortcuts.removeAllDynamicShortcuts()
        HShortcuts.addDynamicShortcutAction(HailData.dynamicShortcutAction)
    }

    private fun iconPackName(pack: String): String = if (pack == HailData.ACTION_NONE) getString(R.string.action_none)
    else _iconPackNames.value[pack] ?: pack

    private fun addPinShortcut() {
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_add_pin_shortcut)
            .setItems(R.array.pin_shortcut_entries) { _, which ->
                when (which) {
                    0 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_freeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(
                                    requireContext(), R.drawable.ic_round_frozen_shortcut
                                )!!,
                                HailApi.ACTION_FREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_FREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    1 -> MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_unfreeze_tag)
                        .setItems(HailData.tags.map { it.first }.toTypedArray()) { _, index ->
                            val tag = HailData.tags[index].first
                            HShortcuts.addPinShortcut(
                                AppCompatResources.getDrawable(
                                    requireContext(), R.drawable.ic_round_unfrozen_shortcut
                                )!!,
                                HailApi.ACTION_UNFREEZE_TAG + tag,
                                tag,
                                HailApi.getIntentForTag(HailApi.ACTION_UNFREEZE_TAG, tag)
                            )
                        }.setNegativeButton(android.R.string.cancel, null).show()

                    2 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_FREEZE_ALL,
                        getString(R.string.action_freeze_all),
                        Intent(HailApi.ACTION_FREEZE_ALL)
                    )

                    3 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_unfrozen_shortcut
                        )!!,
                        HailApi.ACTION_UNFREEZE_ALL,
                        getString(R.string.action_unfreeze_all),
                        Intent(HailApi.ACTION_UNFREEZE_ALL)
                    )

                    4 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_round_frozen_shortcut
                        )!!,
                        HailApi.ACTION_FREEZE_NON_WHITELISTED,
                        getString(R.string.action_freeze_non_whitelisted),
                        Intent(HailApi.ACTION_FREEZE_NON_WHITELISTED)
                    )

                    5 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_outline_lock_shortcut
                        )!!, HailApi.ACTION_LOCK, getString(R.string.action_lock), Intent(HailApi.ACTION_LOCK)
                    )

                    6 -> HShortcuts.addPinShortcut(
                        AppCompatResources.getDrawable(
                            requireContext(), R.drawable.ic_outline_lock_shortcut
                        )!!,
                        HailApi.ACTION_LOCK_FREEZE,
                        getString(R.string.action_lock_freeze),
                        Intent(HailApi.ACTION_LOCK_FREEZE)
                    )
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }

    fun onWorkingModeChange(mode: String, setState: (String) -> Unit): Boolean {
        // Show/hide terminal menu.
        activity.invalidateOptionsMenu()
        when {
            mode.startsWith(HailData.OWNER) -> if (!HPolicy.isDeviceOwnerActive) {
                MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.title_set_owner)
                    .setMessage(getString(R.string.msg_set_owner, HPolicy.ADB_COMMAND))
                    .setPositiveButton(android.R.string.ok, null)
                    .setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(HPolicy.ADB_COMMAND) }.show()
                    .findViewById<MaterialTextView>(android.R.id.message)?.setTextIsSelectable(true)
                return false
            }

            mode.startsWith(HailData.DHIZUKU) -> return runCatching {
                Dhizuku.init(app)
                when {
                    Dhizuku.isPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                Dhizuku.requestPermission(object : DhizukuRequestPermissionListener() {
                                    override fun onRequestPermission(grantResult: Int) {
                                        trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                    }
                                })
                                awaitClose()
                            }.first()
                            if (result) {
                                setState(mode)
                                if (HTarget.O) HDhizuku.setDelegatedScopes()
                            }
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }

            mode.startsWith(HailData.SU) -> if (!HShell.checkSU) {
                HUI.showToast(R.string.permission_denied)
                return false
            }

            mode.startsWith(HailData.SHIZUKU) -> return runCatching {
                when {
                    Shizuku.isPreV11() -> throw IllegalStateException("unsupported shizuku version")
                    Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED -> true
                    Shizuku.shouldShowRequestPermissionRationale() -> {
                        HUI.showToast(R.string.permission_denied)
                        false
                    }

                    else -> {
                        lifecycleScope.launch {
                            val result = callbackFlow {
                                val shizukuRequestCode = 0
                                val listener = Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
                                    if (requestCode != shizukuRequestCode) return@OnRequestPermissionResultListener
                                    trySendBlocking(grantResult == PackageManager.PERMISSION_GRANTED)
                                }
                                Shizuku.addRequestPermissionResultListener(listener)
                                Shizuku.requestPermission(shizukuRequestCode)
                                awaitClose {
                                    Shizuku.removeRequestPermissionResultListener(listener)
                                }
                            }.first()
                            if (result) setState(mode)
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.shizuku_missing)
                false
            }

            mode.startsWith(HailData.ISLAND) -> return runCatching {
                when {
                    mode == HailData.MODE_ISLAND_HIDE && HIsland.freezePermissionGranted() -> true
                    mode == HailData.MODE_ISLAND_SUSPEND && HIsland.suspendPermissionGranted() -> true
                    else -> {
                        lifecycleScope.launch {
                            requestPermissionLauncher.launch(
                                if (mode == HailData.MODE_ISLAND_HIDE) HIsland.PERMISSION_FREEZE_PACKAGE
                                else HIsland.PERMISSION_SUSPEND_PACKAGE
                            )
                        }
                        false
                    }
                }
            }.getOrElse {
                HLog.e(it)
                HUI.showToast(R.string.permission_denied)
                false
            }.also {
                if (it) {
                    HIsland.checkOwnerApp()
                }
            }

            mode.startsWith(HailData.PRIVAPP) -> if (!HPackages.isPrivilegedApp(app.packageName)) {
                HUI.showToast(R.string.permission_denied)
                return false
            }
        }

        return true
    }

    private suspend fun onTerminalResult(exitValue: Int, msg: String?) = withContext(Dispatchers.Main) {
        if (exitValue == 0 && msg.isNullOrBlank()) return@withContext
        MaterialAlertDialogBuilder(requireActivity()).apply {
            if (!msg.isNullOrBlank()) {
                if (exitValue != 0) {
                    setTitle(getString(R.string.operation_failed, exitValue.toString()))
                }
                setMessage(msg)
                setNeutralButton(android.R.string.copy) { _, _ -> HUI.copyText(msg) }
            } else if (exitValue != 0) {
                setMessage(getString(R.string.operation_failed, exitValue.toString()))
            }
        }.setPositiveButton(android.R.string.ok, null).show().findViewById<MaterialTextView>(android.R.id.message)
            ?.setTextIsSelectable(true)
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
                HailData.SHIZUKU
            )
        ) menu.findItem(R.id.action_terminal).isVisible = true
        else if (HPolicy.isDeviceOwnerActive) menu.findItem(R.id.action_remove_owner).isVisible = true
    }

    private fun showTerminalDialog() {
        val binding = DialogInputBinding.inflate(layoutInflater)
        binding.inputLayout.setHint(R.string.command)
        binding.editText.run {
            setSingleLine()
            filters = arrayOf()
        }
        MaterialAlertDialogBuilder(requireActivity()).setTitle(R.string.action_terminal).setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                lifecycleScope.launch {
                    val result = AppManager.execute(binding.editText.text.toString())
                    onTerminalResult(result.first, result.second)
                }
            }.setNegativeButton(android.R.string.cancel, null).show()
    }
}