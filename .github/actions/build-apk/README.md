# Build APK Action

This action builds both release and debug APKs for an Android project with unit testing and code quality checks.

## Features

- Runs unit tests (`./gradlew test`)
- Runs Detekt code quality checks (`./gradlew detekt`)
- Builds both release and debug APKs in parallel using Gradle's `--parallel` flag
- Automatically named APKs (configured in `build.gradle.kts`)
  - Format: `{appName}-v{versionName}-{buildType}.apk`
  - Example: `MyApp-v1.2.3-release.apk`

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
      - uses: .github/actions/read-metadata
        id: metadata
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

1. **Quality Checks**: Runs unit tests and Detekt first - if these fail, the APKs won't be built
2. **Parallel Build**: Uses `./gradlew assembleRelease assembleDebug --no-daemon --parallel` to build both APKs simultaneously
3. **Automatic Naming**: The APK naming is handled by `applicationVariants.all` in `build.gradle.kts`:
   ```kotlin
   applicationVariants.all { variant ->
       val appName = rootProject.findProperty("appName") as String?
       val versionName = variant.versionName
       if (appName != null && versionName != null) {
           variant.outputs.forEach { output ->
               if (output instanceof com.android.build.VariantOutput) {
                   val buildType = variant.buildType.name
                   output.outputFileName = "${appName}-v${versionName}-${buildType}.apk"
               }
           }
       }
   }
   ```

## Required metadata

This action expects the following to be available:
- Java 21 JDK setup (via setup-java-gradle action)
- Version and appName metadata (via read-metadata action or similar)

## Example workflow

See `.github/workflows/release.yml` for a complete example of how to use this action in a release workflow.