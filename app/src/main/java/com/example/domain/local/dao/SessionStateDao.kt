package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import com.example.domain.local.entities.UnifiedSessionStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionStateDao {
    @Query("SELECT * FROM unified_session_states ORDER BY lastActivityTimestamp DESC LIMIT 1")
    fun getLatestActiveSession(): Flow<UnifiedSessionStateEntity?>

    @Query("SELECT * FROM unified_session_states WHERE originDeviceId != :localNodeId ORDER BY lastActivityTimestamp DESC LIMIT 1")
    fun getLatestRemoteSession(localNodeId: String): Flow<UnifiedSessionStateEntity?>

    @Query("SELECT * FROM unified_session_states ORDER BY lastActivityTimestamp DESC")
    fun getAllSessions(): Flow<List<UnifiedSessionStateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: UnifiedSessionStateEntity)

    @Query("DELETE FROM unified_session_states WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM unified_session_states WHERE lastActivityTimestamp < :expireBefore")
    suspend fun purgeExpiredSessions(expireBefore: Long)
}
