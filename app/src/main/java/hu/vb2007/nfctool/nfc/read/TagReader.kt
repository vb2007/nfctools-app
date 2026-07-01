package hu.vb2007.nfctool.nfc.read

import android.nfc.Tag
import android.nfc.TagLostException
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import hu.vb2007.nfctool.nfc.model.NdefRecordModel
import hu.vb2007.nfctool.nfc.model.ScanResult
import hu.vb2007.nfctool.nfc.model.TagInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class TagReader(
    private val tagTypeDetector: TagTypeDetector,
    private val ndefParser: NdefParser,
    private val ultralightDumpReader: UltralightDumpReader,
) {
    suspend fun read(tag: Tag): ScanResult = withContext(Dispatchers.IO) {
        try {
            val uid = tag.id.joinToString(":") { "%02X".format(it) }
            val techList = tag.techList.map { it.substringAfterLast('.') }
            val tagType = tagTypeDetector.detect(tag)

            val ndef = Ndef.get(tag)
            var capacityBytes: Int? = null
            var usedBytes: Int? = null
            var isWritable = false
            var canMakeReadOnly = false
            var records: List<NdefRecordModel> = emptyList()

            if (ndef != null) {
                ndef.connect()
                try {
                    capacityBytes = ndef.maxSize
                    isWritable = ndef.isWritable
                    canMakeReadOnly = ndef.canMakeReadOnly()
                    val message = ndef.ndefMessage ?: ndef.cachedNdefMessage
                    if (message != null) {
                        usedBytes = message.toByteArray().size
                        records = ndefParser.parse(message)
                    }
                } finally {
                    ndef.close()
                }
            } else if (NdefFormatable.get(tag) != null) {
                isWritable = true
            }

            val hexDump = ultralightDumpReader.dump(tag, tagType)

            ScanResult.Success(
                TagInfo(
                    uid = uid,
                    techList = techList,
                    tagType = tagType,
                    capacityBytes = capacityBytes,
                    usedBytes = usedBytes,
                    isWritable = isWritable,
                    canMakeReadOnly = canMakeReadOnly,
                    ndefRecords = records,
                    hexDump = hexDump,
                ),
            )
        } catch (e: TagLostException) {
            ScanResult.Error("Tag was moved away before reading finished")
        } catch (e: IOException) {
            ScanResult.Error(e.message ?: "Couldn't read tag")
        }
    }
}
