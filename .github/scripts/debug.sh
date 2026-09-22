#!/usr/bin/env bash
#
# debug.sh — build a debug APK and print its path to stdout.
#
# The output is renamed to Hail-<short-commit>-debug.apk so downstream
# scripts (zip.sh, upload.sh) can identify it by filename.
#
# Usage: bash debug.sh
# Output: absolute path to the built APK (one line)

set -euo pipefail

cd "$(dirname "$0")/../.."

echo "==> Building debug APK"
chmod +x ./gradlew
./gradlew :app:assembleDebug --no-daemon --parallel --stacktrace

apk_path="$(find app/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' | head -1)"
if [[ -z "${apk_path}" ]]; then
  echo "::error::No debug APK found in app/build/outputs/apk/debug"
  exit 1
fi

commit_hash="$(git rev-parse --short HEAD)"
dest="app/build/outputs/apk/debug/Hail-${commit_hash}-debug.apk"
if [[ "${apk_path}" != "${dest}" ]]; then
  cp "${apk_path}" "${dest}"
  rm -f "${apk_path}"
fi

echo "${dest}"