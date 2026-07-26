package org.cipherkeys.settings.help

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HelpScreen(onElementClick: (HelpEntry) -> Unit) {
    val dark = isSystemInDarkTheme()
    val keyBg = if (dark) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
    val keyFg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val specialBg = if (dark) Color(0xFF383838) else Color(0xFFC0C0C0)
    val kbBg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val compBg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val accent = if (dark) Color(0xFF90CAF9) else Color(0xFF1565C0)
    val dim = keyFg.copy(alpha = 0.45f)

    var tab by rememberSaveable { mutableIntStateOf(0) }

    val collapsed = allHelpEntries.filter { it.mock == MockView.COLLAPSED }
    val expanded = allHelpEntries.filter { it.mock == MockView.EXPANDED }
    val allKeyboard = allHelpEntries  // show all entries in keyboard mock

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Button(onClick = { tab = 0 }, modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (tab == 0) accent else keyBg, contentColor = if (tab == 0) Color.White else keyFg),
                shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)) { Text("Collapsed", fontSize = 12.sp) }
            Button(onClick = { tab = 1 }, modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (tab == 1) accent else keyBg, contentColor = if (tab == 1) Color.White else keyFg),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)) { Text("Expanded", fontSize = 12.sp) }
        }

        Box(modifier = Modifier.fillMaxWidth().background(kbBg)) {
            Column {
                if (tab == 0) {
                    Box(modifier = Modifier.fillMaxWidth().background(keyBg).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(28.dp)) {
                            HelpChip(collapsed.find { it.id == "toggle-arrow-collapsed" }!!, onElementClick, keyFg) {
                                Icon(Icons.Default.KeyboardArrowUp, null, tint = dim, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().background(keyBg).padding(horizontal = 4.dp, vertical = 2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(34.dp)) {
                            HelpChip(expanded.find { it.id == "toggle-arrow-expanded" }!!, onElementClick, keyFg) {
                                Icon(Icons.Default.KeyboardArrowDown, null, tint = dim, modifier = Modifier.size(22.dp))
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                                HelpChip(expanded.find { it.id == "encrypt-btn" }!!, onElementClick, accent) { Icon(Icons.Default.Lock, null, tint = accent, modifier = Modifier.size(20.dp)) }
                                HelpChip(expanded.find { it.id == "decrypt-btn" }!!, onElementClick, accent) { Icon(Icons.Default.LockOpen, null, tint = accent, modifier = Modifier.size(20.dp)) }
                                HelpChip(expanded.find { it.id == "send-btn" }!!, onElementClick, accent) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = accent, modifier = Modifier.size(20.dp)) }
                                HelpChip(expanded.find { it.id == "copy-btn" }!!, onElementClick, accent) { Icon(Icons.Default.ContentCopy, null, tint = accent, modifier = Modifier.size(20.dp)) }
                                HelpChip(expanded.find { it.id == "paste-btn" }!!, onElementClick, accent) { Icon(Icons.Default.ContentPaste, null, tint = accent, modifier = Modifier.size(20.dp)) }
                                Text("2", fontSize = 10.sp, color = dim)
                                HelpChip(expanded.find { it.id == "recipients-btn" }!!, onElementClick, dim) { Icon(Icons.Default.People, null, tint = dim, modifier = Modifier.size(20.dp)) }
                                HelpChip(expanded.find { it.id == "settings-btn" }!!, onElementClick, dim) { Icon(Icons.Default.Settings, null, tint = dim, modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                    // Passphrase mode bar mock
                    Box(
                        modifier = Modifier.fillMaxWidth().height(26.dp).background(Color(0xFFEF5350).copy(alpha = 0.15f))
                            .clickable { onElementClick(expanded.find { it.id == "passphrase-bar" }!!) }.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("Enter passphrase", fontSize = 12.sp, color = Color(0xFFEF5350))
                    }
                    // Error bar mock
                    Box(
                        modifier = Modifier.fillMaxWidth().height(26.dp).background(Color(0xFFEF5350).copy(alpha = 0.15f))
                            .clickable { onElementClick(expanded.find { it.id == "error-bar" }!!) }.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Text("Error: Wrong passphrase?", fontSize = 12.sp, color = Color(0xFFEF5350))
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Box(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(compBg).padding(10.dp, 6.dp).heightIn(min = 54.dp)
                                .clickable { onElementClick(expanded.find { it.id == "compose-panel" }!!) },
                        ) {
                            Row {
                                Text("Type message...", style = TextStyle(fontSize = 17.sp, fontFamily = FontFamily.Monospace), color = dim)
                                Spacer(Modifier.weight(1f))
                                Box(Modifier.width(4.dp).height(22.dp).background(Color.White))
                            }
                        }
                    }
                }

                KeyboardMock(keyBg, keyFg, specialBg, kbBg, dim, allKeyboard, onElementClick)
            }
        }
    }
}

@Composable
private fun HelpChip(entry: HelpEntry, onClick: (HelpEntry) -> Unit, @Suppress("UNUSED_PARAMETER") borderColor: Color, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable { onClick(entry) }.padding(4.dp),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun KeyboardMock(
    keyBg: Color, keyFg: Color, specialBg: Color, kbBg: Color, dim: Color,
    entries: List<HelpEntry>, onElementClick: (HelpEntry) -> Unit,
) {
    data class MkKey(val label: String, val hint: String? = null, val entryId: String?)

    val hintFg = keyFg.copy(alpha = 0.45f)

    val alpha = listOf(
        listOf(MkKey("q","1","num-superscript"),MkKey("w","2","num-superscript"),MkKey("e","3","num-superscript"),MkKey("r","4","num-superscript"),MkKey("t","5","num-superscript"),MkKey("y","6","num-superscript"),MkKey("u","7","num-superscript"),MkKey("i","8","num-superscript"),MkKey("o","9","num-superscript"),MkKey("p","0","num-superscript")),
        listOf(MkKey("a",null,"letter-keys"),MkKey("s",null,"letter-keys"),MkKey("d",null,"letter-keys"),MkKey("f",null,"letter-keys"),MkKey("g",null,"letter-keys"),MkKey("h",null,"letter-keys"),MkKey("j",null,"letter-keys"),MkKey("k",null,"letter-keys"),MkKey("l",null,"letter-keys")),
        listOf(MkKey("⇧",null,"shift-key"),MkKey("z",null,"letter-keys"),MkKey("x",null,"letter-keys"),MkKey("c",null,"letter-keys"),MkKey("v",null,"letter-keys"),MkKey("b",null,"letter-keys"),MkKey("n",null,"letter-keys"),MkKey("m",null,"letter-keys"),MkKey("⌫",null,"backspace-key")),
        listOf(MkKey("?123",null,"symbols-key"),MkKey(",",null,"comma-period"),MkKey(" ",null,"space-bar"),MkKey(".",null,"comma-period"),MkKey("↵",null,"enter-key")),
    )

    Box(modifier = Modifier.fillMaxWidth().background(kbBg).padding(horizontal = 3.dp, vertical = 3.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            alpha.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        val isSpecial = key.label.length > 1
                        val w = when (key.label) {
                            "⌫" -> 1.6f; "↵" -> 1.6f; "⇧" -> 1.4f; "?123" -> 1.6f
                            " " -> 4.5f; "," -> 0.7f; "." -> 0.7f
                            else -> 1f
                        }
                        val fs = when (key.label) {
                            "⌫", "↵", "⇧" -> 28.sp; "?123" -> 14.sp
                            else -> if (isSpecial) 14.sp else 16.sp
                        }
                        val bg = if (isSpecial) specialBg else keyBg
                        val entry = key.entryId?.let { id -> entries.find { e -> e.id == id } }
                        val mod = Modifier.weight(w).height(42.dp).clip(RoundedCornerShape(5.dp)).background(bg)
                            .let { m -> if (entry != null) m.clickable { onElementClick(entry) } else m }

                        Box(modifier = mod, contentAlignment = Alignment.Center) {
                            Text(key.label, color = keyFg, fontSize = fs,
                                fontWeight = if (!isSpecial) FontWeight.Medium else FontWeight.Normal, textAlign = TextAlign.Center)
                            if (key.hint != null) {
                                Text(key.hint, color = hintFg, fontSize = 8.sp,
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 0.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
