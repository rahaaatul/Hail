# Freeze

**Freeze** means blocking an app when it is not needed. Hail can implement that state by force-stopping, disabling, hiding, or suspending an app, depending on the selected working mode. Unfreeze restores the app with the same working mode.

## Force Stop

Force-stop mode stops the app process. It is available through Root, Shizuku, and privileged-system-app modes. Launching the app again clears the stopped state.

## Disable

Disabled apps do not appear in the launcher and are shown as disabled in the installed-apps list. Enable them to revert the action.

## Hide

Hidden apps do not appear in the launcher or the installed-apps list. Unhide them to revert the action. The package becomes unavailable, but its application data and package file remain on the device.

## Suspend (Android 7.0+)

Suspended apps appear in grayscale in the launcher. Their notifications are hidden, started activities are stopped, and the system prevents interaction, toasts, dialogs, and audio. Unsuspend them to revert the action.

::: warning
Suspend prevents user interaction, but it does **not** prevent the app from running in the background.
:::

::: danger
Freezing, disabling, hiding, suspending, or uninstalling system apps can make a device fail to boot. Back up your data and understand the consequence before changing a system app.
:::

See [Working Mode](/guide/working-mode) for the operations available in each mode and [Revert](/guide/revert) for recovery options.