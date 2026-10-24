buildscript {
    configurations.classpath {
        resolutionStrategy {
            // Force patched versions of AGP buildscript-classpath transitives
            // flagged by Dependabot (#22-#29). Build-time only — not packaged
            // in the APK — but the graph still reports them.
            force("org.apache.commons:commons-lang3:3.21.0")
            force("org.bouncycastle:bcprov-jdk18on:1.79")
            force("org.bouncycastle:bcpkix-jdk18on:1.79")
            force("org.bitbucket.b_c:jose4j:0.9.2")
            force("org.jdom:jdom2:2.0.6.1")
            force("org.apache.httpcomponents:httpclient:4.5.16")
        }
    }
}

import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.GroupingEntityType

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aistra.hail"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aistra.hail"
        minSdk = 24
        targetSdk = 37
        versionCode = (rootProject.findProperty("versionCode") as String?)?.toInt()?.takeIf { it > 0 } ?: 1
        versionName = rootProject.findProperty("version") as String?
        ndk {
            val abi = project.findProperty("abi") as String?
            if (abi != null) abiFilters += abi
        }
    }

    signingConfigs {
        // Release key: staged by release.yml, which decodes it from secrets into
        // RUNNER_TEMP because Gradle's signingConfig takes a File. The
        // credentials never touch the filesystem — they arrive as
        // ORG_GRADLE_PROJECT_* environment variables, which Gradle exposes as
        // project properties.
        //
        // The configuration is only created when the key is actually there.
        // A signing config named "release" is picked up by the build type of
        // the same name by default, so an empty one makes every build fail
        // with "Keystore file not set for signing config release", even the
        // tasks that never sign anything. Without the key the build is
        // unsigned, which is a state the build can complete.
        val keystore = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull?.let { file(it) }
        if (keystore != null && keystore.exists()) {
            create("release") {
                storeFile = keystore
                storePassword = providers.gradleProperty("releaseStorePassword").orNull
                keyAlias = providers.gradleProperty("releaseKeyAlias").orNull
                keyPassword = providers.gradleProperty("releaseKeyPassword").orNull
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-Debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Signed with the release key when it was staged, unsigned
            // otherwise. The guard must agree with the one that creates the
            // signing config above.
            signingConfigs.findByName("release")?.let { signingConfig = it }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            // Additional optimization settings
            // Enable strict shrinking to remove more unused code
            // This may cause issues if reflection is used incorrectly, but our rules should handle it
        }
    }
    androidResources {
        generateLocaleConfig = true
        // Do not compress the dex files, so the apk can be imported as a privileged app
        noCompress += "dex"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
    testOptions {
        unitTests.all {
            it.jvmArgs("-Dnet.bytebuddy.experimental=true")
        }
    }
      lint {
            // Enable checking for unused resources
            checkReleaseBuilds = true
            // Treat warnings as errors to catch issues early
            // Disable specific lint checks that are not relevant or too noisy
            disable += "IconTip"
            disable += "ContentDescription"
            disable += "MissingPrefix"
            // Do not compress resources that are already compressed
            // This can help with install time and reduce CPU usage
            // but may slightly increase APK size. For size optimization,
            // we'll keep the default behavior which is to compress
            // unless we have specific needs.
             // Do not zip align debug builds to save time during development
             // zipAlignEnabled = false
      }


    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }
    kotlin {
        jvmToolchain(21)
    }
}

// The APK is named after the app, the version and the build variant, so that
// a downloaded build says which one it is: {app_name}-v{version_name}-{build_variant}.apk
// The legacy android.applicationVariants API that used to do this is gone in
// AGP 9.
androidComponents {
    onVariants { variant ->
        val buildTypeName = variant.buildType
        val appName = (rootProject.findProperty("appName") as String?)?.lowercase()
        if (buildTypeName != null && appName != null) {
            // A build type can carry a suffix of its own on the version name,
            // and the debug build type does. The suffix is dropped here,
            // because the build variant is already in the name of the file.
            val suffix = android.buildTypes.getByName(buildTypeName).versionNameSuffix
            variant.outputs.forEach { output ->
                val versionName = output.versionName.orNull?.removeSuffix(suffix.orEmpty())
                if (versionName != null) {
                    output.outputFileName.set("$appName-v$versionName-$buildTypeName.apk")
                }
            }
        }
    }
}

detekt {
    // detekt 1.x only registers its per-variant tasks when the kotlin-android
    // plugin is applied. AGP 9's built-in Kotlin never applies it, so only the
    // plain "detekt" task is created, and that task touches no AGP variant
    // API - the legacy android.applicationVariants is gone in AGP 9.
    toolVersion = libs.versions.detekt.get()
    // Layer config/detekt.yml on top of detekt's defaults instead of replacing
    // them, so the project config only has to carry the exceptions.
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("config/detekt.yml"))
    // The baseline records every finding that exists today, so the build only
    // fails on findings a change introduces. Regenerate it with
    // ./gradlew detektBaseline.
    baseline = rootProject.file("config/detekt-baseline.xml")
    // Report paths are relative to this directory.
    basePath = rootProject.projectDir.absolutePath
    source.setFrom(
        files(
            "src/main/java",
            "src/main/kotlin",
            "src/test/java",
            "src/test/kotlin",
            "src/androidTest/java",
            "src/androidTest/kotlin",
        ),
    )
    parallel = true
}

kover {
    currentProject {
        // A report variant that holds the classes the unit tests reach. The
        // total variant holds every class, and the percentage of that is
        // low enough that a bound on it would say nothing.
        createVariant("tested") {
            add("debug")
            add("release")
        }
    }
    reports {
        // The full report is published as a CI artifact.
        total {
            xml {
                onCheck = false
            }
            html {
                onCheck = false
            }
        }
        // The gate. It only looks at the classes that have a test suite, so
        // the bound says "the tests of this class still run and still cover
        // it" instead of "the whole app is at least N percent covered", which
        // falls every time a feature is added.
        variant("tested") {
            filters {
                includes {
                    classes("com.aistra.hail.utils.HBackup*")
                    classes("com.aistra.hail.utils.AppActions*")
                    classes("com.aistra.hail.utils.ActionExecutor*")
                    classes("com.aistra.hail.app.HailData*")
                }
            }
            verify {
                rule {
                    groupBy = GroupingEntityType.CLASS
                    bound {
                        minValue = 25
                        coverageUnits = CoverageUnit.LINE
                        aggregationForGroup = AggregationType.COVERED_PERCENTAGE
                    }
                }
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric.compose)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.navigation.compose)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.documentfile)
    implementation(libs.pinyin4j)
    implementation(libs.material)
    implementation(libs.insetter)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
    implementation(libs.dhizuku.api)
    implementation(libs.appiconloader)
    implementation(libs.compose.preference)
    implementation(libs.commons.text)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hiddenapibypass)
    implementation(libs.libsu.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.sqlite.wrapper)
    implementation(libs.androidx.sqlite)
    ksp(libs.androidx.room.compiler)
    compileOnly(libs.libxposed.api)

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.ext:truth:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("androidx.room3:room3-testing:3.0.3")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("org.json:json:20260814")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("androidx.room3:room3-testing:3.0.3")
}
