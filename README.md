# Quick Volume

My phone's physical volume buttons stopped working, and every app I found on the Play Store felt heavy, full of ads, or annoying to use. So I built my own simple, lightweight volume controller for **Android 8.0 and above**.

No ads. No tracking. Nothing running silently in the background draining your battery.

## What It Can Do (v2.0)

- **All 5 Volume Controls:** Separate sliders and `-` / `+` buttons for **Media**, **Call**, **Ringtone**, **Notifications**, and **Alarm**.
- **Notification Panel Controls:** Control volume straight from your notification shade. Pick **only** the controls you actually want there (for example, just Call and Ringtone).
- **No Annoying Top Icon:** Keeps your top status bar clean—no permanent speaker icon sitting next to your clock.
- **Won't Disappear by Mistake:** If you accidentally swipe the notification away, it pops right back automatically (you can turn this off anytime in the app).
- **Home Screen Widget:** Adjust **Media**, **Call**, **Ringtone**, and **Sound Mode** directly from your home screen with quick tap-to-slide bars.
- **Quick Settings Tile:** Add the **Volume** tile to your Quick Settings shade to pop open floating sliders over any app.
- **Matches Your Wallpaper:** Uses Material You colors on Android 12+, including inside the notification controls.
- **~0% Idle Battery:** Only runs when you actually tap a button or drag a slider.

## How to Install

1. Go to the [**Releases**](https://github.com/bpradhan-dev/VolumeControl/releases) page.
2. Download **`QuickVolume-v2.0.apk`** (or `QuickVolume-v1.0.apk` if you prefer v1).
3. Open the APK on your Android phone and allow *Install from unknown apps* if asked.
4. *(Optional)* Turn on **Notification Panel Controls** inside the app, and allow **Do Not Disturb access** if you want to use Silent mode.

## Built With

- Kotlin & Android XML (Material 3)
- Works on **Android 8.0+** (Min SDK 26 | Target SDK 34)
- Coded in **VS Code** using Gradle 8.9 & JDK 17 (no Android Studio!)

## Build It Yourself

    git clone -b v2 https://github.com/bpradhan-dev/VolumeControl.git
    cd VolumeControl
    gradle assembleDebug