# Pre-flight conflict scan — plan: plans/2026-09-29-ci-release-hardening.md

**Status: setup and analysis only. No task was implemented, no file in the worktree was modified.**

This is a table of findings, not a verdict on the work. It reports what the plan's tasks produce and
consume, and where a task's own test disagrees with that task's own implementation.

---

## 1. Worktree — path and isolation proof

```
$ git fetch origin
$ git rev-parse origin/fix/ci-rewrite
9241666cfc553a6c3cbee32d1c091ac2a75814ed

$ git worktree list          # BEFORE
/workspace/…/agent_f2b1e9c3-…   1e58e94 [kilo/rosy-moss-g69]

$ git worktree add .claude/worktrees/sdd-ci -b fix/ci-hardening --detach origin/fix/ci-rewrite
fatal: options '-b', '-B', and '--detach' cannot be used together
```

The command in the task brief combines `-b` with `--detach`, which git rejects. `fix/ci-rewrite` was
**not** checked out in any existing worktree, so the isolated-worktree fallback was unnecessary and
the worktree was created attached instead:

```
$ git worktree add .claude/worktrees/sdd-ci -b fix/ci-hardening origin/fix/ci-rewrite
Preparing worktree (new branch 'fix/ci-hardening')
branch 'fix/ci-hardening' set up to track 'origin/fix/ci-rewrite'.
HEAD is now at 9241666 ci: quote-free boolean SDK licenses and retire the old pipeline
```

| Property | Value |
|---|---|
| Worktree path | `.claude/worktrees/sdd-ci` (absolute: `/workspace/d031c350-535d-4160-8cd6-9e3e237967bd/sessions/agent_f2b1e9c3-e021-43a7-8b77-d6db73ac993f/.claude/worktrees/sdd-ci`) |
| `git rev-parse --show-toplevel` | `/workspace/…/.claude/worktrees/sdd-ci` — confirms a distinct root, not the main checkout |
| Branch | `fix/ci-hardening`, tracking `origin/fix/ci-rewrite` |
| Base commit | `9241666cfc553a6c3cbee32d1c091ac2a75814ed` |
| `git log --oneline -1` | `9241666 ci: quote-free boolean SDK licenses and retire the old pipeline` |
| `git status -sb` | `## fix/ci-hardening...origin/fix/ci-rewrite` — clean, no divergence |
| `git worktree list` (after) | main checkout on `kilo/rosy-moss-g69`, plus this one on `fix/ci-hardening` |

Isolation: the main checkout is on `kilo/rosy-moss-g69` at `1e58e94` and is untouched. The new
worktree sits at a different top level, holds a different branch, and is clean. Note that
`plans/` does not exist inside the worktree — the plan file lives only in the main checkout's
`plans/`. Any worker dispatching into the worktree must be given the plan's absolute path, not a
worktree-relative one.

## 2. Prior ledger

None. `.superpowers/sdd/ci-release-hardening/` did not exist. The old flat
`.superpowers/sdd/progress.md` does not exist either. `.superpowers/sdd/ci-redesign/` exists and
belongs to a different plan — it was left untouched. A fresh directory was created for this plan.

---

## 3. Table A — task pairs sharing a file or an interface

Every pair that shares a file or a named contract. "Found" records what I actually read or ran.

