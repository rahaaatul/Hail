package com.aistra.hail.ui.apps

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.AppMetaCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

@Composable
fun AppGridItem(
    info: ApplicationInfo,
    onClick: () -> Unit,
    onLongClick: () -> Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val metadata = AppMetaCache.get(info.packageName)
    val frozen = metadata?.state == AppInfo.State.FROZEN
    val isSelf = info.packageName == context.packageName
    val name = metadata?.name ?: info.packageName
    val packageName = info.packageName

    var iconBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    val iconSize = remember { context.resources.getDimensionPixelSize(R.dimen.app_icon_size).toFloat() }

    val colorFilter = remember {
        if (HailData.grayscaleIcon && frozen) {
            ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })
        } else null
    }

    val textColor = remember {
        if (HPackages.isAppUninstalled(packageName)) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurface
        }
    }

    val enabled = !(HailData.grayscaleIcon && frozen)

    AppIconCache.loadIconBitmapAsync(context, info, HPackages.myUserId, null).let { job ->
        loadJob = job
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(iconSize.dp),
                contentAlignment = Alignment.Center
            ) {
                iconBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(iconSize.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = if (HailData.grayscaleIcon && frozen) "❄️$name" else name,
                    color = textColor.copy(alpha = if (enabled) 1f else 0.38f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
                Text(
                    text = packageName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.38f),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.TextOverflow.Ellipsis
                )
            }

            Checkbox(
                checked = if (isSelf) false else HailData.isChecked(packageName),
                onCheckedChange = if (isSelf) null else onCheckedChange,
                enabled = !isSelf && enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}