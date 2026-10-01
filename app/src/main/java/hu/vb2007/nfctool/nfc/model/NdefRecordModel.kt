package hu.vb2007.nfctool.nfc.model

enum class NdefRecordKind {
    TEXT,
    URI,
    CONTACT,
    OTHER,
}

data class NdefRecordModel(
    val kind: NdefRecordKind,
    val label: String,
    val value: String,
)
