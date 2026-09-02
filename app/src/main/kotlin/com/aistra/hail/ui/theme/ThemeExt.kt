package com.aistra.hail.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle
import com.materialkolor.dynamicColorScheme as materialKolorDynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
): ColorScheme {
    val mkStyle = when (style) {
        PaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
        PaletteStyle.Content -> MaterialKolorPaletteStyle.Content
    }
    val specVersion = when (colorSpec) {
        ThemeColorSpec.SPEC_2025 -> if (style.supportsSpec2025) ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.SPEC_2021
        ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    }
    return materialKolorDynamicColorScheme(
        seedColor = keyColor,
        isDark = isDark,
        style = mkStyle,
        specVersion = specVersion,
    )
}

@Composable
fun ColorScheme.animateColorScheme(): ColorScheme {
    @Composable
    fun anim(color: Color) = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color",
    ).value
    return ColorScheme(
        primary = anim(primary),
        onPrimary = anim(onPrimary),
        primaryContainer = anim(primaryContainer),
        onPrimaryContainer = anim(onPrimaryContainer),
        inversePrimary = anim(inversePrimary),
        secondary = anim(secondary),
        onSecondary = anim(onSecondary),
        secondaryContainer = anim(secondaryContainer),
        onSecondaryContainer = anim(onSecondaryContainer),
        tertiary = anim(tertiary),
        onTertiary = anim(onTertiary),
        tertiaryContainer = anim(tertiaryContainer),
        onTertiaryContainer = anim(onTertiaryContainer),
        background = anim(background),
        onBackground = anim(onBackground),
        surface = anim(surface),
        onSurface = anim(onSurface),
        surfaceVariant = anim(surfaceVariant),
        onSurfaceVariant = anim(onSurfaceVariant),
        surfaceTint = anim(surfaceTint),
        inverseSurface = anim(inverseSurface),
        inverseOnSurface = anim(inverseOnSurface),
        error = anim(error),
        onError = anim(onError),
        errorContainer = anim(errorContainer),
        onErrorContainer = anim(onErrorContainer),
        outline = anim(outline),
        outlineVariant = anim(outlineVariant),
        scrim = anim(scrim),
        surfaceBright = anim(surfaceBright),
        surfaceDim = anim(surfaceDim),
        surfaceContainer = anim(surfaceContainer),
        surfaceContainerHigh = anim(surfaceContainerHigh),
        surfaceContainerHighest = anim(surfaceContainerHighest),
        surfaceContainerLow = anim(surfaceContainerLow),
        surfaceContainerLowest = anim(surfaceContainerLowest),
        primaryFixed = anim(primaryFixed),
        primaryFixedDim = anim(primaryFixedDim),
        onPrimaryFixed = anim(onPrimaryFixed),
        onPrimaryFixedVariant = anim(onPrimaryFixedVariant),
        secondaryFixed = anim(secondaryFixed),
        secondaryFixedDim = anim(secondaryFixedDim),
        onSecondaryFixed = anim(onSecondaryFixed),
        onSecondaryFixedVariant = anim(onSecondaryFixedVariant),
        tertiaryFixed = anim(tertiaryFixed),
        tertiaryFixedDim = anim(tertiaryFixedDim),
        onTertiaryFixed = anim(onTertiaryFixed),
        onTertiaryFixedVariant = anim(onTertiaryFixedVariant),
    )
}
