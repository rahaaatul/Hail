package com.aistra.hail.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.aistra.hail.ui.theme.HailTheme
import com.aistra.hail.ui.theme.HailThemeState

@Composable
fun HailNavHost(
    modifier: Modifier = Modifier,
    themeState: HailThemeState = HailThemeState(),
) {
    HailTheme(state = themeState) {
        val backStack = rememberNavBackStack(HomeRoute)
        NavDisplay(
            modifier = modifier,
            backStack = backStack,
            onBack = { backStack.removeLastOrNull() },
            entryProvider = entryProvider {
                entry<HomeRoute> { Text("Home") }
                entry<AppsRoute> { Text("Apps") }
                entry<ActionsRoute> { Text("Actions") }
                entry<SettingsRoute> { Text("Settings") }
                entry<AboutRoute> { Text("About") }
            }
        )
    }
}