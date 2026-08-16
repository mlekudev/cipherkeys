package org.cipherkeys.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object CipherPrefs {
    private const val PREFS = "cipherkeys_config"

    private const val KEY_HEIGHT = "key_height"
    private const val PANEL_LINES = "panel_lines"
    private const val HAPTIC_ENABLED = "haptic_enabled"
    private const val SOUND_ENABLED = "sound_enabled"
    private const val SOUND_VOLUME = "sound_volume"
    private const val POPUP_ENABLED = "popup_enabled"
    private const val KEY_BG_SHADING = "key_bg_shading"
    private const val SHIFT_LOCK_METHOD = "shift_lock_method"
    private const val SYM_LOCK_METHOD = "sym_lock_method"
    private const val SYM_AUTO_RETURN = "sym_auto_return"
    private const val AUTO_CAPITALIZE = "auto_capitalize"
    private const val LONG_PRESS_MS = "long_press_ms"
    private const val DEV_MODE = "dev_mode"
    private const val ENCRYPT_TO_SELF = "encrypt_to_self"
    private const val DEFAULT_KEY = "default_key"

    private const val DEFAULT_KEY_HEIGHT = 42
    private const val DEFAULT_PANEL_LINES = 4
    private const val DEFAULT_LONG_PRESS_MS = 400

    private var prefs: SharedPreferences? = null

    var keyHeightDp by mutableIntStateOf(DEFAULT_KEY_HEIGHT)
        private set

    var panelLines by mutableIntStateOf(DEFAULT_PANEL_LINES)
        private set

    var hapticEnabled by mutableStateOf(true)
        private set

    var soundEnabled by mutableStateOf(false)
        private set

    var soundVolume by mutableFloatStateOf(0.2f)
        private set

    var popupEnabled by mutableStateOf(true)
        private set

    var keyBgShading by mutableStateOf(true)
        private set

    var shiftLockMethod by mutableStateOf("double-tap")
        private set

    var symLockMethod by mutableStateOf("double-tap")
        private set

    var symAutoReturn by mutableStateOf(false)
        private set

    var autoCapitalize by mutableStateOf(true)
        private set

    var longPressMs by mutableIntStateOf(DEFAULT_LONG_PRESS_MS)
        private set

    var devMode by mutableStateOf(false)
        private set

    var encryptToSelf by mutableStateOf(false)
        private set

    var defaultKeyId by mutableStateOf<Long?>(null)
        private set

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        keyHeightDp = prefs!!.getInt(KEY_HEIGHT, DEFAULT_KEY_HEIGHT)
        panelLines = prefs!!.getInt(PANEL_LINES, DEFAULT_PANEL_LINES)
        hapticEnabled = prefs!!.getBoolean(HAPTIC_ENABLED, true)
        soundEnabled = prefs!!.getBoolean(SOUND_ENABLED, false)
        soundVolume = prefs!!.getFloat(SOUND_VOLUME, 0.2f)
        popupEnabled = prefs!!.getBoolean(POPUP_ENABLED, true)
        keyBgShading = prefs!!.getBoolean(KEY_BG_SHADING, true)
        shiftLockMethod = prefs!!.getString(SHIFT_LOCK_METHOD, "double-tap") ?: "double-tap"
        symLockMethod = prefs!!.getString(SYM_LOCK_METHOD, "double-tap") ?: "double-tap"
        symAutoReturn = prefs!!.getBoolean(SYM_AUTO_RETURN, false)
        autoCapitalize = prefs!!.getBoolean(AUTO_CAPITALIZE, true)
        longPressMs = prefs!!.getInt(LONG_PRESS_MS, DEFAULT_LONG_PRESS_MS)
        devMode = prefs!!.getBoolean(DEV_MODE, false)
        encryptToSelf = prefs!!.getBoolean(ENCRYPT_TO_SELF, false)
        val defaultId = prefs!!.getLong(DEFAULT_KEY, -1L)
        defaultKeyId = if (defaultId >= 0) defaultId else null
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

    fun updateHapticEnabled(v: Boolean) {
        hapticEnabled = v
        prefs?.edit()?.putBoolean(HAPTIC_ENABLED, v)?.apply()
    }

    fun updateSoundEnabled(v: Boolean) {
        soundEnabled = v
        prefs?.edit()?.putBoolean(SOUND_ENABLED, v)?.apply()
    }

    fun updateSoundVolume(v: Float) {
        soundVolume = v.coerceIn(0f, 1f)
        prefs?.edit()?.putFloat(SOUND_VOLUME, soundVolume)?.apply()
    }

    fun updatePopupEnabled(v: Boolean) {
        popupEnabled = v
        prefs?.edit()?.putBoolean(POPUP_ENABLED, v)?.apply()
    }

    fun updateKeyBgShading(v: Boolean) {
        keyBgShading = v
        prefs?.edit()?.putBoolean(KEY_BG_SHADING, v)?.apply()
    }

    fun updateShiftLockMethod(v: String) {
        shiftLockMethod = v
        prefs?.edit()?.putString(SHIFT_LOCK_METHOD, v)?.apply()
    }

    fun updateSymLockMethod(v: String) {
        symLockMethod = v
        prefs?.edit()?.putString(SYM_LOCK_METHOD, v)?.apply()
    }

    fun updateSymAutoReturn(v: Boolean) {
        symAutoReturn = v
        prefs?.edit()?.putBoolean(SYM_AUTO_RETURN, v)?.apply()
    }

    fun updateAutoCapitalize(v: Boolean) {
        autoCapitalize = v
        prefs?.edit()?.putBoolean(AUTO_CAPITALIZE, v)?.apply()
    }

    fun updateLongPressMs(v: Int) {
        val clamped = v.coerceIn(150, 400)
        longPressMs = clamped
        prefs?.edit()?.putInt(LONG_PRESS_MS, clamped)?.apply()
    }

    fun updateDevMode(v: Boolean) {
        devMode = v
        prefs?.edit()?.putBoolean(DEV_MODE, v)?.apply()
    }

    fun updateEncryptToSelf(v: Boolean) {
        encryptToSelf = v
        prefs?.edit()?.putBoolean(ENCRYPT_TO_SELF, v)?.apply()
    }

    fun updateDefaultKeyId(id: Long?) {
        defaultKeyId = id
        prefs?.edit()?.putLong(DEFAULT_KEY, id ?: -1L)?.apply()
    }
}
