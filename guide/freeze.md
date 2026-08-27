# Freeze

**Freeze** is a word that describes the action of **blocking (immediately stopping) apps when they are not needed/in-use (on-demand request)** which in turn helps the device to cut down on the usage of RAM and save power. Users can also unfreeze them to revert to their original state.

In general, "freeze" means disable, but also Hail can "freeze" apps by hiding and suspending them.

## Disable

Disabled apps will not be shown in the launcher and will be shown as "Disabled" in the installed apps list. Enable them to revert the action.

## Hide

Hidden apps will not be shown in the launcher and in the installed apps list. Unhide them to revert the action.

> While in this state, which is almost like an uninstalled state, the package will be unavailable, however, the application data and the actual package file will not be removed from the device.

## Suspend (Android 7.0+)

Suspended apps will have their icons shown in grayscale within the device's launcher. Unsuspend them to revert the action.

> While in this state, the application's notifications will be hidden, any of its started activities will be stopped and it will not be able to show toasts, dialogs or even play audio. When the user tries to launch a suspended app, the system will, instead, show a dialog to the user informing them that they cannot use this app while it is suspended.

::: warning
Suspend only prevents the user from interacting with the app, it does **NOT** prevent the app from running in the background.
:::
