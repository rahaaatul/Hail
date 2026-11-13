#!/usr/bin/env bash
#
# backmerge.sh — bring the release branch back into the development branch.
#
# The back-merge runs as the last step of the release job, outside
# semantic-release, so a conflict here can never fail the release or hold back
# its APKs. The release branch is main and the development branch is dev.
#
# Both branches carry a release commit that rewrites the same version lines,
# so the merge always conflicts in the version files. Those conflicts are
# resolved in favour of the development branch: a development build has to keep
# its development version. Every other conflict aborts the back-merge and
# leaves the development branch untouched.

set -euo pipefail

SOURCE_BRANCH="${1:-main}"
TARGET_BRANCH="${2:-dev}"
VERSION_FILES=(gradle.properties versionCode.properties)

git config user.name "semantic-release-bot"
git config user.email "semantic-release-bot@martynus.net"

git fetch --no-tags origin "$SOURCE_BRANCH" "$TARGET_BRANCH"
git checkout -B "$TARGET_BRANCH" "origin/$TARGET_BRANCH"

if ! git merge --no-edit "origin/$SOURCE_BRANCH"; then
  conflicted="$(git diff --name-only --diff-filter=U)"
  unexpected="$(printf '%s\n' "$conflicted" | grep -vE '^(gradle\.properties|versionCode\.properties)$' || true)"
  if [ -n "$conflicted" ] && [ -z "$unexpected" ]; then
    git checkout --ours -- "${VERSION_FILES[@]}"
    git add -- "${VERSION_FILES[@]}"
    git commit --no-edit
  else
    git merge --abort || true
    if [ -n "$unexpected" ]; then
      echo "back-merge stopped: conflicts outside the version files:" >&2
      printf '  %s\n' "$unexpected" >&2
    else
      echo "back-merge stopped: the merge failed without a conflict" >&2
    fi
    exit 1
  fi
fi

if [ "$(git rev-parse HEAD)" = "$(git rev-parse "origin/$TARGET_BRANCH")" ]; then
  echo "back-merge: $TARGET_BRANCH is already up to date"
  exit 0
fi

git push origin "HEAD:$TARGET_BRANCH"