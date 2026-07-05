# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

v1 is implemented and builds/tests/lints clean (`./gradlew assembleDebug testDebugUnitTest lintDebug`). The app ("Advanced NFC Tools", short name "NFC Tools") is a native Android NFC toolkit for reading tags and writing NDEF records, with first-class MIFARE Ultralight / NTAG support. Not yet verified on a physical device (no device was connected during initial implementation) — do that before considering v1 done.

**The authoritative spec is [`planning/v1-plan.md`](planning/v1-plan.md).** Read it before writing any code — it defines v1 scope, the package layout, the NFC architecture, and the intended build order. This CLAUDE.md summarizes the decisions that should be treated as settled; the plan has the detail.

## Development environment

- **System/OS package management is manual, done by the user — agents must not run `pacman`/`yay`, or install JDKs.** If something at the OS level appears to be missing, state exactly what's needed and wait rather than trying to install or work around it. This does **not** cover Android SDK components fetched by Gradle/AGP itself during a build (e.g. a missing `platforms;android-NN` or `build-tools;NN` auto-downloaded into `$ANDROID_HOME`) — that's project-scoped tooling, not a system package, and is fine to let happen.
- JDK: system default is JDK 26 (`java-26-openjdk`), with **JDK 21** also installed (`java-21-openjdk` — check with `archlinux-java status`). The project pins JDK 21 for actual compilation **portably via `kotlin { jvmToolchain(21) }`** in `app/build.gradle.kts` (not a hardcoded path in `gradle.properties` — that broke the self-hosted CI runner, which is Debian, not Arch; see below). The Gradle wrapper itself still needs *some* JDK 17+ on `JAVA_HOME`/`PATH` to bootstrap — any modern JDK works for that, the toolchain handles getting 21 for the actual build regardless of which JVM bootstrapped it.
- Android SDK root: `/opt/android-sdk` (`$ANDROID_HOME`/`$ANDROID_SDK_ROOT`). `sdkmanager`/`avdmanager`/`adb` are on `PATH` in login shells (a plain non-login `bash` may not have them — use `bash -lc '...'` if needed).
- No emulator/system-image is installed, and it wouldn't help anyway — **NFC doesn't work in the emulator**. Real testing requires a physical device connected over `adb`.

### AGP 9 build-config gotchas (already handled in `gradle.properties`, documented here so they aren't "fixed" again by accident)

