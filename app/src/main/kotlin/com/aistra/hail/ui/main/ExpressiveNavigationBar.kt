package com.aistra.hail.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    useFloating: Boolean = false,
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

    if (useFloating) {
        val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)
        val floatingNavItems = navItems.map {
            FloatingNavItem(
                icon = if (it.route == currentRoute) it.filledIcon else it.outlinedIcon,
                label = it.label,
                contentDescription = it.label,
            )
        }
        FloatingBottomBar(
            items = floatingNavItems,
            selectedIndex = selectedIndex,
            onSelected = { index ->
                val item = navItems[index]
                if (item.route != currentRoute) {
                    navController.navigate(item.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            modifier = modifier,
        )
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = item.route == currentRoute
                val interactionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!selected) {
                                navController.navigate(item.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.filledIcon else item.outlinedIcon,
                            contentDescription = item.label,
                            tint = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = item.label,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
