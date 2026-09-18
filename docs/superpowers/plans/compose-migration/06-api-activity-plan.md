# Hail App Compose Migration Plan: ApiActivity Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate ApiActivity from XML layout + ViewBinding to Jetpack Compose using setContent, preserving all functionality: translucent activity showing API documentation or help content.

**Architecture:** 
- Replace `activity_api.xml` with a ComposeView in ApiActivity (or use setContent directly since it's an Activity)
- Create `@Composable ApiScreen` that hosts the UI
- Use `ScrollableState` and `VerticalScroller` for scrolling text content
- Use `rememberSaveable` for UI state (scroll position)
- Use `AnnotatedString` to display formatted API documentation (from strings or assets)
- Preserve the translucent theme and window flags
- No ViewModel needed for this simple screen

**Tech Stack:**
- Jetpack Compose, Material3
- Core Compose text handling for scrolling and formatted text
- Activity-ktx for setContent extension

**Spec:** This plan is based on comprehensive codebase analysis of the ApiActivity and related components.

## Global Constraints
- Jetpack Compose BOM 2026.08.00 (stable)
- Material3 1.4.0 (stable)
- Kotlin 2.4.20
- ApiActivity must remain translucent (preserve theme and window flags from XML)
- No ViewModel required for this simple screen

---
### Task 1: Update ApiActivity to Use Compose
**Files:**
- Modify: `app/src/main/java/com/aistra/hail/ApiActivity.kt`

**Steps:**
- [ ] Replace `setContentView(R.layout.activity_api)` with `setContent { HailTheme { ApiScreen() } }`
- [ ] Remove the `activity_api.xml` layout reference entirely
- [ ] Keep the translucent theme and window flags from the original XML (these are set in the activity's theme or programmatically)

### Task 2: Create ApiScreen Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ApiScreen.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun ApiScreen(modifier: Modifier = Modifier)`
- [ ] Use `rememberSaveable` for scroll position state (e.g., `var scrollPosition by rememberSaveable { mutableStateOf(0f) }`)
- [ ] Implement scrolling content using one of these approaches:
    Option A: `VerticalScroller` with scrollable state and text
    Option B: `LazyColumn` with `itemsIndexed` for lines of text
    Option C: `BasicText` with `modifier.verticalScroll(scrollState)`
- [ ] Use `remember` to create `AnnotatedString` from string resource or asset
- [ ] Apply text styles (bodyMedium, etc.) to the `AnnotatedString` ranges
- [ ] Display the formatted text with appropriate padding and scrolling behavior
- [ ] Preserve the translucent background by using `MaterialTheme` colors or explicit transparent background

### Task 3: Validate ApiActivity Migration
**Files:**
- No new files to create (validation uses existing files)

**Steps:**
- [ ] Run `./gradlew assembleDebug` to ensure successful build
- [ ] Test ApiActivity renders correctly in both light/dark theme
- [ ] Verify scrolling functionality works properly with finger gestures
- [ ] Confirm text is readable and formatted correctly (headings, paragraphs, etc.)
- [ ] Check that the translucent background and window flags are preserved (activity appears semi-transparent over background)
- [ ] Ensure no crashes or errors when opening/closing the activity
- [ ] Verify TalkBack accessibility works for scrolling and reading content
- [ ] Test configuration changes (rotation) maintain scroll position and state

## References
- [Compose Text Documentation](https://developer.android.com/jetpack/compose/text)
- [Scrolling in Compose](https://developer.android.com/jetpack/compose/graphics/scrolling)
- [rememberSaveable API](https://developer.android.com/jetpack/compose/state#remember-saveable)
- [AnnotatedString Guide](https://developer.android.com/jetpack/compose/text#annotatedstring)