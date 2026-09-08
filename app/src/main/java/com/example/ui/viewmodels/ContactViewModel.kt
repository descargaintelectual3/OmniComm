package com.example.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.chat.FirestoreEncryptedChatService
import com.example.domain.contacts.ContactRepository
import com.example.domain.local.entities.ContactEntity
import com.example.domain.models.EncryptedChatSession
import com.example.domain.security.FirebaseAuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ContactFilter(val label: String) {
    ALL("Todos"),
    ONLINE("En Línea"),
    PRIORITY("Prioritarios")
}

class ContactViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val contactRepo = ContactRepository(context)
    private val authManager = FirebaseAuthManager(context)
    private val encryptedChatService = FirestoreEncryptedChatService(context)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _activeFilter = MutableStateFlow(ContactFilter.ALL)
    val activeFilter: StateFlow<ContactFilter> = _activeFilter.asStateFlow()

    private val _isAddingContact = MutableStateFlow(false)
    val isAddingContact: StateFlow<Boolean> = _isAddingContact.asStateFlow()

    private val _addContactStatus = MutableStateFlow<String?>(null)
    val addContactStatus: StateFlow<String?> = _addContactStatus.asStateFlow()

    private val _selectedSession = MutableStateFlow<EncryptedChatSession?>(null)
    val selectedSession: StateFlow<EncryptedChatSession?> = _selectedSession.asStateFlow()

    // Flujo unificado con filtrado dinámico reactivo
    val filteredContacts: StateFlow<List<ContactEntity>> = combine(
        contactRepo.allContacts,
        _searchQuery,
        _activeFilter
    ) { contacts, query, filter ->
        var result = when (filter) {
            ContactFilter.ALL -> contacts
            ContactFilter.ONLINE -> contacts.filter { it.isOnline }
            ContactFilter.PRIORITY -> contacts.filter { it.isPriority }
        }

        if (query.isNotBlank()) {
            result = result.filter {
                it.alias.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.deviceId.contains(query, ignoreCase = true)
            }
        }
        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val discoveryEngine = com.example.domain.discovery.AutonomousMeshDiscoveryEngine.getInstance(context)

    val onlineCount: StateFlow<Int> = contactRepo.onlineContacts
        .combine(MutableStateFlow(0)) { list, _ -> list.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val currentUserProfile = authManager.currentUserProfile

    init {
        viewModelScope.launch {
            // Asegurar que existan contactos con claves públicas E2EE precargadas en Room
            contactRepo.seedInitialTacticalE2eeContacts()
            // Iniciar sincronización autónoma global de inmediato
            discoveryEngine.startAutoDiscovery()
            val effectiveInitialUid = authManager.currentFirebaseUser?.uid ?: discoveryEngine.localNodeId
            contactRepo.startRealtimeSynchronization(effectiveInitialUid)
            encryptedChatService.listenToUserSessions(effectiveInitialUid)

            authManager.currentUserProfile.collect { user ->
                val activeUid = if (user != null && user.uid.isNotBlank()) user.uid else discoveryEngine.localNodeId
                contactRepo.startRealtimeSynchronization(activeUid, user?.displayName ?: "", user?.email ?: "")
                encryptedChatService.listenToUserSessions(activeUid)
            }
        }
    }

    fun saveContactWithPublicKey(
        alias: String,
        deviceId: String,
        publicKey: String,
        algorithm: String = "RSA-2048",
        fingerprint: String = ""
    ) {
        viewModelScope.launch {
            val fp = if (fingerprint.isNotBlank()) fingerprint else {
                "SHA256:" + (publicKey.hashCode().toString(16).padStart(8, '0').uppercase().chunked(2).joinToString(":"))
            }
            contactRepo.saveContactWithEncryptedPublicKey(
                deviceId = deviceId.ifBlank { "NODE_" + System.currentTimeMillis().toString().takeLast(6) },
                alias = alias,
                rawPublicKey = publicKey,
                encryptedKeyBundle = "ENC_PK_BUNDLE_${deviceId}_$algorithm",
                fingerprint = fp,
                algorithm = algorithm
            )
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: ContactFilter) {
        _activeFilter.value = filter
    }

    /**
     * Agrega un nuevo contacto o nodo a la libreta sincronizada
     */
    fun addContact(query: String, alias: String? = null, onComplete: (Boolean) -> Unit) {
        val myUid = authManager.currentFirebaseUser?.uid ?: ""
        _isAddingContact.value = true
        _addContactStatus.value = "Buscando nodo en la red táctica..."

        viewModelScope.launch {
            val myName = authManager.currentUserProfile.value?.displayName ?: "Operador"
            val result = contactRepo.addContact(
                myUid = myUid,
                myDisplayName = myName,
                query = query,
                customAlias = alias
            )
            _isAddingContact.value = false
            result.fold(
                onSuccess = { contact ->
                    _addContactStatus.value = "✅ Contacto agregado: ${contact.alias}"
                    onComplete(true)
                },
                onFailure = { error ->
                    _addContactStatus.value = "❌ ${error.localizedMessage ?: "Error al agregar"}"
                    onComplete(false)
                }
            )
        }
    }

    /**
     * Alterna la prioridad (marcador favorito / prioritario)
     */
    fun togglePriority(contact: ContactEntity) {
        val myUid = authManager.currentFirebaseUser?.uid ?: ""
        viewModelScope.launch {
            contactRepo.togglePriority(myUid, contact.deviceId, contact.isPriority)
        }
    }

    /**
     * Elimina un contacto
     */
    fun deleteContact(contact: ContactEntity) {
        val myUid = authManager.currentFirebaseUser?.uid ?: ""
        viewModelScope.launch {
            contactRepo.deleteContact(myUid, contact.deviceId)
        }
    }

    /**
     * Inicia o recupera una sesión de chat cifrado E2EE con el contacto seleccionado
     */
    fun initiateEncryptedChatSession(
        contact: ContactEntity,
        onSessionReady: (EncryptedChatSession) -> Unit
    ) {
        val currentUser = authManager.currentUserProfile.value ?: return
        viewModelScope.launch {
            val result = encryptedChatService.createOrGetDirectSession(
                myUid = currentUser.uid,
                myName = currentUser.displayName,
                partnerUid = contact.deviceId,
                partnerName = contact.alias,
                partnerPublicKeyFingerprint = contact.publicKeyFingerprint
            )
            result.fold(
                onSuccess = { session ->
                    _selectedSession.value = session
                    onSessionReady(session)
                },
                onFailure = { error ->
                    _addContactStatus.value = "Error al iniciar sesión segura: ${error.message}"
                }
            )
        }
    }

    fun clearStatus() {
        _addContactStatus.value = null
    }

    override fun onCleared() {
        super.onCleared()
        contactRepo.stopRealtimeSynchronization()
    }
}
