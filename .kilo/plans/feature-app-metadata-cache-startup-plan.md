# Feature Branch Startup Performance Plan

## Branch

`feature/app-metadata-cache`

## Objective

Reduce the remaining 1-2 second delay before the app is fully usable while preserving the metadata and disk-icon cache changes. Measure the startup stages first; do not assume JSON is the bottleneck.

## Best persistence choice

Keep the current architecture: JSON snapshot for durable metadata and an in-memory map for UI reads. The JSON file is parsed once, while list filtering and binding use memory. Room would not improve `PackageManager` calls, root/Shizuku IPC, bitmap decoding, or RecyclerView work. A database becomes worthwhile only for indexed queries, package history, multi-user inventory, or other relational records.

If measurements prove snapshot parsing is significant, replace only the persistence adapter with Room or a compact binary format. Keep the same in-memory cache and prefetch API so the UI architecture does not change.

## Work items

1. Instrument application startup, installed-app enumeration, metadata prefetch, list submission, first frame, and first complete icon bind.
2. Compare cold start with no cache, metadata snapshot, disk icons, and a warm in-memory process.
3. Prioritize visible/checked package prefetch instead of waiting for the entire installed list.
4. Keep all JSON, disk bitmap, and icon-loader work on background dispatchers.
5. Bound icon decode/generation concurrency and deduplicate identical icon keys.
6. Recompute lists after cache revisions without showing a refresh indicator.
7. Validate package/version, icon-pack, adaptive-icon, user, and size invalidation.
8. Run `:app:assembleDebug` and record before/after timings.

## Completion criteria

The feature is complete when the cache branch builds, cache warming does not block the first frame, stale icons cannot bind to recycled rows, runtime state invalidates on mode/freeze changes, and profiling identifies the remaining startup cost instead of guessing that JSON is responsible.
