# App Metadata Cache Plan

## Goal

Remove repeated `PackageManager` work from home and all-apps filtering, sorting, and binding. The UI path should read from an in-memory snapshot and only perform background refreshes explicitly. Icons should also get a disk-backed tier so cold starts do not feel laggy after process death or app restart.

The cache must not hide work inside ordinary property getters. Loading must be explicit, batched, and owned by the ViewModel or screen lifecycle. This keeps sorting, filtering, and binding deterministic, avoids duplicate work from `DiffUtil` and comparators, and prevents getter-based UI work from becoming a hidden hot path.

## Final recommendation

### Use JSON file snapshot + in-memory cache, not a database for now

This feature is not a relational system. It is a package metadata cache keyed by package name, with a refresh loop and a small persisted snapshot for warm starts. That means the best tool is a simple, versioned immutable snapshot under `filesDir/v1/` plus a bounded in-memory map.

Recommended structure:

1. In-memory map: fast point reads, keyed by package name, with per-package lock or single-writer coordination.
2. Disk snapshot: `v1/app_meta.json`, written atomically as `app_meta.json.tmp` then renamed.
3. No database for the first implementation: the cache is not a query-heavy dataset, and there are no relational joins or migrations in the current use case.

Why not a database:

- `Room` adds dependency, schema, migration, and maintenance overhead without improving the hot-path operation.
- `DataStore` and key-value stores are better for preferences, not app-index metadata.
- SQLite or Room only helps if we expect future metadata queries, history, package inventory, multi-user records, or complex durable app data.
- The critical requirement is a fast lookup and a refresh policy, not query flexibility.

This is the correct tradeoff for Hail right now.

### If we later need a DB, Room is the best option

Room is still the best database choice if the app later needs durable relational metadata or schema-based indexing. But that should be a deliberate future enhancement, not part of the initial optimization. The hot path remains a memory map, and the cache should still be designed as a repository boundary rather than a database-driven UI model.

If Room is added later, it should be used as a batch import/export layer, not as a per-row read during binding.

## Disk icon cache decision

Use the existing `AppIconCache` memory `LruCache` as the first tier, then add a disk tier under `filesDir/v1/icons` with content-addressed or signature-based filenames. This gives the following flow:

1. Memory hit: display immediately.
2. Disk hit: decode asynchronously and show it when ready.
3. Source load: generate icon, place in memory, then write to disk asynchronously.

Key requirements:

- Key must include package name, user ID, icon size, package signature/update signature, icon-pack identity, and adaptive-icon rendering mode.
- Never persist grayscale output; grayscale is presentation state and can be applied at bind time.
- Use atomic writes to a temp file and rename to avoid corruption during process death.
- Keep disk writes background-only and bounded.
- Ignore corrupted or incompatible icon files and fall back to loader.
- Clear stale entries when icon pack or adaptive icon settings change.

The app should not preload every installed icon on startup. Instead, warm only the checked/visible package set in the background to avoid cold-start lag.

## Codebase findings and fixes

### Hot paths identified

These were the real cost centers:

- `AppInfo.name` and `AppInfo.state` were doing work in getters.
- `AppsViewModel.filterList()` repeatedly called package manager, label, and state checks while sorting/filtering.
- `PagerFragment` and adapter binding paths were re-reading state and label values while rendering list rows.
- The same package metadata was being reloaded across comparators, diff operations, and adapter binds.

### Correct design pattern

The actual fix is to move work to explicit prefetch steps:

- `AppMetaCache.prefetch(...)` loads metadata in the background.
- `AppMetaCache.revision` signals a refresh when data has changed.
- The UI rebuilds from the in-memory snapshot after prefetch, rather than calling package manager methods directly during render.

This pattern is already reflected in the worktree code: the cache keeps a `ConcurrentHashMap`, per-package mutex, atomic persistence, and a revision state flow.

## Data model

Separate static metadata from runtime state.

Recommended model:

```text
AppMetadata
- packageName
- name
- isSystemApp
- firstInstallTime
- lastUpdateTime
- flags
- enabled
- sourceSignature

AppState
- NOT_FOUND
- UNFROZEN
- FROZEN
- working mode signature
```

Important rule:

- `AppMetadata` is durable snapshot data and can be persisted.
- `AppState` is runtime state and must be invalidated on working-mode changes and freeze operations.
- `state` should never be treated as durable package metadata.

## Implementation methods

### 1. Metadata cache layer

Create/keep a cache object along the lines of `AppMetaCache`:

- Keep `cache` as an in-memory `ConcurrentHashMap<String, Entry>`.
- Use per-package locks so concurrent refreshes do not race.
- Use a `Mutex` for atomic persistence writes.
- Seed from disk on startup with `seedFromDisk()`.
- Refresh packages using `prefetchPackages(...)` or `prefetch(...)` on `Dispatchers.IO`.
- Publish new data via `MutableStateFlow<Long>` revision, then trigger a UI recompute on the main thread.

The implementation should also guard against stale package entries and partial writes by validating cached data before reusing it.

### 2. Persistence strategy

Use the existing JSON snapshot pattern:

- `v1/app_meta.json`
- write to `app_meta.json.tmp`
- rename after successful file write
- ignore corrupted or incompatible versions
- keep file format version as a validation gate

