---
name: android-build-setup
description: Install and configure all build dependencies for the Hail Android project (JDK 26, Android SDK, Gradle wrapper) and avoid common setup pitfalls. Use when the user asks to set up a build environment, install dependencies, or troubleshoot compilation.
---

# Android Build Environment Setup for Hail

This skill documents how to install and configure all build dependencies for the Hail Android project from a clean environment. It covers the JDK, Android SDK, Gradle, and environment variables, plus the specific pitfalls encountered on Ubuntu 26.04.

## Prerequisites

- Ubuntu 26.04 (or similar Debian-based Linux)
- `sudo` access
- Internet connectivity

## Required Versions

The Hail project (`app/build.gradle.kts`) requires specific versions:

| Component | Version | Source |
|---|---|---|
| JDK | 26 | `openjdk-26-jdk` via apt (Ubuntu 26.04), or toolchain download |
| Gradle | 9.7.1 | Automatic via the Gradle wrapper (`./gradlew`) |
| Kotlin | 2.4.10 | Automatic via AGP 9.3.2 |
| Android Gradle Plugin | 9.3.2 | Automatic via Gradle plugin |
| KSP | 2.3.10 | Automatic via Gradle plugin |
| compileSdk | 37 | Android SDK platform package |
| minSdk | 23 | For reference only |
| targetSdk | 36 | For reference only |

The project uses Kotlin toolchain auto-provisioning: `jvmToolchain(26)` in `app/build.gradle.kts` means Gradle will download a JDK 26 automatically if none is found. However, it is simpler to install one system-wide.

## Pitfalls and Hiccups

### 1. JDK version must match the toolchain spec

The `app/build.gradle.kts` file pins Java 26:

```kotlin
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(26)
    }
}
kotlin {
    jvmToolchain(26)
}
```

Installing JDK 17 or 21 (common defaults) will cause Gradle to either auto-download JDK 26 or fail. To avoid surprises, install `openjdk-26-jdk` explicitly:

```bash
sudo apt-get install -y openjdk-26-jdk
```

**Symptom of mismatch:** `No matching toolchain found` or `Could not determine Java toolchains` errors from Gradle.

### 2. Android platform 37 is versioned as `android-37.0`, not `android-37`

The Android SDK repository changed its naming scheme for API 37+. The package `platforms;android-37` does **not** exist. You must use:

```bash
sdkmanager "platforms;android-37.0"
```

**Symptom of wrong name:** `Warning: Failed to find package 'platforms;android-37'` and a build failure with `Failed to find platform 37`.

### 3. Build-tools version 37.0.0 corresponds to platform 37

```bash
sdkmanager "build-tools;37.0.0"
```

### 4. `local.properties` must point to the SDK

Gradle needs a `local.properties` file at the project root (it is gitignored). Create it:

```bash
echo "sdk.dir=/opt/android-sdk" > /workspaces/Hail/local.properties
```

**Symptom of absence:** `Failed to install the following Android SDK packages as 'android' user:` or `SDK location not found`.

### 5. Environment variables must be set

Set `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` before running Gradle:

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.26.0-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
```

Add these to `~/.bashrc` or a sourced profile file for persistence across shell sessions.

**Symptom of absence:** `java: command not found`, `sdkmanager: command not found`, or Gradle cannot find the Android SDK.

### 6. Accept all SDK licenses

Before installing SDK packages:

```bash
yes | sdkmanager --licenses
```

**Symptom of not doing this:** Build fails with `You have not accepted the license of the following SDK package(s)`.

### 7. The Gradle wrapper must be executable

The `gradlew` script in the repo root must have execute permissions:

```bash
chmod +x /workspaces/Hail/gradlew
```

**Symptom:** `./gradlew: Permission denied`

## Full Setup Procedure

### Step 1: Install JDK 26

```bash
sudo apt-get update
sudo apt-get install -y openjdk-26-jdk-headless
```

Verify:

```bash
java -version
# Expected: openjdk version "26.0.1" ...
```

### Step 2: Download and install Android SDK command-line tools

```bash
mkdir -p /tmp/android-sdk/cmdline-tools/latest
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q -o cmdline-tools.zip -d /tmp/cmdline-temp
cp -r /tmp/cmdline-temp/cmdline-tools/* /tmp/android-sdk/cmdline-tools/latest/
rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-temp
sudo mv /tmp/android-sdk /opt/android-sdk
sudo chown -R $(whoami):$(whoami) /opt/android-sdk
```

### Step 3: Accept licenses and install SDK packages

```bash
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

### Step 4: Configure environment

Create `/home/$(whoami)/.android_env`:

```bash
cat > ~/.android_env << 'EOF'
export JAVA_HOME=$(dirname $(dirname $(readlink -f $(which java))))
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
EOF
```

Add to `~/.bashrc`:

```bash
grep -q "source ~/.android_env" ~/.bashrc || echo 'source ~/.android_env' >> ~/.bashrc
```

### Step 5: Create local.properties

```bash
echo "sdk.dir=/opt/android-sdk" > /workspaces/Hail/local.properties
```

### Step 6: Make gradlew executable and build

```bash
chmod +x /workspaces/Hail/gradlew
cd /workspaces/Hail
source ~/.android_env
./gradlew :app:assembleDebug --no-daemon
```

## Verification

After setup, run the Kotlin compilation check:

```bash
./gradlew :app:compileDebugKotlin
```

Then build the debug APK:

```bash
./gradlew :app:assembleDebug
```

If both succeed, the environment is fully functional. The APK will be output to `app/build/outputs/apk/debug/`.

## Idempotence and Recovery

All steps are safe to repeat:

- `apt-get install` is idempotent.
- `sdkmanager` skips already-installed packages.
- `local.properties` is gitignored and safe to overwrite.
- The Gradle wrapper downloads the correct Gradle distribution (9.7.1) automatically on first run; subsequent runs use the cached copy.

If the build fails with toolchain errors, check `JAVA_HOME`:

```bash
echo $JAVA_HOME
# Should point to /usr/lib/jvm/java-1.26.0-openjdk-amd64
```

If Gradle complains about the Android SDK, verify `local.properties` and `ANDROID_HOME`:

```bash
cat /workspaces/Hail/local.properties
ls /opt/android-sdk/platforms/
```

## Summary of Hiccups

1. JDK 26 is not the default on most systems — install `openjdk-26-jdk` explicitly.
2. Android SDK platform 37 is named `android-37.0`, not `android-37`.
3. `local.properties` is not committed (gitignored) — must be created locally.
4. Environment variables (`JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT`) must be exported in the shell before running Gradle.
5. SDK licenses must be accepted before installing packages.
6. `gradlew` may lack execute permissions on fresh checkouts.
7. Kotlin and Gradle versions are managed automatically by the Gradle wrapper and AGP — no separate installation needed.
