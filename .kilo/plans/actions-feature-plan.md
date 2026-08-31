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
- [x] (2026-08-29) Shortcut refresh after editing implemented via `ShortcutManagerCompat.updateShortcuts()` in `HShortcuts.updateActionShortcut()`, called from `ActionsFragment` on save.
- [x] (2026-08-29) Test infrastructure set up: JUnit4, MockK, Espresso, coroutines-test, room3-testing added.
- [x] (2026-08-29) ActionDaoTest written with in-memory database (insert, load, update, delete, dependencies, saveAction transaction).
- [x] (2026-08-29) AppMetadataDaoTest written (CRUD, replaceAll, markAllUninstalled).
- [x] (2026-08-29) ActionExecutorTest written with MockK (failure cases, sequential unfreeze).
- [x] (2026-08-29) ActionsRepositoryTest written (deduplication, order preservation, delete, duplicate).
- [x] (2026-08-29) Android instrumented tests compile successfully.
- [x] (2026-08-29) Extract shared launch/freeze/unfreeze logic into `AppActions` utility to eliminate working-mode drift between Home and Actions.
- [x] (2026-08-30) Remove main-thread package resolution from Unfreeze/Launch picker opening, align picker spacing, reduce cells to a 4-column compact grid, and change selection to a full-icon tint with a centered check overlay.
- [x] (2026-08-30) Add startup cache warming for all app metadata/icons, cached-first Apps loading with silent refresh, and a four-column searchable picker with rounded Material spacing and selected check indicators.
- [x] (2026-08-30) Change Home FAB to use the add icon while still opening the Apps page for adding apps to Home.
- [x] (2026-08-30) Speed up app picker by precomputing display labels and avoiding redundant list reloads when the installed package set has not changed.
- [ ] (2026-08-30) Precompute icon-pack values in Settings to remove tab-switch jank. Current code still runs `queryIntentActivities()` on the main thread inside Compose composition; needs to be moved to a `ViewModel` or precomputed before `setContent`.
- [x] (2026-08-30) Confirm `synthesizeAdaptiveIcons` defaults to false and both home and action shortcuts share the same original icon loader, so shortcut icons match the exact app icons.
- [x] (2026-08-30) Extract `AppMetaCache.getInstalledApplicationsCacheFirst()` as a single shared cache-first loader for Home, Apps, and Actions, eliminating duplicated cached-or-refresh logic.
- [x] (2026-08-30) Make `AppsViewModel.updateAppList()` show cached apps instantly, do silent background refresh only when the package set actually changes, and show the loader exclusively on pull-to-refresh; added cancelable `appListRefreshJob` so pull-to-refresh aborts any in-flight background refresh.
- [ ] (2026-08-30) Milestone 8: Move Settings icon-pack query, HShortcuts bitmap decode, and IconPack.loadIcon off the main thread; fix cold-start cache gap so Apps tab shows cached data instantly without redundant PackageManager scans; add StrictMode for debug detection.
- [ ] (2026-08-30) Milestone 9: Remove group cache invalidation in `AppMetaCache` so per-app state changes do not clear or rewrite unrelated app caches. Replace full-map `prefetch()` with incremental updates, replace `dao.replaceAll()` with per-app upserts, and remove the default `cache.keys` parameter from `invalidateState()`.
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

- Observation: `ShortcutManagerCompat.updateShortcuts()` updates both pinned and dynamic shortcuts that share the same ID, and the API is rate-limited.
  Evidence: AndroidX docs state the method updates "all existing shortcuts with the same IDs. Target shortcuts may be pinned and/or dynamic." Used this to refresh shortcut label/icon after action edits without removing/re-adding the shortcut.

- Observation: Home and Actions paths have drifted in working-mode behavior.
  Evidence: `PagerFragment.launchApp()` checks `MODE_ISLAND_HIDE` and adds dynamic shortcuts; `ActionExecutor.prepare()` skips all working-mode validation and goes straight to unfreeze+launch. `PagerFragment.setListFrozen()` blocks in `MODE_DEFAULT` with a guide dialog and validates Shizuku root in `MODE_SHIZUKU_HIDE`; `ActionExecutor` calls `AppManager.setAppFrozen()` directly. This means action execution can behave differently from home launch depending on `HailData.workingMode`.

