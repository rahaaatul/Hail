# Build APK Action

This action extracts metadata, then builds both release and debug APKs for an Android project.

The code checks - Detekt, the Spotless formatting check and the Kover coverage
gate - are not part of this action. They run in the
[code-check](../code-check/README.md) action, which the workflows call before
this one, so that a failing check stops the build before an APK is produced.

## Features

- Extracts version and app name from build.gradle.kts and AndroidManifest.xml
- Builds both release and debug APKs in parallel using Gradle's `--parallel` flag
- Automatically named APKs (configured in `app/build.gradle.kts`)
  - Format: `{app_name}-v{version_name}-{build_variant}.apk`
  - Example: `hail-v1.11.4-release.apk`

## Usage

```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: .github/actions/setup-java-gradle
        with:
          java-version: '21'
          distribution: 'temurin'
          gradle-cache: 'true'
      - uses: .github/actions/build-apk
        id: build
      - name: Upload release APK
        uses: actions/upload-release-asset@v1
        with:
          upload_url: ${{ steps.release.outputs.upload_url }}
          asset_path: ${{ steps.build.outputs.releaseApkPath }}
          asset_name: ${{ steps.build.outputs.releaseApkPath }}
          asset_content_type: application/vnd.android.package-archive
      - name: Upload debug APK
        uses: actions/upload-release-asset@v1
        with:
          upload_url: ${{ steps.release.outputs.upload_url }}
          asset_path: ${{ steps.build.outputs.debugApkPath }}
          asset_name: ${{ steps.build.outputs.debugApkPath }}
          asset_content_type: application/vnd.android.package-archive
```

## Outputs

- `releaseApkPath`: Path to the built release APK
- `debugApkPath`: Path to the built debug APK

## How it works

1. **Metadata Extraction**: Reads version from `build.gradle.kts` and app name from `AndroidManifest.xml`
2. **Parallel Build**: Uses `./gradlew assembleRelease assembleDebug --no-daemon --parallel` to build both APKs simultaneously
3. **Automatic Naming**: The APK naming is handled by `androidComponents` in `app/build.gradle.kts`:
   ```kotlin
   androidComponents {
       onVariants { variant ->
           val buildTypeName = variant.buildType
           val appName = (rootProject.findProperty("appName") as String?)?.lowercase()
           if (buildTypeName != null && appName != null) {
               val suffix = android.buildTypes.getByName(buildTypeName).versionNameSuffix
               variant.outputs.forEach { output ->
                   val versionName = output.versionName.orNull?.removeSuffix(suffix.orEmpty())
                   if (versionName != null) {
                       output.outputFileName.set("$appName-v$versionName-$buildTypeName.apk")
                   }
               }
           }
       }
   }
   ```

## Required metadata

This action expects the following to be available in the repository:
- `build.gradle.kts` with version property
- `src/main/AndroidManifest.xml` with android:label attribute

## Example workflow

See `.github/workflows/release.yml` for a complete example of how to use this action in a release workflow.