#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  echo "Usage: prepare-release.sh <version>" >&2
  exit 2
fi

version=$1
run_number=${GITHUB_RUN_NUMBER:?GITHUB_RUN_NUMBER must be set}

case "$run_number" in
  ''|*[!0-9]*|0)
    echo "GITHUB_RUN_NUMBER must be a positive integer" >&2
    exit 1
    ;;
esac

if (( ${#run_number} > 10 || run_number > 1100000000 )); then
  echo "GITHUB_RUN_NUMBER exceeds Android's maximum versionCode" >&2
  exit 1
fi

version_code=$((1000000000 + run_number))
properties_tmp=$(mktemp)
trap 'rm -f "$properties_tmp"' EXIT
awk '!/^[[:space:]]*(version|versionCode)[[:space:]]*=/' gradle.properties > "$properties_tmp"
printf 'version = %s\nversionCode = %s\n' "$version" "$version_code" >> "$properties_tmp"
mv "$properties_tmp" gradle.properties
trap - EXIT

./gradlew test detekt assembleRelease assembleDebug --no-daemon --parallel
