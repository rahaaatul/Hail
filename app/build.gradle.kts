plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.detekt)
    alias(libs.plugins.protobuf)
}

android {
    val signingProps = file("../signing.properties")
    val commitHash = providers.exec {
        workingDir = rootDir
        commandLine = "git rev-parse --short HEAD".split(" ")
    }.standardOutput.asText.get().trim()
    val commitSubject = providers.exec {
        workingDir = rootDir
        commandLine = "git log -1 --pretty=%s".split(" ")
    }.standardOutput.asText.get().trim()

    namespace = "com.aistra.hail"
    compileSdk = 37

    val versionCodeProperties = rootProject.file("versionCode.properties")
    val versionCodeFromProperties = versionCodeProperties.readText().lines()
        .firstOrNull { it.startsWith("VERSION_CODE=") }
        ?.removePrefix("VERSION_CODE=")
        ?.toInt()

    defaultConfig {
        applicationId = "com.aistra.hail"
        minSdk = 24
        targetSdk = 36
        versionCode = versionCodeFromProperties ?: 43
        versionName = project.findProperty("version") as String? ?: "1.11.4"
        ndk {
            val abi = project.findProperty("abi") as String?
            if (abi != null) abiFilters += abi
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-g$commitHash"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = if (signingProps.exists()) {
                val props = `java.util`.Properties().apply { load(signingProps.reader()) }
                signingConfigs.create("release") {
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            } else signingConfigs.getByName("debug")
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
            it.jvmArgs("-Dnet.bytebuddy.experimental=true")
        }
    }
    androidComponents {
        onVariants { variant ->
            val buildTypeName = variant.buildType
            val appName = (rootProject.findProperty("appName") as String?)?.lowercase()
            if (buildTypeName != null && appName != null) {
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
}
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
kotlin {
    jvmToolchain(21)
}

// Force patched versions of transitive dependencies flagged by Dependabot.
// These are pulled in by Gradle plugins (KSP, Detekt, Spotless, Compose) and
// are not declared directly. Forcing them here covers every configuration so
// the vulnerable versions are excluded from the resolved dependency graph.
configurations.all {
    resolutionStrategy.force("org.freemarker:freemarker:2.3.35")
    resolutionStrategy.force("org.bouncycastle:bcprov-jdk18on:1.86")
    resolutionStrategy.force("org.bouncycastle:bcpkix-jdk18on:1.86")
    resolutionStrategy.force("org.apache.commons:commons-lang3:3.21.0")
    resolutionStrategy.force("org.apache.httpcomponents:httpclient:4.5.14")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
    }
}

detekt {
    config.from(rootProject.file("config/detekt.yml"))
    baseline = rootProject.file("config/detekt-baseline.xml")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.32.1"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
                create("kotlin")
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
    implementation("androidx.biometric:biometric:1.4.0-alpha07")
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.work.runtime)
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.hiddenapibypass)
    implementation(libs.libsu.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.sqlite.wrapper)
    implementation(libs.androidx.sqlite)
    ksp(libs.androidx.room.compiler)
    implementation(libs.protobuf.kotlin.lite)
    implementation(libs.protobuf.java.lite)
    implementation(libs.androidx.datastore)
    compileOnly(libs.libxposed.api)

    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.ext:truth:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    testImplementation("androidx.room3:room3-testing:3.0.1")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation(libs.robolectric)

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.4.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
    androidTestImplementation("io.mockk:mockk-android:1.14.11")
    androidTestImplementation("androidx.room3:room3-testing:3.0.1")
}

