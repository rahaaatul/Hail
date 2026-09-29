# CI Release Path Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the release path actually work, restore APK compression for Telegram, and close the remaining review findings on PR #118.

**Architecture:** Four phases in strict dependency order. Phase A unblocks the release path — three CRITICAL defects mean releases cannot complete today. Phase B restores `zip.sh` and wires it into `build.sh`, so unminified debug APKs fit Telegram's 50 MB limit. Phase C fixes the remaining WARNINGs, all robustness not correctness. Phase D clears the SUGGESTIONs, all small.

**Tech Stack:** Bash (5.1+), Python 3.10 stdlib, GitHub Actions, Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, JDK 26.

**Spec:** No separate spec document. The binding requirements are this plan's Global Constraints, plus the `kilo-code-bot` review on PR #118 (findings numbered `[118-1]` through `[118-10]` and two summary-only findings). Where this plan and a review finding disagree, this plan is the argument and the finding is the authority.

## Global Constraints

- **`pr.yml` and `debug.yml` hold `contents: read` and reference no signing secret.** A pull request must not be able to read `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS` or `KEYSTORE_ALIAS_PASSWORD` under any branch scenario, including a branch inside this repository.
- **No `${{ }}` inside any `run:` body.** Values reach scripts through `env:`. This closes the shell-injection path where an artifact filename could execute commands in the step holding the Telegram token.
- **Scripts are invoked as executables, never `source`d.** `bash -c 'source test.sh'` returns 0 even when the script fails, silently turning a red test green.
- **Action versions are pinned:** `actions/checkout@v7`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`, `actions/download-artifact@v7`, `android-actions/setup-android@v4`, `softprops/action-gh-release@v3`. Do not use `setup-java@v6`; its README states it is not yet recommended for production and `v5` is the documented stable release.
- **`accept-android-sdk-licenses: true` unquoted.** YAML 1.2 core schema does not treat bare `yes` as boolean; that is YAML 1.1, and `setup-android`'s `getBooleanInput` rejects it. This exact bug stopped a run on 2026-09-28.
- **Android platform package is `platforms;android-37.0`.** The bare `platforms;android-37` does not exist in the SDK repository. `build-tools;37.0.0` is correct.
- **Use `gradle/actions/setup-gradle`, not `setup-java`'s `cache: gradle`.** The setup-java README defers to it for Gradle; its own cache stores only what the creating run downloaded and is not re-saved on a hit.
- **Only the keystore is written to disk**, into `RUNNER_TEMP`, because Gradle's `signingConfig` takes a `File`. The three credentials are passed as `ORG_GRADLE_PROJECT_release*` project properties and never touch the working tree.
- **A release build with no keystore staged is unsigned, never debug-signed.** A build type with no assigned `signingConfig` produces `*-unsigned.apk`.
- **`versionCode` is derived from the tag**, `MAJOR*10000 + MINOR*100 + PATCH`, and must strictly increase across releases. Pre-release and release share an `applicationId` and a signing key, so a decrease is refused at install time with no useful message. The jump from the legacy 43 to ~11200 is deliberate: it makes `versionCode` a pure function of the tag with no counter to drift.
- **Telegram captions are HTML and must set `parse_mode=HTML`.** The captions are built from `<b>`, `<blockquote>`, `<p>`, `<strong>` and `<a href>`; without `parse_mode` every caption renders as literal tags. The script this replaced sent it.
- **Every dynamic value in a caption passes through `html.escape` at the point of interpolation.** Not a hand-written substitution.
- **Telegram `sendDocument` accepts files up to 50 MB.** `upload.py` detects and warns; it does not mitigate. Mitigation is compression.
- **Compression tools are preinstalled on the runner.** `zip`, `unzip` and `p7zip-full` are in the `ubuntu-24.04` preinstalled apt list, and remain on 26.04 as `7zip`. `android-actions/setup-android@v4` installs only SDK packages, so no extra `apt-get` step is required.
- **A `pr` build is not debuggable.** `create("pr")` with no `initWith` does not inherit `debug`'s `isDebuggable = true`; it resolves to AGP's non-debuggable default. This is existing intended behaviour, recorded here so no task "fixes" it.
- **No R8 on any build the pipeline produces.** `isMinifyEnabled` and `isShrinkResources` are set only on `release`. `pr` and `debug` APKs are fully unminified and must stay that way.
- **Never commit `.actrc`, `.secrets`, or a Gradle init script.** Sandbox scratch only.

## Scope

Deliberately excluded:

- **`HBackup.kt:47-50`** — the `mkdirs()` defect that fails 3 of 19 unit tests. It is a real user-facing bug, but it is app code in PR #109's territory, not CI. Fixing it here would mix subsystems.
- **PR #112 and PR #79** — subsumed by PR #118. Close them, do not salvage findings.
- **PRs #75–#78** — Dependabot PRs, all DIRTY. Rebase or close after this lands.
- **PR #117** — the gradle-wrapper bump is CLEAN and independent. Take it separately.

---

## Phases and why this order

| Phase | Content | Gate before moving on |
|---|---|---|
| **A** | Three CRITICALs + the `build.gradle.kts` guard alignment | The release path completes end to end |
| **B** | Compression restore and `build.sh` wiring | A `pr.yml` run produces a Telegram-deliverable artifact |
| **C** | Four WARNINGs | No step can fail with a raw traceback or misleading error |
| **D** | Five SUGGESTIONs | `version.sh` cannot overflow, dead code removed |

**A is first because nothing else is verifiable until it works.** The three CRITICALs are structural: the keystore is deleted before the build reads it, the publish job has an empty workspace, and every caption renders as literal HTML tags. The release path has never executed, and these are the reasons.

**B is second because it is a contract change** that Phase A does not touch, but Phase C's Telegram findings assume a single `APK_PATH` that may be an archive.

**C is third because all four are robustness.** None changes the happy path. Fixing them before A would mean fixing them twice.

**D is last.** All are small; two are comments that misdescribe code.

---

# Phase A — The release path cannot work

Three CRITICALs. Until these are fixed, no release can complete.

## Task A1: Stage the keystore without destroying it

**Files:**
- Modify: `.github/scripts/signing.sh`

**Interfaces:**
- Consumes: `lib.sh` (`require_env`, `note`, `step`, `die`, `repo_root`); env `KEYSTORE`, `RUNNER_TEMP`, `GITHUB_ENV`
- Produces: sets `RELEASE_KEYSTORE_PATH` in `$GITHUB_ENV`; **leaves the keystore on disk** for the later `Build` step

**Background — [118-1] CRITICAL.** The current script registers `trap cleanup EXIT`, which fires when *the script* exits, i.e. at the end of the `Stage release keystore` step. The `Build` step runs later on the same runner and inherits only `RELEASE_KEYSTORE_PATH` through `$GITHUB_ENV`, so by then the file is gone. `build.gradle.kts` then leaves the `release` signing config empty while `buildTypes.release` still assigns it because the env var is non-null, and AGP fails with `SigningConfig "release" is missing required property 'storeFile'`, or the run produces an unsigned "release". The runner destroys the workspace anyway, so the trap buys nothing.

Ruling: the test captures the expected non-zero exit with `out="$(...)" && rc=0 || rc=$?` rather than `out="$(...)"; rc=$?`. The test `source`s `lib.sh`, whose line 4 is `set -euo pipefail`, so under `errexit` the plain assignment kills the test on the very non-zero exit it is asserting, `rc` is never assigned, and `PASS: all assertions` is never printed. The `||` form is used verbatim in A1, B1, B2 and D1 so the idiom is identical everywhere. Cost if wrong: none — `cmd && rc=0 || rc=$?` yields the command's own status whether it succeeds or fails, and it does not weaken any assertion. The same ruling supplies defect 5 in B1 and D1, so the three harnesses stay in step.

- [ ] **Step 1: Write the failing test**

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

- [ ] **Step 2: Run test to verify it fails**

Run: `bash .github/scripts/test/signing_test.sh`
Expected: **FAIL** on "keystore survives the script" — `signing.sh` deletes the file on exit.

- [ ] **Step 3: Write minimal implementation**

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

- [ ] **Step 4: Run test to verify it passes**

Run: `bash .github/scripts/test/signing_test.sh`
Expected: `PASS: all assertions`, exit 0.

Also confirm the whole set still parses:
```bash
bash -n .github/scripts/signing.sh && echo "syntax OK"
```

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/signing.sh .github/scripts/test/signing_test.sh
git commit -m "fix(ci): stop the staging script deleting its own keystore

The EXIT trap fired when the script returned, so the keystore was gone before
the later Build step on the same runner could read it. That left the release
signing config empty while buildTypes.release still assigned it, producing an
opaque 'missing required property storeFile' or an unsigned release. The
runner discards RUNNER_TEMP when the job ends, so the trap bought nothing."
```

## Task A2: The publish job has an empty workspace

**Files:**
- Modify: `.github/workflows/release.yml` (the `publish` job, currently starting near line 100)
- Modify: `.github/scripts/upload.py` (inside `send_document`)

**Interfaces:**
- Consumes: the `build` job's outputs `channel` and `version`; the artifact named `apk-<version>` uploaded by that job
- Produces: a draft GitHub Release with the APK attached, and a Telegram upload that can actually find `upload.py`

**Background — [118-2] CRITICAL.** `publish` runs on a fresh runner with an empty workspace. There is no `actions/checkout` step, and the APK was produced in the separate `build` job on a different runner. With `fail_on_unmatched_files: true` the release step fails outright, and the `Upload to Telegram` step below fails too because `.github/scripts/upload.py` is not on disk.

- [ ] **Step 1: Write the failing test**

The test asserts the structure of the workflow, because the failure is structural. Create `.github/scripts/test/release_workflow_test.py`:

