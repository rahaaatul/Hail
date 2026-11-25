pluginManagement {
    repositories {
        maven { url = uri("https://packages.jetbrains.team/maven/p/kt/dev") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

// Force patched FreeMarker on the buildscript classpath (Dependabot #33,
// GHSA-27j2-h3m2-8237 / CVE-2026-84939). Build-time only — not packaged
// in the APK — but the graph still reports the vulnerable version.
gradle.beforeProject {
    buildscript.configurations.configureEach {
        resolutionStrategy.force("org.freemarker:freemarker:2.3.35")
    }
    configurations.configureEach {
        // Guava is transitive; force patched 33.7.2-jre on classpaths only
        // (Dependabot #35, GHSA-xxph-c9ww-hj94). No Hail API usage.
        if (name == "runtimeClasspath" || name == "compileClasspath") {
            resolutionStrategy.force("com.google.guava:guava:33.7.2-jre")
        }
    }
}
rootProject.name = "Hail"
include(":app")
