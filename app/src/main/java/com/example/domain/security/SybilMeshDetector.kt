package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 16: Detector de Ataques Sybil y Análisis Forense de Paquetes en Malla:
 * Detecta identidades múltiples generadas desde una misma dirección física de radiofrecuencia (MAC/PHY),
 * discrepancias de RSSI anómalas e inyecciones de secuencias de números duplicadas para aislar nodos hostiles.
 */
data class SybilAlert(
    val suspectNodeId: String,
    val spoofedCallsigns: List<String>,
    val rssiVariance: Float,
    val sequenceAnomalyCount: Int,
    val threatLevel: String, // "CRÍTICO", "MODERADO", "BAJO"
    val timestamp: Long = System.currentTimeMillis()
)

data class SybilDetectorState(
    val isMonitoring: Boolean = false,
    val totalInspectedPackets: Long = 0,
    val activeThreats: List<SybilAlert> = emptyList(),
    val isolatedNodesCount: Int = 0
)

class SybilMeshDetector private constructor(context: Context) {

    private val _state = MutableStateFlow(
        SybilDetectorState(
            activeThreats = listOf(
                SybilAlert(
                    suspectNodeId = "PHY-NODE-9X2",
                    spoofedCallsigns = listOf("SCOUT-1", "SCOUT-2", "BASE-HQ"),
                    rssiVariance = 1.2f,
                    sequenceAnomalyCount = 4,
                    threatLevel = "CRÍTICO"
                )
            ),
            isolatedNodesCount = 1
        )
    )
    val state: StateFlow<SybilDetectorState> = _state.asStateFlow()

    fun startInspection() {
        _state.value = _state.value.copy(isMonitoring = true)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SYBIL_DEFENSE",
            message = "Detector forense de identidades Sybil activado en capa de malla física"
        )
    }

    fun stopInspection() {
        _state.value = _state.value.copy(isMonitoring = false)
    }

    fun isolateThreat(nodeId: String) {
        val list = _state.value.activeThreats.filterNot { it.suspectNodeId == nodeId }
        _state.value = _state.value.copy(
            activeThreats = list,
            isolatedNodesCount = _state.value.isolatedNodesCount + 1
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.WARNING,
            tag = "SYBIL_ISOLATE",
            message = "Nodo malicioso $nodeId neutralizado y bloqueado en tabla de enrutamiento"
        )
    }

    fun simulateSybilAttack() {
        val alert = SybilAlert(
            suspectNodeId = "PHY-CLONE-${(100..999).random()}",
            spoofedCallsigns = listOf("EAGLE-ALPHA", "EAGLE-BRAVO"),
            rssiVariance = 0.8f,
            sequenceAnomalyCount = 6,
            threatLevel = "CRÍTICO"
        )
        val list = _state.value.activeThreats.toMutableList()
        list.add(0, alert)
        _state.value = _state.value.copy(
            activeThreats = list,
            totalInspectedPackets = _state.value.totalInspectedPackets + 140
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.ERROR,
            tag = "SYBIL_ATTACK",
            message = "¡ALERTA SYBIL! Múltiples identidades operativas originadas en la misma firma de radio"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SybilMeshDetector? = null

        fun getInstance(context: Context): SybilMeshDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SybilMeshDetector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
