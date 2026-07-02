# Building a Release Package

## Build

```bash
./gradlew assembleRelease
```

APK lands at `app/build/outputs/apk/release/app-release-unsigned.apk`.

## Current status: unsigned

There's no release signing config yet, so that APK is **unsigned** and won't install as-is. Proper release signing (keystore + Gradle signing config, kept out of git) will be set up when this gets wired into a GitHub Actions release workflow.

For a local one-off install in the meantime, sign it with the debug key:

```bash
$ANDROID_HOME/build-tools/37.0.0/apksigner sign \
  --ks ~/.android/debug.keystore --ks-pass pass:android \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk

adb install -r app-release-signed.apk
```
