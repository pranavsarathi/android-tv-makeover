# Android TV Makeover: a clean cat-wallpaper home screen

Turn a cluttered Android TV home screen (ads, banners, recommendation rows) into this:

- a full-screen **static wallpaper** (ours is five cats)
- a row of **your favourite apps** (Netflix, Prime Video, JioHotstar, YouTube)
- a small green **hacker-style monitor** with a welcome message and live **RAM** and **CPU clock** meters

Nothing else. No ads on the home screen.

> **Tested on:** Xiaomi / Mi TV model `MiTv-AXSo0`, Android 9, 1280x720 screen, about 1 GB RAM.
> Other Android TVs *might* work, but nobody has tested them. See [What is tested](#what-is-tested-and-what-is-not).

## Before and after

## Before vs. After

| Before: Stock Home Screen | After: Custom Launcher |
|:---:|:---:|
| <img src="https://github.com/user-attachments/assets/86b5d260-24ba-407f-ab08-b0a15edf7b49" width="400" alt="Stock home screen"> | <img src="https://github.com/user-attachments/assets/4fb331d3-2d02-4be1-aefa-5385e9525bfe" width="400" alt="Custom launcher"> |

*The "before" photo was taken of the TV screen. The "after" image is a screenshot from the TV.*

## Where to read what

| I want to... | Read |
|---|---|
| Understand the idea and the design, and why the stock launcher is turned off | [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) |
| Copy every command, from the first Wi-Fi connection to the end | [docs/COMMANDS.md](docs/COMMANDS.md) |
| Run ready-made scripts | [scripts/](scripts) |
| See what is proven and what is not | [docs/VALIDATION.md](docs/VALIDATION.md) |
| Fix a broken home screen | [docs/RECOVERY.md](docs/RECOVERY.md) |
| Learn the words (launcher, ADB, APK...) | [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md) |

---

## The idea in 30 seconds

Your TV's home screen is just an app, called a **launcher**. Android lets you install a different launcher and choose it as "Home". This project is a custom launcher written in Java.

You install it from your computer over Wi-Fi using a tool called **ADB**. The stock launcher stays on the TV the whole time, so you can always switch back.

---

## What you need

| Thing | Why |
|---|---|
| An Android TV (Android 9 tested) | The thing we are customising |
| A Linux computer (Ubuntu tested) | To build the app and talk to the TV |
| TV and computer on the **same Wi-Fi/network** | ADB works over the network |
| **Java 17** | To build the app |
| **Android SDK** (platform 34) with `ANDROID_HOME` set | To build the app |
| **ADB** | To install the app on the TV |
| ImageMagick (optional) | To resize your wallpaper |

On Ubuntu: `sudo apt install openjdk-17-jdk adb imagemagick`
The Android SDK comes from Android Studio or the command-line tools: https://developer.android.com/studio

---

## Step by step

### 0. Get the code
```bash
git clone https://github.com/pranavsarathi/android-tv-makeover.git
cd android-tv-makeover/launcher
```

### 1. Turn on ADB on the TV
1. On the TV open **Settings, Device Preferences, About** and press OK on **Build** 7 times. This unlocks Developer options.
2. Open **Developer options** and switch on **USB debugging** (and **Network debugging** if your TV has it).
3. Find the TV's IP address (Settings, Network). We'll call it `<TV_IP>`.

Menu names differ between brands, so look around.

### 2. Connect from your computer
```bash
adb connect <TV_IP>:5555
adb devices
```
You should see your TV listed as `device`. The TV may show an "Allow debugging?" popup. Choose **Allow**.

### 3. Pick your wallpaper (optional)
Your picture should be **1280x720** (16:9). Put it here, keeping the same file name:
```
launcher/app/src/main/res/drawable-nodpi/wallpaper_1.jpg
```
If your picture is not 16:9, this fills the screen by cropping (adjust the `+0+180` number to move the crop up or down):
```bash
convert my-photo.jpg -resize 1280x -crop 1280x720+0+180 +repage -quality 90 \
  app/src/main/res/drawable-nodpi/wallpaper_1.jpg
```
Use **one** wallpaper. The launcher fades between several wallpapers if there are more than one, and we want it static.

### 4. Choose your apps and your name
- **Apps in the row:** edit `launcher/app/src/main/res/values/config.xml`, the `dock_packages` list. Each line is an app's *package name*. Apps that are not installed are skipped. Set `columns` to the number of apps you actually have, or you get an empty gap after the last one.
- **Welcome text:** edit the `WELCOME` line near the top of `launcher/app/src/main/java/com/kulwant/atvhome/MonitorView.java`.

To find a package name: `adb shell pm list packages | grep -i netflix`

### 5. Build
```bash
./gradlew assembleDebug
```
When it says `BUILD SUCCESSFUL`, the app is at `app/build/outputs/apk/debug/app-debug.apk`.

### 6. Install and preview (safe)
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.kulwant.atvhome/.HomeActivity
```
This opens the launcher once but does **not** make it your default. If the TV shows a "choose Home app" popup, pick the stock one with **Just once**.

If you replaced the wallpaper and still see the old one, run `adb shell pm clear com.kulwant.atvhome` and open it again. The app copies its wallpaper into its own storage the first time it runs.

### 7. Make it the real home screen (optional, read the warning first)
If both launchers are enabled, Android asks which one to use. Choosing "Always" on ours is the gentle way.

Going further means **disabling** the stock launcher so it never appears. That is what the author did. **Read [docs/RECOVERY.md](docs/RECOVERY.md) first.** Once the stock launcher is disabled, our launcher is the *only* home screen, so a bug in it can leave you without one. Keep the recovery commands handy, and keep a working APK copy.

### 8. Stop the photo screensaver (optional)
```bash
adb shell settings put secure screensaver_enabled 0
```
Undo with `... 1`. The TV can still turn the screen off by itself. The author set the screen-on time to **10 hours**, see [docs/COMMANDS.md section 10](docs/COMMANDS.md#10-screensaver-and-screen-time).

---

## What is tested, and what is not

Verified on the real TV:
- [x] It builds and installs
- [x] Wallpaper fills the screen and is static
- [x] 4-app dock shows with no empty gap (`columns` = 4)
- [x] Live RAM and CPU-clock meters
- [x] The stock launcher and PatchWall (Xiaomi's home screen) were disabled and this launcher became the Home app
- [x] Screensaver switched off

---

## Things I learned (and honest limits)

- **The boot logo can't be removed.** The Mi/Android logo and boot animation run before any app starts, so a launcher can't touch them. Using standby instead of cutting power avoids most cold boots.
- **There is no power meter.** The TV reports a fake battery (`dummy-battery`) that never changes, and the temperature sensors are blocked for apps. So the monitor shows RAM and CPU clock only, and never a made-up power number.
- **CPU shows clock speed, not percent usage.** Android 8+ blocks apps from reading `/proc/stat`, which is where usage comes from.
- **Ads inside Netflix, YouTube and other apps are not removed.** This only cleans the home screen.
- **The audio spectrum was tried and removed** because it was clutter. It needed a microphone-style permission, and protected audio (Netflix, Prime) may not be readable.

---

## Project layout

```
launcher/                   the custom launcher (the main project)
  app/src/main/java/...     the Java code (HomeActivity, MonitorView, ...)
  app/src/main/res/         wallpaper, settings (config.xml), icons
scripts/                    validated helper scripts (01 to 06) and recovery-UNTESTED.sh
docs/                       ARCHITECTURE, COMMANDS, VALIDATION, RECOVERY, HOW-IT-WORKS, images
apps/live-tv/               a separate app, not part of the launcher
```
More detail: [docs/HOW-IT-WORKS.md](docs/HOW-IT-WORKS.md)

---

## Credits and license

**This project is built on [android-tv-makeover](https://github.com/kulwantnagi/android-tv-makeover) by Kulwant Nagi.** The launcher, the helper scripts in the project root and the Live TV app come from his repository, and his commit history is kept in this repo. The Live TV app is itself a fork of kananinirav/Indian-IPTV-App (MIT, see `apps/live-tv/LICENSE`).

**What was added here, by Pranav Sarathi:**
- the RAM and CPU-clock monitor card with the welcome line (`MonitorView.java`)
- the static cat wallpaper setup and the crop method
- the 4-app dock fix
- the screensaver and 10-hour screen-time setup
- the guides, scripts and validation log in `docs/` and `scripts/`
- testing on a Mi TV (`MiTv-AXSo0`, Android 9)

**License:** see [LICENSE](LICENSE). The original project's license terms apply to the code that comes from it.
