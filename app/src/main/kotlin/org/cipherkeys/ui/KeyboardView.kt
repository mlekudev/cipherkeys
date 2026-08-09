package org.cipherkeys.ui

import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class KbKey(
    val label: String,
    val hint: String? = null,
    val longPress: String? = null,
)

private enum class KbMode { ALPHA, SYM, SYM2 }

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
    val popoverBg = if (dark) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
    val popoverFg = if (dark) Color.White else Color.Black

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
    var popoverSerial by remember { mutableStateOf(0) }

    val kill = CipherUiState.state.backspaceKill
    LaunchedEffect(kill) { backspaceRepeat = false }

    LaunchedEffect(backspaceRepeat) {
        if (backspaceRepeat) {
            while (true) { onBackspace(); delay(50) }
        }
    }

    val rows: List<List<KbKey>> = when (mode) {
        KbMode.ALPHA -> if (shift) ALPHA_SHIFT else ALPHA
        KbMode.SYM -> SYMBOLS
        KbMode.SYM2 -> SYMBOLS2
    }

    fun onKey(key: KbKey) {
        when (key.label) {
            "\u232B" -> onBackspace()
            "\u21B5" -> onEnter()
            " " -> onSpace()
            "\u21E7" -> {
                if (shiftLocked) { shiftLocked = false; shift = false }
                else shift = !shift
            }
            "?123" -> {
                if (symLocked) { symLocked = false; mode = KbMode.ALPHA }
                else { mode = KbMode.SYM; shift = false; shiftLocked = false }
            }
            "=\\<" -> mode = if (mode == KbMode.SYM) KbMode.SYM2 else KbMode.SYM
            "ABC" -> { symLocked = false; mode = KbMode.ALPHA; shift = false; shiftLocked = false }
            else -> {
                onChar(key.label)
                if (shift && !shiftLocked && mode == KbMode.ALPHA) shift = false
                if (!symLocked && CipherPrefs.symAutoReturn && mode != KbMode.ALPHA) mode = KbMode.ALPHA
            }
        }
    }

    fun onLongPress(key: KbKey) {
        if (key.label == "\u21E7" && CipherPrefs.shiftLockMethod == "long-press") {
            shiftLocked = true; shift = true; return
        }
        if (key.label == "?123" && CipherPrefs.symLockMethod == "long-press") {
            symLocked = true; mode = KbMode.SYM; return
        }
        key.longPress?.let { onChar(it) }
    }

    fun onDoubleTap(key: KbKey) {
        if (key.label == "\u21E7" && CipherPrefs.shiftLockMethod == "double-tap") {
            shiftLocked = true; shift = true
        }
        if (key.label == "?123" && CipherPrefs.symLockMethod == "double-tap") {
            symLocked = true; mode = KbMode.SYM
        }
    }

    val shiftDoubleTap = CipherPrefs.shiftLockMethod == "double-tap"
    val symDoubleTap = CipherPrefs.symLockMethod == "double-tap"
    val shiftNeedDoubleTap = shiftDoubleTap || CipherPrefs.shiftLockMethod == "off" && shiftLocked
    val symNeedDoubleTap = symDoubleTap || CipherPrefs.symLockMethod == "off" && symLocked

    Column(
        modifier = modifier.fillMaxWidth().background(kbBg).padding(horizontal = 3.dp, vertical = 3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                row.forEach { key ->
                    val isSpecial = key.label.length > 1
                    val w = when (key.label) {
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
                    val useDoubleTap =
                        (key.label == "\u21E7" && shiftNeedDoubleTap) ||
                            (key.label == "?123" && symNeedDoubleTap)
                    KeyboardKey(
                        displayKey, w, bg, keyFg, hintFg, fs,
                        active = (shift && key.label == "\u21E7") || (mode != KbMode.ALPHA && (key.label == "?123" || key.label == "ABC")) || (mode == KbMode.SYM2 && key.label == "=\\<"),
                        popoverBg = popoverBg,
                        popoverFg = popoverFg,
                        popoverSerial = popoverSerial,
                        onTap = {
                            backspaceRepeat = false
                            doHaptic()
                            playClick()
                            onKey(key)
                        },
                        onDoubleTap = if (useDoubleTap) {
                            { onDoubleTap(key) }
                        } else null,
                        onLongPress = if (useDoubleTap) null else {
                            {
                                if (key.label == "\u232B") {
                                    backspaceRepeat = true
                                } else if (key.label == "\u21B5") {
                                    onEnterLongPress()
                                } else {
                                    onLongPress(key)
                                }
                            }
                        },
                        onRelease = {
                            backspaceRepeat = false
                        },
                        onTapWithPopover = { tap, serial ->
                            popoverSerial = serial
                            tap()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.KeyboardKey(
    key: KbKey, weight: Float, bg: Color, fg: Color, hintFg: Color, fs: androidx.compose.ui.unit.TextUnit,
    active: Boolean = false,
    popoverBg: Color = Color.Transparent,
    popoverFg: Color = Color.White,
    popoverSerial: Int = 0,
    onTap: () -> Unit,
    onDoubleTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onRelease: () -> Unit = {},
    onTapWithPopover: (() -> Unit, Int) -> Unit = { tap, _ -> tap() },
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var pressed by remember { mutableStateOf(false) }
    val highlighted = active || pressed
    val kh = CipherPrefs.keyHeightDp.dp
    val isBackspace = key.label == "\u232B"

    var popoverShown by remember { mutableStateOf(false) }
    var mySerial by remember { mutableStateOf(0) }

    LaunchedEffect(popoverSerial) {
        if (mySerial > 0 && mySerial != popoverSerial) {
            popoverShown = false
        }
    }

    LaunchedEffect(popoverShown) {
        if (popoverShown) {
            delay(300)
            popoverShown = false
        }
    }

    val popoverEnabled = CipherPrefs.popupEnabled && key.label.length == 1
    val spaceUnderline = !CipherPrefs.keyBgShading && key.label == " "

    Box(
        modifier = Modifier.weight(weight).height(kh)
            .clip(RoundedCornerShape(5.dp))
            .background(if (highlighted) fg else bg)
            .pointerInput(key) {
                if (isBackspace) {
                    coroutineScope {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                val longPressTimeout = viewConfiguration.longPressTimeoutMillis
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
                } else if (onDoubleTap != null) {
                    detectTapGestures(
                        onTap = {
                            if (!active) { scope.launch { pressed = true; delay(80); pressed = false } }
                            onTap()
                        },
                        onDoubleTap = {
                            if (CipherPrefs.hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            pressed = true
                            onDoubleTap()
                            scope.launch { delay(100); pressed = false }
                        },
                    )
                } else {
                    detectTapGestures(
                        onTap = {
                            if (!active) { scope.launch { pressed = true; delay(80); pressed = false } }
                            onTapWithPopover(
                                {
                                    if (popoverEnabled) {
                                        mySerial = popoverSerial + 1
                                        popoverShown = true
                                    }
                                    onTap()
                                },
                                if (popoverEnabled) popoverSerial + 1 else popoverSerial,
                            )
                        },
                        onLongPress = onLongPress?.let { lp ->
                            {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                pressed = true
                                lp()
                                scope.launch { delay(100); pressed = false }
                            }
                        },
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (spaceUnderline) "_" else if (pressed && key.longPress != null) key.longPress!! else key.label,
            color = if (highlighted) bg else fg,
            fontSize = if (spaceUnderline) 20.sp else fs,
            fontWeight = if (key.label.length == 1) FontWeight.Medium else FontWeight.Normal,
            textAlign = TextAlign.Center,
        )
        if (key.hint != null) {
            Text(
                text = key.hint,
                color = if (highlighted) bg.copy(alpha = 0.45f) else hintFg,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopEnd).offset(x = (-3).dp, y = 2.dp),
                textAlign = TextAlign.End,
            )
        }
        if (popoverShown) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = -(kh + 8.dp))
                    .zIndex(100f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(popoverBg)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
            ) {
                Text(key.label, color = popoverFg, fontSize = 44.sp, fontWeight = FontWeight.Bold)
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
