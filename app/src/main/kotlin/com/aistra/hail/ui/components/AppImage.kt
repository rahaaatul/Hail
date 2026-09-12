package com.aistra.hail.ui.components

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aistra.hail.R

/**
 * App image loading component using Coil.
 * Replaces AppIconCache.loadIconBitmapAsync() + ImageView.
 *
 * @param info ApplicationInfo to load the icon from
 * @param grayscale If true, applies a grayscale color filter (for frozen apps)
 * @param modifier Layout modifier
 * @param contentDescription Accessibility description
 */
@Composable
fun AppImage(
    info: ApplicationInfo,
    grayscale: Boolean = false,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(info)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(48.dp),
        error = painterResource(id = R.drawable.ic_launcher_foreground),
        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
        transform = if (grayscale) GrayscaleColorFilter else null
    )
}

/**
 * Color filter that converts an image to grayscale.
 * Used for frozen apps in the home/apps screens.
 */
val GrayscaleColorFilter: ColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0.33f, 0.33f, 0.33f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    )
)

/**
 * App image with a specific size, used in grid items.
 */
@Composable
fun AppImage(
    info: ApplicationInfo,
    size: Int,
    grayscale: Boolean = false,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val context = LocalContext.current
    AsyncImage(
        model = ImageRequest.Builder(context)
            .data(info)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier.size(size.dp),
        error = painterResource(id = R.drawable.ic_launcher_foreground),
        placeholder = painterResource(id = R.drawable.ic_launcher_foreground),
        transform = if (grayscale) GrayscaleColorFilter else null
    )
}