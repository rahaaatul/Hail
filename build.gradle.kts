// Top-level build file where you can add configuration options common to all sub-projects/modules.
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
