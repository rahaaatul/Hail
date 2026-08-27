# Changelog

All notable changes to this project will be documented in this file.

## [1.11.0] - 2026-08-26

### Added
- Select all / deselect all controls in the Apps tab, including long-press support
- "All" filter option to display both user and system apps
- Universal APK build script for local release builds

### Changed
- About page access moved from bottom navigation to Settings
- Updated Android Gradle Plugin to 9.3.1 and Kotlin to 2.4.10
- Updated Material3, AndroidX, Compose, lifecycle, navigation, WorkManager, and other dependencies
- Updated `androidx.biometric:biometric-ktx` to 1.4.0-alpha02
- Improved command execution in Root and Shizuku working modes

### Fixed
- Root modes not working on older Android versions, thanks to @LuoYunXi0407
- `Island/Insular - Hide` mode unable to launch apps on some stock ROMs, thanks to @andy-math
- A `NullPointerException` in the interface, thanks to @lerdb
- Dynamic shortcuts removal when biometric login is enabled (#377)
- Foreground service `specialUse` property (#409)
- `pm` commands failing on older Android versions due to `--user current` (#416)
- Launch intent existence check in `Island/Insular - Hide` mode
- Hail self-freeze through Select All, API intents, or bulk operations

### Removed
- Xposed API dependency in favor of libxposed API 102
- Confirmation dialog when switching to the System apps filter
- About tab from the bottom navigation bar

### Translations
- Updated Turkish, Spanish, Korean, Chinese, Indonesian, Ukrainian, Bengali, Italian, Belarusian, French, Russian, Tamil, Portuguese, Urdu, German, Japanese, Polish, Norwegian Bokmål, Finnish, Arabic, Persian, and Vietnamese translations

## [1.10.4] - 2026-08-21

### Added
- Long-press on multiselect button to select all/deselect all apps in current tab
- Back press in multiselect mode now deselects all and exits multiselect
- Visual feedback: icon changes from select_all to checkmark with color tint
- Self-protection: Hail app cannot be selected for freeze/unfreeze operations (visible in list but checkbox disabled)

### Fixed
- Prevent accidental self-freeze via Select All, API intents, or bulk operations

## [1.10.3] - 2026-08-18

### Removed
- About tab from bottom navigation bar

### Added
- About access from Settings tab via info icon
- Back navigation button now appears on About page
- Bottom navigation and nav rail hide when viewing About page

## [1.10.2] - 2026-08-18

### Added
- "All" filter option in Apps tab to display both user and system apps

### Removed
Confirmation dialog when switching to System apps filter

## [1.10.1] - 2026-08-18

### Added
- Select all / deselect all toggle in Apps tab toolbar

### Fixed
- Dynamic shortcuts removal when biometric login is enabled (#377)
- Foreground service specialUse property (#409)
- `pm` commands failing on older Android versions due to `--user current` (#416)
- Launch intent existence check in Island/Insular Hide mode

### Translations
- Turkish, Spanish, Korean, Chinese (Simplified), Indonesian, Ukrainian, Bengali, Italian

## [1.10.0] - 2026-07-15

### Added
- Compose Preference support
- Pinyin search support for app filtering

### Changed
- Migrated to Android Gradle Plugin 9.x
- Updated Kotlin to 2.3.21
- Updated Material3 to 1.13.0

### Fixed
- Various translation updates via Weblate
