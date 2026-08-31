package com.aistra.hail.ui.settings

import androidx.annotation.ArrayRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

enum class ListPreferenceType { ALERT_DIALOG, DROPDOWN_MENU }

@Composable
fun SettingsSwitch(
    headlineContent: @Composable () -> Unit,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    val toggleableModifier = if (enabled) {
        modifier.toggleable(
            value = checked,
            role = Role.Switch,
            enabled = true,
            onValueChange = onCheckedChange
        )
    } else {
        modifier
    }
    ListItem(
        modifier = toggleableModifier,
        headlineContent = headlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    )
}

@Composable
fun SettingsSlider(
    headlineContent: @Composable () -> Unit,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueSteps: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) {
    var sliderValue by remember { mutableStateOf(value) }
    // Update local state when parent value changes
    if (sliderValue != value) {
        sliderValue = value
    }

    ListItem(
        modifier = modifier
            .fillMaxWidth(),
        headlineContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                headlineContent()
                Slider(
                    value = sliderValue,
                    onValueChange = { newValue ->
                        sliderValue = newValue
                        onValueChange(newValue)
                    },
                    valueRange = valueRange,
                    steps = valueSteps,
                    enabled = enabled
                )
            }
        },
        supportingContent = supportingContent,
        leadingContent = leadingContent
    )
}

@Composable
private fun SettingsListInternal(
    headlineContent: @Composable () -> Unit,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    values: List<String>,
    entries: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
) {
    var expanded by remember { mutableStateOf(false) }

    when (type) {
        ListPreferenceType.ALERT_DIALOG -> {
            ListItem(
                modifier = modifier
                    .clickable(enabled = enabled) { expanded = true }
                    .fillMaxWidth(),
                headlineContent = headlineContent,
                supportingContent = supportingContent,
                leadingContent = leadingContent
            )
            if (expanded) {
                AlertDialog(
                    onDismissRequest = { expanded = false },
                    title = { headlineContent() },
                    text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            values.forEachIndexed { index, value ->
                                val selected = value == selectedValue
                                Text(
                                    text = entries.getOrElse(index) { value },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .selectable(
                                            selected = selected,
                                            role = Role.RadioButton,
                                            onClick = {
                                                onValueChange(value)
                                                expanded = false
                                            }
                                        )
                                        .padding(vertical = 12.dp, horizontal = 24.dp)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { expanded = false }) {
                            Text(text = stringResource(android.R.string.cancel))
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                )
            }
        }

        ListPreferenceType.DROPDOWN_MENU -> {
            Box {
                ListItem(
                    modifier = modifier
                        .clickable(enabled = enabled) { expanded = !expanded }
                        .fillMaxWidth(),
                    headlineContent = headlineContent,
                    supportingContent = supportingContent,
                    leadingContent = leadingContent
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    values.forEachIndexed { index, value ->
                        DropdownMenuItem(
                            text = { Text(text = entries.getOrElse(index) { value }) },
                            onClick = {
                                onValueChange(value)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsList(
    headlineContent: @Composable () -> Unit,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    values: List<String>,
    entries: List<String>,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
) = SettingsListInternal(
    headlineContent = headlineContent,
    selectedValue = selectedValue,
    onValueChange = onValueChange,
    values = values,
    entries = entries,
    modifier = modifier,
    enabled = enabled,
    supportingContent = supportingContent,
    leadingContent = leadingContent,
    type = type
)

@Composable
fun SettingsList(
    headlineContent: @Composable () -> Unit,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    values: List<String>,
    @ArrayRes entriesId: Int,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
    type: ListPreferenceType = ListPreferenceType.DROPDOWN_MENU,
) {
    val entries = stringArrayResource(entriesId).toList()
    SettingsListInternal(
        headlineContent = headlineContent,
        selectedValue = selectedValue,
        onValueChange = onValueChange,
        values = values,
        entries = entries,
        modifier = modifier,
        enabled = enabled,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        type = type
    )
}

@Composable
fun SettingsClickable(
    headlineContent: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    supportingContent: (@Composable () -> Unit)? = null,
    leadingContent: (@Composable () -> Unit)? = null,
) = ListItem(
    modifier = modifier.clickable(enabled = enabled, onClick = onClick),
    headlineContent = headlineContent,
    supportingContent = supportingContent,
    leadingContent = leadingContent
)

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsHorizontalDivider() = HorizontalDivider()
