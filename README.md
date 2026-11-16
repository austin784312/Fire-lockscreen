# Fire Lock Screen

A phone-style lock screen for a Firestick: big clock, date, and live Toledo weather. Press any button on the remote and it opens Projectivy.

## Build it

**Option 1: GitHub (no Android tools on your laptop)**
1. Make a new GitHub repo and upload everything in this folder, including the `.github` folder.
2. Open the repo's **Actions** tab. The "Build APK" run starts on its own and takes about 3 minutes.
3. Open the finished run and download **lockscreen-apk**. Unzip it to get the `.apk`.

**Option 2: Android Studio**
Open this folder in Android Studio, let it sync, then use Build → Build App Bundle(s) / APK(s) → Build APK(s).

## Install it on the Firestick

1. Copy the `.apk` to your Pi's website folder, for example as `lockscreen.apk`.
2. On the stick, open Downloader and enter `http://<your-site>/lockscreen.apk`. Install it.
3. Open **Launch on Boot** and change the app from Projectivy to **Lock Screen**.
4. Restart the stick (hold Select + Play/Pause).

## Changing things

Everything you'd want to tweak sits at the top of `MainActivity.java`: the city name, the coordinates for the weather, and which launcher opens when you press a button. The wallpaper colors are in `WallpaperView`.
