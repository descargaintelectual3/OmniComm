package com.example.domain.hardware

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.sqrt

/**
 * Motor de Fusión de Sensores (Reactivo).
 * Monitorea Luz Ambiental, Acelerómetro y Proximidad, exponiendo sus estados
 * como flujos de datos (StateFlow) para disparar acciones contextuales en la app.
 */
class SensorFusionEngine(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _ambientLight = MutableStateFlow(0f)
    val ambientLight: StateFlow<Float> = _ambientLight

    private val _isProximityNear = MutableStateFlow(false)
    val isProximityNear: StateFlow<Boolean> = _isProximityNear

    private val _movementLevel = MutableStateFlow("Inmóvil")
    val movementLevel: StateFlow<String> = _movementLevel

    fun startListening() {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopListening() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        when (event?.sensor?.type) {
            Sensor.TYPE_LIGHT -> {
                _ambientLight.value = event.values[0]
            }
            Sensor.TYPE_PROXIMITY -> {
                // Típicamente < 5cm se considera "Cerca"
                _isProximityNear.value = event.values[0] < 5f
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                _movementLevel.value = when {
                    gForce > 2.5 -> "Movimiento Brusco"
                    gForce > 1.2 -> "Caminando"
                    else -> "Inmóvil"
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Calibración térmica/magnética ignorada por ahora
    }
}
