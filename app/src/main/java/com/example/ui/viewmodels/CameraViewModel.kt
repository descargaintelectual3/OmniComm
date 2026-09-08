package com.example.ui.viewmodels

import android.app.Application
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.security.FirebaseAuthManager
import com.example.domain.storage.CloudPhotoItem
import com.example.domain.storage.FirebaseStorageManager
import com.example.domain.storage.UploadStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val storageManager = FirebaseStorageManager(application.applicationContext)
    private val authManager = FirebaseAuthManager(application.applicationContext)

    val uploadStatus: StateFlow<UploadStatus> = storageManager.uploadStatus
    val recentPhotos: StateFlow<List<CloudPhotoItem>> = storageManager.recentPhotos

    private val _isAutoUploadEnabled = MutableStateFlow(true)
    val isAutoUploadEnabled: StateFlow<Boolean> = _isAutoUploadEnabled.asStateFlow()

    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_AUTO)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _isTorchEnabled = MutableStateFlow(false)
    val isTorchEnabled: StateFlow<Boolean> = _isTorchEnabled.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _tacticalTag = MutableStateFlow("EVIDENCIA_TACTICA")
    val tacticalTag: StateFlow<String> = _tacticalTag.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _selectedPhotoForInspection = MutableStateFlow<CloudPhotoItem?>(null)
    val selectedPhotoForInspection: StateFlow<CloudPhotoItem?> = _selectedPhotoForInspection.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _showGalleryDrawer = MutableStateFlow(false)
    val showGalleryDrawer: StateFlow<Boolean> = _showGalleryDrawer.asStateFlow()

    fun toggleAutoUpload() {
        _isAutoUploadEnabled.value = !_isAutoUploadEnabled.value
    }

    fun toggleLensFacing() {
        _lensFacing.value = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
    }

    fun cycleFlashMode() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
    }

    fun toggleTorch() {
        _isTorchEnabled.value = !_isTorchEnabled.value
    }

    fun setZoomRatio(ratio: Float) {
        _zoomRatio.value = ratio.coerceIn(1.0f, 8.0f)
    }

    fun setTacticalTag(tag: String) {
        _tacticalTag.value = tag
    }

    fun setCapturing(capturing: Boolean) {
        _isCapturing.value = capturing
    }

    fun toggleGalleryDrawer() {
        _showGalleryDrawer.value = !_showGalleryDrawer.value
    }

    fun closeGalleryDrawer() {
        _showGalleryDrawer.value = false
    }

    fun selectPhotoForInspection(photo: CloudPhotoItem?) {
        _selectedPhotoForInspection.value = photo
    }

    fun clearBannerMessage() {
        _statusBannerMessage.value = null
    }

    fun refreshCaptures() {
        storageManager.loadLocalAndCloudCaptures()
    }

    /**
     * Llamado cuando CameraX guarda exitosamente la imagen capturada en el almacenamiento local
     */
    fun onPhotoCaptured(photoFile: File, lensName: String, resolution: String) {
        _isCapturing.value = false
        val user = authManager.currentFirebaseUser
        val uid = user?.uid ?: "local_operator"
        val displayName = user?.displayName ?: "Operador Táctico"

        if (_isAutoUploadEnabled.value) {
            _statusBannerMessage.value = "⚡ Subiendo a Firebase Storage..."
            viewModelScope.launch {
                val result = storageManager.uploadPhotoToFirebaseStorage(
                    photoFile = photoFile,
                    tacticalTag = _tacticalTag.value,
                    lens = lensName,
                    resolution = resolution,
                    uploaderUid = uid,
                    uploaderName = displayName
                )
                if (result.isSuccess) {
                    val item = result.getOrNull()
                    _statusBannerMessage.value = "☁️ Subida a Firebase Storage completada: ${photoFile.name}"
                    _selectedPhotoForInspection.value = item
                } else {
                    _statusBannerMessage.value = "⚠️ Guardada localmente (Error en subida a nube: ${result.exceptionOrNull()?.message})"
                }
            }
        } else {
            _statusBannerMessage.value = "📸 Guardada en Bóveda Local: ${photoFile.name}"
            storageManager.loadLocalAndCloudCaptures()
        }
    }

    /**
     * Subir manualmente una foto guardada localmente hacia Firebase Storage
     */
    fun uploadExistingPhoto(item: CloudPhotoItem) {
        val path = item.localPath ?: return
        val file = File(path)
        if (!file.exists()) {
            _statusBannerMessage.value = "❌ Archivo local no encontrado"
            return
        }

        val user = authManager.currentFirebaseUser
        val uid = user?.uid ?: "local_operator"
        val displayName = user?.displayName ?: "Operador Táctico"

        _statusBannerMessage.value = "⚡ Subiendo ${file.name} a Firebase Storage..."
        viewModelScope.launch {
            val result = storageManager.uploadPhotoToFirebaseStorage(
                photoFile = file,
                tacticalTag = item.tacticalTag,
                lens = item.lens,
                resolution = item.resolution,
                uploaderUid = uid,
                uploaderName = displayName
            )
            if (result.isSuccess) {
                _statusBannerMessage.value = "☁️ Subida a Firebase Storage completada!"
                _selectedPhotoForInspection.value = result.getOrNull()
            } else {
                _statusBannerMessage.value = "❌ Error subiendo a Firebase Storage: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    /**
     * Eliminar foto de la nube y del almacenamiento local
     */
    fun deletePhoto(item: CloudPhotoItem) {
        viewModelScope.launch {
            val result = storageManager.deletePhoto(item)
            if (result.isSuccess) {
                _statusBannerMessage.value = "🗑️ Foto eliminada correctamente"
                if (_selectedPhotoForInspection.value?.id == item.id) {
                    _selectedPhotoForInspection.value = null
                }
            } else {
                _statusBannerMessage.value = "❌ Error al eliminar foto: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun resetUploadStatus() {
        storageManager.resetUploadStatus()
    }
}
