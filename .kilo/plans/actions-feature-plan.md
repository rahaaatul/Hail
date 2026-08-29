# Actions Feature ExecPlan

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds.

If `PLANS.md` is checked into the repo, reference the path to that file here from the repository root and note that this document must be maintained in accordance with `PLANS.md`.

## Purpose / Big Picture

Hail is an Android app that manages other installed applications, including freezing (disabling) and unfreezing (enabling) them. Currently, a user who wants to launch an app like YouTube that depends on companion apps (like MicroG) must manually unfreeze each dependency first, then launch the target.

This change adds **Actions**: reusable launch configurations that automatically unfreeze one or more dependency apps, then launch a single target app. After this work, a user can create an action named "YouTube" that unfreezes MicroG and MicroG Services, then opens YouTube -- all from a single tap or a pinned home-screen shortcut.

The feature is intentionally separate from Hail's existing Home app selections, tags, whitelist state, and normal app shortcuts. It adds a new top-level "Actions" tab between Home and Settings, removes the Apps tab from bottom navigation (while keeping Apps reachable from Home), and repurposes the existing Home floating action button (FAB) into an Apps navigation button. A new plus-shaped FAB on the Actions screen opens a creation dialog.

## Progress

Use a list with checkboxes to summarize granular steps. Every stopping point must be documented here, even if it requires splitting a partially completed task into two ("done" vs. "remaining"). This section must always reflect the actual current state of the work.

- [x] (2026-08-27) Plan and UX specification drafted and pushed to `plan/actions-feature`.
- [x] (2026-08-27) Room Actions schema and repository added on `feature/actions`, including ordered dependencies and a 2-to-3 migration.
- [x] (2026-08-27) Shared sequential action executor and action-ID API execution added without changing existing API actions.
- [x] (2026-08-27) Actions destination, Home-to-Apps FAB navigation, compact action list, Create/Edit dialog, app picker, long-press menu, duplicate/delete behavior, and pinned shortcut creation added.
- [x] (2026-08-27) Initial Actions implementation completed across Room storage, execution/API routing, navigation, UI, long-press operations, and pinned shortcuts. `assembleDebug` passed.
- [x] (2026-08-28) Scroll-driven FAB hiding removed, Home and Actions FAB icons aligned, Actions FAB rebound after Home teardown, searchable app selection with package/name filtering added.
- [x] (2026-08-28) Blocking installed-app loads replaced with cached-first rendering, silent background refresh, four-column selectable app picker, Material-spaced rounded search, and flow-style Actions tab icon.
- [x] (2026-08-28) Malformed Actions automation vector replaced (it crashed navigation inflation); explicit progress dialog upgraded to Material's contiguous progress indicator.
- [x] (2026-08-28) Custom Actions navigation path replaced with the official Material Symbols Outlined `automation` vector.
- [x] (2026-08-28) Room migration crash fixed: migration-created `index_action_dependencies_actionId` existed but the entity schema expected no index. Added a 3-to-4 cleanup migration.
- [x] (2026-08-28) Both Unfreeze and Launch selector dialogs updated to use `Cancel` and `OK`; parent action editor remains `Cancel` and `Save`.
- [x] (2026-08-29) Room 2.8.3 upgraded to 3.0.1 with KSP annotation processor.
- [x] (2026-08-29) Six Java Room classes converted to Kotlin (`ActionEntity`, `ActionDependencyEntity`, `AppMetadataEntity`, `ActionDao`, `AppMetadataDao`, `AppMetadataDatabase`).
- [x] (2026-08-29) Room migrations updated to use `SQLiteConnection` API instead of `SupportSQLiteDatabase`.
- [x] (2026-08-29) Callers in `AppMetaCache.kt` and `ActionsRepository.kt` updated to use new Kotlin data classes.
- [x] (2026-08-29) Build compiles successfully with Room 3.0 and Kotlin DAOs.
- [ ] (2026-08-28) Remove main-thread package resolution from Unfreeze/Launch picker opening, align picker spacing, reduce cells to a 4-column compact grid, and change selection to a full-icon tint with a centered check overlay.
- [ ] (2026-08-28) Add startup cache warming for all app metadata/icons, cached-first Apps loading with silent refresh, and a four-column searchable picker with rounded Material spacing and selected check indicators.
- [ ] Add focused Room, executor, and UI tests; verify interaction flows on an Android device or emulator before release.
- [ ] Backup/Restore feature (out of scope for initial Actions implementation; reserved for a future branch).

