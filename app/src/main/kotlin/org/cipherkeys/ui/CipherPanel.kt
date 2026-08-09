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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.cipherkeys.cipher.RecipientInfo

@Composable
fun CipherPanel(
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onSend: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onSettingsClick: () -> Unit,
    onHelpClick: () -> Unit,
    onRecipientsClick: () -> Unit,
    signingKeyName: String?,
    allRecipients: List<RecipientInfo> = emptyList(),
    onToggleRecipient: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state = CipherUiState.state
    val dark = isSystemInDarkTheme()
    val panelBg = if (dark) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
    val panelFg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val dimFg = panelFg.copy(alpha = 0.45f)
    val needsPassphrase = state.pendingAction != null
    val enabled = !state.isLoading && state.isActive

    Column(
        modifier = modifier.fillMaxWidth().background(panelBg).padding(horizontal = 6.dp, vertical = 3.dp),
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
                    val tint = if (enabled && state.composeText.isNotEmpty()) panelFg else dimFg
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
                    IconButton(onClick = onHelpClick, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Help, "Help", tint = dimFg, modifier = Modifier.size(20.dp))
                    }
                }
            } else {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Settings, "Settings", tint = dimFg, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onHelpClick, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Help, "Help", tint = dimFg, modifier = Modifier.size(20.dp))
                }
            }
        }

        if (needsPassphrase && state.isActive) {
            LaunchedEffect(state.composeText) {
                if (needsPassphrase && !state.showPassword && state.composeText.isNotEmpty()) {
                    CipherUiState.setRevealLastChar(true)
                    delay(2000)
                    CipherUiState.setRevealLastChar(false)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp).background(panelBg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { CipherUiState.toggleShowPassword() }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (state.showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Toggle visibility",
                        tint = dimFg,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text("Enter passphrase", fontSize = 12.sp, color = panelFg, modifier = Modifier.padding(start = 4.dp))
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { CipherUiState.cancelPassphrase() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Cancel", tint = dimFg, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (state.errorMessage != null && state.isActive) {
            Row(
                modifier = Modifier.fillMaxWidth().height(30.dp).background(panelBg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    state.errorMessage!!,
                    fontSize = 12.sp,
                    color = dimFg,
                    modifier = Modifier.padding(horizontal = 8.dp).weight(1f),
                    maxLines = 1,
                )
                IconButton(onClick = { CipherUiState.clearError() }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, "Dismiss", tint = dimFg, modifier = Modifier.size(18.dp))
                }
            }
        }

        if (state.showRecipientPicker && state.isActive) {
            Box(modifier = Modifier.fillMaxWidth().background(panelBg)) {
                Column(modifier = Modifier.padding(bottom = 3.dp)) {
                    Text(
                        "Select Recipients",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = panelFg,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                    allRecipients.forEach { recip ->
                        val isSelected = recip.keyId in state.selectedRecipientIds
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    recip.name.ifEmpty { recip.userId },
                                    fontSize = 13.sp,
                                    color = panelFg,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (recip.name.isNotEmpty()) {
                                    Text(
                                        recip.userId,
                                        fontSize = 10.sp,
                                        color = dimFg,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                            Switch(
                                checked = isSelected,
                                onCheckedChange = { onToggleRecipient(recip.keyId) },
                                modifier = Modifier.size(24.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(panelFg.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Text("${state.selectedRecipientIds.size} selected", fontSize = 11.sp, color = dimFg)
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(panelFg.copy(alpha = 0.1f))
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Cancel", fontSize = 12.sp, color = dimFg)
                            }
                        }
                        IconButton(onClick = { CipherUiState.hideRecipientPicker() }, modifier = Modifier.height(28.dp)) {
                            Icon(Icons.Default.Close, "Cancel", tint = dimFg, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                val sendAfter = CipherUiState.shouldSendAfterPicker()
                                CipherUiState.hideRecipientPicker()
                                CipherUiState.setLoading(true)
                                if (sendAfter) onSend() else onEncrypt()
                            },
                            enabled = state.selectedRecipientIds.isNotEmpty(),
                            modifier = Modifier.height(28.dp),
                        ) {
                            Icon(Icons.Default.Check, "Done", tint = if (state.selectedRecipientIds.isNotEmpty()) panelFg else dimFg.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        } else {
            AnimatedVisibility(visible = state.isActive, enter = expandVertically(), exit = shrinkVertically()) {
                Box(modifier = Modifier.fillMaxWidth().background(panelBg)) {
                    Column(modifier = Modifier.padding(bottom = 3.dp)) {
                        CipherComposePanel(
                            text = state.composeText,
                            cursorPos = state.cursorPos,
                            onCursorMoved = { CipherUiState.setCursorPos(it) },
                            placeholder = when {
                                needsPassphrase -> "Enter passphrase..."
                                state.isActive -> "Type message to encrypt..."
                                else -> ""
                            },
                            isPassword = needsPassphrase && !state.showPassword,
                            revealLastChar = state.revealLastChar,
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
}
