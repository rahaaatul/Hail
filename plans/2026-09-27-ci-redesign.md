# CI/CD Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the 7 shell scripts and the 205-line `build.yml` with three entry-point workflows (PR, Debug, Release) that run unit tests, build signed APKs, deliver them to Telegram, and publish tagged releases as drafts.

**Architecture:** Three workflow files hold triggers and orchestration only. Every action with a quote, a `$`, or a `2>/dev/null` moves into a script under `.github/scripts/`. A release is triggered by pushing a `v*` tag; whether that tag is a release or a pre-release is decided by asking whether the tagged commit is reachable from `origin/main`. Every release is created as a draft and published by hand.

**Tech Stack:** GitHub Actions, Gradle 9.7.1, AGP 9.4.0, Kotlin 2.4.20, JDK 26, Python 3 (stdlib only), `softprops/action-gh-release@v3`, Telegram Bot API.

**Spec:** This document. The design decisions and their rationale are in "Decisions" below and are normative.

---

## Global Constraints

- **JDK is 26.** Pinned once in `.github/.java-version`, consumed by every workflow via `java-version-file`. Never hardcode a JDK version in a workflow.
- **compileSdk 37, targetSdk 37, minSdk 24, AGP 9.4.0, Kotlin 2.4.20, Gradle 9.7.1.** Do not change any of these.
- **Preserve `testOptions { unitTests.all { it.jvmArgs("-Dnet.bytebuddy.experimental=true") } }`.** It is what makes MockK work on JDK 26. Removing it breaks every test.
- **The release signing key is read only by `release.yml`.** `pr.yml` and `debug.yml` must never reference `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, or `KEYSTORE_ALIAS_PASSWORD`.
- **The test keystore is committed** at `.github/debug.keystore` (PKCS12, alias `HailBug`, store and key password `HailBug`). It may only sign `com.aistra.hail.pr.<n>` and `com.aistra.hail.debug`.
- **Version line:** pre-releases increment the patch on `dev` (`v1.12.1`, `v1.12.2`, `v1.12.3`); releases are always patch-zero on `main` (`v1.13.0`, `v1.14.0`).
- **YAML files contain no inline shell beyond a single `run:` line invoking a script**, and no `${{ }}` inside a `run:` body. All values reach scripts via `env:`.
- **Telegram topics live in `.github/telegram.json`,** never in workflow expressions.
- **Every `fetch_depth: 0` checkout** must come before any `git tag` / `git merge-base` use.
- **Nothing in this plan is executed during implementation.** Tasks 1 and 2 are verified by a human running commands; Tasks 3+ are verified by CI.

---

## Decisions

These were decided deliberately. Do not relitigate them during implementation; if one looks wrong, flag it rather than changing it.

1. **A build type with no `signingConfig` produces an `*-unsigned.apk`, never a debug-signed APK.** The previous code silently fell back to debug signing, producing a "release" that could not be verified. Assignment is now conditional on the properties file existing.

2. **The test keystore is committed, not secret.** It can only sign app IDs nobody trusts, so committing it grants nothing, and committing is what makes every PR and debug build carry the same signature. That is what lets a rebuild install over its own previous build instead of demanding an uninstall.

3. **No `environment:` gate on the publish job.** The gate belongs on the artifact being public. The draft already is that gate, and a second approval click buys nothing. Mihon ships this way.

4. **The expected signer is a tracked file, not a repository variable.** Changing which signer is acceptable should require a code review; a variable changes invisibly.

5. **`versionCode` is derived from the tag, not maintained.** The first release moves from 43 to ~11200. That discontinuity is one-time and deliberate. Do not reintroduce a counter.

6. **A 50 MB Telegram upload failure degrades, it does not fail.** The APK is already on the GitHub draft, so an oversized upload costs the notification, not the release.

---

## File Structure

| File | Responsibility |
|---|---|
| `.github/.java-version` | Single source of truth for the JDK major version |
| `.github/telegram.json` | Chat routing: channel name to Telegram topic id |
| `.github/signer.sha256` | Expected release certificate fingerprint |
| `.github/debug.keystore` | Committed PKCS12 test key (already pushed) |
| `.github/scripts/lib.sh` | Sourced helpers: `step`, `note`, `warn`, `die`, `require_env`, `repo_root`, `build_tools_dir`, `single_match` |
| `.github/scripts/test.sh` | Runs the JVM unit tests |
| `.github/scripts/build.sh` | Assembles `pr`, `debug`, or `release` with the right `-P` overrides |
| `.github/scripts/version.sh` | Derives `versionName` and `versionCode` from a tag, refuses to go backwards |
| `.github/scripts/signing.sh` | Materialises the release `signing.properties` from secrets into `RUNNER_TEMP` |
| `.github/scripts/verify-signer.sh` | Proves the APK carries the expected certificate |
| `.github/scripts/notify.py` | Builds the Telegram caption and uploads the APK |
| `.github/workflows/pr.yml` | Trigger: `pull_request`. Tests, builds, delivers. Read-only, no signing secrets. |
| `.github/workflows/debug.yml` | Trigger: `workflow_dispatch` on any ref. Tests, builds, delivers. Read-only. |
| `.github/workflows/release.yml` | Trigger: `push` tag `v*`. Signs, verifies, publishes a draft. |
| `app/build.gradle.kts` | Build types, signing configs, version overrides |

Each script does one thing. `lib.sh` is the only shared file and is sourced, never executed.

---

## Out of Scope

- **PR #109's backup/restore test suite.** Separate plan; see the note at the end.
- **Adding `testDebugUnitTest` to CI as a required check.** Deliberately not a required check until it has been green once.
- **Android Lint as a gate.** Not in this plan.
- **Deleting `build.yml` and the existing scripts.** Task 9, and only after Task 8 is green.

---

## Before You Start

```bash
git status --short
git ls-files .claude/ .toolchain/ .gradle-home/
```

If `.claude/worktrees` appears as tracked gitlinks, untrack them and add `/.claude/worktrees/` to `.gitignore`. Those objects do not exist on the remote, so the push succeeds but every clone gets a broken submodule pointer.

Confirm the toolchain is gone:

```bash
git ls-files | grep -E '^\.(toolchain|gradle-home)/' | head
du -sh .toolchain .gradle-home 2>/dev/null
```

If either path is tracked, `git rm -r --cached` it and gitignore it. GitHub rejects pushes containing files over 100 MB.

Confirm the test keystore is present before Task 1:

```bash
ls -la .github/debug.keystore
keytool -list -v -keystore .github/debug.keystore -storepass HailBug
```

Expected: `Keystore type: PKCS12`, one entry with `Alias name: HailBug`, `Entry type: PrivateKeyEntry`. **If the type is JKS, stop** — `storeType = "PKCS12"` in Task 1 will fail and the cause must be reported.

---

## Task 1: Version overrides and signing configs

**Files:**
- Modify: `app/build.gradle.kts:9-16` (delete), `:25-26` (version), `+signingConfigs` (new, above `buildTypes`), `:33-58` (buildTypes)
- Modify: `.gitignore` (append)

**Interfaces:**
- Consumes: nothing from other tasks
- Produces: Gradle properties `-PversionName` (String), `-PversionCode` (Int-parseable String), `-PprNumber` (String). Signing configs named `release` and `test`, both possibly empty. A PR build's applicationId suffix is `.pr.<prNumber>`.

- [ ] **Step 1: Record the pre-change state**

```bash
cp app/build.gradle.kts /tmp/build.gradle.kts.before
git rev-parse --short HEAD
```

Expected: HEAD printed, backup created. If `build.gradle.kts` differs from what you expect to change, stop and reconcile against this task's file map.

- [ ] **Step 2: Delete the config-time git calls**

Remove lines 9-16 of `app/build.gradle.kts` — the `commitHash` and `commitSubject` `providers.exec` blocks. `commitSubject` is already unused, and `commitHash` is used only by the `debug` suffix that Step 4 removes.

Result: the file begins

```kotlin
android {
    val signingProps = file("../signing.properties")

    namespace = "com.aistra.hail"
    compileSdk = 37
```

- [ ] **Step 3: Make the version overridable**

Replace lines 25-26 in `defaultConfig`:

```kotlin
        versionCode = (providers.gradleProperty("versionCode").orNull ?: "43").toInt()
        versionName = providers.gradleProperty("versionName").orNull ?: "1.11.4"
```

Leave the surrounding `applicationId`, `minSdk`, `targetSdk`, and the `ndk { }` block alone.

- [ ] **Step 4: Add the signingConfigs block**

Insert immediately above `buildTypes {`:

```kotlin
    signingConfigs {
        // Release key: only ever materialised by release.yml, from secrets.
        create("release") {
            if (signingProps.exists()) {
                val props = `java.util`.Properties().apply { load(signingProps.reader()) }
                storeFile = file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }

        // Test key: committed, and can only sign com.aistra.hail.pr.<n> and
        // com.aistra.hail.debug, so it grants nothing. Living in the repo is
        // what makes every PR and debug build carry the same signature, so a
        // rebuild installs over its own previous build instead of demanding an
        // uninstall first. PKCS12 protects the key with the store password,
        // so keyPassword must match storePassword.
        create("test") {
            val keystore = rootProject.file(".github/debug.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storeType = "PKCS12"
                storePassword = "HailBug"
                keyAlias = "HailBug"
                keyPassword = "HailBug"
            }
        }
    }
```

- [ ] **Step 5: Rewrite buildTypes**

Replace the whole `buildTypes { }` block with:

```kotlin
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-Debug"
            signingConfig = signingConfigs.getByName("test")
        }
        create("pr") {
            // Each PR installs as its own app: com.aistra.hail.pr.<n>.
            // versionName and versionCode are fully overridden by the workflow,
            // so there is deliberately no versionNameSuffix here.
            applicationIdSuffix =
                providers.gradleProperty("prNumber").orNull?.let { ".pr.$it" } ?: ".pr"
            signingConfig = signingConfigs.getByName("test")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (signingProps.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }
```

`debug` and `pr` point at `test` **unconditionally**. If `.github/debug.keystore` is missing, the `test` config is empty and AGP fails the build rather than silently falling back. That is intentional: a silent fallback is how the previous bug happened. Step 8 verifies the keystore is present.

- [ ] **Step 6: Append the gitignore entries**

```bash
cat >> .gitignore <<'EOF'

*.jks
*.keystore
!.github/debug.keystore
EOF
```

The negation is mandatory. Without it, `*.keystore` excludes the committed test key, GitHub receives no file, and every PR build fails with a missing signing config — while `git status` shows a clean tree, because the file is ignored rather than absent.

Verify:

```bash
git check-ignore -v .github/debug.keystore && echo "PROBLEM: ignored" || echo "OK: tracked"
```

Expected: `OK: tracked`.

- [ ] **Step 7: Add the JDK version file**

```bash
printf '26\n' > .github/.java-version
cat .github/.java-version
```

Expected: `26`.

- [ ] **Step 8: Verify the PR build locally**

```bash
./gradlew :app:assemblePr -PversionName=PR1 -PversionCode=1 -PprNumber=1
```

Expected: `BUILD SUCCESSFUL`.

If it fails with `storeFile was specified but the file does not exist` or a `PKCS12` parse error, the keystore is missing or is not PKCS12. Re-run the `keytool -list` command from "Before You Start" and report the actual output.

- [ ] **Step 9: Verify the overrides reached the APK**

```bash
$ANDROID_HOME/build-tools/*/aapt dump badging app/build/outputs/apk/pr/*.apk | head -2
```

Expected exactly:

```
package: name='com.aistra.hail.pr.1' versionCode='1' versionName='PR1'
```

If `versionCode='43'` the `-P` overrides are not being read. If the package is `com.aistra.hail.pr` with no number, `prNumber` is not reaching the build type. Both fail silently otherwise.

- [ ] **Step 10: Commit**

```bash
git add app/build.gradle.kts .gitignore .github/.java-version
git status --short
git commit -m "build: allow CI to override version and use a committed test key

versionName and versionCode are now settable via -P so a workflow can derive
them from a tag. A release build with no signing.properties is left
unsigned instead of silently falling back to debug signing, which produced
an unverifiable 'release' APK.

pr and debug builds sign with a committed PKCS12 test key so successive
builds of the same PR share a signature and install over each other.

The config-time git rev-parse calls are gone, so the build no longer
requires a git checkout."
```

---

## Task 2: Shared script library

**Files:**
- Create: `.github/scripts/lib.sh`

**Interfaces:**
- Consumes: nothing
- Produces: `step MSG`, `note MSG`, `warn MSG`, `die MSG`, `require_env NAME...`, `repo_root`, `build_tools_dir`, `single_match PATTERN`. Every later script sources this file and relies on `set -euo pipefail` being set here.

- [ ] **Step 1: Create the file**

```bash
mkdir -p .github/scripts
```

```bash
#!/usr/bin/env bash
# Shared helpers. Sourced, never executed.

set -euo pipefail

step() { printf '\n==> %s\n' "$*"; }
note() { printf '::notice::%s\n' "$*"; }
warn() { printf '::warning::%s\n' "$*"; }
die()  { printf '::error::%s\n' "$*" >&2; exit 1; }

require_env() {
  local name
  for name in "$@"; do
    [ -n "${!name:-}" ] || die "Missing required environment variable: ${name}"
  done
}

repo_root() { cd "$(dirname "${BASH_SOURCE[0]}")/../.."; }

build_tools_dir() {
  local dir
  dir="$(ls -d "${ANDROID_HOME:-/opt/android-sdk}"/build-tools/* 2>/dev/null | sort -V | tail -1)"
  [ -n "$dir" ] || die "No Android build-tools found"
  printf '%s' "$dir"
}

single_match() {
  local pattern="$1" found
  found="$(find "$(repo_root)" -path "$(repo_root)/.git" -prune -o -name "$pattern" -type f -print -quit)"
  [ -n "$found" ] || die "No file matching '$pattern' was produced by the build"
  printf '%s' "$found"
}
```

- [ ] **Step 2: Verify it parses and the helpers work**

```bash
chmod +x .github/scripts/lib.sh
bash -n .github/scripts/lib.sh && echo "syntax OK"
```

Expected: `syntax OK`.

```bash
bash -c 'source .github/scripts/lib.sh; step "hello"; note "a notice"; warn "a warning"; echo "root=$(repo_root)"'
```

Expected: the three annotations, then a `root=` line ending in the repository root.

- [ ] **Step 3: Verify `die` and `require_env` fail correctly**

```bash
bash -c 'source .github/scripts/lib.sh; require_env DEFINITELY_NOT_SET; echo "SHOULD NOT REACH"'
echo "exit=$?"
```

Expected: a line `::error::Missing required environment variable: DEFINITELY_NOT_SET` and `exit=1`. If it prints `SHOULD NOT REACH`, `require_env` is broken.

- [ ] **Step 4: Commit**

```bash
git add .github/scripts/lib.sh
git commit -m "ci: add shared script helpers"
```

---

## Task 3: Run the unit tests

**Files:**
- Create: `.github/scripts/test.sh`

**Interfaces:**
- Consumes: `lib.sh`
- Produces: `test.sh` — exits 0 on success, non-zero on failure. Produces the report directory `app/build/reports/tests/testDebugUnitTest` for the workflow to upload.

- [ ] **Step 1: Create the file**

```bash
cat > .github/scripts/test.sh <<'SCRIPT'
#!/usr/bin/env bash
# JVM unit tests. Reports land in app/build/reports/tests/testDebugUnitTest,
# which the workflow uploads with `if: failure()`.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

step "Running unit tests"
./gradlew --no-daemon --stacktrace :app:testDebugUnitTest
note "Unit tests passed"
SCRIPT
chmod +x .github/scripts/test.sh
```

- [ ] **Step 2: Verify it parses**

```bash
bash -n .github/scripts/test.sh && echo "syntax OK"
```

- [ ] **Step 3: Run the tests**

```bash
bash .github/scripts/test.sh
```

**This is the first time this repository's unit tests have ever been executed.** Expect one of three outcomes:

| Outcome | Meaning | Action |
|---|---|---|
| `BUILD SUCCESSFUL` | Tests pass | Continue to Step 4 |
| `not mocked` or `Method ... not mocked` | `testOptions` lacks `isReturnDefaultValues` | See Step 3a |
| Compilation error in `src/test` | The test sources never compiled | Report the first error verbatim. **Do not fix it** — it is out of this task's scope. |

- [ ] **Step 3a: If tests fail with "not mocked"**

Add the missing option to `testOptions` in `app/build.gradle.kts`, inside the existing `android { }` block:

```kotlin
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.all {
            it.jvmArgs("-Dnet.bytebuddy.experimental=true")
        }
    }
```

Do not remove the existing `jvmArgs` line. Re-run Step 3.

- [ ] **Step 4: Confirm the report exists**

```bash
ls -la app/build/reports/tests/testDebugUnitTest/ | head
```

Expected: `index.html` and a `tests/` directory. If the directory is missing, the test task was skipped rather than run — report that.

- [ ] **Step 5: Commit**

```bash
git add .github/scripts/test.sh
git commit -m "ci: add unit test runner script"
```

If Step 3a applied:

```bash
git add app/build.gradle.kts
git commit -m "build: return default values for unmocked framework calls in unit tests"
```

---

## Task 4: Build script

**Files:**
- Create: `.github/scripts/build.sh`

**Interfaces:**
- Consumes: `lib.sh`; environment `PR_NUMBER`, `RELEASE_VERSION_NAME`, `RELEASE_VERSION_CODE`
- Produces: `build.sh {pr|debug|release}` — writes `APK_PATH` to `$GITHUB_OUTPUT`. `GITHUB_OUTPUT` is unset locally, so append to it only when non-empty; the script must still work without it.

- [ ] **Step 1: Create the file**

```bash
cat > .github/scripts/build.sh <<'SCRIPT'
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
fi
SCRIPT
chmod +x .github/scripts/build.sh
```

The `"${EXTRA[@]+"${EXTRA[@]}"}"` form is required: under `set -u`, expanding an empty array unguarded aborts the script, and the `debug` path has no overrides.

- [ ] **Step 2: Verify it parses and rejects a bad argument**

```bash
bash -n .github/scripts/build.sh && echo "syntax OK"
bash .github/scripts/build.sh nonsense; echo "exit=$?"
```

Expected: `syntax OK`, then `::error::Usage: build.sh {pr|debug|release}` and `exit=1`.

- [ ] **Step 3: Verify `pr` requires its environment variable**

```bash
env -u PR_NUMBER bash .github/scripts/build.sh pr; echo "exit=$?"
```

Expected: `::error::Missing required environment variable: PR_NUMBER` and `exit=1`, before any Gradle invocation.

- [ ] **Step 4: Build a PR APK and confirm the output contract**

```bash
PR_NUMBER=77 bash .github/scripts/build.sh pr
```

Expected: `BUILD SUCCESSFUL`, then `::notice::Built <path>`. No error about `GITHUB_OUTPUT` — that is what the `if` in Step 1's script guards.

- [ ] **Step 5: Build a debug APK and confirm the suffix applied**

```bash
bash .github/scripts/build.sh debug
$ANDROID_HOME/build-tools/*/aapt dump badging app/build/outputs/apk/debug/*.apk | head -2
```

Expected: `package: name='com.aistra.hail.debug'`, `versionName='1.11.4-Debug'`, `versionCode='43'`. The `-Debug` suffix comes from the build type, and the version code is unchanged, per the spec.

- [ ] **Step 6: Commit**

```bash
git add .github/scripts/build.sh
git commit -m "ci: add build script with per-build-type version overrides"
```

---

## Task 5: Telegram notifier

**Files:**
- Create: `.github/scripts/notify.py`
- Create: `.github/telegram.json`

**Interfaces:**
- Consumes: `.github/telegram.json` (read via a path relative to the script's own location, so it works from any working directory)
- Produces: `notify.py` — environment `APK_PATH` (required), `TG_TOKEN`, `TG_CHAT_ID`, `TG_CHANNEL`, `BUILD_TITLE` (all required), `PR_NUMBER`, `GITHUB_REF_NAME`, `GITHUB_SHA`, `CI_STATUS` (optional). Exits 0 on success and on oversize; exits 1 on missing configuration.

- [ ] **Step 1: Create the routing config**

```bash
cat > .github/telegram.json <<'JSON'
{
  "topics": {
    "pr": 218,
    "debug": 84,
    "prerelease": 95,
    "release": 85
  }
}
JSON
```

- [ ] **Step 2: Create the notifier**

```bash
cat > .github/scripts/notify.py <<'PY'
#!/usr/bin/env python3
"""Send a built APK to a Telegram topic.

Every dynamic value goes through html.escape. Secrets arrive via the
environment, so nothing is ever interpolated into a shell string. Channel
routing lives in .github/telegram.json so it changes in a reviewable diff.
"""

import html
import json
import mimetypes
import os
import pathlib
import sys
import urllib.request
import uuid

API = "https://api.telegram.org"
API_TIMEOUT = 60
SIZE_LIMIT = 50 * 1024 * 1024  # Telegram bot API sendDocument ceiling


def required(name: str) -> str:
    value = os.environ.get(name, "")
    if not value:
        sys.exit(f"::error::Missing required environment variable: {name}")
    return value


def topic_for(channel: str) -> int:
    config = json.loads(
        (pathlib.Path(__file__).resolve().parent.parent / "telegram.json").read_text()
    )
    topics = config.get("topics", {})
    if channel not in topics:
        sys.exit(f"::error::Unknown channel '{channel}'. Configured: {sorted(topics)}")
    return int(topics[channel])


def send_document(token: str, chat_id: str, topic: int, path: str, caption: str) -> None:
    boundary = uuid.uuid4().hex

    def field(name: str, value: str) -> bytes:
        return (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="{name}"\r\n\r\n'
            f"{value}\r\n"
        ).encode()

    # The filename lands in a header, so it is sanitised rather than trusted.
    safe_name = "".join(
        c for c in os.path.basename(path) if c.isalnum() or c in "-._"
    )
    ctype = mimetypes.guess_type(path)[0] or "application/octet-stream"

    with open(path, "rb") as handle:
        payload = field("chat_id", chat_id)
        payload += field("message_thread_id", str(topic))
        payload += field("caption", caption)
        payload += (
            f"--{boundary}\r\n"
            f'Content-Disposition: form-data; name="document"; filename="{safe_name}"\r\n'
            f"Content-Type: {ctype}\r\n\r\n"
        ).encode() + handle.read() + f"\r\n--{boundary}--\r\n".encode()

    request = urllib.request.Request(
        f"{API}/bot{token}/sendDocument",
        data=payload,
        headers={"Content-Type": f"multipart/form-data; boundary={boundary}"},
    )
    with urllib.request.urlopen(request, timeout=API_TIMEOUT) as response:
        json.load(response)


def caption() -> str:
    lines = [f"<b>{html.escape(required('BUILD_TITLE'))}</b>"]
    for label, key in (("PR", "PR_NUMBER"), ("Branch", "GITHUB_REF_NAME")):
        value = os.environ.get(key, "")
        if value:
            lines.append(f"<b>{label}</b>  {html.escape(value)}")
    sha = os.environ.get("GITHUB_SHA", "")
    if sha:
        lines.append(f"<b>Commit</b>  {html.escape(sha[:7])}")
    status = os.environ.get("CI_STATUS", "")
    if status:
        lines += ["", f"<i>{html.escape(status)}</i>"]
    return "\n".join(lines)


def main() -> None:
    path = required("APK_PATH")
    size = pathlib.Path(path).stat().st_size

    if size > SIZE_LIMIT:
        # The artifact is already on the GitHub draft, so an oversized upload
        # costs the notification, not the release. Warn with the remedy and
        # exit 0 rather than failing the job.
        print(
            f"::warning::APK is {size / 1024 / 1024:.1f} MB, over Telegram's "
            f"50 MB sendDocument limit. Skipping the upload - the APK is still "
            f"available from the GitHub release. To fit it, build per ABI: "
            f"./gradlew :app:assemblePr -Pabi=arm64-v8a"
        )
        return

    send_document(
        required("TG_TOKEN"),
        required("TG_CHAT_ID"),
        topic_for(required("TG_CHANNEL")),
        path,
        caption(),
    )
    print("::notice::APK sent to Telegram")


if __name__ == "__main__":
    main()
PY
chmod +x .github/scripts/notify.py
```

- [ ] **Step 3: Verify it compiles and that missing config fails loudly**

```bash
python3 -m py_compile .github/scripts/notify.py && echo "compiles OK"
env -u TG_TOKEN python3 .github/scripts/notify.py; echo "exit=$?"
```

Expected: `compiles OK`, then `::error::Missing required environment variable: TG_TOKEN` and `exit=1`.

- [ ] **Step 4: Verify escaping actually happens**

This is the security property. The whole reason this is Python rather than shell is that `html.escape` is not hand-written:

```bash
python3 - <<'PY'
import importlib.util, os
os.environ["APK_PATH"] = "x"
os.environ["TG_TOKEN"] = "t"
os.environ["TG_CHAT_ID"] = "c"
os.environ["TG_CHANNEL"] = "pr"
os.environ["BUILD_TITLE"] = '<b>x</b> & "y"'
os.environ["PR_NUMBER"] = "7"
spec = importlib.util.spec_from_file_location("n", ".github/scripts/notify.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
out = m.caption()
print(repr(out))
assert "<b>x</b>" not in out, "raw markup survived"
assert "&amp;" in out and "&lt;b&gt;" in out and "&quot;" in out, "entities missing"
print("PASS: dynamic values are escaped")
PY
```

Expected: a `PASS: dynamic values are escaped` line. If `raw markup survived` appears, escaping is broken — stop and report.

- [ ] **Step 5: Verify an unknown channel fails**

```bash
APK_PATH=/etc/hostname TG_TOKEN=t TG_CHAT_ID=c TG_CHANNEL=nonsense BUILD_TITLE=t \
  python3 .github/scripts/notify.py; echo "exit=$?"
```

Expected: `::error::Unknown channel 'nonsense'. Configured: ['debug', 'pr', 'prerelease', 'release']` and `exit=1`.

- [ ] **Step 6: Verify the oversize path degrades rather than failing**

```bash
python3 - <<'PY'
import os, pathlib, subprocess, tempfile
big = pathlib.Path(tempfile.gettempdir()) / "big.apk"
big.write_bytes(b"\0" * (51 * 1024 * 1024))
env = dict(os.environ, APK_PATH=str(big), TG_TOKEN="t", TG_CHAT_ID="c",
           TG_CHANNEL="pr", BUILD_TITLE="t")
r = subprocess.run(["python3", ".github/scripts/notify.py"], env=env,
                   capture_output=True, text=True)
print("exit", r.returncode)
print(r.stdout.strip()[:120])
big.unlink()
PY
```

Expected: `exit 0` and a `::warning::APK is 52.0 MB` line. If it exits non-zero, the degrade behaviour is broken.

- [ ] **Step 7: Commit**

```bash
git add .github/scripts/notify.py .github/telegram.json
git commit -m "ci: add Telegram notifier with escaped captions and topic routing

html.escape is applied at the point of interpolation rather than by
hand-written substitutions, which is what caused the no-op escaper and the
bash 5.2 patsub_replacement bug. Channel routing lives in a tracked json
file so changing it is a reviewable diff. An upload over Telegram's 50 MB
ceiling warns and exits 0, because the APK is already on the GitHub draft."
```

---

## Task 6: PR workflow

**Files:**
- Create: `.github/workflows/pr.yml`

**Interfaces:**
- Consumes: `.github/scripts/test.sh`, `.github/scripts/build.sh`, `.github/scripts/notify.py`, `.github/.java-version`, `.github/telegram.json`
- Produces: a `PR Check / Test, build, deliver` check on every pull request. Artifact `pr-apk-<n>` is not produced; the APK goes to Telegram. Read-only token, no signing secrets.

- [ ] **Step 1: Create the workflow**

```bash
cat > .github/workflows/pr.yml <<'YAML'
name: PR

on:
  pull_request:

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.event.pull_request.number }}
  cancel-in-progress: true

jobs:
  check:
    name: Test, build, deliver
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v7

      - name: Set up JDK
        uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up Android SDK
        uses: android-actions/setup-android@v4
        with:
          packages: 'platform-tools platforms;android-37 build-tools;37.0.0'
          accept-android-sdk-licenses: yes

      - name: Unit tests
        run: .github/scripts/test.sh

      - name: Upload test report
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: test-report-pr${{ github.event.pull_request.number }}
          path: app/build/reports/tests/testDebugUnitTest
          if-no-files-found: ignore

      - name: Build
        id: build
        env:
          PR_NUMBER: ${{ github.event.pull_request.number }}
        run: .github/scripts/build.sh pr

      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
        run: mv "$APK_PATH" "Hail-${{ github.event.pull_request.number }}.apk"

      - name: Notify
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/Hail-${{ github.event.pull_request.number }}.apk
          BUILD_TITLE: "PR #${{ github.event.pull_request.number }} — ${{ github.event.pull_request.title }}"
          TG_CHANNEL: pr
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_CHAT_ID: ${{ secrets.TG_CHAT_ID }}
        run: python3 .github/scripts/notify.py
YAML
```

- [ ] **Step 2: Verify the YAML parses**

```bash
python3 -c "
import sys
try:
    import yaml
except ImportError:
    sys.exit('PyYAML unavailable — install it or paste the file into a YAML validator')
d = yaml.safe_load(open('.github/workflows/pr.yml'))
assert d['permissions'] == {'contents': 'read'}, d['permissions']
assert 'pull_request' in d[True] or 'pull_request' in d.get('on', {}), list(d)
assert d['jobs']['check']['steps'][0]['uses'].startswith('actions/checkout')
print('PR: structure OK')
"
```

Expected: `PR: structure OK`.

- [ ] **Step 3: Confirm no signing secret appears anywhere in the file**

```bash
grep -n "KEYSTORE" .github/workflows/pr.yml && echo "PROBLEM: signing secret referenced" || echo "OK: no signing secrets"
```

This is the security boundary. `pr.yml` runs code from pull requests; it must not be able to read the release key.

- [ ] **Step 4: Confirm no `${{ }}` appears inside a run: body**

```bash
python3 -c "
import re, yaml
d = yaml.safe_load(open('.github/workflows/pr.yml'))
for s in d['jobs']['check']['steps']:
    if 'run' in s and '\${{' in s['run']:
        print('PROBLEM:', s.get('name'), s['run']); break
else:
    print('OK: no expressions in run blocks')
"
```

Expected: `OK: no expressions in run blocks`. An expression inside `run:` is a shell-injection site, which is exactly the bug PR #111 fixed.

- [ ] **Step 5: Commit**

```bash
git add .github/workflows/pr.yml
git commit -m "ci: add PR workflow that tests, builds and delivers

Permissions are contents: read with no signing secrets, so a pull request
cannot read the release key even from a branch in this repository."
```

- [ ] **Step 6: Open a throwaway PR and verify**

Push a branch with a trivial change, open a PR, and confirm:

| Check | Expected |
|---|---|
| Job name | `Test, build, deliver` |
| Overall result | green |
| Test report step | not triggered |
| Telegram | an APK in the `pr` topic, named `Hail-<n>.apk` |
| Installed package | `com.aistra.hail.pr.<n>` |

- [ ] **Step 7: Push a second commit to the same PR and verify the signature is stable**

This is the only way to know the committed keystore is actually in use rather than a per-run fallback.

Install the APK from step 6, then push an empty commit to the same PR, wait for the new build, and install it **without uninstalling**.

Expected: it installs directly, with no "signed with a different key" prompt. If you get that prompt, the committed keystore is not being used — go back to Task 1 Step 5 and confirm `signingConfig = signingConfigs.getByName("test")` is present on the `pr` build type.

---

## Task 7: Debug workflow

**Files:**
- Create: `.github/workflows/debug.yml`

**Interfaces:**
- Consumes: the same three scripts as Task 6
- Produces: a manual workflow with a `ref` input. Artifact behaviour matches `pr.yml`; APK is named `HailBug.apk` and lands in the `debug` topic.

- [ ] **Step 1: Create the workflow**

```bash
cat > .github/workflows/debug.yml <<'YAML'
name: Debug

on:
  workflow_dispatch:
    inputs:
      ref:
        description: Branch, tag or commit to build
        required: true
        default: main
        type: string

permissions:
  contents: read

concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true

jobs:
  check:
    name: Test, build, deliver
    runs-on: ubuntu-latest
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v7
        with:
          ref: ${{ inputs.ref }}
          fetch-depth: 0

      - name: Set up JDK
        uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up Android SDK
        uses: android-actions/setup-android@v4
        with:
          packages: 'platform-tools platforms;android-37 build-tools;37.0.0'
          accept-android-sdk-licenses: yes

      - name: Unit tests
        run: .github/scripts/test.sh

      - name: Upload test report
        if: failure()
        uses: actions/upload-artifact@v7
        with:
          name: test-report-debug
          path: app/build/reports/tests/testDebugUnitTest
          if-no-files-found: ignore

      - name: Build
        id: build
        run: .github/scripts/build.sh debug

      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
        run: mv "$APK_PATH" HailBug.apk

      - name: Notify
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/HailBug.apk
          BUILD_TITLE: "Debug build — ${{ inputs.ref }}"
          CI_STATUS: "Not release-signed. Installs as com.aistra.hail.debug."
          TG_CHANNEL: debug
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_CHAT_ID: ${{ secrets.TG_CHAT_ID }}
        run: python3 .github/scripts/notify.py
YAML
```

- [ ] **Step 2: Apply the same three safety checks as Task 6**

```bash
python3 -c "
import yaml
d = yaml.safe_load(open('.github/workflows/debug.yml'))
assert d['permissions'] == {'contents': 'read'}
print('DEBUG: permissions OK')
"
grep -n "KEYSTORE" .github/workflows/debug.yml && echo "PROBLEM" || echo "OK: no signing secrets"
python3 -c "
import yaml
d = yaml.safe_load(open('.github/workflows/debug.yml'))
for s in d['jobs']['check']['steps']:
    assert '\${{' not in s.get('run', ''), s
print('OK: no expressions in run blocks')
"
```

Expected: all three pass.

- [ ] **Step 3: Commit**

```bash
git add .github/workflows/debug.yml
git commit -m "ci: add manual debug workflow for any ref"
```

- [ ] **Step 4: Run it on main and verify**

Trigger **Debug** → **Run workflow** with `ref = main`. Then:

```bash
$ANDROID_HOME/build-tools/*/aapt dump badging app/build/outputs/apk/debug/*.apk | head -2
```

Expected from a local equivalent build: `name='com.aistra.hail.debug'`, `versionName='1.11.4-Debug'`, `versionCode='43'` — the version code unchanged, per the spec. And an `HailBug.apk` arrives in the `debug` Telegram topic.

---

## Task 8: Release workflow

**Files:**
- Create: `.github/scripts/version.sh`
- Create: `.github/scripts/signing.sh`
- Create: `.github/scripts/verify-signer.sh`
- Create: `.github/signer.sha256` (one line: the release certificate's SHA-256)
- Create: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: `lib.sh`, `build.sh`, `notify.py`, `.github/.java-version`
- Produces: `version.sh` writes `RELEASE_VERSION_NAME` and `RELEASE_VERSION_CODE` to `$GITHUB_OUTPUT`. `signing.sh` writes `signing.properties` at the repo root and removes it on exit. `verify-signer.sh` reads `APK_PATH` and exits non-zero on a signer mismatch.

- [ ] **Step 1: Create `version.sh`**

```bash
cat > .github/scripts/version.sh <<'SCRIPT'
#!/usr/bin/env bash
# Derive versionName and versionCode from a vX.Y.Z tag, refusing to go
# backwards. Results go to $GITHUB_OUTPUT.
#
#   v1.12.0 -> 1.12.0, 11200
#   v1.12.3 -> 1.12.3, 11203
#   v1.13.0 -> 1.13.0, 11300
#
# The first release moves versionCode from the legacy 43 to ~11200. That
# discontinuity is deliberate and one-time: it buys a versionCode that is a pure
# function of the tag, with no counter to maintain and no way to drift. Do not
# "fix" it by reintroducing a manual counter.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

require_env TAG

VERSION="${TAG#v}"
if ! [[ "$VERSION" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  die "Tag '${TAG}' is not vMAJOR.MINOR.PATCH"
fi
MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

if (( MINOR > 99 || PATCH > 99 )); then
  die "minor and patch must each be below 100 (got ${MINOR}.${PATCH})"
fi

VERSION_CODE=$(( MAJOR * 10000 + MINOR * 100 + PATCH ))

# Pre-release and release share an applicationId and a signing key, so Android
# requires a strictly higher versionCode. A decrease is refused at install time
# with no useful message, so fail here instead.
PREVIOUS="$(git tag --list 'v[0-9]*' --sort=-v:refname | head -1 || true)"
if [ -n "$PREVIOUS" ]; then
  PREV="${PREVIOUS#v}"
  if [[ "$PREV" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    PREVIOUS_CODE=$(( BASH_REMATCH[1] * 10000 + BASH_REMATCH[2] * 100 + BASH_REMATCH[3] ))
    if (( VERSION_CODE <= PREVIOUS_CODE )); then
      die "Tag ${TAG} derives versionCode ${VERSION_CODE}, not greater than the previous tag ${PREVIOUS} (${PREVIOUS_CODE}). The tag must increase."
    fi
  fi
fi

{
  echo "RELEASE_VERSION_NAME=${MAJOR}.${MINOR}.${PATCH}"
  echo "RELEASE_VERSION_CODE=${VERSION_CODE}"
} >> "$GITHUB_OUTPUT"

note "${MAJOR}.${MINOR}.${PATCH} -> versionCode ${VERSION_CODE}"
SCRIPT
chmod +x .github/scripts/version.sh
```

- [ ] **Step 2: Test `version.sh` derivation**

```bash
GITHUB_OUTPUT=/tmp/vout TAG=v1.12.3 bash .github/scripts/version.sh; cat /tmp/vout
```

Expected: `RELEASE_VERSION_NAME=1.12.3` and `RELEASE_VERSION_CODE=11203`.

- [ ] **Step 3: Test the two rejection paths**

```bash
GITHUB_OUTPUT=/tmp/vout TAG=v1.12 bash .github/scripts/version.sh; echo "exit=$?"
```

Expected: `::error::Tag 'v1.12' is not vMAJOR.MINOR.PATCH`, `exit=1`.

```bash
GITHUB_OUTPUT=/tmp/vout TAG=v1.12.300 bash .github/scripts/version.sh; echo "exit=$?"
```

Expected: `::error::minor and patch must each be below 100 (got 12.300)`, `exit=1`.

- [ ] **Step 4: Test the monotonicity guard**

This repository already has tags, so pick one below the highest:

```bash
git tag --list 'v[0-9]*' --sort=-v:refname | head -3
```

Then:

```bash
GITHUB_OUTPUT=/tmp/vout TAG=v0.0.1 bash .github/scripts/version.sh; echo "exit=$?"
```

Expected: `::error::Tag v0.0.1 derives versionCode 1, not greater than the previous tag ...`, `exit=1`. If it succeeds, the guard is not working and the release path is unsafe.

Delete the tag if Step 1–2 of this task created one:

```bash
git tag -d v1.12.3 2>/dev/null || true
```

- [ ] **Step 5: Create `signing.sh`**

```bash
cat > .github/scripts/signing.sh <<'SCRIPT'
#!/usr/bin/env bash
# Materialise signing.properties from secrets. The keystore lands in RUNNER_TEMP,
# outside the checkout; the properties file is removed on exit.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"
repo_root

require_env KEYSTORE KEYSTORE_PASSWORD KEYSTORE_ALIAS KEYSTORE_ALIAS_PASSWORD

KEYSTORE_PATH="${RUNNER_TEMP}/release.jks"
PROPS="$(repo_root)/signing.properties"

cleanup() { rm -f "$PROPS"; }
trap cleanup EXIT

printf '%s' "$KEYSTORE" | base64 -d > "$KEYSTORE_PATH"
chmod 600 "$KEYSTORE_PATH"

{
  echo "storeFile=${KEYSTORE_PATH}"
  echo "storePassword=${KEYSTORE_PASSWORD}"
  echo "keyAlias=${KEYSTORE_ALIAS}"
  echo "keyPassword=${KEYSTORE_ALIAS_PASSWORD}"
} > "$PROPS"

note "Release signing material staged"
SCRIPT
chmod +x .github/scripts/signing.sh
```

- [ ] **Step 6: Test `signing.sh` requires all four secrets**

```bash
env -u KEYSTORE_KEY bash -c 'KEYSTORE=x KEYSTORE_PASSWORD=y KEYSTORE_ALIAS=z bash .github/scripts/signing.sh'; echo "exit=$?"
```

Expected: `::error::Missing required environment variable: KEYSTORE_ALIAS_PASSWORD` and `exit=1`.

- [ ] **Step 7: Create `verify-signer.sh`**

```bash
cat > .github/scripts/verify-signer.sh <<'SCRIPT'
#!/usr/bin/env bash
# Prove the APK was signed by the expected certificate. Nothing else in the
# pipeline can answer this.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

require_env APK_PATH

BT="$(build_tools_dir)"
"$BT/apksigner" verify --print-certs "$APK_PATH"

ACTUAL="$("$BT/apksigner" verify --print-certs "$APK_PATH" \
  | awk -F': ' '/SHA-256 digest/ { print $2; exit }')"
[ -n "$ACTUAL" ] || die "apksigner reported no SHA-256 digest for ${APK_PATH}"

EXPECTED="$(tr -d '[:space:]' < "$(repo_root)/.github/signer.sha256" | tr '[:lower:]' '[:upper:]')"
[ -n "$EXPECTED" ] || die ".github/signer.sha256 is empty"

if [ "$ACTUAL" != "$EXPECTED" ]; then
  die "Signer mismatch.
  expected: $EXPECTED
  actual:   $ACTUAL"
fi

note "Signer verified: $ACTUAL"
SCRIPT
chmod +x .github/scripts/verify-signer.sh
```

- [ ] **Step 8: Populate the expected signer fingerprint**

From an APK you have already released:

```bash
$ANDROID_HOME/build-tools/*/apksigner verify --print-certs <path-to-released.apk> \
  | grep 'SHA-256 digest'
```

Take that value:

```bash
printf '<the 64 hex characters>\n' > .github/signer.sha256
cat .github/signer.sha256
```

If you have no released APK to hand, **stop here and report.** Do not fabricate a value, and do not run the release workflow until this is real — `verify-signer.sh` fails closed, so a placeholder would block every release.

- [ ] **Step 9: Test `verify-signer.sh` rejects a mismatch**

```bash
cp .github/signer.sha256 /tmp/signer.bak
printf '%064d\n' 0 > .github/signer.sha256
APK_PATH=$(find app/build/outputs/apk/pr -name '*.apk' | head -1) \
  bash .github/scripts/verify-signer.sh; echo "exit=$?"
cp /tmp/signer.bak .github/signer.sha256
```

Expected: a `Signer mismatch` error and `exit=1`. The PR APK is signed with the test key, so it cannot match the release fingerprint — which is exactly the check we want.

- [ ] **Step 10: Create the workflow**

```bash
cat > .github/workflows/release.yml <<'YAML'
name: Release

on:
  push:
    tags:
      - 'v*'

permissions:
  contents: write

concurrency:
  group: ${{ github.workflow }}
  cancel-in-progress: false

jobs:
  build:
    name: Build and sign
    runs-on: ubuntu-latest
    timeout-minutes: 30
    outputs:
      channel: ${{ steps.channel.outputs.channel }}
      version: ${{ steps.version.outputs.RELEASE_VERSION_NAME }}
    steps:
      - uses: actions/checkout@v7
        with:
          fetch-depth: 0

      - name: Set up JDK
        uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v4

      - name: Set up Android SDK
        uses: android-actions/setup-android@v4
        with:
          packages: 'platform-tools platforms;android-37 build-tools;37.0.0'
          accept-android-sdk-licenses: yes

      # A tag reachable from main is a release; anything else is a pre-release.
      # No naming convention to remember.
      - name: Classify
        id: channel
        run: |
          git fetch origin main --no-tags
          if git merge-base --is-ancestor "$GITHUB_SHA" origin/main; then
            echo "channel=release" >> "$GITHUB_OUTPUT"
          else
            echo "channel=prerelease" >> "$GITHUB_OUTPUT"
          fi

      - name: Derive version
        id: version
        env:
          TAG: ${{ github.ref_name }}
        run: .github/scripts/version.sh

      - name: Stage release signing material
        env:
          KEYSTORE: ${{ secrets.KEYSTORE }}
          KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
          KEYSTORE_ALIAS: ${{ secrets.KEYSTORE_ALIAS }}
          KEYSTORE_ALIAS_PASSWORD: ${{ secrets.KEYSTORE_ALIAS_PASSWORD }}
        run: .github/scripts/signing.sh

      - name: Build
        id: build
        env:
          RELEASE_VERSION_NAME: ${{ steps.version.outputs.RELEASE_VERSION_NAME }}
          RELEASE_VERSION_CODE: ${{ steps.version.outputs.RELEASE_VERSION_CODE }}
        run: .github/scripts/build.sh release

      - name: Verify signer
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
        run: .github/scripts/verify-signer.sh

      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
          VERSION: ${{ steps.version.outputs.RELEASE_VERSION_NAME }}
        run: mv "$APK_PATH" "Hail-${VERSION}.apk"

      - name: Upload artifact
        uses: actions/upload-artifact@v7
        with:
          name: apk-${{ steps.version.outputs.RELEASE_VERSION_NAME }}
          path: Hail-*.apk
          if-no-files-found: error

  publish:
    name: Publish draft
    needs: [build]
    runs-on: ubuntu-latest
    steps:
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

      - name: Notify
        if: always()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/Hail-${{ needs.build.outputs.version }}.apk
          BUILD_TITLE: "${{ needs.build.outputs.channel == 'prerelease' && 'Pre-release' || 'Release' }} ${{ needs.build.outputs.version }}"
          CI_STATUS: "Draft on GitHub. Publish when you are happy with it."
          TG_CHANNEL: ${{ needs.build.outputs.channel }}
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_CHAT_ID: ${{ secrets.TG_CHAT_ID }}
        run: python3 .github/scripts/notify.py
YAML
```

`cancel-in-progress: false` is deliberate: cancelling a publish mid-upload leaves a half-attached asset, and a tag push is idempotent so there is nothing to gain by cancelling.

- [ ] **Step 11: Verify the workflow parses and has no injection sites**

```bash
python3 -c "
import yaml
d = yaml.safe_load(open('.github/workflows/release.yml'))
assert d['permissions'] == {'contents': 'write'}
assert d['jobs']['publish']['needs'] == ['build']
print('RELEASE: structure OK')
"
python3 -c "
import yaml
for f in ('pr','debug','release'):
    d = yaml.safe_load(open(f'.github/workflows/{f}.yml'))
    for job in d['jobs'].values():
        for s in job['steps']:
            assert '\${{' not in s.get('run', ''), (f, s)
print('OK: no expressions in any run block')
"
```

- [ ] **Step 12: Commit**

```bash
git add .github/scripts/version.sh .github/scripts/signing.sh \
        .github/scripts/verify-signer.sh .github/signer.sha256 \
        .github/workflows/release.yml
git commit -m "ci: add tag-driven release workflow

A tag reachable from main is a release, anything else is a pre-release, so
there is no naming convention to remember. versionCode is derived from the
tag and guarded against going backwards, because pre-release and release
share an applicationId and a signing key, and Android refuses an install
whose versionCode is not higher.

Every release is created as a draft and published by hand. verify-signer.sh
proves the APK carries the expected certificate before any release exists."
```

- [ ] **Step 13: Run a throwaway release**

Do this before any real version. Tag something disposable on `dev`:

```bash
git fetch origin
git switch dev && git pull
git tag v1.12.99 && git push origin v1.12.99
```

Watch the `Release` run. Confirm **all** of the following:

| Check | Expected |
|---|---|
| Classify output | `channel=prerelease` (the tag is not on `main`) |
| `version.sh` | `1.12.99 -> versionCode 11299` |
| `verify-signer.sh` | `::notice::Signer verified: <fingerprint>` |
| Draft release | exists, has the APK attached, is **not** public |
| Telegram | APK in the `prerelease` topic, caption says "Draft on GitHub" |
| Installing it | replaces your current Hail — the same-key upgrade path |

- [ ] **Step 14: Prove the two guards actually fire**

A guard that has never failed is not known to work.

```bash
git tag v1.12.98 && git push origin v1.12.98
```

Expected: the run **fails** in `version.sh` with `derives versionCode 11298, not greater than the previous tag v1.12.99`. No draft is created.

```bash
cp .github/signer.sha256 /tmp/signer.bak
printf '%064d\n' 0 > .github/signer.sha256
git commit -am "test: corrupt signer" && git push
git tag v1.13.1 && git push origin v1.13.1
```

Expected: the run **fails** at `verify-signer.sh` with `Signer mismatch`, and no draft is created.

```bash
git reset --hard HEAD~1
cp /tmp/signer.bak .github/signer.sha256
git tag -d v1.12.98 v1.13.1
git push --delete origin v1.12.98 v1.13.1
```

- [ ] **Step 15: Delete the throwaway draft**

Go to Releases, delete the `v1.12.99` draft and its tag. Confirm no draft remains.

---

## Task 9: Retire the old pipeline

**Files:**
- Delete: `.github/workflows/build.yml`
- Delete: `.github/scripts/setup.sh`, `pr.sh`, `debug.sh`, `zip.sh`, `upload.sh`, `tg_body.sh`
- Modify: `build.gradle.kts` if `allowDebugSigning` was ever added (it should not have been)

**Interfaces:**
- Consumes: a green `Release` run from Task 8
- Produces: a repository whose only CI entry points are `pr.yml`, `debug.yml`, `release.yml`

- [ ] **Step 1: Confirm the new pipeline is green before deleting anything**

- `PR Check` has passed on at least one pull request.
- `Debug` has been run manually at least once.
- `Release` has completed a full run, including the two negative tests in Task 8 Step 14.

**If any of these is untrue, stop.** Deleting `build.yml` removes the only working build path, and `setup.sh` is what currently installs the toolchain.

- [ ] **Step 2: Inventory the scripts to remove**

```bash
ls -la .github/scripts/
grep -rn "scripts/\(setup\|pr\|debug\|zip\|upload\|tg_body\)\.sh" .github/workflows/ | grep -v release.yml
```

Expected: `build.yml` is the only remaining referrer. If any new workflow references them, stop and reconcile.

- [ ] **Step 3: Remove them in one commit**

```bash
git rm .github/workflows/build.yml
git rm .github/scripts/setup.sh .github/scripts/pr.sh .github/scripts/debug.sh \
       .github/scripts/zip.sh .github/scripts/upload.sh .github/scripts/tg_body.sh
git status --short
```

`lib.sh`, `test.sh`, `build.sh`, `version.sh`, `signing.sh`, `verify-signer.sh`, and `notify.py` must remain.

- [ ] **Step 4: Verify nothing references the removed files**

```bash
grep -rn "setup\.sh\|pr\.sh\|zip\.sh\|upload\.sh\|tg_body\.sh" .github/ app/ 2>/dev/null \
  | grep -v Binary || echo "OK: no dangling references"
```

- [ ] **Step 5: Run one final end-to-end check**

Open a pull request with a trivial change. Confirm `PR Check` still goes green and the APK still arrives. The removal must not have broken the new pipeline.

- [ ] **Step 6: Commit**

```bash
git commit -m "ci: retire the shell-script pipeline

Replaced by pr.yml, debug.yml and release.yml, which run the unit tests,
build with the right version overrides, verify the signer, and deliver to
Telegram. Removing this deletes seven shell scripts and the only path that
had never been exercised."
```

---

## Configuration Checklist

Repository **variables** (Settings → Secrets and variables → Actions → Variables). None.

Repository **secrets** (Settings → Secrets and variables → Actions → Secrets):

| Name | Used by | Notes |
|---|---|---|
| `TG_TOKEN` | all three | Telegram bot token |
| `TG_CHAT_ID` | all three | numeric chat id |
| `KEYSTORE` | `release.yml` | base64 of the release keystore |
| `KEYSTORE_PASSWORD` | `release.yml` | |
| `KEYSTORE_ALIAS` | `release.yml` | |
| `KEYSTORE_ALIAS_PASSWORD` | `release.yml` | |

No new secret is needed for the test key; `.github/debug.keystore` is committed.

No repository **environment** is needed; the draft is the gate.

---

## Final Build Matrix

| | `pr` | `debug` | pre-release | release |
|---|---|---|---|---|
| Trigger | `pull_request` | `workflow_dispatch` on any ref | tag on a `dev` commit | tag on a `main` commit |
| Permissions | `contents: read` | `contents: read` | `contents: write` | `contents: write` |
| Gradle task | `assemblePr` | `assembleDebug` | `assembleRelease` | `assembleRelease` |
| applicationId | `…hail.pr.<n>` | `…hail.debug` | `…hail` | `…hail` |
| versionName | `PR<n>` | `1.11.4-Debug` | `X.Y.Z` | `X.Y.Z` |
| versionCode | `<n>` | `43` unchanged | `X*10000+Y*100+Z` | `X*10000+Y*100+Z` |
| Signing key | test (committed) | test (committed) | release (secret) | release (secret) |
| File name | `Hail-<n>.apk` | `HailBug.apk` | `Hail-<v>.apk` | `Hail-<v>.apk` |
| GitHub | none | none | draft release | draft release |
| Telegram topic | `pr` | `debug` | `prerelease` | `release` |

---

## Known Risks

**The test key is not an authenticity guarantee.** It is committed, so anyone can produce an APK carrying that signature. It distinguishes "built by the pipeline" from "unrelated", never "genuine" from "forged". The trust anchor is the Telegram channel the APK came from.

**A `pull_request` grants no secrets to fork PRs**, so the Telegram step is skipped there and the job still passes. Pull requests from branches in this repository *do* get `TG_TOKEN`; drop the Telegram step before accepting external contributions.

**Telegram's `sendDocument` ceiling is 50 MB.** If the APK exceeds it the notification is skipped with a warning; the APK is still on the GitHub draft. `build.gradle.kts` already supports `-Pabi=arm64-v8a` for a per-ABI build.

**`src/test` uses `org.json:json` while the device uses AOSP `org.json`.** They disagree on numeric typing. Robolectric would not change this. The mitigation is the hand-written-JSON test in `HBackupTest`, not a new framework.

**Nothing in this plan was executed while it was written.** There was no shell and no Android SDK available. Tasks 1 and 2 are verified by a human running the commands; Tasks 3 onward are verified by CI. The first `Release` run is genuinely untested until Task 8 Step 13.

---

## Separate Plan Required

The backup and restore fixes on **PR #109** (`fix/backup-mkdirs-always-fails`, base `dev`) are a distinct subsystem and are **not** covered here. That work — the `mkdirs()` directory check, the Float preference round-trip, the `isReturnDefaultValues` gap, and its 29 tests — needs its own plan so it can be reviewed and shipped independently of the CI redesign.

It also carries a dependency worth stating: **its tests have never been executed.** Task 3 Step 3 will be the first run of this repository's unit tests, which includes `HBackupTest`. Expect findings there before anything from #109 is merged.
