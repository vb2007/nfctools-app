# Advanced NFC Tools — v1 Implementation Plan

## Context

We are building a native **Android** NFC toolkit app from scratch in a currently empty
repository (`nfctools-app`). The long-term product is a full-featured NFC utility
("Advanced NFC Tools"), but **v1 is deliberately narrow**: prove out a clean, modern,
snappy foundation that reads NFC tags and writes a plain-text NDEF record, with
first-class support for the MIFARE Ultralight / NTAG family.

Guiding principles (from the user):
- **Modernity first, backwards-compat second** — use current Kotlin/Compose idioms; only
  reach back in Android versions where it costs nothing meaningful.
- **Works out of the box for general users; fully configurable for power users** — but the
  power-user configuration surface (Settings) is mostly deferred beyond v1. v1 ships sane
  defaults and an (essentially empty) Settings placeholder.
- **Simple, intuitive UI** with light/dark/auto theming.

### Decisions locked with the user
| Topic | Decision |
|---|---|
| v1 operations | Read tags + Write **NDEF plain-text** records |
| Tag scope | Generic NDEF/NFC-Forum tags + full **MIFARE Ultralight family** (UL, UL-C, NTAG213/215/216) |
| Write record types (v1) | **Plain text only** (URL, Wi-Fi, vCard, etc. come later) |
| Read hex dump | Show a **collapsible raw hex dump** for Ultralight/NTAG, **collapsed by default** (read-only, no auth) |
| minSdk | **26** (Android 8.0) |
| Persistence | **Stateless** — no DB/history in v1 |
| DI / architecture | **Koin** + **MVVM** (ViewModel + StateFlow, unidirectional data flow) |
| Theming | **Material You dynamic color** on Android 12+, **custom light/dark palette** fallback below |
| Navigation | **Bottom nav** (Home + Settings for v1). Home hosts the Read & Write actions |
| App ID | `hu.vb2007.nfctool` |
| Display name | "Advanced NFC Tools" (full) / "NFC Tools" (short/launcher) |

---

## Tech stack

- **Kotlin** (latest stable 2.x) + **Jetpack Compose** with the Compose compiler Gradle plugin.
- **Material 3** (`androidx.compose.material3`) incl. dynamic color.
- **Koin** for DI (`koin-androidx-compose`).
- **Coroutines + Flow** for async / NFC event streaming.
- **Navigation-Compose** for the bottom-nav graph.
- **Gradle Kotlin DSL** + **version catalog** (`gradle/libs.versions.toml`).
- **compileSdk / targetSdk = latest stable** (36 / Android 16 as of now — pin the current
  stable at implementation time), **minSdk 26**.
- Single Gradle module `:app` for v1, organized by clean package layers (below). No
  multi-module split yet — unnecessary for this size and slows iteration.

> At implementation time, pin the newest stable versions of AGP, Gradle, Kotlin, Compose
> BOM, and Koin rather than hardcoding today's numbers.

---

## Project structure

```
app/
  build.gradle.kts
  src/main/
    AndroidManifest.xml            # NFC uses-permission + uses-feature(required=false)
    java/hu/vb2007/nfctool/
      NfcToolApp.kt                # Application; starts Koin
      MainActivity.kt              # single Activity; hosts NavHost; owns reader-mode lifecycle
      di/
        AppModule.kt               # Koin module: NfcController, parsers, ViewModels
      nfc/
        NfcController.kt           # central NFC scan/write coordinator (singleton)
        model/
          TagInfo.kt               # UID, techs, tag type, capacity, writable flags
          NdefRecordModel.kt       # parsed record (TNF, type, payload, human-readable)
          TagType.kt               # enum + detection result (UL / UL-C / NTAG213/215/216 / generic)
          ScanResult.kt            # sealed: Success(TagInfo) / Error(reason)
          WriteResult.kt           # sealed: Success / ReadOnly / TooLarge / TagLost / Error
        read/
          TagReader.kt             # reads UID/techs/NDEF from a Tag
          NdefParser.kt            # NdefMessage -> List<NdefRecordModel> (pure, testable)
          TagTypeDetector.kt       # ATQA/SAK + GET_VERSION -> TagType (pure/testable)
          UltralightDumpReader.kt  # page-by-page hex dump for MifareUltralight
        write/
          NdefTextWriter.kt        # build text NdefMessage; write to Ndef/NdefFormatable
      ui/
        theme/                     # Color.kt, Theme.kt (dynamic + fallback), Type.kt
        navigation/                # NavGraph.kt, Destinations.kt, AppBottomBar.kt
        home/                      # HomeScreen.kt (Read/Write action cards)
        read/                      # ReadScreen.kt + ReadViewModel.kt (+ TagResultCard, HexDumpSection)
        write/                     # WriteScreen.kt + WriteViewModel.kt
        settings/                  # SettingsScreen.kt (placeholder for v1)
        components/                # shared composables (ScanPrompt, StatusBanner, etc.)
  src/test/    # JVM unit tests for NdefParser & TagTypeDetector
```

---

## NFC architecture (the core of v1)

