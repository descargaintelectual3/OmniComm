package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * FASE 13 (Plan Maestro): Perímetro de Seguridad Geofence 3D (3D Safe Bubble Proximity Radar):
 * Establece un volumen esférico tridimensional de seguridad (Radio Horizontal + Altitud Vertical)
 * alrededor del operador o campamento táctico, disparando alertas de brecha de perímetro si un nodo o contacto
 * entra sin autorización en la burbuja.
 */
data class SafeBubbleTarget(
    val id: String,
    val callsign: String,
    val distanceMeters: Float,
    val relativeAltitudeMeters: Float,
    val azimuthDegrees: Float,
    val isAuthorizedFriendly: Boolean,
    val isBreachingPerimeter: Boolean
)

data class SafeBubble3DConfig(
    val radiusHorizontalMeters: Float = 50.0f,
    val radiusVerticalMeters: Float = 20.0f,
    val isPerimeterAlarmArmed: Boolean = true,
    val activeBreachesCount: Int = 0,
    val monitoredTargets: List<SafeBubbleTarget> = emptyList()
)

class SafeBubble3DProximityRadar private constructor(private val context: Context) {

    private val _bubbleState = MutableStateFlow(
        SafeBubble3DConfig(
            monitoredTargets = listOf(
                SafeBubbleTarget("T-1", "OPERATOR-BLUE-2", 34f, 2f, 45f, isAuthorizedFriendly = true, isBreachingPerimeter = false),
                SafeBubbleTarget("T-2", "UNIDENTIFIED-DRONE", 42f, 15f, 280f, isAuthorizedFriendly = false, isBreachingPerimeter = true),
                SafeBubbleTarget("T-3", "SCOUT-BLUE-3", 85f, -4f, 160f, isAuthorizedFriendly = true, isBreachingPerimeter = false)
            ),
            activeBreachesCount = 1
        )
    )
    val bubbleState: StateFlow<SafeBubble3DConfig> = _bubbleState.asStateFlow()

    fun updatePerimeterRadius(horizontalM: Float, verticalM: Float) {
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableSafeBubble3D.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "SAFE_BUBBLE",
                message = "Actualización de perímetro 3D omitida: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        val current = _bubbleState.value
        val updatedTargets = current.monitoredTargets.map { target ->
            val isBreach = !target.isAuthorizedFriendly &&
                    target.distanceMeters <= horizontalM &&
                    Math.abs(target.relativeAltitudeMeters) <= verticalM
            target.copy(isBreachingPerimeter = isBreach)
        }
        val breaches = updatedTargets.count { it.isBreachingPerimeter }

        _bubbleState.value = current.copy(
            radiusHorizontalMeters = horizontalM,
            radiusVerticalMeters = verticalM,
            monitoredTargets = updatedTargets,
            activeBreachesCount = breaches
        )

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = if (breaches > 0) LogSeverity.WARNING else LogSeverity.INFO,
            tag = "SAFE_BUBBLE_3D",
            message = "Perímetro 3D actualizado (${horizontalM}m horiz, ${verticalM}m vert). Brechas activas: $breaches"
        )
    }

    fun toggleAlarmArmed(arm: Boolean) {
        _bubbleState.value = _bubbleState.value.copy(isPerimeterAlarmArmed = arm)
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SAFE_BUBBLE_3D",
            message = "Alarma de perímetro 3D: ${if (arm) "ARMADA" else "DESARMADA"}"
        )
    }

    fun simulateHostileIncursion() {
        val current = _bubbleState.value
        val incursionTarget = SafeBubbleTarget(
            id = "INCURSION-${System.currentTimeMillis() % 1000}",
            callsign = "INTRUDER-PROX-01",
            distanceMeters = 18f,
            relativeAltitudeMeters = 1.2f,
            azimuthDegrees = 90f,
            isAuthorizedFriendly = false,
            isBreachingPerimeter = true
        )
        val list = current.monitoredTargets.toMutableList()
        list.add(0, incursionTarget)

        _bubbleState.value = current.copy(
            monitoredTargets = list,
            activeBreachesCount = list.count { it.isBreachingPerimeter }
        )

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.ERROR,
            tag = "PERIMETER_BREACH",
            message = "¡ALERTA DE SEGURIDAD! Intrusión detectada en Burbuja 3D: INTRUDER-PROX-01 a 18m"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SafeBubble3DProximityRadar? = null

        fun getInstance(context: Context): SafeBubble3DProximityRadar {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SafeBubble3DProximityRadar(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
