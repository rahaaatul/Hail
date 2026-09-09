# Task 1: Downgrade build dependencies from Navigation 3 to Navigation 2 Compose

**Goal:** Remove Navigation 3 artifacts and add Navigation Compose 2 for future mixed Fragment/Compose navigation.

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

**Context:** Hail currently has Navigation 3 runtime/ui and `adaptive-navigation3` on the classpath. These must be removed. Navigation 2 `navigation-compose` must be added so the app can later use Compose destinations alongside existing Fragments.

**Constraints:**
- Do not change any source code beyond these two build files
- Keep `androidx.window`, `kotlinx-serialization-json`, `androidx.material3:material3-adaptive-navigation-suite`
- Remove: `navigation3`, `androidx-navigation3-runtime`, `androidx-navigation3-ui`, `androidx-material3-adaptive-nav3`
- Add: `navigation-compose = "2.9.8"` and alias `androidx-navigation-compose`

**Verification:**
- `./gradlew :app:dependencies --configuration debugCompileClasspath | grep -E "navigation3|adaptive-navigation3"` must show no Navigation 3 artifacts
- `./gradlew :app:assembleDebug` must succeed
