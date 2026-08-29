# Material 3 Expressive UI Migration ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

## Purpose / Big Picture

Hail currently uses standard Material 3 theming. Its Views-based screens (Fragments with View Binding) use `Theme.Material3.DynamicColors.DayNight` as the parent theme with custom color tokens in `values/colors.xml` and `values-night/colors.xml`. Its Compose screens (About, Settings, API activity, tag list) use a hand-rolled `AppTheme` composable that wraps `MaterialTheme` with `lightColorScheme`/`darkColorScheme` and a default `Typography()`.

Material 3 Expressive (M3 Expressive) is the latest evolution of Material Design, shipping with Android 16 after 46 rounds of user research with 18,000 participants. It expands on standard M3 with: vibrant, high-chroma color palettes derived from the HCT color space; an `expressive` MotionScheme using spatial/effects springs for more natural, alive-feeling animations; bolder, larger typography with an "emphasized" variant for every text style; an expanded shape library (`largeIncreased`, `extraLarge`, `extraExtraLarge`, etc.); and 14 new or updated components with more configuration options.

Google's stated position is that **M3 Expressive is bidirectional** — all existing M3 features work with both `MaterialTheme` and `MaterialExpressiveTheme`, and the migration is **gradual**: "update screens as you touch them for other reasons. Nothing in M3 Expressive is so urgent it warrants a dedicated migration sprint unless you're targeting Android 16 specifically."

This plan migrates Hail's Compose theming to `MaterialExpressiveTheme` with the expressive color scheme, motion scheme, typography, and shapes, and aligns the Views-based color tokens with the Expressive palette. After this work, the app presents the more vibrant, emotionally engaging, Android-16-style visual language, while preserving existing dynamic color support (Android 12+), the light/dark theme toggle, and all existing behavior. The APIs are library-level (not OS-level), so they work on all API levels down to the project's minSdk of 23, with dynamic color gracefully falling back to static Expressive palettes on Android 11 and below.

## Progress

- [x] (2026-08-29 12:00Z) Research Material 3 Expressive APIs, Google's migration guidance, and codebase integration points completed
- [x] (2026-08-29 12:30Z) Pre-migration audit (dependencies, MaterialTheme references, RoundedCornerShape, shapeAppearance, fontWeight) — see Audit Findings below
- [x] (2026-08-29 12:45Z) Studied InstallerX-Revived reference implementation as real-world example of M3 Expressive adoption
- [ ] Compose theme migrated from `MaterialTheme` to `MaterialExpressiveTheme` with expressive color scheme, motion, typography, and shapes
- [ ] Expressive typography with emphasized variants and bolder headline weights defined
- [ ] Views-based color tokens reviewed for Expressive alignment
- [ ] Build and resource processing validated (compileDebugKotlin, processDebugResources)
- [ ] All Compose callers verified compiling

## Surprises & Discoveries

### 2026-08-29 — InstallerX-Revived reference implementation study

