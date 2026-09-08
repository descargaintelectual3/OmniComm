package com.example.domain.config

enum class ResourceWeight(val label: String, val badgeColorHex: Long) {
    LIGHT("Bajo Consumo", 0xFF69F0AE),
    MEDIUM("Consumo Medio", 0xFFFFD740),
    HEAVY("Alto Impacto", 0xFFFF5252)
}

enum class LiteProfile(
    val title: String,
    val description: String,
    val ramEstimate: String,
    val cpuReduction: String
) {
    FULL_TACTICAL(
        title = "Modo Completo C4ISR (Ultra)",
        description = "Todos los 31 subsistemas, cámara térmica OpenGL, servidor 9090 y audio DSP activos.",
        ramEstimate = "145 MB",
        cpuReduction = "0% (Máxima Capacidad)"
    ),
    BALANCED_LITE(
        title = "Lite Equilibrado (Recomendado)",
        description = "Malla P2P, chat cifrado y radar optimizado; desactiva shaders pesados, video 1080p y servidor REST.",
        ramEstimate = "52 MB",
        cpuReduction = "~45% reducción"
    ),
    EXTREME_SURVIVAL(
        title = "Supervivencia / Batería Extrema",
        description = "Solo chat de texto cifrado y llaves locales; apaga sensores 100Hz, balizas continuas, cámara térmica y animaciones.",
        ramEstimate = "28 MB",
        cpuReduction = "~75% reducción"
    )
}

data class TacticalModuleComponent(
    val id: String,
    val name: String,
    val layerCategory: String,
    val sourceFilePath: String,
    val description: String,
    val resourceWeight: ResourceWeight,
    val estimatedRamKb: Int,
    val affectsBackgroundThreads: Boolean,
    val isEnabled: Boolean
)
