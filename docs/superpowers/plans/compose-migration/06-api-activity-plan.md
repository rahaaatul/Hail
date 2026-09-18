# Hail App Compose Migration Plan: ApiActivity Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Validate that ApiActivity is already using Jetpack Compose via setContent, and ensure it correctly handles all API intents (launch, freeze/unfreeze, lock screen, etc.) with appropriate Compose UI (RedirectBottomSheet, ErrorDialog). No migration from XML is needed.

**Architecture:** 
- Validate that ApiActivity already uses `setContent { AppTheme { ... } }` (no XML layout)
- Ensure the existing Compose UI (RedirectBottomSheet, ErrorDialog) correctly handles all API intents
- Preserve the translucent theme and window flags (already set via activity theme)
- Confirm no ViewModel is needed (current implementation does not use one)

**Tech Stack:**
- Jetpack Compose, Material3
- Activity-ktx for setContent extension
- Core Compose text handling for scrolling and formatted text
- Kotlin coroutines for asynchronous tasks

**Spec:** This plan is based on validating the existing Compose implementation of ApiActivity and ensuring it handles all API intents correctly.

## Global Constraints
- Jetpack Compose BOM 2026.09.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- ApiActivity must remain translucent (preserve theme and window flags from XML)
- No ViewModel required for this simple screen

---
## Tasks


### Task 1: Validate ApiActivity Uses Compose
**Files:**
- Validate: `app/src/main/java/com/aistra/hail/ui/api/ApiActivity.kt`

**Steps:**
- [ ] Validate that `onCreate` does not call `setContentView` and instead uses `setContent { AppTheme { ... } }`
- [ ] Verify that the translucent theme is preserved (check that the activity's theme is set to `@style/Theme.Hail.Translucent` or window flags are set appropriately)
- [ ] Ensure no XML layout file is referenced (remove any reference to `activity_api.xml` if present)

### Task 2: Validate Existing Compose UI
**Files:**
- Validate: `app/src/main/java/com/aistra/hail/ui/api/ApiActivity.kt`

**Steps:**
- [ ] Validate that the `RedirectBottomSheet` composable correctly handles the `Intent.ACTION_SHOW_APP_INFO` action and displays app info with action buttons.
- [ ] Validate that the `ErrorDialog` composable correctly displays error messages.
- [ ] Verify that all API intents (ACTION_VIEW, HailApi actions) are handled and appropriate UI is shown.
- [ ] Ensure the translucent background is maintained in all UI states.
- [ ] Confirm that the UI responds correctly to configuration changes (rotation, multi-window).

### Task 3: Validate ApiActivity Migration
**Files:**
- No new files to create (validation uses existing files)

**Steps:**
- [ ] Refer to Validation Checklist for validation steps.

## Validation Checklist
- [ ] Run `./gradlew assembleDebug` to ensure successful build
- [ ] Test ApiActivity renders correctly in both light/dark theme
- [ ] Verify scrolling functionality works properly with finger gestures
- [ ] Confirm text is readable and formatted correctly (headings, paragraphs, etc.)
- [ ] Check that the translucent background and window flags are preserved (activity appears semi-transparent over background)
- [ ] Ensure no crashes or errors when opening/closing the activity
- [ ] Verify TalkBack accessibility works for scrolling and reading content
- [ ] Test configuration changes (rotation) maintain scroll position and state

## Mitigation Strategies

- **Pager migration complexity (very high):** Break into subtasks (ComposeView, PagerScreen composable, header, grid, multi-select toolbar, tag edit dialog), use rememberSaveable for UI state, validate each tab type independently, and add swipe-to-refresh for app lists.
- **State persistence verification:** Use rememberSaveable for UI state (selected tab, scroll state, multi-select state) and ensure ViewModels are Hilt-injected and survive configuration changes; test rotation and multi-window scenarios.
- **Performance benchmarks:** Use LazyVerticalGrid/LazyColumn for efficient rendering, baseline profiles, test on low-end devices (API 24 emulator), and monitor frame timing with Macrobenchmark.
- **Accessibility compliance:** Test with TalkBack, add contentDescription to icons and interactive elements, use semantic properties for state (selected, checked), and verify focus order.
- **State management and ViewModel conversion:** Ensure all ViewModels expose StateFlow for Compose integration, use collectAsStateWithLifecycle for lifecycle-aware collection, and avoid exposing MutableStateFlow directly to UI.
- **Migration rollback risk:** Use feature branch `migrate/compose`, sequential PRs (one per screen), CI validation (build, unit tests, lint, connectedAndroidTest on emulator), manual QA on physical device, and internal tester releases before production.

## References
- [Compose Text Documentation](https://developer.android.com/jetpack/compose/text)
- [Scrolling in Compose](https://developer.android.com/jetpack/compose/graphics/scrolling)
- [rememberSaveable API](https://developer.android.com/jetpack/compose/state#remember-saveable)
- [AnnotatedString Guide](https://developer.android.com/jetpack/compose/text#annotatedstring)