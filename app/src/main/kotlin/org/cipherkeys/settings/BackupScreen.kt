package org.cipherkeys.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import org.cipherkeys.cipher.CipherException
import org.cipherkeys.cipher.KeyManager
import org.cipherkeys.cipher.PgpEngine
import org.cipherkeys.cipher.RecipientManager

@Composable
fun BackupScreen(
    keyManager: KeyManager,
    recipientManager: RecipientManager,
) {
    val context = LocalContext.current
    val pgpEngine = remember { PgpEngine() }

    var exportBlob by remember { mutableStateOf<String?>(null) }
    var showExportPassword by remember { mutableStateOf(false) }
    var showImportPassword by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<String?>(null) }

    val createDocument = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null && exportBlob != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(exportBlob!!.toByteArray(Charsets.UTF_8))
                } ?: throw CipherException("cannot write file")
                Toast.makeText(context, "Configuration exported", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
            exportBlob = null
        }
    }

    val openDocument = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val data = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.readBytes().toString(Charsets.UTF_8)
                } ?: throw CipherException("cannot read file")
                pendingImportData = data
                showImportPassword = true
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        Text("Configuration Backup", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Export your keys, recipients, and settings to an encrypted file. " +
                "Restore them on another device or after a reinstall.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { showExportPassword = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export configuration")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { openDocument.launch(arrayOf("application/octet-stream", "text/plain")) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Import configuration")
        }

        if (keyManager.listKeys().isEmpty() && recipientManager.listRecipients().isEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Text(
                    "Nothing to back up yet - generate or import a key first.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (showExportPassword) {
        PasswordDialog(
            title = "Export - set a backup password",
            confirmPassword = true,
            onDismiss = { showExportPassword = false },
            onConfirm = { passphrase ->
                try {
                    exportBlob = ConfigBackup.export(keyManager, recipientManager, pgpEngine, passphrase)
                    showExportPassword = false
                    createDocument.launch("cipherkeys-backup.txt")
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            },
        )
    }

    if (showImportPassword) {
        PasswordDialog(
            title = "Import - enter backup password",
            confirmPassword = false,
            onDismiss = { showImportPassword = false; pendingImportData = null },
            onConfirm = { passphrase ->
                val data = pendingImportData
                if (data != null) {
                    try {
                        ConfigBackup.import(data, keyManager, recipientManager, pgpEngine, passphrase)
                        Toast.makeText(context, "Configuration imported", Toast.LENGTH_SHORT).show()
                    } catch (e: CipherException) {
                        Toast.makeText(context, e.message ?: "Import failed", Toast.LENGTH_LONG).show()
                        return@PasswordDialog
                    } catch (e: Exception) {
                        Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                        return@PasswordDialog
                    }
                    showImportPassword = false
                    pendingImportData = null
                }
            },
        )
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    confirmPassword: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = passphrase,
                    onValueChange = { passphrase = it; error = null },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (confirmPassword) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it; error = null },
                        label = { Text("Confirm password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        isError = error != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (error != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (passphrase.length < 4) {
                        error = "Password must be at least 4 characters"
                    } else if (confirmPassword && passphrase != confirm) {
                        error = "Passwords do not match"
                    } else {
                        onConfirm(passphrase)
                    }
                },
            ) {
                Text(if (confirmPassword) "Export" else "Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
