package com.example.domain.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.*
import java.security.spec.ECGenParameterSpec
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Resultado de verificación criptográfica de una orden C2
 */
data class C2VerificationResult(
    val isValid: Boolean,
    val reason: String,
    val signerFingerprint: String = "",
    val ageMs: Long = 0L,
    val isReplayDetected: Boolean = false
)

/**
 * Sobre C2 Firmado Digitalmente
 */
data class SignedC2Payload(
    val commandId: String,
    val commandType: String,
    val targetNodeId: String,
    val payloadArgs: String,
    val timestampMs: Long,
    val nonce: String,
    val signerFingerprint: String,
    val signatureBase64: String
)

/**
 * Estado Operativo del Verificador C2 Anti-Replay
 */
data class C2SecurityState(
    val totalSignedCommands: Int = 0,
    val totalVerifiedValid: Int = 0,
    val totalRejectedSpoofed: Int = 0,
    val totalReplayAttacksBlocked: Int = 0,
    val isHardwareKeyStoreActive: Boolean = false,
    val localKeyFingerprint: String = "",
    val securityLogs: List<String> = emptyList()
)

/**
 * Sistema de Firma Criptográfica Digital y Verificación Anti-Replay para Órdenes C2
 * Utiliza ECDSA (NIST P-256 / SHA-256) respaldado en Android KeyStore con StrongBox
 * para impedir inyección, falsificación o repetición (replay) de órdenes de mando en la malla.
 */
object TacticalC2SignatureVerifier {

    private const val TAG = "C2SignatureVerifier"
    private const val KEY_ALIAS = "TACTICAL_C2_COMMAND_KEY_P256"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val MAX_CLOCK_DRIFT_MS = 15_000L  // 15s de deriva de reloj
    private const val MAX_COMMAND_AGE_MS = 45_000L  // 45s de ventana máxima de validez

    // Caché de nonces ya vistos para prevenir replay attacks
    private val seenNonces = ConcurrentHashMap<String, Long>()

