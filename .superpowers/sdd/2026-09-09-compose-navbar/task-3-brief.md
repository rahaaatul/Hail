# Task 3: Implement Compose navigation scaffold in MainActivity

**Goal:** Add Compose content to MainActivity that renders an adaptive NavigationSuiteScaffold-driven navbar, using the existing NavController for navigation, while preserving current FAB and destination behavior.

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt`
- Read: `app/src/main/res/menu/nav_main.xml`
- Read: `app/src/main/res/navigation/mobile_navigation.xml`

**Context:** The activity layouts already contain a `ComposeView` with id `compose_view`. The existing `NavController`, `AppBarConfiguration`, `OnDestinationChangedListener`, and FAB logic must remain intact. The XML `BottomNavigationView` and `NavigationRailView` are still present in the layouts and will be removed in Task 5; for now the Compose navbar will coexist with them.

**Constraints:**
- Keep existing `NavController`, `AppBarConfiguration`, `setupActionBarWithNavController`, and `OnDestinationChangedListener`
- Keep existing FAB behavior exactly as-is
- The Compose navbar must show the same 3 items as `nav_main.xml`: Home (`R.id.nav_home`), Actions (`R.id.nav_actions`), Settings (`R.id.nav_settings`)
- Use `NavigationSuiteScaffold` from `androidx.compose.material3.adaptive.navigation.suite`
- Use `calculateNavigationSuiteType` to switch between `NavigationBar` and `NavigationRail`
- Drive selection from `navController.currentDestination?.id`
- On item click, call `navController.navigate(itemId)` and use `popUpTo(startDestinationId) { saveState = true }` with `launchSingleTop = true`
- Do NOT add `androidx.navigation.compose` yet; this task only adds Compose UI for the navbar

**Verification:**
- `./gradlew :app:assembleDebug` must succeed
- Launch app: existing fragments render, action bar title updates, FAB behaves correctly
- Portrait: bottom nav bar visible with 3 items
- Landscape: navigation rail visible with 3 items
- Selecting an item navigates to the corresponding fragment
