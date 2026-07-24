package org.cipherkeys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CipherTextPanel(
    text: String,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF1A1A1A) else Color(0xFFE8E8E8)
    val fg = if (dark) Color(0xFFB0B0B0) else Color(0xFF444444)

    if (text.isNotEmpty()) {
        Text(
            text = text,
            style = TextStyle(fontSize = 15.sp, fontFamily = FontFamily.Monospace),
            color = fg,
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(bg)
                .padding(horizontal = 10.dp, vertical = 6.dp)
                .heightIn(min = 40.dp, max = 100.dp),
            maxLines = 5,
        )
    }
}
