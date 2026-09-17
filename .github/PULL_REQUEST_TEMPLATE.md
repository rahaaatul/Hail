<!--
BEFORE SUBMITTING: Read every word of this template. PRs that leave
sections blank, contain multiple unrelated changes, or show no evidence
of human involvement will be closed without review.
-->

> **This PR MUST target the `main` branch.** `main` is the
> active development and release branch. PRs opened against other
> branches will be asked to retarget before review.

## Who is submitting this PR? (required)
<!-- Required. PRs that omit this will be closed. We assume an agent wrote
     this PR — tell us which one and where it ran. We weigh contributions by
     what produced them: content reasoned from documentation is held to a
     different bar than work grounded in a real session. -->

| Field | Value |
|-------|-------|
| Your model + version | |
| Harness + version | |
| All plugins installed | |
| Human partner who reviewed this diff | |

## What problem are you trying to solve?
<!-- Describe the specific problem encountered. If this was a session
     issue, include: what you were doing, what went wrong, the model's
     exact failure mode, and ideally a transcript or session log.

     "Improving" something is not a problem statement. What broke? What
     failed? What was the user experience that motivated this? -->

## What does this PR change?
<!-- 1-3 sentences. What, not why — the "why" belongs above. -->

## Is this change appropriate for Hail?
<!-- Hail is a Kotlin-based Android app for freezing Android apps.
     Ask yourself:

     - Would this be useful to someone working on a completely different
       kind of project than an Android app?
     - Is this project-specific, team-specific, or tool-specific?
     - Does this integrate or promote a third-party service?

     If your change is a new skill for a specific domain, workflow tool,
     or third-party integration, it belongs in its own project — not here. -->

## What alternatives did you consider?
<!-- What other approaches did you try or evaluate before landing on this
     one? Why were they worse? If you didn't consider alternatives, say so
     — but know that's a red flag. -->

## Does this PR contain multiple unrelated changes?
<!-- If yes: stop. Split it into separate PRs. Bundled PRs will be closed.
     If you believe the changes are related, explain the dependency. -->

## Existing PRs
- [ ] I have reviewed all open AND closed PRs for duplicates or prior art
- Related PRs: <!-- #number, #number, or "none found" -->

<!-- If a related closed PR exists, explain what's different about your
     approach and why it should succeed where the other didn't. -->

## Environment tested

| Device/Emulator | API Level | Android SDK | Model | Model version/ID |
|-----------------|-----------|-------------|-------|-------------------|
|                   |           |             |       |                   |

## New Android development environment (required if this PR adds a new build tool, SDK, or dependency)

<!-- If this PR adds support for a new Android development tool, SDK version,
     Gradle plugin, or build dependency, you MUST include a session transcript
     proving the integration actually works in a clean Android development
     environment.

     A real integration builds and runs on a physical device or emulator
     with the specified SDK. The build must pass CI (Android CI workflow)
     without skipping checks.

     ACCEPTANCE TEST: Open a clean development session and build the app
     with the new configuration. A working integration produces a
     successful Gradle build and a running app on the test device.

     These are NOT real integrations and PRs that ship them will be closed:

     - Manually adding dependency versions without testing the build
     - Changes that only work on one specific machine's local setup
     - Anything that requires manual signing config or keystore access
     - Anything where the app does not compile or run on an emulator
-->

<details>
<summary>Build and run transcript</summary>

```
paste the complete transcript here
```
</details>

## Evaluation
- What was the initial prompt you (or your human partner) used to start
  the session that led to this change?
- How many test runs (unit tests, instrumentation tests, CI builds) did
  you run AFTER making the change?
- How did outcomes change compared to before the change?

<!-- "It works" is not evaluation. Describe the before/after difference
     you observed across multiple test runs. -->

## Rigor

- [ ] If this PR adds or changes Kotlin code: I ran the full test suite
      (unit tests + instrumentation tests) and all passed
- [ ] If this PR changes UI/Compose code: I tested on multiple screen
      sizes and orientations
- [ ] This change was tested adversarially, not just on the happy path
- [ ] I did not modify carefully-tuned content (build configs, signing,
      version codes) without verifying the build succeeds

<!-- If you changed build configuration or behavior-shaping code, show
     your test methodology and results. These are not prose — they are
     code. -->

## Human review
- [ ] A human has reviewed the COMPLETE proposed diff before submission

<!--
STOP. If the checkbox above is not checked, do not submit this PR.

PRs will be closed without review if they:
- Show no evidence of human involvement
- Contain multiple unrelated changes
- Promote or integrate third-party services or tools
- Submit project-specific or personal configuration as core changes
- Leave required sections blank or use placeholder text
- Modify build configs or signing without build verification
-->
