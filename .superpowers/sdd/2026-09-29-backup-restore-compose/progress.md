# Backup/restore refactor — progress ledger

Reconstructed 2026-10-01. The original ledger was lost with the local worktree; this file is the
authoritative summary going forward. Keep it updated as rounds land.

## Merge status

| Stage | PR | Status |
|---|---|---|
| Backup/restore correctness | #109 | merged `3a8d319` |
| Staged-flow defects | #120 | merged `6774026` |
| Compose backup/restore screen | #121 | draft, open, **blocked on CI** |

## PR #121 state

Branch `feat/backup-restore-screen`. Worktree `worktree-prB` (detached HEAD; push with
`git push origin HEAD:refs/heads/feat/backup-restore-screen`).

Head `94690ff` = our work through `5192979`, with `origin/main` merged in.

Done and CI-verified before the break:
- archive-preview functions + tests, TDD red `36902536192` → green
- strings, `nav_backup`, ViewModel, screen, host fragment
- three compile-fix rounds (`36883234116` → `36906244890` green)
- step 6: old Settings flow retired, surviving row navigates to `nav_backup`
- step 7: entry names, copy fix, de-duplicated disabled reason, row locks, `entriesOf` tidy
- fix 4: entry rows hoisted out of the `Idle` arm, locked during `Working` instead of hidden
- fix 5a (`5192979`): staged file no longer deleted under the state referencing it; backup staging
  file cleaned in a `finally`; `CancellationException` re-thrown at both `runCatching` sites;
  success message names the picked destination, not the staging file

## BLOCKER — repo-wide CI break, not ours

`main` head `3bffd2c` (Dependabot, 18:47) bumped `.github/.java-version` 26→27 and
`jvmToolchain(26)`→`jvmToolchain(27)` but left `kotlin = "2.4.20"` unchanged. Kotlin 2.4.20 does
not recognise JVM target 27, so every build now fails at `:app:compileDebugUnitTestKotlin` with
`IllegalArgumentException: Unknown Kotlin JVM target: 27`.

- Fails on `5192979` (`36911637487`, reproduced on rerun) and on `94690ff` (`36912469749`).
- Runner log: `JAVA_HOME: /opt/hostedtoolcache/Java_Temurin-Hotspot_jdk/27.0.0-35/x64`.
- Merging `main` in did **not** fix it: our build config is now byte-identical to main's.
- No full build has run on `main` since the bump. The 18:49 green runs on main are Dependabot
  version-update jobs, not builds. Last real build on main: `36887097944`, 15:48 on `6774026`,
  pre-bump, success.
- Decision needed on `main`: bump `kotlin` to a release supporting target 27, or revert the JDK
  toolchain bump to 26. Not being changed unilaterally from inside PR #121 — it is out of scope for
  that PR and would collide with the Dependabot bump.

## Review triage (three-way review, head `958e631`)

Accepted and fixed in 5a: staged-file/state desync, cancellation leak, `CancellationException`
swallowing, wrong filename in the success message.

Declined, with reasons:
- "from Downloads" source suffix on the archive row — the spec's copy table has no resource for it,
  and resolving it needs an extra SAF query the spec elsewhere argues against. Prior ruling stands.
- "leak between copy and state update" — incorrect. `stagedFile` is assigned before any throwable
  point, so `onCleared` covers it.

Found by me, not by any reviewer: `msg_exported` reported the staging filename (fixed in 5a).

Still outstanding from that review — fix round 5b, blocked behind the CI break:
- 5. `HorizontalDivider` between the Backup and Restore sections (spec line 77)
- 6. `label_in_this_archive` heading above the restore entry rows — string exists at
  `strings.xml:125`, spec line 258, referenced by nothing
- 7. three `entriesOf` paths no test pins: the `-1` size clamp, duplicate-entry handling, and the
  corrupt-zip throw. Dropping the clamp, flipping to first-wins, and swallowing the exception all
  pass the full suite today. Must go tests-first with a red run.
- 8. `BackupPreview` KDoc claims code does not support: dedup is by category not name, "last one
  encountered" is not deterministic because `ZipFile.entries()` order is unspecified, and
  `IOException` is thrown alongside `ZipException`

## After 5b

Re-review the changed areas, then un-draft and merge #121. Then Task 3 (PR C) and the whole-branch
review. Manual closure of #113-#115 still credential-blocked (403 FORBIDDEN).