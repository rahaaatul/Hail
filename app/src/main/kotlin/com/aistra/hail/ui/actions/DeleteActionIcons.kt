@file:Suppress("MagicNumber")

package com.aistra.hail.ui.actions

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Material Symbols close (rounded) — dismiss delete prompt. */
internal val DeleteCloseIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "close", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(10f, 11.06f)
                lineTo(6.58f, 14.48f)
                quadTo(6.35f, 14.71f, 6.06f, 14.7f)
                reflectiveQuadTo(5.54f, 14.46f)
                reflectiveQuadTo(5.31f, 13.93f)
                reflectiveQuadTo(5.54f, 13.4f)
                lineTo(8.94f, 10f)
                lineTo(5.52f, 6.58f)
                quadTo(5.29f, 6.35f, 5.3f, 6.05f)
                reflectiveQuadTo(5.54f, 5.52f)
                reflectiveQuadTo(6.07f, 5.29f)
                quadToRelative(0.3f, 0f, 0.53f, 0.23f)
                lineTo(10f, 8.94f)
                lineTo(13.42f, 5.52f)
                quadTo(13.65f, 5.29f, 13.95f, 5.29f)
                reflectiveQuadToRelative(0.53f, 0.23f)
                reflectiveQuadToRelative(0.23f, 0.53f)
                reflectiveQuadTo(14.48f, 6.58f)
                lineTo(11.06f, 10f)
                lineToRelative(3.42f, 3.42f)
                quadToRelative(0.23f, 0.23f, 0.23f, 0.52f)
                reflectiveQuadToRelative(-0.23f, 0.52f)
                reflectiveQuadToRelative(-0.53f, 0.23f)
                reflectiveQuadTo(13.42f, 14.46f)
                lineTo(10f, 11.06f)
                close()
            }
        }.build()
}

/** Material Symbols delete_forever (rounded) — confirm permanent delete. */
internal val DeleteForeverIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "delete_forever", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(6.5f, 17f)
                quadTo(5.88f, 17f, 5.44f, 16.56f)
                reflectiveQuadTo(5f, 15.5f)
                verticalLineTo(5.5f)
                horizontalLineTo(4.75f)
                quadTo(4.43f, 5.5f, 4.22f, 5.29f)
                reflectiveQuadTo(4f, 4.75f)
                reflectiveQuadTo(4.22f, 4.22f)
                reflectiveQuadTo(4.75f, 4f)
                horizontalLineTo(8f)
                verticalLineTo(3.75f)
                quadTo(8f, 3.43f, 8.22f, 3.22f)
                reflectiveQuadTo(8.75f, 3f)
                horizontalLineToRelative(2.5f)
                quadToRelative(0.32f, 0f, 0.53f, 0.22f)
                reflectiveQuadTo(12f, 3.75f)
                verticalLineTo(4f)
                horizontalLineToRelative(3.25f)
                quadToRelative(0.32f, 0f, 0.53f, 0.21f)
                reflectiveQuadTo(16f, 4.75f)
                reflectiveQuadTo(15.78f, 5.28f)
                reflectiveQuadTo(15.25f, 5.5f)
                horizontalLineTo(15f)
                verticalLineToRelative(9.99f)
                quadToRelative(0f, 0.63f, -0.44f, 1.07f)
                reflectiveQuadTo(13.5f, 17f)
                horizontalLineToRelative(-7f)
                close()
                moveToRelative(7f, -11.5f)
                horizontalLineToRelative(-7f)
                verticalLineToRelative(10f)
                horizontalLineToRelative(7f)
                verticalLineTo(5.5f)
                close()
                moveToRelative(-7f, 0f)
                verticalLineToRelative(10f)
                verticalLineTo(5.5f)
                close()
                moveTo(10f, 11.56f)
                lineToRelative(1.15f, 1.15f)
                quadToRelative(0.23f, 0.23f, 0.53f, 0.22f)
                reflectiveQuadToRelative(0.53f, -0.24f)
                reflectiveQuadToRelative(0.23f, -0.53f)
                reflectiveQuadTo(12.21f, 11.63f)
                lineTo(11.06f, 10.5f)
                lineTo(12.21f, 9.35f)
                quadTo(12.44f, 9.13f, 12.44f, 8.82f)
                reflectiveQuadTo(12.21f, 8.29f)
                reflectiveQuadTo(11.68f, 8.06f)
                quadToRelative(-0.3f, 0f, -0.53f, 0.23f)
                lineTo(10f, 9.44f)
                lineTo(8.85f, 8.29f)
                quadTo(8.63f, 8.06f, 8.33f, 8.06f)
                reflectiveQuadTo(7.81f, 8.29f)
                reflectiveQuadTo(7.58f, 8.82f)
                reflectiveQuadTo(7.81f, 9.35f)
                lineTo(8.94f, 10.5f)
                lineTo(7.79f, 11.65f)
                quadTo(7.56f, 11.88f, 7.57f, 12.17f)
                reflectiveQuadToRelative(0.24f, 0.52f)
                reflectiveQuadToRelative(0.53f, 0.23f)
                reflectiveQuadTo(8.88f, 12.69f)
                lineTo(10f, 11.56f)
                close()
            }
        }.build()
}

