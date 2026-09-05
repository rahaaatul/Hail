#!/usr/bin/env bash
#
# setup.sh — install the toolchain needed to build APKs.
# Idempotent: safe to call repeatedly.
#
# Installs:
#   - JDK 26 (Temurin)
#   - Android SDK cmdline-tools + platform-tools + platform android-36
#   - 7z (p7zip-full) for APK compression
#
# The three independent installs (apt packages, JDK download,
# cmdline-tools download) run in parallel to save the time of one full
# download. Each is isolated to its own temp file so failures don't
# clobber each other.
#
# Usage: bash setup.sh
# Exits 0 on success, 1 on failure.

set -euo pipefail

readonly JAVA_VERSION="26"
readonly SDK_PLATFORM="android-36"
readonly SDK_DIR="${ANDROID_HOME:-${HOME}/.android/sdk}"
readonly CMDLINE_TOOLS="${SDK_DIR}/cmdline-tools/latest"

case "$(uname -m)" in
  x86_64|amd64)  ARCH="x64" ;;
  aarch64|arm64) ARCH="aarch64" ;;
  *)             ARCH="x64" ;;
esac

echo "==> Architecture: ${ARCH}"
echo "==> Installing toolchain (apt, JDK, cmdline-tools in parallel)"

# --- Parallel installs ------------------------------------------------------

# 1. System packages (7z for compression)
apt_pid=""
if command -v apt-get >/dev/null 2>&1; then
  ( sudo apt-get update -qq && sudo apt-get install -y -qq p7zip-full unzip zip ) \
    >/tmp/setup_apt.log 2>&1 &
  apt_pid=$!
fi

# 2. JDK 26 — always install: the runner image pre-sets JAVA_HOME to a
#    different JDK (e.g. 17), so gating on it would silently skip the install.
jdk_pid=""
curl -sSL -o /tmp/jdk.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/${JAVA_VERSION}/ga/linux/${ARCH}/jdk/hotspot/normal/eclipse?project=jdk" \
  >/tmp/setup_jdk.log 2>&1 &
jdk_pid=$!

# 3. Android SDK cmdline-tools (14742923 is the current build; 11066011
#    returns 404)
cmd_pid=""
if [[ ! -x "${CMDLINE_TOOLS}/bin/sdkmanager" ]]; then
  curl -sSL -o /tmp/cmdline-tools.zip \
    "https://dl.google.com/android/repository/commandlinetools-linux-14742923_latest.zip" \
    >/tmp/setup_cmd.log 2>&1 &
  cmd_pid=$!
fi

# --- Wait for downloads -----------------------------------------------------

if [[ -n "${jdk_pid}" ]]; then
  wait "${jdk_pid}" || { echo "::error::JDK download failed"; cat /tmp/setup_jdk.log; exit 1; }
fi
if [[ -n "${cmd_pid}" ]]; then
  wait "${cmd_pid}" || { echo "::error::cmdline-tools download failed"; cat /tmp/setup_cmd.log; exit 1; }
fi
if [[ -n "${apt_pid}" ]]; then
  wait "${apt_pid}" || { echo "::error::apt install failed"; cat /tmp/setup_apt.log; exit 1; }
fi

# --- Extract and configure --------------------------------------------------

mkdir -p "${HOME}/.local"
tar -xzf /tmp/jdk.tar.gz -C "${HOME}/.local"
# Glob instead of find|head: with set -o pipefail the pipe would fail
# when head closes early, silently emptying JAVA_HOME.
shopt -s nullglob
candidates=("${HOME}/.local/jdk-${JAVA_VERSION}"*)
shopt -u nullglob
JAVA_HOME="${candidates[0]}"
export JAVA_HOME
if [[ -z "${JAVA_HOME}" || ! -x "${JAVA_HOME}/bin/java" ]]; then
  echo "::error::setup.sh: JDK installation failed - no java binary found"
  exit 1
fi
echo "==> JAVA_HOME=${JAVA_HOME}"
"${JAVA_HOME}/bin/java" -version

# Persist to the workflow environment so subsequent steps (build, gradlew)
# inherit the JDK 26 toolchain. Each run: step is a fresh shell, so an
# export here would not survive to the next step. No-op when run locally.
if [[ -n "${GITHUB_ENV:-}" ]]; then
  echo "JAVA_HOME=${JAVA_HOME}" >> "${GITHUB_ENV}"
  echo "ANDROID_SDK_ROOT=${SDK_DIR}" >> "${GITHUB_ENV}"
fi

echo "==> Installing Android SDK (platform ${SDK_PLATFORM})"

mkdir -p "${SDK_DIR}"
export ANDROID_SDK_ROOT="${SDK_DIR}"

if [[ -x "${CMDLINE_TOOLS}/bin/sdkmanager" ]]; then
  echo "==> cmdline-tools already installed, skipping"
elif ! unzip -t -q /tmp/cmdline-tools.zip >/dev/null 2>&1; then
  echo "::error::setup.sh: downloaded cmdline-tools archive is not a valid zip"
  exit 1
else
  mkdir -p "${SDK_DIR}/cmdline-tools"
  unzip -q -o /tmp/cmdline-tools.zip -d "${SDK_DIR}/cmdline-tools"
  mv "${SDK_DIR}/cmdline-tools/cmdline-tools" "${CMDLINE_TOOLS}"
fi

# Accept licenses; ignore the broken-pipe warning from `yes` closing early.
yes | "${CMDLINE_TOOLS}/bin/sdkmanager" --sdk_root="${SDK_DIR}" --licenses >/dev/null 2>&1 || true
"${CMDLINE_TOOLS}/bin/sdkmanager" --sdk_root="${SDK_DIR}" --install \
  "platform-tools" "platforms;${SDK_PLATFORM}" >/dev/null

echo "==> Toolchain ready"