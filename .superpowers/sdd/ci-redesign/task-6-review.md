# Task 6 review — `.github/workflows/pr.yml`

**Reviewed:** `cb2ba01` on `fix/ci-pr`, diff `cd368df..fix/ci-pr` (1 file, +76).
**Brief:** not on disk; the requirements in the review request were used as the spec.
**Sibling files consulted for contract checks only** (not reviewed): `fix/ci-release:.github/workflows/release.yml`, `fix/ci-build:.github/scripts/{build.sh,lib.sh}`, `fix/ci-test:.github/scripts/test.sh`, `fix/ci-upload:.github/scripts/upload.py`, `.github/telegram.json`.

## Verdicts

- **Spec compliance: ✅**
- **Task quality: Approved**

Every binding requirement holds in the written file. No Critical findings. Three Minor
nits and two non-blocking notes follow; none of them require a change before merge.

---

## Requirement-by-requirement

| Requirement | Verdict | Evidence |
|---|---|---|
| Triggers on every `pull_request` | ✅ | `pr.yml:3-4` — `on: pull_request:` with no type/branch/path filters |
| Runs the JVM unit tests | ✅ | `pr.yml:36-37` → `.github/scripts/test.sh` → `:app:testDebugUnitTest` |
| HTML report only `if: failure()` | ✅ | `pr.yml:39-45`; `if-no-files-found: ignore` because a Gradle crash can precede report generation |
| Builds a PR APK | ✅ | `pr.yml:47-51` → `.github/scripts/build.sh pr` → `:app:assemblePr` with `PR_NUMBER` supplied |
| Artifact named `Hail-<PR number>.apk` | ✅ | `pr.yml:53-57` renames to `Hail-${PR_NUMBER}.apk`; the brief's own snippet names the *file*, which is what this satisfies. Artifact container is `apk-pr<n>`, matching `release.yml:89`'s `apk-${version}` convention |
| Telegram upload, non-fatal | ✅ | `pr.yml:66-76`, `continue-on-error: true` at line 68 |
| `permissions: contents: read` | ✅ | `pr.yml:6-7` — single key; no `write`, no `id-token`, no `packages` |
| No signing secret referenced | ✅ | Full-file read: the only secrets are `TG_TOKEN` and `TG_GROUP` (lines 74-75) |
| Five action pins | ✅ | `checkout@v7` (19), `setup-java@v5` (22), `setup-gradle@v6` (28), `setup-android@v4` (31), `upload-artifact@v7` (41, 60) |
| `platforms;android-37.0 build-tools;37.0.0` | ✅ | `pr.yml:33` — correct three-part form |
| `PR_NUMBER`, `PR_TITLE`, `TG_CHANNEL: pr`, `TG_TOKEN`, `TG_GROUP` | ✅ | `pr.yml:70-75` |

### Binding constraints

- **No `${{ }}` in any `run:` body.** ✅ Verified by reading all four `run:` lines
  (37, 51, 57, 76) plus the absence of any `run: |` block. Every dynamic value reaches
  a script through `env:` (lines 49-50, 54-56, 69-75). The shell-injection hole is closed.
- **Scripts invoked as executables, never `source`d.** ✅ Lines 37, 51, 76 all call the
  path directly, so the shebang runs a child process and its exit status propagates. The
  `bash -c 'source test.sh'` zero-exit trap is structurally unavailable here.
- **Cannot read the release signing key under any branch scenario.** ✅ The release
  secrets (`KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS`,
  `KEYSTORE_ALIAS_PASSWORD` — all present in `release.yml:67,75-77`) appear nowhere in
  this file. `contents: read` cannot read repository secrets, and the token is not
  elevated to `contents: write` anywhere in the job. See the residual-risk note below
  for the one thing this constraint does *not* cover.
- **Artifact stored before Telegram.** ✅ `Upload artifact` is line 59; `Upload to
  Telegram` is line 66. A Telegram outage cannot lose the build, and the ordering is
  not merely adjacent — the Telegram step is gated on `success()`, so if the artifact
  upload fails at all, Telegram is skipped rather than racing it.

