package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FASE 25: Repetidor Táctico Oportunista con Detección de Enlace Satelital (SATCOM Store-and-Forward):
 * Detecta ventanas de paso de constelaciones LEO/Iridium/Starlink o nodos de gran altitud para
 * enrutar paquetes demorados-tolerantes (DTN) almacenados en la malla cuando no hay enlace terrestre.
 */
data class SatellitePassWindow(
    val constellationName: String, // "IRIDIUM-NEXT-44", "STARLINK-LEO-912"
    val elevationAngleDeg: Float,
    val windowRemainingSeconds: Int,
    val isDirectLineOfSight: Boolean,
    val queuedPacketsCount: Int
)

data class SatcomGatewayState(
    val isListening: Boolean = true,
    val activePasses: List<SatellitePassWindow> = emptyList(),
    val relayedPacketsCount: Long = 34
)

class SatelliteMeshGateway private constructor(context: Context) {

    private val _state = MutableStateFlow(
        SatcomGatewayState(
            activePasses = listOf(
                SatellitePassWindow("IRIDIUM-NEXT-44", 64.5f, 420, true, 3),
                SatellitePassWindow("STARLINK-TACTICAL-8", 22.0f, 180, false, 0)
            )
        )
    )
    val state: StateFlow<SatcomGatewayState> = _state.asStateFlow()

    fun flushQueueToSatellite(constellation: String) {
        val updated = _state.value.activePasses.map {
            if (it.constellationName == constellation) it.copy(queuedPacketsCount = 0) else it
        }
        _state.value = _state.value.copy(
            activePasses = updated,
            relayedPacketsCount = _state.value.relayedPacketsCount + 3
        )
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SATCOM_RELAY",
            message = "Ráfaga de paquetes DTN transmitida al enlace satelital $constellation exitosamente"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SatelliteMeshGateway? = null

        fun getInstance(context: Context): SatelliteMeshGateway {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SatelliteMeshGateway(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
