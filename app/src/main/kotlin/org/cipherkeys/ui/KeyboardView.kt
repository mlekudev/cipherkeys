package org.cipherkeys.ui

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class KbKey(
    val label: String,
    val hint: String? = null,
    val longPress: String? = null,
)

private enum class KbMode { ALPHA, SYM, SYM2, NUM }

@Composable
fun KeyboardView(
    onChar: (String) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onSpace: () -> Unit,
    onEnterLongPress: () -> Unit = {},
    showLockHint: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val rawKeyBg = if (dark) Color(0xFF4A4A4A) else Color(0xFFD6D6D6)
    val keyFg = if (dark) Color(0xFFE0E0E0) else Color(0xFF1A1A1A)
    val rawSpecialBg = if (dark) Color(0xFF383838) else Color(0xFFC0C0C0)
    val kbBg = if (dark) Color(0xFF222222) else Color(0xFFF0F0F0)
    val hintFg = keyFg.copy(alpha = 0.45f)
    val keyBg = if (CipherPrefs.keyBgShading) rawKeyBg else kbBg
    val specialBg = if (CipherPrefs.keyBgShading) rawSpecialBg else kbBg

    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val clickSoundId = remember {
        try {
            val sampleRate = 44100
            val durationMs = 12
            val numSamples = sampleRate * durationMs / 1000
            val buffer = ShortArray(numSamples)
            val rng = java.util.Random()
            for (i in buffer.indices) {
                val env = 1f - i.toFloat() / numSamples
                buffer[i] = (rng.nextGaussian() * 16384 * env * env)
                    .toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
            val rawSize = numSamples * 2
            val wavSize = 44 + rawSize
            val wav = java.io.ByteArrayOutputStream()
            fun le32(v: Int) { wav.write(v and 0xFF); wav.write((v shr 8) and 0xFF); wav.write((v shr 16) and 0xFF); wav.write((v shr 24) and 0xFF) }
            fun le16(v: Int) { wav.write(v and 0xFF); wav.write((v shr 8) and 0xFF) }
            wav.write("RIFF".toByteArray())
            le32(wavSize - 8)
            wav.write("WAVE".toByteArray())
            wav.write("fmt ".toByteArray())
            le32(16); le16(1); le16(1); le32(sampleRate)
            le32(sampleRate * 2); le16(2); le16(16)
            wav.write("data".toByteArray())
            le32(rawSize)
            for (s in buffer) { le16(s.toInt() and 0xFFFF) }
            val file = File(context.cacheDir, "click.wav")
            file.writeBytes(wav.toByteArray())
            val pool = SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .build()
            val id = pool.load(file.absolutePath, 1)
            Pair(pool, id)
        } catch (_: Exception) { null }
    }

    DisposableEffect(Unit) {
        onDispose { clickSoundId?.first?.release() }
    }

    fun playClick() {
        if (!CipherPrefs.soundEnabled) return
        val (pool, id) = clickSoundId ?: return
        val vol = CipherPrefs.soundVolume
        pool.play(id, vol, vol, 1, 0, 1f)
    }

    fun doHaptic() {
        if (CipherPrefs.hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    var mode by remember { mutableStateOf(KbMode.ALPHA) }
    var shift by remember { mutableStateOf(false) }
    var shiftLocked by remember { mutableStateOf(false) }
    var symLocked by remember { mutableStateOf(false) }
    var backspaceRepeat by remember { mutableStateOf(false) }
    var popoverSerial by remember { mutableStateOf(0L) }
    var shiftLastTap by remember { mutableStateOf(0L) }
    var symLastTap by remember { mutableStateOf(0L) }

    val kill = CipherUiState.state.backspaceKill
    LaunchedEffect(kill) { backspaceRepeat = false }

    val shiftReset = CipherUiState.state.shiftResetSerial
    LaunchedEffect(shiftReset) { shift = false; shiftLocked = false; CipherUiState.setKeyboardShift(false) }

    val autoShift = CipherUiState.state.autoShiftSerial
    LaunchedEffect(autoShift) { if (!shiftLocked) { shift = true; CipherUiState.setKeyboardShift(true) } }

    val singleShiftClear = CipherUiState.state.singleShiftClearSerial
    LaunchedEffect(singleShiftClear) { if (!shiftLocked) { shift = false; CipherUiState.setKeyboardShift(false) } }

    var numTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(numTrigger) {
        if (numTrigger > 0) {
            mode = KbMode.NUM; shift = false; shiftLocked = false
            CipherUiState.setKeyboardShift(false)
            numTrigger = 0
        }
    }

    val kbReset = CipherUiState.state.kbResetSerial
    LaunchedEffect(kbReset) {
        mode = KbMode.ALPHA
        shiftLocked = false
        symLocked = false
        shift = CipherUiState.state.initialShiftOn
        CipherUiState.setKeyboardShift(shift)
    }

    LaunchedEffect(backspaceRepeat) {
        if (backspaceRepeat) {
            while (true) { onBackspace(); delay(50) }
        }
    }

    fun bumpPopoverSerial(): Long {
        popoverSerial++
        return popoverSerial
    }

    val rows: List<List<KbKey>> = when (mode) {
        KbMode.ALPHA -> if (shift) ALPHA_SHIFT else ALPHA
        KbMode.SYM -> SYMBOLS
        KbMode.SYM2 -> SYMBOLS2
        KbMode.NUM -> NUMERIC
    }

    fun onKey(key: KbKey) {
        when (key.label) {
            "\u232B" -> onBackspace()
            "\u21B5" -> onEnter()
            " " -> onSpace()
            "\u21E7" -> {
                if (shiftLocked) {
                    shiftLocked = false; shift = false
                    shiftLastTap = 0
                } else {
                    val now = System.currentTimeMillis()
                    val dt = now - shiftLastTap
                    shiftLastTap = now
                    if (CipherPrefs.shiftLockMethod == "double-tap" && dt < 300) {
                        shiftLocked = true; shift = true
                    } else {
                        shift = !shift
                    }
                }
                CipherUiState.setKeyboardShift(shift)
            }
            "?123" -> {
                if (symLocked) {
                    symLocked = false; mode = KbMode.ALPHA
                    symLastTap = 0
                } else {
                    symLastTap = System.currentTimeMillis()
                    mode = KbMode.SYM; shift = false; shiftLocked = false
                }
                CipherUiState.setKeyboardShift(false)
            }
            "=\\<" -> mode = if (mode == KbMode.SYM) KbMode.SYM2 else KbMode.SYM
            "ABC" -> {
                if (symLocked) {
                    symLocked = false; mode = KbMode.ALPHA
                    symLastTap = 0
                } else {
                    val dt = System.currentTimeMillis() - symLastTap
                    if (CipherPrefs.symLockMethod == "double-tap" && dt < 300) {
                        symLocked = true; mode = KbMode.SYM
                    } else {
                        mode = KbMode.ALPHA; shift = false; shiftLocked = false
                    }
                    symLastTap = 0
                }
                CipherUiState.setKeyboardShift(false)
            }
            else -> {
                onChar(key.label)
                if (shift && !shiftLocked && mode == KbMode.ALPHA) { shift = false; CipherUiState.setKeyboardShift(false) }
                if (!symLocked && CipherPrefs.symAutoReturn && (mode == KbMode.SYM || mode == KbMode.SYM2)) mode = KbMode.ALPHA
            }
        }
    }

    fun onLongPress(key: KbKey) {
        if (mode == KbMode.NUM && key.label == "0") {
            mode = KbMode.ALPHA; shift = false; shiftLocked = false
            CipherUiState.setKeyboardShift(false)
            return
        }
        if (key.label == "?123") {
            numTrigger++
            return
        }
        if (key.label == "\u21E7" && CipherPrefs.shiftLockMethod == "long-press") {
            shiftLocked = true; shift = true; CipherUiState.setKeyboardShift(true); return
        }
        key.longPress?.let { onChar(it) }
    }

    val shiftDoubleTap = CipherPrefs.shiftLockMethod == "double-tap"
    val symDoubleTap = CipherPrefs.symLockMethod == "double-tap"

    Column(
        modifier = modifier.fillMaxWidth().background(kbBg).padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        key(mode) {
            rows.forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    row.forEach { key ->
                        val isSpecial = key.label.length > 1
                        val w = if (mode == KbMode.NUM) {
                            if (key.label == "\u21B5") 2f else 1f
                        } else when (key.label) {
                            "\u232B" -> 1.6f; "\u21B5" -> 1.6f
                            "\u21E7" -> 1.4f; "?123" -> 1.6f; "ABC" -> 1.6f; "=\\<" -> 1.6f
                            " " -> if (mode == KbMode.ALPHA) 4.5f else 3.0f
                            "," -> 0.7f; "." -> 0.7f; "_" -> 0.8f; "/" -> 0.8f
                            "<" -> 0.7f; ">" -> 0.7f
                            else -> 1f
                        }
                        val bg = if (isSpecial) specialBg else keyBg
                        val fs = when (key.label) {
                            "\u232B", "\u21B5", "\u21E7" -> 28.sp
                            "?123", "ABC", "=\\<" -> 14.sp
                            else -> if (isSpecial) 14.sp else 20.sp
                        }
                    val displayKey = if (showLockHint && key.label == "\u21B5")
                        key.copy(hint = "\uD83D\uDD12") else key
                    KeyboardKey(
                        displayKey, w, bg, keyFg, hintFg, fs,
                        active = (shift && key.label == "\u21E7") || (mode != KbMode.ALPHA && (key.label == "?123" || key.label == "ABC")) || (mode == KbMode.SYM2 && key.label == "=\\<"),
                        numMode = (mode == KbMode.NUM),
                        popoverSerial = popoverSerial,
                        bumpPopoverSerial = { bumpPopoverSerial() },
                        onTap = {
                            backspaceRepeat = false
                            doHaptic()
                            playClick()
                            onKey(key)
                        },
                        onLongPress = {
                            if (key.label == "\u232B") {
                                backspaceRepeat = true
                            } else if (key.label == "\u21B5") {
                                onEnterLongPress()
                            } else {
                                onLongPress(key)
                            }
                        },
                        onRelease = {
                            backspaceRepeat = false
                        },
                    )
                }
            }
        }
        } // key(mode)
    }
}

@Composable
private fun RowScope.KeyboardKey(
    key: KbKey, weight: Float, bg: Color, fg: Color, hintFg: Color, fs: androidx.compose.ui.unit.TextUnit,
    active: Boolean = false,
    numMode: Boolean = false,
    popoverSerial: Long = 0L,
    bumpPopoverSerial: () -> Long = { 0L },
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onRelease: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val highlighted = active || pressed
    val kh = CipherPrefs.keyHeightDp.dp
    val isBackspace = key.label == "\u232B"
    var posInRoot by remember { mutableStateOf(IntOffset.Zero) }
    var keyWidthPx by remember { mutableIntStateOf(0) }
    var hasPosition by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    var popoverShown by remember { mutableStateOf(false) }
    var popoverLabel by remember { mutableStateOf("") }
    var mySerial by remember { mutableStateOf(0L) }
    val popoverEnabled = CipherPrefs.popupEnabled && key.label.length == 1 && key.label[0] != ' '

    fun showPopover(serial: Long, label: String) {
        mySerial = serial
        popoverLabel = label
        popoverShown = true
    }

    LaunchedEffect(popoverSerial) {
        if (mySerial > 0L && mySerial != popoverSerial) {
            popoverShown = false
        }
    }

    LaunchedEffect(popoverShown) {
        if (popoverShown) {
            delay(300)
            popoverShown = false
        }
    }

    val dark = isSystemInDarkTheme()
    val popoverBg = if (dark) Color(0xFF333333) else Color(0xFFCCCCCC)
    val popoverFg = if (dark) Color.White else Color.Black

    val keyContent = @Composable {
        Box(
            modifier = Modifier.fillMaxWidth().height(kh)
                .onGloballyPositioned { posInRoot = it.positionInRoot().round(); keyWidthPx = it.size.width; hasPosition = true }
                .background(if (highlighted) fg else bg, RoundedCornerShape(5.dp))
                .pointerInput(key) {
                    if (isBackspace) {
                        coroutineScope {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val longPressTimeout = CipherPrefs.longPressMs
                                    var longPressTriggered = false
                                    val longPressJob = launch {
                                        delay(longPressTimeout.toLong())
                                        longPressTriggered = true
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        pressed = true
                                        onLongPress?.invoke()
                                    }
                                    var released = false
                                    while (!released) {
                                        val event = awaitPointerEvent()
                                        if (event.changes.all { it.isConsumed || !it.pressed }) released = true
                                    }
                                    longPressJob.cancel()
                                    pressed = false
                                    onRelease()
                                    if (!longPressTriggered) {
                                        if (!active) { scope.launch { pressed = true; delay(80); pressed = false } }
                                        onTap()
                                    }
                                }
                            }
                        }
                    } else {
                        coroutineScope {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    var longPressTriggered = false
                                    val longPressJob = onLongPress?.let { lp ->
                                        val hasLongPressAction = key.longPress != null ||
                                            key.label == "\u21E7" ||
                                            key.label == "?123" ||
                                            key.label == "\u21B5" ||
                                            (numMode && key.label == "0")
                                        launch {
                                            delay(CipherPrefs.longPressMs.toLong())
                                            longPressTriggered = true
                                            if (hasLongPressAction) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            pressed = true
                                            val longLabel = key.longPress ?: key.label
                                            if (CipherPrefs.popupEnabled) showPopover(bumpPopoverSerial(), longLabel)
                                            lp()
                                            scope.launch { delay(100); pressed = false }
                                        }
                                    }
                                    val up = waitForUpOrCancellation()
                                    longPressJob?.cancel()
                                    if (up != null && !longPressTriggered) {
                                        val isTap = (up.position - down.position).getDistance() <= viewConfiguration.touchSlop
                                        if (isTap) {
                                            if (!active) { scope.launch { pressed = true; delay(80); pressed = false } }
                                            if (popoverEnabled) showPopover(bumpPopoverSerial(), key.label)
                                            onTap()
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (key.label == " ") {
                Text(
                    text = "\u2423",
                    color = fg.copy(alpha = 0.5f),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            } else {
                Text(
                    text = if (pressed && key.longPress != null) key.longPress!! else key.label,
                    color = if (highlighted) bg else fg,
                    fontSize = fs,
                    fontWeight = if (key.label.length == 1) FontWeight.Medium else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
            if (key.hint != null) {
                Text(
                    text = key.hint,
                    color = if (highlighted) bg.copy(alpha = 0.45f) else hintFg,
                    fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 2.dp),
                    textAlign = TextAlign.End,
                )
            }
        }
    }

    Box(modifier = Modifier.weight(weight)) {
        keyContent()
        if (popoverShown && hasPosition) {
            val offsetPx = with(density) { 4.dp.roundToPx() }
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect, windowSize: IntSize,
                        layoutDirection: LayoutDirection, popupContentSize: IntSize,
                    ): IntOffset {
                        val keyCenterX = posInRoot.x + keyWidthPx / 2
                        val x = (keyCenterX - popupContentSize.width / 2).coerceIn(0, windowSize.width - popupContentSize.width)
                        val y = posInRoot.y - popupContentSize.height - offsetPx
                        return IntOffset(x, y.coerceAtLeast(0))
                    }
                },
                properties = PopupProperties(focusable = false),
            ) {
                Box(
                    modifier = Modifier
                        .background(popoverBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(popoverLabel, color = popoverFg, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private val ALPHA = listOf(
    listOf(KbKey("q","\u00B9","1"),KbKey("w","\u00B2","2"),KbKey("e","\u00B3","3"),KbKey("r","\u2074","4"),KbKey("t","\u2075","5"),KbKey("y","\u2076","6"),KbKey("u","\u2077","7"),KbKey("i","\u2078","8"),KbKey("o","\u2079","9"),KbKey("p","\u2070","0")),
    listOf(KbKey("a"),KbKey("s"),KbKey("d"),KbKey("f"),KbKey("g"),KbKey("h"),KbKey("j"),KbKey("k"),KbKey("l")),
    listOf(KbKey("\u21E7"),KbKey("z"),KbKey("x"),KbKey("c"),KbKey("v"),KbKey("b"),KbKey("n"),KbKey("m"),KbKey("\u232B")),
    listOf(KbKey("?123"),KbKey(","),KbKey(" "),KbKey("."),KbKey("\u21B5")),
)

private val ALPHA_SHIFT = listOf(
    listOf(KbKey("Q","\u00B9","1"),KbKey("W","\u00B2","2"),KbKey("E","\u00B3","3"),KbKey("R","\u2074","4"),KbKey("T","\u2075","5"),KbKey("Y","\u2076","6"),KbKey("U","\u2077","7"),KbKey("I","\u2078","8"),KbKey("O","\u2079","9"),KbKey("P","\u2070","0")),
    listOf(KbKey("A"),KbKey("S"),KbKey("D"),KbKey("F"),KbKey("G"),KbKey("H"),KbKey("J"),KbKey("K"),KbKey("L")),
    listOf(KbKey("\u21E7"),KbKey("Z"),KbKey("X"),KbKey("C"),KbKey("V"),KbKey("B"),KbKey("N"),KbKey("M"),KbKey("\u232B")),
    listOf(KbKey("?123"),KbKey(","),KbKey(" "),KbKey("."),KbKey("\u21B5")),
)

private val SYMBOLS = listOf(
    listOf(KbKey("1"),KbKey("2"),KbKey("3"),KbKey("4"),KbKey("5"),KbKey("6"),KbKey("7"),KbKey("8"),KbKey("9"),KbKey("0")),
    listOf(KbKey("@"),KbKey("#"),KbKey("\$"),KbKey("%"),KbKey("&"),KbKey("-"),KbKey("+"),KbKey("("),KbKey(")")),
    listOf(KbKey("=\\<"),KbKey("*"),KbKey("\""),KbKey("'"),KbKey(":"),KbKey(";"),KbKey("!"),KbKey("?"),KbKey("\u232B")),
    listOf(KbKey("ABC"),KbKey(","),KbKey("_"),KbKey(" "),KbKey("/"),KbKey("."),KbKey("\u21B5")),
)

private val SYMBOLS2 = listOf(
    listOf(KbKey("~"),KbKey("`"),KbKey("|"),KbKey("\u221A"),KbKey("\u03C0"),KbKey("\u00F7"),KbKey("\u00D7"),KbKey("\u00B6"),KbKey("\u2206")),
    listOf(KbKey("\u00A3"),KbKey("\u00A2"),KbKey("\u20AC"),KbKey("\u00A5"),KbKey("^"),KbKey("\u00B0"),KbKey("="),KbKey("{"),KbKey("}")),
    listOf(KbKey("=\\<"),KbKey("\\"),KbKey("\u00A9"),KbKey("\u00AE"),KbKey("\u2122"),KbKey("\u2105"),KbKey("["),KbKey("]"),KbKey("\u232B")),
    listOf(KbKey("ABC"),KbKey(","),KbKey("<"),KbKey(" "),KbKey(">"),KbKey("."),KbKey("\u21B5")),
)

private val NUMERIC = listOf(
    listOf(KbKey("7"),KbKey("8"),KbKey("9"),KbKey("("),KbKey(")")),
    listOf(KbKey("4"),KbKey("5"),KbKey("6"),KbKey("+"),KbKey("-")),
    listOf(KbKey("1"),KbKey("2"),KbKey("3"),KbKey("*"),KbKey("/")),
    listOf(KbKey("0", hint = "abc"),KbKey("."),KbKey(" "),KbKey("\u21B5")),
)
