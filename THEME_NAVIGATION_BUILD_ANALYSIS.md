# Hail App - Theme, Navigation & Build Configuration Analysis

*Generated: 2026-09-18*

---

## 1. Theme System Analysis

### 1.1 Color.kt - Complete Material3 Color Scheme

**Light Theme Colors:**
| Color Role | Value | Hex |
|---|---|---|
| primary | `primaryLight` | #32628D |
| onPrimary | `onPrimaryLight` | #FFFFFF |
| primaryContainer | `primaryContainerLight` | #CFE5FF |
| onPrimaryContainer | `onPrimaryContainerLight` | #001D34 |
| secondary | `secondaryLight` | #526070 |
| onSecondary | `onSecondaryLight` | #FFFFFF |
| secondaryContainer | `secondaryContainerLight` | #D6E4F7 |
| onSecondaryContainer | `onSecondaryContainerLight` | #0F1D2A |
| tertiary | `tertiaryLight` | #695779 |
| onTertiary | `onTertiaryLight` | #FFFFFF |
| tertiaryContainer | `tertiaryContainerLight` | #F0DBFF |
| onTertiaryContainer | `onTertiaryContainerLight` | #241532 |
| error | `errorLight` | #BA1A1A |
| onError | `onErrorLight` | #FFFFFF |
| errorContainer | `errorContainerLight` | #FFDAD6 |
| onErrorContainer | `onErrorContainerLight` | #410002 |
| background | `backgroundLight` | #F7F9FF |
| onBackground | `onBackgroundLight` | #191C20 |
| surface | `surfaceLight` | #F7F9FF |
| onSurface | `onSurfaceLight` | #191C20 |
| surfaceVariant | `surfaceVariantLight` | #DEE3EB |
| onSurfaceVariant | `onSurfaceVariantLight` | #42474E |
| outline | `outlineLight` | #73777F |
| outlineVariant | `outlineVariantLight` | #C2C7CF |
| scrim | `scrimLight` | #000000 |
| inverseSurface | `inverseSurfaceLight` | #2D3135 |
| inverseOnSurface | `inverseOnSurfaceLight` | #EFF1F6 |
| inversePrimary | `inversePrimaryLight` | #9DCBFC |

**Surface Container Hierarchy (Light):**
- surfaceDim: #D8DAE0
- surfaceBright: #F7F9FF
- surfaceContainerLowest: #FFFFFF
- surfaceContainerLow: #F2F3F9
- surfaceContainer: #ECEEF4
- surfaceContainerHigh: #E6E8EE
- surfaceContainerHighest: #E0E2E8

**Dark Theme Colors:**
| Color Role | Value | Hex |
|---|---|---|
| primary | `primaryDark` | #9DCBFC |
| onPrimary | `onPrimaryDark` | #003355 |
| primaryContainer | `primaryContainerDark` | #134A74 |
| onPrimaryContainer | `onPrimaryContainerDark` | #CFE5FF |
| secondary | `secondaryDark` | #BAC8DA |
| onSecondary | `onSecondaryDark` | #243240 |
| secondaryContainer | `secondaryContainerDark` | #3A4857 |
| onSecondaryContainer | `onSecondaryContainerDark` | #D6E4F7 |
| tertiary | `tertiaryDark` | #D4BEE6 |
| onTertiary | `onTertiaryDark` | #392A49 |
| tertiaryContainer | `tertiaryContainerDark` | #514060 |
| onTertiaryContainer | `onTertiaryContainerDark` | #F0DBFF |
| error | `errorDark` | #FFB4AB |
| onError | `onErrorDark` | #690005 |
| errorContainer | `errorContainerDark` | #93000A |
| onErrorContainer | `onErrorContainerDark` | #FFDAD6 |
| background | `backgroundDark` | #101418 |
| onBackground | `onBackgroundDark` | #E0E2E8 |
| surface | `surfaceDark` | #101418 |
| onSurface | `onSurfaceDark` | #E0E2E8 |
| surfaceVariant | `surfaceVariantDark` | #42474E |
| onSurfaceVariant | `onSurfaceVariantDark` | #C2C7CF |
| outline | `outlineDark` | #8C9199 |
| outlineVariant | `outlineVariantDark` | #42474E |
| scrim | `scrimDark` | #000000 |
| inverseSurface | `inverseSurfaceDark` | #E0E2E8 |
| inverseOnSurface | `inverseOnSurfaceDark` | #2D3135 |
| inversePrimary | `inversePrimaryDark` | #32628D |

**Surface Container Hierarchy (Dark):**
- surfaceDim: #101418
- surfaceBright: #36393E
- surfaceContainerLowest: #0B0E12
- surfaceContainerLow: #191C20
- surfaceContainer: #1D2024
- surfaceContainerHigh: #272A2F
- surfaceContainerHighest: #32353A

