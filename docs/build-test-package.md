# Building a Test (Debug) Package

## Build + install in one step (recommended)

```bash
./gradlew installDebug
adb shell am start -n hu.vb2007.nfctool/hu.vb2007.nfctool.MainActivity
```

## Build only

```bash
./gradlew assembleDebug
```

APK lands at `app/build/outputs/apk/debug/app-debug.apk`.

## Install that APK manually

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Debug builds are auto-signed with the debug keystore AGP generates at `~/.android/debug.keystore` — nothing to set up.

See [`adb-debugging.md`](adb-debugging.md) for launching, logs, and screen mirroring.
