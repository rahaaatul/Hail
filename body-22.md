**Accept** — `e92f9eb`. Right that the claim and the pattern disagreed, and right about the fix: indentation was standing in for "not a constructor parameter", and parentheses can say that directly.

```kotlin
val parameterised = insideParentheses(code(declared))
…
if (parameterised[declaration.range.first]) return@mapNotNull null
```

A parameter is always inside one, at any indentation, so the four-space anchor is gone and a property of a nested `object`, `companion object` or class inside `HailData` is scanned at its own depth:

```kotlin
    private object N {
        val p = app.filesDir.path
    }
```

…now reported at `HailData.kt:240`, where before the pattern simply did not match.

One thing the comment above the test now says that it did not before, because it is true: a local inside a lambda is still scanned along with everything else. It is not an object initializer either — it is initialized by whatever owns it — and this file has no such local naming the application, which is the only reason that costs nothing. Preferring a documented false positive over a silently skipped declaration seemed like the right way round for a check whose failure mode is a green suite.
