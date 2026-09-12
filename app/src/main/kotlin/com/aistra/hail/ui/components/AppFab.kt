package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R

/**
 * Material 3 Expressive ExtendedFloatingActionButton.
 * Replaces the ExtendedFloatingActionButton from app_bar_main.xml.
 *
 * @param text Button label
 * @param onClick Click handler
 * @param icon Icon to display
 * @param visible Whether the FAB is visible
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFab(
    text: String,
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        ExtendedFloatingActionButton(
            onClick = onClick,
            modifier = modifier.sizeIn(minWidth = 64.dp, minHeight = 56.dp),
            icon = icon ?: {},
            text = { Text(text = text) },
            shape = ExtendedFloatingActionButtonDefaults.extendedShape(),
            colors = ExtendedFloatingActionButtonDefaults.floatingActionButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = ExtendedFloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        )
    }
}

/**
 * FAB with icon only (no text), used for the home screen add action.
 * Replaces the FAB from app_bar_main.xml.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppFabIcon(
    onClick: () -> Unit,
    icon: @Composable (() -> Unit)? = null,
    visible: Boolean = true,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier,
            icon = icon ?: {},
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 6.dp,
                pressedElevation = 12.dp
            )
        )
    }
}