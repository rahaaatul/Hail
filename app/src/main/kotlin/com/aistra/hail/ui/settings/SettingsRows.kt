package com.aistra.hail.ui.settings

import androidx.annotation.ArrayRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ListItemShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
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
import androidx.compose.ui.window.DialogProperties

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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
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
    SegmentedListItem(
        modifier = toggleableModifier,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        },
        shapes = shapes ?: ListItemDefaults.shapes(),
        colors = colors ?: ListItemDefaults.segmentedColors()
    ) {
        headlineContent()
    }
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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
) {
    var sliderValue by remember { mutableStateOf(value) }
    if (sliderValue != value) {
        sliderValue = value
    }

    SegmentedListItem(
        modifier = modifier
            .fillMaxWidth(),
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        shapes = shapes ?: ListItemDefaults.shapes(),
        colors = colors ?: ListItemDefaults.segmentedColors()
    ) {
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
    }
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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
) {
    var expanded by remember { mutableStateOf(false) }

    when (type) {
        ListPreferenceType.ALERT_DIALOG -> {
            SegmentedListItem(
                onClick = { expanded = true },
                modifier = modifier
                    .fillMaxWidth(),
                supportingContent = supportingContent,
                leadingContent = leadingContent,
                shapes = shapes ?: ListItemDefaults.shapes(),
                colors = colors ?: ListItemDefaults.segmentedColors()
            ) {
                headlineContent()
            }
            if (expanded) {
                AlertDialog(
                    onDismissRequest = { expanded = false },
                    title = { headlineContent() },
                    text = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
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
                    modifier = Modifier.widthIn(min = 280.dp, max = 560.dp),
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                )
            }
        }

        ListPreferenceType.DROPDOWN_MENU -> {
            Box {
                SegmentedListItem(
                    onClick = { expanded = !expanded },
                    modifier = modifier
                        .fillMaxWidth(),
                    supportingContent = supportingContent,
                    leadingContent = leadingContent,
                    shapes = shapes ?: ListItemDefaults.shapes(),
                    colors = colors ?: ListItemDefaults.segmentedColors()
                ) {
                    headlineContent()
                }
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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
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
    type = type,
    shapes = shapes ?: ListItemDefaults.shapes(),
    colors = colors ?: ListItemDefaults.segmentedColors()
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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
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
        type = type,
        shapes = shapes ?: ListItemDefaults.shapes(),
        colors = colors ?: ListItemDefaults.segmentedColors()
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
    shapes: ListItemShapes? = null,
    colors: ListItemColors? = null,
) = SegmentedListItem(
    onClick = onClick,
    modifier = modifier,
    supportingContent = supportingContent,
    leadingContent = leadingContent,
    shapes = shapes ?: ListItemDefaults.shapes(),
    colors = colors ?: ListItemDefaults.segmentedColors()
) {
    headlineContent()
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMediumEmphasized,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
