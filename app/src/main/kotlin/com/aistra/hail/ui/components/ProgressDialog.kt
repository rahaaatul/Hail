package com.aistra.hail.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import androidx.compose.ui.window.DialogProperties

/**
 * Material 3 Expressive Loading dialog.
 * Replaces dialog_progress.xml.
 *
 * @param visible Whether the dialog is visible
 * @param text Optional message to show below the indicator
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadingDialog(
    visible: Boolean,
    text: String? = null,
    onDismiss: () -> Unit = {}
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it / 2 } + fadeIn(),
        exit = slideOutVertically { it / 2 } + fadeOut()
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            confirm = {},
            dismiss = null,
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (text != null) {
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = false),
            shape = MaterialTheme.shapes.medium,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 6.dp
        )
    }
}