- Cloned and analyzed InstallerX-Revived (`/tmp/kilo/installerx`) as a real-world Compose-only app that adopted M3 Expressive. Key findings:
- **ThemeColorSpec pattern**: InstallerX exposes a two-option toggle — `SPEC_2021` ("Material 3") vs `SPEC_2025` ("Expressive") — backed by a simple `enum class` with a `displayName` field and a `fromValueOrDefault(value: String)` companion factory. This is much simpler than Hail's 3-option theme system (`FOLLOW_SYSTEM`, `THEME_LIGHT`, `THEME_DARK`) and maps directly to Google's bidirectional design: both specs coexist, users pick one.
- **PaletteStyle enum**: InstallerX also exposes a `PaletteStyle` enum (TonalSpot, Neutral, Vibrant, Expressive, Rainbow, FruitSalad, Monochrome, Fidelity, Content) for choosing the tonal palette generation style. Not all styles support SPEC_2025; `Fidelity`, `Content`, and `Monochrome` fall back to SPEC_2021. This is an advanced option — Hail could omit it and just use the default TonalSpot.
- **Centralised ThemeState**: Instead of reading `SharedPreferences` directly in each call site (Hail's current pattern via `HailData.kt`), InstallerX combines all theme preferences into a single `ThemeState` data class, computed reactively via `combine()` of the preferences flow + wallpaper colors flow (for Android < 12 dynamic color fallback via `MonetCompat`). This is a cleaner architecture for when theme settings grow beyond 3 options.
- **materialkolor library**: InstallerX uses `com.materialkolor:material-kolor:5.0.0` for dynamic color generation (not just `expressiveLightColorScheme()`). This provides the `PaletteStyle` and `ThemeColorSpec` options at runtime, with HCT color space generation. The library version is 5.0.0, registered in `gradle/libs.versions.toml` as `materialKolor = "5.0.0"`. Google's own `expressiveLightColorScheme()` is used as the *baseline* light scheme (no parameters), and `materialkolor` is layered on top for user-customizable seed color + palette style.
- **Settings UI pattern**: A `DropDownMenuWidget` in `ThemeSettingsWidget.kt` conditionally enables the `ColorSpecSelector` based on whether the current `PaletteStyle` supports SPEC_2025. If the style doesn't support it, the dropdown is disabled and falls back to `SPEC_2021` with a static "only 2021 supported" string.
- **Preference storage**: Uses Jetpack DataStore (`AppDataStore`) with a `BooleanSetting`, `IntSetting`, `StringSetting` abstraction in `AppSettingsRepositoryImpl.kt` — each preference is an explicit key mapping. Hail uses `SharedPreferences` via `PreferenceManager.getDefaultSharedPreferences(app)` in the `HailData` object. DataStore is Google's recommended modern API but migrating Hail's entire preference system is out of scope for this theme migration.

## Decision Log

- Decision: Use `expressiveLightColorScheme()` directly as the light fallback rather than overriding with custom color values.
  Rationale: Google's official `MaterialExpressiveTheme` sample calls `expressiveLightColorScheme()` with no parameters. This function returns the full vibrant, tonal Expressive palette as the baseline. Hail's existing custom colors in `Color.kt` are standard M3 values that would need to be regenerated through the Material Theme Builder to become proper Expressive tokens. Starting with the built-in `expressiveLightColorScheme()` and then layering custom brand overrides (if needed) follows Google's recommended path.
  Date/Author: 2026-08-29

- Decision: Use `darkColorScheme()` for the dark fallback rather than generating a custom Expressive dark palette.
  Rationale: There is no `expressiveDarkColorScheme()` function in the Compose Material3 library. Google's own sample pairs `expressiveLightColorScheme()` with `darkColorScheme()` for dark mode. The existing Hail dark color values (`primaryDark`, `secondaryDark`, etc.) can be passed as overrides to `darkColorScheme()` to preserve brand colors while benefiting from the Expressive light scheme. If a fully Expressive dark scheme is desired later, the Material Theme Builder can generate the token values.
  Date/Author: 2026-08-29

- Decision: Adopt the InstallerX-Revived `ThemeColorSpec` pattern (SPEC_2021 vs SPEC_2025 toggle) as an optional enhancement beyond the base migration.
  Rationale: InstallerX-Revived exposes a `ThemeColorSpec` enum with `SPEC_2021` ("Material 3") and `SPEC_2025` ("Expressive") options, letting users explicitly choose between standard M3 and Expressive color specs via a settings dropdown. This maps directly to Google's "bidirectional" design — both specs coexist. For Hail, this would be a simple addition to `HailData.kt` (a new `APP_COLOR_SPEC` preference key + a dropdown in SettingsFragment). It is not required for the core Expressive migration, but it is a natural next step that follows Google's guidance ("update screens as you touch them"). The base migration can use `expressiveLightColorScheme()` unconditionally; the toggle is a later refinement.
  Date/Author: 2026-08-29

- Decision: Keep Hail's existing `SharedPreferences`-based theme preference system for now; defer the InstallerX-style `ThemeState` + DataStore centralization.
  Rationale: InstallerX-Revived centralizes all theme preferences into a reactive `ThemeState` data class backed by Jetpack DataStore. Hail currently reads `SharedPreferences` directly in `HailData.kt`. Migrating to DataStore is a significant architectural change that touches every preference in the app, not just theming. For this theme migration, the goal is the visual upgrade (Expressive color/motion/typography), not a preference-system overhaul. The `ThemeColorSpec` toggle can be added as a simple `SharedPreferences` string preference, consistent with Hail's existing `appTheme` pattern.
  Date/Author: 2026-08-29

- Decision: Use `expressiveLightColorScheme()` directly (no `materialkolor` library) for the initial migration, matching Google's official sample.
  Rationale: Google's `MaterialExpressiveTheme` sample calls `expressiveLightColorScheme()` with no parameters — it returns the full vibrant Expressive palette as the baseline. InstallerX-Revived uses `com.materialkolor:material-kolor:5.0.0` for *user-customizable* color generation (palette style + seed color picker), but that is an enhancement beyond Google's minimum. The `expressiveLightColorScheme()` function already provides the Expressive palette Hail needs. Adding `materialkolor` without a seed-color picker UI would be adding a dependency without using its features. If the `ThemeColorSpec` toggle is added later, the SPEC_2025 branch would call `expressiveLightColorScheme()` and the SPEC_2021 branch would call `lightColorScheme()` with Hail's existing custom colors from `Color.kt`.
  Date/Author: 2026-08-29

- Decision: For Views-based screens, keep the existing `Theme.Material3.DynamicColors.DayNight.NoActionBar` parent but update the static color tokens to Expressive palette values.
  Rationale: Material Components `com.google.android.material:material:1.14.0` already supports Expressive natively. The `DynamicColors` delegate automatically applies the Expressive color system on Android 12+ (the system's dynamic color extraction was upgraded for Android 16 with higher chroma and more hue variation). For static color fallback (Android 11 and below, or when dynamic color is disabled), the `md_theme_*` token values in `colors.xml` should be updated to Expressive palette values generated via the Material Theme Builder. No new Views theme parent is needed.
  Date/Author: 2026-08-29

- Decision: Use `MotionScheme.expressive()` as the default motion scheme.
  Rationale: Google recommends the expressive motion scheme "for prominent UI elements and hero interactions." It uses spatial springs (for shape/bounds changes) and effects springs (for color/opacity) to create more alive, fluid, natural motion. The API is `MotionScheme.expressive()` (not the older `expressiveMotionScheme()` name). The default `MaterialExpressiveTheme` uses `MotionScheme.standard()` internally if `motionScheme` is not provided — explicitly passing the expressive variant is required.
   Date/Author: 2026-08-29

- Decision: Adopt a gradual, incremental migration approach rather than a single large refactor.
  Rationale: Google's official guidance states: "If you have an existing app: work through the checklist [below], then update screens as you touch them for other reasons. Nothing in M3 Expressive is so urgent it warrants a dedicated migration sprint unless you're targeting Android 16 specifically." Hail is not Android-16-only-targeted at this time. The plan is structured so that changing the Compose theme wrapper (`AppTheme`) is a single-file change that affects all Compose screens at once, while Views screens can be migrated screen-by-screen as they are touched.
  Date/Author: 2026-08-29

- Decision: Define an `AppTypography` with bolder headline weights and emphasized variants.
  Rationale: The Expressive typography scale moves display and headline styles from `SemiBold` to `Bold`, and introduces "emphasized" variants (`displayLargeEmphasized`, `headlineLargeEmphasized`, etc.) that use higher font weights for stronger visual impact. The current `AppTypography = Typography()` uses default M3 token weights. Defining a custom `Typography` with `FontWeight.Bold` for display/headline styles and exposing emphasized variants where appropriate realizes the Expressive look.
  Date/Author: 2026-08-29

- Decision: Update Views corner radii to Expressive values (buttons 12dp→16dp, cards 4dp→20dp).
  Rationale: Google's migration checklist notes that M3 Expressive increased default corner radii — buttons moved from 12dp to 16dp, cards from 4dp to 20dp. While these are mostly handled by component defaults in the library, any custom `shapeAppearance` overrides or hardcoded `RoundedCornerShape` values in the codebase should be audited and updated to match the Expressive scale.
   Date/Author: 2026-08-29

## Outcomes & Retrospective

### Pre-implementation assessment (2026-08-29)

**Hail's current theming does not fully adhere to Google's M3 Expressive best practices.** The primary gaps are using `MaterialTheme` instead of `MaterialExpressiveTheme`, using custom color tokens instead of `expressiveLightColorScheme()`, and not providing an Expressive motion scheme, typography, or shapes. The migration defined in Milestones 2 through 4 brings Hail into full alignment with Google's recommendations.

The current Compose theme wrapper in `Theme.kt` line 101 calls `MaterialTheme` with a hand-built `lightColorScheme()` that uses 30+ color constants from `Color.kt` as overrides. Google's official `MaterialExpressiveTheme` sample calls `expressiveLightColorScheme()` with no parameters, which returns the full vibrant Expressive palette as the baseline. The `ThemeColorSpec` pattern from InstallerX-Revived demonstrates how this can be turned into a user-facing toggle: a `SPEC_2021` option that uses the existing custom `lightColorScheme()`, and a `SPEC_2025` option that uses `expressiveLightColorScheme()`. Both specs coexist, which maps directly to Google's bidirectional design.

Hail's Compose typography uses `val AppTypography = Typography()` (Type.kt line 5), which applies all default M3 token weights. Expressive typography moves display and headline styles from SemiBold to Bold and introduces emphasized variants (e.g., `displayLargeEmphasized`) that use Black font weight for stronger visual impact. No custom `Shapes` are defined either; Expressive expands the shape scale with `largeIncreased`, `extraLarge`, and `extraExtraLarge` corner radii (buttons 12dp to 16dp, cards 4dp to 20dp).

On the motion side, `AppTheme` does not pass a `motionScheme` parameter, so it uses the library default `MotionScheme.standard()`. Google recommends `MotionScheme.expressive()` for prominent UI elements and hero interactions, which uses spatial springs for shape and bounds changes and effects springs for color and opacity to create a more alive, fluid, natural motion.

The codebase audit confirmed that the migration is low-risk. There are approximately 12 `MaterialTheme.*` references in Compose code (across AboutFragment.kt, SettingsFragment.kt, ApiActivity.kt, and PagerFragment.kt), all standard usages of `colorScheme`, `typography`, and `shapes` that resolve identically under `MaterialExpressiveTheme`. There are zero `RoundedCornerShape` overrides in Compose, zero explicit `fontWeight` overrides, and zero custom `shapeAppearance` XML overrides, because Hail relies entirely on Material Components defaults. The dependencies already satisfy requirements: Compose BOM 2026.08.00 pins material3 to 1.4.0 (where Expressive APIs were promoted to stable) and Material Components 1.14.0 includes Expressive support for Views.

**Comparison with InstallerX-Revived.** InstallerX-Revived goes further than Google's minimum by integrating the `com.materialkolor:material-kolor:5.0.0` library for runtime color generation from a user-chosen seed color and palette style. It centralizes all theme preferences into a reactive `ThemeState` data class backed by Jetpack DataStore, and exposes a full theme settings page with dropdowns for ThemeMode, PaletteStyle, ThemeColorSpec, and a seed color picker. These are valuable enhancements but are not required for Google best-practices compliance. Hail can achieve full compliance with the simpler `MaterialExpressiveTheme` plus `expressiveLightColorScheme()` plus `MotionScheme.expressive()` approach defined in this plan. The materialkolor integration is documented as Milestone 7 for teams that want the full customization experience.

### Pre-implementation assessment outcome

The codebase audit confirmed that Hail's Compose theming is ready for migration with minimal risk: approximately 12 standard `MaterialTheme.*` references that resolve identically under `MaterialExpressiveTheme`, zero `RoundedCornerShape` overrides, zero explicit `fontWeight` overrides, and zero custom `shapeAppearance` XML overrides. Dependencies already satisfy requirements (Compose BOM 2026.08.00, Material Components 1.14.0).

The InstallerX-Revived reference implementation confirmed the `ThemeColorSpec` enum pattern (SPEC_2021 vs SPEC_2025) as a clean, minimal enhancement. The `materialkolor` library (version 5.0.0) path is documented as Milestone 7 for teams that want the full dynamic color generation experience, but is not needed for Google best-practices compliance.

### Implementation outcomes (pending)

_Will be recorded after Milestones 2 through 4 are implemented._

## Context and Orientation

Hail is an Android app written in Kotlin with package name `com.aistra.hail`. The app uses a hybrid UI strategy with two categories of screens. Views-based screens (the majority) use Fragments with View Binding, styled through XML themes and the Material Components for Views library (`com.google.android.material:material:1.14.0`). The app theme is defined in `app/src/main/res/values/themes.xml` with parent `Theme.Material3.DynamicColors.DayNight.NoActionBar` and color tokens in `app/src/main/res/values/colors.xml` (light) and `app/src/main/res/values-night/colors.xml` (dark). The theme selector lives in `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` using the `APP_THEME` preference key with `THEME_LIGHT`, `THEME_DARK`, and `FOLLOW_SYSTEM` constants, and is applied in `app/src/main/kotlin/com/aistra/hail/HailApp.kt` via the `setAppTheme` method, which sets `UiModeManager` on Android 12 and above or calls `AppCompatDelegate.setDefaultNightMode` on older versions.

Compose screens (the minority) include `AboutFragment`, `SettingsFragment`, the `ApiActivity`, and an inline tag-list composable called `TriStateTagList` in `PagerFragment`. These all go through the `AppTheme` composable wrapper defined in `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt`, which currently wraps `MaterialTheme` with `lightColorScheme` and `darkColorScheme` using color constants from `Color.kt`, and `AppTypography` which is a default `Typography()` from `Type.kt` (only 5 lines, no custom weights or emphasized variants).

The key theming files are: `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` (the Compose `AppTheme` wrapper with color scheme selection logic using `HTarget.S` for the Android 12+ check) and `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt` (the Compose `AppTypography`, currently a default `Typography()` with no custom tokens). Compose color constants live in `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt` (30+ light and dark palette tokens that mirror the Views `colors.xml`). The Views theme is in `app/src/main/res/values/themes.xml` (binding `md_theme_*` tokens), with light tokens in `app/src/main/res/values/colors.xml` and dark tokens in `app/src/main/res/values-night/colors.xml`.

The build uses Compose BOM `2026.08.00`, which pins `androidx.compose.material3:material3` to 1.4.0. Material 3 Expressive APIs were promoted from experimental to stable in material3 1.4.0, so `MaterialExpressiveTheme`, `expressiveLightColorScheme()`, `MotionScheme.expressive()`, the `Shapes` class with its eight Expressive shape roles, and the emphasized `Typography` variants are all available. No dependency upgrade is needed. Material Components 1.14.0 includes Expressive support for Views through the `DynamicColors` delegate, which applies the Expressive color system on Android 12 and above.

A `ColorScheme` is a data class holding all the color values a Material theme needs (primary, secondary, tertiary, surface, background, error, and their on-colors, plus outline and container variants). `MotionScheme` is an interface providing animation specifications (durations, easings, spring parameters) for a Material theme. The `Shapes` class defines eight corner-based shape roles: `extraSmall`, `small`, `medium`, `large`, `extraLarge`, `largeIncreased`, `extraLargeIncreased`, and `extraExtraLarge`. `RoundedCornerShape` is a Compose `CornerBasedShape` implementation that applies the same corner radius to all four corners. `Typography` is a class holding `TextStyle` objects for each text style in the Material type scale; emphasized variants (e.g., `displayLargeEmphasized`) use higher font weights for stronger visual impact. `HTarget` is an object in `app/src/main/kotlin/com/aistra/hail/utils/HTarget.kt` that provides version-check properties like `S` (Android 12+, API 31+) and `T` (Android 13+, API 33+).

## Plan of Work

The work is broken into milestones, each independently verifiable and incrementally building toward the full feature. The milestones are ordered so that each produces a working, testable state before the next begins.

### Milestone 1: Pre-Migration Audit and Checklist

This milestone ensures the codebase is ready for the Expressive migration. Begin by confirming that Compose BOM 2026.08.00 (which pins material3 to 1.4.0) and Material Components 1.14.0 are declared in `gradle/libs.versions.toml` at the repository root. Both versions include Expressive APIs that have been promoted from experimental to stable, so no dependency upgrade is needed. Audit all `MaterialTheme.` references in Compose code (at `app/src/main/kotlin/com/aistra/hail/ui/`) — these continue to work under `MaterialExpressiveTheme` because the `MaterialTheme` accessor reads from the nearest enclosing `MaterialExpressiveTheme` composition local, so the same colorScheme, typography, and shapes values are available. Audit all hardcoded `RoundedCornerShape` values in Compose and `shapeAppearance` overrides in XML, comparing them against Expressive values (buttons at 16dp, cards at 20dp, FAB at 28dp or more). Audit all `Text` usages with explicit `fontWeight`, since Expressive moves display and headline styles to Bold. Document the Google migration checklist of breaking changes: spacing increases, touch target minimums, animation timing changes, corner radius changes, typography weight changes, color role shifts, and dark theme contrast adjustments. At the end of this milestone, the implementer knows exactly what needs to change and what breaking changes to watch for.

The audit findings, recorded on 2026-08-29, confirmed the following. A grep for `MaterialTheme.` references in Compose code (excluding the `ui/theme` package) found 10 matches across `AboutFragment.kt` (5), `ApiActivity.kt` (2), and `PagerFragment.kt` (3), all using standard `colorScheme` and `typography` accessors that resolve identically under `MaterialExpressiveTheme`. A grep for `RoundedCornerShape` in Compose found 0 matches — shapes are handled entirely by library defaults with no custom overrides. A grep for `shapeAppearance` or `cornerSize` in XML found 0 matches — Views screens use Material Components defaults with no custom shape overrides. A grep for `fontWeight` in Compose found 0 matches — typography is fully default with no weight overrides. The Compose `AppTheme` composable (defined at `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` line 86) is called from 6 locations across 4 screens: `AboutFragment.kt` (2 calls, including a preview), `SettingsFragment.kt` (1 call), `ApiActivity.kt` (2 calls), and `PagerFragment.kt` (1 call). Additionally, `HailApp.kt` (lines 33 and 64) defines and calls `setAppTheme`, a Views-based theme setter that is separate from the Compose `AppTheme`.

**Verification:** Run the grep commands `grep -rn MaterialTheme. app/src/main/kotlin --include=*.kt | grep -v ui/theme`, `grep -rn RoundedCornerShape app/src/main/kotlin --include=*.kt`, `grep -rn shapeAppearance app/src/main/res --include=*.xml`, and `grep -rn fontWeight app/src/main/kotlin --include=*.kt`. Then run `./gradlew :app:compileDebugKotlin` and confirm the current code compiles.

### Milestone 2: Migrate the Compose Theme to MaterialExpressiveTheme

This milestone replaces `MaterialTheme` with `MaterialExpressiveTheme` in the Compose theme wrapper at `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` line 101. The replacement call uses `expressiveLightColorScheme()` as the light color scheme with no custom overrides, which uses the built-in vibrant Expressive palette. The dark fallback uses `darkColorScheme()` with the existing custom dark color values from `Color.kt`, preserving Hail's brand colors in dark mode. Dynamic color on Android 12+ is preserved via `dynamicLightColorScheme` and `dynamicDarkColorScheme` from `LocalContext.current`. The `MotionScheme.expressive()` static method is added as the `motionScheme` parameter to enable expressive motion with spatial and effects springs. Expressive shapes are defined as a `Shapes` object with `extraSmall` at 4dp, `small` at 8dp, `medium` at 16dp, `large` at 20dp, `extraLarge` at 28dp, `largeIncreased` at 34dp, `extraLargeIncreased` at 40dp, and `extraExtraLarge` at 48dp. In `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt`, the default `Typography()` is replaced with a custom `AppTypography` using Expressive font weights: Bold for display and headline styles, SemiBold for title, label, and body styles, and Black for the emphasized variants (displayLargeEmphasized, displayMediumEmphasized, displaySmallEmphasized, headlineLargeEmphasized, headlineMediumEmphasized, headlineSmallEmphasized, titleLargeEmphasized, titleMediumEmphasized, titleSmallEmphasized, bodyLargeEmphasized, bodyMediumEmphasized, bodySmallEmphasized, labelLargeEmphasized, labelMediumEmphasized, labelSmallEmphasized).

At the end of this milestone, all Compose screens (About, Settings, API, tag list) render with the Expressive theme. The `AppTheme` function signature is unchanged so all call sites compile without modification. All existing `MaterialTheme.*` references in Compose code still resolve because they read from the `MaterialExpressiveTheme` local. Google's recommendation to switch from `MaterialTheme` to `MaterialExpressiveTheme` and use `expressiveLightColorScheme()` is applied here. This is a single wrapper swap plus adding the expressive motion scheme and shapes, after which all existing Compose code using `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and similar references continues to work unchanged.

**Verification:** Run `./gradlew :app:compileDebugKotlin` and confirm no errors. Run `./gradlew :app:processDebugResources` and confirm success (this validates resources including navigation XML). Run `./gradlew :app:assembleDebug` and confirm the build reports `BUILD SUCCESSFUL`.

### Milestone 3: Update Views-Based Color Tokens and Shapes to Expressive

This milestone updates the Views-based color tokens to align with the Expressive palette. The color token files at `app/src/main/res/values/colors.xml` (light) and `app/src/main/res/values-night/colors.xml` (dark) are updated so that the `md_theme_*` values match Expressive palette values. The Material Theme Builder at m3.material.io/theme-builder generates Expressive color tokens from Hail's existing seed color (`#32628D` for primary or `#81D4FA` for accent), and the generated values replace the existing `md_theme_*` tokens. The `expressiveLightColorScheme()` Compose function provides a reference palette that the Views tokens should match or be harmonized with. In `app/src/main/res/values/themes.xml`, there is no structural change to the theme inheritance — `Theme.Material3.DynamicColors.DayNight.NoActionBar` already supports Expressive on Material Components 1.14.0. Only the token values change via `colors.xml`. If custom `shapeAppearance` styles are used, corner radii are updated to Expressive values (16dp for buttons, 20dp for cards, 28dp or more for prominent surfaces). In `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt`, the Compose color constants are updated to match the new Expressive palette values so Compose and Views stay in sync. If `expressiveLightColorScheme()` is used directly for the light scheme, the light color constants in `Color.kt` may become unused, so verify all references and remove dead code.

At the end of this milestone, both Views-based and Compose-based screens use Expressive-aligned color tokens. The app looks more vibrant and emotionally engaging, consistent with the Android 16 visual language. Dynamic color on Android 12+ continues to override the static tokens with wallpaper-derived palettes. Google's recommendation to use the full Material scheme with a variety of primary, secondary, and tertiary accent colors for hierarchy and distinction, and tonal surface colors within thoughtfully grouped and contained content, is applied. Material Components 1.14.0 supports Expressive natively, so the Views theme parent does not need to change — only the static token values.

**Verification:** Run `./gradlew :app:assembleDebug` and inspect the APK's color resources. Confirm the build reports `BUILD SUCCESSFUL`. Perform a visual review on a device or emulator.

### Milestone 4: Audit and Update Expressive-Aware Components

This milestone applies Google's migration checklist to Hail's actual codebase. The corner radii audit searches for `RoundedCornerShape` in Compose and `shapeAppearance` in XML, updating any values that do not match the Expressive scale (buttons at 16dp, cards at 20dp, FAB at 28dp or more, dialogs at 28dp or more). The typography audit searches for `Text` usages with explicit `fontWeight` or `style` overrides, updating display and headline weight overrides to Bold to match Expressive, and using emphasized typography variants such as `displayLargeEmphasized` where drawing attention is intended. The spacing audit reviews component internal padding in layouts, noting that Expressive increased default internal padding from 8dp to 12dp in several components and verifying that lists, buttons, and input fields are not cramped. The touch target audit verifies all clickable elements are at least 48dp, since Expressive enforces this more consistently and accessibility warnings will appear for undersized targets. The animation audit checks for `animate*`, `updateTransition`, or `AnimatedVisibility` with custom durations, noting that Expressive durations are about 15 percent longer and verifying that timing-sensitive interactions such as auto-dismissing snackbars still feel correct.

At the end of this milestone, all hardcoded shapes, typography weights, spacing, touch targets, and animations in the codebase align with Expressive guidelines. No accessibility warnings for undersized touch targets.

**Verification:** Run `./gradlew :app:compileDebugKotlin :app:processDebugResources` and confirm `BUILD SUCCESSFUL`. Run lint checks with `./gradlew :app:lintDebug`.

### Milestone 5: Build Validation and Visual Review

This milestone verifies the full build compiles, resources are valid, and the Expressive theme renders correctly across all screens.

At the end of this milestone, the debug APK builds successfully, the Expressive theme renders on all screens without visual artifacts or missing color roles, and all existing `MaterialTheme.*` references in Compose code resolve correctly through `MaterialExpressiveTheme`.

**Verification:** Run `./gradlew :app:assembleDebug` and expect `BUILD SUCCESSFUL`. Run `./gradlew :app:processDebugResources` and expect success (this validates navigation XML, themes, and colors). Run `./gradlew :app:compileDebugKotlin` and expect success. If an Android 16 or higher device or emulator is available, perform a visual review to confirm the Expressive theme renders correctly; also test on Android 11 or below to confirm fallback palettes work.

### Milestone 6: Add ThemeColorSpec Preference Toggle (Optional Enhancement)

This milestone adds a user-facing option to choose between Material 3 (2021) and Expressive (2025) color specs, following the InstallerX-Revived `ThemeColorSpec` pattern. InstallerX-Revived exposes the full enum (SPEC_2021, SPEC_2025), but Hail can offer a simpler binary toggle labeled "Material 3 Expressive" that maps directly to the two specs: on uses `expressiveLightColorScheme()`, off uses the existing custom `lightColorScheme()`. This reduces the settings complexity from a multi-option enum to a single checkbox while preserving the same underlying behavior. In `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`, a new `APP_COLOR_SPEC` preference key is added with `SPEC_2021` and `SPEC_2025` constants and a default of `SPEC_2025`. A new `appColorSpec` getter reads the preference. In `Theme.kt`, the Compose light color scheme branches on this preference: if `SPEC_2021`, the existing custom `lightColorScheme()` with `Color.kt` tokens is used; if `SPEC_2025`, `expressiveLightColorScheme()` is used. A `ListPreference` is added to the `SettingsFragment` and its settings XML for color spec selection. The `strings.xml` file and all maintained translation files get new `theme_color_spec`, `spec_2021`, and `spec_2025` labels.

At the end of this milestone, users can toggle between standard Material 3 and Expressive color specs in Settings. The change is a single preference read plus a conditional in `AppTheme`.

**Verification:** Run `./gradlew :app:assembleDebug` and confirm `BUILD SUCCESSFUL`. Visual review confirms the color spec changes when toggled.

### Milestone 7: Add Seed Color Picker and PaletteStyle (Advanced Enhancement)

This milestone follows the InstallerX-Revived pattern of using `com.materialkolor:material-kolor:5.0.0` for runtime color generation from a user-chosen seed color and palette style, with version 5.0.0 registered in `gradle/libs.versions.toml` as `materialKolor = "5.0.0"`. A new `ThemeColorSpec` enum with `SPEC_2021` and `SPEC_2025` entries and a `displayName` field is created. A `PaletteStyle` enum covering TonalSpot, Neutral, Vibrant, Expressive, Rainbow, FruitSalad, Monochrome, Fidelity, and Content is created alongside it. In `Theme.kt`, the `expressiveLightColorScheme()` and `darkColorScheme()` calls are replaced with materialkolor-generated schemes based on a user-chosen seed color, palette style, and color spec. The settings UI gains a seed color picker (preset color grid), a palette style dropdown, and a color spec dropdown. This milestone follows the InstallerX-Revived pattern and is not required for Google best-practices compliance.

At the end of this milestone, users can pick a seed color and palette style to generate fully customized Expressive color schemes at runtime.

**Verification:** Run `./gradlew :app:assembleDebug` and confirm `BUILD SUCCESSFUL`. Visual review confirms color changes when seed, palette style, or color spec are modified. This is a significant addition beyond the base migration; the base milestones 2 through 4 achieve Google best-practices compliance without it, so this milestone should only be pursued if the user explicitly requests the enhanced theme customization experience.

## Concrete Steps

All commands assume the working directory is the repository root (`/workspaces/Hail`).

### Step 1: Verify dependencies are Expressive-capable

    grep "composeBom\|material3\|material" gradle/libs.versions.toml

Expected output includes:

    composeBom = "2026.08.00"
    material = "1.14.0"

The Compose BOM 2026.08.00 pins `androidx.compose.material3:material3` to 1.4.0, which includes all Expressive APIs (promoted from experimental to stable). Material Components 1.14.0 supports Expressive for Views. No dependency changes are needed.

### Step 2: Audit existing theming references

    grep -rn "MaterialTheme\." app/src/main/kotlin --include="*.kt" | head -30
    grep -rn "RoundedCornerShape" app/src/main/kotlin --include="*.kt" | head -20
    grep -rn "shapeAppearance" app/src/main/res --include="*.xml" | head -10
    grep -rn "fontWeight" app/src/main/kotlin --include="*.kt" | head -20

Record the results in the Progress and Surprises and Discoveries sections.

### Step 3: Update Type.kt with Expressive typography

File: `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt`

Replace the default `Typography()` with a custom `AppTypography` using Expressive font weights (Bold for display and headline, SemiBold for title, label, and body) and sizes, including emphasized variants (Black weight for emphasized display and headline styles, Bold for emphasized title styles). Expressive moves display and headline styles from SemiBold to Bold and introduces emphasized variants for every text style that use higher font weights for stronger visual impact.

    package com.aistra.hail.ui.theme
    
    import androidx.compose.material3.Typography
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.unit.sp
    
    val AppTypography = Typography(
        displayLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayLargeEmphasized = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 57.sp,
            lineHeight = 64.sp,
            letterSpacing = (-0.25).sp
        ),
        displayMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.15).sp
        ),
        displayMediumEmphasized = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 45.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.15).sp
        ),
        displaySmall = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        displaySmallEmphasized = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 36.sp,
            lineHeight = 44.sp,
            letterSpacing = 0.sp
        ),
        headlineLarge = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineLargeEmphasized = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 32.sp,
            lineHeight = 40.sp,
            letterSpacing = 0.sp
        ),
        headlineMedium = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineMediumEmphasized = TextStyle(
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            lineHeight = 36.sp,
            letterSpacing = 0.sp
        ),
        headlineSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        headlineSmallEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
            letterSpacing = 0.sp
        ),
        titleLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleLargeEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
            letterSpacing = 0.sp
        ),
        titleMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleMediumEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        titleSmallEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLarge = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyLargeEmphasized = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMedium = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodyMediumEmphasized = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            lineHeight = 24.sp,
            letterSpacing = 0.15.sp
        ),
        bodySmall = TextStyle(
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        bodySmallEmphasized = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.25.sp
        ),
        labelLarge = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        labelLargeEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.15.sp
        ),
        labelMedium = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelMediumEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.25.sp
        ),
        labelSmallEmphasized = TextStyle(
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.25.sp
        ),
    )