| Pair | Shared artefact | One produces | The other consumes | Found |
|---|---|---|---|---|
| **B1 → B2** | `zip.sh` (file) | `zip.sh` printing the archive path on **stdout, last line** | `build.sh` line: `APK="$(bash …/zip.sh "$APK" \| tail -1)"` | Interface **holds**: `tail -1` correctly extracts the path. But B1's stdout also carries `==> Compressing…` and `::notice::Archive is XMB`, so the contract "one line" in B1's *Interfaces* block is false — see Defect 2. |
| **B1 → B2** | `lib.sh` → `require_command` (symbol) | `require_command` appended to `lib.sh` | `zip.sh` calls it; `build.sh` inherits `die` behaviour through it | Holds. Append is at end of file, so it does not collide with C2's or D1's edits to the same file. |
| **B2 → B3** | `APK_PATH` (interface) | B2 changes the **meaning** of `APK_PATH`: archive for `pr`/`debug`, raw APK for `release` | B3's `pr.yml` `Rename` and `Upload artifact` steps, and B3's own test, both assume the new meaning | **Ordered correctly** — B2 is Task B2, B3 is Task B3, and the Phase B gate runs `build_sh_test.sh` before `pr_workflow_test.py`. But see Defect 7: B3's test still fails on its own replacement text. |
| **B2 → debug.yml** | `APK_PATH` (interface) | B2 compresses `debug` as well as `pr` | `debug.yml:66` `run: mv "$APK_PATH" HailBug.apk` and `debug.yml:72` `path: HailBug.apk` | **Gap.** B3 updates only `pr.yml`. Nothing in the plan updates `debug.yml`'s `Rename`, so a debug archive gets renamed to `.apk`. Defect 10. |
| **B2 → release.yml** | `APK_PATH` (interface) | release stays raw (B2's `if [ "$1" != "release" ]`) | `release.yml:80` `mv "$APK_PATH" "Hail-${VERSION}.apk"` | Holds. Release is deliberately excluded from compression, so the hardcoded `.apk` stays correct. |
| **A2 → C1** | `upload.py` → `main()` (function) | A2 adds `payload += field("parse_mode", "HTML")` inside `send_document` | C1 rewrites the top of `main()` (the `path`/`size` block) | **No collision** — A2 edits `send_document`, C1 edits `main()`. Both survive. Note A2's test and C1's test both execute `upload.py`; C1's test needs `TG_CHANNEL` set or `main()` exits before the block under test. C1's test does set it. |
| **A2 → B3** | `upload.py` (file) | A2 adds the `parse_mode` field in `send_document` | B3 rewrites the oversize `print()` in `main()` | No collision. Ordering A2 → B3 is also required for A2's test (which greps `upload.py`) to stay green — it does not reference the oversize text, so either order works. |
| **B1 → C2** | `lib.sh` (file) | B1 appends `require_command` at EOF | C2 rewrites `single_match` in place | Holds — different regions of the same file. |
| **C2 → D1** | `lib.sh` (file) | C2 rewrites `single_match`; D1 deletes `build_tools_dir` | D1's own test loops over every function in `lib.sh` | **Interaction.** D1's loop sees `require_command` (added by B1) as well. `require_command` has 2 callers in `zip.sh`, so it passes. But `warn` still fails the loop — Defect 9. |
| **B1 → D1** | `lib.sh` → dead-code test (interface) | B1 adds `require_command` to `lib.sh` | D1's test asserts *every* function in `lib.sh` has a caller | Holds for `require_command`; see Defect 9 for `warn`. |
| **A1 → A3** | `RELEASE_KEYSTORE_PATH` (interface) | A1 sets it in `$GITHUB_ENV` and leaves the file on disk | A3's `if` condition consumes it and adds `.exists()` | Holds, and the pair is what makes A3 meaningful — A3 is a no-op guard unless A1 stops deleting the file. Phase order A1 → A3 is correct. |
| **A2 → C3** | workflow `if:` / secrets (interface) | A2 rewrites `release.yml`'s `publish` job only | C3 changes `pr.yml` and `debug.yml` Telegram conditions only | No overlap. |
| **B3 → C3** | `pr.yml` → Telegram step | B3 rewrites `Rename` + `Upload artifact` | C3 rewrites the Telegram step's `if:` | No collision — different steps. B3's test reads `pr.yml`; C3's test reads `pr.yml` and `debug.yml`. Both must pass after both land. |
| **B2 → B3** | `APK_PATH` filename extension | B2 may emit `.zip` or `.7z` | B3's `ext="${APK_PATH##*.}"` derives the extension | Mechanism is correct, but B3's test forbids the string `.apk` from the whole `run` body, which its own comment violates — Defect 7. |
| **A2 (test) → C1** | `upload.py` caption assertions | A2 Step 5 lists caption re-verification | C1 Step 4 says "see the list in Task A2 Step 5" | Holds by reference. Verified A2's caption assertions are currently green against the real `CHANGELOG.md` (`v1.11.3` has Highlights, `v1.11.4` has none, `pr_caption` escapes). |

### The `APK_PATH` contract, stated exactly

> **B2 changes the meaning of `APK_PATH` in `$GITHUB_OUTPUT`: for `pr` and `debug` it becomes the path
> to a compressed archive (`.zip` at or below 15 MB, `.7z` above); for `release` it stays the raw
> `.apk`. `COMMIT_SUBJECT` and `COMMIT_SHA` are unchanged in name, format and value. Every consumer
> that assumes a fixed `.apk` extension must be updated in the same change. B3 is the consumer of
> record and is ordered after B2; `debug.yml` is an undeclared consumer that B3 does not cover
> (Defect 10).**

- **B2 ordered before B3?** Yes. B2 is Task B2, B3 is Task B3, in that order in the document, and the
  Phase B gate sequences `build_sh_test.sh` before `pr_workflow_test.py`.
- **Would B3's test fail if B2 had not landed?** **No — and this is worth flagging.** B3's test is a
  *static text* test: it reads `pr.yml` and `upload.py` and greps them. It never invokes `build.sh`
  or observes a real `APK_PATH`. With B2 reverted, `pr.yml` still contains
  `mv "$APK_PATH" "Hail-${PR_NUMBER}.apk"` and the test's `".apk" not in run` assertion still fails,
  and `upload.py` still contains the old `-Pabi=arm64-v8a` text and the other assertion still fails.
  B3's test therefore cannot distinguish "B2 landed" from "B2 did not land". The ordering is correct
  by construction and the plan states the dependency, but the test does not enforce it. A test that
  actually exercised the B2 → B3 handoff would need to run `build.sh` through the stub and feed its
  real output into the `Rename` logic.

---

## 4. Table B — per-task internal consistency

| Task | Tests vs. code | Files created vs. files later touched | Disagreements found |
|---|---|---|---|
| **A1** | `signing_test.sh` asserts rc 0, `RELEASE_KEYSTORE_PATH` in `$GITHUB_ENV`, file survives, sha256 and 2618-byte size match, and exit 1 + `Missing required environment variable: KEYSTORE` when unset. The implementation satisfies all of these — I ran a post-fix `signing.sh` and it behaves as asserted. | Creates `test/signing_test.sh`, modifies `signing.sh`. Consistent. | **Defect 5 (blocking).** `out="$(env -u KEYSTORE bash .github/scripts/signing.sh 2>&1)"; rc=$?` — the test does `source .github/scripts/lib.sh`, and `lib.sh:4` is `set -euo pipefail`. `signing.sh` exits 1 (the *expected* result), so `errexit` kills the test at the assignment, before `rc` is ever read. Verified: the test exits 1 and never prints `PASS: all assertions`, contradicting the plan's own Step 4 expectation. The five earlier assertions also cannot report cleanly for the same reason on any failure. |
| **A2** | `release_workflow_test.py` asserts six things about the `publish` job plus `field("parse_mode", "HTML")` in `upload.py`. The YAML replacement and the one-line `parse_mode` addition satisfy all seven. | Creates `test/release_workflow_test.py`; modifies `release.yml` and `upload.py`. Consistent. | None found. Caption re-verification in Step 5 is green against the real `CHANGELOG.md` (checked `v1.11.3`, `v1.11.4`, `pr_caption` escaping). |
| **A3** | `build_gradle_test.py` asserts the `release` block's `if` condition contains both `exists()` **and** `RELEASE_KEYSTORE_PATH`, plus four property checks and two minification checks. | Creates `test/build_gradle_test.py`; modifies `app/build.gradle.kts`. Consistent. | **Defect 6 (blocking).** I applied A3's replacement to the real `build.gradle.kts` and ran A3's own regex. Captured condition: `releaseKeystore != null && releaseKeystore.exists()`. `"exists()" in cond` → True. `"RELEASE_KEYSTORE_PATH" in cond` → **False**. The env-var name sits on the preceding `val` line, outside the `if (…) {` capture group. A3's test fails against A3's own fix. |
| **B1** | `zip_test.sh` asserts: small input → `.zip`; archive exists; flat members; byte-identical round trip; **exactly one stdout line**; missing input → non-zero + `::error::`; large input → `.7z` under 50 MB. | Creates `zip.sh` and `test/zip_test.sh`; appends to `lib.sh`. Consistent. | **Defect 2 (blocking), confirmed.** I ran the specified `zip.sh` against the specified `lib.sh`. Actual stdout is 4 lines: a blank line, `==> Compressing 0MB APK with zip`, `::notice::Archive is 0MB`, and the path. The plan's brief says "at least three" — the true count is **4**, because `step` prints a leading `\n` (`lib.sh:6`, `printf '\n==> %s\n'`). The assertion `[ "$lines" -eq 1 ]` fails. Also **Defect 3** (`require_env 1`) and **Defect 5** (`errexit` on the missing-input case). |
| **B2** | `build_sh_test.sh` asserts three exact argument lists, then the debug/release `APK_PATH` contract, then `COMMIT_SUBJECT`/`COMMIT_SHA`. The implementation produces the right arguments and the right contract. | Creates `test/build_sh_test.sh`; modifies `build.sh`. Consistent. | **Defect 11 (blocking).** The three argument-list assertions run *before* `fake_apk` creates any APK, so `single_match '*.apk'` calls `die` and `build.sh` exits 1. `lib.sh`'s `set -o pipefail` makes `bash … \| grep -q …` return 1 even though `grep` matched the text. Verified: the assertion reports `FAIL … mismatch` while the `GRADLE_ARGS` line is demonstrably present in the output. **Defect 4** (no `zip` guard) is the same root cause from the other side. |
| **B3** | `pr_workflow_test.py` asserts `".apk" not in Rename.run`, `"Hail-" in run`, the artifact `if-no-files-found`, five global constraints, and `"Pabi=arm64-v8a" not in upload.py`. | Creates `test/pr_workflow_test.py`; modifies `pr.yml` and `upload.py`. Consistent. | **Defect 1 (blocking)** — the replacement oversize text contains the literal `(-Pabi=arm64-v8a)`, so the assertion fails. **Defect 7 (blocking)** — the replacement `Rename` comment contains `forcing .apk`, so `".apk" not in run` also fails. Two independent self-contradictions in one task. |
| **C1** | `upload_missing_test.py` runs `upload.py` as a subprocess with a nonexistent `APK_PATH` and asserts: no `Traceback`, `::error::` present, non-zero exit. The replacement satisfies all three. | Creates `test/upload_missing_test.py`; modifies `upload.py`. Consistent. | **Low severity.** The "replace from" block quoted in Step 3 is not contiguous in the real file. Plan quotes `path = required("APK_PATH")` immediately followed by `size = pathlib.Path(path).stat().st_size`; the actual `main()` has `channel = required("TG_CHANNEL")` between them (`upload.py:165-167`). A literal find-and-replace fails; an implementer must splice around the `channel` line and is at risk of deleting it. |
| **C2** | `single_match_test.sh` asserts the returned path matches `*/app/build/outputs/*`, that the path exists, and that `lib.sh` contains `app/build/outputs`. | Creates `test/single_match_test.sh`; modifies `lib.sh`. Consistent. | **Defect 8 (blocking).** The test's probe is `got="$(cd / && single_match '*.apk')"`, on the stated rationale that the helper "must not be able to reach the decoy". But `repo_root_path` is `cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd` (`lib.sh:18`), and `lib.sh` was sourced via the *relative* path `.github/scripts/lib.sh`. From `/` that `cd` fails, `$root` is empty, `find` errors, and `die` fires. I ran C2's fixed `single_match` in a scratch tree: the assertion fails, before the fix *and* after. The test is broken independently of the implementation. Also low-severity: the `trap 'rm -rf app/build src/test/resources' EXIT` deletes `src/test/resources` unconditionally; the repo has no such directory today, so it is inert, but it would destroy real fixtures if one were ever added. |
| **C3** | `secret_gate_test.py` asserts `secrets.TG_TOKEN != ''`, `continue-on-error: true` and no `KEYSTORE` in both `pr.yml` and `debug.yml`. Current files have `if: success()` and one `continue-on-error: true` each, no `KEYSTORE` — so the gate assertions fail as the plan expects, and the specified change satisfies them. | Creates `test/secret_gate_test.py`; modifies `pr.yml` and `debug.yml`. Consistent. | None found. |
| **D1** | `version_bounds_test.sh` asserts overflow rejection at the guard, `v1.12.99` → `11299`, and that every `lib.sh` function has a caller. | Creates `test/version_bounds_test.sh`; modifies `version.sh` and `lib.sh`. Consistent. | **Defect 9 (blocking).** I ran the dead-code loop against the real tree. `warn` has exactly 1 occurrence repo-wide — its own definition in `lib.sh:8` — so the loop emits `FAIL lib.sh: warn is used`, and D1's Step 3 removes only `build_tools_dir`. The plan's own Step 2 predicts failure on `build_tools_dir is used`; it will also fail on `warn`, and that failure is never addressed. **Defect 5 (blocking)** also applies to the overflow-rejection case: the expected `::error::` comes from a non-zero exit, so `errexit` ends the test before the `if [ "$rc" -ne 0 ]` branch. |

---

## 5. Verification of the four suspected defects

### Defect 1 — B3's test contradicts B3's own implementation: **CONFIRMED**

The test asserts:

```python
check("Pabi=arm64-v8a" not in uploader,
      "upload.py no longer recommends per-ABI splitting",
      "APKs are compressed now; the remedy text is wrong")
```

B3's own replacement, quoted from the plan:

```python
f"Re-run with a per-ABI build (-Pabi=arm64-v8a) if the size itself "
```

The forbidden literal is present in the replacement text. The assertion is a plain substring test
over the whole of `upload.py`, so it fails. **The task that introduces the string is the task whose
test forbids it** — the assertion is unsatisfiable by the only implementation the plan specifies.
Settling text: `… Re-run with a per-ABI build (-Pabi=arm64-v8a) …` versus `check("Pabi=arm64-v8a" not in uploader, …)`.

Worth noting the intent is coherent — the message *is* still recommending per-ABI splitting, so the
test is detecting a real problem; the implementation simply does not fix the problem the test
detects. Either the remedy sentence must go, or the assertion must be narrowed.

### Defect 2 — B1's test asserts the wrong output shape: **CONFIRMED**

The test asserts:

```bash
lines="$(bash .github/scripts/zip.sh "$scratch/small/Hail-79.apk" | wc -l)"
[ "$lines" -eq 1 ]
```

Print sites, all in `lib.sh` and all on **stdout** (`printf` with no `>&2`):

| Site | Text | Newlines |
|---|---|---|
| `lib.sh:6` `step()` | `printf '\n==> %s\n'` | **2** — a leading blank line plus the message |
| `lib.sh:7` `note()` | `printf '::notice::%s\n'` | 1 |
| `zip.sh` `echo "$out"` | the path | 1 |
| | **total** | **4** |

Confirmed by running the specified `zip.sh` against the specified `lib.sh`:

```
$ bash .github/scripts/zip.sh $scratch/Hail-79.apk | wc -l
4
```

The count is **4, not 3** — the plan's own `step` helper emits a leading newline, so
`==> Compressing…` occupies two lines of the stream. `[ "$lines" -eq 1 ]` fails.

This also contradicts B1's own *Interfaces* block, which claims the script "Produces: prints the
absolute path of the created archive to stdout, one line." The implementation the same task
specifies prints four. B2's `| tail -1` is the correct consumer and works (verified: it yields the
path), so the fix belongs in the test and in the Interfaces prose, not in B2.