## Surprises & Discoveries

Document unexpected behaviors, bugs, optimizations, or insights discovered during implementation. Provide concise evidence.

- Observation: Room schema validation failed after the automation icon fix because a migration created an index (`index_action_dependencies_actionId`) that the entity schema did not declare.
  Evidence: Crash log showed `Room schema validation failure`. Fixed by removing the undeclared index in a 2-to-3 migration and adding a 3-to-4 cleanup migration for devices already on version 3.

- Observation: The resolved Material 3 artifacts do not expose the newer Compose `LoadingIndicator` API.
  Evidence: Used the Material Components contiguous progress indicator instead.

- Observation: Six Room persistence classes (`ActionEntity`, `ActionDao`, `ActionDependencyEntity`, `AppMetadataEntity`, `AppMetadataDao`, `AppMetadataDatabase`) were written in Java despite the rest of the codebase being Kotlin.
  Evidence: All six files live in `app/src/main/java/com/aistra/hail/utils/`, while the 21 other files in the same package are `.kt`. Room fully supports Kotlin, so this is a consistency issue rather than a technical requirement.

- Observation: Room 3.0 introduces breaking API changes that affect the migration path.
  Evidence: The `Migration.migrate()` method now takes `SQLiteConnection` instead of `SupportSQLiteDatabase`, and the method becomes a `suspend` function. The `execSQL` extension function requires `androidx.sqlite:sqlite` artifact (version 2.7.0). The `androidx.room3:room3-ktx` artifact does not exist — coroutines support is built into `room3-runtime`.

- Observation: A latent bug existed in the Java code where the SQL query referenced column `position` but the entity field was named `ordering`.
  Evidence: KSP processing error: `no such column: position`. Fixed by aligning the SQL query with the actual field name `ordering`.

- Observation: Kotlin interfaces do not use the `default` keyword for default methods.
  Evidence: The Java `default` keyword in interface methods caused a syntax error in Kotlin. Removing `default` and keeping just `fun` with a body resolved it.

## Decision Log

Record every decision made while working on the plan in the format:

- Decision: Store Actions in the existing Room database rather than in `HailData` JSON.
  Rationale: Actions are relational, mutable records with child dependencies and stable IDs. Room provides foreign keys, cascading delete, and ordered storage that JSON does not.
  Date/Author: 2026-08-27

- Decision: Use a separate `ActionDependencyEntity` child table for unfreeze packages rather than serializing the list into a single column.
  Rationale: Preserves package order for deterministic summaries and execution, and allows efficient CRUD on individual dependencies.
  Date/Author: 2026-08-27

- Decision: Repurpose the Home FAB into an Apps navigation button and add a new plus FAB for Actions.
  Rationale: Keeps Apps reachable without a dedicated tab, and gives Actions a clear creation entry point without adding clutter to the Home screen.
  Date/Author: 2026-08-27

- Decision: Use the official Material Symbols Outlined `automation` vector instead of a custom drawable.
  Rationale: Consistent with Material Design language and avoids maintaining a custom asset.
  Date/Author: 2026-08-28

- Decision: Convert the six Java Room classes to Kotlin.
  Rationale: The rest of the Hail codebase is Kotlin. The Java files were an artifact of the implementing agent's default, not a technical requirement. Room fully supports Kotlin, and converting eliminates the inconsistency and simplifies future maintenance.
  Date/Author: 2026-08-29

- Decision: Upgrade Room from 2.8.3 to 3.0.1 before converting Java classes to Kotlin.
  Rationale: Room 3.0 is KSP-only, which forces the annotation processor setup immediately and eliminates dual compatibility. Converting Java files to Kotlin using the new Room 3.0 APIs in a single pass avoids a second transition. This is cleaner than converting on Room 2.8.3 and then upgrading.
  Date/Author: 2026-08-29

## Outcomes & Retrospective

Summarize outcomes, gaps, and lessons learned at major milestones or at completion. Compare the result against the original purpose.

