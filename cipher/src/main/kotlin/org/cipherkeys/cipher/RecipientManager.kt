package org.cipherkeys.cipher

import android.content.Context
import android.content.SharedPreferences
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.pgpainless.PGPainless

data class RecipientInfo(
    val keyId: Long,
    val name: String,
    val userId: String
)

class RecipientManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun addRecipient(name: String, armoredPublicKey: String): RecipientInfo {
        val keyRing = PGPainless.readKeyRing().publicKeyRing(armoredPublicKey)
            ?: throw CipherException("failed to read public key from armored data")
        val keyId = keyRing.publicKey.keyID
        val userId = keyRing.publicKey.getUserIDs().next()
        val ids = prefs.getString(PREF_IDS, null)
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet()
            ?: mutableSetOf()
        ids.add(keyId.toString())
        prefs.edit()
            .putString(PREF_IDS, ids.joinToString(","))
            .putString(keyField(keyId, "name"), name)
            .putString(keyField(keyId, "userid"), userId)
            .putString(keyField(keyId, "key"), armoredPublicKey)
            .apply()

        return RecipientInfo(keyId, name, userId)
    }

    fun removeRecipient(keyId: Long) {
        val ids = prefs.getString(PREF_IDS, null)
            ?.split(",")?.filter { it.isNotEmpty() }?.toMutableSet() ?: return
        ids.remove(keyId.toString())
        prefs.edit()
            .putString(PREF_IDS, ids.joinToString(","))
            .remove(keyField(keyId, "name"))
            .remove(keyField(keyId, "userid"))
            .remove(keyField(keyId, "key"))
            .remove(recencyField(keyId))
            .apply()
    }

    fun markSelected(keyId: Long) {
        val now = System.currentTimeMillis()
        val recency = getRecencyMap().toMutableMap()
        recency[keyId] = now
        prefs.edit().putString(PREF_RECENCY, encodeRecency(recency)).apply()
    }

    fun listRecipients(): List<RecipientInfo> {
        val ids = prefs.getString(PREF_IDS, null) ?: return emptyList()
        val recency = getRecencyMap()
        return ids.split(",").filter { it.isNotEmpty() }.map { idStr ->
            val id = idStr.toLong()
            RecipientInfo(
                keyId = id,
                name = prefs.getString(keyField(id, "name"), "") ?: "",
                userId = prefs.getString(keyField(id, "userid"), "") ?: ""
            )
        }.sortedWith(compareByDescending<RecipientInfo> { recency[it.keyId] ?: 0L }
            .thenBy { it.name.lowercase() })
    }

    private fun getRecencyMap(): Map<Long, Long> {
        val raw = prefs.getString(PREF_RECENCY, null) ?: return emptyMap()
        return raw.split(",").filter { it.isNotEmpty() }.mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val id = parts[0].toLongOrNull()
                val ts = parts[1].toLongOrNull()
                if (id != null && ts != null) id to ts else null
            } else null
        }.toMap()
    }

    private fun encodeRecency(recency: Map<Long, Long>): String {
        return recency.entries.sortedByDescending { it.value }
            .joinToString(",") { "${it.key}:${it.value}" }
    }

    fun getRecipientPublicKey(keyId: Long): PGPPublicKeyRing? {
        val armored = prefs.getString(keyField(keyId, "key"), null) ?: return null
        return PGPainless.readKeyRing().publicKeyRing(armored)
    }

    fun exportArmoredPublicKey(keyId: Long): String? {
        return prefs.getString(keyField(keyId, "key"), null)
    }

    fun searchRecipients(query: String): List<RecipientInfo> {
        val q = query.lowercase()
        return listRecipients().filter {
            it.name.lowercase().contains(q) || it.userId.lowercase().contains(q)
        }
    }

    private fun keyField(keyId: Long, field: String) = "recipient_${keyId}_$field"
    private fun recencyField(keyId: Long) = "recipient_${keyId}_recency"

    companion object {
        private const val PREFS_NAME = "cipherkeys_recipients"
        private const val PREF_IDS = "recipient_ids"
        private const val PREF_RECENCY = "recipient_recency"
    }
}
