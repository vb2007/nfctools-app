package hu.vb2007.nfctool.nfc.write

import android.nfc.FormatException
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import hu.vb2007.nfctool.nfc.model.NdefPayload
import hu.vb2007.nfctool.nfc.model.WriteRequest
import hu.vb2007.nfctool.nfc.model.WriteResult
import hu.vb2007.nfctool.nfc.read.NdefParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

class NdefWriter {

    suspend fun write(tag: Tag, request: WriteRequest): WriteResult = withContext(Dispatchers.IO) {
        try {
            val message = buildMessage(request.payload)
            val ndef = Ndef.get(tag)
            when {
                ndef != null -> writeToNdef(ndef, message, request.makeReadOnly)
                else -> {
                    val formatable = NdefFormatable.get(tag)
                    if (formatable != null) writeToFormatable(formatable, message, request.makeReadOnly)
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

    private fun buildMessage(payload: NdefPayload): NdefMessage = when (payload) {
        is NdefPayload.Text -> buildTextMessage(payload.text, payload.languageCode)
        is NdefPayload.Uri -> buildUriMessage(payload.uri)
        is NdefPayload.Tel -> buildTelMessage(payload.number)
        is NdefPayload.Email -> buildEmailMessage(payload.address)
        is NdefPayload.Sms -> buildSmsMessage(payload.number, payload.body)
        is NdefPayload.Contact -> buildVCardMessage(payload.name, payload.phone, payload.email)
    }

    private fun writeToNdef(ndef: Ndef, message: NdefMessage, makeReadOnly: Boolean): WriteResult {
        ndef.connect()
        try {
            if (!ndef.isWritable) return WriteResult.ReadOnly
            if (message.toByteArray().size > ndef.maxSize) return WriteResult.TooLarge
            ndef.writeNdefMessage(message)
            if (!makeReadOnly) return WriteResult.Success(madeReadOnly = false)
            return if (tryMakeReadOnly(ndef)) {
                WriteResult.Success(madeReadOnly = true)
            } else {
                WriteResult.LockFailed("Tag was written, but locking it read-only failed or isn't supported")
            }
        } finally {
            ndef.close()
        }
    }

    private fun tryMakeReadOnly(ndef: Ndef): Boolean = try {
        ndef.canMakeReadOnly() && ndef.makeReadOnly()
    } catch (e: IOException) {
        false
    }

    private fun writeToFormatable(formatable: NdefFormatable, message: NdefMessage, makeReadOnly: Boolean): WriteResult {
        formatable.connect()
        try {
            if (makeReadOnly) {
                formatable.formatReadOnly(message)
                return WriteResult.Success(madeReadOnly = true)
            }
            formatable.format(message)
            return WriteResult.Success(madeReadOnly = false)
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

        /**
         * Pure: encodes a URI into a well-known-URI record payload, using the longest matching
         * prefix from [NdefParser.URI_PREFIXES]. Testable without hardware. Exact inverse of
         * [NdefParser.decodeUriPayload].
         */
        fun encodeUri(uri: String): ByteArray {
            var bestIndex = 0
            var bestLength = 0
            for ((index, prefix) in NdefParser.URI_PREFIXES.withIndex()) {
                if (prefix.isNotEmpty() && uri.startsWith(prefix) && prefix.length > bestLength) {
                    bestIndex = index
                    bestLength = prefix.length
                }
            }
            val remainderBytes = uri.substring(bestLength).toByteArray(Charsets.UTF_8)
            val payload = ByteArray(1 + remainderBytes.size)
            payload[0] = bestIndex.toByte()
            System.arraycopy(remainderBytes, 0, payload, 1, remainderBytes.size)
            return payload
        }

        /** Pure: builds a well-known-URI NDEF message. Testable without hardware. */
        fun buildUriMessage(uri: String): NdefMessage {
            val record = NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, ByteArray(0), encodeUri(uri))
            return NdefMessage(arrayOf(record))
        }

        /** Pure: builds a tel: URI NDEF message. Testable without hardware. */
        fun buildTelMessage(number: String): NdefMessage = buildUriMessage("tel:$number")

        /** Pure: builds a mailto: URI NDEF message. Testable without hardware. */
        fun buildEmailMessage(address: String): NdefMessage = buildUriMessage("mailto:$address")

        /** Pure: builds an sms: URI NDEF message. Testable without hardware. */
        fun buildSmsMessage(number: String, body: String): NdefMessage {
            val uri = if (body.isBlank()) "sms:$number" else "sms:$number?body=$body"
            return buildUriMessage(uri)
        }

        /** Pure: builds a minimal vCard 3.0 MIME NDEF message. Testable without hardware. */
        fun buildVCardMessage(name: String, phone: String, email: String): NdefMessage {
            val vcard = buildVCardText(name, phone, email)
            val record = NdefRecord(NdefRecord.TNF_MIME_MEDIA, NdefParser.MIME_VCARD, ByteArray(0), vcard.toByteArray(Charsets.UTF_8))
            return NdefMessage(arrayOf(record))
        }

        /** Pure: builds the raw vCard 3.0 text. Testable without hardware. */
        fun buildVCardText(name: String, phone: String, email: String): String = buildString {
            append("BEGIN:VCARD\r\n")
            append("VERSION:3.0\r\n")
            if (name.isNotBlank()) append("FN:$name\r\n")
            if (phone.isNotBlank()) append("TEL:$phone\r\n")
            if (email.isNotBlank()) append("EMAIL:$email\r\n")
            append("END:VCARD\r\n")
        }
    }
}
