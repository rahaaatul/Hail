package com.aistra.hail.ui.theme

import androidx.annotation.StringRes
import com.aistra.hail.R
import com.aistra.hail.app.HailData

enum class ThemeMode {
    LIGHT, DARK, SYSTEM;

    companion object {
        fun fromAppTheme(value: String): ThemeMode = when (value) {
            HailData.THEME_LIGHT -> LIGHT
            HailData.THEME_DARK -> DARK
            else -> SYSTEM
        }
    }
}

enum class PaletteStyle {
    TonalSpot, Neutral, Vibrant, Expressive, Rainbow,
    FruitSalad, Monochrome, Fidelity, Content;

    val supportsSpec2025: Boolean
        get() = this in setOf(TonalSpot, Neutral, Vibrant, Expressive)

    companion object {
        fun fromValueOrDefault(value: String): PaletteStyle =
            entries.find { it.name == value } ?: TonalSpot
    }
}

enum class ThemeColorSpec {
    SPEC_2021, SPEC_2025;

    companion object {
        fun fromValueOrDefault(value: String): ThemeColorSpec =
            entries.find { it.name == value } ?: SPEC_2025
    }
}

@StringRes fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
    ThemeMode.SYSTEM -> R.string.theme_mode_system
}

@StringRes fun PaletteStyle.labelRes(): Int = when (this) {
    PaletteStyle.TonalSpot -> R.string.palette_style_tonal_spot
    PaletteStyle.Neutral -> R.string.palette_style_neutral
    PaletteStyle.Vibrant -> R.string.palette_style_vibrant
    PaletteStyle.Expressive -> R.string.palette_style_expressive
    PaletteStyle.Rainbow -> R.string.palette_style_rainbow
    PaletteStyle.FruitSalad -> R.string.palette_style_fruit_salad
    PaletteStyle.Monochrome -> R.string.palette_style_monochrome
    PaletteStyle.Fidelity -> R.string.palette_style_fidelity
    PaletteStyle.Content -> R.string.palette_style_content
}

@StringRes fun ThemeColorSpec.labelRes(): Int = when (this) {
    ThemeColorSpec.SPEC_2021 -> R.string.color_spec_2021
    ThemeColorSpec.SPEC_2025 -> R.string.color_spec_2025
}