package hu.vb2007.nfctool.nfc.read

import android.nfc.tech.MifareUltralight
import hu.vb2007.nfctool.nfc.model.TagType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagTypeDetectorTest {

    @Test
    fun `classifies NTAG213 from GET_VERSION response`() {
        val response = byteArrayOf(0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x0F, 0x03)
        assertEquals(TagType.NTAG213, TagTypeDetector.classifyGetVersionResponse(response))
    }

    @Test
    fun `classifies NTAG215 from GET_VERSION response`() {
        val response = byteArrayOf(0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x11, 0x03)
        assertEquals(TagType.NTAG215, TagTypeDetector.classifyGetVersionResponse(response))
    }

    @Test
    fun `classifies NTAG216 from GET_VERSION response`() {
        val response = byteArrayOf(0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x13, 0x03)
        assertEquals(TagType.NTAG216, TagTypeDetector.classifyGetVersionResponse(response))
    }

    @Test
    fun `returns null for a null response`() {
        assertNull(TagTypeDetector.classifyGetVersionResponse(null))
    }

    @Test
    fun `returns null for a too-short response`() {
        assertNull(TagTypeDetector.classifyGetVersionResponse(byteArrayOf(0x00, 0x04)))
    }

    @Test
    fun `returns null for a non-NXP-NTAG product type`() {
        val response = byteArrayOf(0x00, 0x04, 0x01, 0x02, 0x01, 0x00, 0x0F, 0x03)
        assertNull(TagTypeDetector.classifyGetVersionResponse(response))
    }

    @Test
    fun `returns null for an unrecognized storage size`() {
        val response = byteArrayOf(0x00, 0x04, 0x04, 0x02, 0x01, 0x00, 0x99.toByte(), 0x03)
        assertNull(TagTypeDetector.classifyGetVersionResponse(response))
    }

    @Test
    fun `classifies legacy Ultralight C`() {
        assertEquals(
            TagType.MIFARE_ULTRALIGHT_C,
            TagTypeDetector.classifyLegacyType(MifareUltralight.TYPE_ULTRALIGHT_C),
        )
    }

    @Test
    fun `classifies legacy Ultralight`() {
        assertEquals(
            TagType.MIFARE_ULTRALIGHT,
            TagTypeDetector.classifyLegacyType(MifareUltralight.TYPE_ULTRALIGHT),
        )
    }

    @Test
    fun `total pages match known tag capacities`() {
        assertEquals(16, TagTypeDetector.totalPages(TagType.MIFARE_ULTRALIGHT))
        assertEquals(48, TagTypeDetector.totalPages(TagType.MIFARE_ULTRALIGHT_C))
        assertEquals(45, TagTypeDetector.totalPages(TagType.NTAG213))
        assertEquals(135, TagTypeDetector.totalPages(TagType.NTAG215))
        assertEquals(231, TagTypeDetector.totalPages(TagType.NTAG216))
    }
}
