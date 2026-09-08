package com.example.ui.viewmodels

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.local.MediaCache
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.CachedFileEntity
import com.example.domain.storage.CloudVaultFileMetadata
import com.example.domain.storage.CloudVaultTransferState
import com.example.domain.storage.EncryptedCloudStorageService
import com.example.domain.storage.FirebaseEncryptedCloudStorageService
import com.example.domain.storage.CloudVaultSyncEngine
import com.example.domain.storage.CloudVaultFileVersion
import com.example.domain.storage.ConflictResolutionPolicy
import com.example.domain.storage.CloudSyncSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class CloudViewModel(application: Application) : AndroidViewModel(application) {
    private val database = OmniDatabase.getDatabase(application)
    private val cachedFileDao = database.cachedFileDao()
    private val mediaCache = MediaCache(application)
    private val filesDir = application.filesDir
    val encryptedCloudStorage: EncryptedCloudStorageService = FirebaseEncryptedCloudStorageService(application)

    val files: StateFlow<List<CachedFileEntity>> = cachedFileDao.getAllCachedFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _status = MutableStateFlow("🟢 Bóveda Cifrada Activa (AES-256-GCM + SQLCipher)")
    val status: StateFlow<String> = _status.asStateFlow()

    private val _storageStats = MutableStateFlow("Calculando...")
    val storageStats: StateFlow<String> = _storageStats.asStateFlow()

    private val _selectedFileContent = MutableStateFlow<String?>(null)
    val selectedFileContent: StateFlow<String?> = _selectedFileContent.asStateFlow()

    private val _cloudTransferState = MutableStateFlow<CloudVaultTransferState>(CloudVaultTransferState.Idle)
    val cloudTransferState: StateFlow<CloudVaultTransferState> = _cloudTransferState.asStateFlow()

    private val _cloudVaultFiles = MutableStateFlow<List<CloudVaultFileMetadata>>(emptyList())
    val cloudVaultFiles: StateFlow<List<CloudVaultFileMetadata>> = _cloudVaultFiles.asStateFlow()

    val syncEngine = CloudVaultSyncEngine(application, encryptedCloudStorage)
    val syncSummary: StateFlow<CloudSyncSummary> = syncEngine.syncState
    val fileVersionsMap: StateFlow<Map<String, List<CloudVaultFileVersion>>> = syncEngine.fileVersionsMap

    init {
        ensureInitialVaultFiles()
        updateStorageStats()
        loadCloudVaultFiles()
    }

    fun triggerBackgroundDeltaSync() {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = "🔄 Sincronizando Bóveda Delta con la Nube..."
            val res = syncEngine.executeDeltaSync()
            res.fold(
                onSuccess = { summary ->
                    _status.value = summary.statusMessage
                    loadCloudVaultFiles()
                },
                onFailure = { err ->
                    _status.value = "⚠️ Error en delta sync: ${err.message}"
                }
            )
        }
    }

    fun setConflictResolutionPolicy(policy: ConflictResolutionPolicy) {
        syncEngine.setConflictPolicy(policy)
        _status.value = "⚙️ Política de resolución: ${policy.label}"
    }

    fun commitVersion(fileEntity: CachedFileEntity, note: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(fileEntity.localAbsolutePath)
            if (file.exists()) {
                _status.value = "📝 Creando commit de versión para ${fileEntity.fileName}..."
                val res = syncEngine.commitNewVersion(fileEntity.fileId, fileEntity.fileName, file, note)
                res.fold(
                    onSuccess = { v ->
                        _status.value = "✅ Versión v${v.versionNumber} creada para ${fileEntity.fileName}"
                        loadCloudVaultFiles()
                    },
                    onFailure = { err ->
                        _status.value = "❌ Error al versionar: ${err.message}"
                    }
                )
            }
        }
    }

    fun rollbackFileVersion(fileId: String, versionNumber: Int, localPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = "⏳ Revirtiendo a versión v$versionNumber..."
            val dest = File(localPath)
            val res = syncEngine.rollbackToVersion(fileId, targetVersionNumber = versionNumber, destinationFile = dest)
            res.fold(
                onSuccess = { v ->
                    _status.value = "⏪ Restaurado a v${v.versionNumber} (${v.commitNote})"
                },
                onFailure = { err ->
                    _status.value = "⚠️ Fallo al revertir versión: ${err.message}"
                }
            )
        }
    }

    fun loadCloudVaultFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val result = encryptedCloudStorage.listVaultFiles()
            result.onSuccess { list ->
                _cloudVaultFiles.value = list
            }
        }
    }

    fun uploadEncryptedToFirebase(fileEntity: CachedFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val localFile = File(fileEntity.localAbsolutePath)
            if (!localFile.exists()) {
                _status.value = "⚠️ Archivo no encontrado en almacenamiento local"
                return@launch
            }

            _status.value = "🔒 Cifrando ${localFile.name} con AES-256-GCM y subiendo..."
            _cloudTransferState.value = CloudVaultTransferState.Transferring(localFile.name, 25, 0, localFile.length(), true)

            val result = encryptedCloudStorage.uploadEncryptedFile(localFile)
            result.fold(
                onSuccess = { meta ->
                    _cloudTransferState.value = CloudVaultTransferState.Completed(
                        localFile.name, true, "Subida cifrada completada con AES-256-GCM (IV: ${meta.ivBase64.take(8)}...)"
                    )
                    _status.value = "☁️ Subido a Firebase Storage con Cifrado AES-256-GCM"
                    cachedFileDao.updateSyncStatus(fileEntity.fileId, true)
                    loadCloudVaultFiles()
                },
                onFailure = { err ->
                    _cloudTransferState.value = CloudVaultTransferState.Failed(localFile.name, err.localizedMessage ?: "Error de subida")
                    _status.value = "❌ Error en subida cifrada: ${err.message}"
                }
            )
        }
    }

    fun downloadAndDecryptFromFirebase(meta: CloudVaultFileMetadata) {
        viewModelScope.launch(Dispatchers.IO) {
            _status.value = "📥 Descargando y descifrando ${meta.fileName}..."
            _cloudTransferState.value = CloudVaultTransferState.Transferring(meta.fileName, 50, 0, meta.originalSizeBytes, false)

            val vaultDir = File(filesDir, "vault").apply { if (!exists()) mkdirs() }
            val destinationFile = File(vaultDir, meta.fileName)

            val result = encryptedCloudStorage.downloadAndDecryptFile(meta, destinationFile)
            result.fold(
                onSuccess = { decryptedFile ->
                    _cloudTransferState.value = CloudVaultTransferState.Completed(
                        meta.fileName, false, "Descifrado completado y verificado por SHA-256"
                    )
                    _status.value = "✅ Archivo ${meta.fileName} descifrado e integrado a la bóveda"

                    // Registrar en SQLite local
                    val entity = CachedFileEntity(
                        fileId = meta.fileId,
                        fileName = meta.fileName,
                        localAbsolutePath = decryptedFile.absolutePath,
                        sizeBytes = decryptedFile.length(),
                        downloadedAt = System.currentTimeMillis(),
                        isSyncedWithCloud = true
                    )
                    cachedFileDao.insertFile(entity)
                    updateStorageStats()
                },
                onFailure = { err ->
                    _cloudTransferState.value = CloudVaultTransferState.Failed(meta.fileName, err.localizedMessage ?: "Error")
                    _status.value = "❌ Error al descifrar: ${err.message}"
                }
            )
        }
    }

    private fun ensureInitialVaultFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val vaultDir = File(filesDir, "vault").apply { if (!exists()) mkdirs() }
            val existing = vaultDir.listFiles()
            if (existing == null || existing.isEmpty()) {
                // Generar archivos iniciales de seguridad y configuración de la malla
                createDocumentFile(
                    title = "protocolo_seguridad_malla.txt",
                    content = "=== PROTOCOLO DE SEGURIDAD OMNICOMM ===\n" +
                            "1. Cifrado AES-256-GCM activo para todos los enlaces.\n" +
                            "2. Topología auto-regenerativa con descubrimiento BLE/Wi-Fi.\n" +
                            "3. Bóveda SQLCipher persistente en disco.\n" +
                            "4. Clave de sesión renovada dinámicamente.\n" +
                            "Fecha de inicialización: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}"
                )
                createDocumentFile(
                    title = "configuracion_nodos.json",
                    content = "{\n  \"mesh_id\": \"OMNICOMM_SECURE_NET\",\n  \"encryption\": \"AES_256_GCM\",\n  \"max_hops\": 7,\n  \"auto_heal\": true,\n  \"heartbeat_ms\": 3000\n}"
                )
            }
            updateStorageStats()
        }
    }

    fun updateStorageStats() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val vaultDir = File(filesDir, "vault")
                var totalBytes = 0L
                vaultDir.listFiles()?.forEach { totalBytes += it.length() }
                
                val cacheBytes = (mediaCache.getCacheSizeMB() * 1024 * 1024).toLong()
                val totalUsed = totalBytes + cacheBytes
                val usedKb = totalUsed / 1024f
                val freeSpaceMb = filesDir.freeSpace / (1024 * 1024f)
                
                _storageStats.value = String.format(Locale.getDefault(), "Uso: %.1f KB • Libre: %.0f MB", usedKb, freeSpaceMb)
            } catch (e: Exception) {
                _storageStats.value = "Almacenamiento Local OK"
            }
        }
    }

    fun createDocumentFile(title: String, content: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sanitizedName = if (title.contains(".")) title else "$title.txt"
                val vaultDir = File(filesDir, "vault").apply { if (!exists()) mkdirs() }
                val targetFile = File(vaultDir, sanitizedName)
                targetFile.writeText(content)

                val entity = CachedFileEntity(
                    fileId = UUID.randomUUID().toString(),
                    fileName = sanitizedName,
                    localAbsolutePath = targetFile.absolutePath,
                    sizeBytes = targetFile.length(),
                    downloadedAt = System.currentTimeMillis(),
                    isSyncedWithCloud = true
                )
                cachedFileDao.insertFile(entity)
                _status.value = "📄 Archivo guardado: $sanitizedName"
                updateStorageStats()
            } catch (e: Exception) {
                Log.e("CloudViewModel", "Error al crear archivo", e)
                _status.value = "❌ Error al escribir archivo"
            }
        }
    }

    fun deleteFile(file: CachedFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val diskFile = File(file.localAbsolutePath)
                if (diskFile.exists()) {
                    diskFile.delete()
                }
                cachedFileDao.deleteFile(file)
                _status.value = "🗑️ Archivo eliminado: ${file.fileName}"
                updateStorageStats()
            } catch (e: Exception) {
                Log.e("CloudViewModel", "Error al eliminar archivo", e)
            }
        }
    }

    fun readFileContent(file: CachedFileEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val diskFile = File(file.localAbsolutePath)
                if (diskFile.exists()) {
                    val text = diskFile.readText()
                    _selectedFileContent.value = text
                } else {
                    _selectedFileContent.value = "El archivo físico no se encontró en el disco local."
                }
            } catch (e: Exception) {
                _selectedFileContent.value = "Error al leer contenido: ${e.localizedMessage}"
            }
        }
    }

    fun clearSelectedFileContent() {
        _selectedFileContent.value = null
    }
}