### Defect 3 — `require_env 1` in `zip.sh` is a no-op: **CONFIRMED**

`lib.sh:11-16`:

```bash
require_env() {
  local name
  for name in "$@"; do
    [ -n "${!name:-}" ] || die "Missing required environment variable: ${name}"
  done
}
```

The intent is obviously an arity check, presumably guarding `INPUT="$1"`. The loop treats `1` as an
environment-variable *name*. `${!name}` with `name=1` is bash's indirect expansion, and the only
binding called `1` in scope is the function's own first positional parameter, whose value is the
literal string `1` — the argument just passed. So the test is `[ -n "1" ]`: always true, always
passes, asserting nothing.

Confirmed by running it:

```
$ bash -c 'source lib.sh; require_env 1; echo "rc=$?"'
rc=0
```

The zero-argument case is no better:

```
$ bash -c 'source lib.sh; require_env; echo "rc=$?"'
rc=0          # the for loop body never runs
```

And `zip.sh` invoked with no argument at all:

```
$ bash .github/scripts/zip.sh
.github/scripts/zip.sh: line 6: $1: unbound variable
rc=1
```

So the line the plan presents as the script's argument guard catches nothing, and the real
behaviour for a missing argument is an `unbound variable` error from `set -u` rather than the
intended `die`. The failure is still non-zero and still mentions the problem, so this one is
cosmetic rather than blocking — but the line is dead code presented as a guard.

