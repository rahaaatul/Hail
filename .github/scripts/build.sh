#!/usr/bin/env bash
# Assemble one of: pr | debug | release
#
#   pr      -PversionName=PR<n> -PversionCode=<n> -PprNumber=<n>
#   debug   no overrides; falls back to the literals in build.gradle.kts with
#           the build type's "-Debug" suffix applied
#   release -PversionName=<tag version> -PversionCode=<derived>

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

case "${1:-}" in
  pr)      TASK=":app:assemblePr" ;;
  debug)   TASK=":app:assembleDebug" ;;
  release) TASK=":app:assembleRelease" ;;
  *) die "Usage: build.sh {pr|debug|release}" ;;
esac

EXTRA=()
case "$1" in
  pr)
    require_env PR_NUMBER
    EXTRA+=("-PversionName=PR${PR_NUMBER}"
             "-PversionCode=${PR_NUMBER}"
             "-PprNumber=${PR_NUMBER}")
    ;;
  release)
    require_env RELEASE_VERSION_NAME RELEASE_VERSION_CODE
    EXTRA+=("-PversionName=${RELEASE_VERSION_NAME}"
             "-PversionCode=${RELEASE_VERSION_CODE}")
    ;;
esac

step "Assembling ${TASK}"
./gradlew --no-daemon --stacktrace "$TASK" "${EXTRA[@]+"${EXTRA[@]}"}"

APK="$(single_match '*.apk')"
note "Built ${APK}"
if [ -n "${GITHUB_OUTPUT:-}" ]; then
  printf 'APK_PATH=%s\n' "$APK" >> "$GITHUB_OUTPUT"
  # A merge commit's subject can end in a "Co-authored-by:" trailer when the
  # trailer sits on the line straight after the subject with no blank line, so
  # git counts both as one paragraph and %s folds them together with a space.
  # That whole string reaches the Telegram caption, showing a bot attribution,
  # so strip any recognised trailer and the whitespace in front of it.
  # Single-line values, so GITHUB_OUTPUT needs no heredoc form here.
  SUBJECT="$(git log -1 --pretty=%s)"
  SUBJECT="$(printf '%s' "$SUBJECT" |
    sed -E 's/[[:space:]]+(Co-authored-by|Signed-off-by|Reviewed-by|acked-by|Tested-by):[[:space:]].*$//')"
  printf 'COMMIT_SUBJECT=%s\n' "$SUBJECT" >> "$GITHUB_OUTPUT"
  printf 'COMMIT_SHA=%s\n' "$(git rev-parse HEAD)" >> "$GITHUB_OUTPUT"
fi
