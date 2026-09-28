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
- **The release signing key is read only by `release.yml`.** `pr.yml` and `debug.yml` must never reference `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`, or `KEYSTORE_ALIAS_PASSWORD`.- **The test keystore is committed** at `.github/debug.keystore` (PKCS12, alias `HailBug`, store and key password `HailBug`). It may only sign `com.aistra.hail.pr.<n>` and `com.aistra.hail.debug`.
- **Version line:** pre-releases increment the patch on `dev` (`v1.12.1`, `v1.12.2`, `v1.12.3`); releases are always patch-zero on `main` (`v1.13.0`, `v1.14.0`).
- **YAML files contain no inline shell beyond a single `run:` line invoking a script**, and no `${{ }}` inside a `run:` body. All values reach scripts via `env:`.
- **Telegram topics live in `.github/telegram.json`,** never in workflow expressions.
- **Every `fetch_depth: 0` checkout** must come before any `git tag` / `git merge-base` use.
- **Action versions, verified against their repositories:** `actions/checkout@v7`, `actions/setup-java@v5`, `gradle/actions/setup-gradle@v6`, `actions/upload-artifact@v7`, `android-actions/setup-android@v4`, `softprops/action-gh-release@v3`. Use `setup-java@v5` rather than `@v6`: the v6 tag exists but its README still states it is "not yet recommended for production workflows" and that `v5` is the latest stable release. Use `setup-gradle` rather than `setup-java`'s own `cache: gradle` — the setup-java README defers to it for "advanced Gradle caching features".
- **Telegram Bot API limits, verified:** `sendDocument` accepts files up to 50 MB and a caption of 0–1024 characters *after entities parsing*; `parse_mode` accepts only `HTML` or `MarkdownV2`; `message_thread_id` works on forum supergroups with topic mode enabled, which is what the existing topics already use.
- **Nothing in this plan is executed during implementation.** Tasks 1 and 2 are verified by a human running commands; Tasks 3+ are verified by CI.

---

## Decisions

These were decided deliberately. Do not relitigate them during implementation; if one looks wrong, flag it rather than changing it.

1. **A build type with no `signingConfig` produces an `*-unsigned.apk`, never a debug-signed APK.** The previous code silently fell back to debug signing, producing a "release" that could not be verified. Assignment is now conditional on the properties file existing.

2. **The test keystore is committed, not secret.** It can only sign app IDs nobody trusts, so committing it grants nothing, and committing is what makes every PR and debug build carry the same signature. That is what lets a rebuild install over its own previous build instead of demanding an uninstall.

3. **No `environment:` gate on the publish job.** The gate belongs on the artifact being public. The draft already is that gate, and a second approval click buys nothing. Mihon ships this way.

4. **The keystore is decoded to a file; the credentials never are.** Gradle's `signingConfig` takes `storeFile` as a `File`, so a keystore that exists only in a secret must be materialised on disk. The passwords need not be: Gradle exposes `ORG_GRADLE_PROJECT_*` environment variables as project properties, so `storePassword`, `keyAlias` and `keyPassword` go from the secret straight to Gradle with no properties file written to the working tree.

5. **`versionCode` is derived from the tag, not maintained.** The first release moves from 43 to ~11200. That discontinuity is one-time and deliberate. Do not reintroduce a counter.

6. **A 50 MB Telegram upload failure degrades, it does not fail.** The APK is already on the GitHub draft, so an oversized upload costs the notification, not the release.

7. **Release captions show Highlights only, or a link — never the whole section.** `### Highlights` exists in 4 of the 11 releases in the current `CHANGELOG.md`, and is absent from the two most recent, so a Highlights-only caption is empty more often than not. Rendering the full section is not an alternative: Telegram's caption limit is 1024 characters after entity parsing, and the `1.11.5` entry alone is roughly 1,700 characters of bullet text. So: Highlights when present, otherwise a blockquoted link to the release page, which carries the generated notes. A `CAPTION_LIMIT` guard degrades to the link if a future Highlights section outgrows the ceiling.

