# What's New

This page summarizes recent Hail releases. See [GitHub Releases](https://github.com/rahaaatul/Hail/releases) for packages and the complete history.

## 1.11.5 - 2026-09-13

- Stop-mode unfreeze now launches the app to clear stopped state and reports missing launch intents correctly.
- Fixed tag rename and removal races that could throw concurrent-modification exceptions.
- Improved ActionsRepository validation and stop-mode error messages.
- Made AutoFreezeWorker and service lifecycle handling null-safe.
- Fixed an Island mode permission callback race and Island launch package handling.

## 1.11.4 - 2026-09-05

- Root shell warm-up now happens when the main app is opened instead of on every process start.
- Auto-freeze stops retrying after three attempts.
- Unfreeze-and-remove-from-home waits for the asynchronous unfreeze operation.
- Root `pm` operations verify that the acquired shell is actually running as root.

## 1.11.3 - 2026-08-31

- Added the Actions tab for creating, editing, duplicating, deleting, and pinning multi-app actions.
- Added dedicated add buttons for Home and Actions.
- Added silent, cancelable app-list refresh and a faster cached app picker.
- Moved Apps access to the Home floating action button.
- Improved app context-menu names, list freshness, and freeze failure messages.

## 1.11.2 - 2026-08-27

- Added a Room-backed cache for installed app metadata and a disk-backed icon cache.
- Added a Settings action to clear and rebuild app caches.
- Added a battery-optimization exemption option for background auto-freeze.
- Improved frozen and unfrozen visual updates.

## 1.11.1 - 2026-08-27

- Added persistent Root shell support and background shell warm-up.
- Reuses one shell for Root operations while Hail is running.
- Releases and reacquires the shell when switching modes or after restart.

## 1.11.0 - 2026-08-26

- Added select-all and deselect-all controls, including long-press behavior.
- Added the All apps filter for user and system apps.
- Moved About access from bottom navigation to Settings.
- Improved Root and Shizuku command execution and Island/Insular launch behavior.
- Protected Hail from accidental self-freeze through bulk actions and API intents.