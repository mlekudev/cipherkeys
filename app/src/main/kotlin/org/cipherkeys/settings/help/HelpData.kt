package org.cipherkeys.settings.help

enum class MockView { COLLAPSED, EXPANDED }

data class HelpEntry(
    val id: String,
    val title: String,
    val description: String,
    val mock: MockView,
)

val allHelpEntries = listOf(
    // Mock A - Collapsed Panel
    HelpEntry("letter-keys", "Letter Keys",
        "Tap any letter key to type that character into the active text field (app) or the Cipher compose panel (when the cipher switch is on).",
        MockView.COLLAPSED),
    HelpEntry("num-superscript", "Number Superscripts",
        "The small numbers (¹²³…) on the top row (Q through P) show the number that will be typed if you long-press that key. " +
        "Hold any top-row key for about half a second to type its number without switching to the number keyboard.",
        MockView.COLLAPSED),
    HelpEntry("shift-key", "Shift Key (⇧)",
        "Tap once to type a single uppercase letter — after one letter, it switches back to lowercase automatically. " +
        "Long-press to lock shift on (stays highlighted). Tap it again when locked to unlock and return to lowercase.",
        MockView.COLLAPSED),
    HelpEntry("backspace-key", "Backspace / Delete (⌫)",
        "Tap to delete the character before the cursor. " +
        "Long-press to delete continuously — characters are removed rapidly while you hold the key down. " +
        "If text is selected in the app, pressing backspace replaces the selection.",
        MockView.COLLAPSED),
    HelpEntry("enter-key", "Enter / Submit (↵)",
        "Tap to insert a newline. Long-press always triggers the app's submit action (send, search, go, etc.). " +
        "If the cipher panel is open and recipients are active, long-press also encrypts the message, " +
        "pastes the encrypted result into the app's text field, then triggers submit — all in one gesture. " +
        "When the panel is closed or no recipients are selected, long-press just triggers submit.",
        MockView.COLLAPSED),
    HelpEntry("enter-lock-superscript", "Enter Lock Superscript (🔒)",
        "This small lock icon appears on the enter key only when the cipher panel is ON and at least one recipient is active. " +
        "It means long-pressing enter will encrypt your message, paste the result into the app, and trigger the send/submit action — all in one gesture.",
        MockView.COLLAPSED),
    HelpEntry("space-bar", "Space Bar",
        "Tap to insert a space.",
        MockView.COLLAPSED),
    HelpEntry("symbols-key", "?123 Key (Numbers & Symbols)",
        "Tap to switch to the number and symbol keyboard. After typing one symbol, the keyboard switches back to letters automatically. " +
        "Long-press to lock in symbol mode — it stays highlighted. Tap again to unlock and return to letters.",
        MockView.COLLAPSED),
    HelpEntry("eq-key", "=\\< Key (Symbol Page Toggle)",
        "When in the symbol/number keyboard, tap this key to switch between the two symbol pages. " +
        "Page 1 has basic symbols (!@#$%…) and page 2 has special characters (©®™√π…).",
        MockView.COLLAPSED),
    HelpEntry("abc-key", "ABC Key",
        "Tap to switch from the symbol keyboard back to the regular letter (QWERTY) keyboard.",
        MockView.COLLAPSED),
    HelpEntry("toggle-arrow-collapsed", "Toggle Arrow (Panel Switch)",
        "This arrow opens and closes the Cipher panel. When pointing up (▲), the panel is collapsed — tap it to expand. " +
        "When pointing down (▼), the panel is expanded — tap to collapse. When collapsed, the keyboard works like a normal keyboard, typing into the active app.",
        MockView.COLLAPSED),
    HelpEntry("comma-period", "Comma & Period Keys",
        "Quick access to comma (,) and period (.) for punctuation.",
        MockView.COLLAPSED),

    // Mock B - Expanded Panel
    HelpEntry("encrypt-btn", "Encrypt Button 🔒",
        "Encrypts the text in the compose panel using all active recipients. " +
        "If a signing key is selected and you haven't entered your passphrase yet, you'll be prompted to type it. " +
        "The encrypted result replaces the text in the compose panel. You can then Send or Copy it.",
        MockView.EXPANDED),
    HelpEntry("decrypt-btn", "Decrypt Button 🔓",
        "Decrypts the text in the compose panel using your stored secret keys. " +
        "If you haven't entered your passphrase yet (or it's not cached), you'll be prompted to type it. " +
        "The decrypted plaintext replaces the text in the compose panel.",
        MockView.EXPANDED),
    HelpEntry("send-btn", "Send Button",
        "Sends the text in the compose panel to the active app's text field — the same as copying and pasting. " +
        "Clears the compose panel after sending.",
        MockView.EXPANDED),
    HelpEntry("copy-btn", "Copy Button",
        "Copies the text in the compose panel to the system clipboard. " +
        "If the compose panel has text, that is copied. If the compose panel is empty but a decrypted result was just shown, the result is copied.",
        MockView.EXPANDED),
    HelpEntry("paste-btn", "Paste Button",
        "Pastes the current system clipboard content into the compose panel at the cursor position. " +
        "Useful for pasting encrypted messages from other apps for decryption, or pasting recipient public keys.",
        MockView.EXPANDED),
    HelpEntry("recipients-btn", "Recipients Button 👤",
        "Shows the number of currently active recipients next to this button. " +
        "Tap to open the Recipients settings page where you can add, remove, and toggle recipients on/off. " +
        "Only active (toggled on) recipients are used when encrypting.",
        MockView.EXPANDED),
    HelpEntry("settings-btn", "Settings Button ⚙",
        "Opens the CipherKeys settings screen where you can manage PGP keys, recipients, and view help.",
        MockView.EXPANDED),
    HelpEntry("passphrase-cancel", "Passphrase Cancel ✕",
        "Appears when the keyboard is waiting for you to enter your passphrase. " +
        "Tap to cancel and restore the original text in the compose panel.",
        MockView.EXPANDED),
    HelpEntry("compose-panel", "Compose Text Panel",
        "This is the main text area for composing messages. " +
        "Type here when the cipher panel is open. Supports multi-line text. " +
        "When passphrase entry is needed, the text is hidden (shown as ••••).",
        MockView.EXPANDED),
    HelpEntry("cursor", "Blinking Cursor",
        "A blinking bar showing where the next typed character will appear. " +
        "Tap anywhere in the text panel to move the cursor to that position. " +
        "You can type, paste, backspace, and the cursor will follow your edits.",
        MockView.EXPANDED),
    HelpEntry("autoscroll", "Auto-Scroll",
        "When text overflows below the visible area of the compose panel, the panel automatically scrolls down " +
        "to keep the new line in view. You can also drag/swipe to scroll manually.",
        MockView.EXPANDED),
    HelpEntry("password-hiding", "Password Hiding",
        "When the keyboard prompts you for a passphrase, characters you type are hidden (shown as ••••). " +
        "This protects your passphrase from shoulder-surfing. The passphrase is cached in memory until you lock the device.",
        MockView.EXPANDED),
    HelpEntry("passphrase-caching", "Passphrase Caching",
        "After you enter your passphrase once successfully, it is cached in memory. " +
        "Subsequent encrypt/decrypt operations use the cached passphrase automatically — no need to re-type it. " +
        "If a decrypt fails (wrong passphrase), the cache is cleared and you'll be prompted again. " +
        "The cache is also cleared when the device screen turns off (locks).",
        MockView.EXPANDED),
    HelpEntry("symbols-lock-indicator", "Symbol Lock Indicator",
        "When shift or the ?123 key is locked on (via long-press), the key stays highlighted (inverted colors). " +
        "This tells you the mode is locked. Tap the highlighted key to unlock.",
        MockView.EXPANDED),
    HelpEntry("dark-light-theme", "Dark / Light Mode",
        "The keyboard and cipher panel automatically match your device's dark mode or light mode setting. " +
        "Colors adjust for readability in both themes.",
        MockView.EXPANDED),
    HelpEntry("toggle-arrow-expanded", "Toggle Arrow (Panel Switch)",
        "This arrow closes the Cipher panel. When pointing down (▼), the panel is expanded — tap to collapse. " +
        "When collapsed, the keyboard works like a normal keyboard, typing into the active app.",
        MockView.EXPANDED),
)
