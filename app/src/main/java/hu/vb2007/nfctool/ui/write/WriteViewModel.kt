package hu.vb2007.nfctool.ui.write

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.vb2007.nfctool.nfc.NfcAdapterState
import hu.vb2007.nfctool.nfc.NfcController
import hu.vb2007.nfctool.nfc.NfcEvent
import hu.vb2007.nfctool.nfc.model.WriteResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WriteUiState {
    data class Editing(val text: String) : WriteUiState
    data class WaitingForTag(val text: String) : WriteUiState
    data object Success : WriteUiState
    data class Failure(val reason: String, val text: String) : WriteUiState
}

class WriteViewModel(
    private val nfcController: NfcController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<WriteUiState>(WriteUiState.Editing(""))
    val uiState: StateFlow<WriteUiState> = _uiState.asStateFlow()

    val adapterState: StateFlow<NfcAdapterState> = nfcController.adapterState

    init {
        viewModelScope.launch {
            nfcController.events.collect { event ->
                if (event is NfcEvent.WriteCompleted) {
                    val text = (uiState.value as? WriteUiState.WaitingForTag)?.text.orEmpty()
                    _uiState.value = when (val result = event.result) {
                        WriteResult.Success -> WriteUiState.Success
                        WriteResult.ReadOnly -> WriteUiState.Failure("This tag is read-only", text)
                        WriteResult.TooLarge -> WriteUiState.Failure("The text is too large for this tag", text)
                        WriteResult.TagLost -> WriteUiState.Failure("Tag was moved away before writing finished", text)
                        is WriteResult.Error -> WriteUiState.Failure(result.reason, text)
                    }
                }
            }
        }
    }

    fun onTextChange(text: String) {
        _uiState.value = WriteUiState.Editing(text)
    }

    fun startWaitingForTag() {
        val text = when (val state = uiState.value) {
            is WriteUiState.Editing -> state.text
            is WriteUiState.Failure -> state.text
            else -> return
        }
        if (text.isBlank()) return
        _uiState.value = WriteUiState.WaitingForTag(text)
        nfcController.startWriting(text)
    }

    fun cancelWaiting() {
        nfcController.goIdle()
        val text = (uiState.value as? WriteUiState.WaitingForTag)?.text.orEmpty()
        _uiState.value = WriteUiState.Editing(text)
    }

    fun reset() {
        _uiState.value = WriteUiState.Editing("")
    }

    override fun onCleared() {
        nfcController.goIdle()
    }
}
