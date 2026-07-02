package hu.vb2007.nfctool.nfc.model

data class TagInfo(
    val uid: String,
    val techList: List<String>,
    val tagType: TagType,
    val capacityBytes: Int?,
    val usedBytes: Int?,
    val isWritable: Boolean,
    val canMakeReadOnly: Boolean,
    val ndefRecords: List<NdefRecordModel>,
    val hexDump: List<String>?,
)
