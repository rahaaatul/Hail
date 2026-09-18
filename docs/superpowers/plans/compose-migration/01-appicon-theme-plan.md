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
- Kotlin 2.4.20, AGP 9.4.0

## File Changes

### 1. Update Dependencies
#### Task 1: Update gradle/libs.versions.toml
- [ ] Add coil = "2.6.0" and coilCompose = "2.6.0" to [versions] section
- [ ] Add coil = { module = "io.coil-kt:coil", version.ref = "coil" } and coilCompose = { module = "io.coil-kt:coil-compose", version.ref = "coilCompose" } to [libraries] section

#### Task 2: Update app/build.gradle.kts
- [ ] Add implementation(libs.coil) and implementation(libs.coilCompose) to dependencies block

### 2. Create AppIcon Data Class + Decoder
#### Task 3: Create AppIconRequest.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/utils/AppIconRequest.kt
- [ ] Implement @Immutable data class AppIconRequest with packageName, userId, size, grayscale, synthesizeAdaptive, iconPack fields
- [ ] Make it implement ImageRequest.Data with proper toString() implementation

#### Task 4: Create AppIconDecoder
- [ ] Create AppIconDecoder class in same file or separate file
- [ ] Implement ImageDecoder<AppIconRequest> with decode() method that reuses AppIconLoader logic
- [ ] Add SimpleImagePool.Closeable inner class for bitmap management
- [ ] Add @Inject constructor with Context parameter

#### Task 5: Configure ImageLoader in HailApp
- [ ] Modify app/src/main/kotlin/com/aistra/hail/HailApp.kt
- [ ] Add val imageLoader by lazy { ImageLoader.Builder(this) ... build() }
- [ ] Configure componentRegistry to add AppIconDecoder.Factory()
- [ ] Set up memory cache with LruMemoryCache(maxSizePercent = 0.25)
- [ ] Set up disk cache with FileDiskCache(File(cacheDir, "coil_icons"))
- [ ] Enable crossfade(true)

### 3. Create Reusable AppIcon Composable
#### Task 6: Create AppIcon.kt
- [ ] Create app/src/main/kotlin/com/aistra/hail/ui/theme/AppIcon.kt
- [ ] Implement @Composable fun AppIcon(request: AppIconRequest, contentDescription: String? = null, modifier: Modifier = Modifier)
- [ ] Use rememberImagePainter with data = request and crossfade(true) builder
- [ ] Create Canvas with modifier.size(request.size.dp)
- [ ] Draw the icon using painter.paint within drawIntoCanvas
- [ ] Add contentDescription handling for accessibility (via parent or semantics)

### 4. Update Theme.kt with Typography and Shapes
#### Task 7: Update Theme.kt
- [ ] Modify app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt
- [ ] Define private val DarkColorScheme = darkColorScheme(...) with specific Material3 colors
- [ ] Define private val LightColorScheme = lightColorScheme(...) with specific Material3 colors
- [ ] Update @Composable fun HailTheme(...) to use MaterialTheme with colorScheme, typography, and shapes
- [ ] Add private val Typography = Typography(...) with appropriate text styles
- [ ] Add private val Shapes = Shapes(...) with rounded corner shapes

### 5. Remove AppIconCache.kt
#### Task 8: Remove AppIconCache.kt
- [ ] Delete app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt
- [ ] Verify no remaining references through compiler errors or search

## Validation Checklist

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

## References

- [Coil Compose Documentation](https://coil-kt.github.io/coil/compose/)
- [Material3 Theming](https://developer.android.com/jetpack/compose/themes)
- [Custom ImageDecoder in Coil](https://coil-kt.github.io/coil/custom-decoders/)