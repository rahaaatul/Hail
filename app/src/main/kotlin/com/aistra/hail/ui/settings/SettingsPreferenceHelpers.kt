package com.aistra.hail.ui.settings

import android.content.res.Resources
import android.util.Log
import androidx.annotation.ArrayRes
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import com.aistra.hail.BuildConfig
import com.aistra.hail.HailApp.Companion.app
import com.aistra.hail.R
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.aistra.hail.utils.HShortcuts
import com.aistra.hail.utils.HSystem
import me.zhanghai.compose.preference.preference
import androidx.compose.material.icons.automirrored.outlined.Shortcut
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.AppIconCache
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ListPreferenceType
import me.zhanghai.compose.preference.preferenceCategory
import me.zhanghai.compose.preference.SwitchPreference
import me.zhanghai.compose.preference.sliderPreference
import me.zhanghai.compose.preference.rememberPreferenceState

internal fun LazyListScope.horizontalDivider() = item { HorizontalDivider() }

internal fun LazyListScope.switchPreference(
    rememberState: @Composable () -> MutableState<Boolean>,
    onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
    @StringRes titleId: Int,
    enabled: Boolean = true,
    icon: ImageVector,
) = item(key = titleId, contentType = "SwitchPreference") {
    if (BuildConfig.DEBUG) {
        SideEffect {
            Log.d("SettingsRecompose", "switchPreference(${app.getString(titleId)}) recomposed")
        }
    }
    val state = rememberState()
    SwitchPreference(
        value = state.value,
        onValueChange = { if (onValueChange(state, it)) state.value = it },
        title = { Text(text = stringResource(titleId)) },
        enabled = enabled,
        icon = { Icon(imageVector = icon, contentDescription = null) },
    )
}

internal fun LazyListScope.switchPreference(
    key: String,
    defaultValue: Boolean,
    onValueChange: (MutableState<Boolean>, Boolean) -> Boolean = { _, _ -> true },
    @StringRes titleId: Int,
    enabled: Boolean = true,
    icon: ImageVector,
) = switchPreference(
    rememberState = { rememberPreferenceState(key, defaultValue) },
    onValueChange = onValueChange,
    titleId = titleId,
    enabled = enabled,
    icon = icon,
)

internal fun LazyListScope.listPreference(
    key: String,
    defaultValue: String,
    onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
    values: List<String>,
    @StringRes titleId: Int,
    icon: ImageVector,
    summary: @Composable (String) -> String,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    valueToText: (String) -> String,
) = item(key = key, contentType = "ListPreference") {
    if (BuildConfig.DEBUG) {
        SideEffect {
            Log.d("SettingsRecompose", "listPreference(${app.getString(titleId)}) recomposed")
        }
    }
    val state = rememberPreferenceState(key, defaultValue)
    ListPreference(
        value = state.value,
        onValueChange = { if (onValueChange(state, it)) state.value = it },
        values = values,
        title = { Text(text = stringResource(titleId)) },
        icon = { Icon(imageVector = icon, contentDescription = null) },
        summary = { Text(text = summary(state.value)) },
        type = type,
        valueToText = { AnnotatedString(valueToText(it)) },
    )
}

internal fun LazyListScope.listPreference(
    key: String,
    defaultValue: String,
    onValueChange: (MutableState<String>, String) -> Boolean = { _, _ -> true },
    values: List<String>,
    @ArrayRes entriesId: Int,
    @StringRes titleId: Int,
    icon: ImageVector,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
    resources: Resources,
) = listPreference(
    key = key,
    defaultValue = defaultValue,
    onValueChange = onValueChange,
    values = values,
    titleId = titleId,
    icon = icon,
    summary = { it.toEntry(values, entriesId, resources) },
    type = type,
    valueToText = { it.toEntry(values, entriesId, resources) },
)

internal fun String.toEntry(values: List<String>, @ArrayRes entriesId: Int, resources: Resources): String =
    resources.getStringArray(entriesId)[values.indexOf(this)]

@Composable
internal fun LazyListScope.workingModeSection(
    onWorkingModeChange: (MutableState<String>, String) -> Boolean,
    resetDynamicShortcuts: () -> Unit,
) {
    listPreference(
        key = HailData.WORKING_MODE,
        defaultValue = HailData.MODE_DEFAULT,
        onValueChange = onWorkingModeChange,
        values = HailData.WORKING_MODE_VALUES,
        entriesId = R.array.working_mode_entries,
        resources = LocalContext.current.resources,
        titleId = R.string.working_mode,
        icon = Icons.Outlined.Adb,
        type = ListPreferenceType.ALERT_DIALOG,
    )
    switchPreference(
        key = HailData.BIOMETRIC_LOGIN,
        defaultValue = false,
        onValueChange = { _, value ->
            if (value) resetDynamicShortcuts()
            true
        },
        titleId = R.string.action_biometric,
        icon = Icons.Outlined.Fingerprint,
    )
    horizontalDivider()
}

