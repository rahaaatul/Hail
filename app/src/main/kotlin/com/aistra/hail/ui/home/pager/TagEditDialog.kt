package com.aistra.hail.ui.home.pager

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
import androidx.compose.ui.res.stringResource
import com.aistra.hail.R

@Composable
fun TagEditDialog(
    currentTagName: String,
    onDismissed: () -> Unit,
    onSaved: (String) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tagName by rememberSaveable { mutableStateOf(currentTagName) }
    val editTagTitle = remember { stringResource(R.string.edit_tag) }
    val tagNameLabel = remember { stringResource(R.string.tag_name_label) }
    val tagNameEmptyError = remember { stringResource(R.string.tag_name_empty_error) }
    val supportingText = remember(tagName) {
        if (tagName.isBlank()) { Text(tagNameEmptyError) } else { null }
    }

    AlertDialog(
        onDismissRequest = onDismissed,
        title = { Text(editTagTitle) },
        text = {
            TextField(
                value = tagName,
                onValueChange = { tagName = it },
                label = { Text(tagNameLabel) },
                isError = tagName.isBlank(),
                supportingText = supportingText,
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

