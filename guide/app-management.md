# App Management

Hail separates app management into Home, Apps, Actions, and Settings.

## Home

Home shows the apps you have checked. Use it to freeze or unfreeze individual apps, organize them into tags, and run bulk operations.

- Tap the floating action button to open the Apps screen and add apps to Home.
- Use a tag tab to view a group of apps.
- Long-press an app to open its actions, including freeze, unfreeze, pin, whitelist, tag, deferred tasks, and export options.
- Use the multiselect button to select several apps. Long-press it to select or deselect every app in the current tag.
- Pull to refresh the current list after installing, removing, freezing, or unfreezing apps.
- Remove an app from Home when you no longer want it in your checked list. This does not uninstall the app.

Hail cannot be selected for freeze or unfreeze operations, including bulk actions and API calls.

## Apps

The Apps screen lists installed applications and lets you add them to Home.

- Search by app name or package name. T9 and Pinyin search are available when enabled in Settings.
- Filter by User, System, or All apps, and independently filter by frozen or unfrozen state.
- Sort by name, install time, or update time.
- Use **Select all** to select the displayed apps. Hail is excluded automatically.
- Open an app's context menu for app details, clipboard export, APK extraction, uninstall, or reinstall when the selected working mode supports the operation.

Be careful with system apps: disabling, hiding, suspending, or uninstalling them can make a device fail to boot. Back up your data and understand the consequence before changing a system app.

## Tags

Tags group checked apps so you can freeze, unfreeze, or manage a related set together. Create a tag from an app's context menu, assign apps to one or more tags, and use the tag tabs on Home to work with that group.

## Cache and Refresh

Hail keeps app metadata and icons in memory and on disk so lists open quickly. Pull to refresh when the installed-app inventory changes. If names, states, or icons remain stale, open **Settings > Cache > Clear and rebuild cache**.

## Working Mode

Freeze operations use the working mode selected in Settings. An app frozen with one mode must be unfrozen with the same mode. See the [Working Mode guide](/guide/working-mode) before changing system apps.