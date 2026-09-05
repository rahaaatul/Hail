#!/usr/bin/env bash
#
# upload.sh — send a zipped APK to Telegram.
#
# Usage: bash upload.sh <zip_path>
#
# Channel is chosen from the filename:
#   *debug*      -> TOPIC_DEBUG     (84)
#   *release*    -> TOPIC_RELEASE   (85)
#   *pre-release*-> TOPIC_PRE_RELEASE (95)
#
# Required env:
#   TG_TOKEN  — bot token
#   TG_GROUP  — chat id
#
# Optional env:
#   TG_BODY   — caption HTML (sourced from tg_body.sh if unset)
#
# If TG_TOKEN is unset, prints what would be sent and exits 0 (dry run).
# Never fails the workflow: any error prints a warning and exits 0.

set -uo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "::warning::upload.sh: no zip path given, skipping"
  exit 0
fi

zip_path="$1"
if [[ ! -f "${zip_path}" ]]; then
  echo "::warning::upload.sh: file not found: ${zip_path}, skipping"
  exit 0
fi

# --- Route by filename -------------------------------------------------------

readonly TOPIC_DEBUG="84"
readonly TOPIC_RELEASE="85"
readonly TOPIC_PRE_RELEASE="95"

name="$(basename "${zip_path}")"
if [[ "${name}" == *pre-release* ]]; then
  topic="${TOPIC_PRE_RELEASE}"
  label="pre-release topic"
elif [[ "${name}" == *release* ]]; then
  topic="${TOPIC_RELEASE}"
  label="release topic"
elif [[ "${name}" == *debug* ]]; then
  topic="${TOPIC_DEBUG}"
  label="debug topic"
else
  topic=""
  label="default"
fi

echo "==> Uploading ${zip_path} to ${label} (chat=${TG_GROUP:-unset} topic=${topic:-none})"

# --- Caption ----------------------------------------------------------------

if [[ -z "${TG_BODY:-}" ]]; then
  script_dir="$(cd "$(dirname "$0")" && pwd)"
  # shellcheck source=/dev/null
  TG_BODY="$(bash "${script_dir}/tg_body.sh" 2>/dev/null || true)"
fi

# --- Dry run ----------------------------------------------------------------

if [[ -z "${TG_TOKEN:-}" ]]; then
  echo "==> TG_TOKEN unset; dry run. Would send:"
  echo "==> chat=${TG_GROUP:-unset} topic=${topic:-none}"
  echo "==> caption=${TG_BODY}"
  exit 0
fi

# --- Send -------------------------------------------------------------------

doc_args=(-F "chat_id=${TG_GROUP}")
if [[ -n "${topic}" ]]; then
  doc_args+=(-F "message_thread_id=${topic}")
fi

response="$(curl -sS -w '\n%{http_code}' \
  "${doc_args[@]}" \
  -F "document=@${zip_path}" \
  --form-string "caption=${TG_BODY}" \
  --form-string "parse_mode=HTML" \
  "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" 2>&1)" || true

http_code="$(tail -n1 <<<"${response}")"
body="$(sed '$d' <<<"${response}")"

if [[ "${http_code}" != "200" ]]; then
  echo "::warning::upload.sh: Telegram document to ${label} failed (HTTP ${http_code:-unknown}): ${body}"
else
  echo "==> Sent ${zip_path} to ${label}"
fi