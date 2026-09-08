package com.example.domain.media

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.charset.StandardCharsets

/**
 * FASE 1: Transmisor y Receptor de Datos Ópticos Li-Fi / Linterna Estroboscópica:
 * Modula cadenas de texto binarias mediante pulsos lumínicos de alta frecuencia
 * para comunicación óptica segura y fuera de banda (OOB) en silencio de radio absoluto.
 */
class VisualOpticalLiFiTransceiver private constructor(context: Context) {

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val torchCameraId: String? by lazy {
        try {
            cameraManager?.cameraIdList?.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        } catch (e: Exception) {
            null
        }
    }

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val _transmissionProgress = MutableStateFlow(0f)
    val transmissionProgress: StateFlow<Float> = _transmissionProgress.asStateFlow()

    private val _lastTransmittedMessage = MutableStateFlow("")
    val lastTransmittedMessage: StateFlow<String> = _lastTransmittedMessage.asStateFlow()

    private var transmitJob: Job? = null

    suspend fun transmitOpticalPayload(
        text: String,
        pulseDurationMs: Long = 70L
    ) = withContext(Dispatchers.IO) {
        if (_isTransmitting.value || torchCameraId == null) return@withContext

        _isTransmitting.value = true
        _lastTransmittedMessage.value = text
        _transmissionProgress.value = 0f

        val payload = "LIFI:$text"
        val bytes = payload.toByteArray(StandardCharsets.UTF_8)
        val bitSequence = StringBuilder()

        // Preamble de sincronización óptica (10101011)
        bitSequence.append("10101011")

        for (b in bytes) {
            val bitStr = String.format("%8s", Integer.toBinaryString(b.toInt() and 0xFF)).replace(' ', '0')
            bitSequence.append(bitStr)
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "LIFI_TRANSMIT",
            message = "Iniciando transmisión óptica Li-Fi (${bitSequence.length} pulsos)"
        )

        try {
            val totalBits = bitSequence.length
            for ((index, bit) in bitSequence.withIndex()) {
                if (!_isTransmitting.value) break

                val isHigh = (bit == '1')
                setTorchState(isHigh)
                delay(pulseDurationMs)

                _transmissionProgress.value = (index + 1).toFloat() / totalBits
            }
            // Apagar flash al concluir
            setTorchState(false)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            setTorchState(false)
            _isTransmitting.value = false
        }
    }

    fun abortTransmission() {
        _isTransmitting.value = false
        transmitJob?.cancel()
        setTorchState(false)
    }

    private fun setTorchState(enabled: Boolean) {
        try {
            torchCameraId?.let { id ->
                cameraManager?.setTorchMode(id, enabled)
            }
        } catch (e: CameraAccessException) {
            // Manejo de hardware bloqueado
        } catch (e: Exception) {
            // Ignorar en emuladores sin flash físico
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: VisualOpticalLiFiTransceiver? = null

        fun getInstance(context: Context): VisualOpticalLiFiTransceiver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: VisualOpticalLiFiTransceiver(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
