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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import org.cipherkeys.cipher.KeyInfo
import org.cipherkeys.cipher.KeyManager

@Composable
fun KeyListScreen(
    keyManager: KeyManager,
    onGenerateClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val context = LocalContext.current
    var keys by remember { mutableStateOf(keyManager.listKeys()) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }

    fun refresh() { keys = keyManager.listKeys() }

    if (showDeleteDialog != null) {
        DeleteConfirmDialog(
            message = "Delete this key? This cannot be undone.",
            onConfirm = {
                keyManager.deleteKey(showDeleteDialog!!)
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
                Text("Stored Keys (${keys.size})", style = MaterialTheme.typography.titleMedium)
                Row {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.Add, "Import key")
                    }
                    IconButton(onClick = onGenerateClick) {
                        Icon(Icons.Default.AutoAwesome, "Generate key")
                    }
                }
            }
        }

        if (keys.isEmpty()) {
            item {
                Text(
                    "No keys stored. Generate or import a PGP key.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(keys) { key ->
            KeyCard(
                keyInfo = key,
                onCopyPublicKey = {
                    try {
                        val pub = keyManager.exportPublicKey(key.keyId, "")
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("pubkey", pub))
                        Toast.makeText(context, "Public key copied", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Failed to export public key", Toast.LENGTH_SHORT).show()
                    }
                },
                onCopySecretKey = {
                    try {
                        val sec = keyManager.exportSecretKey(key.keyId, "")
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("secretkey", sec))
                        Toast.makeText(context, "Secret key copied - guard it carefully!", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(context, "Failed to export secret key", Toast.LENGTH_SHORT).show()
                    }
                },
                onDelete = { showDeleteDialog = key.keyId },
            )
        }
    }
}

@Composable
fun KeyCard(
    keyInfo: KeyInfo,
    onCopyPublicKey: () -> Unit,
    onCopySecretKey: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(keyInfo.userId, style = MaterialTheme.typography.bodyLarge)
            Text(
                "ID: ${keyInfo.keyId.toString(16)}  ${keyInfo.algorithm}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Row {
                TextButton("Copy Public", onClick = onCopyPublicKey)
                Spacer(Modifier.width(8.dp))
                TextButton("Copy Secret", onClick = onCopySecretKey)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
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

@Composable
fun DeleteConfirmDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Delete") },
        text = { Text(message) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
