**Accept** — fixed in `9cff0f3`. You are right, and the consequence was worse than a false report: with no `else` in the value the read never terminated, so `initializerAfter` returned the rest of `HailData.kt` as one initializer and the assertion would have blamed a `mode` for an `app` reference hundreds of lines below it.

The rule is now anchored instead of accumulated. A value is unfinished when it *ends* on something that cannot end an expression:

```kotlin
private val AWAITING_HEAD = Regex("\\b(?:if|when|while)\\s*\\((?:[^()]|\\([^()]*\\))*\\)\\s*$")
private val DANGLING_KEYWORD = Regex("\\b(?:else|if|when|try|do|return|throw|in|by)\\s*$")
```

A `when`'s brace needs nothing from this — the bracket counter already handles it — and the nesting in the condition is `d793285`, which closes the "a call in the condition" version of your finding.
