# App Metadata Cache and Startup Performance Plan

## Branch

`feature/app-metadata-cache`

This branch combines the metadata/icon cache work with the latest `main` branch, including persistent root-shell lifecycle changes. Cache-related changes must remain isolated from unrelated local debug resources.

## Goal

Reduce repeated `PackageManager` work in home and all-apps filtering, sorting, and binding. Warm process-death starts from local cache data, keep cache warming silent, and prevent icon loading from blocking the first frame.

The observed 1-2 second delay should be measured rather than attributed to JSON immediately. Likely contributors include:

- initial installed-application enumeration;
- frozen-state checks and root/Shizuku IPC;
- label loading and sorting;
- RecyclerView binding and icon generation;
- disk bitmap decode and UI layout.

## Storage decision

### Keep JSON for the metadata snapshot in this phase

A database is not automatically faster. The hot path should be an in-memory map regardless of whether the durable source is JSON, SQLite, or Room. A database query during every row bind would usually be slower than the current memory lookup and would not remove `PackageManager` or freeze-state IPC costs.

The current metadata shape is a small key-value snapshot:

- read entries by package name;
- refresh a batch from `PackageManager`;
- replace the snapshot atomically;
- load it once after process creation.

That is a good fit for versioned JSON under `filesDir/v1/app_meta.json`. JSON parsing is paid once at startup, while UI reads use the `ConcurrentHashMap`. The snapshot is written off the main thread using a temporary file and atomic rename.

### When Room becomes the better choice

Adopt Room only when the requirements become database-shaped:

- complex queries across many metadata fields;
- package history or change records;
- multiple Android users/profiles;
- incremental deletion and indexing of a large package inventory;
- migrations and durable records beyond a replaceable cache.

If Room is introduced, use it behind the same repository boundary. Read the needed packages in one batch, convert them to an immutable in-memory map, and never query Room once per adapter row. Room improves data organization and queryability, not `PackageManager` latency or icon generation.

## Cache architecture

### Metadata

`AppMetaCache` owns:

- an in-memory package map for synchronous UI reads;
- explicit `prefetch()` and `prefetchPackages()` operations on `Dispatchers.IO`;
- per-package locking to collapse concurrent refreshes;
- runtime state invalidation when working mode or freeze operations change;
- a versioned JSON snapshot with atomic persistence;
- a revision flow used by screens to recompute their lists.

Static metadata and runtime frozen state must remain conceptually separate. Frozen state depends on the current working mode and must not be treated as authoritative durable metadata.

### Icons

`AppIconCache` uses three stages:

1. memory `LruCache`;
2. disk bitmap cache under `filesDir/v1/icons`;
3. existing icon pack or `AppIconLoader` fallback.

Disk keys include package, user, size, package source signature, icon pack, and adaptive-icon mode. Disk I/O and bitmap decoding happen on the icon dispatcher. Writes use temporary files and rename. Recycled views verify their package tag before applying an asynchronous result.

Do not synchronously preload every installed icon. Warm only checked or visible packages after the first list is known.

## Startup and UI flow

1. `HailApp.onCreate()` seeds the metadata map from the local snapshot and initializes the existing root-shell preference listener.
2. The home and all-apps flows request explicit metadata prefetch for their package set.
3. Filtering and sorting read one cache snapshot rather than repeatedly calling `loadLabel()`, package-info lookup, or frozen-state IPC.
4. Cache revisions trigger list recomputation on the main thread.
5. Freeze operations and working-mode changes invalidate affected runtime state.
6. Icon requests use memory first and perform disk/source work asynchronously.

Cache warming must not toggle the existing refresh indicator or produce user-visible notifications.

## Explaining the 1-2 second delay

Before changing persistence, add timing around these boundaries:

- `HailApp.onCreate()` and `AppMetaCache.seedFromDisk()`;
- installed application enumeration;
- metadata prefetch, split into label/package-info/state timing;
- first `displayApps` or home list submission;
- first visible icon request and first completed icon bind;
- root-shell initialization and first root operation;
- first-frame and fully-bound RecyclerView timing.

Use Android Studio CPU profiler or temporary debug-only counters/timestamps. Compare four runs:

- cold process with no snapshot;
- cold process with metadata snapshot and disk icons;
- warm process with memory cache;
- cache disabled baseline.

This distinguishes JSON parsing from the much more expensive Android framework and IPC work.

## Priority optimizations after measurement

1. Keep all metadata and icon disk work off the main thread.
2. Avoid waiting for every installed package before showing the first home screen; prefetch checked/visible packages first and refresh the rest silently.
3. Use one batch metadata refresh and one filtering/sorting pass.
4. Limit concurrent icon decoding and generation.
5. Avoid repeated `applicationInfo` retrieval in bind paths where a prepared row model is sufficient.
6. Do not initialize Room solely to replace a small JSON snapshot.
7. If JSON parsing is proven to be the measured bottleneck, replace only the persistence adapter with Room or a compact binary format while preserving the in-memory cache and APIs.

## Validation

- `:app:compileDebugKotlin` passes after merging `origin/main`.
- `:app:assembleDebug` must pass before release.
- Metadata cache misses must not start work from getters, comparators, `DiffUtil`, or adapter binding.
- Corrupt or incompatible snapshots must be ignored without startup failure.
- Freeze/unfreeze and working-mode changes must update both list flows.
- Disk icon keys must change when package version, icon pack, adaptive mode, user, or size changes.
- A recycled row must never receive another package's icon.
- Compare measured startup and first-visible-content timings before and after each optimization.

## Out of scope

- Adding Room without a measured requirement.
- Replacing the existing root-shell implementation from `main`.
- Synchronously preloading every installed app icon.
- Persisting frozen state as permanent package metadata.
