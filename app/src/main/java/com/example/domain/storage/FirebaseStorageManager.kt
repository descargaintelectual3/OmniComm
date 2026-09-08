package com.example.domain.storage

import android.content.Context
import android.net.Uri
import android.util.Log
import com.example.domain.local.OmniDatabase
import com.example.domain.local.entities.CachedFileEntity
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed class UploadStatus {
    object Idle : UploadStatus()
    data class InProgress(
        val progressPercent: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val fileName: String
    ) : UploadStatus()
    data class Success(
        val downloadUrl: String,
        val storagePath: String,
        val fileName: String,
        val sizeBytes: Long,
        val durationMs: Long
    ) : UploadStatus()
    data class Error(
        val errorMessage: String,
        val fileName: String
    ) : UploadStatus()
}

data class CloudPhotoItem(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val localPath: String? = null,
    val storagePath: String = "",
    val downloadUrl: String = "",
    val sizeBytes: Long = 0L,
    val capturedAt: Long = System.currentTimeMillis(),
    val isUploadedToCloud: Boolean = true,
    val uploaderUid: String = "",
    val uploaderName: String = "Operador Táctico",
    val tacticalTag: String = "E2EE_CAMERA_STREAM",
    val resolution: String = "1920x1080",
    val lens: String = "POSTERIOR"
)

class FirebaseStorageManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val database = OmniDatabase.getDatabase(context)

    private val _uploadStatus = MutableStateFlow<UploadStatus>(UploadStatus.Idle)
    val uploadStatus: StateFlow<UploadStatus> = _uploadStatus.asStateFlow()

    private val _recentPhotos = MutableStateFlow<List<CloudPhotoItem>>(emptyList())
    val recentPhotos: StateFlow<List<CloudPhotoItem>> = _recentPhotos.asStateFlow()

    init {
        loadLocalAndCloudCaptures()
        listenToFirestorePhotoStream()
    }

    /**
     * Cargar fotos locales de la bóveda
     */
    fun loadLocalAndCloudCaptures() {
        scope.launch {
            try {
                val vaultDir = File(context.filesDir, "vault")
                val localFiles = if (vaultDir.exists()) {
                    vaultDir.listFiles { file -> 
                        file.isFile && (file.name.endsWith(".jpg", ignoreCase = true) || file.name.endsWith(".png", ignoreCase = true))
                    }?.toList() ?: emptyList()
                } else emptyList()

                val localItems = localFiles.sortedByDescending { it.lastModified() }.map { file ->
                    CloudPhotoItem(
                        id = file.name,
                        fileName = file.name,
                        localPath = file.absolutePath,
                        storagePath = "camera_captures/${file.name}",
                        downloadUrl = "",
                        sizeBytes = file.length(),
                        capturedAt = file.lastModified(),
                        isUploadedToCloud = false,
                        uploaderName = "Almacenamiento Local"
                    )
                }

                // Combinar con las ya cargadas manteniendo los estados en la nube
                val current = _recentPhotos.value.toMutableList()
                localItems.forEach { local ->
                    if (current.none { it.fileName == local.fileName }) {
                        current.add(local)
                    }
                }
                _recentPhotos.value = current.sortedByDescending { it.capturedAt }
            } catch (e: Exception) {
                Log.w(TAG, "Error cargando capturas locales: ${e.message}")
            }
        }
    }

    /**
     * Escucha en tiempo real de capturas registradas en Firestore
     */
    private fun listenToFirestorePhotoStream() {
        firestore.collection("camera_captures")
            .orderBy("capturedAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Error escuchando /camera_captures: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val cloudItems = snapshot.documents.mapNotNull { doc ->
                        try {
                            CloudPhotoItem(
                                id = doc.id,
                                fileName = doc.getString("fileName") ?: doc.id,
                                localPath = doc.getString("localPath"),
                                storagePath = doc.getString("storagePath") ?: "",
                                downloadUrl = doc.getString("downloadUrl") ?: "",
                                sizeBytes = doc.getLong("sizeBytes") ?: 0L,
                                capturedAt = doc.getLong("capturedAt") ?: System.currentTimeMillis(),
                                isUploadedToCloud = true,
                                uploaderUid = doc.getString("uploaderUid") ?: "",
                                uploaderName = doc.getString("uploaderName") ?: "Operador",
                                tacticalTag = doc.getString("tacticalTag") ?: "OPTICAL_CAPTURE",
                                resolution = doc.getString("resolution") ?: "1080p",
                                lens = doc.getString("lens") ?: "POSTERIOR"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }

                    // Fusionar capturas locales con las remotas
                    val currentList = _recentPhotos.value.toMutableList()
                    cloudItems.forEach { cloud ->
                        val existingIndex = currentList.indexOfFirst { it.fileName == cloud.fileName }
                        if (existingIndex >= 0) {
                            val local = currentList[existingIndex]
                            currentList[existingIndex] = cloud.copy(
                                localPath = local.localPath ?: cloud.localPath
                            )
                        } else {
                            currentList.add(cloud)
                        }
                    }
                    _recentPhotos.value = currentList.sortedByDescending { it.capturedAt }
                }
            }
    }

    /**
     * Subir foto capturada a Firebase Storage y registrar metadatos en Firestore + Room
     */
    suspend fun uploadPhotoToFirebaseStorage(
        photoFile: File,
        tacticalTag: String = "OPTICAL_STREAM",
        lens: String = "POSTERIOR",
        resolution: String = "1920x1080",
        uploaderUid: String = FirebaseAuth.getInstance().currentUser?.uid ?: "local_operator",
        uploaderName: String = FirebaseAuth.getInstance().currentUser?.displayName ?: "Operador Táctico"
    ): Result<CloudPhotoItem> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        val fileName = photoFile.name
        val storagePath = "camera_captures/$uploaderUid/$fileName"
        val storageRef = storage.reference.child(storagePath)

        _uploadStatus.value = UploadStatus.InProgress(
            progressPercent = 0,
            bytesTransferred = 0L,
            totalBytes = photoFile.length(),
            fileName = fileName
        )

        try {
            val fileUri = Uri.fromFile(photoFile)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("tacticalTag", tacticalTag)
                .setCustomMetadata("uploaderName", uploaderName)
                .setCustomMetadata("uploaderUid", uploaderUid)
                .setCustomMetadata("lens", lens)
                .setCustomMetadata("resolution", resolution)
                .setCustomMetadata("capturedTimestamp", System.currentTimeMillis().toString())
                .build()

            val uploadTask = storageRef.putFile(fileUri, metadata)

            // Monitorear progreso de subida en tiempo real
            uploadTask.addOnProgressListener { taskSnapshot ->
                val progress = if (taskSnapshot.totalByteCount > 0) {
                    ((taskSnapshot.bytesTransferred.toDouble() / taskSnapshot.totalByteCount) * 100).toInt()
                } else 0
                _uploadStatus.value = UploadStatus.InProgress(
                    progressPercent = progress.coerceIn(0, 100),
                    bytesTransferred = taskSnapshot.bytesTransferred,
                    totalBytes = taskSnapshot.totalByteCount,
                    fileName = fileName
                )
            }

            // Esperar que complete la subida
            uploadTask.await()

            // Obtener URL pública de descarga desde Firebase Storage
            val downloadUrl = storageRef.downloadUrl.await().toString()
            val durationMs = System.currentTimeMillis() - startTime

            val cloudItem = CloudPhotoItem(
                id = UUID.randomUUID().toString(),
                fileName = fileName,
                localPath = photoFile.absolutePath,
                storagePath = storagePath,
                downloadUrl = downloadUrl,
                sizeBytes = photoFile.length(),
                capturedAt = System.currentTimeMillis(),
                isUploadedToCloud = true,
                uploaderUid = uploaderUid,
                uploaderName = uploaderName,
                tacticalTag = tacticalTag,
                resolution = resolution,
                lens = lens
            )

            // 1. Guardar metadatos en Firestore
            val firestorePayload = hashMapOf(
                "fileName" to fileName,
                "storagePath" to storagePath,
                "downloadUrl" to downloadUrl,
                "sizeBytes" to photoFile.length(),
                "capturedAt" to System.currentTimeMillis(),
                "uploaderUid" to uploaderUid,
                "uploaderName" to uploaderName,
                "tacticalTag" to tacticalTag,
                "resolution" to resolution,
                "lens" to lens
            )
            firestore.collection("camera_captures").document(cloudItem.id).set(firestorePayload, SetOptions.merge()).await()

            // 2. Indexar en Room Database
            try {
                val cachedFile = CachedFileEntity(
                    fileId = cloudItem.id,
                    fileName = fileName,
                    localAbsolutePath = photoFile.absolutePath,
                    sizeBytes = photoFile.length(),
                    downloadedAt = System.currentTimeMillis(),
                    isSyncedWithCloud = true
                )
                database.cachedFileDao().insertFile(cachedFile)
            } catch (e: Exception) {
                Log.w(TAG, "Error guardando en Room: ${e.message}")
            }

            // 3. Registrar en Telemetría de la App
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.SUCCESS,
                tag = "FIREBASE_STORAGE",
                message = "📸 Foto '$fileName' subida exitosamente a Firebase Storage (${durationMs}ms)",
                rawPayload = "{ \"storagePath\": \"$storagePath\", \"sizeBytes\": ${photoFile.length()}, \"downloadUrl\": \"$downloadUrl\" }",
                durationMs = durationMs
            )

            _uploadStatus.value = UploadStatus.Success(
                downloadUrl = downloadUrl,
                storagePath = storagePath,
                fileName = fileName,
                sizeBytes = photoFile.length(),
                durationMs = durationMs
            )

            // Actualizar lista en memoria
            val updated = _recentPhotos.value.toMutableList()
            val idx = updated.indexOfFirst { it.fileName == fileName }
            if (idx >= 0) {
                updated[idx] = cloudItem
            } else {
                updated.add(0, cloudItem)
            }
            _recentPhotos.value = updated

            Result.success(cloudItem)
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Error desconocido durante la subida a Firebase Storage"
            Log.e(TAG, "Fallo al subir foto a Firebase Storage", e)
            _uploadStatus.value = UploadStatus.Error(
                errorMessage = errorMsg,
                fileName = fileName
            )

            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.ERROR,
                tag = "STORAGE_ERROR",
                message = "❌ Error subiendo '$fileName' a Firebase Storage: $errorMsg",
                rawPayload = "{ \"error\": \"$errorMsg\", \"file\": \"$fileName\" }"
            )

            Result.failure(e)
        }
    }

    /**
     * Eliminar foto de Firebase Storage y metadatos
     */
    suspend fun deletePhoto(item: CloudPhotoItem): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (item.storagePath.isNotBlank()) {
                val ref = storage.reference.child(item.storagePath)
                ref.delete().await()
            }
            firestore.collection("camera_captures").document(item.id).delete().await()

            // Eliminar archivo local si existe
            if (!item.localPath.isNullOrBlank()) {
                val localFile = File(item.localPath)
                if (localFile.exists()) localFile.delete()
            }

            // Eliminar de Room
            try {
                database.cachedFileDao().deleteFileById(item.id)
            } catch (_: Exception) {}

            _recentPhotos.value = _recentPhotos.value.filter { it.id != item.id && it.fileName != item.fileName }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Error eliminando foto: ${e.message}")
            Result.failure(e)
        }
    }

    fun resetUploadStatus() {
        _uploadStatus.value = UploadStatus.Idle
    }

    companion object {
        private const val TAG = "FirebaseStorageManager"
    }
}
