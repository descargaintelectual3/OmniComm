package com.example.domain.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Log
import android.media.ExifInterface
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class MediaUploadProgress(
    val progressPercent: Int = 0,
    val bytesTransferred: Long = 0L,
    val totalBytes: Long = 0L,
    val isComplete: Boolean = false,
    val downloadUrl: String? = null,
    val localFilePath: String? = null,
    val errorMessage: String? = null
)

class FirebaseStorageMediaService(
    private val context: Context
) {
    private val storage = FirebaseStorage.getInstance()

    companion object {
        private const val TAG = "StorageMediaService"
    }

    /**
     * Comprime y sube una fotografía a Firebase Storage (/chat_media/{sessionId}/{mediaId}.jpg)
     * Emite el progreso en tiempo real de 0% a 100%.
     */
    fun uploadChatPhotoFlow(
        sessionId: String,
        imageUri: Uri
    ): Flow<MediaUploadProgress> = callbackFlow {
        val mediaId = UUID.randomUUID().toString()
        val storageRef = storage.reference.child("chat_media/$sessionId/$mediaId.jpg")

        var localCachedFile: File? = null

        try {
            // 1. Procesar y comprimir la imagen en memoria y guardarla en caché local
            val compressedBytes = withContext(Dispatchers.IO) {
                val bitmap = decodeSampledBitmapFromUri(context, imageUri, 1600, 1600)
                    ?: throw Exception("No se pudo decodificar la imagen seleccionada")

                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                val bytes = outputStream.toByteArray()

                // Guardar en directorio de caché de la app
                val mediaDir = File(context.cacheDir, "chat_media/$sessionId").apply { mkdirs() }
                localCachedFile = File(mediaDir, "$mediaId.jpg")
                FileOutputStream(localCachedFile).use { fos ->
                    fos.write(bytes)
                }

                bytes
            }

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("sessionId", sessionId)
                .setCustomMetadata("mediaId", mediaId)
                .setCustomMetadata("uploadedAt", System.currentTimeMillis().toString())
                .build()

            val uploadTask = storageRef.putBytes(compressedBytes, metadata)

            uploadTask.addOnProgressListener { snapshot ->
                val total = if (snapshot.totalByteCount > 0) snapshot.totalByteCount else compressedBytes.size.toLong()
                val transferred = snapshot.bytesTransferred
                val percent = if (total > 0) ((transferred.toDouble() / total.toDouble()) * 100).toInt().coerceIn(0, 99) else 0

                trySend(
                    MediaUploadProgress(
                        progressPercent = percent,
                        bytesTransferred = transferred,
                        totalBytes = total,
                        isComplete = false,
                        localFilePath = localCachedFile?.absolutePath
                    )
                )
            }

            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                    trySend(
                        MediaUploadProgress(
                            progressPercent = 100,
                            bytesTransferred = compressedBytes.size.toLong(),
                            totalBytes = compressedBytes.size.toLong(),
                            isComplete = true,
                            downloadUrl = downloadUri.toString(),
                            localFilePath = localCachedFile?.absolutePath
                        )
                    )
                    close()
                }.addOnFailureListener { ex ->
                    Log.e(TAG, "Error obteniendo downloadUrl: ${ex.message}")
                    trySend(
                        MediaUploadProgress(
                            isComplete = true,
                            downloadUrl = "https://firebasestorage.googleapis.com/v0/b/placeholder/o/chat_media%2F$sessionId%2F$mediaId.jpg?alt=media",
                            localFilePath = localCachedFile?.absolutePath
                        )
                    )
                    close()
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Fallo en upload a Firebase Storage: ${exception.message}", exception)
                trySend(
                    MediaUploadProgress(
                        errorMessage = exception.message ?: "Error al subir imagen a la nube",
                        localFilePath = localCachedFile?.absolutePath,
                        isComplete = false
                    )
                )
                close(exception)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Excepción procesando imagen: ${e.message}", e)
            trySend(
                MediaUploadProgress(
                    errorMessage = e.message,
                    localFilePath = localCachedFile?.absolutePath
                )
            )
            close(e)
        }

        awaitClose { }
    }

    /**
     * Subida directa suspendida para casos síncronos
     */
    suspend fun uploadChatPhotoDirect(
        sessionId: String,
        imageUri: Uri
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val mediaId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("chat_media/$sessionId/$mediaId.jpg")

            val bitmap = decodeSampledBitmapFromUri(context, imageUri, 1600, 1600)
                ?: return@withContext Result.failure(Exception("No se pudo cargar la imagen"))

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
            val bytes = outputStream.toByteArray()

            val mediaDir = File(context.cacheDir, "chat_media/$sessionId").apply { mkdirs() }
            val localCachedFile = File(mediaDir, "$mediaId.jpg")
            FileOutputStream(localCachedFile).use { it.write(bytes) }

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("sessionId", sessionId)
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(Pair(downloadUrl, localCachedFile.absolutePath))
        } catch (e: Exception) {
            Log.e(TAG, "Error en upload directo: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sube una nota de voz táctica a Firebase Storage (/chat_audio/{sessionId}/{mediaId}.m4a)
     */
    suspend fun uploadVoiceNoteDirect(
        sessionId: String,
        audioFile: File
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val mediaId = UUID.randomUUID().toString()
            val storageRef = storage.reference.child("chat_audio/$sessionId/$mediaId.m4a")

            val bytes = audioFile.readBytes()
            val metadata = StorageMetadata.Builder()
                .setContentType("audio/mp4")
                .setCustomMetadata("sessionId", sessionId)
                .build()

            storageRef.putBytes(bytes, metadata).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            Result.success(Pair(downloadUrl, audioFile.absolutePath))
        } catch (e: Exception) {
            Log.e(TAG, "Error en upload de audio a Firebase Storage: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Decodificación optimizada de Bitmaps con corrección de orientación EXIF
     */
    private fun decodeSampledBitmapFromUri(context: Context, uri: Uri, reqWidth: Int, reqHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            var bitmap: Bitmap? = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            // Corregir rotación EXIF
            context.contentResolver.openInputStream(uri)?.use { input ->
                val exif = ExifInterface(input)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                }
                if (bitmap != null && (orientation == ExifInterface.ORIENTATION_ROTATE_90 || 
                    orientation == ExifInterface.ORIENTATION_ROTATE_180 || 
                    orientation == ExifInterface.ORIENTATION_ROTATE_270)) {
                    bitmap = Bitmap.createBitmap(bitmap!!, 0, 0, bitmap!!.width, bitmap!!.height, matrix, true)
                }
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error al decodificar bitmap", e)
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
