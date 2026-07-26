package org.cipherkeys.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val HORIZ_PAD = 10.dp
private val VERT_PAD = 6.dp

@Composable
fun CipherComposePanel(
    text: String,
    cursorPos: Int,
    onCursorMoved: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type message to encrypt...",
    isPassword: Boolean = false,
    active: Boolean = true,
    revealLastChar: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val bg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val fg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val placeholderFg = if (dark) Color(0xFF707070) else Color(0xFF909090)
    val cursorC = if (dark) Color.White else Color.Black

    val display = when {
        !isPassword -> text
        revealLastChar && text.isNotEmpty() -> "\u2022".repeat(text.length - 1) + text.last()
        else -> "\u2022".repeat(text.length)
    }
    val clamped = cursorPos.coerceIn(0, display.length)
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val density = LocalDensity.current
    val padX = with(density) { HORIZ_PAD.toPx() }
    val padY = with(density) { VERT_PAD.toPx() }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var cursorVisible by remember { mutableStateOf(true) }
    var lastCursorPos by remember { mutableStateOf(cursorPos) }
    if (cursorPos != lastCursorPos) { cursorVisible = true; lastCursorPos = cursorPos }
    LaunchedEffect(Unit) {
        while (true) { delay(530); cursorVisible = !cursorVisible }
    }

    val safeLayout = layoutResult?.takeIf { it.layoutInput.text.length == display.length }

    val maxH = (CipherPrefs.panelLines * 30).dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .heightIn(min = 60.dp, max = maxH)
            .pointerInput(display) {
                detectTapGestures { offset ->
                    val lr = safeLayout ?: return@detectTapGestures
                    val adj = Offset(
                        offset.x - padX,
                        offset.y - padY + scrollState.value.toFloat()
                    )
                    onCursorMoved(lr.getOffsetForPosition(adj))
                }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .verticalScroll(scrollState)
                .padding(horizontal = HORIZ_PAD, vertical = VERT_PAD),
        ) {
            if (display.isEmpty()) {
                Text(placeholder,
                    style = TextStyle(fontSize = 17.sp, fontFamily = FontFamily.Monospace),
                    color = placeholderFg,
                    onTextLayout = { layoutResult = it },
                )
                if (active && cursorVisible) {
                    Box(modifier = Modifier.width(4.dp).height(22.dp).background(cursorC))
                }
            } else {
                Text(
                    text = display,
                    style = TextStyle(fontSize = 17.sp, fontFamily = FontFamily.Monospace, color = fg),
                    onTextLayout = { lr ->
                        layoutResult = lr
                        val cursorBottom = lr.getCursorRect(clamped).bottom.toInt()
                        val panelHeightPx = with(density) { maxH.roundToPx() }
                        val target = cursorBottom - panelHeightPx + 40
                        if (target > 0 && target > scrollState.value) {
                            scope.launch { scrollState.animateScrollTo(target) }
                        }
                    },
                )
                if (active && cursorVisible && safeLayout != null) {
                    val r = safeLayout.getCursorRect(clamped)
                    Box(
                        modifier = Modifier
                            .offset { IntOffset(r.left.toInt(), r.top.toInt()) }
                            .height(with(density) { r.height.toDp() })
                            .width(4.dp)
                            .background(cursorC),
                    )
                }
            }
        }
    }
}
