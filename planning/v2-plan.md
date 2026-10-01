# Advanced NFC Tools — v2 Implementation Plan

> **Note:** written retroactively (2026-09-28) after the v2 work was already implemented and
> merged to `dev` (branch `dev-v2-ndef-records`, PR #7). v1 shipped without a matching
> planning doc being written for this branch; this file reconstructs it from the actual
> diff/commit history so the decisions and their rationale are recorded, per the project rule
> that every change gets audited. Treat it as a description of what was built and why, not a
> forward-looking spec — read it alongside [`v1-plan.md`](v1-plan.md), which still governs the
> parts of the app v2 didn't touch.

## Context

v1 shipped read + plain-text-only NDEF write. The single largest item in v1's "explicitly
deferred" list was **writing non-text records** (URL/Wi-Fi/vCard, listed there as "later").
v2's scope is exactly that slice: generalize the write path from a bare string to a small set
of well-known NDEF record types, without touching the read architecture, tag-type detection,
or the overall MVVM/Koin/NfcController shape — all of that stayed exactly as v1 built it.

### Decisions made during v2
| Topic | Decision |
|---|---|
| New write record types | **URL, phone (tel:), email (mailto:), SMS (sms:), contact (vCard 3.0)** — in addition to v1's plain text |
| Wi-Fi record type | **Not done** — despite being named alongside URL/vCard in v1's deferred list, Wi-Fi network config records were not implemented in v2. Still open. |
| Raw page write / PWD-PACK auth | **Not done** — still deferred, unchanged from v1 |
| Read-only / lock bits (`makeReadOnly`) | **Implemented, then removed in the same branch.** See "Make-read-only: implemented and reverted" below. |
| Read-side symmetry | vCard (`text/vcard` MIME) records now decode into a new `CONTACT` `NdefRecordKind` on read, so a contact written by the app round-trips back to a labeled result instead of falling into the generic `OTHER` bucket. |
| URI encoding | Reuses the existing `NdefParser.URI_PREFIXES` table via `encodeUri()` (longest-prefix match) — the exact inverse of `NdefParser.decodeUriPayload`, rather than introducing a separate prefix table for the write side. |

---

## Architecture changes

### `NdefPayload` (new) — `nfc/model/NdefPayload.kt`
A sealed interface replacing the bare `String` the write path used to take, mirroring
`NdefRecordKind` on the read side:
```kotlin
sealed interface NdefPayload {
    data class Text(val text: String, val languageCode: String) : NdefPayload
    data class Uri(val uri: String) : NdefPayload
    data class Tel(val number: String) : NdefPayload
    data class Email(val address: String) : NdefPayload
    data class Sms(val number: String, val body: String) : NdefPayload
    data class Contact(val name: String, val phone: String, val email: String) : NdefPayload
}
```

### `NdefTextWriter` → `NdefWriter` — `nfc/write/NdefWriter.kt`
Renamed and generalized. `write(tag, text)` became `write(tag, payload: NdefPayload)`, dispatching
to one of the new pure, unit-testable builders in the companion object:
- `buildTextMessage` (kept from v1)
- `encodeUri` / `buildUriMessage` — longest-prefix match against `NdefParser.URI_PREFIXES`
- `buildTelMessage` / `buildEmailMessage` / `buildSmsMessage` — all thin wrappers over
  `buildUriMessage` (`tel:`, `mailto:`, `sms:...?body=...`)
- `buildVCardMessage` / `buildVCardText` — builds a minimal vCard 3.0 (`BEGIN:VCARD` /
  `FN:`/`TEL:`/`EMAIL:` / `END:VCARD`) as a `TNF_MIME_MEDIA` record using
  `NdefParser.MIME_VCARD`

The IO wrapper methods (`writeToNdef`, `writeToFormatable`) are otherwise unchanged from v1 —
same read-only/too-large/tag-lost handling, same `Ndef`/`NdefFormatable` fallback.

### Read-side: `NdefRecordKind.CONTACT` — `nfc/model/NdefRecordModel.kt`, `nfc/read/NdefParser.kt`
`NdefRecordKind` gained a `CONTACT` case; `NdefParser` decodes `text/vcard` MIME records into it,
so writing and reading a contact via this app is symmetric (no generic-fallback downgrade).

### `WriteViewModel` / `WriteUiState` — `ui/write/WriteViewModel.kt`
- New `WriteRecordType` enum (`TEXT`, `URL`, `TEL`, `EMAIL`, `SMS`, `CONTACT`).
- `WriteUiState.Editing` now holds one field set per record type (`text`+`languageCode`, `uri`,
  `tel`, `email`, `smsNumber`+`smsBody`, `contactName`+`contactPhone`+`contactEmail`) plus a
  `recordType` selector.
- `Editing.isValid()` / `Editing.toPayload()` (private) do per-type validation and payload
  construction. Contact validation requires *any one* of name/phone/email to be non-blank
  (not all three).
- Editing state is preserved across a failed write and across the `WaitingForTag` round-trip,
  matching v1's retry behavior for plain text but now for every record type.

### `WriteScreen` — `ui/write/WriteScreen.kt`
- Added a horizontally-scrollable `FilterChip` row (`RecordTypeSelector`) to pick the record
  type; the field set below it swaps per type.
- The Text record type now exposes an editable language-code field (previously hardcoded to
  `"en"` in v1; `NdefWriter.DEFAULT_LANGUAGE_CODE` is still `"en"` as the default value).
- No confirmation dialog or lock-related UI ships in v2 (see below).

### Wiring — `NfcController.kt`, `AppModule.kt`
`NfcIntent.Writing` carries an `NdefPayload` instead of a bare `String`; `startWriting()` and
the dispatch in `.onTagDiscovered()` updated accordingly. Koin's `AppModule` registers the
renamed `NdefWriter` in place of `NdefTextWriter`.

---

## Make-read-only: implemented and reverted

Mid-branch, v2 also added a "make tag read-only" option: `WriteRequest` wrapping an
`NdefPayload` + `makeReadOnly: Boolean`, `WriteResult.LockFailed` for a successful write whose
subsequent lock step failed, `Ndef.canMakeReadOnly()`/`makeReadOnly()` and
`NdefFormatable.formatReadOnly()` wiring in the writer, and a checkbox + `AlertDialog`
confirmation in `WriteScreen`.

**It was removed before merging to `dev`.** MIFARE Ultralight/NTAG21x lock bytes are a
hardware-enforced one-way latch — the `WRITE` command ORs new bits onto existing ones, so once
a lock bit is set it cannot be cleared by any NFC command, on any tag in this family including
the newest NTAG21x chips. There is no safe reversible equivalent: NTAG21x password protection
(PWD/PACK) is undoable but only covers the newer chips, protects writes rather than acting as a
real lock, and would need password bookkeeping this stateless app deliberately avoids (already
separately out of scope). Since irreversible tag changes don't fit the app's guiding
principles, the feature was dropped entirely rather than shipped as an unsafe option:
`WriteRequest`/`makeReadOnly`/`LockFailed` were removed from the models, the lock branches from
`writeToNdef`/`writeToFormatable`, and the checkbox/dialog from `WriteScreen`. `NfcController`
and `WriteViewModel` pass `NdefPayload` directly again, with no wrapper type.

**If a read-only/lock feature is revisited later**, treat "irreversible" as the blocking
constraint to solve first, not an afterthought — e.g. requiring an explicit, separately-scoped
"advanced/destructive actions" area with much stronger confirmation than a single dialog, or
building it only for the PWD/PACK-capable write-protection case rather than true lock bits.

---

## Unrelated fix landed on this branch: Nav3 ViewModel scoping

Not part of the NDEF-record scope, but fixed on this branch because it was found while
testing v2's write flow on-device: Navigation 3's default `entryDecorators` only wire up
`SaveableStateHolderNavEntryDecorator`, not `ViewModelStoreNavEntryDecorator` (the
`androidx.lifecycle:lifecycle-viewmodel-navigation3` dependency was already present in
`build.gradle.kts` but never actually used). Without it, `koinViewModel()` fell back to the
single Activity-scoped `ViewModelStoreOwner`, so `ReadViewModel`/`WriteViewModel` were each
created once for the Activity's whole lifetime and never cleared on navigation — breaking
read-after-write, since `WriteViewModel.onCleared()` (which calls `goIdle()`) never fired.
Confirmed fixed on a physical device: write then read round-trips correctly.

---

## Tests

`NdefTextWriterTest` renamed to `NdefWriterTest`; a new Robolectric-free
`NdefWriterEncodeUriTest` covers `encodeUri`'s longest-prefix-match logic (including an
exact-inverse check against `NdefParser.decodeUriPayload`). Robolectric-backed coverage was
added for tel/email/sms/vcard message building and their round-trip through `NdefParser`,
including the new `CONTACT` record kind — following the same pure-function-first testing
approach v1 established (see `CLAUDE.md`'s testing notes).

---

## Out of scope for v2 (carried over from v1, still open)

Wi-Fi network config records (named alongside URL/vCard in v1's list but not built in v2),
raw page write, Ultralight/NTAG21x password auth (PWD/PACK), any form of tag locking/read-only
(attempted and deliberately reverted this round — see above), MIFARE Classic / DESFire /
FeliCa, scan history & export/persistence (app is still fully stateless), a populated Settings
screen, HCE/emulation.

---

## Verification

Same gate as v1: `./gradlew assembleDebug testDebugUnitTest lintDebug` passes clean. The Nav3
ViewModel-scoping fix and the general write-then-read flow for the new record types were
confirmed on a physical device during this branch; a full fresh on-device pass across all six
record types (URL/tel/email/SMS/contact in addition to text) has not been explicitly checklisted
the way v1's on-device test plan was — worth doing before calling v2 fully verified.
