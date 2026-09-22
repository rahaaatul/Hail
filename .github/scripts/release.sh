#!/usr/bin/env bash
#
# release.sh — build a signed release APK and print its path to stdout.
#
# Usage: bash release.sh
# Output: absolute path to the built APK (one line)
#
# Environment:
#   KEYSTORE                — base64-encoded keystore.jks (decoded to ./keystore.jks)
#   KEYSTORE_PASSWORD       — keystore password
#   KEYSTORE_ALIAS          — key alias
#   KEYSTORE_ALIAS_PASSWORD — key password
#   RELEASE_TYPE            — "release" or "pre-release" (affects filename only)
#
# Signing properties and keystore are written to a temporary directory
# and cleaned up on exit. The build fails closed if no signing material exists.

set -euo pipefail

cd "$(dirname "$0")/../.."

RELEASE_TYPE="${RELEASE_TYPE:-release}"

# --- Signing setup (ephemeral, cleaned up on exit) ----------------------------

tmpdir="$(mktemp -d)"
trap 'rm -f "$tmpdir/keystore.jks" "$tmpdir/signing.properties"; rm -rf "$tmpdir"' EXIT

if [[ -n "${KEYSTORE:-}" ]]; then
  echo "${KEYSTORE}" | base64 --decode > "$tmpdir/keystore.jks"
  printf 'storeFile=%s/keystore.jks\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' \
    "$tmpdir" "${KEYSTORE_PASSWORD}" "${KEYSTORE_ALIAS}" "${KEYSTORE_ALIAS_PASSWORD}" \
    > "$tmpdir/signing.properties"
elif [[ ! -f "$tmpdir/signing.properties" ]]; then
  echo "::error::No signing material: set KEYSTORE env or provide signing.properties"
  exit 1
fi

# --- Build ------------------------------------------------------------------

echo "==> Building release APK (${RELEASE_TYPE})"
chmod +x ./gradlew
./gradlew assembleRelease --no-daemon --parallel --stacktrace

apk_path="$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' | head -1)"
if [[ -z "${apk_path}" ]]; then
  echo "::error::No release APK found in app/build/outputs/apk/release"
  exit 1
fi

# Rename to a descriptive filename for downstream scripts
version_name="$(sed -n 's/.*versionName\s*=\s*"\([^"]*\)".*/\1/p' app/build.gradle.kts | head -1)"
dest="app/build/outputs/apk/release/Hail-v${version_name}-${RELEASE_TYPE}.apk"
cp "${apk_path}" "${dest}"
rm -f "${apk_path}"

echo "${dest}"
