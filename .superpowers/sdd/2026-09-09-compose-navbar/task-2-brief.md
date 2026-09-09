# Task 2: Replace activity layouts with a Compose host layout

**Goal:** Replace the portrait and landscape `activity_main.xml` layouts with Compose host layouts that include the existing `content_main.xml` and a `ComposeView` for the adaptive navbar.

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/res/layout-land/activity_main.xml`
- Keep: `app/src/main/res/layout/content_main.xml`

**Context:** The app currently uses XML layouts with `BottomNavigationView` (portrait) and `NavigationRailView` (landscape). These will be removed in Task 5. For now, we need a root layout that includes `content_main.xml` (which hosts the `NavHostFragment`) and adds a `ComposeView` for the adaptive navbar.

**Constraints:**
- Do NOT modify `content_main.xml`
- Do NOT remove the old nav views yet (Task 5 handles cleanup)
- Use `androidx.compose.ui.platform.ComposeView` with id `compose_view`
- The ComposeView should be overlayable on top of content_main (use FrameLayout or CoordinatorLayout)
- Both portrait and landscape layouts should have the same structure

**Verification:**
- `./gradlew :app:assembleDebug` must succeed
- App should launch and existing fragments should still render
