package hu.vb2007.nfctool.ui.read

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import hu.vb2007.nfctool.nfc.model.NdefRecordModel
import hu.vb2007.nfctool.nfc.model.TagInfo
import hu.vb2007.nfctool.ui.components.NfcStatusBanner
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadScreen(
    onBack: () -> Unit,
    viewModel: ReadViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val adapterState by viewModel.adapterState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Read a tag") },
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
                is ReadUiState.Scanning -> ScanningPrompt(modifier = Modifier.fillMaxSize())
                is ReadUiState.Success -> TagResultView(
                    tagInfo = state.tagInfo,
                    onScanAgain = viewModel::scanAgain,
                    modifier = Modifier.fillMaxSize(),
                )
                is ReadUiState.Error -> ErrorView(
                    reason = state.reason,
                    onScanAgain = viewModel::scanAgain,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun ScanningPrompt(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text(
                text = "Hold a tag to the back of your phone",
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun ErrorView(reason: String, onScanAgain: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = reason, color = MaterialTheme.colorScheme.error)
            Button(onClick = onScanAgain, modifier = Modifier.padding(top = 16.dp)) {
                Text("Scan again")
            }
        }
    }
}

@Composable
private fun TagResultView(tagInfo: TagInfo, onScanAgain: () -> Unit, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { TagSummaryCard(tagInfo) }
        if (tagInfo.ndefRecords.isNotEmpty()) {
            item { Text("NDEF records", style = MaterialTheme.typography.titleMedium) }
            items(tagInfo.ndefRecords) { record -> NdefRecordRow(record) }
        }
        if (tagInfo.hexDump != null) {
            item { HexDumpSection(tagInfo.hexDump) }
        }
        item {
            Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
                Text("Scan another tag")
            }
        }
    }
}

@Composable
private fun TagSummaryCard(tagInfo: TagInfo) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = tagInfo.tagType.displayName(), style = MaterialTheme.typography.titleLarge)
            Text(text = "UID: ${tagInfo.uid}")
            Text(text = "Technologies: ${tagInfo.techList.joinToString(", ")}")
            if (tagInfo.capacityBytes != null) {
                val used = tagInfo.usedBytes ?: 0
                Text(text = "Memory: $used / ${tagInfo.capacityBytes} bytes used")
            }
            Text(text = if (tagInfo.isWritable) "Writable" else "Read-only")
        }
    }
}

@Composable
private fun NdefRecordRow(record: NdefRecordModel) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = record.label, style = MaterialTheme.typography.labelLarge)
            Text(text = record.value)
        }
    }
}
