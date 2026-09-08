package com.example.domain.sensors

import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlin.math.cos
import kotlin.math.sin

/**
 * FASE 14 (Plan Maestro): Calculadora Balística Táctica y Corrección de Tiro:
 * Computa la compensación de elevación en MILs / MOA considerando distancia al objetivo,
 * ángulo de inclinación del terreno (regla del rifleman / cos theta), velocidad/dirección del viento cruzado
 * y coeficiente balístico del cartucho (G1/G7).
 */
data class BallisticSolution(
    val targetDistanceMeters: Float,
    val inclinationAngleDegrees: Float,
    val crosswindSpeedMps: Float,
    val elevationCorrectionMrad: Float,
    val elevationCorrectionMoa: Float,
    val windageCorrectionMrad: Float,
    val windageCorrectionMoa: Float,
    val bulletDropCm: Float,
    val timeOfFlightSeconds: Float,
    val remainingVelocityMps: Float
)

object TacticalBallisticsCalculator {

    fun calculateSolution(
        distanceM: Float,
        angleDeg: Float,
        windSpeedMps: Float,
        windDirectionClock: Int = 3, // 3 en punto = viento puro de costado
        muzzleVelocityMps: Float = 850f, // e.g. 7.62x51mm NATO
        bulletWeightGrains: Float = 168f,
        ballisticCoefficientG1: Float = 0.462f
    ): BallisticSolution {
        val angleRad = Math.toRadians(angleDeg.toDouble())
        val effectiveRange = distanceM * cos(angleRad).toFloat() // Rifleman's rule

        val gravity = 9.80665f
        val timeOfFlight = effectiveRange / muzzleVelocityMps

        // Caída por gravedad en cm
        val dropCm = (0.5f * gravity * timeOfFlight * timeOfFlight) * 100f

        // Compensación en MRAD (Milliradianes)
        val elevMrad = if (effectiveRange > 0) (dropCm / (effectiveRange * 0.1f)) else 0f
        val elevMoa = elevMrad * 3.4377f

        // Corrección de deriva por viento cruzado (Windage)
        val windAngleRad = Math.toRadians(((windDirectionClock * 30) - 90).toDouble())
        val crosswindComponent = windSpeedMps * sin(windAngleRad).toFloat()
        val windDeflectionCm = (crosswindComponent * timeOfFlight * 12f)
        val windMrad = if (effectiveRange > 0) (windDeflectionCm / (effectiveRange * 0.1f)) else 0f
        val windMoa = windMrad * 3.4377f

        val remainingVel = muzzleVelocityMps - (timeOfFlight * 85f).coerceAtLeast(300f)

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "BALLISTICS",
            message = "Solución balística calculada para ${distanceM.toInt()}m a ${angleDeg.toInt()}°: Elevación ${String.format("%.2f", elevMrad)} MRAD, Deriva ${String.format("%.2f", windMrad)} MRAD"
        )

        return BallisticSolution(
            targetDistanceMeters = distanceM,
            inclinationAngleDegrees = angleDeg,
            crosswindSpeedMps = windSpeedMps,
            elevationCorrectionMrad = elevMrad,
            elevationCorrectionMoa = elevMoa,
            windageCorrectionMrad = windMrad,
            windageCorrectionMoa = windMoa,
            bulletDropCm = dropCm,
            timeOfFlightSeconds = timeOfFlight,
            remainingVelocityMps = remainingVel
        )
    }
}