- AGP 9.0+ ships **built-in Kotlin support** and a **new DSL** (`ApplicationExtension` instead of `BaseAppModuleExtension`), both enabled by default — and both are incompatible with the classic `org.jetbrains.kotlin.android` plugin, which this project still uses (its interop with the Compose compiler and kotlinx-serialization Gradle plugins isn't well documented yet for the new mode). `gradle.properties` sets `android.builtInKotlin=false` and `android.newDsl=false` to opt back into the classic, well-documented plugin/DSL wiring. Revisit this once built-in-Kotlin + new-DSL support for Compose/Koin/serialization is mature — note Google's docs say the opt-out is removed entirely in AGP 10.0.
- `compileSdk`/`targetSdk` are set as plain integers (`37`), **not** the fractional `37.1` platform revision that happened to be pre-installed locally — AGP 9.2 (stable) has no documented support for fractional API levels; that DSL only starts appearing in AGP 9.3 (still preview/RC as of writing). Gradle auto-downloads whatever plain-integer platform/build-tools it needs.

## CI / Releases

`.github/workflows/release.yml` runs on a **self-hosted runner** (`vbServer`, Debian) on push to `main`/`dev` (`dev` publishes as a pre-release) plus manual `workflow_dispatch`. Three jobs: `test` (unit tests + lint) → `build` (debug + unsigned-release APKs) → `publish` (tags `v<versionName>` from `app/build.gradle.kts`). No release signing config exists yet — the release APK is unsigned.

Branching model this is built for: feature branches → merge to `dev` (bump `versionName` before the merge; publishes a pre-release) → periodically merge `dev` to `main` once enough has accumulated (publishes the real release). Tags aren't branch-scoped, so `publish` handles the merge-to-`main` case specially: if the tag from that same `versionName` **doesn't exist yet**, it creates a fresh release; if it **already exists** (because `dev` already pre-released this exact version), it promotes that existing release from pre-release to latest instead of silently no-oping — `main` never needs its own version bump just to "re-release" what `dev` already built.

Runner prerequisites (not provisioned by the workflow — see the "system packages are manual" rule above, it applies to the CI machine too): JDK 21 discoverable via `JAVA_HOME`/`PATH`/standard install locations (satisfies both the Gradle wrapper bootstrap and the `jvmToolchain(21)` requirement), Android SDK at `$ANDROID_HOME`, and an authenticated `gh` CLI. `workflow_dispatch` only works for workflows already present on the repo's **default branch** (`main`) — it can't be used to test-run a workflow that only exists on a feature branch; use a temporary branch added to the `push:` trigger instead (removed once proven).

## Locked decisions (do not re-litigate without asking)

- **App ID**: `hu.vb2007.nfctool` — all source lives under `hu/vb2007/nfctool/`.
- **v1 scope**: read tags + write **plain-text NDEF only**. Generic NDEF/NFC-Forum tags plus the full MIFARE Ultralight family (UL, UL-C, NTAG213/215/216). **Stateless** — no persistence/history/DB in v1.
- **Stack**: Kotlin (2.4.0), Jetpack Compose + Material 3, **Koin** for DI (ViewModels only — plain Koin, not `koin-compose-navigation3`), **MVVM** (ViewModel + StateFlow, unidirectional data flow), Coroutines/Flow, **Navigation 3** (`androidx.navigation3` core API directly — `NavDisplay`/`entryProvider`/`rememberNavBackStack`, state-based back stack — chosen over the older Navigation-Compose 2.x for being the current Google-recommended Compose-first nav library). Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`). Single `:app` module.
- **minSdk 26**; compileSdk/targetSdk **37** (AGP 9.2.0 stable, Gradle 9.6.1). When bumping any toolchain version later, re-verify via the actual Maven/Google-Maven `maven-metadata.xml` rather than search-engine summaries — they were noticeably stale/wrong during v1 setup (e.g. undercounting Koin's and Navigation 3's true latest stable versions).
- **Theming**: Material You dynamic color on API 31+, custom light/dark palette fallback below; auto light/dark via `isSystemInDarkTheme()`.
- **Navigation**: bottom nav with Home + Settings only in v1. Home hosts the Read & Write actions; Settings is an empty placeholder.
- **Guiding principle**: works out of the box for general users, deep configurability for power users — but the power-user config surface (Settings) is deferred beyond v1. Modernity first, backwards-compat second.

## Architecture

The core is the NFC layer under `nfc/` (models in `nfc/model/`, read-side in `nfc/read/`, write-side in `nfc/write/`):

- Use **`NfcAdapter` reader mode** (`enableReaderMode`/`disableReaderMode`), **not** legacy foreground-dispatch/intent-filters. `MainActivity` (single Activity) owns the reader-mode lifecycle in `onResume`/`onPause`.
- **`NfcController`** (Koin singleton) is the hub: it holds adapter availability state (`NfcAdapterState`), a private **scan intent** (`Idle` / `Reading` / `Writing(text)`) set by the active screen via `startReading()`/`startWriting()`/`goIdle()`, and exposes a `SharedFlow<NfcEvent>` that ViewModels collect. All tag IO runs on `Dispatchers.IO` inside `TagReader`/`NdefTextWriter`, wrapped in try/finally with `close()`, catching `TagLostException`/`IOException` into user-friendly error states.
- Tag type detection (`TagTypeDetector`) distinguishes MIFARE Ultralight / Ultralight C / NTAG213 / NTAG215 / NTAG216 via the GET_VERSION (`0x60`) command, falling back to `MifareUltralight.type` for the legacy (pre-GET_VERSION) family.
- Keep the interesting NDEF/tag-type/hex-dump logic as **pure functions living in companion objects** (`NdefParser.decodeTextPayload`/`decodeUriPayload`, `TagTypeDetector.classifyGetVersionResponse`/`classifyLegacyType`/`totalPages`, `UltralightDumpReader.formatPage`, `NdefTextWriter.buildTextMessage`) separate from the thin Android-IO wrapper methods — these pure functions are the actual unit test target, not the IO wrappers (which touch a real `Tag`/`MifareUltralight`/`Ndef` and can't be meaningfully unit tested).
- UI: `ui/navigation/AppRoot.kt` hosts the `Scaffold` + bottom bar + `NavDisplay`; each screen (`ui/home`, `ui/settings`, `ui/read`, `ui/write`) has its own package, with `ReadViewModel`/`WriteViewModel` injected via `koinViewModel()` and driving `NfcController`.

## Commands

```bash
./gradlew assembleDebug      # build
./gradlew testDebugUnitTest  # JVM unit tests
./gradlew lintDebug          # Android lint
./gradlew installDebug       # install on a connected device (requires adb + a device)
```

Run a single test class:
```bash
./gradlew testDebugUnitTest --tests "hu.vb2007.nfctool.nfc.read.NdefParserTest"
```

## Testing notes

- **Emulators cannot do NFC.** Read/write flows must be verified on a physical device with an NTAG21x / Ultralight tag. Automated coverage is limited to the pure functions described above.
- Constructing real `android.nfc.NdefMessage`/`NdefRecord` throws (`RuntimeException: not mocked`) under Android's plain unit-test stub jar — confirmed empirically, not just theoretical. Any test touching them (e.g. `NdefTextWriterTest`) needs `@RunWith(RobolectricTestRunner::class)` (the `robolectric` test dependency is already wired up). Tests that only touch plain `ByteArray`/`Int`/model classes (`NdefParserTest`, `TagTypeDetectorTest`, `UltralightDumpReaderTest`) don't need it.

## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
