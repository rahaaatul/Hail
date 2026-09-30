**Accept** — `c9db804`. Both directions were real and the second one is the dangerous half: a neighbouring slider satisfying the check is a false pass, which is worse than a misleading message because nothing is red.

Strings, raw strings, char literals, and both comment forms are now blanked before anything is searched or counted — same length, newlines kept, since a location is counted in them. The needle is matched against the blanked text too, which closes a related hole I had not been shown: `callSites` was line-based, so a comment containing `sp.getFloat(` counted as a second read and would have failed the count test for the wrong reason.

Demonstrated rather than asserted, on the real tree with these two mutations applied separately:

- `// the slider is 11..16 (see #91)` inside the argument list, with `defaultValue = 14f` — the extraction survives the comment, and the failure is `SettingsFragment.kt:270 default=14f`, naming the argument that is actually wrong.
- `valueText = { Text(text = "%.0f (".format(it)) }` with the *other* slider's range made literal — the stray `(` in the string is invisible to the counter, and the failure is `SettingsFragment.kt:310 range=0f..30f`, i.e. the neighbour is checked on its own merits instead of being swallowed.

A call inside a `"""…"""` raw string is likewise not a call, which is checked too.
