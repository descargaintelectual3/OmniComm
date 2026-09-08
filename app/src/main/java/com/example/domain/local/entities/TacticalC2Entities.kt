package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Entidad de Registro y Auditoría C2 Criptográfica.
 * Almacena en base de datos cifrada (SQLCipher) cada orden enviada o recibida en la malla táctica.
 */
@Entity(tableName = "tactical_c2_logs")
data class TacticalC2LogEntity(
    @PrimaryKey val logId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val sourceNodeId: String,
    val sourceCallsign: String,
    val targetNodeId: String,
    val targetCallsign: String,
    val actionType: String,
    val transportType: String,
    val payloadJson: String = "{}",
    val success: Boolean,
    val executionTimeMs: Long = 0L,
    val responseMessage: String
)

/**
 * Entidad de Registro Histórico de Telemetría de Drones y Aeronaves MAVLink.
 */
@Entity(tableName = "tactical_drone_telemetry")
data class TacticalDroneTelemetryEntity(
    @PrimaryKey val telemetryId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val uavName: String,
    val lat: Double,
    val lon: Double,
    val altAglMeters: Float,
    val headingDeg: Float,
    val batteryPercent: Int,
    val flightMode: String
)

/**
 * Entidad de Objetivos Clasificados por Visión Artificial Táctica.
 */
@Entity(tableName = "tactical_detected_targets")
data class TacticalDetectedTargetEntity(
    @PrimaryKey val targetId: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val classification: String,
    val confidence: Float,
    val threatLevel: String, // HOSTILE, FRIENDLY, UNKNOWN, NEUTRAL
    val estimatedRangeMeters: Float,
    val azimuthDeg: Float,
    val lat: Double,
    val lon: Double
)
