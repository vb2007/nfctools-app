package hu.vb2007.nfctool.nfc.read

import android.nfc.Tag
import android.nfc.tech.MifareUltralight
import hu.vb2007.nfctool.nfc.model.TagType
import java.io.IOException

class UltralightDumpReader {

    fun dump(tag: Tag, tagType: TagType): List<String>? {
        val mifareUltralight = MifareUltralight.get(tag) ?: return null
        return try {
            mifareUltralight.connect()
            try {
                readAllPages(mifareUltralight, TagTypeDetector.totalPages(tagType))
            } finally {
                mifareUltralight.close()
            }
        } catch (e: IOException) {
            null
        }
    }

    private fun readAllPages(mifareUltralight: MifareUltralight, totalPages: Int): List<String> {
        val pages = mutableListOf<String>()
        var pageIndex = 0
        while (pageIndex < totalPages) {
            val chunk = try {
                mifareUltralight.readPages(pageIndex)
            } catch (e: IOException) {
                break
            }
            val pagesInChunk = minOf(4, totalPages - pageIndex)
            for (offset in 0 until pagesInChunk) {
                val byteOffset = offset * 4
                if (byteOffset + 4 > chunk.size) break
                pages.add(formatPage(pageIndex + offset, chunk.copyOfRange(byteOffset, byteOffset + 4)))
            }
            pageIndex += 4
        }
        return pages
    }

    companion object {
        /** Pure: formats one 4-byte page as a display line. Testable without hardware. */
        fun formatPage(page: Int, bytes: ByteArray): String {
            val hex = bytes.joinToString(" ") { "%02X".format(it) }
            return "%02d  %s".format(page, hex)
        }
    }
}
