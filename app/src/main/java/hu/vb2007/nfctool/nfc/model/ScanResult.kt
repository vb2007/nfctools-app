package hu.vb2007.nfctool.nfc.model

sealed interface ScanResult {
    data class Success(val tagInfo: TagInfo) : ScanResult
    data class Error(val reason: String) : ScanResult
}