The initial Actions implementation is functionally complete across Room storage, execution/API routing, navigation, UI, long-press operations, and pinned shortcuts. The build passes (`assembleDebug` and `testDebugUnitTest`), and static diagnostics are clean. Runtime visual verification on a device or emulator remains undone because no emulator was provided. Focused unit tests for the new Room entities, executor, and UI are not yet written. The Backup/Restore feature is explicitly out of scope for this phase.

## Context and Orientation

Hail is an Android application written in Kotlin. It uses Jetpack Navigation for screen routing, Room for local persistence, and Material 3 for its UI components. The app's package name is `com.aistra.hail`.

The main entry point is `app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt`, which hosts a `NavHostFragment` driven by `app/src/main/res/navigation/mobile_navigation.xml`. The bottom navigation menu is defined in `app/src/main/res/menu/nav_main.xml`. The current top-level destinations are Home, Apps, and Settings.

The Home screen (`app/src/main/kotlin/com/aistra/hail/ui/home/HomeFragment.kt`) shows a list of installed apps and a floating action button (FAB) that currently triggers freeze operations. The Apps screen (`app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt`) shows the full installed-app list with search and filtering.

Room is already used for app metadata caching. The relevant existing surfaces are `AppMetadataEntity`, `AppMetadataDao`, `AppMetadataDatabase`, and `AppMetaCache`. These currently live as Java files in `app/src/main/java/com/aistra/hail/utils/` (see Milestone 7 for the plan to convert them to Kotlin). The Kotlin side of the same package includes `AppMetaCache`, `ActionsRepository`, and `ActionExecutor`.

Hail's API surface for external callers is defined in `app/src/main/kotlin/com/aistra/hail/app/HailApi.kt`, with `app/src/main/kotlin/com/aistra/hail/ui/api/ApiActivity.kt` serving as the entry point for API intents. Pinned shortcut support lives in `app/src/main/kotlin/com/aistra/hail/utils/HShortcuts.kt`. Persistent app/tag configuration is stored as JSON through `app/src/main/kotlin/com/aistra/hail/app/HailData.kt`.

This plan touches four layers of the app: the Room persistence layer (new entities, DAO, migrations), the execution layer (a shared action executor), the navigation layer (new destination, FAB reconfiguration), and the UI layer (Actions list, dialogs, pickers, shortcuts). A novice should start by reading `mobile_navigation.xml` and `MainActivity.kt` to understand the current destination structure, then read the existing `AppMetadataDatabase` to understand the Room conventions before adding new entities.

## Plan of Work

The work is broken into milestones. Each milestone is independently verifiable and incrementally builds toward the full feature. The milestones are ordered so that each one produces a working, testable state before the next begins.

### Milestone 1: Upgrade Room to 3.0 and Convert Java Classes to Kotlin

The six Room persistence classes were written in Java using Room 2.8.3 with the legacy `annotationProcessor`. The rest of the Hail codebase is Kotlin. This milestone upgrades Room to 3.0 (which requires KSP and is Kotlin-first) and converts the six Java files to Kotlin in a single pass. This is the first priority because all subsequent milestones (schema, executor, tests) depend on the persistence layer.

Upgrading Room first is cleaner than converting on Room 2.8.3 because Room 3.0 is KSP-only — it forces the KSP setup immediately and eliminates dual compatibility. The Java-to-Kotlin conversion then uses the new Room 3.0 APIs directly, avoiding a second transition.

**Step 1: Upgrade Room dependencies**

The project currently uses `androidx.room:room-runtime:2.8.3` with `annotationProcessor(libs.androidx.room.compiler)`. Replace these with Room 3.0:

```kotlin
// build.gradle.kts (project-level)
plugins {
    id("com.google.devtools.ksp") version "<ksp-version>" apply false
}

// build.gradle.kts (app-level)
plugins {
    id("com.google.devtools.ksp")
}

dependencies {
    val roomVersion = "3.0.1"
    implementation("androidx.room3:room3-runtime:$roomVersion")
    implementation("androidx.room3:room3-ktx:$roomVersion")
    ksp("androidx.room3:room3-compiler:$roomVersion")
    implementation("androidx.room3:room3-sqlite-wrapper:$roomVersion")
}
```

The KSP version must match the project's Kotlin version (2.4.10). Check the [KSP releases page](https://github.com/google/ksp/releases) for the exact compatible version.

