#!/usr/bin/env bash
# Shell tests for the scripts themselves. Each *.sh under .github/scripts/test
# is standalone: it prints one line per assertion and exits with the number of
# failures, so any non-zero status fails the run.
#
# test.sh covers the JVM suites; this covers the shell that drives them, which
# otherwise has no test at all in CI.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

test_dir=".github/scripts/test"
[ -d "$test_dir" ] || die "No script tests found at ${test_dir}"

mapfile -t tests < <(find "$test_dir" -name '*_test.sh' -type f | sort)
[ "${#tests[@]}" -gt 0 ] || die "No *_test.sh files under ${test_dir}"

failed=0
for test in "${tests[@]}"; do
  step "Running ${test}"
  if ! bash "$test"; then
    failed=$((failed + 1))
  fi
done

if [ "$failed" -gt 0 ]; then
  die "${failed} script test(s) failed"
fi
note "Script tests passed"
