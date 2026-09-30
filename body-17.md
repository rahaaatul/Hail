**Accept** — `09793ca`. This one matters more than the other two, because the spelling you wrote is not exotic: it is how `TILE_ACTION_VALUES` (:112), `DYNAMIC_SHORTCUT_ACTIONS` (:190) and the `FLOAT_PREFERENCES` block (:293) are already written in this object. The guard matched `val dir =`, found nothing after the `=`, and passed — the exact regression it exists to catch.

The initializer is now read balanced over newlines, with the rule a reader applies by eye: a line ends the value unless the value is still open or has not started. So both of these fail, naming `HailData.kt:233` and quoting the declaration plus its initializer:

```kotlin
    private val dir = "${app.filesDir.path}/v1"

    private val dir =
        "${app.filesDir.path}/v1"
```

Also covered, because they are the shapes the same objection applies to: `listOf(` spanning lines, a wrapped lambda, a multiline type annotation, and a raw string whose interior newline is not the end of anything.
