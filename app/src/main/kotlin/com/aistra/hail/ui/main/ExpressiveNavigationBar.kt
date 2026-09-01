@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.aistra.hail.ui.main

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
import androidx.compose.material3.ShortNavigationBarArrangement
import androidx.compose.material3.ShortNavigationBarDefaults
import androidx.compose.material3.ShortNavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavDestination

private data class NavItem(
    val route: String,
    val filledIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val outlinedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
)

private val navItems = listOf(
    NavItem("nav_home", Icons.Filled.Home, Icons.Outlined.Home, "Home"),
    NavItem("nav_actions", Icons.Filled.AutoAwesome, Icons.Outlined.AutoAwesome, "Actions"),
    NavItem("nav_settings", Icons.Filled.Settings, Icons.Outlined.Settings, "Settings"),
)

@Composable
fun ExpressiveNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var currentDestination by remember {
        mutableStateOf<NavDestination?>(navController.currentDestination)
    }

    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            currentDestination = destination
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    val currentRoute = currentDestination?.route

    ShortNavigationBar(
        modifier = modifier.fillMaxWidth(),
        windowInsets = ShortNavigationBarDefaults.windowInsets,
        arrangement = ShortNavigationBarArrangement.EqualWeight,
    ) {
        navItems.forEach { item ->
            val selected = item.route == currentRoute
            ShortNavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(item.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                        contentDescription = item.label,
                    )
                },
                label = { Text(item.label) },
                iconPosition = NavigationItemIconPosition.Top,
            )
        }
    }
}