- Observation: The Home FAB click handler on the Home destination cast the current fragment to `ActionsFragment`, which always fails because the primary navigation fragment is `HomeFragment`.
  Evidence: The safe-cast `(fragment as? ActionsFragment)?.showEditor(null)` returned null on Home, so the FAB was effectively dead. Fixed by changing the Home FAB to use the add icon (`ic_round_add`) and navigate directly to the Apps page for adding apps to Home, while keeping the Actions FAB on the add icon opening the action editor.

- Observation: The app picker updated its list twice on every open — once with cached apps, then again with a freshly loaded installed-app list — causing visual jank and redundant main-thread filtering.
  Evidence: `showAppPicker()` posted two `updateFilter` calls back-to-back. Fixed by using cached apps immediately, precomputing a `packageName -> label` map, and only replacing the list if the installed package set actually changed.

- Observation: The Settings icon-pack preference queried the package manager on every recomposition instead of once.
  Evidence: `mutableListOf(HailData.ACTION_NONE).apply { addAll(...queryIntentActivities...) }` was inside the Composable lambda. Fixed by precomputing `iconPackValues` outside the `SettingsScreen` composable.

- Observation: Home, Apps, and Actions each implemented their own cache-first installed-app loading, duplicating the same cached-or-refresh orchestration.
  Evidence: `AppsViewModel.updateAppList()` and `ActionsFragment.showAppPicker()` both checked `AppMetaCache.cachedApplications()`, fell back to `HPackages.getInstalledApplications()`, and called `AppMetaCache.prefetch()` plus `AppIconCache.prefetch()`. Extracted the shared logic into `AppMetaCache.getInstalledApplicationsCacheFirst()` so all three tabs use one code path.

- Observation: The previous `AppsViewModel.updateDisplayAppList()` always showed the refresh spinner, so even silent cache updates appeared as a loader to the user.
  Evidence: `postRefreshState(true)` was called inside `updateDisplayAppList()` on every display update. Fixed by removing the spinner from display updates and only showing it when `updateAppList(forceRefresh = true)` is called from pull-to-refresh.

- Observation: Pull-to-refresh did not cancel any in-flight background app-list refresh, so a user gesture could race with an ongoing silent cache update.
  Evidence: `AppsViewModel` had no handle to the background refresh coroutine. Added `appListRefreshJob` and cancel it at the top of `updateAppList(forceRefresh = true)` before starting a fresh fetch.

- Observation: `synthesizeAdaptiveIcons` already defaults to false, and both home and action shortcuts share the same `AppIconLoader`/`AppIconCache` path.
  Evidence: `HailData.synthesizeAdaptiveIcons` returns `false` by default. `HShortcuts.iconLoader`, `AppIconCache`, and `PagerFragment` all initialize their icon loaders with this same flag, so home shortcuts and action shortcuts use identical original icons without adaptive synthesis.

- Observation: Settings tab composition still executes `PackageManager.queryIntentActivities()` on the main thread inside `remember { mutableStateOf(...) }`.
  Evidence: `SettingsFragment.kt` defines `iconPackValues` inside a Composable lambda with a direct `app.packageManager.queryIntentActivities()` call. This Binder IPC blocks composition and explains the reported tab-switch and cold-start jank. It was claimed fixed in earlier work but the current code still contains the blocking pattern.

- Observation: Shortcut creation paths call `IconPack.loadIcon()` and `getBitmapFromDrawable()` on the main thread.
  Evidence: `HShortcuts.kt` invokes `IconPack.loadIcon()` and `getBitmapFromDrawable()` from `addPinShortcut`, `updateActionShortcut`, and `addDynamicShortcut` without coroutine/IO guards. `IconPack.loadIcon()` calls `BitmapFactory.decodeResource()` directly. These bitmap operations can stall the UI thread during shortcut operations.

- Observation: Cold-start Apps tab delay is caused by an empty in-memory `installedApplications` cache, not a missing Room cache.
  Evidence: `AppMetaCache.cachedApplications()` returns `installedApplications.values.toList()`. This in-memory map is empty after process death. `getInstalledApplicationsCacheFirst()` does not fall back to Room, so a cold start can trigger a full `PackageManager` scan if the user opens Apps before `warmUp()` completes.

