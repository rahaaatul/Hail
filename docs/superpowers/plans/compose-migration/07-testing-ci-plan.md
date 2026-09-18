# Hail App Compose Migration Plan: Testing and CI/CD Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish testing infrastructure for Compose UI tests and update CI/CD pipeline to validate Compose migrations.

**Architecture:** 
- Add Compose testing dependencies (androidx.compose.ui:ui-test-junit4, androidx.test:core, etc.)
- Create UI test skeleton for each migrated screen using createComposeRule()
- Update existing unit tests to work with Compose (if needed)
- Configure CI to run `./gradlew connectedAndroidTest` on emulator or device
- Add screenshot testing optionally (using Shot or similar)
- Ensure linting includes Compose rules

**Tech Stack:**
- Compose UI Testing (androidx.compose.ui:ui-test-junit4)
- AndroidX Test (androidx.test:core, androidx.test.ext:junit)
- Mockito/Kotlin mockk for mocking ViewModels
- Espresso IdlingResource integration (if needed)
- Android Device Instrumentation Tests (connectedAndroidTest)

**Spec:** This plan is based on standard Android testing practices applied to Jetpack Compose migration.

## Global Constraints
- Jetpack Compose BOM 2026.09.00 (stable)
- Kotlin 2.4.20
- Compose UI Testing dependencies must be compatible with Compose version
- Instrumented tests must run on API level 21+ (practical minimum: 24 for emulators)
- GitHub Actions CI must support Android emulator execution

---
### Task 1: Update Dependencies in gradle/libs.versions.toml
**Files:**
- Modify: `gradle/libs.versions.toml`

**Steps:**
- [ ] Add `composeUiTest = "1.6.8"`, `androidXTestCore = "1.5.0"`, `androidXTestRunner = "1.5.2"`, `androidXTestRules = "1.5.0"`, `mockito = "5.12.0"` to the `[versions]` section
- [ ] Add `composeUiTest = { module = "androidx.compose.ui:ui-test-junit4", version.ref = "composeUiTest" }`, `androidXTestCore = { module = "androidx.test:core", version.ref = "androidXTestCore" }`, `androidXTestRunner = { module = "androidx.test:runner", version.ref = "androidXTestRunner" }`, `androidXTestRules = { module = "androidx.test:rules", version.ref = "androidXTestRules" }`, `mockito = { module = "org.mockito:mockito-core", version.ref = "mockito" }` to the `[libraries]` section

### Task 2: Update Dependencies in app/build.gradle.kts
**Files:**
- Modify: `app/build.gradle.kts`

**Steps:**
- [ ] Add `debugImplementation(libs.composeUiTest)` for Compose UI testing in debug builds
- [ ] Add `androidTestImplementation(libs.androidXTestCore)`, `androidTestImplementation(libs.androidXTestRunner)`, `androidTestImplementation(libs.androidXTestRules)`, `androidTestImplementation(libs.mockito)` for instrumented tests
- [ ] Add `androidTestImplementation("androidx.test.ext:junit")` for JUnit test runner

### Task 3: Update Build Configuration for Compose Tests
**Files:**
- Modify: `app/build.gradle.kts` android section

**Steps:**
- [ ] Set `buildFeatures { compose = true }`
- [ ] Configure `composeOptions { kotlinCompilerExtensionVersion = composeVersions.composeCompiler.toString() }`
- [ ] Add `testOptions { unitTests { includeAndroidResources = true } }`

