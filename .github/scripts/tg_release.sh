#!/usr/bin/env bash
#
# release.sh — build a signed release APK and send it to Telegram.
#
# Local convenience wrapper. Uses the same modular scripts as CI so the
# local and CI paths are identical.
#
# Usage: bash release.sh [release|pre-release]
#
# Environment:
#   TG_TOKEN                — bot token (required to send)
#   TG_GROUP                — chat id (required to send)
#   KEYSTORE                — base64-encoded keystore.jks
#   KEYSTORE_PASSWORD       — keystore password
#   KEYSTORE_ALIAS          — key alias
#   KEYSTORE_ALIAS_PASSWORD — key password

set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
RELEASE_TYPE="${1:-release}"
export RELEASE_TYPE

echo "==> Setting up toolchain"
bash "${script_dir}/setup.sh"

echo "==> Building release APK (${RELEASE_TYPE})"
apk_path="$(bash "${script_dir}/release.sh")"

echo "==> Compressing"
zip_path="$(bash "${script_dir}/zip.sh" "${apk_path}" | tail -1)"

echo "==> Uploading to Telegram"
bash "${script_dir}/upload.sh" "${zip_path}"