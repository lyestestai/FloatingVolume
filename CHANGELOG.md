# Changelog

## 0.9.10+100 (In-App Diagnostic Log Window & Export for Automotive Head-Units)
* **Collapsible Diagnostic Log Window (`lib/src/screens/home.dart`)**: Added `DiagnosticLogsWidget` directly inside the Home Screen with an activatable/deactivatable toggle (*"Journal de diagnostic (Logs)"*) and a scrollable terminal view.
* **Copy, Share (Email/Text Editor) & Save to Disk (`pigeons/native_api.dart`, `NativeApiImpl.kt`, `CrashHandler.kt`)**:
  - **Copier**: Copies the full diagnostic report to the Android clipboard.
  - **Partager / Email**: Launches Android's `ACTION_SEND` (`text/plain`) intent with the full diagnostic text in `EXTRA_TEXT` so email clients or text editor apps can send or save it directly.
  - **Enregistrer (`Download/`)**: Writes `FloatingVolume_Log_<timestamp>.txt` directly into `/storage/emulated/0/Download/` on the head-unit's storage.
  - **Actualiser & Effacer**: Refresh or clear the recorded diagnostic events at any time.
* **Early-Boot Persistent Event Logger (`CrashHandler.kt`, `BootReceiver.kt`, `FloatingVolumeService.kt`)**: Added `CrashHandler.logEvent()` writing timestamped + monotonic uptime (`[up:Xs]`) entries to internal storage (`filesDir/logs/diagnostic_log_current.txt`) alongside a real-time hardware header (displayed Android version vs real `SDK_INT`, `elapsedRealtime` uptime to distinguish Cold Boot from MCU standby, Overlay/Battery permissions, and `SharedPreferences` state).

## 0.9.9+99 (Bulletproof Auto-Start for Android 8+ Automotive Head-Units)
* **Manifest & MCU Wakeup Fix (`AndroidManifest.xml`)**: Enabled `android:exported="true"`, `android:directBootAware="true"`, and high priority (`999`) on `BootReceiver`. Added support for Chinese Head-Unit MCU wakeup / standby exit intents (`QUICKBOOT_POWERON`, `USER_PRESENT`, `ACTION_POWER_CONNECTED`, `com.syu.ms.action.ACC_ON`, `com.tw.action.ACC_ON`, `com.glsx.boot.ACCON`, `com.microntek.bootcheck`, `autochips.intent.action.QB_POWERON`) and set `android:stopWithTask="false"` on `FloatingVolumeService`.
* **Unconditional Auto-Start on Boot (`BootReceiver.kt`)**: Removed the blocking `if (restoreState && !wasServiceRunning) return` check that aborted startup whenever the car's MCU stopped services during ignition power-off (`ACC OFF`).
* **Dual-Stage Auto-Start Architecture (`BootReceiver.kt` & `FloatingVolumeService.kt`)**:
  - **Stage 1 (Immediate Foreground Protection + Internal Delay)**: Starts `FloatingVolumeService` immediately inside `onReceive` so the process is protected in Foreground (`startForeground()`) right away, and executes the 10s–60s delay inside the service coroutine before initializing `AudioManager` and attaching `FloatingVolumeView`.
  - **Stage 2 (Monotonic Safety-Net Alarm)**: Schedules a backup `AlarmManager.ELAPSED_REALTIME_WAKEUP` alarm using `SystemClock.elapsedRealtime()` (immune to GPS/NTP clock jumps during car startup) in case the head-unit MCU kills processes during early boot cleanup.
* **Autonomous NotificationChannel & Synchronous `startForeground()` (`FloatingVolumeService.kt`)**: Created the `NotificationChannel` directly inside `FloatingVolumeService` (no longer relying on `MainActivity`) and moved `startForeground()` to run synchronously on the Main Thread on every `onStartCommand()`, eliminating Android 8+ 5-second `RemoteServiceException` timeouts.
* **Synchronous Flash Persistence & Default Delay Alignment (`NativeApiImpl.kt` & `home.dart`)**: Aligned `DEFAULT_DELAY_MS` to `10000L` (10s), enforced `10000..60000ms` clamping, and switched `SharedPreferences` writes to synchronous `.commit()` on the IO thread so settings survive sudden ignition cutoff.
* **Architecture & Agent Documentation**: Added comprehensive `architecture.md`, `for_agent.md`, and `Roadmap.md`.

