package com.aistra.hail.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData

@Composable
fun HomeAppItem(
    appInfo: AppInfo,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    multiselectMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val size = dimensionResource(R.dimen.app_icon_size)
    val paddingSmall = dimensionResource(R.dimen.padding_small)
    val paddingExtraSmall = dimensionResource(R.dimen.padding_extra_small)

    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        appInfo.state == AppInfo.State.NOT_FOUND -> MaterialTheme.colorScheme.error
        multiselectMode && !HailData.grayscaleIcon && appInfo.state == AppInfo.State.FROZEN ->
            MaterialTheme.colorScheme.onSurfaceVariant

        else -> MaterialTheme.colorScheme.onSurface
    }

    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else Modifier

    val longClickModifier = if (onLongClick != null) {
        Modifier.pointerInput(onLongClick) {
            detectTapGestures(onLongPress = { onLongClick() })
        }
    } else Modifier

    Column(
        modifier = modifier
            .padding(paddingExtraSmall)
            .then(clickModifier)
            .then(longClickModifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIcon(
            info = appInfo,
            modifier = Modifier
                .size(size)
                .padding(paddingSmall),
            grayscale = HailData.grayscaleIcon && appInfo.state == AppInfo.State.FROZEN,
        )
        Text(
            text = buildString {
                if (!HailData.grayscaleIcon && appInfo.state == AppInfo.State.FROZEN) append("\u2744\uFE0F")
                if (appInfo.whitelisted) append("\uD83D\uDD12")
                append(appInfo.name)
            },
            color = textColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp),
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            ),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
