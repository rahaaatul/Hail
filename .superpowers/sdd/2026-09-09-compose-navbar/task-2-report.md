# Task 2 Report: Replace activity layouts with Compose host layout

**Status:** ✅ PASSED

## Commits Made
- `7944966` task-2: replace activity_main layouts with CoordinatorLayout + ComposeView host

## Verification
- `./gradlew :app:assembleDebug` — **BUILD SUCCESSFUL** (19s, 43 actionable tasks)

## Changes
- `app/src/main/res/layout/activity_main.xml` — replaced root `ConstraintLayout` with `CoordinatorLayout`; includes `content_main`, retains `app_bar_main` + `BottomNavigationView` (to be cleaned up in Task 5), adds `ComposeView` with id `compose_view` at `layout_gravity="bottom"`
- `app/src/main/res/layout-land/activity_main.xml` — same structure; retains `app_bar_main` + `NavigationRailView`, adds `ComposeView` with id `compose_view`
- `app/src/main/res/layout/content_main.xml` — **not modified**

## Concerns
- The old `BottomNavigationView` (portrait) and `NavigationRailView` (landscape) are still present in the layout alongside the new `ComposeView`. Both occupy the bottom of the screen and will overlap visually until removed in Task 5. This is intentional per the brief but means the UI will show duplicate nav bars during the transition.
- `MainActivity.kt` still references `bottomNav`, `navRail`, and `appBarMain` IDs — no Kotlin changes were required because those IDs were preserved in the new layouts.
- The `ComposeView` currently has `layout_gravity="bottom"` and `match_parent` dimensions — once the old nav views are removed in Task 5, the ComposeView will need its layout params adjusted to the appropriate navbar size (e.g., `wrap_content` height for bottom nav, fixed width for rail).
