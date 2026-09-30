**Accept** — `e92f9eb`. Both consequences are real and the second is the one I had not considered: `findAll` resuming past the end of a swallowed match means every 4-space declaration *between* the parameter and the far-away `=` was never scanned at all. A check that silently stops scanning is worse than one that reports wrongly.

The type is bounded to its own line, so a declaration with no `=` on that line does not match instead of matching something else:

```kotlin
Regex("^[ \\t]+(?:\\w+ )*(?:val|var) \\w+(?:[ \\t]*:[^=\\n]*?)?[ \\t]*=(?!=)", RegexOption.MULTILINE)
```

Computed properties are now excluded explicitly, both spellings, since a getter body runs on every access and captures nothing at initialization:

```kotlin
    private val sp: String
        get() = app.filesDir.path
```

…is correct code, and the check was going to fail it. That also makes the KDoc's claim true for the annotated spelling rather than only the bare one.

The four-space anchor is gone in the same commit, for the reason in the third thread: parentheses can say "parameter" directly, so indentation no longer has to stand in for it — and a declaration nested in an object or class inside this file is scanned at its own depth rather than skipped without a word.
