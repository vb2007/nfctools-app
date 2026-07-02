package hu.vb2007.nfctool.nfc.write

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import hu.vb2007.nfctool.nfc.model.WriteResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class NdefTextWriter {

    suspend fun write(tag: Tag, text: String): WriteResult = withContext(Dispatchers.IO) {
        try {
            val message = buildTextMessage(text)
            val ndef = Ndef.get(tag)
            when {
                ndef != null -> writeToNdef(ndef, message)
                else -> {
                    val formatable = NdefFormatable.get(tag)
                    if (formatable != null) writeToFormatable(formatable, message)
                    else WriteResult.Error("This tag doesn't support NDEF")
                }
            }
        } catch (e: TagLostException) {
            WriteResult.TagLost
        } catch (e: FormatException) {
            WriteResult.Error("Malformed NDEF data")
        } catch (e: IOException) {
            WriteResult.Error(e.message ?: "I/O error while writing")
        }
    }

    private fun writeToNdef(ndef: Ndef, message: NdefMessage): WriteResult {
        ndef.connect()
        try {
            if (!ndef.isWritable) return WriteResult.ReadOnly
            if (message.toByteArray().size > ndef.maxSize) return WriteResult.TooLarge
            ndef.writeNdefMessage(message)
            return WriteResult.Success
        } finally {
            ndef.close()
        }
    }

    private fun writeToFormatable(formatable: NdefFormatable, message: NdefMessage): WriteResult {
        formatable.connect()
        try {
            formatable.format(message)
            return WriteResult.Success
        } finally {
            formatable.close()
        }
    }

    companion object {
        const val DEFAULT_LANGUAGE_CODE = "en"

        /** Pure: builds a well-known-text NDEF message. Testable without hardware. */
        fun buildTextMessage(text: String, languageCode: String = DEFAULT_LANGUAGE_CODE): NdefMessage {
            val languageBytes = languageCode.lowercase(Locale.ROOT).toByteArray(Charsets.US_ASCII)
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val payload = ByteArray(1 + languageBytes.size + textBytes.size)
            payload[0] = languageBytes.size.toByte()
            System.arraycopy(languageBytes, 0, payload, 1, languageBytes.size)
            System.arraycopy(textBytes, 0, payload, 1 + languageBytes.size, textBytes.size)
            val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, ByteArray(0), payload)
            return NdefMessage(arrayOf(record))
        }
    }
}
