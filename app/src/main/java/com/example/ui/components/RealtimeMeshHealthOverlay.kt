package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.*

/**
 * Overlay de Dashboard de Salud en Tiempo Real para cada nodo conectado:
 * - Intensidad de Señal RSSI en tiempo real (dBm + Barra animada).
 * - Tareas de Sincronización Activas (Progreso y Bóvedas cifradas).
 * - Latencia de Malla Multi-Salto (ms).
 * - Disparador de 'Sync-All' con verificación criptográfica instantánea.
 * - Acceso a Control Remoto Unificado para operar nodos dañados.
 */
@Composable
fun RealtimeMeshHealthOverlay(
    nodes: List<NetworkNode>,
    activeTasks: List<ActiveSyncTask>,
    isSyncAllRunning: Boolean,
    onDismiss: () -> Unit,
    onTriggerSyncAll: () -> Unit,
    onNodeSelect: (NetworkNode) -> Unit,
    onOpenRemoteControl: (NetworkNode) -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_health")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF0A1128),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .shadow(24.dp, RoundedCornerShape(24.dp))
                .testTag("realtime_mesh_health_overlay"),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            ) {
                // Cabecera del Dashboard
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(22.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Dashboard de Salud de Malla", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            Text("Telemetría en Vivo & Latencias", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Botón Acción Primaria: Comando SYNC-ALL
                Button(
                    onClick = onTriggerSyncAll,
                    enabled = !isSyncAllRunning,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSyncAllRunning) Color(0xFF1E293B) else Color(0xFF2563EB)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("trigger_sync_all_button")
                ) {
                    if (isSyncAllRunning) {
                        CircularProgressIndicator(
                            color = Color(0xFF60A5FA),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verificando Tokens RSA/AES-256...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    } else {
                        Icon(Icons.Default.SyncLock, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ejecutar 'Sync-All' Criptográfico", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }

                // Tareas activas de sincronización
                if (activeTasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF1E1B4B),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("🔄 Tarea Activa:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA78BFA))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(activeTasks.first().taskName, fontSize = 11.sp, color = Color.White)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { activeTasks.first().progressPercent / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFA78BFA),
                                trackColor = Color(0xFF312E81)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Nodos Conectados (${nodes.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Lista de Nodos con Estadísticas de Salud
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(nodes, key = { it.id }) { node ->
                        PeerHealthCard(
                            node = node,
                            onInspect = { onNodeSelect(node) },
                            onRemoteControl = { onOpenRemoteControl(node) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de Salud Individual por Peer en el Dashboard.
 */
@Composable
fun PeerHealthCard(
    node: NetworkNode,
    onInspect: () -> Unit,
    onRemoteControl: () -> Unit
) {
    val signalPercent = ((node.rssi + 100).coerceIn(0, 70) / 70f * 100).toInt()
    val signalColor = when {
        node.rssi > -60 -> Color(0xFF00E676)
        node.rssi > -75 -> Color(0xFFFFD600)
        else -> Color(0xFFFF5252)
    }

    val latencyColor = when {
        node.latencyMs < 15 -> Color(0xFF00E676)
        node.latencyMs < 35 -> Color(0xFFFFD600)
        else -> Color(0xFFFF5252)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF131E3A),
        tonalElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onInspect() }
            .testTag("peer_health_card_${node.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Fila 1: Avatar, Nombre y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E293B),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(node.avatarIcon, fontSize = 20.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(node.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            if (node.isPriority) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("⭐", fontSize = 11.sp)
                            }
                        }
                        Text(
                            text = "${node.connectionType.protocol} • ${node.status.label}",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                if (!node.isLocalMaster) {
                    FilledTonalButton(
                        onClick = onRemoteControl,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFE11D48).copy(alpha = 0.25f),
                            contentColor = Color(0xFFFDA4AF)
                        ),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Controlar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Fila 2: Métricas en Cuadrícula (Señal RSSI, Latencia, Batería)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Señal RSSI
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("SEÑAL RSSI", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${node.rssi} dBm", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = signalColor, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Latencia Mesh
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("LATENCIA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${node.latencyMs} ms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = latencyColor, fontFamily = FontFamily.Monospace)
                    }
                }

                // Batería
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F172A),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("BATERÍA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${node.batteryLevel}% ${if (node.isCharging) "⚡" else ""}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }
}
