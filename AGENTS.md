---
description: Hail is an Android app (Kotlin + Jetpack Compose) for managing installed apps via freezing/unfreezing. Contains build/test commands, code style, project structure, and boundaries for AI coding agents.
tags: [android, kotlin, jetpack-compose, material3]
---

# AGENTS.md

**PRIMARY RULES (must not break at any cost):**
- Always work on `rahaaatul/hail` repo — never on upstream `aistra0528/hail`
- Block any push/PR/comment action targeting `aistra0528/hail`; redo it targeting `rahaaatul/hail`
- `@CLAUDE.md` is the main rule file — its behavioral guidelines take precedence and must not be violated unless there is a very good reason

## Project Overview

Hail is an Android application written in Kotlin. It manages other installed applications, including freezing (disabling) and unfreezing (enabling) them. Package: `com.aistra.hail`.

**Stack:** Jetpack Navigation 3, Room 3.0, Material 3 Expressive, Kotlin Coroutines + Flow, Jetpack Compose (with View Binding), Shizuku/Dhizuku, LSPosed/Xposed hooks.

## Build & Test Commands

```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:testDebugUnitTest       # Run unit tests
./gradlew :app:compileDebugKotlin      # Compile Kotlin only (faster iteration)
./gradlew :app:processDebugResources   # Process resources (validate navigation XML)
git diff --check                       # Check for merge conflicts or whitespace
```

## Code Style

