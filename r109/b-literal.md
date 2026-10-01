**Accept** — fixed in `9cff0f3`. Correct, and `code()` was already there for exactly this reason.

```kotlin
if (DANGLING_KEYWORD.containsMatchIn(code(value))) return true
return AWAITING_HEAD.containsMatchIn(code(value))
```

Only the final character is read raw, because a literal that ends on a quote ends there. `private val hint = "retry if (transient)"` no longer leaves an `if (` inside a string deciding where the next value ends.
