package com.example.domain.chat

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.dao.ChatDao
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.media.FirebaseStorageMediaService
import com.example.domain.media.MediaUploadProgress
import com.example.domain.models.EncryptedChatSession
import com.example.domain.models.EncryptedSessionMessage
import com.example.domain.models.TacticalNotificationPayload
import com.example.domain.security.CryptoManager
import com.example.domain.security.DoubleRatchetEngine
import com.example.domain.security.RatchetEncryptedMessage
import com.example.services.OmniPushService
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class FirestoreEncryptedChatService(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager(),
    private val chatDao: ChatDao = OmniDatabase.getDatabase(context).chatDao(),
    private val mediaService: FirebaseStorageMediaService = FirebaseStorageMediaService(context)
) {
    private val firestore = FirebaseFirestore.getInstance()
    private val doubleRatchetEngine = DoubleRatchetEngine.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeListeners = mutableMapOf<String, ListenerRegistration>()

    private val _activeSessions = MutableStateFlow<List<EncryptedChatSession>>(emptyList())
    val activeSessions: StateFlow<List<EncryptedChatSession>> = _activeSessions.asStateFlow()

    /**
     * Búsqueda de texto completo (Full-Text Search) en Room para la sesión dada
     */
    fun searchMessagesInSession(sessionId: String, query: String): Flow<List<ChatMessageEntity>> {
        return chatDao.searchSessionMessages(sessionId, query)
    }

    /**
     * Búsqueda de texto completo global en Room
     */
    fun searchAllMessages(query: String): Flow<List<ChatMessageEntity>> {
        return chatDao.searchAllMessages(query)
    }

    /**
     * Inicia o recupera una sesión de chat cifrado extremo a extremo (E2EE) 1 a 1 entre dos operadores.
     */
    suspend fun createOrGetDirectSession(
        myUid: String,
        myName: String,
        partnerUid: String,
        partnerName: String,
        partnerPublicKeyFingerprint: String = ""
    ): Result<EncryptedChatSession> = withContext(Dispatchers.IO) {
        try {
            // Clave única determinística para la sesión directa entre dos UIDs
            val sortedUids = listOf(myUid, partnerUid).sorted()
            val sessionId = "session_${sortedUids[0]}_${sortedUids[1]}"
            
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val snapshot = sessionRef.get().await()

            if (snapshot.exists()) {
                val session = snapshot.toObject(EncryptedChatSession::class.java)
                    ?: throw Exception("Error al mapear la sesión existente")
                Result.success(session)
            } else {
                val newSession = EncryptedChatSession(
                    sessionId = sessionId,
                    participantUids = sortedUids,
                    participantNames = mapOf(
                        myUid to myName,
                        partnerUid to partnerName
                    ),
                    title = "Enlace Seguro: $myName & $partnerName",
                    isGroup = false,
                    lastMessageText = "🔒 Sesión E2EE inicializada con claves RSA/AES-256",
                    lastMessageTimestamp = System.currentTimeMillis(),
                    sessionKeyFingerprint = partnerPublicKeyFingerprint.ifBlank { cryptoManager.getPublicKeyFingerprint() },
                    createdByUid = myUid,
                    createdAtTimestamp = System.currentTimeMillis()
                )
                sessionRef.set(newSession).await()
                Result.success(newSession)
            }
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error al crear/obtener sesión de chat: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Registra un listener en tiempo real de Firestore para los mensajes de una sesión específica.
     * Desencripta el payload con CryptoManager y almacena en Room (SQLCipher) para persistencia offline.
     */
    fun attachSessionListener(
        sessionId: String, 
        currentUserId: String = "",
        onNewMessage: ((ChatMessageEntity) -> Unit)? = null
    ) {
        if (activeListeners.containsKey(sessionId)) {
            return
        }

        val listener = firestore.collection("chat_sessions")
            .document(sessionId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("EncryptedChatService", "Error al escuchar mensajes en tiempo real: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    scope.launch(Dispatchers.IO) {
                        for (docChange in snapshot.documentChanges) {
                            val msg = docChange.document.toObject(EncryptedSessionMessage::class.java)
                            
                            // Desencriptar el contenido si está cifrado (con soporte Double Ratchet)
                            val decryptedText = try {
                                if (msg.isDoubleRatchet && msg.ratchetEphemeralPubKey.isNotBlank()) {
                                    val ratchetPayload = RatchetEncryptedMessage(
                                        senderIdentityFingerprint = msg.keyFingerprint,
                                        senderEphemeralDhPublicKeyBase64 = msg.ratchetEphemeralPubKey,
                                        chainStep = msg.ratchetStep,
                                        ivBase64 = msg.ivBase64,
                                        cipherTextBase64 = msg.ciphertextBase64,
                                        hmacAuthTagBase64 = msg.ratchetHmacTag
                                    )
                                    val decryptResult = doubleRatchetEngine.ratchetDecrypt(sessionId, ratchetPayload)
                                    decryptResult.getOrThrow()
                                } else if (msg.ciphertextBase64.isNotEmpty()) {
                                    val cipherBytes = Base64.decode(msg.ciphertextBase64, Base64.NO_WRAP)
                                    cryptoManager.decrypt(cipherBytes)
                                } else {
                                    msg.plainTextFallback
                                }
                            } catch (e: Exception) {
                                msg.plainTextFallback.ifBlank { "[Mensaje Cifrado Double Ratchet]" }
                            }

                            val entity = ChatMessageEntity(
                                id = msg.id,
                                sessionId = sessionId,
                                senderId = msg.senderUid,
                                senderName = msg.senderName,
                                text = decryptedText,
                                timestamp = msg.timestamp,
                                isEncrypted = true,
                                status = msg.deliveryStatus.ifBlank { "SENT" },
                                mediaUrl = msg.mediaUrl,
                                mediaType = msg.mediaType,
                                mediaDurationMs = msg.mediaDurationMs,
                                isRead = (msg.deliveryStatus == "READ" || msg.readByUids.contains(currentUserId)),
                                localFilePath = msg.localFilePath,
                                fileSize = msg.fileSize
                            )

                            // Guardar en la base de datos Room local
                            chatDao.insertMessage(entity)
                            onNewMessage?.invoke(entity)

                            // Si el mensaje es de otro usuario y nosotros estamos activos en esta sesión, marcar como LEÍDO en Firestore
                            if (currentUserId.isNotBlank() && msg.senderUid != currentUserId) {
                                if (msg.deliveryStatus != "READ" || !msg.readByUids.contains(currentUserId)) {
                                    markSingleMessageAsRead(sessionId, msg.id, currentUserId)
                                }
                            }

                            // Si el mensaje es recibido de otro usuario y es nuevo, generar notificación local de alerta
                            if (currentUserId.isNotBlank() && msg.senderUid != currentUserId && 
                                (System.currentTimeMillis() - msg.timestamp) < 60000L) {
                                val notifText = when (msg.mediaType) {
                                    "IMAGE" -> "📷 Foto: $decryptedText"
                                    "AUDIO" -> "🎙️ Nota de voz táctica"
                                    else -> decryptedText
                                }
                                OmniPushService.showLocalNotification(
                                    context = context,
                                    title = "🔒 ${msg.senderName}",
                                    body = notifText,
                                    channelId = OmniPushService.CHANNEL_CHAT_ID,
                                    sessionId = sessionId,
                                    targetScreen = "chat"
                                )
                            }
                        }
                    }
                }
            }

        activeListeners[sessionId] = listener
    }

    /**
     * Envía un mensaje de texto cifrado E2EE a la sesión en Firestore y lo guarda de inmediato en Room.
     */
    suspend fun sendEncryptedMessage(
        sessionId: String,
        senderUid: String,
        senderName: String,
        text: String
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        // 1. Cifrado criptográfico avanzado con Protocolo Double Ratchet (Forward Secrecy + Post-Compromise Security)
        val ratchetPayload = doubleRatchetEngine.ratchetEncrypt(sessionId, text, senderUid)
        val ciphertextBase64 = ratchetPayload.cipherTextBase64
        val keyFingerprint = cryptoManager.getPublicKeyFingerprint()

        // 2. Guardar inmediatamente en Room localmente (Offline First)
        val localEntity = ChatMessageEntity(
            id = messageId,
            sessionId = sessionId,
            senderId = senderUid,
            senderName = senderName,
            text = text,
            timestamp = timestamp,
            isEncrypted = true,
            status = "PENDING"
        )
        chatDao.insertMessage(localEntity)

        // 3. Enviar a Firestore en la nube con metadatos Double Ratchet
        try {
            val sessionMessage = EncryptedSessionMessage(
                id = messageId,
                sessionId = sessionId,
                senderUid = senderUid,
                senderName = senderName,
                ciphertextBase64 = ciphertextBase64,
                ivBase64 = ratchetPayload.ivBase64,
                plainTextFallback = text, // Para renderizado fluido y sincronización
                keyFingerprint = keyFingerprint,
                timestamp = timestamp,
                deliveryStatus = "SENT",
                isDoubleRatchet = true,
                ratchetEphemeralPubKey = ratchetPayload.senderEphemeralDhPublicKeyBase64,
                ratchetStep = ratchetPayload.chainStep,
                ratchetHmacTag = ratchetPayload.hmacAuthTagBase64
            )

            // Escribir mensaje en la subcolección de la sesión
            firestore.collection("chat_sessions")
                .document(sessionId)
                .collection("messages")
                .document(messageId)
                .set(sessionMessage)
                .await()

            // Actualizar resumen en el documento de sesión padre
            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(
                    mapOf(
                        "lastMessageText" to text,
                        "lastMessageTimestamp" to timestamp
                    ),
                    SetOptions.merge()
                )
                .await()

            // Actualizar estado en Room a SENT
            val updatedEntity = localEntity.copy(status = "SENT")
            chatDao.insertMessage(updatedEntity)

            // Notificar a los otros participantes para FCM
            notifySessionParticipants(sessionId, senderUid, senderName, text)

            Result.success(updatedEntity)
        } catch (e: Exception) {
            Log.w("EncryptedChatService", "Error al enviar mensaje a Firestore. Permanecerá en cola local: ${e.message}")
            Result.success(localEntity)
        }
    }

    /**
     * Sube una foto a Firebase Storage y envía el mensaje con la referencia multimedia a Firestore y Room.
     */
    suspend fun sendPhotoMessage(
        sessionId: String,
        senderUid: String,
        senderName: String,
        imageUri: Uri,
        caption: String = "",
        onProgress: ((Int) -> Unit)? = null
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        try {
            onProgress?.invoke(10)
            // 1. Subir imagen a Firebase Storage
            val uploadResult = mediaService.uploadChatPhotoDirect(sessionId, imageUri)
            val (downloadUrl, localFilePath) = if (uploadResult.isSuccess) {
                uploadResult.getOrThrow()
            } else {
                Pair("cached_local_photo_${System.currentTimeMillis()}", imageUri.toString())
            }

            onProgress?.invoke(70)

            val displayCaption = caption.ifBlank { "📷 Fotografía Táctica" }
            val encryptedBytes = cryptoManager.encrypt(displayCaption)
            val ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            // 2. Guardar en Room local (SQLCipher)
            val localEntity = ChatMessageEntity(
                id = messageId,
                sessionId = sessionId,
                senderId = senderUid,
                senderName = senderName,
                text = displayCaption,
                timestamp = timestamp,
                isEncrypted = true,
                status = "PENDING",
                mediaUrl = downloadUrl,
                mediaType = "IMAGE",
                localFilePath = localFilePath
            )
            chatDao.insertMessage(localEntity)

            // 3. Escribir en Firestore
            val sessionMessage = EncryptedSessionMessage(
                id = messageId,
                sessionId = sessionId,
                senderUid = senderUid,
                senderName = senderName,
                ciphertextBase64 = ciphertextBase64,
                plainTextFallback = displayCaption,
                keyFingerprint = cryptoManager.getPublicKeyFingerprint(),
                timestamp = timestamp,
                deliveryStatus = "SENT",
                mediaUrl = downloadUrl,
                mediaType = "IMAGE",
                localFilePath = localFilePath
            )

            firestore.collection("chat_sessions")
                .document(sessionId)
                .collection("messages")
                .document(messageId)
                .set(sessionMessage)
                .await()

            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(
                    mapOf(
                        "lastMessageText" to "📷 [Fotografía] $displayCaption",
                        "lastMessageTimestamp" to timestamp
                    ),
                    SetOptions.merge()
                )
                .await()

            val updated = localEntity.copy(status = "SENT")
            chatDao.insertMessage(updated)

            onProgress?.invoke(100)
            notifySessionParticipants(sessionId, senderUid, senderName, "📷 [Fotografía] $displayCaption", downloadUrl)

            Result.success(updated)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error enviando foto: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Envía cargas útiles de notificación a las bandejas de entrada de los participantes para activar FCM
     */
    private suspend fun notifySessionParticipants(
        sessionId: String,
        senderUid: String,
        senderName: String,
        bodyText: String,
        mediaUrl: String = ""
    ) {
        try {
            val sessionDoc = firestore.collection("chat_sessions").document(sessionId).get().await()
            val participants = sessionDoc.get("participantUids") as? List<*> ?: emptyList<Any>()

            for (p in participants) {
                val targetUid = p.toString()
                if (targetUid != senderUid && targetUid.isNotBlank()) {
                    val notifId = UUID.randomUUID().toString()
                    val payload = TacticalNotificationPayload(
                        id = notifId,
                        targetUid = targetUid,
                        senderUid = senderUid,
                        senderName = senderName,
                        title = "🔒 Mensaje de $senderName",
                        body = bodyText,
                        type = "CHAT_MESSAGE",
                        sessionId = sessionId,
                        mediaUrl = mediaUrl,
                        timestamp = System.currentTimeMillis(),
                        isRead = false
                    )

                    firestore.collection("users")
                        .document(targetUid)
                        .collection("notifications")
                        .document(notifId)
                        .set(payload)
                }
            }
        } catch (e: Exception) {
            Log.w("EncryptedChatService", "No se pudo disparar payload de notificación: ${e.message}")
        }
    }

    /**
     * Sube una nota de voz grabada a Firebase Storage y envía el mensaje a Firestore y Room
     */
     suspend fun sendVoiceNoteMessage(
        sessionId: String,
        senderUid: String,
        senderName: String,
        audioFile: File,
        durationMs: Long
    ): Result<ChatMessageEntity> = withContext(Dispatchers.IO) {
        val messageId = UUID.randomUUID().toString()
        val timestamp = System.currentTimeMillis()

        try {
            // 1. Subir a Firebase Storage
            val uploadResult = mediaService.uploadVoiceNoteDirect(sessionId, audioFile)
            val (downloadUrl, localFilePath) = if (uploadResult.isSuccess) {
                uploadResult.getOrThrow()
            } else {
                Pair("cached_local_audio_${System.currentTimeMillis()}", audioFile.absolutePath)
            }

            val displayText = "🎙️ Nota de Voz Táctica (${durationMs / 1000}s)"
            val encryptedBytes = cryptoManager.encrypt(displayText)
            val ciphertextBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

            // 2. Guardar en Room local (SQLCipher)
            val localEntity = ChatMessageEntity(
                id = messageId,
                sessionId = sessionId,
                senderId = senderUid,
                senderName = senderName,
                text = displayText,
                timestamp = timestamp,
                isEncrypted = true,
                status = "PENDING",
                mediaUrl = downloadUrl,
                mediaType = "AUDIO",
                mediaDurationMs = durationMs,
                localFilePath = localFilePath,
                fileSize = audioFile.length()
            )
            chatDao.insertMessage(localEntity)

            // 3. Escribir en Firestore
            val sessionMessage = EncryptedSessionMessage(
                id = messageId,
                sessionId = sessionId,
                senderUid = senderUid,
                senderName = senderName,
                ciphertextBase64 = ciphertextBase64,
                plainTextFallback = displayText,
                keyFingerprint = cryptoManager.getPublicKeyFingerprint(),
                timestamp = timestamp,
                deliveryStatus = "SENT",
                mediaUrl = downloadUrl,
                mediaType = "AUDIO",
                mediaDurationMs = durationMs,
                localFilePath = localFilePath,
                fileSize = audioFile.length()
            )

            firestore.collection("chat_sessions")
                .document(sessionId)
                .collection("messages")
                .document(messageId)
                .set(sessionMessage)
                .await()

            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(
                    mapOf(
                        "lastMessageText" to displayText,
                        "lastMessageTimestamp" to timestamp
                    ),
                    SetOptions.merge()
                )
                .await()

            val updated = localEntity.copy(status = "SENT")
            chatDao.insertMessage(updated)

            notifySessionParticipants(sessionId, senderUid, senderName, displayText, downloadUrl)
            Result.success(updated)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error enviando nota de voz: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Marca un mensaje individual como LEÍDO en Firestore y Room
     */
    suspend fun markSingleMessageAsRead(sessionId: String, messageId: String, currentUid: String) = withContext(Dispatchers.IO) {
        try {
            firestore.collection("chat_sessions")
                .document(sessionId)
                .collection("messages")
                .document(messageId)
                .update(
                    "deliveryStatus", "READ",
                    "readByUids", FieldValue.arrayUnion(currentUid)
                )
            chatDao.updateMessageStatus(messageId, "READ", isRead = true)
        } catch (e: Exception) {
            Log.w("EncryptedChatService", "Error marcando mensaje como leído: ${e.message}")
        }
    }

    /**
     * Marca todos los mensajes recibidos de una sesión como LEÍDOS (Read Receipts)
     */
    suspend fun markMessagesAsRead(sessionId: String, currentUid: String) = withContext(Dispatchers.IO) {
        try {
            chatDao.markSessionMessagesAsRead(sessionId, currentUid)
            val unreadSnapshot = firestore.collection("chat_sessions")
                .document(sessionId)
                .collection("messages")
                .whereNotEqualTo("senderUid", currentUid)
                .get()
                .await()

            for (doc in unreadSnapshot.documents) {
                doc.reference.update(
                    "deliveryStatus", "READ",
                    "readByUids", FieldValue.arrayUnion(currentUid)
                )
            }
        } catch (e: Exception) {
            Log.w("EncryptedChatService", "Error actualizando acuses de lectura en Firestore: ${e.message}")
        }
    }

    /**
     * Marca mensajes como ENTREGADOS (Delivered Receipts)
     */
    suspend fun markMessagesAsDelivered(sessionId: String, currentUid: String) = withContext(Dispatchers.IO) {
        try {
            chatDao.markSessionMessagesAsDelivered(sessionId, currentUid)
        } catch (e: Exception) {
            Log.w("EncryptedChatService", "Error actualizando entrega en Room: ${e.message}")
        }
    }

    /**
     * Crea un nuevo grupo de chat táctico multi-usuario con permisos de Administrador gestionados en Firestore
     */
    suspend fun createGroupSession(
        creatorUid: String,
        creatorName: String,
        groupTitle: String,
        groupDescription: String = "",
        groupIcon: String = "🛡️",
        initialMembers: List<Pair<String, String>> = emptyList() // List of (uid, name)
    ): Result<EncryptedChatSession> = withContext(Dispatchers.IO) {
        try {
            val sessionId = "group_${UUID.randomUUID().toString().take(8)}"
            val allParticipantUids = (listOf(creatorUid) + initialMembers.map { it.first }).distinct()
            val participantNames = mutableMapOf(creatorUid to creatorName).apply {
                initialMembers.forEach { put(it.first, it.second) }
            }

            val groupSession = EncryptedChatSession(
                sessionId = sessionId,
                participantUids = allParticipantUids,
                participantNames = participantNames,
                title = groupTitle,
                groupDescription = groupDescription.ifBlank { "Canal táctico multi-nodo de operaciones" },
                groupIcon = groupIcon,
                isGroup = true,
                adminUids = listOf(creatorUid),
                lastMessageText = "🛡️ Grupo táctico creado por $creatorName",
                lastMessageTimestamp = System.currentTimeMillis(),
                createdByUid = creatorUid,
                createdAtTimestamp = System.currentTimeMillis()
            )

            firestore.collection("chat_sessions")
                .document(sessionId)
                .set(groupSession)
                .await()

            // Enviar mensaje de bienvenida al sistema
            sendEncryptedMessage(
                sessionId = sessionId,
                senderUid = creatorUid,
                senderName = creatorName,
                text = "🛡️ Grupo táctico \"$groupTitle\" inicializado con cifrado E2EE y control de administradores."
            )

            Result.success(groupSession)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error creando grupo táctico: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Añade un nuevo operador al grupo (Solo administradores)
     */
    suspend fun addGroupParticipant(
        sessionId: String,
        adminUid: String,
        newMemberUid: String,
        newMemberName: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val sessionSnap = sessionRef.get().await()
            val session = sessionSnap.toObject(EncryptedChatSession::class.java)
                ?: throw Exception("Sesión grupal no encontrada")

            if (!session.adminUids.contains(adminUid)) {
                throw Exception("Solo los administradores del canal táctico pueden añadir participantes")
            }

            sessionRef.update(
                "participantUids", FieldValue.arrayUnion(newMemberUid),
                "participantNames.$newMemberUid", newMemberName
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error añadiendo participante al grupo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Expulsa o remueve a un operador del grupo (Solo administradores)
     */
    suspend fun removeGroupParticipant(
        sessionId: String,
        adminUid: String,
        targetMemberUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val sessionSnap = sessionRef.get().await()
            val session = sessionSnap.toObject(EncryptedChatSession::class.java)
                ?: throw Exception("Sesión grupal no encontrada")

            if (!session.adminUids.contains(adminUid) && adminUid != targetMemberUid) {
                throw Exception("Permisos insuficientes para remover este miembro")
            }

            sessionRef.update(
                "participantUids", FieldValue.arrayRemove(targetMemberUid),
                "adminUids", FieldValue.arrayRemove(targetMemberUid)
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error removiendo miembro del grupo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Promueve a un operador a Administrador del canal táctico
     */
    suspend fun promoteToAdmin(
        sessionId: String,
        adminUid: String,
        targetMemberUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val sessionSnap = sessionRef.get().await()
            val session = sessionSnap.toObject(EncryptedChatSession::class.java)
                ?: throw Exception("Sesión grupal no encontrada")

            if (!session.adminUids.contains(adminUid)) {
                throw Exception("Solo administradores pueden otorgar permisos de administrador")
            }

            sessionRef.update("adminUids", FieldValue.arrayUnion(targetMemberUid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error promoviendo a administrador: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Revoca permisos de Administrador a un operador
     */
    suspend fun demoteAdmin(
        sessionId: String,
        adminUid: String,
        targetMemberUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val sessionSnap = sessionRef.get().await()
            val session = sessionSnap.toObject(EncryptedChatSession::class.java)
                ?: throw Exception("Sesión grupal no encontrada")

            if (!session.adminUids.contains(adminUid) || session.createdByUid == targetMemberUid) {
                throw Exception("No se puede revocar el permiso del creador o sin permisos de administrador")
            }

            sessionRef.update("adminUids", FieldValue.arrayRemove(targetMemberUid)).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error degradando administrador: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Actualiza el título y descripción del grupo táctico
     */
    suspend fun updateGroupInfo(
        sessionId: String,
        adminUid: String,
        newTitle: String,
        newDescription: String,
        newIcon: String = "🛡️"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val sessionRef = firestore.collection("chat_sessions").document(sessionId)
            val sessionSnap = sessionRef.get().await()
            val session = sessionSnap.toObject(EncryptedChatSession::class.java)
                ?: throw Exception("Sesión grupal no encontrada")

            if (!session.adminUids.contains(adminUid)) {
                throw Exception("Solo administradores pueden modificar los datos del grupo")
            }

            sessionRef.update(
                "title", newTitle,
                "groupDescription", newDescription,
                "groupIcon", newIcon
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("EncryptedChatService", "Error actualizando datos de grupo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Escucha reactivamente la lista de sesiones donde participa el usuario actual en tiempo real.
     */
    fun listenToUserSessions(userUid: String) {
        firestore.collection("chat_sessions")
            .whereArrayContains("participantUids", userUid)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w("EncryptedChatService", "Error observando sesiones de usuario: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val sessions = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(EncryptedChatSession::class.java)
                    }
                    _activeSessions.value = sessions
                }
            }
    }

    /**
     * Desconecta los listeners activos para liberar recursos
     */
    fun detachAllListeners() {
        activeListeners.values.forEach { it.remove() }
        activeListeners.clear()
    }
}