Use **NfcAdapter reader mode** (`enableReaderMode` / `disableReaderMode`) rather than the
legacy foreground-dispatch/intent-filter approach — it is the modern, reliable path and
gives us full control of the tag technologies and platform sounds.

- `MainActivity` enables reader mode in `onResume` and disables in `onPause`, delegating the
  discovered `Tag` into `NfcController`. Reader flags cover NFC-A (Ultralight/NTAG) plus the
  other NFC-Forum types; use `FLAG_READER_NO_PLATFORM_SOUNDS` so the app controls feedback.
- **`NfcController`** (Koin singleton) holds:
  - hardware/adapter availability state (`NoHardware` / `Disabled` / `Ready`),
  - a **scan intent** (`Idle` / `Reading` / `Writing(payload)`) set by the active screen,
  - a `SharedFlow` of `ScanResult` / `WriteResult` that ViewModels collect.
- On each discovered `Tag`, `NfcController` runs the appropriate IO **on `Dispatchers.IO`**:
  - `Reading` → `TagReader` builds a `TagInfo`; `UltralightDumpReader` lazily produces the
    hex dump when the tag is an Ultralight/NTAG.
  - `Writing(text)` → `NdefTextWriter` connects and writes the text `NdefMessage`.
- All `Tag` IO wrapped in try/finally with `close()`, catching `TagLostException` and
  `IOException` → mapped to user-friendly `*.Error` states.

### Read details surfaced
UID (hex), technology list, detected `TagType`, total/used capacity, writable &
can-make-read-only flags, and parsed NDEF records (text/URI/etc. all displayed even though
we only *write* text in v1). The **hex dump** renders in a collapsible section, **collapsed
by default**.

### Write flow (v1)
Write screen: a text field + language-code default ("en") → user taps "Write", which sets
`NfcController` intent to `Writing`; the next tag tap writes a single well-known text record.
Handle read-only tags, capacity-exceeded, and unformatted-but-formattable
(`NdefFormatable`) tags.

---

## UI / UX

- **Bottom nav**: `Home` and `Settings`. Home shows two prominent action cards — **Read a
  tag** and **Write a tag** — that navigate to the Read/Write routes (nested under the Home
  graph so the bottom bar persists and Home stays selected). Settings is a placeholder
  scaffold for v1.
- **Scan affordance**: Read/Write screens show a clear "Hold a tag to the back of your
  phone" prompt with animated state; results replace the prompt (inline or bottom sheet).
- **Theme**: `Theme.kt` uses `dynamicLightColorScheme`/`dynamicDarkColorScheme` on API 31+,
  else our custom `Color.kt` palettes; light/dark auto-selected via `isSystemInDarkTheme()`.
- **NFC-off / no-hardware** states surface a `StatusBanner` with a shortcut to system NFC
  settings when applicable.

---

## Manifest / config

- `<uses-permission android:name="android.permission.NFC" />`
- `<uses-feature android:name="android.hardware.nfc" android:required="false" />` — app still
  installs on non-NFC devices and degrades gracefully.
- No internet permission (fully offline).
- Launcher label = "NFC Tools"; full app name = "Advanced NFC Tools".

---

## Out of scope for v1 (explicitly deferred)
Writing non-text records (URL/Wi-Fi/vCard), raw page write & Ultralight password auth
(PWD/PACK)/lock bits, MIFARE Classic / DESFire / FeliCa, scan history & export/persistence,
a populated Settings screen (dump-visibility toggle etc.), HCE/emulation.

---

## Verification

1. **Build**: `./gradlew assembleDebug` compiles cleanly; `./gradlew lint` passes.
2. **Unit tests** (JVM, no hardware): `./gradlew test` — cover `NdefParser` (well-known
   text/URI decoding, malformed payloads) and `TagTypeDetector` (SAK/ATQA + GET_VERSION →
   correct `TagType`). These are pure functions, so they are the reliable automated gate.
3. **On-device manual test** (NFC required — emulators cannot do NFC):
   - Read: tap an NTAG21x / Ultralight tag → verify UID, tech list, detected type,
     capacity, parsed NDEF, and that the hex dump expands correctly.
   - Read a blank/unknown tag → verify graceful UID-only display.
   - Write: enter text, tap a writable tag, confirm success; re-read (or use another NFC
     reader app) to confirm the text record; confirm read-only and too-large tags produce
     clear errors.
   - Toggle system NFC off and remove-NFC scenario → verify banners/degradation.
   - Toggle system light/dark + (on Android 12+) change wallpaper → verify dynamic theming
     and fallback palette.

---

## Suggested build order
1. Gradle skeleton (version catalog, `:app`, Compose + Koin wiring, manifest, `NfcToolApp`).
2. Theme (dynamic + fallback) + navigation scaffold (bottom nav, Home, empty Settings).
3. NFC data layer: models, `NfcController`, `TagReader`, `NdefParser`, `TagTypeDetector`.
4. Read screen + ViewModel (incl. collapsible hex dump via `UltralightDumpReader`).
5. Write screen + ViewModel (`NdefTextWriter`).
6. Edge-case/error states + polish; unit tests; on-device verification.
