package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableInteractionSourceOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.ui.home.Tag
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
    onDeleteTag: (String) -> Unit = {},
    onCheckedChange: (AppInfo) -> Unit = {},
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
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                interactionSource = interactionSource,
            )
            .padding(8.dp)
            .align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(
            request = AppIconRequest(packageName = app.packageName, userId = HPackages.myUserId),
            contentDescription = app.name,
            grayscale = HailData.grayscaleIcon && app.state == AppInfo.State.FROZEN,
            modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
        )
        if (showTagBadge && tags.isNotEmpty()) {
            TagChip(
                text = tags.joinToString(", ") { it.label },
                onDelete = onDeleteTag,
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
                androidx.compose.material3.Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onCheckedChange(app) },
                    colors = androidx.compose.material3.CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

