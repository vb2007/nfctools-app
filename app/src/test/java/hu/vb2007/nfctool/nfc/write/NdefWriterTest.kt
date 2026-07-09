package hu.vb2007.nfctool.nfc.write

import android.nfc.NdefRecord
import hu.vb2007.nfctool.nfc.read.NdefParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

class NdefWriterEncodeUriTest {

    // encodeUri is pure ByteArray in/out - no Android types involved, so no Robolectric needed.

    @Test
    fun `picks the longest matching prefix`() {
        val payload = NdefWriter.encodeUri("https://www.example.com")
        assertEquals(2, payload[0].toInt()) // "https://www." is a longer match than "https://"
        assertEquals("example.com", String(payload, 1, payload.size - 1, Charsets.UTF_8))
    }

    @Test
    fun `prefers the plain urn prefix over more specific epc variants that don't match`() {
        val payload = NdefWriter.encodeUri("urn:isbn:0451450523")
        assertEquals(19, payload[0].toInt()) // "urn:" - the epc: variants don't match this string
        assertEquals("isbn:0451450523", String(payload, 1, payload.size - 1, Charsets.UTF_8))
    }

    @Test
    fun `falls back to index 0 when no prefix matches`() {
        val payload = NdefWriter.encodeUri("customscheme:foo")
        assertEquals(0, payload[0].toInt())
        assertEquals("customscheme:foo", String(payload, 1, payload.size - 1, Charsets.UTF_8))
    }

    @Test
    fun `is the exact inverse of decodeUriPayload`() {
        val uris = listOf("https://www.example.com", "http://example.org/path", "tel:+123456", "mailto:a@b.com")
        for (uri in uris) {
            val payload = NdefWriter.encodeUri(uri)
            assertEquals(uri, NdefParser.decodeUriPayload(payload))
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class NdefWriterTest {

    @Test
    fun `builds a single well-known text record`() {
        val message = NdefWriter.buildTextMessage("Hello", languageCode = "en")
        assertEquals(1, message.records.size)

        val record = message.records[0]
        assertEquals(NdefRecord.TNF_WELL_KNOWN, record.tnf)
        assertTrue(record.type.contentEquals(NdefRecord.RTD_TEXT))
        assertEquals("Hello", NdefParser.decodeTextPayload(record.payload))
    }

    @Test
    fun `text round-trips through the parser`() {
        val message = NdefWriter.buildTextMessage("Round trip test")
        val parsed = NdefParser().parse(message)
        assertEquals(1, parsed.size)
        assertEquals("Round trip test", parsed[0].value)
    }

    @Test
    fun `builds a well-known URI record`() {
        val message = NdefWriter.buildUriMessage("https://example.com/page")
        val record = message.records[0]
        assertEquals(NdefRecord.TNF_WELL_KNOWN, record.tnf)
        assertTrue(record.type.contentEquals(NdefRecord.RTD_URI))
        assertEquals("https://example.com/page", NdefParser.decodeUriPayload(record.payload))
    }

    @Test
    fun `tel message round-trips as a tel URI`() {
        val message = NdefWriter.buildTelMessage("+15551234567")
        val parsed = NdefParser().parse(message)
        assertEquals("tel:+15551234567", parsed[0].value)
    }

    @Test
    fun `email message round-trips as a mailto URI`() {
        val message = NdefWriter.buildEmailMessage("someone@example.com")
        val parsed = NdefParser().parse(message)
        assertEquals("mailto:someone@example.com", parsed[0].value)
    }

    @Test
    fun `sms message includes the body as a query param`() {
        val message = NdefWriter.buildSmsMessage("+15551234567", "hello there")
        val parsed = NdefParser().parse(message)
        assertEquals("sms:+15551234567?body=hello there", parsed[0].value)
    }

    @Test
    fun `sms message omits the query param when body is blank`() {
        val message = NdefWriter.buildSmsMessage("+15551234567", "")
        val parsed = NdefParser().parse(message)
        assertEquals("sms:+15551234567", parsed[0].value)
    }

    @Test
    fun `vcard text includes only non-blank fields`() {
        val full = NdefWriter.buildVCardText("Jane Doe", "+15551234567", "jane@example.com")
        assertEquals(
            "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Jane Doe\r\nTEL:+15551234567\r\nEMAIL:jane@example.com\r\nEND:VCARD\r\n",
            full,
        )

        val nameOnly = NdefWriter.buildVCardText("Jane Doe", "", "")
        assertEquals("BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Jane Doe\r\nEND:VCARD\r\n", nameOnly)
    }

    @Test
    fun `vcard message round-trips as a CONTACT record`() {
        val message = NdefWriter.buildVCardMessage("Jane Doe", "+15551234567", "jane@example.com")
        val record = message.records[0]
        assertEquals(NdefRecord.TNF_MIME_MEDIA, record.tnf)
        assertTrue(record.type.contentEquals(NdefParser.MIME_VCARD))

        val parsed = NdefParser().parse(message)
        assertEquals(1, parsed.size)
        assertTrue(parsed[0].value.contains("FN:Jane Doe"))
    }
}
