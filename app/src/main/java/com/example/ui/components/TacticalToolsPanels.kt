package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.audio.MorseBeaconSynthesizer
import com.example.domain.hardware.*
import com.example.domain.media.ThermalNightVisionProcessor
import com.example.domain.media.VisionShaderMode
import com.example.domain.models.SitrepReportManager
import com.example.domain.models.TacticalSitrepReport
import com.example.domain.p2p.MeshTopologyGraphManager
import com.example.domain.media.VisualOpticalLiFiTransceiver
import com.example.domain.media.AcousticShotClassifier
import com.example.domain.sensors.PhysioBioTelemetryManager
import com.example.domain.security.ShamirSecretSharingVault
import com.example.domain.security.ShamirShare
import com.example.domain.security.EmergencyZeroizeManager
import com.example.domain.sensors.PedestrianDeadReckoningEngine
import com.example.domain.p2p.FrequencyHoppingMeshEngine
import com.example.domain.p2p.TacticalBinaryCompressor
import com.example.domain.ai.TacticalMissionAgent
import com.example.domain.models.NatoSymbologyRegistry
import com.example.domain.models.NatoTacticalSymbol
import com.example.domain.models.NatoAffiliation
import com.example.domain.models.NatoUnitType
import com.example.domain.sensors.BarometerStormAlertEngine
import com.example.domain.audio.UltrasonicDataLinkTransceiver
import com.example.domain.sensors.SafeBubble3DProximityRadar
import com.example.domain.sensors.TacticalBallisticsCalculator
import com.example.domain.security.MeshHoneypotGenerator
import com.example.domain.security.SybilMeshDetector
import com.example.domain.security.PostQuantumKyberVault
import com.example.domain.security.PqcKeyPair
import com.example.domain.security.TacticalMissionBlockchain
import com.example.domain.security.RfEmissionSignatureMeter
import com.example.domain.security.EmconAlphaManager
import com.example.domain.security.EmconLevel
import com.example.domain.sensors.TacticalPhotogrammetryEngine
import com.example.domain.sensors.MavlinkTelemetryTransceiver
import com.example.domain.sensors.TacticalMedevacEngine
import com.example.domain.sensors.NineLineMedevacRequest
import com.example.domain.sensors.SolarEphemerisCompass
import com.example.domain.sensors.SatelliteMeshGateway
import com.example.domain.c4isr.TacticalVoiceSpectralEngine
import com.example.domain.c4isr.TacticalAStarRouter
import com.example.domain.c4isr.SaluteIntelReportEngine
import com.example.domain.c4isr.SaluteReport
import com.example.domain.c4isr.MeshTimeSynchronizer
import com.example.domain.c4isr.C4IsrMasterDashboardEngine
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

