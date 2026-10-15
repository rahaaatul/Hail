plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.aistra.hail"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.aistra.hail"
        minSdk = 24
        targetSdk = 37
        versionCode = (rootProject.findProperty("versionCode") as String?)?.toInt() ?: 0
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
        // project properties. With neither present the config stays empty and
        // the build type is left unsigned rather than debug-signed.
        create("release") {
            val keystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
            if (keystore != null && keystore.exists()) {
                storeFile = keystore
                storePassword = project.findProperty("releaseStorePassword") as String?
                keyAlias = project.findProperty("releaseKeyAlias") as String?
                keyPassword = project.findProperty("releaseKeyPassword") as String?
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-Debug"
            signingConfig = signingConfigs.getByName("release")
        }
        create("pr") {
            // Each PR installs as its own app: com.aistra.hail.pr.<n>.
            // versionName and versionCode are fully overridden by the workflow,
            // so there is deliberately no versionNameSuffix here.
            // ".pr<n>" with no dot: aapt2 rejects a package segment that starts
            // with a digit, so "com.aistra.hail.pr.79" cannot link. Every
            // segment must begin with a letter.
            applicationIdSuffix =
                providers.gradleProperty("prNumber").orNull?.let { ".pr$it" } ?: ".pr"
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Mirrors the keystore.exists() test inside the "release" signing
            // config above. The two must agree: assigning a config whose
            // storeFile was never set produces an opaque AGP failure
            // ("missing required property 'storeFile'") instead of a plainly
            // unsigned build.
            val keystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
            if (keystore != null && keystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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
        }
        // Do not compress resources that are already compressed
        // This can help with install time and reduce CPU usage
        // but may slightly increase APK size. For size optimization,
        // we'll keep the default behavior which is to compress
        // unless we have specific needs.
        // Do not zip align debug builds to save time during development
        // zipAlignEnabled = false

    // Automatically rename APK outputs to include app name, version, and build type
    applicationVariants.all { variant ->
        val appName = rootProject.findProperty("appName") as String?
        val versionName = variant.versionName.orNull
        if (appName != null && versionName != null) {
            variant.outputs.forEach { output ->
                if (output instanceof com.android.build.VariantOutput) {
                    val buildType = variant.buildType.name
                    output.outputFileName = "${appName}-v${versionName}-${buildType}.apk"
                }
            }
        }
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
