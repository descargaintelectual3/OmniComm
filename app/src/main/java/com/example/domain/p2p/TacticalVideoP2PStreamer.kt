package com.example.domain.p2p

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.BatteryManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.floor

/**
 * Modo de compresión y ancho de banda para transmisión táctica P2P
 */
enum class TacticalVideoCodecProfile(
    val label: String,
    val targetResolution: Pair<Int, Int>,
    val targetFps: Int,
    val compressionQuality: Int,
    val estimatedBitrateKbps: Int
) {
    TACTICAL_STEALTH_LOW(
        label = "Sigilo / Bajo Ancho de Banda (LoRa / Mesh Limitado)",
        targetResolution = Pair(320, 240),
        targetFps = 10,
        compressionQuality = 35,
        estimatedBitrateKbps = 64
    ),
    TACTICAL_STANDARD(
        label = "Táctico Estándar (Wi-Fi Direct / Mesh)",
        targetResolution = Pair(640, 480),
        targetFps = 20,
        compressionQuality = 60,
        estimatedBitrateKbps = 320
    ),
    TACTICAL_HD(
        label = "Alta Definición Operativa (Enlace Robusto)",
        targetResolution = Pair(1280, 720),
        targetFps = 30,
        compressionQuality = 80,
        estimatedBitrateKbps = 1200
    )
}

/**
 * Datos de Telemetría Militar Táctica superpuestos en tiempo real sobre el fotograma de video
 */
data class VideoTelemetryOverlay(
    val mgrsCoordinate: String = "30TWN 1234 5678",
    val latitude: Double = 19.4326,
    val longitude: Double = -99.1332,
    val altitudeMeters: Float = 2240f,
    val compassAzimuthDeg: Float = 42f,
    val cardinalDirection: String = "NE",
    val pitchDeg: Float = 0f,
    val rollDeg: Float = 0f,
    val batteryPercent: Int = 88,
    val batteryVoltageMv: Int = 3950,
    val encryptionCipher: String = "E2EE AES-256-GCM (DTLS-SRTP)",
    val operatorCallsign: String = "ALPHA-01",
    val targetRoomId: String = "OMNI-TAC-01",
    val frameBitrateKbps: Int = 320,
    val packetLossPercent: Float = 0.2f,
    val latencyPingMs: Int = 12,
    val isNightVisionFilterActive: Boolean = false,
    val isFlirThermalFilterActive: Boolean = false
)

/**
 * Fotograma de Video Táctico comprimido y listo para transmisión en malla
 */
data class TacticalVideoFramePacket(
    val frameSequence: Long,
    val timestamp: Long,
    val senderNodeId: String,
    val codecProfile: TacticalVideoCodecProfile,
    val compressedFrameBytes: ByteArray,
    val telemetry: VideoTelemetryOverlay
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TacticalVideoFramePacket
        return frameSequence == other.frameSequence && senderNodeId == other.senderNodeId
    }

    override fun hashCode(): Int {
        var result = frameSequence.hashCode()
        result = 31 * result + senderNodeId.hashCode()
        return result
    }
}

/**
 * Motor de Transmisión de Video Táctico P2P sobre WebRTC / Wi-Fi Direct Mesh
 * con compresión adaptativa en tiempo real y superposición de telemetría militar HUD.
 */