### Defect 4 — B2's test has no tool-availability guard: **CONFIRMED**

B1's test guards explicitly (`zip_test.sh`):

```bash
command -v zip >/dev/null 2>&1 || { echo "SKIP: zip not installed"; exit 0; }
command -v 7z  >/dev/null 2>&1 || note "7z absent; the large-size case will be skipped"
```

B2's test has no equivalent. It exercises the debug contract by running the real `build.sh`,
which under B2's implementation calls the real `zip.sh`; with a 200 KB fake APK, `apk_mb` is 0, so
the `else` branch runs `require_command zip`. With `zip` absent, `zip.sh` dies, `build.sh` inherits
`set -e` from `lib.sh` and aborts before writing `GITHUB_OUTPUT`, and the test's
`sed -n 's/^APK_PATH=//p'` reads nothing.

Confirmed by running B2's exact implementation with `zip` removed from `PATH`:

```
$ env PATH=<no zip> GITHUB_OUTPUT=out/gout bash .github/scripts/build.sh debug
(exit 1 — zip.sh: ::error::Missing required command: zip)
$ sed -n 's/^APK_PATH=//p' out/gout
APK_PATH=[]        → assertion "debug publishes the compressed archive" FAILS
```

With `zip` present, the same code path produces `…/app-debug.zip` and the assertion passes. So B2's
test is environment-dependent in a way B1's is not. On the CI runner the plan's Global Constraints
say `zip` is preinstalled, so this bites locally and in review rather than in production — but the
plan's own Phase C gate demands "the whole set must be runnable without the Android SDK and without a
network" on any contributor's machine, which this breaks.

