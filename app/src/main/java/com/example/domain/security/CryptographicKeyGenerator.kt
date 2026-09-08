package com.example.domain.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import java.security.spec.ECGenParameterSpec
import java.util.UUID

data class GeneratedKeyResult(
    val keyId: String,
    val alias: String,
    val algorithm: String,
    val purpose: String,
    val publicKeyBase64: String,
    val fingerprint: String,
    val keySizeBits: Int,
    val isHardwareBacked: Boolean,
    val notes: String = ""
)

/**
 * Generador avanzado de pares de claves criptográficas para mensajería E2EE táctica.
 * Soporta algoritmos asimétricos estándar (RSA 2048/4096, ECC Secp256r1) y post-cuánticos (Kyber-768/X25519).
 */
object CryptographicKeyGenerator {

    private const val TAG = "CryptoKeyGenerator"

    fun generateRsaKeyPair(
        alias: String,
        keySizeBits: Int = 2048,
        useAndroidKeyStore: Boolean = true,
        purpose: String = "E2EE_MESSAGING"
    ): GeneratedKeyResult {
        val keyId = "KEY-RSA-${UUID.randomUUID().toString().take(6).uppercase()}"
        var isHw = false
        var pubKey: PublicKey? = null

        if (useAndroidKeyStore) {
            try {
                val keyStoreAlias = "omni_rsa_${UUID.randomUUID().toString().take(8)}"
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                kpg.initialize(
                    KeyGenParameterSpec.Builder(
                        keyStoreAlias,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setKeySize(keySizeBits)
                        .build()
                )
                val kp = kpg.generateKeyPair()
                pubKey = kp.public
                isHw = true
            } catch (e: Exception) {
                Log.w(TAG, "AndroidKeyStore no disponible para RSA-$keySizeBits, usando generador software: ${e.message}")
            }
        }

        if (pubKey == null) {
            val kpg = KeyPairGenerator.getInstance("RSA")
            kpg.initialize(keySizeBits)
            val kp = kpg.generateKeyPair()
            pubKey = kp.public
            isHw = false
        }

        val pubBytes = pubKey.encoded
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val fingerprint = computeFingerprint(pubBytes)

        DiscoveryLogCollector.log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = LogSeverity.INFO,
            tag = TAG,
            message = "Par de claves RSA-$keySizeBits generado exitosamente: $keyId (HW=$isHw)"
        )

        return GeneratedKeyResult(
            keyId = keyId,
            alias = alias.ifBlank { "Par RSA-$keySizeBits E2EE" },
            algorithm = "RSA-$keySizeBits",
            purpose = purpose,
            publicKeyBase64 = pubBase64,
            fingerprint = fingerprint,
            keySizeBits = keySizeBits,
            isHardwareBacked = isHw,
            notes = if (isHw) "Residenciado en Android KeyStore TEE" else "Generador de software seguro"
        )
    }

    fun generateEcKeyPair(
        alias: String,
        useAndroidKeyStore: Boolean = true,
        purpose: String = "E2EE_MESSAGING"
    ): GeneratedKeyResult {
        val keyId = "KEY-ECC-${UUID.randomUUID().toString().take(6).uppercase()}"
        var isHw = false
        var pubKey: PublicKey? = null

        if (useAndroidKeyStore) {
            try {
                val keyStoreAlias = "omni_ecc_${UUID.randomUUID().toString().take(8)}"
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, "AndroidKeyStore")
                kpg.initialize(
                    KeyGenParameterSpec.Builder(
                        keyStoreAlias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_AGREE_KEY
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                        .build()
                )
                val kp = kpg.generateKeyPair()
                pubKey = kp.public
                isHw = true
            } catch (e: Exception) {
                Log.w(TAG, "AndroidKeyStore no disponible para EC-P256, usando generador software: ${e.message}")
            }
        }

        if (pubKey == null) {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val kp = kpg.generateKeyPair()
            pubKey = kp.public
            isHw = false
        }

        val pubBytes = pubKey.encoded
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val fingerprint = computeFingerprint(pubBytes)

        DiscoveryLogCollector.log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = LogSeverity.INFO,
            tag = TAG,
            message = "Par de claves Curva Elíptica secp256r1 generado: $keyId (HW=$isHw)"
        )

        return GeneratedKeyResult(
            keyId = keyId,
            alias = alias.ifBlank { "Par ECC Secp256r1 E2EE" },
            algorithm = "EC-P256",
            purpose = purpose,
            publicKeyBase64 = pubBase64,
            fingerprint = fingerprint,
            keySizeBits = 256,
            isHardwareBacked = isHw,
            notes = if (isHw) "Curva Elíptica NIST P-256 en Hardware TEE" else "Curva Elíptica software seguro"
        )
    }

    fun generatePostQuantumKyberKey(
        alias: String,
        purpose: String = "POST_QUANTUM"
    ): GeneratedKeyResult {
        val pqc = PostQuantumKyberVault.generateHybridKeyPair()
        val pubBytes = pqc.publicKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val pubBase64 = Base64.encodeToString(pubBytes, Base64.NO_WRAP)
        val fingerprint = computeFingerprint(pubBytes)

        return GeneratedKeyResult(
            keyId = pqc.keyId,
            alias = alias.ifBlank { "Bóveda Híbrida Kyber-768/X25519" },
            algorithm = "KYBER-768-PQC",
            purpose = purpose,
            publicKeyBase64 = pubBase64,
            fingerprint = fingerprint,
            keySizeBits = 768,
            isHardwareBacked = false,
            notes = "NIST PQC ML-KEM FIPS 203 Cuántico-Resistente"
        )
    }

    fun computeFingerprint(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
            digest.take(12).joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "SHA256:ERR:" + Math.abs(bytes.hashCode()).toString(16).uppercase()
        }
    }

    fun computeFingerprintFromBase64(base64Str: String): String {
        return try {
            val bytes = Base64.decode(base64Str.trim(), Base64.DEFAULT)
            computeFingerprint(bytes)
        } catch (e: Exception) {
            "SHA256:RAW:" + Math.abs(base64Str.hashCode()).toString(16).uppercase().take(8)
        }
    }
}
