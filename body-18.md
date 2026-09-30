**Accept** — `09793ca`. You are right on both counts, and the second is the one I would have shipped: the KDoc claimed a URL containing `app` survives `withoutComments`, and for any URL with a scheme it did not — the two slashes in `https://` opened a comment and blanked the rest of the line, so a genuine `app` reference *after* a string literal read as clean. In a file that is mostly URL constants, that is not an edge case.

A literal is now stepped over as a unit in both modes; `keepStrings` only decides whether its text survives:

```kotlin
val literal = literalEndingAt(text, index)
if (literal >= 0) {
    if (!keepStrings) blankOut(out, index, literal)
    index = literal
    continue
}
```

So a kept literal containing what would otherwise open a comment is still a literal, and the flag means what its name says. `initializerAfter` steps over literals for the same reason — a line that is only `"` and a newline is not the end of a raw string's value.

Verified on the real tree with the two shapes you describe: `private val x = "https://h/a" + app.filesDir` now fails, and `private val site = "https://example.com/app/store"` still passes.
