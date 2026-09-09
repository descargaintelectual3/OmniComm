package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.ai.TacticalComputerVisionClassifier
import com.example.domain.ai.TargetThreatLevel
import com.example.domain.c2.TacticalFormationClusteringEngine
import com.example.domain.c2.TacticalFormationGroup
import com.example.domain.c2.VirtualInputType
import com.example.domain.c2.VirtualRemoteInputTunnel
import com.example.domain.hardware.TacticalUsbRadioSerialEngine
import com.example.domain.media.AfskBell202ModemEngine
import com.example.domain.media.AfskModemState
import com.example.domain.repository.TacticalC2AuditRepository
import com.example.domain.security.C2SecurityState
import com.example.domain.security.TacticalC2SignatureVerifier
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Suite Avanzada de Control y Mando Táctico Distribuido:
 * 1. Túnel de Entrada Remota Virtual (Trackpad, Navegación y Teclado Táctico).
 * 2. Canal Serie Táctico USB-OTG / Módem KISS TNC para Radios VHF/UHF/LoRa.
 * 3. Clasificador Óptico de Visión Artificial (HUD Táctico y Detección de Blancos).
 * 4. Auditoría Forense C2 Cifrada en Base de Datos Local (Room / SQLCipher).
 */
@Composable
fun TacticalAdvancedC2SuiteDialog(
    initialTab: Int = 0,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableIntStateOf(initialTab) } // 0: Input Virtual, 1: USB Radio TNC, 2: Visión Artificial, 3: Auditoría Room

    // Motores de dominio
    val inputTunnel = remember { VirtualRemoteInputTunnel.getInstance(context) }
    val inputState by inputTunnel.state.collectAsStateWithLifecycle()

    val usbRadioEngine = remember { TacticalUsbRadioSerialEngine.getInstance(context) }
    val usbState by usbRadioEngine.state.collectAsStateWithLifecycle()

    val afskModemEngine = remember { AfskBell202ModemEngine.getInstance() }
    val afskState by afskModemEngine.state.collectAsStateWithLifecycle()

    val formationEngine = remember { TacticalFormationClusteringEngine.getInstance() }
    val formations by formationEngine.formations.collectAsStateWithLifecycle()

    val c2SecurityVerifier = remember { TacticalC2SignatureVerifier }
    val c2SecState by c2SecurityVerifier.state.collectAsStateWithLifecycle()

    val visionClassifier = remember { TacticalComputerVisionClassifier.getInstance(context) }
    val visionState by visionClassifier.state.collectAsStateWithLifecycle()

    val auditRepo = remember { TacticalC2AuditRepository.getInstance(context) }
    val c2Logs by auditRepo.recentC2Logs.collectAsStateWithLifecycle(initialValue = emptyList())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.5.dp, Color(0xFF38BDF8).copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .shadow(24.dp, RoundedCornerShape(20.dp))
                .testTag("tactical_advanced_c2_suite_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Táctico
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF38BDF8).copy(alpha = 0.2f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.DeveloperBoard, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            }
                        }
                        Column {
                            Text(
                                "SUITE C2 TÁCTICA AVANZADA",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "INPUT | USB TNC | AFSK 202 | OTAN APP-6D | ED25519 ANTI-REPLAY | IA | DB",
                                fontSize = 7.5.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.LightGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Selector de pestañas horizontales desplazable
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs = listOf(
                        Triple(0, "Input Virtual", Icons.Default.Mouse),
                        Triple(1, "Radio USB", Icons.Default.Radio),
                        Triple(2, "Módem AFSK", Icons.Default.GraphicEq),
                        Triple(3, "Formación OTAN", Icons.Default.Groups),
                        Triple(4, "C2 Anti-Replay", Icons.Default.Security),
                        Triple(5, "IA Visión", Icons.Default.CameraAlt),
                        Triple(6, "Auditoría DB", Icons.Default.Storage)
                    )

                    tabs.forEach { (idx, title, icon) ->
                        val isSelected = activeTab == idx
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF38BDF8).copy(alpha = 0.25f) else Color(0xFF161B22),
                            border = BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0xFF30363D)),
                            modifier = Modifier
                                .clickable { activeTab = idx }
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    title,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Contenido de la pestaña activa
                Box(modifier = Modifier.fillMaxSize().weight(1f)) {
                    when (activeTab) {
                        0 -> VirtualRemoteInputTab(inputTunnel, inputState)
                        1 -> TacticalUsbRadioTab(usbRadioEngine, usbState)
                        2 -> TacticalAfskModemTab(afskModemEngine, afskState)
                        3 -> TacticalFormationTab(formationEngine, formations)
                        4 -> TacticalC2SecurityTab(c2SecurityVerifier, c2SecState)
                        5 -> TacticalVisionClassifierTab(visionClassifier, visionState)
                        6 -> TacticalC2AuditRoomTab(auditRepo, c2Logs)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 1: TÚNEL DE ENTRADA REMOTA VIRTUAL (TRACKPAD & TECLADO)
// -------------------------------------------------------------------------------------------------
@Composable
private fun VirtualRemoteInputTab(
    tunnel: VirtualRemoteInputTunnel,
    state: com.example.domain.c2.VirtualInputTunnelState
) {
    var textInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Estado y coordenadas
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Nodo Objetivo: ${state.targetNodeId}", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Text("Cursor Normalizado: X=${"%.2f".format(state.virtualCursorX)} | Y=${"%.2f".format(state.virtualCursorY)}", fontSize = 9.sp, color = Color(0xFF38BDF8))
                }
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                ) {
                    Text(
                        "TX: ${state.eventsSentCount} | RX: ${state.eventsReceivedCount}",
                        fontSize = 8.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Pista / Trackpad Táctil Interactivo
        Text("Trackpad Táctil y Gestos (Desliza para mover el cursor):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF030712),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val deltaX = dragAmount.x / 1000f
                        val deltaY = dragAmount.y / 1000f
                        tunnel.sendPointerMove(deltaX, deltaY)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color(0xFF38BDF8).copy(alpha = 0.4f), modifier = Modifier.size(40.dp))
                    Text("Área de Trackpad Táctil", fontSize = 10.sp, color = Color.Gray)
                    Text("Desliza aquí para dirigir el puntero remoto", fontSize = 8.sp, color = Color.DarkGray)
                }

                // Representación de cursor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (state.virtualCursorX * 240).dp,
                                y = (state.virtualCursorY * 110).dp
                            )
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.8f))
                            .shadow(4.dp)
                    )
                }
            }
        }

        // Botones de Acción de Puntero
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = { tunnel.sendTapAtCurrentPosition() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tap / Clic", fontSize = 10.sp)
            }

            Button(
                onClick = { tunnel.sendLongPressAtCurrentPosition() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.weight(1f).height(38.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Pulsación Larga", fontSize = 10.sp)
            }

            Button(
                onClick = { tunnel.sendScroll(-0.15f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.weight(0.8f).height(38.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Subir", fontSize = 9.sp)
            }

            Button(
                onClick = { tunnel.sendScroll(0.15f) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.weight(0.8f).height(38.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(14.dp))
                Text("Bajar", fontSize = 9.sp)
            }
        }

        // Barra de Navegación del Sistema Operativo Remoto
        Text("Controles de Sistema Remoto (Android Nav Bar):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(
                onClick = { tunnel.sendNavigationKey(VirtualInputType.NAV_BACK) },
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Atrás", fontSize = 9.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = { tunnel.sendNavigationKey(VirtualInputType.NAV_HOME) },
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF38BDF8))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Inicio", fontSize = 9.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = { tunnel.sendNavigationKey(VirtualInputType.NAV_RECENTS) },
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.LightGray)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recientes", fontSize = 9.sp, color = Color.White)
            }

            OutlinedButton(
                onClick = { tunnel.sendNavigationKey(VirtualInputType.NAV_POWER_LOCK) },
                modifier = Modifier.weight(1f).height(36.dp),
                border = BorderStroke(1.dp, Color(0xFFE11D48))
            ) {
                Icon(Icons.Default.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFFDA4AF))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Bloqueo", fontSize = 9.sp, color = Color(0xFFFDA4AF))
            }
        }

        // Inyección de Teclado Remoto
        Text("Inyección de Teclado y Texto Remoto:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                placeholder = { Text("Escribe comando o texto a enviar...", fontSize = 9.sp, color = Color.Gray) },
                modifier = Modifier.weight(1f).height(46.dp),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = Color.White),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF38BDF8),
                    unfocusedBorderColor = Color(0xFF30363D)
                )
            )

            Button(
                onClick = {
                    if (textInput.isNotBlank()) {
                        tunnel.sendTextInput(textInput)
                        textInput = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.height(46.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 2: CANAL SERIE TÁCTICO USB-OTG / RADIO TRANSCEPTOR (KISS TNC)
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalUsbRadioTab(
    radioEngine: TacticalUsbRadioSerialEngine,
    state: com.example.domain.hardware.UsbRadioState
) {
    var rawAtCommand by remember { mutableStateOf("AT+BAND=433.000") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera de Hardware USB
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, if (state.isConnected) Color(0xFF10B981) else Color(0xFF30363D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        state.activeDevice?.deviceName ?: "Sin Radio USB Detectada",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.White
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (state.isConnected) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.2f)
                    ) {
                        Text(
                            if (state.isConnected) "ENLACE FÍSICO ACTIVO" else "DESCONECTADO",
                            fontSize = 8.sp,
                            color = if (state.isConnected) Color(0xFF34D399) else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    "Chip: ${state.activeDevice?.chipType ?: "N/A"} | Baud: ${state.baudRate} bps | Modo: ${state.radioMode}",
                    fontSize = 8.sp,
                    color = Color.LightGray
                )
            }
        }

        // Acciones de Conexión y Escaneo
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    if (state.isConnected) radioEngine.disconnectRadio() else radioEngine.connectRadio()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isConnected) Color(0xFFEF4444) else Color(0xFF10B981)
                ),
                modifier = Modifier.weight(1f).height(38.dp)
            ) {
                Icon(if (state.isConnected) Icons.Default.LinkOff else Icons.Default.Usb, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (state.isConnected) "Desconectar Radio" else "Conectar Radio OTG", fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { radioEngine.scanAvailableUsbDevices() },
                modifier = Modifier.weight(0.7f).height(38.dp),
                border = BorderStroke(1.dp, Color(0xFF475569))
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Escanear", fontSize = 9.sp, color = Color.White)
            }
        }

        // Selector de Baud Rate y Botón PTT
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val bauds = listOf(1200, 9600, 115200)
            bauds.forEach { b ->
                val isBaud = state.baudRate == b
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isBaud) Color(0xFF0284C7) else Color(0xFF1E293B),
                    modifier = Modifier.weight(1f).clickable { radioEngine.setBaudRate(b) }
                ) {
                    Text(
                        "$b bps",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // BOTÓN PTT (PUSH-TO-TALK) TÁCTICO
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (state.isPttActive) Color(0xFFDC2626) else Color(0xFF161B22),
            border = BorderStroke(1.5.dp, if (state.isPttActive) Color(0xFFEF4444) else Color(0xFF38BDF8).copy(alpha = 0.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clickable { radioEngine.togglePtt(!state.isPttActive) }
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (state.isPttActive) Color.White else Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            if (state.isPttActive) "PORTADORA TX ACTIVA (PTT ON)" else "PTT TRANSMISOR DISPONIBLE",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                        Text(
                            if (state.isPttActive) "Emitiendo en portadora VHF/UHF..." else "Toca para abrir canal de voz/datos RF",
                            fontSize = 8.sp,
                            color = if (state.isPttActive) Color(0xFFFCA5A5) else Color.Gray
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = if (state.isPttActive) Color.White else Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                ) {}
            }
        }

        // Transmisión de Trama KISS de Prueba
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Button(
                onClick = {
                    val frame = "OMNICOMM_BEACON_GPS_4.6145_-74.0780".toByteArray()
                    radioEngine.transmitKissFrame(frame)
                },
                enabled = state.isConnected,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.weight(1f).height(36.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("TX Baliza TNC (AX.25)", fontSize = 9.sp)
            }
        }

        // Terminal de Monitoreo Serial
        Text("Monitor de Tráfico Serial RF (${state.recentLog.size} líneas):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF030712),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(6.dp)) {
                items(state.recentLog) { line ->
                    Text(
                        line,
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        color = if (line.contains("TX")) Color(0xFF38BDF8) else if (line.contains("RX")) Color(0xFF34D399) else Color.LightGray
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 3: VISIÓN ARTIFICIAL ÓPTICA & CLASIFICACIÓN DE BLANCOS
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalVisionClassifierTab(
    classifier: TacticalComputerVisionClassifier,
    state: com.example.domain.ai.TacticalVisionState
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera y Controles
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("INFERENCIA ÓPTICA 24 FPS", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Text("Blancos Activos: ${state.detectedTargets.size} | Total: ${state.totalClassifications}", fontSize = 8.sp, color = Color(0xFF38BDF8))
                }

                Button(
                    onClick = {
                        if (state.isAnalyzingFeed) classifier.stopOpticalFeedAnalysis() else classifier.startOpticalFeedAnalysis()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isAnalyzingFeed) Color(0xFFEF4444) else Color(0xFF10B981)
                    ),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (state.isAnalyzingFeed) "Pausar Inferencia" else "Iniciar Reconocimiento", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Selector de Filtro Óptico / Térmico
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            val filters = listOf(
                Pair("OPTICAL_RGB", "Óptico RGB"),
                Pair("FLIR_THERMAL_WHITE_HOT", "FLIR Térmico"),
                Pair("FLIR_GREEN_NIGHT", "Visión Nocturna")
            )
            filters.forEach { (mode, label) ->
                val isSel = state.opticalFilterMode == mode
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSel) Color(0xFF0284C7) else Color(0xFF1E293B),
                    modifier = Modifier.weight(1f).clickable { classifier.setFilterMode(mode) }
                ) {
                    Text(
                        label,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Visor Táctico HUD Reticular con Bounding Boxes
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (state.opticalFilterMode) {
                "FLIR_THERMAL_WHITE_HOT" -> Color(0xFF1E293B)
                "FLIR_GREEN_NIGHT" -> Color(0xFF064E3B)
                else -> Color(0xFF030712)
            },
            border = BorderStroke(1.dp, Color(0xFF38BDF8)),
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Retícula central
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.FilterCenterFocus, contentDescription = null, tint = Color(0xFF38BDF8).copy(alpha = 0.5f), modifier = Modifier.size(50.dp))
                }

                // Superposición de blancos y bounding boxes
                state.detectedTargets.forEach { target ->
                    val color = Color(target.threatLevel.colorHex)
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (target.boundingBox.left * 220).dp,
                                y = (target.boundingBox.top * 120).dp
                            )
                            .size(
                                width = ((target.boundingBox.right - target.boundingBox.left) * 220).dp.coerceAtLeast(30.dp),
                                height = ((target.boundingBox.bottom - target.boundingBox.top) * 120).dp.coerceAtLeast(30.dp)
                            )
                            .border(BorderStroke(1.5.dp, color), RoundedCornerShape(4.dp))
                    ) {
                        Surface(
                            shape = RoundedCornerShape(bottomEnd = 4.dp),
                            color = color.copy(alpha = 0.85f),
                            modifier = Modifier.align(Alignment.TopStart)
                        ) {
                            Text(
                                "${target.category.label.take(8)} ${"%.0f".format(target.confidence * 100)}%",
                                fontSize = 6.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                // HUD Telemetría Óptica Inferior
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("ZOOM: 4.2x | AZIMUTH: 142°", fontSize = 7.sp, color = Color.LightGray)
                    Text("AMENAZA: ${state.highestThreatDetected.label}", fontSize = 7.sp, color = Color(state.highestThreatDetected.colorHex), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Lista de Blancos Clasificados
        Text("Blancos Identificados en el Sector:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
        state.detectedTargets.forEach { target ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF161B22),
                border = BorderStroke(1.dp, Color(target.threatLevel.colorHex).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(target.category.label, fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.White)
                        Text(
                            "Distancia: ${"%.0f".format(target.estimatedDistanceMeters)}m | Rumbo: ${"%.0f".format(target.azimuthBearingDeg)}° | Térmica: ${target.thermalSignatureLevel}",
                            fontSize = 7.sp,
                            color = Color.LightGray
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(target.threatLevel.colorHex).copy(alpha = 0.2f)
                    ) {
                        Text(
                            target.threatLevel.name,
                            fontSize = 7.sp,
                            color = Color(target.threatLevel.colorHex),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB 4: AUDITORÍA FORENSE C2 CIFRADA (ROOM DATABASE / SQLCIPHER)
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalC2AuditRoomTab(
    auditRepo: TacticalC2AuditRepository,
    logs: List<com.example.domain.local.entities.TacticalC2LogEntity>
) {
    val coroutineScope = rememberCoroutineScope()
    val sdf = remember { SimpleDateFormat("HH:mm:ss dd/MM", Locale.US) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Resumen de Base de Datos
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("REGISTRO AUDITADO ROOM/SQLCIPHER", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    Text("${logs.size} eventos criptográficamente almacenados", fontSize = 8.sp, color = Color(0xFF34D399))
                }

                Button(
                    onClick = { coroutineScope.launch { auditRepo.clearAuditHistory() } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    modifier = Modifier.height(28.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text("Purgar Registros", fontSize = 8.sp, color = Color(0xFFFCA5A5), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Historial de Órdenes C2
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (logs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros C2 aún en la base de datos local.", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            } else {
                items(logs, key = { it.logId }) { log ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF0D1117),
                        border = BorderStroke(1.dp, if (log.success) Color(0xFF30363D) else Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    log.actionType,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (log.success) Color(0xFF38BDF8) else Color(0xFFEF4444)
                                )
                                Text(
                                    sdf.format(Date(log.timestamp)),
                                    fontSize = 8.sp,
                                    color = Color.Gray,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                "Origen: ${log.sourceCallsign} -> Destino: ${log.targetCallsign} [${log.transportType}]",
                                fontSize = 8.sp,
                                color = Color.LightGray
                            )
                            Text(
                                "Respuesta: ${log.responseMessage}",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// COMPONENTE DE ESTADÍSTICA TÁCTICA
// -------------------------------------------------------------------------------------------------
@Composable
private fun C2StatBadge(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF0D1117),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 7.sp, color = Color.Gray, maxLines = 1)
            Text(value, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = color, maxLines = 1)
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB: MÓDEM DE AUDIO ANALÓGICO AFSK BELL 202 / AX.25 (1200 BAUDIOS)
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalAfskModemTab(
    modemEngine: AfskBell202ModemEngine,
    state: AfskModemState
) {
    var callsign by remember { mutableStateOf("ECHO-7") }
    var payloadText by remember { mutableStateOf("SITREP: POSICIÓN ASEGURADA, LISTO PARA ENLACE") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Tarjeta de Estado AFSK
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
                        Text("Módem Analógico AFSK Bell 202", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (state.isTransmitting) Color(0xFFEF4444).copy(alpha = 0.2f) else Color(0xFF10B981).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (state.isTransmitting) Color(0xFFEF4444) else Color(0xFF10B981))
                    ) {
                        Text(
                            if (state.isTransmitting) "TX EN VIVO (AUDIO)" else "STANDBY / RX",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isTransmitting) Color(0xFFEF4444) else Color(0xFF10B981),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    C2StatBadge(title = "Baudios", value = "${state.baudRate} bps", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                    C2StatBadge(title = "Mark (Bit 1)", value = "${state.markFreqHz.toInt()} Hz", color = Color(0xFF34D399), modifier = Modifier.weight(1f))
                    C2StatBadge(title = "Space (Bit 0)", value = "${state.spaceFreqHz.toInt()} Hz", color = Color(0xFFFBBF24), modifier = Modifier.weight(1f))
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    C2StatBadge(title = "TX Enviados", value = "${state.packetsSentCount}", color = Color(0xFF38BDF8), modifier = Modifier.weight(1f))
                    C2StatBadge(title = "RX Recibidos", value = "${state.packetsReceivedCount}", color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                    C2StatBadge(title = "Errores CRC", value = "${state.crcErrorsCount}", color = Color(0xFFF87171), modifier = Modifier.weight(1f))
                }

                // Barra de VU Meter de Audio
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nivel de Salida de Audio / Modulación", fontSize = 8.sp, color = Color.Gray)
                        Text("${(state.audioLevelRms * 100).toInt()}%", fontSize = 8.sp, color = Color(0xFF00E5FF), fontFamily = FontFamily.Monospace)
                    }
                    LinearProgressIndicator(
                        progress = { state.audioLevelRms },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF00E5FF),
                        trackColor = Color(0xFF21262D)
                    )
                }
            }
        }

        // Panel de Transmisión Táctica
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transmisión sobre Radio Analógica (Jack 3.5mm / Bluetooth)", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF38BDF8))

                OutlinedTextField(
                    value = callsign,
                    onValueChange = { callsign = it.uppercase() },
                    label = { Text("Indicativo / Callsign", fontSize = 9.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF30363D)
                    )
                )

                OutlinedTextField(
                    value = payloadText,
                    onValueChange = { payloadText = it },
                    label = { Text("Mensaje / Trama AX.25", fontSize = 9.sp) },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.LightGray,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF30363D)
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = { modemEngine.transmitTacticalFrame(callsign, payloadText) },
                        enabled = !state.isTransmitting && payloadText.isNotBlank(),
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Emitir Audio AFSK", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { modemEngine.simulateIncomingAfskFrame("TANGO-9", "COORD 19.4326,-99.1332 SOS") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981)),
                        border = BorderStroke(1.dp, Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.SyncAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simular RX", fontSize = 8.sp)
                    }
                }
            }
        }

        // Registro de actividad AFSK
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Registro de Modulación de Audio", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                if (state.recentLogs.isEmpty()) {
                    Text("Sin actividad registrada aún.", fontSize = 8.sp, color = Color.DarkGray)
                } else {
                    state.recentLogs.takeLast(6).reversed().forEach { logLine ->
                        Text(logLine, fontSize = 7.5.sp, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB: FORMACIONES TÁCTICAS MILITARES OTAN APP-6D
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalFormationTab(
    engine: TacticalFormationClusteringEngine,
    formations: List<TacticalFormationGroup>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera de Formaciones
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color(0xFFA855F7), modifier = Modifier.size(18.dp))
                        Text("Clustering Táctico OTAN APP-6D", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                    Button(
                        onClick = { engine.loadBaselineTacticalClusters() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Recalcular", fontSize = 8.sp)
                    }
                }

                Text(
                    "Agrupación automática por geometría espacial, vector de avance y arcos de tiro entre nodos de la malla.",
                    fontSize = 8.sp,
                    color = Color.LightGray
                )
            }
        }

        // Listado de formaciones
        if (formations.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF161B22),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Text("No hay formaciones activas detectadas en el teatro.", fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(16.dp))
            }
        } else {
            formations.forEach { group ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFA855F7).copy(alpha = 0.25f),
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(group.echelon.symbol, color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                Column {
                                    Text(group.formationType.label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                                    Text("${group.echelon.label} • ${group.formationType.natoCode}", fontSize = 7.5.sp, color = Color(0xFFA855F7))
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFF10B981).copy(alpha = 0.2f),
                                border = BorderStroke(0.8.dp, Color(0xFF10B981))
                            ) {
                                Text(
                                    "Confianza: ${(group.detectionConfidence * 100).toInt()}%",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFF10B981),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(group.formationType.description, fontSize = 8.sp, color = Color.LightGray)

                        // Métricas del grupo
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            C2StatBadge("Radio Dispersión", "${"%.0f".format(group.radiusMeters)} m", Color(0xFF38BDF8), Modifier.weight(1f))
                            C2StatBadge("Rumbo Medio", "${"%.0f".format(group.averageHeadingDegrees)}°", Color(0xFFFBBF24), Modifier.weight(1f))
                            C2StatBadge("Velocidad", "${"%.1f".format(group.averageSpeedKmh)} km/h", Color(0xFF34D399), Modifier.weight(1f))
                        }

                        // Coordenadas del Centroide y Arco de Fuego
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF0D1117),
                            border = BorderStroke(0.8.dp, Color(0xFF21262D))
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    "Centroide Táctico: ${"%.5f".format(group.centroidLatitude)}, ${"%.5f".format(group.centroidLongitude)}",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFF94A3B8),
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "Arco de Cobertura de Fuego: ${"%.0f".format(group.fireArcStartDegrees)}° a ${"%.0f".format(group.fireArcEndDegrees)}°",
                                    fontSize = 7.5.sp,
                                    color = Color(0xFFF43F5E)
                                )
                                Text(
                                    "Elementos integrados: ${group.members.joinToString(", ") { it.callsign }}",
                                    fontSize = 7.5.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------------------
// TAB: CRIPTOGRAFÍA C2 ED25519 / P-256 CON VALIDACIÓN ANTI-REPLAY
// -------------------------------------------------------------------------------------------------
@Composable
private fun TacticalC2SecurityTab(
    verifier: TacticalC2SignatureVerifier,
    state: C2SecurityState
) {
    var lastVerificationResult by remember { mutableStateOf<com.example.domain.security.C2VerificationResult?>(null) }
    var cachedSignedPayload by remember { mutableStateOf<com.example.domain.security.SignedC2Payload?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Cabecera de Hardware Criptográfico
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        Text("Criptoseguridad C2 Anti-Replay", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (state.isHardwareKeyStoreActive) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, if (state.isHardwareKeyStoreActive) Color(0xFF10B981) else Color(0xFFF59E0B))
                    ) {
                        Text(
                            if (state.isHardwareKeyStoreActive) "HARDWARE KEYSTORE" else "SOFTWARE FALLBACK",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isHardwareKeyStoreActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    "Huella Digital Clave Local (ECDSA P-256): ${state.localKeyFingerprint}",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF38BDF8)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    C2StatBadge("Firmadas", "${state.totalSignedCommands}", Color(0xFF38BDF8), Modifier.weight(1f))
                    C2StatBadge("Auténticas", "${state.totalVerifiedValid}", Color(0xFF10B981), Modifier.weight(1f))
                    C2StatBadge("Replay Bloqueados", "${state.totalReplayAttacksBlocked}", Color(0xFFEF4444), Modifier.weight(1f))
                }
            }
        }

        // Laboratorio Interactivo de Prueba de Ataques y Firmas
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Laboratorio de Validación Criptográfica en Malla", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color(0xFF38BDF8))
                Text(
                    "Firma órdenes con timestamp atómico y nonce único. Si un adversario intercepta la radiofrecuencia y reinyecta el paquete, el sistema lo bloquea automáticamente.",
                    fontSize = 7.5.sp,
                    color = Color.LightGray
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val signed = verifier.signC2Command(
                                commandId = "CMD-${System.currentTimeMillis().toString().takeLast(4)}",
                                commandType = "FIRE_CONTROL_PERMIT",
                                targetNodeId = "DRONE_UAV_01",
                                args = "WAYPOINT_LAT_19.432_LON_-99.133"
                            )
                            cachedSignedPayload = signed
                            val result = verifier.verifyC2Payload(signed)
                            lastVerificationResult = result
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Emitir Orden Firmada", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            cachedSignedPayload?.let { cached ->
                                // Reinyectar el mismo sobre con el mismo nonce
                                val result = verifier.verifyC2Payload(cached)
                                lastVerificationResult = result
                            }
                        },
                        enabled = cachedSignedPayload != null,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                    ) {
                        Icon(Icons.Default.GppBad, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simular Replay Attack", fontSize = 8.5.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Banner de Resultado de Verificación
                lastVerificationResult?.let { result ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (result.isValid) Color(0xFF065F46) else Color(0xFF7F1D1D),
                        border = BorderStroke(1.dp, if (result.isValid) Color(0xFF10B981) else Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                if (result.isValid) "ORDEN AUTÉNTICA Y VÁLIDA" else "¡ATAQUE INTERCEPTADO Y BLOQUEADO!",
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = Color.White
                            )
                            Text(
                                "Motivo: ${result.reason} • Latencia: ${result.ageMs}ms • Replay: ${if (result.isReplayDetected) "SÍ (DESCARTADO)" else "NO"}",
                                fontSize = 7.5.sp,
                                color = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }

        // Historial de Seguridad
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF161B22),
            border = BorderStroke(1.dp, Color(0xFF30363D))
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Bitácora de Eventos de Criptoseguridad", fontWeight = FontWeight.Bold, fontSize = 9.sp, color = Color.Gray)
                if (state.securityLogs.isEmpty()) {
                    Text("Sin alertas registradas.", fontSize = 8.sp, color = Color.DarkGray)
                } else {
                    state.securityLogs.takeLast(6).reversed().forEach { log ->
                        Text(log, fontSize = 7.5.sp, color = Color(0xFFCBD5E1), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
    }
}

