# Utility Classes Analysis - Hail Project

## Overview
Analysis of 9 utility classes in `com.aistra.hail.utils` package for API documentation and Compose migration needs.

---

## 1. FuzzySearch.kt

### API
```kotlin
object FuzzySearch {
    fun search(raw: String?, query: String?): Boolean
}
```

### Description
Levenshtein distance-based fuzzy string matching. Returns true if:
- Query is empty/null (matches everything)
- Raw contains query (case-insensitive exact match)
- Fuzzy search enabled AND Levenshtein distance < raw length AND query characters appear in order in raw

### Dependencies
- `org.apache.commons.text.similarity.LevenshteinDistance`
- `HailData.fuzzySearch` (settings flag)

### Compose Migration Needs
- **None** - Pure algorithm, no UI state, no Android dependencies
- Stateless function, safe to use directly in Compose

---

## 2. NineKeySearch.kt

### API
```kotlin
object NineKeySearch {
    fun search(query: String?, vararg strings: String?): Boolean
}
```

### Description
T9/9-key predictive text search. Converts input strings to pinyin, then to 9-key mapping, then uses FuzzySearch.

### Dependencies
- `net.sourceforge.pinyin4j.PinyinHelper`
- `FuzzySearch`

### Compose Migration Needs
- **None** - Pure algorithm, stateless
- Safe for direct Compose usage

---

## 3. PinyinSearch.kt

### API
```kotlin
object PinyinSearch {
    fun searchPinyinAll(raw: String?, query: String?): Boolean
}
```

### Description
Chinese pinyin search supporting:
- Initials search (首字母): "jsq" → "计算器"
- Full pinyin search (全拼): "jisuanqi" → "计算器"
- Only active when system locale is Chinese

### Dependencies
- `net.sourceforge.pinyin4j.PinyinHelper`
- `Locale.getDefault()`

### Compose Migration Needs
- **None** - Pure algorithm, stateless
- Locale check uses system default - may need `LocalConfiguration.current.locales` in Compose for proper locale-aware behavior

---

## 4. NameComparator.kt

### API
```kotlin
object NameComparator : Comparator<Any> {
    override fun compare(a: Any, b: Any): Int
}
```

### Description
Compares `ApplicationInfo` or `AppInfo` by localized label/name using `Collator`. Pinned apps sort first.

### Dependencies
- `android.content.pm.ApplicationInfo`
- `java.text.Collator`
- `HailApp.Companion.app` (context)

### Compose Migration Needs
- **Low** - Comparator is stateless
- Uses `app.packageManager` - in Compose, prefer passing `PackageManager` as parameter or using `LocalContext.current.packageManager`

---

## 5. HUI.kt

### API
```kotlin
object HUI {
    val INSETS_TYPE_DEFAULT: Int
    fun showToast(text: CharSequence, isLengthLong: Boolean = false): Unit
    fun showToast(resId: Int, isLengthLong: Boolean = false): Unit
    fun showToast(resId: Int, text: CharSequence, isLengthLong: Boolean = false): Unit
    fun startActivity(action: String = Intent.ACTION_VIEW, uri: String): Boolean
    fun openLink(url: String): Boolean
    fun copyText(text: String): Unit
    fun pasteText(): String?
}
```

### Description
UI utilities: toasts, activity launching, clipboard operations, window insets constant.

### Dependencies
- Android SDK: `Toast`, `Intent`, `ClipboardManager`, `WindowInsetsCompat`
- `HailApp.Companion.app` (application context)

### Compose Migration Needs - **HIGH**

| Function | Migration Approach |
|----------|-------------------|
| `showToast` | Replace with `Snackbar` via `SnackbarHost` or `Toast` via `LocalContext.current.toast()`; consider `rememberCoroutineScope` for non-composable calls |
| `startActivity` / `openLink` | Use `LocalContext.current.startActivity()` in composables; extract to `LaunchedEffect` for side effects |
| `copyText` / `pasteText` | Use `LocalContext.current.getSystemService(ClipboardManager::class.java)` in composables |
| `INSETS_TYPE_DEFAULT` | Use `WindowInsets.systemBars` / `WindowInsets.ime` with `Modifier.windowInsetsPadding()` |

**Recommended Pattern:**
```kotlin
@Composable
fun ToastHost() {
    val context = LocalContext.current
    // Or use a proper SnackbarHost
}

@Composable
fun copyToClipboard(text: String) {
    val clipboard = LocalContext.current.getSystemService(ClipboardManager::class.java)
    clipboard.setPrimaryClip(ClipData.newPlainText("label", text))
}
```

---

## 6. HFiles.kt

### API
```kotlin
object HFiles {
    fun exists(path: String): Boolean
    fun createDirectories(dir: String): Boolean
    suspend fun copy(source: InputStream, target: OutputStream): Unit
    fun read(source: String): String?
    fun write(target: String, text: String): Boolean
}
```

### Description
File operations with Android version-aware implementation (uses `java.nio.file` on API 26+, `java.io.File` otherwise). `copy` is suspend function on IO dispatcher.

### Dependencies
- `kotlinx.coroutines.Dispatchers.IO`
- `java.io`, `java.nio.file`, `kotlin.io.path`

### Compose Migration Needs - **LOW**

| Function | Notes |
|----------|-------|
| `exists` / `createDirectories` / `read` / `write` | Pure suspend/blocking functions - call from `LaunchedEffect` or `produceState` if needed in UI |
| `copy` | Already suspend - use directly in coroutine scope |

**Recommendation:** Wrap in `withContext(Dispatchers.IO)` when called from Compose. No `State`/`MutableState` needed.

---

