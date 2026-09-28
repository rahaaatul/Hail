#!/usr/bin/env bash
# Derive versionName and versionCode from a vX.Y.Z tag, refusing to go
# backwards. Results go to $GITHUB_OUTPUT.
#
#   v1.12.0 -> 1.12.0, 11200
#   v1.12.3 -> 1.12.3, 11203
#   v1.13.0 -> 1.13.0, 11300
#
# The first release moves versionCode from the legacy 43 to ~11200. That
# discontinuity is deliberate and one-time: it buys a versionCode that is a pure
# function of the tag, with no counter to maintain and no way to drift. Do not
# "fix" it by reintroducing a manual counter.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

require_env TAG

VERSION="${TAG#v}"
if ! [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  die "Tag '${TAG}' is not vMAJOR.MINOR.PATCH"
fi
MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

if (( MINOR > 99 || PATCH > 99 )); then
  die "minor and patch must each be below 100 (got ${MINOR}.${PATCH})"
fi

VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))

# Pre-release and release share an applicationId and a signing key, so Android
# requires a strictly higher versionCode. A decrease is refused at install time
# with no useful message, so fail here instead.
PREVIOUS="$(git tag --list 'v[0-9]*' --sort=-v:refname | head -1 || true)"
if [ -n "$PREVIOUS" ]; then
  PREV="${PREVIOUS#v}"
  if [[ "$PREV" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    PREVIOUS_CODE=$(( BASH_REMATCH[1] * 10000 + BASH_REMATCH[2] * 100 + BASH_REMATCH[3] ))
    if (( VERSION_CODE <= PREVIOUS_CODE )); then
      die "Tag ${TAG} derives versionCode ${VERSION_CODE}, not greater than the previous tag ${PREVIOUS} (${PREVIOUS_CODE}). The tag must increase."
    fi
  fi
fi

{
  echo "RELEASE_VERSION_NAME=${MAJOR}.${MINOR}.${PATCH}"
  echo "RELEASE_VERSION_CODE=${VERSION_CODE}"
} >> "$GITHUB_OUTPUT"

note "${MAJOR}.${MINOR}.${PATCH} -> versionCode ${VERSION_CODE}"
