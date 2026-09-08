package com.example.domain.security

import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import java.security.SecureRandom

/**
 * FASE 2: Bóveda Criptográfica Shamir Secret Sharing (K-de-N):
 * Permite dividir una clave maestra de cifrado de misión en N partes (fragmentos).
 * Se requiere un quórum de al menos K partes para reconstruir el secreto original,
 * garantizando que ningún operador individual pueda comprometer la red por sí solo.
 */
data class ShamirShare(
    val index: Int,
    val shareDataHex: String
)

object ShamirSecretSharingVault {

    private val random = SecureRandom()
    private const val PRIME_MOD = 257 // Aritmética modular simple para bytes [0..255]

    /**
     * Divide un secreto en N partes con un umbral K requerido.
     */
    fun splitSecret(secretBytes: ByteArray, totalPartsN: Int, thresholdK: Int): List<ShamirShare> {
        require(thresholdK in 2..totalPartsN) { "El umbral K debe ser >= 2 y <= N" }
        require(totalPartsN <= 16) { "Máximo 16 operadores soportados" }

        val shares = mutableListOf<StringBuilder>()
        for (i in 0 until totalPartsN) {
            shares.add(StringBuilder())
        }

        // Para cada byte del secreto, generamos un polinomio de grado K-1
        for (b in secretBytes) {
            val secretVal = b.toInt() and 0xFF
            val coefficients = IntArray(thresholdK)
            coefficients[0] = secretVal

            for (c in 1 until thresholdK) {
                coefficients[c] = random.nextInt(PRIME_MOD)
            }

            // Evaluar polinomio en x = 1..N
            for (x in 1..totalPartsN) {
                var y = 0
                var xPow = 1
                for (c in 0 until thresholdK) {
                    y = (y + coefficients[c] * xPow) % PRIME_MOD
                    xPow = (xPow * x) % PRIME_MOD
                }
                shares[x - 1].append(String.format("%04X", y))
            }
        }

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SHAMIR_VAULT",
            message = "Clave secreta fragmentada en $totalPartsN partes (Umbral Quórum: $thresholdK)"
        )

        return shares.mapIndexed { idx, sb ->
            ShamirShare(index = idx + 1, shareDataHex = sb.toString())
        }
    }

    /**
     * Reconstruye el secreto original a partir de un conjunto de al menos K fragmentos
     * utilizando interpolación de Lagrange en x = 0.
     */
    fun reconstructSecret(shares: List<ShamirShare>, thresholdK: Int): ByteArray? {
        if (shares.size < thresholdK) return null

        try {
            val selectedShares = shares.take(thresholdK)
            val shareHexList = selectedShares.map { it.shareDataHex }
            val len = shareHexList[0].length / 4

            val resultBytes = ByteArray(len)

            for (byteIdx in 0 until len) {
                val xs = selectedShares.map { it.index }
                val ys = selectedShares.map {
                    val hexChunk = it.shareDataHex.substring(byteIdx * 4, byteIdx * 4 + 4)
                    hexChunk.toInt(16)
                }

                // Interpolación de Lagrange en x = 0
                var reconstructedByte = 0
                for (i in 0 until thresholdK) {
                    var num = 1
                    var den = 1
                    for (j in 0 until thresholdK) {
                        if (i != j) {
                            num = (num * (-xs[j])) % PRIME_MOD
                            den = (den * (xs[i] - xs[j])) % PRIME_MOD
                        }
                    }

                    while (num < 0) num += PRIME_MOD
                    while (den < 0) den += PRIME_MOD

                    val invDen = modInverse(den, PRIME_MOD)
                    val term = (ys[i] * num % PRIME_MOD) * invDen % PRIME_MOD
                    reconstructedByte = (reconstructedByte + term) % PRIME_MOD
                }

                while (reconstructedByte < 0) reconstructedByte += PRIME_MOD
                resultBytes[byteIdx] = (reconstructedByte and 0xFF).toByte()
            }

            return resultBytes
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun modInverse(n: Int, prime: Int): Int {
        var t = 0
        var newt = 1
        var r = prime
        var newr = n % prime

        while (newr != 0) {
            val quotient = r / newr
            val tempT = t - quotient * newt
            t = newt
            newt = tempT

            val tempR = r - quotient * newr
            r = newr
            newr = tempR
        }

        if (t < 0) t += prime
        return t
    }
}
