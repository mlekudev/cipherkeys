package org.cipherkeys.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.cipherkeys.cipher.RecipientInfo
import org.cipherkeys.cipher.RecipientManager
import org.cipherkeys.ui.CipherPrefs
import org.cipherkeys.ui.CipherUiState

@Composable
fun RecipientListScreen(
    recipientManager: RecipientManager,
    onImportClick: () -> Unit,
) {
    val context = LocalContext.current
    var recipients by remember { mutableStateOf(recipientManager.listRecipients()) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    val activeIds = CipherUiState.state.selectedRecipientIds

    fun refresh() { recipients = recipientManager.listRecipients() }

    if (showDeleteDialog != null) {
        DeleteConfirmDialog(
            message = "Remove this recipient?",
            onConfirm = {
                CipherUiState.toggleRecipient(showDeleteDialog!!)
                recipientManager.removeRecipient(showDeleteDialog!!)
                showDeleteDialog = null
                refresh()
            },
            onDismiss = { showDeleteDialog = null },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Recipients (${recipients.size})", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onImportClick) {
                    Icon(Icons.Default.Add, "Add recipient")
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Always select recipients", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Show recipient picker on every encryption",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = CipherPrefs.alwaysAskRecipients,
                        onCheckedChange = { CipherPrefs.updateAlwaysAskRecipients(it) },
                    )
                }
            }
        }

        if (recipients.isEmpty()) {
            item {
                Text(
                    "No recipients added. Import a public key to encrypt messages.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(recipients) { recipient ->
            val isActive = activeIds.contains(recipient.keyId)
            RecipientCard(
                recipient = recipient,
                isActive = isActive,
                onToggle = {
                    val wasActive = activeIds.contains(recipient.keyId)
                    CipherUiState.toggleRecipient(recipient.keyId)
                    if (!wasActive) recipientManager.markSelected(recipient.keyId)
                },
                onCopyKey = {
                    val armored = recipientManager.exportArmoredPublicKey(recipient.keyId)
                    if (armored != null) {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("pubkey", armored))
                        Toast.makeText(context, "Key copied", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = { showDeleteDialog = recipient.keyId },
            )
        }
    }
}

@Composable
fun RecipientCard(
    recipient: RecipientInfo,
    isActive: Boolean,
    onToggle: () -> Unit,
    onCopyKey: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(recipient.name, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        recipient.userId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = isActive, onCheckedChange = { onToggle() })
            }
            Text(
                "ID: ${recipient.keyId.toString(16)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(4.dp))
            Row {
                TextButton("Copy Key", onClick = onCopyKey)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Remove", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun TextButton(text: String, onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}
