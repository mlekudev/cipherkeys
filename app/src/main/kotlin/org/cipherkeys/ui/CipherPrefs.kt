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
    private const val ALWAYS_ASK_RECIPIENTS = "always_ask_recipients"

    private const val DEFAULT_KEY_HEIGHT = 47
    private const val DEFAULT_PANEL_LINES = 4
    private const val DEFAULT_LONG_PRESS_MS = 250

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

    var alwaysAskRecipients by mutableStateOf(true)
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
        alwaysAskRecipients = prefs!!.getBoolean(ALWAYS_ASK_RECIPIENTS, true)
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

    fun updateAlwaysAskRecipients(v: Boolean) {
        alwaysAskRecipients = v
        prefs?.edit()?.putBoolean(ALWAYS_ASK_RECIPIENTS, v)?.apply()
    }

    fun exportPrefs(): Map<String, String> = mapOf(
        KEY_HEIGHT to keyHeightDp.toString(),
        PANEL_LINES to panelLines.toString(),
        HAPTIC_ENABLED to hapticEnabled.toString(),
        SOUND_ENABLED to soundEnabled.toString(),
        SOUND_VOLUME to soundVolume.toString(),
        POPUP_ENABLED to popupEnabled.toString(),
        KEY_BG_SHADING to keyBgShading.toString(),
        SHIFT_LOCK_METHOD to shiftLockMethod,
        SYM_LOCK_METHOD to symLockMethod,
        SYM_AUTO_RETURN to symAutoReturn.toString(),
        AUTO_CAPITALIZE to autoCapitalize.toString(),
        LONG_PRESS_MS to longPressMs.toString(),
        DEV_MODE to devMode.toString(),
        ENCRYPT_TO_SELF to encryptToSelf.toString(),
        ALWAYS_ASK_RECIPIENTS to alwaysAskRecipients.toString(),
        DEFAULT_KEY to (defaultKeyId?.toString() ?: "-1"),
    )

    fun importPrefs(map: Map<String, String>) {
        map[KEY_HEIGHT]?.toIntOrNull()?.let { updateKeyHeight(it) }
        map[PANEL_LINES]?.toIntOrNull()?.let { updatePanelLines(it) }
        map[HAPTIC_ENABLED]?.toBooleanStrictOrNull()?.let { updateHapticEnabled(it) }
        map[SOUND_ENABLED]?.toBooleanStrictOrNull()?.let { updateSoundEnabled(it) }
        map[SOUND_VOLUME]?.toFloatOrNull()?.let { updateSoundVolume(it) }
        map[POPUP_ENABLED]?.toBooleanStrictOrNull()?.let { updatePopupEnabled(it) }
        map[KEY_BG_SHADING]?.toBooleanStrictOrNull()?.let { updateKeyBgShading(it) }
        map[SHIFT_LOCK_METHOD]?.let { updateShiftLockMethod(it) }
        map[SYM_LOCK_METHOD]?.let { updateSymLockMethod(it) }
        map[SYM_AUTO_RETURN]?.toBooleanStrictOrNull()?.let { updateSymAutoReturn(it) }
        map[AUTO_CAPITALIZE]?.toBooleanStrictOrNull()?.let { updateAutoCapitalize(it) }
        map[LONG_PRESS_MS]?.toIntOrNull()?.let { updateLongPressMs(it) }
        map[DEV_MODE]?.toBooleanStrictOrNull()?.let { updateDevMode(it) }
        map[ENCRYPT_TO_SELF]?.toBooleanStrictOrNull()?.let { updateEncryptToSelf(it) }
        map[ALWAYS_ASK_RECIPIENTS]?.toBooleanStrictOrNull()?.let { updateAlwaysAskRecipients(it) }
        map[DEFAULT_KEY]?.toLongOrNull()?.let { id -> updateDefaultKeyId(if (id >= 0) id else null) }
    }
}
