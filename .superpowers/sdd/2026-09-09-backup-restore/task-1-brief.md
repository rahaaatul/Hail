# Task 1: Create HBackup Utility Class

## Goal

Create `HBackup.kt` utility class with `backup()` and `restore()` suspend functions that serialize/deserialize Hail data to/from a ZIP file using `java.util.zip`.

## Files

- **Create:** `app/src/main/kotlin/com/aistra/hail/utils/HBackup.kt`
- **Create:** `app/src/test/kotlin/com/aistra/hail/utils/HBackupTest.kt`

## Requirements

### HBackup.kt

Create a Kotlin `object HBackup` in `com.aistra.hail.utils` with:

1. **Data classes:**
```kotlin
data class BackupOptions(
    val apps: Boolean = true,
    val whitelist: Boolean = true,
    val actions: Boolean = true,
    val settings: Boolean = true
)

data class RestoreOptions(
    val apps: Boolean = true,
    val whitelist: Boolean = true,
    val actions: Boolean = true,
    val settings: Boolean = true
)
```

2. **Constants:**
```kotlin
private const val FILE_APPS = "apps.json"
private const val FILE_WHITELIST = "whitelist.json"
private const val FILE_ACTIONS = "actions.json"
private const val FILE_SETTINGS = "settings.json"
```

3. **`backup()` function:**
```kotlin
suspend fun backup(
    context: Context,
    outputFile: File,
    options: BackupOptions
): Result<Unit>
```

Behavior:
- Uses `Dispatchers.IO`
- Creates a ZIP at `outputFile`
- If `options.apps`: writes `apps.json` containing JSON array of package names from `HailData.checkedList`
- If `options.whitelist`: writes `whitelist.json` containing JSON array of package names where `whitelisted == true`
- If `options.actions`: writes `actions.json` containing JSON array of action objects with `id`, `launchPackage`, `unfreezePackages`
- If `options.settings`: writes `settings.json` containing JSON object of all SharedPreferences key/value pairs
- Uses `org.json.JSONArray` and `org.json.JSONObject` for serialization
- Returns `Result.success(Unit)` or `Result.failure(exception)` via `runCatching`

4. **`restore()` function:**
```kotlin
suspend fun restore(
    context: Context,
    inputFile: File,
    options: RestoreOptions
): Result<Unit>
```

Behavior:
- Uses `Dispatchers.IO`
- Reads ZIP from `inputFile`
- If `options.apps` and `apps.json` exists: reads package names, calls `HailData.addCheckedApp(pkg, 0, false)` for each not already checked, then `HailData.saveApps()`
- If `options.whitelist` and `whitelist.json` exists: reads package names, sets `whitelisted = true` on matching `AppInfo` in `HailData.checkedList`, then `HailData.saveApps()`
- If `options.actions` and `actions.json` exists: reads actions, calls `ActionsRepository.save(id, launchPackage, unfreezePackages)` for each
- If `options.settings` and `settings.json` exists: reads settings JSON object, applies all key/value pairs to SharedPreferences via `sp.edit()`
- Returns `Result.success(Unit)` or `Result.failure(exception)` via `runCatching`

### HBackupTest.kt

Create unit tests in `com.aistra.hail.utils` using JUnit 4, MockK, and `runTest`.

**Required tests:**

1. `backup writes apps json when apps enabled` — mocks `HailData.checkedList`, calls `HBackup.backup()` with `apps=true`, verifies ZIP contains `apps.json` with correct package names

2. `backup writes whitelist json when whitelist enabled` — mocks `HailData.checkedList`, calls `HBackup.backup()` with `whitelist=true`, verifies ZIP contains `whitelist.json` with only whitelisted packages

3. `restore reads apps json and adds to checked list` — creates a temp ZIP with `apps.json`, calls `HBackup.restore()` with `apps=true`, verifies `HailData.addCheckedApp()` called for each package

4. `restore ignores missing files and succeeds` — creates a temp ZIP with `nonexistent.json`, calls restore with all options true, verifies success

5. `backup handles empty checked list` — mocks empty `HailData.checkedList` and empty SharedPreferences, calls backup with all options true, verifies success

**Test setup pattern:**
```kotlin
@Before
fun setUp() {
    val mockApp = mockk<HailApp>(relaxed = true)
    every { mockApp.filesDir } returns File(System.getProperty("java.io.tmpdir"), "hail-test-${System.currentTimeMillis()}")
    every { mockApp.packageManager } returns mockk(relaxed = true)
    HailApp.setAppForTest(mockApp)

    mockkObject(HailData)
    mockkObject(ActionsRepository)
}

@After
fun tearDown() {
    unmockkAll()
}
```

## Constraints

- Use `java.util.zip` only — no new dependencies
- Use `org.json` for JSON (already used in codebase)
- Follow existing patterns: `runCatching`, `withContext(Dispatchers.IO)`
- `HailData` and `ActionsRepository` are Kotlin objects — use `mockkObject`
- `HailData.checkedList` returns `MutableList<AppInfo>`
- `AppInfo` constructor: `AppInfo(packageName, pinned, whitelisted, tagIdList)`

## Deliverable

Write both files, run the tests, ensure they all pass, commit.
