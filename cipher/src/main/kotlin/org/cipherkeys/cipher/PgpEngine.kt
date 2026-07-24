package org.cipherkeys.cipher

import org.bouncycastle.openpgp.PGPPublicKeyRing
import org.bouncycastle.openpgp.PGPSecretKeyRing
import org.pgpainless.PGPainless
import org.pgpainless.algorithm.DocumentSignatureType
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
        passphrase: String
    ): String {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val options = ConsumerOptions.get()
            .addDecryptionKey(secretKey, protector)

        return decryptInternal(ciphertext.toByteArray(Charsets.UTF_8), options)
    }

    fun decryptTryAll(
        ciphertext: String,
        secretKeys: List<PGPSecretKeyRing>,
        passphrase: String
    ): String {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val options = ConsumerOptions.get()
        for (key in secretKeys) {
            options.addDecryptionKey(key, protector)
        }
        return decryptInternal(ciphertext.toByteArray(Charsets.UTF_8), options)
    }

    fun sign(
        plaintext: String,
        secretKey: PGPSecretKeyRing,
        passphrase: String
    ): String {
        val protector = SecretKeyRingProtector.unlockAnyKeyWith(
            Passphrase.fromPassword(passphrase)
        )
        val signingOptions = SigningOptions.get()
            .addInlineSignature(
                protector,
                secretKey,
                DocumentSignatureType.CANONICAL_TEXT_DOCUMENT
            )

        val producerOptions = ProducerOptions.sign(signingOptions).apply {
            setAsciiArmor(true)
        }

        val out = ByteArrayOutputStream()
        val stream = PGPainless.encryptAndOrSign()
            .onOutputStream(out)
            .withOptions(producerOptions)

        stream.write(plaintext.toByteArray(Charsets.UTF_8))
        stream.close()
        return String(out.toByteArray())
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
    ): String {
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

        return String(output.toByteArray())
    }
}

data class VerificationResult(
    val keyId: Long,
    val verified: Boolean,
    val algorithm: String,
    val plaintext: String
)
