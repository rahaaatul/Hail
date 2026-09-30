**Accept** — `a72334a`. You are right, and your version of the consequence is the accurate one: not "loses its quotes" but *reports the argument as absent*. A diagnostic that says a value is not there when it is, is worse than one that quotes a comment, because the reader stops looking.

So it does both of the things you offered, in the order you listed them — the consequence is now stated accurately, and the fallback is there:

```kotlin
    private fun Call.valueOf(name: String): String {
        val afterName = Regex("\\b${Regex.escape(name)}\\s*=\\s*([^\\n]*)")
        val masked = afterName.find(code(arguments))?.groupValues?.get(1)?.trim()
        if (!masked.isNullOrEmpty()) return masked
        val raw = afterName.find(arguments)?.groupValues?.get(1)?.trim()
        return if (raw.isNullOrEmpty()) "no $name" else raw
    }
```

A masked read that finds something is used as before, so a comment still cannot supply the value. A masked read that finds *nothing* means the value was a string literal — or there is no value — and the raw text is quoted instead, so a literal appears with its quotes. The residual is spelled out in the KDoc rather than left: a comment can be quoted in exactly the case where there is no real value to quote, which is also the case the message already describes as having no value.

Agreed that it is unreachable today for `valueRange` and `defaultValue`; the point of the KDoc is the next caller.