// --- 1. Panel de Bóveda Esteganográfica ---
@Composable
fun SteganographyToolDialog(onDismiss: () -> Unit) {
    var secretText by remember { mutableStateOf("") }
    var extractedText by remember { mutableStateOf<String?>(null) }
    var statusMessage by remember { mutableStateOf("Listo para codificar o extraer.") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.EnhancedEncryption, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Bóveda Esteganográfica", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Oculta datos sensibles dentro de matrices de píxeles (LSB) o portadoras de audio sin alterar la apariencia visual.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = secretText,
                    onValueChange = { secretText = it },
                    label = { Text("Texto Secreto a Inyectar", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ESTADO DE PORTADORA:", color = TacticalAmberTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(statusMessage, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        if (extractedText != null) {
                            Text("PAYLOAD RECUPERADO:", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(extractedText ?: "", color = TacticalEmeraldSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (secretText.isBlank()) {
                                statusMessage = "Introduce texto primero."
                                return@Button
                            }
                            statusMessage = "✓ Payload inyectado en buffer LSB simulado con cabecera OMNI_STEGO."
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Codificar LSB", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            extractedText = if (secretText.isNotBlank()) secretText else "COLS-ALPHA-7749-SECURE-KEY"
                            statusMessage = "✓ Extracción completada desde la matriz."
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Extraer", fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- 2. Panel de Baliza Morse ---
@Composable
fun MorseBeaconToolDialog(onDismiss: () -> Unit) {
    var messageToTransmit by remember { mutableStateOf("SOS BASE OMNI") }
    val isTransmitting by MorseBeaconSynthesizer.isTransmitting.collectAsStateWithLifecycle()
    val morseCode = remember(messageToTransmit) { MorseBeaconSynthesizer.textToMorse(messageToTransmit) }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Baliza Morse Táctica", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Generador de tonos senoidales acústicos de supervivencia (850 Hz) para señales de socorro y transmisiones fuera de banda.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = messageToTransmit,
                    onValueChange = { messageToTransmit = it },
                    label = { Text("Mensaje / Código", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalAmberTertiary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("CÓDIGO MORSE GENERADO:", color = TacticalAmberTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (morseCode.isBlank()) "..." else morseCode,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (isTransmitting) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0x33FF9100), RoundedCornerShape(8.dp)).padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TacticalAmberTertiary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Transmitiendo tono senoidal en altavoz...", color = TacticalAmberTertiary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (!isTransmitting) {
                                coroutineScope.launch {
                                    MorseBeaconSynthesizer.transmitMorse(morseCode)
                                }
                            } else {
                                MorseBeaconSynthesizer.stopTransmission()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isTransmitting) Color.Red else TacticalAmberTertiary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(if (isTransmitting) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isTransmitting) "Detener Transmisión" else "Transmitir Baliza Acústica", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                MorseBeaconSynthesizer.stopTransmission()
                onDismiss()
            }) { Text("Cerrar") }
        }
    )
}

// --- 3. Panel de Espectro RF y Jamming ---
@Composable
fun RfSpectrumToolDialog(onDismiss: () -> Unit) {
    val spectrumSnapshot by RfSpectrumAnalyzer.spectrumData.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        RfSpectrumAnalyzer.startContinuousScanning()
        onDispose {
            RfSpectrumAnalyzer.stopScanning()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Analizador de Espectro RF", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (spectrumSnapshot.isJammingSuspected) Color(0x33FF0000) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (spectrumSnapshot.isJammingSuspected) Color.Red else TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("ESTADO DE RADIOFRECUENCIA (2.4 GHz):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                        Text(spectrumSnapshot.detectedInterferenceLevel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (spectrumSnapshot.isJammingSuspected) Color.Red else TacticalEmeraldSecondary)
                        Text("SNR Promedio: ${String.format("%.1f", spectrumSnapshot.averageSnr)} dB", fontSize = 11.sp, color = Color.White, fontFamily = FontFamily.Monospace)
                    }
                }

                Text("Canales Wi-Fi / BLE (1-13) & Densidad de Ruido:", fontSize = 11.sp, fontWeight = FontWeight.Bold)

                // Gráfico de barras de canales
                Surface(shape = RoundedCornerShape(8.dp), color = Color.Black, modifier = Modifier.fillMaxWidth().height(120.dp).padding(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        spectrumSnapshot.channels.forEach { ch ->
                            val heightFraction = (ch.snrRatioDb / 40f).coerceIn(0.1f, 1f)
                            val barColor = if (ch.isCongested) Color.Red else if (ch.snrRatioDb < 22f) TacticalAmberTertiary else TacticalEmeraldSecondary

                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                        .background(barColor)
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text("C${ch.channel}", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- 4. Panel de Informes SITREP ---
@Composable
fun SitrepToolDialog(onDismiss: () -> Unit) {
    val reports by SitrepReportManager.reports.collectAsStateWithLifecycle()
    var showCreateForm by remember { mutableStateOf(false) }

    var unitName by remember { mutableStateOf("PATRULLA-7") }
    var coords by remember { mutableStateOf("4.6105° N, 74.0820° W") }
    var enemy by remember { mutableStateOf("Ninguna señal detectada") }
    var friendly by remember { mutableStateOf("Completos / Operativos") }
    var logistics by remember { mutableStateOf("Baterías al 90%, Radios al 100%") }
    var support by remember { mutableStateOf("Ruta libre") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Informes SITREP Tácticos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!showCreateForm) {
                    Button(
                        onClick = { showCreateForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Crear Nuevo SITREP", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    reports.forEach { report ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF161B22),
                            border = BorderStroke(1.dp, Color.DarkGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(report.unitDesignator, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary, fontSize = 12.sp)
                                    Text(report.gridCoordinates, fontSize = 10.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                                }
                                Text("Tropas: ${report.friendlyStatus}", fontSize = 11.sp, color = Color.White)
                                Text("Logística: ${report.logisticsAndAmmo}", fontSize = 10.sp, color = Color.Gray)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = unitName, onValueChange = { unitName = it }, label = { Text("Unidad / Indicativo", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = coords, onValueChange = { coords = it }, label = { Text("Coordenadas / MGRS", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = friendly, onValueChange = { friendly = it }, label = { Text("Estado de Tropas / Bajas", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = logistics, onValueChange = { logistics = it }, label = { Text("Logística / Baterías", fontSize = 10.sp) }, modifier = Modifier.fillMaxWidth())

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val rep = TacticalSitrepReport(
                                    unitDesignator = unitName,
                                    gridCoordinates = coords,
                                    friendlyStatus = friendly,
                                    logisticsAndAmmo = logistics,
                                    supportRequested = support
                                )
                                SitrepReportManager.createAndBroadcastReport(rep)
                                showCreateForm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Transmitir", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(onClick = { showCreateForm = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) {
                            Text("Atrás")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- 5. Panel de Horizonte Artificial & Brújula HUD ---
@Composable
fun HorizonHudToolDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val hudManager = remember { HorizonCompassHudManager.getInstance(context) }
    val orientation by hudManager.orientation.collectAsStateWithLifecycle()

    DisposableEffect(Unit) {
        hudManager.startTracking()
        onDispose {
            hudManager.stopTracking()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Horizonte Artificial & Brújula HUD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Brújula e Inclinómetro HUD
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.6f)),
                    modifier = Modifier.size(220.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val radius = size.minDimension / 2f

                            // Círculo exterior brújula
                            drawCircle(color = Color(0xFF00E5FF).copy(alpha = 0.3f), radius = radius, style = Stroke(width = 2.dp.toPx()))

                            // Línea de horizonte artificial inclinada por Pitch y Roll
                            val pitchOffset = (orientation.pitchDegrees / 90f) * (radius * 0.7f)
                            val rollRad = Math.toRadians(orientation.rollDegrees.toDouble())

                            val dx = cos(rollRad).toFloat() * radius * 0.8f
                            val dy = sin(rollRad).toFloat() * radius * 0.8f

                            drawLine(
                                color = Color(0xFF69F0AE),
                                start = Offset(center.x - dx, center.y + pitchOffset - dy),
                                end = Offset(center.x + dx, center.y + pitchOffset + dy),
                                strokeWidth = 3.dp.toPx()
                            )

                            // Punto de mira central
                            drawCircle(color = Color(0xFFFFD600), radius = 4.dp.toPx(), center = center)
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${orientation.azimuthDegrees.toInt()}° ${orientation.cardinalDirection}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                            Text("PITCH: ${String.format("%.1f", orientation.pitchDegrees)}°", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Text("ROLL: ${String.format("%.1f", orientation.rollDegrees)}°", color = TacticalAmberTertiary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- 6. Panel de Interruptor de Hombre Muerto ---
@Composable
fun DeadMansSwitchToolDialog(onDismiss: () -> Unit) {
    val isActive by DeadMansSwitchWatchdog.isActive.collectAsStateWithLifecycle()
    val remainingSeconds by DeadMansSwitchWatchdog.remainingSeconds.collectAsStateWithLifecycle()
    val isWarning by DeadMansSwitchWatchdog.isWarningState.collectAsStateWithLifecycle()
    val selectedInterval by DeadMansSwitchWatchdog.selectedInterval.collectAsStateWithLifecycle()

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.HourglassBottom, contentDescription = null, tint = if (isWarning) Color.Red else TacticalAmberTertiary)
                Text("Hombre Muerto (Watchdog)", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Temporizador de seguridad para patrullas en solitario. Requiere confirmación periódica o disparará baliza SOS automática.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isWarning) Color(0x44FF0000) else Color(0xFF161B22),
                    border = BorderStroke(2.dp, if (isWarning) Color.Red else if (isActive) TacticalEmeraldSecondary else Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            if (isActive) "TEMPORIZADOR ACTIVO" else "SISTEMA DESARMADO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) TacticalEmeraldSecondary else Color.Gray
                        )
                        Text(
                            String.format("%02d:%02d", minutes, seconds),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isWarning) Color.Red else Color.White
                        )
                    }
                }

                if (isActive) {
                    Button(
                        onClick = { DeadMansSwitchWatchdog.checkInOperatorAlive() },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ESTOY BIEN (RESET WATCHDOG)", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { DeadMansSwitchWatchdog.disarmWatchdog() },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Desarmar Sistema", color = Color.Red)
                    }
                } else {
                    Text("Selecciona Intervalo de Chequeo:", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DeadManInterval.values().forEach { interval ->
                            val isSel = (selectedInterval == interval)
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isSel) TacticalCyanPrimary.copy(alpha = 0.2f) else Color(0xFF161B22),
                                border = BorderStroke(1.dp, if (isSel) TacticalCyanPrimary else Color.DarkGray),
                                modifier = Modifier.weight(1f).clickable { DeadMansSwitchWatchdog.startWatchdog(interval) }
                            ) {
                                Text(
                                    interval.label,
                                    fontSize = 9.sp,
                                    color = if (isSel) TacticalCyanPrimary else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- 7. Panel de Topología de Malla Gráfica ---
@Composable
fun MeshTopologyToolDialog(onDismiss: () -> Unit) {
    val topology by MeshTopologyGraphManager.topologyState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Hub, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Grafo de Topología Malla", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().height(260.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        val nodeMap = topology.nodes.associateBy { it.nodeId }

                        // Dibujar enlaces
                        topology.links.forEach { link ->
                            val from = nodeMap[link.fromNodeId]
                            val to = nodeMap[link.toNodeId]
                            if (from != null && to != null) {
                                val p1 = Offset(from.xPos * size.width, from.yPos * size.height)
                                val p2 = Offset(to.xPos * size.width, to.yPos * size.height)

                                drawLine(
                                    color = if (link.isDirect) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFFFFD600).copy(alpha = 0.4f),
                                    start = p1,
                                    end = p2,
                                    strokeWidth = if (link.isDirect) 2.dp.toPx() else 1.dp.toPx(),
                                    pathEffect = if (!link.isDirect) PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f) else null
                                )
                            }
                        }

                        // Dibujar nodos
                        topology.nodes.forEach { node ->
                            val pos = Offset(node.xPos * size.width, node.yPos * size.height)
                            val isLocal = node.nodeId == "LOCAL_HOST"

                            drawCircle(
                                color = if (isLocal) Color(0xFF00E5FF) else if (node.isRelayActive) Color(0xFF69F0AE) else Color(0xFFFF9100),
                                radius = if (isLocal) 8.dp.toPx() else 6.dp.toPx(),
                                center = pos
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Nodos: ${topology.nodes.size} | Salud: ${topology.networkHealthPercent}%", color = TacticalEmeraldSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Button(
                        onClick = { MeshTopologyGraphManager.refreshTopology() },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Re-escanear", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 1: Panel de Transmisión Li-Fi Óptico ---
@Composable
fun VisualOpticalLiFiDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val liFiTransceiver = remember { VisualOpticalLiFiTransceiver.getInstance(context) }
    val isTransmitting by liFiTransceiver.isTransmitting.collectAsStateWithLifecycle()
    val progress by liFiTransceiver.transmissionProgress.collectAsStateWithLifecycle()
    var messageInput by remember { mutableStateOf("ALPHA SECURE EVAC 0400") }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FlashlightOn, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Enlace Óptico Li-Fi (Flash)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Modula señales estroboscópicas mediante el flash de la cámara para transmitir paquetes binarios en silencio de radio (OOB).",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = messageInput,
                    onValueChange = { messageInput = it },
                    label = { Text("Mensaje Li-Fi a Modular", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isTransmitting
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10141D),
                    border = BorderStroke(1.dp, if (isTransmitting) TacticalCyanPrimary else Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            if (isTransmitting) "TRANSMITIENDO PULSOS ÓPTICOS (${(progress * 100).toInt()}%)" else "MODULADOR LISTO",
                            color = if (isTransmitting) TacticalCyanPrimary else TacticalEmeraldSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = TacticalCyanPrimary,
                            trackColor = Color.DarkGray
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            scope.launch {
                                liFiTransceiver.transmitOpticalPayload(messageInput)
                            }
                        },
                        enabled = !isTransmitting && messageInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Iniciar Li-Fi", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }

                    if (isTransmitting) {
                        Button(
                            onClick = { liFiTransceiver.abortTransmission() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Abortar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 2: Panel de Shamir Secret Sharing ---
@Composable
fun ShamirSecretSharingDialog(onDismiss: () -> Unit) {
    var secretKeyToSplit by remember { mutableStateOf("AES256_TOP_SECRET_MISSION_KEY_DELTA") }
    var totalPartsN by remember { mutableStateOf("5") }
    var thresholdK by remember { mutableStateOf("3") }
    var generatedShares by remember { mutableStateOf<List<ShamirShare>>(emptyList()) }
    var reconstructedResult by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Bóveda Shamir (K-de-N)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Divide una clave de cifrado en N partes distribuidas. Se requiere un quórum de al menos K partes para reconstruir el secreto original.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = secretKeyToSplit,
                    onValueChange = { secretKeyToSplit = it },
                    label = { Text("Clave Maestra Secreta", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = totalPartsN,
                        onValueChange = { totalPartsN = it },
                        label = { Text("Partes Totales (N)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = thresholdK,
                        onValueChange = { thresholdK = it },
                        label = { Text("Umbral Quórum (K)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val n = totalPartsN.toIntOrNull() ?: 5
                        val k = thresholdK.toIntOrNull() ?: 3
                        val bytes = secretKeyToSplit.toByteArray(Charsets.UTF_8)
                        val shares = ShamirSecretSharingVault.splitSecret(bytes, n, k)
                        generatedShares = shares
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Fragmentar Clave (Split K-of-N)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                if (generatedShares.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D1117),
                        border = BorderStroke(1.dp, Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("FRAGMENTOS GENERADOS:", color = TacticalAmberTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            generatedShares.forEach { share ->
                                Text("Parte #${share.index}: ${share.shareDataHex.take(24)}...", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val k = thresholdK.toIntOrNull() ?: 3
                            val reconBytes = ShamirSecretSharingVault.reconstructSecret(generatedShares, k)
                            reconstructedResult = if (reconBytes != null) String(reconBytes, Charsets.UTF_8) else "Error al reconstruir"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Reconstruir con Quórum ($thresholdK partes)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                if (reconstructedResult != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0F1E19),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("SECRETO RECONSTRUIDO:", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text(reconstructedResult ?: "", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 3: Panel de Clasificación Acústica de Disparos ---
@Composable
fun AcousticShotClassifierDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val classifier = remember { AcousticShotClassifier.getInstance(context) }
    val isMonitoring by classifier.isMonitoring.collectAsStateWithLifecycle()
    val ambientDb by classifier.ambientSoundLevelDb.collectAsStateWithLifecycle()
    val lastEvent by classifier.lastDetectedEvent.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Hearing, contentDescription = null, tint = Color(0xFFFF5252))
                Text("Detección Acústica de Disparos", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Clasificador espectral de ondas de choque acústicas y disparos para alerta temprana de emboscadas e impactos.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF181114),
                    border = BorderStroke(1.dp, if (lastEvent != null) Color(0xFFFF5252) else Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NIVEL SONORO RMS:", color = TacticalAmberTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${ambientDb.toInt()} dB SPL", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        LinearProgressIndicator(
                            progress = { (ambientDb / 120f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = if (ambientDb > 85f) Color(0xFFFF5252) else TacticalCyanPrimary,
                            trackColor = Color.DarkGray
                        )

                        if (lastEvent != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("ÚLTIMO EVENTO DETECTADO:", color = Color(0xFFFF5252), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("💥 ${lastEvent?.eventType} (${lastEvent?.estimatedDb?.toInt()} dB)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("Confianza: ${(lastEvent?.confidence?.times(100))?.toInt()}%", color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isMonitoring) classifier.stopAcousticSurveillance() else classifier.startAcousticSurveillance()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isMonitoring) Color(0xFFFF5252) else TacticalEmeraldSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isMonitoring) "Detener Escucha" else "Activar Escucha", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = { classifier.triggerTestAcousticImpulse() },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simular Disparo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 4: Panel de Telemetría Biológica de Operador ---
@Composable
fun PhysioBioTelemetryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bioManager = remember { PhysioBioTelemetryManager.getInstance(context) }
    val isTracking by bioManager.isBioTrackingActive.collectAsStateWithLifecycle()
    val metrics by bioManager.bioMetrics.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFFF4081))
                Text("Telemetría Biológica de Operador", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Monitorización fisiológica en combate: estimación de pulso, índice de estrés inercial y Combat Readiness Score.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF13101C),
                    border = BorderStroke(1.dp, Color(0xFFFF4081).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PULSO CARDÍACO EST.:", color = Color(0xFFFF4081), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${metrics.estimatedHeartRateBpm} BPM", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("COMBAT READINESS SCORE:", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${metrics.combatReadinessScore}%", color = TacticalEmeraldSecondary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ÍNDICE DE ESTRÉS:", color = TacticalAmberTertiary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("${(metrics.stressIndex * 100).toInt()}%", color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ESTADO DE FATIGA:", color = Color.LightGray, fontSize = 10.sp)
                            Text(metrics.fatigueStatus, color = TacticalCyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isTracking) bioManager.stopBioTracking() else bioManager.startBioTracking()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (isTracking) Color(0xFFFF5252) else Color(0xFFFF4081)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isTracking) "Desactivar Telemetría Bio" else "Activar Sensores Bio", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 5: Panel de Destrucción de Emergencia ZEROIZE ---
@Composable
fun EmergencyZeroizeDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val zeroizeManager = remember { EmergencyZeroizeManager.getInstance(context) }
    val isZeroizing by zeroizeManager.isZeroizing.collectAsStateWithLifecycle()
    val completed by zeroizeManager.zeroizeCompleted.collectAsStateWithLifecycle()
    val logs by zeroizeManager.statusLog.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var confirmedByOperator by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFFF1744))
                Text("Protocolo Cero-Huella ZEROIZE", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFFF1744))
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "ADVERTENCIA CRÍTICA: Ejecuta una sobreescritura de 3 pasadas (DoD 5220.22-M) en RAM, cachés, registros y claves de cifrado. Acción irreversible ante riesgo de captura.",
                    fontSize = 11.sp,
                    color = Color(0xFFFF8A80)
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, if (completed) TacticalEmeraldSecondary else Color(0xFFFF1744)),
                    modifier = Modifier.fillMaxWidth().height(140.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
                        if (logs.isEmpty()) {
                            Text("En espera de autorización del operador...", color = Color.Gray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        } else {
                            logs.forEach { l ->
                                Text(l, color = if (completed) TacticalEmeraldSecondary else Color(0xFFFF5252), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = confirmedByOperator,
                        onCheckedChange = { confirmedByOperator = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFFFF1744))
                    )
                    Text("Confirmo autorización de purga total", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = {
                        scope.launch {
                            zeroizeManager.executeEmergencyZeroize()
                        }
                    },
                    enabled = confirmedByOperator && !isZeroizing && !completed,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF1744)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isZeroizing) "EJECUTANDO PURGA..." else "DISPARAR ZEROIZE INMEDIATO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 6: Panel de Navegación Inercial a Estima (PDR) ---
@Composable
fun PdrNavigationDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val pdrEngine = remember { PedestrianDeadReckoningEngine.getInstance(context) }
    val pdrState by pdrEngine.pdrState.collectAsStateWithLifecycle()
    val isTracking by pdrEngine.isTracking.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Navegación Inercial (PDR)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cálculo continuo de posición en modo GPS-Denied mediante acelerómetro, giróscopo y detección inercial de pasos.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DESPLAZAMIENTO X (E):", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f m", pdrState.relativeX), color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DESPLAZAMIENTO Y (N):", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f m", pdrState.relativeY), color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DISTANCIA TOTAL:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f m", pdrState.totalDistanceMeters), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PASOS TÁCTICOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${pdrState.stepCount}", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RUMBO MAGNÉTICO:", color = Color.Gray, fontSize = 10.sp)
                            Text("${pdrState.currentHeadingDegrees.toInt()}°", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isTracking) pdrEngine.stopPdrTracking() else pdrEngine.startPdrTracking()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTracking) Color(0xFFFF5252) else TacticalCyanPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isTracking) "Pausar PDR" else "Iniciar PDR", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { pdrEngine.resetOrigin() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Punto Cero (0,0)", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = { pdrEngine.simulateStep() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simular Paso Táctico (+0.76m)", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 7: Panel de Salto de Frecuencia Virtual (FHSS) ---
@Composable
fun FrequencyHoppingDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val fhssEngine = remember { FrequencyHoppingMeshEngine.getInstance(context) }
    val hopStatus by fhssEngine.hopStatus.collectAsStateWithLifecycle()
    val isHoppingActive by fhssEngine.isHoppingActive.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SyncAlt, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Salto de Frecuencia (FHSS)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Conmutación pseudoaleatoria determinista de frecuencias de enlace para evasión de radiolocalización e interferencia hostil (Anti-Jamming).",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1B2A),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CANAL ACTUAL:", color = Color.Gray, fontSize = 10.sp)
                            Text("CH ${hopStatus.currentChannel}", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FRECUENCIA ISM:", color = Color.Gray, fontSize = 10.sp)
                            Text("${hopStatus.frequencyMhz} MHz", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TASA DE SALTO:", color = Color.Gray, fontSize = 10.sp)
                            Text("${hopStatus.hopRatePerSec} hops/seg", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PATRÓN CRIPTO:", color = Color.Gray, fontSize = 10.sp)
                            Text(hopStatus.hoppingPattern, color = TacticalCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (isHoppingActive) fhssEngine.stopFrequencyHopping() else fhssEngine.startFrequencyHopping()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isHoppingActive) Color(0xFFFF5252) else TacticalEmeraldSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isHoppingActive) "Detener Secuencia FHSS" else "Iniciar Salto de Frecuencia", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 8: Panel de Simbología OTAN APP-6 ---
@Composable
fun NatoSymbologyDialog(onDismiss: () -> Unit) {
    val symbols = remember { NatoSymbologyRegistry.sampleTacticalSymbols }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Tablero OTAN APP-6 / MIL-STD", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Simbología táctica estandarizada para identificación unificada de unidades y amenazas sobre el terreno:",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                symbols.forEach { sym ->
                    val color = Color(sym.affiliation.colorHex)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, color.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(color.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sym.unitType.code, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = color, fontFamily = FontFamily.Monospace)
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sym.callsign, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                Text("${sym.affiliation.displayName} • ${sym.unitType.label}", fontSize = 9.sp, color = Color.Gray)
                                if (sym.remarks.isNotEmpty()) {
                                    Text(sym.remarks, fontSize = 9.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 9: Panel del Agente Táctico de Misión ---
@Composable
fun TacticalMissionAgentDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val agent = remember { TacticalMissionAgent.getInstance(context) }
    val recommendations by agent.recentRecommendations.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Psychology, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Agente Táctico Autónomo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Asistente local inteligente para evaluación offline de condiciones de combate y cursos de acción recomendados (COA):",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                recommendations.forEach { rec ->
                    val priorityColor = when (rec.priority) {
                        "CRÍTICO" -> Color(0xFFFF1744)
                        "URGENTE" -> TacticalAmberTertiary
                        else -> TacticalCyanPrimary
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, priorityColor.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(rec.title, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                Text(rec.priority, fontWeight = FontWeight.Bold, fontSize = 9.sp, color = priorityColor, fontFamily = FontFamily.Monospace)
                            }
                            Text(rec.rationale, fontSize = 10.sp, color = Color.LightGray)
                            Text("Acción: ${rec.suggestedAction}", fontSize = 10.sp, color = TacticalEmeraldSecondary)
                        }
                    }
                }

                Button(
                    onClick = {
                        agent.evaluateMissionConditions(
                            batteryLevel = 15,
                            activePeersCount = 3,
                            hostileDetected = true,
                            signalNoiseDb = 82f
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Evaluar Entorno de Misión", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 10: Panel de Compresor Binario Ultradenso ---
@Composable
fun TacticalBinaryCompressorDialog(onDismiss: () -> Unit) {
    var rawText by remember { mutableStateOf("SITREP POS: 34.0522N, 118.2437W | ESTADO: OPERATIVO | CONSUMO BATERIA: NORMAL") }
    var result by remember { mutableStateOf<com.example.domain.p2p.CompressionResult?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Compress, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Compresor Binario Ultradenso", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Compresión de paquetes para enlaces de ancho de banda ultrabajo (<1200 bps) en condiciones críticas de malla.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    label = { Text("Texto / Telemetría a Comprimir", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Button(
                    onClick = {
                        result = TacticalBinaryCompressor.compressPayload(rawText)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Comprimir Payload", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (result != null) {
                    val r = result!!
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black,
                        border = BorderStroke(1.dp, TacticalAmberTertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("RESULTADO DE COMPRESIÓN:", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            Text("Original: ${r.originalSizeBytes} bytes | Comprimido: ${r.compressedSizeBytes} bytes", color = Color.White, fontSize = 10.sp)
                            Text("Reducción de Ancho de Banda: ${r.compressionRatioPercentage.toInt()}%", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("Payload Hex: ${r.compressedHex.take(36)}...", color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 11: Panel de Barómetro Táctico & Alerta de Tormenta ---
@Composable
fun BarometerStormDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { BarometerStormAlertEngine.getInstance(context) }
    val atmoState by engine.atmosphereState.collectAsStateWithLifecycle()
    val isMonitoring by engine.isMonitoring.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CloudQueue, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Barómetro y Alerta de Tormenta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Monitoreo continuo de presión atmosférica (QNH), altitud relativa y detección temprana de caídas bruscas barométricas.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (atmoState.stormAlertActive) Color(0xFF330000) else Color.Black,
                    border = BorderStroke(1.dp, if (atmoState.stormAlertActive) Color.Red else TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PRESIÓN ATMOSFÉRICA:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f hPa", atmoState.pressureHpa), color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ALTITUD ESTIMADA:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f m", atmoState.estimatedAltitudeMeters), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TENDENCIA 3H:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f hPa/h", atmoState.pressureTrendHpaPerHr), color = if (atmoState.pressureTrendHpaPerHr < -1.5f) Color(0xFFFF5252) else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ESTADO ATMOSFÉRICO:", color = Color.Gray, fontSize = 10.sp)
                            Text(atmoState.weatherConditionSummary, color = if (atmoState.stormAlertActive) Color.Red else TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isMonitoring) engine.stopBarometerMonitoring() else engine.startBarometerMonitoring()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isMonitoring) Color(0xFFFF5252) else TacticalCyanPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isMonitoring) "Pausar Sensor" else "Iniciar Sensor", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = { engine.calibrateBaseElevation() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Calibrar QNH", fontSize = 11.sp)
                    }
                }

                OutlinedButton(
                    onClick = { engine.simulateStormPressureDrop() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Simular Caída de Presión (-4.2 hPa)", color = Color(0xFFFF5252), fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 12: Panel de Enlace Acústico Sub-audible / Ultrasonido ---
@Composable
fun UltrasonicLinkDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val transceiver = remember { UltrasonicDataLinkTransceiver.getInstance(context) }
    val txState by transceiver.state.collectAsStateWithLifecycle()
    var inputData by remember { mutableStateOf("COMANDO: AVANZAR A PUNTO BRAVO") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Enlace Acústico Sub-audible (18-20 kHz)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Transmisión P2P inaudible mediante modulación FSK ultrasónica (18.5 / 19.5 kHz) en condición de silencio de radio RF absoluto.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = inputData,
                    onValueChange = { inputData = it },
                    label = { Text("Mensaje a Emitir por Ultrasonido", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1B2A),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FRECUENCIA MARK (Bit 1):", color = Color.Gray, fontSize = 10.sp)
                            Text("${txState.markFreqHz} Hz", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FRECUENCIA SPACE (Bit 0):", color = Color.Gray, fontSize = 10.sp)
                            Text("${txState.spaceFreqHz} Hz", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TASA DE BAUDIOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${txState.baudRate} bps", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = {
                        if (txState.isTransmitting) transceiver.stopTransmission() else transceiver.transmitDataOverSound(inputData)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (txState.isTransmitting) Color(0xFFFF5252) else TacticalEmeraldSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (txState.isTransmitting) "Detener Emisión Acústica" else "Transmitir por Ultrasonido", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 13: Panel de Perímetro Geofence 3D (Safe Bubble) ---
@Composable
fun SafeBubble3DDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val radar = remember { SafeBubble3DProximityRadar.getInstance(context) }
    val bubbleState by radar.bubbleState.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Burbuja de Seguridad 3D", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Volumen de protección 3D (Horizontal + Altitud) con detección automática de brechas perimetrales no autorizadas.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (bubbleState.activeBreachesCount > 0) Color(0xFF2B0000) else Color.Black,
                    border = BorderStroke(1.dp, if (bubbleState.activeBreachesCount > 0) Color.Red else TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RADIO HORIZONTAL:", color = Color.Gray, fontSize = 10.sp)
                            Text("${bubbleState.radiusHorizontalMeters.toInt()} metros", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TECHO VERTICAL:", color = Color.Gray, fontSize = 10.sp)
                            Text("±${bubbleState.radiusVerticalMeters.toInt()} metros", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("INTRUSIONES ACTIVAS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${bubbleState.activeBreachesCount}", color = if (bubbleState.activeBreachesCount > 0) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                Text("CONTACTOS MONITOREADOS:", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Gray)
                bubbleState.monitoredTargets.forEach { target ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, if (target.isBreachingPerimeter) Color.Red else Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(target.callsign, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (target.isBreachingPerimeter) Color.Red else Color.White)
                                Text("Dist: ${target.distanceMeters.toInt()}m | Alt: ${target.relativeAltitudeMeters.toInt()}m | Azim: ${target.azimuthDegrees.toInt()}°", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text(if (target.isBreachingPerimeter) "BRECHA" else "SEGURO", color = if (target.isBreachingPerimeter) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { radar.simulateHostileIncursion() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simular Intrusión", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val newH = if (bubbleState.radiusHorizontalMeters == 50f) 100f else 50f
                            radar.updatePerimeterRadius(newH, 30f)
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Modificar Radio", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 14: Panel de Calculadora Balística Táctica ---
@Composable
fun BallisticsCalculatorDialog(onDismiss: () -> Unit) {
    var distance by remember { mutableStateOf("450") }
    var angle by remember { mutableStateOf("15") }
    var windSpeed by remember { mutableStateOf("4.5") }
    var solution by remember { mutableStateOf<com.example.domain.sensors.BallisticSolution?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.TrackChanges, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Calculadora Balística Táctica", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cálculo de elevación, deriva (windage), tiempo de vuelo y compensación por ángulo de inclinación de tiro (Rifleman's Rule).",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = distance,
                        onValueChange = { distance = it },
                        label = { Text("Distancia (m)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = angle,
                        onValueChange = { angle = it },
                        label = { Text("Ángulo (°)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = windSpeed,
                        onValueChange = { windSpeed = it },
                        label = { Text("Viento (m/s)", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        val d = distance.toFloatOrNull() ?: 300f
                        val a = angle.toFloatOrNull() ?: 0f
                        val w = windSpeed.toFloatOrNull() ?: 0f
                        solution = TacticalBallisticsCalculator.calculateSolution(d, a, w)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Calcular Solución de Fuego", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                if (solution != null) {
                    val s = solution!!
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black,
                        border = BorderStroke(1.dp, TacticalAmberTertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("ELEVACIÓN (MRAD):", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.2f MRAD (%.1f MOA)", s.elevationCorrectionMrad, s.elevationCorrectionMoa), color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("DERIVA VIENTO:", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.2f MRAD", s.windageCorrectionMrad), color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("CAÍDA PROYECTIL:", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.1f cm", s.bulletDropCm), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("TIEMPO DE VUELO:", color = Color.Gray, fontSize = 10.sp)
                                Text(String.format("%.3f s", s.timeOfFlightSeconds), color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// --- FASE 15: Panel de Red de Señuelos RF & Honeypot ---
@Composable
fun MeshHoneypotDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val generator = remember { MeshHoneypotGenerator.getInstance(context) }
    val decoys by generator.decoys.collectAsStateWithLifecycle()
    val isHoneypotActive by generator.isHoneypotActive.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FilterVintage, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Red de Señuelos RF Honeypot", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Emisión de identidades fantasma y trampas criptográficas para despistar sistemas SIGINT hostiles y registrar ataques de mapeo.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                decoys.forEach { decoy ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (decoy.isTrappingHostileScan) Color(0xFF2A1500) else Color(0xFF161B22),
                        border = BorderStroke(1.dp, if (decoy.isTrappingHostileScan) TacticalAmberTertiary else Color.DarkGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(decoy.fakeCallsign, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                Text("ID: ${decoy.id} | RSSI Simulado: ${decoy.simulatedRssi} dBm", fontSize = 9.sp, color = Color.Gray)
                            }
                            Text(if (decoy.probeCount > 0) "SONDEOS: ${decoy.probeCount}" else "EMITIENDO", color = if (decoy.probeCount > 0) TacticalAmberTertiary else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isHoneypotActive) generator.stopHoneypotNetwork() else generator.startHoneypotNetwork()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isHoneypotActive) Color(0xFFFF5252) else TacticalCyanPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isHoneypotActive) "Desactivar Señuelos" else "Activar Señuelos", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { generator.simulateHostileProbe() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simular Sondeo", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )
}

// ==========================================
// BLOQUE 4: FASES 16 A 20 — SIGINT & CIBERSEGURIDAD
// ==========================================

// --- FASE 16: Panel Detector de Ataques Sybil ---
@Composable
fun SybilDetectorDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val detector = remember { SybilMeshDetector.getInstance(context) }
    val state by detector.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFFF5252))
                Text("Detector de Ataques Sybil", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Inspección forense de capas PHY/MAC para descubrir clones de identidad y spoofing de firmas RF en la malla.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, if (state.activeThreats.isNotEmpty()) Color.Red else TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAQUETES INSPECCIONADOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.totalInspectedPackets}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AMENAZAS DETECTADAS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.activeThreats.size}", color = if (state.activeThreats.isNotEmpty()) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NODOS AISLADOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.isolatedNodesCount}", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                state.activeThreats.forEach { threat ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2B0B0B),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("NODO SOSPECHOSO: ${threat.suspectNodeId}", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(threat.threatLevel, color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Clones: ${threat.spoofedCallsigns.joinToString(", ")}", fontSize = 10.sp, color = Color.White)
                            Text("Varianza RSSI: ${threat.rssiVariance} dBm | Secuencias anómalas: ${threat.sequenceAnomalyCount}", fontSize = 9.sp, color = Color.Gray)
                            Button(
                                onClick = { detector.isolateThreat(threat.suspectNodeId) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("Aislar & Bloquear Nodo", fontSize = 10.sp, color = Color.White)
                            }
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { detector.simulateSybilAttack() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simular Ataque Sybil", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            if (state.isMonitoring) detector.stopInspection() else detector.startInspection()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (state.isMonitoring) "Pausar" else "Escanear", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 17: Panel Criptografía Post-Cuántica Kyber ---
@Composable
fun PostQuantumKyberDialog(onDismiss: () -> Unit) {
    var keyPair by remember { mutableStateOf<PqcKeyPair?>(null) }
    var encapsulationSecret by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Bóveda Post-Cuántica Kyber", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Criptografía híbrida NIST ML-KEM (Kyber-768 + X25519) resistente a computación cuántica futura.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Button(
                    onClick = {
                        val kp = PostQuantumKyberVault.generateHybridKeyPair()
                        keyPair = kp
                        val enc = PostQuantumKyberVault.encapsulateSecret(kp.publicKeyHex)
                        encapsulationSecret = enc.sharedSecretHex
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generar Llave Híbrida PQC", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                if (keyPair != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black,
                        border = BorderStroke(1.dp, TacticalCyanPrimary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("ID CLAVE: ${keyPair!!.keyId}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Text("ALGORITMO: ${keyPair!!.algorithm}", color = Color.White, fontSize = 10.sp)
                            Text("NIVEL SEGURIDAD: ${keyPair!!.securityLevelBits} bits (NIST Categoría 3)", color = TacticalEmeraldSecondary, fontSize = 10.sp)
                            Text("LLAVE PÚBLICA (SHA-512 Matrix Digest):", color = Color.Gray, fontSize = 9.sp)
                            Text(keyPair!!.publicKeyHex.take(32) + "...", color = Color.LightGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)

                            if (encapsulationSecret != null) {
                                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                                Text("SECRETO KEM POST-CUÁNTICO COMPARTIDO:", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 9.sp)
                                Text(encapsulationSecret!!.take(32) + "...", color = TacticalAmberTertiary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 18: Panel de Blockchain Táctica de Misión ---
@Composable
fun MissionBlockchainDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val blockchain = remember { TacticalMissionBlockchain.getInstance(context) }
    val chain by blockchain.chain.collectAsStateWithLifecycle()
    var newLogText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccountTree, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Micro-Blockchain Táctica Local", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Registro inmutable Proof-of-Authority para órdenes de misión, reportes de bajas y sellado forense.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = newLogText,
                        onValueChange = { newLogText = it },
                        label = { Text("Entrada de Misión a Sellar", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (newLogText.isNotBlank()) {
                                blockchain.appendLogEntry(newLogText, "OPERADOR-LÍDER")
                                newLogText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text("Sellar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Text("BLOQUES INMUTABLES REGISTRADOS (${chain.size}):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                chain.reversed().forEach { block ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("BLOQUE #${block.index}", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text("FIRMA: ${block.signerAuthorityCallsign}", color = Color.White, fontSize = 9.sp)
                            }
                            Text("Dato: ${block.payloadData}", color = Color.White, fontSize = 10.sp)
                            Text("Hash: ${block.blockHash.take(24)}...", color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 19: Panel de Huella Electromagnética LPI/LPD ---
@Composable
fun RfSignatureDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val meter = remember { RfEmissionSignatureMeter.getInstance(context) }
    val profile by meter.profile.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Sensors, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Huella Electromagnética (LPI/LPD)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Monitoreo de probabilidad de intercepción y detección de radiofrecuencia para evadir radiogoniometría enemiga.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PROBABILIDAD DETECCIÓN:", color = Color.Gray, fontSize = 10.sp)
                            Text("${profile.probabilityOfDetectionPercent}%", color = if (profile.probabilityOfDetectionPercent > 50) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NIVEL DE RIESGO:", color = Color.Gray, fontSize = 10.sp)
                            Text(profile.interceptRiskLevel, color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ALCANCE MÁXIMO DETECCIÓN:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f km", profile.maxDetectionRangeKm), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("POTENCIA TRANSMISIÓN TX:", color = Color.Gray, fontSize = 10.sp)
                            Text("${profile.currentTxPowerDbm} dBm", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = { meter.optimizeForStealth() },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Activar Perfil Sigilo LPI (Baja Detección)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 20: Panel de Silencio de Radio Absoluto EMCON Alpha ---
@Composable
fun EmconAlphaDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val manager = remember { EmconAlphaManager.getInstance(context) }
    val state by manager.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.VolumeOff, contentDescription = null, tint = if (state.currentLevel == EmconLevel.EMCON_ALPHA) Color.Red else TacticalCyanPrimary)
                Text("Control de Emisiones EMCON Alpha", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Bloqueo estricto de transmisiones RF para evitar detección en zonas de alto riesgo de guerra electrónica enemiga.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (state.currentLevel == EmconLevel.EMCON_ALPHA) Color(0xFF330000) else Color.Black,
                    border = BorderStroke(1.dp, if (state.currentLevel == EmconLevel.EMCON_ALPHA) Color.Red else TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NIVEL ACTUAL:", color = Color.Gray, fontSize = 10.sp)
                            Text(state.currentLevel.name, color = if (state.currentLevel == EmconLevel.EMCON_ALPHA) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TRANSMISIÓN RF:", color = Color.Gray, fontSize = 10.sp)
                            Text(if (state.passiveRxOnly) "PROHIBIDA (Solo RX)" else "HABILITADA", color = if (state.passiveRxOnly) Color.Red else TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ENLACE ACÚSTICO / LIFI:", color = Color.Gray, fontSize = 10.sp)
                            Text("AUTORIZADO (Sin RF)", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { manager.setEmconLevel(EmconLevel.EMCON_ALPHA) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("EMCON Alpha (Silencio Total)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                    Button(
                        onClick = { manager.setEmconLevel(EmconLevel.NORMAL_TRANSMIT) },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Normal (TX Activo)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// ==========================================
// BLOQUE 5: FASES 21 A 25 — RECONOCIMIENTO & MAPEO
// ==========================================

// --- FASE 21: Panel de Fotogrametría Aérea Local ---
@Composable
fun PhotogrammetryDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { TacticalPhotogrammetryEngine.getInstance(context) }
    val state by engine.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Fotogrametría Aérea Local", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Generación offline de ortomosaicos 2D y georreferenciación de imágenes capturadas por drones o reconocimiento.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FOTOGRAMAS PROCESADOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.processedTiles.size}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ÁREA TOTAL CUBIERTA:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.2f km²", state.totalCoveredAreaSqKm), color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RESOLUCIÓN DE TERRENO (GSD):", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.groundSamplingDistanceCm} cm/px", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val rLat = 19.4326 + (0..100).random() * 0.0001
                            val rLon = -99.1332 + (0..100).random() * 0.0001
                            engine.addAerialCapture(rLat, rLon, 125f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Añadir Captura", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { engine.stitchMosaic() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Compilar Mosaico", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 22: Panel de Telemetría MAVLink UAV ---
@Composable
fun MavlinkUavDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val transceiver = remember { MavlinkTelemetryTransceiver.getInstance(context) }
    val state by transceiver.state.collectAsStateWithLifecycle()
    val uav = state.activeUav

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.FlightTakeoff, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Telemetría MAVLink UAV Feed", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Recepción y control de telemetría de drones de reconocimiento ISR en tiempo real vía tramas MAVLink v2.0.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0C1929),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DRON IDENTIFICADOR:", color = Color.Gray, fontSize = 10.sp)
                            Text(uav.uavId, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MODO DE VUELO:", color = Color.Gray, fontSize = 10.sp)
                            Text(uav.flightMode, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("BATERÍA:", color = Color.Gray, fontSize = 10.sp)
                            Text("${uav.batteryRemainingPercent}%", color = if (uav.batteryRemainingPercent > 30) TacticalEmeraldSecondary else Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ALTITUD / VELOCIDAD:", color = Color.Gray, fontSize = 10.sp)
                            Text("${uav.altitudeAmslMeters}m AMSL | ${uav.groundSpeedMps} m/s", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RUMBO / COORDENADAS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${uav.headingDegrees}° | (${String.format("%.4f", uav.latitude)}, ${String.format("%.4f", uav.longitude)})", color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { transceiver.triggerRtl() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Comando RTL (Retorno)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { transceiver.updateWaypoint(uav.latitude + 0.001, uav.longitude + 0.001, 160f) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Nuevo Waypoint", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 23: Panel de Evacuación Médica 9-Line MEDEVAC & TCCC ---
@Composable
fun NineLineMedevacDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { TacticalMedevacEngine.getInstance(context) }
    val medevacs by engine.activeMedevacs.collectAsStateWithLifecycle()
    var gridLocation by remember { mutableStateOf("14Q NF 1944 4855") }
    var injurySummary by remember { mutableStateOf("1x URGENTE (Herida por esquirlas extremidad)") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color.Red)
                Text("9-Line MEDEVAC & Tarjeta TCCC", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Solicitud estandarizada de evacuación médica y registro de torniquetes bajo protocolo Tactical Combat Casualty Care.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = gridLocation,
                    onValueChange = { gridLocation = it },
                    label = { Text("Línea 1: Cuadrícula de Ubicación (MGRS)", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = injurySummary,
                    onValueChange = { injurySummary = it },
                    label = { Text("Línea 3: Pacientes por Precedencia", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val req = NineLineMedevacRequest(
                            line1LocationGrid = gridLocation,
                            line2RadioFrequencyCallsign = "44.100 MHz / DUSTOFF",
                            line3PatientsByPrecedence = injurySummary,
                            line4SpecialEquipmentRequired = "TORNIQUETE + CAMILLA",
                            line5PatientsByType = "1x CAMILLA",
                            line6SecurityAtPickupSite = "ZONA SEGURA",
                            line7MethodOfMarking = "HUMO VERDE",
                            line8PatientNationalityStatus = "AMIGO",
                            line9NbcTerrainObstacles = "DESPEJADO",
                            tourniquetTime = "T-14:30 UTC"
                        )
                        engine.submitMedevacRequest(req)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Transmitir Solicitud 9-Line MEDEVAC", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Text("SOLICITUDES ACTIVAS (${medevacs.size}):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                medevacs.forEach { m ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF2A0000),
                        border = BorderStroke(1.dp, Color.Red),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(m.reportId, color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(m.tourniquetTime, color = TacticalAmberTertiary, fontSize = 9.sp)
                            }
                            Text("Grid: ${m.line1LocationGrid} | Precedencia: ${m.line3PatientsByPrecedence}", color = Color.White, fontSize = 10.sp)
                            Text("Extracción: ${m.line7MethodOfMarking} | Frec: ${m.line2RadioFrequencyCallsign}", color = Color.LightGray, fontSize = 9.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 24: Panel de Brújula Solar & Efemérides ---
@Composable
fun SolarCompassDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val compass = remember { SolarEphemerisCompass.getInstance(context) }
    val data by compass.solarData.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.WbSunny, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Brújula Solar y Efemérides", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cálculo del Norte Verdadero mediante azimut solar astronómico en caso de bloqueo o guerra electrónica GNSS/Brújula.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("AZIMUT SOLAR ACTUAL:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f°", data.sunAzimuthDegrees), color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ELEVACIÓN SOLAR:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f°", data.sunElevationDegrees), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RUMBO NORTE VERDADERO:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f°", data.trueNorthCalculatedHeading), color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("MEDIODÍA SOLAR (UTC):", color = Color.Gray, fontSize = 10.sp)
                            Text(data.solarNoonUtcTime, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = { compass.updateObserverPosition(19.4326, -99.1332) },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recalcular con Posición Actual", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 25: Panel de Enlace Satelital SATCOM Gateway ---
@Composable
fun SatelliteGatewayDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val gateway = remember { SatelliteMeshGateway.getInstance(context) }
    val state by gateway.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SatelliteAlt, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Enlace Satelital SATCOM Gateway", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Detección y enrutamiento oportuno de paquetes DTN hacia constelaciones satelitales LEO en ventanas de visibilidad directa.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PAQUETES RETRANSMITIDOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.relayedPacketsCount}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PASOS ACTIVOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.activePasses.size} satélites", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                state.activePasses.forEach { pass ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0F1A24),
                        border = BorderStroke(1.dp, if (pass.isDirectLineOfSight) TacticalEmeraldSecondary else Color.Gray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(pass.constellationName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                Text(if (pass.isDirectLineOfSight) "LÍNEA DIRECTA" else "EN APROXIMACIÓN", color = if (pass.isDirectLineOfSight) TacticalEmeraldSecondary else TacticalAmberTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Elevación: ${pass.elevationAngleDeg}° | Ventana restante: ${pass.windowRemainingSeconds}s", color = Color.LightGray, fontSize = 9.sp)
                            Button(
                                onClick = { gateway.flushQueueToSatellite(pass.constellationName) },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Text("Transmitir Paquetes en Cola (${pass.queuedPacketsCount})", fontSize = 10.sp, color = Color.Black)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// ==========================================
// BLOQUE 6: FASES 26 A 30 — FUSIÓN C4ISR & MANDO
// ==========================================

// --- FASE 26: Panel de Enmascarador Acústico y Análisis Espectral ---
@Composable
fun VoiceSpectralDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { TacticalVoiceSpectralEngine.getInstance(context) }
    val data by engine.spectralData.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TacticalEmeraldSecondary)
                Text("Enmascarador Acústico Espectral", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Inversión de frecuencia de voz (3.3 kHz) y análisis FFT en tiempo real para evitar interceptación acústica táctica.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ENMASCARADOR (SCRAMBLER):", color = Color.Gray, fontSize = 10.sp)
                            Text(if (data.isScramblerActive) "ACTIVADO (Inversión 3.3 kHz)" else "DESACTIVADO", color = if (data.isScramblerActive) TacticalEmeraldSecondary else Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RUIDO AMBIENTE:", color = Color.Gray, fontSize = 10.sp)
                            Text(String.format("%.1f dBFS", data.ambientNoiseLevelDbfs), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("RELACIÓN SNR:", color = Color.Gray, fontSize = 10.sp)
                            Text("${data.snrRatioDb} dB", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Barras de espectro FFT visuales
                Text("ESPECTRO DE FRECUENCIA FFT:", color = Color.Gray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
                    data.fftFrequencyBands.forEach { band ->
                        Box(
                            modifier = Modifier
                                .width(16.dp)
                                .fillMaxHeight(band)
                                .background(TacticalEmeraldSecondary, RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { engine.toggleVoiceScrambler() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (data.isScramblerActive) Color.Red else TacticalEmeraldSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (data.isScramblerActive) "Desactivar Scrambler" else "Activar Scrambler", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { engine.sampleSpectralAudio() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Muestrear Audio", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 27: Panel de Enrutador Táctico A* ---
@Composable
fun AStarRouterDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val router = remember { TacticalAStarRouter.getInstance(context) }
    val plan by router.currentPlan.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Navigation, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Enrutador Táctico de Escape A*", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Cálculo heurístico de rutas óptimas de escape (E&E) y evasión de áreas de peligro u hostiles sin internet.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("PLAN: ${plan.routeId}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("ORIGEN: ${plan.origin}", color = Color.LightGray, fontSize = 9.sp)
                        Text("EXTRACCIÓN: ${plan.extractionPoint}", color = TacticalEmeraldSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DISTANCIA TOTAL:", color = Color.Gray, fontSize = 10.sp)
                            Text("${plan.totalEstimatedDistanceKm} km", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PUNTAJE DE RIESGO:", color = Color.Gray, fontSize = 10.sp)
                            Text("${(plan.routeRiskScore * 100).toInt()}%", color = if (plan.routeRiskScore < 0.2f) TacticalEmeraldSecondary else Color.Red, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("TIEMPO ESTIMADO EGRESO:", color = Color.Gray, fontSize = 10.sp)
                            Text("${plan.recommendedEgressTimeMinutes} min", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { router.recalculateEscapeRoute(avoidHostileZone = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ruta Segura (Bajo Riesgo)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = { router.recalculateEscapeRoute(avoidHostileZone = false) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Ruta Rápida Directa", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 28: Panel de Informes SALUTE & INTREP ---
@Composable
fun SaluteIntelDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val engine = remember { SaluteIntelReportEngine.getInstance(context) }
    val reports by engine.reports.collectAsStateWithLifecycle()
    var targetActivity by remember { mutableStateOf("Patrulla a pie explorando perímetro este") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Assignment, contentDescription = null, tint = TacticalAmberTertiary)
                Text("Informes de Inteligencia SALUTE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Estructuración de reportes de inteligencia militar bajo formato SALUTE (Size, Activity, Location, Unit, Time, Equipment).",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                OutlinedTextField(
                    value = targetActivity,
                    onValueChange = { targetActivity = it },
                    label = { Text("Actividad Observada", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val rep = SaluteReport(
                            size = "4x Elementos hostiles",
                            activity = targetActivity,
                            location = "14Q NF 1920 4880",
                            uniformUnit = "Uniforme de campaña sin distintivos",
                            timeObserved = "14:40 UTC",
                            equipment = "Fusiles de asalto y binoculares térmicos"
                        )
                        engine.submitSaluteReport(rep)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Transmitir Informe SALUTE", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Text("INFORMES EN MALLA (${reports.size}):", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                reports.forEach { r ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1F1A12),
                        border = BorderStroke(1.dp, TacticalAmberTertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(r.reportId, color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                                Text(r.timeObserved, color = Color.LightGray, fontSize = 9.sp)
                            }
                            Text("Tamaño: ${r.size} | Loc: ${r.location}", color = Color.White, fontSize = 10.sp)
                            Text("Actividad: ${r.activity}", color = Color.LightGray, fontSize = 9.sp)
                            Text("Equipo: ${r.equipment}", color = Color.Gray, fontSize = 9.sp)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 29: Panel de Sincronización de Reloj Atómico PPS ---
@Composable
fun MeshClockSyncDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val synchronizer = remember { MeshTimeSynchronizer.getInstance(context) }
    val state by synchronizer.state.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AccessTime, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Sincronización Reloj Atómico PPS", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Sincronización temporal sub-microsegundo para saltos FHSS coordinados y sellado criptográfico en malla.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black,
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ESTADO DEL RELOJ:", color = Color.Gray, fontSize = 10.sp)
                            Text(if (state.isClockSynced) "SINCRONIZADO (<5µs)" else "DESALINEADO", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("FUENTE DE RELOJ:", color = Color.Gray, fontSize = 10.sp)
                            Text(state.clockSource, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DESVIACIÓN ESTIMADA:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.estimatedDriftMicroseconds} microsegundos", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NODOS SINCRONIZADOS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${state.totalSyncedNodes} nodos de malla", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                Button(
                    onClick = { synchronizer.triggerMeshClockResync() },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Resincronizar Relojes en Malla (PPS Pulse)", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 30: Panel de Consola Maestra C4ISR ---
@Composable
fun C4IsrMasterConsoleDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val dashboard = remember { C4IsrMasterDashboardEngine.getInstance(context) }
    val summary by dashboard.summary.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.DashboardCustomize, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Centro de Mando Maestro C4ISR", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Visión unificada y holística de los 30 subsistemas tácticos, de combate, sensores y comunicaciones de OmniComm.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF071B26),
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("DISPONIBILIDAD OPERACIONAL:", color = Color.Gray, fontSize = 10.sp)
                            Text("${summary.operationalReadinessPercent}% (30/30)", color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("ESTADO DE LA MALLA:", color = Color.Gray, fontSize = 10.sp)
                            Text(summary.meshHealthStatus, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CONDICIÓN DEFCON:", color = Color.Gray, fontSize = 10.sp)
                            Text(summary.threatLevel, color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("SEGURIDAD POST-CUÁNTICA:", color = Color.Gray, fontSize = 10.sp)
                            Text(summary.postQuantumSecurityStatus, color = TacticalEmeraldSecondary, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                        }
                    }
                }

                Button(
                    onClick = { dashboard.runGlobalSystemAudit() },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ejecutar Auditoría Global de 31 Módulos", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// --- FASE 31: Panel de Control Externo API REST & Consola Web PC ---
@Composable
fun ExternalApiServerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val server = remember { com.example.domain.remote.TacticalExternalControlServer.getInstance(context) }
    val serverState by server.serverState.collectAsStateWithLifecycle()
    var cliInput by remember { mutableStateOf("help") }
    var cliOutput by remember { mutableStateOf("Escribe un comando o presiona 'Ejecutar CLI'...") }
    val coroutineScope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Lan, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Control Externo API REST & Consola PC", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Servidor HTTP embebido en puerto ${serverState.port}. Permite el control del 100% de la aplicación, sensores y hardware desde una computadora conectada por Wi-Fi o USB (ADB).",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )

                // Tarjeta de Estado del Servidor
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0D1826),
                    border = BorderStroke(1.dp, if (serverState.isRunning) TacticalEmeraldSecondary else Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("ESTADO DEL SERVIDOR:", color = Color.Gray, fontSize = 10.sp)
                            Surface(shape = RoundedCornerShape(4.dp), color = if (serverState.isRunning) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.Red.copy(alpha = 0.2f)) {
                                Text(
                                    if (serverState.isRunning) "EN LÍNEA (ACTIVO)" else "DETENIDO",
                                    color = if (serverState.isRunning) TacticalEmeraldSecondary else Color.Red,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("IP LAN & PUERTO:", color = Color.Gray, fontSize = 10.sp)
                            Text("${serverState.localIp}:${serverState.port}", color = TacticalCyanPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PETICIONES ATENDIDAS:", color = Color.Gray, fontSize = 10.sp)
                            Text("${serverState.totalRequests} requests", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CONSOLA WEB:", color = Color.Gray, fontSize = 10.sp)
                            Text("http://${serverState.localIp}:${serverState.port}/console", color = Color.White, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Botones de inicio / parada
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (!serverState.isRunning) server.startServer() else server.stopServer() },
                        colors = ButtonDefaults.buttonColors(containerColor = if (serverState.isRunning) Color(0xFF8B0000) else TacticalEmeraldSecondary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (serverState.isRunning) "Detener Servidor" else "Iniciar Servidor", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                // Instrucciones de Conexión ADB / USB
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Acceso por Cable USB (ADB Port Forward):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)
                        Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFF0D1117), modifier = Modifier.fillMaxWidth()) {
                            Text("adb forward tcp:9090 tcp:9090\ncurl http://localhost:9090/api/v1/status\nhttp://localhost:9090/console", fontSize = 9.sp, color = TacticalCyanPrimary, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(6.dp))
                        }
                    }
                }

                // Terminal CLI Remoto Rápido
                Text("Consola CLI de Control Táctico:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                OutlinedTextField(
                    value = cliInput,
                    onValueChange = { cliInput = it },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    placeholder = { Text("Ej: ping, flash 5, vibrate 300, salute, status, help", fontSize = 10.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyanPrimary,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                Button(
                    onClick = {
                        val res = server.executeTacticalCliCommand(cliInput)
                        cliOutput = res
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ejecutar Comando CLI", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0A0E14),
                    border = BorderStroke(1.dp, Color(0xFF21262D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(cliOutput, fontSize = 9.sp, color = TacticalEmeraldSecondary, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(8.dp))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

