# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

This is a **greenfield Android project** — as of this writing there is **no source code yet**, only planning artifacts. The app ("Advanced NFC Tools", short name "NFC Tools") is a native Android NFC toolkit for reading tags and writing NDEF records, with first-class MIFARE Ultralight / NTAG support.

**The authoritative spec is [`planning/v1-plan.md`](planning/v1-plan.md).** Read it before writing any code — it defines v1 scope, the package layout, the NFC architecture, and the intended build order. This CLAUDE.md summarizes the decisions that should be treated as settled; the plan has the detail.

## Locked decisions (do not re-litigate without asking)

- **App ID**: `hu.vb2007.nfctool` — all source lives under `hu/vb2007/nfctool/`.
- **v1 scope**: read tags + write **plain-text NDEF only**. Generic NDEF/NFC-Forum tags plus the full MIFARE Ultralight family (UL, UL-C, NTAG213/215/216). **Stateless** — no persistence/history/DB in v1.
- **Stack**: Kotlin (latest stable 2.x), Jetpack Compose + Material 3, **Koin** for DI, **MVVM** (ViewModel + StateFlow, unidirectional data flow), Coroutines/Flow, Navigation-Compose. Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`). Single `:app` module.
- **minSdk 26**; compileSdk/targetSdk = latest stable. Pin newest stable AGP/Gradle/Kotlin/Compose-BOM/Koin at implementation time rather than hardcoding older versions.
- **Theming**: Material You dynamic color on API 31+, custom light/dark palette fallback below; auto light/dark via `isSystemInDarkTheme()`.
- **Navigation**: bottom nav with Home + Settings only in v1. Home hosts the Read & Write actions; Settings is an empty placeholder.
- **Guiding principle**: works out of the box for general users, deep configurability for power users — but the power-user config surface (Settings) is deferred beyond v1. Modernity first, backwards-compat second.

## Architecture (once scaffolded)

The core is the NFC layer under `nfc/`:

- Use **`NfcAdapter` reader mode** (`enableReaderMode`/`disableReaderMode`), **not** legacy foreground-dispatch/intent-filters. `MainActivity` (single Activity) owns the reader-mode lifecycle in `onResume`/`onPause`.
- **`NfcController`** (Koin singleton) is the hub: it holds adapter availability state, a **scan intent** (`Idle` / `Reading` / `Writing(payload)`) set by the active screen, and exposes a `SharedFlow` of results that ViewModels collect. All tag IO runs on `Dispatchers.IO`, wrapped in try/finally with `close()`, catching `TagLostException`/`IOException` into user-friendly error states.
- Keep NDEF/tag-type parsing as **pure functions** (`NdefParser`, `TagTypeDetector`) so they're unit-testable without hardware — these are the primary automated test target.

## Commands (valid once the Gradle project exists)

```bash
./gradlew assembleDebug   # build
./gradlew test            # JVM unit tests (NdefParser, TagTypeDetector)
./gradlew lint            # Android lint
./gradlew installDebug    # install on a connected device
```

Run a single test:
```bash
./gradlew test --tests "hu.vb2007.nfctool.nfc.read.NdefParserTest"
```

## Testing notes

- **Emulators cannot do NFC.** Read/write flows must be verified on a physical device with an NTAG21x / Ultralight tag. Automated coverage is limited to the pure parser/detector functions.
