# Nor Launcher (`com.nor.launchernor`)

A fast, customizable, privacy-first Android home-screen launcher built with **Kotlin**, **Jetpack Compose**, and **Material Design 3**. Designed with effortless one-handed ergonomics, a full Smart Widgets pack, personalized India + Global news briefing, and curated Gen Z-friendly aesthetics.

- **Application ID:** `com.nor.launchernor`
- **Minimum Android Version:** Android 8.0 (API 26)
- **Target SDK:** Android 15 (API 35)
- **Compile SDK:** Android 16 (API 36.1)
- **Tech Stack:** Kotlin `2.4.21`, Jetpack Compose, Material 3, DataStore Preferences, WorkManager, OkHttp, Coil, BiometricPrompt

---

## Features

### 1. Home Screen, Gestures & Smart App Drawer
- **Categorized App Drawer:** Swipe up from Home to browse apps automatically organized into *Communication, Internet, Games, Media, Productivity, Utilities,* and *Other*.
- **Custom Categories:** Create, rename, and delete your own categories, and long-press any app to move it or reset it to auto-categorization.
- **Customizable Dock:** Pin or unpin favorite apps directly from the app long-press menu.
- **Home Gestures:** Assign actions (*Open App Drawer, Lock Screen, Open Notifications, Open Quick Settings, Open Search, Open Hidden Apps, None*) to **Swipe Up**, **Swipe Down**, and **Double-Tap**.
- **Notification Dots:** Live unread badge counts on home and drawer icons powered by Android's `NotificationListenerService`.

### 2. Nor Search (Universal Launcher Search)
- **Instant App Launching:** Find and open installed apps by name right from the Home screen **Nor Search** bar.
- **Persistent Recent Searches:** Keeps your recent queries in a quick-tap dropdown list with individual removal and one-tap clear.
- **Inline Calculator:** Evaluate math directly in the search bar (e.g., `18% * 240`, `(45 + 15) / 3`) and copy results in one tap.
- **Contacts & Quick Actions:** Search device contacts (on-demand permission) to call or message directly, or run system actions like `wifi`, `bluetooth`, `alarm 7:30`, `call`, or `settings`.
- **Web Search Fallback:** One-tap DuckDuckGo search for any query.

### 3. Full Smart Widgets Pack & Android AppWidget Hosting
- **Swipeable Smart Widget Stack & Pinnable Widgets:** Swipe vertically through built-in smart widgets or pin standalone widgets directly onto your Home screen:
  - **Clock:** Minimal Clock & Date, Glass Clock & Battery, Compact Battery & Date, and live ticking **Analog Clock**.
  - **Live Weather:** Real-time conditions, feels-like, high/low, humidity, and wind speed via city search or coarse location (`°C` / `°F` toggle).
  - **Music Player:** Live track info and playback controls (previous, play/pause, next) connected to active media sessions.
  - **Calendar:** Upcoming 7-day schedule preview with one-tap refresh and Calendar app launch.
  - **Battery Telemetry:** Live charge level, charging source (AC / USB / Wireless), temperature, and health status.
  - **Quick Note:** Instant on-home scratchpad and checklist.
  - **Icon Group:** Customizable quick-launch app group right on Home.
- **Native Android Widget Hosting (`AppWidgetHost`):** Add, configure, resize (width & height), reorder, and remove third-party widgets from your installed Android apps.

### 4. Personalized News Briefing (India + Global)
- **Choose Your Contexts & Channels:** Swipe right from Home for your personalized Briefing. Pick contexts (*India, World, Tech, Business, Science, Sports, Entertainment*) and only the news channels you trust.
- **14 Built-In Indian News Channels:**
  - *India National:* **NDTV India**, **The Hindu**, **Times of India**, **The Indian Express**, **Hindustan Times**, **India Today**
  - *Indian Tech:* **Gadgets 360**, **The Hindu Tech**
  - *Indian Business:* **Economic Times**, **Livemint**
  - *Sports & Cricket:* **NDTV Sports**, **TOI Cricket**
  - *Entertainment:* **NDTV Movies**, **TOI Entertainment**
- **Global & Custom Feeds:** Mix in BBC, NPR, Al Jazeera, The Verge, Ars Technica, Hacker News, CNBC, NASA, ScienceDaily, ESPN, or add any custom `https://` RSS/Atom feed URL.
- **Gen Z Quick-Pick Focus Filters:** One-tap trending focus chips (`IPL`, `Cricket`, `AI`, `Startups`, `Gaming`, `Anime`, `Bollywood`, `Tech`, `Music`, `Space`) plus custom keywords and an optional strict keyword filter.
- **Background Sync:** Automatically refreshes headlines every 30 minutes via `WorkManager`.

