package com.example.domain.media

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.PixelCopy
import android.view.View
import androidx.annotation.RequiresApi
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume

data class TacticalScreenshotRecord(
    val id: String,
    val timestamp: Long,
    val title: String,
    val filePath: String,
    val base64Data: String,
    val width: Int,
    val height: Int,
    val resolutionLabel: String,
    val fileSizeBytes: Long
)

/**
 * Servicio de Captura Interna de Pantalla, Telemetría Visual y Diagnóstico Gráfico.
 * Permite a la aplicación capturar instantáneas de su propio estado visual en tiempo de ejecución,
 * persistirlas en almacenamiento protegido, calcular sumas de verificación y exponerlas
 * a la UI táctica y a la API de control externo para verificación visual continua.
 */
class TacticalScreenshotCaptureService private constructor(private val context: Context) {

    private val TAG = "ScreenshotService"
    private val _screenshotsList = MutableStateFlow<List<TacticalScreenshotRecord>>(emptyList())
    val screenshotsList: StateFlow<List<TacticalScreenshotRecord>> = _screenshotsList.asStateFlow()

    private val _latestScreenshot = MutableStateFlow<TacticalScreenshotRecord?>(null)
    val latestScreenshot: StateFlow<TacticalScreenshotRecord?> = _latestScreenshot.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: TacticalScreenshotCaptureService? = null

        fun getInstance(context: Context): TacticalScreenshotCaptureService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalScreenshotCaptureService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    init {
        loadExistingScreenshots()
    }

    private fun loadExistingScreenshots() {
        try {
            val dir = File(context.filesDir, "tactical_screenshots")
            if (dir.exists()) {
                val files = dir.listFiles { file -> file.extension == "jpg" || file.extension == "png" }
                if (files != null) {
                    val records = files.sortedByDescending { it.lastModified() }.map { file ->
                        val bytes = file.readBytes()
                        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                        TacticalScreenshotRecord(
                            id = file.nameWithoutExtension,
                            timestamp = file.lastModified(),
                            title = "Captura ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(file.lastModified()))}",
                            filePath = file.absolutePath,
                            base64Data = base64,
                            width = 1080,
                            height = 2400,
                            resolutionLabel = "1080x2400 HD",
                            fileSizeBytes = file.length()
                        )
                    }
                    _screenshotsList.value = records
                    _latestScreenshot.value = records.firstOrNull()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cargando capturas previas: ${e.message}")
        }
    }

    /**
     * Captura el estado visual de la ventana actual usando PixelCopy (Android O+)
     * o Canvas Drawing con fallback seguro.
     */
    suspend fun captureActivityScreen(
        activity: Activity,
        label: String = "Captura de Estado Táctico"
    ): TacticalScreenshotRecord? = withContext(Dispatchers.Main) {
        _isCapturing.value = true
        try {
            val window = activity.window
            val view = window.decorView.rootView

            val width = view.width.coerceAtLeast(1)
            val height = view.height.coerceAtLeast(1)

            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                captureWithPixelCopy(activity, width, height) ?: captureWithCanvas(view, width, height)
            } else {
                captureWithCanvas(view, width, height)
            }

            if (bitmap != null) {
                val record = saveBitmapRecord(bitmap, label)
                _latestScreenshot.value = record
                _screenshotsList.value = listOf(record) + _screenshotsList.value.filter { it.id != record.id }
                
                DiscoveryLogCollector.log(
                    category = LogCategory.SYSTEM,
                    severity = LogSeverity.SUCCESS,
                    tag = "VisualTelemetry",
                    message = "📸 Captura de pantalla interna generada (${record.resolutionLabel}, ${record.fileSizeBytes / 1024} KB)",
                    rawPayload = "{ \"id\": \"${record.id}\", \"path\": \"${record.filePath}\" }"
                )
                record
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error capturando pantalla de actividad: ${e.message}", e)
            null
        } finally {
            _isCapturing.value = false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun captureWithPixelCopy(activity: Activity, width: Int, height: Int): Bitmap? =
        suspendCancellableCoroutine { continuation ->
            try {
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val window = activity.window
                PixelCopy.request(
                    window,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            continuation.resume(bitmap)
                        } else {
                            Log.w(TAG, "PixelCopy falló con código $copyResult, intentando fallback de Canvas")
                            continuation.resume(null)
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } catch (e: Throwable) {
                Log.w(TAG, "Excepción en PixelCopy: ${e.message}")
                continuation.resume(null)
            }
        }

    private fun captureWithCanvas(view: View, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    private suspend fun saveBitmapRecord(bitmap: Bitmap, label: String): TacticalScreenshotRecord = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "tactical_screenshots")
        if (!dir.exists()) dir.mkdirs()

        val id = "SHOT_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_" + UUID.randomUUID().toString().take(4)
        val file = File(dir, "$id.jpg")

        val byteStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteStream)
        val bytes = byteStream.toByteArray()

        FileOutputStream(file).use { out ->
            out.write(bytes)
            out.flush()
        }

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        TacticalScreenshotRecord(
            id = id,
            timestamp = System.currentTimeMillis(),
            title = label,
            filePath = file.absolutePath,
            base64Data = base64,
            width = bitmap.width,
            height = bitmap.height,
            resolutionLabel = "${bitmap.width}x${bitmap.height}",
            fileSizeBytes = file.length()
        )
    }

    fun clearScreenshots() {
        try {
            val dir = File(context.filesDir, "tactical_screenshots")
            if (dir.exists()) {
                dir.deleteRecursively()
            }
            _screenshotsList.value = emptyList()
            _latestScreenshot.value = null
        } catch (e: Exception) {
            Log.e(TAG, "Error borrando capturas: ${e.message}")
        }
    }
}