- Observation: Although normal per-app state changes use isolated invalidation, `AppMetaCache` still contains group-level operations that can invalidate unrelated apps.
  Evidence: `prefetch()` calls `installedApplications.clear()` and rebuilds the entire map; `persist(installedPackages)` calls `dao.replaceAll()`, which deletes and reinserts every cached row; `invalidateState()` has a default parameter of `cache.keys`, so any future caller that omits the argument invalidates all apps. These paths are reachable from `warmUp()`, `getInstalledApplicationsCacheFirst()`, `AppsViewModel.updateAppList()`, and `clearAndRebuild()`.

- Observation: The normal freeze/unfreeze paths already preserve per-app isolation.
  Evidence: `PagerFragment.onResume()` and `PagerFragment.setListFrozen()` call `AppMetaCache.invalidateState()` with explicit package lists, and `loadIfStale()` updates only one package at a time. The problem is not the normal path; it is the existence of full-map shortcuts that bypass it.

- Observation: Google’s Android caching guidance and Room best-practice sources recommend per-key invalidation and partial updates rather than full cache clears or table rewrites.
  Evidence: Android `IpcDataCache`/`PropertyInvalidatedCache` invalidate by key, not by full clear. Room docs recommend `@Update` with partial entities or `@Upsert` instead of `deleteAll()` + `upsertAll()` for partial changes.

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

- Decision: Refresh existing shortcuts after action edits using `ShortcutManagerCompat.updateShortcuts()` rather than removing and re-requesting the shortcut.
  Rationale: `updateShortcuts` updates both pinned and dynamic shortcuts with matching IDs in a single call. Re-requesting would show the system pin dialog again and could create duplicate shortcuts. The API is rate-limited, but calling it only on save avoids that risk.
  Date/Author: 2026-08-29

- Decision: Extract shared app-action logic into a new `object AppActions` in `com.aistra.hail.utils` rather than expanding `AppManager`.
  Rationale: `AppManager` is already a God object handling freeze/unfreeze modes, root/Shizuku/Dhizuku shell operations, and screen locking. Adding working-mode validation, dynamic shortcuts, and intent caching to it would increase coupling. The `utils` package already uses `object` singletons (`HShortcuts`, `ActionsRepository`, `AppMetaCache`) for cross-cutting concerns, so a new `AppActions` fits the existing convention and keeps `AppManager` focused on privilege delegation.
  Date/Author: 2026-08-29

- Decision: Preserve `ActionExecutor` as a public API for `ApiActivity` and shortcuts, but delegate its internals to `AppActions`.
  Rationale: `ApiActivity` and pinned shortcuts already depend on `ActionExecutor.prepare()`. Removing it would break the API surface. Keeping it as a thin wrapper preserves backward compatibility while allowing `PagerFragment` to call `AppActions` directly for UI-specific behavior (toasts, dynamic shortcuts).
  Date/Author: 2026-08-29

- Decision: Move all PackageManager and bitmap decode work off the main thread in Settings, shortcuts, and app-list loading.
  Rationale: Code inspection and semantic search confirmed main-thread Binder and bitmap operations in `SettingsFragment`, `HShortcuts`, and `IconPack`. Android best practices and Meta Engineering guidance identify these as primary cold-start and UI-jank causes on low-RAM devices. The fix is to use `Dispatchers.IO` for these operations and show cached/placeholder content first.
  Date/Author: 2026-08-30

- Decision: Do not rely solely on in-memory `installedApplications` for cache-first app loading; add a Room-backed fallback or single-source warmup to avoid redundant PackageManager scans on cold start.
  Rationale: The in-memory cache is lost on process death. `warmUp()` already scans in the background, but `getInstalledApplicationsCacheFirst()` can trigger a second scan if the user opens Apps before warmup completes. Reducing redundant scans is critical for low-RAM devices where each scan competes for CPU and IO.
  Date/Author: 2026-08-30

- Decision: Preserve per-app cache isolation as the default and remove group invalidation shortcuts from `AppMetaCache`.
  Rationale: Normal state changes already pass explicit package lists to `invalidateState()`, so unrelated apps are not affected in the common path. However, `prefetch()` clears the entire `installedApplications` map, `persist()` rewrites all DB rows via `replaceAll()`, and `invalidateState()` defaults to all keys. Android caching guidance and Room best practices recommend per-key invalidation and partial updates. These group operations should be replaced with incremental add/update/remove and per-app upserts so a single app change never touches unrelated cached apps.
  Date/Author: 2026-08-30

