package org.cipherkeys.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.cipherkeys.cipher.CipherException
import org.cipherkeys.cipher.KeyInfo
import org.cipherkeys.cipher.KeyManager
import org.cipherkeys.ui.CipherPrefs

@Composable
fun KeyListScreen(
    keyManager: KeyManager,
    onGenerateClick: () -> Unit,
    onImportClick: () -> Unit,
) {
    val context = LocalContext.current
    var keys by remember { mutableStateOf(keyManager.listKeys()) }
    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var showSecretExport by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(keys) {
        if (CipherPrefs.defaultKeyId == null && keys.isNotEmpty()) {
            CipherPrefs.updateDefaultKeyId(keys.first().keyId)
        }
    }

    fun refresh() { keys = keyManager.listKeys() }

    if (showDeleteDialog != null) {
        DeleteConfirmDialog(
            message = "Delete this key? This cannot be undone.",
            onConfirm = {
                keyManager.deleteKey(showDeleteDialog!!)
                if (CipherPrefs.defaultKeyId == showDeleteDialog) {
                    val remaining = keyManager.listKeys()
                    CipherPrefs.updateDefaultKeyId(remaining.firstOrNull()?.keyId)
                }
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

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Always encrypt to self", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "If disabled, you cannot decrypt messages you send",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Text(
                                "Automatically add selected key to all encryptions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = CipherPrefs.encryptToSelf,
                            onCheckedChange = { v ->
                                CipherPrefs.updateEncryptToSelf(v)
                                if (v && CipherPrefs.defaultKeyId == null && keys.isNotEmpty()) {
                                    CipherPrefs.updateDefaultKeyId(keys.first().keyId)
                                }
                            },
                        )
                    }
                    Text(
                        "Uses the default key selected by radio button below",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(keys) { key ->
            KeyCard(
                keyInfo = key,
                isDefault = CipherPrefs.defaultKeyId == key.keyId,
                onSetDefault = { CipherPrefs.updateDefaultKeyId(key.keyId) },
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
                onCopySecretKey = { showSecretExport = key.keyId },
                onDelete = { showDeleteDialog = key.keyId },
            )
        }
    }

    if (showSecretExport != null) {
        SecretExportDialog(
            keyId = showSecretExport!!,
            keyManager = keyManager,
            onDismiss = { showSecretExport = null },
        )
    }
}

@Composable
fun KeyCard(
    keyInfo: KeyInfo,
    isDefault: Boolean,
    onSetDefault: () -> Unit,
    onCopyPublicKey: () -> Unit,
    onCopySecretKey: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = isDefault,
                    onClick = onSetDefault,
                )
                Column(Modifier.weight(1f)) {
                    Text(keyInfo.userId, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "ID: ${keyInfo.keyId.toString(16)}  ${keyInfo.algorithm}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        if (isDefault) "Default signing/encryption key" else "Tap radio to make default",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDefault) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
private fun SecretExportDialog(
    keyId: Long,
    keyManager: KeyManager,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var passphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Secret Key") },
        text = {
            Column {
                Text(
                    "Enter the key passphrase to confirm. The secret key will be copied to the clipboard.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; error = null },
                    label = { Text("Passphrase") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    try {
                        val sec = keyManager.exportSecretKey(keyId, passphrase)
                        val clip = ClipData.newPlainText("secretkey", sec)
                        clip.description.extras = PersistableBundle().apply {
                            putBoolean("android.content.extra.IS_SENSITIVE", true)
                        }
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(clip)
                        Toast.makeText(context, "Secret key copied - guard it carefully!", Toast.LENGTH_LONG).show()
                        Handler(Looper.getMainLooper()).postDelayed({
                            val current = cm.primaryClip?.getItemAt(0)?.text?.toString()
                            if (current == sec) cm.clearPrimaryClip()
                        }, 30000)
                        onDismiss()
                    } catch (e: CipherException) {
                        error = e.message ?: "Export failed"
                    } catch (_: Exception) {
                        error = "Export failed"
                    }
                },
            ) {
                Text("Copy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
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
