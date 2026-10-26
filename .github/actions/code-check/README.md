# Code Check Action

This action runs the code checks of the project before the APKs are built. A
failing check stops the build, so an APK is only produced from code that passed
the checks.

## Features

- Runs Detekt static analysis (`./gradlew detekt`)
- Runs the Spotless formatting check (`./gradlew spotlessCheck`)
- Runs the unit tests under the coverage agent and verifies the coverage bound
  (`./gradlew test koverVerifyTested koverXmlReport koverHtmlReport`)
- Uploads the coverage report as an artifact, also when a check failed
- Resolves the git ref that the Spotless ratchet compares against, so only the
  files that a change touches are format checked

## Usage

```yaml
      - name: Setup Java and Gradle
        uses: ./.github/actions/setup-java-gradle
        with:
          java-version: '21'
          distribution: 'temurin'
          gradle-cache: 'true'
      - name: Code check (Detekt, Spotless, Kover)
        uses: ./.github/actions/code-check
      - name: Build both APKs
        uses: ./.github/actions/build-apk
        id: build
```

## Inputs

- `gradle-args` (optional): extra arguments appended to every Gradle
  invocation, for example `--no-build-cache`.
- `ratchet-base` (optional): the git ref that the Spotless ratchet compares
  against. Empty means the target of the pull request, or `origin/main` outside
  a pull request.
- `run-detekt` (optional): run the Detekt static analysis. Defaults to `true`.
- `run-spotless` (optional): run the Spotless formatting check. Defaults to
  `true`.
- `run-kover` (optional): run the tests under the coverage agent and verify the
  coverage bound. Defaults to `true`.

## Detekt

Detekt reports the findings that exist today in `config/detekt-baseline.xml`.
That baseline is what keeps the check green while the code is cleaned up over
time: a finding that is already recorded is ignored, so the build only fails
when a change introduces a finding that is not in the baseline.

To see the findings, read the reports in `app/build/reports/detekt/`. To record
the findings of new code in the baseline, run `./gradlew detektBaseline` and
commit the updated `config/detekt-baseline.xml`.

## Spotless

Spotless runs ktlint over the Kotlin of the project. The ratchet limits the
check to the files that differ from `ratchet-base`, so the formatting that the
code does not follow yet does not fail a build. A file that a change touches
has to be ktlint clean as a whole.

To format the code, run `./gradlew spotlessApply`. The rules come from the
`.editorconfig` at the root of the repository.

## Kover

Kover measures the coverage of the unit tests. The full report is uploaded as
an artifact.

The verification bound does not apply to the whole app, because most of the app
has no test yet and a bound on the whole app would say nothing. It applies to
the classes that have a test suite, and it says that the tests of those
classes still run and still cover them. The bound lives in
`app/build.gradle.kts`.

## How it works

1. **Ratchet base**: resolves the git ref and its merge base. A ref that is not
   in the checkout falls back to `origin/main`, because Spotless fails the task
   when the ref is missing.
2. **Cheap checks first**: Detekt and Spotless do not compile anything, so they
   run before the tests. A formatting mistake does not cost a test run.
3. **Tests and coverage in one invocation**: the coverage agent is attached to
   the same test tasks, and the reports read that single run of the tests
   instead of a second one.
4. **Coverage report**: uploaded whether the checks passed or failed, so a
   failing build still shows what the coverage was.
