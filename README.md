<a href="https://f-droid.org/en/packages/com.github.mkalmousli.floating_volume"><img src="https://img.shields.io/f-droid/v/com.github.mkalmousli.floating_volume?label=F-Droid" alt="Current F-Droid version"/></a> <a href="https://github.com/mkalmousli/FloatingVolume/releases/latest"><img src="https://img.shields.io/github/v/tag/mkalmousli/FloatingVolume?label=GitHub" alt="Current GitHub version"/></a>

<p align="center">
    <img src="logo.svg" width="200px" />
</p>
<h1 align="center">Floating Volume</h1>
<p align="center">
    Control your device’s volume anywhere with a floating slider on Android.
</p>



## What's New in v0.6.0

- **🚀 Auto-start on boot**: Service automatically starts when device boots (configurable)
- **📋 Accessible crash logs**: Export and manage logs easily from the app
- **⚙️ Settings section**: Configure auto-start, restore state, and startup delay
- **🔍 Logs management**: View stats, export logs as ZIP, or clear all logs
- **🛠️ Kotlin 2.1.0**: Updated for better compatibility with Flutter SDK

## Get it

F-Droid is the official and recommend way to get Floating Volume:

<a href="https://f-droid.org/en/packages/com.github.mkalmousli.floating_volume">
    <img src="https://f-droid.org/badge/get-it-on.svg" width="250px" />
</a>

F-Droid takes a while to publish new versions, if you want to get latest version, you can get it from GitHub releases:

<a href="https://github.com/mkalmousli/FloatingVolume/releases/latest">
    <img src="get_it_on_github.png" width="200px"/>
</a>


## Screenshots

<table>
    <tr>
        <td>
            <img src="screenshots/output/01.png" width="150px" />
            <img src="screenshots/output/02.png" width="150px" />
            <img src="screenshots/output/03.png" width="150px" />
            <img src="screenshots/output/04.png" width="150px" />
            <img src="screenshots/output/05.png" width="150px" />
            <img src="screenshots/output/06.png" width="150px" />
        </td>
    </tr>
</table>


## Features

- 🎵 **Floating volume control** - Draggable volume slider accessible from any app
- 🔊 **Smart audio detection** - Automatically adjusts to active audio stream (calls, media, alarms, notifications)
- 📱 **Repositionable widget** - Drag and place anywhere on screen
- 🌓 **Theme support** - Light, Dark, or System theme
- 🔔 **Persistent notification** - Quick access controls (Stop, Show/Hide)
- **🚀 Auto-start on boot** (NEW v0.6.0)
  - Configurable startup delay (0-30 seconds)
  - Restore previous service state option
- **📋 Crash logs management** (NEW v0.6.0)
  - Accessible log directory
  - Export and share logs as ZIP
  - Automatic rotation (max 5 MB per file)
  - View statistics (file count, size, dates)

## Permissions

Required permissions and their purpose:

| Permission | Purpose |
|------------|---------|
| `SYSTEM_ALERT_WINDOW` | Display floating widget over other apps |
| `MODIFY_AUDIO_SETTINGS` | Control system volume levels |
| `FOREGROUND_SERVICE` | Keep service running in background |
| `POST_NOTIFICATIONS` | Show persistent notification |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevent battery optimization from killing service |
| `RECEIVE_BOOT_COMPLETED` | Auto-start service on device boot (NEW v0.6.0) |

## Build

This app uses [Pigeon](https://pub.dev/packages/pigeon) to generate Dart and Kotlin code for Flutter → Android communication.

1. Generate required code with:

```bash
dart run pigeon --input pigeons/native_api.dart
dart run pigeon --input pigeons/native_events.dart
```


2. Create `key.properties` in `android/`, here is an example:

```bash
storePassword=your_password
keyPassword=your_password
keyAlias=key0
storeFile=/path/to/your_keystore.jks
```

> [!NOTE]  
> Make sure to build the app while having `.git`, also not a from shallow clone or from a tarball.

2. Then, you should be able to build it with flutter:

```bash
flutter build apk --release
```

The output will be in `build/app/outputs/flutter-apk/app-release.apk`.

This is not considered a reproducible build, as it depends on the environment, such as the OS, the version of the JDK, the location of the source code, and so on.

Look below if you want to build it in a reproducible way.


### Reproducible Build

This app is meant to be a Reproducible build for F-Droid.

The building environment does play a crucial role in the build process, for example which OS, where the source code is located, or the version of the JDK used.

To make this easier, for me to build, I have created a simple script that tries to mimic the F-Droid build environment as closely as possible.

```bash
docker build -t floating_volume .
docker run --rm -v $(pwd):/tmp/app floating_volume /tmp/app/build.py
```

This will result in `app.apk` in the current directory, which is a reproducible build of Floating Volume.



## License
[![GNU GPLv3 Image](https://www.gnu.org/graphics/gplv3-127x51.png)](https://www.gnu.org/licenses/gpl-3.0.en.html)  

Floating Volume is Free Software: You can use, study, share, and improve it at will. Specifically you can redistribute and/or modify it under the terms of the [GNU General Public License](https://www.gnu.org/licenses/gpl.html) as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
