# Revert

Use the same working mode that froze an app whenever possible. Hail's normal unfreeze operation is the safest way to restore an app.

## By ADB

Replace `com.package.name` with the target package name.

```shell
# Enable a disabled app
adb shell pm enable com.package.name

# Unhide an app; root is required
adb shell su -c pm unhide com.package.name

# Unsuspend an app
adb shell pm unsuspend com.package.name
```

For force-stop mode, launch the app again or use Hail's unfreeze operation. A stopped app does not require a separate `pm` command to become launchable.

## Device Owner

If Hail is the device owner, remove device-owner status before uninstalling Hail. Frozen apps remain frozen after device-owner removal, so unfreeze important apps first.

## System Apps

Do not edit Android system restriction files or wipe application data as a shortcut for restoring an app. Those operations can damage the system partition or erase user data. Use the matching Hail mode or the Android package commands above, and recover system changes with a known-good backup or device recovery procedure.