**Assessment:** ✅ **Complete** - All Material3 color roles defined for both light/dark themes with proper tonal palettes.

---

### 1.2 Type.kt - Minimal Typography

```kotlin
val AppTypography = Typography()
```

**Current State:** Uses **default Material3 typography** with no customization.

**Missing:** Custom typography scale for:
- Display styles (Display Large/Medium/Small)
- Headline styles (Headline Large/Medium/Small)
- Title styles (Title Large/Medium/Small)
- Body styles (Body Large/Medium/Small)
- Label styles (Label Large/Medium/Small)

**Default Material3 values will be used** - may not match Hail's design language.

---

### 1.3 Theme.kt - Theme Composition

```kotlin
@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable() () -> Unit
) {
    val colorScheme = when {
        dynamicColor && HTarget.S -> { /* dynamic color */ }
        darkTheme -> darkScheme
        else -> lightScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
```

**Features:**
- ✅ Light/dark color schemes from Color.kt
- ✅ Dynamic color support (Android 12+/API 31+)
- ✅ System theme detection via `isSystemInDarkTheme()`

**Missing:**
- ❌ **Shapes** - No custom `Shapes` (corner sizes for small/medium/large/extraLarge)
- ❌ **Custom Typography** - Uses default (see Type.kt)
- ❌ **Elevation/Shadows** - No custom elevation overlay
- ❌ **Spacing** - No custom spacing scale

---

## 2. Navigation Analysis

### 2.1 mobile_navigation.xml - 5 Destinations

```xml
<navigation android:id="@+id/mobile_navigation" app:startDestination="@+id/nav_home">
    <fragment android:id="@+id/nav_home" android:name="com.aistra.hail.ui.home.HomeFragment" />
    <fragment android:id="@+id/nav_actions" android:name="com.aistra.hail.ui.actions.ActionsFragment" />
    <fragment android:id="@+id/nav_apps" android:name="com.aistra.hail.ui.apps.AppsFragment" />
    <fragment android:id="@+id/nav_settings" android:name="com.aistra.hail.ui.settings.SettingsFragment" />
    <fragment android:id="@+id/nav_about" android:name="com.aistra.hail.ui.about.AboutFragment" />
</navigation>
```

### 2.2 Navigation Destinations

| ID | Fragment | Label | Entry Point |
|---|---|---|---|
| `nav_home` | `HomeFragment` | `@string/app_name` | **Start Destination** |
| `nav_actions` | `ActionsFragment` | `@string/title_actions` | Bottom Nav / Nav Rail |
| `nav_apps` | `AppsFragment` | `@string/title_apps` | FAB from Home/Actions |
| `nav_settings` | `SettingsFragment` | `@string/title_settings` | Bottom Nav / Nav Rail |
| `nav_about` | `AboutFragment` | `@string/title_about` | Settings menu → Help |

### 2.3 Navigation Actions (Explicit in XML)

| Action ID | From | To | Trigger |
|---|---|---|---|
| `action_home_to_apps` | `nav_home` | `nav_apps` | FAB click in MainActivity |

### 2.4 Implicit Navigation (Handled in Code)

| From | To | Trigger | Location |
|---|---|---|---|
| Any (Home/Actions/Settings) | `nav_apps` | FAB click | `MainActivity.onDestinationChanged()` |
| `nav_actions` | Action Editor Dialog | FAB click | `MainActivity.onDestinationChanged()` |
| `nav_settings` | `nav_about` | Menu: Help | `SettingsFragment.onMenuItemSelected()` |
| `nav_home` | App Details | Context menu | `AppsFragment.onContextItemSelected()` |

### 2.5 Navigation UI Components (MainActivity)

- **BottomNavigationView** - Shows for Home, Actions, Settings (hidden for About)
- **NavigationRail** - Same destinations as BottomNav (hidden for About)
- **ExtendedFloatingActionButton** - Context-aware:
  - Home → Navigate to Apps
  - Actions → Show Action Editor dialog
  - Others → Hidden
- **AppBarConfiguration** - Top-level destinations: Home, Actions, Settings

---

## 3. Build Configuration Analysis

### 3.1 libs.versions.toml - Key Versions

| Dependency | Version |
|---|---|
| Android Gradle Plugin | 9.4.0 |
| Kotlin | 2.4.20 |
| **Compose BOM** | **2026.09.00** |
| Compose Compiler | 2.4.20 (tied to Kotlin) |
| Material3 | Via Compose BOM |
| Navigation (fragment) | 2.10.1 |
| Compose Preference | 2.2.0 |
| Core KTX | 1.19.0 |
| Activity Compose | Via Compose BOM |
| AppCompat | 1.8.0 |
| ConstraintLayout | 2.2.2 |
| Biometric KTX | 1.4.0-alpha02 |
| Lifecycle Livedata | 2.11.0 |
| Work Runtime | 2.11.2 |
| Room | 3.0.2 |
| KSP | 2.4.20 |

