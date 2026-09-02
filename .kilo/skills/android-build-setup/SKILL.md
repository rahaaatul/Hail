---
name: android-build-setup
description: Use when setting up a build environment, installing dependencies, or troubleshooting compilation for the Hail Android project. Symptoms include toolchain errors, missing SDK packages, build-tools version mismatches, platform naming issues, and gradlew permission failures.
---

# Android Build Environment Setup for Hail

## Overview
This skill documents the verified setup procedure for building the Hail Android project from a clean environment, covering JDK 26, Android SDK 37, Gradle wrapper, and known pitfalls on Ubuntu 26.04.

## When to Use
- Fresh checkout fails with `No matching toolchain found` or `Could not determine Java toolchains`
- Build fails with `Failed to find platform 37`
- Build fails with `SDK location not found` or `You have not accepted the license`
- Build fails with `./gradlew: Permission denied`
- `sdkmanager` reports `Failed to find package 'platforms;android-37'`

## Quick Reference

| Component | Required | Verified Path / Command |
|---|---|---|
| JDK | 26 | `/usr/lib/jvm/java-1.26.0-openjdk-amd64` |
| Gradle | 9.7.1 | Via `./gradlew` wrapper |
| Kotlin | 2.4.10 | Via AGP / Kotlin toolchain |
| AGP | 9.3.2 | Automatic |
| compileSdk | 37 | `platforms;android-37.0` |
| build-tools | 37.0.0 | `build-tools;37.0.0` |
| SDK location | `/opt/android-sdk` | `local.properties` + env vars |
| Env file | `~/.android_env` | Source in `~/.bashrc` |

## Prerequisites

- Ubuntu 26.04 (or similar Debian-based Linux)
- `sudo` access
- Internet connectivity

## Setup Procedure

### Step 1: Install JDK 26

```bash
sudo apt-get update
sudo apt-get install -y openjdk-26-jdk-headless
```

Verify:

```bash
java -version
# Expected: openjdk version "26.0.x"
```

### Step 2: Install Android SDK command-line tools

```bash
sudo mkdir -p /opt/android-sdk/cmdline-tools/latest
sudo chown -R $(whoami):$(whoami) /opt/android-sdk
cd /tmp
wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O cmdline-tools.zip
unzip -q -o cmdline-tools.zip -d /tmp/cmdline-temp
cp -r /tmp/cmdline-temp/cmdline-tools/* /opt/android-sdk/cmdline-tools/latest/
rm -rf /tmp/cmdline-tools.zip /tmp/cmdline-temp
```

### Step 3: Configure environment variables

Create `~/.android_env`:

```bash
cat > ~/.android_env << 'EOF'
export JAVA_HOME=/usr/lib/jvm/java-1.26.0-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH
EOF
grep -q "source ~/.android_env" ~/.bashrc || echo 'source ~/.android_env' >> ~/.bashrc
source ~/.android_env
```

### Step 4: Accept licenses and install SDK packages

```bash
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;37.0.0"
```

### Step 5: Configure project-local settings

Create `/workspaces/Hail/local.properties`:

```bash
echo "sdk.dir=/opt/android-sdk" > /workspaces/Hail/local.properties
```

Ensure gradlew is executable:

```bash
chmod +x /workspaces/Hail/gradlew
```

## Verification

Run Kotlin compilation:

```bash
cd /workspaces/Hail && source ~/.android_env
./gradlew :app:compileDebugKotlin
```

Build debug APK:

```bash
./gradlew :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/`

## Common Mistakes

| Symptom | Cause | Fix |
|---|---|---|
| `No matching toolchain found` | JDK 17/21 installed instead of 26 | Install `openjdk-26-jdk` |
| `Failed to find package 'platforms;android-37'` | Wrong SDK platform name | Use `platforms;android-37.0` |
| `SDK location not found` | Missing `local.properties` | Create with `sdk.dir=/opt/android-sdk` |
| `Permission denied` on `./gradlew` | Missing execute bit | `chmod +x gradlew` |
| `You have not accepted the license` | Licenses not accepted | `yes \| sdkmanager --licenses` |
| `sdkmanager: command not found` | Env vars not exported | Source `~/.android_env` |

## Recovery

All steps are idempotent:
- `apt-get install` skips existing packages
- `sdkmanager` skips existing packages
- `local.properties` is gitignored and safe to overwrite
- Gradle wrapper caches Gradle 9.7.1 after first run

If build fails, verify in order:

```bash
echo $JAVA_HOME
# Should be /usr/lib/jvm/java-1.26.0-openjdk-amd64

cat /workspaces/Hail/local.properties
# Should contain sdk.dir=/opt/android-sdk

ls /opt/android-sdk/platforms/
# Should contain android-37.0

ls /opt/android-sdk/build-tools/
# Should contain 37.0.0
```

## Summary of Verified Pitfalls

1. JDK 26 is not the default — install `openjdk-26-jdk` explicitly
2. Android SDK platform 37 uses the name `android-37.0`, not `android-37`
3. `local.properties` is gitignored — must be created locally
4. `JAVA_HOME`, `ANDROID_HOME`, and `ANDROID_SDK_ROOT` must be exported
5. SDK licenses must be accepted before installing packages
6. `gradlew` requires execute permissions on fresh checkouts
7. Kotlin and Gradle versions are managed automatically by the Gradle wrapper and AGP
