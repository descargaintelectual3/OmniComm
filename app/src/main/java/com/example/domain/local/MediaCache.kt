package com.example.domain.local

import android.content.Context
import java.io.File

class MediaCache(private val context: Context) {
    private val cacheDirectory: File = context.cacheDir

    fun saveToCache(fileName: String, data: ByteArray) {
        val file = File(cacheDirectory, fileName)
        file.writeBytes(data)
    }

    fun getFromCache(fileName: String): ByteArray? {
        val file = File(cacheDirectory, fileName)
        return if (file.exists()) file.readBytes() else null
    }

    fun clearCache() {
        cacheDirectory.listFiles()?.forEach { it.delete() }
    }
    
    fun getCacheSizeMB(): Float {
        var size = 0L
        cacheDirectory.listFiles()?.forEach { size += it.length() }
        return size / (1024f * 1024f)
    }
}
