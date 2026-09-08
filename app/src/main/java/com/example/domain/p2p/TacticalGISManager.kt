package com.example.domain.p2p

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.example.domain.models.GISMarkerType
import com.example.domain.models.TacticalGISMarker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * Gestor de Marcadores GIS, Zonas de Operaciones y Perímetros Tácticos.
 * Permite agregar puntos de encuentro (Rally Points), perímetros de peligro y
 * compartirlos a través de la malla P2P (StoreAndForward).
 */
class TacticalGISManager private constructor() {

    private val _markers = MutableStateFlow<List<TacticalGISMarker>>(
        listOf(
            TacticalGISMarker(
                title = "Punto Alpha (Reunión)",
                type = GISMarkerType.RALLY_POINT,
                latitude = 4.6105,
                longitude = -74.0820,
                radiusMeters = 50f,
                notes = "Punto seguro de reabastecimiento"
            ),
            TacticalGISMarker(
                title = "Zona de Interferencia RF",
                type = GISMarkerType.HAZARD_ZONE,
                latitude = 4.6080,
                longitude = -74.0850,
                radiusMeters = 150f,
                notes = "Ruido electromagnético detectado"
            ),
            TacticalGISMarker(
                title = "Helipuerto / Extracción",
                type = GISMarkerType.EXTRACTION_ZONE,
                latitude = 4.6130,
                longitude = -74.0790,
                radiusMeters = 80f,
                notes = "Área despejada para extracción"
            )
        )
    )
    val markers: StateFlow<List<TacticalGISMarker>> = _markers.asStateFlow()

    private val _selectedMarker = MutableStateFlow<TacticalGISMarker?>(null)
    val selectedMarker: StateFlow<TacticalGISMarker?> = _selectedMarker.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: TacticalGISManager? = null

        fun getInstance(): TacticalGISManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalGISManager().also { INSTANCE = it }
            }
        }
    }

    fun addMarker(
        title: String,
        type: GISMarkerType,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float,
        notes: String
    ): TacticalGISMarker {
        val marker = TacticalGISMarker(
            title = title,
            type = type,
            latitude = latitude,
            longitude = longitude,
            radiusMeters = radiusMeters,
            notes = notes
        )
        _markers.value = _markers.value + marker

        // Difundir en la malla P2P con QoS de Telemetría
        StoreAndForwardRouter.getInstance().enqueueLocalPacket(
            sourceId = "LOCAL_NODE",
            destinationId = "BROADCAST_ALL",
            priority = PacketPriority.EMERGENCY_TELEMETRY,
            payloadType = "GIS_MARKER",
            data = "GIS:${marker.type.name}|${marker.title}|${marker.latitude}|${marker.longitude}|${marker.radiusMeters}",
            ttlHops = 6
        )

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "GIS_MARKER_ADDED",
            message = "Marcador Táctico [${type.title}] agregado y transmitido a la malla."
        )

        return marker
    }

    fun removeMarker(markerId: String) {
        _markers.value = _markers.value.filterNot { it.id == markerId }
        if (_selectedMarker.value?.id == markerId) {
            _selectedMarker.value = null
        }
    }

    fun selectMarker(marker: TacticalGISMarker?) {
        _selectedMarker.value = marker
    }

    /**
     * Calcula distancia en metros entre dos coordenadas (Fórmula Haversine)
     */
    fun calculateDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Radio de la Tierra en metros
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
