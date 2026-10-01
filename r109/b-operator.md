**Accept** — fixed in `e92f9eb`. You are right that the codebase uses the trailing-operator form and that a rule seeing only it is a rule for the spelling in front of me.

A line now ends the value only if all of these are false: the value ends in something a value cannot end in, the next line leads with an operator or an `else`, and the value does not end on a head with nothing behind it. So this is read whole:

```kotlin
    private val prefs = context
        .getSharedPreferences(app.packageName, 0)
```

and so is a `when` whose brace is on the following line. Later rounds of this review tightened the rule further — a `when` with no `else` was ending the read wrongly, and a string mentioning `if (` could decide where a value ended — so the final version is `d793285`, and those patterns run over the value with its literals blanked.