/** Material Symbols mirror_stack (rounded) — prerequisites section header. */
internal val MirrorStackIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "mirror_stack", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(5.5f, 18f)
                quadTo(4.81f, 18f, 4.41f, 17.59f)
                reflectiveQuadTo(4f, 16.5f)
                verticalLineTo(7f)
                quadTo(4f, 6.31f, 4.41f, 5.91f)
                reflectiveQuadTo(5.5f, 5.5f)
                horizontalLineTo(10f)
                quadToRelative(0.63f, 0f, 1.06f, 0.44f)
                reflectiveQuadTo(11.5f, 7f)
                verticalLineToRelative(9.5f)
                quadToRelative(0f, 0.69f, -0.41f, 1.09f)
                reflectiveQuadTo(10f, 18f)
                horizontalLineTo(5.5f)
                close()
                moveTo(13f, 14.5f)
                verticalLineTo(6.25f)
                quadTo(13f, 5.31f, 12.34f, 4.66f)
                reflectiveQuadTo(10.75f, 4f)
                horizontalLineTo(8.5f)
                verticalLineTo(3.5f)
                quadTo(8.5f, 2.81f, 8.91f, 2.41f)
                reflectiveQuadTo(10f, 2f)
                horizontalLineToRelative(4.5f)
                quadToRelative(0.69f, 0f, 1.09f, 0.41f)
                reflectiveQuadTo(16f, 3.5f)
                verticalLineTo(13f)
                quadToRelative(0f, 0.69f, -0.41f, 1.09f)
                reflectiveQuadTo(14.5f, 14.5f)
                horizontalLineTo(13f)
                close()
            }
        }.build()
}

/** Material Symbols kid_star (rounded) — main app section header. */
internal val KidStarIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "kid_star", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(6.54f, 5.46f)
                lineTo(8.83f, 2.5f)
                quadTo(9.06f, 2.21f, 9.38f, 2.06f)
                reflectiveQuadTo(10.02f, 1.92f)
                reflectiveQuadToRelative(0.64f, 0.15f)
                reflectiveQuadTo(11.19f, 2.5f)
                lineToRelative(2.29f, 2.96f)
                lineToRelative(3.5f, 1.15f)
                quadToRelative(0.48f, 0.17f, 0.75f, 0.55f)
                reflectiveQuadTo(18f, 8.02f)
                quadToRelative(0f, 0.23f, -0.06f, 0.45f)
                reflectiveQuadTo(17.73f, 8.9f)
                lineToRelative(-2.25f, 3.15f)
                lineToRelative(0.08f, 3.38f)
                quadToRelative(0f, 0.65f, -0.44f, 1.09f)
                reflectiveQuadToRelative(-1.04f, 0.45f)
                quadToRelative(-0.04f, 0f, -0.4f, -0.06f)
                lineTo(10f, 15.88f)
                lineTo(6.31f, 16.9f)
                quadToRelative(-0.1f, 0.04f, -0.2f, 0.05f)
                reflectiveQuadToRelative(-0.2f, 0.01f)
                quadToRelative(-0.6f, 0f, -1.05f, -0.45f)
                reflectiveQuadTo(4.44f, 15.42f)
                lineTo(4.52f, 12.04f)
                lineTo(2.27f, 8.9f)
                quadTo(2.13f, 8.69f, 2.06f, 8.48f)
                reflectiveQuadTo(2f, 8.04f)
                quadTo(2f, 7.56f, 2.27f, 7.17f)
                reflectiveQuadTo(3.04f, 6.6f)
                lineTo(6.54f, 5.46f)
                close()
            }
        }.build()
}