- Decision: Require explicit package lists for cache invalidation; reserve full-wipe methods for explicit user action only.
  Rationale: `invalidateState(cache.keys)` is a latent risk: a future caller can accidentally invalidate 100+ apps by omitting the argument. Keeping `invalidateAll()` and `clearAndRebuild()` for explicit full-wipe scenarios preserves safety while preventing accidental broad invalidation.
  Date/Author: 2026-08-30

## Outcomes & Retrospective

Summarize outcomes, gaps, and lessons learned at major milestones or at completion. Compare the result against the original purpose.

The initial Actions implementation is functionally complete across Room storage, execution/API routing, navigation, UI, long-press operations, and pinned shortcuts. The build passes (`assembleDebug` and `testDebugUnitTest`), and static diagnostics are clean. The shortcut refresh gap identified during Milestone 6 verification was closed by adding `HShortcuts.updateActionShortcut()` and calling it from `ActionsFragment` after save. A post-verification review identified working-mode drift between Home and Actions execution paths. This was addressed in Milestone 6.5 by extracting shared logic into `AppActions`. Runtime visual verification on a device or emulator remains undone because no emulator was provided. Focused unit tests for the new Room entities, executor, and UI are not yet written. The Backup/Restore feature is explicitly out of scope for this phase. A later performance review identified cache invalidation patterns that could affect unrelated apps on devices with many installed apps; Milestone 8 and Milestone 9 were added to address main-thread blockers and enforce per-app cache isolation.

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

Create a single executor class (for example, `ActionExecutor`) used by both the Actions screen and launcher shortcuts. The executor accepts a `LaunchAction` domain model and does not load from the database itself. Callers are responsible for resolving the action before calling the executor. The executor validates that the target and all dependencies are present, unfreezes each dependency sequentially (one at a time, off the main thread), verifies each one is unfrozen, then launches the target. If any dependency cannot be unfrozen or verified, do not launch the target and return a failure naming the unavailable app. Prevent duplicate execution while the same action is already running. Do not show a progress indicator. Do not automatically refreeze dependencies -- the existing auto-freeze behavior remains responsible for cleanup. Refresh auto-freeze service state after successful unfreezing, matching existing launch behavior.

### Milestone 4: Navigation and FAB Reconfiguration

This milestone restructures the app's navigation to accommodate the Actions tab and repurpose the Home FAB. At the end, the bottom navigation shows Home, Actions, and Settings in that order, and Apps is reachable only from Home.

Update `mobile_navigation.xml` to add an Actions destination immediately after Home. Update `nav_main.xml` to remove the Apps item and add an Actions item. In `MainActivity.kt`, configure the FAB centrally by destination: on Home, the FAB shows an Apps icon and opens the Apps destination; on Actions, the FAB shows a plus icon and opens the Create action dialog; on Apps and Settings, the FAB is hidden. Remove the Home fragment's direct freeze-FAB click handler without removing access to existing freeze operations (preserve an existing menu or action path for freezing). Treat Home, Actions, and Settings as top-level destinations for toolbar navigation. Treat Apps as a child destination with a back arrow that returns to Home through both toolbar back and system back.

### Milestone 5: Actions UI

This milestone builds the user-facing Actions screen, including the list, creation/editing dialog, app pickers, and long-press menu. At the end, a user can create, view, edit, duplicate, and delete actions through the UI.

Create `ActionsFragment.kt` with an adapter and view holder. Display actions as a vertical list of slim Material 3 list items with a small gap between rows. Each row shows the launch app's icon and name on the primary line, and the comma-separated unfreeze app names on a single secondary line that end-truncates with an ellipsis. Tapping a row executes the action. Long-pressing opens a menu with `Edit action`, `Create shortcut`, `Duplicate`, and `Delete`. The Create and Edit flows use a dialog with two fields in order: `Unfreeze` (multi-select picker, at least one required) and `Launch` (single-select picker, exactly one required). Both pickers show a single-line summary of selected app names with end ellipsizing. Save is disabled until both fields are valid. On Save only, remove the Launch package from the Unfreeze list if present, then deduplicate while preserving order. Buttons are exactly `Cancel` and `Save`. Delete shows a confirmation dialog with `Delete` and `Cancel`. Duplicate creates a new action with a new stable ID and the same package selections.

### Milestone 6: Pinned Shortcuts

