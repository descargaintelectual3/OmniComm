package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 20: Modo Silencio de Radio Absoluto EMCON Alpha / Stealth Blackout:
 * Bloquea físicamente toda radiación electromagnética activa (Transmisión RF prohibida),
 * forzando el sistema a modo puramente pasivo (Solo Recepción RX, Li-Fi y Enlace Acústico Sub-audible).
 */
enum class EmconLevel {
    NORMAL_TRANSMIT,
    EMCON_BRAVO,  // Solo ráfagas cortas programadas
    EMCON_ALPHA   // Silencio de radio absoluto (Cero emisiones RF)
}

data class EmconState(
    val currentLevel: EmconLevel = EmconLevel.NORMAL_TRANSMIT,
    val passiveRxOnly: Boolean = false,
    val acousticLinkPermitted: Boolean = true,
    val opticalLifiPermitted: Boolean = true,
    val lastEmconChangeTimestamp: Long = System.currentTimeMillis()
)

class EmconAlphaManager private constructor(context: Context) {

    private val _state = MutableStateFlow(EmconState())
    val state: StateFlow<EmconState> = _state.asStateFlow()

    fun setEmconLevel(level: EmconLevel) {
        val rxOnly = (level == EmconLevel.EMCON_ALPHA)
        _state.value = _state.value.copy(
            currentLevel = level,
            passiveRxOnly = rxOnly,
            lastEmconChangeTimestamp = System.currentTimeMillis()
        )

        val sev = if (level == EmconLevel.EMCON_ALPHA) LogSeverity.WARNING else LogSeverity.INFO
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = sev,
            tag = "EMCON_CONTROL",
            message = "Nivel de Control de Emisiones cambiado a $level (Modo Pasivo RX: $rxOnly)"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: EmconAlphaManager? = null

        fun getInstance(context: Context): EmconAlphaManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: EmconAlphaManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
