package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.hardware.AdaptivePowerProfileManager
import com.example.domain.hardware.PowerProfile
import com.example.domain.hardware.TacticalManDownDetector
import com.example.domain.p2p.PacketPriority
import com.example.domain.p2p.StoreAndForwardRouter
import com.example.domain.security.DuressAndSecurityManager
import com.example.domain.security.MessageTtlOption
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

enum class EnhancementsTab(val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    SECURITY("Zero-Trust", Icons.Default.Security),
    MISSIONS("Misiones", Icons.Default.Bolt),
    QOS_ROUTING("Multi-Salto", Icons.Default.AltRoute),
    MAN_DOWN("Man Down", Icons.Default.Warning),
    POWER("Batería", Icons.Default.BatteryChargingFull)
}

@Composable
fun TacticalSecurityAndEnhancementsDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(EnhancementsTab.SECURITY) }

    val duressManager = remember { DuressAndSecurityManager.getInstance(context) }
    val isDuressActive by duressManager.isDuressActive.collectAsStateWithLifecycle()
    val selectedTtl by duressManager.selectedMessageTtl.collectAsStateWithLifecycle()
    val ratchetState by duressManager.ratchetState.collectAsStateWithLifecycle()

    val router = remember { StoreAndForwardRouter.getInstance() }
    val queuedPackets by router.queuedPacketsCount.collectAsStateWithLifecycle()
    val relayedPackets by router.relayedPacketsCount.collectAsStateWithLifecycle()
    val qosDropCount by router.qosDropCount.collectAsStateWithLifecycle()
    val recentPackets by router.recentRelayedPackets.collectAsStateWithLifecycle()
    val custodyTransfers by router.custodyTransfersCount.collectAsStateWithLifecycle()
    val antiEntropySyncs by router.antiEntropySyncCount.collectAsStateWithLifecycle()

    val manDownDetector = remember { TacticalManDownDetector.getInstance(context) }
    val isMonitoringActive by manDownDetector.isMonitoringActive.collectAsStateWithLifecycle()

    val powerManager = remember { AdaptivePowerProfileManager.getInstance(context) }
    val currentPowerProfile by powerManager.currentProfile.collectAsStateWithLifecycle()
    val batteryLevel by powerManager.batteryLevel.collectAsStateWithLifecycle()
    val isEcoForced by powerManager.isEcoModeForced.collectAsStateWithLifecycle()

    var masterPinInput by remember { mutableStateOf(duressManager.getMasterPin()) }
    var duressPinInput by remember { mutableStateOf(duressManager.getDuressPin()) }
    var showPinSavedToast by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = TacticalCyanPrimary)
                        Text(
                            "Mejoras & Protocolos Tácticos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                    }
                }

                // Tabs de navegación
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = Color(0xFF161B22),
                    contentColor = TacticalCyanPrimary
                ) {
                    EnhancementsTab.values().forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            text = { Text(tab.title, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }

                // Contenido de la pestaña activa
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    when (selectedTab) {
                        EnhancementsTab.SECURITY -> {
                            SecurityTabContent(
                                duressManager = duressManager,
                                isDuressActive = isDuressActive,
                                selectedTtl = selectedTtl,
                                ratchetState = ratchetState,
                                masterPin = masterPinInput,
                                duressPin = duressPinInput,
                                onMasterPinChange = { masterPinInput = it; duressManager.setMasterPin(it) },
                                onDuressPinChange = { duressPinInput = it; duressManager.setDuressPin(it) }
                            )
                        }
                        EnhancementsTab.MISSIONS -> {
                            TacticalMissionsTabContent(context = context)
                        }
                        EnhancementsTab.QOS_ROUTING -> {
                            QosRoutingTabContent(
                                queuedCount = queuedPackets,
                                relayedCount = relayedPackets,
                                dropCount = qosDropCount,
                                custodyCount = custodyTransfers,
                                antiEntropyCount = antiEntropySyncs,
                                recentPackets = recentPackets,
                                onTestPacket = {
                                    router.enqueueLocalPacket(
                                        sourceId = "LOCAL_NODE",
                                        destinationId = "BROADCAST_ALL",
                                        priority = PacketPriority.HIGH_PRIORITY_TEXT,
                                        payloadType = "PING_QOS",
                                        data = "Ping de telemetría de ruta multi-salto",
                                        ttlHops = 4
                                    )
                                },
                                onSyncAntiEntropy = {
                                    val summary = router.createAntiEntropySummary("LOCAL_NODE")
                                    router.getBundlesToSyncWithPeer(summary)
                                }
                            )
                        }
                        EnhancementsTab.MAN_DOWN -> {
                            ManDownTabContent(
                                isMonitoringActive = isMonitoringActive,
                                onToggleMonitoring = {
                                    if (isMonitoringActive) manDownDetector.stopMonitoring() else manDownDetector.startMonitoring()
                                },
                                onSimulateImpact = {
                                    manDownDetector.triggerEmergencyCountdown("Simulación de Caída Táctica / Impacto G-Spike")
                                }
                            )
                        }
                        EnhancementsTab.POWER -> {
                            PowerTabContent(
                                currentProfile = currentPowerProfile,
                                batteryLevel = batteryLevel,
                                isEcoForced = isEcoForced,
                                onSelectProfile = { powerManager.setProfile(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityTabContent(
    duressManager: DuressAndSecurityManager,
    isDuressActive: Boolean,
    selectedTtl: MessageTtlOption,
    ratchetState: com.example.domain.security.RatchetSessionState,
    masterPin: String,
    duressPin: String,
    onMasterPinChange: (String) -> Unit,
    onDuressPinChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Sección 1: Modo Coacción (Duress PIN)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (isDuressActive) TacticalAmberTertiary else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = TacticalAmberTertiary)
                    Text("PIN de Coacción (Modo Pánico)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Text(
                    "Si el operador es forzado a desbloquear la app, ingresar el PIN de coacción muestra datos señuelo, purga claves criptográficas en memoria y emite una alerta silenciosa al equipo.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = masterPin,
                        onValueChange = onMasterPinChange,
                        label = { Text("PIN Maestro", fontSize = 10.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalCyanPrimary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    OutlinedTextField(
                        value = duressPin,
                        onValueChange = onDuressPinChange,
                        label = { Text("PIN Coacción", fontSize = 10.sp, color = TacticalAmberTertiary) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalAmberTertiary,
                            unfocusedBorderColor = Color.Gray,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }

                if (isDuressActive) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = TacticalAmberTertiary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TacticalAmberTertiary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("MODO SEÑUELO ACTIVO", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            Button(
                                onClick = { duressManager.exitDuressMode() },
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary)
                            ) {
                                Text("Restaurar Normal", color = Color.Black, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sección 2: Destrucción Programada (Burn-on-Read / TTL)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = TacticalAmberTertiary)
                    Text("Auto-Destrucción de Mensajes (TTL)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
                Text("Elimina automáticamente los mensajes leídos tras expirar el temporizador:", color = Color.LightGray, fontSize = 11.sp)

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MessageTtlOption.values().forEach { option ->
                        val isSelected = (selectedTtl == option)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.15f) else Color(0xFF0D1117),
                            border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color.DarkGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { duressManager.setMessageTtl(option) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(option.label, color = if (isSelected) TacticalCyanPrimary else Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sección 3: Double Ratchet & Forward Secrecy
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = TacticalEmeraldSecondary)
                        Text("Double Ratchet Protocol", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    TextButton(onClick = { duressManager.rotateRatchetKeys() }) {
                        Text("Rotar Clave", color = TacticalEmeraldSecondary, fontSize = 11.sp)
                    }
                }
                Text("Garantiza Perfect Forward Secrecy rotando claves efímeras ECDH en cada mensaje.", color = Color.LightGray, fontSize = 11.sp)

                Surface(shape = RoundedCornerShape(6.dp), color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Root Key: ${ratchetState.rootKeyFingerprint}", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Cadena de Trinquete: Paso #${ratchetState.chainStep}", color = Color.LightGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        Text("Clave Efímera: ${ratchetState.ephemeralDhKey}", color = TacticalCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

@Composable
private fun QosRoutingTabContent(
    queuedCount: Int,
    relayedCount: Int,
    dropCount: Int,
    custodyCount: Int,
    antiEntropyCount: Int,
    recentPackets: List<com.example.domain.p2p.MeshRelayPacket>,
    onTestPacket: () -> Unit,
    onSyncAntiEntropy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            StatMetricBox(title = "En Cola (Room)", value = "$queuedCount", color = TacticalCyanPrimary, modifier = Modifier.weight(1f))
            StatMetricBox(title = "Custodia DTN", value = "$custodyCount", color = TacticalEmeraldSecondary, modifier = Modifier.weight(1f))
            StatMetricBox(title = "Vector Sync", value = "$antiEntropyCount", color = Color(0xFFB388FF), modifier = Modifier.weight(1f))
            StatMetricBox(title = "Drops QoS", value = "$dropCount", color = TacticalAmberTertiary, modifier = Modifier.weight(1f))
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onTestPacket,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Emitir DTN", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = onSyncAntiEntropy,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TacticalEmeraldSecondary),
                border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Sync, contentDescription = null, tint = TacticalEmeraldSecondary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Anti-Entropy Sync", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Text("Jerarquía de Calidad de Servicio (QoS):", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PacketPriority.values().forEach { priority ->
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(priority.badgeColorHex).copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = CircleShape, color = Color(priority.badgeColorHex), modifier = Modifier.size(8.dp)) {}
                            Text(priority.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("Nivel ${priority.value}", color = Color(priority.badgeColorHex), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManDownTabContent(
    isMonitoringActive: Boolean,
    onToggleMonitoring: () -> Unit,
    onSimulateImpact: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (isMonitoringActive) TacticalEmeraldSecondary else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = TacticalEmeraldSecondary)
                        Text("Monitoreo de Impacto / Caída", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                    }
                    Switch(
                        checked = isMonitoringActive,
                        onCheckedChange = { onToggleMonitoring() },
                        colors = SwitchDefaults.colors(checkedThumbColor = TacticalEmeraldSecondary)
                    )
                }

                Text(
                    "Utiliza el acelerómetro para detectar fuerzas de impacto superiores a 3.2G seguidas de inmovilidad física. Tras una cuenta regresiva de 10 segundos, emite una baliza MAYDAY con coordenadas a toda la red.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                Button(
                    onClick = onSimulateImpact,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Sensors, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simular Impacto Físico (Test 10s Countdown)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun PowerTabContent(
    currentProfile: PowerProfile,
    batteryLevel: Int,
    isEcoForced: Boolean,
    onSelectProfile: (PowerProfile) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Nivel de Batería del Dispositivo", color = Color.LightGray, fontSize = 11.sp)
                    Text("$batteryLevel%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
                }
                if (isEcoForced) {
                    Surface(shape = RoundedCornerShape(6.dp), color = TacticalAmberTertiary.copy(alpha = 0.2f)) {
                        Text("ECO-MESH AUTO (<20%)", color = TacticalAmberTertiary, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(6.dp))
                    }
                }
            }
        }

        Text("Seleccionar Perfil de Radiofrecuencia:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PowerProfile.values().forEach { profile ->
                val isSelected = (currentProfile == profile)
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.12f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color.DarkGray),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectProfile(profile) }
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(profile.title, color = if (isSelected) TacticalCyanPrimary else Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${profile.estimatedMilliwatts} mW • ${profile.dutyCyclePercent}% Duty", color = TacticalEmeraldSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(profile.description, color = Color.LightGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMetricBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFF161B22),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, color = Color.LightGray, fontSize = 10.sp)
            Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun TacticalMissionsTabContent(context: android.content.Context) {
    val shotClassifier = remember { com.example.domain.media.AcousticShotClassifier.getInstance(context) }
    val shotMonitoring by shotClassifier.isMonitoring.collectAsStateWithLifecycle()
    val ambientDb by shotClassifier.ambientSoundLevelDb.collectAsStateWithLifecycle()
    val lastShotEvent by shotClassifier.lastDetectedEvent.collectAsStateWithLifecycle()

    val ultrasonicTransceiver = remember { com.example.domain.audio.UltrasonicDataLinkTransceiver.getInstance(context) }
    val ultrasonicState by ultrasonicTransceiver.state.collectAsStateWithLifecycle()
    var ultrasonicInputText by remember { mutableStateOf("SITREP_ALPHA_OK") }

    val pdrEngine = remember { com.example.domain.sensors.PedestrianDeadReckoningEngine.getInstance(context) }
    val pdrState by pdrEngine.pdrState.collectAsStateWithLifecycle()
    val isPdrTracking by pdrEngine.isTracking.collectAsStateWithLifecycle()

    val barometerEngine = remember { com.example.domain.sensors.BarometerStormAlertEngine.getInstance(context) }
    val atmosphere by barometerEngine.atmosphereState.collectAsStateWithLifecycle()
    val isBarometerMonitoring by barometerEngine.isMonitoring.collectAsStateWithLifecycle()

    // Estado Calculadora Balística
    var targetDistMeters by remember { mutableStateOf(400f) }
    var inclinationAngleDeg by remember { mutableStateOf(0f) }
    var crosswindMps by remember { mutableStateOf(3.5f) }
    val ballisticSol = remember(targetDistMeters, inclinationAngleDeg, crosswindMps) {
        com.example.domain.sensors.TacticalBallisticsCalculator.calculateSolution(
            distanceM = targetDistMeters,
            angleDeg = inclinationAngleDeg,
            windSpeedMps = crosswindMps
        )
    }

    // Estado Shamir Secret Sharing
    var originalSecret by remember { mutableStateOf("ALPHA-OMEGA-9942") }
    var shamirShares by remember { mutableStateOf<List<com.example.domain.security.ShamirShare>>(emptyList()) }
    var reconstructedSecretText by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // --- PANEL 1: DETECTOR ACÚSTICO DE DISPAROS & ONDA DE CHOQUE ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (shotMonitoring) TacticalEmeraldSecondary.copy(alpha = 0.5f) else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = null, tint = TacticalAmberTertiary)
                        Text("1. Detector Acústico de Disparos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (shotMonitoring) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.DarkGray
                    ) {
                        Text(
                            text = if (shotMonitoring) "VIGILANCIA ACTIVA" else "STANDBY",
                            color = if (shotMonitoring) TacticalEmeraldSecondary else Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("NIVEL AMBIENTE", "${ambientDb.toInt()} dB", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox(
                        "ÚLTIMO EVENTO",
                        lastShotEvent?.let { "${it.estimatedDb.toInt()}dB (${(it.confidence * 100).toInt()}%)" } ?: "NINGUNO",
                        if (lastShotEvent != null) TacticalAmberTertiary else Color.Gray,
                        Modifier.weight(1f)
                    )
                }

                if (lastShotEvent != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TacticalAmberTertiary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TacticalAmberTertiary.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "DETECCIÓN: ${lastShotEvent?.eventType} • Confianza: ${((lastShotEvent?.confidence ?: 0f) * 100).toInt()}%",
                            color = TacticalAmberTertiary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (shotMonitoring) shotClassifier.stopAcousticSurveillance() else shotClassifier.startAcousticSurveillance()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (shotMonitoring) Color(0xFF21262D) else TacticalEmeraldSecondary
                        )
                    ) {
                        Text(if (shotMonitoring) "Detener Escucha" else "Activar Micrófono", fontSize = 11.sp, color = if (shotMonitoring) Color.White else Color.Black)
                    }

                    OutlinedButton(
                        onClick = {
                            shotClassifier.triggerTestAcousticImpulse("DISPARO_CALIBRE_LIGERO", 95.2f)
                        },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalAmberTertiary)
                    ) {
                        Text("Simular Disparo", fontSize = 11.sp, color = TacticalAmberTertiary)
                    }
                }
            }
        }

        // --- PANEL 2: ENLACE ACÚSTICO DATA-OVER-SOUND (18.5/19.5 kHz FSK) ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (ultrasonicState.isTransmitting) TacticalCyanPrimary else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Hearing, contentDescription = null, tint = TacticalCyanPrimary)
                        Text("2. Enlace Acústico Ultrasonido (FSK)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Text("18.5 kHz / 50 Baud", color = TacticalCyanPrimary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }

                Text(
                    "Transmite datos mediante ráfagas acústicas inaudibles fuera del espectro auditivo humano para operaciones en silencio de radio RF.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = ultrasonicInputText,
                    onValueChange = { ultrasonicInputText = it },
                    label = { Text("Payload a transmitir", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Button(
                    onClick = { ultrasonicTransceiver.transmitDataOverSound(ultrasonicInputText) },
                    enabled = !ultrasonicState.isTransmitting && ultrasonicInputText.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (ultrasonicState.isTransmitting) "Emitiendo Ráfaga FSK Inaudible..." else "Emitir Paquete Acústico FSK",
                        color = Color.Black,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // --- PANEL 3: NAVEGACIÓN INERCIAL PDR (GPS-DENIED) ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (isPdrTracking) TacticalEmeraldSecondary.copy(alpha = 0.5f) else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.DirectionsWalk, contentDescription = null, tint = TacticalEmeraldSecondary)
                        Text("3. Navegación Inercial sin GPS (PDR)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPdrTracking) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.DarkGray
                    ) {
                        Text(
                            text = if (isPdrTracking) "RASTREO ACTIVO" else "DETENIDO",
                            color = if (isPdrTracking) TacticalEmeraldSecondary else Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("PASOS", "${pdrState.stepCount}", TacticalEmeraldSecondary, Modifier.weight(1f))
                    StatMetricBox("DISTANCIA", String.format("%.1f m", pdrState.totalDistanceMeters), TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("RUMBO", "${pdrState.currentHeadingDegrees.toInt()}°", TacticalAmberTertiary, Modifier.weight(1f))
                }

                Text(
                    "Posición Relativa respecto al origen: ΔX = ${String.format("%.1f", pdrState.relativeX)}m, ΔY = ${String.format("%.1f", pdrState.relativeY)}m",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isPdrTracking) pdrEngine.stopPdrTracking() else pdrEngine.startPdrTracking()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPdrTracking) Color(0xFF21262D) else TacticalEmeraldSecondary)
                    ) {
                        Text(if (isPdrTracking) "Detener PDR" else "Iniciar Sensores PDR", fontSize = 11.sp, color = if (isPdrTracking) Color.White else Color.Black)
                    }

                    OutlinedButton(
                        onClick = { pdrEngine.simulateStep() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                    ) {
                        Text("Simular Zancada", fontSize = 11.sp, color = TacticalEmeraldSecondary)
                    }
                }
            }
        }

        // --- PANEL 4: CALCULADORA BALÍSTICA TÁCTICA ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = null, tint = TacticalCyanPrimary)
                    Text("4. Solución Balística Mil-Dot / MOA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text("Distancia Objetivo: ${targetDistMeters.toInt()} metros", color = Color.White, fontSize = 11.sp)
                Slider(
                    value = targetDistMeters,
                    onValueChange = { targetDistMeters = it },
                    valueRange = 100f..1200f,
                    steps = 10
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("CAÍDA BALÍSTICA", String.format("%.1f cm", ballisticSol.bulletDropCm), TacticalAmberTertiary, Modifier.weight(1f))
                    StatMetricBox("ELEVACIÓN", String.format("%.2f MIL", ballisticSol.elevationCorrectionMrad), TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("TIEMPO VUELO", String.format("%.2f s", ballisticSol.timeOfFlightSeconds), TacticalEmeraldSecondary, Modifier.weight(1f))
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF0D1117),
                    border = BorderStroke(1.dp, Color.DarkGray),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Solución de Visor:", color = TacticalCyanPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "• Elevación: +${String.format("%.2f", ballisticSol.elevationCorrectionMoa)} MOA (${String.format("%.2f", ballisticSol.elevationCorrectionMrad)} MRAD)\n" +
                            "• Corrección Viento: ${String.format("%.2f", ballisticSol.windageCorrectionMrad)} MRAD\n" +
                            "• Velocidad Residual: ${ballisticSol.remainingVelocityMps.toInt()} m/s",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // --- PANEL 5: BÓVEDA SHAMIR SECRET SHARING (K-DE-N) ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalAmberTertiary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = TacticalAmberTertiary)
                    Text("5. Bóveda Criptográfica Shamir (K-de-N)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Text(
                    "Divide un secreto crítico en 3 fragmentos matemáticos. Se requiere el quórum de al menos 2 operadores para reconstruir la clave maestra.",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )

                OutlinedTextField(
                    value = originalSecret,
                    onValueChange = { originalSecret = it },
                    label = { Text("Clave Maestra Secreta", fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            try {
                                shamirShares = com.example.domain.security.ShamirSecretSharingVault.splitSecret(
                                    secretBytes = originalSecret.toByteArray(Charsets.UTF_8),
                                    totalPartsN = 3,
                                    thresholdK = 2
                                )
                                reconstructedSecretText = ""
                            } catch (_: Throwable) {}
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalAmberTertiary)
                    ) {
                        Text("Dividir (3 Partes)", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                if (shamirShares.size >= 2) {
                                    val subset = shamirShares.take(2)
                                    val recovered = com.example.domain.security.ShamirSecretSharingVault.reconstructSecret(
                                        shares = subset,
                                        thresholdK = 2
                                    )
                                    if (recovered != null) {
                                        reconstructedSecretText = String(recovered, Charsets.UTF_8)
                                    } else {
                                        reconstructedSecretText = "Error: no se pudo reconstruir quórum"
                                    }
                                }
                            } catch (e: Exception) {
                                reconstructedSecretText = "Error: ${e.message}"
                            }
                        },
                        enabled = shamirShares.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                    ) {
                        Text("Reconstruir (K=2)", color = TacticalEmeraldSecondary, fontSize = 11.sp)
                    }
                }

                if (shamirShares.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        shamirShares.forEach { share ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF0D1117),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    "Parte ${share.index}: ${share.shareDataHex.take(16)}...",
                                    color = TacticalCyanPrimary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                        }
                    }
                }

                if (reconstructedSecretText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "SECRETO RECONSTRUIDO CON ÉXITO:\n$reconstructedSecretText",
                            color = TacticalEmeraldSecondary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // --- PANEL 6: BARÓMETRO & ALERTA DE TORMENTA ---
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (atmosphere.stormAlertActive) TacticalAmberTertiary else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = TacticalCyanPrimary)
                        Text("6. Barómetro Táctico & Alerta Tormenta", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (atmosphere.stormAlertActive) TacticalAmberTertiary.copy(alpha = 0.2f) else TacticalEmeraldSecondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (atmosphere.stormAlertActive) "ALERTA TORMENTA" else "ESTABLE",
                            color = if (atmosphere.stormAlertActive) TacticalAmberTertiary else TacticalEmeraldSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("PRESIÓN", "${atmosphere.pressureHpa.toInt()} hPa", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("ALTITUD", "${atmosphere.estimatedAltitudeMeters.toInt()} m", TacticalEmeraldSecondary, Modifier.weight(1f))
                    StatMetricBox("TENDENCIA", "${atmosphere.pressureTrendHpaPerHr} hPa/h", if (atmosphere.stormAlertActive) TacticalAmberTertiary else TacticalEmeraldSecondary, Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isBarometerMonitoring) barometerEngine.stopBarometerMonitoring() else barometerEngine.startBarometerMonitoring()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isBarometerMonitoring) Color(0xFF21262D) else TacticalCyanPrimary)
                    ) {
                        Text(if (isBarometerMonitoring) "Detener Barómetro" else "Iniciar Barómetro", fontSize = 11.sp, color = if (isBarometerMonitoring) Color.White else Color.Black)
                    }

                    OutlinedButton(
                        onClick = { barometerEngine.simulateStormPressureDrop() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalAmberTertiary)
                    ) {
                        Text("Simular Frente Tormenta", fontSize = 11.sp, color = TacticalAmberTertiary)
                    }
                }
            }
        }

        // --- PANEL 7: SALTO DE FRECUENCIA FHSS (ANTI-JAMMING & LPI) ---
        val fhssEngine = remember { com.example.domain.p2p.FrequencyHoppingMeshEngine.getInstance(context) }
        val hopStatus by fhssEngine.hopStatus.collectAsStateWithLifecycle()
        val isHoppingActive by fhssEngine.isHoppingActive.collectAsStateWithLifecycle()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (isHoppingActive) TacticalCyanPrimary.copy(alpha = 0.5f) else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WifiTethering, contentDescription = null, tint = TacticalCyanPrimary)
                        Text("7. Salto de Frecuencia FHSS", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isHoppingActive) TacticalCyanPrimary.copy(alpha = 0.2f) else Color.DarkGray
                    ) {
                        Text(
                            text = if (isHoppingActive) "HOPPING ACTIVO" else "STANDBY",
                            color = if (isHoppingActive) TacticalCyanPrimary else Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("CANAL", "CH-${hopStatus.currentChannel}", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("FRECUENCIA", "${hopStatus.frequencyMhz} MHz", TacticalEmeraldSecondary, Modifier.weight(1f))
                    StatMetricBox("TASA", "${hopStatus.hopRatePerSec} hops/s", TacticalAmberTertiary, Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            if (isHoppingActive) fhssEngine.stopFrequencyHopping() else fhssEngine.startFrequencyHopping()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (isHoppingActive) Color(0xFF21262D) else TacticalCyanPrimary)
                    ) {
                        Text(if (isHoppingActive) "Detener Salto" else "Iniciar FHSS Anti-Jamming", fontSize = 11.sp, color = if (isHoppingActive) Color.White else Color.Black)
                    }
                }
            }
        }

        // --- PANEL 8: RADAR DE BURBUJA 3D (GEOFENCE ESFÉRICO) ---
        val safeBubbleRadar = remember { com.example.domain.sensors.SafeBubble3DProximityRadar.getInstance(context) }
        val bubbleState by safeBubbleRadar.bubbleState.collectAsStateWithLifecycle()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (bubbleState.activeBreachesCount > 0) TacticalAmberTertiary.copy(alpha = 0.5f) else TacticalEmeraldSecondary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = TacticalEmeraldSecondary)
                        Text("8. Radar Geofence Burbuja 3D", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (bubbleState.activeBreachesCount > 0) Color.Red.copy(alpha = 0.2f) else TacticalEmeraldSecondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (bubbleState.activeBreachesCount > 0) "BRECHA DETECTADA" else "PERÍMETRO SEGURO",
                            color = if (bubbleState.activeBreachesCount > 0) Color(0xFFFF5252) else TacticalEmeraldSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("RADIO HORIZ.", "${bubbleState.radiusHorizontalMeters.toInt()} m", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("TECHO VERT.", "${bubbleState.radiusVerticalMeters.toInt()} m", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("BRECHAS", "${bubbleState.activeBreachesCount}", if (bubbleState.activeBreachesCount > 0) Color(0xFFFF5252) else TacticalEmeraldSecondary, Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { safeBubbleRadar.updatePerimeterRadius(30f, 15f) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, Color.Gray)
                    ) {
                        Text("Perímetro 30m", fontSize = 10.sp, color = Color.White)
                    }
                    OutlinedButton(
                        onClick = { safeBubbleRadar.updatePerimeterRadius(80f, 40f) },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalCyanPrimary)
                    ) {
                        Text("Perímetro 80m", fontSize = 10.sp, color = TacticalCyanPrimary)
                    }
                }
            }
        }

        // --- PANEL 9: ENMASCARADOR ESPECTRAL DE VOZ & FFT ---
        val voiceSpectralEngine = remember { com.example.domain.c4isr.TacticalVoiceSpectralEngine.getInstance(context) }
        val spectralData by voiceSpectralEngine.spectralData.collectAsStateWithLifecycle()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, if (spectralData.isScramblerActive) TacticalEmeraldSecondary.copy(alpha = 0.5f) else Color.DarkGray)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = TacticalEmeraldSecondary)
                        Text("9. Enmascarador de Voz & FFT", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (spectralData.isScramblerActive) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.DarkGray
                    ) {
                        Text(
                            text = if (spectralData.isScramblerActive) "INVERSIÓN 3.3 kHz" else "AUDIO LIMPIO",
                            color = if (spectralData.isScramblerActive) TacticalEmeraldSecondary else Color.LightGray,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("PORTADORA", "${spectralData.carrierInversionFreqHz} Hz", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("RUIDO DBFS", "${spectralData.ambientNoiseLevelDbfs.toInt()} dBFS", TacticalAmberTertiary, Modifier.weight(1f))
                    StatMetricBox("SNR", "${spectralData.snrRatioDb} dB", TacticalEmeraldSecondary, Modifier.weight(1f))
                }

                // Barras visuales de FFT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .background(Color(0xFF0D1117), RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    spectralData.fftFrequencyBands.forEach { bandHeight ->
                        Box(
                            modifier = Modifier
                                .width(12.dp)
                                .fillMaxHeight(bandHeight.coerceIn(0.1f, 1f))
                                .background(if (spectralData.isScramblerActive) TacticalEmeraldSecondary else TacticalCyanPrimary, RoundedCornerShape(2.dp))
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { voiceSpectralEngine.toggleVoiceScrambler() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = if (spectralData.isScramblerActive) TacticalEmeraldSecondary else Color(0xFF21262D))
                    ) {
                        Text(if (spectralData.isScramblerActive) "Desactivar Inversión" else "Activar Scrambler 3.3kHz", fontSize = 11.sp, color = if (spectralData.isScramblerActive) Color.Black else Color.White)
                    }
                    OutlinedButton(
                        onClick = { voiceSpectralEngine.sampleSpectralAudio() },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, TacticalCyanPrimary)
                    ) {
                        Text("Muestrear FFT", fontSize = 11.sp, color = TacticalCyanPrimary)
                    }
                }
            }
        }

        // --- PANEL 10: BRÚJULA SOLAR & EFEMÉRIDES ASTRONÓMICAS ---
        val solarCompass = remember { com.example.domain.sensors.SolarEphemerisCompass.getInstance(context) }
        val solarData by solarCompass.solarData.collectAsStateWithLifecycle()

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalAmberTertiary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.WbSunny, contentDescription = null, tint = TacticalAmberTertiary)
                        Text("10. Brújula Solar (Sin GPS/Magnetómetro)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TacticalAmberTertiary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = if (solarData.isDaylight) "SOL VISIBLE" else "NOCTURNO",
                            color = TacticalAmberTertiary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatMetricBox("AZIMUT SOL", "${solarData.sunAzimuthDegrees.toInt()}°", TacticalAmberTertiary, Modifier.weight(1f))
                    StatMetricBox("ELEVACIÓN", "${solarData.sunElevationDegrees.toInt()}°", TacticalCyanPrimary, Modifier.weight(1f))
                    StatMetricBox("MEDIODÍA UTC", solarData.solarNoonUtcTime, TacticalEmeraldSecondary, Modifier.weight(1f))
                }

                OutlinedButton(
                    onClick = {
                        // Recalcular efemérides solares con coordenadas tácticas actuales
                        solarCompass.updateObserverPosition(19.4326, -99.1332)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, TacticalAmberTertiary)
                ) {
                    Text("Recalcular Norte Verdadero por Sombra Solar", fontSize = 11.sp, color = TacticalAmberTertiary)
                }
            }
        }
    }
}

