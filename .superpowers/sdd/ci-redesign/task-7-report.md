# Task 7 report — `.github/workflows/debug.yml`

**Status: DONE_WITH_CONCERNS**
**Commit: `c9eee86`** on `fix/ci-debug`, branched from `fix/ci-rewrite` at `cd368df`.
**One new file, nothing else touched:** `git status --short` showed only `A .github/workflows/debug.yml`.

## Scope

The manual escape hatch. A maintainer whose local hardware is too weak to build dispatches this
workflow on any ref and gets a debug-signed APK stored as a workflow artifact and delivered to the
debug Telegram topic (id 84, from `.github/telegram.json`). Releases stay on the separate
tag-triggered `release.yml` owned by Task 8; this workflow never runs that path.

## Verification

RED first: `ls .github/workflows/debug.yml` → `No such file or directory`.

| Check | Result |
|---|---|
| YAML parses (js-yaml) | pass |
| `permissions` exactly `{contents: 'read'}` | `permissions OK` |
| No `KEYSTORE*` reference | `OK: no signing secrets` |
| No `${{ }}` in any `run:` body | `OK: no expressions in any run block` |
| `workflow_dispatch` present, `ref` required, default `main` | `trigger OK` |
| Telegram env vs `upload.py` | exact match, both directions |
| `COMMIT_SUBJECT` emitted by `build.sh` | confirmed on `fix/ci-build:42` |
| Diff vs reviewed `pr.yml` | 9 hunks, all intended |

Note on tooling: `mkdir -p /tmp/yamlcheck` was denied by project policy
(`{"permission":"external_directory"}`). The js-yaml install from Task 6 survives at
`/tmp/agent_f2b1e9c3-e021-43a7-8b77-d6db73ac993f/yamllib/node_modules` (ledger line 12), so the same
four checks were run through `NODE_PATH` against that copy instead.

### Telegram env cross-check against `upload.py`

Every key supplied is read, and every key the debug path needs is supplied:

- `APK_PATH` — `required()`, `upload.py:165`
- `TG_CHANNEL: debug` — `required()`, `:166`, routed to the debug branch at `:184`
- `GITHUB_SHA` — `:186`
- `COMMIT_SUBJECT` — `:186`, emitted by `build.sh` (`fix/ci-build:42`, `git log -1 --pretty=%s`)
- `TG_TOKEN` / `TG_GROUP` — `required()`, `:192` / `:193`

No unused keys. `PR_NUMBER`, `PR_TITLE` and `RELEASE_TAG` are correctly absent.

### Consistency with the reviewed `pr.yml`

Every diff hunk is one of the seven intended changes: workflow name; trigger
`pull_request` → `workflow_dispatch` with the `ref` input; concurrency key (PR number → `github.ref`);
checkout `with: ref` + `fetch-depth: 0`; report/artifact names; task `pr` → `debug`; the Rename and
artifact path collapsing to the literal `HailBug.apk`; and the Telegram env swapping `PR_NUMBER` /
`PR_TITLE` for `GITHUB_SHA` / `COMMIT_SUBJECT` with the channel set to `debug`. Nothing else differs.

The `Rename` step carries no `${{ }}` in its `run:` body and the artifact step uses the literal
`HailBug.apk`, the shape `pr.yml` was reviewed on. Here the literal is also strictly correct rather
than a tolerated deviation: the filename has no dynamic part to re-derive, so the re-derived form
the PR review called a Minor does not arise at all.

## Self-review

- **`Upload artifact` precedes `Upload to Telegram`** — the APK is durably stored even if Telegram
  rejects the upload.
- **`if: success()`** — a failed `Unit tests` step fails the job, so Build, Rename, Upload artifact
  and Telegram are all skipped. A red test can never be followed by a delivered APK.
- **`if-no-files-found: error`** — an empty or missing APK fails the job rather than silently
  producing an empty artifact.
- **`fetch-depth: 0` is required, not merely tidy.** `build.sh` derives `COMMIT_SUBJECT` from
  `git log -1`; at the default shallow depth of 1 against an arbitrary branch or tag that history may
  be absent and the caption would be blank. Full depth is what makes the debug caption correct.
- **`continue-on-error: true` on Telegram** — a delivery failure costs the notification, not the
  already-stored artifact.

## Constraints held

No write scope and no `KEYSTORE`, `KEYSTORE_PASSWORD`, `KEYSTORE_ALIAS` or
`KEYSTORE_ALIAS_PASSWORD`: the debug build is signed by the committed test key inside
`app/build.gradle.kts`, never a secret. Scripts are invoked as executables, never `source`d —
`test.sh` is mode 100755 on `fix/ci-test`, so direct invocation works and a non-zero exit propagates
instead of being swallowed by `set -e`. Pinned actions are `checkout@v7`, `setup-java@v5`,
`gradle/actions/setup-gradle@v6`, `upload-artifact@v7`, `setup-android@v4`; `setup-java@v6` is not
used, and Gradle caching comes from `setup-gradle`, not `setup-java`'s `cache: gradle`. The platform
package is `platforms;android-37.0`, the corrected value, not the `platforms;android-37` the plan
text still carries. The `debug` task passes no `-P` overrides, so applicationId
`com.aistra.hail.debug` and versionName `<base>-Debug` both come from the build type.

## Concerns

**1. `GITHUB_SHA` does not follow `inputs.ref` (functional, not fixed here).**
`${{ github.sha }}` on `workflow_dispatch` is the SHA of the ref the dispatch was started *from*, not
of the checked-out `inputs.ref`. Whenever the maintainer names a ref different from their currently
selected branch, `debug_caption` (`upload.py:65`) links a commit that was not built, while
`COMMIT_SUBJECT` — taken from `git log -1` after checkout — correctly describes the built commit. The
caption then pairs a wrong SHA with a right subject. There is no in-scope fix: `build.sh` (Task 4,
`fix/ci-build`) emits only `APK_PATH` and `COMMIT_SUBJECT`, and changing it is outside this task's
one-file scope. Cleanest remedy is for `build.sh` to also emit `COMMIT_SHA=$(git rev-parse HEAD)`;
this workflow can then switch to `${{ steps.build.outputs.COMMIT_SHA }}`. Until then the practical
workaround is to dispatch from the branch being built.

**2. Concurrency group uses the dispatch ref, not `inputs.ref`.**
`group: ${{ github.workflow }}-${{ github.ref }}` means two dispatches started from the same UI branch
but naming different `inputs.ref` values will cancel one another, with
`cancel-in-progress: true`. Grouping on `inputs.ref` would be stricter, but the task specifies this
exact form and it matches `pr.yml`'s convention of keying on the run's own ref, so it is left as
specified. Worth knowing if the maintainer ever dispatches two refs in parallel from one branch.

Neither concern affects the security property, which holds: read-only permissions, no signing secret,
and no expression in any `run:` body.
