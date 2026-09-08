package com.example.ui.viewmodels

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.CryptographicKeyEntity
import com.example.domain.repository.KeyManagementRepository
import com.example.domain.security.CryptoManager
import com.example.domain.security.CryptographicKeyGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class KeyFilterOption(val displayName: String) {
    ALL("Todas"),
    LOCAL_PAIRS("Mis Claves (Pares)"),
    PEER_PUBLIC_KEYS("Nodos Pares"),
    POST_QUANTUM("Post-Cuánticas (PQC)")
}

data class KeyManagementUiState(
    val keys: List<CryptographicKeyEntity> = emptyList(),
    val primaryActiveKey: CryptographicKeyEntity? = null,
    val searchQuery: String = "",
    val activeFilter: KeyFilterOption = KeyFilterOption.ALL,
    val isGenerating: Boolean = false,
    val statusNotification: String? = null
)

class KeyManagementViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = OmniDatabase.getDatabase(context)
    private val repository = KeyManagementRepository(database.cryptographicKeyDao())
    private val cryptoManager = CryptoManager()

    private val _searchQuery = MutableStateFlow("")
    private val _activeFilter = MutableStateFlow(KeyFilterOption.ALL)
    private val _isGenerating = MutableStateFlow(false)
    private val _statusNotification = MutableStateFlow<String?>(null)

    val primaryKey: StateFlow<CryptographicKeyEntity?> = repository.primaryActiveKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private data class FilterParams(
        val query: String,
        val filter: KeyFilterOption,
        val isGen: Boolean,
        val notification: String?
    )

    private val _filterParams = combine(
        _searchQuery,
        _activeFilter,
        _isGenerating,
        _statusNotification
    ) { query, filter, isGen, notif ->
        FilterParams(query, filter, isGen, notif)
    }

    val uiState: StateFlow<KeyManagementUiState> = combine(
        repository.allKeys,
        repository.primaryActiveKey,
        _filterParams
    ) { allKeys, primaryKey, params ->
        val filtered = allKeys.filter { key ->
            val matchesFilter = when (params.filter) {
                KeyFilterOption.ALL -> true
                KeyFilterOption.LOCAL_PAIRS -> key.ownerNodeId == "LOCAL_NODE"
                KeyFilterOption.PEER_PUBLIC_KEYS -> key.ownerNodeId != "LOCAL_NODE"
                KeyFilterOption.POST_QUANTUM -> key.algorithm.contains("KYBER", ignoreCase = true) || key.purpose == "POST_QUANTUM"
            }

            val matchesQuery = if (params.query.isBlank()) true else {
                key.alias.contains(params.query, ignoreCase = true) ||
                key.fingerprint.contains(params.query, ignoreCase = true) ||
                key.algorithm.contains(params.query, ignoreCase = true) ||
                key.ownerDisplayName.contains(params.query, ignoreCase = true) ||
                key.purpose.contains(params.query, ignoreCase = true)
            }

            matchesFilter && matchesQuery
        }

        KeyManagementUiState(
            keys = filtered,
            primaryActiveKey = primaryKey,
            searchQuery = params.query,
            activeFilter = params.filter,
            isGenerating = params.isGen,
            statusNotification = params.notification
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), KeyManagementUiState())

    init {
        seedInitialKeyIfEmpty()
    }

    private fun seedInitialKeyIfEmpty() {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = repository.getKeyById("MASTER-E2EE-IDENTITY")
            if (existing == null) {
                // Registrar la llave maestra de identidad de AndroidKeyStore en Room
                val pubBase64 = cryptoManager.getPublicEncryptionKeyBase64()
                val fingerprint = cryptoManager.getPublicKeyFingerprint()
                val masterKey = CryptographicKeyEntity(
                    keyId = "MASTER-E2EE-IDENTITY",
                    alias = "Identidad Maestra Táctica (${Build.MODEL})",
                    algorithm = "RSA-2048",
                    purpose = "E2EE_MESSAGING",
                    publicKeyBase64 = pubBase64,
                    fingerprint = fingerprint,
                    keySizeBits = 2048,
                    isHardwareBacked = true,
                    isPrimary = true,
                    ownerNodeId = "LOCAL_NODE",
                    ownerDisplayName = "Terminal Local (${Build.MODEL})",
                    notes = "Clave raíz primaria vinculada al Keystore de hardware para cifrado E2EE de chats y datos."
                )
                repository.insertKey(masterKey)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: KeyFilterOption) {
        _activeFilter.value = filter
    }

    fun clearNotification() {
        _statusNotification.value = null
    }

    fun generateNewKeyPair(
        alias: String,
        algorithm: String,
        purpose: String,
        useHardware: Boolean = true,
        setAsPrimaryNow: Boolean = false
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _isGenerating.value = true
            try {
                val generated = when {
                    algorithm.startsWith("RSA-4096") -> {
                        CryptographicKeyGenerator.generateRsaKeyPair(
                            alias = alias,
                            keySizeBits = 4096,
                            useAndroidKeyStore = useHardware,
                            purpose = purpose
                        )
                    }
                    algorithm.startsWith("EC") -> {
                        CryptographicKeyGenerator.generateEcKeyPair(
                            alias = alias,
                            useAndroidKeyStore = useHardware,
                            purpose = purpose
                        )
                    }
                    algorithm.contains("KYBER") || algorithm.contains("PQC") -> {
                        CryptographicKeyGenerator.generatePostQuantumKyberKey(
                            alias = alias,
                            purpose = purpose
                        )
                    }
                    else -> { // Default RSA-2048
                        CryptographicKeyGenerator.generateRsaKeyPair(
                            alias = alias,
                            keySizeBits = 2048,
                            useAndroidKeyStore = useHardware,
                            purpose = purpose
                        )
                    }
                }

                val entity = CryptographicKeyEntity(
                    keyId = generated.keyId,
                    alias = generated.alias,
                    algorithm = generated.algorithm,
                    purpose = generated.purpose,
                    publicKeyBase64 = generated.publicKeyBase64,
                    fingerprint = generated.fingerprint,
                    keySizeBits = generated.keySizeBits,
                    isHardwareBacked = generated.isHardwareBacked,
                    isPrimary = setAsPrimaryNow,
                    ownerNodeId = "LOCAL_NODE",
                    ownerDisplayName = "Terminal Local",
                    notes = generated.notes
                )

                repository.insertKey(entity)
                if (setAsPrimaryNow) {
                    repository.setAsPrimary(entity.keyId)
                }

                _statusNotification.value = "Par de claves ${entity.algorithm} '${entity.alias}' generado y guardado en Room."
            } catch (e: Exception) {
                _statusNotification.value = "Error generando par de claves: ${e.message}"
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun setAsPrimary(keyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setAsPrimary(keyId)
            _statusNotification.value = "Clave $keyId establecida como primaria para E2EE."
        }
    }

    fun revokeKey(keyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.revokeKey(keyId)
            _statusNotification.value = "Clave $keyId revocada de operaciones criptográficas."
        }
    }

    fun deleteKey(keyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteKeyById(keyId)
            _statusNotification.value = "Clave $keyId eliminada de la base de datos Room."
        }
    }

    fun importPeerPublicKey(
        alias: String,
        algorithm: String,
        publicKeyBase64: String,
        ownerNodeId: String,
        ownerDisplayName: String,
        notes: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val keyId = "PEER-KEY-${UUID.randomUUID().toString().take(6).uppercase()}"
            val fingerprint = CryptographicKeyGenerator.computeFingerprintFromBase64(publicKeyBase64)
            val entity = CryptographicKeyEntity(
                keyId = keyId,
                alias = alias.ifBlank { "Clave Pública de $ownerDisplayName" },
                algorithm = algorithm.ifBlank { "RSA-2048" },
                purpose = "E2EE_MESSAGING",
                publicKeyBase64 = publicKeyBase64.trim(),
                fingerprint = fingerprint,
                keySizeBits = if (algorithm.contains("4096")) 4096 else 2048,
                isHardwareBacked = false,
                isPrimary = false,
                ownerNodeId = ownerNodeId.ifBlank { "REMOTE-PEER" },
                ownerDisplayName = ownerDisplayName.ifBlank { "Operador Remoto" },
                notes = notes.ifBlank { "Importada manualmente al llavero local Room" }
            )

            repository.insertKey(entity)
            _statusNotification.value = "Clave pública de '$ownerDisplayName' importada exitosamente."
        }
    }

    fun copyToClipboard(label: String, text: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText(label, text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "$label copiado al portapapeles", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error al copiar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
