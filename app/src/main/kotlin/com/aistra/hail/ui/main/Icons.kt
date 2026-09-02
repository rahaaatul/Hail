package com.aistra.hail.ui.main

import androidx.compose.material.icons.Icons
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

@Suppress("CheckReturnValue")
val Icons.Filled.Home: ImageVector
  get() {
    if (_home != null) {
      return _home!!
    }
    _home =
      ImageVector.Builder(
          name = "snowflake",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Unspecified),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(11f, 22f)
            verticalLineTo(20.18f)
            lineTo(9.4f, 21.53f)
            lineTo(8.1f, 20f)
            lineTo(11f, 17.55f)
            verticalLineToRelative(-3.8f)
            lineToRelative(-3.3f, 1.9f)
            lineTo(7.03f, 19.38f)
            lineTo(5.05f, 19.02f)
            lineTo(5.43f, 16.95f)
            lineToRelative(-1.6f, 0.93f)
            lineToRelative(-1f, -1.73f)
            lineToRelative(1.6f, -0.92f)
            lineTo(2.45f, 14.5f)
            lineTo(3.13f, 12.63f)
            lineTo(6.7f, 13.9f)
            lineTo(10f, 12f)
            lineTo(6.7f, 10.1f)
            lineTo(3.13f, 11.4f)
            lineTo(2.45f, 9.5f)
            lineTo(4.43f, 8.8f)
            lineTo(2.83f, 7.88f)
            lineToRelative(1f, -1.73f)
            lineToRelative(1.6f, 0.93f)
            lineTo(5.05f, 5f)
            lineTo(7.03f, 4.65f)
            lineTo(7.7f, 8.38f)
            lineToRelative(3.3f, 1.9f)
            verticalLineTo(6.47f)
            lineTo(8.1f, 4.02f)
            lineTo(9.4f, 2.5f)
            lineTo(11f, 3.85f)
            verticalLineTo(2f)
            horizontalLineToRelative(2f)
            verticalLineTo(3.85f)
            lineTo(14.6f, 2.5f)
            lineToRelative(1.3f, 1.52f)
            lineTo(13f, 6.47f)
            verticalLineToRelative(3.8f)
            lineToRelative(3.28f, -1.9f)
            lineTo(16.95f, 4.65f)
            lineTo(18.93f, 5f)
            lineTo(18.55f, 7.07f)
            lineToRelative(1.6f, -0.93f)
            lineToRelative(1f, 1.73f)
            lineTo(19.55f, 8.8f)
            lineToRelative(1.98f, 0.7f)
            lineToRelative(-0.68f, 1.9f)
            lineTo(17.28f, 10.1f)
            lineTo(14f, 12f)
            lineToRelative(3.28f, 1.9f)
            lineToRelative(3.57f, -1.27f)
            lineToRelative(0.68f, 1.88f)
            lineToRelative(-1.98f, 0.72f)
            lineToRelative(1.6f, 0.92f)
            lineToRelative(-1f, 1.73f)
            lineToRelative(-1.6f, -0.93f)
            lineToRelative(0.38f, 2.07f)
            lineToRelative(-1.98f, 0.35f)
            lineTo(16.28f, 15.65f)
            lineTo(13f, 13.75f)
            verticalLineToRelative(3.8f)
            lineTo(15.9f, 20f)
            lineToRelative(-1.3f, 1.52f)
            lineTo(13f, 20.18f)
            verticalLineTo(22f)
            horizontalLineTo(11f)
            close()
          }
        }
        .build()
    return _home!!
  }

private var _home: ImageVector? = null

