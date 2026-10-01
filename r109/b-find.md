**Accept** — fixed in `a1ce450`. Correct on both counts: `find` searches forward from its start index and never backwards, and the consequence was a `start` past `open`, so `text.substring(start, open)` threw.

```kotlin
val start = HEADER_START.findAll(text)
    .takeWhile { it.range.last < open }
    .lastOrNull()?.range?.last?.plus(1) ?: return false
```

Every match in the sequence ends before `open`, so `start` cannot exceed it; an empty sequence returns `false` rather than substituting `0`, which would have let a match from the top of the file classify an unrelated declaration.
