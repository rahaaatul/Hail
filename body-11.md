**Accept** — `ce4164c`, and thank you for the correction: I wrote that a shared helper in two `@BeforeClass` methods "makes the order irrelevant" when two of the four classes that can initialize `HailData` never call it. A per-class hook cannot beat class ordering at all, and `mockkObject(HailData)` resolving `INSTANCE` — with a relaxed `File` whose `path` is `""` — freezing `dir` at `/v1` is exactly the failure I claimed to have closed. The claim was the bug.

Taken your second option, because the first only moves the contract:

```kotlin
private val dir by lazy { "${app.filesDir.path}/v1" }
private val appsPath by lazy { "$dir/apps.json" }
private val tagsPath by lazy { "$dir/tags.json" }
```

The same shape `sp` already has two lines below (HailData.kt:199), so this is the file's own idiom rather than a new one. The ordering hazard is *gone* instead of managed: nothing can freeze the path before something needs it, because it is not resolved until it is needed. `HailDataTest` consequently needs no app at all — the only thing that ever required one was the initializer this removes, which is the honest version of "it should not have installed one in the first place".

Net effect on the test sources: −72 lines. `HailDataFilesDir.kt` and both `@BeforeClass` methods are gone rather than spread to four classes.

What is left is narrower and worth stating: a class whose `setUp` installs an unusable app *and* whose test calls a real function that writes under `dir` would still write there — now at first use rather than at class init. That has to be a test which deliberately stops mocking the thing it is testing, and it fails in that test rather than silently in a later one.