---

## 6. Further defects found during the scan

Seven additional defects, each verified by executing the plan's own code. All are blocking in the
sense that the named task's test fails against that task's own implementation.

**Defect 5 (A1, B1, D1) — `errexit` in the test harness aborts before the summary.**
Every shell test begins `source .github/scripts/lib.sh`, and `lib.sh:4` is `set -euo pipefail`. Each
then uses the idiom `out="$(cmd 2>&1)"; rc=$?` to *expect* a non-zero exit. Under `errexit` the
assignment itself fails and the script terminates — `rc` is never assigned, the assertion never runs,
and the final `PASS: all assertions` line is never printed. Verified for A1 (post-fix `signing.sh`
returns 1 on the unset-`KEYSTORE` case: test exits 1, no summary), for B1's missing-input case, and
for D1's overflow case. The plan's own Step 4 for A1 states "Expected: `PASS: all assertions`, exit
0", which is unreachable. Fix: `set +e` around those invocations, or `out="$(cmd 2>&1)" && rc=0 ||
rc=$?`.

**Defect 6 (A3) — the guard assertion captures the wrong text.** Asserted:
`check("RELEASE_KEYSTORE_PATH" in cond, "the guard still names the env var")`, where `cond` comes
from `re.search(r"if \((.*?)\) \{", block)`. A3's own replacement is:

```kotlin
val releaseKeystore = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }
if (releaseKeystore != null && releaseKeystore.exists()) {
```

The env-var name is on the `val` line, outside the capture group. Verified by applying the
replacement to the real `build.gradle.kts` and running the real regex: `cond` =
`'releaseKeystore != null && releaseKeystore.exists()'`, `exists()` → True,
`RELEASE_KEYSTORE_PATH` → **False**. A3 cannot go green.

**Defect 7 (B3) — the `Rename` assertion is defeated by B3's own comment.** Asserted:
`check(".apk" not in run, "Rename does not hardcode a .apk extension")`. B3's replacement `run`
body is:

```yaml
# build.sh compresses pr builds, so the extension may be .zip or .7z.
# Derive it from the path rather than forcing .apk, which would
# produce a file that is not an APK.
ext="${APK_PATH##*.}"
mv "$APK_PATH" "Hail-${PR_NUMBER}.${ext}"
```

The comment contains the forbidden literal. Verified: `".apk" not in run` → False. The assertion
should target the `mv` line, not the whole body.

**Defect 8 (C2) — the test's `cd /` breaks the helper under test.** `got="$(cd / && single_match
'*.apk')"` is intended to prove the helper cannot reach a decoy outside the build output.
`repo_root_path` resolves `dirname "${BASH_SOURCE[0]}"`, and `lib.sh` was sourced as the *relative*
path `.github/scripts/lib.sh`. After `cd /` that `cd` fails, `$root` is empty, `find` errors and
`die` fires. Verified against C2's own fixed `single_match` in a scratch tree:

