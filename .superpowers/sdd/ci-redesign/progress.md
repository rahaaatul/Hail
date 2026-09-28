# SDD ledger — plan: plans/2026-09-27-ci-redesign.md
Task 1: complete (commits 6895a58..cd368df, review clean) — build.gradle.kts version overrides + signing configs; pr id fixed to com.aistra.hail.pr79 (aapt2 rejects a digit-leading segment)
Task 2: complete (db395b1, review clean) — lib.sh, with repo_root_path added after single_match was found broken
Task 5: complete (0587ef1, review clean) — upload.py + telegram.json
Task 8: complete (a353c81, review clean) — version.sh, signing.sh, release.yml
Ruling: pr applicationId is com.aistra.hail.pr79, not .pr.79 — aapt2 rejects a package segment starting with a digit, so the dotted form cannot link at all — cost if wrong: Task 6 and 7 must be re-edited, nothing else
Ruling: Android platform package is platforms;android-37.0, not platforms;android-37 — the bare name does not exist in the SDK repository — cost if wrong: the workflow fails at setup-android
Ruling: empty KEYSTORE must fail closed (exit 1), not exit 0 — require_env rejects the empty string, and fail-closed is correct for signing material — cost if wrong: nothing; the plan text was wrong, the implementation is right
Task 6: complete (cb2ba01, on branch fix/ci-pr, base cd368df) — pr.yml; permissions exactly {contents: read}, no KEYSTORE*, no expressions in any run body, Telegram env matches upload.py exactly, artifact uploaded before Telegram
Ruling: the Rename step's run body must use $PR_NUMBER from env, not an inline ${{ github.event.pull_request.number }} — the plan's own Task 6 snippet fails its own Step 4 check, and Task 8's merged release.yml already shows the correct form for the identical step — cost if wrong: re-edit one step, nothing else
Note for Task 7: the plan's debug.yml snippet still says platforms;android-37, which does not exist; use platforms;android-37.0
Note for verification: no PyYAML or js-yaml in the container and no pip; js-yaml was installed under /tmp/agent_f2b1e9c3-e021-43a7-8b77-d6db73ac993f/yamllib and reached via NODE_PATH
Task 7: complete (c9eee86, on branch fix/ci-debug, base cd368df) — debug.yml; permissions exactly {contents: read}, no KEYSTORE*, no expressions in any run body, Telegram env matches upload.py in both directions, artifact before Telegram
Concern (Task 7, not fixed, needs a Task 4 follow-up): ${{ github.sha }} on workflow_dispatch is the SHA of the ref the dispatch was started from, not of the checked-out inputs.ref — so when they differ the debug caption links an unbuilt commit, while COMMIT_SUBJECT (from git log -1 after checkout) is correct — cost if left: a wrong commit link in the debug Telegram caption only
Ruling: debug.yml's Rename/artifact path literal "HailBug.apk" is not a repeat of the Minor raised against pr.yml — with no dynamic part there is nothing to re-derive, so the re-derived form cannot arise — cost if wrong: nothing
Note for verification: /tmp/yamlcheck is denied by policy (external_directory); reuse the existing /tmp/agent_f2b1e9c3-e021-43a7-8b77-d6db73ac993f/yamllib via NODE_PATH instead
