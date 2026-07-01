package hu.vb2007.nfctool.nfc.read

import android.nfc.NdefMessage
import android.nfc.NdefRecord
import hu.vb2007.nfctool.nfc.model.NdefRecordKind
import hu.vb2007.nfctool.nfc.model.NdefRecordModel

class NdefParser {

    fun parse(message: NdefMessage): List<NdefRecordModel> =
        message.records.map { parseRecord(it) }

    private fun parseRecord(record: NdefRecord): NdefRecordModel = when {
        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) ->
            NdefRecordModel(NdefRecordKind.TEXT, "Text", decodeTextPayload(record.payload))

        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) ->
            NdefRecordModel(NdefRecordKind.URI, "URI", decodeUriPayload(record.payload))

        record.tnf == NdefRecord.TNF_ABSOLUTE_URI ->
            NdefRecordModel(NdefRecordKind.URI, "URI", String(record.payload, Charsets.UTF_8))

        else ->
            NdefRecordModel(NdefRecordKind.OTHER, "Record", "${record.payload.size} bytes")
    }

    companion object {
        // NFC Forum URI Record Type Definition, well-known prefix table (index = payload[0]).
        val URI_PREFIXES = arrayOf(
            "", "http://www.", "https://www.", "http://", "https://",
            "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
            "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
            "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
            "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
            "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
            "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:",
        )

        /** Pure: decodes a well-known-text record payload. Testable without hardware. */
        fun decodeTextPayload(payload: ByteArray): String {
            if (payload.isEmpty()) return ""
            val statusByte = payload[0].toInt()
            val isUtf16 = (statusByte and 0x80) != 0
            val languageCodeLength = statusByte and 0x3F
            val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
            val textStart = 1 + languageCodeLength
            if (textStart > payload.size) return ""
            return String(payload, textStart, payload.size - textStart, charset)
        }

        /** Pure: decodes a well-known-URI record payload. Testable without hardware. */
        fun decodeUriPayload(payload: ByteArray): String {
            if (payload.isEmpty()) return ""
            val prefix = URI_PREFIXES.getOrElse(payload[0].toInt()) { "" }
            return prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
        }
    }
}