```python
#!/usr/bin/env python3
"""Assert the release workflow's publish job can reach the artifact it publishes."""
import pathlib
import re
import sys

WORKFLOW = pathlib.Path(".github/workflows/release.yml")
text = WORKFLOW.read_text(encoding="utf-8")

failures = []


def check(ok: bool, name: str, detail: str = "") -> None:
    print(f"  {'ok  ' if ok else 'FAIL'} {name}" + (f"\n       {detail}" if not ok and detail else ""))
    if not ok:
        failures.append(name)


# Isolate the publish job: it is the last job in the file.
start = text.index("\n  publish:")
publish = text[start:]

check("actions/checkout" in publish,
      "publish job checks out the repository",
      "upload.py is not on disk without a checkout")
check(re.search(r"actions/download-artifact@v\d+", publish) is not None,
      "publish job downloads the build artifact",
      "the APK was produced in the build job on a different runner")
check("name: apk-${{ needs.build.outputs.version }}" in publish,
      "downloads the artifact the build job uploaded",
      "expected 'apk-${{ needs.build.outputs.version }}'")
check("needs:" in publish and "build" in publish.split("steps:")[0],
      "publish declares a dependency on the build job")
check("${{ needs.build.outputs.channel }}" in publish,
      "publish uses the build job's channel output")
check("Hail-*.apk" in publish, "the release attaches the APK")

# parse_mode must be sent, or captions render as literal tags.
uploader = pathlib.Path(".github/scripts/upload.py").read_text(encoding="utf-8")
check('field("parse_mode", "HTML")' in uploader,
      "upload.py sends parse_mode=HTML",
      "captions are HTML; without parse_mode they render as literal tags")

print(f"\n{'PASS: all assertions' if not failures else 'FAIL: ' + str(len(failures)) + ' assertion(s)'}")
sys.exit(len(failures))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 .github/scripts/test/release_workflow_test.py`
Expected: **FAIL** on "publish job checks out the repository", "publish job downloads the build artifact", and "upload.py sends parse_mode=HTML".

- [ ] **Step 3: Write minimal implementation**

In `.github/workflows/release.yml`, replace the `publish` job with:

```yaml
  publish:
    name: Publish draft
    needs: [build]
    runs-on: ubuntu-latest
    steps:
      # publish runs on its own runner, so the repository and the artifact the
      # build job produced both have to be brought back before anything can use
      # them. Without this the workspace is empty: softprops finds no APK and
      # upload.py is not on disk.
      - uses: actions/checkout@v7

      - name: Download APK
        uses: actions/download-artifact@v7
        with:
          name: apk-${{ needs.build.outputs.version }}
          path: .

      - name: Create draft release
        uses: softprops/action-gh-release@v3
        with:
          tag_name: ${{ github.ref_name }}
          name: Hail ${{ needs.build.outputs.version }}
          draft: true
          prerelease: ${{ needs.build.outputs.channel == 'prerelease' }}
          fail_on_unmatched_files: true
          generate_release_notes: true
          files: Hail-*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}

      - name: Upload to Telegram
        if: always()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/Hail-${{ needs.build.outputs.version }}.apk
          RELEASE_TAG: ${{ github.ref_name }}
          TG_CHANNEL: ${{ needs.build.outputs.channel }}
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
```

- [ ] **Step 4: Write minimal implementation — add `parse_mode`**

In `.github/scripts/upload.py`, inside `send_document`, insert these three lines **immediately after** the `caption` field line — as new lines, with a blank line after them so the following `payload += (` stays separate:

```python
        # The captions are HTML (<b>, <blockquote>, <p>, <strong>, <a href>).
        # Telegram defaults to no parse mode, so without this every caption
        # renders as literal tags. The script this replaced sent it.
        payload += field("parse_mode", "HTML")

```

- [ ] **Step 5: Run test to verify it passes**

Run: `python3 .github/scripts/test/release_workflow_test.py`
Expected: `PASS: all assertions`, exit 0.

Re-verify the uploader still compiles and its existing assertions hold:
```bash
python3 -m py_compile .github/scripts/upload.py && echo "compiles OK"
python3 - <<'PY'
import importlib.util
spec = importlib.util.spec_from_file_location("u", ".github/scripts/upload.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
out = m.release_caption("v1.11.3")
assert "<b>Highlights</b>" in out and "1.11.3" not in out, out
out = m.release_caption("v1.11.4")
assert 'href="https://github.com/rahaaatul/Hail/releases/tag/v1.11.4"' in out, out
out = m.pr_caption("79", '<b>x</b> & "y"')
assert "<b>x</b>" not in out and "&lt;b&gt;" in out, out
print("PASS: upload.py captions still correct")
PY
```

And confirm the workflow still satisfies the security constraints:
```bash
python3 - <<'PY'
import yaml
d = yaml.safe_load(open('.github/workflows/release.yml'))
for job, spec in d['jobs'].items():
    for s in spec['steps']:
        assert '${{' not in s.get('run', ''), (job, s.get('name'))
assert d['permissions'] == {'contents': 'write'}, d['permissions']
print('PASS: no expressions in run blocks, permissions correct')
PY
```

- [ ] **Step 6: Commit**

```bash
git add .github/workflows/release.yml .github/scripts/upload.py .github/scripts/test/release_workflow_test.py
git commit -m "fix(ci): give the publish job a workspace and send parse_mode

publish runs on its own runner, so without a checkout and an artifact
download it had an empty workspace: softprops found no APK and upload.py
was not on disk.

upload.py never sent parse_mode, so every caption rendered as literal
<b>...</b> tags. The script it replaced did."
```

## Task A3: Align the two signing-guard conditions

**Files:**
- Modify: `app/build.gradle.kts` (the `release` build type)

**Interfaces:**
- Consumes: env `RELEASE_KEYSTORE_PATH`; project properties `releaseStorePassword`, `releaseKeyAlias`, `releaseKeyPassword`
- Produces: one consistent condition, so a missing keystore yields an unsigned build rather than an opaque AGP failure

**Background — [118-4] WARNING.** `signingConfigs.create("release")` bails out when `keystore.exists()` is false, but `buildTypes.release` still assigns that same empty config as soon as `RELEASE_KEYSTORE_PATH` is non-null. The two conditions disagree whenever the file is missing, which is exactly what happened under the old `signing.sh`. AGP then fails deep inside signing instead of producing the clear "no keystore staged" outcome.

Ruling: the test asserts against the `val` line *and* the `if` condition, not the `if` condition alone. The fix reads the env var into a local and tests the file, so `RELEASE_KEYSTORE_PATH` necessarily appears on the `val` line, outside any `if (...)` capture — asserting it inside the condition asserts text the implementation cannot contain. Cost if wrong: if a future edit drops the `val` and inlines `System.getenv(...)` into the condition, the `val` check fails and points at the real regression rather than passing silently. A third assertion now also pins the *negative*: the old bare env-var test must be gone, so the test cannot pass against the pre-fix file.

- [ ] **Step 1: Write the failing test**

Create `.github/scripts/test/build_gradle_test.py`:

```python
#!/usr/bin/env python3
"""Assert the release signing decision is made from one condition, once."""
import pathlib
import re
import sys

text = pathlib.Path("app/build.gradle.kts").read_text(encoding="utf-8")
failures = []


def check(ok: bool, name: str, detail: str = "") -> None:
    print(f"  {'ok  ' if ok else 'FAIL'} {name}" + (f"\n       {detail}" if not ok and detail else ""))
    if not ok:
        failures.append(name)


# The build type must not assign the release config on a bare env-var test.
release = re.search(r"release \{.*?\n        \}", text, re.S)
check(release is not None, "release build type is present")
block = release.group(0) if release else ""

if "signingConfigs.getByName(\"release\")" in block:
    # The decision is the `val` that reads the env var PLUS the `if` that tests
    # the file. Capturing only `if (...)` misses the env-var name, which
    # necessarily lives on the preceding line, and the guard is then asserted
    # to be something it never claimed to be.
    val = re.search(r'(val\s+\w+\s*=\s*System\.getenv\("RELEASE_KEYSTORE_PATH"\).*?)\n', block, re.S)
    guard = re.search(r"if \((.*?)\) \{", block, re.S)
    cond = guard.group(1).strip() if guard else ""
    reading = val.group(1) if val else ""
    # The env var alone is not enough: the file has to be there.
    check("exists()" in cond,
          "release assigns the signing config only when the keystore file exists",
          f"condition is `{cond}` - tests the env var, not the file")
    check("RELEASE_KEYSTORE_PATH" in reading,
          "the guard still names the env var",
          f"no line reading RELEASE_KEYSTORE_PATH in the release block; condition is `{cond}`")
    check("System.getenv(\"RELEASE_KEYSTORE_PATH\") != null" not in cond,
          "the env var alone no longer gates signing",
          f"condition is `{cond}` - it must also require the file to exist")
else:
    check(True, "release assigns the signing config only when the keystore file exists (no assignment)")

# Credentials must come from ORG_GRADLE_PROJECT_* project properties, never a
# properties file in the working tree.
check("signing.properties" not in text,
      "no signing.properties is read from the working tree",
      "credentials must arrive as project properties")
check('findProperty("releaseStorePassword")' in text,
      "store password comes from a project property")
check('findProperty("releaseKeyAlias")' in text, "key alias comes from a project property")
check('findProperty("releaseKeyPassword")' in text, "key password comes from a project property")

# The pr applicationId must not regress to the unlinkable dotted form.
check('".pr$it"' in text,
      "pr applicationId suffix has no leading dot before the number",
      "aapt2 rejects a package segment starting with a digit")

# Debug and pr must never gain minification.
for name in ("debug", 'create("pr")'):
    seg = re.search(re.escape(name) + r" \{.*?\n        \}", text, re.S)
    body = seg.group(0) if seg else ""
    check("isMinifyEnabled" not in body,
          f"{name} has no minification",
          "debug APKs must stay unminified so they compress predictably")

print(f"\n{'PASS: all assertions' if not failures else 'FAIL: ' + str(len(failures)) + ' assertion(s)'}")
sys.exit(len(failures))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 .github/scripts/test/build_gradle_test.py`
Expected: **FAIL** on "release assigns the signing config only when the keystore file exists".

- [ ] **Step 3: Write minimal implementation**

In `app/build.gradle.kts`, change the `release` build type from:

```kotlin
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (System.getenv("RELEASE_KEYSTORE_PATH") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
```

to:

```kotlin
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Decide signing from the same condition the signingConfigs block
            // uses. Testing only the env var assigns an empty config when the
            // keystore file is absent, which fails deep inside AGP with
            // "missing required property 'storeFile'" instead of producing the
            // clear unsigned build this branch is meant to guarantee.
            val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
            if (releaseKeystore != null && releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 .github/scripts/test/build_gradle_test.py`
