package hu.vb2007.nfctool.ui.read

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import hu.vb2007.nfctool.nfc.NfcAdapterState
import hu.vb2007.nfctool.nfc.NfcController
import hu.vb2007.nfctool.nfc.NfcEvent
import hu.vb2007.nfctool.nfc.model.ScanResult
import hu.vb2007.nfctool.nfc.model.TagInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ReadUiState {
    data object Scanning : ReadUiState
    data class Success(val tagInfo: TagInfo) : ReadUiState
    data class Error(val reason: String) : ReadUiState
}

class ReadViewModel(
    private val nfcController: NfcController,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReadUiState>(ReadUiState.Scanning)
    val uiState: StateFlow<ReadUiState> = _uiState.asStateFlow()

    val adapterState: StateFlow<NfcAdapterState> = nfcController.adapterState

    init {
        nfcController.startReading()
        viewModelScope.launch {
            nfcController.events.collect { event ->
                if (event is NfcEvent.ReadCompleted) {
                    _uiState.value = when (val result = event.result) {
                        is ScanResult.Success -> ReadUiState.Success(result.tagInfo)
                        is ScanResult.Error -> ReadUiState.Error(result.reason)
                    }
                }
            }
        }
    }

    fun scanAgain() {
        _uiState.value = ReadUiState.Scanning
        nfcController.startReading()
    }

    override fun onCleared() {
        nfcController.goIdle()
    }
}
