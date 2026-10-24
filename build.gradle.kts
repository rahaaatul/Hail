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
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.spotless)
}

spotless {
    // Only the files that a change touches are checked, so the formatting
    // that the code does not follow yet does not fail a build. The base is
    // the branch the change is merged into, which the workflow passes in.
    ratchetFrom(
        providers.gradleProperty("spotlessRatchetFrom")
            .getOrElse("origin/main"),
    )
    // A second target call replaces the first one, so every pattern of a
    // block goes into a single call.
    kotlin {
        target("app/src/**/*.kt")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "gradle/**/*.kts")
        ktlint(libs.versions.ktlint.get())
    }
    // The workflows are YAML, and the shell scripts they run are not
    // formatted by ktlint.
    format("yaml") {
        target(".github/**/*.yml", ".github/**/*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
        leadingTabsToSpaces(2)
    }
}
