**Accept** — fixed in `e92f9eb`. Right that the claim and the pattern disagreed, and right about the fix: indentation was standing in for "not a constructor parameter", and the header a parameter list belongs to can say that directly.

```kotlin
val parameterised = parameters(code(declared))
```

A parameter list belongs to a `fun` or a class header and to nothing else, so that is what is tested — see the KDoc on `isHeader`. "Inside some parenthesis" was the wrong test twice over: it is true of a member of an object expression passed as an argument, which is exactly the thing to catch, and false of nothing that mattered. A declaration nested in an object or class inside `HailData` is now scanned at its own depth.
