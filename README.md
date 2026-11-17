# Quick Volume

A lightweight, ad-free volume replacement app for **Android 8.0 and above**—built for phones with broken physical volume buttons and designed for **~0% idle battery** using a 100% event-driven architecture.

## Why This Exists
When physical volume keys stop working, most Play Store volume apps are bloated, ad-heavy, or drain battery running background services. **Quick Volume** solves that with clean native controls right where you need them.

## Features (v1.0)
- **Full Stream Control:** Independent sliders and `-` / `+` buttons for **Media**, **Call**, **Ringtone**, **Notifications**, and **Alarm**.
- **Sound Mode Switcher:** Quick toggle between **Normal**, **Vibrate**, and **Silent** (with safe Do Not Disturb permission handling).
- **Quick Settings Tile:** Pull down the Quick Settings shade and tap **Volume** to open a floating mini-panel over any app.
- **Notification Panel Controls:** Optional persistent notification bar with quick controls for Media, Call, and Sound Mode.
- **Home Screen Widget:** Resizable widget for Media, Call, Ringtone, Sound Mode, and quick panel access.
- **Zero Background Drain:** Uses `AppWidgetProvider`, `TileService`, and `BroadcastReceiver`—no polling loops or background timers.

## Tech Stack & Compatibility
- **Language:** Kotlin
- **UI:** Android XML + Material 3 (`DayNight`)
- **Supported Versions:** Android 8.0+ (Min SDK 26 | Target SDK 34)
- **Tooling:** Built in VS Code with Gradle 8.9 & Temurin JDK 17; tested on a real Android device

## Download & Install
1. Go to the [**Releases**](https://github.com/bpradhan-dev/VolumeControl/releases) page.
2. Download **`QuickVolume-v1.0.apk`**.
3. Open the APK on your Android phone (allow *Install from unknown sources* if prompted).
4. *(Optional)* Grant **Notification** permission (Android 13+) and **Do Not Disturb access** inside the app if you want the Notification bar and Silent mode.

## Build from Source

    git clone https://github.com/bpradhan-dev/VolumeControl.git
    cd VolumeControl
    gradle assembleDebug

## Roadmap (v2.0 Planned)
- Material You Dynamic Colors & Material 3 thick pill sliders
- Haptic feedback ticks
- Tap-to-slide segmented bars in the Notification Panel (focused on Call & Ringtone)