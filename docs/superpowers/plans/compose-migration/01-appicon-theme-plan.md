# Hail App Compose Infrastructure Plan: AppIcon + Theme Implementation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Establish Compose foundation by replacing AppIconCache with Coil, completing Material3 theme with Typography and Shapes, and creating reusable AppIcon composable.

**Architecture:** 
1. Add Coil dependency for async image loading with memory/disk caching
2. Replace custom AppIconCache with Coil-based AsyncImage solution
3. Extend Theme.kt with full Material3 Typography and Shapes
4. Create reusable @Composable AppIcon for use in all screens
5. Remove AppIconCache.kt after verification

**Tech Stack:**
- Coil 2.6.0 (io.coil-kt:coil-compose)
- Material3 1.4.0 (androidx.compose.material3:material3)
- Kotlin 2.4.20, AGP 9.4.0, compileSdk 37

**Spec:** This plan is based on comprehensive codebase analysis of all 50+ Kotlin files, 14 layout XMLs, 5 menu XMLs, and resource files.

## Global Constraints
- Jetpack Compose BOM 2026.09.00 (stable)
- Material3 1.4.0 (stable, not Expressive)
- Kotlin 2.4.20
- AGP 9.4.0
- Navigation Component 2.10.1 (Fragment-based XML nav graph preserved)
- Coil 2.6.0 for image loading (replaces AppIconCache)

---
## Tasks

### Task 1: Update Dependencies in gradle/libs.versions.toml
**Files:**
- Modify: `gradle/libs.versions.toml`

**Steps:**
- [ ] Add `coil = "2.6.0"` and `coilCompose = "2.6.0"` to the `[versions]` section
- [ ] Add `coil = { module = "io.coil-kt:coil", version.ref = "coil" }` and `coilCompose = { module = "io.coil-kt:coil-compose", version.ref = "coilCompose" }` to the `[libraries]` section

### Task 2: Update Dependencies in app/build.gradle.kts
**Files:**
- Modify: `app/build.gradle.kts`

**Steps:**
- [ ] Add `implementation(libs.coil)` to the dependencies block
- [ ] Add `implementation(libs.coilCompose)` to the dependencies block

### Task 3: Create AppIconRequest Data Class
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/utils/AppIconRequest.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.utils`
- [ ] Add `@Immutable` annotation to the data class
- [ ] Implement `data class AppIconRequest(val packageName: String, val userId: Int = 0, val size: Int = Dp.Size.dpToPx(48.dp), val grayscale: Boolean = false, val synthesizeAdaptive: Boolean = HailData.synthesizeAdaptiveIcons, val iconPack: String = HailData.iconPack)`
- [ ] Make it implement `ImageRequest.Data`
- [ ] Override `toString()` to return `"appicon://$packageName|$userId|$size|$grayscale|$synthesizeAdaptive|$iconPack"`

### Task 4: Create AppIconDecoder
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/utils/AppIconDecoder.kt` (or add to AppIconRequest.kt)

**Steps:**
- [ ] Create the class `AppIconDecoder @Inject constructor(@Suppress("UNUSED_PARAMETER") context: Context) : ImageDecoder<AppIconRequest>`
- [ ] Implement the `decode()` method that reuses `AppIconLoader.getOrLoadBitmap` logic
- [ ] Add inner class `SimpleImagePool.Closeable` for bitmap management
- [ ] Ensure proper close() and getBitmap() implementations

### Task 5: Configure ImageLoader in HailApp
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/HailApp.kt`

**Steps:**
- [ ] Add `val imageLoader by lazy { ImageLoader.Builder(this) ... build() }`
- [ ] Configure `.componentRegistry { add(AppIconDecoder.Factory()) }`
- [ ] Set up `.memoryCache { LruMemoryCache(maxSizePercent = 0.25) }`
- [ ] Set up `.diskCache { FileDiskCache(File(cacheDir, "coil_icons")) }`
- [ ] Enable `.crossfade(true)`

### Task 6: Create Reusable AppIcon Composable
**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/AppIcon.kt`

