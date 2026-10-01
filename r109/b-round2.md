**Accept** — all three fixed in `d793285`.

An expression-bodied `fun` has no brace of its own, so the first `{` after the keyword belonged to the expression and everything between was being blanked — including `private val sp by lazy { … app … }` on `HailData.kt:199`. This was not hypothetical: `HailData.kt` has six expression-bodied functions (164, 169, 176, 189, 343, 345), and all four of the first ones landed on the same brace. The body is now located from the parameter list's closing bracket, so an `=` between that bracket and the `{` means there is no body to blank:

```kotlin
val parameters = code.indexOf('(', function.range.last)
val close = if (parameters < 0) null else matchingBracket(code, parameters)
val open = code.indexOf(BRACE, function.range.last)
if (open < 0 || close == null) continue
val assigned = code.indexOf('=', close)
if (assigned in (close + 1) until open) continue
```

The search is *after* the parameter list rather than inside it, so a default argument does not read as an expression body. `closingBrace` is replaced by `matchingBracket(text, open)`, which counts whichever bracket opens at `open`.

The other two are the widenings you named: `AWAITING_HEAD`'s condition now allows one level of nesting, so `if (isReady(x))` is read as a head with nothing behind it, and the annotation group is `@[\w.:]+`, so `@field:Volatile private var queue = …` matches.

CI run 36827838838 on that commit: success.