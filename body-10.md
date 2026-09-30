**Accept** — `4850afd`. Agreed, and it is worse than you put it: `length + tail` indexes past the end of the buffer it is blanking, and it did so on the one input where a diagnostic matters.

```kotlin
private fun Int.orEndAt(length: Int, tail: Int = 0): Int =
    (if (this < 0) length else this + tail).coerceAtMost(length)
```

Clamped, so the documented behaviour — an unterminated `/*` or `"""` blanks the rest — is the actual behaviour, and a malformed file is scanned to the end instead of throwing from inside the helper with nothing to say which file it was in.

You are right that it is latent: neither can appear in a tree that compiles. It is worth having anyway, because the helper's whole job is to be the one place that decides what counts as code, and a function that throws on the inputs it is supposed to degrade on has no business being that function.
