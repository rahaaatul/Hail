package com.aistra.hail.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.longClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableInteractionSourceOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.utils.HPackages

@Composable
fun AppGridItem(
    app: AppInfo,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    tags: List<Tag> = emptyList(),
    showTagBadge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        isPressed -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Column(
        modifier = modifier
            .size(72.dp)
            .background(backgroundColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .longClickable(onLongClick = onLongClick)
            .interactionSource(interactionSource)
            .padding(8.dp)
            .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            request = AppIconRequest(packageName = app.packageName, userId = HPackages.myUserId),
            contentDescription = app.name,
            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
        )
        if (showTagBadge && tags.isNotEmpty()) {
            TagChip(
                text = tags.first().label,
                onDelete = { },
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = app.name,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        if (isMultiSelect) {
            Box(
                modifier = Modifier.align(Alignment.TopEnd).size(20.dp),
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}