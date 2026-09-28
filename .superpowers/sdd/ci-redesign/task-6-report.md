# Task 6 report — `.github/workflows/pr.yml`

**Status:** DONE_WITH_CONCERNS
**Branch:** `fix/ci-pr` (worktree `.claude/worktrees/task-6`, based on `fix/ci-rewrite` @ `cd368df`)
**Commit:** `cb2ba0115b2899576f53c4bfd8ee271612a72e97`
**Files changed:** `.github/workflows/pr.yml` only. New file, 76 lines. Nothing else touched.

---

## Deviation from the supplied file text — one, deliberate

The YAML given in the task prompt failed the task's own verification step
"no `${{ }}` inside any `run:` body":

```yaml
      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
        run: mv "$APK_PATH" "Hail-${{ github.event.pull_request.number }}.apk"
                                   # ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ expression in a run body
```

The check raised: `Error: expression in run: check/Rename`.

`github.event.pull_request.number` is numerically typed by GitHub, so this is not
directly exploitable today. It is still a violation of the constraint the redesign
exists to enforce, and Task 8's already-merged `release.yml` solves the identical
step correctly:

```yaml
      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
          VERSION: ${{ steps.version.outputs.RELEASE_VERSION_NAME }}
        run: mv "$APK_PATH" "Hail-${VERSION}.apk"
```

Committed form follows that precedent:

```yaml
      - name: Rename
        env:
          APK_PATH: ${{ steps.build.outputs.APK_PATH }}
          PR_NUMBER: ${{ github.event.pull_request.number }}
        run: mv "$APK_PATH" "Hail-${PR_NUMBER}.apk"
```

Behaviour is identical: same source, same `Hail-<n>.apk` target, same `apk-pr<n>`
artifact. This is the same class of ruling already in the ledger for the empty
`KEYSTORE` case — the plan text is wrong, the implementation is right.
**Cost if the deviation is unwanted: re-edit one step, nothing else.**

---

## Verification results

Tooling note: neither PyYAML nor `js-yaml` was available in the container
(`python3 -c "import yaml"` → `ModuleNotFoundError`; no `pip`; `require('js-yaml')`
→ `MODULE_NOT_FOUND`). `js-yaml` was installed into a scratch dir under
`/tmp/agent_f2b1e9c3-e021-43a7-8b77-d6db73ac993f/yamllib` and reached via
`NODE_PATH`. No repository or system file was changed to make verification pass.

| Check | Result |
|---|---|
| RED — file absent before | `ls: cannot access '.github/workflows/pr.yml': No such file or directory` (exit 2) |
| YAML parses | PASS — top-level keys `["name","on","permissions","concurrency","jobs"]` |
| `permissions` is exactly `{contents: read}` | PASS — single key, no write/id-token/package scopes |
| No `KEYSTORE*` anywhere | PASS — `OK: no signing secrets` |
| No `${{ }}` in any `run:` body | PASS **after the fix above** (failed before it) |
| No `pull_request_target`, no `contents: write` | PASS |
| Secrets referenced | only `secrets.TG_TOKEN`, `secrets.TG_GROUP` |
| Triggers | PASS — `pull_request` with no filters, and nothing else |
| Concurrency | `group: ${{ github.workflow }}-${{ github.event.pull_request.number }}`, `cancel-in-progress: true` |
| First step | `actions/checkout@v7` |
| Action pins | `checkout@v7`, `setup-java@v5`, `gradle/actions/setup-gradle@v6`, `setup-android@v4`, `upload-artifact@v7` (x2) — all as specified; no `setup-java@v6`, no `cache: gradle` |
| SDK package | `platform-tools platforms;android-37.0 build-tools;37.0.0` |
| Telegram env vs `upload.py` | PASS — exact match, no missing and none superfluous: `APK_PATH`, `PR_NUMBER`, `PR_TITLE`, `TG_CHANNEL`, `TG_TOKEN`, `TG_GROUP` |
| `TG_CHANNEL: pr` resolves | PASS — `pr` → topic `218` in `.github/telegram.json` |
| Staged diff | 1 file, 76 insertions, 0 deletions. Working tree clean after commit. |

Every `run:` body, in order:

