package hu.vb2007.nfctool.nfc.read

import kotlin.test.Test
import kotlin.test.assertEquals

class UltralightDumpReaderTest {

    @Test
    fun `formats a page as a two-digit index followed by hex bytes`() {
        val bytes = byteArrayOf(0x04.toByte(), 0xA1.toByte(), 0xB2.toByte(), 0xC3.toByte())
        assertEquals("04  04 A1 B2 C3", UltralightDumpReader.formatPage(4, bytes))
    }

    @Test
    fun `pads single-digit page indices`() {
        val bytes = byteArrayOf(0x00, 0x00, 0x00, 0x00)
        assertEquals("00  00 00 00 00", UltralightDumpReader.formatPage(0, bytes))
    }
}