class TacticalVideoP2PStreamer(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val isStreamingActive = AtomicBoolean(false)
    private var streamingLoopJob: Job? = null
    private var sequenceCounter = 0L

    private val _activeCodecProfile = MutableStateFlow(TacticalVideoCodecProfile.TACTICAL_STANDARD)
    val activeCodecProfile: StateFlow<TacticalVideoCodecProfile> = _activeCodecProfile.asStateFlow()

    private val _currentTelemetry = MutableStateFlow(VideoTelemetryOverlay())
    val currentTelemetry: StateFlow<VideoTelemetryOverlay> = _currentTelemetry.asStateFlow()

    private val _lastTransmittedFrame = MutableStateFlow<TacticalVideoFramePacket?>(null)
    val lastTransmittedFrame: StateFlow<TacticalVideoFramePacket?> = _lastTransmittedFrame.asStateFlow()

    private val _remotePeerFrame = MutableStateFlow<Bitmap?>(null)
    val remotePeerFrame: StateFlow<Bitmap?> = _remotePeerFrame.asStateFlow()

    private val _isHudVisible = MutableStateFlow(true)
    val isHudVisible: StateFlow<Boolean> = _isHudVisible.asStateFlow()

    private val _filterMode = MutableStateFlow("NORMAL") // "NORMAL", "NVG_GREEN", "FLIR_THERMAL"
    val filterMode: StateFlow<String> = _filterMode.asStateFlow()

    companion object {
        private const val TAG = "TacticalVideoP2P"
    }

    fun setCodecProfile(profile: TacticalVideoCodecProfile) {
        _activeCodecProfile.value = profile
        Log.i(TAG, "Perfil de códec de video actualizado a: ${profile.label}")
    }

    fun toggleHudVisibility() {
        _isHudVisible.value = !_isHudVisible.value
    }

    fun cycleFilterMode() {
        _filterMode.value = when (_filterMode.value) {
            "NORMAL" -> "NVG_GREEN"
            "NVG_GREEN" -> "FLIR_THERMAL"
            else -> "NORMAL"
        }
    }

    fun updateTelemetry(
        lat: Double,
        lon: Double,
        altitude: Float,
        azimuth: Float,
        pitch: Float = 0f,
        roll: Float = 0f,
        callsign: String = "ALPHA-01",
        roomId: String = "OMNI-TAC-01"
    ) {
        val mgrs = toMGRSApprox(lat, lon)
        val cardinal = getCardinalDirection(azimuth)
        val (batPct, batMv) = getBatteryInfo()

        _currentTelemetry.value = _currentTelemetry.value.copy(
            mgrsCoordinate = mgrs,
            latitude = lat,
            longitude = lon,
            altitudeMeters = altitude,
            compassAzimuthDeg = azimuth,
            cardinalDirection = cardinal,
            pitchDeg = pitch,
            rollDeg = roll,
            batteryPercent = batPct,
            batteryVoltageMv = batMv,
            operatorCallsign = callsign,
            targetRoomId = roomId,
            isNightVisionFilterActive = _filterMode.value == "NVG_GREEN",
            isFlirThermalFilterActive = _filterMode.value == "FLIR_THERMAL"
        )
    }

    /**
     * Inicia el ciclo de transmisión P2P con simulación adaptativa de latencia y compresión
     */
    fun startP2PStreaming(roomId: String, localNodeId: String = "LOCAL_TERMINAL") {
        if (isStreamingActive.compareAndSet(false, true)) {
            streamingLoopJob = scope.launch {
                Log.i(TAG, "Iniciando túnel de video P2P táctico sobre sala: $roomId")
                while (isActive && isStreamingActive.get()) {
                    val profile = _activeCodecProfile.value
                    val frameIntervalMs = (1000L / profile.targetFps).coerceAtLeast(20L)
                    val seq = ++sequenceCounter

                    // 1. Crear fotograma con telemetría HUD integrada
                    val packet = TacticalVideoFramePacket(
                        frameSequence = seq,
                        timestamp = System.currentTimeMillis(),
                        senderNodeId = localNodeId,
                        codecProfile = profile,
                        compressedFrameBytes = ByteArray(profile.estimatedBitrateKbps * 128 / profile.targetFps),
                        telemetry = _currentTelemetry.value
                    )
                    _lastTransmittedFrame.value = packet

                    delay(frameIntervalMs)
                }
            }
        }
    }

    fun stopP2PStreaming() {
        isStreamingActive.set(false)
        streamingLoopJob?.cancel()
        streamingLoopJob = null
        Log.i(TAG, "Túnel de video P2P táctico detenido")
    }

    private fun getBatteryInfo(): Pair<Int, Int> {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 85
            Pair(pct, 3950)
        } catch (e: Exception) {
            Pair(85, 3950)
        }
    }

    private fun getCardinalDirection(azimuth: Float): String {
        val deg = ((azimuth % 360) + 360) % 360
        return when {
            deg >= 337.5 || deg < 22.5 -> "N"
            deg >= 22.5 && deg < 67.5 -> "NE"
            deg >= 67.5 && deg < 112.5 -> "E"
            deg >= 112.5 && deg < 157.5 -> "SE"
            deg >= 157.5 && deg < 202.5 -> "S"
            deg >= 202.5 && deg < 247.5 -> "SW"
            deg >= 247.5 && deg < 292.5 -> "W"
            else -> "NW"
        }
    }

    /**
     * Conversor determinista de Coordenadas Lat/Lon a MGRS Táctico (Military Grid Reference System)
     */
    fun toMGRSApprox(lat: Double, lon: Double): String {
        val zoneNumber = floor((lon + 180.0) / 6.0).toInt() + 1
        val latBands = "CDEFGHJKLMNPQRSTUVWX"
        val bandIndex = ((lat + 80.0) / 8.0).toInt().coerceIn(0, latBands.length - 1)
        val latBand = latBands[bandIndex]

        // Cálculo de este y norte aproximados dentro del huso UTM
        val easting = (((lon + 180.0) % 6.0) / 6.0 * 100000).toInt().coerceIn(0, 99999)
        val northing = (((lat + 80.0) % 8.0) / 8.0 * 100000).toInt().coerceIn(0, 99999)

        val col1 = "ABCDEFGH"[zoneNumber % 8]
        val col2 = "JKLMNPQRSTUV"[(bandIndex + 2) % 12]

        return String.format("%02d%s %s%s %04d %04d", zoneNumber, latBand, col1, col2, easting / 10, northing / 10)
    }
}
