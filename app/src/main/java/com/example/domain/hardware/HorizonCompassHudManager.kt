package com.example.domain.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TacticalOrientationData(
    val azimuthDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val cardinalDirection: String = "N",
    val altitudeMeters: Float = 2600f
)

/**
 * Sensor de Horizonte Artificial y Brújula Táctica HUD:
 * Proporciona telemetría inercial y magnética en tiempo real.
 */
class HorizonCompassHudManager private constructor(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(TacticalOrientationData())
    val orientation: StateFlow<TacticalOrientationData> = _orientation.asStateFlow()

    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)

    fun startTracking() {
        rotationSensor?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopTracking() {
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
        SensorManager.getOrientation(rotationMatrix, orientationAngles)

        var azimuth = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f

        val pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()

        val cardinal = when {
            azimuth >= 337.5 || azimuth < 22.5 -> "N"
            azimuth >= 22.5 && azimuth < 67.5 -> "NE"
            azimuth >= 67.5 && azimuth < 112.5 -> "E"
            azimuth >= 112.5 && azimuth < 157.5 -> "SE"
            azimuth >= 157.5 && azimuth < 202.5 -> "S"
            azimuth >= 202.5 && azimuth < 247.5 -> "SW"
            azimuth >= 247.5 && azimuth < 292.5 -> "W"
            else -> "NW"
        }

        _orientation.value = TacticalOrientationData(
            azimuthDegrees = azimuth,
            pitchDegrees = pitch,
            rollDegrees = roll,
            cardinalDirection = cardinal
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    companion object {
        @Volatile
        private var INSTANCE: HorizonCompassHudManager? = null

        fun getInstance(context: Context): HorizonCompassHudManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HorizonCompassHudManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
