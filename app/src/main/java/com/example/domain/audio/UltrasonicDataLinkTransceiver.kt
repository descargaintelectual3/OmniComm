package com.example.domain.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sin

/**
 * FASE 12 (Plan Maestro): Enlace Acústico Sub-audible / Ultrasonido P2P (Data-Over-Sound 18–20 kHz):
 * Transmite secuencias binarias mediante modulación FSK (Frequency-Shift Keying) inaudible para el oído humano
 * (Mark: 18.5 kHz, Space: 19.5 kHz) permitiendo comunicación P2P en silencio de radio RF absoluto.
 */
data class UltrasonicTransmissionState(
    val isTransmitting: Boolean = false,
    val markFreqHz: Int = 18500,
    val spaceFreqHz: Int = 19500,
    val baudRate: Int = 50,
    val lastTransmittedPayload: String = "",
    val signalStrengthRssi: Int = -58
)

class UltrasonicDataLinkTransceiver private constructor(private val context: Context) {

    private val _state = MutableStateFlow(UltrasonicTransmissionState())
    val state: StateFlow<UltrasonicTransmissionState> = _state.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var transmitJob: Job? = null

    fun transmitDataOverSound(payload: String) {
        if (_state.value.isTransmitting) return
        val featureManager = com.example.domain.config.FeatureManager.getInstance(context)
        if (!featureManager.enableUltrasonicLink.value) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "ULTRASONIC_LINK",
                message = "Transmisión Data-Over-Sound omitida: Módulo deshabilitado en Modo Lite / Configuración"
            )
            return
        }
        _state.value = _state.value.copy(
            isTransmitting = true,
            lastTransmittedPayload = payload
        )

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "ULTRASONIC_LINK",
            message = "Iniciando transmisión Data-Over-Sound (18.5/19.5 kHz FSK): '$payload'"
        )

        transmitJob = scope.launch {
            try {
                val sampleRate = 44100
                val bitDurationMs = 20
                val samplesPerBit = (sampleRate * bitDurationMs) / 1000

                val audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    samplesPerBit * 2,
                    AudioTrack.MODE_STREAM
                )

                audioTrack.play()

                // Preámbulo de sincronización
                playTone(audioTrack, 18500, samplesPerBit, sampleRate)
                playTone(audioTrack, 19500, samplesPerBit, sampleRate)

                // Transmitir cada byte
                val bytes = payload.toByteArray(Charsets.UTF_8)
                for (b in bytes) {
                    for (i in 7 downTo 0) {
                        val bit = (b.toInt() shr i) and 1
                        val freq = if (bit == 1) 18500 else 19500
                        playTone(audioTrack, freq, samplesPerBit, sampleRate)
                    }
                }

                // Tono de cierre
                playTone(audioTrack, 18500, samplesPerBit, sampleRate)

                audioTrack.stop()
                audioTrack.release()

                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.INFO,
                    tag = "ULTRASONIC_LINK",
                    message = "Transmisión acústica sub-audible completada con éxito (${bytes.size} bytes)"
                )
            } catch (e: Exception) {
                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.WARNING,
                    tag = "ULTRASONIC_LINK",
                    message = "Transmisión de audio finalizada: ${e.message}"
                )
            } finally {
                _state.value = _state.value.copy(isTransmitting = false)
            }
        }
    }

    private fun playTone(track: AudioTrack, freqHz: Int, samples: Int, sampleRate: Int) {
        val buffer = ShortArray(samples)
        for (i in 0 until samples) {
            val angle = 2.0 * Math.PI * i / (sampleRate.toDouble() / freqHz)
            buffer[i] = (sin(angle) * 16000.0).toInt().toShort()
        }
        track.write(buffer, 0, samples)
    }

    fun stopTransmission() {
        transmitJob?.cancel()
        _state.value = _state.value.copy(isTransmitting = false)
    }

    companion object {
        @Volatile
        private var INSTANCE: UltrasonicDataLinkTransceiver? = null

        fun getInstance(context: Context): UltrasonicDataLinkTransceiver {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UltrasonicDataLinkTransceiver(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
