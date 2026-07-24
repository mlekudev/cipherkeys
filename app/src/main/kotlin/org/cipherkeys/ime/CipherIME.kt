package org.cipherkeys.ime

import android.content.Intent
import android.content.IntentFilter
import android.inputmethodservice.InputMethodService
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
import org.cipherkeys.cipher.RecipientManager
import org.cipherkeys.settings.CipherSettingsActivity
import org.cipherkeys.ui.CipherPanel
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

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
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
                        onEncrypt = { onEncryptAction() },
                        onDecrypt = { onDecryptAction() },
                        onSend = {
                            val ic = currentInputConnection
                            val text = CipherUiState.state.composeText
                            if (ic != null && text.isNotEmpty()) { ic.commitText(text, 1) }
                            CipherUiState.updateComposeText("")
                        },
                        onCopy = {
                            val text = CipherUiState.state.composeText.ifEmpty { CipherUiState.state.decryptedText }
                            if (text.isNotEmpty()) {
                                (getSystemService(android.content.ClipboardManager::class.java))
                                    ?.setPrimaryClip(android.content.ClipData.newPlainText("cipher", text))
                                Toast.makeText(this@CipherIME, "Copied", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onPaste = {
                            val cm = getSystemService(android.content.ClipboardManager::class.java)
                            val clip = cm?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            if (clip.isNotEmpty()) {
                                CipherUiState.insertTextAtCursor(clip)
                            }
                        },
                        onSettingsClick = {
                            startActivity(Intent(this@CipherIME, CipherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        },
                        onRecipientsClick = {
                            startActivity(Intent(this@CipherIME, CipherSettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("screen", "recipients"))
                        },
                        signingKeyName = signName,
                    )

                    KeyboardView(
                        onChar = { c ->
                            if (CipherUiState.state.isActive) {
                                CipherUiState.insertAtCursor(c)
                            } else {
                                currentInputConnection?.commitText(c, 1)
                            }
                        },
                        onBackspace = {
                            if (CipherUiState.state.isActive) {
                                CipherUiState.deleteBeforeCursor()
                            } else {
                                currentInputConnection?.deleteSurroundingText(1, 0)
                            }
                        },
                        onEnter = {
                            if (CipherUiState.state.isActive) {
                                CipherUiState.insertAtCursor("\n")
                            } else {
                                currentInputConnection?.commitText("\n", 1)
                            }
                        },
                        onSpace = {
                            if (CipherUiState.state.isActive) {
                                CipherUiState.insertAtCursor(" ")
                            } else {
                                currentInputConnection?.commitText(" ", 1)
                            }
                        },
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        try { unregisterReceiver(screenOffReceiver) } catch (_: Exception) {}
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
            doEncrypt(s.composeText)
            return
        }
        if (s.composeText.isBlank()) { CipherUiState.setError("Enter text to encrypt"); return }
        if (s.selectedRecipientIds.isEmpty()) { CipherUiState.setError("Add recipients via person icon"); return }
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
            doDecrypt(s.composeText)
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
        val s = CipherUiState.state; CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val recips = s.selectedRecipientIds.mapNotNull { recipientManager.getRecipientPublicKey(it) }
                if (recips.isEmpty()) { CipherUiState.setError("No valid recipient keys"); cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); return@execute }
                val signKey = s.selectedSigningKeyId?.let { keyManager.getSecretKeyRing(it) }
                val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
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

    private fun doDecrypt(pw: String) {
        val s = CipherUiState.state; CipherUiState.setLoading(true)
        cryptoExecutor.execute {
            try {
                val allKeys = keyManager.listKeys().mapNotNull { keyManager.getSecretKeyRing(it.keyId) }
                if (allKeys.isEmpty()) { CipherUiState.setError("No keys stored"); if (s.pendingAction != null) CipherUiState.cancelPassphrase(); return@execute }
                val msg = if (s.savedComposeText.isNotEmpty()) s.savedComposeText else s.composeText
                val r = pgpEngine.decryptTryAll(msg, allKeys, pw)
                cachedPassphrase = pw
                CipherUiState.updateComposeText(r)
                CipherUiState.clearPendingAndSaved()
                CipherUiState.clearError()
            } catch (e: CipherException) { cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError(e.message ?: "Wrong passphrase?") }
            catch (e: Exception) { cachedPassphrase = null; if (s.pendingAction != null) CipherUiState.cancelPassphrase(); CipherUiState.setError("Error: ${e.message}") }
            finally { CipherUiState.setLoading(false) }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting) }
}
