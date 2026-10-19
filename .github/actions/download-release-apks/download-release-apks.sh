#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN must be set}"
: "${RELEASE_TAG:?RELEASE_TAG must be set}"
: "${GITHUB_OUTPUT:?GITHUB_OUTPUT must be set}"
: "${GITHUB_REPOSITORY:?GITHUB_REPOSITORY must be set}"

if [[ ! "$RELEASE_TAG" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]]; then
  echo "Invalid release tag: $RELEASE_TAG" >&2
  exit 1
fi

download_directory="$RUNNER_TEMP/release-apks"
mkdir -p "$download_directory"
gh release download "$RELEASE_TAG" \
  --repo "$GITHUB_REPOSITORY" \
  --pattern '*-release.apk' \
  --pattern '*-debug.apk' \
  --dir "$download_directory"

release_apk=$(find "$download_directory" -type f -name '*-release.apk' -print -quit)
debug_apk=$(find "$download_directory" -type f -name '*-debug.apk' -print -quit)

if [[ -z "$release_apk" || -z "$debug_apk" ]]; then
  echo "Release $RELEASE_TAG does not contain both release and debug APKs" >&2
  exit 1
fi

{
  echo "releaseApkPath=$release_apk"
  echo "debugApkPath=$debug_apk"
} >> "$GITHUB_OUTPUT"
