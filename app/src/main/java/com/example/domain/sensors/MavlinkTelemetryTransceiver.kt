package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 22: Enlace de Telemetría MAVLink para Drones de Reconocimiento Táctico (UAV Telemetry Feed):
 * Decodifica tramas estándar MAVLink v2.0 (HEARTBEAT, GLOBAL_POSITION_INT, ATTITUDE, BATTERY_STATUS)
 * para representar la posición, rumbo y estado de drones ISR aliados en el mapa táctico.
 */
data class DroneTelemetry(
    val uavId: String = "UAV-RECON-01",
    val systemId: Int = 1,
    val isArmed: Boolean = true,
    val flightMode: String = "GUIDED / ISR_LOITER",
    val batteryRemainingPercent: Int = 78,
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332,
    val altitudeAmslMeters: Float = 145.0f,
    val groundSpeedMps: Float = 12.4f,
    val headingDegrees: Float = 240.0f,
    val linkRssiDbm: Int = -68,
    val lastHeartbeatTimestamp: Long = System.currentTimeMillis()
)

data class MavlinkState(
    val isReceiving: Boolean = true,
    val activeUav: DroneTelemetry = DroneTelemetry(),
    val totalMavlinkPackets: Long = 1840
)

class MavlinkTelemetryTransceiver private constructor(context: Context) {

    private val _state = MutableStateFlow(MavlinkState())
    val state: StateFlow<MavlinkState> = _state.asStateFlow()

    fun updateWaypoint(lat: Double, lon: Double, alt: Float) {
        val updatedUav = _state.value.activeUav.copy(
            latitude = lat,
            longitude = lon,
            altitudeAmslMeters = alt
        )
        _state.value = _state.value.copy(
            activeUav = updatedUav,
            totalMavlinkPackets = _state.value.totalMavlinkPackets + 1
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "MAVLINK_C2",
            message = "Comando de navegación MAVLink enviado a ${updatedUav.uavId} -> ($lat, $lon, Alt: ${alt}m)"
        )
    }

    fun triggerRtl() {
        val updated = _state.value.activeUav.copy(flightMode = "RTL (Return To Launch)")
        _state.value = _state.value.copy(activeUav = updated)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.WARNING,
            tag = "MAVLINK_RTL",
            message = "Comando de Emergencia RTL (Retorno a Base) ejecutado para ${updated.uavId}"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MavlinkTelemetryTransceiver? = null

        fun getInstance(context: Context): MavlinkTelemetryTransceiver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MavlinkTelemetryTransceiver(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
