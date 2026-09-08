package com.example.domain.media

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisionShaderMode(
    val title: String,
    val description: String
) {
    STANDARD("Color Real", "Sin modificaciones ópticas"),
    NIGHT_VISION_PHOSPHOR("Visión Nocturna Verde (NVG)", "Intensificador de luz fósforo verde P43 táctico"),
    THERMAL_IRONBOW("Térmica Simulación Ironbow", "Gradiente de falso color termográfico violeta/naranja/amarillo"),
    FLIR_WHITE_HOT("FLIR White-Hot", "Infrarrojo de alto contraste escala de grises para detección de calor"),
    AMBER_CONTRAST("Filtro Ámbar Antideslumbramiento", "Optimizado para operaciones nocturnas de baja fatiga visual")
}

/**
 * Procesador Óptico de Simulación de Visión Nocturna y Termografía:
 * Genera matrices de color para el visor de la cámara y previsualización de capturas.
 */
object ThermalNightVisionProcessor {

    private val _currentMode = MutableStateFlow(VisionShaderMode.STANDARD)
    val currentMode: StateFlow<VisionShaderMode> = _currentMode.asStateFlow()

    fun setMode(mode: VisionShaderMode) {
        _currentMode.value = mode
    }

    fun getColorMatrix(mode: VisionShaderMode): ColorMatrix {
        val matrix = ColorMatrix()
        when (mode) {
            VisionShaderMode.STANDARD -> {
                matrix.reset()
            }
            VisionShaderMode.NIGHT_VISION_PHOSPHOR -> {
                // Dominancia verde con brillo aumentado y contraste elevado
                matrix.set(
                    floatArrayOf(
                        0.1f, 0.2f, 0.1f, 0f, 0f,
                        0.2f, 1.4f, 0.2f, 0f, 40f,
                        0.05f, 0.1f, 0.05f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            VisionShaderMode.THERMAL_IRONBOW -> {
                // Simulación de paleta térmica Ironbow
                matrix.set(
                    floatArrayOf(
                        1.5f, 0.3f, -0.2f, 0f, 50f,
                        0.2f, 0.8f, 0.8f, 0f, 0f,
                        -0.4f, 0.1f, 1.6f, 0f, 20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
            VisionShaderMode.FLIR_WHITE_HOT -> {
                // Escala de grises con contraste extremo para resaltar siluetas térmicas
                matrix.setSaturation(0f)
                val contrast = 1.8f
                val translate = (-0.5f * contrast + 0.5f) * 255f
                val contrastMatrix = ColorMatrix(
                    floatArrayOf(
                        contrast, 0f, 0f, 0f, translate,
                        0f, contrast, 0f, 0f, translate,
                        0f, 0f, contrast, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                matrix.postConcat(contrastMatrix)
            }
            VisionShaderMode.AMBER_CONTRAST -> {
                // Tono cálido ámbar/naranja de visión nocturna militar
                matrix.set(
                    floatArrayOf(
                        1.3f, 0.3f, 0f, 0f, 30f,
                        0.5f, 0.7f, 0f, 0f, 15f,
                        0.1f, 0.1f, 0.2f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            }
        }
        return matrix
    }
}