### Step 4: Update Theme.kt to use MaterialExpressiveTheme

File: `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt`

Replace the `MaterialTheme` call with `MaterialExpressiveTheme`, add the expressive motion scheme and shapes, and use `expressiveLightColorScheme()` for the light fallback:

    package com.aistra.hail.ui.theme
    
    import androidx.compose.foundation.isSystemInDarkTheme
    import androidx.compose.material3.*
    import androidx.compose.runtime.Composable
    import androidx.compose.ui.platform.LocalContext
    import com.aistra.hail.utils.HTarget
    
    @Composable
    fun AppTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        dynamicColor: Boolean = true, content: @Composable() () -> Unit
    ) {
        val colorScheme = when {
            dynamicColor && HTarget.S -> {
                val context = LocalContext.current
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            }
            darkTheme -> darkScheme
            else -> expressiveLightColorScheme()
        }
    
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            shapes = expressiveShapes,
            typography = AppTypography,
            content = content
        )
    }

Define the expressive shapes using only valid constructor parameters:

    private val expressiveShapes = Shapes(
        extraSmall = RoundedCornerShape(4.0.dp),
        small = RoundedCornerShape(8.0.dp),
        medium = RoundedCornerShape(16.0.dp),
        large = RoundedCornerShape(20.0.dp),
        extraLarge = RoundedCornerShape(28.0.dp),
        largeIncreased = RoundedCornerShape(34.0.dp),
        extraLargeIncreased = RoundedCornerShape(40.0.dp),
        extraExtraLarge = RoundedCornerShape(48.0.dp)
    )

