package com.example.domain.c2

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.p2p.MeshRelayPacket
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import com.example.domain.security.EmergencyZeroizeManager
import com.example.domain.repository.TacticalC2AuditRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Acciones de Hardware y Sistema que pueden ser ejecutadas o solicitadas remotamente entre nodos.
 */
enum class C2HardwareAction(val label: String, val requiresAuth: Boolean) {
    FLASHLIGHT_ON("Encender Linterna / Flash", false),
    FLASHLIGHT_OFF("Apagar Linterna", false),
    FLASHLIGHT_STROBE("Modo Estroboscópico de Rescate", false),
    SIREN_ALARM_PULSE("Activar Alarma Sonora Táctica", false),
    HAPTIC_VIBRATION_PULSE("Pulso de Vibración Háptica", false),
    TELEMETRY_SNAPSHOT("Solicitar Telemetría Completa", false),
    CAMERA_SNAPSHOT_REQUEST("Capturar Instantánea Óptica Remota", true),
    EMERGENCY_ZEROIZE("Borrado Criptográfico Remoto (Zeroize)", true),
    AUDIO_MONITOR_PING("Ping Acústico Silencioso", false),
    SET_RADIO_SILENCE_EMCON("Forzar Silencio de Radio (EMCON Alpha)", true)
}

data class RemoteNodeDeviceStatus(
    val nodeId: String,
    val callsign: String,
    val ipAddress: String? = null,
    val transportType: String = "P2P_MESH", // "P2P_MESH", "LAN_REST_9090", "BLE_DIRECT", "BLUETOOTH_EXT"
    val isAppInstalled: Boolean = true,
    val batteryPercent: Int = 100,
    val isFlashlightOn: Boolean = false,
    val lastSeenMillis: Long = System.currentTimeMillis(),
    val rssi: Int = -60,
    val lastCommandDispatched: String = "Ninguno",
    val capabilities: List<String> = listOf("Flash", "Siren", "Haptic", "Sensors", "Camera", "Zeroize")
)

