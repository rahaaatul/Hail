**Accept** — fixed in `0ed734c`.

```kotlin
private val HEADER = Regex("(?:fun|class|interface|object|constructor)\\s+[\\w<>,.?: ]*$")
private val HEADER_START = Regex("(?m)^[ \\t]*(?:@\\w[\\w.:]*(?:\\([^)\\n]*\\))?[ \\t]*)*$|[;{}]")
```

(`constructor` and the spaced-annotation-argument case are `d793285`, from your follow-ups.)

`isHeader` also had the search direction backwards: `Regex.find(text, open - 1)` searches *forward* from that index, so it found a `}` below the declaration under test and put `start` past `open` — CI run 36823876788 caught it as a `StringIndexOutOfBoundsException` at the `substring`. It now takes the last match ending strictly before `open`, which is `a1ce450`.
