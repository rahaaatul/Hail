**Accept** — `3197fb2`. `mockPreferences(HOME_FONT_SIZE to 14f)` is a Float record, so `recorded is Float` routes the value into the Float arm on its own. Delete the declared-key half of `declaredFloat` and all three stay green while nothing is restored for any declared key. Correct, and the sibling test at `1E1000` was already recording nothing for the same reason.

All three now record nothing. Verified against the reader at the new head, since the point of each test is which branch refuses:

- `1.2345678E22` → magnitude branch, `Float`, key in the warning;
- `-5.0` → range branch, `Float in 0f..30f`, key in the warning;
- `16.0` → the inclusive top of `11f..16f` reaches `putFloat(key, 16f)`, no warning.

Each carries the reason inline now — the test has to say why a missing record is the interesting part of the setup, or the next reader will "helpfully" restore the `14f` and take the guarantee with it.
