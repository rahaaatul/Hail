Both issues have been fixed:
1. Removed unused `NavBackStackEntry` import - `currentBackStackEntryAsState()` infers its type, so the explicit import is unnecessary.
2. Updated `AppBarConfiguration` to include all 5 top-level destinations: `nav_home`, `nav_actions`, `nav_apps`, `nav_settings`, `nav_about`.