package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.models.NetworkNode
import com.example.domain.models.RemoteActionType
import com.example.domain.p2p.FrameBufferPacket
import com.example.domain.p2p.InputTunnelStats

/**
 * 📺 Visor Interactivo de Transmisión de Pantalla y Túnel de Entrada Táctil:
 * Permite ver y controlar en tiempo real cualquier terminal de la malla,
 * diseñado específicamente para rescate de emergencia en dispositivos con pantalla destrozada o averiada.
 */
@Composable
fun ScreenMirrorViewerDialog(
    node: NetworkNode,
    currentFrame: FrameBufferPacket?,
    tunnelStats: InputTunnelStats,
    onDismiss: () -> Unit,
    onSendTap: (Float, Float) -> Unit,
    onSendSwipe: (Float, Float, Float, Float) -> Unit,
    onSendSystemNav: (String) -> Unit,
    onExecuteAction: (RemoteActionType, String) -> Unit
) {
    var isBrokenScreenFilterActive by remember { mutableStateOf(true) }
    var touchFeedbackText by remember { mutableStateOf("Toca o desliza en la pantalla remota") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0B132B),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .border(1.5.dp, Color(0xFFE11D48).copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                .testTag("screen_mirror_viewer_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cabecera del Visor
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFFE11D48).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(node.avatarIcon, fontSize = 20.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Espejo de Pantalla en Vivo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF22C55E).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "30 FPS • ${currentFrame?.latencyMs ?: 16}ms",
                                        color = Color(0xFF4ADE80),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text("${node.name} • ${currentFrame?.activeApp ?: "Terminal Mesh"}", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_screen_mirror_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Viewport del Frame-Buffer con Captura de Gestos
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                        .testTag("frame_buffer_viewport")
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                val normX = (offset.x / size.width).coerceIn(0f, 1f)
                                val normY = (offset.y / size.height).coerceIn(0f, 1f)
                                touchFeedbackText = "Toque enviado: (${(normX * 100).toInt()}%, ${(normY * 100).toInt()}%)"
                                onSendTap(normX, normY)
                            }
                        }
                        .pointerInput(Unit) {
                            var startX = 0f
                            var startY = 0f
                            detectDragGestures(
                                onDragStart = { offset ->
                                    startX = (offset.x / size.width).coerceIn(0f, 1f)
                                    startY = (offset.y / size.height).coerceIn(0f, 1f)
                                },
                                onDragEnd = {
                                    // Swipe completado
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val currentX = (change.position.x / size.width).coerceIn(0f, 1f)
                                    val currentY = (change.position.y / size.height).coerceIn(0f, 1f)
                                    touchFeedbackText = "Deslizado a: (${(currentX * 100).toInt()}%, ${(currentY * 100).toInt()}%)"
                                    onSendSwipe(startX, startY, currentX, currentY)
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (currentFrame?.bitmap != null) {
                        Image(
                            bitmap = currentFrame.bitmap.asImageBitmap(),
                            contentDescription = "Pantalla Remota en Tiempo Real",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFFE11D48), modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Sincronizando túnel de fotogramas P2P...", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        }
                    }

                    // Overlay informativo de rescate
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.65f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = touchFeedbackText,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Barra de Botones de Navegación del Sistema Remoto
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            touchFeedbackText = "Comando: ATRÁS"
                            onSendSystemNav("BACK")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("remote_back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Atrás", color = Color.White, fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            touchFeedbackText = "Comando: INICIO"
                            onSendSystemNav("HOME")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("remote_home_button")
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Inicio", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Inicio", color = Color.White, fontSize = 12.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            touchFeedbackText = "Comando: RECIENTES"
                            onSendSystemNav("RECENTS")
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E293B)),
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp).testTag("remote_recents_button")
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Recientes", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Apps", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Acciones Rápidas de Rescate de Emergencia
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onExecuteAction(RemoteActionType.DUMP_VAULT_BACKUP, "FULL_DUMP") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.weight(1f).padding(end = 4.dp).testTag("remote_dump_vault_button")
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rescatar Bóveda", fontSize = 11.sp, maxLines = 1)
                    }

                    Button(
                        onClick = { onExecuteAction(RemoteActionType.AUDIO_ALARM_BEACON, "MAX_VOLUME") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.weight(1f).padding(start = 4.dp).testTag("remote_alarm_beacon_button")
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sonar Alarma", fontSize = 11.sp, maxLines = 1)
                    }
                }
            }
        }
    }
}
