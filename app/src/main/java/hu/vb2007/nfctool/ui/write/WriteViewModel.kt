package hu.vb2007.nfctool.ui.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.vb2007.nfctool.nfc.NfcAdapterState
import hu.vb2007.nfctool.nfc.NfcController
import hu.vb2007.nfctool.nfc.NfcEvent
import hu.vb2007.nfctool.nfc.model.NdefPayload
import hu.vb2007.nfctool.nfc.model.WriteRequest
import hu.vb2007.nfctool.nfc.model.WriteResult
import hu.vb2007.nfctool.nfc.write.NdefWriter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class WriteRecordType { TEXT, URL, TEL, EMAIL, SMS, CONTACT }

sealed interface WriteUiState {
    data class Editing(
        val recordType: WriteRecordType = WriteRecordType.TEXT,
        val text: String = "",
        val languageCode: String = NdefWriter.DEFAULT_LANGUAGE_CODE,
        val uri: String = "",
        val tel: String = "",
        val email: String = "",
        val smsNumber: String = "",
        val smsBody: String = "",
        val contactName: String = "",
        val contactPhone: String = "",
        val contactEmail: String = "",
        val makeReadOnly: Boolean = false,
    ) : WriteUiState

    data class WaitingForTag(val editing: Editing) : WriteUiState
    data class Success(val madeReadOnly: Boolean, val warning: String? = null) : WriteUiState
    data class Failure(val reason: String, val editing: Editing) : WriteUiState
}

/** True when the current record type has enough input to attempt a write. */
fun WriteUiState.Editing.isValid(): Boolean = when (recordType) {
    WriteRecordType.TEXT -> text.isNotBlank()
    WriteRecordType.URL -> uri.isNotBlank()
    WriteRecordType.TEL -> tel.isNotBlank()
    WriteRecordType.EMAIL -> email.isNotBlank()
    WriteRecordType.SMS -> smsNumber.isNotBlank()
    WriteRecordType.CONTACT -> contactName.isNotBlank() || contactPhone.isNotBlank() || contactEmail.isNotBlank()
}

private fun WriteUiState.Editing.toPayload(): NdefPayload = when (recordType) {
    WriteRecordType.TEXT -> NdefPayload.Text(text, languageCode)
    WriteRecordType.URL -> NdefPayload.Uri(uri)
    WriteRecordType.TEL -> NdefPayload.Tel(tel)
    WriteRecordType.EMAIL -> NdefPayload.Email(email)
    WriteRecordType.SMS -> NdefPayload.Sms(smsNumber, smsBody)
    WriteRecordType.CONTACT -> NdefPayload.Contact(contactName, contactPhone, contactEmail)
}

class WriteViewModel(
    private val nfcController: NfcController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WriteUiState>(WriteUiState.Editing())
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    val adapterState: StateFlow<NfcAdapterState> = nfcController.adapterState

    init {
        viewModelScope.launch {
            nfcController.events.collect { event ->
                if (event is NfcEvent.WriteCompleted) {
                    val editing = (uiState.value as? WriteUiState.WaitingForTag)?.editing ?: WriteUiState.Editing()
                    _uiState.value = when (val result = event.result) {
                        is WriteResult.Success -> WriteUiState.Success(result.madeReadOnly)
                        WriteResult.ReadOnly -> WriteUiState.Failure("This tag is read-only", editing)
                        WriteResult.TooLarge -> WriteUiState.Failure("The data is too large for this tag", editing)
                        WriteResult.TagLost -> WriteUiState.Failure("Tag was moved away before writing finished", editing)
                        is WriteResult.Error -> WriteUiState.Failure(result.reason, editing)
                        is WriteResult.LockFailed -> WriteUiState.Success(madeReadOnly = false, warning = result.reason)
                    }
                }
            }
        }
    }

    /** Applies [transform] to the current editing fields, whether idle or retrying after a failure. */
    fun updateEditing(transform: (WriteUiState.Editing) -> WriteUiState.Editing) {
        val current = when (val state = uiState.value) {
            is WriteUiState.Editing -> state
            is WriteUiState.Failure -> state.editing
            else -> return
        }
        _uiState.value = transform(current)
    }

    fun startWaitingForTag() {
        val editing = when (val state = uiState.value) {
            is WriteUiState.Editing -> state
            is WriteUiState.Failure -> state.editing
            else -> return
        }
        if (!editing.isValid()) return
        _uiState.value = WriteUiState.WaitingForTag(editing)
        nfcController.startWriting(WriteRequest(editing.toPayload(), editing.makeReadOnly))
    }

    fun cancelWaiting() {
        nfcController.goIdle()
        val editing = (uiState.value as? WriteUiState.WaitingForTag)?.editing ?: WriteUiState.Editing()
        _uiState.value = editing
    }

    fun reset() {
        _uiState.value = WriteUiState.Editing()
    }

    override fun onCleared() {
        nfcController.goIdle()
    }
}
