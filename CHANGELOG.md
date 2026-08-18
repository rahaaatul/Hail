# Changelog

All notable changes to this project will be documented in this file.

## [1.10.1] - 2026-08-18

### Added
- Select all / deselect all toggle in Apps tab toolbar

### Changed
- Release APK is now built as arm64-v8a only by default
- Updated Android Gradle Plugin to 9.2.1
- Updated multiple dependencies to latest versions

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
