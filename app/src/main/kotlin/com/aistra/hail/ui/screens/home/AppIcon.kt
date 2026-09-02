package com.aistra.hail.ui.screens.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistra.hail.R
import com.aistra.hail.app.AppInfo
import com.aistra.hail.app.HailData
import com.aistra.hail.utils.AppIconCache
import com.aistra.hail.utils.HPackages

@Composable
fun AppIcon(
    info: AppInfo,
    modifier: Modifier = Modifier,
    grayscale: Boolean = HailData.grayscaleIcon && info.state == AppInfo.State.FROZEN,
) {
    val context = LocalContext.current
    val applicationInfo = info.applicationInfo
    if (applicationInfo != null) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(applicationInfo)
                .size(64)
                .crossfade(false)
                .build(),
            contentDescription = null,
            modifier = modifier.size(64.dp),
            colorFilter = if (grayscale) ColorFilter.tint(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) else null,
            placeholder = painterResource(R.drawable.ic_round_apps),
            error = painterResource(R.drawable.ic_round_apps),
        )
    } else {
        Icon(
            painter = painterResource(R.drawable.ic_round_apps),
            contentDescription = null,
            modifier = modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