Expected: `PASS: all assertions`, exit 0.

Report honestly which of these you actually ran, because no Android SDK is available locally:
```bash
ls ~/.gradle/wrapper/dists 2>/dev/null || echo "no Gradle distribution cached locally"
./gradlew --offline -q help 2>&1 | head -5 || true
```
If neither can run, say so in the commit report rather than implying the file was compiled.

- [ ] **Step 5: Commit**

```bash
git add app/build.gradle.kts .github/scripts/test/build_gradle_test.py
git commit -m "fix(build): decide release signing from the keystore file, not the env var

signingConfigs bails out when the keystore is absent, but buildTypes.release
still assigned the empty config on a bare env-var check, so the two
disagreed and AGP failed deep inside signing instead of producing the
unsigned build this branch intends."
```

## Phase A gate

Do not start Phase B until this passes. **No release has ever completed, and these are the three reasons.**

Verify before moving on:
```bash
bash .github/scripts/test/signing_test.sh
python3 .github/scripts/test/release_workflow_test.py
python3 .github/scripts/test/build_gradle_test.py
for f in .github/scripts/*.sh; do bash -n "$f" || echo "PARSE FAIL $f"; done
python3 -m py_compile .github/scripts/upload.py && echo "upload.py compiles"
git status --short
```

Then **run a real release.** This is the first execution of the release path in the project's history and it is the only way to confirm Phase A.

```bash
git push origin fix/ci-rewrite
# then, from a dev-branch commit:
git tag v1.12.99 && git push origin v1.12.99
```

Watch it:
```bash
gh run list --branch fix/ci-rewrite --limit 3 --json databaseId,name,conclusion,url
gh pr view 118 --json statusCheckRollup
```

Expected, in order: `create-release` runs, a **draft** release appears with the APK attached, the Telegram message arrives in the `prerelease` topic with rendered bold text rather than literal tags, and deleting that draft is a valid no-op recovery.

**If the run fails, stop and diagnose before Phase B.** Do not batch a second unverified change into an unverified pipeline.

---

# Phase B — Restore APK compression

The Telegram ceiling is 50 MB. `upload.py` detects and warns; nothing mitigates. A `pr` or `debug` APK is unminified and uncompressed, and the old pipeline measured **73 MB**, so both Telegram topics would go silent.

## Task B1: Restore `zip.sh` with a size check

**Files:**
- Create: `.github/scripts/zip.sh`
- Modify: `.github/scripts/lib.sh` (add `require_command`)
- Test: `.github/scripts/test/zip_test.sh`

**Interfaces:**
- Consumes: `lib.sh` (`require_env`, `require_command`, `note`, `step`, `die`); one argument, the absolute path to an `.apk`
- Produces: prints the absolute path of the created archive to stdout **as the last line**. Progress lines precede it, so the caller must read the last line (`| tail -1`). Writes nothing to `$GITHUB_OUTPUT` — the caller reads stdout.

**Design, carried forward from the deleted `origin/main:.github/scripts/zip.sh`:** `7z a -t7z -mx=9` above 15 MB, `zip -j -9` at or below. Its own benchmark: 73 MB debug APK → **13 MB in 12 s**, versus 20 MB from `zip -9`. Both variants remove the target first, and both use flat-storage flags (`-w` for 7z, `-j` for zip) so the archive holds the APK directly rather than a path.

**What changes from the old script:** it assumed compression always sufficed and never re-checked. This one verifies, and fails loudly if the result is still over the limit, so the problem surfaces at build time rather than as a silently skipped notification.

Ruling: the contract is **"the path is the last stdout line"**, not "one line". `step` and `note` both print to stdout and `step` emits a leading blank line, so a real run is four lines: `""`, `==> Compressing …`, `::notice::Archive is …`, then the path. A one-line assertion is unsatisfiable without re-plumbing the helpers onto stderr, which would move `step`/`note` output out of every other script that sources `lib.sh` — far more change than this plan should make for a test bug. `build.sh` already reads the stream with `| tail -1`, so asserting the last line asserts the contract that is actually consumed. Cost if wrong: a future change that appends something *after* the path line would still pass here while breaking `| tail -1` consumers — so the test pins the path to the final position, not merely to "being present".

Ruling: `require_env 1` is **deleted**, not repaired. `${!name}` with `name=1` is bash's indirect expansion, and the only binding called `1` in scope is `zip.sh`'s own first positional parameter, so the "check" was always `[ -n "1" ]` — a no-op that also passes with no argument at all, where the script then dies on `$1: unbound variable`. An arity check is not an environment check, so it is replaced with a real one, and the no-argument case is covered in the test rather than left to a dead line. Cost if wrong: none; a direct `[ "$#" -ge 1 ]` cannot be fooled by a variable named `1`.

- [ ] **Step 1: Write the failing test**

Create `.github/scripts/test/zip_test.sh`:

```bash
#!/usr/bin/env bash
# Test zip.sh: picks a tool by size, produces a flat archive, and fails
# loudly rather than silently returning an oversized archive.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
source .github/scripts/lib.sh

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n     %s\n' "$1" "$2"; FAILURES=$((FAILURES+1)); }

command -v zip >/dev/null 2>&1 || { echo "SKIP: zip not installed"; exit 0; }
command -v 7z  >/dev/null 2>&1 || note "7z absent; the large-size case will be skipped"

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

# --- small file: zip -j -9 path ---
mkdir -p "$scratch/small"
head -c 200000 /dev/urandom > "$scratch/small/Hail-79.apk"
out="$(bash .github/scripts/zip.sh "$scratch/small/Hail-79.apk" | tail -1)"

case "$out" in
  *.zip) pass "small input produces a .zip" ;;
  *)     fail "small input produces a .zip" "got: $out" ;;
esac
[ -f "$out" ] && pass "archive exists" || fail "archive exists" "$out missing"

if [ -f "$out" ]; then
  members="$(unzip -Z1 "$out")"
  [ "$members" = "Hail-79.apk" ] \
    && pass "archive is flat: holds the APK directly, not a path" \
    || fail "archive is flat" "members: $members"
  unzip -p "$out" Hail-79.apk | cmp -s - "$scratch/small/Hail-79.apk" \
    && pass "archived bytes are identical to the input" \
    || fail "archived bytes" "content differs after round trip"
fi

# --- the contract build.sh depends on: the path is the LAST stdout line ---
# Not "exactly one line". step and note both print to stdout and step emits a
# leading blank line, so a small run is 4 lines: "", "==> ...", "::notice::...",
# path. `build.sh` already reads it with `| tail -1`; assert that, not a count.
stream="$(bash .github/scripts/zip.sh "$scratch/small/Hail-79.apk")"
last="$(printf '%s\n' "$stream" | tail -1)"
[ "$last" = "$out" ] \
  && pass "the archive path is the last stdout line (what build.sh reads)" \
  || fail "the archive path is the last stdout line" "last line was: $last"
case "$stream" in
  *"::error::"*) fail "no ::error:: on the success path" "got: $stream" ;;
  *)             pass "no ::error:: on the success path" ;;
esac

# --- missing input fails loudly ---
# lib.sh sets `set -e`, and this case EXPECTS a non-zero exit, so the capture is
# the left side of `||`: errexit does not apply there and rc is still the
# command's own status.
out="" && rc=0
out="$(bash .github/scripts/zip.sh "$scratch/nope.apk" 2>&1)" && rc=0 || rc=$?
[ "$rc" -ne 0 ] \
  && pass "missing input exits non-zero" \
  || fail "missing input exits non-zero" "got exit $rc"
case "$out" in
  *"::error::"*) pass "missing input emits a ::error:: annotation" ;;
  *) fail "missing input emits ::error::" "got: $out" ;;
esac

# --- no argument at all fails with a usable message, not an unbound variable ---
out="" && rc=0
out="$(bash .github/scripts/zip.sh 2>&1)" && rc=0 || rc=$?
[ "$rc" -ne 0 ] \
  && pass "no argument exits non-zero" \
  || fail "no argument exits non-zero" "got exit $rc"
case "$out" in
  *"unbound variable"*) fail "no argument reports usage" "got a raw shell error: $out" ;;
  *"::error::"*)        pass "no argument reports usage via ::error::" ;;
  *) fail "no argument reports usage" "got: $out" ;;
esac
[ "$rc" -ne 0 ] \
  && pass "missing input exits non-zero" \
  || fail "missing input exits non-zero" "got exit $rc"
case "$out" in
  *"::error::"*) pass "missing input emits a ::error:: annotation" ;;
  *) fail "missing input emits ::error::" "got: $out" ;;
esac

# --- large case, only where 7z exists ---
if command -v 7z >/dev/null 2>&1; then
  mkdir -p "$scratch/big"
  head -c 20000000 /dev/urandom > "$scratch/big/Hail-80.apk"
  out="$(bash .github/scripts/zip.sh "$scratch/big/Hail-80.apk" | tail -1)"
  case "$out" in
    *.7z) pass "large input produces a .7z" ;;
    *)    fail "large input produces a .7z" "got: $out" ;;
  esac
  mb="$(du -m "$out" | cut -f1)"
  [ "${mb:-99}" -lt 50 ] \
    && pass "compressed result is under 50 MB (${mb}MB)" \
    || fail "compressed under 50 MB" "got ${mb}MB"
else
  note "7z absent: the large-input branch is UNVERIFIED here and must be checked in CI"
fi

printf '\n%s\n' "$([ "$FAILURES" -eq 0 ] && echo 'PASS: all assertions' || echo "FAIL: $FAILURES assertion(s)")"
exit "$FAILURES"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash .github/scripts/test/zip_test.sh`
Expected: **FAIL** — `zip.sh` does not exist.

- [ ] **Step 3: Write minimal implementation**

First add the missing helper to `lib.sh`:
```bash
cat >> .github/scripts/lib.sh <<'SH'

require_command() {
  local name
  for name in "$@"; do
    command -v "$name" >/dev/null 2>&1 \
      || die "Missing required command: ${name}"
  done
}
SH
```

