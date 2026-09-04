---
name: compose
description: Use when doing anything with Jetpack Compose in this repo. Routes to the correct sub-skill or reference inside .kilo/skills/jetpack-compose/.
---

# Compose Skills Index

This directory contains skills and reference docs for Jetpack Compose. Load the sub-skill that matches the task before writing or changing Compose code.

## Skills inside .kilo/skills/jetpack-compose/

| Task | Load this skill |
|---|---|
| Adaptive UI (foldables, tablets, multi-pane, NavigationSuiteScaffold, Grid, FlexBox, MediaQuery) | `adaptive` |
| Migrate an XML layout or whole app to Compose | `migrate-xml-views-to-jetpack-compose` |
| Integrate Compose Styles API (ComponentStyles, Modifier.styleable, StyleScope) | `styles` |
| Look up Compose API docs, semantics, performance rules, layout, state, testing | `android-compose-reference` |

## Reference docs inside .kilo/skills/jetpack-compose/reference/

| Topic | File |
|---|---|
| Material 3 theming, ColorScheme, Typography, Shapes | `reference/designsystems/material3.md` |
| M2 → M3 migration | `reference/designsystems/material2-material3.md` |
| Custom design systems | `reference/designsystems/custom.md` |
| Layouts (Row, Column, Box, ConstraintLayout, custom Layout) | `reference/layouts/basics.md`, `reference/layouts/custom.md` |
| Lazy lists, grids | `reference/lists.md` |
| State, remember, hoisting, snapshot | `reference/state.md` |
| Modifiers, chaining, custom modifiers | `reference/modifiers.md` |
| Side-effects (LaunchedEffect, DisposableEffect) | `reference/side-effects.md` |
| Performance, stability, key, skipping | `reference/performance.md` |
| Accessibility, semantics, test tags | `reference/accessibility.md`, `reference/accessibility/semantics.md` |
| Touch input, gestures, pointer input | `reference/touch-input/pointer-input.md` |
| Animation | `reference/animation/introduction.md` |
| Compose testing | `reference/testing.md` |
| Migration strategy, interop APIs | `reference/migrate/strategy.md`, `reference/migrate/interoperability-apis.md` |

## How to load

Call `skill(<name>)` with the name from the table above before writing Compose code.

## REQUIRED: citation for every Compose change

Before writing or accepting a Compose change, answer in your reply:

1. Which `reference/*.md` file informed the change?
2. Cite one concrete rule from that file (with line number).
3. How does the change conform to that rule?

If you cannot answer all three, you have not consulted the reference — stop and read it.

## When NO sub-skill fits

If the task is not covered by any sub-skill above:

1. Check `android-compose-reference` for API docs.
2. Check `material3-expressive` for M3-expressive components.
3. Check `navigation-3` for routing.
4. Use expert judgment for the rest.

## Red flags — wrong skill or no skill loaded

| Excuse | Reality |
|---|---|
| "I know Compose, no need for a skill" | Sub-skills encode repo-specific rules (BOM version, M3-expressive, Navigation 3). |
| "This is just a small modifier change" | Modifier order, M3-expressive motion, and accessibility rules are skill-governed. |
| "I'll load the reference later" | `android-compose-reference` is the source of truth; load it first. |
| "adaptive/styles don't apply here" | If the task touches those areas, the skill does apply. |
| "I'll skip the citation, it's obvious" | Citation forces actual reading. Skipping it = skipped reading. |
