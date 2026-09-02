@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.aistra.hail.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.aistra.hail.ui.theme.dynamicColorScheme
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val colorSchemeCache = ConcurrentHashMap<String, ColorScheme>()

@Composable
fun ColorSwatchPreview(
    rawColor: RawColor,
    currentStyle: PaletteStyle,
    colorSpec: ThemeColorSpec,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val cacheKey = remember(rawColor.color, currentStyle, colorSpec) {
        "${rawColor.color.toArgb()}_${currentStyle.name}_${colorSpec.name}_false"
    }

    val scheme by produceState<ColorScheme?>(
        initialValue = colorSchemeCache[cacheKey],
        key1 = cacheKey,
    ) {
        val cachedScheme = colorSchemeCache[cacheKey]
        if (cachedScheme != null) {
            value = cachedScheme
        } else {
            withContext(Dispatchers.Default) {
                val newScheme = dynamicColorScheme(
                    keyColor = rawColor.color,
                    isDark = false,
                    style = currentStyle,
                    colorSpec = colorSpec,
                )
                colorSchemeCache[cacheKey] = newScheme
                value = newScheme
            }
        }
    }

    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (scheme != null) {
            FullSwatchContent(scheme = scheme!!, isSelected = isSelected)
        } else {
            FallbackSwatchContent(baseColor = rawColor.color, isSelected = isSelected)
        }
    }
}

@Composable
private fun FullSwatchContent(scheme: ColorScheme, isSelected: Boolean) {
    val primaryForSwatch = remember(scheme) { scheme.primaryContainer.copy(alpha = 0.9f) }
    val secondaryForSwatch = remember(scheme) { scheme.secondaryContainer.copy(alpha = 0.6f) }
    val tertiaryForSwatch = remember(scheme) { scheme.tertiaryContainer.copy(alpha = 0.9f) }
    val squircleBackgroundColor = remember(scheme) { scheme.primary.copy(alpha = 0.3f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = squircleBackgroundColor, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawArc(color = primaryForSwatch, startAngle = 180f, sweepAngle = 180f, useCenter = true)
                drawArc(color = tertiaryForSwatch, startAngle = 90f, sweepAngle = 90f, useCenter = true)
                drawArc(color = secondaryForSwatch, startAngle = 0f, sweepAngle = 90f, useCenter = true)
            }

            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.inversePrimary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FallbackSwatchContent(baseColor: Color, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = baseColor.copy(alpha = 0.1f), shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(baseColor.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(baseColor),
                contentAlignment = Alignment.Center,
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}
