@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.aistra.hail.ui.theme

import androidx.activity.ComponentActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.HTarget

data class HailThemeState(
    val themeMode: ThemeMode = ThemeMode.fromAppTheme(HailData.appTheme),
    val paletteStyle: PaletteStyle = HailData.paletteStyle,
    val colorSpec: ThemeColorSpec = HailData.colorSpec,
    val useDynamicColor: Boolean = HailData.useDynamicColor,
    val seedColor: Int = HailData.seedColor,
)

private val LocalHailColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }

private val LocalIsDark = staticCompositionLocalOf { false }

object HailTheme {
    val colorScheme: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalHailColorScheme.current

    val isDark: Boolean
        @Composable
        @ReadOnlyComposable
        get() = LocalIsDark.current
}

@Composable
fun HailTheme(
    state: HailThemeState,
    content: @Composable () -> Unit
) {
    val isDark = when (state.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val keyColor = if (state.useDynamicColor && HTarget.S) {
        Color(LocalContext.current.getColor(android.R.color.system_accent1_500))
    } else {
        Color(state.seedColor)
    }
    val baseColorScheme = remember(keyColor, isDark, state.paletteStyle, state.colorSpec) {
        dynamicColorScheme(
            keyColor = keyColor,
            isDark = isDark,
            style = state.paletteStyle,
            colorSpec = state.colorSpec,
        )
    }
    val colorScheme = baseColorScheme.animateColorScheme()
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? ComponentActivity)?.window ?: return@SideEffect
        window.statusBarColor = colorScheme.surface.toArgb()
        window.navigationBarColor = colorScheme.surface.toArgb()
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
    }
    CompositionLocalProvider(
        LocalHailColorScheme provides colorScheme,
        LocalIsDark provides isDark,
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            shapes = expressiveShapes,
            content = content,
        )
    }
}
