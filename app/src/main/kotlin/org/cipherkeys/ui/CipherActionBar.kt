package org.cipherkeys.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CipherActionBar(
    isActive: Boolean,
    hasComposeText: Boolean,
    hasDecryptedText: Boolean,
    isLoading: Boolean,
    onEncrypt: () -> Unit,
    onDecrypt: () -> Unit,
    onSend: () -> Unit,
    onCopy: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = !isLoading

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onEncrypt,
            enabled = isActive && hasComposeText && enabled,
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Encrypt",
                tint = if (isActive && hasComposeText && enabled)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(
            onClick = onDecrypt,
            enabled = isActive && hasComposeText && enabled,
        ) {
            Icon(
                imageVector = Icons.Default.LockOpen,
                contentDescription = "Decrypt",
                tint = if (isActive && hasComposeText && enabled)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(
            onClick = onSend,
            enabled = isActive && hasComposeText && enabled,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = if (isActive && hasComposeText && enabled)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp),
            )
        }
        IconButton(
            onClick = onCopy,
            enabled = isActive && (hasComposeText || hasDecryptedText) && enabled,
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy",
                tint = if (isActive && (hasComposeText || hasDecryptedText) && enabled)
                    MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(22.dp),
            )
        }
    }
}
