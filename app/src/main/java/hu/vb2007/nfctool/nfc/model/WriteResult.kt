package hu.vb2007.nfctool.nfc.model

sealed interface WriteResult {
    data class Success(val madeReadOnly: Boolean) : WriteResult
    data object ReadOnly : WriteResult
    data object TooLarge : WriteResult
    data object TagLost : WriteResult
    data class Error(val reason: String) : WriteResult
    data class LockFailed(val reason: String) : WriteResult
}
