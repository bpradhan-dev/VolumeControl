# Quick Volume (Android 14)

A lightweight, ad-free Android volume replacement app built for phones with broken physical volume buttons—designed to stay **~0% idle battery** using a 100% event-driven architecture.

## Why This Exists
When physical volume keys stop working, most Play Store volume apps are bloated, ad-heavy, or drain battery running background services. **Quick Volume** solves that with clean native controls right where you need them.

## Features (v1.0)
- **Full Stream Control:** Independent sliders and `-` / `+` buttons for **Media**, **Call**, **Ringtone**, **Notifications**, and **Alarm**.
- **Sound Mode Switcher:** Quick toggle between **Normal**, **Vibrate**, and **Silent** (with safe Android 14 Do Not Disturb permission handling).
- **Quick Settings Tile:** Pull down the Quick Settings shade and tap **Volume** to open a floating mini-panel over any app.
- **Notification Panel Controls:** Optional persistent notification bar with quick controls for Media, Call, and Sound Mode.
- **Home Screen Widget:** Resizable widget for Media, Call, Ringtone, Sound Mode, and quick panel access.
- **Zero Background Drain:** Uses `AppWidgetProvider`, `TileService`, and `BroadcastReceiver`—no polling loops or background timers.

## Tech Stack
- **Language:** Kotlin
- **UI:** Android XML + Material 3 (`DayNight`)
- **Target SDK:** Android 14 (API 34) | **Min SDK:** Android 8.0 (API 26)
- **Tooling:** Built in VS Code with Gradle 8.9 & Temurin JDK 17; tested on Android 14 device

## Download & Install
1. Go to the [**Releases**](https://github.com/bpradhan-dev/VolumeControl/releases) page.
2. Download **`QuickVolume-v1.0.apk`**.
3. Open the APK on your Android phone (allow *Install from unknown sources* if prompted).
4. *(Optional)* Grant **Notification** permission and **Do Not Disturb access** inside the app if you want the Notification bar and Silent mode.

## Build from Source

    git clone https://github.com/bpradhan-dev/VolumeControl.git
    cd VolumeControl
    gradle assembleDebug

## Roadmap (v2.0 Planned)
- Material You Dynamic Colors & Material 3 thick pill sliders
- Haptic feedback ticks
- Tap-to-slide segmented bars in the Notification Panel (focused on Call & Ringtone)