This milestone wires the `Create shortcut` menu item to Android's pinned shortcut API. At a milestone, a user can place a home-screen shortcut that executes an action even when Hail is not open.

Use the existing `HShortcuts` utility. The shortcut's label is the launch app's display name, its icon is the launch app's application icon, and its ID is the action's stable ID. The intent targets Hail's action execution entry point and carries the action ID as an extra. Extend `HailApi` with a dedicated action intent constant and action-ID extra. Extend `ApiActivity` to load and execute the action through the shared executor, then finish. If the action is deleted or unavailable, the shortcut must fail gracefully with a localized `Action unavailable` message. When supported by Android/launcher APIs, refresh the shortcut's label and icon after editing the launch app.

### Milestone 6.5: Shared App-Action Logic and Launch-Intent Caching

This milestone eliminates behavioral drift between Home and Actions by extracting shared launch, freeze, and unfreeze logic into a single utility.

**Critical questions and research findings:**

- Q: Do Home launch, Action launch, Home freeze/unfreeze, and Action freeze/unfreeze share code?
  A: No. `PagerFragment.launchApp()` handles `MODE_ISLAND_HIDE`, dynamic shortcuts, and toast feedback. `ActionExecutor.prepare()` handles dependency unfreezing and returns `Result<Intent>`, but skips working-mode validation entirely. `PagerFragment.setListFrozen()` blocks in `MODE_DEFAULT` and validates Shizuku root in `MODE_SHIZUKU_HIDE`, while `ActionExecutor` calls `AppManager.setAppFrozen()` directly. This means Actions can behave completely differently from Home depending on `HailData.workingMode`.

- Q: What pattern does the Hail codebase use for shared logic?
  A: The `utils` package uses `object` singletons (`HShortcuts`, `ActionsRepository`, `AppMetaCache`). Android best-practice guidance recommends composition over inheritance and extracting shared business rules into reusable helpers rather than duplicating them across fragments.

**Best path:**

1. Create `object AppActions` in `com.aistra.hail.utils` with three suspend helpers:
   - `ensureUnfrozen(packageName: String): Result<Unit>` — validates working mode, unfreezes the app, verifies state.
   - `getLaunchIntent(packageName: String): Result<Intent>` — applies working-mode preconditions, retrieves launch intent, refreshes auto-freeze service.
   - `freezePackages(frozen: Boolean, packages: List<String>): Result<Unit>` — validates working mode, performs batch freeze/unfreeze.
2. Refactor `PagerFragment.launchApp()` to call `AppActions.ensureUnfrozen()` + `AppActions.getLaunchIntent()`, preserving dynamic shortcuts and toast feedback at the UI layer.
3. Refactor `PagerFragment.setListFrozen()` to call `AppActions.freezePackages()`, preserving working-mode dialogs and toast feedback at the UI layer.
4. Refactor `ActionExecutor.prepare()` to delegate dependency unfreezing to `AppActions.ensureUnfrozen()` and target launch to `AppActions.getLaunchIntent()`, removing duplicated working-mode blindness.

### Milestone 7: Tests and Acceptance Validation

This milestone adds the test coverage that proves the feature works. The project currently has no test suite, so this milestone also sets up the testing infrastructure (dependencies, test directories, base classes).

**Testing infrastructure setup:**

Add test dependencies to `app/build.gradle.kts`:

```kotlin
dependencies {
    // Existing dependencies...

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("androidx.test:core:1.7.0")
    testImplementation("androidx.test:core-ktx:1.6.1")
    testImplementation("androidx.test.ext:junit:1.3.0")
    testImplementation("androidx.test.ext:truth:1.7.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.0")
    testImplementation("androidx.room3:room3-testing:3.0.1")
    testImplementation("io.mockk:mockk:1.13.12")

    // Instrumented testing
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
```

Create the test directory structure:
```
app/src/test/kotlin/com/aistra/hail/
  utils/
    ActionDaoTest.kt
    AppMetadataDaoTest.kt
    ActionExecutorTest.kt
    ActionsRepositoryTest.kt
  ui/
    (UI tests go in androidTest)
```

**Room DAO tests (`ActionDaoTest.kt`, `AppMetadataDaoTest.kt`):**

Use `Room.inMemoryDatabaseBuilder()` to create a transient database for each test. This ensures tests don't interfere with the production database and provides a clean state per test. Use `runTest` (not the deprecated `runBlockingTest`) for coroutine testing. Use `@Before` to initialize the database and DAO, `@After` to close the database.