/** Material Symbols edit (filled) — edit action. */
internal val EditFilledIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "edit", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(3.75f, 17f)
                quadTo(3.44f, 17f, 3.22f, 16.79f)
                reflectiveQuadTo(3f, 16.25f)
                verticalLineTo(14.44f)
                quadToRelative(0f, -0.3f, 0.1f, -0.57f)
                reflectiveQuadToRelative(0.33f, -0.5f)
                lineTo(13.38f, 3.44f)
                quadTo(13.6f, 3.21f, 13.87f, 3.1f)
                reflectiveQuadTo(14.43f, 3f)
                reflectiveQuadTo(15f, 3.1f)
                reflectiveQuadToRelative(0.5f, 0.33f)
                lineTo(16.56f, 4.5f)
                quadTo(16.79f, 4.73f, 16.9f, 5f)
                reflectiveQuadTo(17f, 5.55f)
                quadToRelative(0f, 0.3f, -0.1f, 0.57f)
                reflectiveQuadToRelative(-0.33f, 0.5f)
                lineTo(6.63f, 16.56f)
                quadTo(6.4f, 16.79f, 6.13f, 16.9f)
                reflectiveQuadTo(5.56f, 17f)
                horizontalLineTo(3.75f)
                close()
                moveTo(14.44f, 6.63f)
                lineTo(15.5f, 5.56f)
                lineTo(14.44f, 4.5f)
                lineTo(13.38f, 5.56f)
                lineToRelative(1.06f, 1.06f)
                close()
            }
        }.build()
}

/** Material Symbols mobile_arrow_up_right (rounded) — create shortcut. */
internal val MobileArrowUpRightIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "mobile_arrow_up_right", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(6f, 19f)
                quadTo(5.38f, 19f, 4.94f, 18.56f)
                reflectiveQuadTo(4.5f, 17.5f)
                verticalLineTo(2.5f)
                quadTo(4.5f, 1.81f, 4.91f, 1.41f)
                reflectiveQuadTo(6f, 1f)
                horizontalLineToRelative(8f)
                quadToRelative(0.62f, 0f, 1.06f, 0.44f)
                reflectiveQuadTo(15.5f, 2.5f)
                verticalLineToRelative(3f)
                quadToRelative(0.42f, 0f, 0.71f, 0.29f)
                reflectiveQuadTo(16.5f, 6.5f)
                verticalLineToRelative(2f)
                quadToRelative(0f, 0.42f, -0.29f, 0.71f)
                reflectiveQuadTo(15.5f, 9.5f)
                verticalLineToRelative(8f)
                quadToRelative(0f, 0.62f, -0.44f, 1.06f)
                reflectiveQuadTo(14f, 19f)
                horizontalLineTo(6f)
                close()
                moveToRelative(5f, -8.94f)
                verticalLineToRelative(1.19f)
                quadToRelative(0f, 0.32f, 0.21f, 0.53f)
                reflectiveQuadTo(11.75f, 12f)
                reflectiveQuadToRelative(0.54f, -0.22f)
                reflectiveQuadTo(12.5f, 11.25f)
                verticalLineToRelative(-3f)
                quadToRelative(0f, -0.32f, -0.22f, -0.54f)
                reflectiveQuadTo(11.75f, 7.5f)
                horizontalLineToRelative(-3f)
                quadTo(8.44f, 7.5f, 8.22f, 7.71f)
                reflectiveQuadTo(8f, 8.25f)
                reflectiveQuadTo(8.21f, 8.78f)
                reflectiveQuadTo(8.75f, 9f)
                horizontalLineTo(9.94f)
                lineTo(8.02f, 10.92f)
                quadTo(7.79f, 11.14f, 7.79f, 11.44f)
                reflectiveQuadToRelative(0.23f, 0.53f)
                reflectiveQuadToRelative(0.53f, 0.23f)
                reflectiveQuadTo(9.08f, 11.98f)
                lineTo(11f, 10.06f)
                close()
            }
        }.build()
}

