package org.cipherkeys.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CipherPanel(
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onSend: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSettingsClick: () -> Unit,
    onRecipientsClick: () -> Unit,
    signingKeyName: String?,
    modifier: Modifier = Modifier,
) {
    val state = CipherUiState.state
    val dark = isSystemInDarkTheme()
    val panelBg = if (dark) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
    val panelFg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val dimFg = panelFg.copy(alpha = 0.45f)
    val accent = if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val needsPassphrase = state.pendingAction != null
    val enabled = !state.isLoading && state.isActive

    Column(
        modifier = modifier.fillMaxWidth()
            .background(panelBg)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = { CipherUiState.toggle() }, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (state.isActive) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                    "Toggle", tint = panelFg, modifier = Modifier.size(24.dp),
                )
            }

            if (state.isActive) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    val tint = if (enabled && state.composeText.isNotEmpty()) accent else dimFg
                    IconButton(onClick = onEncrypt, enabled = enabled && state.composeText.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Lock, "Encrypt", tint = tint, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onDecrypt, enabled = enabled && state.composeText.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.LockOpen, "Decrypt", tint = tint, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onSend, enabled = enabled && state.composeText.isNotEmpty() && !needsPassphrase, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send",
                            tint = if (needsPassphrase) dimFg.copy(alpha = 0.15f) else tint,
                            modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onCopy, enabled = enabled && (state.composeText.isNotEmpty() || state.decryptedText.isNotEmpty()), modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, "Copy", tint = tint, modifier = Modifier.size(22.dp))
                    }
                    IconButton(onClick = onPaste, enabled = enabled && !state.isLoading, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentPaste, "Paste", tint = tint, modifier = Modifier.size(22.dp))
                    }
                    Text("${state.selectedRecipientIds.size}", fontSize = 13.sp, color = dimFg)
                    IconButton(onClick = onRecipientsClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.People, "Recipients", tint = dimFg, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Settings, "Settings", tint = dimFg, modifier = Modifier.size(20.dp))
                    }
                    if (needsPassphrase) {
                        Text(" passphrase", style = MaterialTheme.typography.bodySmall, color = Color(0xFFEF5350))
                        IconButton(onClick = { CipherUiState.cancelPassphrase() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Close, "Cancel", tint = Color(0xFFEF5350), modifier = Modifier.size(22.dp))
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = state.isActive, enter = expandVertically(), exit = shrinkVertically()) {
            Box(modifier = Modifier.fillMaxWidth().background(panelBg)) {
                Column(modifier = Modifier.padding(bottom = 3.dp)) {
                    if (state.errorMessage != null) {
                        Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(2.dp))
                    }
                    CipherComposePanel(
                        text = state.composeText,
                        cursorPos = state.cursorPos,
                        onCursorMoved = { CipherUiState.setCursorPos(it) },
                        placeholder = when {
                            needsPassphrase -> "Enter passphrase..."
                            state.isActive -> "Type message to encrypt..."
                            else -> ""
                        },
                        isPassword = needsPassphrase,
                        active = state.isActive,
                    )
                }
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(16.dp), strokeWidth = 2.dp)
                }
            }
        }
    }
}
