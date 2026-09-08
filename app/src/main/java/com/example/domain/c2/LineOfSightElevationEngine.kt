package com.example.domain.c2

import android.content.Context
import kotlin.math.*

data class LosProfilePoint(
    val distanceMeters: Double,
    val groundElevationMeters: Double,
    val lineOfSightElevationMeters: Double,
    val isObstructed: Boolean
)

data class LosCalculationResult(
    val isDirectLosClear: Boolean,
    val obstructionDistanceMeters: Double?,
    val maxClearanceMeters: Double,
    val fresnelZoneClearancePercent: Double,
    val distanceTotalMeters: Double,
    val profilePoints: List<LosProfilePoint>
)

/**
 * Motor de Intervisibilidad Táctica y Cálculo de Línea de Visión (LOS / NLOS / Fresnel 1st Zone).
 * Permite a operadores en terreno evaluar si un enlace RF o una línea de tiro/visión óptica
 * está bloqueada por el relieve orográfico o edificios.
 */
class LineOfSightElevationEngine private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LineOfSightElevationEngine? = null

        fun getInstance(context: Context): LineOfSightElevationEngine {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LineOfSightElevationEngine(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Calcula la línea de visión e intervisibilidad entre dos coordenadas dadas las alturas de antena.
     */
    fun calculateLos(
        originLat: Double,
        originLon: Double,
        originAntennaHeightMeters: Double = 2.0,
        targetLat: Double,
        targetLon: Double,
        targetAntennaHeightMeters: Double = 2.0,
        samples: Int = 30
    ): LosCalculationResult {
        val totalDistance = calculateHaversineDistance(originLat, originLon, targetLat, targetLon)

        val originBaseAlt = getSyntheticGroundElevation(originLat, originLon)
        val targetBaseAlt = getSyntheticGroundElevation(targetLat, targetLon)

        val originEffectiveAlt = originBaseAlt + originAntennaHeightMeters
        val targetEffectiveAlt = targetBaseAlt + targetAntennaHeightMeters

        val points = mutableListOf<LosProfilePoint>()
        var isObstructed = false
        var obstructionDist: Double? = null
        var minFresnelRatio = 1.0

        for (i in 0..samples) {
            val fraction = i.toDouble() / samples
            val currentDist = totalDistance * fraction
            val currentLat = originLat + fraction * (targetLat - originLat)
            val currentLon = originLon + fraction * (targetLon - originLon)

            val groundAlt = getSyntheticGroundElevation(currentLat, currentLon)
            // Altura del rayo LOS recto considerando la curvatura terrestre
            val earthCurvatureDrop = (currentDist * (totalDistance - currentDist)) / (2.0 * 6371000.0)
            val losRayAlt = (originEffectiveAlt + fraction * (targetEffectiveAlt - originEffectiveAlt)) - earthCurvatureDrop

            // Radio de la primera zona de Fresnel a 2.4 GHz (lambda ~ 0.125m)
            val lambda = 0.125
            val d1 = currentDist.coerceAtLeast(1.0)
            val d2 = (totalDistance - currentDist).coerceAtLeast(1.0)
            val fresnelRadius = sqrt((lambda * d1 * d2) / totalDistance)

            val clearance = losRayAlt - groundAlt
            val pointObstructed = clearance < 0.0

            if (pointObstructed && !isObstructed) {
                isObstructed = true
                obstructionDist = currentDist
            }

            if (fresnelRadius > 0.0) {
                val ratio = clearance / fresnelRadius
                if (ratio < minFresnelRatio) {
                    minFresnelRatio = ratio
                }
            }

            points.add(
                LosProfilePoint(
                    distanceMeters = currentDist,
                    groundElevationMeters = groundAlt,
                    lineOfSightElevationMeters = losRayAlt,
                    isObstructed = pointObstructed
                )
            )
        }

        return LosCalculationResult(
            isDirectLosClear = !isObstructed,
            obstructionDistanceMeters = obstructionDist,
            maxClearanceMeters = points.maxOfOrNull { it.lineOfSightElevationMeters - it.groundElevationMeters } ?: 0.0,
            fresnelZoneClearancePercent = (minFresnelRatio * 100.0).coerceIn(0.0, 100.0),
            distanceTotalMeters = totalDistance,
            profilePoints = points
        )
    }

    private fun getSyntheticGroundElevation(lat: Double, lon: Double): Double {
        // Modelo DEM de elevación táctico sintético basado en topografía andina (2600m base)
        val wave1 = sin(lat * 800.0) * 45.0
        val wave2 = cos(lon * 800.0) * 60.0
        val hill = exp(-(((lat - 4.610) * 200).pow(2) + ((lon - (-74.080)) * 200).pow(2))) * 180.0
        return 2600.0 + wave1 + wave2 + hill
    }

    private fun calculateHaversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
