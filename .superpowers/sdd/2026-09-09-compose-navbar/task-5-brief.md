# Task 5: Clean up unused XML navigation views

**Goal:** Remove the old XML `BottomNavigationView` and `NavigationRailView` from the activity layouts, leaving only the Compose host and the existing `content_main` include.

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout-land/activity_main.xml`

**Context:** The Compose `NavigationSuiteScaffold` is now rendering the adaptive navbar. The XML `BottomNavigationView` (portrait) and `NavigationRailView` (landscape) are redundant and must be removed.

**Constraints:**
- Remove ONLY the old nav views (`BottomNavigationView` and `NavigationRailView`)
- Keep the `ComposeView` with id `compose_view`
- Keep the `app_bar_main` include (it hosts the Toolbar and content_main)
- Do NOT modify `content_main.xml`
- Do NOT remove any inset/padding logic that is still used by other views

**Verification:**
- `./gradlew :app:assembleDebug` must succeed
- App should launch and show only the Compose navbar
- Old XML nav views should no longer be present in the layouts
