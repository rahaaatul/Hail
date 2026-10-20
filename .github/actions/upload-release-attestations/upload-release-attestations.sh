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

# The two bundles must not share a name. Uploading two assets with the same
# name in one command fails with a 404, because the second upload takes the
# place of the first one and the replacement cannot be found again.
work_dir="$(mktemp -d)"
trap 'rm -rf "$work_dir"' EXIT
cp "$RELEASE_BUNDLE" "$work_dir/attestation-release.json"
cp "$DEBUG_BUNDLE" "$work_dir/attestation-debug.json"

gh release upload "$RELEASE_TAG" \
  "$work_dir/attestation-release.json" \
  "$work_dir/attestation-debug.json" \
  --clobber
