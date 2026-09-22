fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android metadata_check

```sh
[bundle exec] fastlane android metadata_check
```

Validate store metadata

### android build_debug

```sh
[bundle exec] fastlane android build_debug
```

Build debug APK

### android build_pr

```sh
[bundle exec] fastlane android build_pr
```

Build PR APK

### android build_release

```sh
[bundle exec] fastlane android build_release
```

Build signed release APK

### android github_release

```sh
[bundle exec] fastlane android github_release
```

Create or update GitHub Release

### android telegram_notify

```sh
[bundle exec] fastlane android telegram_notify
```

Send Telegram notification

### android release

```sh
[bundle exec] fastlane android release
```

Full release orchestration

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
