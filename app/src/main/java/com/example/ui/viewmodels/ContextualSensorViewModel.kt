package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.domain.hardware.SensorFusionEngine

class ContextualSensorViewModel(application: Application) : AndroidViewModel(application) {
    private val sensorEngine = SensorFusionEngine(application).apply { startListening() }

    val ambientLight = sensorEngine.ambientLight
    val isProximityNear = sensorEngine.isProximityNear
    val movementLevel = sensorEngine.movementLevel

    override fun onCleared() {
        super.onCleared()
        sensorEngine.stopListening()
    }
}
