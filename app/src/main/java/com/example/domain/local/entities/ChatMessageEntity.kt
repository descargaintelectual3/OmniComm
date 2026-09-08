package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String = "global",
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long,
    val isEncrypted: Boolean = true,
    val status: String = "SENT", // PENDING, SENT, DELIVERED, READ, FAILED
    val mediaUrl: String = "",
    val mediaType: String = "", // "IMAGE", "DOCUMENT", "AUDIO", ""
    val mediaDurationMs: Long = 0L,
    val isRead: Boolean = false,
    val localFilePath: String = "",
    val fileSize: Long = 0L
)
