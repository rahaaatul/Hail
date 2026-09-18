# Backup & Restore

Hail can export and import its management data as a ZIP file. Use this to move your configuration to another device or keep a copy before making major changes.

## Backup

Open **Settings**, choose **Backup**, select the data to include, and choose a destination ZIP file.

Available categories are:

- **Apps**: checked apps on Home.
- **Whitelist**: apps marked as whitelisted.
- **Actions**: saved multi-app actions and their dependencies.
- **Settings**: Hail preferences, including working mode and automation options.

The backup contains Hail's management data. It does not contain application data, APK files, or a full Android device backup.

## Restore

Open **Settings**, choose **Restore**, select a ZIP file, and choose the categories to restore.

- Restoring apps adds their package names to Hail's checked list.
- Restoring the whitelist marks the listed packages as whitelisted.
- Restoring actions recreates saved actions and dependencies.
- Restoring settings applies the stored preferences to the current installation.

A package must be installed on the destination device before it can be managed. Review restored settings, especially the working mode, before running bulk operations.

## Safety

Back up your data before changing system apps or switching to a privileged or device-owner setup. Keep backup files somewhere you control; they can contain package names and Hail preferences.