```
$ bash t.sh
lib.sh: line 3: cd: .github/scripts/../..: No such file or directory
::error::No file matching '*.apk' under /app/build/outputs
exit=1
```

Fails before the fix and after it. The assertion tests the *test's* `cd`, not the scoping.

**Defect 9 (D1) — the dead-code loop catches a function the plan does not remove.** D1's loop
requires every `lib.sh` function to have more than one occurrence across `.github/scripts/*.sh`.
Verified against the real tree:

```
  ok   step (n=4)          ok   note (n=5)        ok   die (n=8)
  ok   require_env (n=5)   ok   repo_root_path (n=3)  ok   repo_root (n=4)
  ok   single_match (n=2)
  FAIL warn (n=1)          FAIL build_tools_dir (n=1)
```

`build_tools_dir` is the one the plan predicts and removes. `warn` (`lib.sh:8`) is also
single-occurrence — it is dead code too, and Step 3 never mentions it, so the test still fails
after the fix.

**Defect 10 (B2 → `debug.yml`) — an undeclared consumer of the changed `APK_PATH`.** B2 compresses
`pr` **and** `debug`. B3 updates `pr.yml`'s `Rename` and `Upload artifact`, and its test only reads
`pr.yml`. `debug.yml:66` remains:

```yaml
      - name: Rename
        run: mv "$APK_PATH" HailBug.apk
      - name: Upload artifact
        with:
          path: HailBug.apk
```

