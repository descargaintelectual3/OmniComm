package com.example.domain.media

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

/**
 * Grabador de notas de voz y mensajes de audio tácticos mediante MediaRecorder nativo.
 */
class TacticalAudioRecorder(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var currentOutputFile: File? = null
    private var recordingStartTime = 0L
    private var timerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _recordingDurationMs = MutableStateFlow(0L)
    val recordingDurationMs: StateFlow<Long> = _recordingDurationMs.asStateFlow()

    private val _amplitudes = MutableStateFlow<List<Float>>(emptyList())
    val amplitudes: StateFlow<List<Float>> = _amplitudes.asStateFlow()

    /**
     * Inicia la captura de audio por micrófono hacia un archivo temporal .m4a
     */
    fun startRecording(): Result<File> {
        if (_isRecording.value) {
            cancelRecording()
        }

        val cacheDir = context.cacheDir
        val audioFile = File(cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
        currentOutputFile = audioFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(128000)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }

            mediaRecorder = recorder
            recordingStartTime = System.currentTimeMillis()
            _isRecording.value = true
            _recordingDurationMs.value = 0L
            _amplitudes.value = emptyList()

            // Iniciar monitoreo de amplitud y temporizador en tiempo real
            startMonitoring()

            Log.d("TacticalAudioRecorder", "Grabación de audio iniciada: ${audioFile.absolutePath}")
            return Result.success(audioFile)
        } catch (e: Exception) {
            Log.e("TacticalAudioRecorder", "Error iniciando grabación de audio: ${e.message}", e)
            cleanup()
            return Result.failure(e)
        }
    }

    private fun startMonitoring() {
        timerJob?.cancel()
        timerJob = scope.launch {
            val recentAmps = mutableListOf<Float>()
            while (isActive && _isRecording.value) {
                val elapsed = System.currentTimeMillis() - recordingStartTime
                _recordingDurationMs.value = elapsed

                val maxAmp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) {
                    0
                }

                val normalized = (maxAmp / 32767f).coerceIn(0.05f, 1.0f)
                recentAmps.add(normalized)
                if (recentAmps.size > 30) {
                    recentAmps.removeAt(0)
                }
                _amplitudes.value = recentAmps.toList()

                delay(100L)
            }
        }
    }

    /**
     * Finaliza la grabación y retorna el archivo y la duración total en milisegundos
     */
    fun stopRecording(): Pair<File, Long>? {
        if (!_isRecording.value || mediaRecorder == null) {
            return null
        }

        val duration = System.currentTimeMillis() - recordingStartTime
        val file = currentOutputFile

        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.w("TacticalAudioRecorder", "Error deteniendo MediaRecorder: ${e.message}")
        } finally {
            cleanup()
        }

        return if (file != null && file.exists() && file.length() > 0) {
            Pair(file, duration)
        } else {
            null
        }
    }

    /**
     * Cancela la grabación y descarta el archivo
     */
    fun cancelRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignorar error al cancelar
        } finally {
            currentOutputFile?.delete()
            cleanup()
        }
    }

    private fun cleanup() {
        timerJob?.cancel()
        timerJob = null
        mediaRecorder = null
        _isRecording.value = false
        _recordingDurationMs.value = 0L
    }
}