    private val _state = MutableStateFlow(C2SecurityState())
    val state: StateFlow<C2SecurityState> = _state.asStateFlow()

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val kpg = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                )
                    .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
                    .setDigests(KeyProperties.DIGEST_SHA256)
                    .setUserAuthenticationRequired(false)
                    .build()

                kpg.initialize(spec)
                kpg.generateKeyPair()
            }

            val cert = keyStore.getCertificate(KEY_ALIAS)
            val pubKey = cert.publicKey
            val fingerprint = computeFingerprint(pubKey.encoded)

            _state.value = _state.value.copy(
                isHardwareKeyStoreActive = true,
                localKeyFingerprint = fingerprint
            )
            addLog("Clave ECDSA P-256 inicializada en KeyStore -> Huella: $fingerprint")
        } catch (e: Exception) {
            Log.w(TAG, "Hardware KeyStore no disponible, utilizando proveedor de fallback en memoria", e)
            initFallbackSoftwareKey()
        }
    }

    private var fallbackKeyPair: KeyPair? = null

    private fun initFallbackSoftwareKey() {
        try {
            val kpg = KeyPairGenerator.getInstance("EC")
            kpg.initialize(ECGenParameterSpec("secp256r1"))
            val pair = kpg.generateKeyPair()
            fallbackKeyPair = pair
            val fingerprint = computeFingerprint(pair.public.encoded)
            _state.value = _state.value.copy(
                isHardwareKeyStoreActive = false,
                localKeyFingerprint = fingerprint
            )
            addLog("Clave software fallback P-256 inicializada -> Huella: $fingerprint")
        } catch (e: Exception) {
            Log.e(TAG, "Error generando clave software fallback", e)
        }
    }

    /**
     * Firma digitalmente una orden C2 saliente
     */
    fun signC2Command(commandId: String, commandType: String, targetNodeId: String, args: String): SignedC2Payload {
        val timestamp = System.currentTimeMillis()
        val nonce = UUID.randomUUID().toString().take(12)
        val rawToSign = "$commandId|$commandType|$targetNodeId|$args|$timestamp|$nonce"

        val signatureBytes = signData(rawToSign.toByteArray(Charsets.UTF_8))
        val signatureBase64 = Base64.encodeToString(signatureBytes, Base64.NO_WRAP)

        _state.value = _state.value.copy(
            totalSignedCommands = _state.value.totalSignedCommands + 1
        )
        addLog("Orden C2 firmada: $commandType -> $targetNodeId (Nonce: $nonce)")

        return SignedC2Payload(
            commandId = commandId,
            commandType = commandType,
            targetNodeId = targetNodeId,
            payloadArgs = args,
            timestampMs = timestamp,
            nonce = nonce,
            signerFingerprint = _state.value.localKeyFingerprint,
            signatureBase64 = signatureBase64
        )
    }

    /**
     * Verifica la autenticidad, integridad temporal y anti-repetición de una orden C2 entrante
     */
    fun verifyC2Payload(payload: SignedC2Payload, expectedPublicKey: PublicKey? = null): C2VerificationResult {
        val now = System.currentTimeMillis()
        val age = now - payload.timestampMs

        // 1. Detección Anti-Replay de Nonce
        if (seenNonces.containsKey(payload.nonce)) {
            _state.value = _state.value.copy(
                totalReplayAttacksBlocked = _state.value.totalReplayAttacksBlocked + 1
            )
            addLog("ALERTA: ATAQUE REPLAY DETECTADO. Nonce duplicado ${payload.nonce} descartado.")
            DiscoveryLogCollector.log(
                tag = TAG,
                message = "Ataque Replay interceptado para comando ${payload.commandType}",
                category = LogCategory.SYSTEM,
                severity = LogSeverity.ERROR
            )
            return C2VerificationResult(isValid = false, reason = "REPLAY_ATTACK_DETECTED", isReplayDetected = true)
        }

        // 2. Comprobación de Expiración Temporal
        if (age > MAX_COMMAND_AGE_MS) {
            _state.value = _state.value.copy(
                totalRejectedSpoofed = _state.value.totalRejectedSpoofed + 1
            )
            addLog("RECHAZADO: Orden expirada (${age}ms de antigüedad)")
            return C2VerificationResult(isValid = false, reason = "COMMAND_EXPIRED", ageMs = age)
        }

        // 3. Comprobación de Viaje en el Tiempo (Reloj alterado hacia el futuro)
        if (payload.timestampMs - now > MAX_CLOCK_DRIFT_MS) {
            _state.value = _state.value.copy(
                totalRejectedSpoofed = _state.value.totalRejectedSpoofed + 1
            )
            addLog("RECHAZADO: Deriva de reloj futura inválida")
            return C2VerificationResult(isValid = false, reason = "CLOCK_DRIFT_INVALID", ageMs = age)
        }

        // 4. Verificación de Firma Criptográfica
        val rawData = "${payload.commandId}|${payload.commandType}|${payload.targetNodeId}|${payload.payloadArgs}|${payload.timestampMs}|${payload.nonce}".toByteArray(Charsets.UTF_8)
        val sigBytes = try {
            Base64.decode(payload.signatureBase64, Base64.NO_WRAP)
        } catch (e: Exception) {
            return C2VerificationResult(isValid = false, reason = "MALFORMED_SIGNATURE_BASE64")
        }

        val isValidSig = verifySignature(rawData, sigBytes, expectedPublicKey)
        if (!isValidSig) {
            _state.value = _state.value.copy(
                totalRejectedSpoofed = _state.value.totalRejectedSpoofed + 1
            )
            addLog("ALERTA: Firma criptográfica FALSIFICADA o corrupta para orden ${payload.commandType}")
            return C2VerificationResult(isValid = false, reason = "INVALID_CRYPTOGRAPHIC_SIGNATURE")
        }

        // Registrar Nonce para prevenir Replay futuro
        seenNonces[payload.nonce] = now
        cleanupOldNonces(now)

        _state.value = _state.value.copy(
            totalVerifiedValid = _state.value.totalVerifiedValid + 1
        )
        addLog("VALIDADO: Orden legítima ${payload.commandType} (${age}ms)")

        return C2VerificationResult(
            isValid = true,
            reason = "SUCCESS_AUTHENTIC",
            signerFingerprint = payload.signerFingerprint,
            ageMs = age
        )
    }

    private fun signData(data: ByteArray): ByteArray {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val privateKey = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.PrivateKeyEntry)?.privateKey
                ?: fallbackKeyPair?.private
                ?: throw IllegalStateException("Clave privada no disponible")

            val signer = Signature.getInstance("SHA256withECDSA")
            signer.initSign(privateKey)
            signer.update(data)
            signer.sign()
        } catch (e: Exception) {
            Log.e(TAG, "Error al firmar con KeyStore, usando fallback", e)
            fallbackKeyPair?.let { pair ->
                val signer = Signature.getInstance("SHA256withECDSA")
                signer.initSign(pair.private)
                signer.update(data)
                signer.sign()
            } ?: ByteArray(64)
        }
    }

    private fun verifySignature(data: ByteArray, signature: ByteArray, explicitKey: PublicKey?): Boolean {
        return try {
            val pubKey = explicitKey ?: run {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                keyStore.getCertificate(KEY_ALIAS)?.publicKey ?: fallbackKeyPair?.public
            } ?: return false

            val verifier = Signature.getInstance("SHA256withECDSA")
            verifier.initVerify(pubKey)
            verifier.update(data)
            verifier.verify(signature)
        } catch (e: Exception) {
            Log.e(TAG, "Excepción verificando firma", e)
            false
        }
    }

    private fun cleanupOldNonces(now: Long) {
        val iterator = seenNonces.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > MAX_COMMAND_AGE_MS * 2) {
                iterator.remove()
            }
        }
    }

    private fun computeFingerprint(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(bytes)
        return hash.take(6).joinToString(":") { "%02X".format(it) }
    }

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = "[$time] $message"
        val updated = (_state.value.securityLogs + entry).takeLast(25)
        _state.value = _state.value.copy(securityLogs = updated)
    }
}
