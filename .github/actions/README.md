# GitHub Reusable Actions

This directory contains reusable GitHub Actions that encapsulate common workflows for Android projects.

## Available Actions

### Release helpers

- `find-release-apks` exposes the release and debug APK paths as action outputs.
- `upload-release-attestations` uploads both APK provenance bundles to the
  semantic-release GitHub release.
- `download-release-apks` retrieves both APKs for an existing release so
  attestations can be regenerated and uploaded by the manual recovery workflow.
- `lint-workflows` runs actionlint against the repository workflows in CI.

To recover missing attestations, run the **Recover Release Attestations**
workflow and provide the existing release tag. It downloads the APKs from that
release, generates provenance bundles, and uploads them back to the same tag.

### 1. Code Check (`code-check`)
Runs the code checks of the project before the APKs are built: the Detekt
static analysis, the Spotless formatting check and the Kover coverage gate. A
failing check stops the build, so an APK is only produced from code that passed
the checks.

**Inputs:**
- `gradle-args` (optional): extra arguments appended to every Gradle invocation
- `ratchet-base` (optional): the git ref that the Spotless ratchet compares against
- `run-detekt` (optional): run the Detekt static analysis, defaults to `true`
- `run-spotless` (optional): run the Spotless formatting check, defaults to `true`
- `run-kover` (optional): run the tests under the coverage agent, defaults to `true`

**Usage:**
```yaml
- name: Code check (Detekt, Spotless, Kover)
  uses: ./.github/actions/code-check
```

### 2. Build APK (`build-apk`)
Extracts the metadata and builds the Android APKs with Gradle.

**Inputs:**
- `gradle-args` (optional): extra arguments appended to every Gradle invocation
- `variants` (optional): the Gradle assemble targets, defaults to `assembleRelease assembleDebug`

**Outputs:**
- `releaseApkPath`: Path to the built release APK
- `debugApkPath`: Path to the built debug APK

**Usage:**
```yaml
- name: Build both APKs
  uses: ./.github/actions/build-apk
  id: build
  with:
    variants: 'assembleRelease assembleDebug'
```

### 3. Setup Java and Gradle (`setup-java-gradle`)
Sets up Java environment, checks out code, and configures Gradle with performance optimizations.

**Inputs:**
- `java-version` (optional): Java version to setup, defaults to `21`
- `distribution` (optional): Java distribution (`temurin`, `zulu`, etc.), defaults to `temurin`
- `gradle-cache` (optional): Enable Gradle caching, defaults to `true`

**Usage:**
```yaml
- name: Setup Java and Gradle
  uses: ./.github/actions/setup-java-gradle
  with:
    java-version: '21'
    distribution: 'temurin'
    gradle-cache: 'true'
```

## Example Workflow Usage

Here's how these actions work together in a typical release workflow:

```yaml
jobs:
  release:
    runs-on: ubuntu-latest
    steps:
      # Setup environment
      - uses: ./.github/actions/setup-java-gradle

      # Check the code before it is built
      - uses: ./.github/actions/code-check

      # Build the APKs
      - uses: ./.github/actions/build-apk
        id: build

      # Use the built APKs (e.g., upload to release)
      - uses: actions/upload-release-asset@v1
        with:
          asset_path: ${{ steps.build.outputs.releaseApkPath }}
          asset_name: ${{ steps.build.outputs.releaseApkPath }}
          asset_content_type: application/vnd.android.package-archive
```

## Action Design Principles

1. **Single Responsibility**: Each action does one thing well
2. **Composability**: Actions can be chained together to build complex workflows
3. **Minimal Inputs**: Actions require only essential inputs
4. **Clear Outputs**: Actions expose useful outputs for downstream steps
5. **Reusability**: Actions work across different projects and workflows

## Maintenance

To modify an action:
1. Edit the corresponding `action.yml` file in the action's directory
2. Update version numbers if publishing to Marketplace
3. Ensure backward compatibility when making changes
4. Test with workflows that use the action

## Best Practices

- Use specific versions when calling actions in workflows (e.g., `@v1`)
- Keep action inputs and outputs well-documented
- Handle errors gracefully in shell scripts
- Use consistent naming conventions
- Document any assumptions or requirements