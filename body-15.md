**Accept** — `a72334a`. You are right, and I should not have wrapped all three.

```kotlin
    private val dir by lazy { "${app.filesDir.path}/v1" }

    // Derived rather than lazy: `dir` already caches, and these are on the write path, where
    // every save reads them.
    private val appsPath get() = "$dir/apps.json"
    private val tagsPath get() = "$dir/tags.json"
```

A `SynchronizedLazyImpl` delegate to cache a two-string template is not a trade worth making, and your count of the reads is the argument: `saveAppsLocked` touches `appsPath` twice per save and `saveTags` once, so this was two monitor acquisitions per write to re-derive a value `dir` has already cached.

On the comment being wrong in a way that mattered: it said all three paths "name `app`", which is true of `dir` alone, and the justification for a production change rested on that imprecision. Corrected in place rather than left to be found later — the block now explains why `dir` is resolved on first use, and the two lines under it say why they are not.
