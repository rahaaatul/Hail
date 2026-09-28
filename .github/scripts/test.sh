#!/usr/bin/env bash
# JVM unit tests. Reports land in app/build/reports/tests/testDebugUnitTest,
# which the workflow uploads with `if: failure()`.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

step "Running unit tests"
./gradlew --no-daemon --stacktrace :app:testDebugUnitTest
note "Unit tests passed"
