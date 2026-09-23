package com.aistra.hail.ui.home.pager

import androidx.compose.material3.Chip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun TagChip(text: String, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onDeleteRequest = onDelete,
        modifier = modifier,
    ) {
        Text(text)
    }
}

