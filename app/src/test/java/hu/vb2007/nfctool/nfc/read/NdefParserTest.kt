package hu.vb2007.nfctool.nfc.read

import kotlin.test.Test
import kotlin.test.assertEquals

class NdefParserTest {

    @Test
    fun `decodes UTF-8 text payload with language code`() {
        val languageCode = "en"
        val text = "Hello NFC"
        val payload = ByteArray(1 + languageCode.length + text.toByteArray(Charsets.UTF_8).size)
        payload[0] = languageCode.length.toByte()
        languageCode.toByteArray(Charsets.US_ASCII).copyInto(payload, 1)
        text.toByteArray(Charsets.UTF_8).copyInto(payload, 1 + languageCode.length)

        assertEquals(text, NdefParser.decodeTextPayload(payload))
    }

    @Test
    fun `decodes empty text payload as empty string`() {
        assertEquals("", NdefParser.decodeTextPayload(ByteArray(0)))
    }

    @Test
    fun `decodes URI payload with well-known https prefix`() {
        val payload = byteArrayOf(0x04) + "example.com".toByteArray(Charsets.UTF_8)
        assertEquals("https://example.com", NdefParser.decodeUriPayload(payload))
    }

    @Test
    fun `decodes URI payload with well-known https-www prefix`() {
        val payload = byteArrayOf(0x02) + "example.com".toByteArray(Charsets.UTF_8)
        assertEquals("https://www.example.com", NdefParser.decodeUriPayload(payload))
    }

    @Test
    fun `decodes URI payload with no prefix`() {
        val payload = byteArrayOf(0x00) + "custom:scheme".toByteArray(Charsets.UTF_8)
        assertEquals("custom:scheme", NdefParser.decodeUriPayload(payload))
    }

    @Test
    fun `decodes empty URI payload as empty string`() {
        assertEquals("", NdefParser.decodeUriPayload(ByteArray(0)))
    }
}