Then create the script:
```bash
cat > .github/scripts/zip.sh <<'SH'
#!/usr/bin/env bash
# Compress an APK so it fits Telegram's 50 MB sendDocument ceiling.
#
# An unminified debug APK measures ~73 MB; 7z -mx=9 brings that to ~13 MB,
# zip -9 to ~20 MB. Both are below the limit, so the tool is chosen by size
# purely to save time on small inputs.
#
# Prints the archive's absolute path to stdout as the LAST line. Progress goes
# to stdout too, so the caller must read the last line (`| tail -1`). Writes
# nothing to $GITHUB_OUTPUT - the caller reads stdout.
#
# The old pipeline shipped archives to the debug and PR topics, so recipients
# already know these need extracting.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# Above this, 7z is meaningfully faster and smaller. Below, zip is instant.
readonly THRESHOLD_MB=15
# Telegram's hard ceiling for a bot upload.
readonly TELEGRAM_LIMIT_MB=50

# A real arity check. `require_env 1` was here before and was a no-op:
# ${!name} with name=1 is bash's indirect expansion, and the only binding
# called "1" in scope is this script's own first positional parameter, so the
# test was always `[ -n "1" ]`. With no argument at all it did not catch
# anything either, and set -u then killed the script with "$1: unbound
# variable" instead of a usable message.
[ "$#" -ge 1 ] || die "Usage: zip.sh <path-to-apk>"
INPUT="$1"

[ -f "$INPUT" ] || die "No such file to compress: ${INPUT}"

apk_mb=$(( $(stat -c%s "$INPUT") / 1024 / 1024 ))
out_dir="$(dirname "$INPUT")"
base="$(basename "$INPUT" .apk)"

if (( apk_mb > THRESHOLD_MB )); then
  require_command 7z
  out="${out_dir}/${base}.7z"
  rm -f "$out"
  step "Compressing ${apk_mb}MB APK with 7z"
  7z a -t7z -mx=9 -w"${out_dir}" "$out" "$INPUT" >/dev/null
else
  require_command zip
  out="${out_dir}/${base}.zip"
  rm -f "$out"
  step "Compressing ${apk_mb}MB APK with zip"
  # -j drops the stored path, so the archive holds the APK directly.
  ( cd "$out_dir" && zip -q -j -9 "$out" "$INPUT" )
fi

[ -f "$out" ] || die "Compression produced no archive at ${out}"

result_mb=$(( $(stat -c%s "$out") / 1024 / 1024 ))
if (( result_mb > TELEGRAM_LIMIT_MB )); then
  # The old script assumed compression always sufficed. Assume nothing.
  die "Archive is still ${result_mb}MB, over Telegram's ${TELEGRAM_LIMIT_MB}MB limit. The upload will be skipped; investigate rather than shipping a silently undelivered artifact."
fi

note "Archive is ${result_mb}MB"
echo "$out"
SH
chmod +x .github/scripts/zip.sh
```

Prove the new `lib.sh` function works both ways:
```bash
bash -n .github/scripts/lib.sh && echo "syntax OK"
bash -c 'source .github/scripts/lib.sh; require_command zip; echo "require_command passes for a real command"'
bash -c 'source .github/scripts/lib.sh; require_command definitely-not-a-command; echo BAD' 2>&1; echo "exit=$?"
```
Expect `::error::Missing required command: definitely-not-a-command` and `exit=1`.

- [ ] **Step 4: Run test to verify it passes**

Run: `bash .github/scripts/test/zip_test.sh`
Expected: `PASS: all assertions`, exit 0. If `7z` is absent locally, the large-input branch is skipped and the test says so; **that branch is then unverified and must be exercised in CI.**

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/zip.sh .github/scripts/lib.sh .github/scripts/test/zip_test.sh
git commit -m "ci: restore APK compression with a post-compression size check

An unminified debug APK measures ~73 MB, over Telegram's 50 MB sendDocument
ceiling, so the pr and debug topics would go silent. The deleted zip.sh
handled this with 7z above 15 MB and zip below; restore that split and add
the size check it lacked, so an archive that still does not fit fails the
build instead of vanishing at upload time."
```

## Task B2: Wire compression into `build.sh`

**Files:**
- Modify: `.github/scripts/build.sh`
- Test: `.github/scripts/test/build_sh_test.sh`

**Interfaces:**
- Consumes: `lib.sh` (`single_match`, `require_env`, `note`, `step`, `die`); `zip.sh`
- Produces: `APK_PATH` in `$GITHUB_OUTPUT` — **the compressed archive for `pr` and `debug`, the raw APK for `release`.** `COMMIT_SUBJECT` and `COMMIT_SHA` unchanged.

**Why in `build.sh` and not a workflow step.** It already owns APK discovery and the output contract. A separate workflow step would have to fight `Rename`'s hardcoded `mv "$APK_PATH" "Hail-<n>.apk"`, which assumes an `.apk`. Confining it to `build.sh` keeps every downstream step assuming one path.

**Release is excluded deliberately.** A minified release APK is a fraction of the size, and a compressed archive as a GitHub release asset would be worse for users.

Ruling: the test opens with the same `command -v zip` skip guard B1 already uses. The debug/release cases run the real `build.sh`, which under B2's implementation runs the real `zip.sh`; with `zip` absent, `zip.sh` calls `require_command zip` and dies, `build.sh` inherits `set -e` and aborts before writing `GITHUB_OUTPUT`, and the test then reports a missing `APK_PATH` — a failure that says nothing about the code under test. The Phase C gate requires the whole set to run on any contributor's machine, so a hard dependency on an external binary has to be stated, not assumed. Cost if wrong: a contributor without `zip` sees `SKIP` instead of a result, which is a loss of local signal but never a false pass; CI installs `zip` per the Global Constraints, so the gate still runs for real there.

Ruling: the argument-list assertions run **after** a fake APK exists, and match against a captured string rather than a pipeline. Previously they ran first, so `single_match '*.apk'` found nothing and `build.sh` exited 1 — and because `lib.sh` sets `-o pipefail`, `build.sh … | grep -q …` reported the *worse* of the two statuses, so the assertion failed while the `GRADLE_ARGS` line sat visible in the stream. Creating the APK first and grepping a variable separates "did Gradle get the right arguments" from "did the script exit cleanly". Cost if wrong: none — `assert_args` still matches the exact expected argument string, so a changed argument list fails just as loudly; it simply stops conflating that with a missing artifact.

- [ ] **Step 1: Write the failing test**

Create `.github/scripts/test/build_sh_test.sh`:

```bash
#!/usr/bin/env bash
# Test that build.sh publishes the archive for pr/debug and the raw APK for
# release, and that the argument list is exactly as specified.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
source .github/scripts/lib.sh

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n     %s\n' "$1" "$2"; FAILURES=$((FAILURES+1)); }

command -v zip >/dev/null 2>&1 || { echo "SKIP: zip not installed (build.sh calls zip.sh for pr/debug)"; exit 0; }

# Replace ./gradlew with a print-only stub. build.sh calls it by explicit
# relative path, so PATH is not consulted and the stub must sit in the tree.
git diff --quiet -- gradlew || { echo "gradlew already modified; refusing to stub"; exit 1; }
mkdir -p .scratch-test
printf '#!/usr/bin/env bash\necho "GRADLE_ARGS: $*"\n' > .scratch-test/gw
chmod +x .scratch-test/gw
cp .github/scripts/build.sh .scratch-test/build.sh.orig
sed -i 's|\./gradlew |./.scratch-test/gw |' .github/scripts/build.sh
trap 'cp .scratch-test/build.sh.orig .github/scripts/build.sh; rm -rf .scratch-test .scratch-out app/build' EXIT

fake_apk() {
  mkdir -p "app/build/outputs/apk/$1"
  head -c 200000 /dev/urandom > "app/build/outputs/apk/$1/app-$1.apk"
}

# Assert on a captured stream, not on `build.sh | grep -q`.
# lib.sh sets `set -o pipefail`, so a pipeline reports the WORST status in it:
# even though grep matches, build.sh's own non-zero exit (single_match finds no
# APK, because the argument assertions used to run before any APK existed)
# failed the pipeline and the assertion reported a false mismatch. Capture the
# output, then grep the variable, so the two concerns stay separate.
assert_args() {
  local name="$1" want="$2"; shift 2
  local stream="" rc=0
  stream="$("$@" 2>&1)" && rc=0 || rc=$?
  case "$stream" in
    *"$want"*) pass "$name argument list is exact" ;;
    *) fail "$name argument list is exact" "mismatch (build.sh exit $rc): $(printf '%s' "$stream" | grep GRADLE_ARGS || printf '%s' "$stream")" ;;
  esac
}

# --- argument lists, unchanged from the reviewed spec ---
# An APK must exist before build.sh can get past single_match.
fake_apk pr
assert_args "pr" \
  "GRADLE_ARGS: --no-daemon --stacktrace :app:assemblePr -PversionName=PR79 -PversionCode=79 -PprNumber=79" \
  env PR_NUMBER=79 bash .github/scripts/build.sh pr

assert_args "debug" \
  "GRADLE_ARGS: --no-daemon --stacktrace :app:assembleDebug" \
  bash .github/scripts/build.sh debug

assert_args "release" \
  "GRADLE_ARGS: --no-daemon --stacktrace :app:assembleRelease -PversionName=1.12.3 -PversionCode=11203" \
  env RELEASE_VERSION_NAME=1.12.3 RELEASE_VERSION_CODE=11203 bash .github/scripts/build.sh release

# --- the contract under test ---
rm -rf app/build
fake_apk debug
mkdir -p .scratch-out
GITHUB_OUTPUT=.scratch-out/gout bash .github/scripts/build.sh debug >/dev/null 2>&1
apk_path="$(sed -n 's/^APK_PATH=//p' .scratch-out/gout)"

case "$apk_path" in
  *.zip|*.7z) pass "debug publishes the compressed archive" ;;
  *) fail "debug publishes the compressed archive" "APK_PATH=$apk_path" ;;
esac
[ -f "$apk_path" ] && pass "the published path exists" || fail "the published path exists" "$apk_path missing"