The `darkScheme` is still built from the existing `darkColorScheme(...)` call with custom color values from `Color.kt`. The `lightScheme` variable is no longer needed (replaced by `expressiveLightColorScheme()`). Remove the unused `lightScheme` and its import of `RoundedCornerShape` if not used elsewhere.

### Step 5: Verify Compose references still resolve

The existing Compose code uses `MaterialTheme.colorScheme` and `MaterialTheme.typography`. Under `MaterialExpressiveTheme`, these accessors resolve to the values provided. Confirm with:

    grep -rn "MaterialTheme\." app/src/main/kotlin --include="*.kt" | grep -v "ui/theme"

These references should compile without changes.

### Step 6: Update Views color tokens (optional, can be deferred)

Use the Material Theme Builder at m3.material.io/theme-builder to generate Expressive color tokens from Hail's seed color. Export the generated `colors.xml` values and replace the existing `md_theme_*` values in `app/src/main/res/values/colors.xml` (light) and `app/src/main/res/values-night/colors.xml` (dark). Also update `Color.kt` Compose constants to match.

### Step 7: Update corner radii in Compose and XML

    grep -rn "RoundedCornerShape" app/src/main/kotlin --include="*.kt"
    grep -rn "shapeAppearance\|cornerSize" app/src/main/res --include="*.xml"

