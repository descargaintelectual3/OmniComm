package com.example.domain.security

import android.util.Base64
import android.util.Log
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Carga útil cifrada con el protocolo Double Ratchet (Signal Protocol).
 * Contiene la clave pública efímera DH actual, el número de secuencia en la cadena y el texto cifrado.
 */
data class RatchetEncryptedMessage(
    val senderIdentityFingerprint: String,
    val senderEphemeralDhPublicKeyBase64: String,
    val chainStep: Int,
    val ivBase64: String,
    val cipherTextBase64: String,
    val hmacAuthTagBase64: String
)

/**
 * Estado serializable de una sesión Double Ratchet bilateral activa con un contacto.
 */
data class DoubleRatchetSession(
    val peerId: String,
    var rootKey: ByteArray, // 32 bytes
    var ourSendingChainKey: ByteArray, // 32 bytes
    var ourReceivingChainKey: ByteArray, // 32 bytes
    var ourCurrentEphemeralKeyPair: KeyPair,
    var peerCurrentEphemeralPublicKey: PublicKey?,
    var sendingStep: Int = 0,
    var receivingStep: Int = 0,
    var safetyNumber: String = ""
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as DoubleRatchetSession
        return peerId == other.peerId && sendingStep == other.sendingStep
    }

    override fun hashCode(): Int {
        var result = peerId.hashCode()
        result = 31 * result + sendingStep
        return result
    }
}

/**
 * Implementación pura del Protocolo Double Ratchet (Signal Protocol) para Android:
 * - KDF Ratchet simétrico basado en HMAC-SHA256 para Forward Secrecy por mensaje.
 * - DH Ratchet asimétrico basado en ECDH (secp256r1) para Post-Compromise Security (recuperación automática de clave).
 * - Cifrado autenticado AES-256-GCM.
 * - Cálculo de Números de Seguridad (Safety Numbers) fuera de banda de 60 dígitos / 12 bloques.
 */
class DoubleRatchetEngine private constructor() {

    private val sessions = mutableMapOf<String, DoubleRatchetSession>()
    private val keyPairGenerator: KeyPairGenerator = KeyPairGenerator.getInstance("EC").apply {
        initialize(256)
    }
    private val keyFactory: KeyFactory = KeyFactory.getInstance("EC")

