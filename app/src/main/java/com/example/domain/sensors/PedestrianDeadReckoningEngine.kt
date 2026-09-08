package com.example.domain.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FASE 6 (Plan Maestro): Motor de Navegación Inercial por Estimación de Pasos (PDR - Pedestrian Dead Reckoning):
 * Permite la navegación táctica continua y cálculo de posición relativa en interiores o bajo guerra electrónica
 * cuando la señal GPS está completamente bloqueada o degradada (GPS-Denied Navigation).
 */
data class PdrPosition(
    val relativeX: Double = 0.0, // Metros Este/Oeste
    val relativeY: Double = 0.0, // Metros Norte/Sur
    val totalDistanceMeters: Double = 0.0,
    val stepCount: Int = 0,
    val currentHeadingDegrees: Float = 0f,
    val estimatedSpeedMps: Float = 0f,
    val isGpsDeniedActive: Boolean = true
)

class PedestrianDeadReckoningEngine private constructor(private val context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val stepDetector = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _pdrState = MutableStateFlow(PdrPosition())
    val pdrState: StateFlow<PdrPosition> = _pdrState.asStateFlow()

    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    private val gravityValues = FloatArray(3)
    private val geomagneticValues = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var averageStepLengthMeters = 0.76 // Longitud promedio de paso estándar táctico

    fun startPdrTracking(stepLengthM: Double = 0.76) {
        if (_isTracking.value) return
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enablePdrNavigation.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "PDR_ENGINE",
                message = "Rastreo inercial PDR omitido: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        _isTracking.value = true
        averageStepLengthMeters = stepLengthM

        accelerometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        magnetometer?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        stepDetector?.let { sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST) }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "PDR_ENGINE",
            message = "Navegación Inercial PDR iniciada en modo GPS-Denied (Paso: ${stepLengthM}m)"
        )
    }

    fun stopPdrTracking() {
        _isTracking.value = false
        sensorManager?.unregisterListener(this)
    }

    fun resetOrigin() {
        _pdrState.value = PdrPosition(
            currentHeadingDegrees = _pdrState.value.currentHeadingDegrees,
            isGpsDeniedActive = true
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "PDR_RESET",
            message = "Origen PDR restablecido a punto cero relativo (0,0)"
        )
    }

    fun simulateStep() {
        registerStep()
    }

    private fun registerStep() {
        val current = _pdrState.value
        val headingRad = Math.toRadians(current.currentHeadingDegrees.toDouble())

        val deltaX = averageStepLengthMeters * sin(headingRad)
        val deltaY = averageStepLengthMeters * cos(headingRad)

        val newDist = current.totalDistanceMeters + averageStepLengthMeters
        val newSteps = current.stepCount + 1

        _pdrState.value = current.copy(
            relativeX = current.relativeX + deltaX,
            relativeY = current.relativeY + deltaY,
            totalDistanceMeters = newDist,
            stepCount = newSteps,
            estimatedSpeedMps = 1.35f
        )
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !_isTracking.value) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_DETECTOR -> {
                registerStep()
            }
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravityValues, 0, 3)
                hasGravity = true
                updateOrientation()
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, geomagneticValues, 0, 3)
                hasGeomagnetic = true
                updateOrientation()
            }
        }
    }

    private fun updateOrientation() {
        if (hasGravity && hasGeomagnetic) {
            val r = FloatArray(9)
            val i = FloatArray(9)
            if (SensorManager.getRotationMatrix(r, i, gravityValues, geomagneticValues)) {
                val orientation = FloatArray(3)
                SensorManager.getOrientation(r, orientation)
                var azimuthDeg = Math.toDegrees(orientation[0].toDouble()).toFloat()
                if (azimuthDeg < 0) azimuthDeg += 360f

                _pdrState.value = _pdrState.value.copy(currentHeadingDegrees = azimuthDeg)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        @Volatile
        private var INSTANCE: PedestrianDeadReckoningEngine? = null

        fun getInstance(context: Context): PedestrianDeadReckoningEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PedestrianDeadReckoningEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