grep -q '^COMMIT_SUBJECT=' .scratch-out/gout \
  && pass "COMMIT_SUBJECT still written" || fail "COMMIT_SUBJECT still written" "absent"
grep -q '^COMMIT_SHA=' .scratch-out/gout \
  && pass "COMMIT_SHA still written" || fail "COMMIT_SHA still written" "absent"

# --- release stays raw ---
rm -rf .scratch-out app/build && mkdir -p .scratch-out
fake_apk release
GITHUB_OUTPUT=.scratch-out/gout \
  RELEASE_VERSION_NAME=1.12.3 RELEASE_VERSION_CODE=11203 \
  bash .github/scripts/build.sh release >/dev/null 2>&1
apk_path="$(sed -n 's/^APK_PATH=//p' .scratch-out/gout)"
case "$apk_path" in
  *.apk) pass "release publishes the raw APK, uncompressed" ;;
  *)    fail "release publishes the raw APK" "APK_PATH=$apk_path" ;;
esac
case "$apk_path" in
  *.zip|*.7z) fail "release is not compressed" "APK_PATH=$apk_path" ;;
  *)          pass "release is not compressed" ;;
esac

printf '\n%s\n' "$([ "$FAILURES" -eq 0 ] && echo 'PASS: all assertions' || echo "FAIL: $FAILURES assertion(s)")"
exit "$FAILURES"
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash .github/scripts/test/build_sh_test.sh`
Expected: **FAIL** on "debug publishes the compressed archive" — `APK_PATH` is still the raw `.apk`.

- [ ] **Step 3: Write minimal implementation**

In `.github/scripts/build.sh`, replace from `step "Assembling ${TASK}"` to the end of the `GITHUB_OUTPUT` block with:

```bash
step "Assembling ${TASK}"
./gradlew --no-daemon --stacktrace "$TASK" "${EXTRA[@]+"${EXTRA[@]}"}"

APK="$(single_match '*.apk')"
note "Built ${APK}"

# An unminified pr/debug APK measures ~73 MB, over Telegram's 50 MB
# sendDocument ceiling, so the topic notification would be skipped. The
# release APK is minified and a fraction of the size, and a compressed
# archive would be a worse release asset, so it is left raw.
if [ "$1" != "release" ]; then
  APK="$(bash "$(dirname "${BASH_SOURCE[0]}")/zip.sh" "$APK" | tail -1)"
  [ -f "$APK" ] || die "Compression reported ${APK}, which does not exist"
fi

if [ -n "${GITHUB_OUTPUT:-}" ]; then
  printf 'APK_PATH=%s\n' "$APK" >> "$GITHUB_OUTPUT"
  # Single-line values, so GITHUB_OUTPUT needs no heredoc form here.
  printf 'COMMIT_SUBJECT=%s\n' "$(git log -1 --pretty=%s | sed -E 's/[[:space:]]+(Co-authored-by|Signed-off-by|Reviewed-by|acked-by|Tested-by):[[:space:]].*$//')" >> "$GITHUB_OUTPUT"
  printf 'COMMIT_SHA=%s\n' "$(git rev-parse HEAD)" >> "$GITHUB_OUTPUT"
fi
```

- [ ] **Step 4: Run test to verify it passes**

Run: `bash .github/scripts/test/build_sh_test.sh`
Expected: `PASS: all assertions`, exit 0, and the `EXIT` trap restores `gradlew`.

Verify outside the stub:
```bash
git diff --quiet -- gradlew && echo "gradlew restored"
bash -n .github/scripts/build.sh && echo "syntax OK"
```

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/build.sh .github/scripts/test/build_sh_test.sh
git commit -m "ci: publish a compressed archive for pr and debug builds

An unminified debug APK is ~73 MB, over Telegram's 50 MB ceiling, so the pr
and debug topics would go silent. Compress pr and debug in build.sh, where
the APK is already located and the output contract already lives, and leave
release raw: it is minified, and a compressed release asset is worse for
users."
```

## Task B3: Align `pr.yml` and `debug.yml` with the archive contract

**Files:**
- Modify: `.github/workflows/pr.yml` (the `Rename`, `Upload artifact` and `Upload to Telegram` steps)
- Modify: `.github/workflows/debug.yml` (the same three steps — an undeclared consumer of the same contract)
- Modify: `.github/scripts/upload.py` (the oversize warning text)
- Test: `.github/scripts/test/pr_workflow_test.py`

**Interfaces:**
- Consumes: `build.sh`'s `APK_PATH`, now an archive for `pr` **and** `debug`
- Produces: a Telegram message whose filename ends `.zip` or `.7z`, and an Actions artifact matching it — in both workflows

**Three consequences of Task B2 that must be handled in the same change.** In both workflows `Rename` is a hardcoded `mv "$APK_PATH" ….apk`, which renames a `.7z` to `.apk` — a file that is not an APK. Each workflow's `Upload to Telegram` step *also* hardcodes a `.apk` path, so fixing only `Rename` would leave Telegram pointed at a filename that no longer exists. And the oversize warning still recommends `-Pabi=arm64-v8a` as the remedy, which is now wrong advice: the file is already compressed.

Ruling: B3 covers **`pr.yml` and `debug.yml` together**, because `debug.yml` is a real consumer of the `APK_PATH` contract B2 changes and nothing else in the plan touched it — B3 updating only `pr.yml` meant a debug archive was renamed to `HailBug.apk` and published under that name, with no task and no test covering it. Declaring a new consumer here is cheaper than a second task later, and it keeps the contract's blast radius inside the task that creates it. Cost if wrong: B3 is larger than it would otherwise be, and the test now fails if either workflow drifts — so a `debug.yml` change made for an unrelated reason can surface in B3 rather than in the task that caused it. The two workflows stay individually testable because the assertions loop over `(wf, base)` pairs.

Ruling: the per-ABI assertion is narrowed to the **remedy sentence**, and the flag itself is removed from `upload.py` entirely. The intent is that per-ABI splitting stops being the *primary* remedy now that compression is the primary remedy; a bare `"Pabi=arm64-v8a" not in uploader` cannot express that, because the same literal legitimately appears in any text that merely mentions the flag. The old wording — `To fit it, build per ABI:` — is what asserts the old priority, so the replacement text drops that sentence and the test forbids the old phrasing. Cost if wrong: a future message may legitimately re-mention `-Pabi=arm64-v8a` as a *secondary* suggestion and this test will fail on it, which is a false alarm; that is the safer direction, because the alternative is a test that can be satisfied while the message still leads with the wrong remedy.

Ruling: the `Rename` assertion targets the **`mv` and `name=` lines**, not the whole `run` body. The previous whole-body check forbade the substring `.apk` anywhere in the step, which an explanatory comment mentioning `.apk` defeats — the assertion was testing prose. The workflow no longer hardcodes an extension *anywhere* in the step, and the test now checks the lines that actually build the filename, per workflow, against each workflow's own established base name. Cost if wrong: a `.apk` reintroduced only inside a comment passes here, which is the correct outcome — a comment cannot mislabel an artifact.

- [ ] **Step 1: Write the failing test**

Create `.github/scripts/test/pr_workflow_test.py`:

```python
#!/usr/bin/env python3
"""Assert pr.yml's rename and artifact steps match what build.sh now emits.

debug.yml consumes the same APK_PATH contract as pr.yml, so it is covered by
the same assertions here rather than left to a later task.
"""
import pathlib
import sys
import yaml

failures = []


def check(ok: bool, name: str, detail: str = "") -> None:
    print(f"  {'ok  ' if ok else 'FAIL'} {name}" + (f"\n       {detail}" if not ok and detail else ""))
    if not ok:
        failures.append(name)


for wf, base in (("pr", "Hail-${PR_NUMBER}"), ("debug", "HailBug")):
    raw = pathlib.Path(f".github/workflows/{wf}.yml").read_text(encoding="utf-8")
    d = yaml.safe_load(raw)
    steps = d["jobs"]["check"]["steps"]
    by_name = {s.get("name", ""): s for s in steps}

    # The rename step must derive the extension, and must not name one. The
    # assertion targets the mv line: a comment that merely MENTIONS .apk is not
    # a hardcoded extension, and forbidding the substring in the whole body
    # punishes an explanatory comment.
    run = by_name.get("Rename", {}).get("run", "")
    lines = [l.strip() for l in run.splitlines()]
    mv_line = next((l for l in lines if l.startswith("mv ")), "")
    name_line = next((l for l in lines if l.startswith("name=")), "")
    check("${APK_PATH##*.}" in run,
          f"{wf}.yml derives the archive extension from APK_PATH",
          f"run is: {run!r}")
    check(base in name_line,
          f"{wf}.yml rename keeps its established base name",
          f"expected {base!r} in the name line, got: {name_line!r}")
    check(".apk" not in mv_line and ".apk" not in name_line,
          f"{wf}.yml rename does not hardcode a .apk extension",
          f"mv is {mv_line!r} and name is {name_line!r}")
    # The derived name is published, so the steps below read it instead of
    # re-deriving (or hardcoding) the extension a second time.
    check("GITHUB_OUTPUT" in run and "steps.rename.outputs.artifact" in raw,
          f"{wf}.yml publishes the renamed path for the steps below",
          "the artifact and Telegram steps must not re-guess the extension")

    artifact = by_name.get("Upload artifact", {})
    check(artifact.get("with", {}).get("if-no-files-found") == "error",
          f"{wf}.yml artifact upload fails when the file is missing")

    tg = by_name.get("Upload to Telegram", {})
    check(tg.get("env", {}).get("APK_PATH") == "${{ steps.rename.outputs.artifact }}",
          f"{wf}.yml Telegram step reads the renamed artifact",
          f"APK_PATH is: {tg.get('env', {}).get('APK_PATH')!r}")
    check(".apk" not in str(tg.get("env", {}).get("APK_PATH", "")),
          f"{wf}.yml Telegram step does not hardcode a .apk path")

    # Global constraints still hold, for both workflows.
    check(d["permissions"] == {"contents": "read"}, f"{wf}.yml holds contents: read")
    for job, spec in d["jobs"].items():
        for s in spec["steps"]:
            check("${{" not in s.get("run", ""), f"{wf}.yml: no expression in run body: {s.get('name', job)}")
    check("KEYSTORE" not in raw, f"{wf}.yml references no signing secret")
    check("accept-android-sdk-licenses: true" in raw,
          f"{wf}.yml uses a bare boolean for the license input")

check("platforms;android-37.0" in pathlib.Path(".github/workflows/pr.yml").read_text(encoding="utf-8"),
      "pr.yml requests the existing platform package")

uploader = pathlib.Path(".github/scripts/upload.py").read_text(encoding="utf-8")
# Assert the remedy SENTENCE is gone, not the flag alone. build.sh now
# compresses, so per-ABI splitting is no longer the primary remedy - but the
# old text recommended it outright ("To fit it, build per ABI"). A bare
# `"Pabi=arm64-v8a" not in uploader` test cannot tell the two apart and would
# also fail any future text that merely MENTIONS the flag.
check("build per ABI" not in uploader and "-Pabi" not in uploader,
      "upload.py no longer recommends per-ABI splitting as the remedy",
      "APKs are compressed now; the remedy text is wrong")

print(f"\n{'PASS: all assertions' if not failures else 'FAIL: ' + str(len(failures)) + ' assertion(s)'}")
sys.exit(len(failures))
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 .github/scripts/test/pr_workflow_test.py`
Expected: **FAIL** on the rename, Telegram-path and remedy assertions for **both** `pr.yml` and `debug.yml`.

