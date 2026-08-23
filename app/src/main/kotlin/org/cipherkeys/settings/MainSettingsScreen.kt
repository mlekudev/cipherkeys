package org.cipherkeys.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cipherkeys.ui.CipherPrefs

@Composable
fun MainSettingsScreen(
    onKeysClick: () -> Unit,
    onRecipientsClick: () -> Unit,
    onKeyboardClick: () -> Unit = {},
    onBackupClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
) {
    var lines by remember { mutableIntStateOf(CipherPrefs.panelLines) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        SettingsCard(
            title = "Keys",
            subtitle = "Generate, import, and manage your PGP keys",
            icon = { Icon(Icons.Default.Key, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onKeysClick,
        )
        Spacer(Modifier.height(12.dp))
        SettingsCard(
            title = "Recipients",
            subtitle = "Manage encryption recipients and their public keys",
            icon = { Icon(Icons.Default.People, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onRecipientsClick,
        )
        Spacer(Modifier.height(12.dp))
        Card(
            onClick = onKeyboardClick,
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.height(24.dp).width(36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Abc", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Text("Keyboard", style = MaterialTheme.typography.titleMedium)
                Text("Haptic, sound, popup, capitalization, and key behavior",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(12.dp))
        SettingsCard(
            title = "Backup & Restore",
            subtitle = "Export or import keys, recipients, and settings as an encrypted file",
            icon = { Icon(Icons.Default.Save, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onBackupClick,
        )
        Spacer(Modifier.height(12.dp))
        SettingsCard(
            title = "Help",
            subtitle = "Learn how each button and feature works",
            icon = { Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onHelpClick,
        )
        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Dev mode", style = MaterialTheme.typography.titleMedium)
                    Text("Enable testing features", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = CipherPrefs.devMode,
                    onCheckedChange = { CipherPrefs.updateDevMode(it) },
                )
            }
        }

        // Panel lines control
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Panel Lines: $lines", style = MaterialTheme.typography.titleMedium)
                Text("How many lines of text in the cipher panel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { lines = (lines - 1).coerceAtLeast(2); CipherPrefs.updatePanelLines(lines) },
                        enabled = lines > 2,
                        modifier = Modifier.size(40.dp),
                    ) { Icon(Icons.Default.Remove, "Fewer", tint = MaterialTheme.colorScheme.primary) }
                    Text("$lines", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 16.dp))
                    IconButton(
                        onClick = { lines = (lines + 1).coerceAtMost(8); CipherPrefs.updatePanelLines(lines) },
                        enabled = lines < 8,
                        modifier = Modifier.size(40.dp),
                    ) { Icon(Icons.Default.Add, "More", tint = MaterialTheme.colorScheme.primary) }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            icon()
            Spacer(Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
