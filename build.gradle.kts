// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.version.catalog.update)
    alias(libs.plugins.ben.manes.versions)
}

spotless {
    java {
        target("app/src/**/*.java")
        googleJavaFormat()
    }
    kotlin {
        target("app/src/**/*.kt")
        ktlint()
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktlint()
    }
}
