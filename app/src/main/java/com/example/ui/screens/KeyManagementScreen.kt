package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.local.entities.CryptographicKeyEntity
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.viewmodels.KeyFilterOption
import com.example.ui.viewmodels.KeyManagementViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyManagementScreen(
    onBack: () -> Unit,
    viewModel: KeyManagementViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showGenerateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var inspectingKey by remember { mutableStateOf<CryptographicKeyEntity?>(null) }
    var keyToDelete by remember { mutableStateOf<CryptographicKeyEntity?>(null) }

    // Manejo de notificaciones de estado
    LaunchedEffect(uiState.statusNotification) {
        uiState.statusNotification?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearNotification()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("key_management_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Bóveda de Claves E2EE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "ROOM DB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalEmeraldSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Gestión local de pares asimétricos y claves públicas de malla",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("key_screen_back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.testTag("import_peer_key_btn")
                    ) {
                        Icon(
                            Icons.Default.Download,
                            contentDescription = "Importar Clave Pública",
                            tint = TacticalCyanPrimary
                        )
                    }
                    IconButton(
                        onClick = { showGenerateDialog = true },
                        modifier = Modifier.testTag("open_generate_key_dialog_btn")
                    ) {
                        Icon(
                            Icons.Default.AddModerator,
                            contentDescription = "Generar Nuevo Par",
                            tint = TacticalEmeraldSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showGenerateDialog = true },
                icon = {
                    Icon(
                        Icons.Default.Key,
                        contentDescription = null,
                        tint = Color.Black
                    )
                },
                text = {
                    Text(
                        "Generar Par E2EE",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 13.sp
                    )
                },
                containerColor = TacticalCyanPrimary,
                modifier = Modifier.testTag("generate_key_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Tarjeta de Estado de la Llave Maestra Primaria Activa
            uiState.primaryActiveKey?.let { primaryKey ->
                ActiveMasterKeyBanner(
                    key = primaryKey,
                    onInspect = { inspectingKey = primaryKey },
                    onCopyPublicKey = {
                        viewModel.copyToClipboard("Clave Pública (${primaryKey.alias})", primaryKey.publicKeyBase64)
                    },
                    onCopyFingerprint = {
                        viewModel.copyToClipboard("Huella Digital SHA-256", primaryKey.fingerprint)
                    }
                )
            }

            // 2. Buscador y Filtros
            SearchAndFilterBar(
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.updateSearchQuery(it) },
                activeFilter = uiState.activeFilter,
                onFilterSelected = { viewModel.setFilter(it) },
                keysCount = uiState.keys.size
            )

            // 3. Lista de Claves Almacenadas en Room
            if (uiState.keys.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            text = "No se encontraron claves con los filtros actuales",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = { showGenerateDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                            modifier = Modifier.testTag("empty_generate_key_btn")
                        ) {
                            Text("Generar Nuevo Par", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
                ) {
                    items(uiState.keys, key = { it.keyId }) { keyItem ->
                        CryptographicKeyCard(
                            key = keyItem,
                            onInspect = { inspectingKey = keyItem },
                            onSetPrimary = { viewModel.setAsPrimary(keyItem.keyId) },
                            onRevoke = { viewModel.revokeKey(keyItem.keyId) },
                            onDelete = { keyToDelete = keyItem },
                            onCopyPub = {
                                viewModel.copyToClipboard("Clave Pública (${keyItem.alias})", keyItem.publicKeyBase64)
                            },
                            onCopyFingerprint = {
                                viewModel.copyToClipboard("Huella Digital SHA-256", keyItem.fingerprint)
                            }
                        )
                    }
                }
            }
        }
    }

    // Diálogo para Generar Nuevo Par de Claves
    if (showGenerateDialog) {
        GenerateKeyPairDialog(
            isGenerating = uiState.isGenerating,
            onDismiss = { showGenerateDialog = false },
            onConfirm = { alias, algorithm, purpose, useHw, setPrimary ->
                viewModel.generateNewKeyPair(alias, algorithm, purpose, useHw, setPrimary)
                showGenerateDialog = false
            }
        )
    }

    // Diálogo para Importar Clave Pública Externa
    if (showImportDialog) {
        ImportPublicKeyDialog(
            onDismiss = { showImportDialog = false },
            onConfirm = { alias, algorithm, pubBase64, ownerName, ownerNodeId, notes ->
                viewModel.importPeerPublicKey(alias, algorithm, pubBase64, ownerNodeId, ownerName, notes)
                showImportDialog = false
            }
        )
    }

    // Diálogo de Inspección de Detalles y Huella Táctica
    inspectingKey?.let { key ->
        KeyDetailInspectionDialog(
            key = key,
            onDismiss = { inspectingKey = null },
            onCopy = { label, text -> viewModel.copyToClipboard(label, text) }
        )
    }

    // Diálogo de Confirmación de Eliminación
    keyToDelete?.let { key ->
        AlertDialog(
            onDismissRequest = { keyToDelete = null },
            icon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Eliminar Clave de Room", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
            text = {
                Text(
                    "¿Estás seguro de eliminar '${key.alias}' (${key.keyId}) de la base de datos local?\n" +
                    "Si es una clave de nodo par, no podrás verificar su firma hasta importarla nuevamente.",
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteKey(key.keyId)
                        keyToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_key_btn")
                ) {
                    Text("Eliminar de Room", color = Color.White, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { keyToDelete = null }) {
                    Text("Cancelar", fontSize = 12.sp)
                }
            }
        )
    }
}

@Composable
fun ActiveMasterKeyBanner(
    key: CryptographicKeyEntity,
    onInspect: () -> Unit,
    onCopyPublicKey: () -> Unit,
    onCopyFingerprint: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("active_master_key_banner"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = TacticalCyanPrimary.copy(alpha = 0.08f)
        ),
        border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = CircleShape,
                        color = TacticalCyanPrimary.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.VpnKey,
                                contentDescription = null,
                                tint = TacticalCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "CLAVE MAESTRA ACTIVA (E2EE)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = key.alias,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "PRIMARIA",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalEmeraldSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Huella SHA-256
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "HUELLA SHA-256:",
                            fontSize = 8.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = key.fingerprint,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TacticalCyanPrimary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCopyFingerprint,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("copy_master_fingerprint_btn")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar Huella",
                            tint = TacticalCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Barra de acciones rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onCopyPublicKey,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_copy_pubkey_master"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = TacticalCyanPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copiar Pública", fontSize = 11.sp, color = TacticalCyanPrimary)
                }

                Button(
                    onClick = onInspect,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("btn_inspect_master_key"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Detalles / QR", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SearchAndFilterBar(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    activeFilter: KeyFilterOption,
    onFilterSelected: (KeyFilterOption) -> Unit,
    keysCount: Int
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar por alias, huella, algoritmo o nodo...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar", modifier = Modifier.size(16.dp))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_key_input"),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalCyanPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(KeyFilterOption.values()) { filter ->
                val isSelected = filter == activeFilter
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterSelected(filter) },
                    label = {
                        Text(
                            text = if (isSelected) "${filter.displayName} ($keysCount)" else filter.displayName,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = TacticalCyanPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = TacticalCyanPrimary
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) TacticalCyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("filter_chip_${filter.name}")
                )
            }
        }
    }
}

