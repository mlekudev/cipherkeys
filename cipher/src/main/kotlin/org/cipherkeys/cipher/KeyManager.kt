package org.cipherkeys.cipher

import org.pgpainless.PGPainless
import org.pgpainless.key.generation.type.rsa.RsaLength
import org.pgpainless.util.ArmorUtils
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing

data class KeyInfo(
    val keyId: Long,
    val userId: String,
    val created: Long,
    val algorithm: String
)

class KeyManager(private val keyStore: KeyStore) {

    fun generateModernKey(userId: String, passphrase: String): KeyInfo {
        val key = PGPainless.generateKeyRing()
            .modernKeyRing(userId, passphrase)

        val armored = ArmorUtils.toAsciiArmoredString(key)
        val keyId = key.publicKey.keyID
        val info = keyInfo(keyId, key)
        keyStore.storeSecretKey(keyId, armored, passphrase)
        return info
    }

    fun generateRsaKey(userId: String, passphrase: String): KeyInfo {
        val key = PGPainless.generateKeyRing()
            .simpleRsaKeyRing(userId, RsaLength._4096, passphrase)

        val armored = ArmorUtils.toAsciiArmoredString(key)
        val keyId = key.publicKey.keyID
        val info = keyInfo(keyId, key)
        keyStore.storeSecretKey(keyId, armored, passphrase)
        return info
    }

    fun importSecretKey(armoredKey: String, passphrase: String): KeyInfo {
        val key = PGPainless.readKeyRing().secretKeyRing(armoredKey)
            ?: throw CipherException("failed to read secret key from armored data")
        val keyId = key.publicKey.keyID
        val info = keyInfo(keyId, key)
        keyStore.storeSecretKey(keyId, armoredKey, passphrase)
        return info
    }

    fun exportPublicKey(keyId: Long, passphrase: String): String {
        val keyRing = keyStore.getSecretKeyRing(keyId)
            ?: throw CipherException("key not found")
        val pubKeys = mutableListOf<org.bouncycastle.openpgp.PGPPublicKey>()
        keyRing.publicKeys.forEach { pubKeys.add(it as org.bouncycastle.openpgp.PGPPublicKey) }
        val pubKeyRing = PGPPublicKeyRing(pubKeys)
        return ArmorUtils.toAsciiArmoredString(pubKeyRing)
    }

    fun exportSecretKey(keyId: Long, passphrase: String): String {
        val armored = keyStore.getArmoredKey(keyId)
            ?: throw CipherException("key not found")
        return armored
    }

    fun deleteKey(keyId: Long) {
        keyStore.deleteSecretKey(keyId)
    }

    fun getSecretKeyRing(keyId: Long): org.bouncycastle.openpgp.PGPSecretKeyRing? {
        return keyStore.getSecretKeyRing(keyId)
    }

    fun listKeys(): List<KeyInfo> {
        return keyStore.listStoredKeys().map { stored ->
            KeyInfo(
                keyId = stored.keyId,
                userId = stored.userId,
                created = 0L,
                algorithm = ""
            )
        }
    }

    private fun keyInfo(keyId: Long, keyRing: org.bouncycastle.openpgp.PGPSecretKeyRing): KeyInfo {
        val info = PGPainless.inspectKeyRing(keyRing)
        return KeyInfo(
            keyId = keyId,
            userId = info.userIds.firstOrNull() ?: "",
            created = info.creationDate.time,
            algorithm = info.algorithm.name
        )
    }
}
