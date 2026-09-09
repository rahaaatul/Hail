# Task 3 Report: Compose NavigationSuiteScaffold in MainActivity

**Date:** 2026-09-09
**Branch:** feature/compose-navbar

## Status: ✅ COMPLETED

## Summary

Implemented Compose-based adaptive navigation in `MainActivity` using `NavigationSuiteScaffold` from `androidx.compose.material3.adaptive.navigationsuite`. The navbar coexists with existing XML `BottomNavigationView` and `NavigationRailView` (to be removed in Task 5).

## Commit

```
a10b203 feat: add Compose NavigationSuiteScaffold to MainActivity
```

- **Files modified:**
  - `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt` (+54/-2 lines)
  - `app/src/main/res/layout/activity_main.xml` (compose_view height → wrap_content)
  - `app/src/main/res/layout-land/activity_main.xml` (compose_view width → wrap_content, gravity → start)

## Implementation Details

### MainActivity.kt changes
1. **ComposeView setup in `initView()`:**
   - `composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)`
   - `composeView.setContent { AppTheme { ... } }`

2. **Adaptive navbar:**
   - `NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo(currentWindowAdaptiveInfoV2())` for adaptive type switching
   - `layoutType` parameter drives NavigationBar (portrait) vs NavigationRail (landscape)

3. **Nav items** (3 items matching `nav_main.xml`):
   - Home → `R.id.nav_home`, `R.drawable.ic_round_frozen`, `R.string.title_home`
   - Actions → `R.id.nav_actions`, `R.drawable.ic_round_action_flow`, `R.string.title_actions`
   - Settings → `R.id.nav_settings`, `R.drawable.ic_settings_selector`, `R.string.title_settings`

4. **Selection state** driven by `navController.currentDestination?.id` via `remember(currentDestId)` key

5. **Navigation on click:** `navController.navigate(itemId) { popUpTo(startDestinationId) { saveState = true }; launchSingleTop = true }`

6. **Preserved behavior:**
   - `NavController`, `AppBarConfiguration`, `setupActionBarWithNavController` — unchanged
   - `OnDestinationChangedListener` — FAB logic (show/hide, icon, click listeners) unchanged
   - `bottomNav?.isVisible` / `navRail?.isVisible` toggling unchanged

## Verification

```
./gradlew :app:assembleDebug --no-daemon

BUILD SUCCESSFUL in 43s
43 actionable tasks, 9 executed, 34 up-to-date
```

No compilation errors. Pre-existing warnings in unrelated files (PagerFragment, HShell) are unchanged.

## Concerns

1. **`calculateNavigationSuiteType` vs `calculateFromAdaptiveInfo`:** The brief mentions `calculateNavigationSuiteType` but the actual API in `material3-adaptive-navigation-suite:1.5.0-alpha27` is `NavigationSuiteScaffoldDefaults.calculateFromAdaptiveInfo()`. The behavior is equivalent (adaptive type switching). If a wrapper named `calculateNavigationSuiteType` is intended for future abstraction, it can be added in Task 5+.

2. **Layout width/height adjustments:** Changed `compose_view` from `match_parent` to `wrap_content` in both portrait and landscape layouts to prevent the Compose navbar from obscuring the existing XML navigation views. These layout changes are necessary for coexistence until Task 5 removes the XML views.

3. **`NavigationSuiteScope.item()` composable:** Used the `NavigationSuiteScope.item()` function (available in `material3-adaptive-navigation-suite:1.5.0-alpha27`) rather than standalone `NavigationSuiteItem`. The `navigationSuiteItems` lambda receiver is `NavigationSuiteScope`, not `@Composable`, so `remember` and state declarations were hoisted to the enclosing `@Composable` block.

4. **No runtime testing performed:** Build verification only. Device/emulator testing (portrait nav bar visibility, landscape rail visibility, fragment navigation) should be confirmed in integration testing.
