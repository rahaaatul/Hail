package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import androidx.compose.ui.window.DialogProperties

/**
 * Material 3 Expressive AlertDialog wrapper.
 * Replaces MaterialAlertDialogBuilder from various fragments.
 *
 * @param onDismissRequest Called when the dialog is dismissed
 * @param confirmButton Composable for the confirm button
 * @param title Dialog title
 * @param text Dialog body text
 * @param dismissButton Composable for the dismiss/cancel button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable (() -> Unit),
    title: String? = null,
    text: String? = null,
    dismissButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirm = confirmButton,
        dismiss = dismissButton,
        title = title?.let { Text(text = it) },
        text = text?.let { Text(text = it) },
        modifier = modifier,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        tonalElevation = 6.dp
    )
}

/**
 * Simple confirmation dialog with a title and message.
 * Replaces MaterialAlertDialogBuilder with set POSITIVE/NEGATIVE.
 */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(id = android.R.string.ok),
    dismissText: String = stringResource(id = android.R.string.cancel)
) {
    AppDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = dismissText)
            }
        },
        title = title,
        text = message
    )
}