# Hail App Jetpack Compose Migration Master Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Provide a single source of truth for Compose migration tracking all phases, dependencies, and validation criteria.

**Architecture:** Centralized plan that references all detailed phase plans, ensuring consistency across the migration effort. Tracks dependency versions, validation gates, and open issues throughout the migration process.

**Tech Stack:**
- Same as individual phase plans (Compose BOM 2026.08.00, Material3 1.4.0, etc.)

**Spec:** Consolidates information from all individual phase plans and dependency resolution results.

## Global Constraints
- Navigation remains at 2.10.1 (Fragment-based, XML nav graph kept)
- androidx.compose.foundation:pager used (part of Compose BOM)
- Material3 1.4.0 is stable (not Expressive)
- Compose BOM 2026.09.00 is the latest as of September 2026
- Coil 2.6.0 chosen for better Compose integration over Glide

---
## Tasks

### Task 1: Review Dependency Versions
**Files:**
- Read: `docs/superpowers/plans/compose-migration/00-master-plan.md` (this file)

**Steps:**
- [ ] Review the resolved dependency versions for the migration
- [ ] Verify these versions are compatible with each other
- [ ] Confirm they meet the project's Global Constraints

### Task 2: Understand Migration Phases
**Files:**
- Read: All 0X-*.md plan files in this directory

**Steps:**
- [ ] Familiarize yourself with the 9 migration phases
- [ ] Understand what each phase accomplishes
- [ ] Note the validation gates for each phase

## Dependency Versions (Resolved September 2026)

After validating with Context7 and web search:

```toml
# gradle/libs.versions.toml
[versions]
composeBom = "2026.09.00"
composeCompiler = "1.6.8"
navigation = "2.10.1"
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
coroutines = "1.8.0"
coroutinesCore = "1.8.0"
```

## Migration Phases

Each phase corresponds to a detailed plan file:

1. **00-main-plan.md** - Overall migration strategy and constraints
2. **01-appicon-theme-plan.md** - Infrastructure: Coil + theming + AppIcon
3. **02-apps-fragment-plan.md** - AppsFragment migration (LazyVerticalGrid)
4. **03-actions-fragment-plan.md** - ActionsFragment migration (LazyColumn)
5. **04-pager-fragment-plan.md** - PagerFragment migration (tab pages using foundation pager)
6. **05-home-fragment-plan.md** - HomeFragment migration (TabRow + foundation pager)
7. **06-api-activity-plan.md** - ApiActivity migration (translucent)
8. **07-testing-ci-plan.md** - Testing and CI setup
9. **08-resource-viewmodel-plan.md** - Resources and ViewModel StateFlow conversion

## Validation Checklist

Each phase must pass:
- [ ] Local build: `./gradlew assembleDebug`
- [ ] Unit tests: `./gradlew test`
- [ ] Lint: `./gradlew lint`
- [ ] Manual QA on device/emulator
- [ ] Screen-specific validation (see individual plans)

## Open Issues

- [ ] Pager migration complexity (very high)
- [ ] State persistence verification
- [ ] Performance benchmarks
- [ ] Accessibility compliance
## References

- [Compose BOM Versions](https://developer.android.com/jetpack/androidx/releases/compose-bom)
- [Material3 Stability](https://developer.android.com/jetpack/compose/material3/migration)