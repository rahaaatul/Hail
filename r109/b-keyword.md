**Accept** — fixed in `04343b5`. You are right that `fun` is a hard keyword and that backticks do not help.

```kotlin
for (function in FUNCTIONS.findAll(code)) {
```

The unit-test run on the previous commit failed with `Syntax error: Expecting a variable name` for exactly that reason, and CI never reached the scan. The two constants the same commit was missing are in `0ed734c`.
