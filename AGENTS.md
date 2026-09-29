# AGENTS.md

## Verification runs in pull request CI, never locally

Do not run tests, builds, or linters in this environment. `./gradlew`, the JVM
suites, and anything needing the Android SDK or a JDK do not work here — there is
no toolchain installed, and installing one is not worth the time. The repository
now has `pr.yml`, which runs the script tests and the JVM suites on every pull
request, and `test_scripts.sh` gates the shell scripts it exercises.

That pipeline exists so this work does not have to be done twice. Push the
branch and read the run.

A consequence worth stating plainly: **do not report "could not run the tests
locally" as a caveat, a gap, or a reason to hold a change back.** It is not a
limitation of the change, it is the intended workflow. Say what the change does
and let CI be the evidence. If a check is needed, add it to `pr.yml`.

## Commits

Context-First: the body explains what was broken, why it mattered, and what the
fix changes, before the reader has to open the diff. Write it for someone
reviewing on GitHub who has not read the issue.

Close issues in the footer with `Closes #<issue-number>`. Do not write
`Action #IssueNumber`; it is ambiguous and reads as a comment, not a close.

Do not fabricate issue references. If nothing tracks a problem, say so in the
body rather than inventing a number.

## Reviews

Respond to every review finding with accept or decline, and say why in the
comment. Resolve the thread after replying. A declined finding stays open rather
than being quietly closed, so the maintainer can overrule it.
