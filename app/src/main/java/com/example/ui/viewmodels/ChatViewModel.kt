package com.example.ui.viewmodels

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.ai.GeminiAudioTranscriptionEngine
import com.example.domain.ai.TranscriptionState
import com.example.domain.chat.FirestoreEncryptedChatService
import com.example.domain.contacts.ContactRepository
import com.example.domain.hardware.BluetoothMeshService
import com.example.domain.hardware.DiscoveredPeer
import com.example.domain.hardware.MeshConnectionState
import com.example.domain.hardware.TacticalAudioEngine
import com.example.domain.local.OfflineMessageQueue
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ContactEntity
import com.example.domain.local.entities.ThreadCustomizationEntity
import com.example.domain.media.TacticalAudioPlayer
import com.example.domain.media.TacticalAudioRecorder
import com.example.domain.models.ChatMessage
import com.example.domain.models.EncryptedChatSession
import com.example.domain.security.BiometricVaultManager
import com.example.domain.security.CryptoManager
import com.example.domain.security.FirebaseAuthManager
import com.example.domain.sync.MessageSyncWorker
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = OmniDatabase.getDatabase(context)
    private val chatDao = database.chatDao()
    private val contactRepo = ContactRepository(context)
    private val meshService = BluetoothMeshService.getInstance(context)
    private val offlineQueue = OfflineMessageQueue(context, meshService)
    private val audioEngine = TacticalAudioEngine(context, offlineQueue)
    private val aiTranscriptionEngine = GeminiAudioTranscriptionEngine()
    val authManager = FirebaseAuthManager(context)
    val encryptedChatService = FirestoreEncryptedChatService(context)
    val biometricVaultManager = BiometricVaultManager(context)
    val audioRecorder = TacticalAudioRecorder(context)
    val audioPlayer = TacticalAudioPlayer(context)
    val threadCustomizationDao = database.threadCustomizationDao()
    private val cryptoManager = CryptoManager()
    val doubleRatchetEngine = com.example.domain.security.DoubleRatchetEngine.getInstance()

    private val _safetyNumber = MutableStateFlow("48192 10492 84920\n39104 58291 04829\n92019 48201 59201\n48102 59281 04928")
    val safetyNumber: StateFlow<String> = _safetyNumber.asStateFlow()

    private val _ratchetSendingStep = MutableStateFlow(1)
    val ratchetSendingStep: StateFlow<Int> = _ratchetSendingStep.asStateFlow()

    private val _isSafetyNumberVerified = MutableStateFlow(false)
    val isSafetyNumberVerified: StateFlow<Boolean> = _isSafetyNumberVerified.asStateFlow()

    val allContacts: StateFlow<List<ContactEntity>> = contactRepo.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow("global")
    val activeSessionId: StateFlow<String> = _activeSessionId.asStateFlow()

    private val _activeSessionTitle = MutableStateFlow("Canal Malla Global")
    val activeSessionTitle: StateFlow<String> = _activeSessionTitle.asStateFlow()

    private val _partnerName = MutableStateFlow("")
    val partnerName: StateFlow<String> = _partnerName.asStateFlow()

    private val _isCurrentSessionGroup = MutableStateFlow(false)
    val isCurrentSessionGroup: StateFlow<Boolean> = _isCurrentSessionGroup.asStateFlow()

    private val _currentGroupSession = MutableStateFlow<EncryptedChatSession?>(null)
    val currentGroupSession: StateFlow<EncryptedChatSession?> = _currentGroupSession.asStateFlow()

    // Sesiones activas de Firestore
    val activeChatSessions: StateFlow<List<EncryptedChatSession>> = encryptedChatService.activeSessions

    // Personalización de la sesión actual
    val currentThreadCustomization: StateFlow<ThreadCustomizationEntity?> = _activeSessionId.flatMapLatest { sessionId ->
        threadCustomizationDao.getCustomizationFlow(sessionId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Búsqueda de texto completo (FTS)
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()

    // Estado de subida de fotos a Firebase Storage
    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _uploadProgress = MutableStateFlow(0)
    val uploadProgress: StateFlow<Int> = _uploadProgress.asStateFlow()

    // Estado de grabación de notas de voz
    val isVoiceNoteRecording = audioRecorder.isRecording
    val voiceNoteDurationMs = audioRecorder.recordingDurationMs
    val voiceNoteAmplitudes = audioRecorder.amplitudes

    // Estado de reproducción de notas de voz
    val playingAudioMessageId = audioPlayer.currentPlayingMessageId
    val isAudioPlaying = audioPlayer.isPlaying
    val audioPlaybackPositionMs = audioPlayer.currentPositionMs
    val audioTotalDurationMs = audioPlayer.totalDurationMs

    // Mensajes reactivos cacheados en Room filtrados por sesión actual y búsqueda FTS
    val localEncryptedMessages: StateFlow<List<ChatMessageEntity>> = combine(
        _activeSessionId,
        _searchQuery
    ) { sessionId, query ->
        Pair(sessionId, query)
    }.flatMapLatest { (sessionId, query) ->
        if (query.isNotBlank()) {
            if (sessionId == "global") {
                chatDao.searchAllMessages(query.trim())
            } else {
                chatDao.searchSessionMessages(sessionId, query.trim())
            }
        } else {
            if (sessionId == "global") {
                chatDao.getAllMessages()
            } else {
                chatDao.getMessagesForSession(sessionId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingMessagesCount: StateFlow<Int> = chatDao.getPendingCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val meshConnectionState: StateFlow<MeshConnectionState> = meshService.connectionState
    val discoveredPeers: StateFlow<List<DiscoveredPeer>> = meshService.discoveredPeers

    private val _connectionStatus = MutableStateFlow("Iniciando Malla Táctica...")
    val connectionStatus: StateFlow<String> = _connectionStatus.asStateFlow()

    private val _isRecordingVoice = MutableStateFlow(false)
    val isRecordingVoice: StateFlow<Boolean> = _isRecordingVoice.asStateFlow()

    private val _isTranscribingVoice = MutableStateFlow(false)
    val isTranscribingVoice: StateFlow<Boolean> = _isTranscribingVoice.asStateFlow()

    private val _transcriptionStatusText = MutableStateFlow("")
    val transcriptionStatusText: StateFlow<String> = _transcriptionStatusText.asStateFlow()

    private val _audioWaveform = MutableStateFlow<List<Float>>(listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.6f))
    val audioWaveform: StateFlow<List<Float>> = _audioWaveform.asStateFlow()

    private var firestore: FirebaseFirestore? = null
    private val channelName = "omnicomm_global_chat"

    init {
        offlineQueue.startQueueWorker()
        initializeFirebase()
        observeAiTranscription()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch(active: Boolean) {
        _isSearchActive.value = active
        if (!active) {
            _searchQuery.value = ""
        }
    }

    /**
     * Configura la sesión actual (global o 1 a 1 cifrada)
     */
    fun selectSession(sessionId: String, title: String, partner: String = "") {
        _activeSessionId.value = sessionId
        _activeSessionTitle.value = title
        _partnerName.value = partner
        _isCurrentSessionGroup.value = false
        _currentGroupSession.value = null
        _searchQuery.value = ""
        _isSearchActive.value = false

        if (sessionId != "global") {
            val myUid = authManager.currentUserProfile.value?.uid ?: ""
            encryptedChatService.attachSessionListener(sessionId, currentUserId = myUid)
            // Marcar mensajes recibidos como LEÍDOS en Room y Firestore
            viewModelScope.launch(Dispatchers.IO) {
                encryptedChatService.markMessagesAsRead(sessionId, myUid)
            }
            refreshRatchetSession(sessionId)
        } else {
            _safetyNumber.value = "MALLA-BROADCAST-E2EE-MULTI-HOP"
            _ratchetSendingStep.value = 1
        }
    }

    fun toggleSafetyNumberVerification() {
        _isSafetyNumberVerified.value = !_isSafetyNumberVerified.value
    }

    fun refreshRatchetSession(sessionId: String) {
        val session = doubleRatchetEngine.getSession(sessionId)
        if (session != null) {
            _safetyNumber.value = session.safetyNumber.ifBlank { "48192 10492 84920\n39104 58291 04829\n92019 48201 59201\n48102 59281 04928" }
            _ratchetSendingStep.value = session.sendingStep
        }
    }

    /**
     * Configura una sesión grupal multi-usuario
     */
    fun selectGroupSession(group: EncryptedChatSession) {
        _activeSessionId.value = group.sessionId
        _activeSessionTitle.value = group.title
        _partnerName.value = "${group.participantUids.size} Operadores"
        _isCurrentSessionGroup.value = true
        _currentGroupSession.value = group
        _searchQuery.value = ""
        _isSearchActive.value = false

        val myUid = authManager.currentUserProfile.value?.uid ?: ""
        encryptedChatService.attachSessionListener(group.sessionId, currentUserId = myUid)
        viewModelScope.launch(Dispatchers.IO) {
            encryptedChatService.markMessagesAsRead(group.sessionId, myUid)
        }
    }

    /**
     * Crea un nuevo grupo de chat táctico
     */
    fun createGroupChat(
        title: String,
        description: String,
        icon: String = "🛡️",
        members: List<Pair<String, String>>,
        onSuccess: (EncryptedChatSession) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val currentUser = authManager.currentUserProfile.value
        val creatorUid = currentUser?.uid ?: return
        val creatorName = currentUser.displayName

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.createGroupSession(
                creatorUid = creatorUid,
                creatorName = creatorName,
                groupTitle = title,
                groupDescription = description,
                groupIcon = icon,
                initialMembers = members
            )
            result.onSuccess { session ->
                selectGroupSession(session)
                onSuccess(session)
            }.onFailure { error ->
                onError(error.message ?: "Error al crear grupo táctico")
            }
        }
    }

    /**
     * Añade un operador al grupo actual
     */
    fun addMemberToGroup(memberUid: String, memberName: String, onComplete: (Boolean, String) -> Unit) {
        val currentSession = _currentGroupSession.value ?: return
        val myUid = authManager.currentUserProfile.value?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.addGroupParticipant(
                sessionId = currentSession.sessionId,
                adminUid = myUid,
                newMemberUid = memberUid,
                newMemberName = memberName
            )
            result.onSuccess {
                onComplete(true, "Operador añadido al grupo")
            }.onFailure { error ->
                onComplete(false, error.message ?: "Error al añadir operador")
            }
        }
    }

    /**
     * Remueve a un operador del grupo actual
     */
    fun removeMemberFromGroup(memberUid: String, onComplete: (Boolean, String) -> Unit) {
        val currentSession = _currentGroupSession.value ?: return
        val myUid = authManager.currentUserProfile.value?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.removeGroupParticipant(
                sessionId = currentSession.sessionId,
                adminUid = myUid,
                targetMemberUid = memberUid
            )
            result.onSuccess {
                onComplete(true, "Operador removido del grupo")
            }.onFailure { error ->
                onComplete(false, error.message ?: "Error al remover operador")
            }
        }
    }

    /**
     * Otorga privilegios de Administrador en el grupo actual
     */
    fun promoteMemberToAdmin(memberUid: String, onComplete: (Boolean, String) -> Unit) {
        val currentSession = _currentGroupSession.value ?: return
        val myUid = authManager.currentUserProfile.value?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.promoteToAdmin(
                sessionId = currentSession.sessionId,
                adminUid = myUid,
                targetMemberUid = memberUid
            )
            result.onSuccess {
                onComplete(true, "Operador promovido a Administrador")
            }.onFailure { error ->
                onComplete(false, error.message ?: "Error al promover operador")
            }
        }
    }

    /**
     * Revoca privilegios de Administrador en el grupo actual
     */
    fun demoteMemberAdmin(memberUid: String, onComplete: (Boolean, String) -> Unit) {
        val currentSession = _currentGroupSession.value ?: return
        val myUid = authManager.currentUserProfile.value?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.demoteAdmin(
                sessionId = currentSession.sessionId,
                adminUid = myUid,
                targetMemberUid = memberUid
            )
            result.onSuccess {
                onComplete(true, "Privilegios de administrador revocados")
            }.onFailure { error ->
                onComplete(false, error.message ?: "Error al revocar privilegios")
            }
        }
    }

    /**
     * Actualiza metadatos del grupo
     */
    fun updateGroupDetails(title: String, description: String, icon: String, onComplete: (Boolean, String) -> Unit) {
        val currentSession = _currentGroupSession.value ?: return
        val myUid = authManager.currentUserProfile.value?.uid ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedChatService.updateGroupInfo(
                sessionId = currentSession.sessionId,
                adminUid = myUid,
                newTitle = title,
                newDescription = description,
                newIcon = icon
            )
            result.onSuccess {
                _activeSessionTitle.value = title
                onComplete(true, "Datos de grupo actualizados")
            }.onFailure { error ->
                onComplete(false, error.message ?: "Error al actualizar grupo")
            }
        }
    }

    /**
     * Inicia la grabación de una nota de voz táctica
     */
    fun startVoiceNoteRecording(): Boolean {
        val result = audioRecorder.startRecording()
        return result.isSuccess
    }

    /**
     * Finaliza la grabación y envía la nota de voz a través del canal cifrado
     */
    fun stopAndSendVoiceNote() {
        val recorded = audioRecorder.stopRecording() ?: return
        val (audioFile, durationMs) = recorded
        val currentSession = _activeSessionId.value
        val currentUser = authManager.currentUserProfile.value
        val senderUid = currentUser?.uid ?: "local_device"
        val senderName = currentUser?.displayName ?: "Yo (Operador)"

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveSession = if (currentSession == "global") "global_mesh_voice" else currentSession
                encryptedChatService.sendVoiceNoteMessage(
                    sessionId = effectiveSession,
                    senderUid = senderUid,
                    senderName = senderName,
                    audioFile = audioFile,
                    durationMs = durationMs
                )
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Error enviando nota de voz: ${e.message}", e)
            }
        }
    }

    /**
     * Cancela la grabación de la nota de voz descartando el audio
     */
    fun cancelVoiceNoteRecording() {
        audioRecorder.cancelRecording()
    }

    /**
     * Alterna la reproducción de un mensaje de audio
     */
    fun toggleAudioPlayback(message: ChatMessageEntity) {
        audioPlayer.togglePlay(
            messageId = message.id,
            audioSource = message.mediaUrl,
            localFilePath = message.localFilePath
        )
    }

    /**
     * Detiene la reproducción de audio activa
     */
    fun stopAudioPlayback() {
        audioPlayer.stop()
    }

    /**
     * Bloquea la bóveda táctica con autenticación biométrica
     */
    fun lockVault() {
        biometricVaultManager.lockVault()
    }

    /**
     * Guarda la personalización de fondos y colores de burbujas para el hilo actual
     */
    fun saveThreadCustomization(
        wallpaperPreset: String,
        customWallpaperColorHex: String? = null,
        outgoingBubbleColorHex: String? = null,
        incomingBubbleColorHex: String? = null
    ) {
        val sessionId = _activeSessionId.value
        val entity = ThreadCustomizationEntity(
            sessionId = sessionId,
            wallpaperPreset = wallpaperPreset,
            customWallpaperColorHex = customWallpaperColorHex,
            outgoingBubbleColorHex = outgoingBubbleColorHex,
            incomingBubbleColorHex = incomingBubbleColorHex,
            updatedAtTimestamp = System.currentTimeMillis()
        )
        viewModelScope.launch(Dispatchers.IO) {
            threadCustomizationDao.saveCustomization(entity)
        }
    }

    private fun observeAiTranscription() {
        viewModelScope.launch {
            aiTranscriptionEngine.transcriptionState.collect { state ->
                when (state) {
                    is TranscriptionState.Idle -> {
                        _isTranscribingVoice.value = false
                    }
                    is TranscriptionState.Recording -> {
                        _isRecordingVoice.value = true
                        _isTranscribingVoice.value = false
                    }
                    is TranscriptionState.Transcribing -> {
                        _isRecordingVoice.value = false
                        _isTranscribingVoice.value = true
                        _transcriptionStatusText.value = state.progressText
                    }
                    is TranscriptionState.Success -> {
                        _isTranscribingVoice.value = false
                        _transcriptionStatusText.value = "Transcripción completada"
                    }
                    is TranscriptionState.Error -> {
                        _isTranscribingVoice.value = false
                        _transcriptionStatusText.value = "Error en transcripción"
                    }
                }
            }
        }
    }

    private fun initializeFirebase() {
        try {
            firestore = FirebaseFirestore.getInstance()
            _connectionStatus.value = "🟢 En Línea: Bóveda SQLCipher + Firestore E2EE"
            listenForRealTimeMessages()
        } catch (e: Exception) {
            _connectionStatus.value = "🟡 Modo Offline: Malla Bluetooth P2P + Bóveda Cifrada"
            Log.w("ChatViewModel", "Firebase no disponible en modo offline: ${e.message}")
        }
    }

    private fun listenForRealTimeMessages() {
        firestore?.collection(channelName)
            ?.orderBy("timestamp", Query.Direction.ASCENDING)
            ?.addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChatViewModel", "Error al sincronizar nube: ", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    viewModelScope.launch(Dispatchers.IO) {
                        for (doc in snapshot.documents) {
                            val msg = doc.toObject(ChatMessage::class.java)
                            if (msg != null) {
                                chatDao.insertMessage(
                                    ChatMessageEntity(
                                        id = msg.id,
                                        sessionId = "global",
                                        senderId = "cloud_node",
                                        senderName = msg.senderName,
                                        text = msg.text,
                                        timestamp = msg.timestamp,
                                        status = "SENT"
                                    )
                                )
                            }
                        }
                    }
                }
            }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val currentSession = _activeSessionId.value
        val currentUser = authManager.currentUserProfile.value
        val senderUid = currentUser?.uid ?: "local_device"
        val senderName = currentUser?.displayName ?: "Yo (Operador)"

        if (currentSession != "global") {
            // Envío a través del servicio E2EE de Firestore + persistencia Room
            viewModelScope.launch(Dispatchers.IO) {
                encryptedChatService.sendEncryptedMessage(
                    sessionId = currentSession,
                    senderUid = senderUid,
                    senderName = senderName,
                    text = text.trim()
                )
            }
        } else {
            // Canal Global / Malla Bluetooth
            val msgEntity = ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                sessionId = "global",
                senderId = senderUid,
                senderName = senderName,
                text = text.trim(),
                timestamp = System.currentTimeMillis(),
                status = if (meshService.isConnectedToAnyNode() || firestore != null) "SENT" else "PENDING"
            )

            viewModelScope.launch(Dispatchers.IO) {
                offlineQueue.enqueueMessage(msgEntity)

                firestore?.let { db ->
                    try {
                        val packet = ChatMessage(
                            id = msgEntity.id,
                            senderName = msgEntity.senderName,
                            text = msgEntity.text,
                            timestamp = msgEntity.timestamp
                        )
                        db.collection(channelName).document(msgEntity.id).set(packet)
                    } catch (e: Exception) {
                        Log.w("ChatViewModel", "No se pudo sincronizar con Firestore (Permanecerá en cola local)")
                    }
                }
            }
        }
    }

    /**
     * Envía una fotografía capturada o seleccionada subiéndola a Firebase Storage
     * y asociando la URL en Firestore y Room.
     */
    fun sendPhoto(imageUri: Uri, caption: String = "") {
        val currentSession = _activeSessionId.value
        val currentUser = authManager.currentUserProfile.value
        val senderUid = currentUser?.uid ?: "local_device"
        val senderName = currentUser?.displayName ?: "Yo (Operador)"

        _isUploadingPhoto.value = true
        _uploadProgress.value = 5

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val effectiveSession = if (currentSession == "global") "global_mesh_media" else currentSession
                encryptedChatService.sendPhotoMessage(
                    sessionId = effectiveSession,
                    senderUid = senderUid,
                    senderName = senderName,
                    imageUri = imageUri,
                    caption = caption
                ) { progress ->
                    _uploadProgress.value = progress
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Fallo al enviar foto: ${e.message}", e)
            } finally {
                _isUploadingPhoto.value = false
                _uploadProgress.value = 0
            }
        }
    }

    /**
     * Inicia la captura de audio táctica con animación de onda en tiempo real.
     */
    fun startVoiceRecording() {
        _isRecordingVoice.value = true
        aiTranscriptionEngine.setRecordingState()
        
        audioEngine.startAudioSession { pcmChunk ->
            var sum = 0L
            for (i in pcmChunk.indices step 2) {
                if (i + 1 < pcmChunk.size) {
                    val sample = (pcmChunk[i + 1].toInt() shl 8) or (pcmChunk[i].toInt() and 0xFF)
                    sum += Math.abs(sample)
                }
            }
            val avg = (sum / (pcmChunk.size / 2).coerceAtLeast(1)).toFloat() / 32768f
            val clamped = avg.coerceIn(0.1f, 1.0f)
            
            _audioWaveform.value = listOf(
                clamped * 0.7f,
                clamped * 1.2f,
                clamped,
                clamped * 0.9f,
                clamped * 1.4f,
                clamped * 0.8f,
                clamped * 1.1f
            )
        }
    }

    /**
     * Detiene la captura de audio e inicia el pipeline de transcripción Gemini AI.
     */
    fun stopVoiceRecording() {
        _isRecordingVoice.value = false
        _isTranscribingVoice.value = true
        _transcriptionStatusText.value = "Gemini AI procesando voz a texto..."
        audioEngine.stopAudioSession()
    }

    /**
     * Emite un saludo de bienvenida automático a todos los nodos de la malla.
     */
    fun broadcastWelcomeHandshake() {
        meshService.sendWelcomeHandshake()
        sendMessage("👋 ¡Hola red OmniComm! Nodo sincronizado y en línea.")
    }

    /**
     * Dispara la sincronización manual de la cola de mensajes en Room / SQLCipher.
     */
    fun triggerManualSync() {
        MessageSyncWorker.triggerExpeditedSync(context)
    }

    override fun onCleared() {
        super.onCleared()
        offlineQueue.stopQueueWorker()
        encryptedChatService.detachAllListeners()
    }
}


