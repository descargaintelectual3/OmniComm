package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.local.entities.ContactEntity
import com.example.domain.models.EncryptedChatSession
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.viewmodels.ContactFilter
import com.example.ui.viewmodels.ContactViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    onBack: () -> Unit,
    onStartChat: (EncryptedChatSession) -> Unit,
    onStartVideo: (String) -> Unit,
    onOpenLogs: (() -> Unit)? = null,
    contactViewModel: ContactViewModel = viewModel()
) {
    val contacts by contactViewModel.filteredContacts.collectAsStateWithLifecycle()
    val searchQuery by contactViewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by contactViewModel.activeFilter.collectAsStateWithLifecycle()
    val onlineCount by contactViewModel.onlineCount.collectAsStateWithLifecycle()
    val isAdding by contactViewModel.isAddingContact.collectAsStateWithLifecycle()
    val addStatus by contactViewModel.addContactStatus.collectAsStateWithLifecycle()
    val currentUserProfile by contactViewModel.currentUserProfile.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var addQueryInput by remember { mutableStateOf("") }
    var addAliasInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Contactos & Nodos Tácticos",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "$onlineCount en línea • Sincronización Firestore + Room SQLCipher",
                            fontSize = 11.sp,
                            color = TacticalEmeraldSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_contacts")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    if (onOpenLogs != null) {
                        IconButton(
                            onClick = onOpenLogs,
                            modifier = Modifier.testTag("btn_logs_contacts")
                        ) {
                            Icon(Icons.Default.Dns, contentDescription = "Monitor de Telemetría", tint = TacticalCyanPrimary)
                        }
                    }
                    IconButton(
                        onClick = { 
                            showAddDialog = true 
                            contactViewModel.clearStatus()
                        },
                        modifier = Modifier.testTag("btn_add_contact_topbar")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Añadir Contacto", tint = TacticalCyanPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    showAddDialog = true 
                    contactViewModel.clearStatus()
                },
                containerColor = TacticalCyanPrimary,
                contentColor = Color.Black,
                modifier = Modifier.testTag("fab_add_contact")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Añadir Contacto")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Buscador
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    TextField(
                        value = searchQuery,
                        onValueChange = { contactViewModel.setSearchQuery(it) },
                        placeholder = { Text("Buscar por alias, correo o ID táctico...", fontSize = 13.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_search_contacts"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { contactViewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar búsqueda")
                        }
                    }
                }
            }

            // Chips de filtrado
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactFilter.values().forEach { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { contactViewModel.setFilter(filter) },
                        label = {
                            Text(
                                text = filter.label,
                                fontSize = 12.sp,
                                fontWeight = if (activeFilter == filter) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = if (filter == ContactFilter.ONLINE) {
                            {
                                Surface(
                                    shape = CircleShape,
                                    color = TacticalEmeraldSecondary,
                                    modifier = Modifier.size(8.dp)
                                ) {}
                            }
                        } else if (filter == ContactFilter.PRIORITY) {
                            {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp), tint = TacticalAmberTertiary)
                            }
                        } else null
                    )
                }
            }

            // Lista de contactos
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = TacticalCyanPrimary.copy(alpha = 0.1f),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(36.dp), tint = TacticalCyanPrimary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No se encontraron contactos para '$searchQuery'" else "Libreta de Contactos Vacía",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Agrega operadores mediante su correo de Firebase o ID táctico para sincronizar enlaces E2EE.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                showAddDialog = true 
                                contactViewModel.clearStatus()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Añadir Primer Contacto", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(contacts, key = { it.deviceId }) { contact ->
                        ContactItemCard(
                            contact = contact,
                            onStartChat = {
                                contactViewModel.initiateEncryptedChatSession(contact) { session ->
                                    onStartChat(session)
                                }
                            },
                            onStartVideo = { onStartVideo(contact.deviceId) },
                            onTogglePriority = { contactViewModel.togglePriority(contact) },
                            onDelete = { contactViewModel.deleteContact(contact) }
                        )
                    }
                }
            }
        }
    }

    // Modal para Agregar Contacto
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { 
                showAddDialog = false 
                contactViewModel.clearStatus()
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = TacticalCyanPrimary)
                    Text("Añadir Nodo / Contacto", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Busca en Firestore por correo electrónico registrado o identificador de nodo UID.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = addQueryInput,
                        onValueChange = { addQueryInput = it },
                        label = { Text("Correo o ID de Nodo UID") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_add_contact_query"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = addAliasInput,
                        onValueChange = { addAliasInput = it },
                        label = { Text("Alias Personalizado (Opcional)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_add_contact_alias"),
                        shape = RoundedCornerShape(8.dp)
                    )

                    if (addStatus != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = addStatus ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        contactViewModel.addContact(addQueryInput, addAliasInput) { success ->
                            if (success) {
                                addQueryInput = ""
                                addAliasInput = ""
                                showAddDialog = false
                            }
                        }
                    },
                    enabled = !isAdding && addQueryInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.testTag("btn_confirm_add_contact")
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Vincular Nodo", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun ContactItemCard(
    contact: ContactEntity,
    onStartChat: () -> Unit,
    onStartVideo: () -> Unit,
    onTogglePriority: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("contact_card_${contact.deviceId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            if (contact.isPriority) TacticalAmberTertiary.copy(alpha = 0.4f)
            else if (contact.isOnline) TacticalEmeraldSecondary.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar con indicador de presencia
                    Box {
                        Surface(
                            shape = CircleShape,
                            color = if (contact.isOnline) TacticalEmeraldSecondary.copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                            modifier = Modifier.size(46.dp),
                            border = BorderStroke(1.dp, if (contact.isOnline) TacticalEmeraldSecondary else Color.Gray.copy(alpha = 0.3f))
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = contact.avatarIcon.ifBlank { "🛡️" },
                                    fontSize = 20.sp
                                )
                            }
                        }
                        
                        // Badge En línea / Offline
                        Surface(
                            shape = CircleShape,
                            color = if (contact.isOnline) TacticalEmeraldSecondary else Color.Gray,
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .border(2.dp, MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                        ) {}
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = contact.alias,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (contact.isPriority) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Prioritario",
                                    tint = TacticalAmberTertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = if (contact.email.isNotBlank()) contact.email else "UID: ${contact.deviceId.take(12)}...",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Indicador de presencia en tiempo real (RTDB) y Batería
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = if (contact.isOnline) "🟢 En línea" else "⚪ Desconectado",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (contact.isOnline) TacticalEmeraldSecondary else Color.Gray
                            )
                            if (contact.batteryPercent > 0) {
                                Text(
                                    text = "• 🔋 ${contact.batteryPercent}%",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (contact.publicKeyFingerprint.isNotBlank() || contact.rawPublicKeyBase64.isNotBlank() || contact.encryptedPublicKey.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(10.dp), tint = TacticalCyanPrimary)
                                Text(
                                    text = "E2EE [${contact.keyAlgorithm}]: ${if (contact.publicKeyFingerprint.isNotBlank()) contact.publicKeyFingerprint else contact.rawPublicKeyBase64.take(16) + "..."}",
                                    fontSize = 10.sp,
                                    color = TacticalCyanPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onTogglePriority) {
                    Icon(
                        if (contact.isPriority) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Prioridad",
                        tint = if (contact.isPriority) TacticalAmberTertiary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            // Barra de acciones del contacto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Eliminar", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(6.dp))

                FilledTonalButton(
                    onClick = onStartVideo,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                        contentColor = TacticalEmeraldSecondary
                    )
                ) {
                    Icon(Icons.Default.VideoCall, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))

                Button(
                    onClick = onStartChat,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.testTag("btn_chat_${contact.deviceId}")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Chat E2EE", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
