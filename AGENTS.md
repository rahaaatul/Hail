# AGENTS.md

## Project Overview

Hail is an Android application written in Kotlin. It manages other installed applications, including freezing (disabling) and unfreezing (enabling) them. The app's package name is `com.aistra.hail`.

The project uses:
- Jetpack Navigation for screen routing
- Room 3.0 for local persistence (migrated from 2.8.3)
- Material 3 for UI components
- Kotlin Coroutines with `Flow` for reactive data
- Jetpack Compose (with View Binding also enabled)
- Shizuku/Dhizuku for privileged operations
- LSPosed/Xposed hooks for system-level app management

## Build and Test Commands

All commands assume the working directory is the repository root (`/workspaces/Hail`).

To build the debug APK:

    ./gradlew :app:assembleDebug

To run unit tests:

    ./gradlew :app:testDebugUnitTest

To compile Kotlin only (faster iteration):

    ./gradlew :app:compileDebugKotlin

To process resources (validate navigation XML, etc.):

    ./gradlew :app:processDebugResources

To check for merge conflicts or whitespace issues:

    git diff --check

## Code Style

- Kotlin is the primary language. All new code should be in Kotlin, not Java.
- Follow existing patterns in the codebase. The `utils` package (`app/src/main/kotlin/com/aistra/hail/utils/`) is a good reference for conventions.
- Use `object` for singletons, `data class` for value types, and `sealed class`/`sealed interface` for result types.
- Use `suspend fun` with `withContext(Dispatchers.IO)` for asynchronous or I/O operations.
- Use `Flow` for reactive data streams (e.g., Room DAO observations).
- Prefer `val` over `var`, and immutable collections where possible.
- Use `runCatching { }.getOrNull()` or `getOrDefault()` for safe fallible operations.
- Do not add comments unless they explain non-obvious behavior.

## Source Structure

- Kotlin sources: `app/src/main/kotlin/com/aistra/hail/`
- Resources: `app/src/main/res/`
- Navigation graph: `app/src/main/res/navigation/mobile_navigation.xml`
- Bottom nav menu: `app/src/main/res/menu/nav_main.xml`
- Strings: `app/src/main/res/values/strings.xml` (with translations in `values-*` directories)

Key packages:
- `com.aistra.hail.ui` — Fragments, Activities, Adapters
- `com.aistra.hail.app` — Application class, AppManager, HailApi, HailData
- `com.aistra.hail.utils` — Utilities, Room classes, executors, caches
- `com.aistra.hail.services` — Background services
- `com.aistra.hail.xposed` — LSPosed/Xposed hooks
- `com.aistra.hail.work` — WorkManager workers

## Room Conventions

Room classes live as Kotlin files in `app/src/main/kotlin/com/aistra/hail/utils/`:
- `ActionEntity.kt`, `ActionDao.kt`, `ActionDependencyEntity.kt`
- `AppMetadataEntity.kt`, `AppMetadataDao.kt`, `AppMetadataDatabase.kt`

Room 3.0 uses the `androidx.room3` package namespace and requires KSP (Kotlin Symbol Processing) for code generation.

When working with Room:
- Use `data class` for entities in Kotlin
- Use `interface` for DAOs with `@Dao`
- Use `@Entity`, `@PrimaryKey`, `@ForeignKey`, `@Query`, `@Insert`, `@Update`, `@Delete` annotations
- Use `@Transaction` for multi-operation DAO methods
- The database is accessed via `AppMetaCache.database()` (defined in `AppMetaCache.kt`)
- Schema version is tracked in `AppMetadataDatabase.java` (currently version 4)
- Migrations are defined as `Migration` objects in `AppMetaCache.kt`
- Table and column names must match existing SQL exactly

## ExecPlan Usage

This repository uses an ExecPlan (as described in `.kilo/plans/actions-feature-plan.md`) for the Actions feature implementation. When working on the Actions feature:

1. Read the entire ExecPlan before starting work
2. Update the `Progress` section with checkbox items as you complete steps
3. Record discoveries in `Surprises & Discoveries`
4. Log decisions in `Decision Log`
5. Write outcomes in `Outcomes & Retrospective`
6. Follow milestones in order — each produces a working, testable state
7. Do not mark items complete until validation commands pass

When creating new plans for future features, adhere to the OpenAI ExecPlan template described at `https://developers.openai.com/cookbook/articles/codex_exec_plans`. Key requirements:

- Every plan must be fully self-contained — a novice with only the plan and the working tree can implement it end-to-end
- Plans are living documents — revise them as progress is made, discoveries occur, and decisions are finalized
- Include and maintain these sections: `Progress` (checkbox list with timestamps), `Surprises & Discoveries`, `Decision Log`, `Outcomes & Retrospective`
- Use prose-first narrative; avoid tables and checklists outside the `Progress` section
- Define every term of art in plain language; do not refer to external docs
- Anchor with observable outcomes — state what the user can do and what commands to run
- Name files with full repository-relative paths; show exact commands and expected outputs
- Make steps idempotent and safe; include retry or rollback paths for risky operations

## Research-First Decision Policy

Before deciding, coding, or answering:
- Run `semantic_search` on the codebase for existing patterns.
- Run `websearch` for current syntax, changelogs, or best practices.
- Use Context7 docs lookup for any library/framework/SDK in scope.

## Behavioral Guidelines

These principles reduce common LLM coding mistakes. Bias toward caution over speed; for trivial tasks, use judgment.

### 1. Think Before Coding

Don't assume. Don't hide confusion. Surface tradeoffs.

Before implementing:

- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them — don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

### 2. Simplicity First

Minimum code that solves the problem. Nothing speculative.

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

### 3. Surgical Changes

Touch only what you must. Clean up only your own mess.

When editing existing code:

- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it — don't delete it.

When your changes create orphans:

- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

### 4. Goal-Driven Execution

Define success criteria. Loop until verified.

Transform tasks into verifiable goals:

- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:

    1. [Step] → verify: [check]
    2. [Step] → verify: [check]
    3. [Step] → verify: [check]

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

## Testing

The project currently has no existing test suite. The Actions ExecPlan calls for adding:
- Room DAO tests (insert, read, update, delete, duplicate)
- Save-validation tests (empty Unfreeze, empty Launch, Launch overlap)
- Executor tests (already-unfrozen deps, sequential unfreeze, verification failure, successful launch)
- Shortcut intent tests (action-ID routing)
- UI tests (field order, truncation, Save/Cancel, long-press menu, navigation)

When adding tests, place them in `app/src/test/kotlin/com/aistra/hail/` following the package structure of the code under test.

## Security

- Never commit `signing.properties` or `local.properties` — these contain secrets and are gitignored
- The `release` build type requires signing properties; use `debug` for development and testing
- The app uses `hiddenapibypass` for accessing hidden Android APIs — do not remove this dependency
- Shizuku/Dhizuku integration requires user consent — do not bypass permission checks
- The Xposed module (`xposed` package) hooks into system services — changes here can affect device stability

## Localization

When adding user-facing strings:
- Add to `app/src/main/res/values/strings.xml` (English)
- Update all maintained translations in `values-*` directories
- Use string resources, never hardcode user-visible text
- Mark new strings with a translator comment if the meaning is not obvious from the key name
