#!/usr/bin/env bash
set -euo pipefail

apk_directory="${GITHUB_WORKSPACE:?GITHUB_WORKSPACE must be set}/app/build/outputs/apk"
release_apk=$(find "$apk_directory" -type f -name '*-release.apk' -print -quit)
debug_apk=$(find "$apk_directory" -type f -name '*-debug.apk' -print -quit)

if [[ -z "$release_apk" || -z "$debug_apk" ]]; then
  echo "Could not find both release and debug APKs under $apk_directory" >&2
  exit 1
fi

{
  echo "releaseApkPath=$release_apk"
  echo "debugApkPath=$debug_apk"
} >> "${GITHUB_OUTPUT:?GITHUB_OUTPUT must be set}"
