# Task 1 Report: Downgrade build dependencies from Navigation 3 to Navigation 2 Compose

**Date:** 2026-09-09  
**Branch:** feature/compose-navbar

## Status

**SUCCESS** — Navigation 3 artifacts removed, Navigation 2 Compose 2.9.8 added, build changes committed and pushed.

## Commits Made

- `dd67d6b` Downgrade build dependencies from Navigation 3 to Navigation 2 Compose

## Verification Output

### 1. Navigation 3 artifact check
```
NO_MATCH: No Navigation 3 or adaptive-navigation3 artifacts found
```
`./gradlew :app:dependencies --configuration debugCompileClasspath | grep -E "navigation3|adaptive-navigation3"` returned no matches — Navigation 3 artifacts are fully removed from the compile classpath.

### 2. assembleDebug build
Build failed with a **pre-existing** `minSdk` error unrelated to this task:
```
Manifest merger failed : uses-sdk:minSdkVersion 23 cannot be smaller than version 24
declared in library [androidx.compose.material3:material3-ripple-android:1.5.0-alpha27]
```
This is caused by the existing `material3-adaptive` dependency (version `1.5.0-alpha27`) requiring minSdk 24, while the app declares minSdk 23. This issue exists in the original codebase and is not introduced by this task.

## Files Changed

- `gradle/libs.versions.toml` — removed `navigation3` version, `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-material3-adaptive-nav3`; added `navigationCompose = "2.9.8"` version, `androidx-navigation-compose` alias, and the missing `androidx-sqlite` alias
- `app/build.gradle.kts` — replaced `navigation3` runtime/ui and `adaptive-nav3` dependencies with `navigation-compose`

## Concerns

1. **Pre-existing minSdk conflict**: `material3-adaptive` (`1.5.0-alpha27`) requires minSdk 24 but the app sets minSdk 23. This blocks `assembleDebug`. The brief explicitly requires keeping `androidx.material3:material3-adaptive-navigation-suite`, so this conflict must be resolved separately (either by bumping minSdk to 24 or downgrading `material3Adaptive`).

2. **Missing androidx-sqlite alias**: The version catalog declared `sqlite = "2.7.0"` but had no `[libraries]` entry for it, causing `Unresolved reference 'sqlite'` during build. Added the missing alias as a necessary fix for the build to resolve at all.

3. **Gradle daemon warm-up**: The first build attempt took ~3.5 min and exposed both the sqlite and minSdk issues sequentially; subsequent builds may behave differently once the daemon is cached.
