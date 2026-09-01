package com.aistra.hail.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationItemIconPosition
import androidx.compose.material3.ShortNavigationBar
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.aistra.hail.R

data class NavItem(
    @IdRes val id: Int,
    @DrawableRes val iconRes: Int,
    @StringRes val titleRes: Int,
)

private val navItems = listOf(
    NavItem(R.id.nav_home, R.drawable.ic_round_frozen, R.string.title_home),
    NavItem(R.id.nav_actions, R.drawable.ic_round_action_flow, R.string.title_actions),
    NavItem(R.id.nav_settings, R.drawable.ic_outline_settings, R.string.title_settings),
)

@Composable
fun ExpressiveNavigationBar(
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    var currentDestination by remember {
        mutableStateOf(navController.currentDestination)
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

    val selectedId = currentDestination?.id

    ShortNavigationBar(
        modifier = modifier.fillMaxWidth(),
        windowInsets = ShortNavigationBarDefaults.windowInsets,
    ) {
        navItems.forEach { item ->
            ShortNavigationBarItem(
                selected = item.id == selectedId,
                onClick = {
                    if (item.id != selectedId) {
                        navController.navigate(item.id) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                    )
                },
                label = { Text(stringResource(id = item.titleRes)) },
                iconPosition = NavigationItemIconPosition.Top,
            )
        }
    }
}