/** Material Symbols file_copy (rounded) — duplicate action. */
internal val FileCopyIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "file_copy", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(15.5f, 16f)
                horizontalLineToRelative(-9f)
                quadTo(5.9f, 16f, 5.45f, 15.55f)
                reflectiveQuadTo(5f, 14.5f)
                verticalLineTo(2.5f)
                quadTo(5f, 1.9f, 5.45f, 1.45f)
                reflectiveQuadTo(6.5f, 1f)
                horizontalLineToRelative(5.88f)
                quadToRelative(0.29f, 0f, 0.56f, 0.1f)
                reflectiveQuadToRelative(0.5f, 0.33f)
                lineToRelative(3.13f, 3.13f)
                quadToRelative(0.23f, 0.23f, 0.33f, 0.5f)
                reflectiveQuadTo(17f, 5.63f)
                verticalLineTo(14.5f)
                quadToRelative(0f, 0.6f, -0.45f, 1.05f)
                reflectiveQuadTo(15.5f, 16f)
                close()
                moveToRelative(0f, -10f)
                lineTo(12f, 2.5f)
                verticalLineTo(4.88f)
                quadToRelative(0f, 0.48f, 0.32f, 0.8f)
                reflectiveQuadTo(13.13f, 6f)
                horizontalLineTo(15.5f)
                close()
                moveTo(3.5f, 19f)
                quadTo(2.9f, 19f, 2.45f, 18.55f)
                reflectiveQuadTo(2f, 17.5f)
                verticalLineTo(6.75f)
                quadTo(2f, 6.44f, 2.22f, 6.22f)
                reflectiveQuadTo(2.75f, 6f)
                reflectiveQuadTo(3.28f, 6.22f)
                reflectiveQuadTo(3.5f, 6.75f)
                verticalLineTo(17.5f)
                horizontalLineToRelative(8.75f)
                quadToRelative(0.31f, 0f, 0.53f, 0.22f)
                reflectiveQuadTo(13f, 18.25f)
                reflectiveQuadToRelative(-0.22f, 0.53f)
                reflectiveQuadTo(12.25f, 19f)
                horizontalLineTo(3.5f)
                close()
            }
        }.build()
}

/** Material Symbols delete_forever (filled) — delete action. */
internal val DeleteForeverFilledIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "delete_forever", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(6.5f, 17f)
                quadTo(5.88f, 17f, 5.44f, 16.56f)
                reflectiveQuadTo(5f, 15.5f)
                verticalLineTo(5.5f)
                horizontalLineTo(4.75f)
                quadTo(4.44f, 5.5f, 4.22f, 5.28f)
                reflectiveQuadTo(4f, 4.75f)
                reflectiveQuadTo(4.22f, 4.22f)
                reflectiveQuadTo(4.75f, 4f)
                horizontalLineTo(8f)
                verticalLineTo(3.75f)
                quadTo(8f, 3.44f, 8.22f, 3.22f)
                reflectiveQuadTo(8.75f, 3f)
                horizontalLineToRelative(2.5f)
                quadToRelative(0.31f, 0f, 0.53f, 0.22f)
                reflectiveQuadTo(12f, 3.75f)
                verticalLineTo(4f)
                horizontalLineToRelative(3.25f)
                quadToRelative(0.31f, 0f, 0.53f, 0.22f)
                reflectiveQuadTo(16f, 4.75f)
                reflectiveQuadTo(15.78f, 5.28f)
                reflectiveQuadTo(15.25f, 5.5f)
                horizontalLineTo(15f)
                verticalLineToRelative(10f)
                quadToRelative(0f, 0.63f, -0.44f, 1.06f)
                reflectiveQuadTo(13.5f, 17f)
                horizontalLineToRelative(-7f)
                close()
                moveTo(10f, 11.56f)
                lineToRelative(1.15f, 1.15f)
                quadToRelative(0.23f, 0.23f, 0.53f, 0.22f)
                reflectiveQuadToRelative(0.53f, -0.24f)
                reflectiveQuadToRelative(0.23f, -0.53f)
                reflectiveQuadTo(12.21f, 11.63f)
                lineTo(11.06f, 10.5f)
                lineTo(12.21f, 9.35f)
                quadTo(12.44f, 9.13f, 12.44f, 8.82f)
                reflectiveQuadTo(12.21f, 8.29f)
                reflectiveQuadTo(11.68f, 8.06f)
                quadToRelative(-0.3f, 0f, -0.53f, 0.23f)
                lineTo(10f, 9.44f)
                lineTo(8.85f, 8.29f)
                quadTo(8.63f, 8.06f, 8.33f, 8.06f)
                reflectiveQuadTo(7.81f, 8.29f)
                reflectiveQuadTo(7.58f, 8.82f)
                reflectiveQuadTo(7.81f, 9.35f)
                lineTo(8.94f, 10.5f)
                lineTo(7.79f, 11.65f)
                quadTo(7.56f, 11.88f, 7.57f, 12.17f)
                reflectiveQuadToRelative(0.24f, 0.52f)
                reflectiveQuadToRelative(0.53f, 0.23f)
                reflectiveQuadTo(8.88f, 12.69f)
                lineTo(10f, 11.56f)
                close()
            }
        }.build()
}

