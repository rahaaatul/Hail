#!/usr/bin/env bash
#
# setup.sh — install the toolchain needed to build APKs.
# Idempotent: safe to call repeatedly.
#
# Installs:
#   - JDK 26 (Temurin)
#   - Android SDK cmdline-tools + platform-tools + platform android-37.0
#   - 7z (p7zip-full) for APK compression
#
# The three independent installs (apt packages, JDK download,
# cmdline-tools download) run in parallel to save the time of one full
# download. Each is isolated to its own temp file so failures don't
# clobber each other.
#
# Usage: bash setup.sh
# Exits 0 on success, 1 on failure.
#
# Requires: Linux — hard requirement, the script exits 1 on any other OS.
# Also required, and checked separately because they are capabilities rather
# than an OS: apt-get and sudo, to install the system packages below.
# Beyond those this script uses curl, tar, yes, dirname, uname and mkdir, and
# installs only p7zip-full, unzip and zip. The rest are expected to be present
# on the runner; they are checked, not installed. Both guards below report
# which requirement was not met, rather than letting a missing binary surface
# later as an opaque download or extract failure.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
readonly SCRIPT_DIR

# --- Fail fast on a non-Linux runner ----------------------------------------
# Everything below is Linux-only, so bail out before downloading anything. The
# reasons are printed to the user here and deliberately not restated in a
# comment: two copies of the same three facts drift apart, and this one is the
# copy a macOS maintainer actually reads when the script refuses to run.
# uname is captured once and tolerates being absent, so a minimal container
# PATH gets the diagnostic instead of a bare exit 127.
os="$(uname -s 2>/dev/null || echo unknown)"
if [[ "${os}" != "Linux" ]]; then
  if [[ "${os}" == "unknown" ]]; then
    echo "::error::setup.sh: cannot determine the OS (uname unavailable) - this toolchain is Linux-only"
  else
    echo "::error::setup.sh: unsupported OS '${os}' - this toolchain is Linux-only"
  fi
  echo "setup.sh: Linux is required because:"
  echo "  * packages (p7zip 7z, unzip, zip) come from apt-get + sudo"
  echo "  * the JDK and cmdline-tools downloads are linux builds"
  echo "  * zip.sh needs GNU stat -c%s, which BSD stat rejects"
  exit 1
fi

# --- Fail fast on a missing package-manager capability ------------------------
# A Linux job is not automatically a Debian one. Without this, an image that
# has neither tool walks through the full JDK and cmdline-tools downloads and
# only fails when the install step cannot run. Distinct from the OS check
# above, and reported as such.
missing=""
command -v apt-get >/dev/null 2>&1 || missing="apt-get"
command -v sudo >/dev/null 2>&1 || missing="${missing:+$missing }sudo"
if [[ -n "${missing}" ]]; then
  echo "::error::setup.sh: unsupported environment - missing required command(s): ${missing}"
  echo "setup.sh: the OS is supported, but installing p7zip 7z, unzip and zip needs apt-get + sudo."
  exit 1
fi

# --- Fail fast on a command this script uses but does not install -------------
# The apt block below installs p7zip-full, unzip and zip, and nothing else.
# curl is not among them, and that has to be said rather than assumed: without
# it both downloads above fail, and the only clue a runner gives is
# `::error::JDK download failed`, which names neither the cause nor the tool.
# Checking costs one `command -v` per tool and turns an opaque download failure
# into a named missing prerequisite, before the first byte is fetched.
missing_used=""
for tool in curl tar yes dirname uname mkdir; do
  command -v "${tool}" >/dev/null 2>&1 || missing_used="${missing_used:+${missing_used} }${tool}"
done
if [[ -n "${missing_used}" ]]; then
  echo "::error::setup.sh: missing required command(s): ${missing_used}"
  echo "setup.sh: the apt block below installs p7zip-full, unzip and zip only; install the rest first."
  exit 1
fi

# --- Regression tests --------------------------------------------------------
# The caption escaper is the only thing between an attacker-supplied PR title
# and markup injection into the public channel, and its substitutions have
# been silent no-ops before. Run its tests here, ahead of the downloads: they
# need nothing but coreutils, and a caption regression should fail in seconds
# rather than after a few hundred MB of JDK.
echo "==> Running tg_body_test.sh"
if ! test_output="$(bash "${SCRIPT_DIR}/tg_body_test.sh" 2>&1)"; then
  printf '%s\n' "${test_output}"
  # The suite is not only about the escaper. It also asserts the upload.sh
  # wiring, that the tools its extractions are built from are on PATH, and that
  # tg_body.sh and upload.sh parse — none of which is the escaper regressing.
  # "the escaper regressed" was the wrong headline for all of those, and this
  # annotation is the only thing a reader has when the step fails, so it named a
  # diagnosis the output above may well contradict. Report the suite and the
  # first assertion that actually failed instead: every exit path in
  # tg_body_test.sh prints at least one `  FAIL ` line, so this distinguishes
  # an escaper regression from a wiring change or a broken harness.
  first_fail="$(grep -m1 '^  FAIL ' <<<"${test_output}" || true)"
  first_fail="${first_fail#  FAIL }"
  if [[ -z "${first_fail}" ]]; then
    first_fail='no FAIL line in the output above'
  fi
  echo "::error::setup.sh: tg_body_test.sh failed - ${first_fail}"
  exit 1
fi
printf '%s\n' "${test_output}"

readonly JAVA_VERSION="26"
readonly SDK_PLATFORM="android-37.0"
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

# 1. System packages (7z for compression). Unconditional: the capability guard
# above already proved apt-get and sudo exist, so a conditional here would only
# be a second place to forget the check.
apt_pid=""
( sudo apt-get update -qq && sudo apt-get install -y -qq p7zip-full unzip zip ) \
  >/tmp/setup_apt.log 2>&1 &
apt_pid=$!

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