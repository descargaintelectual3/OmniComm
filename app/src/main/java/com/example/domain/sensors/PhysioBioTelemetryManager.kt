package com.example.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * FASE 4: Gestor de Telemetría Biológica y Resistencia del Operador:
 * Evalúa aceleración corporal, cadencia de paso, nivel de agitación cinemática,
 * índice de estrés térmico estimado y cálculo de Combat Readiness Score (0-100%).
 */
data class OperatorBioMetrics(
    val estimatedHeartRateBpm: Int = 74,
    val stressIndex: Float = 0.22f, // 0.0 a 1.0
    val stepCadenceSpm: Int = 0,    // Steps Per Minute
    val combatReadinessScore: Int = 94, // 0 a 100%
    val thermalDiscomfortLevel: String = "NOMINAL (21°C)",
    val fatigueStatus: String = "ÓPTIMO / ALERTA"
)

class PhysioBioTelemetryManager private constructor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val stepCounterSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

    private val _bioMetrics = MutableStateFlow(OperatorBioMetrics())
    val bioMetrics: StateFlow<OperatorBioMetrics> = _bioMetrics.asStateFlow()

    private val _isBioTrackingActive = MutableStateFlow(false)
    val isBioTrackingActive: StateFlow<Boolean> = _isBioTrackingActive.asStateFlow()

    private var lastAccMagnitude = 9.81f
    private var activityJitterAcc = 0.0f
    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startBioTracking() {
        if (_isBioTrackingActive.value) return
        _isBioTrackingActive.value = true

        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        stepCounterSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "PHYSIO_BIO",
            message = "Telemetría biométrica del operador activada (Fusión inercial + Stress Vector)"
        )

        trackingJob = scope.launch {
            while (_isBioTrackingActive.value) {
                updateComputedBiometrics()
                delay(2000)
            }
        }
    }

    private fun updateComputedBiometrics() {
        val jitter = activityJitterAcc.coerceIn(0f, 15f)
        activityJitterAcc = 0f // reset temporal

        // Modelado bio-dinámico de pulso y estrés en combate
        val baseBpm = 68 + (jitter * 6.5f).toInt()
        val estimatedBpm = baseBpm.coerceIn(60, 185)

        val stressFactor = (jitter / 12f).coerceIn(0.05f, 0.95f)
        val readiness = (100 - (stressFactor * 35).toInt() - if (estimatedBpm > 150) 25 else 0).coerceIn(10, 100)

        val fatigueStr = when {
            readiness > 80 -> "ÓPTIMO / ALERTA"
            readiness > 50 -> "DESGASTE MODERADO"
            else -> "CRÍTICO / FATIGA EXTREMA"
        }

        _bioMetrics.value = OperatorBioMetrics(
            estimatedHeartRateBpm = estimatedBpm,
            stressIndex = stressFactor,
            stepCadenceSpm = (jitter * 14).toInt().coerceIn(0, 160),
            combatReadinessScore = readiness,
            thermalDiscomfortLevel = if (jitter > 8f) "ELEVADO (+32°C Est.)" else "NOMINAL (22°C)",
            fatigueStatus = fatigueStr
        )
    }

    fun stopBioTracking() {
        _isBioTrackingActive.value = false
        trackingJob?.cancel()
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]
            val mag = sqrt(ax * ax + ay * ay + az * az)
            val delta = kotlin.math.abs(mag - lastAccMagnitude)
            lastAccMagnitude = mag
            activityJitterAcc += delta
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        @Volatile
        private var INSTANCE: PhysioBioTelemetryManager? = null

        fun getInstance(context: Context): PhysioBioTelemetryManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PhysioBioTelemetryManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