/** Material Symbols delete (rounded) — editor delete action. */
internal val DeleteIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "delete", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(6.5f, 17f)
                quadTo(5.88f, 17f, 5.44f, 16.56f)
                reflectiveQuadTo(5f, 15.5f)
                verticalLineTo(5.5f)
                horizontalLineTo(4.75f)
                quadTo(4.43f, 5.5f, 4.22f, 5.29f)
                reflectiveQuadTo(4f, 4.75f)
                reflectiveQuadTo(4.22f, 4.22f)
                reflectiveQuadTo(4.75f, 4f)
                horizontalLineTo(8f)
                verticalLineTo(3.75f)
                quadTo(8f, 3.43f, 8.22f, 3.22f)
                reflectiveQuadTo(8.75f, 3f)
                horizontalLineToRelative(2.5f)
                quadToRelative(0.32f, 0f, 0.53f, 0.22f)
                reflectiveQuadTo(12f, 3.75f)
                verticalLineTo(4f)
                horizontalLineToRelative(3.25f)
                quadToRelative(0.32f, 0f, 0.53f, 0.21f)
                reflectiveQuadTo(16f, 4.75f)
                reflectiveQuadTo(15.78f, 5.28f)
                reflectiveQuadTo(15.25f, 5.5f)
                horizontalLineTo(15f)
                verticalLineToRelative(9.99f)
                quadToRelative(0f, 0.63f, -0.44f, 1.07f)
                reflectiveQuadTo(13.5f, 17f)
                horizontalLineToRelative(-7f)
                close()
                moveToRelative(7f, -11.5f)
                horizontalLineToRelative(-7f)
                verticalLineToRelative(10f)
                horizontalLineToRelative(7f)
                verticalLineTo(5.5f)
                close()
                moveTo(9.28f, 13.78f)
                quadTo(9.5f, 13.57f, 9.5f, 13.25f)
                verticalLineTo(7.75f)
                quadTo(9.5f, 7.43f, 9.29f, 7.22f)
                reflectiveQuadTo(8.75f, 7f)
                reflectiveQuadTo(8.22f, 7.22f)
                reflectiveQuadTo(8f, 7.75f)
                verticalLineToRelative(5.5f)
                quadToRelative(0f, 0.32f, 0.21f, 0.53f)
                reflectiveQuadTo(8.75f, 14f)
                reflectiveQuadTo(9.28f, 13.78f)
                close()
                moveToRelative(2.5f, 0f)
                quadTo(12f, 13.57f, 12f, 13.25f)
                verticalLineTo(7.75f)
                quadTo(12f, 7.43f, 11.79f, 7.22f)
                reflectiveQuadTo(11.25f, 7f)
                reflectiveQuadTo(10.72f, 7.22f)
                reflectiveQuadTo(10.5f, 7.75f)
                verticalLineToRelative(5.5f)
                quadToRelative(0f, 0.32f, 0.21f, 0.53f)
                reflectiveQuadTo(11.25f, 14f)
                reflectiveQuadToRelative(0.54f, -0.22f)
                close()
                moveTo(6.5f, 5.5f)
                verticalLineToRelative(10f)
                verticalLineTo(5.5f)
                close()
            }
        }.build()
}

