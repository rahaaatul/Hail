# GitHub Reusable Actions

This directory contains reusable GitHub Actions that encapsulate common workflows for Android projects.

## Available Actions

### Release helpers

- `prepare-release/prepare-release.sh` sets the semantic version and derives
  Android's monotonically increasing `versionCode` from the GitHub Actions
  workflow run number, then runs tests, Detekt, and both APK builds. It is
  invoked by the semantic-release prepare hook.
- `find-release-apks` exposes the release and debug APK paths as action outputs.
- `upload-release-attestations` uploads both APK provenance bundles to the
  semantic-release GitHub release.
- `download-release-apks` retrieves both APKs for an existing release so
  attestations can be regenerated and uploaded by the manual recovery workflow.
- `lint-workflows` runs actionlint against the repository workflows in CI.

To recover missing attestations, run the **Recover Release Attestations**
workflow and provide the existing release tag. It downloads the APKs from that
release, generates provenance bundles, and uploads them back to the same tag.

### 1. Read Metadata (`read-metadata`)
Extracts version and application name from project files.

**Inputs:** None

**Outputs:**
- `version`: Version string from `build.gradle.kts`
- `appName`: Application name from `AndroidManifest.xml`

**Usage:**
```yaml
- name: Read metadata
  uses: .github/actions/read-metadata
  id: metadata
- name: Use version
  run: echo "Version is ${{ steps.metadata.outputs.version }}"
- name: Use app name
  run: echo "App name is ${{ steps.metadata.outputs.appName }}"
```

### 2. Build APK (`build-apk`)
Builds the Android APK for a specified build type using Gradle.

**Inputs:**
- `buildType` (required): Type of build (`release` or `debug`), defaults to `debug`

**Outputs:**
- `apkPath`: Path to the built APK file

**Usage:**
```yaml
- name: Build APK
  uses: .github/actions/build-apk
  id: build
  with:
    buildType: 'release'
- name: Use APK path
  run: echo "APK built at ${{ steps.build.outputs.apkPath }}"
```

### 3. Rename APK (`rename-apk`)
Renames the built APK to include application name and version.

**Inputs:**
- `apkPath` (required): Path to the APK to rename
- `version` (required): Version string to include in filename
- `appName` (required): Application name to include in filename

**Outputs:**
- `renamedApkPath`: Path to the renamed APK file

**Usage:**
```yaml
- name: Rename APK
  uses: .github/actions/rename-apk
  id: rename
  with:
    apkPath: ${{ steps.build.outputs.apkPath }}
    version: '1.0.0'
    appName: 'MyApplication'
- name: Use renamed APK
  run: echo "Renamed APK at ${{ steps.rename.outputs.renamedApkPath }}"
```

### 4. Setup Java and Gradle (`setup-java-gradle`)
Sets up Java environment, checks out code, and configures Gradle with performance optimizations.

**Inputs:**
- `java-version` (optional): Java version to setup, defaults to `21`
- `distribution` (optional): Java distribution (`temurin`, `zulu`, etc.), defaults to `temurin`
- `gradle-cache` (optional): Enable Gradle caching, defaults to `true`

**Usage:**
```yaml
- name: Setup Java and Gradle
  uses: .github/actions/setup-java-gradle
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
      - uses: .github/actions/setup-java-gradle
      
      # Read project metadata
      - uses: .github/actions/read-metadata
        id: metadata
      
      # Build the APK
      - uses: .github/actions/build-apk
        id: build
        with:
          buildType: 'release'
      
      # Rename the APK with version info
      - uses: .github/actions/rename-apk
        id: rename
        with:
          apkPath: ${{ steps.build.outputs.apkPath }}
          version: ${{ steps.metadata.outputs.version }}
          appName: ${{ steps.metadata.outputs.appName }}
      
      # Use the renamed APK (e.g., upload to release)
      - uses: actions/upload-release-asset@v1
        with:
          asset_path: ${{ steps.rename.outputs.renamedApkPath }}
          asset_name: ${{ steps.rename.outputs.renamedApkPath }}
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