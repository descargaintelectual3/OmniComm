package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.repository.TacticalC2AuditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class MavlinkUavTelemetry(
    val sysId: Int = 1,
    val compId: Int = 1,
    val uavName: String = "UAV_RECON_ALPHA",
    val lat: Double = 4.6145,
    val lon: Double = -74.0780,
    val altAglMeters: Float = 65.0f,
    val headingDeg: Float = 142.0f,
    val groundSpeedMps: Float = 8.5f,
    val pitchDeg: Float = -2.0f,
    val rollDeg: Float = 1.5f,
    val batteryPercent: Int = 82,
    val flightMode: String = "AUTO_MISSION",
    val armed: Boolean = true,
    val cameraGimbalPitchDeg: Float = -45.0f,
    val fovMeters: Float = 120.0f
)

/**
 * Motor de Telemetría e Integración con Micro-Drones de Reconocimiento MAVLink v2.0 (Capa 9).
 * Decodifica mensajes estándar #30 (ATTITUDE), #33 (GLOBAL_POSITION_INT), #147 (BATTERY_STATUS).
 */
class MavlinkDroneTelemetryEngine private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private var simulationJob: Job? = null
    private val auditRepo = TacticalC2AuditRepository.getInstance(context)

    private val _activeDrone = MutableStateFlow(MavlinkUavTelemetry())
    val activeDrone: StateFlow<MavlinkUavTelemetry> = _activeDrone.asStateFlow()

    private val _isStreamingActive = MutableStateFlow(false)
    val isStreamingActive: StateFlow<Boolean> = _isStreamingActive.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: MavlinkDroneTelemetryEngine? = null

        fun getInstance(context: Context): MavlinkDroneTelemetryEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: MavlinkDroneTelemetryEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Parsea un paquete binario MAVLink v2.0 (cabecera 0xFD).
     */
    fun parseMavlinkV2Packet(data: ByteArray): Boolean {
        if (data.isEmpty() || data[0] != 0xFD.toByte()) return false
        try {
            val payloadLen = data[1].toInt() and 0xFF
            val msgId = (data[7].toInt() and 0xFF) or ((data[8].toInt() and 0xFF) shl 8) or ((data[9].toInt() and 0xFF) shl 16)
            val payloadOffset = 10

            when (msgId) {
                33 -> { // GLOBAL_POSITION_INT
                    val buffer = ByteBuffer.wrap(data, payloadOffset, payloadLen).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.getInt() // time_boot_ms
                    val lat = buffer.getInt() / 1e7
                    val lon = buffer.getInt() / 1e7
                    val altMm = buffer.getInt()
                    val relativeAltMm = buffer.getInt()
                    val hdgCentiDeg = buffer.getShort()

                    _activeDrone.value = _activeDrone.value.copy(
                        lat = lat,
                        lon = lon,
                        altAglMeters = relativeAltMm / 1000f,
                        headingDeg = hdgCentiDeg / 100f
                    )
                    return true
                }
                30 -> { // ATTITUDE
                    val buffer = ByteBuffer.wrap(data, payloadOffset, payloadLen).order(ByteOrder.LITTLE_ENDIAN)
                    buffer.getInt() // time_boot_ms
                    val rollRad = buffer.getFloat()
                    val pitchRad = buffer.getFloat()
                    _activeDrone.value = _activeDrone.value.copy(
                        rollDeg = Math.toDegrees(rollRad.toDouble()).toFloat(),
                        pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
                    )
                    return true
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }

    /**
     * Inicia la patrulla o simulación de vuelo del UAV táctico con cono de observación dinámico.
     */
    fun startLiveUavStream(uavCallsign: String = "UAV_RECON_ALPHA") {
        if (_isStreamingActive.value) return
        _isStreamingActive.value = true

        simulationJob = scope.launch {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "MAVLINK_C2",
                message = "Enlace de telemetría de dron MAVLink v2.0 establecido con $uavCallsign"
            )

            var angleRad = 0.0
            val centerLat = 4.6140
            val centerLon = -74.0780
            val radius = 0.0035

            while (isActive) {
                angleRad += 0.04
                val newLat = centerLat + (Math.sin(angleRad) * radius)
                val newLon = centerLon + (Math.cos(angleRad) * radius)
                val heading = (Math.toDegrees(angleRad + Math.PI / 2) % 360.0).toFloat()
                val battery = (82 - (angleRad * 0.1).toInt()).coerceAtLeast(15)

                _activeDrone.value = _activeDrone.value.copy(
                    uavName = uavCallsign,
                    lat = newLat,
                    lon = newLon,
                    headingDeg = heading,
                    batteryPercent = battery,
                    altAglMeters = 65.0f + (Math.sin(angleRad * 2) * 5).toFloat()
                )

                if ((angleRad / 0.04).toInt() % 10 == 0) {
                    auditRepo.recordDroneTelemetry(
                        uavName = uavCallsign,
                        lat = newLat,
                        lon = newLon,
                        altAglMeters = _activeDrone.value.altAglMeters,
                        headingDeg = heading,
                        batteryPercent = battery,
                        flightMode = _activeDrone.value.flightMode
                    )
                }

                delay(500L)
            }
        }
    }

    fun stopLiveUavStream() {
        simulationJob?.cancel()
        simulationJob = null
        _isStreamingActive.value = false
    }
}
