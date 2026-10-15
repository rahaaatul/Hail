# Upstream Changes Implementation Notes

This document records the upstream changes that were implemented and consolidated into the "great squash" commit. These changes can be reapplied after resetting to origin/main.

## 1. Translation Commits from Upstream

The following translation commits were cherry-picked from upstream/main:

- `a028106` - Translated using Weblate (Spanish)
- `1d72df1` - Translated using Weblate (Korean)
- `e9c435c` - Translated using Weblate (Korean) [skipped during reapplication - empty after conflict resolution]
- `78d8363` - Translated using Weblate (Chinese Traditional Han script)
- `0263801` - Translated using Weblate (Korean)
- `e795ead` - Translated using Weblate (Thai)
- `a0ccabb` - Translated using Weblate (German)
- `26f3f07` - Translated using Weblate (Thai)

**Files affected**: 
- `app/src/main/res/values-de/strings.xml`
- `app/src/main/res/values-ko/strings.xml` 
- `app/src/main/res/values-th/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/main/res/values/strings.xml`

**Reapplication method**:
```bash
git fetch upstream
git cherry-pick a028106 1d72df1 78d8363 0263801 e795ead a0ccabb 26f3f07
# Skip e9c435c if it causes conflicts (it was empty in our integration)
# Resolve any conflicts by taking upstream version for values files
```

## 2. Jailbreak-Disable Mode (Upstream Commit 3f62339)

Added "Jailbreak - Disable" mode functionality.

### Key Changes:

#### HailData.kt
- Added constant: `const val JAILBREAK = "jailbreak_"`
- Added constant: `const val MODE_JAILBREAK_DISABLE = JAILBREAK + DISABLE`
- Added `MODE_JAILBREAK_DISABLE` to `WORKING_MODE_VALUES` list

#### AppManager.kt
Modified functions to use `with(HailData.workingMode)` pattern and added JAILBREAK handling:

- `lockScreen`: `startsWith(HailData.SU) || startsWith(HailData.JAILBREAK) -> HShell.lockScreen`
- `isAppFrozen`: Generic handling (no special JAILBREAK case needed)
- `setListFrozen`: Added `HailData.MODE_JAILBREAK_DISABLE -> { for (info in excludeMe) { if (!HShell.setAppDisabled(frozen, info)) { ... } } }`
- `uninstallApp`: Added `|| ((startsWith(HailData.SU) || startsWith(HailData.JAILBREAK)) && HShell.uninstallApp(packageName))`
- `reinstallApp`: Added `startsWith(HailData.SU) || startsWith(HailData.JAILBREAK) -> HShell.reinstallApp(packageName)`
- `execute`: Added `startsWith(HailData.SU) || startsWith(HailData.JAILBREAK) -> HShell.execute(command, true)`

#### SettingsFragment.kt
- Added imports:
  ```kotlin
  import android.Manifest.permission.CHANGE_COMPONENT_ENABLED_STATE
  import androidx.core.content.ContextCompat
  import com.aistra.hail.utils.HShell
  ```
- Added JAILBREAK_DISABLE handling in `onWorkingModeChange`:
  ```kotlin
  mode == HailData.MODE_JAILBREAK_DISABLE -> if (
      ContextCompat.checkSelfPermission(
          requireContext(),
          CHANGE_COMPONENT_ENABLED_STATE
      ) == PackageManager.PERMISSION_DENIED
          && !HShell.grantPermission(requireContext().packageName, CHANGE_COMPONENT_ENABLED_STATE)
  ) {
      HUI.showToast(R.string.permission_denied)
      return false
  }
  ```

### Reapplication method:
Apply the changes manually to the files as described above, or cherry-pick commit 3f62339 and resolve conflicts.

## 3. Biometric Fix (Upstream Commit 966b096)

Fixed biometric authentication prompt.

### Key Change:
#### MainActivity.kt
Changed line in `promptBiometricLogin()`:
```kotlin
// FROM:
.setSubtitle(getString(R.string.msg_biometric)).setNegativeButtonText(getString(android.R.string.cancel))

// TO:
.setSubtitle(getString(R.string.msg_biometric)).setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
```

### Reapplication method:
```bash
# In MainActivity.kt, replace the setNegativeButtonText line with setAllowedAuthenticators
sed -i 's/\.setNegativeButtonText(getString(android.R.string.cancel))/\.setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)/' app/src/main/kotlin/com/aistra/hail/ui/main/MainActivity.kt
```

## 4. Consolidation Process

All changes were consolidated into a single commit using:
```bash
git reset --soft ca156af  # Stage all changes from upstream
git commit -m "chore: the great squash"
```

## 5. Files to Verify After Reapplication

After reapplying changes, verify:
- `HailData.kt`: Contains JAILBREAK and MODE_JAILBREAK_DISABLE constants
- `AppManager.kt`: JAILBREAK handling in lockScreen, setListFrozen, uninstallApp, reinstallApp, execute
- `SettingsFragment.kt`: JAILBREAK_DISABLE permission check in onWorkingModeChange
- `MainActivity.kt`: Uses setAllowedAuthenticators instead of setNegativeButtonText
- Values files: Updated translations in values-de/, values-ko/, values-th/, values-zh-rTW/

## Notes:
- This document is intentionally NOT git-tracked so it won't be affected by git reset/pull operations
- To reapply all changes: fetch upstream, apply translations, then manually apply the jailbreak and biometric changes as documented
