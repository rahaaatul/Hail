package com.aistra.hail.ui.home.pager

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import com.aistra.hail.R
import com.aistra.hail.utils.HPackages
import com.aistra.hail.utils.AppIconCache

@Composable
fun AppIcon(
    request: AppIconRequest,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(request.packageName, request.userId) {
        bitmap = runCatching {
            val info = HPackages.getApplicationInfoOrNull(request.packageName)
            if (info != null) {
                val size = context.resources.getDimensionPixelSize(R.dimen.app_icon_size)
                AppIconCache.getOrLoadBitmap(context, info, request.userId, size)
            } else {
                null
            }
        }.getOrNull()
    }

    bitmap?.let { bmp ->
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = modifier
        )
    }
}

data class AppIconRequest(
    val packageName: String,
    val userId: Int = HPackages.myUserId,
)
