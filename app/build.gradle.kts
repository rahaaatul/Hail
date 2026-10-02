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
        versionCode = (providers.gradleProperty("versionCode").orNull ?: "43").toInt()
        versionName = providers.gradleProperty("versionName").orNull ?: "1.11.4"
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

        // Test key: committed, and can only sign com.aistra.hail.pr.<n> and
        // com.aistra.hail.debug, so it grants nothing. Living in the repo is
        // what makes every PR and debug build carry the same signature, so a
        // rebuild installs over its own previous build instead of demanding an
        // uninstall first. PKCS12 protects the key with the store password,
        // so keyPassword must match storePassword.
        create("test") {
            val keystore = rootProject.file(".github/debug.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storeType = "PKCS12"
                storePassword = "HailBug"
                keyAlias = "HailBug"
                keyPassword = "HailBug"
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-Debug"
            signingConfig = signingConfigs.getByName("test")
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
            signingConfig = signingConfigs.getByName("test")
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
            it.jvmArgs(
                "-Dnet.bytebuddy.experimental=true",
                // Robolectric's FileDescriptorInterceptor reaches into
                // jdk.internal.access.SharedSecrets, which JDK 27 no longer
                // exports to the unnamed module, so it throws
                // "Failed to interact with raw FileDescriptor internals".
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                // Robolectric loads librobolectric-nativeruntime with System.load,
                // which JEP 472 restricts from JDK 24 on: the JVM runs it but warns,
                // and promises to block it once native access stops being implied.
                "--enable-native-access=ALL-UNNAMED",
            )
        }
        // Robolectric loads real resources and a real Android runtime, so the
        // merged resources and manifest have to be handed to the unit test JVM
        // instead of being stubbed out.
        unitTests.isIncludeAndroidResources = true
    }
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(27)
    }
}
kotlin {
    jvmToolchain(27)
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
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
    testImplementation(libs.robolectric)
    testImplementation("org.json:json:20260814")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("androidx.room3:room3-testing:3.0.3")
}
