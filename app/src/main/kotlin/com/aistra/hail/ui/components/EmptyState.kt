package com.aistra.hail.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox

/**
 * Empty state placeholder shown when a list has no items.
 * Replaces the empty MaterialTextView from fragment_pager.xml.
 */
@Composable
fun EmptyState(
    text: String = stringResource(R.string.nothing_here),
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

/**
 * Default empty state with inbox icon.
 */
@Composable
fun EmptyListState(
    modifier: Modifier = Modifier,
    text: String = stringResource(R.string.nothing_here)
) {
    EmptyState(
        text = text,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Inbox,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(64.dp)
            )
        }
    )
}
