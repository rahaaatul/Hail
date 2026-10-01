**Accept** — fixed in `e92f9eb`, and you named the consequence I had not: a match that swallows four lines makes `findAll` resume past the end of it, so every declaration in between was never scanned at all. A check that silently stops scanning is worse than one that reports wrongly.

The type annotation is bounded to its own line, so a declaration with no `=` on that line does not match instead of matching something else:

```kotlin
Regex("^[ \\t]+(?:@[\\w.]+(?:\\([^()]*\\))?[ \\t]+)*(?:\\w+ )*(?:val|var) \\w+(?:[ \\t]*:[^=\\n]*?)?[ \\t]*=(?!=)", RegexOption.MULTILINE)
```

Computed properties are now excluded explicitly, both spellings — annotated and not — since a getter body runs on every access and captures nothing at initialization:

```kotlin
    private val sp: String
        get() = app.filesDir.path
```

That also makes the KDoc's claim true for the annotated spelling rather than only the bare one.
