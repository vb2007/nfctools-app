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

```bash
adb shell am start -n hu.vb2007.nfctool/hu.vb2007.nfctool.MainActivity
adb shell am force-stop hu.vb2007.nfctool
```

## Uninstall

```bash
adb uninstall hu.vb2007.nfctool
```
