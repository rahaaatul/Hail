package com.aistra.hail.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberImagePainter
import com.aistra.hail.HailApp
import com.aistra.hail.utils.AppIconRequest

@Composable
fun AppIcon(
    request: AppIconRequest,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val imageLoader = (LocalContext.current.applicationContext as HailApp).imageLoader
    val painter = rememberImagePainter(
        data = request,
        imageLoader = imageLoader,
        builder = { crossfade(true) }
    )
    val size = request.size.dp
    Canvas(
        modifier = modifier
            .size(size)
            .semantics { contentDescription?.let { this.contentDescription = it } }
    ) {
        painter.draw(this)
    }
}