## 7. HSystem.kt

### API
```kotlin
object HSystem {
    fun isInteractive(context: Context): Boolean
    fun isCharging(context: Context): Boolean
    fun checkOpUsageStats(context: Context): Boolean
    fun isForegroundApp(context: Context, packageName: String): Boolean
}
```

### Description
System state checks: screen on, charging, usage stats permission, foreground app detection.

### Dependencies
- Android SDK: `PowerManager`, `BatteryManager`, `AppOpsManager`, `UsageStatsManager`
- `HailData.autoFreezeDelay`

### Compose Migration Needs - **MEDIUM**

| Function | Migration Approach |
|----------|-------------------|
| `isInteractive` | Convert to `State<Boolean>` via `remember { mutableStateOf(...) }` + observer, or use `produceState` with `PowerManager` broadcast |
| `isCharging` | Use `BatteryManager` broadcast receiver → `MutableState` |
| `checkOpUsageStats` | One-time check - no state needed; call in `LaunchedEffect` |
| `isForegroundApp` | Polling-based - convert to `StateFlow`/`MutableState` with periodic updates |

**Recommended Pattern:**
```kotlin
@Composable
fun rememberChargingState(): State<Boolean> = produceState(initialValue = false) {
    val context = LocalContext.current
    val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            value = isCharging(context!!)
        }
    }
    context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    awaitDispose { context.unregisterReceiver(receiver) }
}
```

---

## 8. HLog.kt

### API
```kotlin
object HLog {
    fun i(tag: String, string: String): Unit
    fun e(t: Throwable): Unit
    fun e(string: String): Unit
    fun d(string: String): Unit
    fun w(tag: String, string: String): Unit
    fun e(string: String, t: Throwable): Unit
}
```

### Description
Simple logging wrapper around `android.util.Log` with fixed tag "Hail".

### Dependencies
- `android.util.Log`

### Compose Migration Needs
- **None** - Pure logging, no UI state
- Can be used directly anywhere

---

## 9. HPackages.kt

### API
```kotlin
object HPackages {
    val myUserId: Int
    fun packageUri(packageName: String): String
    fun packageUid(packageName: String): Int
    fun getInstalledApplications(flags: Int = ...): List<ApplicationInfo>
    fun getUnhiddenPackageInfoOrNull(packageName: String, flags: Int = ...): PackageInfo?
    fun getApplicationInfoOrNull(packageName: String, flags: Int = ...): ApplicationInfo?
    fun isAppDisabled(packageName: String): Boolean
    fun isAppHidden(packageName: String): Boolean
    fun isAppStopped(packageName: String): Boolean
    fun isAppSuspended(packageName: String): Boolean
    fun isAppUninstalled(packageName: String): Boolean
    fun isAppPrivileged(packageName: String): Boolean
    fun canUninstallNormally(packageName: String): Boolean
    fun forceStopApp(packageName: String): Boolean
    fun setAppDisabled(packageName: String, disabled: Boolean): Boolean
    fun setAppRestricted(packageName: String, restricted: Boolean): Boolean
}
```

### Description
PackageManager utilities for app state queries and modifications (disable, force-stop, restrict). Uses HiddenApiBypass for some operations.

### Dependencies
- Android SDK: `PackageManager`, `ActivityManager`, `AppOpsManager`
- `org.lsposed.hiddenapibypass.HiddenApiBypass`
- `HailApp.Companion.app`
- Version-specific API handling (`HTarget`)

### Compose Migration Needs - **MEDIUM-HIGH**

**Query functions (no migration needed - stateless):**
- `getInstalledApplications`, `getApplicationInfoOrNull`, `isAppDisabled`, `isAppHidden`, `isAppStopped`, `isAppSuspended`, `isAppUninstalled`, `isAppPrivileged`, `canUninstallNormally`

**Mutation functions (need side-effect handling):**
- `forceStopApp` → Call in `LaunchedEffect` or event handler
- `setAppDisabled` → Call in `LaunchedEffect` + refresh UI state
- `setAppRestricted` → Call in `LaunchedEffect` + refresh UI state

**Recommended Pattern for Mutations:**
```kotlin
@Composable
fun AppControlButtons(packageName: String) {
    val disabledState = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Button(onClick = { scope.launch { 
        val result = HPackages.setAppDisabled(packageName, !disabledState.value)
        if (result) disabledState.value = !disabledState.value
    }}) {
        Text(if (disabledState.value) "Enable" else "Disable")
    }
}
```

---

## Summary: Compose Migration Priority

| Priority | Files | Reason |
|----------|-------|--------|
| **High** | `HUI.kt` | Direct UI operations (toasts, clipboard, navigation) |
| **Medium-High** | `HPackages.kt` | Mutation functions need side-effect handling |
| **Medium** | `HSystem.kt` | System state observation → `State`/`StateFlow` |
| **Low** | `HFiles.kt` | Suspend functions - use in coroutines |
| **None** | `FuzzySearch.kt`, `NineKeySearch.kt`, `PinyinSearch.kt`, `NameComparator.kt`, `HLog.kt` | Pure algorithms/logging |

---

## Recommended Migration Strategy

1. **Create Compose-friendly wrappers** in a new `ui.utils` package
2. **Use `remember`/`mutableStateOf`/`produceState`** for observable system state
3. **Use `LaunchedEffect`/`rememberCoroutineScope`** for side effects
4. **Keep algorithm classes unchanged** - they're already Compose-compatible
5. **Replace `HUI.showToast`** with `SnackbarHost` or `Toast` composable
6. **Inject `Context`/`PackageManager`** via `LocalContext` instead of `HailApp.Companion.app`