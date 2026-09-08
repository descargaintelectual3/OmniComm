package com.example.domain.media

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sin

data class AcousticModemState(
    val isTransmitting: Boolean = false,
    val isReceiving: Boolean = false,
    val centerFreqKhz: Double = 19.0,
    val baudRate: Int = 100,
    val packetsTransmitted: Int = 0,
    val lastDecodedText: String? = null
)

/**
 * Módem de Datos Acústicos Ultrasonido FSK (Data-Over-Sound Acoustic Modem).
 * Opera en banda de 18.5 kHz (Espacio / '0') y 19.5 kHz (Marca / '1') para
 * permitir comunicación física punto a punto entre terminales bajo EMCON Alpha
 * (Silencio Total de Radioeléctrico / Jamming masivo de RF).
 */
class DataOverSoundAcousticModem private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Default)

    private val _state = MutableStateFlow(AcousticModemState())
    val state: StateFlow<AcousticModemState> = _state.asStateFlow()

    private val FREQ_MARK_HZ = 19500.0  // Bit 1 (Inaudible para adultos humanos)
    private val FREQ_SPACE_HZ = 18500.0 // Bit 0
    private val SAMPLE_RATE = 44100
    private val SAMPLES_PER_BIT = (SAMPLE_RATE / 100) // 100 Baudios (~10ms por bit)

    companion object {
        @Volatile
        private var INSTANCE: DataOverSoundAcousticModem? = null

        fun getInstance(context: Context): DataOverSoundAcousticModem {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DataOverSoundAcousticModem(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Transmite una cadena de texto corta (SOS o coordenadas) mediante modulación FSK ultrasónica.
     */
    fun transmitAcousticData(text: String) {
        if (_state.value.isTransmitting) return
        _state.value = _state.value.copy(isTransmitting = true)

        scope.launch {
            try {
                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.INFO,
                    tag = "DATA_OVER_SOUND",
                    message = "Iniciando ráfaga ultrasónica FSK (EMCON Alpha) [18.5kHz-19.5kHz]: $text"
                )

                val bytes = text.toByteArray(Charsets.UTF_8)
                val bitList = mutableListOf<Boolean>()

                // Preamble de sincronización: 8 bits alternados 10101010
                for (i in 0..7) {
                    bitList.add(i % 2 == 0)
                }

                for (b in bytes) {
                    for (bitIndex in 7 downTo 0) {
                        bitList.add(((b.toInt() shr bitIndex) and 1) == 1)
                    }
                }

                // Generar muestras PCM
                val totalSamples = bitList.size * SAMPLES_PER_BIT
                val pcmBuffer = ShortArray(totalSamples)

                var samplePointer = 0
                for (bit in bitList) {
                    val freq = if (bit) FREQ_MARK_HZ else FREQ_SPACE_HZ
                    for (s in 0 until SAMPLES_PER_BIT) {
                        val angle = 2.0 * Math.PI * s / (SAMPLE_RATE / freq)
                        // Ventana Tukey suave para evitar clics audibles
                        val window = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * s / SAMPLES_PER_BIT))
                        pcmBuffer[samplePointer++] = (sin(angle) * window * Short.MAX_VALUE * 0.85).toInt().toShort()
                    }
                }

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(totalSamples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmBuffer, 0, totalSamples)
                audioTrack.play()

                val durationMs = (totalSamples * 1000L) / SAMPLE_RATE
                kotlinx.coroutines.delay(durationMs + 200L)
                audioTrack.release()

                _state.value = _state.value.copy(
                    isTransmitting = false,
                    packetsTransmitted = _state.value.packetsTransmitted + 1,
                    lastDecodedText = text
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isTransmitting = false)
            }
        }
    }
}
