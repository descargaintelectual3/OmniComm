package com.example.domain.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * Sintetizador Táctico de Código Morse y Balizas de Supervivencia:
 * Genera tonos senoidales precisos (800 Hz / 1000 Hz) para señales de emergencia acústicas y ópticas.
 */
object MorseBeaconSynthesizer {

    private val _isTransmitting = MutableStateFlow(false)
    val isTransmitting: StateFlow<Boolean> = _isTransmitting.asStateFlow()

    private val MORSE_DICTIONARY = mapOf(
        'A' to ".-", 'B' to "-...", 'C' to "-.-.", 'D' to "-..", 'E' to ".",
        'F' to "..-.", 'G' to "--.", 'H' to "....", 'I' to "..", 'J' to ".---",
        'K' to "-.-", 'L' to ".-..", 'M' to "--", 'N' to "-.", 'O' to "---",
        'P' to ".--.", 'Q' to "--.-", 'R' to ".-.", 'S' to "...", 'T' to "-",
        'U' to "..-", 'V' to "...-", 'W' to ".--", 'X' to "-..-", 'Y' to "-.--",
        'Z' to "--..", '0' to "-----", '1' to ".----", '2' to "..---",
        '3' to "...--", '4' to "....-", '5' to ".....", '6' to "-....",
        '7' to "--...", '8' to "---..", '9' to "----.", ' ' to "/"
    )

    fun textToMorse(text: String): String {
        return text.uppercase().map { char ->
            MORSE_DICTIONARY[char] ?: ""
        }.filter { it.isNotEmpty() }.joinToString(" ")
    }

    suspend fun transmitMorse(
        morseString: String,
        frequencyHz: Int = 850,
        unitDurationMs: Long = 100L
    ) = withContext(Dispatchers.IO) {
        if (_isTransmitting.value) return@withContext
        _isTransmitting.value = true

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "MORSE_BEACON",
            message = "Iniciando transmisión de baliza Morse acústica ($frequencyHz Hz)"
        )

        try {
            val sampleRate = 44100
            for (char in morseString) {
                if (!_isTransmitting.value) break

                when (char) {
                    '.' -> {
                        playSineTone(frequencyHz, unitDurationMs, sampleRate)
                        kotlinx.coroutines.delay(unitDurationMs) // Espacio inter-elemento
                    }
                    '-' -> {
                        playSineTone(frequencyHz, unitDurationMs * 3, sampleRate)
                        kotlinx.coroutines.delay(unitDurationMs) // Espacio inter-elemento
                    }
                    ' ' -> {
                        kotlinx.coroutines.delay(unitDurationMs * 3) // Espacio entre letras
                    }
                    '/' -> {
                        kotlinx.coroutines.delay(unitDurationMs * 7) // Espacio entre palabras
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            _isTransmitting.value = false
        }
    }

    fun stopTransmission() {
        _isTransmitting.value = false
    }

    private fun playSineTone(freqHz: Int, durationMs: Long, sampleRate: Int) {
        val numSamples = (durationMs * sampleRate / 1000).toInt()
        val generatedSnd = ByteArray(2 * numSamples)

        var idx = 0
        for (i in 0 until numSamples) {
            val dVal = sin(2.0 * Math.PI * i / (sampleRate.toDouble() / freqHz.toDouble()))
            val valShort = (dVal * 32767).toInt().toShort()
            generatedSnd[idx++] = (valShort.toInt() and 0x00ff).toByte()
            generatedSnd[idx++] = ((valShort.toInt() and 0xff00) ushr 8).toByte()
        }

        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(generatedSnd.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(generatedSnd, 0, generatedSnd.size)
            audioTrack.play()
            Thread.sleep(durationMs)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            // Manejo silencioso en caso de hardware ocupado
        }
    }
}
