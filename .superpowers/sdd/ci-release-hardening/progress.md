# SDD ledger — plan: plans/2026-09-29-ci-release-hardening.md

## Setup (pre-flight scan only — no implementation)

- Worktree: `.claude/worktrees/sdd-ci`, branch `fix/ci-hardening` created from `origin/fix/ci-rewrite`.
- Base commit: `9241666cfc553a6c3cbee32d1c091ac2a75814ed` ("ci: quote-free boolean SDK licenses and retire the old pipeline").
- Prior ledger: none. `.superpowers/sdd/ci-release-hardening/` did not exist; the old flat
  `.superpowers/sdd/progress.md` does not exist either. `.superpowers/sdd/ci-redesign/` is a
  different plan and was left untouched.
- Scan written to `.superpowers/sdd/ci-release-hardening/scan.md`.

### Rulings — the four suspected defects

| # | Suspected defect | Ruling |
|---|---|---|
| 1 | B3's test asserts `"Pabi=arm64-v8a" not in uploader`, but B3's replacement text contains `(-Pabi=arm64-v8a)` | **CONFIRMED** |
| 2 | B1's test asserts `wc -l` equals 1, but `step` and `note` print to stdout | **CONFIRMED** — actual count is 4, not 3 |
| 3 | `require_env 1` in `zip.sh` is a no-op | **CONFIRMED** |
| 4 | B2's test has no `command -v zip` guard, unlike B1's | **CONFIRMED** |

### Rulings — further defects found during the scan (7 additional)

| # | Defect | Task | Severity |
|---|---|---|---|
| 5 | Test harness aborts on `errexit` before its own summary — `source lib.sh` turns on `set -euo pipefail`, so `out="$(cmd)"; rc=$?` kills the script whenever `cmd` exits non-zero, which is exactly the *expected* case in the assertion that follows | A1, B1, D1 | **Blocking** |
| 6 | A3's test asserts `"RELEASE_KEYSTORE_PATH" in cond`, but A3's own `if` condition is `releaseKeystore != null && releaseKeystore.exists()`; the env-var name lives on the preceding `val` line, outside the captured group | A3 | **Blocking** |
| 7 | B3's test asserts `".apk" not in run`, but B3's own replacement comment contains `forcing .apk` | B3 | **Blocking** |
| 8 | C2's test does `cd / && single_match`, and `repo_root_path` resolves `${BASH_SOURCE[0]}` — a *relative* path once `lib.sh` was sourced relatively. From `/` the `cd` fails, `root` is empty, `find` errors, and `die` fires. The assertion cannot pass before or after the fix | C2 | **Blocking** |
| 9 | D1's dead-code loop requires every function in `lib.sh` to have a caller. `warn` has exactly one occurrence repo-wide (its own definition) and D1 removes only `build_tools_dir` | D1 | **Blocking** |
| 10 | B2 compresses `debug` as well as `pr`, but B3 updates only `pr.yml`'s `Rename`. `debug.yml:66` stays `mv "$APK_PATH" HailBug.apk`, renaming a `.zip`/`.7z` to `.apk`. No task touches it | B2 → debug.yml | **Blocking, unreached consumer** |
| 11 | B2's three argument-list assertions cannot pass. At that point no APK exists, so `single_match` calls `die` and `build.sh` exits 1; `lib.sh`'s `set -o pipefail` makes the `… \| grep -q …` pipeline return 1 even though `grep` matched | B2 | **Blocking** |

Two lower-severity notes are recorded in `scan.md` (C1's non-contiguous "replace from" block; C2's
`trap … rm -rf src/test/resources` on a directory the repo does not have).

### Verdict

The plan is not executable as written. Defects 5–11 are not cosmetic: each makes a task's own test
fail against that task's own implementation, so the Phase A/B/C/D gates cannot be reached. Recommend
a plan revision before any task is dispatched to a worker.
