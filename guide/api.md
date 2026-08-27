# API

```shell
adb shell am start -a action -e key value
```

`action` can be one of the following constants:

| Action | Description | Extra |
|---|---|---|
| `com.aistra.hail.action.LAUNCH` | Unfreeze and launch target app. If it is unfrozen, it will launch directly. | `key="package"` `value="com.package.name"` |
| `com.aistra.hail.action.FREEZE` | Freeze target app. It must be checked at Home. | `key="package"` `value="com.package.name"` |
| `com.aistra.hail.action.UNFREEZE` | Unfreeze target app. | `key="package"` `value="com.package.name"` |
| `com.aistra.hail.action.FREEZE_TAG` | Freeze all non-whitelisted apps in the target tag. | `key="tag"` `value="Tag name"` |
| `com.aistra.hail.action.UNFREEZE_TAG` | Unfreeze all apps in the target tag. | `key="tag"` `value="Tag name"` |
| `com.aistra.hail.action.FREEZE_ALL` | Freeze all apps at Home. | Not necessary |
| `com.aistra.hail.action.UNFREEZE_ALL` | Unfreeze all apps at Home. | Not necessary |
| `com.aistra.hail.action.FREEZE_NON_WHITELISTED` | Freeze all non-whitelisted apps at Home. | Not necessary |
| `com.aistra.hail.action.FREEZE_AUTO` | Auto freeze apps at Home. | Not necessary |
| `com.aistra.hail.action.LOCK` | Lock screen. | Not necessary |
| `com.aistra.hail.action.LOCK_FREEZE` | Freeze all apps at Home and lock screen. | Not necessary |

## Deep Link Schema

You can also use the following `schema`:

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