/** Material Symbols check (rounded) — editor save action. */
internal val CheckIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "check", defaultWidth = 20.dp, defaultHeight = 20.dp, viewportWidth = 20f, viewportHeight = 20f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(8.1f, 12.31f)
                lineTo(14.33f, 6.08f)
                quadTo(14.56f, 5.85f, 14.86f, 5.85f)
                reflectiveQuadTo(15.4f, 6.08f)
                quadToRelative(0.23f, 0.23f, 0.23f, 0.53f)
                reflectiveQuadTo(15.4f, 7.15f)
                lineTo(8.65f, 13.92f)
                quadTo(8.42f, 14.15f, 8.11f, 14.15f)
                reflectiveQuadTo(7.58f, 13.92f)
                lineTo(4.6f, 10.94f)
                quadTo(4.38f, 10.71f, 4.38f, 10.41f)
                reflectiveQuadTo(4.6f, 9.88f)
                reflectiveQuadTo(5.14f, 9.65f)
                reflectiveQuadTo(5.67f, 9.88f)
                lineTo(8.1f, 12.31f)
                close()
            }
        }.build()
}

/** Material Symbols chevron_right (rounded) — segmented list row affordance. */
internal val ChevronRightIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "chevron_right", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(12.6f, 12f)
                lineTo(8.7f, 8.1f)
                quadTo(8.43f, 7.82f, 8.43f, 7.4f)
                reflectiveQuadTo(8.7f, 6.7f)
                reflectiveQuadTo(9.4f, 6.43f)
                reflectiveQuadTo(10.1f, 6.7f)
                lineToRelative(4.6f, 4.6f)
                quadToRelative(0.15f, 0.15f, 0.21f, 0.33f)
                reflectiveQuadTo(14.98f, 12f)
                reflectiveQuadToRelative(-0.06f, 0.38f)
                reflectiveQuadTo(14.7f, 12.7f)
                lineToRelative(-4.6f, 4.6f)
                quadTo(9.83f, 17.58f, 9.4f, 17.58f)
                reflectiveQuadTo(8.7f, 17.3f)
                quadTo(8.43f, 17.02f, 8.43f, 16.6f)
                reflectiveQuadTo(8.7f, 15.9f)
                lineTo(12.6f, 12f)
                close()
            }
        }.build()
}

/** Material Symbols search (rounded) — search bar leading icon. */
internal val SearchIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "search", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(9.5f, 16f)
                quadTo(6.78f, 16f, 4.89f, 14.11f)
                quadTo(3f, 12.23f, 3f, 9.5f)
                quadTo(3f, 6.77f, 4.89f, 4.89f)
                reflectiveQuadTo(9.5f, 3f)
                reflectiveQuadToRelative(4.61f, 1.89f)
                reflectiveQuadTo(16f, 9.5f)
                quadToRelative(0f, 1.1f, -0.35f, 2.07f)
                reflectiveQuadTo(14.7f, 13.3f)
                lineToRelative(5.6f, 5.6f)
                quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
                quadToRelative(0f, 0.42f, -0.28f, 0.7f)
                quadToRelative(-0.27f, 0.27f, -0.7f, 0.27f)
                reflectiveQuadTo(18.9f, 20.3f)
                lineTo(13.3f, 14.7f)
                quadToRelative(-0.75f, 0.6f, -1.72f, 0.95f)
                reflectiveQuadTo(9.5f, 16f)
                close()
                moveToRelative(0f, -2f)
                quadToRelative(1.88f, 0f, 3.19f, -1.31f)
                reflectiveQuadTo(14f, 9.5f)
                reflectiveQuadTo(12.69f, 6.31f)
                reflectiveQuadTo(9.5f, 5f)
                reflectiveQuadTo(6.31f, 6.31f)
                reflectiveQuadTo(5f, 9.5f)
                reflectiveQuadToRelative(1.31f, 3.19f)
                reflectiveQuadTo(9.5f, 14f)
                close()
            }
        }.build()
}

