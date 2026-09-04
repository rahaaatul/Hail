package com.aistra.hail.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun AppIcon(
    info: AppInfo,
    modifier: Modifier = Modifier,
    grayscale: Boolean = HailData.grayscaleIcon && info.state == AppInfo.State.FROZEN,
) {
    val context = LocalContext.current
    val size = dimensionResource(R.dimen.app_icon_size)
    val paddingSmall = dimensionResource(R.dimen.padding_small)

    val bitmapState = produceState<Bitmap?>(null, info.packageName, size) {
        val applicationInfo = info.applicationInfo
        if (applicationInfo != null) {
            try {
                value = withContext(Dispatchers.IO) {
                    AppIconCache.getOrLoadBitmap(context, applicationInfo, HPackages.myUserId, size)
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = null,
            modifier = modifier
                .padding(paddingSmall)
                .size(size - paddingSmall * 2),
            colorFilter = if (grayscale) ColorFilter.tint(Color.Gray) else null,
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_round_apps),
            contentDescription = null,
            modifier = modifier
                .padding(paddingSmall)
                .size(size - paddingSmall * 2),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