Note the package rename: `androidx.room` becomes `androidx.room3`. The `room3-ktx` artifact provides coroutine extensions (suspend functions, Flow). The `room3-sqlite-wrapper` artifact provides the `SupportSQLite` compatibility API.

**Step 2: Convert Java entities to Kotlin**

The files live in `app/src/main/java/com/aistra/hail/utils/`. Convert each to Kotlin using Room 3.0 imports:

- `ActionEntity.java` — Maps to the `actions` table. Has a `@PrimaryKey` string `id` and a `launchPackage` column. Becomes:
  ```kotlin
  @Entity(tableName = "actions")
  data class ActionEntity(
      @PrimaryKey val id: String = "",
      val launchPackage: String = ""
  )
  ```
- `ActionDependencyEntity.java` — Maps to the `action_dependencies` table. Has a composite primary key, foreign key with cascade, and `ordering` column. Becomes:
  ```kotlin
  @Entity(
      tableName = "action_dependencies",
      primaryKeys = ["actionId", "packageName"],
      foreignKeys = [ForeignKey(
          entity = ActionEntity::class,
          parentColumns = ["id"],
          childColumns = ["actionId"],
          onDelete = ForeignKey.CASCADE
      )]
  )
  data class ActionDependencyEntity(
      val actionId: String = "",
      val packageName: String = "",
      val ordering: Int = 0
  )
  ```
- `AppMetadataEntity.java` — Maps to the `app_metadata` table. Has a `@PrimaryKey` string `packageName` plus 8 other columns. Becomes a `data class` with all fields and appropriate defaults.

**Step 3: Convert Java DAOs to Kotlin**

- `ActionDao.java` — Interface with `@Query`, `@Insert`, `@Delete` methods and a `@Transaction` default method `saveAction`. The default method becomes a Kotlin `default` method. Add `suspend` modifiers to functions that perform IO.
- `AppMetadataDao.java` — Interface with `@Query`, `@Insert`, `@Delete` methods and a `@Transaction` default method `replaceAll`. Same treatment as ActionDao.

**Step 4: Convert the database class**

- `AppMetadataDatabase.java` — Abstract `RoomDatabase` subclass at version 4. Update imports to `androidx.room3` and ensure the `entities` array references the converted Kotlin classes.

**Step 5: Update all callers**

The Java classes are used by `AppMetaCache.kt` and `ActionsRepository.kt`. Update:
- All imports from `androidx.room` to `androidx.room3`
- Entity instantiation from `ActionEntity().also { it.id = ... }` to `ActionEntity(id = ..., launchPackage = ...)`
- The `toEntry()` and `toEntity()` extension methods in `AppMetaCache.kt`
- Any direct `SupportSQLite` usage to use `roomDatabase.getSupportWrapper()` instead of `openHelper.writableDatabase`

**Step 6: Clean up**

Move the converted files to `app/src/main/kotlin/com/aistra/hail/utils/` and delete the Java originals from `app/src/main/java/com/aistra/hail/utils/`. Remove the old `annotationProcessor(libs.androidx.room.compiler)` dependency.

**SQL preservation:** The exact SQL queries must not change: `SELECT * FROM actions ORDER BY rowid`, `SELECT * FROM action_dependencies WHERE actionId = :actionId ORDER BY position`, `DELETE FROM action_dependencies WHERE actionId = :actionId`, `DELETE FROM actions WHERE id = :actionId`, `SELECT * FROM app_metadata`, `DELETE FROM app_metadata`, and `UPDATE app_metadata SET installed = 0`.

Verify by running `./gradlew :app:compileDebugKotlin :app:processDebugResources` and confirm KSP generates the `_Impl` classes.

### Milestone 2: Room Schema and Repository

This milestone adds the persistence layer for actions. At the end, the database can store and retrieve actions with their ordered dependency lists, and the existing app metadata cache is untouched.

Start by inspecting the current `AppMetadataDatabase` to understand the existing schema version, entity conventions, and DAO patterns. Add an `ActionEntity` with a stable string ID (a unique identifier that does not change for the life of the action), a launch package name, and timestamps if the existing conventions use them. Add an `ActionDependencyEntity` child table to store the ordered list of unfreeze package names for each action, with a foreign key back to the parent action that cascades on delete. Add DAO methods for observing all actions, inserting, updating, duplicating, and deleting. Expose these through a repository or use-case layer so that fragments never call DAOs directly. Increment the existing Room schema version and write an explicit migration that preserves the existing app metadata tables. If the checked-out branch does not yet include the metadata-cache work, integrate it first so the database version is consistent.