@Composable
fun CryptographicKeyCard(
    key: CryptographicKeyEntity,
    onInspect: () -> Unit,
    onSetPrimary: () -> Unit,
    onRevoke: () -> Unit,
    onDelete: () -> Unit,
    onCopyPub: () -> Unit,
    onCopyFingerprint: () -> Unit
) {
    var expandedActions by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("key_card_${key.keyId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        border = BorderStroke(
            1.dp,
            when {
                key.isRevoked -> MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                key.isPrimary -> TacticalEmeraldSecondary.copy(alpha = 0.6f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Cabecera de la tarjeta: Título, Algoritmo y Estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            key.isRevoked -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            key.isPrimary -> TacticalEmeraldSecondary.copy(alpha = 0.15f)
                            key.algorithm.contains("KYBER") -> TacticalAmberTertiary.copy(alpha = 0.15f)
                            else -> TacticalCyanPrimary.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = when {
                                    key.isRevoked -> Icons.Default.Block
                                    key.ownerNodeId != "LOCAL_NODE" -> Icons.Default.Person
                                    key.algorithm.contains("KYBER") -> Icons.Default.Shield
                                    else -> Icons.Default.VpnKey
                                },
                                contentDescription = null,
                                tint = when {
                                    key.isRevoked -> MaterialTheme.colorScheme.error
                                    key.isPrimary -> TacticalEmeraldSecondary
                                    key.algorithm.contains("KYBER") -> TacticalAmberTertiary
                                    else -> TacticalCyanPrimary
                                },
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = key.alias,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${key.algorithm} (${key.keySizeBits}b)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TacticalCyanPrimary
                            )
                            Text("•", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                            Text(
                                text = if (key.ownerNodeId == "LOCAL_NODE") "Local" else key.ownerDisplayName,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Badges de estado
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (key.isPrimary) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                        ) {
                            Text(
                                text = "PRIMARIA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalEmeraldSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (key.isHardwareBacked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TacticalCyanPrimary.copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "HW TEE",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (key.isRevoked) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                        ) {
                            Text(
                                text = "REVOCADA",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Huella digital con botón copiar
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHA-256: ${key.fingerprint}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onCopyFingerprint,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("copy_fingerprint_${key.keyId}")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copiar Huella",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Botones de acción principales
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = onCopyPub,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("copy_pub_${key.keyId}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pública", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = onInspect,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("inspect_${key.keyId}"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Detalles", fontSize = 10.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!key.isPrimary && !key.isRevoked && key.ownerNodeId == "LOCAL_NODE") {
                        FilledTonalButton(
                            onClick = onSetPrimary,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("set_primary_${key.keyId}"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = TacticalEmeraldSecondary.copy(alpha = 0.2f),
                                contentColor = TacticalEmeraldSecondary
                            )
                        ) {
                            Text("Activar", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    IconButton(
                        onClick = { expandedActions = !expandedActions },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expandedActions) Icons.Default.ExpandLess else Icons.Default.MoreVert,
                            contentDescription = "Más opciones",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Opciones desplegables avanzadas
            AnimatedVisibility(visible = expandedActions) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (key.notes.isNotBlank()) {
                        Text(
                            text = "Nota: ${key.notes}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "ID: ${key.keyId} • Creada: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(key.createdAt))}",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!key.isRevoked) {
                            TextButton(
                                onClick = {
                                    onRevoke()
                                    expandedActions = false
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = TacticalAmberTertiary)
                            ) {
                                Icon(Icons.Default.Block, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Revocar", fontSize = 10.sp)
                            }
                        }

                        TextButton(
                            onClick = {
                                onDelete()
                                expandedActions = false
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Eliminar de Room", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GenerateKeyPairDialog(
    isGenerating: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (alias: String, algorithm: String, purpose: String, useHardware: Boolean, setPrimary: Boolean) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var selectedAlgorithm by remember { mutableStateOf("RSA-2048") }
    var selectedPurpose by remember { mutableStateOf("E2EE_MESSAGING") }
    var useHardwareKeyStore by remember { mutableStateOf(true) }
    var setAsPrimary by remember { mutableStateOf(true) }

    val algorithms = listOf(
        "RSA-2048" to "RSA 2048 bits (Estándar E2EE)",
        "RSA-4096" to "RSA 4096 bits (Alta Resistencia)",
        "EC-P256" to "Curva Elíptica NIST P-256 (Ligera & Veloz)",
        "KYBER-768" to "ML-KEM Kyber-768 (Post-Cuántica Híbrida)"
    )

    val purposes = listOf(
        "E2EE_MESSAGING" to "Mensajería Cifrada Extremo a Extremo",
        "DIGITAL_SIGNATURE" to "Firma Digital de Mensajes y Órdenes",
        "KEY_EXCHANGE" to "Intercambio de Claves de Malla",
        "POST_QUANTUM" to "Bóveda Cuántico-Resistente"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Key, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Generar Par de Claves E2EE", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Nombre / Alias Táctico") },
                    placeholder = { Text("Ej: Clave Maestra Nodo Bravo") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_key_alias")
                )

                Text("Algoritmo Criptográfico:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                algorithms.forEach { (algo, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedAlgorithm = algo }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedAlgorithm == algo,
                            onClick = { selectedAlgorithm = algo },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text("Propósito Táctico:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                purposes.forEach { (purp, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPurpose = purp }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedPurpose == purp,
                            onClick = { selectedPurpose = purp },
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, fontSize = 11.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Android KeyStore Hardware (TEE)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Protege la clave privada en el enclave seguro del procesador", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = useHardwareKeyStore,
                        onCheckedChange = { useHardwareKeyStore = it },
                        modifier = Modifier.testTag("switch_hardware_keystore")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Activar como Clave Primaria", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Usar inmediatamente para cifrar chats salientes", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = setAsPrimary,
                        onCheckedChange = { setAsPrimary = it },
                        modifier = Modifier.testTag("switch_set_as_primary")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(alias, selectedAlgorithm, selectedPurpose, useHardwareKeyStore, setAsPrimary)
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                modifier = Modifier.testTag("confirm_generate_key_btn")
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generando...", color = Color.Black, fontSize = 12.sp)
                } else {
                    Text("Generar y Guardar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isGenerating) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun ImportPublicKeyDialog(
    onDismiss: () -> Unit,
    onConfirm: (alias: String, algorithm: String, pubBase64: String, ownerName: String, ownerNodeId: String, notes: String) -> Unit
) {
    var alias by remember { mutableStateOf("") }
    var ownerName by remember { mutableStateOf("") }
    var ownerNodeId by remember { mutableStateOf("") }
    var algorithm by remember { mutableStateOf("RSA-2048") }
    var pubBase64 by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Download, contentDescription = null, tint = TacticalCyanPrimary)
                Text("Importar Clave Pública Par", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("Nombre del Operador Par") },
                    placeholder = { Text("Ej: Operador Alfa / Delta-9") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_import_owner_name")
                )

                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias de la Clave") },
                    placeholder = { Text("Ej: Clave Pública Alfa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("input_import_alias")
                )

                OutlinedTextField(
                    value = pubBase64,
                    onValueChange = { pubBase64 = it },
                    label = { Text("Clave Pública (Base64 / X.509)") },
                    placeholder = { Text("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8...") },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth().testTag("input_import_pubkey")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas / Canal de Intercambio (Opcional)") },
                    placeholder = { Text("Ej: Recibida por QR / Bluetooth") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (pubBase64.isNotBlank()) {
                        onConfirm(alias, algorithm, pubBase64, ownerName, ownerNodeId, notes)
                    }
                },
                enabled = pubBase64.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                modifier = Modifier.testTag("confirm_import_key_btn")
            ) {
                Text("Guardar en Room", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 12.sp)
            }
        }
    )
}

@Composable
fun KeyDetailInspectionDialog(
    key: CryptographicKeyEntity,
    onDismiss: () -> Unit,
    onCopy: (String, String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = TacticalCyanPrimary)
                    Text(key.alias, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Inspección de Bóveda & Huella de Seguridad", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Bloque de Huella Digital para verificación Out-of-Band
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("HUELLA DIGITAL SHA-256 (NÚMERO DE SEGURIDAD):", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                        Text(
                            text = key.fingerprint,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { onCopy("Huella SHA-256 (${key.alias})", key.fingerprint) },
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copiar Huella", fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Metadatos
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("• Algoritmo: ${key.algorithm} (${key.keySizeBits} bits)", fontSize = 11.sp)
                    Text("• Propósito: ${key.purpose}", fontSize = 11.sp)
                    Text("• Propietario: ${key.ownerDisplayName} (${key.ownerNodeId})", fontSize = 11.sp)
                    Text("• Almacenamiento: ${if (key.isHardwareBacked) "Android KeyStore TEE Hardware" else "Software Seguro / Room SQLite"}", fontSize = 11.sp)
                    Text("• Mensajes procesados: ${key.usageCount}", fontSize = 11.sp)
                }

                // Clave Pública Base64
                Text("Clave Pública (Base64 X.509):", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 110.dp)
                ) {
                    Box(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = key.publicKeyBase64,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = { onCopy("Clave Pública (${key.alias})", key.publicKeyBase64) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copiar Clave Pública Completa", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", fontSize = 12.sp)
            }
        }
    )
}