Update values to Expressive defaults where needed: buttons at 16dp, cards at 20dp, FAB at 28dp or more, dialogs at 28dp or more.

### Step 8: Build and validate

    ./gradlew :app:compileDebugKotlin :app:processDebugResources

Expect `BUILD SUCCESSFUL`.

    ./gradlew :app:assembleDebug

Expect `BUILD SUCCESSFUL`.

## Validation and Acceptance

After completing the milestones, build the debug APK with `./gradlew :app:assembleDebug` and expect `BUILD SUCCESSFUL`.

Acceptance is verified through observable behavior. After implementation, the Compose `AppTheme` wrapper must use `MaterialExpressiveTheme` with `MotionScheme.expressive()`, Expressive shapes, and Expressive typography, with `expressiveLightColorScheme()` as the light color scheme and `darkColorScheme()` for dark. All existing Compose code referencing `MaterialTheme.colorScheme`, `MaterialTheme.typography`, or `MaterialTheme.shapes` must continue to compile and resolve correctly, because `MaterialExpressiveTheme` populates the same composition locals. Dynamic color on Android 12+ must be preserved through `dynamicLightColorScheme` and `dynamicDarkColorScheme`. On Android 11 and below, the app must fall back to `expressiveLightColorScheme()` for light and `darkColorScheme()` for dark. The Views-based color tokens in `colors.xml` and `values-night/colors.xml` should align with the Expressive palette. Corner radii should match the Expressive scale (buttons at 16dp, cards at 20dp). Typography must use Expressive weights (Bold for display and headline styles, with emphasized variants defined). Visual review should confirm more vibrant colors and bolder typography. The existing light/dark theme toggle in `HailData.APP_THEME` and the dynamic color setting must continue to work unchanged. No new lint warnings for undersized touch targets or missing color roles.