### 5. Vibe & Theme, Live Wallpapers & Icon Packs
- **Aesthetic Color Palettes (Light & Dark M3 Schemes):**
  - **Material You** (Android 12+ dynamic system colors) & **Wallpaper Colors**
  - **Cyber Y2K** (acid lime, electric cyan & obsidian)
  - **Matcha Cream** (ceremonial matcha, oat milk & warm caramel)
  - **Neon Berry** (hyper magenta, electric violet & midnight plum)
  - **Nordic Slate** & **Forest Sage**
- **Procedural Live Wallpapers & Photo Picker:**
  - Built-in Canvas backdrops: **Cyber Mesh**, **Matcha Aura**, **Midnight Vapor**, **Nordic Peaks**, **Aurora Glass**, **Forest Horizon**, and **Sunset Dunes**.
  - Pick any gallery image with Android's zero-permission **Photo Picker** (`PickVisualMedia`), tune the readability dim overlay, and optionally apply it as your system wallpaper.
- **Typography, Icon Shapes & Icon Packs:**
  - Fonts: *Outfit, Plus Jakarta Sans, Space Grotesk, JetBrains Mono,* and *System Sans*.
  - Adaptive shapes: *Squircle, Circle, Rounded,* and *Teardrop*.
  - Supports installed standard Android icon packs (`appfilter.xml` — Nova, Smart Launcher, ADW, Apex, Go).
  - Optional immersive mode (*Hide system bars*).

### 6. Hidden Apps Vault (Privacy & Security)
- Hide any app from Home, Drawer, and Search.
- Protected by **BiometricPrompt** (fingerprint/face) plus an on-device **salted & hashed 4–8 digit fallback PIN** with automatic lockout after repeated failed attempts.

---

## Permissions & Privacy

Nor Launcher follows least-privilege Android & Google Play guidelines:

| Permission / System Service | Required? | Purpose |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Install-time | Fetching RSS news feeds, live weather, and DuckDuckGo search suggestions |
| `USE_BIOMETRIC` | Install-time | Unlocking the Hidden Apps vault via device biometrics |
| `SET_WALLPAPER` | Install-time | Optional "Set as Android wallpaper too" action |
| `SET_ALARM` | Install-time | Launching Clock alarm quick actions from Nor Search |
| `READ_CONTACTS` | Optional (Runtime) | Only requested if you enable Contact Search in Nor Search |
| `ACCESS_COARSE_LOCATION` | Optional (Runtime) | Only requested if you tap "Use location" in the Weather widget (manual city entry requires no permission) |
| `READ_CALENDAR` | Optional (Runtime) | Only requested if you allow Calendar preview in the Calendar widget |
| Notification Access Service | Optional (Settings) | Counts unread notifications per package for Notification Dots and reads active media sessions — **never reads, logs, or uploads message text** |
| Accessibility Service | Optional (Settings) | Enables Double-Tap to Lock Screen (Android 9+) and notification shade gestures — `canRetrieveWindowContent` is strictly **`false`** |

> **Zero storage permissions:** Custom wallpapers use Android's built-in `PickVisualMedia` Photo Picker. App discovery uses targeted `<queries>` filters rather than `QUERY_ALL_PACKAGES`.

---

## Building the APK

### Option A: Automated GitHub Actions Build (Recommended)
This repository includes `.github/workflows/build-apk.yml` configured for **`com.nor.launchernor`**:

1. Push to `main` / `master`, open a Pull Request, push a tag (`v*`), or go to **Actions → Build Nor Launcher APK (`com.nor.launchernor`) → Run workflow**.
2. Once the workflow finishes, download **`nor-launcher-com.nor.launchernor-debug-apk`** from the workflow run's **Artifacts** section.
3. *(Optional) Signed Release APKs:* Add these three **Repository Secrets** under *Settings → Secrets and variables → Actions*:
   - `KEYSTORE_BASE64`: Base64-encoded `.jks` upload keystore (`base64 -w 0 my-upload-key.jks`)
   - `STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password for alias `upload`

When all three secrets are present, the workflow automatically builds and uploads `nor-launcher-com.nor.launchernor-release.apk` as well.

### Option B: Local Build
**Prerequisites:** JDK 21, Android SDK with Platform 36.1, and Gradle `9.3.1` on your `PATH`.

```bash
# 1. Decode the debug keystore (first time only)
base64 -d debug.keystore.base64 > debug.keystore

