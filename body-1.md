**Accept, all three parts** — `3197fb2`.

The example you wrote is the finding: `sp.getFloat(NEW_KEY, floatDefault(HOME_FONT_SIZE))` passes a scan that only asks whether the line mentions `floatDefault(`, and loses NEW_KEY on the next restore. Same-line matching was the wrong check — it is satisfied by a comment and blind to which key is which, which is the part that matters.

The scan now pins the property rather than the shape:

- exactly one `SharedPreferences` Float read in `src/main/kotlin`, so a second one fails wherever it is, and the count cannot be satisfied by a scan that finds nothing;
- it lives in `HailData.kt`, so a read that does not go through the accessor fails even if it is otherwise well-formed;
- the key `getFloat` is handed and the key `floatDefault` is asked about are the same identifier — which is the property that makes the accessor's default the right one for whatever key a caller passed it.

It reports `file:line` and the offending text on every failure, since a check whose message says "something drifted" is a check people disable.

Your second point — that nothing asserted every *declared* key is read — was true and is the more interesting half, because that drift bounds a key no setting has while every test stays green. `every declared float preference is read through the declared accessor` resolves each key through the `const val` that spells it and requires a `declaredFloat(<CONST>)` call naming that constant, so renaming a constant fails rather than silently undeclaring the preference behind it.

**Declining the KDoc hedge, for the record.** You are right that the tests are weaker than the doc claimed. I am fixing the tests instead of weakening the claim, because "declaredFloat is the only call" is the design the branch is built on and a doc that says "the design intends this, please" is how `pendingBackupOptions` happened. If a later change makes the guarantee genuinely unmechanisable, the KDoc is what should give, not the check.