## Idempotence and Recovery

All steps are idempotent — re-running the file edits or build commands produces the same result. If the build fails after a theme change, revert the last edit to `Theme.kt`, `Type.kt`, or `Color.kt` and run `./gradlew :app:compileDebugKotlin` to confirm the rollback restored a compilable state. Then re-apply the change incrementally, compiling after each file. Room is not affected by this change; no database migrations are needed. The Compose BOM `2026.08.00` already provides material3 1.4.0 with Expressive APIs, so no dependency upgrade is required. The `lightScheme` variable in `Theme.kt` becomes unused after the migration, so remove it to avoid warnings. Color token changes in `colors.xml` are safe to retry: the values can be regenerated from the Material Theme Builder and re-applied. If a color looks wrong, revert to the previous `md_theme_*` hex value and regenerate.

## Artifacts and Notes

The Material Theme Builder at m3.material.io/theme-builder generates Expressive color tokens from seed colors with code export. This is an external tool, not a library; the generated token values are copied manually into `colors.xml`.

The `expressiveLightColorScheme()` function takes no parameters and returns a `ColorScheme` with vibrant, tonal Expressive palette values. It is the default light color scheme for `MaterialExpressiveTheme`. For dark mode, use `darkColorScheme()` — there is no `expressiveDarkColorScheme()` function. To customize dark Expressive colors, pass overrides to `darkColorScheme()` or generate tokens via the Material Theme Builder.

