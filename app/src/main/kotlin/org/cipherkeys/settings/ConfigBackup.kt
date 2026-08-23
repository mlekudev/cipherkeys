package org.cipherkeys.settings

import org.cipherkeys.cipher.CipherException
import org.cipherkeys.cipher.KeyManager
import org.cipherkeys.cipher.PgpEngine
import org.cipherkeys.cipher.RecipientManager
import org.cipherkeys.ui.CipherPrefs
import org.json.JSONArray
import org.json.JSONObject

object ConfigBackup {
    private const val MAGIC = "CipherKeysBackup"

    fun export(
        keyManager: KeyManager,
        recipientManager: RecipientManager,
        pgpEngine: PgpEngine,
        passphrase: String,
    ): String {
        val root = JSONObject()
        root.put("magic", MAGIC)
        root.put("version", 1)

        val keysArr = JSONArray()
        for (key in keyManager.listKeys()) {
            val armored = keyManager.getArmoredSecretKey(key.keyId) ?: continue
            val obj = JSONObject()
            obj.put("keyId", key.keyId)
            obj.put("userId", key.userId)
            obj.put("armored", armored)
            keysArr.put(obj)
        }
        root.put("keys", keysArr)

        val recipsArr = JSONArray()
        for (r in recipientManager.listRecipients()) {
            val armored = recipientManager.exportArmoredPublicKey(r.keyId) ?: continue
            val obj = JSONObject()
            obj.put("keyId", r.keyId)
            obj.put("name", r.name)
            obj.put("userId", r.userId)
            obj.put("armored", armored)
            recipsArr.put(obj)
        }
        root.put("recipients", recipsArr)

        val prefsObj = JSONObject()
        for ((k, v) in CipherPrefs.exportPrefs()) {
            prefsObj.put(k, v)
        }
        root.put("prefs", prefsObj)

        return pgpEngine.encryptSymmetric(root.toString(), passphrase)
    }

    fun import(
        data: String,
        keyManager: KeyManager,
        recipientManager: RecipientManager,
        pgpEngine: PgpEngine,
        passphrase: String,
    ) {
        val plain = try {
            pgpEngine.decryptSymmetric(data, passphrase)
        } catch (e: CipherException) {
            throw e
        } catch (e: Exception) {
            throw CipherException("incorrect passphrase or invalid backup")
        }
        val root = JSONObject(plain)
        if (root.optString("magic") != MAGIC) {
            throw CipherException("not a CipherKeys backup file")
        }

        val keysArr = root.optJSONArray("keys")
        if (keysArr != null) {
            for (i in 0 until keysArr.length()) {
                val obj = keysArr.getJSONObject(i)
                val keyId = obj.optLong("keyId")
                val armored = obj.optString("armored")
                val userId = obj.optString("userId")
                if (armored.isNotEmpty() && keyManager.getArmoredSecretKey(keyId) == null) {
                    keyManager.importBackupKey(keyId, userId, armored)
                }
            }
        }

        val recipsArr = root.optJSONArray("recipients")
        if (recipsArr != null) {
            val existingIds = recipientManager.listRecipients().map { it.keyId }.toSet()
            for (i in 0 until recipsArr.length()) {
                val obj = recipsArr.getJSONObject(i)
                val keyId = obj.optLong("keyId")
                val name = obj.optString("name")
                val armored = obj.optString("armored")
                if (armored.isNotEmpty() && keyId !in existingIds) {
                    recipientManager.addRecipient(name, armored)
                }
            }
        }

        val prefsObj = root.optJSONObject("prefs")
        if (prefsObj != null) {
            val map = HashMap<String, String>()
            val it = prefsObj.keys()
            while (it.hasNext()) {
                val k = it.next()
                map[k] = prefsObj.optString(k)
            }
            CipherPrefs.importPrefs(map)
        }
    }
}
