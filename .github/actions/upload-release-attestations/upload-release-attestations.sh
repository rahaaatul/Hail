#!/usr/bin/env bash
set -euo pipefail

: "${GH_TOKEN:?GH_TOKEN must be set}"
: "${RELEASE_TAG:?RELEASE_TAG must be set}"
: "${RELEASE_BUNDLE:?Release attestation bundle path must be set}"
: "${DEBUG_BUNDLE:?Debug attestation bundle path must be set}"

if [[ ! "$RELEASE_TAG" =~ ^[A-Za-z0-9][A-Za-z0-9._+-]*$ ]]; then
  echo "Invalid release tag: $RELEASE_TAG" >&2
  exit 1
fi

for bundle in "$RELEASE_BUNDLE" "$DEBUG_BUNDLE"; do
  if [[ ! -f "$bundle" ]]; then
    echo "Attestation bundle does not exist: $bundle" >&2
    exit 1
  fi
done

gh release upload "$RELEASE_TAG" "$RELEASE_BUNDLE" "$DEBUG_BUNDLE" --clobber
