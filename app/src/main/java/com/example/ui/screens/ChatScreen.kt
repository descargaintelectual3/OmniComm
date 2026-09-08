package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ThreadCustomizationEntity
import com.example.ui.components.CreateGroupChatDialog
import com.example.ui.components.GroupInfoAndAdminDialog
import com.example.ui.components.ThreadCustomizationDialog
import com.example.ui.components.WALLPAPER_PRESETS
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.viewmodels.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onBack: () -> Unit,
    onOpenContacts: (() -> Unit)? = null,
    onOpenCryptoKeys: (() -> Unit)? = null,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    var messageInput by remember { mutableStateOf("") }
    var selectedMediaPreview by remember { mutableStateOf<String?>(null) }

    // Diálogos modales
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var showCreateGroupDialog by remember { mutableStateOf(false) }
    var showGroupAdminDialog by remember { mutableStateOf(false) }
    var showSafetyNumberDialog by remember { mutableStateOf(false) }

    val activeSessionId by viewModel.activeSessionId.collectAsStateWithLifecycle()
    val activeSessionTitle by viewModel.activeSessionTitle.collectAsStateWithLifecycle()
    val partnerName by viewModel.partnerName.collectAsStateWithLifecycle()
    val isCurrentGroup by viewModel.isCurrentSessionGroup.collectAsStateWithLifecycle()
    val currentGroupSession by viewModel.currentGroupSession.collectAsStateWithLifecycle()
    val customization by viewModel.currentThreadCustomization.collectAsStateWithLifecycle()
    val safetyNumber by viewModel.safetyNumber.collectAsStateWithLifecycle()
    val ratchetStep by viewModel.ratchetSendingStep.collectAsStateWithLifecycle()
    val isSafetyVerified by viewModel.isSafetyNumberVerified.collectAsStateWithLifecycle()

    val messages by viewModel.localEncryptedMessages.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingMessagesCount.collectAsStateWithLifecycle()
    val meshState by viewModel.meshConnectionState.collectAsStateWithLifecycle()
    val discoveredPeers by viewModel.discoveredPeers.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContacts.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()

    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val uploadProgress by viewModel.uploadProgress.collectAsStateWithLifecycle()

    // Grabación y Reproducción de Audio
    val isRecordingVoice by viewModel.isVoiceNoteRecording.collectAsStateWithLifecycle()
    val recordingDurationMs by viewModel.voiceNoteDurationMs.collectAsStateWithLifecycle()
    val waveformAmplitudes by viewModel.voiceNoteAmplitudes.collectAsStateWithLifecycle()
    val isTranscribingVoice by viewModel.isTranscribingVoice.collectAsStateWithLifecycle()
    val transcriptionStatusText by viewModel.transcriptionStatusText.collectAsStateWithLifecycle()

    val isAudioPlaying by viewModel.isAudioPlaying.collectAsStateWithLifecycle()
    val playingAudioMessageId by viewModel.playingAudioMessageId.collectAsStateWithLifecycle()
    val audioPlaybackPosMs by viewModel.audioPlaybackPositionMs.collectAsStateWithLifecycle()
    val audioTotalDurationMs by viewModel.audioTotalDurationMs.collectAsStateWithLifecycle()

    val currentUserProfile by viewModel.authManager.currentUserProfile.collectAsStateWithLifecycle()
    val currentUserId = currentUserProfile?.uid ?: "local_device"

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioPermission = isGranted
    }

    // Selector de fotos de la galería
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.sendPhoto(uri, messageInput)
            messageInput = ""
        }
    }

    // Gradient / Background wallpaper del hilo activo
    val activeWallpaperBrush = remember(customization) {
        val presetId = customization?.wallpaperPreset ?: "TACTICAL_DARK"
        WALLPAPER_PRESETS.find { it.id == presetId }?.gradientBrush
            ?: Brush.verticalGradient(listOf(Color(0xFF0D1117), Color(0xFF161B22)))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier.clickable(enabled = isCurrentGroup) {
                            if (isCurrentGroup && currentGroupSession != null) {
                                showGroupAdminDialog = true
                            }
                        }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrentGroup) {
                                Text(currentGroupSession?.groupIcon?.ifBlank { "🛡️" } ?: "🛡️", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            Text(
                                if (activeSessionId != "global") activeSessionTitle else "Team Mesh Chat",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                if (isCurrentGroup) Icons.Default.Groups else Icons.Default.Lock,
                                contentDescription = "E2EE Encrypted",
                                modifier = Modifier.size(14.dp),
                                tint = if (isCurrentGroup) TacticalEmeraldSecondary else TacticalCyanPrimary
                            )
                        }
                        Text(
                            text = if (isCurrentGroup) "🛡️ Grupo Multi-Usuario • ${currentGroupSession?.participantUids?.size ?: 0} miembros (Toca para admin)"
                                   else if (activeSessionId != "global") "🔒 Sesión E2EE Directa • Firestore + Room SQLCipher"
                                   else if (meshState.isConnected) "Malla activa • ${meshState.connectedDeviceName ?: "Enlazado"}" 
                                   else "Bóveda cifrada • ${if (pendingCount > 0) "$pendingCount en cola" else "En línea"}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (activeSessionId != "global") {
                        TextButton(onClick = { viewModel.selectSession("global", "Canal Malla Global") }) {
                            Text("Global", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Botón Crear Grupo Táctico
                    IconButton(
                        onClick = { showCreateGroupDialog = true },
                        modifier = Modifier.testTag("btn_create_group_action")
                    ) {
                        Icon(Icons.Default.GroupAdd, contentDescription = "Crear Grupo", tint = TacticalEmeraldSecondary)
                    }

                    // Botón Personalizar Fondo y Burbujas
                    IconButton(
                        onClick = { showCustomizationDialog = true },
                        modifier = Modifier.testTag("btn_customize_thread_action")
                    ) {
                        Icon(Icons.Default.Palette, contentDescription = "Personalizar Hilo", tint = TacticalAmberTertiary)
                    }

                    // Botón Buscar
                    IconButton(
                        onClick = { viewModel.toggleSearch(!isSearchActive) },
                        modifier = Modifier.testTag("toggle_chat_search_btn")
                    ) {
                        Icon(
                            if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Buscar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (onOpenCryptoKeys != null) {
                        IconButton(onClick = onOpenCryptoKeys, modifier = Modifier.testTag("chat_open_crypto_keys_btn")) {
                            Icon(Icons.Default.Key, contentDescription = "Bóveda de Claves E2EE", tint = TacticalCyanPrimary)
                        }
                    }

                    if (onOpenContacts != null) {
                        IconButton(onClick = onOpenContacts, modifier = Modifier.testTag("chat_open_contacts_btn")) {
                            Icon(Icons.Default.PersonSearch, contentDescription = "Contactos", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Botón Verificación Double Ratchet (Safety Number)
                    IconButton(
                        onClick = { showSafetyNumberDialog = true },
                        modifier = Modifier.testTag("chat_safety_number_btn")
                    ) {
                        Icon(
                            if (isSafetyVerified) Icons.Default.VerifiedUser else Icons.Default.Security,
                            contentDescription = "Número de Seguridad Double Ratchet",
                            tint = if (isSafetyVerified) TacticalEmeraldSecondary else TacticalCyanPrimary
                        )
                    }

                    // Botón Bloqueo Rápido de Bóveda (BiometricPrompt)
                    IconButton(
                        onClick = { viewModel.lockVault() },
                        modifier = Modifier.testTag("btn_quick_lock_vault")
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = "Bloquear Bóveda", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(activeWallpaperBrush)
        ) {

            // 1. INDICADOR DE ESTADO DE LA MALLA BLUETOOTH Y COLA OFFLINE
            MeshConnectionBanner(
                meshState = meshState,
                pendingCount = pendingCount,
                onBroadcastWelcome = { viewModel.broadcastWelcomeHandshake() },
                onSyncQueue = { viewModel.triggerManualSync() }
            )

            // 2. LISTA DE NODOS CERCANOS DESCUBIERTOS (PEERS)
            if (discoveredPeers.isNotEmpty()) {
                DiscoveredPeersHeader(peers = discoveredPeers, onGreetPeer = { viewModel.broadcastWelcomeHandshake() })
            }

            // 3. BARRA DE BÚSQUEDA DE TEXTO COMPLETO (ROOM FTS)
            AnimatedVisibility(
                visible = isSearchActive,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text("Buscar en Room (FTS)...", fontSize = 13.sp) },
                            modifier = Modifier.weight(1f).testTag("chat_search_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // 4. INDICADOR DE PROGRESO DE SUBIDA DE FOTO A FIREBASE STORAGE
            AnimatedVisibility(
                visible = isUploadingPhoto,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Subiendo multimedia a Firebase Storage... $uploadProgress%",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            LinearProgressIndicator(
                                progress = { uploadProgress / 100f },
                                modifier = Modifier.fillMaxWidth().height(4.dp).padding(top = 4.dp),
                            )
                        }
                    }
                }
            }

            // 5. BANNER DE ESTADO VISUAL / ANIMACIÓN DE ONDA DE NOTAS DE VOZ & GEMINI
            AnimatedVisibility(
                visible = isRecordingVoice || isTranscribingVoice,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                AiTranscriptionWaveformPanel(
                    isRecording = isRecordingVoice,
                    isTranscribing = isTranscribingVoice,
                    statusText = transcriptionStatusText,
                    waveformAmplitudes = waveformAmplitudes,
                    durationSeconds = (recordingDurationMs / 1000).toInt()
                )
            }

            // 6. LISTA DE MENSAJES (HISTORIAL EN TIEMPO REAL DESDE ROOM CIFRADO)
            LazyColumn(
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (searchQuery.isEmpty()) "Bóveda cifrada activa.\n¡Envía notas de voz, fotos, texto o crea un grupo táctico!" else "No se encontraron coincidencias para '$searchQuery'.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(messages, key = { it.id }) { msg ->
                        val isThisMessagePlaying = isAudioPlaying && playingAudioMessageId == msg.id
                        val progress = if (isThisMessagePlaying && audioTotalDurationMs > 0) {
                            (audioPlaybackPosMs.toFloat() / audioTotalDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        ChatMessageItem(
                            msg = msg,
                            customization = customization,
                            isPlaying = isThisMessagePlaying,
                            playbackProgress = progress,
                            currentPosMs = if (isThisMessagePlaying) audioPlaybackPosMs else 0L,
                            totalDurationMs = if (isThisMessagePlaying && audioTotalDurationMs > 0) audioTotalDurationMs else msg.mediaDurationMs,
                            onPlayPauseAudio = {
                                viewModel.toggleAudioPlayback(msg)
                            },
                            onPhotoClick = { url -> selectedMediaPreview = url }
                        )
                    }
                }
            }

            // 7. BARRA INFERIOR DE ENTRADA CON NOTAS DE VOZ, ADJUNTAR FOTOS Y BOTÓN PTT
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            ) {
                if (isRecordingVoice) {
                    // Modo de Grabación de Nota de Voz en Vivo
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = { viewModel.cancelVoiceNoteRecording() },
                            modifier = Modifier.testTag("btn_cancel_voice_record")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Cancelar Grabación", tint = MaterialTheme.colorScheme.error)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            val totalSec = (recordingDurationMs / 1000).toInt()
                            val min = totalSec / 60
                            val sec = totalSec % 60
                            Text(
                                String.format(Locale.getDefault(), "%02d:%02d", min, sec),
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("🎙️ Grabando nota de voz...", fontSize = 12.sp)
                        }

                        IconButton(
                            onClick = { viewModel.stopAndSendVoiceNote() },
                            modifier = Modifier.testTag("btn_send_voice_note")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar Audio", tint = TacticalEmeraldSecondary)
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Botón Adjuntar Foto / Galería
                        IconButton(
                            onClick = { photoPickerLauncher.launch("image/*") },
                            modifier = Modifier.size(40.dp).testTag("attach_photo_button")
                        ) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Adjuntar Fotografía",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        OutlinedTextField(
                            value = messageInput,
                            onValueChange = { messageInput = it },
                            modifier = Modifier.weight(1f).testTag("chat_message_input"),
                            placeholder = { Text("Escribe o envía una foto/audio...") },
                            shape = RoundedCornerShape(24.dp),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Botón Grabar Nota de Voz (Micrófono)
                        Surface(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .clickable {
                                    if (!hasAudioPermission) {
                                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    } else {
                                        viewModel.startVoiceNoteRecording()
                                    }
                                }
                                .testTag("voice_note_record_button"),
                            shape = CircleShape,
                            color = TacticalCyanPrimary.copy(alpha = 0.2f)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = "Grabar Nota de Voz",
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Botón Enviar Mensaje de Texto
                        FloatingActionButton(
                            onClick = {
                                if (messageInput.isNotBlank()) {
                                    viewModel.sendMessage(messageInput)
                                    messageInput = ""
                                }
                            },
                            shape = CircleShape,
                            containerColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp).testTag("chat_send_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }

    // 8. DIÁLOGO VISOR DE FOTOGRAFÍA EN PANTALLA COMPLETA
    if (selectedMediaPreview != null) {
        Dialog(
            onDismissRequest = { selectedMediaPreview = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black.copy(alpha = 0.95f)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(selectedMediaPreview)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Fotografía Táctica Full",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    IconButton(
                        onClick = { selectedMediaPreview = null },
                        modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
            }
        }
    }

    // 9. DIÁLOGO DE PERSONALIZACIÓN DE FONDOS Y BURBUJAS (ROOM PERSISTENCE)
    if (showCustomizationDialog) {
        ThreadCustomizationDialog(
            currentCustomization = customization,
            onDismiss = { showCustomizationDialog = false },
            onSaveCustomization = { preset, outColor, inColor ->
                viewModel.saveThreadCustomization(
                    wallpaperPreset = preset,
                    outgoingBubbleColorHex = outColor,
                    incomingBubbleColorHex = inColor
                )
            }
        )
    }

    // 10. DIÁLOGO DE CREACIÓN DE GRUPO MULTI-USUARIO
    if (showCreateGroupDialog) {
        CreateGroupChatDialog(
            availableContacts = allContacts,
            onDismiss = { showCreateGroupDialog = false },
            onCreateGroup = { title, desc, icon, members ->
                viewModel.createGroupChat(
                    title = title,
                    description = desc,
                    icon = icon,
                    members = members
                )
            }
        )
    }

    // 11. DIÁLOGO DE ADMINISTRACIÓN DE GRUPO
    if (showGroupAdminDialog && currentGroupSession != null) {
        GroupInfoAndAdminDialog(
            groupSession = currentGroupSession!!,
            currentUserId = currentUserId,
            availableContacts = allContacts,
            onDismiss = { showGroupAdminDialog = false },
            onAddMember = { uid, name ->
                viewModel.addMemberToGroup(uid, name) { _, _ -> }
            },
            onRemoveMember = { uid ->
                viewModel.removeMemberFromGroup(uid) { _, _ -> }
            },
            onPromoteAdmin = { uid ->
                viewModel.promoteMemberToAdmin(uid) { _, _ -> }
            },
            onDemoteAdmin = { uid ->
                viewModel.demoteMemberAdmin(uid) { _, _ -> }
            },
            onUpdateInfo = { title, desc, icon ->
                viewModel.updateGroupDetails(title, desc, icon) { _, _ -> }
            }
        )
    }

    // 12. DIÁLOGO DE NÚMERO DE SEGURIDAD FUERA DE BANDA (DOUBLE RATCHET)
    if (showSafetyNumberDialog) {
        DoubleRatchetSafetyNumberDialog(
            safetyNumber = safetyNumber,
            chainStep = ratchetStep,
            isVerified = isSafetyVerified,
            onToggleVerified = { viewModel.toggleSafetyNumberVerification() },
            onDismiss = { showSafetyNumberDialog = false }
        )
    }
}

/**
 * Diálogo de Verificación Criptográfica Fuera de Banda (OOB) Double Ratchet
 */
@Composable
fun DoubleRatchetSafetyNumberDialog(
    safetyNumber: String,
    chainStep: Int,
    isVerified: Boolean,
    onToggleVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth().padding(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (isVerified) TacticalEmeraldSecondary.copy(alpha = 0.2f) else TacticalCyanPrimary.copy(alpha = 0.2f),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isVerified) Icons.Default.VerifiedUser else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isVerified) TacticalEmeraldSecondary else TacticalCyanPrimary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Verificación de Clave (OOB)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    "Protocolo Double Ratchet (Signal) • Forward Secrecy",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "NÚMERO DE SEGURIDAD (60 DÍGITOS)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            safetyNumber,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF1F5F9),
                            lineHeight = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Trinquete KDF: Paso #$chainStep",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        "ECDH secp256r1",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TacticalEmeraldSecondary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onToggleVerified,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isVerified) TacticalEmeraldSecondary else TacticalCyanPrimary,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_toggle_verify_safety_number")
                ) {
                    Icon(
                        if (isVerified) Icons.Default.CheckCircle else Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isVerified) "Contacto Verificado ✓" else "Marcar como Verificado",
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Cerrar", color = Color(0xFF94A3B8))
                }
            }
        }
    }
}

/**
 * Banner de conexión Bluetooth Mesh y estado de encolado offline en Room/SQLCipher.
 */
@Composable
fun MeshConnectionBanner(
    meshState: com.example.domain.hardware.MeshConnectionState,
    pendingCount: Int,
    onBroadcastWelcome: () -> Unit,
    onSyncQueue: () -> Unit
) {
    val isOnline = meshState.isConnected
    val containerColor = if (isOnline) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    val contentColor = if (isOnline) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(
                    if (isOnline) Icons.Default.Bluetooth else Icons.Default.BluetoothSearching,
                    contentDescription = "Bluetooth Mesh",
                    tint = if (isOnline) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = if (isOnline) "🟢 Malla P2P Conectada (${meshState.activePeersCount} nodos)" 
                               else "🟡 Modo Offline: Transmisión en Espera",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = contentColor
                    )
                    Text(
                        text = if (pendingCount > 0) "🔒 $pendingCount mensajes en cola local (SQLCipher)" 
                               else if (isOnline) "Enlace RFCOMM de baja latencia activo" 
                               else "Los mensajes se encolan automáticamente en Room",
                        fontSize = 10.sp,
                        color = contentColor.copy(alpha = 0.8f)
                    )
                }
            }

            Row {
                if (pendingCount > 0) {
                    FilledTonalButton(
                        onClick = onSyncQueue,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sincronizar", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

/**
 * Carrusel interactivo de pares descubiertos automáticamente con botón de saludo de bienvenida.
 */
@Composable
fun DiscoveredPeersHeader(
    peers: List<com.example.domain.hardware.DiscoveredPeer>,
    onGreetPeer: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nodos Descubiertos en el Área (${peers.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "👋 Saludo Automático Activo",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(peers) { peer ->
                    SuggestionChip(
                        onClick = onGreetPeer,
                        label = { Text(peer.name, fontSize = 11.sp) },
                        icon = {
                            Icon(
                                Icons.Default.Handshake,
                                contentDescription = "Saludar",
                                modifier = Modifier.size(14.dp),
                                tint = if (peer.isConnected) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }
            }
        }
    }
}

/**
 * Panel animado visual de formas de onda (Waveform) y estado durante la grabación y transcripción.
 */
@Composable
fun AiTranscriptionWaveformPanel(
    isRecording: Boolean,
    isTranscribing: Boolean,
    statusText: String,
    waveformAmplitudes: List<Float>,
    durationSeconds: Int = 0
) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave_anim")
    val wavePulse by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val primaryColor = TacticalCyanPrimary
    val errorColor = MaterialTheme.colorScheme.error
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    if (isRecording) Icons.Default.Mic else Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = if (isRecording) errorColor else primaryColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val min = durationSeconds / 60
                val sec = durationSeconds % 60
                Text(
                    text = if (isRecording) "🎙️ Grabando audio táctico (${String.format(Locale.getDefault(), "%02d:%02d", min, sec)})" 
                           else "🧠 Gemini AI: $statusText",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = if (isRecording) errorColor else primaryColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas de Renderizado de Onda Dinámica (Soundwave Bars)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                val count = 28
                val barWidth = (size.width / (count * 1.5f)).coerceAtLeast(3f)
                val spacing = barWidth * 0.5f

                for (i in 0 until count) {
                    val x = i * (barWidth + spacing) + 8f
                    val ampIndex = i % waveformAmplitudes.size.coerceAtLeast(1)
                    val baseAmp = waveformAmplitudes.getOrElse(ampIndex) { 0.4f }
                    
                    val heightFactor = if (isRecording) {
                        (baseAmp * ((i % 3) + 1) * 0.4f).coerceIn(0.15f, 0.95f)
                    } else {
                        val phase = ((i + (wavePulse * 10f)) % 6f) / 6f
                        (phase * wavePulse).coerceIn(0.2f, 0.9f)
                    }

                    val barHeight = size.height * heightFactor
                    val y = (size.height - barHeight) / 2

                    val barBrush = Brush.verticalGradient(
                        colors = if (isRecording) {
                            listOf(errorColor, Color(0xFFFF8A80))
                        } else {
                            listOf(primaryColor, tertiaryColor)
                        }
                    )

                    drawRoundRect(
                        brush = barBrush,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
                    )
                }
            }
        }
    }
}

/**
 * Elemento de Mensaje individual con soporte para:
 * - Notas de voz / Reproductor de Audio interactivo (Firebase Storage / Room)
 * - Fotografías multimedia (Coil)
 * - Rastreador de estado en tiempo real ('sent' ✓, 'delivered' ✓✓, 'read' ✓✓)
 * - Colores de burbuja personalizables desde Room.
 */
@Composable
fun ChatMessageItem(
    msg: ChatMessageEntity,
    customization: ThreadCustomizationEntity?,
    isPlaying: Boolean = false,
    playbackProgress: Float = 0f,
    currentPosMs: Long = 0L,
    totalDurationMs: Long = 0L,
    onPlayPauseAudio: () -> Unit = {},
    onPhotoClick: ((String) -> Unit)? = null
) {
    val isMe = msg.senderId == "local_device" || msg.senderId == "local_node" || msg.senderName.startsWith("Yo")
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(msg.timestamp))
    val context = LocalContext.current

    val isAudio = msg.mediaType == "AUDIO"
    val hasPhoto = msg.mediaType == "IMAGE" || (!isAudio && (!msg.mediaUrl.isNullOrBlank() || !msg.localFilePath.isNullOrBlank()))
    val mediaSource = msg.localFilePath?.takeIf { it.isNotBlank() } ?: msg.mediaUrl

    // Color de burbuja personalizado o por defecto
    val bubbleColor: Color = remember(customization, isMe) {
        val hex = if (isMe) customization?.outgoingBubbleColorHex else customization?.incomingBubbleColorHex
        if (!hex.isNullOrBlank()) {
            try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                if (isMe) Color(0xFF00838F) else Color(0xFF263238)
            }
        } else {
            if (isMe) Color(0xFF00838F) else Color(0xFF263238)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
    ) {
        Text(
            text = "${msg.senderName} • $formattedTime",
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isMe) 16.dp else 4.dp,
                bottomEnd = if (isMe) 4.dp else 16.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 320.dp),
            tonalElevation = 3.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {

                // 1. REPRODUCTOR DE NOTA DE VOZ (AUDIO PLAYBACK)
                if (isAudio) {
                    AudioMessagePlayer(
                        isPlaying = isPlaying,
                        playbackProgress = playbackProgress,
                        currentPosMs = currentPosMs,
                        totalDurationMs = totalDurationMs,
                        onPlayPause = onPlayPauseAudio
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 2. FOTOGRAFÍA MULTIMEDIA CON COIL
                if (hasPhoto && !mediaSource.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick?.invoke(mediaSource) }
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(mediaSource)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto en chat",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(180.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 3. TEXTO DEL MENSAJE
                if (msg.text.isNotBlank() && msg.text != "📷 Fotografía Táctica" && msg.text != "🎙️ Nota de voz táctica") {
                    Text(
                        text = msg.text,
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }

                // 4. RASTREADOR DE ESTADO DEL MENSAJE (SENT, DELIVERED, READ)
                if (isMe) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when (msg.status.uppercase()) {
                            "READ" -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Leído",
                                    modifier = Modifier.size(14.dp),
                                    tint = TacticalEmeraldSecondary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Leído",
                                    fontSize = 9.sp,
                                    color = TacticalEmeraldSecondary,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            "DELIVERED" -> {
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = "Entregado",
                                    modifier = Modifier.size(14.dp),
                                    tint = TacticalCyanPrimary
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Entregado",
                                    fontSize = 9.sp,
                                    color = TacticalCyanPrimary,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            "SENT" -> {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Enviado",
                                    modifier = Modifier.size(13.dp),
                                    tint = Color.White.copy(alpha = 0.8f)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    "Enviado",
                                    fontSize = 9.sp,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            else -> {
                                if (msg.status == "PENDING") {
                                    Icon(
                                        Icons.Default.Schedule,
                                        contentDescription = "En cola offline (Room)",
                                        modifier = Modifier.size(12.dp),
                                        tint = TacticalAmberTertiary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "En cola",
                                        fontSize = 9.sp,
                                        color = TacticalAmberTertiary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Enviado",
                                        modifier = Modifier.size(13.dp),
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        "Enviado",
                                        fontSize = 9.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Componente Interactivo de Reproducción de Nota de Voz
 */
@Composable
fun AudioMessagePlayer(
    isPlaying: Boolean,
    playbackProgress: Float,
    currentPosMs: Long,
    totalDurationMs: Long,
    onPlayPause: () -> Unit
) {
    Surface(
        color = Color.Black.copy(alpha = 0.25f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(TacticalCyanPrimary)
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                LinearProgressIndicator(
                    progress = { if (isPlaying) playbackProgress else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = TacticalCyanPrimary,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val posSec = currentPosMs / 1000
                    val totalSec = if (totalDurationMs > 0) totalDurationMs / 1000 else 0
                    val timeText = if (isPlaying && totalDurationMs > 0) {
                        String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", posSec / 60, posSec % 60, totalSec / 60, totalSec % 60)
                    } else if (totalDurationMs > 0) {
                        String.format(Locale.getDefault(), "%02d:%02d", totalSec / 60, totalSec % 60)
                    } else {
                        "🎙️ Nota de voz"
                    }

                    Text(
                        text = timeText,
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "AAC • E2EE",
                        fontSize = 9.sp,
                        color = TacticalEmeraldSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