---

## Control flow

Traced against GitHub Actions semantics (`if:` defaults to `success()`, which is a
sticky "all previous steps succeeded" and is not reset by a later `continue-on-error`
step or by a subsequent successful step).

| Scenario | Outcome | Correct? |
|---|---|---|
| Tests fail | `Upload test report` runs (`if: failure()`); `Build`, `Rename`, `Upload artifact`, `Upload to Telegram` all skip (default `if: success()` is false). Job red. | ✅ |
| Tests pass, build fails | `Build` fails; `Rename`/`Upload artifact`/Telegram skip. `Upload test report` does *not* run — `if: failure()` was already evaluated and is false at that point in the run. Job red with no stray artifact. | ✅ |
| Tests pass, build passes | `Rename` → `Upload artifact` → `Upload to Telegram`. Job green. | ✅ |
| Telegram unreachable / secret absent (fork PR) | `upload.py` `required("TG_TOKEN")` exits 1; `continue-on-error: true` absorbs it. Job stays green, artifact already stored. | ✅ |
| Run cancelled (superseded push) | `if: failure()` is false on cancellation, so no report upload. `cancel-in-progress: true` makes this the right outcome — you do not want a stale report from a superseded commit. | ✅ |
| `single_match` returned a stale APK that `mv` moved away | `if-no-files-found: error` on line 64 fails the job loudly instead of publishing an empty artifact. | ✅ |

The report's note that `if: success()` on the Telegram step is "redundant but harmless"
is accurate; it is not redundant with `continue-on-error` (which controls job outcome,
not step execution), and it is doing real work by suppressing Telegram on a red build.

---

## The deviation

**Correct, complete, and consistent. I endorse it.**

- The brief's own YAML is self-defeating: it would have failed the brief's own
  verification rule, which the implementer's own tooling caught. The implementation is
  the thing that is right.
- `pr.yml:53-57` is structurally identical to `release.yml:80-84` — a two-variable
  `env:` block feeding the same `mv "$APK_PATH" "Hail-${VAR}.apk"` shape. The redesign's
  two deliverable workflows now express the rename rule one way.
- Behaviour is provably unchanged. `steps.build.outputs.APK_PATH` is absolute
  (`lib.sh:30` roots `single_match` at `repo_root_path`), so `mv` resolves from the
  step's cwd regardless. Target is byte-identical: `Hail-<n>.apk`. Downstream references
  — the artifact name `apk-pr<n>` (line 62), the artifact `path` (line 63), and the
  Telegram `APK_PATH` (line 70) — all still line up.
- `PR_NUMBER` is declared twice, at line 50 and line 62. That is **not** redundancy:
  `env:` is per-step, and `Build` genuinely needs it for `build.sh`'s
  `require_env PR_NUMBER`. This mirrors `release.yml`, which likewise re-declares
  `APK_PATH` in both the `Build` and `Rename` steps.
- `github.event.pull_request.number` is integer-typed by GitHub, so nothing was being
  defended against today. But the constraint is a rule, not a threat model, and the
  fix is one line. Reverting it would reintroduce a `${{ }}` in a `run:` body purely to
  reproduce a rule violation the implementer had already identified.

---

## Findings

**Minor — `pr.yml:63` — the artifact filename is re-derived by a second, independent
expression.** Line 57 derives the name from the `env:`-sourced `PR_NUMBER`; line 63
spells it out again as `path: Hail-${{ github.event.pull_request.number }}.apk`. Both
resolve identically today, and the expression is in a `with:` value so the binding
constraint is not violated — but the naming rule now lives in two places, and a future
rename that touched only one would produce a green-looking run with a missing artifact
(which `if-no-files-found: error` would catch, so the failure mode is loud, not silent).
`release.yml:90` sidesteps this with the glob `Hail-*.apk`. Either form is acceptable;
this is a maintainability note, not a request to change it.

