# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.7.0] - 2026-03-06

### Added
- **Media Controls:** Added Previous, Play/Pause, and Next buttons directly to the floating widget for quick track control.
- **Dynamic Visibility:** Media buttons automatically show up when adjusting media volume (`STREAM_MUSIC`) and update their state based on active background audio sessions.
- **Media App Detection:** Native support (`android.intent.action.MEDIA_BUTTON`) for controlling background music players (e.g. Spotify, YouTube Music) on Android 11+.
- **Pigeon Integration:** Added `MediaApi` and `MediaStateStream` streams to support future Flutter-side metadata displays.
- **Service Rebind Mechanism:** Added automatic reconnection (`requestRebind`) to `NotificationListenerService` to fix common Android issues where background listeners fail to start.

## [0.6.1] - 2026-02-21

### Fixed
- **Audio Adjustment Bug**: Fixed an issue where the floating slider would stop controlling system volume after the app was closed from recent tasks. This was caused by BLoCs being tied to the MainActivity lifecycle instead of the background service.
- **Auto-start on Android 12+**: Resolved a `ForegroundServiceStartNotAllowedException` occurring on device boot by replacing the delayed `Handler` in `BootReceiver` with an `AlarmManager` deferred intent.

## [0.6.0] - 2026-02-21

### Added
- **Auto-start on boot** - Service can now automatically start when device boots
  - Configurable toggle in settings section
  - Startup delay configurable from 0-30 seconds (default: 3s)
  - Option to restore previous service state after reboot
  - Uses `RECEIVE_BOOT_COMPLETED` permission
  - New `BootReceiver` handles boot and app update events
- **Accessible crash logs** - Logs moved to user-accessible directory
  - Automatic file rotation when exceeding 5 MB
  - Export logs as ZIP file for easy sharing
  - Clear all logs functionality with confirmation dialog
  - Log statistics display (file count, total size, dates)
  - Improved log format with unique IDs, timestamps, and thread names
  - Logs saved in `/Android/data/.../files/logs/` instead of hidden root directory
- **Settings UI section** - New dedicated section in home screen
  - Auto-start on boot toggle
  - Restore service state toggle
  - Startup delay slider (0-30 seconds)
- **Logs management UI** - Debug section in home screen
  - View crash logs statistics
  - Export logs button (shares via Intent)
  - Clear logs button with confirmation dialog
  - Real-time file count and size display

### Changed
- Crash logs now saved in accessible `/files/logs/` directory with automatic rotation
- Log format improved with unique IDs (`[timestamp]`), timestamps (`yyyy-MM-dd HH:mm:ss.SSS`), and thread information
- Service state now persisted in SharedPreferences for auto-start functionality
- Home screen reorganized with new Settings and Logs sections

### Fixed
- Kotlin version upgraded to 2.1.0 (fixes compatibility warnings with Flutter SDK)
- Improved error handling in crash log saving with better exception management
- FileProvider configured for secure file sharing (fixes Android 11+ file access restrictions)

### Technical
- New Android components:
  - `receiver/BootReceiver.kt` - Handles boot completion and app updates
  - Enhanced `CrashHandler.kt` with rotation, export, and stats features
  - FileProvider configuration (`res/xml/file_paths.xml`)
- New Flutter BLoC:
  - `bloc/settings/` - Manages auto-start and restore state preferences
- API additions:
  - 6 new Pigeon methods for auto-start configuration
  - 3 new Pigeon methods for logs management
  - New `LogStatsData` class for log statistics
- SharedPreferences keys:
  - `auto_start_enabled`, `restore_service_state`, `auto_start_delay_ms`, `last_service_state`

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
