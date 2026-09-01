package com.aistra.hail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HTarget

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable() () -> Unit
) {
    val colorScheme = colorSchemeFromHailData(darkTheme)

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        motionScheme = MotionScheme.expressive(),
        shapes = expressiveShapes,
        content = content
    )
}

@Composable
private fun colorSchemeFromHailData(isDark: Boolean): ColorScheme {
    val data = HailData
    return when {
        data.useDynamicColor && HTarget.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> colorSchemeFromSeed(data.seedColor, isDark, data.paletteStyle)
    }
}

fun colorSchemeFromSeed(seedColor: Int, isDark: Boolean, style: PaletteStyle): ColorScheme {
    val seed = Color(seedColor)
    return when {
        style.supportsSpec2025 && !isDark -> expressiveLightColorScheme().copy(primary = seed)
        style.supportsSpec2025 -> darkColorScheme().copy(primary = seed)
        isDark -> darkColorScheme(primary = seed)
        else -> lightColorScheme(primary = seed)
    }
}
