**Fixed** — `634730a`, and the `@BeforeClass` that caused it is gone again in `ce4164c`.

The setup now lives in the companion that was already there, so there is one. And the reason it was needed at all is gone too: `HailData`'s storage paths are `by lazy` (`HailData.kt:232-234`), so nothing about initializing the object touches `app` any more, and `HailDataTest` needs no installed application — which is why it should not have been installing one in the first place. `HailDataFilesDir.kt` and both `@BeforeClass` methods are deleted.
