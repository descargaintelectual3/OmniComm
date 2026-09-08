package com.example.ui.screens

import android.Manifest
import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.StopScreenShare
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.p2p.TacticalVideoCodecProfile
import com.example.domain.p2p.VideoTelemetryOverlay
import com.example.ui.viewmodels.CallState
import com.example.ui.viewmodels.VideoParticipant
import com.example.ui.viewmodels.VideoViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun VideoScreen(
    onBack: () -> Unit,
    viewModel: VideoViewModel = viewModel()
) {
    var inputRoomId by remember { mutableStateOf("") }
    val callState by viewModel.callState.collectAsStateWithLifecycle()
    val currentRoomId by viewModel.currentRoomId.collectAsStateWithLifecycle()
    val engineStatus by viewModel.engineStatus.collectAsStateWithLifecycle()
    val durationSeconds by viewModel.callDurationSeconds.collectAsStateWithLifecycle()
    val participants by viewModel.participants.collectAsStateWithLifecycle()
    
    val isCameraEnabled by viewModel.isCameraEnabled.collectAsStateWithLifecycle()
    val isMicEnabled by viewModel.isMicEnabled.collectAsStateWithLifecycle()
    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()
    val isScreenSharing by viewModel.isScreenSharing.collectAsStateWithLifecycle()
    
    val networkFps by viewModel.networkFps.collectAsStateWithLifecycle()
    val networkPingMs by viewModel.networkPingMs.collectAsStateWithLifecycle()

    val permissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    )

    LaunchedEffect(Unit) {
        if (!permissionsState.allPermissionsGranted) {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (callState == CallState.IN_CALL) "Sala: $currentRoomId" else "Video Rooms",
                                fontWeight = FontWeight.Bold
                            )
                            if (callState == CallState.IN_CALL) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF16A34A).copy(alpha = 0.2f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF22C55E))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("E2EE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                                    }
                                }
                            }
                        }
                        Text(
                            if (callState == CallState.IN_CALL) {
                                val minutes = durationSeconds / 60
                                val seconds = durationSeconds % 60
                                String.format(Locale.getDefault(), "%02d:%02d • %d FPS • %d ms", minutes, seconds, networkFps, networkPingMs)
                            } else engineStatus,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (callState == CallState.IN_CALL) {
                                viewModel.endCall()
                            }
                            onBack()
                        },
                        modifier = Modifier.testTag("video_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (callState) {
                CallState.IDLE -> {
                    VideoLobbyView(
                        roomId = inputRoomId,
                        onRoomIdChange = { inputRoomId = it },
                        onJoin = { viewModel.connectToRoom(inputRoomId) },
                        onGenerate = { inputRoomId = viewModel.generateSecureRoomId() }
                    )
                }
                CallState.CONNECTING -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Negociando túnel WebRTC de baja latencia...", fontWeight = FontWeight.SemiBold)
                            Text("Cifrando handshake con par de llaves ECDH", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                CallState.IN_CALL -> {
                    val currentTelemetry by viewModel.currentTelemetry.collectAsStateWithLifecycle()
                    val isHudVisible by viewModel.isHudVisible.collectAsStateWithLifecycle()
                    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
                    val activeCodec by viewModel.activeCodecProfile.collectAsStateWithLifecycle()

                    ActiveVideoCallView(
                        participants = participants,
                        isCameraEnabled = isCameraEnabled,
                        isMicEnabled = isMicEnabled,
                        isFrontCamera = isFrontCamera,
                        isScreenSharing = isScreenSharing,
                        telemetry = currentTelemetry,
                        isHudVisible = isHudVisible,
                        filterMode = filterMode,
                        activeCodec = activeCodec,
                        onToggleCamera = { viewModel.toggleCamera() },
                        onToggleMic = { viewModel.toggleMic() },
                        onFlipCamera = { viewModel.toggleCameraLens() },
                        onToggleScreenShare = { viewModel.toggleScreenShare() },
                        onToggleHud = { viewModel.toggleHud() },
                        onCycleFilter = { viewModel.cycleVideoFilter() },
                        onSelectCodec = { viewModel.setCodecProfile(it) },
                        onEndCall = { viewModel.endCall() }
                    )
                }
                CallState.ENDED -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Llamada finalizada", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { viewModel.connectToRoom(inputRoomId.ifBlank { viewModel.generateSecureRoomId() }) }) {
                                Text("Reconectar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoLobbyView(
    roomId: String,
    onRoomIdChange: (String) -> Unit,
    onJoin: () -> Unit,
    onGenerate: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWide = maxWidth > 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = if (isWide) 64.dp else 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Videocam,
                        contentDescription = "Video",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Salas WebRTC Tácticas E2EE",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Canal audiovisual descentralizado de alta fidelidad para nodos de la malla",
                textAlign = TextAlign.Center,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = roomId,
                onValueChange = onRoomIdChange,
                label = { Text("Identificador de Sala") },
                placeholder = { Text("Ej: OMNI-4819-2093") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .testTag("room_id_input")
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onJoin, 
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(50.dp)
                    .testTag("join_room_button"),
                shape = RoundedCornerShape(12.dp),
                enabled = roomId.isNotBlank()
            ) {
                Icon(Icons.Default.Videocam, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Entrar a la Sala Mesh", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(12.dp))
            FilledTonalButton(
                onClick = onGenerate, 
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(50.dp)
                    .testTag("generate_room_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Generar Nueva Sala Segura")
            }
        }
    }
}

@Composable
fun ActiveVideoCallView(
    participants: List<VideoParticipant>,
    isCameraEnabled: Boolean,
    isMicEnabled: Boolean,
    isFrontCamera: Boolean,
    isScreenSharing: Boolean,
    telemetry: VideoTelemetryOverlay,
    isHudVisible: Boolean,
    filterMode: String,
    activeCodec: TacticalVideoCodecProfile,
    onToggleCamera: () -> Unit,
    onToggleMic: () -> Unit,
    onFlipCamera: () -> Unit,
    onToggleScreenShare: () -> Unit,
    onToggleHud: () -> Unit,
    onCycleFilter: () -> Unit,
    onSelectCodec: (TacticalVideoCodecProfile) -> Unit,
    onEndCall: () -> Unit
) {
    var showCodecMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A))) {
        val isWide = maxWidth > 600.dp
        val columns = if (participants.size == 1) 1 else if (isWide || participants.size > 2) 2 else 1

        Column(modifier = Modifier.fillMaxSize()) {

            // --- BARRA SUPERIOR HUD TÁCTICO ---
            AnimatedVisibility(visible = isHudVisible) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Black.copy(alpha = 0.85f),
                    contentColor = Color(0xFF38BDF8)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "MGRS: ${telemetry.mgrsCoordinate}",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = Color(0xFF22C55E)
                            )
                            Text(
                                "AZ: %03d° %s • ALT: %.0fm".format(Locale.getDefault(), telemetry.compassAzimuthDeg.toInt(), telemetry.cardinalDirection, telemetry.altitudeMeters),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                            Text(
                                "BAT: ${telemetry.batteryPercent}%",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = if (telemetry.batteryPercent < 20) Color(0xFFEF4444) else Color(0xFFFBBF24)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF10B981))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    telemetry.encryptionCipher,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            Text(
                                "${activeCodec.targetFps} FPS • ${activeCodec.estimatedBitrateKbps} Kbps • ${telemetry.latencyPingMs}ms",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Fila de Filtros y Configuración Rápida Táctica
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1E293B).copy(alpha = 0.7f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de Perfil Códec Táctico
                Box {
                    AssistChip(
                        onClick = { showCodecMenu = true },
                        label = {
                            Text(
                                when (activeCodec) {
                                    TacticalVideoCodecProfile.TACTICAL_STEALTH_LOW -> "Sigilo 10 FPS"
                                    TacticalVideoCodecProfile.TACTICAL_STANDARD -> "Táctico 20 FPS"
                                    TacticalVideoCodecProfile.TACTICAL_HD -> "HD 30 FPS"
                                },
                                fontSize = 10.sp
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Tune, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    )
                    DropdownMenu(
                        expanded = showCodecMenu,
                        onDismissRequest = { showCodecMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sigilo / Bajo Ancho de Banda (10 FPS, 64 Kbps)") },
                            onClick = {
                                onSelectCodec(TacticalVideoCodecProfile.TACTICAL_STEALTH_LOW)
                                showCodecMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Táctico Estándar (20 FPS, 320 Kbps)") },
                            onClick = {
                                onSelectCodec(TacticalVideoCodecProfile.TACTICAL_STANDARD)
                                showCodecMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("HD Operativo (30 FPS, 1200 Kbps)") },
                            onClick = {
                                onSelectCodec(TacticalVideoCodecProfile.TACTICAL_HD)
                                showCodecMenu = false
                            }
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Botón Filtro Visión Nocturna / Térmica
                    FilterChip(
                        selected = filterMode != "NORMAL",
                        onClick = onCycleFilter,
                        label = {
                            Text(
                                when (filterMode) {
                                    "NVG_GREEN" -> "🟢 NVG Nocturno"
                                    "FLIR_THERMAL" -> "🟠 FLIR Térmico"
                                    else -> "Óptico Normal"
                                },
                                fontSize = 10.sp
                            )
                        }
                    )

                    // Botón Ocultar/Mostrar HUD
                    FilterChip(
                        selected = isHudVisible,
                        onClick = onToggleHud,
                        label = { Text("HUD", fontSize = 10.sp) }
                    )
                }
            }

            // Cuadrícula de Participantes con posible tinte NVG/FLIR
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(participants, key = { it.id }) { participant ->
                        VideoParticipantTile(
                            participant = participant,
                            isFrontCamera = isFrontCamera
                        )
                    }
                }

                // Superposición de Tinte Táctico Nocturno / FLIR
                if (filterMode == "NVG_GREEN") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF00FF66).copy(alpha = 0.18f))
                    )
                } else if (filterMode == "FLIR_THERMAL") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFFF6600).copy(alpha = 0.18f))
                    )
                }

                // Retícula / Cruz Táctica en el centro si el HUD está activo
                if (isHudVisible) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(50.dp)
                            .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(Color(0xFF22C55E), CircleShape)
                        )
                    }
                }
            }

            // Barra Flotante Inferior de Controles
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botón Micrófono
                    FilledIconButton(
                        onClick = onToggleMic,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isMicEnabled) Color(0xFF334155) else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(50.dp).testTag("video_toggle_mic_btn")
                    ) {
                        Icon(
                            if (isMicEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = "Mic",
                            tint = Color.White
                        )
                    }

                    // Botón Cámara
                    FilledIconButton(
                        onClick = onToggleCamera,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isCameraEnabled) Color(0xFF334155) else MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.size(50.dp).testTag("video_toggle_cam_btn")
                    ) {
                        Icon(
                            if (isCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            contentDescription = "Cam",
                            tint = Color.White
                        )
                    }

                    // Botón Voltear Cámara
                    FilledIconButton(
                        onClick = onFlipCamera,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF334155)),
                        modifier = Modifier.size(50.dp).testTag("video_flip_cam_btn"),
                        enabled = isCameraEnabled
                    ) {
                        Icon(Icons.Default.FlipCameraAndroid, contentDescription = "Flip", tint = Color.White)
                    }

                    // Botón Compartir Pantalla
                    FilledIconButton(
                        onClick = onToggleScreenShare,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = if (isScreenSharing) Color(0xFF2563EB) else Color(0xFF334155)
                        ),
                        modifier = Modifier.size(50.dp).testTag("video_screen_share_btn")
                    ) {
                        Icon(
                            if (isScreenSharing) Icons.Default.StopScreenShare else Icons.Default.ScreenShare,
                            contentDescription = "ScreenShare",
                            tint = Color.White
                        )
                    }

                    // Botón Colgar
                    FilledIconButton(
                        onClick = onEndCall,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFFE11D48)),
                        modifier = Modifier.size(54.dp).testTag("video_end_call_btn")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "Colgar", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun VideoParticipantTile(
    participant: VideoParticipant,
    isFrontCamera: Boolean
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 320.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (participant.isSpeaking) 2.dp else 1.dp,
                color = if (participant.isSpeaking) Color(0xFF22C55E) else Color(0xFF334155),
                shape = RoundedCornerShape(16.dp)
            ),
        color = Color(0xFF1E293B)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (participant.isLocal && participant.isCameraOn) {
                // Cámara Local real con CameraX
                val previewView = remember { PreviewView(context) }
                val lensFacing = if (isFrontCamera) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK

                LaunchedEffect(lensFacing) {
                    try {
                        val cameraProvider = context.getCameraProvider()
                        cameraProvider.unbindAll()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview)
                    } catch (e: Exception) {
                        // fallback
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (participant.isCameraOn) {
                // Video Remoto Feed cifrado WebRTC con gradiente y animación de transmisión
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    participant.name.take(2).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Stream P2P Seguro", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    }
                }
            } else {
                // Cámara apagada
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF64748B))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Cámara Desactivada", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    }
                }
            }

            // Indicador de Nombre y Estado de Audio
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        participant.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (participant.isLocal) {
                        Text(" (Tú)", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (participant.isSpeaking) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "Hablando",
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Icon(
                        if (participant.isMicOn) Icons.Default.Mic else Icons.Default.MicOff,
                        contentDescription = null,
                        tint = if (participant.isMicOn) Color.White else Color(0xFFEF4444),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

