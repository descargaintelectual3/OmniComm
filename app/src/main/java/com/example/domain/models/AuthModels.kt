package com.example.domain.models

import java.util.UUID

/**
 * Perfil de usuario sincronizado en Firestore (/users/{uid})
 */
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val fcmToken: String = "",
    val publicKeyFingerprint: String = "",
    val rawPublicKeyBase64: String = "",
    val isOnline: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val statusMessage: String = "Nodo Táctico Operacional",
    val batteryPercent: Int = 100,
    val roles: List<String> = listOf("OPERATOR", "MESH_NODE")
)

/**
 * Sesión de chat cifrado extremo a extremo (E2EE) en Firestore (/chat_sessions/{sessionId})
 */
data class EncryptedChatSession(
    val sessionId: String = UUID.randomUUID().toString(),
    val participantUids: List<String> = emptyList(),
    val participantNames: Map<String, String> = emptyMap(),
    val title: String = "",
    val groupDescription: String = "",
    val groupIcon: String = "👥",
    val isGroup: Boolean = false,
    val adminUids: List<String> = emptyList(),
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val sessionKeyFingerprint: String = "",
    val createdByUid: String = "",
    val createdAtTimestamp: Long = System.currentTimeMillis()
)

/**
 * Paquete de mensaje cifrado para sesiones E2EE en Firestore (/chat_sessions/{sessionId}/messages/{messageId})
 */
data class EncryptedSessionMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val ciphertextBase64: String = "", // Cifrado AES-256 GCM o RSA
    val ivBase64: String = "",
    val plainTextFallback: String = "", // Para interoperabilidad o texto plano local
    val keyFingerprint: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val deliveryStatus: String = "SENT", // SENT, DELIVERED, READ
    val readByUids: List<String> = emptyList(),
    val mediaUrl: String = "", // URL de Firebase Storage
    val mediaType: String = "", // "IMAGE", "DOCUMENT", "AUDIO", ""
    val mediaDurationMs: Long = 0L, // Para notas de voz / audio
    val localFilePath: String = "",
    val fileSize: Long = 0L,
    val isDoubleRatchet: Boolean = false,
    val ratchetEphemeralPubKey: String = "",
    val ratchetStep: Int = 0,
    val ratchetHmacTag: String = ""
)

/**
 * Modelo para notificaciones y alertas tácticas en Firestore
 */
data class TacticalNotificationPayload(
    val id: String = UUID.randomUUID().toString(),
    val targetUid: String = "",
    val senderUid: String = "",
    val senderName: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "CHAT_MESSAGE", // "CHAT_MESSAGE", "CONTACT_REQUEST", "TACTICAL_ALERT"
    val sessionId: String = "",
    val mediaUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

/**
 * Estado de autenticación del usuario
 */
sealed interface AuthUiState {
    data object Idle : AuthUiState
    data object Loading : AuthUiState
    data class Authenticated(val user: UserProfile) : AuthUiState
    data class Unauthenticated(val message: String? = null) : AuthUiState
    data class Error(val errorMessage: String) : AuthUiState
}
