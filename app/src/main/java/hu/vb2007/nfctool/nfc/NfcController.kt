package hu.vb2007.nfctool.nfc

import android.app.Activity
import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import hu.vb2007.nfctool.nfc.model.ScanResult
import hu.vb2007.nfctool.nfc.model.WriteRequest
import hu.vb2007.nfctool.nfc.model.WriteResult
import hu.vb2007.nfctool.nfc.read.TagReader
import hu.vb2007.nfctool.nfc.write.NdefWriter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class NfcAdapterState { NO_HARDWARE, DISABLED, READY }

private sealed interface NfcIntent {
    data object Idle : NfcIntent
    data object Reading : NfcIntent
    data class Writing(val request: WriteRequest) : NfcIntent
}

sealed interface NfcEvent {
    data class ReadCompleted(val result: ScanResult) : NfcEvent
    data class WriteCompleted(val result: WriteResult) : NfcEvent
}

class NfcController(
    context: Context,
    private val tagReader: TagReader,
    private val ndefWriter: NdefWriter,
) : NfcAdapter.ReaderCallback {

    private val appContext = context.applicationContext
    private val nfcAdapter: NfcAdapter? = NfcAdapter.getDefaultAdapter(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _adapterState = MutableStateFlow(computeAdapterState())
    val adapterState: StateFlow<NfcAdapterState> = _adapterState.asStateFlow()

    private val _events = MutableSharedFlow<NfcEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<NfcEvent> = _events.asSharedFlow()

    @Volatile
    private var intent: NfcIntent = NfcIntent.Idle

    fun startReading() {
        intent = NfcIntent.Reading
    }

    fun startWriting(request: WriteRequest) {
        intent = NfcIntent.Writing(request)
    }

    fun goIdle() {
        intent = NfcIntent.Idle
    }

    fun refreshAdapterState() {
        _adapterState.value = computeAdapterState()
    }

    fun enableReaderMode(activity: Activity) {
        refreshAdapterState()
        val flags = NfcAdapter.FLAG_READER_NFC_A or
            NfcAdapter.FLAG_READER_NFC_B or
            NfcAdapter.FLAG_READER_NFC_F or
            NfcAdapter.FLAG_READER_NFC_V or
            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS
        nfcAdapter?.enableReaderMode(activity, this, flags, Bundle.EMPTY)
    }

    fun disableReaderMode(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
    }

    override fun onTagDiscovered(tag: Tag) {
        when (val current = intent) {
            is NfcIntent.Reading -> scope.launch {
                _events.emit(NfcEvent.ReadCompleted(tagReader.read(tag)))
            }
            is NfcIntent.Writing -> scope.launch {
                _events.emit(NfcEvent.WriteCompleted(ndefWriter.write(tag, current.request)))
            }
            NfcIntent.Idle -> Unit
        }
    }

    private fun computeAdapterState(): NfcAdapterState = when {
        nfcAdapter == null -> NfcAdapterState.NO_HARDWARE
        !nfcAdapter.isEnabled -> NfcAdapterState.DISABLED
        else -> NfcAdapterState.READY
    }
}
