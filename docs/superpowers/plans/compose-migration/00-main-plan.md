# Hail App Jetpack Compose Migration Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Migrate all Hail app screens from ViewBinding/XML layouts to Jetpack Compose while preserving the existing Fragment/Activity-based navigation structure (Navigation Component with `mobile_navigation.xml`).

**Architecture:** Incremental screen-by-screen migration using `ComposeView` inside existing Fragments/layouts. Keep Navigation Component (NavHostFragment + XML nav graph) intact. Each screen becomes a `@Composable` function rendered in a `ComposeView` within its existing Fragment container. Shared state (ViewModels, HailData, AppManager) remains unchanged.

**Tech Stack:**
- Jetpack Compose BOM 2026.08.00 (stable) / Material3 1.4.0
- Kotlin 2.4.20, AGP 9.4.0, compileSdk 37
- Navigation Component 2.10.0 (Fragment-based, kept as-is)
- Coil 2.6.0 for async image loading (replaces AppIconCache)
- Room 2.6.0 (kept, DAOs extended with Flow queries)
- me.zhanghai.compose.preference 2.3.0 (already used in SettingsFragment)

**Spec:** This plan is based on comprehensive codebase analysis of all 50+ Kotlin files, 14 layout XMLs, 5 menu XMLs, and resource files.

## Global Constraints
- Preserve existing navigation: `mobile_navigation.xml` with 5 destinations (nav_home, nav_actions, nav_apps, nav_settings, nav_about) stays unchanged
- Preserve MainActivity: AppCompatActivity with BottomNavigationView, NavigationRailView, FAB, biometric auth - stays as-is
- Preserve Fragment containers: Each destination Fragment remains but its `onCreateView` returns a `ComposeView` instead of inflating XML
- Preserve business logic: HailData, AppManager, AppMetaCache, ActionsRepository, WorkManager services unchanged
- Material3 only: Use stable Material3 components (no Expressive/Material3 Expressive alpha)
- Coil for images: Replace custom AppIconCache with Coil's AsyncImage
- No breaking changes: Each screen migration must be independently testable; app must build and run after each screen

---
### Task 1: Review and Understand Migration Strategy
**Files:**
- Read: `docs/superpowers/plans/compose-migration/00-main-plan.md` (this file)
- Read: `docs/superpowers/plans/compose-migration/00-master-plan.md`

**Steps:**
- [ ] Read and understand the overall migration strategy
- [ ] Review the screen migration order and dependencies
- [ ] Familiarize yourself with the validation checklist and rollout strategy

### Task 2: Prepare for Implementation
**Files:**
- No new files to create

**Steps:**
- [ ] Ensure you're on the `migrate/compose` branch
- [ ] Verify the development environment is set up (Android Studio, JDK, etc.)
- [ ] Check that all necessary dependencies can be resolved

## Screen Migration Order (Dependencies First)

| Phase | Screen | Fragment | Complexity | Depends On |
|-------|--------|----------|------------|------------|
| 1 | **AppIcon/Compose Theme** | Infrastructure | Low | - |
| 2 | **ApiActivity** | ApiActivity | Low | Theme |
| 3 | **AppsFragment** | AppsFragment | High | AppIcon, Theme |
| 4 | **ActionsFragment** | ActionsFragment | High | AppIcon, Theme |
| 5 | **PagerFragment** | PagerFragment (inside HomeFragment) | Very High | AppIcon, Theme |
| 6 | **HomeFragment** | HomeFragment | Medium | PagerFragment |
| 7 | **SettingsFragment** | Already Compose | ✅ Done | - |
| 8 | **AboutFragment** | Already Compose | ✅ Done | - |

## Plan Files

Each screen has a detailed plan file in this directory:

- `01-appicon-theme-plan.md` — Coil integration, Material3 theme completion, AppIcon composable
- `02-apps-fragment-plan.md` — AppsFragment → Compose (LazyVerticalGrid, search, filters, context menu)
- `03-actions-fragment-plan.md` — ActionsFragment → Compose (LazyColumn, dialogs, app picker)
- `04-pager-fragment-plan.md` — PagerFragment → Compose (LazyVerticalGrid, multi-select, tag dialogs)
- `05-home-fragment-plan.md` — HomeFragment → Compose (TabRow + HorizontalPager, tab sync)
- `06-api-activity-plan.md` — ApiActivity → Compose (translucent activity)
- `07-testing-ci-plan.md` — Testing and CI/CD infrastructure for Compose UI testing
- `08-resource-viewmodel-plan.md` — Resource migration and ViewModel conversion to StateFlow

## Validation Checklist (Per Screen)

After each screen migration:
- [ ] App builds successfully (`./gradlew assembleDebug`)
- [ ] Screen renders correctly in both light/dark theme
- [ ] Screen renders correctly in landscape/portrait
- [ ] All interactions work (click, long-click, search, filters, dialogs)
- [ ] State persists across configuration changes
- [ ] No memory leaks (ComposeView dispose strategy)
- [ ] Performance: no jank on scroll, smooth animations
- [ ] Accessibility: TalkBack navigation works
- [ ] Compose UI tests compile and pass (when applicable)
- [ ] Existing unit tests still pass (`./gradlew test`)
- [ ] No increase in APK size beyond expected Compose dependencies

## Rollout Strategy

1. **Feature branch**: `migrate/compose` from `main`
2. **Sequential PRs**: 8 PRs total (one per screen + final cleanup)
3. **CI validation**: Each PR runs `./gradlew assembleDebug test lint`
4. **Manual QA**: Test on physical device (API 24+) and emulator (API 37)
5. **Merge order**: Infrastructure → ApiActivity → Apps → Actions → Pager → Home → Settings/About → Cleanup
6. **User testing**: Release to internal testers after each phase
7. **Final release**: After all phases complete and validated

## Open Questions (Resolved)

| Question | Decision |
|----------|----------|
| Navigation 3 vs Navigation 2? | Keep Navigation 2 (Fragment-based XML nav graph) |
| Material3 Expressive? | No - use stable Material3 1.4.0 only |
| ViewBinding removal? | Deferred to final cleanup PR |
- Coil vs Glide? | Coil (better Compose integration, smaller) |
- AppIconCache removal? | After all screens migrated and verified |

## References
- [Android Compose Migration Guide](https://developer.android.com/jetpack/compose/migration)
- [Coil Compose Integration](https://coil-kt.github.io/coil/compose/)
- [Material3 Components](https://developer.android.com/jetpack/compose/designsystems/material3)
- [Navigation Component with Compose Interop](https://developer.android.com/guide/navigation/navigation-compose-interop)