This is the correct lightweight persistence for this app and avoids a large migration burden.

### 3. State invalidation strategy

State invalidation should happen when:

- working mode changes
- freeze/unfreeze succeeds
- app resumes after external system changes
- package install/uninstall or version changes are detected

`AppMetaCache.invalidateState(...)` is correct and should be called from the owner boundary, not hidden inside UI code.

### 4. UI refresh strategy

The UI should:

- request prefetch for visible packages before rendering the list
- rebuild list content once metadata revision changes
- use stable sorting/tie-breakers for equal labels and timestamps
- avoid reading live package-manager state inside adapter binding or comparator logic

This avoids the repeated hot path work that originally made list rendering slow.

### 5. Icon cache behavior

The disk icon cache should be layered on top of the memory cache and existing `AppIconLoader` pipeline. The current implementation already does this correctly in principle:

- memory lookup first
- disk lookup second
- source loader fallback
- disk write after generation
- view tag validation to avoid stale icon binding to recycled rows

The important part is to keep I/O off the main thread and never block first-frame rendering while disk icons are being read.

## Why this is the best choice

Compared to Room or a heavier database approach:

- Simpler to implement and reason about
- Smaller dependency and maintenance cost
- Faster startup and easier debugging
- Better match to package index semantics and app state lifecycle
- Easy to atomic-write and recover from partial persistence

A database becomes worth it only when we need rich queries, historical data, or multi-dimensional package metadata. This feature does not.

## Validation plan

The implementation should be validated with focused checks:

1. Concurrent requests for the same package should collapse to one refresh.
2. Batch prefetch should continue when a single package fails.
3. Corrupt or stale snapshot files should be ignored safely.
4. Disk icon cache should not block UI rendering and must avoid stale view binding.
5. State invalidation must happen when freeze mode changes.

The current implementation in the feature worktree already matches this direction, and the Kotlin compile succeeded in the correct worktree with the Java/Android env configured, which confirms the core approach is viable.

## Final decision

Use:

- Memory cache for hot reads
- JSON snapshot file for warm startup persistence
- Disk-backed icon cache for cold-start icon responsiveness
- Room only if future requirements demand durable relational metadata

Do not introduce a database as part of this optimization unless a concrete future requirement justifies it. The cost/benefit ratio does not support it for the current app metadata and icon cache use case.
5. Working-mode changes invalidate state but retain valid static labels.
6. Package version changes invalidate only the affected static entry.
7. Revision events are delivered on the main dispatcher and obsolete requests cannot overwrite newer results.
8. Filtering and sorting use one metadata snapshot and make zero package-manager calls after prefetch.
9. A disk-cached icon is shown after process recreation without invoking the source icon loader for that package.
10. A package update, icon-pack change, adaptive-icon setting change, or size change does not reuse an incompatible icon.
11. Concurrent requests for the same icon key generate/decode it once, and recycled rows never receive another package's icon.

### Manual and build checks

- Cold start with a valid snapshot shows labels without per-row `loadLabel()` calls.
- Cold start with no snapshot shows a cheap fallback and then silently updates labels.
- Freeze/unfreeze updates both lists without a manual refresh and without an indicator from cache warming.
- Switching working mode refreshes frozen state.
- Install, uninstall, and package update do not leave stale labels or timestamps.
- Home filtering/sorting and all-apps filtering/sorting recompute after metadata completion.
- Launch, multiselect, import/export, and freeze-all behavior remains unchanged.
- `./gradlew assembleDebug` passes.
- Use a profiler or temporary test counter to verify that list filter/sort/bind paths no longer issue repeated package-manager calls.

## Affected files

- New: `app/src/main/kotlin/com/aistra/hail/utils/AppMetaCache.kt` and possibly `AppMetadataRepository.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/AppIconCache.kt` for disk reads/writes, keying, deduplication, cleanup, and recycled-view protection.
- `app/src/main/kotlin/com/aistra/hail/app/AppInfo.kt`.
- `app/src/main/kotlin/com/aistra/hail/app/AppManager.kt`.
- `app/src/main/kotlin/com/aistra/hail/HailApp.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerFragment.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/home/PagerAdapter.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsViewModel.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsFragment.kt`.
- `app/src/main/kotlin/com/aistra/hail/ui/apps/AppsAdapter.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/NameComparator.kt`.
- `app/src/main/kotlin/com/aistra/hail/utils/HFiles.kt` only if atomic file replacement is added.
- `gradle/libs.versions.toml`, `app/build.gradle.kts`, and `settings.gradle.kts` only if Room is selected.

## Out of scope

- Replacing the icon source loader or changing icon-pack behavior; the change adds persistence around the existing icon pipeline.
- Changing WorkManager deferred-freeze behavior.
- Network or remote metadata; `HRepository` is unrelated.
- Persisting frozen state as authoritative durable metadata.

## Decision checkpoint

Start with the file-backed metadata repository and file-backed icon cache unless there is a confirmed requirement for relational queries or durable package history. If the team chooses a database, choose Room for metadata only, keep the same in-memory snapshot and explicit prefetch API, and keep icons as bounded files rather than database BLOBs. Do not store bitmap BLOBs in Room: that increases database size, transaction cost, and migration/backup complexity without helping bitmap decode or rendering.