### Milestone 3: Action Executor

This milestone adds the shared logic that unfreezes dependencies and launches the target app. At the end, an action can be executed programmatically, and the result is success or a named failure.

Create a single executor class (for example, `ActionExecutor`) used by both the Actions screen and launcher shortcuts. The executor loads the action by ID, validates that the target and all dependencies are present, unfreezes each dependency sequentially (one at a time, off the main thread), verifies each one is unfrozen, then launches the target. If any dependency cannot be unfrozen or verified, do not launch the target and return a failure naming the unavailable app. Prevent duplicate execution while the same action is already running. Do not show a progress indicator. Do not automatically refreeze dependencies -- the existing auto-freeze behavior remains responsible for cleanup. Refresh auto-freeze service state after successful unfreezing, matching existing launch behavior.

### Milestone 4: Navigation and FAB Reconfiguration

This milestone restructures the app's navigation to accommodate the Actions tab and repurpose the Home FAB. At the end, the bottom navigation shows Home, Actions, and Settings in that order, and Apps is reachable only from Home.

Update `mobile_navigation.xml` to add an Actions destination immediately after Home. Update `nav_main.xml` to remove the Apps item and add an Actions item. In `MainActivity.kt`, configure the FAB centrally by destination: on Home, the FAB shows an Apps icon and opens the Apps destination; on Actions, the FAB shows a plus icon and opens the Create action dialog; on Apps and Settings, the FAB is hidden. Remove the Home fragment's direct freeze-FAB click handler without removing access to existing freeze operations (preserve an existing menu or action path for freezing). Treat Home, Actions, and Settings as top-level destinations for toolbar navigation. Treat Apps as a child destination with a back arrow that returns to Home through both toolbar back and system back.

### Milestone 5: Actions UI

This milestone builds the user-facing Actions screen, including the list, creation/editing dialog, app pickers, and long-press menu. At the end, a user can create, view, edit, duplicate, and delete actions through the UI.

Create `ActionsFragment.kt` with an adapter and view holder. Display actions as a vertical list of slim Material 3 list items with a small gap between rows. Each row shows the launch app's icon and name on the primary line, and the comma-separated unfreeze app names on a single secondary line that end-truncates with an ellipsis. Tapping a row executes the action. Long-pressing opens a menu with `Edit action`, `Create shortcut`, `Duplicate`, and `Delete`. The Create and Edit flows use a dialog with two fields in order: `Unfreeze` (multi-select picker, at least one required) and `Launch` (single-select picker, exactly one required). Both pickers show a single-line summary of selected app names with end ellipsizing. Save is disabled until both fields are valid. On Save only, remove the Launch package from the Unfreeze list if present, then deduplicate while preserving order. Buttons are exactly `Cancel` and `Save`. Delete shows a confirmation dialog with `Delete` and `Cancel`. Duplicate creates a new action with a new stable ID and the same package selections.

### Milestone 6: Pinned Shortcuts

This milestone wires the `Create shortcut` menu item to Android's pinned shortcut API. At a milestone, a user can place a home-screen shortcut that executes an action even when Hail is not open.

Use the existing `HShortcuts` utility. The shortcut's label is the launch app's display name, its icon is the launch app's application icon, and its ID is the action's stable ID. The intent targets Hail's action execution entry point and carries the action ID as an extra. Extend `HailApi` with a dedicated action intent constant and action-ID extra. Extend `ApiActivity` to load and execute the action through the shared executor, then finish. If the action is deleted or unavailable, the shortcut must fail gracefully with a localized `Action unavailable` message. When supported by Android/launcher APIs, refresh the shortcut's label and icon after editing the launch app.

### Milestone 7: Tests and Acceptance Validation

This milestone adds the test coverage that proves the feature works. At the end, there are unit tests for the Room DAO, the executor's edge cases, and the save-validation logic, plus a plan for manual UI verification.

