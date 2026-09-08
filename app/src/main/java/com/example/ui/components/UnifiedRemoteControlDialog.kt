package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.NetworkNode
import com.example.domain.models.RemoteActionType

/**
 * 🎮 Centro de Control Remoto Universal Omnidireccional:
 * Permite usar y controlar completamente cualquier nodo como si fuera uno solo,
 * incluso si el teléfono tiene la pantalla estrellada o rota con solo encenderlo.
 */
@Composable
fun UnifiedRemoteControlDialog(
    node: NetworkNode,
    onDismiss: () -> Unit,
    onExecuteAction: (RemoteActionType, String) -> Unit,
    onOpenScreenMirror: (NetworkNode) -> Unit = {}
) {
    var ttsMessage by remember { mutableStateOf("") }
    var lastExecutedAction by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0F172A),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .testTag("unified_remote_control_dialog"),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFE11D48).copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Cabecera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE11D48).copy(alpha = 0.2f),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(node.avatarIcon, fontSize = 22.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Control Remoto Universal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text(node.name, fontSize = 12.sp, color = Color(0xFFFDA4AF), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Botón Primario: Espejo de Pantalla en Vivo (Frame-Buffer Streaming & Túnel de Entrada)
                Button(
                    onClick = {
                        onDismiss()
                        onOpenScreenMirror(node)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("open_live_screen_mirror_button")
                ) {
                    Icon(Icons.Default.Tv, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("📺 Ver Pantalla en Vivo & Control Táctil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Banner de Caso de Uso: "Pantalla Rota / Control Total"
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF881337).copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ScreenLockLandscape, contentDescription = null, tint = Color(0xFFFDA4AF), modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Modo Terminal Espejo: Opera este dispositivo remotamente aunque tenga la pantalla estrellada o sin respuesta táctil.",
                            fontSize = 11.sp,
                            color = Color(0xFFFEE2E2),
                            lineHeight = 14.sp
                        )
                    }
                }

                if (lastExecutedAction != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF065F46),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "✅ Orden enviada: $lastExecutedAction",
                            fontSize = 11.sp,
                            color = Color(0xFFA7F3D0),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Acciones Inmediatas de Rescate", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                Spacer(modifier = Modifier.height(8.dp))

                // Tarjetas de Acciones Rápidas
                RemoteActionCard(
                    title = "🚨 Emitir Alarma Baliza Táctica",
                    description = "Hace sonar el altavoz del nodo a volumen máximo para localizarlo o alertar.",
                    buttonText = "Sonar Alarma",
                    icon = Icons.Default.VolumeUp,
                    accentColor = Color(0xFFEF4444),
                    onClick = {
                        onExecuteAction(RemoteActionType.AUDIO_ALARM_BEACON, "")
                        lastExecutedAction = "Alarma Baliza Activada"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                RemoteActionCard(
                    title = "🔦 Linterna SOS Intermitente",
                    description = "Activa el flash LED trasero en patrón de rescate para visibilidad física.",
                    buttonText = "Encender Linterna",
                    icon = Icons.Default.FlashlightOn,
                    accentColor = Color(0xFFF59E0B),
                    onClick = {
                        onExecuteAction(RemoteActionType.TRIGGER_TORCH, "")
                        lastExecutedAction = "Linterna Táctica Activada"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                RemoteActionCard(
                    title = "📸 Captura de Foto Remota",
                    description = "Dispara la cámara remota en segundo plano y transmite la toma cifrada vía Wi-Fi Direct.",
                    buttonText = "Capturar Imagen",
                    icon = Icons.Default.PhotoCamera,
                    accentColor = Color(0xFF3B82F6),
                    onClick = {
                        onExecuteAction(RemoteActionType.CAPTURE_REMOTE_PHOTO, "")
                        lastExecutedAction = "Captura Remota Iniciada"
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                RemoteActionCard(
                    title = "💾 Rescatar Bóveda de Datos",
                    description = "Extrae todos los archivos de Room DB y notas cifradas de este nodo hacia este terminal.",
                    buttonText = "Extraer Bóveda",
                    icon = Icons.Default.Download,
                    accentColor = Color(0xFF10B981),
                    onClick = {
                        onExecuteAction(RemoteActionType.DUMP_VAULT_BACKUP, "")
                        lastExecutedAction = "Extracción de Bóveda en Curso"
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text("Voz Sintetizada Remota (TTS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = ttsMessage,
                    onValueChange = { ttsMessage = it },
                    placeholder = { Text("Escribe mensaje para que hable el otro dispositivo...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFE11D48),
                        unfocusedBorderColor = Color(0xFF334155)
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (ttsMessage.isNotBlank()) {
                            onExecuteAction(RemoteActionType.TEXT_TO_SPEECH_ALERT, ttsMessage)
                            lastExecutedAction = "Mensaje de voz enviado: '$ttsMessage'"
                            ttsMessage = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hablar en Dispositivo Remoto", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun RemoteActionCard(
    title: String,
    description: String,
    buttonText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E293B),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.9f)),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text(buttonText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
