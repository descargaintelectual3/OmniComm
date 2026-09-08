package com.example.domain.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

enum class EmergencyStatus {
    STANDBY,
    COUNTDOWN_ACTIVE,
    SOS_TRANSMITTING,
    CANCELLED
}

data class EmergencyAlertData(
    val alertId: String,
    val operatorName: String,
    val latitude: Double,
    val longitude: Double,
    val triggerReason: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Detector Táctico de Emergencia "Man Down" / Caída & Impacto Brusco.
 * Monitorea picos de aceleración G seguidos de inmovilidad y activa una cuenta regresiva
 * antes de emitir una baliza SOS de máxima prioridad (QoS 1) a través de la malla P2P y Firestore.
 */
class TacticalManDownDetector private constructor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vm?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _emergencyStatus = MutableStateFlow(EmergencyStatus.STANDBY)
    val emergencyStatus: StateFlow<EmergencyStatus> = _emergencyStatus.asStateFlow()

    private val _countdownSeconds = MutableStateFlow(10)
    val countdownSeconds: StateFlow<Int> = _countdownSeconds.asStateFlow()

    private val _lastAlert = MutableStateFlow<EmergencyAlertData?>(null)
    val lastAlert: StateFlow<EmergencyAlertData?> = _lastAlert.asStateFlow()

    private val _isMonitoringActive = MutableStateFlow(true)
    val isMonitoringActive: StateFlow<Boolean> = _isMonitoringActive.asStateFlow()

    private var countdownJob: Job? = null
    private var lastImpactTime = 0L

    companion object {
        @Volatile
        private var INSTANCE: TacticalManDownDetector? = null

        fun getInstance(context: Context): TacticalManDownDetector {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalManDownDetector(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableManDownAlert.value) {
            _isMonitoringActive.value = false
            return
        }
        _isMonitoringActive.value = true
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopMonitoring() {
        _isMonitoringActive.value = false
        sensorManager.unregisterListener(this)
        cancelCountdown()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (!_isMonitoringActive.value || _emergencyStatus.value != EmergencyStatus.STANDBY) return
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]
            val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH

            // Umbral de impacto táctico: fuerza G superior a 3.2G
            if (gForce > 3.2 && System.currentTimeMillis() - lastImpactTime > 5000) {
                lastImpactTime = System.currentTimeMillis()
                triggerEmergencyCountdown("Impacto / Caída Táctica Detectada (${String.format("%.1f", gForce)}G)")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    /**
     * Inicia la cuenta regresiva antes de enviar la baliza SOS
     */
    fun triggerEmergencyCountdown(reason: String) {
        if (_emergencyStatus.value == EmergencyStatus.COUNTDOWN_ACTIVE) return

        _emergencyStatus.value = EmergencyStatus.COUNTDOWN_ACTIVE
        _countdownSeconds.value = 10

        vibratePattern()

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.WARNING,
            tag = "MAN_DOWN_ALERT",
            message = "Alerta $reason. Cuenta regresiva iniciada (10s)."
        )

        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (i in 10 downTo 1) {
                _countdownSeconds.value = i
                vibratePulse()
                delay(1000)
            }
            // Si no fue cancelado por el operador, transmitir SOS
            broadcastEmergencySOS(reason)
        }
    }

    /**
     * Cancela la alarma si fue una falsa alarma
     */
    fun cancelCountdown() {
        countdownJob?.cancel()
        _emergencyStatus.value = EmergencyStatus.CANCELLED
        scope.launch {
            delay(1500)
            _emergencyStatus.value = EmergencyStatus.STANDBY
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SOS_CANCELLED",
            message = "Alerta SOS cancelada por el operador."
        )
    }

    /**
     * Transmite de inmediato la señal SOS de emergencia con QoS Crítico
     */
    fun broadcastEmergencySOS(reason: String) {
        countdownJob?.cancel()
        _emergencyStatus.value = EmergencyStatus.SOS_TRANSMITTING

        val alert = EmergencyAlertData(
            alertId = "SOS-${System.currentTimeMillis()}",
            operatorName = "Operador Táctico",
            latitude = 4.6097, // Coordenadas de prueba/GPS
            longitude = -74.0817,
            triggerReason = reason,
            timestamp = System.currentTimeMillis()
        )
        _lastAlert.value = alert

        // Encolar en el enrutador multi-salto con QoS 1 (CRITICAL_SOS)
        StoreAndForwardRouter.getInstance().enqueueLocalPacket(
            sourceId = "LOCAL_NODE",
            destinationId = "BROADCAST_ALL",
            priority = PacketPriority.CRITICAL_SOS,
            payloadType = "SOS",
            data = "MAYDAY MAYDAY: ${alert.triggerReason} @ [${alert.latitude}, ${alert.longitude}]",
            ttlHops = 10
        )

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.ERROR,
            tag = "SOS_BROADCAST",
            message = "🚨 BALIZA SOS EN TRANSMISIÓN MALLA (QoS: CRITICAL_SOS, Motivo: $reason)"
        )
    }

    fun dismissActiveSOS() {
        _emergencyStatus.value = EmergencyStatus.STANDBY
    }

    private fun vibratePulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
        } catch (_: Exception) {}
    }

    private fun vibratePattern() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 300, 150, 300)
                vibrator?.vibrate(VibrationEffect.createWaveform(timings, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 300, 150, 300), -1)
            }
        } catch (_: Exception) {}
    }
}
