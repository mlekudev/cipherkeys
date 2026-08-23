package org.cipherkeys.cipher

import android.util.Log
import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.DocumentSignatureType
import org.pgpainless.algorithm.HashAlgorithm
import org.pgpainless.decryption_verification.ConsumerOptions
import org.pgpainless.encryption_signing.EncryptionOptions
import org.pgpainless.encryption_signing.ProducerOptions
import org.pgpainless.encryption_signing.SigningOptions
import org.pgpainless.key.protection.SecretKeyRingProtector
import org.pgpainless.util.Passphrase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class PgpEngine {

    fun encrypt(
        plaintext: String,
        recipients: List<PGPPublicKeyRing>,
        signKey: PGPSecretKeyRing? = null,
        passphrase: String? = null
    ): String {
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()

        val signingOptions = if (signKey != null && passphrase != null) {
            val protector = SecretKeyRingProtector.unlockAnyKeyWith(
                Passphrase.fromPassword(passphrase)
            )
            SigningOptions.get().addInlineSignature(protector, signKey)
        } else {
            null
        }

        val encryptionOptions = EncryptionOptions()
        encryptionOptions.setAllowEncryptionWithMissingKeyFlags()
        for (recipient in recipients) {
            encryptionOptions.addRecipient(recipient)
        }

        val producerOptions = if (signingOptions != null) {
            ProducerOptions.signAndEncrypt(encryptionOptions, signingOptions)
        } else {
            ProducerOptions.encrypt(encryptionOptions)
        }

        val encryptionStream = PGPainless.encryptAndOrSign()
            .onOutputStream(out)
            .withOptions(producerOptions)

        encryptionStream.write(plainBytes)
        encryptionStream.close()
        return String(out.toByteArray())
    }

    fun encryptArmored(
        plaintext: String,
        recipients: List<PGPPublicKeyRing>,
        signKey: PGPSecretKeyRing? = null,
        passphrase: String? = null
    ): String {
        val plainBytes = plaintext.toByteArray(Charsets.UTF_8)
        val out = ByteArrayOutputStream()

        val signingOptions = if (signKey != null && passphrase != null) {
            val protector = SecretKeyRingProtector.unlockAnyKeyWith(
                Passphrase.fromPassword(passphrase)
            )
            SigningOptions.get().addInlineSignature(protector, signKey)
        } else {
            null
        }

        val encryptionOptions = EncryptionOptions()
        encryptionOptions.setAllowEncryptionWithMissingKeyFlags()
        for (recipient in recipients) {
            encryptionOptions.addRecipient(recipient)
        }

        val producerOptions = if (signingOptions != null) {
            ProducerOptions.signAndEncrypt(encryptionOptions, signingOptions)
        } else {
            ProducerOptions.encrypt(encryptionOptions)
        }.apply { setAsciiArmor(true) }

        val encryptionStream = PGPainless.encryptAndOrSign()
            .onOutputStream(out)
            .withOptions(producerOptions)

        encryptionStream.write(plainBytes)
        encryptionStream.close()
        return String(out.toByteArray())
    }

    fun decrypt(
        ciphertext: String,
        secretKey: PGPSecretKeyRing,
        passphrase: String,
        verificationCerts: List<PGPPublicKeyRing> = emptyList()
    ): DecryptResult {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val options = ConsumerOptions.get()
            .addDecryptionKey(secretKey, protector)
        for (cert in verificationCerts) options.addVerificationCert(cert)

        return decryptInternal(ciphertext.toByteArray(Charsets.UTF_8), options)
    }

    fun decryptTryAll(
        ciphertext: String,
        secretKeys: List<PGPSecretKeyRing>,
        passphrase: String,
        verificationCerts: List<PGPPublicKeyRing> = emptyList()
    ): DecryptResult {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val options = ConsumerOptions.get()
        for (key in secretKeys) {
            options.addDecryptionKey(key, protector)
        }
        for (cert in verificationCerts) options.addVerificationCert(cert)
        return decryptInternal(ciphertext.toByteArray(Charsets.UTF_8), options)
    }

    fun sign(
        plaintext: String,
        secretKey: PGPSecretKeyRing,
        passphrase: String,
        expirySeconds: Long = 0L,
        alreadyExpired: Boolean = false
    ): String {
        val text = if (expirySeconds > 0 || alreadyExpired) {
            val exp = if (alreadyExpired) System.currentTimeMillis() / 1000 - 1
                      else System.currentTimeMillis() / 1000 + expirySeconds
            Log.d("PgpEngine", "sign with expiry=$expirySeconds sec (epoch $exp, past=$alreadyExpired) for text len=${plaintext.length}")
            "[CipherKeys:expires=$exp]\n$plaintext"
        } else {
            Log.d("PgpEngine", "sign without expiry for text len=${plaintext.length}")
            plaintext
        }

        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val signingOptions = SigningOptions.get()
            .overrideHashAlgorithm(HashAlgorithm.SHA256)
            .addDetachedSignature(
                protector,
                secretKey,
                DocumentSignatureType.CANONICAL_TEXT_DOCUMENT
            )

        val producerOptions = ProducerOptions.sign(signingOptions).apply {
            setAsciiArmor(true)
            setCleartextSigned()
        }

        val out = ByteArrayOutputStream()
        val stream = PGPainless.encryptAndOrSign()
            .onOutputStream(out)
            .withOptions(producerOptions)

        stream.write(text.toByteArray(Charsets.UTF_8))
        stream.close()
        val result = String(out.toByteArray())
        Log.d("PgpEngine", "sign output len=${result.length}")
        return result
    }

    fun verifySigned(
        signedMessage: String,
        publicKeys: List<PGPPublicKeyRing>
    ): VerificationResult? {
        if (publicKeys.isEmpty()) {
            Log.d("PgpEngine", "verifySigned: no public keys")
            return null
        }
        Log.d("PgpEngine", "verifySigned: msgLen=${signedMessage.length}, numKeys=${publicKeys.size}")
        try {
            val options = ConsumerOptions.get()
            for (key in publicKeys) {
                options.addVerificationCert(key)
            }
            val input = ByteArrayInputStream(signedMessage.toByteArray(Charsets.UTF_8))
            val output = ByteArrayOutputStream()
            val stream = PGPainless.decryptAndOrVerify()
                .onInputStream(input)
                .withOptions(options)
            val buf = ByteArray(4096)
            var n: Int
            while (stream.read(buf).also { n = it } != -1) output.write(buf, 0, n)
            stream.close()
            val meta = stream.metadata
            if (meta.verifiedSignatures.isNotEmpty()) {
                val sig = meta.verifiedSignatures.first()
                return VerificationResult(
                    keyId = sig.signingKey.fingerprint.keyId,
                    verified = true,
                    algorithm = sig.signature.hashAlgorithm.toString(),
                    plaintext = String(output.toByteArray())
                )
            }
        } catch (e: Exception) {
            Log.e("PgpEngine", "verifySigned exception", e)
        }
        return null
    }

    fun extractPlaintext(signedMessage: String): String? {
        val marker = "-----BEGIN PGP SIGNED MESSAGE-----"
        val sigMarker = "-----BEGIN PGP SIGNATURE-----"
        val idx = signedMessage.indexOf(marker)
        if (idx < 0) return null
        val afterMarker = signedMessage.substring(idx + marker.length)
        val sigIdx = afterMarker.indexOf(sigMarker)
        if (sigIdx < 0) return null
        val body = afterMarker.substring(0, sigIdx)
        val lines = body.lines()
            .dropWhile { it.startsWith("Hash:") || it.isBlank() }
            .dropLastWhile { it.isBlank() }
        return lines.joinToString("\n")
    }

    fun checkExpired(plaintext: String): Boolean {
        val prefix = "[CipherKeys:expires="
        val idx = plaintext.indexOf(prefix)
        if (idx < 0) return false
        val endIdx = plaintext.indexOf(']', idx)
        if (endIdx < 0) return false
        val ts = plaintext.substring(idx + prefix.length, endIdx).toLongOrNull() ?: return false
        return System.currentTimeMillis() / 1000 > ts
    }

    fun stripExpiration(plaintext: String): String {
        val firstLine = plaintext.lines().firstOrNull() ?: return plaintext
        if (firstLine.startsWith("[CipherKeys:expires=")) {
            return plaintext.substringAfter("\n").trimStart()
        }
        return plaintext
    }

    fun verify(
        signedMessage: String,
        publicKey: PGPPublicKeyRing
    ): List<VerificationResult> {
        val results = mutableListOf<VerificationResult>()

        val options = ConsumerOptions.get()
            .addVerificationCert(publicKey)

        val input = ByteArrayInputStream(signedMessage.toByteArray(Charsets.UTF_8))
        val output = ByteArrayOutputStream()

        val stream = PGPainless.decryptAndOrVerify()
            .onInputStream(input)
            .withOptions(options)

        stream.use { decryptionStream ->
            val buf = ByteArray(4096)
            var n: Int
            while (decryptionStream.read(buf).also { n = it } != -1) {
                output.write(buf, 0, n)
            }

            val metadata = decryptionStream.metadata
            for (sig in metadata.verifiedSignatures) {
                results.add(
                    VerificationResult(
                        keyId = sig.signingKey.fingerprint.keyId,
                        verified = true,
                        algorithm = sig.signature.hashAlgorithm.toString(),
                        plaintext = String(output.toByteArray())
                    )
                )
            }
        }

        return results
    }

    fun verifyMultiple(
        signedMessage: String,
        publicKeys: List<PGPPublicKeyRing>
    ): List<VerificationResult> {
        val results = mutableListOf<VerificationResult>()

        val options = ConsumerOptions.get()
        for (key in publicKeys) {
            options.addVerificationCert(key)
        }

        val input = ByteArrayInputStream(signedMessage.toByteArray(Charsets.UTF_8))
        val output = ByteArrayOutputStream()

        val stream = PGPainless.decryptAndOrVerify()
            .onInputStream(input)
            .withOptions(options)

        stream.use { decryptionStream ->
            val buf = ByteArray(4096)
            var n: Int
            while (decryptionStream.read(buf).also { n = it } != -1) {
                output.write(buf, 0, n)
            }

            val metadata = decryptionStream.metadata
            for (sig in metadata.verifiedSignatures) {
                results.add(
                    VerificationResult(
                        keyId = sig.signingKey.fingerprint.keyId,
                        verified = true,
                        algorithm = sig.signature.hashAlgorithm.toString(),
                        plaintext = String(output.toByteArray())
                    )
                )
            }
        }

        return results
    }

    private fun decryptInternal(
        cipherBytes: ByteArray,
        consumerOptions: ConsumerOptions
    ): DecryptResult {
        val input = ByteArrayInputStream(cipherBytes)
        val output = ByteArrayOutputStream()

        val stream = PGPainless.decryptAndOrVerify()
            .onInputStream(input)
            .withOptions(consumerOptions)

        val buf = ByteArray(4096)
        var n: Int
        while (stream.read(buf).also { n = it } != -1) {
            output.write(buf, 0, n)
        }
        stream.close()

        if (!stream.metadata.isEncrypted) {
            throw CipherException("decryption failed - data is not encrypted")
        }

        val signerKeyId = stream.metadata.verifiedSignatures.firstOrNull()
            ?.signingKey?.fingerprint?.keyId
        return DecryptResult(String(output.toByteArray()), signerKeyId)
    }
}

data class DecryptResult(
    val plaintext: String,
    val signerKeyId: Long?
)

data class VerificationResult(
    val keyId: Long,
    val verified: Boolean,
    val algorithm: String,
    val plaintext: String
)