**Steps:**
- [ ] Create the file with package `com.aistra.hail.ui.theme`
- [ ] Implement `@Composable fun AppIcon(request: AppIconRequest, contentDescription: String? = null, modifier: Modifier = Modifier)`
- [ ] Use `rememberImagePainter(data = request, builder = { crossfade(true) })`
- [ ] Calculate size from `request.size.dp`
- [ ] Create `Canvas(modifier = modifier.size(size))`
- [ ] Draw the icon using `painter.paint?.let { paint -> drawIntoCanvas { it.drawImage(...) } }`
- [ ] Handle contentDescription for accessibility (note: relies on parent to set semantics)

### Task 7: Update Theme.kt with Typography and Shapes
**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt`

**Steps:**
- [ ] Define `private val DarkColorScheme = darkColorScheme(...)` with specific Material3 colors
- [ ] Define `private val LightColorScheme = lightColorScheme(...)` with specific Material3 colors
- [ ] Update `@Composable fun HailTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)` to use `MaterialTheme` with `colorScheme`, `typography`, and `shapes`
- [ ] Add `private val Typography = Typography(...)` with appropriate text styles (bodyLarge, bodyMedium, etc.)
- [ ] Add `private val Shapes = Shapes(...)` with rounded corner shapes (small, medium, large)

### Task 8: Remove AppIconCache.kt
**Files:**
- Delete: `app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt`

**Steps:**
- [ ] Delete the file
- [ ] Verify no remaining references through compiler errors or IDE search

## Validation Checklist

After completing all tasks above:
- [ ] App builds successfully (`./gradlew assembleDebug`)
- [ ] AppIcon composable displays icons correctly in all screens
- [ ] Icons load with placeholder and error handling
- [ ] Memory and disk caching works (verify via Coil logs)
- [ ] Theme colors and typography applied correctly in light/dark mode
- [ ] No regression in existing icon-dependent features (AppsFragment, ActionsFragment, etc.)
- [ ] AppIconCache.kt removed and no references remain
- [ ] Coil dependency resolves without conflicts
- [ ] ImageLoader singleton properly initialized in HailApp
- [ ] Accessibility: Icons have contentDescription when used
- [ ] Performance: Icon loading doesn't cause jank on scroll

## Mitigation Strategies

- **Pager migration complexity (very high):** Break into subtasks (ComposeView, PagerScreen composable, header, grid, multi-select toolbar, tag edit dialog), use rememberSaveable for UI state, validate each tab type independently, and add swipe-to-refresh for app lists.
- **State persistence verification:** Use rememberSaveable for UI state (selected tab, scroll state, multi-select state) and ensure ViewModels are Hilt-injected and survive configuration changes; test rotation and multi-window scenarios.
- **Performance benchmarks:** Use LazyVerticalGrid/LazyColumn for efficient rendering, baseline profiles, test on low-end devices (API 24 emulator), and monitor frame timing with Macrobenchmark.
- **Accessibility compliance:** Test with TalkBack, add contentDescription to icons and interactive elements, use semantic properties for state (selected, checked), and verify focus order.
- **State management and ViewModel conversion:** Ensure all ViewModels expose StateFlow for Compose integration, use collectAsStateWithLifecycle for lifecycle-aware collection, and avoid exposing MutableStateFlow directly to UI.
- **Migration rollback risk:** Use feature branch `migrate/compose`, sequential PRs (one per screen), CI validation (build, unit tests, lint, connectedAndroidTest on emulator), manual QA on physical device, and internal tester releases before production.

## References
- [Coil Compose Documentation](https://coil-kt.github.io/coil/compose/)
- [Material3 Theming](https://developer.android.com/jetpack/compose/themes)
- [Custom ImageDecoder in Coil](https://coil-kt.github.io/coil/custom-decoders/)