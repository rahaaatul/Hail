package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.Outlined.Edit
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PagerHeader(
    title: String,
    onEditTagsClicked: () -> Unit = {},
    canEditTags: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(16.dp).height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        if (canEditTags) {
            IconButton(onClick = onEditTagsClicked) {
                Icon(Icons.Outlined.Edit, contentDescription = null)
            }
        }
    }
}
