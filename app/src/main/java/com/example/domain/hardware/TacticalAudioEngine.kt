package com.example.domain.hardware

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.domain.ai.GeminiAudioTranscriptionEngine
import com.example.domain.local.OfflineMessageQueue
import com.example.domain.local.entities.ChatMessageEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Motor de Audio Táctico P2P (Baja Latencia).
 * Utiliza AudioRecord y AudioTrack de Android para capturar y reproducir PCM en tiempo real.
 * Se acopla al DSP físico (Acoustic Echo Canceler, Noise Suppressor) para calidad VoIP.
 */
class TacticalAudioEngine(
    private val context: Context,
    private val offlineMessageQueue: OfflineMessageQueue? = null // Inyectado para Logging
) {
    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    
    // Configuración estándar para VoIP (16 kHz, Mono, 16-bit PCM)
    private val sampleRate = 16000
    private val channelConfigIn = AudioFormat.CHANNEL_IN_MONO
    private val channelConfigOut = AudioFormat.CHANNEL_OUT_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    
    private val bufferSizeIn = AudioRecord.getMinBufferSize(sampleRate, channelConfigIn, audioFormat)
    private val bufferSizeOut = AudioTrack.getMinBufferSize(sampleRate, channelConfigOut, audioFormat)
    
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.IO)
    private val mediaController = AdvancedMediaController(context)
    private val aiTranscriptionEngine = GeminiAudioTranscriptionEngine()
    
    private var sessionAudioBuffer: ByteArrayOutputStream? = null

    fun startAudioSession(onAudioDataCaptured: (ByteArray) -> Unit) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e("TacticalAudio", "Permiso RECORD_AUDIO denegado por el usuario/sistema.")
            return
        }

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                sampleRate,
                channelConfigIn,
                audioFormat,
                bufferSizeIn
            )

            audioTrack = AudioTrack(
                AudioManager.STREAM_VOICE_CALL,
                sampleRate,
                channelConfigOut,
                audioFormat,
                bufferSizeOut,
                AudioTrack.MODE_STREAM
            )

            val sessionId = audioRecord?.audioSessionId ?: -1
            if (sessionId != -1) {
                mediaController.optimizeAudioForConferencing(sessionId)
            }

            audioRecord?.startRecording()
            audioTrack?.play()
            isRunning.set(true)
            sessionAudioBuffer = ByteArrayOutputStream()

            Log.d("TacticalAudio", "🎙️ Sesión de Audio P2P Iniciada (HW DSP Acoplado).")

            scope.launch {
                val buffer = ByteArray(bufferSizeIn)
                while (isRunning.get()) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readBytes > 0) {
                        val validData = buffer.copyOfRange(0, readBytes)
                        sessionAudioBuffer?.write(validData)
                        onAudioDataCaptured(validData)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TacticalAudio", "Fallo al inicializar motor de audio nativo", e)
        }
    }

    fun playIncomingAudio(data: ByteArray) {
        if (isRunning.get()) {
            try {
                audioTrack?.write(data, 0, data.size)
            } catch (e: Exception) {
                Log.e("TacticalAudio", "Error al escribir en AudioTrack buffer", e)
            }
        }
    }

    fun stopAudioSession() {
        isRunning.set(false)
        try {
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            
            Log.d("TacticalAudio", "🔇 Sesión de Audio P2P Detenida y Recursos Liberados.")
            
            // Post-procesamiento: Transcripción AI y Logging Offline
            val audioBytes = sessionAudioBuffer?.toByteArray()
            if (audioBytes != null && audioBytes.isNotEmpty() && offlineMessageQueue != null) {
                scope.launch {
                    Log.d("TacticalAudio", "🧠 Iniciando Transcripción de Audio On-Device via Gemini API...")
                    val transcription = aiTranscriptionEngine.transcribeAudio(audioBytes)
                    Log.d("TacticalAudio", "📝 Transcripción Exitosa: $transcription")
                    
                    val message = ChatMessageEntity(
                        id = UUID.randomUUID().toString(),
                        senderId = "local_node",
                        senderName = "Yo (Radio)",
                        text = "🎤 [Transcripción de Voz]: $transcription",
                        timestamp = System.currentTimeMillis()
                    )
                    offlineMessageQueue.enqueueMessage(message)
                }
            }
            sessionAudioBuffer = null
            
        } catch (e: Exception) {
            Log.e("TacticalAudio", "Error al detener sesión de audio", e)
        }
    }
}
