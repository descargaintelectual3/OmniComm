package com.example.domain.models

import java.util.UUID

enum class GISMarkerType(
    val title: String,
    val iconSymbol: String,
    val colorHex: Long,
    val defaultRadiusMeters: Float
) {
    RALLY_POINT("Punto de Encuentro", "🚩", 0xFF00E5FF, 50f),
    HAZARD_ZONE("Zona de Peligro / Jamming", "⚠️", 0xFFFF5252, 120f),
    EXTRACTION_ZONE("Zona de Extracción (LZ)", "🚁", 0xFF69F0AE, 80f),
    COMMAND_POST("Puesto de Mando (HQ)", "🏰", 0xFFFFD600, 100f),
    SUPPLY_CACHE("Bóveda / Suministros", "📦", 0xFFB388FF, 30f)
}

data class TacticalGISMarker(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: GISMarkerType,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float = type.defaultRadiusMeters,
    val createdBy: String = "Operador Local",
    val timestamp: Long = System.currentTimeMillis(),
    val notes: String = ""
)
