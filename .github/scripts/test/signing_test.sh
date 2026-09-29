#!/usr/bin/env bash
# Test that signing.sh stages the keystore and leaves it readable for the
# Build step that runs later on the same runner.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
source .github/scripts/lib.sh

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n     %s\n' "$1" "$2"; FAILURES=$((FAILURES+1)); }

# A real 2618-byte PKCS12 keystore, committed at .github/debug.keystore.
KEYSTORE="$(base64 -w0 .github/debug.keystore)"

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT
export RUNNER_TEMP="$scratch" GITHUB_ENV="$scratch/env"

# `|| stage_rc=$?` rather than a bare call: lib.sh sets -e, and errexit does not
# apply to the left of a || list, so a failing signing.sh is reported as a FAIL
# line instead of aborting the file with no output at all. Without it the test
# goes silent in exactly the case it exists to diagnose.
stage_rc=0
RUNNER_TEMP="$scratch" GITHUB_ENV="$scratch/env" \
  KEYSTORE="$KEYSTORE" bash .github/scripts/signing.sh >/dev/null 2>&1 || stage_rc=$?

[ "$stage_rc" -eq 0 ] \
  && pass "signing.sh exits 0" \
  || fail "signing.sh exits 0" "got exit $stage_rc"

keystore_path="$(sed -n 's/^RELEASE_KEYSTORE_PATH=//p' "$scratch/env" 2>/dev/null || true)"
[ -n "$keystore_path" ] \
  && pass "exports RELEASE_KEYSTORE_PATH" \
  || fail "exports RELEASE_KEYSTORE_PATH" "GITHUB_ENV has no entry: $(cat "$scratch/env" 2>/dev/null || true)"

[ -f "$keystore_path" ] \
  && pass "keystore survives the script, readable by the Build step" \
  || fail "keystore survives the script" "$keystore_path does not exist after the script returned"

if [ -f "$keystore_path" ]; then
  a="$(sha256sum "$keystore_path" | cut -d' ' -f1)"
  b="$(sha256sum .github/debug.keystore | cut -d' ' -f1)"
  [ "$a" = "$b" ] \
    && pass "decoded bytes match the committed keystore" \
    || fail "decoded bytes match" "sha256 $a != $b"
  [ "$(stat -c%s "$keystore_path")" -eq 2618 ] \
    && pass "decoded size is 2618 bytes" \
    || fail "decoded size is 2618" "got $(stat -c%s "$keystore_path")"
fi

# The guard the release path depends on. lib.sh sets `set -e`, and this case
# EXPECTS a non-zero exit, so the capture must be the left side of `||`:
# errexit does not apply there, and rc is still the command's own status.
out="" && rc=0
out="$(env -u KEYSTORE bash .github/scripts/signing.sh 2>&1)" && rc=0 || rc=$?
[ "$rc" -eq 1 ] \
  && pass "exits 1 when KEYSTORE is unset" \
  || fail "exits 1 when KEYSTORE is unset" "got exit $rc"
case "$out" in
  *"Missing required environment variable: KEYSTORE"*) pass "names the missing variable" ;;
  *) fail "names the missing variable" "got: $out" ;;
esac

printf '\n%s\n' "$([ "$FAILURES" -eq 0 ] && echo 'PASS: all assertions' || echo "FAIL: $FAILURES assertion(s)")"
exit "$FAILURES"