**Minor — `pr.yml:67 — `if: success()` diverges from `release.yml:112`'s `if: always()`.**
For a PR the divergence is the better choice — under `always()`, a failed build would
attempt a Telegram send against a nonexistent APK, and `upload.py` would `stat()` a
missing path and crash. So the asymmetry is correct here. Flagging it so it reads as a
decision rather than an accident, since the two workflows now sit side by side.

**Minor — `pr.yml:34 — the report's `accept-android-sdk-licenses: yes` reasoning is
wrong, though its conclusion is right.** The report attributes the value to YAML 1.1
boolean coercion. GitHub Actions does not use a YAML 1.1 parser: the same parser that
keeps the `on:` key at line 3 from becoming boolean `true` uses the YAML 1.2 core
schema, under which `yes` is the string `"yes"`. The action receives `yes`. The
conclusion — leave it, matching `release.yml:41` — stands, and changing it in only one
of the three workflows would be worse. Recording this so a later reader does not
"repair" the quoting on a false premise.

**Minor — `pr.yml:33 — `platform-tools` is an extra package** beyond the brief's
`platforms;android-37.0 build-tools;37.0.0`. Benign, and identical to `release.yml:40`.
Noted only because the brief's list was exhaustive.

---

## Noted separately — not verifiable from this diff, not blocking

1. **`test.sh` and `build.sh` are absent from `fix/ci-pr`.** Confirmed: `.github/scripts/`
   on this branch holds `lib.sh`, `upload.py`, and the nine legacy scripts Task 9 removes.
   **The workflow is inert until Tasks 3 and 4 merge** — it will fail at the `Unit tests`
   step, not at parse time, so a CI run on this branch in isolation is expected to be red
   and should not be read as a defect. I verified on their branches that both are mode
   `100755`, so the executable bit the workflow depends on survives the merge. The report
   discloses this correctly.
2. **`.github/.java-version` exists** at `cd368df`, so `setup-java`'s
   `java-version-file` input resolves. Checked, because a missing file would have been a
   silent hard failure at line 25 that no amount of YAML review would catch.
3. **`upload.py`'s environment contract matches exactly** — I read `fix/ci-upload`'s copy
   rather than relying on the report. For `channel == "pr"` the script reads exactly
   `APK_PATH`, `PR_NUMBER`, `PR_TITLE`, `TG_CHANNEL`, `TG_TOKEN`, `TG_GROUP`; nothing is
   missing and nothing superfluous. `COMMIT_SUBJECT` is read only on the `debug` branch of
   the caption dispatch, so its absence here is correct, not an oversight.
4. **All five pinned majors exist upstream** (checked against the GitHub API, not assumed):
   `checkout` v7, `setup-java` v5, `upload-artifact` v7, `gradle/actions` v6,
   `setup-android` v4. Note `actions/setup-java` also has a v6 published; the brief binds
   v5 and `release.yml:29` uses v5, so v5 is the correct call for cross-workflow
   consistency and is not a finding against this file.
5. **Residual risk the signing-key constraint does not cover.** `pull_request` (unlike
   `pull_request_target`) runs the workflow definition *from the PR head*, and same-repo
   branches receive real secrets. A contributor with push access can therefore edit
   `pr.yml` to print `secrets.TG_TOKEN`. That is a property of any `pull_request` workflow
   that consumes secrets, not a defect in this file — and the exposure is the Telegram
   bot token, never the release signing key, which this file cannot reach by any route.
   Worth recording in the ledger so a future reviewer does not read the signing-key
   constraint as a general write-access boundary. Tightening it would mean
   `pull_request_target` with a job-level gate — the exact pattern this redesign removed
   — so I do not recommend it.
6. **No behaviour was exercised against GitHub Actions, the Android SDK, Gradle, or
   Telegram.** The control-flow table above is derived from Actions semantics, not from
   a live run. The report's plan Steps 6-7 still need a human with repository write
   access.
