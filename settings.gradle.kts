pluginManagement {
    repositories {
        gradlePluginPortal()
        maven { url = uri("https://packages.jetbrains.team/maven/p/kt/dev") }
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
rootProject.name = "Hail"
include(":app")
