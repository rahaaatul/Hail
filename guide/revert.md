# Revert

## By adb

Replace `com.package.name` to the package name of target app.

```shell
# Enable app
adb shell pm enable com.package.name
# Unhide app (root required)
adb shell su -c pm unhide com.package.name
# Unsuspend app
adb shell pm unsuspend com.package.name
```

## Modify file

Access `/data/system/users/0/package-restrictions.xml`, this file stores the restrictions about apps. You can modify, rename or just delete it.

- **Enable app**: Modify the value of `enabled` from 2 (DISABLED) or 3 (DISABLED_USER) to 1 (ENABLED)

- **Unhide app**: Modify the value of `hidden` from true to false

- **Unsuspend app**: Modify the value of `suspended` from true to false

## Wipe data by recovery

None of my business :(