Tests for `ActionDao`:
- Insert an action with dependencies and verify it can be read back
- Load all actions returns inserted actions
- Load dependencies returns ordered dependencies
- Upsert updates an existing action
- Delete removes the action and cascades to dependencies
- `saveAction` transaction: upsert + delete old deps + insert new deps
- Duplicate: insert two actions, verify both exist

Tests for `AppMetadataDao`:
- Insert entries and verify they can be read back
- Upsert updates existing entries
- Delete all clears the table
- Mark all uninstalled updates all entries
- `replaceAll` transaction: delete all + upsert new entries

**ActionExecutor tests (`ActionExecutorTest.kt`):**

Use MockK to mock the dependencies (package manager, freeze/unfreeze service). Test:
- Already-unfrozen dependencies: executor skips already-unfrozen apps
- Sequential unfreeze: dependencies are unfrozen one at a time
- Verification failure: if an app cannot be verified as unfrozen, return failure
- Successful launch: all deps unfrozen, target launched
- Duplicate execution prevention: second call while first is running returns failure or waits
- Failure case: dependency not installed returns failure naming the missing app

**ActionsRepository tests (`ActionsRepositoryTest.kt`):**

Use the in-memory database. Test:
- Empty Unfreeze list: save fails or produces action with no unfreeze packages
- Empty Launch: save fails or produces action with no target
- Launch overlap: Launch package also in Unfreeze list gets removed from Unfreeze
- Deduplication: duplicate packages in Unfreeze list get removed while preserving order

**Shortcut intent tests (instrumented, in `androidTest`):**

