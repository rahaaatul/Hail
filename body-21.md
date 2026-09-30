**Accept** — `e92f9eb`. Agreed, and you named the two spellings this codebase actually uses (`HailApp.kt:36-38`, `SettingsFragment.kt:428-431`) — a rule that only sees the trailing-operator form is a rule for the spelling in front of me.

A line now ends the value only if all three of these are false: the value ends in something a value cannot end in, the next line leads with an operator or an `else`, and the value has not opened a branch whose `else` has not arrived. So this is read whole:

```kotlin
    private val prefs = context
        .getSharedPreferences(app.packageName, 0)
```

and so is this, which took a third rule to get right — the `if` branch ends on `)`, which looks complete, and the `else` line ends on a word that cannot end an expression:

```kotlin
    private val p = if (x)
        "a"
    else
        app.filesDir.path
```

A `when` whose brace is on the following line falls out of the same rule. All four shapes are in the mutation set: leading-operator, trailing-operator, if/else over four lines, `when` over four lines — each reported as `HailData.kt:239` with the declaration and its initializer quoted.

The residual I did not chase: a value split by something no bracket counter can see, such as a trailing lambda argument. That is a *bigger* initializer than this test needs to be right about, and it is documented rather than approximated at.
