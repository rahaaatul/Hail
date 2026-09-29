# Task A1 — Stage the keystore without destroying it

Extracted verbatim from `plans/2026-09-29-ci-release-hardening.md`, `## Task A1`
(lines 67-208). Only Task A1 is in scope. Tasks A2, A3, B*, C*, D* are out of scope.

## Global Constraints that bind this task (from the plan)

- **No `${{ }}` inside any `run:` body.** Values reach scripts through `env:`.
- **Scripts are invoked as executables, never `source`d.** `bash -c 'source test.sh'`
  returns 0 even when the script fails, silently turning a red test green.
- **Only the keystore is written to disk**, into `RUNNER_TEMP`, because Gradle's
  `signingConfig` takes a `File`. The three credentials are passed as
  `ORG_GRADLE_PROJECT_release*` project properties and never touch the working tree.
- **A release build with no keystore staged is unsigned, never debug-signed.**

## Task A1 body (verbatim from the plan)

### Task A1: Stage the keystore without destroying it

**Files:**
- Modify: `.github/scripts/signing.sh`

**Interfaces:**
- Consumes: `lib.sh` (`require_env`, `note`, `step`, `die`, `repo_root`); env `KEYSTORE`, `RUNNER_TEMP`, `GITHUB_ENV`
- Produces: sets `RELEASE_KEYSTORE_PATH` in `$GITHUB_ENV`; **leaves the keystore on disk** for the later `Build` step

**Background — [118-1] CRITICAL.** The current script registers `trap cleanup EXIT`, which fires when *the script* exits, i.e. at the end of the `Stage release keystore` step. The `Build` step runs later on the same runner and inherits only `RELEASE_KEYSTORE_PATH` through `$GITHUB_ENV`, so by then the file is gone. `build.gradle.kts` then leaves the `release` signing config empty while `buildTypes.release` still assigns it because the env var is non-null, and AGP fails with `SigningConfig "release" is missing required property 'storeFile'`, or the run produces an unsigned "release". The runner destroys the workspace anyway, so the trap buys nothing.

Ruling: the test captures the expected non-zero exit with `out="$(...)" && rc=0 || rc=$?` rather than `out="$(...)"; rc=$?`. The test `source`s `lib.sh`, whose line 4 is `set -euo pipefail`, so under `errexit` the plain assignment kills the test on the very non-zero exit it is asserting, `rc` is never assigned, and `PASS: all assertions` is never printed. The `||` form is used verbatim in A1, B1, B2 and D1 so the idiom is identical everywhere. Cost if wrong: none — `cmd && rc=0 || rc=$?` yields the command's own status whether it succeeds or fails, and it does not weaken any assertion. The same ruling supplies defect 5 in B1 and D1, so the three harnesses stay in step.

#### Step 1: Write the failing test

The test is a shell script, since the production code is shell. Create `.github/scripts/test/signing_test.sh`:

```bash
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

RUNNER_TEMP="$scratch" GITHUB_ENV="$scratch/env" \
  KEYSTORE="$KEYSTORE" bash .github/scripts/signing.sh >/dev/null 2>&1
stage_rc=$?

[ "$stage_rc" -eq 0 ] \
  && pass "signing.sh exits 0" \
  || fail "signing.sh exits 0" "got exit $stage_rc"

keystore_path="$(sed -n 's/^RELEASE_KEYSTORE_PATH=//p' "$scratch/env" 2>/dev/null)"
[ -n "$keystore_path" ] \
  && pass "exports RELEASE_KEYSTORE_PATH" \
  || fail "exports RELEASE_KEYSTORE_PATH" "GITHUB_ENV has no entry: $(cat "$scratch/env" 2>/dev/null)"

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
```

#### Step 2: Run test to verify it fails

Run: `bash .github/scripts/test/signing_test.sh`
Expected: **FAIL** on "keystore survives the script" — `signing.sh` deletes the file on exit.

#### Step 3: Write minimal implementation

Replace `.github/scripts/signing.sh` with:

```bash
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
```

#### Step 4: Run test to verify it passes

Run: `bash .github/scripts/test/signing_test.sh`
Expected: `PASS: all assertions`, exit 0.

Also confirm the whole set still parses:
```bash
bash -n .github/scripts/signing.sh && echo "syntax OK"
```

#### Step 5: Commit

```bash
git add .github/scripts/signing.sh .github/scripts/test/signing_test.sh
git commit -m "fix(ci): stop the staging script deleting its own keystore

The EXIT trap fired when the script returned, so the keystore was gone before
the later Build step on the same runner could read it. That left the release
signing config empty while buildTypes.release still assigned it, producing an
opaque 'missing required property storeFile' or an unsigned release. The
runner discards RUNNER_TEMP when the job ends, so the trap bought nothing."
```

## Commit-message requirement added by the dispatcher

Follow the Context-First pattern this project now requires: imperative summary line,
then why the trap is conceptually wrong, then what it caused across the release path,
then the mechanics. Footer references the issue it addresses. The four CI issues
already closed (#85, #86, #89, #90) were resolved by merged PRs and need no reference.
