package com.example.domain.repository

import android.content.Context
import com.example.domain.local.OmniDatabase
import com.example.domain.local.dao.TacticalC2AuditDao
import com.example.domain.local.entities.TacticalC2LogEntity
import com.example.domain.local.entities.TacticalDetectedTargetEntity
import com.example.domain.local.entities.TacticalDroneTelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio de Auditoría C2 y Registro Táctico Cifrado.
 */
class TacticalC2AuditRepository private constructor(private val dao: TacticalC2AuditDao) {

    val recentC2Logs: Flow<List<TacticalC2LogEntity>> = dao.getLatestC2Logs(100)
    val detectedTargets: Flow<List<TacticalDetectedTargetEntity>> = dao.getDetectedTargets(50)

    suspend fun recordC2Log(
        sourceNodeId: String,
        sourceCallsign: String,
        targetNodeId: String,
        targetCallsign: String,
        actionType: String,
        transportType: String,
        payloadJson: String = "{}",
        success: Boolean,
        executionTimeMs: Long = 0L,
        responseMessage: String
    ) {
        val entity = TacticalC2LogEntity(
            sourceNodeId = sourceNodeId,
            sourceCallsign = sourceCallsign,
            targetNodeId = targetNodeId,
            targetCallsign = targetCallsign,
            actionType = actionType,
            transportType = transportType,
            payloadJson = payloadJson,
            success = success,
            executionTimeMs = executionTimeMs,
            responseMessage = responseMessage
        )
        dao.insertC2Log(entity)
    }

    suspend fun recordDroneTelemetry(
        uavName: String,
        lat: Double,
        lon: Double,
        altAglMeters: Float,
        headingDeg: Float,
        batteryPercent: Int,
        flightMode: String
    ) {
        val entity = TacticalDroneTelemetryEntity(
            uavName = uavName,
            lat = lat,
            lon = lon,
            altAglMeters = altAglMeters,
            headingDeg = headingDeg,
            batteryPercent = batteryPercent,
            flightMode = flightMode
        )
        dao.insertDroneTelemetry(entity)
    }

    suspend fun recordDetectedTarget(
        classification: String,
        confidence: Float,
        threatLevel: String,
        estimatedRangeMeters: Float,
        azimuthDeg: Float,
        lat: Double,
        lon: Double
    ) {
        val entity = TacticalDetectedTargetEntity(
            classification = classification,
            confidence = confidence,
            threatLevel = threatLevel,
            estimatedRangeMeters = estimatedRangeMeters,
            azimuthDeg = azimuthDeg,
            lat = lat,
            lon = lon
        )
        dao.insertDetectedTarget(entity)
    }

    suspend fun clearAuditHistory() {
        dao.clearC2Logs()
    }

    companion object {
        @Volatile
        private var INSTANCE: TacticalC2AuditRepository? = null

        fun getInstance(context: Context): TacticalC2AuditRepository {
            return INSTANCE ?: synchronized(this) {
                val db = OmniDatabase.getDatabase(context)
                INSTANCE ?: TacticalC2AuditRepository(db.tacticalC2AuditDao()).also { INSTANCE = it }
            }
        }
    }
}