# 2. Build the Debug APK
gradle :app:assembleDebug --stacktrace
```

Output APK location: `app/build/outputs/apk/debug/app-debug.apk`

---

## What to Do & What NOT to Do (Preventing Mishaps)

### For Users

#### ✅ DO
- **Keep your stock launcher installed:** Set Nor Launcher as default via *Android Settings → Apps → Default apps → Home app*, and always keep your phone's built-in launcher installed as a safe fallback.
- **Remember your Hidden Apps PIN:** Your fallback PIN is salted and hashed locally on-device with no cloud backup. If forgotten, the only reset is clearing app data (which resets launcher layout).
- **Verify the package ID when sideloading:** Make sure the APK you install has package ID `com.nor.launchernor` and comes directly from this repository's GitHub Actions or Releases.
- **Grant optional permissions selectively:** You only need Notification Access, Accessibility, Contacts, Calendar, or Coarse Location if you use those specific widgets or gestures.
- **Back up custom RSS links if switching devices:** Preferences and custom feed URLs are stored locally via Jetpack DataStore.

#### ❌ DO NOT
- **Do NOT disable or uninstall your OEM/stock launcher** using ADB or root tools — if an update ever crashes or resets, Android needs a fallback Home app.
- **Do NOT grant Accessibility or Notification Access to unofficial or tampered APKs** downloaded from unknown third-party sites.
- **Do NOT expect Screen Lock gestures on Android 8.0 / 8.1:** Android's `GLOBAL_ACTION_LOCK_SCREEN` API requires Android 9 (API 28) or higher.
- **Do NOT switch between Debug and Release APKs without uninstalling first:** Debug and Release builds use different signing keys; Android will block an in-place overwrite with a signature conflict error.

---

### For Developers & Contributors

#### ✅ DO
- **Keep `applicationId` and `namespace` intact:** `applicationId` MUST stay `"com.nor.launchernor"` and `namespace` MUST stay `"com.example"`.
- **Use JDK 21 + Gradle `9.3.1`:** Always run `gradle :app:assembleDebug` (or let `.github/workflows/build-apk.yml` handle it via `gradle/actions/setup-gradle@v4`).
- **Keep secrets out of Git:** Put release signing keys/passwords exclusively in GitHub Actions Secrets (`KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD`) and never commit `.jks`, `.keystore`, or `.env` files.
- **Respect Google Play & Android safety boundaries:** Keep using `PickVisualMedia` for images, targeted `<queries>` in `AndroidManifest.xml`, and `canRetrieveWindowContent="false"` in `accessibility_service_config.xml`.
- **Run a build check before pushing:** Verify `gradle :app:assembleDebug` passes cleanly after any dependency, resource, or enum changes (especially exhaustive `when` blocks on `NewsTopic`, `ThemeColorSource`, and `WallpaperPreset`).

#### ❌ DO NOT
- **Do NOT delete or overwrite `debug.keystore.base64` or edit `signingConfigs` / `buildTypes` signing assignments in `app/build.gradle.kts`:** Breaking the debug keystore breaks in-place APK upgrades across test devices and CI builds.
- **Do NOT commit `local.properties`:** Configure `ANDROID_HOME` / `ANDROID_SDK_ROOT` via environment variables instead so CI and other machines aren't broken by hardcoded paths.
- **Do NOT run `./gradlew` in CI or scripts without generating wrapper binaries first:** This repo intentionally tracks `gradle/wrapper/gradle-wrapper.properties` and installs Gradle `9.3.1` via `setup-gradle`.
- **Do NOT bump AGP (`9.1.1`), Kotlin (`2.4.21`), KSP (`2.3.5`), or `compileSdk` (`36.1`) independently:** These versions are pinned as a verified compatible group.
- **Do NOT add broad permissions** such as `READ_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `MANAGE_EXTERNAL_STORAGE`, or `QUERY_ALL_PACKAGES`.
- **Do NOT introduce `!!` non-null assertions** in Kotlin code — handle nullable Android system managers, cursors, and intents defensively so the launcher never crashes on OEM edge cases.

---

## Troubleshooting

| Issue | Cause & Fix |
|---|---|
| **`App not installed` / `INSTALL_FAILED_UPDATE_INCOMPATIBLE`** | You are installing an APK signed with a different key (e.g., Release over Debug, or a different debug keystore). Uninstall the existing `com.nor.launchernor` build first, then install the new APK. |
| **Double-tap to lock doesn't work** | Enable *Nor Launcher Gestures* in *Android Settings → Accessibility*. Note that screen locking requires Android 9 (API 28)+. |
| **Notification dots or Music track info not updating** | Enable *Nor Launcher Notification Dots* in *Android Settings → Notification access*, then swipe back to Home. |
| **News Briefing shows no stories** | Check internet connectivity and verify you selected at least 1 context and 1 news channel in **Personalize**. If *Only show stories matching focus keywords* is turned on, turn it off or broaden your keywords. |
| **Third-party widget shows "Widget unavailable"** | The source app may have been uninstalled or updated its widget provider. Long-press the widget card to remove and re-add it. |
| **GitHub Actions build fails on SDK license** | Ensure the *Accept Android SDK licenses* step in `.github/workflows/build-apk.yml` remains intact and `ANDROID_HOME` is available on `ubuntu-latest`. |
