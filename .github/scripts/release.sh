#!/usr/bin/env bash
#
# release.sh — build a signed release APK and print its path to stdout.
#
# Usage: bash release.sh
# Output: absolute path to the built APK (one line)
#
# Environment (all required; the build fails closed if any is missing):
#   KEYSTORE                — base64-encoded keystore.jks (decoded into a temp dir)
#   KEYSTORE_PASSWORD       — keystore password
#   KEYSTORE_ALIAS          — key alias
#   KEYSTORE_ALIAS_PASSWORD — key password
#   RELEASE_TYPE            — "release" or "pre-release" (affects filename only)
#
# The keystore and signing.properties are created in a temporary directory and
# cleaned up on exit; a copy of signing.properties is placed at the repository
# root (mode 600) so Gradle can pick it up, and removed again on exit.

set -euo pipefail

cd "$(dirname "$0")/../.."

RELEASE_TYPE="${RELEASE_TYPE:-release}"

# --- Signing setup (ephemeral, cleaned up on exit) ----------------------------

# Fail closed: every signing variable is required.
missing=()
for var in KEYSTORE KEYSTORE_PASSWORD KEYSTORE_ALIAS KEYSTORE_ALIAS_PASSWORD; do
  [[ -n "${!var:-}" ]] || missing+=("$var")
done
if (( ${#missing[@]} > 0 )); then
  echo "::error::Missing signing environment variable(s): ${missing[*]}"
  exit 1
fi

tmpdir="$(mktemp -d)"
keystore="$tmpdir/keystore.jks"
signing_props="$tmpdir/signing.properties"
trap 'rm -f "./signing.properties" "$keystore" "$signing_props"; rm -rf "$tmpdir"' EXIT

echo "${KEYSTORE}" | base64 --decode > "$keystore"
printf 'storeFile=%s\nstorePassword=%s\nkeyAlias=%s\nkeyPassword=%s\n' \
  "$keystore" "${KEYSTORE_PASSWORD}" "${KEYSTORE_ALIAS}" "${KEYSTORE_ALIAS_PASSWORD}" \
  > "$signing_props"
# Gradle reads signing.properties from the project root; keep it at 0600.
cp "$signing_props" ./signing.properties
chmod 600 ./signing.properties

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