Follow the [official Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and [Google Android Kotlin style guide](https://developer.android.com/kotlin/style-guide) as the baseline. Key points:

- Naming: `camelCase` for functions/properties, `PascalCase` for classes/types, `UPPER_SNAKE_CASE` for constants
- Indentation: 4 spaces, no tabs
- Braces: K&R style ("Egyptian brackets")
- Prefer `val` over `var`, immutable collections where possible
- Use `object` for singletons, `data class` for value types, `sealed class`/`sealed interface` for result types
- Use `suspend fun` with `withContext(Dispatchers.IO)` for async/I/O operations
- Use `Flow` for reactive data streams (Room DAO observations)
- Use `runCatching { }.getOrNull()` or `getOrDefault()` for safe fallible operations
- Do not add comments unless they explain non-obvious behavior
- Match existing patterns in `com.aistra.hail/utils/`

For Compose code, follow the [Jetpack Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md):
- `@Composable` functions returning `Unit` use `PascalCase` noun names
- `@Composable` functions returning values use `camelCase`
- Element functions accept `modifier` as first optional parameter

## Project Structure

```
app/src/main/kotlin/com/aistra/hail/
  ui/          — Fragments, Activities, Adapters
  app/         — Application class, AppManager, HailApi, HailData
  utils/       — Utilities, Room classes, executors, caches
  services/    — Background services
  xposed/      — LSPosed/Xposed hooks
  work/        — WorkManager workers

app/src/main/res/
  navigation/mobile_navigation.xml  — Navigation graph
  menu/nav_main.xml                 — Bottom nav menu
  values/strings.xml                — Strings (with values-* translations)
```

## Room Conventions

Room classes live in `app/src/main/kotlin/com/aistra/hail/utils/` as Kotlin files. Uses `androidx.room3` namespace with KSP.

- Entities: `data class`, `@Entity`, `@PrimaryKey`, `@ForeignKey`
- DAOs: `interface` with `@Dao`, `@Query`, `@Insert`, `@Update`, `@Delete`
- Multi-operation: `@Transaction`
- Access via `AppMetaCache.database()` in `AppMetaCache.kt`
- Schema version 6 in `AppMetadataDatabase.kt`
- Table/column names must match existing SQL exactly

## Git Workflow

- Branch naming: `feature/description` or `fix/description`
- Commit format: Conventional commits (`feat:`, `fix:`, `chore:`, `docs:`)
- PR title: `[Hail] <description>`
- Squash merge only
- Pre-push gate: `./gradlew :app:compileDebugKotlin && ./gradlew :app:testDebugUnitTest`

## Testing

Tests live in `app/src/test/kotlin/com/aistra/hail/` (unit tests) and `app/src/androidTest/kotlin/com/aistra/hail/` (instrumented tests).

- Framework: JUnit4 + MockK + Turbine + Robolectric
- Run all unit tests: `./gradlew :app:testDebugUnitTest`
- Run single test: `./gradlew :app:testDebugUnitTest --tests="ClassName.testMethod"`
- Coverage report: `./gradlew :app:jacocoTestReport` (output: `build/reports/jacoco/jacocoTestReport/`)
- Coroutine tests use `runTest { }`
- Room tests use `Room.inMemoryDatabaseBuilder()` with `allowMainThreadQueries()`
- Mocking: `mockkObject()`, `every { }`, `coEvery { }` for suspend functions
- Compose UI tests: `androidx.compose.ui:ui-test-junit4` + `createAndroidComposeRule`
- Robolectric: run instrumented-style tests locally without a device

When adding tests, place them in `app/src/test/kotlin/com/aistra/hail/` following the package structure of the code under test.

## Agentic Workflow CLI Tools

The following CLI tools are installed and benefit agentic workflows. Use them when built-in tools are insufficient or when operating in pipelines.

| Tool | Use case | Example |
|------|----------|---------|
| `rg` (`ripgrep`) | Fast regex/line search; respects `.gitignore` | `rg "pattern" --type kotlin` |
| `fd` | Fast file discovery by pattern/extension | `fd -e kt` |
| `jq` | JSON parsing/transformation in pipelines | `curl ... \| jq '.field'` |
| `curl` | HTTP requests for API inspection | `curl -s <url> \| jq` |

These complement built-in tools (`Grep`, `Glob`, `Read`, `bash`). Prefer built-ins for structured operations; use these for text processing, JSON pipelines, or when regex flexibility is needed.

## Security

- Never commit `signing.properties` or `local.properties` (gitignored, contain secrets)
- Release build requires signing properties; use `debug` for development
- `hiddenapibypass` for hidden Android APIs — do not remove
- Shizuku/Dhizuku requires user consent — do not bypass
- Xposed module hooks system services — changes can affect device stability

## Boundaries

**Always do:**
- Run `./gradlew :app:compileDebugKotlin` after code changes
- Use `material3-expressive` skill for any Compose UI work
- Use `kotlin-ultimate` skill for any Kotlin code

**Ask first:**
- Adding new dependencies
- Modifying Xposed hooks or system-level code
- Changing Room schema (requires migration)
- Modifying `AndroidManifest.xml` components

**Never do:**
- Force-push to shared branches
- Run destructive git commands without confirmation

## Skills

Skills live in `.kilo/skills/`. Load the relevant skill before starting work. For full skill index with invocation triggers, see [SKILLS.md](SKILLS.md).

### Always Use

| Skill | When |
|-------|------|
| `material3-expressive` | Any Compose UI work |
| `kotlin-ultimate` | Any Kotlin code |
| `using-superpowers` | Start of any conversation |
| `edge-to-edge` | UI layout or system bar work |
| `navigation-3` | Screen routing or navigation changes |
| `styles` | Compose theming with Styles API |

### Invoke When Needed

| Skill | When |
|-------|------|
| `android-build-setup` | Build environment issues |
| `android-intent-security` | Intent handling or manifest audit |
| `systematic-debugging` | Any bug or unexpected behavior |
| `verification-before-completion` | Before claiming work complete |
| `brainstorming` | Before any creative work |
| `test-driven-development` | Implementing features or fixes |
| `jetpack-compose` | Compose migration, adaptive layouts, theming |

## Research-First Decision Policy

Before deciding, coding, or answering:
- Run `semantic_search` on the codebase for existing patterns
- Run `websearch` for current syntax, changelogs, or best practices
- Use Context7 docs lookup for any library/framework/SDK in scope

## Localization

- Add strings to `app/src/main/res/values/strings.xml` (English)
- Update all maintained translations in `values-*`
- Use string resources, never hardcode user-visible text
- Mark new strings with translator comments if meaning is not obvious
