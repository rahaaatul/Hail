# Actions

Actions let you unfreeze one or more apps and then launch another app. They are useful for restoring a group of apps and opening the app you actually need.

## Create an Action

1. Open the **Actions** tab.
2. Tap the floating action button.
3. Select one or more apps to unfreeze.
4. Select the app to launch after the dependencies are unfrozen.
5. Save the action.

The launch app is not kept as a dependency when it is also selected for unfreezing. An action needs at least one dependency and a launch app.

## Manage Actions

Open an action's menu to edit it, create a shortcut, duplicate it, or delete it. Editing preserves the action's identity; duplicating creates a new action.

## Shortcuts

Create a shortcut to place an action on the Android home screen. Tapping the shortcut runs the action without opening the Actions screen first.

## Run an Action from Another App

Saved actions can be started with the `LAUNCH_ACTION` intent:

```shell
adb shell am start \
  -a com.aistra.hail.action.LAUNCH_ACTION \
  -e action_id ACTION_ID
```

Replace `ACTION_ID` with the saved action's ID. Hail loads the action, unfreezes its dependencies, and launches its target app. If the action is missing or an app is unavailable, Hail reports an error.

See the [API reference](/guide/api) for the complete intent list.