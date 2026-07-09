package hu.vb2007.nfctool.ui.write

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
                    state = state,
                    onEditingChange = viewModel::updateEditing,
                    onWriteClick = viewModel::startWaitingForTag,
                    modifier = Modifier.fillMaxSize(),
                )
                is WriteUiState.WaitingForTag -> WaitingForTagContent(
                    onCancel = viewModel::cancelWaiting,
                    modifier = Modifier.fillMaxSize(),
                )
                is WriteUiState.Success -> ResultContent(
                    message = successMessage(state),
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

private fun successMessage(state: WriteUiState.Success): String = when {
    state.warning != null -> "Tag written, but ${state.warning}"
    state.madeReadOnly -> "Tag written and locked read-only"
    else -> "Tag written successfully"
}

@Composable
private fun EditingContent(
    state: WriteUiState.Editing,
    onEditingChange: ((WriteUiState.Editing) -> WriteUiState.Editing) -> Unit,
    onWriteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLockConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        RecordTypeSelector(
            selected = state.recordType,
            onSelect = { type -> onEditingChange { it.copy(recordType = type) } },
        )

        when (state.recordType) {
            WriteRecordType.TEXT -> {
                OutlinedTextField(
                    value = state.text,
                    onValueChange = { text -> onEditingChange { it.copy(text = text) } },
                    label = { Text("Text") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.languageCode,
                    onValueChange = { code -> onEditingChange { it.copy(languageCode = code) } },
                    label = { Text("Language code (e.g. en)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            WriteRecordType.URL -> OutlinedTextField(
                value = state.uri,
                onValueChange = { uri -> onEditingChange { it.copy(uri = uri) } },
                label = { Text("URL") },
                modifier = Modifier.fillMaxWidth(),
            )
            WriteRecordType.TEL -> OutlinedTextField(
                value = state.tel,
                onValueChange = { tel -> onEditingChange { it.copy(tel = tel) } },
                label = { Text("Phone number") },
                modifier = Modifier.fillMaxWidth(),
            )
            WriteRecordType.EMAIL -> OutlinedTextField(
                value = state.email,
                onValueChange = { email -> onEditingChange { it.copy(email = email) } },
                label = { Text("Email address") },
                modifier = Modifier.fillMaxWidth(),
            )
            WriteRecordType.SMS -> {
                OutlinedTextField(
                    value = state.smsNumber,
                    onValueChange = { number -> onEditingChange { it.copy(smsNumber = number) } },
                    label = { Text("Phone number") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.smsBody,
                    onValueChange = { body -> onEditingChange { it.copy(smsBody = body) } },
                    label = { Text("Message (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            WriteRecordType.CONTACT -> {
                OutlinedTextField(
                    value = state.contactName,
                    onValueChange = { name -> onEditingChange { it.copy(contactName = name) } },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.contactPhone,
                    onValueChange = { phone -> onEditingChange { it.copy(contactPhone = phone) } },
                    label = { Text("Phone (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.contactEmail,
                    onValueChange = { email -> onEditingChange { it.copy(contactEmail = email) } },
                    label = { Text("Email (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Checkbox(
                checked = state.makeReadOnly,
                onCheckedChange = { checked ->
                    if (checked) {
                        showLockConfirmDialog = true
                    } else {
                        onEditingChange { it.copy(makeReadOnly = false) }
                    }
                },
            )
            Text("Make read-only after writing")
        }

        Button(
            onClick = onWriteClick,
            enabled = state.isValid(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Write")
        }
    }

    if (showLockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showLockConfirmDialog = false },
            title = { Text("Make tag read-only?") },
            text = {
                Text(
                    "This permanently locks the tag after writing. It can never be " +
                        "written to again, on this or any other device. This cannot be undone.",
                )
            },
            confirmButton = {
                Button(onClick = {
                    onEditingChange { it.copy(makeReadOnly = true) }
                    showLockConfirmDialog = false
                }) {
                    Text("Lock permanently")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLockConfirmDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordTypeSelector(
    selected: WriteRecordType,
    onSelect: (WriteRecordType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WriteRecordType.entries.forEach { type ->
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(type.label()) },
            )
        }
    }
}

private fun WriteRecordType.label(): String = when (this) {
    WriteRecordType.TEXT -> "Text"
    WriteRecordType.URL -> "URL"
    WriteRecordType.TEL -> "Phone"
    WriteRecordType.EMAIL -> "Email"
    WriteRecordType.SMS -> "SMS"
    WriteRecordType.CONTACT -> "Contact"
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
