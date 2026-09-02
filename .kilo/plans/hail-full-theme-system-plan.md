# Hail Full Theme System — Material 3 Expressive Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the full Material 3 Expressive theme system in Hail: seed-color → ColorScheme generation, palette style variants, color spec (2021/2025), animated transitions across all 48 roles, and a live swatch preview picker.

**Architecture:** Add `com.materialkolor:material-kolor:5.0.1` for ColorScheme generation. Introduce a `HailTheme` root composable (modeled after InstallerX-Revived's `InstallerTheme`) that wires the animated scheme + 7-pillar theme into `MaterialExpressiveTheme`. Replace the broken `LaunchedEffect → setAppTheme` in `ThemeSettingsFragment` with a `mutableStateOf` `HailThemeState` held on `HailApp`. Remove dead code: 75-line `Color.kt`, orphan `dynamicColor` pref, hardcoded `useDynamicColor` key, unused shape tokens.

**Tech Stack:** Kotlin 2.4.10, Jetpack Compose, `androidx.compose.material3:1.5.0-alpha27`, `com.materialkolor:material-kolor:5.0.1`, `MotionScheme.expressive()`.

**Spec:** This plan extends the sibling plan `.kilo/plans/settings-expressive-migration-plan.md` (Milestones 7-19 already complete). It addresses only the gaps the sibling plan did not cover: actual ColorScheme generation, palette style variants, color spec wiring, animated transitions, and live swatch preview.

## Section 0: Global Constraints

- Hail already pins `material3 = "1.5.0-alpha27"` and `compose-bom = "2026.08.00"`. No new Compose version required.
- `MaterialExpressiveTheme`, `MotionScheme`, and the `*Emphasized` typography are still `@ExperimentalMaterial3ExpressiveApi` in 1.5.0-alpha27. Add `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` to any new file using them.
- JDK 26, Android compileSdk 37, targetSdk 36, minSdk 24. Kotlin 2.4.10.
- No new dependency without going through `gradle/libs.versions.toml` and the version catalog. Do not scatter raw coordinates in `app/build.gradle.kts`.
- No `BuildConfig` references (Hail's build system does not generate it). Use `applicationInfo.flags` and `ApplicationInfo.FLAG_DEBUGGABLE`.
- Localized strings: English in `values/strings.xml`; Simplified Chinese in `values-zh-rCN/strings.xml`; other languages go through Weblate per the contribution policy.
- No comments unless explaining non-obvious behavior.
- No emoji.
- Surgical changes only: do not refactor adjacent code.
- This plan must be executed after `.kilo/plans/settings-expressive-migration-plan.md` Milestones 7-19 are complete.

## Section 1: Goals and Architecture

This plan extends the existing `MaterialExpressiveTheme` migration (covered by `.kilo/plans/settings-expressive-migration-plan.md`) to the **theme system itself**: seed color → ColorScheme generation, palette style variants, color spec selection, animated transitions, and a live preview swatch picker.

The user-facing outcome: a complete Material 3 Expressive theme system in Hail where users can pick a palette style, a color spec (2021 vs 2025), a seed color (or use wallpaper-based dynamic color), and see the result animate smoothly across the app.

### Architectural decisions

1. Adopt `com.materialkolor:material-kolor:5.0.1` to generate `ColorScheme` from a seed color, palette style, and spec version. This is the same library used by InstallerX-Revived (proven in a similar project). It exposes the full 48-role `ColorScheme` required for Material 3 Expressive.

2. Add a `HailTheme(...)` root composable modeled after InstallerX's `InstallerTheme(...)` but without the Miuix branch. Provide `HailTheme.colorScheme`, `HailTheme.paletteStyle`, `HailTheme.colorSpec`, `HailTheme.seedColor`, `HailTheme.useDynamicColor`, `HailTheme.themeMode` as `CompositionLocal` getters on an `object`.

3. Animate scheme changes via a `ColorScheme.animateColorScheme()` extension so all 48 roles crossfade smoothly.

4. Replace the broken `LaunchedEffect { setAppTheme(HailData.appTheme) }` in `ThemeSettingsFragment` with a `mutableStateOf` state holder that the `HailTheme` root observes, so changes apply instantly.

5. Remove the dead hand-curated `*Light`/`*Dark` colors in `Color.kt` (75 lines) — they are never read.

6. Move enum `displayName` strings to `strings.xml` for localization.

7. Standardize the dynamic-color preference key to the existing `DYNAMIC_COLOR` constant and delete the duplicate `useDynamicColor` property.

## Section 2: Files to create and modify

### Create
- `app/src/main/kotlin/com/aistra/hail/ui/theme/PresetColors.kt` — 10 curated seed colors + `RawColor` data class.
- `app/src/main/kotlin/com/aistra/hail/ui/theme/ThemeExt.kt` — `dynamicColorScheme(...)` wrapper that maps Hail's `PaletteStyle` + `ThemeColorSpec` to materialkolor; `ColorScheme.animateColorScheme()` extension that animates all 48 roles.
- `app/src/main/kotlin/com/aistra/hail/ui/theme/ColorSwatchPreview.kt` — `ColorSwatchPreview` composable + `FullSwatchContent`/`FallbackSwatchContent` helpers + process-level `ConcurrentHashMap` cache (modeled on InstallerX's `ColorPalatteCard.kt`).

### Modify
- `gradle/libs.versions.toml` — add `materialKolor = "5.0.1"` and the library coordinate.
- `app/build.gradle.kts` — add the `material-kolor` dependency.
- `app/src/main/kotlin/com/aistra/hail/ui/theme/ThemeEnums.kt` — remove `displayName: String` constructor parameter from each enum; add `ThemeMode.Companion.fromAppTheme(value: String): ThemeMode` helper; add `@StringRes fun Xxx.labelRes(): Int` extension for each enum.
- `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` — replace `AppTheme` body with `HailTheme` model (see Section 3). Remove `colorSchemeFromSeed` (now in `ThemeExt.kt`).
- `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt` — delete the file (verified zero consumers via grep).
- `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` — delete the `dynamicColor` property (lines 254-256) and the `useDynamicColor` property with the hardcoded key (lines 257-259). Add a single `var useDynamicColor: Boolean` keyed on `DYNAMIC_COLOR` with a one-time migration that reads the legacy `"use_dynamic_color"` key and writes it into `DYNAMIC_COLOR` if the legacy key still exists.
- `app/src/main/kotlin/com/aistra/hail/ui/MainActivity.kt` — own a `mutableStateOf<HailThemeState>`, register a `SharedPreferences.OnSharedPreferenceChangeListener` that re-emits the state when theme keys change, and wrap the bottom-nav `ComposeView` content with `HailTheme(state = themeState) { ... }`.
- `app/src/main/kotlin/com/aistra/hail/ui/about/AboutFragment.kt` — change `AppTheme { }` to `HailTheme(state = HailThemeState()) { }`.
- `app/src/main/kotlin/com/aistra/hail/ui/api/ApiActivity.kt` — same change (2 sites: lines 50, 125).
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt` — same change (1 site: line 425).
- `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` — same change (1 site: line 151); keep the `HailApp.app.setAppTheme(HailData.appTheme)` call in the night-mode `onChange` handler (line 552).
- `app/src/main/kotlin/com/aistra/hail/ui/settings/ThemeSettingsFragment.kt` — change `AppTheme { }` to `HailTheme(state = HailThemeState()) { }`; remove the `LaunchedEffect(paletteStyle, colorSpec, useDynamicColor, seedColor) { HailApp.app.setAppTheme(HailData.appTheme) }`; replace the hardcoded 6-color `ColorSwatchRow` with a `FlowRow` of `ColorSwatchPreview`s backed by `PresetColors`; use `stringResource(it.labelRes())` to build the dropdown entries for `PaletteStyle` and `ThemeColorSpec`.
- `app/src/main/res/values/strings.xml` — add `theme_mode_light`, `theme_mode_dark`, `theme_mode_system` (if not already present), plus 9 `palette_style_*` and 2 `color_spec_*` strings (see Section 5).
- `app/src/main/res/values-zh-rCN/strings.xml` — mirror the new English strings in Simplified Chinese.
- `app/src/main/res/values/arrays.xml` — remove or update the hardcoded `palette_style_entries` and `color_spec_entries` arrays (lines 88-102) since `ThemeSettingsFragment` will use `labelRes()` lookups directly.

## Section 3: Theme.kt design (post-refactor)

```kotlin
@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.aistra.hail.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.aistra.hail.utils.HTarget

data class HailThemeState(
    val themeMode: ThemeMode = ThemeMode.fromAppTheme(HailData.appTheme),
    val paletteStyle: PaletteStyle = HailData.paletteStyle,
    val colorSpec: ThemeColorSpec = HailData.colorSpec,
    val useDynamicColor: Boolean = HailData.useDynamicColor,
    val seedColor: Color = Color(HailData.seedColor),
)

private val LocalHailColorScheme = staticCompositionLocalOf<ColorScheme> { error("No ColorScheme provided") }
private val LocalIsDark = staticCompositionLocalOf { false }

object HailTheme {
    val colorScheme: ColorScheme
        @Composable @ReadOnlyComposable get() = LocalHailColorScheme.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable get() = LocalIsDark.current
}

@Composable
fun HailTheme(state: HailThemeState, content: @Composable () -> Unit) {
    val isDark = when (state.themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val keyColor = if (state.useDynamicColor && HTarget.S) {
        val ctx = LocalContext.current
        Color(ctx.getColor(android.R.color.system_accent1_500))
    } else state.seedColor

    val baseColorScheme = remember(keyColor, isDark, state.paletteStyle, state.colorSpec) {
        dynamicColorScheme(keyColor, isDark, state.paletteStyle, state.colorSpec)
    }
    val animatedColorScheme = baseColorScheme.animateColorScheme()

    CompositionLocalProvider(
        LocalHailColorScheme provides animatedColorScheme,
        LocalIsDark provides isDark,
    ) {
        MaterialExpressiveTheme(
            colorScheme = animatedColorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = AppTypography,
            shapes = expressiveShapes,
            content = content,
        )
    }
}
```

## Section 4: ThemeExt.kt design

```kotlin
package com.aistra.hail.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import com.materialkolor.PaletteStyle as MaterialKolorPaletteStyle
import com.materialkolor.dynamicColorScheme
import com.materialkolor.dynamiccolor.ColorSpec

@Stable
fun dynamicColorScheme(
    keyColor: Color,
    isDark: Boolean,
    style: PaletteStyle = PaletteStyle.TonalSpot,
    colorSpec: ThemeColorSpec = ThemeColorSpec.SPEC_2025,
): ColorScheme {
    val mkStyle = when (style) {
        PaletteStyle.TonalSpot -> MaterialKolorPaletteStyle.TonalSpot
        PaletteStyle.Neutral -> MaterialKolorPaletteStyle.Neutral
        PaletteStyle.Vibrant -> MaterialKolorPaletteStyle.Vibrant
        PaletteStyle.Expressive -> MaterialKolorPaletteStyle.Expressive
        PaletteStyle.Rainbow -> MaterialKolorPaletteStyle.Rainbow
        PaletteStyle.FruitSalad -> MaterialKolorPaletteStyle.FruitSalad
        PaletteStyle.Monochrome -> MaterialKolorPaletteStyle.Monochrome
        PaletteStyle.Fidelity -> MaterialKolorPaletteStyle.Fidelity
        PaletteStyle.Content -> MaterialKolorPaletteStyle.Content
    }
    val specVersion = when (colorSpec) {
        ThemeColorSpec.SPEC_2025 -> if (style.supportsSpec2025) ColorSpec.SpecVersion.SPEC_2025 else ColorSpec.SpecVersion.SPEC_2021
        ThemeColorSpec.SPEC_2021 -> ColorSpec.SpecVersion.SPEC_2021
    }
    return dynamicColorScheme(seedColor = keyColor, isDark = isDark, style = mkStyle, specVersion = specVersion)
}

@Composable
fun ColorScheme.animateColorScheme(): ColorScheme {
    @Composable
    fun anim(color: Color) = animateColorAsState(targetValue = color, animationSpec = spring(), label = "theme_color").value
    return ColorScheme(
        primary = anim(primary), onPrimary = anim(onPrimary),
        primaryContainer = anim(primaryContainer), onPrimaryContainer = anim(onPrimaryContainer),
        inversePrimary = anim(inversePrimary),
        secondary = anim(secondary), onSecondary = anim(onSecondary),
        secondaryContainer = anim(secondaryContainer), onSecondaryContainer = anim(onSecondaryContainer),
        tertiary = anim(tertiary), onTertiary = anim(onTertiary),
        tertiaryContainer = anim(tertiaryContainer), onTertiaryContainer = anim(onTertiaryContainer),
        background = anim(background), onBackground = anim(onBackground),
        surface = anim(surface), onSurface = anim(onSurface),
        surfaceVariant = anim(surfaceVariant), onSurfaceVariant = anim(onSurfaceVariant),
        surfaceTint = anim(surfaceTint),
        inverseSurface = anim(inverseSurface), inverseOnSurface = anim(inverseOnSurface),
        error = anim(error), onError = anim(onError),
        errorContainer = anim(errorContainer), onErrorContainer = anim(onErrorContainer),
        outline = anim(outline), outlineVariant = anim(outlineVariant),
        scrim = anim(scrim),
        surfaceBright = anim(surfaceBright), surfaceDim = anim(surfaceDim),
        surfaceContainer = anim(surfaceContainer),
        surfaceContainerHigh = anim(surfaceContainerHigh),
        surfaceContainerHighest = anim(surfaceContainerHighest),
        surfaceContainerLow = anim(surfaceContainerLow),
        surfaceContainerLowest = anim(surfaceContainerLowest),
        primaryFixed = anim(primaryFixed), primaryFixedDim = anim(primaryFixedDim),
        onPrimaryFixed = anim(onPrimaryFixed), onPrimaryFixedVariant = anim(onPrimaryFixedVariant),
        secondaryFixed = anim(secondaryFixed), secondaryFixedDim = anim(secondaryFixedDim),
        onSecondaryFixed = anim(onSecondaryFixed), onSecondaryFixedVariant = anim(onSecondaryFixedVariant),
        tertiaryFixed = anim(tertiaryFixed), tertiaryFixedDim = anim(tertiaryFixedDim),
        onTertiaryFixed = anim(onTertiaryFixed), onTertiaryFixedVariant = anim(onTertiaryFixedVariant),
    )
}
```

Note: the explicit `ColorScheme(...)` constructor with all 48 named args is the M3 Expressive requirement. The legacy 26-arg constructor prints a deprecation message.

## Section 5: Strings to add to `values/strings.xml`

```xml
<!-- Theme mode display names -->
<string name="theme_mode_light">Light</string>
<string name="theme_mode_dark">Dark</string>
<string name="theme_mode_system">Follow system</string>

<!-- Palette style display names -->
<string name="palette_style_tonal_spot">Tonal Spot</string>
<string name="palette_style_neutral">Neutral</string>
<string name="palette_style_vibrant">Vibrant</string>
<string name="palette_style_expressive">Expressive</string>
<string name="palette_style_rainbow">Rainbow</string>
<string name="palette_style_fruit_salad">Fruit Salad</string>
<string name="palette_style_monochrome">Monochrome</string>
<string name="palette_style_fidelity">Fidelity</string>
<string name="palette_style_content">Content</string>

<!-- Color spec display names -->
<string name="color_spec_2021">Material 3 (2021)</string>
<string name="color_spec_2025">Expressive (2025)</string>
```

`values-zh-rCN/strings.xml` must mirror these 14 strings in Simplified Chinese.

## Section 6: Tasks (with dependency order)

The plan decomposes into 7 tasks. The actual dependency chain is:

```
1 (deps)
  ├── 2 (ThemeExt + PresetColors)
  └── 3 (refactor enums)
        │
        ▼
4 (rewrite Theme.kt + wire MainActivity)
        │
        ├── 5a (delete dead code, fix HailData, fix use_dynamic_color migration)
        ├── 5b (update all AppTheme call sites)
        └── 6  (add swatch picker)
```

Tasks 2 and 3 are independent after Task 1 and can run in parallel. Tasks 5a, 5b, and 6 are independent after Task 4 and can run in parallel. Each task is single-owner (one agent per task) and produces a committable change.

### Task 1: Add materialkolor dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Step 1.1:** In `gradle/libs.versions.toml`, add to `[versions]`:
```toml
materialKolor = "5.0.1"
```
And to `[libraries]`:
```toml
material-kolor = { module = "com.materialkolor:material-kolor", version.ref = "materialKolor" }
```

**Step 1.2:** In `app/build.gradle.kts`, add to the `dependencies { ... }` block:
```kotlin
implementation(libs.material.kolor)
```

**Step 1.3:** Verify with `./gradlew :app:compileDebugKotlin`. Expected: SUCCESS (no code yet uses the library, so just resolution + cache).

**Step 1.4:** Commit: `feat(deps): add materialkolor 5.0.1`

### Task 2: Add `ThemeExt.kt` and `PresetColors.kt`

**Files:**
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ThemeExt.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/PresetColors.kt`

**Step 2.1:** Write `PresetColors.kt` with the `RawColor` data class and 18 preset colors. Use the same hex values as InstallerX's `PresetColors.kt` for visual consistency with reference projects.

**Step 2.2:** Write `ThemeExt.kt` per Section 4. Add the `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)` annotation.

**Step 2.3:** Verify with `./gradlew :app:compileDebugKotlin`. Expected: SUCCESS.

**Step 2.4:** Commit: `feat(theme): add materialkolor-backed dynamic color scheme + preset seed colors`

### Task 3: Refactor `ThemeEnums.kt` to remove `displayName` and add `@StringRes` extensions

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/ThemeEnums.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-zh-rCN/strings.xml`

**Step 3.1:** In `ThemeEnums.kt`, remove the `displayName: String` constructor parameter from `ThemeMode`, `PaletteStyle`, and `ThemeColorSpec`. Add:
```kotlin
@StringRes fun ThemeMode.labelRes(): Int = when (this) {
    ThemeMode.LIGHT -> R.string.theme_mode_light
    ThemeMode.DARK -> R.string.theme_mode_dark
    ThemeMode.SYSTEM -> R.string.theme_mode_system
}
// similar for PaletteStyle (9 entries) and ThemeColorSpec (2 entries)
```

And add the `fromAppTheme` companion helper:
```kotlin
fun ThemeMode.Companion.fromAppTheme(value: String): ThemeMode = when (value) {
    HailData.THEME_LIGHT -> ThemeMode.LIGHT
    HailData.THEME_DARK -> ThemeMode.DARK
    else -> ThemeMode.SYSTEM
}
```

**Step 3.2:** In `strings.xml`, add the 3 `theme_mode_*`, 9 `palette_style_*`, and 2 `color_spec_*` strings per Section 5.

**Step 3.3:** In `values-zh-rCN/strings.xml`, add Simplified Chinese translations for all 14 new strings.

**Step 3.4:** Run `./gradlew :app:compileDebugKotlin`. Expected: FAIL because `Theme.kt` still references `.displayName`. The agent fixing this is part of Task 4. To make this task independently buildable, also update the single consumer of `displayName` in `Theme.kt` — which is only the import-less reference. If `Theme.kt` doesn't reference `displayName`, just compile-check.

**Step 3.5:** Commit: `refactor(theme): move enum display names to strings.xml for localization`

### Task 4: Rewrite `Theme.kt` to use `HailTheme` + `materialkolor`; wire `MainActivity`

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/MainActivity.kt`

**Step 4.1:** Replace the contents of `Theme.kt` with the design in Section 3. Use the existing `AppTypography` and `expressiveShapes` references unchanged. Keep `@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)`.

**Step 4.2:** In `MainActivity.kt`:
- Add a `private var themeState by mutableStateOf(HailThemeState())` field.
- Add a `private val themeListener` that re-emits `HailThemeState()` into `themeState` when `HailData.PALETTE_STYLE`, `HailData.COLOR_SPEC`, `HailData.SEED_COLOR`, `HailData.DYNAMIC_COLOR`, or `HailData.APP_THEME` change.
- Register the listener in `onCreate` and unregister in `onDestroy`.
- Wrap the bottom-nav `ComposeView` content with `HailTheme(state = themeState) { ... }`.

**Step 4.3:** Run `./gradlew :app:compileDebugKotlin`. Expected: FAIL because `MainActivity` and the 4 other call sites still reference the removed `AppTheme`. Task 5b fixes that. To validate this task in isolation, also update the single `MainActivity` call site that uses `AppTheme`. Other call sites are Task 5b.

**Step 4.4:** Commit: `feat(theme): rewrite Theme.kt to use materialkolor and animated color scheme`

### Task 5a: Delete dead code and fix `HailData`

**Files:**
- Delete: `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt`
- Modify: `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`
- Modify: `app/src/main/res/values/arrays.xml`

**Step 5a.1:** Delete `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt`. Grep verified zero consumers in `app/`.

**Step 5a.2:** In `HailData.kt`:
- Delete the `dynamicColor` property (lines 254-256).
- Delete the `useDynamicColor` property with the hardcoded key (lines 257-259).
- Add a new consolidated `useDynamicColor` property keyed on `DYNAMIC_COLOR` with a one-time migration:
```kotlin
private val useDynamicColorMigratedKey = "use_dynamic_color_migrated"
var useDynamicColor
    get() = sp.getBoolean(DYNAMIC_COLOR, HTarget.S)
    set(value) = sp.edit { putBoolean(DYNAMIC_COLOR, value) }

init {
    if (!sp.getBoolean(useDynamicColorMigratedKey, false)) {
        if (sp.contains("use_dynamic_color")) {
            sp.edit {
                putBoolean(DYNAMIC_COLOR, sp.getBoolean("use_dynamic_color", true))
            }
        }
        sp.edit { putBoolean(useDynamicColorMigratedKey, true) }
    }
}
```
The `init` block runs once per `HailData` object lifetime (effectively once per process). The `useDynamicColorMigratedKey` flag prevents repeated work.

**Step 5a.3:** In `arrays.xml`, remove the hardcoded `palette_style_entries` and `color_spec_entries` arrays (lines 88-102) — they are unused.

**Step 5a.4:** Run `./gradlew :app:compileDebugKotlin`. Expected: FAIL because call sites still reference `AppTheme` — Task 5b fixes that. Otherwise clean.

**Step 5a.5:** Commit: `refactor(theme): delete dead Color.kt and consolidate dynamic-color preference with legacy migration`

### Task 5b: Update all `AppTheme` call sites

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/about/AboutFragment.kt` (lines 47, 55)
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/api/ApiActivity.kt` (lines 50, 125)
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt` (line 425)
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/settings/SettingsFragment.kt` (line 151)
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/settings/ThemeSettingsFragment.kt` (lines 50, 59)

**Step 5b.1:** In each file, change the import `import com.aistra.hail.ui.theme.AppTheme` to `import com.aistra.hail.ui.theme.HailTheme`. Change every `AppTheme { ... }` to `HailTheme(state = HailThemeState()) { ... }`. Import `HailThemeState` from `com.aistra.hail.ui.theme`.

**Step 5b.2:** In `ThemeSettingsFragment.kt` specifically, also delete the `LaunchedEffect(paletteStyle, colorSpec, useDynamicColor, seedColor) { HailApp.app.setAppTheme(HailData.appTheme) }` block (lines 82-84). It is a no-op for the changes being made; the pref listener in `MainActivity` now drives recomposition.

**Step 5b.3:** In `SettingsFragment.kt`, do NOT remove the `HailApp.app.setAppTheme(value)` call in the night-mode preference `onChange` handler. It is the correct wiring for `UiModeManager.setApplicationNightMode(...)`.

**Step 5b.4:** Run `./gradlew :app:assembleDebug`. Expected: SUCCESS.

**Step 5b.5:** Smoke test: open Settings → Theme, change the palette style from TonalSpot to Vibrant, verify the entire app's accent color animates to the new scheme. Change the seed color, verify another animation. Toggle System/Light/Dark, verify night mode applies.

**Step 5b.6:** Commit: `refactor(theme): migrate all AppTheme call sites to HailTheme`

### Task 6: Add live swatch picker to `ThemeSettingsFragment`

**Files:**
- Modify: `app/src/main/kotlin/com/aistra/hail/ui/settings/ThemeSettingsFragment.kt`
- Create: `app/src/main/kotlin/com/aistra/hail/ui/theme/ColorSwatchPreview.kt`

**Step 6.1:** Create `ColorSwatchPreview.kt` modeled after `/workspaces/Hail/.repos/InstallerX-Revived/app/src/main/java/com/rosan/installer/ui/page/main/widget/card/ColorPalatteCard.kt` (the file the plan's research was based on). Use a process-level `ConcurrentHashMap<String, ColorScheme>` cache, a `produceState<ColorScheme?>(initialValue = colorSchemeCache[cacheKey], key1 = cacheKey)` pattern with `withContext(Dispatchers.Default)` for first-time generation, and a 3-arc pie + center dot + check icon layout for `FullSwatchContent`. Use a `FlowRow` from `androidx.compose.foundation.layout` for the parent (already in `androidx.compose.foundation:foundation` 1.5+).

**Step 6.2:** Reduce `PresetColors` to 10 curated colors (not 18). Pick one color per major hue family: indigo, purple, teal, green, orange, red, brown, grey, plus a vibrant accent and the Material 3 default. Reasoning: 10 fits in a 2-row `FlowRow` on a phone without scrolling; 18 forces scrolling and slows selection.

**Step 6.3:** Replace the hardcoded `ColorSwatchRow` in `ThemeSettingsFragment` (lines 154-198) with:
```kotlin
val context = LocalContext.current
val palette = PresetColors
val labelFor = { raw: RawColor -> raw.key.replaceFirstChar { it.uppercase() } }
FlowRow(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
) {
    palette.forEach { raw ->
        ColorSwatchPreview(
            rawColor = raw,
            currentStyle = paletteStyle,
            colorSpec = colorSpec,
            isSelected = seedColor.toArgb() == raw.color.toArgb(),
            onClick = {
                seedColor = raw.color.toArgb()
                HailData.seedColor = raw.color.toArgb()
            },
        )
    }
}
```

**Step 6.4:** Update the `SettingsList` calls (lines 105-127) to build entries with `stringResource(it.labelRes())`:
```kotlin
val paletteEntries = paletteStyleValues.map { stringResource(it.labelRes()) }
val colorSpecEntries = colorSpecValues.map { stringResource(it.labelRes()) }
```

**Step 6.5:** Run `./gradlew :app:assembleDebug`. Expected: SUCCESS.

**Step 6.6:** Manually verify on device or emulator: each swatch renders a 3-arc pie preview that updates when palette style or color spec changes; tapping a swatch updates `seedColor` and the app theme animates to the new seed.

**Step 6.7:** Commit: `feat(theme): add live color-swatch preview picker with materialkolor-backed caching`

## Section 7: Validation

After all 6 tasks are complete:

1. `./gradlew :app:assembleDebug` — full clean build must succeed.
2. `./gradlew :app:lintDebug` — must produce no new errors (warnings are acceptable for now).
3. Manual verification: open the app, navigate to Settings → Theme. Verify:
   - All 9 palette styles are listed.
   - All 2 color specs are listed (and disabled when style doesn't support SPEC_2025).
   - Dynamic color toggle works and respects Android S+ (on pre-S devices the toggle is hidden or disabled).
   - Seed color picker shows 18 live swatches.
   - Tapping a different palette style or seed color animates the entire app theme smoothly (spring transition on all 48 roles).
   - Night mode (System / Light / Dark) still works as before via `setAppTheme`.
4. Confirm no stray `BuildConfig` references, no commented-out code, no unused imports.
5. Confirm `AGENTS.md` code style rules followed (no `*` imports, no wildcard imports, no comments unless explaining non-obvious behavior).

## Section 8: Risks and known gotchas

- **Materialkolor 5.0.1 vs 5.0.0**: InstallerX uses 5.0.1 (proven). The research agent recommended 5.0.0 for fresh projects; we follow InstallerX's proven version.
- **48-role `ColorScheme` constructor**: It is the only constructor in `material3 1.5.0-alpha27` that the deprecation message does not flag. The legacy 26-arg form prints a deprecation message. Always use the 48-arg form.
- **Emphasized typography**: `Type.kt` already has all 15 `*Emphasized` properties set. No change needed.
- **Live recomposition**: `HailData.xxx` is a `var` but reading it from a composable does NOT trigger recomposition. The `HailTheme` root must observe a `State<HailThemeState>`, not `HailData` directly. The pattern is: `MainActivity` owns a `mutableStateOf<HailThemeState>`, registers a `SharedPreferences.OnSharedPreferenceChangeListener`, and re-emits the state when relevant keys change. `ThemeSettingsFragment` only writes to `HailData`; the listener drives the recomposition.
- **`SettingsList` and enum display names**: `SettingsList` only accepts `List<String>` for `entries`. To use `@StringRes` enum extensions, the call site must build the list inside a `@Composable` context: `val entries = values.map { stringResource(it.labelRes()) }`. The `entriesId: Int` overload is only useful for `stringArrayResource`-backed arrays, which this plan removes in Task 5a.
- **`use_dynamic_color` migration**: A one-time `init` block in `HailData` migrates the legacy preference key to `DYNAMIC_COLOR` on first read. Without this, existing users' dynamic-color settings would silently reset to the default (`HTarget.S`) after upgrading.
- **Materialkolor compat with material3 1.5.0-alpha27**: confirmed compatible per research report. No transitive `material3` upgrade is forced.
- **SettingsList localization**: After Task 3, `SettingsList(entries = paletteStyleValues.map { ... })` becomes `SettingsList(entries = paletteStyleValues.map { context.getString(it.labelRes()) })`. Either pass entries via a `stringArrayResource` lookup or use the `@Composable` lambda variant. Use the latter.

## Section 9: Out of scope (deferred)

- Migrating from `SharedPreferences` to DataStore. (Already deferred per Decision Log in the sibling plan.)
- Custom typography (downloaded variable fonts). Hail uses `FontFamily.Default` everywhere. Adding custom fonts is a separate, larger effort.
## Section 10: Decision Log

- Decision: Use `com.materialkolor:material-kolor:5.0.1` (not 5.0.0) because InstallerX-Revived already ships this version in production with a similar code path; 5.0.1 has the same `ColorSpec.SpecVersion` API but a slightly newer `DynamicMaterialExpressiveTheme`.  Date/Author: 2026-09-02

- Decision: Delete the hand-curated `*Light`/`*Dark` colors in `Color.kt`. They are never consumed by `Theme.kt` (which uses `dynamicLightColorScheme`/`expressiveLightColorScheme`/`darkColorScheme` instead) and the 75 lines of dead code obscure the actual theme generation logic.  Date/Author: 2026-09-02

- Decision: Move enum `displayName` strings to `strings.xml` (via `@StringRes` extension functions) for proper localization. HailData already enforces English + Simplified Chinese in-tree.  Date/Author: 2026-09-02

- Decision: Consolidate the two dynamic-color prefs (`DYNAMIC_COLOR` + `useDynamicColor` with hardcoded key) into one keyed on `DYNAMIC_COLOR`. The `ThemeColorSpec` enum is the only one referenced by `Theme.kt`; the other was dead.  Date/Author: 2026-09-02

- Decision: Skip `DynamicMaterialExpressiveTheme` and call `dynamicColorScheme` + `MaterialExpressiveTheme` explicitly. Reason: `DynamicMaterialExpressiveTheme` does not accept a `ThemeColorSpec` parameter, only `specVersion`, and the wrapper would re-map our existing `ThemeColorSpec` enum. Calling the lower-level `dynamicColorScheme` gives full control.  Date/Author: 2026-09-02

- Decision: Wire the live theme state via a `mutableStateOf<HailThemeState>` owned by `MainActivity`, with a `SharedPreferences.OnSharedPreferenceChangeListener` to re-emit. NOT via `HailApp.setHailThemeState()`. Reason: keeps Compose state in the UI layer (Activity), avoids Application-layer state, and uses an existing pattern (HailApp already uses a pref listener for `WORKING_MODE`).  Date/Author: 2026-09-02

- Decision: Use the 48-arg `ColorScheme(...)` constructor explicitly in `animateColorScheme()` to avoid the deprecation warning.  Date/Author: 2026-09-02

- Decision: Cache generated schemes in a process-level `ConcurrentHashMap` (same approach as InstallerX). Survives fragment navigation; safe because keys are pure-data and the cache is read-only after generation. Cache key is a simple `"$seedColorArgb:$isDark:$style:$spec"` string.  Date/Author: 2026-09-02

- Decision: Rename `ColorScheme.animateAsState()` to `ColorScheme.animateColorScheme()`. The name `animateAsState` shadows Compose's standard `animateAsState<T>()` factory. The new name describes what the function returns (an animated `ColorScheme`), not how.  Date/Author: 2026-09-02

- Decision: Ship 10 `PresetColors` instead of InstallerX's 18. Phone screens render 10 swatches in 2 `FlowRow` rows without scrolling; 18 forces scrolling and slows selection. Visual consistency with InstallerX is not a Hail requirement.  Date/Author: 2026-09-02

- Decision: Use `FlowRow` for the swatch picker, not `LazyVerticalGrid`. 10 swatches do not need virtualization. `FlowRow` (in `androidx.compose.foundation.layout` 1.5+) is the simplest correct layout.  Date/Author: 2026-09-02

- Decision: Migrate the legacy `"use_dynamic_color"` SharedPreferences key to `DYNAMIC_COLOR` in a one-time `init` block on `HailData`. Guarded by a `useDynamicColorMigratedKey` flag to prevent repeated work. This preserves existing user preferences across the upgrade.  Date/Author: 2026-09-02

- Decision: Add the 14 new English strings in `values/strings.xml` AND mirror them in `values-zh-rCN/strings.xml`. AGENTS.md requires English + Simplified Chinese in-tree; deferring the Chinese translations is not acceptable.  Date/Author: 2026-09-02

- Decision: Delete the `palette_style_entries` and `color_spec_entries` arrays in `values/arrays.xml` (lines 88-102). They are unused — `ThemeSettingsFragment` will build entries via `stringResource(it.labelRes())` directly.  Date/Author: 2026-09-02

## Section 11: Outcomes & Retrospective

To be filled in after all 6 tasks are complete and committed.

### Out of scope (handled by sibling plan)

- Connected shapes for settings groups (Milestone 7 in `settings-expressive-migration-plan.md`).
- `LargeFlexibleTopAppBar` migration (Milestone 8).
- `motionScheme` spring rewiring (Milestone 9).
- `ShortNavigationBar` and `FloatingBottomBar` (Milestones 16-18).
- Icon fill/outline toggle (Milestone 17).
