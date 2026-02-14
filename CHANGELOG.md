# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.5.0rc1] - 2026-02-14

### Added
- **Dynamic Volume Scaling**: The app now dynamically adapts to the device's audio stream types and maximum volume levels, ensuring correct mapping across different devices.
- **Crash Logging**: Implemented a local `CrashHandler` to capture and log uncaught exceptions for better debugging.
- **Slider Toggle**: A tap on the floating handle now toggles the visibility of the volume slider, improving screen real estate usage.

### Changed
- **UI Updates**:
    - Increased the slider height to **900px** to provide finer control over volume adjustments.
    - Updated `FloatingVolumeView` to support multi-channel volume logic.

### Fixed
- **Tablet Stability**: Fixed a critical crash on tablet devices caused by incorrect volume stream handling.

## [0.0.3] - 2025-10-11

### Added
- Initial F-Droid release preparation.
- Reproducible build scripts.

### Changed
- Downgraded AGP to 8.11.1 for compatibility.
- Upgraded Gradle to 8.13 for AGP support (subsequent fix).
