package hu.vb2007.nfctool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import hu.vb2007.nfctool.nfc.NfcController
import hu.vb2007.nfctool.ui.navigation.AppRoot
import hu.vb2007.nfctool.ui.theme.NfcToolTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val nfcController: NfcController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NfcToolTheme {
                AppRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        nfcController.enableReaderMode(this)
    }

    override fun onPause() {
        super.onPause()
        nfcController.disableReaderMode(this)
    }
}
