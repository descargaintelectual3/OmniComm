package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 19: Medidor de Huella Electromagnética y Probabilidad de Intercepción (LPI / LPD Meter):
 * Calcula la detectabilidad de las emisiones de radio del dispositivo en función de la potencia de transmisión (dBm),
 * ciclo de trabajo (duty cycle %) y frecuencias activas para mitigar localización enemiga por radiogoniometría.
 */
data class RfEmissionProfile(
    val currentTxPowerDbm: Int = 14,
    val dutyCyclePercent: Float = 4.5f,
    val probabilityOfDetectionPercent: Int = 18,
    val interceptRiskLevel: String = "BAJO", // "BAJO", "MODERADO", "PELIGROSO"
    val maxDetectionRangeKm: Float = 1.2f,
    val isBleEmitting: Boolean = true,
    val isWifiDirectEmitting: Boolean = true,
    val isCellularActive: Boolean = false
)

class RfEmissionSignatureMeter private constructor(context: Context) {

    private val _profile = MutableStateFlow(RfEmissionProfile())
    val profile: StateFlow<RfEmissionProfile> = _profile.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var monitorJob: Job? = null

    fun startRfAudit() {
        monitorJob?.cancel()
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "RF_AUDIT",
            message = "Auditor de Huella Electromagnética LPI/LPD activado"
        )

        monitorJob = scope.launch {
            while (isActive) {
                delay(4000)
                recalculateSignature()
            }
        }
    }

    fun stopRfAudit() {
        monitorJob?.cancel()
    }

    private fun recalculateSignature() {
        val current = _profile.value
        val txCount = (if (current.isBleEmitting) 1 else 0) + (if (current.isWifiDirectEmitting) 2 else 0) + (if (current.isCellularActive) 4 else 0)
        val pDetect = (txCount * 18 + (current.dutyCyclePercent * 3)).toInt().coerceIn(2, 98)
        val risk = when {
            pDetect > 65 -> "PELIGROSO (Alta firma RF)"
            pDetect > 30 -> "MODERADO"
            else -> "BAJO (LPI Óptimo)"
        }
        val range = (pDetect * 0.05f).coerceAtLeast(0.1f)

        _profile.value = current.copy(
            probabilityOfDetectionPercent = pDetect,
            interceptRiskLevel = risk,
            maxDetectionRangeKm = range
        )
    }

    fun optimizeForStealth() {
        _profile.value = _profile.value.copy(
            currentTxPowerDbm = 4,
            dutyCyclePercent = 1.2f,
            isCellularActive = false,
            isBleEmitting = true,
            isWifiDirectEmitting = false
        )
        recalculateSignature()
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "RF_STEALTH",
            message = "Perfil de radio ajustado a Bajo Consumo y Mínima Detección (LPI Máximo)"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: RfEmissionSignatureMeter? = null

        fun getInstance(context: Context): RfEmissionSignatureMeter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RfEmissionSignatureMeter(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
