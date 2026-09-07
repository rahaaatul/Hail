#!/usr/bin/env bash
#
# pr.sh — build a PR debug APK and print its path to stdout.
#
# The output is renamed to Hail-<PR Number>-pr.apk so downstream
# scripts (zip.sh, upload.sh) can identify it by filename and route
# the Telegram upload to the PR topic.
#
# Usage: bash pr.sh
# Output: absolute path to the built APK (one line)
#
# Environment:
#   PR_NUMBER — pull request number (e.g. 19); the versionNameSuffix and
#               the APK filename both derive from it. Required.
#
# The build runs the `pr` build type (com.aistra.hail.pr), which is
# unsigned like debug.

set -euo pipefail

cd "$(dirname "$0")/../.."

if [[ -z "${PR_NUMBER:-}" ]]; then
  echo "::error::pr.sh: PR_NUMBER env var is required"
  exit 1
fi

echo "==> Building PR #${PR_NUMBER} APK"
chmod +x ./gradlew
./gradlew :app:assemblePr --no-daemon --parallel --stacktrace

apk_path="$(find app/build/outputs/apk/pr -maxdepth 1 -type f -name '*.apk' | head -1)"
if [[ -z "${apk_path}" ]]; then
  echo "::error::pr.sh: No PR APK found in app/build/outputs/apk/pr"
  exit 1
fi

dest="app/build/outputs/apk/pr/Hail-${PR_NUMBER}-pr.apk"
if [[ "${apk_path}" != "${dest}" ]]; then
  cp "${apk_path}" "${dest}"
  rm -f "${apk_path}"
fi

echo "${dest}"