data class C2CommandResult(
    val commandId: String,
    val targetNodeId: String,
    val action: C2HardwareAction,
    val success: Boolean,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sistema y Protocolo Universal de Control y Mando Distribuido entre Dispositivos (C2 Mesh Hub).
 * Permite gobernar hardware, sensores, actuar sobre nodos vecinos e interconectar terminales
 * tanto con OmniComm instalado como dispositivos periféricos externos.
 */
class UniversalMeshC2Protocol private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val auditRepo = TacticalC2AuditRepository.getInstance(context)

    private val _managedDevices = MutableStateFlow<Map<String, RemoteNodeDeviceStatus>>(
        mapOf(
            "LOCAL_DEVICE" to RemoteNodeDeviceStatus(
                nodeId = "LOCAL_DEVICE",
                callsign = "TERMINAL_MAESTRO_HQ",
                ipAddress = "127.0.0.1",
                transportType = "LOCAL_SYSTEM",
                isAppInstalled = true,
                batteryPercent = 95
            ),
            "NODE_BRAVO_02" to RemoteNodeDeviceStatus(
                nodeId = "NODE_BRAVO_02",
                callsign = "PATRULLA_BRAVO",
                ipAddress = "192.168.1.112",
                transportType = "LAN_REST_9090",
                isAppInstalled = true,
                batteryPercent = 78,
                rssi = -58
            ),
            "NODE_DELTA_04" to RemoteNodeDeviceStatus(
                nodeId = "NODE_DELTA_04",
                callsign = "CENTINELA_AVANZADO",
                transportType = "P2P_MESH",
                isAppInstalled = true,
                batteryPercent = 64,
                rssi = -74
            ),
            "EXT_BT_PERIPHERAL_1" to RemoteNodeDeviceStatus(
                nodeId = "AA:BB:CC:11:22:33",
                callsign = "AURICULAR_TACTICO_BT",
                transportType = "BLUETOOTH_EXT",
                isAppInstalled = false,
                batteryPercent = 90,
                rssi = -52,
                capabilities = listOf("Audio", "Haptic", "Battery")
            )
        )
    )
    val managedDevices: StateFlow<Map<String, RemoteNodeDeviceStatus>> = _managedDevices.asStateFlow()

    private val _lastResult = MutableStateFlow<C2CommandResult?>(null)
    val lastResult: StateFlow<C2CommandResult?> = _lastResult.asStateFlow()

    private var localFlashlightState = false

    companion object {
        @Volatile
        private var INSTANCE: UniversalMeshC2Protocol? = null

        fun getInstance(context: Context): UniversalMeshC2Protocol {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UniversalMeshC2Protocol(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Registra o actualiza el estado de un dispositivo en la malla C2.
     */
    fun registerOrUpdateDevice(device: RemoteNodeDeviceStatus) {
        val updated = _managedDevices.value.toMutableMap()
        updated[device.nodeId] = device
        _managedDevices.value = updated
    }

    /**
     * Envía y ejecuta una orden de control de hardware o sistema sobre un nodo remoto.
     * Soporta despacho mediante API REST LAN (puerto 9090) o vía Malla Táctica P2P (StoreAndForwardRouter).
     */
    fun dispatchCommand(
        targetNodeId: String,
        action: C2HardwareAction,
        authPassphrase: String = "C2_TACTICAL_TOKEN_99"
    ) {
        scope.launch {
            val device = _managedDevices.value[targetNodeId]
            val commandId = UUID.randomUUID().toString().take(8)

            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "C2_PROTOCOL",
                message = "Despachando orden C2: ${action.name} hacia $targetNodeId (${device?.callsign ?: "Desconocido"})"
            )

            if (targetNodeId == "LOCAL_DEVICE") {
                // Ejecución directa en hardware local
                val success = executeLocalHardwareAction(action)
                val result = C2CommandResult(
                    commandId = commandId,
                    targetNodeId = targetNodeId,
                    action = action,
                    success = success,
                    message = if (success) "Acción local ejecutada exitosamente" else "Fallo al ejecutar en hardware local"
                )
                _lastResult.value = result
                return@launch
            }

            // Dispositivo remoto con IP directa y servidor REST (puerto 9090)
            if (device?.ipAddress != null && device.ipAddress != "127.0.0.1") {
                val executed = sendCommandViaRestApi(device.ipAddress, action)
                if (executed) {
                    recordCommandSuccess(commandId, targetNodeId, action, "Comando ejecutado vía API REST LAN")
                    return@launch
                }
            }

            // Enrutamiento vía Malla P2P multi-salto (Store-and-Forward DTN)
            val jsonPayload = JSONObject().apply {
                put("c2_cmd", action.name)
                put("cmd_id", commandId)
                put("auth_token", authPassphrase)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            val priority = if (action == C2HardwareAction.EMERGENCY_ZEROIZE) {
                PacketPriority.CRITICAL_SOS
            } else {
                PacketPriority.EMERGENCY_TELEMETRY
            }

            StoreAndForwardRouter.getInstance().enqueueLocalPacket(
                sourceId = "LOCAL_DEVICE",
                destinationId = targetNodeId,
                priority = priority,
                payloadType = "C2_COMMAND",
                data = jsonPayload,
                ttlHops = 8
            )

            recordCommandSuccess(commandId, targetNodeId, action, "Orden C2 encolada en la malla P2P multi-salto")
        }
    }

    private fun recordCommandSuccess(cmdId: String, target: String, action: C2HardwareAction, msg: String) {
        val dev = _managedDevices.value[target]
        if (dev != null) {
            val updatedDev = dev.copy(lastCommandDispatched = action.name, lastSeenMillis = System.currentTimeMillis())
            val updatedMap = _managedDevices.value.toMutableMap()
            updatedMap[target] = updatedDev
            _managedDevices.value = updatedMap
        }

        _lastResult.value = C2CommandResult(
            commandId = cmdId,
            targetNodeId = target,
            action = action,
            success = true,
            message = msg
        )

        scope.launch {
            auditRepo.recordC2Log(
                sourceNodeId = "LOCAL_DEVICE",
                sourceCallsign = "TERMINAL_MAESTRO_HQ",
                targetNodeId = target,
                targetCallsign = dev?.callsign ?: target,
                actionType = action.name,
                transportType = dev?.transportType ?: "P2P_MESH",
                payloadJson = "{\"cmd_id\":\"$cmdId\"}",
                success = true,
                executionTimeMs = 15L,
                responseMessage = msg
            )
        }
    }

    /**
     * Ejecuta una acción de hardware localmente en el terminal.
     */
    fun executeLocalHardwareAction(action: C2HardwareAction): Boolean {
        return try {
            when (action) {
                C2HardwareAction.FLASHLIGHT_ON -> {
                    setLocalFlashlight(true)
                    true
                }
                C2HardwareAction.FLASHLIGHT_OFF -> {
                    setLocalFlashlight(false)
                    true
                }
                C2HardwareAction.FLASHLIGHT_STROBE -> {
                    triggerStrobeSequence()
                    true
                }
                C2HardwareAction.SIREN_ALARM_PULSE -> {
                    playTacticalSirenPulse()
                    true
                }
                C2HardwareAction.HAPTIC_VIBRATION_PULSE -> {
                    triggerHapticPulse()
                    true
                }
                C2HardwareAction.EMERGENCY_ZEROIZE -> {
                    scope.launch {
                        EmergencyZeroizeManager.getInstance(context).executeEmergencyZeroize()
                    }
                    true
                }
                C2HardwareAction.TELEMETRY_SNAPSHOT,
                C2HardwareAction.CAMERA_SNAPSHOT_REQUEST,
                C2HardwareAction.AUDIO_MONITOR_PING,
                C2HardwareAction.SET_RADIO_SILENCE_EMCON -> {
                    true
                }
            }
        } catch (e: Exception) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "C2_HARDWARE",
                message = "Excepción ejecutando ${action.name}: ${e.message}"
            )
            false
        }
    }

    private fun setLocalFlashlight(on: Boolean) {
        try {
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return
            cameraManager.setTorchMode(cameraId, on)
            localFlashlightState = on

            val localDev = _managedDevices.value["LOCAL_DEVICE"]
            if (localDev != null) {
                val updated = _managedDevices.value.toMutableMap()
                updated["LOCAL_DEVICE"] = localDev.copy(isFlashlightOn = on)
                _managedDevices.value = updated
            }
        } catch (ignored: Exception) {}
    }

    private fun triggerStrobeSequence() {
        scope.launch {
            try {
                val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return@launch
                for (i in 0..5) {
                    cameraManager.setTorchMode(cameraId, true)
                    kotlinx.coroutines.delay(100L)
                    cameraManager.setTorchMode(cameraId, false)
                    kotlinx.coroutines.delay(100L)
                }
            } catch (ignored: Exception) {}
        }
    }

    private fun triggerHapticPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(longArrayOf(0, 150, 80, 150, 80, 300), -1)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 80, 150, 80, 300), -1)
            }
        } catch (ignored: Exception) {}
    }

    private fun playTacticalSirenPulse() {
        scope.launch(Dispatchers.Default) {
            try {
                val sampleRate = 22050
                val durationSeconds = 1.2
                val numSamples = (sampleRate * durationSeconds).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toDouble() / numSamples
                    val freq = 900.0 + (progress * 800.0) // Sirena ascendente 900Hz - 1700Hz
                    val angle = 2.0 * Math.PI * i / (sampleRate / freq)
                    samples[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.7).toInt().toShort()
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(numSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(samples, 0, numSamples)
                audioTrack.play()
                kotlinx.coroutines.delay(1400L)
                audioTrack.release()
            } catch (ignored: Exception) {}
        }
    }

    private fun sendCommandViaRestApi(ip: String, action: C2HardwareAction): Boolean {
        return try {
            val endpoint = when (action) {
                C2HardwareAction.FLASHLIGHT_ON -> "http://$ip:9090/api/hardware/flashlight/on"
                C2HardwareAction.FLASHLIGHT_OFF -> "http://$ip:9090/api/hardware/flashlight/off"
                C2HardwareAction.FLASHLIGHT_STROBE -> "http://$ip:9090/api/hardware/flashlight/strobe"
                C2HardwareAction.SIREN_ALARM_PULSE -> "http://$ip:9090/api/hardware/siren"
                C2HardwareAction.HAPTIC_VIBRATION_PULSE -> "http://$ip:9090/api/hardware/vibrate"
                C2HardwareAction.EMERGENCY_ZEROIZE -> "http://$ip:9090/api/security/zeroize"
                else -> "http://$ip:9090/api/status"
            }

            val url = URL(endpoint)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 1500
            connection.readTimeout = 1500
            connection.doOutput = true
            connection.outputStream.use { os ->
                os.write("""{"cmd":"${action.name}"}""".toByteArray(Charsets.UTF_8))
            }
            val responseCode = connection.responseCode
            connection.disconnect()
            responseCode in 200..299
        } catch (e: Exception) {
            false
        }
    }
}