@Suppress("CheckReturnValue")
val Icons.Filled.Automation: ImageVector
  get() {
    if (_automation != null) {
      return _automation!!
    }
    _automation =
      ImageVector.Builder(
          name = "automation",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Unspecified),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(7.4f, 17.25f)
            quadToRelative(-1.05f, 0.88f, -2.19f, 0.8f)
            reflectiveQuadTo(3.23f, 17.27f)
            quadTo(2.38f, 16.58f, 2.06f, 15.44f)
            reflectiveQuadTo(2.48f, 13.1f)
            lineTo(4.35f, 10f)
            quadTo(3.73f, 9.45f, 3.36f, 8.67f)
            reflectiveQuadTo(3f, 7f)
            quadTo(3f, 5.35f, 4.18f, 4.17f)
            reflectiveQuadTo(7f, 3f)
            reflectiveQuadTo(9.83f, 4.17f)
            reflectiveQuadTo(11f, 7f)
            reflectiveQuadTo(9.83f, 9.82f)
            reflectiveQuadTo(7f, 11f)
            quadTo(6.78f, 11f, 6.55f, 10.98f)
            reflectiveQuadTo(6.13f, 10.9f)
            lineTo(4.2f, 14.15f)
            quadTo(3.93f, 14.6f, 4.03f, 15.04f)
            reflectiveQuadToRelative(0.42f, 0.71f)
            reflectiveQuadToRelative(0.78f, 0.31f)
            reflectiveQuadTo(6.1f, 15.75f)
            lineTo(16.6f, 6.72f)
            quadTo(17.65f, 5.85f, 18.8f, 5.94f)
            quadToRelative(1.15f, 0.09f, 2f, 0.79f)
            quadToRelative(0.85f, 0.7f, 1.15f, 1.84f)
            reflectiveQuadTo(21.53f, 10.9f)
            lineTo(19.65f, 14f)
            quadToRelative(0.63f, 0.55f, 0.99f, 1.32f)
            reflectiveQuadTo(21f, 17f)
            quadToRelative(0f, 1.65f, -1.17f, 2.82f)
            reflectiveQuadTo(17f, 21f)
            reflectiveQuadTo(14.18f, 19.83f)
            reflectiveQuadTo(13f, 17f)
            reflectiveQuadToRelative(1.18f, -2.83f)
            reflectiveQuadTo(17f, 13f)
            quadToRelative(0.23f, 0f, 0.44f, 0.02f)
            reflectiveQuadToRelative(0.41f, 0.08f)
            lineTo(19.8f, 9.85f)
            quadTo(20.08f, 9.4f, 19.98f, 8.96f)
            reflectiveQuadTo(19.55f, 8.25f)
            quadTo(19.23f, 7.97f, 18.78f, 7.94f)
            reflectiveQuadTo(17.9f, 8.25f)
            lineToRelative(-10.5f, 9f)
            close()
            moveTo(8.41f, 8.41f)
            quadTo(9f, 7.82f, 9f, 7f)
            reflectiveQuadTo(8.41f, 5.59f)
            quadTo(7.83f, 5f, 7f, 5f)
            reflectiveQuadTo(5.59f, 5.59f)
            quadTo(5f, 6.18f, 5f, 7f)
            reflectiveQuadTo(5.59f, 8.41f)
            reflectiveQuadTo(7f, 9f)
            quadTo(7.83f, 9f, 8.41f, 8.41f)
            close()
            moveToRelative(10f, 10f)
            quadTo(19f, 17.83f, 19f, 17f)
            reflectiveQuadTo(18.41f, 15.59f)
            reflectiveQuadTo(17f, 15f)
            reflectiveQuadToRelative(-1.41f, 0.59f)
            reflectiveQuadTo(15f, 17f)
            reflectiveQuadToRelative(0.59f, 1.41f)
            reflectiveQuadTo(17f, 19f)
            reflectiveQuadToRelative(1.41f, -0.59f)
            close()
            moveTo(7f, 7f)
            close()
            moveTo(17f, 17f)
            close()
          }
        }
        .build()
    return _automation!!
  }

private var _automation: ImageVector? = null

@Suppress("CheckReturnValue")
val Icons.Filled.Settings: ImageVector
  get() {
    if (_settings != null) {
      return _settings!!
    }
    _settings =
      ImageVector.Builder(
          name = "settings",
          defaultWidth = 24.dp,
          defaultHeight = 24.dp,
          viewportWidth = 24f,
          viewportHeight = 24f,
        )
        .apply {
          path(
            fill = SolidColor(Color.Unspecified),
            fillAlpha = 1f,
            stroke = null,
            strokeAlpha = 1f,
            strokeLineWidth = 1f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Bevel,
            strokeLineMiter = 1f,
            pathFillType = PathFillType.Companion.NonZero,
          ) {
            moveTo(9.25f, 22f)
            lineTo(8.85f, 18.8f)
            quadTo(8.53f, 18.68f, 8.24f, 18.5f)
            reflectiveQuadTo(7.68f, 18.13f)
            lineTo(4.7f, 19.38f)
            lineTo(1.95f, 14.63f)
            lineTo(4.53f, 12.68f)
            quadTo(4.5f, 12.5f, 4.5f, 12.34f)
            quadToRelative(0f, -0.16f, 0f, -0.34f)
            reflectiveQuadToRelative(0f, -0.34f)
            reflectiveQuadTo(4.53f, 11.33f)
            lineTo(1.95f, 9.38f)
            lineTo(4.7f, 4.63f)
            lineTo(7.68f, 5.88f)
            quadTo(7.95f, 5.68f, 8.25f, 5.5f)
            reflectiveQuadTo(8.85f, 5.2f)
            lineTo(9.25f, 2f)
            horizontalLineToRelative(5.5f)
            lineToRelative(0.4f, 3.2f)
            quadToRelative(0.33f, 0.13f, 0.61f, 0.3f)
            reflectiveQuadToRelative(0.56f, 0.38f)
            lineTo(19.3f, 4.63f)
            lineToRelative(2.75f, 4.75f)
            lineToRelative(-2.57f, 1.95f)
            quadToRelative(0.02f, 0.18f, 0.02f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(0f, 0.34f)
            reflectiveQuadToRelative(-0.05f, 0.34f)
            lineToRelative(2.57f, 1.95f)
            lineToRelative(-2.75f, 4.75f)
            lineTo(16.33f, 18.13f)
            quadToRelative(-0.27f, 0.2f, -0.57f, 0.38f)
            reflectiveQuadToRelative(-0.6f, 0.3f)
            lineTo(14.75f, 22f)
            horizontalLineTo(9.25f)
            close()
            moveToRelative(2.8f, -6.5f)
            quadToRelative(1.45f, 0f, 2.47f, -1.03f)
            reflectiveQuadTo(15.55f, 12f)
            reflectiveQuadTo(14.53f, 9.52f)
            reflectiveQuadTo(12.05f, 8.5f)
            quadToRelative(-1.47f, 0f, -2.49f, 1.02f)
            reflectiveQuadTo(8.55f, 12f)
            reflectiveQuadToRelative(1.01f, 2.47f)
            reflectiveQuadToRelative(2.49f, 1.03f)
            close()
          }
        }
        .build()
    return _settings!!
  }

private var _settings: ImageVector? = null
