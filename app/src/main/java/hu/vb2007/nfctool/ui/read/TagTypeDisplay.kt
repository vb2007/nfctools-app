package hu.vb2007.nfctool.ui.read

import hu.vb2007.nfctool.nfc.model.TagType

fun TagType.displayName(): String = when (this) {
    TagType.MIFARE_ULTRALIGHT -> "MIFARE Ultralight"
    TagType.MIFARE_ULTRALIGHT_C -> "MIFARE Ultralight C"
    TagType.NTAG213 -> "NTAG213"
    TagType.NTAG215 -> "NTAG215"
    TagType.NTAG216 -> "NTAG216"
    TagType.OTHER_NDEF -> "NDEF tag"
    TagType.UNKNOWN -> "Unknown tag"
}
