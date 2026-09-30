**Accept** — `4850afd`. You are right that a fix which only holds if this class wins the race is not a fix, and the point about `HBackupTest` is the part I had not thought through: it installs a `TemporaryFolder` subdirectory and its own `@Rule` deletes that, so it can freeze `HailData.dir` onto a path that is gone before anything else in the suite runs. One JVM, no guaranteed order, coin flip.

Fixed at the source rather than per class, as you suggested. `installHailDataFilesDirForTests` lives in the test sources, and **both** classes call it from `@BeforeClass` — before either can touch the object — so whichever runs first freezes the same directory:

```kotlin
@BeforeClass
@JvmStatic
fun freezeHailDataFilesDir() {
    installHailDataFilesDirForTests()
}
```

Each class then installs its own mock app per test, which is safe precisely because the object is already frozen by then, so `HBackupTest` keeps the isolation it needs for the files it actually writes.

I did not take the other half of your suggestion — making `dir` resolve lazily from `app` per access. In production `app` is set in `onCreate` before any `HailData` use, so the eager `val` is not the defect; a test needing a different `filesDir` is the unusual case, and changing production initialization order to serve it would be a wider change than this branch should carry. The alternative to one shared directory would be a `@Rule` that cannot be undone, which is the trap rather than the cure.
