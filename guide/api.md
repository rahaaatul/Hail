# API

Hail exposes Android intents and deep links for app management and automation.

```shell
adb shell am start -a ACTION -e KEY VALUE
```

Replace `ACTION` with one of the following action constants.

| Action | Description | Extra |
|---|---|---|
| `com.aistra.hail.action.LAUNCH` | Unfreeze and launch the target app. If it is already unfrozen, launch it directly. An optional tag can be supplied. | `key="package"` `value="com.package.name"`; optional `key="tag"` |
| `com.aistra.hail.action.LAUNCH_ACTION` | Load and run a saved multi-app action. | `key="action_id"` `value="ACTION_ID"` |
| `com.aistra.hail.action.FREEZE` | Freeze the target app. The app must be checked on Home. | `key="package"` `value="com.package.name"` |
| `com.aistra.hail.action.UNFREEZE` | Unfreeze the target app. | `key="package"` `value="com.package.name"` |
| `com.aistra.hail.action.FREEZE_TAG` | Freeze checked apps in the target tag. | `key="tag"` `value="Tag name"` |
| `com.aistra.hail.action.UNFREEZE_TAG` | Unfreeze checked apps in the target tag. | `key="tag"` `value="Tag name"` |
| `com.aistra.hail.action.FREEZE_ALL` | Freeze checked apps on Home. | Not necessary |
| `com.aistra.hail.action.UNFREEZE_ALL` | Unfreeze checked apps on Home. | Not necessary |
| `com.aistra.hail.action.FREEZE_NON_WHITELISTED` | Freeze checked, non-whitelisted apps on Home. | Not necessary |
| `com.aistra.hail.action.FREEZE_AUTO` | Schedule auto-freeze using the configured delay and skip rules. | Not necessary |
| `com.aistra.hail.action.LOCK` | Lock the screen. | Not necessary |
| `com.aistra.hail.action.LOCK_FREEZE` | Freeze checked apps on Home and lock the screen. | Not necessary |

## Deep Link Schema

Hail also handles these `hail://` links:

- `hail://launch?package=xxx`
- `hail://freeze?package=xxx`
- `hail://unfreeze?package=xxx`
- `hail://freeze_tag?tag=xxx`
- `hail://unfreeze_tag?tag=xxx`
- `hail://freeze_all`
- `hail://unfreeze_all`
- `hail://freeze_non_whitelisted`
- `hail://freeze_auto`
- `hail://lock`
- `hail://lock_freeze`

Intents and links run with the working mode and permissions currently configured in Hail. If an app is unavailable, an action is missing, or the selected mode cannot perform the operation, Hail reports an error.