@Composable
internal fun LazyListScope.customizeSection(
    iconPackValues: List<String>,
    iconPackName: (String) -> String,
) {
    preferenceCategory(key = "customize", title = { Text(text = stringResource(R.string.title_customize)) })
    listPreference(
        key = HailData.APP_THEME,
        defaultValue = HailData.FOLLOW_SYSTEM,
        onValueChange = { _, value ->
            app.setAppTheme(value)
            true
        },
        values = HailData.APP_THEME_VALUES,
        entriesId = R.array.app_theme_entries,
        resources = LocalContext.current.resources,
        titleId = R.string.app_theme,
        icon = Icons.Outlined.DarkMode,
    )
    listPreference(
        key = HailData.ICON_PACK,
        defaultValue = HailData.ACTION_NONE,
        onValueChange = { _, _ ->
            AppIconCache.clear()
            true
        },
        values = iconPackValues,
        titleId = R.string.icon_pack,
        icon = Icons.Outlined.Palette,
        summary = { iconPackName(it) },
        valueToText = { iconPackName(it) },
    )
}

@Composable
internal fun LazyListScope.iconSettingsSection() {
    switchPreference(
        key = HailData.GRAYSCALE_ICON,
        defaultValue = true,
        titleId = R.string.grayscale_icon,
        icon = Icons.Outlined.FilterBAndW,
    )
    switchPreference(
        key = HailData.COMPACT_ICON,
        defaultValue = false,
        titleId = R.string.compact_icon,
        icon = Icons.Outlined.Apps,
    )
    switchPreference(
        key = HailData.SYNTHESIZE_ADAPTIVE_ICONS,
        defaultValue = false,
        titleId = R.string.synthesize_adaptive_icons,
        icon = Icons.Outlined.Layers,
    )
}

@Composable
internal fun LazyListScope.searchSection() {
    sliderPreference(
        key = HailData.HOME_FONT_SIZE,
        defaultValue = HailData.floatDefault(HailData.HOME_FONT_SIZE),
        title = { Text(text = stringResource(R.string.home_font_size)) },
        valueRange = HailData.floatRange(HailData.HOME_FONT_SIZE),
        valueSteps = 4,
        icon = { Icon(imageVector = Icons.Outlined.TextFields, contentDescription = null) },
        valueText = { Text(text = "%.0f".format(it)) },
    )
    switchPreference(
        key = HailData.FUZZY_SEARCH,
        defaultValue = false,
        titleId = R.string.fuzzy_search,
        icon = Icons.AutoMirrored.Outlined.ManageSearch,
    )
    switchPreference(
        key = HailData.NINE_KEY_SEARCH,
        defaultValue = false,
        titleId = R.string.nine_key,
        icon = Icons.Outlined.Dialpad,
    )
    listPreference(
        key = HailData.TILE_ACTION,
        defaultValue = HailData.tileAction,
        values = HailData.TILE_ACTION_VALUES,
        entriesId = R.array.tile_action_entries,
        resources = LocalContext.current.resources,
        titleId = R.string.tile_action,
        icon = Icons.Outlined.DashboardCustomize,
    )
    horizontalDivider()
}

@Composable
internal fun LazyListScope.autoFreezeSection(autoFreezeAfterLock: MutableState<Boolean>) {
    preferenceCategory(key = "auto_freeze", title = { Text(text = stringResource(R.string.auto_freeze)) })
    switchPreference(
        rememberState = { autoFreezeAfterLock },
        onValueChange = { _, value ->
            app.setAutoFreezeService(value)
            true
        },
        titleId = R.string.auto_freeze_after_lock,
        icon = Icons.Outlined.ScreenLockPortrait,
    )
    sliderPreference(
        key = HailData.AUTO_FREEZE_DELAY,
        defaultValue = HailData.floatDefault(HailData.AUTO_FREEZE_DELAY),
        title = { Text(text = stringResource(R.string.auto_freeze_delay)) },
        valueRange = HailData.floatRange(HailData.AUTO_FREEZE_DELAY),
        valueSteps = 29,
        enabled = { autoFreezeAfterLock.value },
        icon = { Icon(imageVector = Icons.Outlined.LockClock, contentDescription = null) },
        valueText = { Text(text = "%.0f".format(it)) },
    )
}

