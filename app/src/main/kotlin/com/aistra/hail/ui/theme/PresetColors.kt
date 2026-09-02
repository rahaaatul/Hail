package com.aistra.hail.ui.theme

import androidx.compose.ui.graphics.Color

data class RawColor(val key: String, val color: Color)

val PresetColors = listOf(
    RawColor("default",     Color(0xFF6750A4)),
    RawColor("purple",      Color(0xFF7E42A4)),
    RawColor("deep_purple", Color(0xFF5355A9)),
    RawColor("indigo",      Color(0xFF335BBC)),
    RawColor("teal",        Color(0xFF006874)),
    RawColor("green",       Color(0xFF006D39)),
    RawColor("orange",      Color(0xFF944A00)),
    RawColor("red",         Color(0xFFBA1A1A)),
    RawColor("brown",       Color(0xFF7D524A)),
    RawColor("grey",        Color(0xFF5F6162)),
    RawColor("pink",        Color(0xFFB02575)),
    RawColor("cyan",        Color(0xFF006A6B)),
    RawColor("amber",       Color(0xFF7A5700)),
    RawColor("lime",        Color(0xFF4C6D00)),
    RawColor("blue_grey",   Color(0xFF4A6267)),
)
