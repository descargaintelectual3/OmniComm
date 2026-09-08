package com.example.domain.models

import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class TacticalSitrepReport(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val unitDesignator: String = "ALPHA-1",
    val gridCoordinates: String = "4.6105° N, 74.0820° W",
    val enemyActivity: String = "Sin contacto visual directo",
    val friendlyStatus: String = "100% Efectivos operativos / 0 Bajas",
    val logisticsAndAmmo: String = "Munición Verde (85%), Baterías 92%, Raciones 3 Días",
    val supportRequested: String = "Ninguno / Rutina",
    val generalNotes: String = "Malla P2P operativa en silencio de radio."
) {
    fun toFormattedNatoString(): String {
        val sdf = SimpleDateFormat("ddHHmm'Z' MMM yy", Locale.US)
        val dtg = sdf.format(Date(timestamp)).uppercase()
        return """
            === REPORTE DE SITUACIÓN TÁCTICO (SITREP) ===
            DTG: $dtg
            LÍNEA 1 (UNIDAD): $unitDesignator
            LÍNEA 2 (POSICIÓN/MGRS): $gridCoordinates
            LÍNEA 3 (ACTIVIDAD HOSTIL): $enemyActivity
            LÍNEA 4 (ESTADO PROPIO/BAJAS): $friendlyStatus
            LÍNEA 5 (LOGÍSTICA/SUMINISTROS): $logisticsAndAmmo
            LÍNEA 6 (PETICIÓN DE APOYO/MEDEVAC): $supportRequested
            OBSERVACIONES: $generalNotes
            ============================================
        """.trimIndent()
    }
}

/**
 * Gestor de Reportes SITREP Tácticos:
 * Genera, almacena y transmite informes estandarizados a través de la malla.
 */
object SitrepReportManager {

    private val _reports = MutableStateFlow<List<TacticalSitrepReport>>(
        listOf(
            TacticalSitrepReport(
                unitDesignator = "BASE-BRAVO",
                gridCoordinates = "4.6200° N, 74.0900° W",
                enemyActivity = "Monitoreo perimetral activo",
                friendlyStatus = "4 Operadores en guardia",
                logisticsAndAmmo = "Recursos al 95%",
                supportRequested = "Enlace repetidor malla",
                generalNotes = "Canal principal establecido sin novedades."
            )
        )
    )
    val reports: StateFlow<List<TacticalSitrepReport>> = _reports.asStateFlow()

    fun createAndBroadcastReport(report: TacticalSitrepReport) {
        _reports.value = listOf(report) + _reports.value
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SITREP_TRANSMITTED",
            message = "Informe SITREP [${report.unitDesignator}] creado y transmitido a la malla (${report.gridCoordinates})"
        )
    }

    fun deleteReport(id: String) {
        _reports.value = _reports.value.filter { it.id != id }
    }
}