Write Room DAO tests for insert, read, update, delete, and duplicate operations. Write save-validation tests for empty Unfreeze, empty Launch, and Launch-overlap scenarios. Write executor tests for already-unfrozen dependencies, sequential unfreezing, verification failure, and successful launch. Write shortcut intent tests confirming action-ID routing rather than ordinary package launch. For UI tests, cover field order, truncation, Save/Cancel behavior, long-press menu, and Home/Apps navigation. Manual verification on a device or emulator should confirm that tapping a card unfreezes dependencies before launching the target, that no progress indicator appears, and that failure prevents launch and identifies the failing app.

### Milestone 7: Convert Java Room Classes to Kotlin

The six Room persistence classes were written in Java by the implementing agent. The rest of the Hail codebase is Kotlin. This milestone converts those six files to Kotlin so the persistence layer matches the project's language convention.

The files live in `app/src/main/java/com/aistra/hail/utils/`. They are currently used by Kotlin code in `AppMetaCache.kt` and `ActionsRepository.kt`, which instantiate the Java entities directly (for example, `ActionEntity().also { it.id = ... }` and `AppMetadataEntity().also { it.packageName = ... }`). After conversion, these classes will become Kotlin `data class` types with immutable `val` properties, and callers will be updated to use the `copy()` constructor or named arguments instead of mutable property assignment.

The exact files to convert are:

- `ActionEntity.java` — Maps to the `actions` table. Has a `@PrimaryKey` string `id` and a `launchPackage` column. Becomes a `data class` with `val id: String` and `val launchPackage: String`.
- `ActionDependencyEntity.java` — Maps to the `action_dependencies` table. Has a composite primary key of `actionId` and `packageName`, a foreign key to `actions(id)` with cascading delete, and an `ordering` column. Becomes a `data class` with all three properties.
- `ActionDao.java` — Interface with `@Query` methods for loading all actions and loading dependencies by action ID, `@Insert` methods for upserting, `@Delete` methods for removing dependencies and actions, and a `@Transaction` default method `saveAction` that upserts the action, deletes old dependencies, and inserts new ones. The default method becomes a Kotlin `default` method in the interface.
- `AppMetadataEntity.java` — Maps to the `app_metadata` table. Has a `@PrimaryKey` string `packageName`, plus `name`, `systemApp`, `firstInstallTime`, `lastUpdateTime`, `flags`, `enabled`, `installed`, and `sourceSignature` columns. Becomes a `data class`.
- `AppMetadataDao.java` — Interface with `@Query` methods for loading all entries and marking all uninstalled, `@Insert` for bulk upsert, `@Delete` for clearing all, and a `@Transaction` default method `replaceAll` that deletes all then upserts. The default method becomes a Kotlin `default` method.
- `AppMetadataDatabase.java` — Abstract `RoomDatabase` subclass at version 4, declaring `appMetadataDao()` and `actionDao()` accessors. The `entities` array in `@Database` must continue to reference the converted Kotlin classes using `KotlinClass::class`.

After conversion, move the files to `app/src/main/kotlin/com/aistra/hail/utils/` and delete the Java originals from `app/src/main/java/com/aistra/hail/utils/`. Preserve all annotations (`@Entity`, `@Dao`, `@Database`, `@PrimaryKey`, `@ForeignKey`, `@Query`, `@Insert`, `@Delete`, `@Transaction`) and the exact table names, column names, and SQL queries. The SQL in particular must not change: `SELECT * FROM actions ORDER BY rowid`, `SELECT * FROM action_dependencies WHERE actionId = :actionId ORDER BY position`, `DELETE FROM action_dependencies WHERE actionId = :actionId`, `DELETE FROM actions WHERE id = :actionId`, `SELECT * FROM app_metadata`, `DELETE FROM app_metadata`, and `UPDATE app_metadata SET installed = 0`.

Update the callers in `AppMetaCache.kt` and `ActionsRepository.kt` to use the new Kotlin data classes. Replace patterns like `ActionEntity().also { it.id = ... }` with `ActionEntity(id = ..., launchPackage = ...)`. Update the `toEntry()` and `toEntity()` extension methods in `AppMetaCache.kt` to work with the new data class types.

Verify the build after conversion by running `./gradlew :app:compileDebugKotlin :app:processDebugResources` and confirm no behavior changes. The Room annotation processor (`kapt` or `annotationProcessor`) must successfully generate the `_Impl` classes from the Kotlin DAOs.

