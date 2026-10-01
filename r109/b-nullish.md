**Accept** — fixed in `cbd94a1`. You are right that `HEADER` is `$`-anchored, so widening the
candidate prefix to the whole file cannot turn a non-header into a header; it can only answer the
question differently in the case where nothing in the text marks where the header begins.

```kotlin
val start = HEADER_START.findAll(text)
    .takeWhile { it.range.last < open }
    .lastOrNull()?.range?.last?.plus(1) ?: 0
```

The `return false` is gone. My objection to `0` was that a match from the top of the file might
classify an unrelated declaration, which is true of a `find`-style search but not of this one: the
only thing the answer depends on is the substring's tail. Both branches still satisfy
`start <= open`, so the `substring` is well-formed.