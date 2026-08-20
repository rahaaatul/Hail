# Changelog

All notable changes to this project will be documented in this file.

## [1.10.4] - 2026-08-20

### Added
- **Progress notifications for bulk operations** - Heads-up notifications showing current app being frozen/unfrozen, progress bar, and success/failed summary
- **Working speed setting** - Choose between Aggressive (200ms delay, batches of 10), Balanced (500ms delay after 4 apps), and Relaxed (sequential, no artificial delay) in Settings
- **Cancel bulk operation** - Tap "Cancel" in notification to abort ongoing freeze/unfreeze
- **Batched pm commands for SU modes** - Single su process executes multiple pm commands, reducing overhead 3-5x

### Changed
- **Bulk operations run on background thread** - UI no longer freezes during freeze/unfreeze, especially with su_disable mode
- **Throttling now configurable** - Default "Balanced" preserves previous behavior (pause after 4 apps), Relaxed is truly sequential

### Fixed
- Crash when switching tabs during freeze operation
- Toolbar up navigation on About page
- System stuttering during bulk freeze/unfreeze with SU/Shizuku modes

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

### Fixed
- Various translation updates via Weblate
