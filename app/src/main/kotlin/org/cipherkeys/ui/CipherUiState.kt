package org.cipherkeys.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

data class CipherState(
    val isActive: Boolean = false,
    val composeText: String = "",
    val cursorPos: Int = 0,
    val decryptedText: String = "",
    val selectedSigningKeyId: Long? = null,
    val selectedRecipientIds: Set<Long> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pendingAction: PendingAction? = null,
    val savedComposeText: String = "",
    val backspaceKill: Long = 0,
    val showPassword: Boolean = false,
)

enum class PendingAction { ENCRYPT, DECRYPT }

object CipherUiState {
    var state by mutableStateOf(CipherState())
        private set

    fun toggle() { state = state.copy(isActive = !state.isActive) }
    fun deactivate() { state = state.copy(isActive = false) }
    fun updateComposeText(text: String) { state = state.copy(composeText = text) }
    fun setDecryptedText(text: String) { state = state.copy(decryptedText = text, errorMessage = null) }
    fun setSigningKey(id: Long?) { state = state.copy(selectedSigningKeyId = id) }
    fun toggleRecipient(id: Long) {
        val current = state.selectedRecipientIds.toMutableSet()
        if (current.contains(id)) current.remove(id) else current.add(id)
        state = state.copy(selectedRecipientIds = current)
    }
    fun setLoading(loading: Boolean) { state = state.copy(isLoading = loading) }
    fun setError(msg: String?) { state = state.copy(errorMessage = msg, isLoading = false) }
    fun clearError() { state = state.copy(errorMessage = null) }
    fun requestPassphrase(action: PendingAction) {
        state = state.copy(
            savedComposeText = state.composeText, composeText = "",
            cursorPos = 0, pendingAction = action, errorMessage = null,
        )
    }
    fun cancelPassphrase() {
        state = state.copy(
            composeText = state.savedComposeText, savedComposeText = "",
            cursorPos = state.savedComposeText.length, pendingAction = null,
        )
    }
    fun clearPendingAndSaved() {
        state = state.copy(savedComposeText = "", pendingAction = null)
    }
    fun needsPassphrase() = state.pendingAction != null

    fun insertAtCursor(char: String) {
        val t = state.composeText
        val pos = state.cursorPos.coerceIn(0, t.length)
        val newText = t.substring(0, pos) + char + t.substring(pos)
        state = state.copy(composeText = newText, cursorPos = pos + 1)
    }

    fun insertTextAtCursor(text: String) {
        val t = state.composeText
        val pos = state.cursorPos.coerceIn(0, t.length)
        val newText = t.substring(0, pos) + text + t.substring(pos)
        state = state.copy(composeText = newText, cursorPos = pos + text.length)
    }

    fun deleteBeforeCursor() {
        val t = state.composeText
        val pos = state.cursorPos.coerceIn(0, t.length)
        if (pos > 0) {
            val newText = t.substring(0, pos - 1) + t.substring(pos)
            state = state.copy(composeText = newText, cursorPos = pos - 1)
        }
    }

    fun deleteAfterCursor() {
        val t = state.composeText
        val pos = state.cursorPos.coerceIn(0, t.length)
        if (pos < t.length) {
            val newText = t.substring(0, pos) + t.substring(pos + 1)
            state = state.copy(composeText = newText)
        }
    }

    fun moveCursor(offset: Int) {
        val t = state.composeText
        val pos = (state.cursorPos + offset).coerceIn(0, t.length)
        state = state.copy(cursorPos = pos)
    }

    fun setCursorPos(pos: Int) {
        val t = state.composeText
        state = state.copy(cursorPos = pos.coerceIn(0, t.length))
    }

    fun clearText() {
        state = state.copy(composeText = "", cursorPos = 0)
    }

    fun cancelBackspaceRepeat() {
        state = state.copy(backspaceKill = state.backspaceKill + 1)
    }

    fun toggleShowPassword() {
        state = state.copy(showPassword = !state.showPassword)
    }
}
