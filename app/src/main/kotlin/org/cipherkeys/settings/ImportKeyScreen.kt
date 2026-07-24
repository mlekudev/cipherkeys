package org.cipherkeys.settings

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import org.cipherkeys.cipher.CipherException
import org.cipherkeys.cipher.KeyManager

@Composable
fun ImportKeyScreen(
    keyManager: KeyManager,
    title: String,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    var armoredKey by remember { mutableStateOf("") }
    var passphrase by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPassphrase by remember { mutableStateOf(false) }

    val canImport = armoredKey.isNotBlank() &&
        (passphrase.isNotBlank() || !showPassphrase)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            "Paste the PGP private key in ASCII-armored format (starts with -----BEGIN PGP PRIVATE KEY BLOCK-----)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = armoredKey,
            onValueChange = { armoredKey = it },
            label = { Text("Armored Private Key") },
            minLines = 5,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth(),
        )

        if (!showPassphrase) {
            TextButton(onClick = { showPassphrase = true }) {
                Text("+ Add passphrase")
            }
        } else {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                label = { Text("Passphrase") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isLoading = true
                try {
                    val info = keyManager.importSecretKey(armoredKey, passphrase)
                    Toast.makeText(context, "Key imported: ${info.userId}", Toast.LENGTH_SHORT).show()
                    onDone()
                } catch (e: CipherException) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            },
            enabled = canImport && !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (isLoading) "Importing..." else "Import Key")
        }
    }
}
