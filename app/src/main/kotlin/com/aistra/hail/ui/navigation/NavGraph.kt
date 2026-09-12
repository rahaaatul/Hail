package com.aistra.hail.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable

/**
 * Type-safe route definitions for the Hail navigation graph.
 * These replace the string-based routes in res/navigation/mobile_navigation.xml.
 * They implement NavKey for use with rememberNavBackStack (required for Nav3).
 */
@Serializable
sealed interface HailRoute {
    @Serializable
    data object Home : HailRoute

    @Serializable
    data object Actions : HailRoute

    @Serializable
    data object Apps : HailRoute

    @Serializable
    data object Settings : HailRoute

    @Serializable
    data object About : HailRoute
}

/**
 * Top-level routes that appear in the bottom navigation bar / navigation rail.
 * These correspond to the items in res/menu/nav_main.xml.
 */
val topLevelRoutes = listOf(
    HailRoute.Home,
    HailRoute.Actions,
    HailRoute.Settings
)

/**
 * Creates a NavHostController for Compose navigation.
 * This is the interim step before migrating to Nav3's NavDisplay.
 */
@Composable
fun rememberHailNavController(): NavHostController = rememberNavController()

/**
 * Builds the navigation graph using Compose Navigation.
 * Routes are forward-compatible with Nav3 (they implement NavKey).
 *
 * Screen composables are stubs for now and will be filled in during
 * Phase 1 (Settings, About), Phase 2 (Actions, Home), and Phase 3 (Apps).
 */
@Composable
fun HailNavHost(
    navController: NavHostController,
    startDestination: HailRoute = HailRoute.Home
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<HailRoute.Home> { /* HomeScreen() - Phase 2 */ }
        composable<HailRoute.Actions> { /* ActionsScreen() - Phase 2 */ }
        composable<HailRoute.Apps> { /* AppsScreen() - Phase 3 */ }
        composable<HailRoute.Settings> { /* SettingsScreen() - Phase 1 */ }
        composable<HailRoute.About> { /* AboutScreen() - Phase 1 */ }
    }
}

/**
 * Navigate to a typed route.
 * Replaces findNavController().navigate(R.id.xxx).
 */
fun NavHostController.hailNavigate(route: HailRoute) {
    navigate(route)
}

/**
 * Navigate back one step.
 * Replaces findNavController().navigateUp() / popBackStack().
 */
fun NavHostController.hailGoBack() {
    popBackStack()
}