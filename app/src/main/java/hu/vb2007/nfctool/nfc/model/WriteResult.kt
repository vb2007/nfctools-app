package hu.vb2007.nfctool.nfc.model

sealed interface WriteResult {
    data object Success : WriteResult
    data object ReadOnly : WriteResult
    data object TooLarge : WriteResult
    data object TagLost : WriteResult
    data class Error(val reason: String) : WriteResult
}
