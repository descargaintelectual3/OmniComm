package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * FASE 23: Matriz de Evacuación Médica y Triaje Táctico 9-Line MEDEVAC & TCCC Casualty Card:
 * Permite estructurar solicitudes de rescate médico táctico urgente bajo estándar OTAN 9-Line,
 * registrando torniquetes aplicados, vía aérea, analgésicos y estado vital del herido.
 */
data class NineLineMedevacRequest(
    val reportId: String = "MEDEVAC-${UUID.randomUUID().toString().take(6).uppercase()}",
    val line1LocationGrid: String,
    val line2RadioFrequencyCallsign: String,
    val line3PatientsByPrecedence: String, // "1x URGENT, 1x PRIORITY"
    val line4SpecialEquipmentRequired: String, // "HOIST, VENTILATOR"
    val line5PatientsByType: String, // "1x LITTER, 1x AMBULATORY"
    val line6SecurityAtPickupSite: String, // "HOT / HOSTILE FIRE"
    val line7MethodOfMarking: String, // "SMOKE GREEN / IR STROBE"
    val line8PatientNationalityStatus: String, // "MILITARY ALLIED"
    val line9NbcTerrainObstacles: String, // "TREES 20M NORTH"
    val tourniquetTime: String = "14:22 UTC (Ext. Der)",
    val timestamp: Long = System.currentTimeMillis()
)

class TacticalMedevacEngine private constructor(context: Context) {

    private val _activeMedevacs = MutableStateFlow<List<NineLineMedevacRequest>>(
        listOf(
            NineLineMedevacRequest(
                line1LocationGrid = "14Q NF 1928 4812",
                line2RadioFrequencyCallsign = "44.200 MHz / DUSTOFF-3",
                line3PatientsByPrecedence = "1x URGENTE (Herida Tórax Abierta)",
                line4SpecialEquipmentRequired = "VENTILADOR + CABLE EXTRACCIÓN",
                line5PatientsByType = "1x CAMILLA",
                line6SecurityAtPickupSite = "SEGURO (Perímetro Establecido)",
                line7MethodOfMarking = "HUMO PÚRPURA + ESPEJO SEÑALES",
                line8PatientNationalityStatus = "OPERADOR AMIGO",
                line9NbcTerrainObstacles = "CABLES ELÉCTRICOS AL SUR",
                tourniquetTime = "T-14:05 UTC (Brazo Izquierdo)"
            )
        )
    )
    val activeMedevacs: StateFlow<List<NineLineMedevacRequest>> = _activeMedevacs.asStateFlow()

    fun submitMedevacRequest(request: NineLineMedevacRequest) {
        val list = _activeMedevacs.value.toMutableList()
        list.add(0, request)
        _activeMedevacs.value = list

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.WARNING,
            tag = "9LINE_MEDEVAC",
            message = "Solicitud 9-Line MEDEVAC enviada a la malla: ${request.reportId} en ${request.line1LocationGrid}"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: TacticalMedevacEngine? = null

        fun getInstance(context: Context): TacticalMedevacEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalMedevacEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