The `MotionScheme` interface provides motion specifications (animation durations, easings, springs) for a Material theme. `MotionScheme.expressive()` returns the expressive motion scheme, recommended for prominent UI elements and hero interactions. `MotionScheme.standard()` returns the standard scheme for utilitarian elements. The API was renamed from `expressiveMotionScheme()` to `MotionScheme.expressive()` and from `standardMotionScheme()` to `MotionScheme.standard()`. Use the current names.

The `Shapes` class defines eight corner-based shape roles: `extraSmall`, `small`, `medium`, `large`, `extraLarge`, `largeIncreased`, `extraLargeIncreased`, and `extraExtraLarge`. The `largeIncreased` is a slightly larger variant of `large`, `extraLargeIncreased` is slightly larger than `extraLarge`, and `extraExtraLarge` is the largest. Each role maps to components: buttons use `medium` (16dp in Expressive), cards use `large` (20dp in Expressive), FABs use `extraLarge` or `extraExtraLarge`.

The `Typography` class defines text styles for the Material type scale, including standard variants (display, headline, title, body, label) and emphasized variants (displayLargeEmphasized, headlineLargeEmphasized, etc.). Emphasized variants use higher font weights (typically Black for display and headline emphasized styles, Bold for title emphasized styles). Expressive typography moves display and headline styles from SemiBold to Bold and introduces these emphasized variants.

The Compose BOM 2026.08.00 maps to material3 1.4.0 (stable), which includes the Expressive APIs. No version bump is needed — `libs.androidx.material3` is already dependency-free (version managed by the BOM). Material Components 1.14.0 includes Expressive support for Views via the `DynamicColors` delegate, which automatically applies the Expressive color system on Android 12 and above.

Google's official migration guidance states: "If you have an existing app: work through the checklist above, then update screens as you touch them for other reasons. Nothing in M3 Expressive is so urgent it warrants a dedicated migration sprint unless you're targeting Android 16 specifically." This plan follows that gradual approach.

