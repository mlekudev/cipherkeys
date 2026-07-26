package org.cipherkeys.settings.help

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpDetailScreen(entry: HelpEntry) {
    val dark = isSystemInDarkTheme()
    val keyBg = if (dark) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
    val keyFg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val specialBg = if (dark) Color(0xFF383838) else Color(0xFFC0C0C0)
    val kbBg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val compBg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val accent = if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val dim = keyFg.copy(alpha = 0.45f)
    val highlight = Color(0xFFFF9800)

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        Text(entry.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(entry.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))

        // Mock preview with highlight
        Box(
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(BorderStroke(2.dp, highlight), RoundedCornerShape(12.dp)).background(kbBg),
        ) {
            when (entry.mock) {
                MockView.COLLAPSED -> {
                    Column {
                        // Collapsed header
                        Box(modifier = Modifier.fillMaxWidth().background(keyBg).padding(horizontal = 6.dp, vertical = 3.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp)) {
                                Box(
                                    modifier = if (entry.id == "toggle-arrow-collapsed") Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).border(2.dp, highlight) else Modifier
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Toggle", tint = accent, modifier = Modifier.size(24.dp).padding(4.dp))
                                }
                                Spacer(Modifier.weight(1f))
                            }
                        }
                        // Keyboard
                        KeyboardMock(keyBg, keyFg, specialBg, kbBg, dim, listOf()) {}
                    }
                }
                MockView.EXPANDED -> {
                    Column {
                        // Expanded header
                        Box(modifier = Modifier.fillMaxWidth().background(keyBg).padding(horizontal = 4.dp, vertical = 2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(36.dp)) {
                                Box(
                                    modifier = if (entry.id == "toggle-arrow-expanded") Modifier.size(32.dp).clip(RoundedCornerShape(4.dp)).border(2.dp, highlight) else Modifier
                                ) {
                                    Icon(Icons.Default.KeyboardArrowDown, "Toggle", tint = accent, modifier = Modifier.size(24.dp).padding(4.dp))
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    listOf(
                                        "encrypt-btn" to Icons.Default.Lock,
                                        "decrypt-btn" to Icons.Default.LockOpen,
                                        "send-btn" to Icons.AutoMirrored.Filled.Send,
                                        "copy-btn" to Icons.Default.ContentCopy,
                                        "paste-btn" to Icons.Default.ContentPaste,
                                    ).forEach { (id, icon) ->
                                        Box(
                                            modifier = if (entry.id == id) Modifier.size(32.dp).border(2.dp, highlight, RoundedCornerShape(4.dp)) else Modifier.size(32.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(icon, id, tint = accent, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    listOf(
                                        "recipients-btn" to Icons.Default.People,
                                        "settings-btn" to Icons.Default.Settings,
                                    ).forEach { (id, icon) ->
                                        Box(
                                            modifier = if (entry.id == id) Modifier.size(32.dp).border(2.dp, highlight, RoundedCornerShape(4.dp)) else Modifier.size(32.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(icon, id, tint = keyFg.copy(alpha = 0.45f), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                        }
                        // Compose panel
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                                .clip(RoundedCornerShape(6.dp)).background(compBg)
                                .padding(12.dp).height(60.dp)
                                .then(if (entry.id == "compose-panel" || entry.id == "cursor") Modifier.border(2.dp, highlight, RoundedCornerShape(6.dp)) else Modifier),
                            contentAlignment = Alignment.TopStart,
                        ) {
                            Text("Type message to encrypt...", style = TextStyle(fontSize = 17.sp, fontFamily = FontFamily.Monospace), color = keyFg.copy(alpha = 0.4f))
                        }
                        // Keyboard
                        KeyboardMock(keyBg, keyFg, specialBg, kbBg, dim, listOf()) {}
                    }
                }
            }
        }
    }
}
