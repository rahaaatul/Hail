#!/usr/bin/env bash
# Stage the release keystore from secrets.
#
# Gradle's signingConfig takes storeFile as a File, so a keystore that lives
# only in a secret has to be decoded somewhere on disk. It lands in RUNNER_TEMP,
# outside the checkout. The credentials are not written anywhere: the workflow
# passes them as ORG_GRADLE_PROJECT_* environment variables, which Gradle
# surfaces as project properties.
#
# No EXIT trap. An earlier version had one, which was wrong: it fired when THIS
# script returned, deleting the keystore before the later Build step on the same
# runner could read it. The runner discards RUNNER_TEMP when the job ends, so
# nothing needs cleaning up here.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_env KEYSTORE

KEYSTORE_PATH="${RUNNER_TEMP}/release.jks"

printf '%s' "$KEYSTORE" | base64 -d > "$KEYSTORE_PATH"
chmod 600 "$KEYSTORE_PATH"

# build.gradle.kts reads this. GITHUB_ENV persists it to the later Build step.
echo "RELEASE_KEYSTORE_PATH=${KEYSTORE_PATH}" >> "$GITHUB_ENV"

note "Release keystore staged at ${KEYSTORE_PATH}"
