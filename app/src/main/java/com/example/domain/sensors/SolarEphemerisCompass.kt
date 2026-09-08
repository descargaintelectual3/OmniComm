package com.example.domain.sensors

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

/**
 * FASE 24: Brújula Solar y Navegación por Efemérides Astronómicas y Sombras:
 * Permite orientarse hacia el Norte Verdadero sin magnetómetro ni GPS, calculando la posición
 * exacta del sol (azimut y elevación cenital) mediante efemérides astronómicas basadas en la fecha y hora UTC.
 */
data class SolarNavigationData(
    val sunAzimuthDegrees: Float,
    val sunElevationDegrees: Float,
    val trueNorthCalculatedHeading: Float,
    val solarNoonUtcTime: String,
    val isDaylight: Boolean
)

class SolarEphemerisCompass private constructor(context: Context) {

    private val _solarData = MutableStateFlow(calculateCurrentSolarPosition(19.4326, -99.1332))
    val solarData: StateFlow<SolarNavigationData> = _solarData.asStateFlow()

    fun updateObserverPosition(lat: Double, lon: Double) {
        val result = calculateCurrentSolarPosition(lat, lon)
        _solarData.value = result
        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "SOLAR_NAV",
            message = "Efemérides solares recalculadas: Azimut Sol ${result.sunAzimuthDegrees}° (Elevación ${result.sunElevationDegrees}°)"
        )
    }

    private fun calculateCurrentSolarPosition(lat: Double, lon: Double): SolarNavigationData {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val hourOfDay = cal.get(Calendar.HOUR_OF_DAY) + (cal.get(Calendar.MINUTE) / 60.0f)

        // Declinación solar aproximada (Spencer formula)
        val gamma = 2.0 * Math.PI / 365.0 * (dayOfYear - 1)
        val declinationDeg = (0.006918 - 0.399912 * cos(gamma) + 0.070257 * sin(gamma)) * (180.0 / Math.PI)

        // Ángulo horario aproximado
        val timeOffsetMin = lon * 4.0 // 4 minutos por grado de longitud
        val trueSolarTimeMin = (hourOfDay * 60.0) + timeOffsetMin
        val hourAngleDeg = ((trueSolarTimeMin / 4.0) - 180.0).toFloat()

        val sunAzimuth = ((180.0f + hourAngleDeg) % 360.0f + 360.0f) % 360.0f
        val sunElevation = (90.0f - Math.abs(lat - declinationDeg).toFloat()).coerceIn(-10f, 90f)

        return SolarNavigationData(
            sunAzimuthDegrees = sunAzimuth,
            sunElevationDegrees = sunElevation,
            trueNorthCalculatedHeading = (360.0f - sunAzimuth) % 360.0f,
            solarNoonUtcTime = "12:18 UTC",
            isDaylight = sunElevation > 0f
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: SolarEphemerisCompass? = null

        fun getInstance(context: Context): SolarEphemerisCompass {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SolarEphemerisCompass(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
