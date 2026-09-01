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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
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

    override fun onResume() {
        super.onResume()
        // Hide the Activity's toolbar to avoid double top bar with Compose Scaffold
        (requireActivity() as MainActivity).appbar.isVisible = false
    }

    override fun onPause() {
        super.onPause()
        // Restore the Activity's toolbar when leaving settings
        (requireActivity() as MainActivity).appbar.isVisible = true
    }

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

        // Handle system back button to navigate within settings screens
        BackHandler(enabled = currentScreen != SettingsScreen.MAIN) {
            currentScreen = when (currentScreen) {
                SettingsScreen.WORKING_MODE -> SettingsScreen.MAIN
                SettingsScreen.PROVIDER_SELECTION -> SettingsScreen.WORKING_MODE
                SettingsScreen.MODE_SELECTION -> SettingsScreen.WORKING_MODE
                else -> SettingsScreen.MAIN
            }
        }

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
            SettingsScreen.PROVIDER_SELECTION -> {
                // Sort providers alphabetically by label
                val sortedProviders = HailData.WORKING_MODE_PROVIDERS.sortedBy { getString(it.labelRes) }
                val sortedOptions = sortedProviders.map { getString(it.labelRes) }
                val currentProvider = HailData.providerForMode(workingMode)
                val selectedIndex = sortedProviders.indexOfFirst { it.key == currentProvider?.key }.takeIf { it >= 0 } ?: 0
                val notAvailableLabel = stringResource(R.string.not_available)
                val sortedEnabled = sortedProviders.map { isProviderAvailable(it.key) }
                val sortedSubtitles = sortedEnabled.map { if (it) null else notAvailableLabel }
                SelectionScreen(
                    title = stringResource(R.string.working_mode),
                    options = sortedOptions,
                    selectedOption = selectedIndex,
                    enabled = sortedEnabled,
                    subtitles = sortedSubtitles,
                    onSelect = { index ->
                        val chosen = sortedProviders[index]
                        if (chosen.modes.size == 1) {
                            val accepted = onWorkingModeChange(chosen.modes.first()) { workingMode = it }
                            if (!accepted) {
                                workingMode = HailData.MODE_DEFAULT
                                HailData.workingMode = HailData.MODE_DEFAULT
                            }
                            currentScreen = SettingsScreen.MAIN
                        } else {
                            // Sort modes alphabetically
                            val sortedModes = chosen.modes.sortedBy { getString(HailData.labelResForMode(it)) }
                            val accepted = onWorkingModeChange(sortedModes.first()) { workingMode = it }
                            if (!accepted) {
                                workingMode = HailData.MODE_DEFAULT
                                HailData.workingMode = HailData.MODE_DEFAULT
                            }
                            currentScreen = if (accepted) SettingsScreen.MODE_SELECTION else SettingsScreen.MAIN
                        }
                    },
                    onBack = { currentScreen = SettingsScreen.WORKING_MODE }
                )
            }
            SettingsScreen.MODE_SELECTION -> {
                val provider = HailData.providerForMode(workingMode)
                // Sort modes alphabetically by label
                val sortedModes = (provider?.modes ?: emptyList()).sortedBy { getString(HailData.labelResForMode(it)) }
                val sortedOptions = sortedModes.map { getString(HailData.labelResForMode(it)) }
                val selectedIndex = sortedModes.indexOf(workingMode).takeIf { it >= 0 } ?: 0
                SelectionScreen(
                    title = stringResource(R.string.mode),
                    options = sortedOptions,
                    selectedOption = selectedIndex,
                    onSelect = { index ->
                        val chosenMode = sortedModes.get(index) ?: return@SelectionScreen
                        val accepted = onWorkingModeChange(chosenMode) { workingMode = it }
                        if (!accepted) {
                            workingMode = HailData.MODE_DEFAULT
                            HailData.workingMode = HailData.MODE_DEFAULT
                        }
                        currentScreen = SettingsScreen.MAIN
                    },
                    onBack = { currentScreen = SettingsScreen.WORKING_MODE }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun MainSettingsScreen(
        workingMode: String,
        onWorkingModeChange: (String) -> Unit,
        onNavigateToWorkingMode: () -> Unit
    ) {
        val context = LocalContext.current
        val scrollState = rememberScrollState()
        val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
        val appThemeEntries = stringArrayResource(R.array.app_theme_entries)
        val tileActionEntries = stringArrayResource(R.array.tile_action_entries)
        val dynamicShortcutEntries = stringArrayResource(R.array.dynamic_shortcut_entries)

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = { Text(stringResource(R.string.title_settings), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                // ── General ──
                SettingsSectionHeader(stringResource(R.string.section_general))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    val generalItems = 6
                SegmentedListItem(
                        onClick = { onNavigateToWorkingMode() },
                        supportingContent = { Text(getString(HailData.providerForMode(workingMode)?.labelRes ?: R.string.provider_idle)) },
                        leadingContent = { Icon(Icons.Outlined.Adb, contentDescription = null) },
                        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                        shapes = ListItemDefaults.segmentedShapes(index = 0, count = generalItems),
                        colors = ListItemDefaults.segmentedColors()
                    ) {
                        Text(stringResource(R.string.working_mode))
                    }
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.fuzzy_search)) },
                        checked = fuzzySearch,
                        onCheckedChange = {
                            fuzzySearch = it
                            HailData.fuzzySearch = it
                        },
                        leadingContent = { Icon(Icons.AutoMirrored.Outlined.ManageSearch, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.nine_key)) },
                        checked = nineKeySearch,
                        onCheckedChange = {
                            nineKeySearch = it
                            HailData.nineKeySearch = it
                        },
                        leadingContent = { Icon(Icons.Outlined.Dialpad, contentDescription = null) },
                    )
                    SettingsList(
                        headlineContent = { Text(stringResource(R.string.tile_action)) },
                        selectedValue = tileAction,
                        onValueChange = { value ->
                            tileAction = value
                            HailData.tileAction = value
                            true
                        },
                        values = HailData.TILE_ACTION_VALUES,
                        entriesId = R.array.tile_action_entries,
                        leadingContent = { Icon(Icons.Outlined.DashboardCustomize, contentDescription = null) },
                        supportingContent = {
                            val index = HailData.TILE_ACTION_VALUES.indexOf(tileAction)
                            Text(tileActionEntries.getOrElse(index) { tileAction })
                        },
                    )
                    SettingsList(
                        headlineContent = { Text(stringResource(R.string.dynamic_shortcut_action)) },
                        selectedValue = dynamicShortcutAction,
                        onValueChange = { action ->
                            HShortcuts.removeAllDynamicShortcuts()
                            HShortcuts.addDynamicShortcutAction(action)
                            dynamicShortcutAction = action
                            HailData.dynamicShortcutAction = action
                            true
                        },
                        values = HailData.DYNAMIC_SHORTCUT_ACTIONS,
                        entriesId = R.array.dynamic_shortcut_entries,
                        leadingContent = { Icon(Icons.Outlined.AppShortcut, contentDescription = null) },
                        supportingContent = {
                            val index = HailData.DYNAMIC_SHORTCUT_ACTIONS.indexOf(dynamicShortcutAction)
                            Text(dynamicShortcutEntries.getOrElse(index) { dynamicShortcutAction })
                        },
                    )
                    SettingsClickable(
                        headlineContent = { Text(stringResource(R.string.action_add_pin_shortcut)) },
                        onClick = ::addPinShortcut,
                        leadingContent = { Icon(Icons.AutoMirrored.Outlined.Shortcut, contentDescription = null) },
                    )
                }

                // ── Appearance ──
                SettingsSectionHeader(stringResource(R.string.section_appearance))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
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
                        },
                    )
                    SettingsList(
                        headlineContent = { Text(stringResource(R.string.icon_pack)) },
                        selectedValue = iconPack,
                        onValueChange = { value ->
                            AppIconCache.clear()
                            iconPack = value
                            HailData.iconPack = value
                            true
                        },
                        values = iconPackValues,
                        entries = iconPackValues.map { iconPackName(it) },
                        leadingContent = { Icon(Icons.Outlined.Palette, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.grayscale_icon)) },
                        checked = grayscaleIcon,
                        onCheckedChange = {
                            grayscaleIcon = it
                            HailData.grayscaleIcon = it
                        },
                        leadingContent = { Icon(Icons.Outlined.FilterBAndW, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.compact_icon)) },
                        checked = compactIcon,
                        onCheckedChange = {
                            compactIcon = it
                            HailData.compactIcon = it
                        },
                        leadingContent = { Icon(Icons.Outlined.Apps, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.synthesize_adaptive_icons)) },
                        checked = synthesizeAdaptiveIcons,
                        onCheckedChange = {
                            synthesizeAdaptiveIcons = it
                            HailData.synthesizeAdaptiveIcons = it
                        },
                        leadingContent = { Icon(Icons.Outlined.Layers, contentDescription = null) },
                    )
                    SettingsSlider(
                        headlineContent = { Text(stringResource(R.string.home_font_size)) },
                        value = homeFontSize,
                        onValueChange = {
                            homeFontSize = it
                            HailData.homeFontSize = it
                        },
                        valueRange = 11f..16f,
                        valueSteps = 4,
                        leadingContent = { Icon(Icons.Outlined.TextFields, contentDescription = null) },
                    )
                }

                // ── Security ──
                SettingsSectionHeader(stringResource(R.string.section_security))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.action_biometric)) },
                        checked = biometricLogin,
                        onCheckedChange = { value ->
                            if (value) resetDynamicShortcuts()
                            biometricLogin = value
                            HailData.biometricLogin = value
                        },
                        leadingContent = { Icon(Icons.Outlined.Fingerprint, contentDescription = null) },
                    )
                }

                // ── Automation ──
                SettingsSectionHeader(stringResource(R.string.section_automation))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.auto_freeze_after_lock)) },
                        checked = autoFreezeAfterLock,
                        onCheckedChange = { value ->
                            autoFreezeAfterLock = value
                            HailData.autoFreezeAfterLock = value
                            app.setAutoFreezeService(value)
                        },
                        leadingContent = { Icon(Icons.Outlined.ScreenLockPortrait, contentDescription = null) },
                    )
                    SettingsSlider(
                        headlineContent = { Text(stringResource(R.string.auto_freeze_delay)) },
                        value = autoFreezeDelay.toFloat(),
                        onValueChange = {
                            autoFreezeDelay = it.toLong()
                            HailData.autoFreezeDelay = it.toLong()
                        },
                        valueRange = 0f..30f,
                        valueSteps = 29,
                        enabled = autoFreezeAfterLock,
                        leadingContent = { Icon(Icons.Outlined.LockClock, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.skip_while_charging)) },
                        checked = skipWhileCharging,
                        onCheckedChange = {
                            skipWhileCharging = it
                            HailData.skipWhileCharging = it
                        },
                        enabled = autoFreezeAfterLock,
                        leadingContent = { Icon(Icons.Outlined.BatteryChargingFull, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.skip_foreground_app)) },
                        checked = skipForegroundApp,
                        onCheckedChange = { value ->
                            if (value && !HSystem.checkOpUsageStats(context)) {
                                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                false
                            } else {
                                skipForegroundApp = value
                                HailData.skipForegroundApp = value
                                true
                            }
                        },
                        enabled = autoFreezeAfterLock,
                        leadingContent = { Icon(Icons.Outlined.Android, contentDescription = null) },
                    )
                    SettingsSwitch(
                        headlineContent = { Text(stringResource(R.string.skip_notifying_app)) },
                        checked = skipNotifyingApp,
                        onCheckedChange = { value ->
                            val isGranted = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
                            if (value && !isGranted) {
                                app.setAutoFreezeServiceEnabled(true)
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                false
                            } else {
                                skipNotifyingApp = value
                                HailData.skipNotifyingApp = value
                                true
                            }
                        },
                        enabled = autoFreezeAfterLock,
                        leadingContent = { Icon(Icons.Outlined.NotificationsActive, contentDescription = null) },
                    )
                }

                 // ── Advanced ──
                SettingsSectionHeader(stringResource(R.string.section_advanced))
                Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                    SettingsClickable(
                        headlineContent = { Text(stringResource(R.string.action_clear_dynamic_shortcuts)) },
                        onClick = ::resetDynamicShortcuts,
                        leadingContent = { Icon(Icons.Outlined.CleaningServices, contentDescription = null) },
                    )
                    SettingsClickable(
                        headlineContent = { Text(stringResource(R.string.action_rebuild_cache)) },
                        onClick = ::confirmRebuildCache,
                        supportingContent = { Text(stringResource(R.string.summary_rebuild_cache)) },
                        leadingContent = { Icon(Icons.Outlined.DeleteSweep, contentDescription = null) },
                    )
                    SettingsClickable(
                        headlineContent = { Text(stringResource(R.string.allow_background_activity)) },
                        onClick = ::requestBackgroundActivity,
                        supportingContent = { Text(stringResource(R.string.summary_background_activity)) },
                        leadingContent = { Icon(Icons.Outlined.BatterySaver, contentDescription = null) },
                    )
                    if (workingMode.startsWith(HailData.SU) ||
                        workingMode.startsWith(HailData.SHIZUKU) ||
                        workingMode.startsWith(HailData.DHIZUKU) ||
                        workingMode.startsWith(HailData.OWNER)
                    ) {
                        SettingsClickable(
                            headlineContent = { Text(stringResource(R.string.action_terminal)) },
                            onClick = ::showTerminalDialog,
                            leadingContent = { Icon(Icons.Outlined.Terminal, contentDescription = null) },
                        )
                    }
                    SettingsClickable(
                        headlineContent = { Text(stringResource(R.string.title_about)) },
                        onClick = { findNavController().navigate(R.id.nav_about) },
                        leadingContent = { Icon(Icons.Outlined.Info, contentDescription = null) },
                    )
                }
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
                SegmentedListItem(
                    onClick = { onNavigateToProviderSelection() },
                    supportingContent = { Text(getString(currentProvider?.labelRes ?: R.string.provider_idle)) },
                    leadingContent = { Icon(Icons.Outlined.Adb, contentDescription = null) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shapes = ListItemDefaults.segmentedShapes(index = 0, count = 2),
                    colors = ListItemDefaults.segmentedColors()
                ) {
                    Text(stringResource(R.string.provider))
                }
                SegmentedListItem(
                    onClick = { onNavigateToModeSelection() },
                    enabled = (currentProvider?.modes?.size ?: 0) > 1,
                    supportingContent = { Text(getString(HailData.labelResForMode(workingMode))) },
                    leadingContent = { Icon(Icons.Outlined.Tune, contentDescription = null) },
                    trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
                    shapes = ListItemDefaults.segmentedShapes(index = 1, count = 2),
                    colors = ListItemDefaults.segmentedColors()
                ) {
                    Text(stringResource(R.string.mode))
                }
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
        onBack: () -> Unit,
        enabled: List<Boolean> = options.map { true },
        subtitles: List<String?> = options.map { null }
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
                    .animateContentSize(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                options.forEachIndexed { index, option ->
                    val selected = index == selectedOption
                    val isEnabled = enabled.getOrElse(index) { true }
                    val subtitle = subtitles.getOrElse(index) { null }
                    val interactionSource = remember { MutableInteractionSource() }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = if (subtitle != null) 64.dp else 48.dp)
                            .selectable(
                                selected = selected,
                                enabled = isEnabled,
                                role = Role.RadioButton,
                                interactionSource = interactionSource,
                                onClick = { onSelect(index) }
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                interactionSource = interactionSource,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = when {
                                    !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    selected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        if (subtitle != null) {
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                                modifier = Modifier.padding(start = 36.dp)
                            )
                        }
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
    ) = SegmentedListItem(
        onClick = onClick,
        enabled = enabled,
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Filled.ChevronRight, contentDescription = null) },
        shapes = ListItemDefaults.segmentedShapes(index = 0, count = 1),
        colors = ListItemDefaults.segmentedColors()
    ) {
        Text(title)
    }

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

    private fun isProviderAvailable(key: String): Boolean = when (key) {
        "idle" -> true
        "shizuku" -> runCatching { Shizuku.pingBinder() }.getOrDefault(false)
        "su" -> HShell.checkSU
        "dhizuku" -> runCatching { Dhizuku.init(app) }.getOrDefault(false)
        "owner" -> HPolicy.isDeviceOwnerActive
        "island" -> HPackages.getApplicationInfoOrNull("com.oasisfeng.island") != null ||
            HPackages.getApplicationInfoOrNull("com.oasisfeng.island.fdroid") != null
        "privapp" -> HPackages.isPrivilegedApp(app.packageName)
        else -> true
    }

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
            R.id.action_remove_owner -> (requireActivity() as MainActivity).ownerRemoveDialog()
        }
        return false
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_settings, menu)
    }

    override fun onPrepareMenu(menu: Menu) {
        super.onPrepareMenu(menu)
        if (HPolicy.isDeviceOwnerActive) menu.findItem(R.id.action_remove_owner).isVisible = true
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