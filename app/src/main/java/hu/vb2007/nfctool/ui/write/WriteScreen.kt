package hu.vb2007.nfctool.ui.write

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hu.vb2007.nfctool.ui.components.NfcStatusBanner
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteScreen(
    onBack: () -> Unit,
    viewModel: WriteViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adapterState by viewModel.adapterState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Write a tag") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            NfcStatusBanner(
                adapterState = adapterState,
                onOpenNfcSettings = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
            )

            when (val state = uiState) {
                is WriteUiState.Editing -> EditingContent(
                    text = state.text,
                    onTextChange = viewModel::onTextChange,
                    onWriteClick = viewModel::startWaitingForTag,
                    modifier = Modifier.fillMaxSize(),
                )
                is WriteUiState.WaitingForTag -> WaitingForTagContent(
                    onCancel = viewModel::cancelWaiting,
                    modifier = Modifier.fillMaxSize(),
                )
                WriteUiState.Success -> ResultContent(
                    message = "Tag written successfully",
                    isError = false,
                    onDismiss = viewModel::reset,
                    modifier = Modifier.fillMaxSize(),
                )
                is WriteUiState.Failure -> ResultContent(
                    message = state.reason,
                    isError = true,
                    onDismiss = viewModel::reset,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun EditingContent(
    text: String,
    onTextChange: (String) -> Unit,
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { Text("Text") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onWriteClick,
            enabled = text.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Write")
        }
    }
}

@Composable
private fun WaitingForTagContent(onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "Hold a tag to the back of your phone",
                modifier = Modifier.padding(top = 16.dp),
            )
            OutlinedButton(onClick = onCancel, modifier = Modifier.padding(top = 16.dp)) {
                Text("Cancel")
            }
        }
    }
}

@Composable
private fun ResultContent(
    message: String,
    isError: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = message,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Button(onClick = onDismiss, modifier = Modifier.padding(top = 16.dp)) {
                Text(if (isError) "Try again" else "Write another")
            }
        }
    }
}
