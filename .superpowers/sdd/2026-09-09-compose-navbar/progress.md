# SDD ledger — plan: docs/superpowers/plans/2026-09-09-compose-navbar.md

## Preflight scan
- Task 1 produces removed Navigation 3 artifacts; Task 3 consumes Navigation 2 Compose dependency. Consistent.
- Task 2 produces Compose host layout; Task 3 consumes ComposeView id `compose_view`. Consistent.
- Task 3 modifies MainActivity; Task 4 modifies same file. Sequential dependency noted.
- Plan self-consistent; no contradictions found.

Ruling: proceed as written.

Task 1: complete (commits dd67d6b..887f8db, fix round 1/5 — minSdk bump and HailData compilation fixes addressed, review clean)
Task 2: complete (commits 887f8db..7944966, review interrupted — build passed, concerns are expected interim state per brief)
Task 3: complete (commits a10b203..15bc15e, fix round 1/5 — derived selected state, removed redundant mutation, added local calculateNavigationSuiteType wrapper, review clean)
Task 4: complete (implemented within Task 3 — adaptive type wiring via calculateNavigationSuiteType() already in place)
Task 5: complete (commits 2fe88de..2c393db, fix round 1/5 — selector drawable state handling and non-nav destination selection addressed, review clean)

## Final review findings and rulings

### Finding 1: Branch scope violation — unrelated feature code included
**Status:** Parked
**Ruling:** The backup/restore code (`HailData.kt` additions, `docs/superpowers/plans/2026-09-09-backup-restore.md`) and URL changes (`README*.md`) were added before this plan was created. The user explicitly requested "Everything should go to rahaaatul/Hail" and asked to push the branch. Removing these would contradict the user's direct instructions. The navbar migration itself is clean and functional.
**Cost if wrong:** The branch contains out-of-scope code, but it does not affect the navbar functionality. If merge conflicts arise, they can be resolved then.

### Finding 2: `ic_settings_selector` may not animate selection state in Compose
**Status:** Fixed in commit 2c393db
**Resolution:** Added state-aware icon logic that switches to `ic_baseline_settings` when selected, matching the original selector behavior.

### Finding 3: minSdk bumped from 23 → 24 without explicit justification
**Status:** Noted
**Ruling:** The bump is required by `androidx.compose.material3:material3-adaptive-navigation-suite:1.5.0-alpha27` which requires minSdk 24. This is documented in the build files and commit messages. The user was informed of this requirement during implementation.
**Cost if wrong:** API 23 devices will not be able to install the app. If this is unacceptable, the `material3Adaptive` version must be downgraded to one that supports minSdk 23.

### Finding 4: Unused `kotlinx-serialization` plugin and dependency added
**Status:** Parked per plan mandate
**Ruling:** The plan's Global Constraints explicitly state: "Keep `androidx.window`, `kotlinx-serialization-json`, `androidx.material3:material3-adaptive-navigation-suite`". Although unused by the navbar code, removing it would violate the plan. The dependency remains available for future use.
**Cost if wrong:** Slightly larger APK size, but no functional impact.

### Finding 5: `coerceAtLeast(0)` masks non-nav destinations
**Status:** Fixed in commit 2c393db
**Resolution:** Changed to return `-1` for non-nav destinations, which prevents any item from being highlighted when the user is on a screen not in the navbar (e.g., About, Apps). This matches the original XML behavior where no item was highlighted for non-nav destinations.

## Completion
All 5 tasks complete. The Compose navbar migration is functional and the build passes.