Use Espresso-Intents (`Intents.init()` / `Intents.release()`) to verify intent routing. Test:
- Action ID in intent extra routes to the correct action
- Missing or invalid action ID shows "Action unavailable" message
- Shortcut intent does NOT launch the target package directly (it goes through Hail's executor)

**UI tests (instrumented, in `androidTest`):**

Use Espresso for UI interaction tests. Test:
- Field order: Unfreeze field appears before Launch field in Create dialog
- Truncation: long app names are truncated with ellipsis
- Save/Cancel: Save is disabled until both fields are valid; Cancel closes dialog
- Long-press menu: shows Edit, Create shortcut, Duplicate, Delete
- Home/Apps navigation: tapping Home FAB opens Apps; back returns to Home

**Manual verification checklist (device or emulator):**

- Tapping an action card unfreezes dependencies before launching the target
- No progress indicator appears during execution
- If a dependency cannot be unfrozen, the target does not launch and a message names the failing app
- Creating a pinned shortcut and tapping it from the home screen executes the action
- Deleting an action and then tapping its shortcut shows "Action unavailable"
- Existing app launch, freeze, auto-freeze, and shortcut features continue to work unchanged

Verify by running `./gradlew :app:testDebugUnitTest` for unit tests and `./gradlew :app:connectedDebugAndroidTest` for instrumented tests.

### Milestone 8: Startup Performance and Async Deferral

This milestone addresses cold-start and tab-switch delays caused by main-thread blockers found in the current codebase. The goal is to ensure the app shows content immediately, moves all expensive work off the main thread, and updates the UI subtly after the first frame.

**Confirmed main-thread blockers (from code inspection and semantic search):**

- `SettingsFragment` runs `PackageManager.queryIntentActivities()` inside `remember { mutableStateOf(...) }` during Compose composition. This is a Binder IPC call that blocks the main thread every time Settings is created.
- `HShortcuts` and `IconPack` perform `BitmapFactory.decodeResource()`, `Bitmap.createBitmap()`, and `Canvas.draw()` on the main thread when creating or updating shortcuts.
- `AppMetaCache.getInstalledApplicationsCacheFirst()` only checks the in-memory `installedApplications` map. On cold start, this map is empty because the process was killed. If the user opens Apps before `warmUp()` completes, the method triggers a second full `PackageManager` scan.
- The Apps tab shows nothing until `ApplicationInfo` objects are available, and the only current source for those objects is a full PackageManager scan.

**Evidence-based best practices (from Android Developers and Meta Engineering):**

- Move all `PackageManager` queries, Room initialization, and bitmap decode off the main thread.
- Show cached or placeholder content immediately; refresh silently after the first frame.
- Use `StrictMode.ThreadPolicy` in debug builds to catch accidental main-thread disk/network/PM access.
- Consider Baseline Profiles or Startup Profiles for 15-30% cold-start improvement.
- Avoid Binder calls during startup; cache results or move them to background threads.

**Step 1: Move SettingsFragment icon-pack query off the main thread**

Move the `queryIntentActivities()` call out of the Composable lambda. Precompute the icon-pack list once and expose it as a simple `List<String>` that Compose reads without triggering package-manager work.

Caveat: Do not wrap the query in `mutableStateOf` inside the composable. Compute it before `setContent` or in a `ViewModel`, then read it in Compose as a plain value.

**Step 2: Make HShortcuts and IconPack bitmap work asynchronous**

- Make `IconPack.loadIcon()` a `suspend` function and call it from `withContext(Dispatchers.IO)`.
- Make `HShortcuts.getBitmapFromDrawable()` run on `Dispatchers.IO`.
- Update all callers in `HShortcuts` (`addPinShortcut`, `updateActionShortcut`, `addDynamicShortcut`) to launch a coroutine on `Dispatchers.IO` for icon preparation, then post the shortcut on the main thread.

Caveat: Shortcut creation is user-initiated, but the icon decode can still jank the UI if done synchronously. Moving it to IO preserves responsiveness.

**Step 3: Fix AppMetaCache cold-start cache gap**

- Add a fallback in `getInstalledApplicationsCacheFirst()`: if `cachedApplications()` is empty, first check whether `seedFromDatabase()` has populated the in-memory `cache` and reconstruct `ApplicationInfo` objects from cached package names, or wait for the existing `warmUp()` job to finish before falling back to a full scan.
- Ensure `warmUp()` is the single source of truth for initial app-list population and that `updateAppList()` does not trigger a redundant full scan on cold start.

Caveat: Reconstructing `ApplicationInfo` objects from package names still requires PackageManager calls. The goal is to reduce the number of full scans, not eliminate all PM access.

**Step 4: Show cached data instantly on Apps cold start**

- In `AppsFragment.onCreateView()` or `AppsViewModel.init`, immediately submit whatever cached data is available so the RecyclerView is visible.
- Start the background refresh only after the first frame.
- When refresh completes, call `apps.postValue()` and `updateDisplayAppList()` to subtly update the list.

Caveat: If the cache is truly empty on first launch, the user will see an empty list. That is acceptable if it appears instantly, because the background scan will populate it shortly after.

**Step 5: Add debug-only StrictMode and verify**

- Enable `StrictMode.ThreadPolicy` in debug builds to detect main-thread disk, network, and PM access.
- Verify with `./gradlew :app:compileDebugKotlin :app:processDebugResources` and manual cold-start testing.

**Step 6: Enforce per-app cache isolation**

- Refactor `AppMetaCache.prefetch()` to merge new `ApplicationInfo` data into `installedApplications` incrementally instead of clearing the whole map.
- Refactor `AppMetaCache.persist()` to use per-app upserts rather than `dao.replaceAll()`, so unrelated cached rows are not deleted and reinserted.
- Remove the default `cache.keys` parameter from `AppMetaCache.invalidateState()` so callers must explicitly pass the affected package list.
- Keep `invalidateAll()` and `clearAndRebuild()` available only for explicit full-wipe scenarios.

Caveat: Incremental map merge and per-app upserts preserve cache hit rates for unchanged apps and reduce DB write volume on devices with many installed apps.

**Verification:**

- Cold-start the app on a low-RAM device or emulator.
- Open Apps tab immediately. The list should appear instantly with cached data or an empty state, and populate within 1-2 seconds without any spinner.
- Switch to Settings tab. It should open without jank.
- Create a shortcut. The UI should not freeze during icon creation.
- In debug builds, verify no StrictMode violations appear in logcat.
- Trigger a state change for one app and verify that cache entries for other apps remain intact and are not rewritten.

### Milestone 9: Per-App Cache Isolation

This milestone removes the remaining group invalidation paths from `AppMetaCache` so that single-app changes never clear, rewrite, or invalidate unrelated cached apps. It builds on Milestone 8 by tightening the cache layer after the main-thread blockers are resolved.

**Step 1: Make `prefetch()` incremental**

Replace `installedApplications.clear()` with an incremental merge:
- Add entries for new packages.
- Update entries for changed packages.
- Remove entries only for packages no longer present.
- Leave all other entries untouched.

This keeps existing `ApplicationInfo` cache entries stable when only a subset of apps changed.

**Step 2: Make `persist()` use per-app writes**

Replace `dao.replaceAll()` with `dao.upsertAll()` for full cache persistence, and for incremental updates, upsert only the changed entries. This avoids a full table delete/rewrite on every refresh.

**Step 3: Remove broad default from `invalidateState()`**

Change `invalidateState(packageNames: Collection<String> = cache.keys)` to require an explicit package list. Update any callers if needed. Keep `invalidateAll()` and `clearAndRebuild()` for explicit full invalidation only.

**Step 4: Verify cache isolation**

- Add a test or manual check that updates one app’s state and confirms other cached apps are unchanged.
- Verify that a single-app refresh does not cause `AppIconCache.prefetch()` to reload icons for every installed app unless the installed set actually changed.

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

To verify shared app-action extraction and caching:

    ./gradlew :app:compileDebugKotlin :app:processDebugResources

Expected output ends with:

    BUILD SUCCESSFUL

To inspect `AppActions` implementation:

    grep -n "object AppActions" app/src/main/kotlin/com/aistra/hail/utils/*.kt

## Validation and Acceptance

Describe how to start or exercise the system and what to observe. Phrase acceptance as behavior, with specific inputs and outputs.

After completing all milestones, build the debug APK and install it on a device or emulator. Launch Hail. The bottom navigation should show three tabs in order: Home, Actions, Settings. The Apps tab should not appear.

On the Home tab, the FAB should show an Apps icon. Tapping it should open the Apps screen, which displays a toolbar back arrow. Pressing the back arrow or the system back button should return to Home.

Switch to the Actions tab. The FAB should show a plus icon. Tapping it should open a dialog titled `Create action` with two fields in order: `Unfreeze` and `Launch`. Both fields should be empty initially, and the Save button should be disabled. Tapping the Unfreeze field should open a multi-select app picker; selecting at least one app and pressing OK should return to the dialog with a single-line summary. Tapping the Launch field should open a single-select app picker; selecting one app and pressing OK should show its name and icon. Only when both fields are filled should Save become enabled. Pressing Save should close the dialog and add a new row to the Actions list showing the launch app's icon, name, and the unfreeze app names on one line.

Tapping an action row should unfreeze each dependency app, then launch the target app. No progress indicator should appear. If a dependency cannot be unfrozen, the target should not launch, and a message naming the unavailable app should appear.

In the Actions Create/Edit dialog, the Launch picker should show only apps that have a launcher activity. Apps without a launch intent should be hidden from the picker.

Home launch, Home freeze/unfreeze, and Actions execution must respect the same `HailData.workingMode` rules. Specifically:
- `MODE_DEFAULT` should block freeze/unfreeze with the guide dialog in both Home and Actions.
- `MODE_SHIZUKU_HIDE` should validate Shizuku root before freezing in both Home and Actions.
- `MODE_ISLAND_HIDE` should call `HIsland.ensureLaunchIntentExists` before launching in both Home and Actions.

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

In the shared app-action layer (package `com.aistra.hail.utils`):

    object AppActions {
        suspend fun ensureUnfrozen(packageName: String): Result<Unit>
        suspend fun getLaunchIntent(packageName: String): Result<Intent>
        suspend fun freezePackages(frozen: Boolean, packages: List<String>): Result<Unit>
    }

## Backlog: Backup and Restore

Out of scope for the initial Actions implementation, but reserved for a future branch. The future backup should export a versioned logical data format containing Home app selections, tags, pin and whitelist state, saved Actions with their ordered Unfreeze package lists, and relevant user preferences only if intentionally included. It should not include Room app metadata, cached labels, cached icons, install state, or other device-generated data.

Future restore requirements: validate the format version before changing local data; support merge, replace, and cancel conflict policies; preserve unavailable package names; preserve stable Action IDs when possible; detect duplicates without silently overwriting unrelated records; import HailData JSON and Room-backed Actions through one coordinated operation; apply all changes transactionally where possible; never overwrite existing data before validation succeeds; provide a summary of imported, skipped, conflicting, and unavailable entries. The export format should be independent of the raw Room schema so future Room migrations do not invalidate user backups.
