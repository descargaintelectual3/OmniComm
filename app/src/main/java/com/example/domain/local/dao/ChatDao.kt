package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.local.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    /**
     * Búsqueda de texto completo (Full-Text Search) en el historial de mensajes de todas las sesiones
     */
    @Query("SELECT * FROM chat_messages WHERE text LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchAllMessages(query: String): Flow<List<ChatMessageEntity>>

    /**
     * Búsqueda de texto completo (Full-Text Search) filtrada por sesión activa
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND (text LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchSessionMessages(sessionId: String, query: String): Flow<List<ChatMessageEntity>>

    /**
     * Obtener mensajes que contienen archivos o imágenes multimedia
     */
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId AND (mediaUrl != '' OR mediaType != '') ORDER BY timestamp DESC")
    fun getMediaMessagesForSession(sessionId: String): Flow<List<ChatMessageEntity>>

    /**
     * Conteo rápido de coincidencias de búsqueda
     */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE text LIKE '%' || :query || '%' OR senderName LIKE '%' || :query || '%'")
    fun countSearchResults(query: String): Flow<Int>

    @Query("SELECT * FROM chat_messages WHERE id = :id LIMIT 1")
    fun getMessageById(id: String): Flow<ChatMessageEntity?>

    @Query("SELECT * FROM chat_messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getPendingMessages(): List<ChatMessageEntity>

    @Query("SELECT * FROM chat_messages WHERE status = 'PENDING' ORDER BY timestamp ASC")
    fun getPendingMessagesFlow(): Flow<List<ChatMessageEntity>>

    @Query("SELECT COUNT(*) FROM chat_messages WHERE status = 'PENDING'")
    fun getPendingCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageEntity>)
    
    @Update
    suspend fun updateMessage(message: ChatMessageEntity)

    @Delete
    suspend fun deleteMessage(message: ChatMessageEntity)

    @Query("UPDATE chat_messages SET status = :newStatus, isRead = :isRead WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, newStatus: String, isRead: Boolean = (newStatus == "READ"))

    @Query("UPDATE chat_messages SET status = 'READ', isRead = 1 WHERE sessionId = :sessionId AND senderId != :myUid AND status != 'READ'")
    suspend fun markSessionMessagesAsRead(sessionId: String, myUid: String)

    @Query("UPDATE chat_messages SET status = 'DELIVERED' WHERE sessionId = :sessionId AND senderId != :myUid AND status = 'SENT'")
    suspend fun markSessionMessagesAsDelivered(sessionId: String, myUid: String)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun clearSessionMessages(sessionId: String)

    @Query("DELETE FROM chat_messages")
    suspend fun clearAll()
}
