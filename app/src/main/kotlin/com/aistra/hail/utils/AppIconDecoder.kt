package com.aistra.hail.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import coil.decode.ImageDecoder
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppIconDecoder @Suppress("UNUSED_PARAMETER") constructor(
    context: Context
) : ImageDecoder<AppIconRequest> {

    private val cf = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) })

    @Throws(Exception::class)
    override suspend fun decode(data: AppIconRequest): Bitmap {
        return withContext(Dispatchers.IO) {
            val bitmap = AppIconCache.getOrLoadBitmap(
                context = context,
                info = context.packageManager.getApplicationInfo(data.packageName, 0),
                userId = data.userId,
                size = data.size
            )
            if (data.grayscale) {
                Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config).also { grayscaleBitmap ->
                    grayscaleBitmap.setHasAlpha(true)
                    val canvas = android.graphics.Canvas(grayscaleBitmap)
                    val paint = android.graphics.Paint().apply { colorFilter = cf }
                    canvas.drawBitmap(bitmap, 0f, 0f, paint)
                }
            } else {
                bitmap
            }
        }
    }

    class Factory : ImageDecoder.Factory<AppIconRequest> {
        override fun create(context: Context): ImageDecoder<AppIconRequest> {
            return AppIconDecoder(context)
        }
    }
}