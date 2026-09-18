# Hail App Jetpack Compose Migration Master Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a single source of truth for Compose migration tracking all phases, dependencies, and validation criteria.

**Architecture:** Centralized plan that references all detailed phase plans, ensuring consistency across the migration effort. Tracks dependency versions, validation gates, and open issues throughout the migration process.

**Tech Stack:**
- Same as individual phase plans (Compose BOM 2026.08.00, Material3 1.4.0, etc.)

**Spec:** Consolidates information from all individual phase plans and dependency resolution results.

## Dependency Versions (Resolved September 2026)

After validating with Context7 and web search:

```toml
# gradle/libs.versions.toml
[versions]
composeBom = "2026.08.00"
composeCompiler = "1.6.8"
navigation = "2.10.0"
lifecycle = "2.8.2"
room = "2.6.0"
kotlin = "2.4.20"
agp = "9.4.0"
compileSdk = "37"
material3 = "1.4.0"
material3WindowSizeClass = "1.2.0"
preference = "2.3.0"
coil = "2.6.0"
coilCompose = "2.6.0"
accompanistSystemUi = "0.37.2"
accompanistMaterial3HorizPager = "0.37.2"
coroutines = "1.8.0"
coroutinesCore = "1.8.0"
```

**Notes:**
- Navigation remains at 2.10.0 (Fragment-based, XML nav graph kept)
- Accompanist Material3 HorizontalPager 0.37.2 used (consider migrating to androidx.compose.foundation:pager when stable)
- Material3 1.4.0 is stable (not Expressive)
- Compose BOM 2026.08.00 is the latest as of September 2026
- Coil 2.6.0 chosen for better Compose integration over Glide

## Migration Phases

See individual plan files for detailed breakdown:

1. **00-main-plan.md** - Overall migration strategy and constraints
2. **01-appicon-theme-plan.md** - Infrastructure: Coil + theming + AppIcon
3. **02-apps-fragment-plan.md** - AppsFragment migration (LazyVerticalGrid)
4. **03-actions-fragment-plan.md** - ActionsFragment migration (LazyColumn)
5. **04-pager-fragment-plan.md** - PagerFragment migration (tab pages)
6. **05-home-fragment-plan.md** - HomeFragment migration (TabRow + HorizontalPager)
7. **06-api-activity-plan.md** - ApiActivity migration (translucent)
8. **07-testing-ci-plan.md** - Testing and CI setup
9. **08-resource-viewmodel-plan.md** - Resources and ViewModel StateFlow conversion

## Validation Gates

Each phase must pass:
- [ ] Local build: `./gradlew assembleDebug`
- [ ] Unit tests: `./gradlew test`
- [ ] Lint: `./gradlew lint`
- [ ] Manual QA on device/emulator
- [ ] Screen-specific validation (see individual plans)

## Open Issues

- [ ] Pager migration complexity (very high)
- [ ] HorizontalPager stability vs foundation:pager
- [ ] State persistence verification
- [ ] Performance benchmarks
- [ ] Accessibility compliance

## References

- [Compose BOM Versions](https://developer.android.com/jetpack/androidx/releases/compose-bom)
- [Material3 Stability](https://developer.android.com/jetpack/compose/material3/migration)
- [Accompanist Status](https://github.com/google/accompanist#maven)