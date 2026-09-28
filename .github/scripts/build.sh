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
  # Single-line subject, so GITHUB_OUTPUT needs no heredoc form here.
  printf 'COMMIT_SUBJECT=%s\n' "$(git log -1 --pretty=%s)" >> "$GITHUB_OUTPUT"
fi
