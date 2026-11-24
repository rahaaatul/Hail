package com.aistra.hail.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.aistra.hail.R

private val GoogleSansFlex =
    FontFamily(
        Font(R.font.google_sans_flex_regular, FontWeight.Normal),
        Font(R.font.google_sans_flex_medium, FontWeight.Medium),
        Font(R.font.google_sans_flex_semi_bold, FontWeight.SemiBold),
        Font(R.font.google_sans_flex_bold, FontWeight.Bold),
    )

val AppTypography = Typography(fontFamily = GoogleSansFlex)
