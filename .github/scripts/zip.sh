#!/usr/bin/env bash
#
# zip.sh — compress APK(s) for Telegram upload.
#
# Usage: bash zip.sh <apk_path> [apk_path ...]
# Output: path(s) of the compressed file(s), one per line
#
# Threshold: APKs over 15MB are compressed with 7z -mx=9 (7z format),
# smaller ones with zip -9. 7z beats every other available tool on both
# ratio and speed for this workload (73MB debug APK -> 13MB in 12s,
# vs zstd 14MB/34s and zip 20MB/6s), keeping well under Telegram's 50MB
# upload limit.
#
# Output extension: .7z for 7z, .zip for zip. Telegram accepts either as
# a document; upload.sh routes by filename pattern, not extension.

set -euo pipefail

if [[ "$#" -eq 0 ]]; then
  echo "::error::zip.sh: no APK paths given"
  exit 1
fi

readonly THRESHOLD_MB=15

for apk in "$@"; do
  if [[ ! -f "${apk}" ]]; then
    echo "::error::zip.sh: file not found: ${apk}"
    exit 1
  fi

  size_bytes="$(stat -c%s "${apk}")"
  size_mb=$(( size_bytes / 1024 / 1024 ))

  base="$(basename "${apk}" .apk)"
  out_dir="$(dirname "${apk}")"

  if (( size_mb > THRESHOLD_MB )); then
    echo "==> ${apk}: ${size_mb}MB > ${THRESHOLD_MB}MB, using 7z -mx=9"
    out="${out_dir}/${base}.7z"
    rm -f "${out}"
    # -w${out_dir} stores only the basename so the archive contains the
    # APK directly, not the full path
    apk_abs="$(cd "$(dirname "${apk}")" && pwd)/$(basename "${apk}")"
    7z a -t7z -mx=9 -bb0 -bd -w"${out_dir}" "${out}" "${apk_abs}" >/dev/null
  else
    echo "==> ${apk}: ${size_mb}MB <= ${THRESHOLD_MB}MB, using zip -9"
    out="${out_dir}/${base}.zip"
    rm -f "${out}"
    # -j stores only the basename
    zip -j -9 "${out}" "${apk}" >/dev/null
  fi

  echo "${out}"
done