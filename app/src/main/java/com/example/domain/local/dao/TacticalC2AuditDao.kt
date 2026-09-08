package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.domain.local.entities.TacticalC2LogEntity
import com.example.domain.local.entities.TacticalDetectedTargetEntity
import com.example.domain.local.entities.TacticalDroneTelemetryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO para auditoría de órdenes de mando C2, telemetría de drones y objetivos de visión artificial.
 */
@Dao
interface TacticalC2AuditDao {

    // --- C2 Logs ---
    @Query("SELECT * FROM tactical_c2_logs ORDER BY timestamp DESC")
    fun getAllC2Logs(): Flow<List<TacticalC2LogEntity>>

    @Query("SELECT * FROM tactical_c2_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getLatestC2Logs(limit: Int = 100): Flow<List<TacticalC2LogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertC2Log(log: TacticalC2LogEntity)

    @Query("DELETE FROM tactical_c2_logs")
    suspend fun clearC2Logs()

    // --- Telemetría de Drones ---
    @Query("SELECT * FROM tactical_drone_telemetry WHERE uavName = :uavName ORDER BY timestamp DESC LIMIT :limit")
    fun getDroneTelemetryHistory(uavName: String, limit: Int = 50): Flow<List<TacticalDroneTelemetryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDroneTelemetry(telemetry: TacticalDroneTelemetryEntity)

    // --- Objetivos Detectados ---
    @Query("SELECT * FROM tactical_detected_targets ORDER BY timestamp DESC LIMIT :limit")
    fun getDetectedTargets(limit: Int = 50): Flow<List<TacticalDetectedTargetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDetectedTarget(target: TacticalDetectedTargetEntity)

    @Query("DELETE FROM tactical_detected_targets WHERE targetId = :targetId")
    suspend fun deleteDetectedTarget(targetId: String)
}
