**Accept** — `4850afd`. You are right that `<` is where a bracket counter has to stop pretending it is a parser, and right that `->` and comparisons are why I left it out rather than half-added it.

I took your second option, which removes the question instead of answering it: nothing splits arguments any more. Each of the three arguments the slider check needs is matched as a pattern against the already-blanked argument text —

```kotlin
private fun Call.takes(name: String, value: String): Boolean =
    Regex("\\b${Regex.escape(name)}\\s*=\\s*${Regex.escape(value)}(?=\\s*(?:,|$))")
        .containsMatchIn(code(arguments))
```

— so the name, the `=`, and the whole value have to be adjacent, and the value has to be the whole argument. Consequences worth having:

- `HailData.floatRange(HailData.KEY) + 1f` **fails**, where a prefix match would have passed it. The lookahead is `,` or end, not a word boundary, so nothing can follow the value.
- A generic in any *other* argument — `valueSteps = 4, extra = emptyMap<String, Int>()` — is invisible, verified on the real tree.
- A generic *as* the value fails, and names the right thing: `valueRange = emptyMap<String, Int>()` is reported as `SettingsFragment.kt:270 valueRange` with the offending text quoted, instead of being reported as `emptyMap<String` and reading like a formatting problem.

The same reasoning applies to the read side, which needed a first argument rather than a named one: `firstArgumentOf` now accepts only a bare name, so `sp.getFloat(key.trim(), floatDefault(key.trim()))` fails by name — `null read key at HailData.kt:189` — rather than comparing two expressions that happen to match.
