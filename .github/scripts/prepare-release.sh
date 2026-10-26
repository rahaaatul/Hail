#!/usr/bin/env bash
#
# prepare-release.sh — set the version name and the version code for a release.
#
# Usage: bash prepare-release.sh <version>
#
# The version is the one the release tooling computed, which is a plain
# version for a release and a version with a prerelease identifier for a
# prerelease, for example 1.12.0 and 1.12.0-dev.3.
#
# The version name of a prerelease is the version it leads to followed by the
# number of the prerelease, so the development branch produces 1.12.0.1,
# 1.12.0.2 and so on, while the release itself stays 1.12.0. The tag keeps
# the identifier, so the version of a prerelease is still unambiguous in the
# tag list.
#
# The version code moves on every release, prereleases included, so that
# each build can be installed over the one before it. The counter lives in
# versionCode.properties, which is committed with the release, and the
# ceiling that refuses a version code above the maximum Android accepts
# stops the release.
#
# The properties file is rewritten with the new version and version code,
# and the settings the build action applies to the build are stripped from
# it, so the file in the repository stays what it was.

set -euo pipefail

VERSION="${1:?Usage: prepare-release.sh <version>}"

cd "$(dirname "$0")/../.."

# 1.12.0-dev.3 -> 1.12.0.3, and 1.12.0 stays 1.12.0.
VERSION_NAME="$(printf '%s' "$VERSION" | sed -E 's/^([0-9]+(\.[0-9]+)*)-.*\.([0-9]+)$/\1.\3/')"

CURRENT_VERSION_CODE="$(grep -oE 'VERSION_CODE=[0-9]+' versionCode.properties | cut -d'=' -f2)"
NEW_VERSION_CODE=$((CURRENT_VERSION_CODE + 1))

if [ "$NEW_VERSION_CODE" -gt 2100000000 ]; then
  echo "versionCode=$NEW_VERSION_CODE exceeds the maximum Android accepts" >&2
  exit 1
fi

echo "VERSION_CODE=$NEW_VERSION_CODE" > versionCode.properties

printf 'version = %s\n' "$VERSION_NAME" > gradle.properties.tmp
printf 'versionCode = %s\n' "$NEW_VERSION_CODE" >> gradle.properties.tmp
sed '/^# --- applied by the setup-java-gradle action/,$d' gradle.properties \
  | grep -vE '^(version|versionCode)[[:space:]]*=' >> gradle.properties.tmp
mv gradle.properties.tmp gradle.properties

echo "version $VERSION_NAME, versionCode $NEW_VERSION_CODE"
