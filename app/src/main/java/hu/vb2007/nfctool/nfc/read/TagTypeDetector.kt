package hu.vb2007.nfctool.nfc.read

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import hu.vb2007.nfctool.nfc.model.TagType
import java.io.IOException

class TagTypeDetector {

    fun detect(tag: Tag): TagType {
        val mifareUltralight = MifareUltralight.get(tag) ?: return detectNonUltralight(tag)
        return try {
            mifareUltralight.connect()
            try {
                val response = runCatching { mifareUltralight.transceive(GET_VERSION_COMMAND) }.getOrNull()
                classifyGetVersionResponse(response) ?: classifyLegacyType(mifareUltralight.type)
            } finally {
                mifareUltralight.close()
            }
        } catch (e: IOException) {
            TagType.UNKNOWN
        }
    }

    private fun detectNonUltralight(tag: Tag): TagType =
        if (Ndef.get(tag) != null) TagType.OTHER_NDEF else TagType.UNKNOWN

    companion object {
        val GET_VERSION_COMMAND = byteArrayOf(0x60)
        private const val NTAG_PRODUCT_TYPE: Byte = 0x04
        private const val STORAGE_SIZE_NTAG213: Byte = 0x0F
        private const val STORAGE_SIZE_NTAG215: Byte = 0x11
        private const val STORAGE_SIZE_NTAG216: Byte = 0x13

        /**
         * Classifies a raw GET_VERSION (0x60) response per the NXP NTAG21x datasheet.
         * Pure function, testable without hardware.
         */
        fun classifyGetVersionResponse(response: ByteArray?): TagType? {
            if (response == null || response.size < 8 || response[2] != NTAG_PRODUCT_TYPE) return null
            return when (response[6]) {
                STORAGE_SIZE_NTAG213 -> TagType.NTAG213
                STORAGE_SIZE_NTAG215 -> TagType.NTAG215
                STORAGE_SIZE_NTAG216 -> TagType.NTAG216
                else -> null
            }
        }

        /** Classifies the legacy (pre-GET_VERSION) Ultralight family. Pure, testable without hardware. */
        fun classifyLegacyType(mifareType: Int): TagType = when (mifareType) {
            MifareUltralight.TYPE_ULTRALIGHT_C -> TagType.MIFARE_ULTRALIGHT_C
            else -> TagType.MIFARE_ULTRALIGHT
        }

        /** Total user-addressable pages for a detected type; used to bound the hex dump. */
        fun totalPages(tagType: TagType): Int = when (tagType) {
            TagType.MIFARE_ULTRALIGHT -> 16
            TagType.MIFARE_ULTRALIGHT_C -> 48
            TagType.NTAG213 -> 45
            TagType.NTAG215 -> 135
            TagType.NTAG216 -> 231
            else -> 16
        }
    }
}