### Task 4: Create Compose Test Base Class
**Files:**
- Create: `src/androidTest/java/com/aistra/hail/ui/theme/ComposeTest.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `package com.aistra.hail.ui.theme`
- [ ] Create `abstract class ComposeTest`
- [ ] Add `@get:Rule val composeTestRule = createComposeRule()`
- [ ] Use `@RunWith(AndroidJUnit4::class)` annotation

### Task 5: Create Example Compose Test for AppsScreen
**Files:**
- Create: `src/androidTest/java/com/aistra/hail/ui/theme/AppsScreenTest.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Use `@HiltAndroidTest` and `@UninstallModules(AppsViewModel::class)` for ViewModel mocking
- [ ] Use `@RunWith(AndroidJUnit4::class)` test runner
- [ ] Extend `ComposeTest` base class
- [ ] In `@Before` `setup()`, mock ViewModel state flows with test data:
    - `mockViewModel.uiState = MutableStateFlow(AppsUiState(apps = StateFlow(listOf(AppInfo(...))), query = StateFlow(""), selectedFilter = StateFlow(AllAppsFilter.INSTANCE), isMultiSelect = StateFlow(false), selectedApps = StateFlow(emptySet())))`
- [ ] Create test method `appsScreen_displaysApps()` that:
    - Sets content with `HailTheme { AppsScreen(viewModel = mockViewModel) }`
    - Asserts that "Test App 1" and "Test App 2" are displayed using `composeTestRule.onNodeWithText().assertIsDisplayed()`
- [ ] Create test method `appsScreen_searchFiltersApps()` that:
    - Sets content with `HailTheme { AppsScreen(viewModel = mockViewModel) }`
    - Performs click on search field and enters test text
    - Verifies only matching apps are shown (implementation detail: enter text, verify filtered results)

### Task 6: Update CI/CD Workflow (GitHub Actions)
**Files:**
- Modify: `.github/workflows/android-ci.yml`

**Steps:**
- [ ] Under `jobs.build.steps`, add instrumented tests step after unit tests and lint
- [ ] Use: `uses: reactivecircus/android-emulator-runner@v2`
- [ ] With parameters:
    - `api-level: 33`
    - `target: google_apis`
    - `arch: x86_64`
    - `avd-name: test-avd`
    - `script: ./gradlew connectedAndroidTest`
- [ ] Ensure this runs on `push` and `pull_request` to `main` and `migrate/compose` branches

### Task 7: Update Lint Rules for Compose
**Files:**
- Modify: `app/build.gradle.kts` dependencies

**Steps:**
- [ ] Add `implement("androidx.compose.compiler:compiler:1.6.8")`
- [ ] Add `debugImplement("androidx.compose.ui:ui-tooling:1.6.8")`
- [ ] Add `debugImplement("androidx.compose.ui:ui-tooling-preview:1.6.8")`

### Task 8: Create Screenshot Test Placeholder (Optional)
**Files:**
- Create: `src/androidTest/java/com/aistra/hail/ui/theme/ScreenshotTest.kt` (optional)

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Add comment: `/* Placeholder for screenshot testing using Shot or similar */`
- [ ] Note: Would require additional setup and dependencies

## Validation Checklist

After completing all tasks above:
- [ ] App builds with new test dependencies
- [ ] Compose UI tests compile and run on emulator/device
- [ ] Basic screenshot: AppsScreen displays at least one app
- [ ] Interaction test: Search field accepts input
- [ ] State test: Multi-select mode toggles correctly
- [ ] Existing unit tests still pass (no regression)
- [ ] CI pipeline runs connectedAndroidTest on PRs
- [ ] No test-related crashes or hangs
- [ ] Tests run in reasonable time (<5 minutes for UI tests)
- [ ] Lint passes with new Compose rules
- [ ] Mockk/Mockito properly mocks ViewModel dependencies
- [ ] ComposeTest rule properly sets up Activity context
- [ ] Tests verify both light and dark themes (optional)
- [ ] Accessibility tests: Check for content descriptions

## References
- [Compose Testing Documentation](https://developer.android.com/jetpack/compose/testing)
- [Android Testing with Hilt](https://developer.android.com/training/dependency-injection/hilt-android/testing)
- [MockK Android Guide](https://mockk.io/#ANDROID)
- [GitHub Actions Android Emulator](https://github.com/marketplace/actions/android-emulator-runner)
- [Jetpack Compose Lint Rules](https://developer.android.com/studio/write/lint#compose-lint)