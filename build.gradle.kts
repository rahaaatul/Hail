// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    configurations.classpath {
        resolutionStrategy {
            // The Android Gradle Plugin pulls commons-compress, which resolves
            // commons-lang3 3.16.0, a version affected by CVE-2025-48924.
            force("org.apache.commons:commons-lang3:3.21.0")
        }
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
}