    companion object {
        private const val TAG = "DoubleRatchet"
        private val KDF_SALT = "OmniComm_Double_Ratchet_HKDF_Salt_2026".toByteArray(Charsets.UTF_8)
        private val CONST_MESSAGE_KEY = byteArrayOf(0x01)
        private val CONST_NEXT_CHAIN_KEY = byteArrayOf(0x02)

        @Volatile
        private var INSTANCE: DoubleRatchetEngine? = null

        fun getInstance(): DoubleRatchetEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DoubleRatchetEngine().also { INSTANCE = it }
            }
        }
    }

    /**
     * Inicializa una sesión Double Ratchet como INICIADOR (Alice)
     */
    fun initSessionAsInitiator(peerId: String, peerStaticPublicKey: PublicKey, sharedRootSecret: ByteArray): DoubleRatchetSession {
        val ourEphemeral = keyPairGenerator.generateKeyPair()
        val dhOutput = performECDH(ourEphemeral.private, peerStaticPublicKey)
        val (newRootKey, sendingChainKey) = kdfRoot(sharedRootSecret, dhOutput)

        val session = DoubleRatchetSession(
            peerId = peerId,
            rootKey = newRootKey,
            ourSendingChainKey = sendingChainKey,
            ourReceivingChainKey = ByteArray(32),
            ourCurrentEphemeralKeyPair = ourEphemeral,
            peerCurrentEphemeralPublicKey = peerStaticPublicKey,
            sendingStep = 0,
            receivingStep = 0,
            safetyNumber = computeSafetyNumber(ourEphemeral.public, peerStaticPublicKey)
        )
        sessions[peerId] = session
        Log.i(TAG, "Sesión Double Ratchet inicializada como INICIADOR con nodo $peerId")
        return session
    }

    /**
     * Inicializa una sesión Double Ratchet como RECEPTOR (Bob)
     */
    fun initSessionAsResponder(peerId: String, ourKeyPair: KeyPair, sharedRootSecret: ByteArray): DoubleRatchetSession {
        val session = DoubleRatchetSession(
            peerId = peerId,
            rootKey = sharedRootSecret,
            ourSendingChainKey = ByteArray(32),
            ourReceivingChainKey = ByteArray(32),
            ourCurrentEphemeralKeyPair = ourKeyPair,
            peerCurrentEphemeralPublicKey = null,
            sendingStep = 0,
            receivingStep = 0,
            safetyNumber = "PENDIENTE_HANDSHAKE"
        )
        sessions[peerId] = session
        Log.i(TAG, "Sesión Double Ratchet inicializada como RECEPTOR con nodo $peerId")
        return session
    }

    fun getSession(peerId: String): DoubleRatchetSession? = sessions[peerId]

    /**
     * Cifra un mensaje de texto saliente avanzando el trinquete simétrico (Symmetric Ratchet)
     */
    @Synchronized
    fun ratchetEncrypt(peerId: String, plainText: String, senderFingerprint: String): RatchetEncryptedMessage {
        var session = sessions[peerId]
        if (session == null) {
            // Generar sesión por defecto con semilla compartida determinista para la malla
            val defaultSeed = MessageDigest.getInstance("SHA-256").digest(("SEED_OMNI_" + peerId).toByteArray())
            val ourEphemeral = keyPairGenerator.generateKeyPair()
            session = initSessionAsInitiator(peerId, ourEphemeral.public, defaultSeed)
        }

        // 1. Derivar Message Key (MK) y siguiente Chain Key (CK)
        val (messageKey, nextChainKey) = kdfChain(session.ourSendingChainKey)
        session.ourSendingChainKey = nextChainKey
        session.sendingStep += 1

        // 2. Cifrar con AES-256-GCM usando el Message Key
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val secretKey = SecretKeySpec(messageKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ephemeralPubBase64 = Base64.encodeToString(session.ourCurrentEphemeralKeyPair.public.encoded, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherTextBase64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)

        // 3. Autenticación HMAC de cabecera para detección de manipulación
        val hmac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(messageKey, "HmacSHA256"))
        }
        val tag = hmac.doFinal(cipherBytes)
        val hmacTagBase64 = Base64.encodeToString(tag, Base64.NO_WRAP)

        return RatchetEncryptedMessage(
            senderIdentityFingerprint = senderFingerprint,
            senderEphemeralDhPublicKeyBase64 = ephemeralPubBase64,
            chainStep = session.sendingStep,
            ivBase64 = ivBase64,
            cipherTextBase64 = cipherTextBase64,
            hmacAuthTagBase64 = hmacTagBase64
        )
    }

    /**
     * Descifra un mensaje entrante. Si detecta una nueva clave efímera, avanza el Trinquete DH (DH Ratchet)
     */
    @Synchronized
    fun ratchetDecrypt(peerId: String, payload: RatchetEncryptedMessage): Result<String> {
        return try {
            var session = sessions[peerId]
            if (session == null) {
                val defaultSeed = MessageDigest.getInstance("SHA-256").digest(("SEED_OMNI_" + peerId).toByteArray())
                val ourKeyPair = keyPairGenerator.generateKeyPair()
                session = initSessionAsResponder(peerId, ourKeyPair, defaultSeed)
            }

            // Reconstruir clave pública efímera del remitente
            val peerEphemeralBytes = Base64.decode(payload.senderEphemeralDhPublicKeyBase64, Base64.NO_WRAP)
            val peerEphemeralPub = keyFactory.generatePublic(X509EncodedKeySpec(peerEphemeralBytes))

            // Si la clave efímera remota cambió o no existe, avanzar el Diffie-Hellman Ratchet (DH Step)
            if (session.peerCurrentEphemeralPublicKey == null || !session.peerCurrentEphemeralPublicKey!!.encoded.contentEquals(peerEphemeralPub.encoded)) {
                // Paso 1 DH: Trinquete de recepción
                val dhReceive = performECDH(session.ourCurrentEphemeralKeyPair.private, peerEphemeralPub)
                val (newRoot1, receivingChainKey) = kdfRoot(session.rootKey, dhReceive)
                session.rootKey = newRoot1
                session.ourReceivingChainKey = receivingChainKey
                session.peerCurrentEphemeralPublicKey = peerEphemeralPub

                // Paso 2 DH: Generar nuevo par efímero local y trinquete de envío
                val newOurEphemeral = keyPairGenerator.generateKeyPair()
                val dhSend = performECDH(newOurEphemeral.private, peerEphemeralPub)
                val (newRoot2, sendingChainKey) = kdfRoot(session.rootKey, dhSend)
                session.rootKey = newRoot2
                session.ourSendingChainKey = sendingChainKey
                session.ourCurrentEphemeralKeyPair = newOurEphemeral

                session.receivingStep = 0
                session.safetyNumber = computeSafetyNumber(newOurEphemeral.public, peerEphemeralPub)
                Log.d(TAG, "DH Ratchet avanzado exitosamente para contacto $peerId")
            }

            // Derivar Message Key de la cadena de recepción
            val (messageKey, nextChainKey) = kdfChain(session.ourReceivingChainKey)
            session.ourReceivingChainKey = nextChainKey
            session.receivingStep += 1

            // Descifrar con AES-256-GCM
            val iv = Base64.decode(payload.ivBase64, Base64.NO_WRAP)
            val cipherBytes = Base64.decode(payload.cipherTextBase64, Base64.NO_WRAP)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(messageKey, "AES"), GCMParameterSpec(128, iv))
            val plainBytes = cipher.doFinal(cipherBytes)

            Result.success(String(plainBytes, Charsets.UTF_8))
        } catch (e: Exception) {
            Log.e(TAG, "Error al descifrar con Double Ratchet: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Cálculo de Diffie-Hellman en curva elíptica
     */
    private fun performECDH(privateKey: PrivateKey, publicKey: PublicKey): ByteArray {
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        return agreement.generateSecret()
    }

    /**
     * KDF para la clave raíz (Root Key): combina clave raíz anterior y salida DH
     */
    private fun kdfRoot(rootKey: ByteArray, dhSecret: ByteArray): Pair<ByteArray, ByteArray> {
        val mac = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(rootKey.ifEmpty { KDF_SALT }, "HmacSHA256"))
        }
        val prk = mac.doFinal(dhSecret)

        val macDerive1 = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(prk, "HmacSHA256")) }
        macDerive1.update("DoubleRatchet_Root".toByteArray())
        macDerive1.update(0x01.toByte())
        val newRoot = macDerive1.doFinal()

        val macDerive2 = Mac.getInstance("HmacSHA256").apply { init(SecretKeySpec(prk, "HmacSHA256")) }
        macDerive2.update(newRoot)
        macDerive2.update("DoubleRatchet_Chain".toByteArray())
        macDerive2.update(0x02.toByte())
        val newChain = macDerive2.doFinal()

        return Pair(newRoot, newChain)
    }

    /**
     * KDF simétrico para avance de cadena (Symmetric Ratchet): genera MessageKey y actualiza ChainKey
     */
    private fun kdfChain(chainKey: ByteArray): Pair<ByteArray, ByteArray> {
        val effectiveKey = if (chainKey.isEmpty() || chainKey.all { it == 0.toByte() }) {
            KDF_SALT
        } else chainKey

        val macMessage = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(effectiveKey, "HmacSHA256"))
        }
        val messageKey = macMessage.doFinal(CONST_MESSAGE_KEY)

        val macNextChain = Mac.getInstance("HmacSHA256").apply {
            init(SecretKeySpec(effectiveKey, "HmacSHA256"))
        }
        val nextChainKey = macNextChain.doFinal(CONST_NEXT_CHAIN_KEY)

        return Pair(messageKey, nextChainKey)
    }

    /**
     * Genera el Número de Seguridad Fuera de Banda (Safety Number) de 12 bloques de 5 dígitos (60 dígitos)
     * para verificación visual y prevención de ataques MITM.
     */
    fun computeSafetyNumber(key1: PublicKey, key2: PublicKey): String {
        val digest = MessageDigest.getInstance("SHA-512")
        // Ordenar claves canónicamente para que ambos extremos generen exactamente el mismo número
        val enc1 = key1.encoded
        val enc2 = key2.encoded
        if (compareByteArrays(enc1, enc2) <= 0) {
            digest.update(enc1)
            digest.update(enc2)
        } else {
            digest.update(enc2)
            digest.update(enc1)
        }
        val hash = digest.digest()

        val blocks = mutableListOf<String>()
        for (i in 0 until 12) {
            val offset = i * 4
            val value = ((hash[offset].toInt() and 0xFF) shl 24) or
                    ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                    ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                    (hash[offset + 3].toInt() and 0xFF)
            val num = Math.abs(value) % 100000
            blocks.add(String.format("%05d", num))
        }
        return blocks.chunked(3).joinToString("\n") { it.joinToString(" ") }
    }

    private fun compareByteArrays(a: ByteArray, b: ByteArray): Int {
        val minLen = minOf(a.size, b.size)
        for (i in 0 until minLen) {
            val cmp = (a[i].toInt() and 0xFF).compareTo(b[i].toInt() and 0xFF)
            if (cmp != 0) return cmp
        }
        return a.size.compareTo(b.size)
    }

    private fun ByteArray.ifEmpty(defaultProvider: () -> ByteArray): ByteArray {
        return if (this.isEmpty() || this.all { it == 0.toByte() }) defaultProvider() else this
    }
}
