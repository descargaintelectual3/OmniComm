package com.example.domain.ai

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 9 (Plan Maestro): Agente Táctico Autónomo de Misión (Edge Offline Recon Assistant):
 * Evalúa reportes de situación, amenazas detectadas por radar y estado de batería para generar
 * recomendaciones de acción táctica inmediata (COA - Course of Action) de forma 100% offline.
 */
data class TacticalRecommendation(
    val id: String,
    val timestamp: Long = System.currentTimeMillis(),
    val priority: String, // "CRÍTICO", "URGENTE", "INFORMATIVO"
    val title: String,
    val rationale: String,
    val suggestedAction: String
)

class TacticalMissionAgent private constructor(context: Context) {

    private val _isAgentActive = MutableStateFlow(true)
    val isAgentActive: StateFlow<Boolean> = _isAgentActive.asStateFlow()

    private val _recentRecommendations = MutableStateFlow<List<TacticalRecommendation>>(
        listOf(
            TacticalRecommendation(
                id = "REC-001",
                priority = "INFORMATIVO",
                title = "Mantener Silencio de Radio OOB",
                rationale = "Detección de posibles receptores no aliados en cuadrante Este.",
                suggestedAction = "Utilizar Li-Fi óptico o pulsos Morse a 850 Hz para enlace local."
            ),
            TacticalRecommendation(
                id = "REC-002",
                priority = "URGENTE",
                title = "Activar Salto de Frecuencia FHSS",
                rationale = "Ruido de fondo en canal WiFi 6 superior a -65 dBm.",
                suggestedAction = "Sincronizar semilla de salto de canal en la constelación mesh."
            )
        )
    )
    val recentRecommendations: StateFlow<List<TacticalRecommendation>> = _recentRecommendations.asStateFlow()

    fun evaluateMissionConditions(
        batteryLevel: Int,
        activePeersCount: Int,
        hostileDetected: Boolean,
        signalNoiseDb: Float
    ) {
        val list = _recentRecommendations.value.toMutableList()

        if (batteryLevel < 20) {
            list.add(0, TacticalRecommendation(
                id = "REC-${System.currentTimeMillis() % 1000}",
                priority = "CRÍTICO",
                title = "Conservación de Energía Táctica",
                rationale = "Nivel de batería crítico ($batteryLevel%).",
                suggestedAction = "Desactivar cámara térmica continua y reducir tasa de balizas mesh a 60s."
            ))
        }

        if (hostileDetected) {
            list.add(0, TacticalRecommendation(
                id = "REC-${System.currentTimeMillis() % 1000}",
                priority = "CRÍTICO",
                title = "Alerta de Contacto Hostil Inminente",
                rationale = "Entidad enemiga registrada en tablero OTAN APP-6.",
                suggestedAction = "Verificar fragmentos de llave Shamir y preparar protocolo Zeroize si la posición es comprometida."
            ))
        }

        if (signalNoiseDb > 75f) {
            list.add(0, TacticalRecommendation(
                id = "REC-${System.currentTimeMillis() % 1000}",
                priority = "URGENTE",
                title = "Contramedida ante Jamming Hostil",
                rationale = "Interferencia de banda ancha detectada.",
                suggestedAction = "Conmutar enlaces a navegación inercial PDR y balizas acústicas."
            ))
        }

        _recentRecommendations.value = list.take(6)

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "TACTICAL_AGENT",
            message = "Agente Táctico de Misión evaluó condiciones operativas (${_recentRecommendations.value.size} directivas activas)"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: TacticalMissionAgent? = null

        fun getInstance(context: Context): TacticalMissionAgent {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalMissionAgent(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
