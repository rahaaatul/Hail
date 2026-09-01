package com.aistra.hail.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aistra.hail.HailApp
import com.aistra.hail.R
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.theme.AppTheme
import com.aistra.hail.ui.theme.PaletteStyle
import com.aistra.hail.ui.theme.ThemeColorSpec

class ThemeSettingsFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                AppTheme {
                    ThemeSettingsScreen(onBack = { findNavController().popBackStack() })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSettingsScreen(onBack: () -> Unit) {
    var paletteStyle by remember { mutableStateOf(HailData.paletteStyle) }
    var colorSpec by remember { mutableStateOf(HailData.colorSpec) }
    var useDynamicColor by remember { mutableStateOf(HailData.useDynamicColor) }
    var seedColor by remember { mutableStateOf(HailData.seedColor) }

    val paletteStyleValues = PaletteStyle.entries.toList()
    val paletteStyleEntries = paletteStyleValues.map { it.displayName }
    val colorSpecValues = ThemeColorSpec.entries.toList()
    val colorSpecEntries = colorSpecValues.map { it.displayName }
    val scrollState = rememberScrollState()
    val colorSpecEnabled = paletteStyle.supportsSpec2025

    LaunchedEffect(paletteStyle, colorSpec, useDynamicColor, seedColor) {
        HailApp.app.setAppTheme(HailData.appTheme)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_theme_settings)) },
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
                .verticalScroll(scrollState)
        ) {
            SettingsSectionHeader(stringResource(R.string.theme_palette_style))
            SettingsList(
                headlineContent = { Text(stringResource(R.string.theme_palette_style)) },
                selectedValue = paletteStyle.name,
                onValueChange = { value ->
                    paletteStyle = PaletteStyle.fromValueOrDefault(value)
                    HailData.paletteStyle = paletteStyle
                },
                values = paletteStyleValues.map { it.name },
                entries = paletteStyleEntries,
            )

            SettingsSectionHeader(stringResource(R.string.theme_color_spec))
            SettingsList(
                headlineContent = { Text(stringResource(R.string.theme_color_spec)) },
                selectedValue = colorSpec.name,
                onValueChange = { value ->
                    colorSpec = ThemeColorSpec.fromValueOrDefault(value)
                    HailData.colorSpec = colorSpec
                },
                values = colorSpecValues.map { it.name },
                entries = colorSpecEntries,
                enabled = colorSpecEnabled,
            )

            SettingsSectionHeader(stringResource(R.string.theme_dynamic_color))
            SettingsSwitch(
                headlineContent = { Text(stringResource(R.string.theme_dynamic_color)) },
                supportingContent = { Text(stringResource(R.string.theme_dynamic_color_desc)) },
                checked = useDynamicColor,
                onCheckedChange = {
                    useDynamicColor = it
                    HailData.useDynamicColor = it
                },
            )

            if (!useDynamicColor) {
                SettingsSectionHeader(stringResource(R.string.theme_seed_color))
                ColorSwatchRow(
                    currentColor = seedColor,
                    onColorSelected = { color ->
                        seedColor = color
                        HailData.seedColor = color
                    }
                )
            }
        }
    }
}

@Composable
private fun ColorSwatchRow(currentColor: Int, onColorSelected: (Int) -> Unit) {
    val presetColors = listOf(
        0xFF6750A4.toInt(),
        0xFF625B71.toInt(),
        0xFF7D5260.toInt(),
        0xFFB3261E.toInt(),
        0xFF006D40.toInt(),
        0xFF004D40.toInt(),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        presetColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .then(
                        if (currentColor == color) {
                            Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onColorSelected(color) },
                contentAlignment = Alignment.Center
            ) {
                if (currentColor == color) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