## Concrete Steps

State the exact commands to run and where to run them (working directory). When a command generates output, show a short expected transcript so the reader can compare. This section must be updated as work proceeds.

All commands assume the working directory is the repository root (`/workspaces/Hail`).

To inspect the current Room database version and entities:

    grep -r "version\s*=" app/src/main/kotlin --include="*.kt" | grep -i database

To build the debug APK after schema changes:

    ./gradlew :app:assembleDebug

Expected output ends with:

    BUILD SUCCESSFUL

To run unit tests:

    ./gradlew :app:testDebugUnitTest

Expected output ends with:

    BUILD SUCCESSFUL

To compile Kotlin only (faster iteration during schema or executor work):

    ./gradlew :app:compileDebugKotlin

To check for merge conflicts or whitespace issues before committing:

    git diff --check

Expected output is empty (no output means no issues).

To verify the navigation graph is well-formed:

    grep -n "fragment\|action\|destination" app/src/main/res/navigation/mobile_navigation.xml

## Validation and Acceptance

Describe how to start or exercise the system and what to observe. Phrase acceptance as behavior, with specific inputs and outputs.

After completing all milestones, build the debug APK and install it on a device or emulator. Launch Hail. The bottom navigation should show three tabs in order: Home, Actions, Settings. The Apps tab should not appear.

On the Home tab, the FAB should show an Apps icon. Tapping it should open the Apps screen, which displays a toolbar back arrow. Pressing the back arrow or the system back button should return to Home.

Switch to the Actions tab. The FAB should show a plus icon. Tapping it should open a dialog titled `Create action` with two fields in order: `Unfreeze` and `Launch`. Both fields should be empty initially, and the Save button should be disabled. Tapping the Unfreeze field should open a multi-select app picker; selecting at least one app and pressing OK should return to the dialog with a single-line summary. Tapping the Launch field should open a single-select app picker; selecting one app and pressing OK should show its name and icon. Only when both fields are filled should Save become enabled. Pressing Save should close the dialog and add a new row to the Actions list showing the launch app's icon, name, and the unfreeze app names on one line.

Tapping an action row should unfreeze each dependency app, then launch the target app. No progress indicator should appear. If a dependency cannot be unfrozen, the target should not launch, and a message naming the unavailable app should appear.

Long-pressing an action row should open a menu with `Edit action`, `Create shortcut`, `Duplicate`, and `Delete`. Edit should reopen the dialog with the action's current selections. Duplicate should add a new row with the same selections. Delete should show a confirmation dialog; confirming should remove the row.

Create a shortcut for an action, then close Hail. Tapping the shortcut from the home screen should unfreeze the dependencies and launch the target. Deleting the action and then tapping its shortcut should show an `Action unavailable` message.

Existing app launch, freeze, auto-freeze, and shortcut features must continue to work unchanged.

## Idempotence and Recovery

If steps can be repeated safely, say so. If a step is risky, provide a safe retry or rollback path.

Building and running tests are safe to repeat. Room migrations are additive and should not destroy data; if a migration fails, the fallback is to clear the app data (which resets the database to the latest schema). The 3-to-4 cleanup migration is specifically designed to repair devices that already received the invalid 2-to-3 migration. If a future migration fails, add a new corrective migration rather than modifying an existing one.

When editing navigation resources, validate the XML after each change by running `./gradlew :app:processDebugResources`. If the build fails, revert the last change and re-validate before trying a different approach.

## Artifacts and Notes

Include the most important transcripts, diffs, or snippets as indented examples. Keep them concise and focused on what proves success.

The official Material Symbols Outlined `automation` vector used for the Actions tab icon is fetched from:

    https://fonts.gstatic.com/render/v1/Material+Symbols+Outlined/24dp/automation.xml?var=opsz,wght,FILL,GRAD,ROND@24,400,0,0,50

The Room migration crash was caused by a mismatch between the migration script (which created `index_action_dependencies_actionId`) and the entity schema (which declared no index). The fix was to remove the index creation from the 2-to-3 migration and add a 3-to-4 cleanup migration that drops the index on devices that already received it.

## Interfaces and Dependencies

Be prescriptive. Name the libraries, modules, and services to use and why. Specify the types, traits/interfaces, and function signatures that must exist at the end of the milestone.

