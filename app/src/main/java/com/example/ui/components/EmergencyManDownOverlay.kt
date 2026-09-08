package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.hardware.EmergencyStatus
import com.example.domain.hardware.TacticalManDownDetector

@Composable
fun EmergencyManDownOverlay(
    detector: TacticalManDownDetector
) {
    val status by detector.emergencyStatus.collectAsStateWithLifecycle()
    val countdown by detector.countdownSeconds.collectAsStateWithLifecycle()
    val lastAlert by detector.lastAlert.collectAsStateWithLifecycle()

    AnimatedVisibility(
        visible = status == EmergencyStatus.COUNTDOWN_ACTIVE || status == EmergencyStatus.SOS_TRANSMITTING,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (status == EmergencyStatus.COUNTDOWN_ACTIVE) Color(0xFF7F1D1D) else Color(0xFF991B1B),
            border = BorderStroke(2.dp, Color(0xFFFF5252)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (status == EmergencyStatus.COUNTDOWN_ACTIVE) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        Text(
                            "ALERTA MAN DOWN • IMPACTO DETECTADO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    // Anillo de Cuenta Regresiva
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
                        CircularProgressIndicator(
                            progress = { countdown / 10f },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            strokeWidth = 4.dp
                        )
                        Text(
                            "$countdown",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Text(
                        "Transmitiendo señal MAYDAY en $countdown seg...",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { detector.cancelCountdown() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Falsa Alarma", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }

                        Button(
                            onClick = { detector.broadcastEmergencySOS("Impacto confirmado por operador") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SOS Inmediato", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                } else if (status == EmergencyStatus.SOS_TRANSMITTING) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color.White)
                        Text("🚨 BALIZA MAYDAY EN TRANSMISIÓN MALLA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text(
                        "Coordenadas emitidas: [${lastAlert?.latitude}, ${lastAlert?.longitude}] • QoS 1 Crítico",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Button(
                        onClick = { detector.dismissActiveSOS() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Detener Transmisión SOS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
