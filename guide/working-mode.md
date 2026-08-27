# Working Mode

**Any app that has been frozen on Hail will need to be unfrozen by the same working mode.**

1. For devices supporting wireless debugging (Android 11+) or rooted devices, `Shizuku` is recommended.

2. For rooted devices, `Root` is an alternative. **It is slower.**

In Root mode, Hail starts a cached libsu shell in the background when the app process starts. This moves the root authorization delay away from the first freeze or unfreeze action while keeping the UI responsive. The shell is reused for all Root operations in that process, including operations on different apps.

If Root mode is not selected, no root shell is started. Switching away from Root mode closes the cached shell. If root authorization is denied or the shell exits unexpectedly, the failed shell is discarded and the next Root operation attempts to acquire a new one. A new shell is also acquired after Hail is restarted.

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
You must remove Hail as a device owner before you can uninstall it
:::

### Set device owner by adb

[Android Debug Bridge (adb) Guide](https://developer.android.com/studio/command-line/adb)

[Download Android SDK Platform-Tools](https://developer.android.com/studio/releases/platform-tools)

Issue adb command:

```shell
adb shell dpm set-device-owner com.aistra.hail/.receiver.DeviceAdminReceiver
```

In response, adb prints this message if device owner has been successfully set:

```
Success: Device owner set to package com.aistra.hail. Active admin set to component {com.aistra.hail/com.aistra.hail.receiver.DeviceAdminReceiver}
```

Search the message by search engine otherwise.

### Remove device owner

Settings > Remove Device Owner

## Privileged System App

The following privapp-permissions is required:

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

To use this mode, you should install Hail as a privileged system app.

The recommended approach is to import Hail when building your ROM, here's an example for `Android.bp`:

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
