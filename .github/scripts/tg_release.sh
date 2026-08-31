#!/usr/bin/env bash
#
# Sends built release APKs to Telegram as a single grouped album message
# (sendMediaGroup). Called from release.yml with the APK paths as args.
#
# Never fails the calling workflow: any problem (missing file, bad
# response, network error) prints a GitHub Actions warning and exits 0,
# since a failed notification shouldn't block a successful release.

set -uo pipefail

: "${TG_TOKEN:?TG_TOKEN is not set}"
: "${TG_RELEASE:?TG_RELEASE is not set}"
: "${VERSION:?VERSION is not set}"
: "${RELEASE_URL:?RELEASE_URL is not set}"

apk_files=("$@")

if [[ ${#apk_files[@]} -eq 0 ]]; then
    echo "::warning::tg_release.sh: no APK files given, skipping Telegram notification"
    exit 0
fi

for f in "${apk_files[@]}"; do
    if [[ ! -f "$f" ]]; then
        echo "::warning::tg_release.sh: file not found: $f - skipping Telegram notification"
        exit 0
    fi
done

# plain-text version (real newline) for the sendDocument fallback,
# JSON-escaped version (literal \n) for the sendMediaGroup caption field
caption="<b>Hail v${VERSION}</b>
${RELEASE_URL}"
caption_json="${caption//$'\n'/\\n}"

if [[ ${#apk_files[@]} -eq 1 ]]; then
    # sendMediaGroup requires 2-10 items; fall back to a single document
    response="$(curl -sS -w '\n%{http_code}' \
        -F "chat_id=${TG_RELEASE}" \
        -F "document=@${apk_files[0]}" \
        --form-string "caption=${caption}" \
        --form-string "parse_mode=HTML" \
        "https://api.telegram.org/bot${TG_TOKEN}/sendDocument" 2>&1)" || true
else
    curl_args=()
    media_items=()
    for i in "${!apk_files[@]}"; do
        field="f${i}"
        curl_args+=(-F "${field}=@${apk_files[$i]}")
        if [[ "$i" -eq 0 ]]; then
            media_items+=("{\"type\":\"document\",\"media\":\"attach://${field}\",\"caption\":\"${caption_json}\",\"parse_mode\":\"HTML\"}")
        else
            media_items+=("{\"type\":\"document\",\"media\":\"attach://${field}\"}")
        fi
    done
    media_json="[$(IFS=,; echo "${media_items[*]}")]"

    response="$(curl -sS -w '\n%{http_code}' \
        -F "chat_id=${TG_RELEASE}" \
        -F "media=${media_json}" \
        "${curl_args[@]}" \
        "https://api.telegram.org/bot${TG_TOKEN}/sendMediaGroup" 2>&1)" || true
fi

http_code="$(tail -n1 <<<"${response}")"
body="$(sed '$d' <<<"${response}")"

if [[ "${http_code}" != "200" ]]; then
    echo "::warning::tg_release.sh: Telegram notification failed (HTTP ${http_code:-unknown}): ${body}"
    exit 0
fi

echo "==> Sent ${#apk_files[@]} APK(s) to Telegram"