/** Material Symbols close (rounded, 24dp) — search bar trailing clear icon. */
internal val CloseIcon24: ImageVector by lazy {
    ImageVector
        .Builder(name = "close", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(12f, 13.4f)
                lineTo(7.1f, 18.3f)
                quadTo(6.83f, 18.58f, 6.4f, 18.58f)
                reflectiveQuadTo(5.7f, 18.3f)
                quadTo(5.43f, 18.02f, 5.43f, 17.6f)
                reflectiveQuadTo(5.7f, 16.9f)
                lineTo(10.6f, 12f)
                lineTo(5.7f, 7.1f)
                quadTo(5.43f, 6.82f, 5.43f, 6.4f)
                reflectiveQuadTo(5.7f, 5.7f)
                reflectiveQuadTo(6.4f, 5.43f)
                reflectiveQuadTo(7.1f, 5.7f)
                lineTo(12f, 10.6f)
                lineTo(16.9f, 5.7f)
                quadTo(17.18f, 5.43f, 17.6f, 5.43f)
                reflectiveQuadTo(18.3f, 5.7f)
                reflectiveQuadToRelative(0.27f, 0.7f)
                reflectiveQuadTo(18.3f, 7.1f)
                lineTo(13.4f, 12f)
                lineToRelative(4.9f, 4.9f)
                quadToRelative(0.27f, 0.28f, 0.27f, 0.7f)
                quadToRelative(0f, 0.42f, -0.27f, 0.7f)
                reflectiveQuadToRelative(-0.7f, 0.27f)
                reflectiveQuadTo(16.9f, 18.3f)
                lineTo(12f, 13.4f)
                close()
            }
        }.build()
}

/** Material Symbols chevron_left (rounded) — search bar leading icon when expanded. */
internal val ChevronLeftIcon: ImageVector by lazy {
    ImageVector
        .Builder(name = "chevron_left", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(10.8f, 12f)
                lineToRelative(3.9f, 3.9f)
                quadToRelative(0.28f, 0.28f, 0.28f, 0.7f)
                quadToRelative(0f, 0.42f, -0.28f, 0.7f)
                reflectiveQuadTo(14f, 17.58f)
                reflectiveQuadTo(13.3f, 17.3f)
                lineTo(8.7f, 12.7f)
                quadTo(8.55f, 12.55f, 8.49f, 12.38f)
                reflectiveQuadTo(8.43f, 12f)
                reflectiveQuadTo(8.49f, 11.63f)
                reflectiveQuadTo(8.7f, 11.3f)
                lineTo(13.3f, 6.7f)
                quadTo(13.58f, 6.43f, 14f, 6.43f)
                reflectiveQuadTo(14.7f, 6.7f)
                reflectiveQuadToRelative(0.28f, 0.7f)
                reflectiveQuadTo(14.7f, 8.1f)
                lineTo(10.8f, 12f)
                close()
            }
        }.build()
}

/** Material Symbols check (rounded, 24dp) — OK button and selected item trailing icon. */
internal val CheckIcon24: ImageVector by lazy {
    ImageVector
        .Builder(name = "check", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                fill = SolidColor(Color.Black),
                fillAlpha = 1f,
                stroke = null,
                strokeAlpha = 1f,
                strokeLineWidth = 1f,
                strokeLineCap = StrokeCap.Butt,
                strokeLineJoin = StrokeJoin.Bevel,
                strokeLineMiter = 1f,
                pathFillType = PathFillType.Companion.NonZero,
            ) {
                moveTo(9.55f, 15.15f)
                lineTo(18.03f, 6.68f)
                quadToRelative(0.3f, -0.3f, 0.7f, -0.3f)
                reflectiveQuadToRelative(0.7f, 0.3f)
                quadToRelative(0.3f, 0.3f, 0.3f, 0.71f)
                reflectiveQuadTo(19.43f, 8.1f)
                lineToRelative(-9.18f, 9.2f)
                quadToRelative(-0.3f, 0.3f, -0.7f, 0.3f)
                reflectiveQuadTo(8.85f, 17.3f)
                lineTo(4.55f, 13f)
                quadTo(4.25f, 12.7f, 4.26f, 12.29f)
                reflectiveQuadTo(4.58f, 11.58f)
                reflectiveQuadToRelative(0.71f, -0.3f)
                reflectiveQuadTo(6f, 11.58f)
                lineToRelative(3.55f, 3.58f)
                close()
            }
        }.build()
}
