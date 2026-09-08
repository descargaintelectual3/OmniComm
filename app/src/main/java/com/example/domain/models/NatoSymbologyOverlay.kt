package com.example.domain.models

/**
 * FASE 8 (Plan Maestro): Tablero Táctico Mil-Std 2525D & Simbología OTAN APP-6:
 * Estandarización de entidades sobre el mapa táctico (Fuerzas Propias, Enemigas, Neutrales, Desconocidas)
 * con dimensiones de batalla, tamaño de escalón y estado de combate.
 */
enum class NatoAffiliation(val displayName: String, val colorHex: Long) {
    FRIEND("Amigo / Propio (BlueForce)", 0xFF00E5FF),
    HOSTILE("Hostil / Enemigo (RedForce)", 0xFFFF1744),
    NEUTRAL("Neutral (GreenForce)", 0xFF00E676),
    UNKNOWN("Desconocido (YellowForce)", 0xFFFFD600)
}

enum class NatoUnitType(val code: String, val label: String) {
    INFANTRY("INF", "Infantería / Operador"),
    ARMORED("ARM", "Vehículo Blindado"),
    RECON("REC", "Reconocimiento / Scout"),
    UAV("UAV", "Dron / Vehículo No Tripulado"),
    HQ("HQ", "Puesto de Mando"),
    MEDEVAC("MED", "Evacuación Médica"),
    SUPPLY("LOG", "Logística / Suministros")
}

data class NatoTacticalSymbol(
    val id: String,
    val callsign: String,
    val affiliation: NatoAffiliation,
    val unitType: NatoUnitType,
    val latitude: Double,
    val longitude: Double,
    val altitudeMeters: Double = 0.0,
    val speedKmh: Float = 0f,
    val headingDegrees: Float = 0f,
    val remarks: String = ""
)

object NatoSymbologyRegistry {
    val sampleTacticalSymbols = listOf(
        NatoTacticalSymbol(
            id = "SYM-01",
            callsign = "EAGLE-LEADER",
            affiliation = NatoAffiliation.FRIEND,
            unitType = NatoUnitType.HQ,
            latitude = 0.0,
            longitude = 0.0,
            remarks = "Puesto de mando avanzado"
        ),
        NatoTacticalSymbol(
            id = "SYM-02",
            callsign = "VIPER-1",
            affiliation = NatoAffiliation.FRIEND,
            unitType = NatoUnitType.INFANTRY,
            latitude = 0.0012,
            longitude = 0.0008,
            remarks = "Patrulla punto norte"
        ),
        NatoTacticalSymbol(
            id = "SYM-03",
            callsign = "UNKNOWN-UAV-09",
            affiliation = NatoAffiliation.UNKNOWN,
            unitType = NatoUnitType.UAV,
            latitude = -0.0015,
            longitude = 0.0021,
            altitudeMeters = 150.0,
            remarks = "Firma radar no identificada"
        ),
        NatoTacticalSymbol(
            id = "SYM-04",
            callsign = "RED-HOSTILE-VEH",
            affiliation = NatoAffiliation.HOSTILE,
            unitType = NatoUnitType.ARMORED,
            latitude = 0.0028,
            longitude = -0.0019,
            remarks = "Avistamiento visual confirmado"
        )
    )
}
