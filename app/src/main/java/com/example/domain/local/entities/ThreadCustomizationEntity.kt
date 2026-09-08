package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de persistencia en Room para personalización visual por hilo de chat táctico.
 * Permite configurar fondo de pantalla y colores de burbujas personalizados.
 */
@Entity(tableName = "thread_customizations")
data class ThreadCustomizationEntity(
    @PrimaryKey val sessionId: String,
    val wallpaperPreset: String = "TACTICAL_DEFAULT", // "TACTICAL_DEFAULT", "CYBER_EMERALD", "AMBER_RADAR", "CARBON_STEALTH", "NAVY_GRID", "CUSTOM_SOLID"
    val customWallpaperColorHex: String? = null,
    val outgoingBubbleColorHex: String? = null, // e.g. "#00E5FF", "#00E676", "#FF9100", "#D500F9"
    val incomingBubbleColorHex: String? = null, // e.g. "#263238", "#1B2A32", "#37474F"
    val outgoingTextColorHex: String? = null,
    val incomingTextColorHex: String? = null,
    val enableSoundEffects: Boolean = true,
    val updatedAtTimestamp: Long = System.currentTimeMillis()
)
