---
name: android-compose-reference
description: Use when writing, modifying, reviewing, or debugging Jetpack Compose code in this repo (Hail — Kotlin Android app on material3 1.5.0-alpha27 + compose-bom 2026.08.00). Symptoms include unfamiliar Compose APIs, state hoisting questions, modifier ordering, recomposition/perf issues, layout decisions, theming, accessibility, testing, or migration from Views.
---

# android-compose-reference

Local mirror of every page under `developer.android.com/develop/ui/compose/` lives in this skill's `reference/` directory. It is the source of truth for Compose APIs in this repo. Prefer it over training-data recall and over the `context7` MCP when both are available.

## When to load

Load this skill on any of these signals:

- Touching a `@Composable`, `Modifier`, `MaterialTheme`, `remember*`, `derivedStateOf`, `LaunchedEffect`, `CompositionLocal`, `NavHost`, `LazyColumn`, `Scaffold`, `Surface`, or `Text` site.
- Choosing between Compose APIs (`Row` vs `Box`, `FlowRow` vs `LazyRow`, `collectAsStateWithLifecycle` vs `collectAsState`).
- Reviewing a PR that adds or restructures Compose code.
- Debugging recomposition, performance, layout, or accessibility behavior.
- Migrating XML Views → Compose.

## Reference map

All paths below are relative to this skill's directory.

| Topic | File |
|---|---|
| Mental model, recomposition, when Compose re-runs | `reference/mental-model.md` |
| `remember`, hoisting, `MutableState`, snapshot system | `reference/state.md` |
| Lifecycle of composables, `DisposableEffect` | `reference/lifecycle.md` |
| Modifier order, chaining, custom modifiers | `reference/modifiers.md` |
| `LaunchedEffect`, `SideEffect`, `rememberCoroutineScope` | `reference/side-effects.md` |
| Composition phases, deferred reads | `reference/phases.md` |
| Architectural layers, compiler, runtime | `reference/layering.md` |
| Performance, stability, `key`, skipping | `reference/performance.md` |
| Semantics tree, test tags, a11y merging | `reference/accessibility/semantics.md` |
| `CompositionLocal` for theme/scopes | `reference/compositionlocal.md` |
| Material 3 components, theming, `MaterialTheme` | `reference/designsystems/material3.md` |
| M2 → M3 mapping | `reference/designsystems/material2-material3.md` |
| M2 (legacy) | `reference/designsystems/material3.md` *(avoid — project is M3)* |
| Custom design systems | `reference/designsystems/custom.md` |
| Theme anatomy, `ColorScheme`, `Typography`, `Shapes` | `reference/designsystems/anatomy.md` |
| `Row`, `Column`, `Box`, `ConstraintLayout`, basics | `reference/layouts/basics.md` |
| Custom `Layout`, `MeasurePolicy` | `reference/layouts/custom.md` |
| Intrinsic measurements | `reference/layouts/intrinsic-measurements.md` |
| Alignment lines | `reference/layouts/alignment-lines.md` |
| `ConstraintLayout` in Compose | `reference/layouts/constraintlayout.md` |
| `LazyColumn`, `LazyRow`, `LazyGrid`, `LazyListState` | `reference/lists.md` |
| `Text`, `AnnotatedString`, text overflow | `reference/text.md` |
| `Canvas`, `drawScope`, graphics modifiers | `reference/graphics.md` |
| Animation APIs (`animate*AsState`, `Transition`, `Animatable`) | `reference/animation/introduction.md` |
| Pointer input, gestures, drag, detect* | `reference/touch-input/pointer-input.md` |
| Click/long-click/indication | `reference/touch-input/user-interactions/handling-interactions.md` |
| Navigation (`NavHost`, `NavController`) | **not in local reference** — use `context7` for `androidx.navigation:navigation-compose` |
| Resources, `stringResource`, painters | `reference/resources.md` |
| Accessibility (semantics, touch targets, content descriptions) | `reference/accessibility.md` |
| Testing (`createComposeRule`, semantics matchers) | `reference/testing.md` + `reference/compose-testing-cheatsheet.pdf` |
| View ↔ Compose interop | `reference/libraries.md` |
| Architecture (UDF, ViewModel, state holders) | `reference/architecture.md` |
| Migration strategy from Views | `reference/migrate/strategy.md` |
| Interop APIs (`ComposeView`, `AndroidView`) | `reference/migrate/interoperability-apis.md` |
| Compose BOM | `reference/bom.md` |
| Android Studio tooling, previews, `@Preview` | `reference/tooling.md` |
| Kotlin idioms for Compose (scope functions, receivers) | `reference/kotlin.md` |
| APK size / runtime impact vs Views | `reference/migrate/compare-metrics.md` |
| List of all pages and regen instructions | `reference/README.md` |

## How to use the reference

1. **Identify the topic** in the table above.
2. **Open the file with the Read tool** (do NOT use `@path/to/file.md` — that force-loads and wastes context).
3. **Quote or cite** the relevant section (`file_path:line_number` format).
4. **Cross-check** against the project's actual dependency versions in `app/build.gradle.kts` before recommending an API.

If the topic is not in the map (e.g. navigation, paging, hilt-compose), use `context7` MCP or the `material3-expressive` skill — do not invent.

## REQUIRED: process for every Compose change

Before writing or accepting a Compose change, answer in your reply:

1. Which `reference/*.md` file informed the change?
2. Cite one concrete rule from that file (with line number).
3. How does the change conform to that rule?

If you cannot answer all three, you have not consulted the reference — stop and read it.

## Red flags — stop and consult the reference

| Excuse | Reality |
|---|---|
| "I know Compose, no need to check" | Project uses a specific BOM + M3-expressive; recall is stale. |
| "The reference is too big" | Read only the relevant file, not the whole folder. |
| "context7 already covers it" | It may not be available; local reference is authoritative. |
| "I'll just use M2 patterns I remember" | Project is M3-only. M2 imports are a regression. |
| "Modifier order doesn't matter much" | It does. Reference says so explicitly. |
| "State inside a reusable composable is fine" | No — hoist it. `state.md`. |
| "It's a small change" | Small changes ship wrong APIs. Read the file. |
| "I'll skip the citation, it's obvious" | Citation forces actual reading. Skipping it = skipped reading. |

## When NOT to read the reference

- Pure Kotlin logic (no `@Composable`, no UI types).
- Build/CI/Gradle-only changes.
- Xposed/Shizuku/Room code with no UI surface.

Even then, if the change touches a composable indirectly (e.g. a `StateFlow` consumed by a composable), you still need `state.md` or `architecture.md`.

## Notes on the reference itself

- It is a **mirror**, not a substitute for running code. Compile-check after any change.
- Two pages are missing from Google's `.md.txt` endpoint (see `reference/README.md`): `semantics` is mirrored at `accessibility/semantics.md`; `testing-cheatsheet` lives as `compose-testing-cheatsheet.pdf`.
- Regenerate the mirror with the script in `reference/README.md` if docs change upstream.