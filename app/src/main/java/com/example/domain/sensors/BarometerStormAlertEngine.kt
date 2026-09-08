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
import kotlin.math.pow

/**
 * FASE 11 (Plan Maestro): Barómetro Táctico, Altitud y Alerta de Tormenta / Despresurización:
 * Monitorea presión atmosférica (hPa / mbar), calcula altitud barométrica respecto al nivel del mar (QNH)
 * y detecta caídas bruscas de presión (>2.5 hPa/3h) indicativas de frentes de tormenta o despresurización.
 */
data class BarometricAtmosphere(
    val pressureHpa: Float = 1013.25f,
    val estimatedAltitudeMeters: Float = 120.0f,
    val pressureTrendHpaPerHr: Float = -0.2f,
    val stormAlertActive: Boolean = false,
    val weatherConditionSummary: String = "ESTABLE / DESPEJADO",
    val elevationDeltaFromBaseMeters: Float = 0.0f
)

class BarometerStormAlertEngine private constructor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val pressureSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PRESSURE)

    private val _atmosphereState = MutableStateFlow(BarometricAtmosphere())
    val atmosphereState: StateFlow<BarometricAtmosphere> = _atmosphereState.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    val isMonitoring: StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private var basePressureHpa = 1013.25f
    private val pressureHistory = mutableListOf<Pair<Long, Float>>()
    private var monitorJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun startBarometerMonitoring() {
        if (_isMonitoring.value) return
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableBarometerStorm.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "BAROMETER_ENGINE",
                message = "Monitoreo barométrico omitido: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        _isMonitoring.value = true

        pressureSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "BAROMETER_ENGINE",
            message = "Monitor barométrico y alerta de tormenta activado (Sensor de presión atmosférica registrado)"
        )

        monitorJob = scope.launch {
            while (_isMonitoring.value) {
                evaluateTrendAndAlerts()
                delay(3000)
            }
        }
    }

    fun calibrateBaseElevation() {
        basePressureHpa = _atmosphereState.value.pressureHpa
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "BAROMETER_CALIB",
            message = "Presión base QNH calibrada a ${basePressureHpa} hPa"
        )
    }

    fun simulateStormPressureDrop() {
        val current = _atmosphereState.value
        val dropPressure = (current.pressureHpa - 4.2f).coerceAtLeast(920f)
        _atmosphereState.value = current.copy(
            pressureHpa = dropPressure,
            pressureTrendHpaPerHr = -3.8f,
            stormAlertActive = true,
            weatherConditionSummary = "ALERTA: FRENTE DE TORMENTA SEVERO (Caída rápida de presión)"
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.ERROR,
            tag = "STORM_ALERT",
            message = "¡ALERTA METEOROLÓGICA TÁCTICA! Caída de presión detectada (-3.8 hPa/h)"
        )
    }

    fun stopBarometerMonitoring() {
        _isMonitoring.value = false
        monitorJob?.cancel()
        sensorManager?.unregisterListener(this)
    }

    private fun evaluateTrendAndAlerts() {
        val now = System.currentTimeMillis()
        val currentHpa = _atmosphereState.value.pressureHpa
        pressureHistory.add(Pair(now, currentHpa))

        while (pressureHistory.size > 60) {
            pressureHistory.removeAt(0)
        }

        val alt = calculateAltitude(currentHpa)
        val deltaElevation = calculateAltitude(currentHpa) - calculateAltitude(basePressureHpa)

        val trend = if (pressureHistory.size >= 5) {
            val oldest = pressureHistory.first().second
            (currentHpa - oldest)
        } else -0.1f

        val isStorm = trend < -2.5f

        val summary = when {
            isStorm -> "ALERTA: FRENTE DE TORMENTA / BAJA PRESIÓN"
            currentHpa > 1020f -> "ALTA PRESIÓN / TIEMPO ESTABLE"
            currentHpa < 1000f -> "INESTABILIDAD / POSIBLE PRECIPITACIÓN"
            else -> "NOMINAL / CONDICIONES OPERATIVAS"
        }

        _atmosphereState.value = _atmosphereState.value.copy(
            estimatedAltitudeMeters = alt,
            pressureTrendHpaPerHr = trend,
            stormAlertActive = isStorm,
            weatherConditionSummary = summary,
            elevationDeltaFromBaseMeters = deltaElevation
        )
    }

    private fun calculateAltitude(pressureHpa: Float): Float {
        return 44330.0f * (1.0f - (pressureHpa / 1013.25f).pow(0.190295f))
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isMonitoring.value) return
        if (event.sensor.type == Sensor.TYPE_PRESSURE) {
            val hpa = event.values[0]
            val alt = calculateAltitude(hpa)
            _atmosphereState.value = _atmosphereState.value.copy(
                pressureHpa = hpa,
                estimatedAltitudeMeters = alt
            )
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        @Volatile
        private var INSTANCE: BarometerStormAlertEngine? = null

        fun getInstance(context: Context): BarometerStormAlertEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BarometerStormAlertEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