## 0.9.8+98 (Auto-Start UI & DND Fixes)
* **Auto-Start Configuration UI**: Added a control panel in the Home Screen to configure Auto-Start with a configurable delay (10 to 60 seconds). This prevents Android 12+ Background Service crash restrictions.
* **Hardware Diagnostic Conclusion**: Successfully isolated the "Volume stuck at 0" issue to Android's "Do Not Disturb" (DND) state. Floating Volume acted as the perfect diagnostic tool to uncover this native OS behavior, confirming the Head Unit Firmware works perfectly.
* Retained all 7 experimental audio streams for advanced laboratory use.

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.9.2-basic] - Automotive Head-Unit Edition (2026-04-05)

### Added
- **Multi-Channel Toggle:** Added an interactive icon above the CoolSlider. Tapping it manually loops through the target system channels (`Music`, `Alarms / Navigation`, `Calls`, `Ringtones`). This directly solves the Android Automotive issue where Waze volume could not be adjusted independently.
- **Default Music Fallback:** System fallback stream is now forcefully rerouted to `STREAM_MUSIC` instead of `STREAM_NOTIFICATION`, fixing "no effect" bugs.

### Changed
- **Architectural Rollback:** Completely removed all Material 3 UI tabs, glassmorphism, background rendering, `clipToOutline` rounding, and `ViewPropertyAnimator` animations that caused severe graphic driver failures (invisible panels) on older Android Automotive systems.
- **Minimalist Core:** Restored the absolute minimalist UI from version 0.0.3, presenting a single clean status toggle to start the background service without excess functionality. 
- **Volume Interpolation:** Enforced the logarithmic/non-linear math mappings in the native background service (30% physical height maps to 15% audio volume), enabling precise volume control required for driving.
- **Removed Deprecated Plugins:** Automatically purged legacy Dart packages (`volume_controller`) and updated the Kotlin JVM tooling to compile flawlessly on modern Java engines without breaking `25.0.1`.

## [0.8.3] - 2026-03-06

### Fixed
- **Memory Leaks:** Removed infinite `Future.wait` deadlocks and polling loops in `PermissionsBloc` and `StatusBloc` that caused background thread congestion and battery drain.

### Added
- **Live Preview:** Added a mini live representation of the widget in the Appearance tab to show how Corner Radius and Blur adjustments will look before activating them.

### Changed
- **UI Architecture:** Extracted all Settings tabs into modular components, removing massive code duplication from `HomeScreen`.
- **UI Architecture:** Fixed an invisible exception with `Positioned.fill` missing a `Stack` parent.
- **Theming:** Replaced hardcoded default colors with dynamic Material 3 tokens.

## [0.8.2] - 2026-03-06

### Fixed
- **Memory Leak:** Fixed a critical bug where `FloatingVolumeView` coroutines were not cancelled on view detach, causing background UI threading congestion.
- **Media Controls Disappearing:** Removed `LayoutTransition` and fixed animation cancellations that caused the entire multimedia control bar to randomly get stuck in an invisible state after multiple toggles.

## [0.8.1] - 2026-03-06

### Added
- **Flutter UI Redesign:** Completely refactored the main app settings interface adopting Material 3 design principles.
- **Categorized Navigation:** Replaced the continuous scrolling list with a modern `BottomNavigationBar` separating options into "Appearance", "Behavior", and "About" tabs.
- **Interactive Visual Controls:** Added a dedicated "Appearance" tab allowing real-time customization of the floating widget's Glassmorphism (blur), Material You dynamic colors, and Corner Radius.
- **Pigeon Communication:** Implemented a synchronous Pigeon API channel (`AppearanceApi`) sending Dart UI preferences directly into the Native Kotlin View for instant live previews.
- **State Persistence:** Wired the new visual customization controls to `rx_shared_preferences` ensuring settings survive app restarts and device reboots.

## [0.8.0] - 2026-03-06

### Added
- **Visual Overhaul ("Woah Effect"):** Completely redesigned the floating widget for a modern "Premium" look.
- **Glassmorphism:** Added true blur behind effects for the floating widget container on Android 12+ devices.
- **Material You:** The volume slider progress bar now dynamically adapts to the user's system theme and wallpaper colors using `system_accent1_500` (Android 12+).
- **Fluid Animations:** Added smooth scale, fade, and overshoot/anticipate animations when showing and hiding the widget and media controls.
- **Rounded Design:** Changed the widget background to a fully rounded pill shape with a semi-transparent glass effect.
- **Interactive Feedback:** Added standard Android modern ripple effects (`selectableItemBackgroundBorderless`) to all media buttons for better tactile feedback.

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
