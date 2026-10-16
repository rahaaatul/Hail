#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN must be set}"
: "${RELEASE_TAG:?RELEASE_TAG must be set}"
: "${RELEASE_BUNDLE:?Release attestation bundle path must be set}"
: "${DEBUG_BUNDLE:?Debug attestation bundle path must be set}"

for bundle in "$RELEASE_BUNDLE" "$DEBUG_BUNDLE"; do
  if [[ ! -f "$bundle" ]]; then
    echo "Attestation bundle does not exist: $bundle" >&2
    exit 1
  fi
done

gh release upload "$RELEASE_TAG" "$RELEASE_BUNDLE" "$DEBUG_BUNDLE" --clobber