The following classes should exist after Milestone 7 converts the Java Room classes to Kotlin. These signatures match the current Java implementations exactly, preserving table names, column names, SQL queries, and behavior.

In the Room persistence layer (package `com.aistra.hail.utils`; migrated from Java to Kotlin using Room 3.0):

    @Entity(tableName = "actions")
    data class ActionEntity(
        @PrimaryKey val id: String,
        val launchPackage: String
    )

    @Entity(
        tableName = "action_dependencies",
        primaryKeys = ["actionId", "packageName"],
        foreignKeys = [ForeignKey(
            entity = ActionEntity::class,
            parentColumns = ["id"],
            childColumns = ["actionId"],
            onDelete = ForeignKey.CASCADE
        )]
    )
    data class ActionDependencyEntity(
        val actionId: String,
        val packageName: String,
        val ordering: Int
    )

    @Dao
    interface ActionDao {
        @Query("SELECT * FROM actions ORDER BY rowid")
        fun loadAll(): List<ActionEntity>

        @Query("SELECT * FROM action_dependencies WHERE actionId = :actionId ORDER BY position")
        fun loadDependencies(actionId: String): List<ActionDependencyEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun upsert(action: ActionEntity)

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun upsertDependencies(dependencies: List<ActionDependencyEntity>)

        @Query("DELETE FROM action_dependencies WHERE actionId = :actionId")
        fun deleteDependencies(actionId: String)

        @Query("DELETE FROM actions WHERE id = :actionId")
        fun delete(actionId: String)

        @Transaction
        default fun saveAction(action: ActionEntity, dependencies: List<ActionDependencyEntity>) {
            upsert(action)
            deleteDependencies(action.id)
            upsertDependencies(dependencies)
        }
    }

    @Entity(tableName = "app_metadata")
    data class AppMetadataEntity(
        @PrimaryKey val packageName: String,
        val name: String,
        val systemApp: Boolean,
        val firstInstallTime: Long,
        val lastUpdateTime: Long,
        val flags: Int,
        val enabled: Boolean,
        val installed: Boolean,
        val sourceSignature: String
    )

    @Dao
    interface AppMetadataDao {
        @Query("SELECT * FROM app_metadata")
        fun loadAll(): List<AppMetadataEntity>

        @Insert(onConflict = OnConflictStrategy.REPLACE)
        fun upsertAll(entries: List<AppMetadataEntity>)

        @Query("DELETE FROM app_metadata")
        fun deleteAll()

        @Query("UPDATE app_metadata SET installed = 0")
        fun markAllUninstalled()

        @Transaction
        default fun replaceAll(entries: List<AppMetadataEntity>) {
            deleteAll()
            upsertAll(entries)
        }
    }

    @Database(
        entities = [AppMetadataEntity::class, ActionEntity::class, ActionDependencyEntity::class],
        version = 4,
        exportSchema = false
    )
    abstract class AppMetadataDatabase : RoomDatabase() {
        abstract fun appMetadataDao(): AppMetadataDao
        abstract fun actionDao(): ActionDao
    }

The app uses Jetpack Room 3.0 for persistence (migrated from 2.8.3), Jetpack Navigation for routing, Material 3 for UI components, and Kotlin Coroutines with `Flow` for reactive data. Pinned shortcuts use `androidx.core.content.pm.ShortcutManagerCompat`. Room 3.0 requires KSP and uses the `androidx.room3` package namespace.

## Backlog: Backup and Restore

Out of scope for the initial Actions implementation, but reserved for a future branch. The future backup should export a versioned logical data format containing Home app selections, tags, pin and whitelist state, saved Actions with their ordered Unfreeze package lists, and relevant user preferences only if intentionally included. It should not include Room app metadata, cached labels, cached icons, install state, or other device-generated data.

Future restore requirements: validate the format version before changing local data; support merge, replace, and cancel conflict policies; preserve unavailable package names; preserve stable Action IDs when possible; detect duplicates without silently overwriting unrelated records; import HailData JSON and Room-backed Actions through one coordinated operation; apply all changes transactionally where possible; never overwrite existing data before validation succeeds; provide a summary of imported, skipped, conflicting, and unavailable entries. The export format should be independent of the raw Room schema so future Room migrations do not invalidate user backups.
