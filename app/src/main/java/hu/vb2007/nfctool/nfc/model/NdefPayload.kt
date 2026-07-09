package hu.vb2007.nfctool.nfc.model

/** What to write to a tag. Mirrors [NdefRecordKind] on the read side. */
sealed interface NdefPayload {
    data class Text(val text: String, val languageCode: String) : NdefPayload
    data class Uri(val uri: String) : NdefPayload
    data class Tel(val number: String) : NdefPayload
    data class Email(val address: String) : NdefPayload
    data class Sms(val number: String, val body: String) : NdefPayload
    data class Contact(val name: String, val phone: String, val email: String) : NdefPayload
}

data class WriteRequest(
    val payload: NdefPayload,
    val makeReadOnly: Boolean = false,
)
