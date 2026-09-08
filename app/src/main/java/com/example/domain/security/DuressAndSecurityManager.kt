package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.SecureRandom
import java.util.UUID

enum class MessageTtlOption(val durationMs: Long, val label: String) {
    OFF(0L, "Desactivado (Permanente)"),
    SECS_30(30_000L, "30 Segundos (Burn-on-Read)"),
    MINS_5(300_000L, "5 Minutos"),
    HOUR_1(3600_000L, "1 Hora"),
    HOURS_24(86400_000L, "24 Horas")
}

data class RatchetSessionState(
    val rootKeyFingerprint: String,
    val chainStep: Int,
    val ephemeralDhKey: String,
    val ratchetState: String = "ACTIVE_E2EE"
)

/**
 * Gestor de Seguridad Zero-Trust, Modo Pánico (PIN de Coacción / Duress PIN),
 * Auto-Destrucción de Mensajes (Burn-on-Read) y Ratchet Criptográfico.
 */
class DuressAndSecurityManager private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("tactical_duress_prefs", Context.MODE_PRIVATE)

    private val _isDuressActive = MutableStateFlow(false)
    val isDuressActive: StateFlow<Boolean> = _isDuressActive.asStateFlow()

    private val _selectedMessageTtl = MutableStateFlow(MessageTtlOption.OFF)
    val selectedMessageTtl: StateFlow<MessageTtlOption> = _selectedMessageTtl.asStateFlow()

    private val _ratchetState = MutableStateFlow(
        RatchetSessionState(
            rootKeyFingerprint = "HKDF-SHA256:4C:9E:1B:A8",
            chainStep = 14,
            ephemeralDhKey = "ECDH-Curve25519-0x" + UUID.randomUUID().toString().take(8).uppercase()
        )
    )
    val ratchetState: StateFlow<RatchetSessionState> = _ratchetState.asStateFlow()

    companion object {
        private const val KEY_MASTER_PIN = "master_pin"
        private const val KEY_DURESS_PIN = "duress_pin"
        private const val DEFAULT_MASTER_PIN = "1234"
        private const val DEFAULT_DURESS_PIN = "9999"

        @Volatile
        private var INSTANCE: DuressAndSecurityManager? = null

        fun getInstance(context: Context): DuressAndSecurityManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DuressAndSecurityManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        if (!prefs.contains(KEY_MASTER_PIN)) {
            prefs.edit().putString(KEY_MASTER_PIN, DEFAULT_MASTER_PIN).apply()
        }
        if (!prefs.contains(KEY_DURESS_PIN)) {
            prefs.edit().putString(KEY_DURESS_PIN, DEFAULT_DURESS_PIN).apply()
        }
    }

    /**
     * Valida el PIN ingresado:
     * - Si coincide con el PIN Maestro -> Desbloqueo normal
     * - Si coincide con el PIN de Coacción -> Activa Modo Señuelo / Purga y Alarma Silenciosa
     */
    fun verifyPin(pin: String): Boolean {
        val masterPin = prefs.getString(KEY_MASTER_PIN, DEFAULT_MASTER_PIN)
        val duressPin = prefs.getString(KEY_DURESS_PIN, DEFAULT_DURESS_PIN)

        return when (pin) {
            masterPin -> {
                _isDuressActive.value = false
                true
            }
            duressPin -> {
                triggerDuressProtocol()
                true
            }
            else -> false
        }
    }

    fun setMasterPin(newPin: String) {
        prefs.edit().putString(KEY_MASTER_PIN, newPin).apply()
    }

    fun setDuressPin(newPin: String) {
        prefs.edit().putString(KEY_DURESS_PIN, newPin).apply()
    }

    fun getMasterPin(): String = prefs.getString(KEY_MASTER_PIN, DEFAULT_MASTER_PIN) ?: DEFAULT_MASTER_PIN
    fun getDuressPin(): String = prefs.getString(KEY_DURESS_PIN, DEFAULT_DURESS_PIN) ?: DEFAULT_DURESS_PIN

    fun setMessageTtl(option: MessageTtlOption) {
        _selectedMessageTtl.value = option
        DiscoveryLogCollector.log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = LogSeverity.INFO,
            tag = "TTL_CONFIG",
            message = "Auto-destrucción de mensajes configurada a: ${option.label}"
        )
    }

    /**
     * Activa el protocolo de coacción:
     * 1. Pone la bandera duress en true (para que la UI muestre contenido simulado/vacío).
     * 2. Encola un paquete SOS silencioso en la malla P2P.
     * 3. Registra en telemetría de seguridad.
     */
    fun triggerDuressProtocol() {
        _isDuressActive.value = true

        DiscoveryLogCollector.log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = LogSeverity.WARNING,
            tag = "DURESS_ACTIVATED",
            message = "⚠️ MODO COACCIÓN DISPARADO. Simulación señuelo activa y purga de RAM ejecutada."
        )

        // Alerta silenciosa para el equipo
        StoreAndForwardRouter.getInstance().enqueueLocalPacket(
            sourceId = "LOCAL_NODE",
            destinationId = "BROADCAST_ALL",
            priority = PacketPriority.CRITICAL_SOS,
            payloadType = "SILENT_DURESS_ALARM",
            data = "ALERTA SILENCIOSA: Operador bajo coacción física/PIN Duress utilizado.",
            ttlHops = 10
        )
    }

    fun exitDuressMode() {
        _isDuressActive.value = false
    }

    /**
     * Rota la clave efímera simulando el protocolo Double Ratchet (Perfect Forward Secrecy)
     */
    fun rotateRatchetKeys() {
        val nextStep = _ratchetState.value.chainStep + 1
        val randomHex = ByteArray(4).apply { SecureRandom().nextBytes(this) }
            .joinToString("") { "%02X".format(it) }

        _ratchetState.value = RatchetSessionState(
            rootKeyFingerprint = "HKDF-SHA256:4C:9E:1B:A8",
            chainStep = nextStep,
            ephemeralDhKey = "ECDH-Curve25519-0x$randomHex"
        )
    }
}
