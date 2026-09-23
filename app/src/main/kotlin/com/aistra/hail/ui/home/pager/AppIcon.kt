package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.colorFilter
import androidx.compose.ui.graphics.ColorMatrixColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmapOrNull
import com.aistra.hail.R
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HLog
import com.aistra.hail.utils.HPackages

private val EMPTY_IMAGE_BITMAP = ImageBitmap(1, 1)

data class AppIconRequest(
    val packageName: String,
    val userId: Int = HPackages.myUserId,
)

@Composable
fun AppIcon(
    request: AppIconRequest,
    contentDescription: String?,
    grayscale: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val iconSize = context.resources.getDimensionPixelSize(R.dimen.app_icon_size)
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(request.packageName, request.userId) {
        val info = HPackages.getApplicationInfoOrNull(request.packageName)
        bitmap = if (info != null) {
            try {
                AppIconCache.getOrLoadBitmap(context, info, request.userId, iconSize)
            } catch (e: Exception) {
                HLog.e("Failed to load icon for ${request.packageName}", e)
                null
            }
        } else {
            null
        }
    }

    val fallbackBitmap = remember(iconSize) {
        val defaultIcon = context.packageManager.defaultActivityIcon
        runCatching { defaultIcon.toBitmapOrNull(width = iconSize, height = iconSize) }.getOrNull()
    }
    val imageBitmap by derivedStateOf {
        runCatching { bitmap?.asImageBitmap() }.getOrNull()
            ?: runCatching { fallbackBitmap?.asImageBitmap() }.getOrNull()
            ?: EMPTY_IMAGE_BITMAP
    }
    Image(
        bitmap = imageBitmap,
        contentDescription = contentDescription,
        modifier = modifier.colorFilter(
            if (grayscale) {
                ColorMatrixColorFilter(
                    android.graphics.ColorMatrix().apply { setSaturation(0f) }
                )
            } else {
                null
            }
        )
    )
}