```
[Unit tests]        .github/scripts/test.sh
[Build]             .github/scripts/build.sh pr
[Rename]            mv "$APK_PATH" "Hail-${PR_NUMBER}.apk"
[Upload to Telegram] python3 .github/scripts/upload.py
```

All three scripts are invoked as executables, never `source`d — the constraint
that stops `bash -c 'source test.sh'` from returning 0 over a failing script.

### SDK package names verified against the live repository

`https://dl.google.com/android/repository/repository2-3.xml` lists
`platforms;android-{30..36, 36.1, 37.0, 37.1, 37.2}` and
`build-tools;{35.0.0, 35.0.1, 36.0.0, 36.1.0, 37.0.0}`.

There is **no** `platforms;android-37`. The ledger ruling is confirmed correct,
and the prompt's text already carried the fixed value — the plan's own Task 6
snippet still says `platforms;android-37` and would have failed at setup-android.
**Task 7 will hit the same stale string; fix it there when editing debug.yml.**

---

## Contract cross-checks against the sibling branches

- `fix/ci-test:.github/scripts/test.sh` runs `:app:testDebugUnitTest` and documents
  that reports land in `app/build/reports/tests/testDebugUnitTest` — matches the
  `Upload test report` `path:` exactly.
- `fix/ci-build:.github/scripts/build.sh` takes `pr`, calls `require_env PR_NUMBER`
  (the Build step supplies it), and writes `APK_PATH` plus `COMMIT_SUBJECT` to
  `$GITHUB_OUTPUT`; the Rename step reads `steps.build.outputs.APK_PATH`. `APK_PATH`
  is absolute (`single_match` uses `repo_root_path`), so `mv` resolves from any cwd.

## Discrepancies reported, not fixed

`.github/scripts/` on this branch holds `lib.sh` and `upload.py`, plus the seven
legacy scripts (`debug.sh`, `pr.sh`, `release.sh`, `setup.sh`, `tg_body.sh`,
`tg_debug.sh`, `tg_release.sh`, `upload.sh`, `zip.sh`) that Task 9 removes.

**`test.sh` and `build.sh` are absent from this branch**, as expected. They are owned
by `fix/ci-test` and `fix/ci-build` and both carry mode `100755`, so they will be
executable once merged. `pr.yml` will not run until Tasks 3 and 4 land. Not created
here, per instructions.

## Self-review notes

- `Upload artifact` (index 8) precedes `Upload to Telegram` (index 9), so the APK
  exists in Actions even if Telegram is unreachable. `continue-on-error: true` on
  the Telegram step keeps a Telegram outage from turning a green build red.
- `if: success()` on the Telegram step is redundant with the default and with
  `continue-on-error`, but harmless and explicit — the job is already failed by
  then if tests or build failed, so Telegram is correctly skipped. It does **not**
  re-evaluate to `false` just because a `continue-on-error` step ran, and nothing
  follows it.
- `if-no-files-found: error` on the artifact catches a silently empty build: if
  `single_match` ever returned a stale APK that `mv` moved away, the upload fails
  loudly rather than producing an empty artifact. The test report uses
  `if-no-files-found: ignore` because a Gradle crash can precede report generation.
- `if: failure()` on the test report means the step is skipped on a green run, as
  the plan's Step 6 checklist expects.

### Low-severity, left as-is for consistency

`accept-android-sdk-licenses: yes` is unquoted, so YAML 1.1 parses it as boolean
`true` rather than the string `"yes"`. GitHub coerces action inputs to strings and
`android-actions/setup-android` treats the value as truthy, and the already-merged
`release.yml` uses the same unquoted form — so changing it here would introduce an
inconsistency between the two workflows. Flagged rather than changed. If it ever
misbehaves, quote it as `'yes'` in all three workflow files together.

## Not verified

Nothing was executed against GitHub Actions, the Android SDK, Gradle, or Telegram.
Plan Steps 6 and 7 (open a throwaway PR, confirm the check name, confirm the APK
lands in topic `pr`, confirm `com.aistra.hail.pr.<n>` installs, then push a second
commit and confirm the signature is stable across installs) need a human with
repository write access. Note that per the ledger ruling the installed id is
`com.aistra.hail.pr79`, **not** `com.aistra.hail.pr.79` as the plan's Step 6
checklist states.
