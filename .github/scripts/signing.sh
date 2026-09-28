#!/usr/bin/env bash
# Stage the release keystore from secrets.
#
# Gradle's signingConfig takes storeFile as a File, so a keystore that lives
# only in a secret has to be decoded somewhere on disk. It lands in RUNNER_TEMP,
# outside the checkout, and is removed on exit. The credentials are not written
# anywhere: the workflow passes them as ORG_GRADLE_PROJECT_* environment
# variables, which Gradle surfaces as project properties.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_env KEYSTORE

KEYSTORE_PATH="${RUNNER_TEMP}/release.jks"

cleanup() { rm -f "$KEYSTORE_PATH"; }
trap cleanup EXIT

printf '%s' "$KEYSTORE" | base64 -d > "$KEYSTORE_PATH"
chmod 600 "$KEYSTORE_PATH"

# build.gradle.kts reads this. GITHUB_ENV persists it to the later Build step.
echo "RELEASE_KEYSTORE_PATH=${KEYSTORE_PATH}" >> "$GITHUB_ENV"

note "Release keystore staged at ${KEYSTORE_PATH}"
