package com.example.domain.media

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
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

/**
 * Reproductor de Notas de Voz Tácticas mediante MediaPlayer nativo de Android.
 */
class TacticalAudioPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _currentPlayingMessageId = MutableStateFlow<String?>(null)
    val currentPlayingMessageId: StateFlow<String?> = _currentPlayingMessageId.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _totalDurationMs = MutableStateFlow(0L)
    val totalDurationMs: StateFlow<Long> = _totalDurationMs.asStateFlow()

    /**
     * Alterna la reproducción del mensaje de audio (Play / Pause)
     */
    fun togglePlay(messageId: String, audioSource: String, localFilePath: String = "") {
        if (_currentPlayingMessageId.value == messageId) {
            if (_isPlaying.value) {
                pause()
            } else {
                resume()
            }
        } else {
            play(messageId, audioSource, localFilePath)
        }
    }

    /**
     * Inicia la reproducción desde una URL de Firebase Storage o archivo local en caché
     */
    fun play(messageId: String, audioSource: String, localFilePath: String = "") {
        stop()

        try {
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )

                val localFile = if (localFilePath.isNotBlank()) File(localFilePath) else null
                if (localFile != null && localFile.exists() && localFile.length() > 0) {
                    setDataSource(localFile.absolutePath)
                } else if (audioSource.startsWith("http://") || audioSource.startsWith("https://")) {
                    setDataSource(audioSource)
                } else if (audioSource.startsWith("content://") || audioSource.startsWith("file://")) {
                    setDataSource(context, Uri.parse(audioSource))
                } else {
                    setDataSource(audioSource)
                }

                setOnPreparedListener { mp ->
                    mp.start()
                    _isPlaying.value = true
                    _currentPlayingMessageId.value = messageId
                    _totalDurationMs.value = mp.duration.toLong().coerceAtLeast(1000L)
                    startProgressTracker()
                }

                setOnCompletionListener {
                    stop()
                }

                setOnErrorListener { _, what, extra ->
                    Log.e("TacticalAudioPlayer", "Error en MediaPlayer: what=$what, extra=$extra")
                    stop()
                    true
                }

                prepareAsync()
            }

            mediaPlayer = player
            _currentPlayingMessageId.value = messageId
        } catch (e: Exception) {
            Log.e("TacticalAudioPlayer", "Error reproduciendo audio: ${e.message}", e)
            stop()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
        progressJob?.cancel()
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracker()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            it.seekTo(positionMs.toInt())
            _currentPositionMs.value = positionMs
        }
    }

    fun stop() {
        progressJob?.cancel()
        progressJob = null
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            // Ignorar error al liberar
        } finally {
            mediaPlayer = null
            _isPlaying.value = false
            _currentPlayingMessageId.value = null
            _currentPositionMs.value = 0L
            _totalDurationMs.value = 0L
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { mp ->
                    if (mp.isPlaying) {
                        _currentPositionMs.value = mp.currentPosition.toLong()
                    }
                }
                delay(100L)
            }
        }
    }
}
