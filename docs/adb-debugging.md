# ADB & USB Debugging

Assumes: USB debugging + ADB debugging enabled on the phone (charge-only USB mode is fine — it doesn't block ADB), `adb`/`scrcpy` on `PATH`.

## Connect

```bash
adb devices
```

Should list your phone. On first connect, accept the "Allow USB debugging?" prompt on the phone screen — until you do, the device won't show up here at all.

If nothing shows up after accepting the prompt:

```bash
adb kill-server && adb start-server && adb devices
```

## Mirror the screen (to watch the UI while tapping tags with your other hand)

```bash
scrcpy
```

## Watch app logs

```bash
adb logcat --pid=$(adb shell pidof -s hu.vb2007.nfctool)
```

(the app must already be running; for logs from launch, use an unfiltered `adb logcat | grep -i nfctool` instead)

## Launch / stop the app manually

The app must already be installed (see [`build-test-package.md`](build-test-package.md)) — launching an activity that was never installed fails with `Error type 3: Activity class {...} does not exist`.

```bash
adb shell am start -n hu.vb2007.nfctool/hu.vb2007.nfctool.MainActivity
adb shell am force-stop hu.vb2007.nfctool
```

Check it's actually alive (a crash right after launch won't always show up as a launch error):

```bash
adb shell pidof hu.vb2007.nfctool
```

## Diagnosing a crash

If the app dies, `logcat`'s live buffer rotates fast and can lose the stack trace by the time you go looking for it. The **dropbox** crash log survives that and is the reliable source:

```bash
adb shell dumpsys dropbox --print | grep -B2 -A100 "vb2007"
```

Look for `data_app_crash` entries — they contain the full exception + stack trace (e.g. a Koin `NoDefinitionFoundException`, a null-pointer in a Composable, etc.).

## Uninstall

```bash
adb uninstall hu.vb2007.nfctool
```
