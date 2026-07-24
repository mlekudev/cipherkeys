package org.cipherkeys.cipher

import android.content.Context
import android.content.SharedPreferences
import org.pgpainless.PGPainless

data class StoredKey(
    val keyId: Long,
    val userId: String,
    val armorHeader: String
)

class KeyStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun storeSecretKey(keyId: Long, armoredKey: String, passphrase: String) {
        if (passphrase.isEmpty()) throw CipherException("passphrase cannot be empty")

        val secretKey = PGPainless.readKeyRing().secretKeyRing(armoredKey)
            ?: throw CipherException("failed to read secret key from armored data")
        val userId = secretKey.publicKey.getUserIDs().next()
        val fingerprint = secretKey.publicKey.fingerprint
        val fid = fingerprint.fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }

        val entry = prefs.getString(PREF_KEYS, null)
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet()
            ?: mutableSetOf()
        entry.add(fid.toString())
        prefs.edit()
            .putString(PREF_KEYS, entry.joinToString(","))
            .putString(keyEntry(fid), armoredKey)
            .putString(keyMeta(fid, "user"), userId)
            .apply()
    }

    fun getArmoredKey(keyId: Long): String? {
        return prefs.getString(keyEntry(keyId), null)
    }

    fun getSecretKeyRing(keyId: Long): org.bouncycastle.openpgp.PGPSecretKeyRing? {
        val armored = prefs.getString(keyEntry(keyId), null) ?: return null
        return PGPainless.readKeyRing().secretKeyRing(armored)
    }

    fun deleteSecretKey(keyId: Long) {
        val entry = prefs.getString(PREF_KEYS, null)
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: return
        entry.remove(keyId.toString())
        prefs.edit()
            .putString(PREF_KEYS, entry.joinToString(","))
            .remove(keyEntry(keyId))
            .remove(keyMeta(keyId, "user"))
            .apply()
    }

    fun listStoredKeys(): List<StoredKey> {
        val ids = prefs.getString(PREF_KEYS, null) ?: return emptyList()
        return ids.split(",").filter { it.isNotEmpty() }.map { idStr ->
            val id = idStr.toLong()
            val userId = prefs.getString(keyMeta(id, "user"), "unknown") ?: "unknown"
            val armored = prefs.getString(keyEntry(id), "") ?: ""
            val armorHeader = armored.lines().firstOrNull { it.isNotEmpty() } ?: ""
            StoredKey(id, userId, armorHeader)
        }
    }

    private fun keyEntry(keyId: Long) = "key_$keyId"
    private fun keyMeta(keyId: Long, field: String) = "key_${keyId}_$field"

    companion object {
        private const val PREFS_NAME = "cipherkeys_keystore"
        private const val PREF_KEYS = "key_ids"
    }
}

class CipherException(message: String, cause: Throwable? = null) : Exception(message, cause)