A `.zip`/`.7z` gets renamed to `.apk` and published under that name. C3 touches `debug.yml` but only
the Telegram step's `if:`. No task in the plan updates `debug.yml`'s `Rename`, and no test asserts on
it. The plan's Self-review claims "every consumer is updated in B2 and B3" — that is not true for
the debug path.

**Defect 11 (B2) — the argument-list assertions cannot pass.** The three `grep -q` assertions run
before `fake_apk` creates any APK, so `single_match '*.apk'` calls `die` and `build.sh` exits 1.
`lib.sh`'s `set -o pipefail` makes `bash build.sh … 2>&1 | grep -q …` return 1 even though `grep`
matched. Verified with a faithful stand-in: the `GRADLE_ARGS` line is present in the stream and the
assertion still reports `FAIL … mismatch`.

Lower severity, same family: **C1's "replace from" block is not contiguous** in `upload.py`
(`channel = required("TG_CHANNEL")` sits between the two quoted lines), so a literal find-and-replace
fails; and **C2's `trap … rm -rf src/test/resources`** deletes a path the repo does not currently
have, which is inert today but destructive if fixtures are ever added.

---

## 7. Summary

| Suspected defect | Ruling |
|---|---|
| 1 — B3 test vs. B3 implementation (`Pabi=arm64-v8a`) | **Confirmed** |
| 2 — B1 test asserts one stdout line; actual is **4** | **Confirmed** |
| 3 — `require_env 1` is a no-op | **Confirmed** |
| 4 — B2 test has no `command -v zip` guard | **Confirmed** |

Seven further defects found: 5 (`errexit` in three test harnesses), 6 (A3 guard assertion), 7 (B3
`Rename` comment), 8 (C2 `cd /`), 9 (D1 `warn`), 10 (`debug.yml` undeclared consumer), 11 (B2
argument lists). Plus two low-severity notes on C1 and C2.

Nothing was implemented. The worktree is clean at `9241666`.
