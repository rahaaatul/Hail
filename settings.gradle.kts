pluginManagement {
    repositories {
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        google()
        mavenCentral()
    }
}
rootProject.name = "Hail"
include(":app")
