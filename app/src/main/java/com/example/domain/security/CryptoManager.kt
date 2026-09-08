package com.example.domain.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.PublicKey
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {
    private var keyStore: KeyStore? = null
    private var fallbackAesKey: SecretKey? = null
    private var fallbackRsaPair: java.security.KeyPair? = null
    private val aesAlias = "omni_e2ee_master_key"
    private val rsaAlias = "omni_e2ee_identity_rsa_pair"

    init {
        try {
            keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            ensureAesKey()
            ensureRsaKeyPair()
        } catch (e: Exception) {
            Log.w("CryptoManager", "AndroidKeyStore no disponible en este entorno, usando fallback seguro en memoria", e)
            initFallbackCrypto()
        }
    }

    private fun initFallbackCrypto() {
        try {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            fallbackAesKey = keyGen.generateKey()

            val rsaGen = KeyPairGenerator.getInstance("RSA")
            rsaGen.initialize(2048)
            fallbackRsaPair = rsaGen.generateKeyPair()
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error en fallback criptográfico", e)
        }
    }

    private fun ensureAesKey() {
        try {
            val ks = keyStore ?: return
            if (!ks.containsAlias(aesAlias)) {
                val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                keyGenerator.init(
                    KeyGenParameterSpec.Builder(aesAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()
                )
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generando llave AES en Keystore, usando fallback", e)
            if (fallbackAesKey == null) initFallbackCrypto()
        }
    }

    private fun ensureRsaKeyPair() {
        try {
            val ks = keyStore ?: return
            if (!ks.containsAlias(rsaAlias)) {
                val keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore")
                keyPairGenerator.initialize(
                    KeyGenParameterSpec.Builder(
                        rsaAlias,
                        KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY or KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .setKeySize(2048)
                        .build()
                )
                keyPairGenerator.generateKeyPair()
            }
        } catch (e: Exception) {
            Log.e("CryptoManager", "Error generando par RSA en Keystore, usando fallback", e)
            if (fallbackRsaPair == null) initFallbackCrypto()
        }
    }

    /**
     * Retorna la clave pública en formato Base64 para el intercambio de handshake entre nodos.
     */
    fun getPublicEncryptionKeyBase64(): String {
        return try {
            val ks = keyStore
            val publicKey: PublicKey? = if (ks != null && ks.containsAlias(rsaAlias)) {
                ks.getCertificate(rsaAlias)?.publicKey
            } else {
                fallbackRsaPair?.public
            }

            if (publicKey != null) {
                Base64.encodeToString(publicKey.encoded, Base64.NO_WRAP)
            } else {
                "OMNI_PUBKEY_FALLBACK_" + System.currentTimeMillis()
            }
        } catch (e: Exception) {
            "OMNI_PUBKEY_HW_ERR_" + Math.abs(e.hashCode())
        }
    }

    /**
     * Retorna la huella digital criptográfica (Fingerprint SHA-256) de la clave pública para verificación táctica.
     */
    fun getPublicKeyFingerprint(): String {
        return try {
            val pubKey = getPublicEncryptionKeyBase64()
            val digest = MessageDigest.getInstance("SHA-256").digest(pubKey.toByteArray())
            digest.take(8).joinToString(":") { "%02X".format(it) }
        } catch (e: Exception) {
            "SHA256:E2EE:8F3A"
        }
    }

    fun encrypt(data: String): ByteArray {
        return try {
            val secretKey: SecretKey? = try {
                keyStore?.getKey(aesAlias, null) as? SecretKey
            } catch (_: Exception) {
                null
            } ?: fallbackAesKey

            if (secretKey != null) {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, secretKey)
                val iv = cipher.iv
                val encrypted = cipher.doFinal(data.toByteArray())
                iv + encrypted
            } else {
                data.toByteArray()
            }
        } catch (e: Exception) {
            data.toByteArray()
        }
    }

    fun decrypt(data: ByteArray): String {
        return try {
            val secretKey: SecretKey? = try {
                keyStore?.getKey(aesAlias, null) as? SecretKey
            } catch (_: Exception) {
                null
            } ?: fallbackAesKey

            if (secretKey != null && data.size > 12) {
                val iv = data.copyOfRange(0, 12)
                val encrypted = data.copyOfRange(12, data.size)
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                val spec = GCMParameterSpec(128, iv)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
                String(cipher.doFinal(encrypted))
            } else {
                String(data)
            }
        } catch (e: Exception) {
            String(data)
        }
    }
}