@Composable
internal fun LazyListScope.skipOptionsSection(autoFreezeAfterLock: MutableState<Boolean>) {
    val context = LocalContext.current
    switchPreference(
        key = HailData.SKIP_WHILE_CHARGING,
        defaultValue = false,
        titleId = R.string.skip_while_charging,
        enabled = autoFreezeAfterLock.value,
        icon = Icons.Outlined.BatteryChargingFull,
    )
    switchPreference(
        key = HailData.SKIP_FOREGROUND_APP,
        defaultValue = false,
        onValueChange = { _, value ->
            if (value && !HSystem.checkOpUsageStats(context)) {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                false
            } else {
                true
            }
        },
        titleId = R.string.skip_foreground_app,
        enabled = autoFreezeAfterLock.value,
        icon = Icons.Outlined.Android,
    )
    switchPreference(
        key = HailData.SKIP_NOTIFYING_APP,
        defaultValue = false,
        onValueChange = { _, value ->
            val isGranted = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
            if (value && !isGranted) {
                app.setAutoFreezeServiceEnabled(true)
                context.startActivity(
                    Intent(
                        Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS,
                    ).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
                false
            } else {
                true
            }
        },
        titleId = R.string.skip_notifying_app,
        enabled = autoFreezeAfterLock.value,
        icon = Icons.Outlined.NotificationsActive,
    )
    horizontalDivider()
}

@Composable
internal fun LazyListScope.shortcutsSection(
    onAddPinShortcut: () -> Unit,
    onResetDynamicShortcuts: () -> Unit,
) {
    preferenceCategory(key = "shortcuts", title = { Text(text = stringResource(R.string.title_shortcuts)) })
    preference(
        key = "add_pin_shortcut",
        title = { Text(text = stringResource(R.string.action_add_pin_shortcut)) },
        icon = { Icon(imageVector = Icons.AutoMirrored.Outlined.Shortcut, contentDescription = null) },
        onClick = { onAddPinShortcut() },
    )
    listPreference(
        key = HailData.DYNAMIC_SHORTCUT_ACTION,
        defaultValue = HailData.ACTION_NONE,
        onValueChange = { _, action ->
            HShortcuts.removeAllDynamicShortcuts()
            HShortcuts.addDynamicShortcutAction(action)
            true
        },
        values = HailData.DYNAMIC_SHORTCUT_ACTIONS,
        entriesId = R.array.dynamic_shortcut_entries,
        resources = LocalContext.current.resources,
        titleId = R.string.dynamic_shortcut_action,
        icon = Icons.Outlined.AppShortcut,
    )
    preference(
        key = "clear_dynamic_shortcuts",
        title = { Text(text = stringResource(R.string.action_clear_dynamic_shortcuts)) },
        icon = { Icon(imageVector = Icons.Outlined.CleaningServices, contentDescription = null) },
        onClick = { onResetDynamicShortcuts() },
    )
    horizontalDivider()
}

@Composable
internal fun LazyListScope.cacheSection(
    onConfirmRebuildCache: () -> Unit,
    onRequestBackgroundActivity: () -> Unit,
) {
    preferenceCategory(key = "cache", title = { Text(text = stringResource(R.string.title_cache)) })
    preference(
        key = "rebuild_cache",
        title = { Text(text = stringResource(R.string.action_rebuild_cache)) },
        summary = { Text(text = stringResource(R.string.summary_rebuild_cache)) },
        icon = { Icon(imageVector = Icons.Outlined.DeleteSweep, contentDescription = null) },
        onClick = { onConfirmRebuildCache() },
    )
    preference(
        key = "background_activity",
        title = { Text(text = stringResource(R.string.allow_background_activity)) },
        summary = { Text(text = stringResource(R.string.summary_background_activity)) },
        icon = { Icon(imageVector = Icons.Outlined.BatterySaver, contentDescription = null) },
        onClick = { onRequestBackgroundActivity() },
    )
    horizontalDivider()
}

@Composable
internal fun LazyListScope.backupSection(
    onShowBackupDialog: () -> Unit,
    onRestore: () -> Unit,
) {
    preferenceCategory(key = "backup", title = { Text(text = stringResource(R.string.title_backup)) })
    preference(
        key = "backup_preference",
        title = { Text(text = stringResource(R.string.action_backup)) },
        icon = { Icon(imageVector = Icons.Outlined.Backup, contentDescription = null) },
        onClick = { onShowBackupDialog() },
    )
    preference(
        key = "restore",
        title = { Text(text = stringResource(R.string.action_restore)) },
        icon = { Icon(imageVector = Icons.Outlined.FileDownload, contentDescription = null) },
        onClick = { onRestore() },
    )
}
