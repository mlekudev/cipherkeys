package org.cipherkeys.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

object CipherPrefs {
    private const val PREFS = "cipherkeys_config"
    private const val KEY_HEIGHT = "key_height"
    private const val PANEL_LINES = "panel_lines"
    private const val DEFAULT_KEY_HEIGHT = 42
    private const val DEFAULT_PANEL_LINES = 4

    private var prefs: SharedPreferences? = null

    var keyHeightDp by mutableIntStateOf(DEFAULT_KEY_HEIGHT)
        private set

    var panelLines by mutableIntStateOf(DEFAULT_PANEL_LINES)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        keyHeightDp = prefs!!.getInt(KEY_HEIGHT, DEFAULT_KEY_HEIGHT)
        panelLines = prefs!!.getInt(PANEL_LINES, DEFAULT_PANEL_LINES)
    }

    fun updateKeyHeight(value: Int) {
        val v = value.coerceIn(34, 60)
        keyHeightDp = v
        prefs?.edit()?.putInt(KEY_HEIGHT, v)?.apply()
    }

    fun updatePanelLines(value: Int) {
        val v = value.coerceIn(2, 8)
        panelLines = v
        prefs?.edit()?.putInt(PANEL_LINES, v)?.apply()
    }
}
