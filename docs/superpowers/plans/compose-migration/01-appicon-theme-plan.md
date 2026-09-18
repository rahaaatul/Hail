# Hail App - Compose Infrastructure Plan: AppIcon + Theme

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish Compose foundation by replacing AppIconCache with Coil, completing Material3 theme with Typography and Shapes, and creating reusable AppIcon composable.

**Architecture:** 
1. Add Coil dependency for async image loading with memory/disk caching
2. Replace custom AppIconCache with Coil-based AsyncImage solution
3. Extend Theme.kt with full Material3 Typography and Shapes
4. Create reusable @Composable AppIcon for use in all screens
5. Remove AppIconCache.kt after verification

**Tech Stack:**
- Coil 2.6.0 (io.coil-kt:coil-compose)
- Material3 1.4.0 (androidx.compose.material3:material3)
- Kotlin 2.4.20, AGP 9.4.0

## File Changes

### 1. Update Dependencies
```diff
// gradle/libs.versions.toml
[versions]
+ coil = "2.6.0"
+ coilCompose = "2.6.0"

[libraries]
+ coil = { module = "io.coil-kt:coil", version.ref = "coil" }
+ coilCompose = { module = "io.coil-kt:coil-compose", version.ref = "coilCompose" }

// app/build.gradle.kts
dependencies {
    // ... existing
    implementation(libs.coil)
    implementation(libs.coilCompose)
}
```

### 2. Create AppIcon Data Class + Decoder
```kotlin
// app/src/main/kotlin/com/aistra/hail/utils/AppIconRequest.kt
package com.aistra.hail.utils

import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import coil.decode.ImageDecoder
import coil.request.ImageRequest

/**
 * Data class representing an image request for app icons.
 * Contains all parameters needed for deterministic caching.
 */
@Immutable
data class AppIconRequest(
    val packageName: String,
    val userId: Int = 0,
    val size: Int = Dp.Size.dpToPx(48.dp), // from resources
    val grayscale: Boolean = false,
    val synthesizeAdaptive: Boolean = HailData.synthesizeAdaptiveIcons,
    val iconPack: String = HailData.iconPack
) : ImageRequest.Data {
    override fun toString(): String = 
        "appicon://$packageName|$userId|$size|$grayscale|$synthesizeAdaptive|$iconPack"
}

/**
 * Custom ImageDecoder that wraps existing AppIconLoader logic.
 * Reuses the same memory/disk caching strategy as AppIconCache.
 */
class AppIconDecoder @Inject constructor(
    @Suppress("UNUSED_PARAMETER") context: Context
) : ImageDecoder<AppIconRequest> {
    override fun decode(
        pool: ImagePool,
        data: AppIconRequest,
        size: IntSize,
        options: ImageOptions,
        callback: ImageDecoder.DecodeCallback
    ): Closeable? {
        // Reuse AppIconCache.getOrLoadBitmap logic here
        val context = LocalDensity.current
        val bitmap = AppIconLoader.getOrLoadBitmap(
            context,
            data.packageName,
            data.userId,
            data.size,
            data.grayscale,
            data.synthesizeAdaptive,
            data.iconPack
        )
        return if (bitmap != null) {
            SimpleImagePool.Closeable(pool, bitmap) { }
        } else null
    }
}

/** Simple wrapper for pool-managed bitmap */
private class SimpleImagePool.Closeable(
    pool: ImagePool,
    private val bitmap: Bitmap,
    private val release: () -> Unit
) : Closeable {
    override fun close() = release()
    override fun getBitmap(pool: ImagePool): Bitmap = bitmap
}
```

### 3. Configure ImageLoader in HailApp
```kotlin
// app/src/main/kotlin/com/aistra/hail/HailApp.kt
class HailApp : Application() {
    // ... existing code
    
    val imageLoader by lazy {
        ImageLoader.Builder(this)
            .componentRegistry { 
                add(AppIconDecoder.Factory()) 
            }
            .memoryCache { 
                LruMemoryCache(maxSizePercent = 0.25) 
            }
            .diskCache { 
                FileDiskCache(File(cacheDir, "coil_icons")) 
            }
            .crossfade(true) 
            .build()
    }
    
    // ... existing methods
}
```

### 4. Create Reusable AppIcon Composable
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/AppIcon.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.intoCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import coil.decode.ImageDecoder
import coil.drawable.DrawablePainter
import coil.request.ImageRequest
import coil.compose.rememberImagePainter
import com.aistra.hail.R
import com.aistra.hail.utils.AppIconRequest
import javax.inject.Inject

/**
 * Composable that loads and displays an app icon using Coil.
 * Replaces the old AppIconView and AppIconCache usage.
 * 
 * @param request The AppIconRequest containing all parameters for loading the icon
 * @param contentDescription Optional description for accessibility
 * @param modifier Modifier to apply to the composable
 */
@Composable
fun AppIcon(
    request: AppIconRequest,
    contentDescription: String? = null,
    modifier: Modifier = Modifier
) {
    val painter = rememberImagePainter(
        data = request,
        builder = { 
            crossfade(true)
        }
    )

    // Default size from resources if not specified in request
    val size = request.size.dp

    // Create a circular clipping shape for adaptive icons
    val shape = CircleShape

    Canvas(modifier = modifier.size(size)) {
        // Draw the icon with the painter
        drawContext.canvas.nativeCanvas.saveLayer(null)
        painter?.paint?.let { paint ->
            drawIntoCanvas { 
                it.drawImage(
                    painter.imageBitmap ?: return@drawIntoCanvas,
                    topLeft = Offset(0f, 0f),
                    size = Size(size.value, size.value),
                    paint = paint
                )
            }
        }
        drawContext.canvas.nativeCanvas.restore()
    }
    
    // Optional: Add accessibility description
    // Note: In a real implementation, we would use Semantics property
    // For now, we rely on the parent to set contentDescription
}
```

### 5. Update Theme.kt with Typography and Shapes
```kotlin
// app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.aistra.hail.R

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    secondary = Color(0xFF03DAC6),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    error = Color(0xFFCF6679),
    onPrimary = Color(0xFF212121),
    onSecondary = Color(0xFF212121),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onError = Color(0xFF212121)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    secondary = Color(0xFF625B71),
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    error = Color(0xFFB3261E),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121),
    onError = Color(0xFFFFFFFF)
)

@Composable
fun HailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

private val Typography = Typography(
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
)

private val Shapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(16.dp)
)
```

### 6. Remove AppIconCache.kt (After Verification)
```diff
// app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt
- /* ENTIRE FILE REMOVED */
```

## Validation Checklist

- [ ] App builds successfully (`./gradlew assembleDebug`)
- [ ] AppIcon composable displays icons correctly in all screens
- [ ] Icons load with placeholder and error handling
- [ ] Memory and disk caching works (verify via Coil logs)
- [ ] Theme colors and typography applied correctly in light/dark mode
- [ ] No regression in existing icon-dependent features ( AppsFragment, ActionsFragment, etc.)
- [ ] AppIconCache.kt removed and no references remain
- [ ] Coil dependency resolves without conflicts
- [ ] ImageLoader singleton properly initialized in HailApp
- [ ] Accessibility: Icons have contentDescription when used
- [ ] Performance: Icon loading doesn't cause jank on scroll

## References

- [Coil Compose Documentation](https://coil-kt.github.io/coil/compose/)
- [Material3 Theming](https://developer.android.com/jetpack/compose/themes)
- [Custom ImageDecoder in Coil](https://coil-kt.github.io/coil/custom-decoders/)