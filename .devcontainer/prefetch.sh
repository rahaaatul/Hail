#!/usr/bin/env bash
#
# Runs as updateContentCommand: executed at prebuild creation and every
# subsequent prebuild content refresh, so it re-runs whenever gradle files
# or source change. Warms the Gradle/Maven dependency cache and compiles
# the debug variant so a live codespace only needs an incremental build.
#
# assembleRelease is intentionally NOT run here - release builds require
# the KEYSTORE_* signing secrets, which aren't available in Codespaces
# (only in the release.yml Actions workflow), and local dev never builds
# release locally anyway.

set -euo pipefail

readonly LOCAL_TOOLS_DIR="${HOME}/.local"
export JAVA_HOME="${JAVA_HOME:-${LOCAL_TOOLS_DIR}/jdk-26}"
export ANDROID_HOME="${ANDROID_HOME:-${LOCAL_TOOLS_DIR}/android-sdk}"
export ANDROID_SDK_ROOT="${ANDROID_HOME}"
export PATH="${JAVA_HOME}/bin:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools:${PATH}"

chmod +x ./gradlew
./gradlew assembleDebug --console=plain
