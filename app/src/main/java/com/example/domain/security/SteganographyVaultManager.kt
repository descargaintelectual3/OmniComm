package com.example.domain.security

import android.graphics.Bitmap
import android.graphics.Color
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

/**
 * Bóveda Esteganográfica Táctica:
 * Permite incrustar y extraer mensajes cifrados dentro de los bits menos significativos (LSB)
 * de imágenes fotográficas o generar secuencias binarias para portadoras acústicas.
 */
object SteganographyVaultManager {

    private const val STEGO_MAGIC_HEADER = "OMNI_STEGO_V1:"

    suspend fun embedSecretMessage(bitmap: Bitmap, secretMessage: String): Bitmap = withContext(Dispatchers.Default) {
        val payload = (STEGO_MAGIC_HEADER + secretMessage).toByteArray(StandardCharsets.UTF_8)
        val payloadLength = payload.size

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val width = mutableBitmap.width
        val height = mutableBitmap.height
        val totalPixels = width * height

        // Requerimos al menos (payloadLength + 4 bytes de longitud) * 8 píxeles
        val totalBitsNeeded = (payloadLength + 4) * 8
        if (totalPixels < totalBitsNeeded) {
            throw IllegalArgumentException("La imagen es demasiado pequeña para el tamaño del mensaje ($totalBitsNeeded bits necesarios)")
        }

        val binaryData = StringBuilder()
        // Primeros 32 bits: tamaño del payload
        val lengthBits = String.format("%32s", Integer.toBinaryString(payloadLength)).replace(' ', '0')
        binaryData.append(lengthBits)

        // Bits del payload
        for (byte in payload) {
            val byteBits = String.format("%8s", Integer.toBinaryString(byte.toInt() and 0xFF)).replace(' ', '0')
            binaryData.append(byteBits)
        }

        var bitIndex = 0
        val totalBits = binaryData.length

        pixelLoop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if (bitIndex >= totalBits) break@pixelLoop

                val pixel = mutableBitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                var red = Color.red(pixel)
                var green = Color.green(pixel)
                val blue = Color.blue(pixel)

                // Modificar el LSB del canal Rojo
                val bitToEmbed = binaryData[bitIndex] - '0'
                red = (red and 0xFE) or bitToEmbed
                bitIndex++

                // Modificar el LSB del canal Verde si aún hay bits
                if (bitIndex < totalBits) {
                    val secondBit = binaryData[bitIndex] - '0'
                    green = (green and 0xFE) or secondBit
                    bitIndex++
                }

                mutableBitmap.setPixel(x, y, Color.argb(alpha, red, green, blue))
            }
        }

        DiscoveryLogCollector.log(
            category = LogCategory.AUTH_HANDSHAKE,
            severity = LogSeverity.INFO,
            tag = "STEGO_EMBED",
            message = "Mensaje secreto (${payload.size} bytes) ocultado exitosamente en la matriz de la imagen."
        )

        return@withContext mutableBitmap
    }

    suspend fun extractSecretMessage(bitmap: Bitmap): String? = withContext(Dispatchers.Default) {
        try {
            val width = bitmap.width
            val height = bitmap.height

            val bits = StringBuilder()
            var lengthExtracted: Int? = null
            var expectedTotalBits: Int? = null

            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = bitmap.getPixel(x, y)
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)

                    bits.append((red and 1).toString())

                    if (lengthExtracted == null && bits.length >= 32) {
                        val lenString = bits.substring(0, 32)
                        lengthExtracted = lenString.toInt(2)
                        if (lengthExtracted <= 0 || lengthExtracted > 50000) {
                            return@withContext null // No contiene payload válido
                        }
                        expectedTotalBits = (lengthExtracted + 4) * 8
                    }

                    bits.append((green and 1).toString())

                    if (expectedTotalBits != null && bits.length >= expectedTotalBits) {
                        // Extraer bytes
                        val payloadBytes = ByteArray(lengthExtracted!!)
                        for (i in 0 until lengthExtracted) {
                            val byteStart = (4 + i) * 8
                            val byteStr = bits.substring(byteStart, byteStart + 8)
                            payloadBytes[i] = byteStr.toInt(2).toByte()
                        }

                        val result = String(payloadBytes, StandardCharsets.UTF_8)
                        if (result.startsWith(STEGO_MAGIC_HEADER)) {
                            DiscoveryLogCollector.log(
                                category = LogCategory.AUTH_HANDSHAKE,
                                severity = LogSeverity.INFO,
                                tag = "STEGO_EXTRACT",
                                message = "Payload esteganográfico recuperado y verificado con éxito."
                            )
                            return@withContext result.removePrefix(STEGO_MAGIC_HEADER)
                        } else {
                            return@withContext null
                        }
                    }
                }
            }
            return@withContext null
        } catch (e: Exception) {
            return@withContext null
        }
    }
}
