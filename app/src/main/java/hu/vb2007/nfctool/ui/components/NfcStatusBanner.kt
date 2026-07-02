package hu.vb2007.nfctool.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import hu.vb2007.nfctool.nfc.NfcAdapterState

@Composable
fun NfcStatusBanner(
    adapterState: NfcAdapterState,
    onOpenNfcSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (adapterState) {
        NfcAdapterState.NO_HARDWARE -> "This device doesn't support NFC."
        NfcAdapterState.DISABLED -> "NFC is turned off."
        NfcAdapterState.READY -> return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = message, color = MaterialTheme.colorScheme.onErrorContainer)
            if (adapterState == NfcAdapterState.DISABLED) {
                Button(onClick = onOpenNfcSettings) {
                    Text("Open settings")
                }
            }
        }
    }
}