8. **The `## [x.y.z] - date` heading locates a release and is never rendered.** It is markdown link syntax with no matching definition, so the brackets are syntax rather than content, and the version is already in the tag and the APK filename.

---

## File Structure

| File | Responsibility |
|---|---|
| `.github/.java-version` | Single source of truth for the JDK major version |
| `.github/telegram.json` | Chat routing: channel name to Telegram topic id |
| `.github/debug.keystore` | Committed PKCS12 test key (already pushed) |
| `.github/scripts/lib.sh` | Sourced helpers: `step`, `note`, `warn`, `die`, `require_env`, `repo_root`, `build_tools_dir`, `single_match` |
| `.github/scripts/test.sh` | Runs the JVM unit tests |
| `.github/scripts/build.sh` | Assembles `pr`, `debug`, or `release` with the right `-P` overrides |
| `.github/scripts/version.sh` | Derives `versionName` and `versionCode` from a tag, refuses to go backwards |
| `.github/scripts/signing.sh` | Decodes the release keystore into `RUNNER_TEMP` and removes it on exit |
| `.github/scripts/upload.py` | Uploads the APK to a Telegram topic, with a caption |
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
        // Release key: staged by release.yml, which decodes it from secrets into
        // RUNNER_TEMP because Gradle's signingConfig takes a File. The
        // credentials never touch the filesystem — they arrive as
        // ORG_GRADLE_PROJECT_* environment variables, which Gradle exposes as
        // project properties. With neither present the config stays empty and
        // the build type is left unsigned rather than debug-signed.
        create("release") {
            val keystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
            if (keystore != null && keystore.exists()) {
                storeFile = keystore
                storePassword = project.findProperty("releaseStorePassword") as String?
                keyAlias = project.findProperty("releaseKeyAlias") as String?
                keyPassword = project.findProperty("releaseKeyPassword") as String?
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
            // ".pr<n>" with no dot: aapt2 rejects a package segment that starts
            // with a digit, so "com.aistra.hail.pr.79" cannot link. Every
            // segment must begin with a letter.
            applicationIdSuffix =
                providers.gradleProperty("prNumber").orNull?.let { ".pr$it" } ?: ".pr"
            signingConfig = signingConfigs.getByName("test")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (System.getenv("RELEASE_KEYSTORE_PATH") != null) {
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
  # Single-line subject, so GITHUB_OUTPUT needs no heredoc form here.
  printf 'COMMIT_SUBJECT=%s\n' "$(git log -1 --pretty=%s)" >> "$GITHUB_OUTPUT"
fi
SCRIPT
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

## Task 5: Telegram upload

**Files:**
- Create: `.github/scripts/upload.py`
- Create: `.github/telegram.json`

**Interfaces:**
- Consumes: `.github/telegram.json` (read via a path relative to the script's own location, so it works from any working directory)
- Produces: `upload.py` — environment `APK_PATH`, `TG_TOKEN`, `TG_GROUP`, `TG_CHANNEL` (all required), plus per channel: `PR_NUMBER` + `PR_TITLE` for `pr`, `GITHUB_SHA` + `COMMIT_SUBJECT` for `debug`, `RELEASE_TAG` for `prerelease` and `release`. Reads `CHANGELOG.md` from the repository root. Exits 0 on success and on oversize; exits 1 on missing configuration.

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

- [ ] **Step 2: Create the upload script**

```bash
cat > .github/scripts/upload.py <<'PY'
#!/usr/bin/env python3
"""Upload a built APK to Telegram.

The upload is the job; the caption is metadata on it. Every dynamic value in
the caption passes through html.escape at the point of interpolation. Release
and pre-release captions read the Highlights subsection of the tagged version
out of CHANGELOG.md. Channel routing lives in .github/telegram.json so it
changes in a reviewable diff.
"""

import html
import json
import mimetypes
import os
import pathlib
import re
import sys
import urllib.request
import uuid

API = "https://api.telegram.org"
API_TIMEOUT = 60
SIZE_LIMIT = 50 * 1024 * 1024   # Telegram bot API sendDocument ceiling
CAPTION_LIMIT = 1024            # Telegram bot API caption ceiling
REPO = "rahaaatul/Hail"

# .github/scripts/upload.py -> three levels up is the repository root.
ROOT = pathlib.Path(__file__).resolve().parent.parent.parent
CHANGELOG = ROOT / "CHANGELOG.md"
TOPICS = ROOT / ".github" / "telegram.json"

HEADING = re.compile(r"^#{1,6}\s+(.*?)\s*#*\s*$")
BULLET = re.compile(r"^\s*[*+-]\s+(.*)$")
TAGS = re.compile(r"<[^>]+>")


def required(name: str) -> str:
    value = os.environ.get(name, "")
    if not value:
        sys.exit(f"::error::Missing required environment variable: {name}")
    return value


def visible(text: str) -> int:
    """Telegram counts a caption after entity parsing, so markup is free."""
    return len(TAGS.sub("", text))


def topic_for(channel: str) -> int:
    topics = json.loads(TOPICS.read_text(encoding="utf-8")).get("topics", {})
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


def pr_caption(number: str, title: str) -> str:
    return (
        f"<p><strong>PR #{html.escape(number)}</strong></p>\n"
        f"<blockquote>\n<p>{html.escape(title)}</p>\n</blockquote>"
    )


def debug_caption(sha: str, subject: str) -> str:
    return (
        f'<p><strong><a href="https://github.com/{REPO}/commit/{html.escape(sha)}">'
        f"Build {html.escape(sha[:7])}</a></strong></p>\n"
        f"<blockquote>\n<p>{html.escape(subject)}</p>\n</blockquote>"
    )


def changelog_link(tag: str) -> str:
    url = f"https://github.com/{REPO}/releases/tag/{html.escape(tag)}"
    return f'<blockquote>\n<a href="{url}">See full changelog</a>\n</blockquote>'


def release_section(version: str) -> list[str] | None:
    """Lines belonging to one release, excluding its `## [x.y.z] - date` marker.

    The marker only locates the section; it is never rendered.
    """
    if not CHANGELOG.is_file():
        return None
    lines = CHANGELOG.read_text(encoding="utf-8").splitlines()
    head = re.compile(rf"^##\s+\[?{re.escape(version)}\]?(\s|$)")
    start = next((i for i, line in enumerate(lines) if head.match(line)), None)
    if start is None:
        return None
    end = next(
        (j for j in range(start + 1, len(lines)) if lines[j].startswith("## ")),
        len(lines),
    )
    return lines[start + 1:end]


def highlights(section: list[str]) -> list[str]:
    """Bullet text under `### Highlights`, and nothing else."""
    out: list[str] = []
    inside = False
    for raw in section:
        line = raw.rstrip()
        heading = HEADING.match(line)
        if heading:
            inside = heading.group(1).strip().lower() == "highlights"
            continue
        if inside:
            bullet = BULLET.match(line)
            if bullet:
                out.append(bullet.group(1))
    return out


def release_caption(tag: str) -> str:
    head = "<b>📚 Changelogs</b>\n\n"
    version = tag[1:] if tag.startswith("v") else tag
    section = release_section(version)
    items = highlights(section) if section else []

    if items:
        bullets = "\n".join(f"‣ {html.escape(text)}" for text in items)
        caption = f"{head}<b>Highlights</b>\n<blockquote>\n{bullets}\n</blockquote>"
        # Highlights sections are short today. If a future one outgrows the
        # caption limit, degrade to the link rather than fail the upload.
        if visible(caption) <= CAPTION_LIMIT:
            return caption

    return head + changelog_link(tag)


def main() -> None:
    path = required("APK_PATH")
    channel = required("TG_CHANNEL")
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

    if channel == "pr":
        caption = pr_caption(
            os.environ.get("PR_NUMBER", "?"), os.environ.get("PR_TITLE", "")
        )
    elif channel == "debug":
        caption = debug_caption(
            os.environ.get("GITHUB_SHA", ""), os.environ.get("COMMIT_SUBJECT", "")
        )
    else:
        caption = release_caption(os.environ.get("RELEASE_TAG", ""))

    send_document(
        required("TG_TOKEN"),
        required("TG_GROUP"),
        topic_for(channel),
        path,
        caption,
    )
    print("::notice::APK sent to Telegram")


if __name__ == "__main__":
    main()
PY
chmod +x .github/scripts/upload.py
```

- [ ] **Step 3: Verify it compiles and that missing config fails loudly**

```bash
python3 -m py_compile .github/scripts/upload.py && echo "compiles OK"
env -u TG_TOKEN python3 .github/scripts/upload.py; echo "exit=$?"
```

Expected: `compiles OK`, then `::error::Missing required environment variable: TG_TOKEN` and `exit=1`.

- [ ] **Step 4: Verify every caption shape, against the real CHANGELOG.md**

This is the security and correctness property. `html.escape` is not hand-written, and the parser is exercised against the actual file, so the assertions below double as a fixture.

```bash
python3 - <<'PY'
import importlib.util
spec = importlib.util.spec_from_file_location("n", ".github/scripts/upload.py")
m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)

# PR caption, with a title carrying markup
out = m.pr_caption("79", '<b>x</b> & "y"')
assert "<strong>PR #79</strong>" in out, out
assert "<b>x</b>" not in out, "raw markup survived"
assert "&lt;b&gt;" in out and "&amp;" in out and "&quot;" in out, out
print("PASS: pr caption escapes the title")

# Debug caption links the full sha, displays seven, escapes the subject
sha = "20887bfdbe5dbcfcfe14275552f3e899dd0f1d5b"
out = m.debug_caption(sha, 'fix <ci> & "x"')
assert f'href="https://github.com/rahaaatul/Hail/commit/{sha}"' in out, out
assert "Build 20887bf<" in out, out
assert "&lt;ci&gt;" in out, out
print("PASS: debug caption links the commit and escapes the subject")

# A version that HAS a Highlights section
out = m.release_caption("v1.11.3")
assert out.startswith("<b>📚 Changelogs</b>"), out
assert "<b>Highlights</b>" in out, f"Highlights not used for 1.11.3: {out}"
assert "‣ New Actions tab" in out, out
assert "1.11.3" not in out, "the ## marker leaked into the caption"
assert "Fixed" not in out, "a non-Highlights subsection leaked in"
print("PASS: Highlights parsed for 1.11.3")

# 1.11.4 has no Highlights section, so it falls back to the link
out = m.release_caption("v1.11.4")
assert out.startswith("<b>📚 Changelogs</b>"), out
assert "<b>Highlights</b>" not in out, "1.11.4 has no Highlights section"
assert 'href="https://github.com/rahaaatul/Hail/releases/tag/v1.11.4"' in out, out
print("PASS: link fallback for 1.11.4")

# An unknown version must still produce a usable caption
out = m.release_caption("v9.9.9")
assert "See full changelog" in out, out
print("PASS: unknown version falls back to the link")

# Every shape must fit Telegram's ceiling
for cap in (m.pr_caption("1", "x"), m.debug_caption("a" * 40, "x"),
            m.release_caption("v1.11.0"), m.release_caption("v1.11.4")):
    assert m.visible(cap) <= m.CAPTION_LIMIT, (m.visible(cap), cap)
print("PASS: all captions within the 1024-character limit")
PY
```

Expected: six `PASS:` lines. Any `AssertionError` prints the offending caption — stop and report rather than adjusting the assertion to match the output.

- [ ] **Step 4a: Confirm the parser matches reality**

```bash
grep -c '^### Highlights' CHANGELOG.md
grep -n '^## \[1.11.3\]\|^### Highlights' CHANGELOG.md | head -4
```

Expected: `4` Highlights sections in the file, and `1.11.3` immediately followed by its Highlights heading. If the count differs, Step 4's fixture assumptions are wrong — re-read `CHANGELOG.md` and update the version numbers used there.

- [ ] **Step 5: Verify an unknown channel fails**

```bash
APK_PATH=/etc/hostname TG_TOKEN=t TG_GROUP=g TG_CHANNEL=nonsense \
  python3 .github/scripts/upload.py; echo "exit=$?"
```

Expected: `::error::Unknown channel 'nonsense'. Configured: ['debug', 'pr', 'prerelease', 'release']` and `exit=1`.

- [ ] **Step 6: Verify the oversize path degrades rather than failing**

```bash
python3 - <<'PY'
import os, pathlib, subprocess, tempfile
big = pathlib.Path(tempfile.gettempdir()) / "big.apk"
big.write_bytes(b"\0" * (51 * 1024 * 1024))
env = dict(os.environ, APK_PATH=str(big), TG_TOKEN="t", TG_GROUP="g",
           TG_CHANNEL="pr", PR_NUMBER="1", PR_TITLE="t")
r = subprocess.run(["python3", ".github/scripts/upload.py"], env=env,
                   capture_output=True, text=True)
print("exit", r.returncode)
print(r.stdout.strip()[:120])
big.unlink()
PY
```

Expected: `exit 0` and a `::warning::APK is 52.0 MB` line. If it exits non-zero, the degrade behaviour is broken.

- [ ] **Step 7: Commit**

```bash
git add .github/scripts/upload.py .github/telegram.json
git commit -m "ci: upload the APK to Telegram with an escaped caption

html.escape is applied at the point of interpolation rather than by
hand-written substitutions, which is what caused the no-op escaper and the
bash 5.2 patsub_replacement bug. Release captions read Highlights out of
CHANGELOG.md. Channel routing lives in a tracked json file so changing it is
a reviewable diff. An upload over Telegram's 50 MB ceiling warns and exits 0,
because the APK is already on the GitHub draft."
```

---

## Task 6: PR workflow

**Files:**
- Create: `.github/workflows/pr.yml`

**Interfaces:**
- Consumes: `.github/scripts/test.sh`, `.github/scripts/build.sh`, `.github/scripts/upload.py`, `.github/.java-version`, `.github/telegram.json`
- Produces: a `PR Check / Test, build, deliver` check on every pull request. Two outputs: artifact `apk-pr<n>`, and the APK uploaded to Telegram. Read-only token, no signing secrets.

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
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

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

      - name: Upload artifact
        uses: actions/upload-artifact@v7
        with:
          name: apk-pr${{ github.event.pull_request.number }}
          path: Hail-${{ github.event.pull_request.number }}.apk
          if-no-files-found: error

      - name: Upload to Telegram
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/Hail-${{ github.event.pull_request.number }}.apk
          PR_NUMBER: ${{ github.event.pull_request.number }}
          PR_TITLE: ${{ github.event.pull_request.title }}
          TG_CHANNEL: pr
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
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
- Produces: a manual workflow with a `ref` input. Artifact `apk-debug`, plus the APK uploaded to the `debug` Telegram topic.

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
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

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

      - name: Upload artifact
        uses: actions/upload-artifact@v7
        with:
          name: apk-debug
          path: HailBug.apk
          if-no-files-found: error

      - name: Upload to Telegram
        if: success()
        continue-on-error: true
        env:
          APK_PATH: ${{ github.workspace }}/HailBug.apk
          GITHUB_SHA: ${{ github.sha }}
          COMMIT_SUBJECT: ${{ steps.build.outputs.COMMIT_SUBJECT }}
          TG_CHANNEL: debug
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
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
- Create: `.github/workflows/release.yml`

**Interfaces:**
- Consumes: `lib.sh`, `build.sh`, `upload.py`, `.github/.java-version`
- Produces: `version.sh` writes `RELEASE_VERSION_NAME` and `RELEASE_VERSION_CODE` to `$GITHUB_OUTPUT`. `signing.sh` decodes `KEYSTORE` to `$RUNNER_TEMP/release.jks`, exports `RELEASE_KEYSTORE_PATH` through `$GITHUB_ENV`, and deletes the file on exit. `build.gradle.kts` reads that path from the environment and the three credentials from the `ORG_GRADLE_PROJECT_release*` project properties.

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
SCRIPT
chmod +x .github/scripts/signing.sh
```

- [ ] **Step 6: Test `signing.sh` decodes a keystore and cleans up**

```bash
RUNNER_TEMP=/tmp GITHUB_ENV=/tmp/genv KEYSTORE="$(base64 -w0 /dev/null)" \
  bash .github/scripts/signing.sh; echo "exit=$?"
cat /tmp/genv
ls -la /tmp/release.jks 2>&1 | tail -1
```

Expected: a `::notice::Release keystore staged` line, `exit=0`, `/tmp/genv` containing `RELEASE_KEYSTORE_PATH=/tmp/release.jks`, and **the keystore already gone** — the `EXIT` trap removes it before the script returns, which is the behaviour that matters.

Then confirm the required-secret guard:

```bash
env -u KEYSTORE bash .github/scripts/signing.sh; echo "exit=$?"
```

Expected: `::error::Missing required environment variable: KEYSTORE` and `exit=1`.

- [ ] **Step 7: Create the workflow**

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
        uses: actions/setup-java@v5
        with:
          distribution: temurin
          java-version-file: .github/.java-version

      - name: Set up Gradle
        uses: gradle/actions/setup-gradle@v6

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

      # The keystore must exist as a file for Gradle, so it is staged into
      # RUNNER_TEMP. The three credentials never reach the filesystem: Gradle
      # reads ORG_GRADLE_PROJECT_* environment variables as project properties,
      # so they are scoped to the Build step only rather than the whole job.
      - name: Stage release keystore
        env:
          KEYSTORE: ${{ secrets.KEYSTORE }}
        run: .github/scripts/signing.sh

      - name: Build
        id: build
        env:
          RELEASE_VERSION_NAME: ${{ steps.version.outputs.RELEASE_VERSION_NAME }}
          RELEASE_VERSION_CODE: ${{ steps.version.outputs.RELEASE_VERSION_CODE }}
          ORG_GRADLE_PROJECT_releaseStorePassword: ${{ secrets.KEYSTORE_PASSWORD }}
          ORG_GRADLE_PROJECT_releaseKeyAlias: ${{ secrets.KEYSTORE_ALIAS }}
          ORG_GRADLE_PROJECT_releaseKeyPassword: ${{ secrets.KEYSTORE_ALIAS_PASSWORD }}
        run: .github/scripts/build.sh release

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
          RELEASE_TAG: ${{ github.ref_name }}
          TG_CHANNEL: ${{ needs.build.outputs.channel }}
          TG_TOKEN: ${{ secrets.TG_TOKEN }}
          TG_GROUP: ${{ secrets.TG_GROUP }}
        run: python3 .github/scripts/upload.py
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
        .github/workflows/release.yml
git commit -m "ci: add tag-driven release workflow

A tag reachable from main is a release, anything else is a pre-release, so
there is no naming convention to remember. versionCode is derived from the
tag and guarded against going backwards, because pre-release and release
share an applicationId and a signing key, and Android refuses an install
whose versionCode is not higher.

Only the keystore is decoded to disk, into RUNNER_TEMP, because Gradle's
signingConfig takes a File. The credentials are passed as
ORG_GRADLE_PROJECT_* variables so no secret is ever written to the working
tree. Every release is created as a draft and published by hand."
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
| Draft release | exists, has the APK attached, is **not** public |
| Telegram | APK in the `prerelease` topic, caption shows Highlights or the changelog link |
| Installing it | replaces your current Hail — the same-key upgrade path |

- [ ] **Step 14: Prove the versionCode guard actually fires**

A guard that has never failed is not known to work.

```bash
git tag v1.12.98 && git push origin v1.12.98
```

Expected: the run **fails** in `version.sh` with `derives versionCode 11298, not greater than the previous tag v1.12.99`. No draft is created.

Clean up:

```bash
git tag -d v1.12.98
git push --delete origin v1.12.98
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

`lib.sh`, `test.sh`, `build.sh`, `version.sh`, `signing.sh`, and `upload.py` must remain.

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
| `TG_GROUP` | all three | numeric chat group id |
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
| Telegram | `pr` | `debug` | `prerelease` | `release` |
| Actions artifact | `apk-pr<n>` | `apk-debug` | `apk-<v>` | `apk-<v>` |

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
