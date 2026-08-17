package org.cipherkeys.ime

import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import org.cipherkeys.cipher.CipherException
import org.cipherkeys.cipher.KeyManager
import org.cipherkeys.cipher.KeyStore
import org.cipherkeys.cipher.PgpEngine
import org.cipherkeys.cipher.RecipientInfo
import org.cipherkeys.cipher.RecipientManager
import org.cipherkeys.settings.CipherSettingsActivity
import org.cipherkeys.ui.CipherPanel
import org.cipherkeys.ui.CipherPrefs
import org.cipherkeys.ui.CipherUiState
import org.cipherkeys.ui.KeyboardView
import org.cipherkeys.ui.PendingAction
import java.util.concurrent.Executors

class CipherIME : InputMethodService(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    private val keyStore by lazy { KeyStore(this) }
    private val keyManager by lazy { KeyManager(keyStore) }
    private val recipientManager by lazy { RecipientManager(this) }
    private val pgpEngine by lazy { PgpEngine() }
    private val cryptoExecutor = Executors.newSingleThreadExecutor()
    private var cachedPassphrase: String? = null
    private var inputManager: android.hardware.input.InputManager? = null
    private var isHardwareKeyboardConnected = false

    private val inputDeviceListener = object : android.hardware.input.InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) {
            updateHardwareKeyboardState()
        }

        override fun onInputDeviceRemoved(deviceId: Int) {
            updateHardwareKeyboardState()
        }

        override fun onInputDeviceChanged(deviceId: Int) {
            updateHardwareKeyboardState()
        }
    }

    private fun readHasHardwareKeyboard(conf: android.content.res.Configuration): Boolean {
        return conf.keyboard != android.content.res.Configuration.KEYBOARD_NOKEYS &&
            conf.hardKeyboardHidden != android.content.res.Configuration.HARDKEYBOARDHIDDEN_YES
    }

    private fun hasExternalKeyboardDevice(): Boolean {
        val ids: IntArray = android.view.InputDevice.getDeviceIds()
        val found: MutableList<android.view.InputDevice> = mutableListOf()
        for (deviceId in ids) {
            val device: android.view.InputDevice? = android.view.InputDevice.getDevice(deviceId)
            if (device != null) found.add(device)
        }
        return found.any { d ->
            d.isExternal &&
                (d.sources and android.view.InputDevice.SOURCE_KEYBOARD) != 0
        }
    }

    private fun updateHardwareKeyboardState() {
        val configConnected = readHasHardwareKeyboard(resources.configuration)
        val deviceConnected = hasExternalKeyboardDevice()
        val connected = configConnected || deviceConnected
        if (connected != isHardwareKeyboardConnected) {
            isHardwareKeyboardConnected = connected
            CipherUiState.setPhysicalKeyboardConnected(connected)
            Handler(Looper.getMainLooper()).post {
                if (CipherUiState.state.isActive) {
                    // Keep the panel visible: physical keyboard replaces the touch keyboard.
                    // The Compose layer hides the KeyboardView based on physicalKeyboardConnected.
                    requestShowSelf(0)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        CipherPrefs.init(this)
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
        try {
            inputManager = getSystemService(android.content.Context.INPUT_SERVICE) as? android.hardware.input.InputManager
            inputManager?.registerInputDeviceListener(inputDeviceListener, Handler(Looper.getMainLooper()))
            updateHardwareKeyboardState()
        } catch (e: Exception) {
            Log.e("CipherIME", "onCreate detection error", e)
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        updateHardwareKeyboardState()
    }

    override fun onCreateInputView(): View {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        window?.window?.decorView?.apply {
            setViewTreeLifecycleOwner(this@CipherIME)
            setViewTreeSavedStateRegistryOwner(this@CipherIME)
        }
        return ComposeView(this).apply {
            setBackgroundColor(0xFF1C1B1F.toInt())
            setContent {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 13.dp),
                ) {
                    val signId = CipherUiState.state.selectedSigningKeyId
                    val signName = if (signId != null) {
                        keyManager.listKeys().find { it.keyId == signId }?.userId
                            ?.split("<")?.firstOrNull()?.trim()
                    } else null

                    CipherPanel(
                        onEncrypt = { CipherUiState.cancelBackspaceRepeat(); onEncryptAction() },
                        onDecrypt = { CipherUiState.cancelBackspaceRepeat(); onDecryptAction() },
                        onSend = { CipherUiState.cancelBackspaceRepeat(); handleSend() },
                        onSign = {
                            CipherUiState.cancelBackspaceRepeat()
                            val s = CipherUiState.state
                            if (s.composeText.isBlank()) { CipherUiState.setError("Enter text to sign"); return@CipherPanel }
                            val keys = keyManager.listKeys()
                            if (keys.isEmpty()) { CipherUiState.setError("Create or import a PGP key to sign with"); return@CipherPanel }
                            if (s.selectedSigningKeyId == null) {
                                val def = CipherPrefs.defaultKeyId
                                if (def != null && keys.any { it.keyId == def }) {
                                    CipherUiState.setSigningKey(def)
                                    if (cachedPassphrase == null) {
                                        CipherUiState.requestPassphrase(PendingAction.SIGN)
                                    } else {
                                        doSign(cachedPassphrase!!)
                                    }
                                } else {
                                    CipherUiState.showSignerPicker()
                                }
                            } else if (cachedPassphrase == null) {
                                CipherUiState.requestPassphrase(PendingAction.SIGN)
                            } else {
                                doSign(cachedPassphrase!!)
                            }
                        },
                        onSignLongPress = {
                            CipherUiState.cancelBackspaceRepeat()
                            CipherUiState.setSigningKey(null)
                            CipherUiState.showSignerPicker()
                        },
                        onCopy = {
                            CipherUiState.cancelBackspaceRepeat()
                            val text = CipherUiState.state.composeText.ifEmpty { CipherUiState.state.decryptedText }
                            if (text.isNotEmpty()) {
                                (getSystemService(android.content.ClipboardManager::class.java))
                                    ?.setPrimaryClip(android.content.ClipData.newPlainText("cipher", text))
                                Toast.makeText(this@CipherIME, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPaste = {
                            CipherUiState.cancelBackspaceRepeat()
                            val cm = getSystemService(android.content.ClipboardManager::class.java)
                            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            Log.d("CipherIME", "onPaste: clipLen=${clip.length}, startsWithPGP=${clip.startsWith("-----BEGIN PGP")}")
                            if (clip.isNotEmpty()) {
                                CipherUiState.setLoading(true)
                                cryptoExecutor.execute {
                                    try {
                                        val allPubKeys = keyManager.listKeys().mapNotNull { keyManager.getPublicKeyRing(it.keyId) }
                                        Log.d("CipherIME", "onPaste: pubKeys=${allPubKeys.size}")
                                        val result = pgpEngine.verifySigned(clip, allPubKeys)
                                        Log.d("CipherIME", "onPaste: verifyResult=$result")
                                        Handler(Looper.getMainLooper()).post {
                                            CipherUiState.setLoading(false)
                                            if (result != null) {
                                                val stripped = pgpEngine.stripExpiration(result.plaintext)
                                                CipherUiState.insertTextAtCursor(stripped)
                                                Log.d("CipherIME", "onPaste: verified, plaintext=${result.plaintext.take(100)}")
                                                if (pgpEngine.checkExpired(result.plaintext)) {
                                                    CipherUiState.setInfo("Message expired", isWarning = true)
                                                } else {
                                                    CipherUiState.setInfo("Message valid", isWarning = false)
                                                }
                                            } else {
                                                val extracted = pgpEngine.extractPlaintext(clip)
                                                Log.d("CipherIME", "onPaste: not verified, extracted=$extracted")
                                                if (extracted != null) {
                                                    val stripped = pgpEngine.stripExpiration(extracted)
                                                    CipherUiState.insertTextAtCursor(stripped)
                                                    CipherUiState.setInfo("Invalid signature", isWarning = true)
                                                } else {
                                                    CipherUiState.insertTextAtCursor(clip)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CipherIME", "onPaste error", e)
                                        Handler(Looper.getMainLooper()).post {
                                            CipherUiState.setLoading(false)
                                            CipherUiState.insertTextAtCursor(clip)
                                        }
                                    }
                                }
                            }
                        },
                        onClear = {
                            CipherUiState.cancelBackspaceRepeat()
                            CipherUiState.clearText()
                        },
                        onSettingsClick = {
                            CipherUiState.cancelBackspaceRepeat()
                            startActivity(Intent(this@CipherIME, CipherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        onHelpClick = {
                            CipherUiState.cancelBackspaceRepeat()
                            startActivity(Intent(this@CipherIME, CipherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screen", "help"))
                        },
                        onRecipientsClick = {
                            CipherUiState.cancelBackspaceRepeat()
                            startActivity(Intent(this@CipherIME, CipherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screen", "recipients"))
                        },
                        signingKeyName = signName,
                        allRecipients = recipientManager.listRecipients(),
                        onToggleRecipient = { id ->
                            val wasSelected = CipherUiState.state.selectedRecipientIds.contains(id)
                            CipherUiState.toggleRecipient(id)
                            if (!wasSelected) recipientManager.markSelected(id)
                        },
                        allKeys = keyManager.listKeys(),
                        onSelectSigner = { id ->
                            CipherUiState.setSigningKey(id)
                            CipherUiState.setSignExpiry(0)
                        },
                    )

                    if (!CipherUiState.state.physicalKeyboardConnected) {
                    KeyboardView(
                        onChar = { c ->
                            if (CipherUiState.state.isActive) {
                                CipherUiState.insertAtCursor(c)
                            } else {
                                val ic = currentInputConnection
                                val isPasswordField = isPasswordInput(currentInputEditorInfo)
                                if (ic != null && CipherPrefs.autoCapitalize && !isPasswordField) {
                                    val prev = ic.getTextBeforeCursor(20, 0)?.toString() ?: ""
                                    val lastChar = prev.lastOrNull()
                                    val precededByWs = prev.isEmpty() || lastChar == null ||
                                        lastChar.isWhitespace()
                                    val trimmed = prev.trimEnd()
                                    val shouldCap = prev.isEmpty() || (precededByWs && (
                                        trimmed.endsWith(".") ||
                                        trimmed.endsWith("!") ||
                                        trimmed.endsWith("?") ||
                                        trimmed.endsWith(".\n") ||
                                        trimmed.endsWith("!\n") ||
                                        trimmed.endsWith("?\n")
                                    ))
                                    val kbShift = CipherUiState.state.keyboardShiftOn
                                    val char = if (shouldCap && c.length == 1 && c[0].isLowerCase() && kbShift) {
                                        c.uppercase()
                                    } else c
                                    ic.commitText(char, 1)
                                } else {
                                    currentInputConnection?.commitText(c, 1)
                                }
                                checkAutoShift()
                            }
                        },
                        onBackspace = {
                            if (CipherUiState.state.isActive) {
                                CipherUiState.deleteBeforeCursor()
                            } else {
                                val ic = currentInputConnection
                                if (ic?.getSelectedText(0) != null) {
                                    ic.commitText("", 1)
                                } else {
                                    ic?.deleteSurroundingText(1, 0)
                                }
                            }
                        },
                        onEnter = {
                            if (CipherUiState.state.isActive) {
                                if (CipherUiState.state.pendingAction != null && CipherUiState.state.composeText.isNotBlank()) {
                                    val pw = CipherUiState.state.composeText
                                    when (CipherUiState.state.pendingAction) {
                                        PendingAction.ENCRYPT -> doEncrypt(pw)
                                        PendingAction.DECRYPT -> doDecrypt(pw)
                                        PendingAction.SIGN -> doSign(pw)
                                        null -> {}
                                    }
                                } else {
                                    CipherUiState.insertAtCursor("\n")
                                }
                            } else {
                                currentInputConnection?.commitText("\n", 1)
                                CipherUiState.triggerAutoShift()
                            }
                        },
                        onSpace = {
                            if (CipherUiState.state.isActive) {
                                CipherUiState.insertAtCursor(" ")
                            } else {
                                currentInputConnection?.commitText(" ", 1)
                                checkAutoShift()
                            }
                        },
                        onEnterLongPress = {
                            CipherUiState.cancelBackspaceRepeat()
                            val s = CipherUiState.state
                            if (s.isActive && s.composeText.isNotBlank()) {
                                if (s.selectedRecipientIds.isNotEmpty()) {
                                    encryptAndSend(cachedPassphrase)
                                } else if (s.selectedSigningKeyId != null) {
                                    if (cachedPassphrase == null) {
                                        CipherUiState.requestPassphrase(PendingAction.SIGN)
                                    } else {
                                        doSign(cachedPassphrase!!)
                                    }
                                } else if (CipherPrefs.encryptToSelf && CipherPrefs.defaultKeyId != null) {
                                    encryptAndSend(cachedPassphrase)
                                } else if (keyManager.listKeys().isEmpty()) {
                                    CipherUiState.setError("Create or import a PGP key to encrypt or sign")
                                } else if (recipientManager.listRecipients().isEmpty()) {
                                    CipherUiState.setError("Add recipients via person icon")
                                } else {
                                    CipherUiState.showRecipientPicker(sendAfter = true)
                                }
                            } else {
                                val ic = currentInputConnection
                                if (ic != null) {
                                    val ei = currentInputEditorInfo
                                    val action = ei?.imeOptions?.and(android.view.inputmethod.EditorInfo.IME_MASK_ACTION) ?: 0
                                    if (action != 0) ic.performEditorAction(action)
                                    else sendDefaultEditorAction(true)
                                }
                            }
                        },
                        showLockHint = CipherUiState.state.isActive && CipherUiState.state.selectedRecipientIds.isNotEmpty(),
                    )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
        try { inputManager?.unregisterInputDeviceListener(inputDeviceListener) } catch (_: Exception) {}
    }

    private val screenOffReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: Intent?) {
            cachedPassphrase = null
        }
    }

    private fun onEncryptAction() {
        val s = CipherUiState.state
        if (s.pendingAction != null) {
            if (s.composeText.isBlank()) { CipherUiState.setError("Enter passphrase"); return }
            when (s.pendingAction) {
                PendingAction.ENCRYPT -> doEncrypt(s.composeText)
                PendingAction.DECRYPT -> doDecrypt(s.composeText)
                PendingAction.SIGN -> doSign(s.composeText)
            }
            return
        }
        if (s.composeText.isBlank()) { CipherUiState.setError("Enter text to encrypt"); return }
        if (keyManager.listKeys().isEmpty()) {
            CipherUiState.setError("Create or import a PGP key to encrypt"); return
        }
        val justConfirmed = s.recipientPickerConfirmed
        if (justConfirmed) {
            CipherUiState.consumeRecipientPickerConfirmed()
        }
        if (s.selectedRecipientIds.isEmpty()) {
            if (recipientManager.listRecipients().isEmpty()) {
                CipherUiState.setError("Add recipients via person icon")
            } else {
                CipherUiState.showRecipientPicker()
            }
            return
        }
        if (CipherPrefs.alwaysAskRecipients && !justConfirmed) {
            if (recipientManager.listRecipients().isEmpty()) {
                CipherUiState.setError("Add recipients via person icon")
            } else {
                CipherUiState.clearRecipients()
                CipherUiState.showRecipientPicker()
            }
            return
        }
        if (s.selectedSigningKeyId != null && cachedPassphrase == null) {
            CipherUiState.requestPassphrase(PendingAction.ENCRYPT)
        } else {
            val pw = cachedPassphrase
            doEncrypt(pw)
        }
    }

    private fun onDecryptAction() {
        val s = CipherUiState.state
        if (s.pendingAction != null) {
            if (s.composeText.isBlank()) { CipherUiState.setError("Enter passphrase"); return }
            when (s.pendingAction) {
                PendingAction.ENCRYPT -> doEncrypt(s.composeText)
                PendingAction.DECRYPT -> doDecrypt(s.composeText)
                PendingAction.SIGN -> doSign(s.composeText)
            }
            return
        }
        if (s.composeText.isBlank()) { CipherUiState.setError("Enter encrypted text"); return }
        if (cachedPassphrase != null) {
            val pw = cachedPassphrase!!
            doDecrypt(pw)
        } else {
            CipherUiState.requestPassphrase(PendingAction.DECRYPT)
        }
    }

    private fun doEncrypt(pw: String?) {
        val s = CipherUiState.state
        val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
        if (isEncryptedMessage(msg)) {
            CipherUiState.setInfo("Message already encrypted", isWarning = true)
            CipherUiState.clearPendingAndSaved()
            if (s.pendingAction != null) CipherUiState.cancelPassphrase()
            return
        }
        CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val recips = getEncryptRecipients(s)
                if (recips.isEmpty()) { CipherUiState.setError("No valid recipient keys"); cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); return@execute }
                val signKey = s.selectedSigningKeyId?.let { keyManager.getSecretKeyRing(it) }
                val r = pgpEngine.encryptArmored(msg, recips, signKey, pw)
                if (pw != null) cachedPassphrase = pw
                CipherUiState.updateComposeText(r)
                CipherUiState.clearPendingAndSaved()
                CipherUiState.clearError()
            } catch (e: CipherException) { cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError(e.message ?: "Encrypt failed") }
            catch (e: Exception) { cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError("Error: ${e.message}") }
            finally { CipherUiState.setLoading(false) }
        }
    }

    private fun encryptAndSend(pw: String?) {
        val s = CipherUiState.state
        val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
        if (isEncryptedMessage(msg)) {
            // Already encrypted - commit directly, do not re-encrypt
            val ic = currentInputConnection
            if (ic != null) {
                ic.commitText(msg, 1)
                val ei = currentInputEditorInfo
                val action = ei?.imeOptions?.and(android.view.inputmethod.EditorInfo.IME_MASK_ACTION) ?: 0
                if (action != 0) ic.performEditorAction(action)
                else sendDefaultEditorAction(true)
            }
            CipherUiState.clearText()
            CipherUiState.clearPendingAndSaved()
            CipherUiState.clearError()
            return
        }
        val justConfirmed = s.recipientPickerConfirmed
        if (justConfirmed) {
            CipherUiState.consumeRecipientPickerConfirmed()
        }
        if (CipherPrefs.alwaysAskRecipients && !justConfirmed) {
            if (recipientManager.listRecipients().isEmpty()) {
                CipherUiState.setError("Add recipients via person icon")
            } else {
                CipherUiState.clearRecipients()
                CipherUiState.showRecipientPicker(sendAfter = true)
            }
            return
        }
        CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val recips = getEncryptRecipients(s)
                if (recips.isEmpty()) { CipherUiState.setError("No valid recipient keys"); return@execute }
                val signKey = s.selectedSigningKeyId?.let { keyManager.getSecretKeyRing(it) }
                val r = pgpEngine.encryptArmored(msg, recips, signKey, pw)
                if (pw != null) cachedPassphrase = pw
                Handler(Looper.getMainLooper()).post {
                    val ic = currentInputConnection
                    if (ic != null) {
                        ic.commitText(r, 1)
                        val ei = currentInputEditorInfo
                        val action = ei?.imeOptions?.and(android.view.inputmethod.EditorInfo.IME_MASK_ACTION) ?: 0
                        if (action != 0) ic.performEditorAction(action)
                        else sendDefaultEditorAction(true)
                    }
                    CipherUiState.clearText()
                    CipherUiState.clearPendingAndSaved()
                    CipherUiState.clearError()
                    CipherUiState.setLoading(false)
                }
            } catch (e: CipherException) { CipherUiState.setError(e.message ?: "Encrypt failed"); CipherUiState.setLoading(false) }
            catch (e: Exception) { CipherUiState.setError("Error: ${e.message}"); CipherUiState.setLoading(false) }
        }
    }

    private fun doDecrypt(pw: String) {
        val s = CipherUiState.state; CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val allKeys = keyManager.listKeys().mapNotNull { keyManager.getSecretKeyRing(it.keyId) }
                Log.d("CipherIME", "doDecrypt: numKeys=${allKeys.size}, keyIDs=${allKeys.map { String.format("%016X", it.publicKey.keyID) }}")
                if (allKeys.isEmpty()) { CipherUiState.setError("No keys stored"); if (s.pendingAction != null) CipherUiState.cancelPassphrase(); return@execute }
                val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
                val r = pgpEngine.decryptTryAll(msg, allKeys, pw)
                cachedPassphrase = pw
                CipherUiState.updateComposeText(r)
                CipherUiState.clearPendingAndSaved()
                CipherUiState.clearError()
            } catch (e: CipherException) { Log.e("CipherIME", "Decrypt CipherException", e); cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError(e.message ?: "Wrong passphrase?") }
            catch (e: Exception) { Log.e("CipherIME", "Decrypt exception", e); cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError("Error: ${e.message}") }
            finally { CipherUiState.setLoading(false) }
        }
    }

    private fun doSign(pw: String) {
        val s = CipherUiState.state
        Log.d("CipherIME", "doSign: expiryDays=${s.signExpiryDays}, msgLen=${s.composeText.length}, savedLen=${s.savedComposeText.length}, keyId=${s.selectedSigningKeyId}")
        CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val signKey = s.selectedSigningKeyId?.let { keyManager.getSecretKeyRing(it) }
                if (signKey == null) { CipherUiState.setError("No signing key - create or import a PGP key to sign with"); return@execute }
                val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
                val expirySecs = if (s.signExpiryPast) 1L else if (s.signExpiryDays > 0) s.signExpiryDays * 86400L else 0L
                val r = pgpEngine.sign(msg, signKey, pw, expirySecs, s.signExpiryPast)
                cachedPassphrase = pw
                Handler(Looper.getMainLooper()).post {
                    val ic = currentInputConnection
                    if (ic != null) ic.commitText(r, 1)
                    CipherUiState.clearText()
                    CipherUiState.clearPendingAndSaved()
                    CipherUiState.clearError()
                    CipherUiState.setLoading(false)
                }
            } catch (e: CipherException) {
                Log.e("CipherIME", "Sign CipherException", e)
                cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase()
                CipherUiState.setSigningKey(null)
                CipherUiState.showSignerPicker()
                CipherUiState.setError(e.message ?: "Sign failed"); CipherUiState.setLoading(false)
            } catch (e: Exception) {
                Log.e("CipherIME", "Sign exception", e)
                cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase()
                CipherUiState.setSigningKey(null)
                CipherUiState.showSignerPicker()
                CipherUiState.setError("Error: ${e.message}"); CipherUiState.setLoading(false)
            }
        }
    }

    private fun getEncryptRecipients(s: org.cipherkeys.ui.CipherState): List<org.bouncycastle.openpgp.PGPPublicKeyRing> {
        val recips = s.selectedRecipientIds.mapNotNull { recipientManager.getRecipientPublicKey(it) }.toMutableList()
        Log.d("CipherIME", "getEncryptRecipients: selectedRids=${s.selectedRecipientIds}, encryptToSelf=${CipherPrefs.encryptToSelf}, selfKeyId=${CipherPrefs.defaultKeyId}")
        if (CipherPrefs.encryptToSelf && CipherPrefs.defaultKeyId != null) {
            val selfKeyId = CipherPrefs.defaultKeyId!!
            val selfKey = keyManager.listKeys().find { it.keyId == selfKeyId }
            Log.d("CipherIME", "getEncryptRecipients: selfKey found=${selfKey != null}")
            if (selfKey != null) {
                val alreadyIn = s.selectedRecipientIds.any { rid ->
                    recipientManager.getRecipientPublicKey(rid)?.publicKey?.keyID == selfKeyId
                }
                Log.d("CipherIME", "getEncryptRecipients: alreadyIn=$alreadyIn")
                if (!alreadyIn) {
                    val selfPub = keyManager.getPublicKeyRing(selfKeyId)
                    Log.d("CipherIME", "getEncryptRecipients: selfPub=${selfPub != null}, adding")
                    if (selfPub != null) recips.add(selfPub)
                }
            }
        }
        Log.d("CipherIME", "getEncryptRecipients: total recips=${recips.size}")
        return recips
    }

    override fun onStartInput(editorInfo: EditorInfo?, restarting: Boolean) {
        super.onStartInput(editorInfo, restarting)
        if (CipherUiState.state.pendingAction == null) {
            CipherUiState.resetKeyboardForInput(isPasswordInput(editorInfo))
        }
    }

    private fun handleSend() {
        val s = CipherUiState.state
        if (s.isActive && s.selectedRecipientIds.isNotEmpty() && s.composeText.isNotBlank()) {
            encryptAndSend(cachedPassphrase)
        } else if (s.isActive && s.selectedRecipientIds.isEmpty() && s.composeText.isNotBlank()) {
            if (s.selectedSigningKeyId != null) {
                if (cachedPassphrase == null) {
                    CipherUiState.requestPassphrase(PendingAction.SIGN)
                } else {
                    doSign(cachedPassphrase!!)
                }
            } else if (CipherPrefs.encryptToSelf && CipherPrefs.defaultKeyId != null) {
                encryptAndSend(cachedPassphrase)
            } else if (keyManager.listKeys().isEmpty()) {
                CipherUiState.setError("Create or import a PGP key to encrypt or sign")
            } else if (recipientManager.listRecipients().isEmpty()) {
                CipherUiState.setError("Add recipients via person icon")
            } else {
                CipherUiState.showRecipientPicker(sendAfter = true)
            }
        } else {
            val ic = currentInputConnection
            val text = s.composeText
            if (ic != null && text.isNotEmpty()) { ic.commitText(text, 1) }
            CipherUiState.updateComposeText("")
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        val s = CipherUiState.state
        if (s.isActive && isHardwareKeyboardConnected) {
            // Ctrl+Enter submits
            if (event.isCtrlPressed && (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER)) {
                handleSend()
                return true
            }
            // Plain Enter: submit passphrase or insert newline
            if (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER) {
                if (s.pendingAction != null && s.composeText.isNotBlank()) {
                    val pw = s.composeText
                    when (s.pendingAction) {
                        PendingAction.ENCRYPT -> doEncrypt(pw)
                        PendingAction.DECRYPT -> doDecrypt(pw)
                        PendingAction.SIGN -> doSign(pw)
                        null -> {}
                    }
                } else {
                    CipherUiState.insertAtCursor("\n")
                }
                return true
            }
            val char = event.unicodeChar
            if (char != 0) {
                CipherUiState.insertAtCursor(char.toChar().toString())
                return true
            }
            when (keyCode) {
                KeyEvent.KEYCODE_DEL -> { CipherUiState.deleteBeforeCursor(); return true }
                KeyEvent.KEYCODE_FORWARD_DEL -> { CipherUiState.deleteAfterCursor(); return true }
                KeyEvent.KEYCODE_DPAD_LEFT -> { CipherUiState.moveCursor(-1); return true }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { CipherUiState.moveCursor(1); return true }
                KeyEvent.KEYCODE_MOVE_HOME -> { CipherUiState.setCursorPos(0); return true }
                KeyEvent.KEYCODE_MOVE_END -> { CipherUiState.setCursorPos(CipherUiState.state.composeText.length); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun isPasswordInput(editorInfo: EditorInfo?): Boolean {
        val type = editorInfo?.inputType ?: return false
        val variation = type and android.text.InputType.TYPE_MASK_VARIATION
        return variation == android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
            variation == android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            ((type and android.text.InputType.TYPE_MASK_CLASS) == android.text.InputType.TYPE_CLASS_NUMBER &&
                variation == android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD)
    }

    private fun isEncryptedMessage(text: String): Boolean {
        return text.contains("-----BEGIN PGP MESSAGE-----")
    }

    private var lastSelStart = -1
    private var lastSelEnd = -1

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candidatesStart: Int, candidatesEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd)
        if (lastSelStart != -1 && (newSelStart != lastSelStart || newSelEnd != lastSelEnd)) {
            val jump = kotlin.math.abs(newSelStart - lastSelStart) > 1
            if (jump && CipherUiState.state.keyboardShiftOn) {
                CipherUiState.clearAutoShift()
            }
        }
        lastSelStart = newSelStart
        lastSelEnd = newSelEnd
    }

    private fun checkAutoShift() {
        if (!CipherPrefs.autoCapitalize) return
        val ic = currentInputConnection ?: return
        val prev = ic.getTextBeforeCursor(20, 0)?.toString() ?: return
        val lastChar = prev.lastOrNull() ?: return
        if (!lastChar.isWhitespace()) return
        val trimmed = prev.trimEnd()
        if (trimmed.endsWith(".") || trimmed.endsWith("!") || trimmed.endsWith("?") ||
            trimmed.endsWith(".\n") || trimmed.endsWith("!\n") || trimmed.endsWith("?\n")) {
            CipherUiState.triggerAutoShift()
        }
    }
}
