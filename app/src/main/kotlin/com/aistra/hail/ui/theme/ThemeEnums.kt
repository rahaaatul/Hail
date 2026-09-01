package com.aistra.hail.ui.theme

enum class ThemeMode(val displayName: String) {
    LIGHT("Light"), DARK("Dark"), SYSTEM("System");
    companion object { fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SYSTEM }
}

enum class PaletteStyle(val displayName: String) {
    TonalSpot("Tonal Spot"), Neutral("Neutral"), Vibrant("Vibrant"), Expressive("Expressive"),
    Rainbow("Rainbow"), FruitSalad("Fruit Salad"), Monochrome("Monochrome"),
    Fidelity("Fidelity"), Content("Content");
    val supportsSpec2025: Boolean
        get() = this in listOf(TonalSpot, Neutral, Vibrant, Expressive)
    companion object { fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: TonalSpot }
}

enum class ThemeColorSpec(val displayName: String) {
    SPEC_2021("Material 3 (2021)"), SPEC_2025("Expressive (2025)");
    companion object { fun fromValueOrDefault(value: String) = entries.find { it.name == value } ?: SPEC_2025 }
}
