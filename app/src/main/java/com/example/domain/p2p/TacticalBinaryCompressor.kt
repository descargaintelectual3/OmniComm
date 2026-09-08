package com.example.domain.p2p

import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.Deflater
import java.util.zip.Inflater

/**
 * FASE 10 (Plan Maestro): Compresor Binario Ultradenso de Ancho de Banda Mínimo:
 * Reduce el payload de mensajes de texto, telemetría y coordenadas GPS hasta en un 70%
 * para permitir transmisión ultrarrápida a través de canales de baja tasa de datos (< 1200 bps).
 */
data class CompressionResult(
    val originalSizeBytes: Int,
    val compressedSizeBytes: Int,
    val compressionRatioPercentage: Float,
    val compressedHex: String
)

object TacticalBinaryCompressor {

    fun compressPayload(rawText: String): CompressionResult {
        val inputBytes = rawText.toByteArray(Charsets.UTF_8)
        if (inputBytes.isEmpty()) {
            return CompressionResult(0, 0, 0f, "")
        }

        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(inputBytes)
        deflater.finish()

        val outputStream = ByteArrayOutputStream(inputBytes.size)
        val buffer = ByteArray(1024)
        while (!deflater.finished()) {
            val count = deflater.deflate(buffer)
            outputStream.write(buffer, 0, count)
        }
        deflater.end()

        val compressedBytes = outputStream.toByteArray()
        val ratio = if (inputBytes.isNotEmpty()) {
            (1.0f - (compressedBytes.size.toFloat() / inputBytes.size.toFloat())) * 100f
        } else 0f

        val hexBuilder = StringBuilder()
        for (b in compressedBytes) {
            hexBuilder.append(String.format("%02X", b))
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "COMPRESSOR",
            message = "Payload comprimido: ${inputBytes.size}B -> ${compressedBytes.size}B (${ratio.toInt()}% reducción)"
        )

        return CompressionResult(
            originalSizeBytes = inputBytes.size,
            compressedSizeBytes = compressedBytes.size,
            compressionRatioPercentage = ratio.coerceAtLeast(0f),
            compressedHex = hexBuilder.toString()
        )
    }

    fun decompressPayload(compressedBytes: ByteArray): String? {
        return try {
            val inflater = Inflater(true)
            inflater.setInput(compressedBytes)
            val outputStream = ByteArrayOutputStream(compressedBytes.size * 2)
            val buffer = ByteArray(1024)
            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                outputStream.write(buffer, 0, count)
            }
            inflater.end()
            outputStream.toString(Charsets.UTF_8.name())
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
