#!/usr/bin/env bash
#
# debug.sh — build a debug APK and send it to the Telegram debug channel.
#
# Local convenience wrapper. Uses the same modular scripts as CI so the
# local and CI paths are identical.
#
# Usage: bash debug.sh
#
# Environment:
#   TG_TOKEN — bot token (required to send)
#   TG_GROUP — chat id (required to send)

set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"

echo "==> Setting up toolchain"
bash "${script_dir}/setup.sh"

echo "==> Building debug APK"
apk_path="$(bash "${script_dir}/debug.sh")"

echo "==> Compressing"
zip_path="$(bash "${script_dir}/zip.sh" "${apk_path}" | tail -1)"

echo "==> Uploading to Telegram"
bash "${script_dir}/upload.sh" "${zip_path}"