package org.cipherkeys.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.cipherkeys.ui.CipherPrefs

@Composable
fun SettingsKeyboardScreen() {
    var haptic by remember { mutableStateOf(CipherPrefs.hapticEnabled) }
    var sound by remember { mutableStateOf(CipherPrefs.soundEnabled) }
    var volume by remember { mutableFloatStateOf(CipherPrefs.soundVolume) }
    var keyH by remember { mutableIntStateOf(CipherPrefs.keyHeightDp) }
    var popup by remember { mutableStateOf(CipherPrefs.popupEnabled) }
    var keyBg by remember { mutableStateOf(CipherPrefs.keyBgShading) }
    var shiftLock by remember { mutableStateOf(CipherPrefs.shiftLockMethod) }
    var symLock by remember { mutableStateOf(CipherPrefs.symLockMethod) }
    var symReturn by remember { mutableStateOf(CipherPrefs.symAutoReturn) }
    var autoCap by remember { mutableStateOf(CipherPrefs.autoCapitalize) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
    ) {
        SettingToggle("Haptic feedback", "Vibrate on key press", haptic,
            { haptic = it; CipherPrefs.updateHapticEnabled(it) })

        SettingToggle("Sound feedback", "Click sound on key press", sound,
            { sound = it; CipherPrefs.updateSoundEnabled(it) })

        if (sound) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Sound volume: ${(volume * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Slider(
                        value = volume,
                        onValueChange = { volume = it; CipherPrefs.updateSoundVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SettingToggle("Key press popover", "Brief preview above pressed key", popup,
            { popup = it; CipherPrefs.updatePopupEnabled(it) })

        SettingToggle("Key background shading", "Show shaded key backgrounds", keyBg,
            { keyBg = it; CipherPrefs.updateKeyBgShading(it) })

        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Key height: ${keyH}dp", style = MaterialTheme.typography.titleMedium)
                Text("Adjust keyboard key size", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = keyH.toFloat(),
                    onValueChange = { keyH = it.toInt(); CipherPrefs.updateKeyHeight(it.toInt()) },
                    valueRange = 34f..60f,
                    steps = 25,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        LockMethodSelector("Shift lock method", shiftLock,
            { shiftLock = it; CipherPrefs.updateShiftLockMethod(it) })

        LockMethodSelector("Symbol lock method", symLock,
            { symLock = it; CipherPrefs.updateSymLockMethod(it) })

        SettingToggle("Symbol auto-return", "Switch back to letters after typing a symbol", symReturn,
            { symReturn = it; CipherPrefs.updateSymAutoReturn(it) })

        SettingToggle("Auto-capitalize", "Capitalize first letter after period", autoCap,
            { autoCap = it; CipherPrefs.updateAutoCapitalize(it) })
    }
}

@Composable
private fun SettingToggle(
    title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun LockMethodSelector(title: String, selected: String, onSelect: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            LockRadio("Double-tap", "double-tap", selected, onSelect)
            LockRadio("Long-press", "long-press", selected, onSelect)
            LockRadio("Off", "off", selected, onSelect)
        }
    }
}

@Composable
private fun LockRadio(label: String, value: String, selected: String, onSelect: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        RadioButton(
            selected = selected == value,
            onClick = { onSelect(value) },
        )
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 8.dp))
    }
}
