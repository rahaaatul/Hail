# Hail App - Compose Migration Plan: Testing and CI/CD

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

## File Changes

### 1. Update Dependencies
```diff
// gradle/libs.versions.toml
[versions]
+ composeUiTest = "1.6.8"
+ androidXTestCore = "1.5.0"
+ androidXTestRunner = "1.5.2"
+ androidXTestRules = "1.5.0"
+ mockito = "5.12.0"

[libraries]
+ composeUiTest = { module = "androidx.compose.ui:ui-test-junit4", version.ref = "composeUiTest" }
+ androidXTestCore = { module = "androidx.test:core", version.ref = "androidXTestCore" }
+ androidXTestRunner = { module = "androidx.test:runner", version.ref = "androidXTestRunner" }
+ androidXTestRules = { module = "androidx.test:rules", version.ref = "androidXTestRules" }
+ mockito = { module = "org.mockito:mockito-core", version.ref = "mockito" }

// app/build.gradle.kts
dependencies {
    // ... existing
    // Compose Testing
    debugImplementation(libs.composeUiTest)
    androidTestImplementation(libs.androidXTestCore)
    androidTestImplementation(libs.androidXTestRunner)
    androidTestImplementation(libs.androidXTestRules)
    androidTestImplementation(libs.mockito)
    
    // For AndroidJUnitRunner
    androidTestImplementation("androidx.test.ext:junit")
}
```

### 2. Update Build Configuration for Compose Tests
```diff
// app/build.gradle.kts
android {
    // ... existing
    buildFeatures {
        // ... existing
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = composeVersions.composeCompiler.toString()
    }
    // Add test options for Compose
    testOptions {
        unitTests {
            includeAndroidResources = true
        }
    }
}

// Create src/androidTest/java/com/aistra/hail/ui/theme/ComposeTest.kt
```

### 3. Create Compose Test Base Class
```kotlin
// src/androidTest/java/com/aistra/hail/ui/theme/ComposeTest.kt
package com.aistra.hail.ui.theme

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
abstract class ComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()
}
```

### 4. Create Example Compose Test for AppsScreen
```kotlin
// src/androidTest/java/com/aistra/hail/ui/theme/AppsScreenTest.kt
package com.aistra.hail.ui.theme

import androidx.compose.foundation.layout.isSystemInDarkTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertNotExists
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performGesture
import androidx.compose.ui.test.tag
import com.aistra.hail.HailApp
import com.aistra.hail.ui.apps.AppsViewModel
import com.aistra.hail.ui.theme.AppsScreen
import com.aistra.hail.ui.theme.HailTheme
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import dagger.hilt.android.testing.HiltTestApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@UninstallModules(AppsViewModel::class)
@RunWith(AndroidJUnit4::class)
class AppsScreenTest : ComposeTest() {

    // Mock ViewModel
    private val mockViewModel: AppsViewModel = mockk(relaxed = true)

    @Before
    fun setup() {
        // Mock ViewModel state flows
        mockViewModel.uiState = MutableStateFlow(AppsUiState(
            apps = StateFlow(listOf(
                AppInfo("com.test.app1", "Test App 1", "Description 1", 0, false, ""),
                AppInfo("com.test.app2", "Test App 2", "Description 2", 0, true, "")
            )),
            query = StateFlow(""),
            selectedFilter = StateFlow(AllAppsFilter.INSTANCE),
            isMultiSelect = StateFlow(false),
            selectedApps = StateFlow(emptySet())
        ))
    }

    @Test
    fun appsScreen_displaysApps() {
        // Given
        composeTestRule.setContent {
            HailTheme {
                AppsScreen(viewModel = mockViewModel)
            }
        }

        // Then
        composeTestRule.onNodeWithText("Test App 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test App 2").assertIsDisplayed()
    }

    @Test
    fun appsScreen_searchFiltersApps() {
        // Given
        composeTestRule.setContent {
            HailTheme {
                AppsScreen(viewModel = mockViewModel)
            }
        }

        // When
        composeTestRule.onNodeWithText("Search apps").performClick()
        composeTestRule.onNodeWithText("Test App 1").performClick() // This would be the text field
        // In a real test, we'd enter text and verify filtering
        // For brevity, we'll skip the exact input steps

        // Then
        // Would verify only matching apps are shown
    }
}
```

### 5. Update CI/CD Workflow (GitHub Actions)
```yaml
# .github/workflows/android-ci.yml
name: Android CI

on:
  push:
    branches: [ main, migrate/compose ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3
    - name: Set up JDK
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
    - name: Build
      run: ./gradlew assembleDebug
    - name: Unit Tests
      run: ./gradlew test
    - name: Lint
      run: ./gradlew lint
    - name: Instrumented Tests
      uses: reactivecircus/android-emulator-runner@v2
      with:
        api-level: 33
        target: google_apis
        arch: x86_64
        avd-name: test-avd
        script: ./gradlew connectedAndroidTest
```

### 6. Create Screenshot Test Placeholder (Optional)
```kotlin
// src/androidTest/java/com/aistra/hail/ui/theme/ScreenshotTest.kt
/*
 * Placeholder for screenshot testing using Shot or similar
 * Would require additional setup and dependencies
 */
```

### 7. Update Lint Rules for Compose
```diff
// app/build.gradle.kts
dependencies {
    // ... existing
    // Compose lint
    implement("androidx.compose.compiler:compiler:1.6.8")
    debugImplement("androidx.compose.ui:ui-tooling:1.6.8")
    debugImplement("androidx.compose.ui:ui-tooling-preview:1.6.8")
}
```

## Validation Checklist

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