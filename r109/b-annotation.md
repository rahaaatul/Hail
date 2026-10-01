**Accept** — fixed in `9cff0f3`, and the use-site-target version you raised as a follow-on is in `d793285`.

```kotlin
Regex("^[ \\t]+(?:@[\\w.:]+(?:\\([^()]*\\))?[ \\t]+)*(?:\\w+ )*(?:val|var) \\w+…")
```

An annotation on its own line already worked — the `val` is on the next line and matches. The same-line spelling did not, because `(?:\w+ )*` cannot absorb `@Volatile`. Annotations are now allowed ahead of the modifiers, with or without arguments, and `:` is in the class so `@field:Volatile` matches too.
