# Working Mode

**An app frozen with one Hail working mode must be unfrozen with the same working mode.**

For devices with wireless debugging on Android 11 or later, or for rooted devices, Shizuku is recommended. Root is an alternative on rooted devices and can be slower.

## Root Shell

In Root mode, Hail warms a cached libsu shell when the main app is opened. The shell is reused for Root operations while Hail is running, including operations on different apps and bulk actions.

If Root mode is not selected, no root shell is started. Switching away from Root mode closes the cached shell. If authorization is denied or the shell exits unexpectedly, the failed shell is discarded and the next Root operation acquires a new one. Hail also acquires a new shell after restart.

| Privilege | Force Stop | Disable | Hide | Suspend | Uninstall/Reinstall (System Apps) |
|---|---|---|---|---|---|
| Root | ✓ | ✓ | ✓ | ✓ | ✓ |
| Device Owner | ✗ | ✗ | ✓ | ✓ | ✗ |
| Privileged System App | ✓ | ✓ | ✗ | ✗ | ✗ |
| [Shizuku](https://github.com/RikkaApps/Shizuku) (root)/[Sui](https://github.com/RikkaApps/Sui) | ✓ | ✓ | ✓ | ✓ | ✓ |
| [Shizuku](https://github.com/RikkaApps/Shizuku) (adb) | ✓ | ✓ | ✗ | ✓ | ✓ |
| [Dhizuku](https://github.com/iamr0s/Dhizuku) | ✗ | ✗ | ✓ | ✓ | ✗ |
| [Island](https://github.com/oasisfeng/island)/[Insular](https://gitlab.com/secure-system/Insular) | ✗ | ✗ | ✓ | ✓ | ✗ |

## Device Owner

::: danger
Remove Hail as device owner before uninstalling it. Frozen apps remain frozen after removal.
:::

### Set Device Owner by ADB

Install Android Debug Bridge (adb) and Android SDK Platform-Tools, then run:

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

A successful command prints:

```text
Success: Device owner set to package com.aistra.hail. Active admin set to component {com.aistra.hail/com.aistra.hail.receiver.DeviceAdminReceiver}
```

Remove device-owner status from **Settings > Remove Device Owner**.

## Privileged System App

A privileged system app installation requires these permissions:

```xml
<?xml version="1.0" encoding="utf-8"?>
<permissions>
    <privapp-permissions package="com.aistra.hail">
        <permission name="android.permission.PACKAGE_USAGE_STATS"/>
        <permission name="android.permission.FORCE_STOP_PACKAGES"/>
        <permission name="android.permission.CHANGE_COMPONENT_ENABLED_STATE"/>
        <permission name="android.permission.MANAGE_APP_OPS_MODES"/>
    </privapp-permissions>
</permissions>
```

Install Hail as a privileged system app. When building a ROM, import it with `privileged: true` and include the permission file, for example:

```bp
android_app_import {
    name: "Hail",
    apk: "Hail.apk",
    privileged: true,
    dex_preopt: {
        enabled: false,
    },
    presigned: true,
    preprocessed: true,
    required: ["privapp-permissions_com.aistra.hail.xml"]
}

prebuilt_etc {
    name: "privapp-permissions_com.aistra.hail.xml",
    src: "privapp-permissions.xml",
    sub_dir: "permissions",
}
```