- [ ] **Step 3: Write minimal implementation**

In `.github/workflows/pr.yml`, replace the `Rename`, `Upload artifact` and `Upload to Telegram` steps with:

```yaml
      - name: Rename
        id: rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
          PR_NUMBER: ${{ github.event.pull_request.number }}
        run: |
          # build.sh compresses pr builds, so what it produced is an archive,
          # not an APK. Take the extension from the path instead of naming one,
          # and publish the result so the two steps below never have to guess.
          ext="${APK_PATH##*.}"
          name="Hail-${PR_NUMBER}.${ext}"
          mv "$APK_PATH" "$name"
          echo "artifact=${GITHUB_WORKSPACE}/${name}" >> "$GITHUB_OUTPUT"

      - name: Upload artifact
        uses: actions/upload-artifact@v7
        with:
          name: apk-pr${{ github.event.pull_request.number }}
          path: ${{ steps.rename.outputs.artifact }}
          if-no-files-found: error

      - name: Upload to Telegram
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ steps.rename.outputs.artifact }}
          PR_NUMBER: ${{ github.event.pull_request.number }}
          PR_TITLE: ${{ github.event.pull_request.title }}
          TG_CHANNEL: pr
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
```

In `.github/workflows/debug.yml`, replace the same three steps with:

```yaml
      - name: Rename
        id: rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
        run: |
          # build.sh compresses debug builds, so what it produced is an
          # archive, not an APK. Take the extension from the path instead of
          # naming one, and publish the result so the steps below never guess.
          ext="${APK_PATH##*.}"
          name="HailBug.${ext}"
          mv "$APK_PATH" "$name"
          echo "artifact=${GITHUB_WORKSPACE}/${name}" >> "$GITHUB_OUTPUT"

      - name: Upload artifact
        uses: actions/upload-artifact@v7
        with:
          name: apk-debug
          path: ${{ steps.rename.outputs.artifact }}
          if-no-files-found: error

      - name: Upload to Telegram
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ steps.rename.outputs.artifact }}
          GITHUB_SHA: ${{ steps.build.outputs.COMMIT_SHA }}
          COMMIT_SUBJECT: ${{ steps.build.outputs.COMMIT_SUBJECT }}
          TG_CHANNEL: debug
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
```

Leave the `if: success()` on both Telegram steps exactly as it is — Task C3 changes those conditions, and this task only replaces the `APK_PATH` value inside the same `env:` block.

In `.github/scripts/upload.py`, replace the oversize warning text with:

```python
        print(
            f"::warning::APK is {size / 1024 / 1024:.1f} MB, over Telegram's "
            f"50 MB sendDocument limit even after compression. Skipping the "
            f"upload - the artifact is still available from the GitHub release. "
            f"Compression is already the primary remedy and it did not get this "
            f"under the limit, so the size itself is the problem."
        )
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 .github/scripts/test/pr_workflow_test.py`
Expected: `PASS: all assertions`, exit 0.

Re-verify the uploader:
```bash
python3 -m py_compile .github/scripts/upload.py && echo "compiles OK"
python3 - <<'PY'
import importlib.util
spec = importlib.util.spec_from_file_location("u", ".github/scripts/upload.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
for tag, needle in (("v1.11.3", "<b>Highlights</b>"), ("v1.11.4", "See full changelog")):
    out = m.release_caption(tag)
    assert needle in out, (tag, out)
print("PASS: captions unchanged")
PY
```

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/pr.yml .github/workflows/debug.yml .github/scripts/upload.py .github/scripts/test/pr_workflow_test.py
git commit -m "fix(ci): derive the artifact extension in pr and debug

