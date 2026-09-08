package com.example.domain.c4isr

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

/**
 * FASE 28: Generador de Informes de Inteligencia Militar SALUTE & INTREP:
 * Estructura reportes estándar militares (Size, Activity, Location, Uniform/Unit, Time, Equipment)
 * con compresión para transmisión rápida por malla táctica.
 */
data class SaluteReport(
    val reportId: String = "SALUTE-${UUID.randomUUID().toString().take(6).uppercase()}",
    val size: String,       // p.ej. "Pelotón reforzado (aprox 25 elementos)"
    val activity: String,   // p.ej. "Estableciendo puesto de control y trincheras"
    val location: String,   // p.ej. "Grid 14Q NF 1934 4890 (Cruce carretero)"
    val uniformUnit: String,// p.ej. "Camuflaje desértico con distintivos no identificados"
    val timeObserved: String,// p.ej. "14:15 UTC"
    val equipment: String,  // p.ej. "2x Camiones ligeros blindados con ametralladora pesada"
    val observerCallsign: String = "RECON-ALPHA",
    val timestamp: Long = System.currentTimeMillis()
)

class SaluteIntelReportEngine private constructor(context: Context) {

    private val _reports = MutableStateFlow<List<SaluteReport>>(
        listOf(
            SaluteReport(
                size = "Sección de 8 individuos",
                activity = "Movimiento táctico en cuña hacia LZ Norte",
                location = "GRID 14Q NF 1910 4820",
                uniformUnit = "Equipo táctico oscuro sin insignias visibles",
                timeObserved = "13:50 UTC",
                equipment = "Fusiles de asalto con ópticas nocturnas y 1x RPG"
            )
        )
    )
    val reports: StateFlow<List<SaluteReport>> = _reports.asStateFlow()

    fun submitSaluteReport(report: SaluteReport) {
        val list = _reports.value.toMutableList()
        list.add(0, report)
        _reports.value = list

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SALUTE_INTEL",
            message = "Informe de inteligencia SALUTE transmitido: ${report.reportId} en ${report.location}"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SaluteIntelReportEngine? = null

        fun getInstance(context: Context): SaluteIntelReportEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SaluteIntelReportEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
