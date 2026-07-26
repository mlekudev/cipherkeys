package org.cipherkeys.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
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
    onHelpClick: () -> Unit = {},
) {
    var keyH by remember { mutableIntStateOf(CipherPrefs.keyHeightDp) }
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
        SettingsCard(
            title = "Help",
            subtitle = "Learn how each button and feature works",
            icon = { Icon(Icons.Default.Help, null, tint = MaterialTheme.colorScheme.primary) },
            onClick = onHelpClick,
        )
        Spacer(Modifier.height(16.dp))

        // Key height slider
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Key Height: ${keyH}dp", style = MaterialTheme.typography.titleMedium)
                Slider(
                    value = keyH.toFloat(),
                    onValueChange = { keyH = it.toInt(); CipherPrefs.updateKeyHeight(keyH) },
                    valueRange = 34f..60f,
                    steps = 12,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        Spacer(Modifier.height(8.dp))

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