build.sh now compresses pr and debug builds, so what Rename received is an
archive, not an APK. Both workflows hardcoded .apk in Rename, in the artifact
path, and in the Telegram step's APK_PATH, so fixing only the mv would have
left Telegram pointed at a filename that no longer existed. Derive the
extension from the path once and publish it as a step output, and stop
recommending per-ABI splitting from the oversize warning now that compression
is the primary remedy."
```

## Phase B gate

```bash
bash .github/scripts/test/zip_test.sh
bash .github/scripts/test/build_sh_test.sh
python3 .github/scripts/test/pr_workflow_test.py
for f in .github/scripts/*.sh; do bash -n "$f" || echo "PARSE FAIL $f"; done
python3 -m py_compile .github/scripts/upload.py && echo "upload.py compiles"
```

Then open a throwaway PR and confirm end to end: the Actions artifact is an archive, the Telegram message carries a file that extracts to a working `Hail-79.pr.apk`, and the caption renders bold rather than literal tags.

**If `7z` was absent locally, the large-input branch is unverified.** Confirm the compression step's log line on the real runner shows a result under 50 MB before trusting it.

---

# Phase C — The remaining WARNINGs

All robustness. None changes the happy path, and all were shaped by Phase B.

## Task C1: Fail cleanly when the APK is missing

**Background — [118-5] WARNING.** `Path(path).stat()` is unguarded. `release.yml` runs the upload step with `if: always()`, so any earlier build or rename failure lands here and the log shows a Python traceback instead of an actionable `::error::`. Every other failure mode in the script exits cleanly.

Ruling: the "replace from" block below includes the intervening `channel = required("TG_CHANNEL")` line, because the two originally quoted lines are **not contiguous** in `upload.py` — that call sits between them. A literal find-and-replace on the shorter block fails, and an implementer splicing around it is one keystroke from deleting the line. Cost if wrong: none; the wider anchor is still unique in `main()`.

- [ ] **Step 1: Write the failing test**

```bash
cat > .github/scripts/test/upload_missing_test.py <<'PY'
#!/usr/bin/env python3
"""Assert upload.py reports a missing APK cleanly rather than tracing back."""
import os
import pathlib
import subprocess
import sys

script = pathlib.Path(".github/scripts/upload.py")

env = dict(os.environ, APK_PATH="/nonexistent/Hail-99.apk", TG_TOKEN="t",
           TG_GROUP="g", TG_CHANNEL="pr", PR_NUMBER="99", PR_TITLE="t")
r = subprocess.run([sys.executable, str(script)], env=env,
                   capture_output=True, text=True)

out = r.stdout + r.stderr
print(out.strip())

ok = True
if "Traceback" in out:
    print("  FAIL no traceback"); ok = False
else:
    print("  ok   no traceback")
if "::error::" not in out:
    print("  FAIL emits a ::error:: annotation"); ok = False
else:
    print("  ok   emits a ::error:: annotation")
if r.returncode == 0:
    print("  FAIL exits non-zero"); ok = False
else:
    print("  ok   exits non-zero")

sys.exit(0 if ok else 1)
PY
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 .github/scripts/test/upload_missing_test.py`
Expected: **FAIL** — output contains `Traceback` and no `::error::`.

- [ ] **Step 3: Write minimal implementation**

In `.github/scripts/upload.py`, in `main()`, replace:

```python
    path = required("APK_PATH")
    channel = required("TG_CHANNEL")
    size = pathlib.Path(path).stat().st_size
```

with:

```python
    path = required("APK_PATH")
    channel = required("TG_CHANNEL")
    artifact = pathlib.Path(path)
    if not artifact.is_file():
        # release.yml runs this step with `if: always()`, so an earlier build
        # or rename failure lands here. Report it as a normal CI error rather
        # than letting Path.stat() raise a traceback.
        sys.exit(f"::error::APK not found at {path}. An earlier step failed; see the log above.")
    size = artifact.stat().st_size
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 .github/scripts/test/upload_missing_test.py`
Expected: exit 0, `::error::APK not found at ...`, no traceback.

Re-verify the existing assertions still hold — see the list in Task A2 Step 5.
```bash
python3 -m py_compile .github/scripts/upload.py && echo "compiles OK"
```

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/upload.py .github/scripts/test/upload_missing_test.py
git commit -m "fix(ci): report a missing APK as a CI error, not a traceback

release.yml runs the upload with if: always(), so any earlier build or
rename failure reached Path.stat() and produced a raw Python traceback
instead of an actionable message. Every other failure mode in this script
exits cleanly; make this one match."
```

## Task C2: Scope the APK search to the build output directory

**Background — [118-6] WARNING.** `single_match '*.apk'` runs `find` from the repository root. It works today only because a clean CI checkout contains exactly one APK. The moment a second exists — an `-Pabi` split, a leftover from an earlier task in the same job, a test fixture — `-print -quit` takes whichever the traversal reaches first, and the wrong APK gets renamed, uploaded and published as the release.

Ruling: the test `source`s `lib.sh` by **absolute path** and keeps its `cd /` probe. The probe is the point — it proves the helper is scoped rather than merely positioned — but `repo_root_path` resolves `dirname "${BASH_SOURCE[0]}"`, and with a relative source path that `cd` failed from `/`, the root came back empty, `find` errored and `die` fired. The test therefore failed before *and* after the fix, measuring its own `cd` instead of the helper. Making the source path absolute keeps the probe and makes it meaningful. Cost if wrong: the helper could still resolve its root from a relative path in some future caller; nothing here would catch that. The added explicit `rc` check is what covers it — a helper that dies from `/` now fails the test as a helper failure rather than as a silent empty result.

Ruling: the `EXIT` trap removes `src/test/resources` **only if the test created it**. The unconditional `rm -rf app/build src/test/resources` is inert today because the repository has no such directory, but the test is a committed fixture that will outlive this plan, and the day real fixtures land under `src/test/resources` it would delete them. Cost if wrong: one extra `had_resources` flag in a test; no behaviour change while the directory is absent.

- [ ] **Step 1: Write the failing test**

```bash
cat > .github/scripts/test/single_match_test.sh <<'SH'
#!/usr/bin/env bash
# Test that single_match scopes its search to the build output directory, so a
# second APK elsewhere in the tree can never be selected.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
# Source by ABSOLUTE path. lib.sh derives its root from ${BASH_SOURCE[0]}, and
# the test deliberately runs the helper from `/`. A relative source path would
# make that `cd` fail, the root empty, and `die` fire - the test would be
# measuring its own `cd`, not the helper's scoping.
REPO_ROOT="$(pwd)"
source "$REPO_ROOT/.github/scripts/lib.sh"

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n     %s\n' "$1" "$2"; FAILURES=$((FAILURES+1)); }

# Clean up only what this test created. An unconditional `rm -rf
# src/test/resources` would destroy real fixtures the moment any were added.
had_resources=0
[ -d src/test/resources ] && had_resources=1
cleanup() {
  rm -rf app/build
  [ "$had_resources" -eq 1 ] || rm -rf src/test/resources
  return 0
}
trap cleanup EXIT

mkdir -p app/build/outputs/apk/debug app/build/outputs/apk/release src/test/resources
touch app/build/outputs/apk/debug/app-debug.apk
touch app/build/outputs/apk/release/app-release.apk
touch src/test/resources/fixture.apk   # the decoy

# The production helper must not be able to reach the decoy. The helper itself
# must not die here either, so a failing call is a test failure, not an exit.
got="" && rc=0
got="$(cd / && single_match '*.apk')" && rc=0 || rc=$?
if [ "$rc" -ne 0 ]; then
  fail "single_match resolves its root from an unrelated working directory" \
       "helper exited $rc from /: ${got}"
else
  pass "single_match resolves its root from an unrelated working directory"
fi

case "$got" in
  */app/build/outputs/*) pass "single_match only returns a build output" ;;
  *) fail "single_match only returns a build output" "matched $got" ;;
esac

# And it must find the real APK when one exists.
[ -f "$got" ] && pass "the returned path exists" || fail "the returned path exists" "$got missing"

# The scoping must be explicit in lib.sh, not accidental.
if grep -qE 'app/build/outputs' .github/scripts/lib.sh; then
  pass "the search root is the build output directory"
else
  fail "the search root is the build output directory" "no app/build/outputs reference in lib.sh"
fi

printf '\n%s\n' "$([ "$FAILURES" -eq 0 ] && echo 'PASS: all assertions' || echo "FAIL: $FAILURES assertion(s)")"
exit "$FAILURES"
SH
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash .github/scripts/test/single_match_test.sh`
Expected: **FAIL** on "the search root is the build output directory".

- [ ] **Step 3: Write minimal implementation**

In `.github/scripts/lib.sh`, replace `single_match` with a version that searches the build output directory:

```bash
# Locate the single APK produced by a build. Scoped to the build output
# directory: a whole-repository search returns whichever APK the filesystem
# traversal reaches first, so a second APK anywhere in the tree - an -Pabi
# split, a leftover, a test fixture - would ship the wrong one.
single_match() {
  local pattern="$1" found
  local root="$(repo_root_path)/app/build/outputs"
  found="$(find "$root" -name "$pattern" -type f -print -quit 2>/dev/null)"
  [ -n "$found" ] || die "No file matching '$pattern' under ${root}"
  printf '%s' "$found"
}
```

`build.sh` needs no change: it calls `single_match '*.apk'`, which now searches the build tree.

- [ ] **Step 4: Run test to verify it passes**

Run: `bash .github/scripts/test/single_match_test.sh`
Expected: `PASS: all assertions`, exit 0.

Re-verify `build.sh`'s other assertions, since `lib.sh` is shared:
```bash
bash .github/scripts/test/build_sh_test.sh
bash .github/scripts/test/zip_test.sh
bash -n .github/scripts/lib.sh && echo "syntax OK"
```

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/lib.sh .github/scripts/test/single_match_test.sh
git commit -m "fix(ci): scope the APK search to the build output directory

A whole-repository find with -print -quit returns whichever APK the
traversal reaches first. It works only while a clean checkout contains
exactly one; a second APK anywhere in the tree would ship the wrong one as
the release asset."
```

## Task C3: Do not fail fork PRs on a missing Telegram token

**Background — [118-7] WARNING.** `pull_request` does not expose repository secrets to runs from a fork. `TG_TOKEN`/`TG_GROUP` are empty, `upload.py` exits 1 with `Missing required environment variable: TG_TOKEN`, and `continue-on-error: true` is the only thing hiding an error-shaped log line for a perfectly normal external contribution.

- [ ] **Step 1: Write the failing test**

```bash
cat > .github/scripts/test/secret_gate_test.py <<'PY'
#!/usr/bin/env python3
"""Assert the Telegram step is skipped, not failed, when the token is absent."""
import pathlib
import sys

failures = []


def check(ok: bool, name: str, detail: str = "") -> None:
    print(f"  {'ok  ' if ok else 'FAIL'} {name}" + (f"\n       {detail}" if not ok and detail else ""))
    if not ok:
        failures.append(name)


for wf in ("pr", "debug"):
    text = pathlib.Path(f".github/workflows/{wf}.yml").read_text(encoding="utf-8")
    check("secrets.TG_TOKEN != ''" in text,
          f"{wf}.yml gates the Telegram step on the token being present",
          "a fork PR would fail the step instead of skipping it")
    check("continue-on-error: true" in text, f"{wf}.yml keeps the step non-fatal")
    check("KEYSTORE" not in text, f"{wf}.yml references no signing secret")

print(f"\n{'PASS: all assertions' if not failures else 'FAIL: ' + str(len(failures)) + ' assertion(s)'}")
sys.exit(len(failures))
PY
```

- [ ] **Step 2: Run test to verify it fails**

Run: `python3 .github/scripts/test/secret_gate_test.py`
Expected: **FAIL** on the gate assertions for both workflows.

- [ ] **Step 3: Write minimal implementation**

In `pr.yml` and `debug.yml`, change the Telegram step's condition from `if: success()` to:
```yaml
        # A pull_request run from a fork gets no repository secrets, so the
        # token is empty there. Skip rather than failing with a misleading
        # "missing TG_TOKEN" on a perfectly normal external contribution.
        if: success() && secrets.TG_TOKEN != ''
```

- [ ] **Step 4: Run test to verify it passes**

Run: `python3 .github/scripts/test/secret_gate_test.py`
Expected: `PASS: all assertions`, exit 0.

Re-run the per-workflow constraint checks:
```bash
python3 .github/scripts/test/pr_workflow_test.py
python3 - <<'PY'
import yaml
for f in ("pr", "debug", "release"):
    d = yaml.safe_load(open(f".github/workflows/{f}.yml"))
    for job, spec in d["jobs"].items():
        for s in spec["steps"]:
            assert "${{" not in s.get("run", ""), (f, job, s.get("name"))
    want = {"contents": "write"} if f == "release" else {"contents": "read"}
    assert d["permissions"] == want, (f, d["permissions"])
print("PASS: all three workflows satisfy the global constraints")
PY
```

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/pr.yml .github/workflows/debug.yml .github/scripts/test/secret_gate_test.py
git commit -m "fix(ci): skip the Telegram step when no token is available

A pull_request run from a fork receives no repository secrets, so the
step exited 1 with 'Missing required environment variable: TG_TOKEN' and
was hidden only by continue-on-error. Gate the step so a normal external
contribution skips it cleanly."
```

## Phase C gate

```bash
for t in .github/scripts/test/*.sh; do bash "$t" || echo "FAIL: $t"; done
for t in .github/scripts/test/*.py; do python3 "$t" || echo "FAIL: $t"; done
for f in .github/scripts/*.sh; do bash -n "$f" || echo "PARSE FAIL $f"; done
python3 -m py_compile .github/scripts/upload.py && echo "upload.py compiles"
```

Every test in `.github/scripts/test/` must pass, and the whole set must runnable **without** the Android SDK and without a network. That is what makes them useful in review and in a local container.

---

# Phase D — The SUGGESTIONs

## Task D1: Cap `MAJOR` in `version.sh` and drop dead code

**Background — [118-9] and [118-10].** `MINOR` and `PATCH` are capped below 100; `MAJOR` is not. `v215.0.0` already derives 2 150 000, and `MAJOR >= 214749` pushes `VERSION_CODE` past `Int.MAX_VALUE`, where `build.gradle.kts`'s `.toInt()` throws an opaque `NumberFormatException` at configuration time — long after the tag guard passed. Separately, two functions in `lib.sh` are defined and called by nothing: `build_tools_dir` and `warn`.

Ruling: `warn` is removed alongside `build_tools_dir`, not exempted. The test's loop requires *every* `lib.sh` function to have a call site, and `warn` has exactly one occurrence repository-wide — its own definition — so it is dead by the test's own standard, which is the standard the task exists to enforce. Leaving it would mean the test still fails after the fix, for a reason the plan never mentions. Cost if wrong: a future script that wants a `::warning::` line must redefine it; the helper is two characters to re-add, and no current caller wants it.

- [ ] **Step 1: Write the failing test**

```bash
cat > .github/scripts/test/version_bounds_test.sh <<'SH'
#!/usr/bin/env bash
# Test that version.sh refuses a tag whose versionCode would overflow, and
# that lib.sh carries no unreachable functions.
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/../../.."
source .github/scripts/lib.sh

FAILURES=0
pass() { printf '  ok   %s\n' "$1"; }
fail() { printf '  FAIL %s\n     %s\n' "$1" "$2"; FAILURES=$((FAILURES+1)); }

scratch="$(mktemp -d)"
trap 'rm -rf "$scratch"' EXIT

# A tag whose derived code exceeds Int.MAX_VALUE (2147483647) must be
# rejected at the guard, not at .toInt() during Gradle configuration.
# lib.sh sets `set -e`, and this case EXPECTS a non-zero exit, so the capture is
# the left side of `||`: errexit does not apply there and rc is still the
# command's own status.
out="" && rc=0
out="$(GITHUB_OUTPUT="$scratch/o" TAG=v214749.0.0 bash .github/scripts/version.sh 2>&1)" && rc=0 || rc=$?
if [ "$rc" -ne 0 ]; then
  case "$out" in
    *"::error::"*) pass "an overflowing MAJOR is rejected at the guard" ;;
    *) fail "overflowing MAJOR rejected" "got: $out" ;;
  esac
else
  got="$(sed -n 's/^RELEASE_VERSION_CODE=//p' "$scratch/o" 2>/dev/null)"
  fail "an overflowing MAJOR is rejected at the guard" \
       "TAG=v214749.0.0 succeeded with RELEASE_VERSION_CODE=${got:-?}"
fi

# The boundary below the cap must still work.
GITHUB_OUTPUT="$scratch/o2" TAG=v1.12.99 bash .github/scripts/version.sh >/dev/null 2>&1
code="$(sed -n 's/^RELEASE_VERSION_CODE=//p' "$scratch/o2" 2>/dev/null)"
[ "$code" = "11299" ] && pass "an ordinary tag still derives correctly (11299)" \
  || fail "ordinary tag derives" "got ${code:-nothing}"

# Every exported helper must be reachable: more than one occurrence across the
# scripts means at least one call site beyond the definition itself.
for fn in $(grep -oE '^[a-z_]+\(\)' .github/scripts/lib.sh | tr -d '()'); do
  n="$(grep -rhoE "\b$fn\b" .github/scripts/*.sh | wc -l)"
  [ "$n" -gt 1 ] && pass "lib.sh: $fn is used" || fail "lib.sh: $fn is used" "no caller found (n=$n)"
done

printf '\n%s\n' "$([ "$FAILURES" -eq 0 ] && echo 'PASS: all assertions' || echo "FAIL: $FAILURES assertion(s)")"
exit "$FAILURES"
SH
```

- [ ] **Step 2: Run test to verify it fails**

Run: `bash .github/scripts/test/version_bounds_test.sh`
Expected: **FAIL** on the overflow rejection and on `build_tools_dir is used` **and** `warn is used`. Both are dead: each has exactly one occurrence across `.github/scripts/*.sh`, its own definition.

- [ ] **Step 3: Write minimal implementation**

In `.github/scripts/version.sh`, after the minor/patch check, add:

```bash
# Android's versionCode is a signed 32-bit int, capped at 2147483647, and
# build.gradle.kts parses this with .toInt(). Without a bound here a large
# MAJOR passes the tag guard and then throws an opaque NumberFormatException
# at configuration time.
readonly INT_MAX=2147483647
if (( VERSION_CODE > INT_MAX )); then
  die "Tag ${TAG} derives versionCode ${VERSION_CODE}, over Android's ${INT_MAX} limit. The scheme is MAJOR*10000 + MINOR*100 + PATCH and cannot express this tag."
fi
```

Remove **both** dead functions from `lib.sh` — `build_tools_dir` and `warn`. Each has no caller, and an unused `ls -d`/`sort -V` pipeline still has to pass shellcheck.

- [ ] **Step 4: Run test to verify it passes**

Run: `bash .github/scripts/test/version_bounds_test.sh`
Expected: `PASS: all assertions`, exit 0.

Re-verify the tag arithmetic, which several tasks now depend on:
```bash
for t in v1.12.0 v1.12.3 v1.13.0 v1.9.99; do
  s="$(mktemp)"
  GITHUB_OUTPUT="$s" TAG="$t" bash .github/scripts/version.sh >/dev/null 2>&1
  printf '%-9s %s\n' "$t" "$(grep VERSION_ "$s" | tr '\n' ' ')"
  rm -f "$s"
done
bash -n .github/scripts/lib.sh .github/scripts/version.sh && echo "syntax OK"
```

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/version.sh .github/scripts/lib.sh .github/scripts/test/version_bounds_test.sh
git commit -m "fix(ci): bound the derived versionCode and drop unused helpers

MAJOR was uncapped, so a tag past v214749.0.0 derived a versionCode over
Int.MAX_VALUE and failed later as an opaque NumberFormatException inside
Gradle configuration. Reject it at the guard, where the message can name
the tag. Also remove build_tools_dir and warn, neither of which anything
calls."
```

## Phase D gate

```bash
for t in .github/scripts/test/*.sh; do bash "$t" || echo "FAIL: $t"; done
for t in .github/scripts/test/*.py; do python3 "$t" || echo "FAIL: $t"; done
for f in .github/scripts/*.sh; do bash -n "$f" || echo "PARSE FAIL $f"; done
python3 -m py_compile .github/scripts/upload.py && echo "upload.py compiles"
git status --short
```

---

# Final verification

Run everything, then exercise the pipeline for real. This is the point of the plan.

```bash
# every test, no Android SDK and no network required
for t in .github/scripts/test/*.sh; do bash "$t" || echo "FAIL: $t"; done
for t in .github/scripts/test/*.py; do python3 "$t" || echo "FAIL: $t"; done

# the global constraints, restated as a single check
python3 - <<'PY'
import pathlib, yaml
ok = True
for f in ("pr", "debug", "release"):
    p = pathlib.Path(f".github/workflows/{f}.yml")
    text = p.read_text(encoding="utf-8")
    d = yaml.safe_load(text)
    want = {"contents": "write"} if f == "release" else {"contents": "read"}
    if d["permissions"] != want:
        print(f"FAIL {f}: permissions {d['permissions']}"); ok = False
    for job, spec in d["jobs"].items():
        for s in spec["steps"]:
            if "${{" in s.get("run", ""):
                print(f"FAIL {f}/{job}: expression in run body of {s.get('name')}"); ok = False
    # release.yml is the one workflow that is SUPPOSED to hold the signing
    # secrets - staging the keystore is its job. The constraint reads "pr.yml
    # and debug.yml reference no signing secret", so applying it to release.yml
    # fails a tree that is correct by construction. Check the two that must.
    if f != "release" and "KEYSTORE" in text:
        print(f"FAIL {f}: references a signing secret"); ok = False
    if "accept-android-sdk-licenses: true" not in text:
        print(f"FAIL {f}: license input is not a bare boolean"); ok = False
    if "platforms;android-37.0" not in text:
        print(f"FAIL {f}: wrong platform package"); ok = False
g = pathlib.Path("app/build.gradle.kts").read_text(encoding="utf-8")
if '".pr$it"' not in g:
    print("FAIL gradle: pr applicationId could not link"); ok = False
if "isMinifyEnabled" in g.split('create("pr")')[1].split("release {")[0]:
    print("FAIL gradle: pr build gained minification"); ok = False
print("PASS: every global constraint holds" if ok else "FAIL: constraints violated")
raise SystemExit(0 if ok else 1)
PY
```

Then, in order:

1. **Push and open a throwaway PR.** Confirm the unit tests run, the artifact is a `.zip`/`.7z`, and the Telegram message arrives with **rendered bold text**, not literal tags.
2. **Confirm the compression step's log shows a result under 50 MB.** If `7z` was absent locally, this is the first execution of that branch.
3. **Tag a throwaway release** on `dev`, e.g. `v1.12.99`. Confirm a **draft** release appears with the APK attached, the Telegram message lands in the `prerelease` topic, and deleting the draft is a valid recovery.
4. **Merge #118.**
5. **Then, separately:** fix `HBackup.kt:47-50` (3 of 19 unit tests fail on it — that is PR #109's territory, deliberately excluded here), close #112 and #79, rebase the four DIRTY dependabot PRs, and take #117.

## Self-review

**Spec coverage.** [118-1] → A1. [118-2] → A2. [118-3] → A2 Step 4. [118-4] → A3. [118-5] → C1. [118-6] → C2. [118-7] → C3. [118-8] is moot — `plans/2026-09-27-ci-redesign.md` was removed from the branch, so there is nothing to split. [118-9] and [118-10] → D1. The zip.sh summary-only finding → B1. The build.yml pre-flight summary-only finding is **not** actioned: each tag is unique, so a re-dispatch of a published tag cannot happen, and the 470-line pre-flight was not worth restoring. All four review WARNINGs and all eight SUGGESTIONs are accounted for.

**Placeholder scan.** No TBD or TODO. Two known gaps, both reported rather than hidden: the `7z` large-input branch is untestable where `7z` is absent, and the Gradle script is verified by parser assertion rather than by a compile, since no Android SDK is available locally.

**Type consistency.** `require_command` is introduced in B1 and used only there. `single_match` gains an explicit build-output search root in C2, so `build.sh`'s existing call keeps working. `APK_PATH` changes meaning in B2 — an archive for `pr`/`debug`, raw for `release` — and every consumer is updated in B2 and B3. That includes all three consumers in `pr.yml` and all three in `debug.yml`: `Rename`, `Upload artifact` and `Upload to Telegram`. `release.yml` keeps its hardcoded `.apk` in both places, which stays correct because B2 excludes `release` from compression. `COMMIT_SUBJECT` and `COMMIT_SHA` are unchanged in name and format. Test filenames are unique across phases.

**Consumers of the `APK_PATH` contract, enumerated.** B2 changes the meaning of `APK_PATH`: an archive for `pr` and `debug`, a raw `.apk` for `release`. The consumers are `pr.yml` `Rename`, `pr.yml` `Upload to Telegram`, `debug.yml` `Rename`, `debug.yml` `Upload to Telegram`, and `release.yml` `Rename` + `Upload to Telegram`. B3 updates the first four and its test asserts on all four; `release.yml` is deliberately unchanged and the test asserts the exclusion holds. Both workflows derive the extension once in `Rename` and publish it as a step output, so neither the artifact path nor the Telegram step re-derives it.