### 3.2 app/build.gradle.kts - Compose-Related Dependencies

```kotlin
dependencies {
    // Compose BOM (manages all Compose library versions)
    implementation(platform(libs.androidx.compose.bom))  // 2026.09.00

    // Core Compose
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.ui.tooling.preview)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation - FRAGMENT-BASED (XML)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // Compose Preference (Zhanghai)
    implementation(libs.compose.preference)  // 2.2.0

    // Legacy View dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.swiperefreshlayout)
}
```

### 3.3 Compose BOM 2026.09.00 - Included Libraries

Key libraries managed by BOM 2026.09.00:
- `androidx.compose:compose-bom:2026.09.00`
- `androidx.compose.material3:material3` (includes Material3 Expressive)
- `androidx.compose.ui:ui`
- `androidx.compose.ui:ui-tooling`
- `androidx.compose.ui:ui-tooling-preview`
- `androidx.compose.foundation:foundation`
- `androidx.compose.animation:animation`
- `androidx.compose.runtime:runtime`
- `androidx.activity:activity-compose`
- `androidx.lifecycle:lifecycle-runtime-compose`
- `androidx.navigation:navigation-compose` **NOT INCLUDED** (must add manually)

---

## 4. HailApp.kt - Application Class

### 4.1 Theme Management

```kotlin
fun setAppTheme(theme: String) {
    if (HTarget.S) getSystemService<UiModeManager>()!!.setApplicationNightMode(...)
    else AppCompatDelegate.setDefaultNightMode(...)
}
```

- Uses `UiModeManager` for Android 12+ (API 31+)
- Falls back to `AppCompatDelegate` for older versions
- Supports: Light, Dark, Follow System

### 4.2 Working Modes
- `MODE_DEFAULT` - Standard
- `MODE_SU` - Root via libsu
- `MODE_SHIZUKU` - Shizuku API
- `MODE_DHIZUKU` - Dhizuku API
- `MODE_ISLAND_HIDE/SUSPEND` - Island app
- `MODE_OWNER/PROFILE_OWNER` - Device/Profile Owner

---

## 5. Migration Requirements

### 5.1 Compose Navigation (Replace XML Nav Graph)

**Current:** `navigation-fragment-ktx` + `navigation-ui-ktx` + XML graph

**Required Changes:**

| Task | Details |
|---|---|
| Add dependency | `androidx.navigation:navigation-compose` (version from BOM) |
| Remove dependencies | `navigation-fragment-ktx`, `navigation-ui-ktx` |
| Create NavHost | Replace `NavHostFragment` with `NavHost(navController, startDestination)` |
| Define routes | Kotlin sealed class or route strings for each destination |
| BottomNav/NavRail | Use `NavigationBarItem` / `NavigationRailItem` with `NavigationUI` |
| FAB handling | Move FAB logic to Compose or use `NavigationUI.setupWithNavController` |
| Deep links | Migrate any deep links to Compose navigation |

**New Dependencies Needed:**
```toml
# In libs.versions.toml
navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "composeBom" }
```

### 5.2 Material3 Expressive (2026 Components)

**Available in Compose BOM 2026.09.00:**

| New Component | Description |
|---|---|
| `ExpressiveCard` / `ExpressiveElevatedCard` | Updated card styles |
| `ExpressiveButton` / `ExpressiveFilledButton` | New button variants |
| `ExpressiveChip` / `ExpressiveFilterChip` | Updated chip styles |
| `ExpressiveTextField` | New text field design |
| `ExpressiveNavigationBar` | Updated navigation bar |
| `ExpressiveNavigationRail` | Updated navigation rail |
| `ExpressiveProgressIndicator` | New progress indicators |

**Migration:**
- Update imports to use Expressive variants
- Review color roles - Expressive uses extended color palette
- Test all interactive components for visual changes

### 5.3 Compose BOM 2026.09.00 Features

**Key Features:**
- ✅ Material3 Expressive components
- ✅ Improved Text handling (Hyphenation, LineHeightStyle)
- ✅ New Animation APIs (animateContentSize, lookahead)
- ✅ Better LazyList performance
- ✅ WindowInsets animation support
- ✅ Predicative back gesture integration

**Action:** Already on 2026.09.00 - verify no breaking changes in migration.

### 5.4 Predictive Back Gesture Support

**Current:** Not implemented

