package com.aistra.hail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.ui.navigation.HailRoute
import com.aistra.hail.ui.navigation.topLevelRoutes

/**
 * Material 3 Expressive NavigationBar.
 * Replaces Bottom NavigationView from activity_main.xml.
 *
 * @param currentRoute The currently selected route
 * @param onItemSelected Callback when a navigation item is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomNav(
    currentRoute: HailRoute?,
    onItemSelected: (HailRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        topLevelRoutes.forEach { route ->
            NavigationItem(
                selected = route == currentRoute,
                onClick = { onItemSelected(route) },
                icon = { NavigationItemIcon(route) },
                label = { Text(text = stringResource(route.labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis) }
            )
        }
    }
}

/**
 * Material 3 Expressive NavigationRail.
 * Replaces NavigationRailView from app_bar_main.xml.
 * Used on wider screens (tablets, landscape).
 *
 * @param currentRoute The currently selected route
 * @param onItemSelected Callback when a navigation item is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationRail(
    currentRoute: HailRoute?,
    onItemSelected: (HailRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationRail(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        topLevelRoutes.forEach { route ->
            NavigationItem(
                selected = route == currentRoute,
                onClick = { onItemSelected(route) },
                icon = { NavigationItemIcon(route) },
                label = { Text(text = stringResource(route.labelRes)) }
            )
        }
    }
}

/**
 * Maps a route to its icon resource.
 * Replaces the menu items from res/menu/nav_main.xml.
 */
@Composable
private fun NavigationItemIcon(route: HailRoute) {
    when (route) {
        HailRoute.Home -> Icon(painterResource(id = R.drawable.ic_round_frozen), contentDescription = null)
        HailRoute.Actions -> Icon(painterResource(id = R.drawable.ic_round_action_flow), contentDescription = null)
        HailRoute.Settings -> Icon(painterResource(id = R.drawable.ic_settings_selector), contentDescription = null)
        HailRoute.Apps -> Icon(Icons.Outlined.Apps, contentDescription = null)
        HailRoute.About -> Icon(Icons.Outlined.Info, contentDescription = null)
    }
}

/**
 * Extension to get the label string resource for a route.
 */
val HailRoute.labelRes: Int
    get() = when (this) {
        HailRoute.Home -> R.string.title_home
        HailRoute.Actions -> R.string.title_actions
        HailRoute.Apps -> R.string.title_apps
        HailRoute.Settings -> R.string.title_settings
        HailRoute.About -> R.string.title_about
    }