Google's official m3 Expressive migration checklist includes the following breaking changes to watch: component internal padding increased from 8dp to 12dp in several components; anything interactive under 48dp height triggers accessibility warnings; animation durations are about 15 percent longer so time-sensitive interactions should be verified; corner radii moved from buttons at 12dp to 16dp and cards from 4dp to 20dp; headline styles moved from SemiBold to Bold; the `surfaceVariant` and `onSurfaceVariant` color roles have shifted in the Expressive palette; and dark theme color roles have slightly different contrast ratios.

## Interfaces and Dependencies

The migration requires specific classes, functions, and signatures at defined file paths.

In `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt`, the `AppTheme` composable wrapper has the signature `fun AppTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = true, content: @Composable () -> Unit)`. Internally, it uses `MaterialExpressiveTheme(colorScheme, motionScheme, shapes, typography, content)` from `androidx.compose.material3`, which accepts nullable `colorScheme`, `motionScheme`, `shapes`, and `typography` parameters plus a required `content` lambda. The light fallback uses `expressiveLightColorScheme()` (no parameters, returns a `ColorScheme` with the full vibrant Expressive palette). The dark fallback uses `darkColorScheme()` with custom color overrides from `Color.kt`. Dynamic color on Android 12 and above uses `dynamicLightColorScheme(context)` and `dynamicDarkColorScheme(context)`. The motion scheme uses `MotionScheme.expressive()`. The shapes use `Shapes(...)` with the eight valid Expressive shape roles. The typography uses `AppTypography` from `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt`.

In `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt`, the `AppTypography` value is a `Typography` with Expressive font weights (Bold for display and headline styles, SemiBold for title, label, and body styles) and all emphasized variants defined (displayLargeEmphasized through labelSmallEmphasized).

In `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt`, the color constants (`primaryLight`, `onPrimaryLight`, `primaryDark`, `onPrimaryDark`, and all other light and dark palette tokens) are used as overrides in `darkColorScheme()`. The light scheme uses `expressiveLightColorScheme()` directly, so the light color constants may become unused after the migration and should be checked for dead code.

In `app/src/main/res/values/colors.xml` and `app/src/main/res/values-night/colors.xml`, the `md_theme_*` color tokens are updated to Expressive palette values generated via the Material Theme Builder.

In `app/src/main/res/values/themes.xml`, there is no structural change — the theme parent `Theme.Material3.DynamicColors.DayNight.NoActionBar` remains. Only the `md_theme_*` color token values change via `colors.xml`. This theme parent is from Material Components `com.google.android.material:material:1.14.0`, which natively supports Expressive on Android 12 and above through the `DynamicColors` delegate.

The migration depends on two external libraries. `androidx.compose.material3:material3` (via Compose BOM `2026.08.00`, pinned to 1.4.0) provides `MaterialExpressiveTheme`, `expressiveLightColorScheme`, `MotionScheme.expressive()`, the `Shapes` class with Expressive shape roles, and the emphasized `Typography` variants. All of these were promoted from experimental to stable in version 1.4.0. `com.google.android.material:material:1.14.0` provides Views-based Material Components with Expressive support (color extraction and component defaults). For the optional Milestone 7, `com.materialkolor:material-kolor:5.0.0` provides runtime color generation from a seed color and palette style, enabling user-customizable Expressive color schemes.

The files touched for the base migration (Milestones 2 through 5) are: `app/src/main/kotlin/com/aistra/hail/ui/theme/Theme.kt` (Compose theme wrapper, `MaterialTheme` to `MaterialExpressiveTheme`), `app/src/main/kotlin/com/aistra/hail/ui/theme/Type.kt` (Expressive typography), `app/src/main/kotlin/com/aistra/hail/ui/theme/Color.kt` (color constants, possibly simplified), `app/src/main/res/values/colors.xml` (Views light color tokens), `app/src/main/res/values-night/colors.xml` (Views dark color tokens), and `app/src/main/res/values/themes.xml` (Views theme, only token values change). The audit found zero `RoundedCornerShape` overrides in Compose and zero custom `shapeAppearance` styles in XML, so no other files are needed for the base migration. The optional Milestone 6 extends `app/src/main/kotlin/com/aistra/hail/app/HailData.kt` with the `APP_COLOR_SPEC` preference, `app/src/main/res/values/strings.xml` and all translation files with labels, and `SettingsFragment` with a `ListPreference`. The optional Milestone 7 adds the `materialkolor` dependency to the app module's build file and creates new enum files for `ThemeColorSpec` and `PaletteStyle`.

---

**Revision note (2026-08-29):** Verified all Expressive API signatures against the official Android Developer reference and the Compose Material3 release notes. Fixed a bug in the Shapes definition where `mediumIncreased` and `smallIncreased` parameters were included — these do not exist in the Shapes constructor, which only defines `extraSmall`, `small`, `medium`, `large`, `extraLarge`, `largeIncreased`, `extraLargeIncreased`, and `extraExtraLarge`. Corrected the Expressive shape dp values (medium at 16dp for buttons, large at 20dp for cards, matching Google's migration guidance that buttons moved from 12dp to 16dp and cards from 4dp to 20dp). Converted all numbered acceptance criteria and numbered recovery steps to prose per the OpenAI ExecPlan format. Converted milestone "What it adds or changes" bullet lists to prose paragraphs. Removed orphaned headers left by prior edits. Corrected the audit findings with actual grep results (10 MaterialTheme references, 0 RoundedCornerShape, 0 shapeAppearance, 0 fontWeight, 6 Compose AppTheme call sites across 4 screens). Fixed the materialkolor Maven coordinate to the correct value. Removed duplicate Decision Log entry about Views-based screens. Converted the Context and Orientation section from bullet lists to prose narrative. Defined all terms of art (ColorScheme, MotionScheme, Shapes, Typography, emphasized variants, RoundedCornerShape, HTarget) in plain language within the plan. Converted bullet lists in the Artifacts and Interfaces sections to prose. Removed external URL references, replacing them with embedded explanations. Converted all triple-backtick code blocks to indented blocks per the OpenAI ExecPlan format requirement for .md files. Reduced the plan from 757 lines to 365 lines while adding more technical precision.
