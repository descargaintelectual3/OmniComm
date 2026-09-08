package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.local.entities.ThreadCustomizationEntity
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

data class WallpaperPresetInfo(
    val id: String,
    val name: String,
    val description: String,
    val previewColors: List<Color>,
    val gradientBrush: Brush
)

val WALLPAPER_PRESETS = listOf(
    WallpaperPresetInfo(
        id = "TACTICAL_DARK",
        name = "Carbón Táctico",
        description = "Negro sigiloso militar",
        previewColors = listOf(Color(0xFF0F141C), Color(0xFF1B2230)),
        gradientBrush = Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF161B22)))
    ),
    WallpaperPresetInfo(
        id = "MATRIX_CYBER",
        name = "Cyber Matrix",
        description = "Gris oscuro con reflejos esmeralda",
        previewColors = listOf(Color(0xFF081C15), Color(0xFF1B4332)),
        gradientBrush = Brush.verticalGradient(listOf(Color(0xFF05120D), Color(0xFF0F261C)))
    ),
    WallpaperPresetInfo(
        id = "MIDNIGHT_NEBULA",
        name = "Nebulosa Azul",
        description = "Azul profundo interestelar",
        previewColors = listOf(Color(0xFF0A1128), Color(0xFF1C2541)),
        gradientBrush = Brush.verticalGradient(listOf(Color(0xFF060B1A), Color(0xFF131D36)))
    ),
    WallpaperPresetInfo(
        id = "DESERT_CAMO",
        name = "Camuflaje Desierto",
        description = "Ámbar táctico de operaciones",
        previewColors = listOf(Color(0xFF261C08), Color(0xFF4A3710)),
        gradientBrush = Brush.verticalGradient(listOf(Color(0xFF1A1406), Color(0xFF33250A)))
    ),
    WallpaperPresetInfo(
        id = "MINIMAL_SLATE",
        name = "Titanio Slate",
        description = "Gris minimalista de alta gama",
        previewColors = listOf(Color(0xFF1E242B), Color(0xFF2E3844)),
        gradientBrush = Brush.verticalGradient(listOf(Color(0xFF12161A), Color(0xFF222830)))
    )
)

val BUBBLE_COLOR_PALETTE = listOf(
    "#00E5FF" to "Cian Táctico",
    "#00E676" to "Esmeralda",
    "#FFB300" to "Ámbar Alerta",
    "#7C4DFF" to "Violeta Sigilo",
    "#FF5252" to "Rojo Comando",
    "#2979FF" to "Azul Cobalto",
    "#263238" to "Gris Pizarra"
)

@Composable
fun ThreadCustomizationDialog(
    currentCustomization: ThreadCustomizationEntity?,
    onDismiss: () -> Unit,
    onSaveCustomization: (preset: String, outgoingColor: String?, incomingColor: String?) -> Unit
) {
    var selectedPreset by remember {
        mutableStateOf(currentCustomization?.wallpaperPreset ?: "TACTICAL_DARK")
    }
    var selectedOutgoingColor by remember {
        mutableStateOf(currentCustomization?.outgoingBubbleColorHex ?: "#00E5FF")
    }
    var selectedIncomingColor by remember {
        mutableStateOf(currentCustomization?.incomingBubbleColorHex ?: "#263238")
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("thread_customization_dialog")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Palette,
                        contentDescription = null,
                        tint = TacticalCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Personalizar Fondo y Burbujas",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Fondos de Pantalla (Wallpaper Presets)
                Text(
                    "Fondo del Hilo de Chat",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(WALLPAPER_PRESETS) { preset ->
                        val isSelected = selectedPreset == preset.id
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .width(110.dp)
                                .clickable { selectedPreset = preset.id }
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) TacticalCyanPrimary else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(preset.gradientBrush),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = TacticalCyanPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    preset.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Color de Burbuja Saliente (Mis Mensajes)
                Text(
                    "Color de Mensajes Salientes (Tus Mensajes)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BUBBLE_COLOR_PALETTE) { (hex, name) ->
                        val isSelected = selectedOutgoingColor.equals(hex, ignoreCase = true)
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { TacticalCyanPrimary }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedOutgoingColor = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Color de Burbuja Entrante (Mensajes de Contactos)
                Text(
                    "Color de Mensajes Entrantes",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(BUBBLE_COLOR_PALETTE) { (hex, name) ->
                        val isSelected = selectedIncomingColor.equals(hex, ignoreCase = true)
                        val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (e: Exception) { Color(0xFF263238) }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedIncomingColor = hex }
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) Color.White else Color.Transparent,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones Guardar / Cancelar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSaveCustomization(selectedPreset, selectedOutgoingColor, selectedIncomingColor)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.testTag("btn_save_thread_customization")
                    ) {
                        Text("Guardar en Bóveda", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