**Required:**
```xml
<!-- AndroidManifest.xml -->
<application android:enableOnBackInvokedCallback="true" ...>
```

```kotlin
// In Compose NavHost
val backDispatcher = OnBackPressedDispatcher()
val backHandler = backDispatcher.addCallback(enabled = true) {
    if (navController.currentBackStackEntry?.destination?.route == "home") {
        // Handle home back press (exit app)
    } else {
        navController.popBackStack()
    }
}
```

**Integration with Compose Navigation:**
- Use `BackHandler` composable for custom back handling
- `PopUpTo` with `inclusive = true` for proper back stack management

### 5.5 Edge-to-Edge with Compose

**Current:** Partial - `WindowCompat.setDecorFitsSystemWindows(window, false)` in MainActivity

**Required for Full Compose Edge-to-Edge:**

```kotlin
@Composable
fun AppNavHost() {
    val windowInsets = WindowInsets.navigationBars.asPaddingValues()
    
    NavHost(navController, startDestination = "home", modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.systemBars)  // Status + nav bars
        .imePadding()  // Keyboard
    ) {
        composable("home") {
            HomeScreen(Modifier.padding(windowInsets))
        }
        // ... other destinations
    }
}
```

**Key APIs:**
- `WindowInsets.systemBars` - Status bar + navigation bar
- `WindowInsets.navigationBars` - Navigation bar only
- `WindowInsets.statusBars` - Status bar only
- `Modifier.windowInsetsPadding()` - Apply insets as padding
- `Modifier.imePadding()` - Keyboard insets

### 5.6 Proper Compose Theme (Full Typography, Shapes, etc.)

**Required Theme.kt Enhancement:**

```kotlin
// Type.kt - Full Typography
val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = -0.25.sp
    ),
    displayMedium = TextStyle(fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall = TextStyle(fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)
)

// Theme.kt - Add Shapes
val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// Theme.kt - Full MaterialTheme
MaterialTheme(
    colorScheme = colorScheme,
    typography = AppTypography,
    shapes = AppShapes,
    content = content
)
```

---

## 6. Summary - Migration Priority

| Priority | Task | Effort | Risk |
|---|---|---|---|
| **P0** | Add `navigation-compose` dependency | Low | Low |
| **P0** | Create Compose NavHost + routes | Medium | Medium |
| **P0** | Migrate BottomNav/NavRail to Compose | Medium | Medium |
| **P0** | Enable predictive back gesture | Low | Low |
| **P1** | Full Edge-to-Edge in Compose | Medium | Low |
| **P1** | Complete Typography in Type.kt | Low | Low |
| **P1** | Add Shapes to Theme.kt | Low | Low |
| **P2** | Migrate to Material3 Expressive components | High | Medium |
| **P2** | Remove fragment-based navigation deps | Low | Low |
| **P2** | Update SettingsFragment to pure Compose nav | Medium | Medium |

---

## 7. Files to Modify

| File | Changes Needed |
|---|---|
| `gradle/libs.versions.toml` | Add `navigation-compose` |
| `app/build.gradle.kts` | Replace fragment nav deps with compose nav |
| `app/src/main/res/navigation/mobile_navigation.xml` | **Delete** (replace with Kotlin) |
| `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt` | Full Typography definition |
| `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` | Add Shapes, update MaterialTheme |
| `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt` | Replace with Compose NavHost |
| `app/src/main/kotlin/com/aistra/hail/ui/main/MainFragment.kt` | May remove if fully Compose |
| `app/src/main/kotlin/com/aistra/hail/ui/home/HomeFragment.kt` | Convert to @Composable |
| `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt` | Convert to @Composable |
| `app/src/main/kotlin/com/aistra/hail/ui/actions/ActionsFragment.kt` | Convert to @Composable |
| `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` | Already Compose, update nav calls |
| `app/src/main/kotlin/com/aistra/hail/ui/about/AboutFragment.kt` | Already Compose, update nav calls |
| `AndroidManifest.xml` | Add `enableOnBackInvokedCallback="true"` |

---

## 8. Testing Checklist

- [ ] Build passes with new dependencies
- [ ] Navigation works: Home ↔ Apps ↔ Actions ↔ Settings ↔ About
- [ ] BottomNav/NavRail sync with navigation state
- [ ] FAB actions work correctly
- [ ] Predictive back gesture animates properly
- [ ] Edge-to-edge: content not obscured by system bars
- [ ] Keyboard (IME) doesn't cover input fields
- [ ] Dynamic color works on Android 12+
- [ ] Typography/Shapes applied consistently
- [ ] Material3 Expressive components render correctly
- [ ] Theme switching (Light/Dark/System) works
- [ ] All existing functionality preserved

---

*End of Analysis*