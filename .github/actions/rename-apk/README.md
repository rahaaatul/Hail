# Rename APK Action (Deprecated)

⚠️ **DEPRECATED**: This action is no longer needed as of the latest workflow updates.

APK renaming is now handled automatically by `build.gradle.kts` using the `applicationVariants.all` configuration:

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

This means APKs are automatically named as `{appName}-v{versionName}-{buildType}.apk` during the build process.

## Keep for backward compatibility

This action is kept in the repository for backward compatibility with older workflows, but it should not be used in new workflows.

## Original purpose

This action previously renamed APK files to include the app name and version:
- Input: `app-release-unsigned.apk`
- Output: `MyApp-v1.2.3-release.apk`

## Usage (not recommended)

If you still need to use this action for some reason:

```yaml
- uses: .github/actions/rename-apk
  with:
    apkPath: path/to/apk
    version: 1.2.3
    appName: MyApp
```