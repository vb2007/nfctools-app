package hu.vb2007.nfctool.nfc.write

import android.nfc.NdefRecord
import hu.vb2007.nfctool.nfc.read.NdefParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NdefTextWriterTest {

    @Test
    fun `builds a single well-known text record`() {
        val message = NdefTextWriter.buildTextMessage("Hello", languageCode = "en")
        assertEquals(1, message.records.size)

        val record = message.records[0]
        assertEquals(NdefRecord.TNF_WELL_KNOWN, record.tnf)
        assertTrue(record.type.contentEquals(NdefRecord.RTD_TEXT))
        assertEquals("Hello", NdefParser.decodeTextPayload(record.payload))
    }

    @Test
    fun `round-trips through the parser`() {
        val message = NdefTextWriter.buildTextMessage("Round trip test")
        val parsed = NdefParser().parse(message)
        assertEquals(1, parsed.size)
        assertEquals("Round trip test", parsed[0].value)
    }
}
