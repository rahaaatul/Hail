package com.aistra.hail.ui.theme

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@Composable
fun TagEditDialog(
    currentTagName: String,
    onDismissed: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tagName by rememberSaveable { mutableStateOf(currentTagName) }

    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text("Edit Tag") },
        text = {
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text("Tag name") },
                isError = tagName.isBlank(),
                errorMessage = if (tagName.isBlank()) { Text("Tag name cannot be empty") } else null,
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (tagName.isNotBlank()) {
                    onSaved(tagName)
                    onDismissed()
                }
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissed) {
                Text("Cancel")
            }
        },
        modifier = modifier,
    )
}