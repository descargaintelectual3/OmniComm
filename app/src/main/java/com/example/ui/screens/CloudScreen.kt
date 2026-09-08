package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.local.entities.CachedFileEntity
import com.example.domain.storage.CloudVaultFileMetadata
import com.example.domain.storage.CloudVaultTransferState
import com.example.domain.storage.CloudVaultFileVersion
import com.example.domain.storage.ConflictResolutionPolicy
import com.example.domain.storage.CloudSyncSummary
import com.example.ui.viewmodels.CloudViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudScreen(
    onBack: () -> Unit,
    viewModel: CloudViewModel = viewModel()
) {
    val files by viewModel.files.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
    val selectedFileContent by viewModel.selectedFileContent.collectAsStateWithLifecycle()
    val cloudFiles by viewModel.cloudVaultFiles.collectAsStateWithLifecycle()
    val transferState by viewModel.cloudTransferState.collectAsStateWithLifecycle()
    val syncSummary by viewModel.syncSummary.collectAsStateWithLifecycle()
    val fileVersionsMap by viewModel.fileVersionsMap.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var viewingFile by remember { mutableStateOf<CachedFileEntity?>(null) }
    var fileToDelete by remember { mutableStateOf<CachedFileEntity?>(null) }
    var showCommitDialogForFile by remember { mutableStateOf<CachedFileEntity?>(null) }

    val filteredFiles = remember(files, searchQuery) {
        if (searchQuery.isBlank()) files
        else files.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    val filteredCloudFiles = remember(cloudFiles, searchQuery) {
        if (searchQuery.isBlank()) cloudFiles
        else cloudFiles.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Bóveda & Nube Segura", fontWeight = FontWeight.Bold)
                        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("cloud_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        viewModel.updateStorageStats()
                        viewModel.loadCloudVaultFiles()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Actualizar Almacenamiento")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_file_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Crear Documento Seguro")
            }
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            val isWideScreen = maxWidth > 600.dp
            val gridColumns = if (isWideScreen) 4 else 2

            Column(modifier = Modifier.fillMaxSize()) {
                // Banner de Transferencia Cifrada
                when (val state = transferState) {
                    is CloudVaultTransferState.Transferring -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        if (state.isUpload) "🔒 Cifrando y subiendo: ${state.fileName}"
                                        else "📥 Descargando y descifrando: ${state.fileName}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("${state.progressPercent}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { state.progressPercent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                    is CloudVaultTransferState.Completed -> {
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "✅ ${state.message}",
                                fontSize = 12.sp,
                                modifier = Modifier.padding(10.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    is CloudVaultTransferState.Failed -> {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "❌ Error en ${state.fileName}: ${state.error}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    else -> {}
                }

                // Pestañas de Navegación Local vs Cloud Vault
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Bóveda Local (${files.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_local_vault")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { 
                            selectedTab = 1
                            viewModel.loadCloudVaultFiles()
                        },
                        text = { Text("Cloud Vault (${cloudFiles.size})", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_cloud_vault")
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Delta Sync & Versiones", fontWeight = FontWeight.Bold) },
                        icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.testTag("tab_cloud_sync_versions")
                    )
                }

                // Barra de Estadísticas y Búsqueda
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    when (selectedTab) {
                                        0 -> "Almacenamiento Local Cifrado (SQLCipher)"
                                        1 -> "Bóveda Nube Remota (Firebase Storage AES-256-GCM)"
                                        else -> "Sincronización en Segundo Plano & Linaje Criptográfico"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(storageStats, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selectedTab != 2) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Buscar en la bóveda...", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("vault_search_input")
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        // Vista de Archivos Locales
                    if (filteredFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.InsertDriveFile, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    if (searchQuery.isBlank()) "La bóveda no contiene archivos creados todavía."
                                    else "No se encontraron archivos coincidentes.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showCreateDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Crear Primer Documento")
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(gridColumns),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .testTag("vault_files_grid"),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filteredFiles, key = { it.fileId }) { file ->
                                FileVaultCard(
                                    file = file,
                                    onClick = {
                                        viewingFile = file
                                        viewModel.readFileContent(file)
                                    },
                                    onDelete = { fileToDelete = file },
                                    onUploadEncrypted = {
                                        viewModel.uploadEncryptedToFirebase(file)
                                    }
                                )
                            }
                        }
                    }
                    }
                    1 -> {
                        // Vista de Archivos en la Bóveda de la Nube (Firebase Storage)
                        if (filteredCloudFiles.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Cloud, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No hay archivos en la Bóveda Nube aún.\nPuedes cifrar y respaldar archivos locales con AES-256-GCM hacia Firebase Storage.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedButton(onClick = { selectedTab = 0 }) {
                                        Icon(Icons.Default.Lock, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Ver Archivos Locales para Subir")
                                    }
                                }
                            }
                        } else {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(gridColumns),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp)
                                    .testTag("cloud_vault_grid"),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(filteredCloudFiles, key = { it.fileId }) { cloudFile ->
                                    CloudVaultItemCard(
                                        file = cloudFile,
                                        onDownloadAndDecrypt = {
                                            viewModel.downloadAndDecryptFromFirebase(cloudFile)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Vista de Sincronización en Segundo Plano y Versionado Criptográfico
                        CloudVaultSyncAndVersioningView(
                            syncSummary = syncSummary,
                            fileVersionsMap = fileVersionsMap,
                            localFiles = files,
                            onTriggerSync = { viewModel.triggerBackgroundDeltaSync() },
                            onSelectPolicy = { viewModel.setConflictResolutionPolicy(it) },
                            onCommitVersion = { file -> showCommitDialogForFile = file },
                            onRollback = { fileId, vNum, path -> viewModel.rollbackFileVersion(fileId, vNum, path) }
                        )
                    }
                }
            }
        }
    }

    // Modal para crear nuevo documento
    if (showCreateDialog) {
        CreateDocumentDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { title, content ->
                viewModel.createDocumentFile(title, content)
                showCreateDialog = false
            }
        )
    }

    // Modal para ver y leer contenido del archivo
    if (viewingFile != null) {
        FileContentDialog(
            file = viewingFile!!,
            content = selectedFileContent,
            onDismiss = {
                viewingFile = null
                viewModel.clearSelectedFileContent()
            },
            onDelete = {
                fileToDelete = viewingFile
                viewingFile = null
                viewModel.clearSelectedFileContent()
            }
        )
    }

    // Confirmación de eliminación
    if (fileToDelete != null) {
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Eliminar Archivo") },
            text = { Text("¿Deseas eliminar permanentemente \"${fileToDelete!!.fileName}\" de la bóveda local cifrada?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(fileToDelete!!)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showCommitDialogForFile != null) {
        CommitVersionDialog(
            file = showCommitDialogForFile!!,
            onDismiss = { showCommitDialogForFile = null },
            onCommit = { note ->
                viewModel.commitVersion(showCommitDialogForFile!!, note)
                showCommitDialogForFile = null
            }
        )
    }
}

@Composable
fun FileVaultCard(
    file: CachedFileEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onUploadEncrypted: () -> Unit = {}
) {
    val fileIcon = getFileIcon(file.fileName)
    val formattedDate = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(file.downloadedAt))
    val sizeKb = file.sizeBytes / 1024f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("file_card_${file.fileName}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(fileIcon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Row {
                    IconButton(
                        onClick = onUploadEncrypted,
                        modifier = Modifier.size(28.dp).testTag("upload_encrypted_${file.fileName}")
                    ) {
                        Icon(
                            Icons.Default.CloudUpload,
                            contentDescription = "Cifrar y subir a Cloud Vault",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                file.fileName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (sizeKb < 1024) String.format(Locale.getDefault(), "%.1f KB", sizeKb)
                    else String.format(Locale.getDefault(), "%.2f MB", sizeKb / 1024f),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formattedDate, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
fun CloudVaultItemCard(
    file: CloudVaultFileMetadata,
    onDownloadAndDecrypt: () -> Unit
) {
    val formattedDate = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(file.uploadedAtTimestamp))
    val sizeKb = file.originalSizeBytes / 1024f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("cloud_file_card_${file.fileId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        file.encryptionAlgorithm,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                file.fileName,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "SHA-256: ${file.sha256Checksum.take(12)}...",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (sizeKb < 1024) String.format(Locale.getDefault(), "%.1f KB", sizeKb)
                    else String.format(Locale.getDefault(), "%.2f MB", sizeKb / 1024f),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onDownloadAndDecrypt,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("download_decrypt_${file.fileId}")
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Descifrar", fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
fun CreateDocumentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo Documento Seguro", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Nombre del archivo") },
                    placeholder = { Text("bitacora_malla.txt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenido / Notas Cifradas") },
                    placeholder = { Text("Escribe notas tácticas, claves o telemetría...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    maxLines = 8
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalTitle = title.ifBlank { "nota_${System.currentTimeMillis() % 10000}.txt" }
                    onSave(finalTitle, content)
                },
                enabled = content.isNotBlank() || title.isNotBlank()
            ) {
                Text("Guardar en Bóveda")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun FileContentDialog(
    file: CachedFileEntity,
    content: String?,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(file.downloadedAt))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(file.fileName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Tamaño: ${file.sizeBytes} bytes • Creado: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 320.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(12.dp)) {
                    if (content == null) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp).align(Alignment.Center))
                    } else {
                        Text(
                            text = content,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Eliminar")
            }
        }
    )
}

fun getFileIcon(fileName: String): ImageVector {
    val lower = fileName.lowercase(Locale.ROOT)
    return when {
        lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") -> Icons.Default.Image
        lower.endsWith(".json") || lower.endsWith(".xml") || lower.endsWith(".kt") -> Icons.Default.Code
        lower.endsWith(".txt") || lower.endsWith(".log") || lower.endsWith(".md") -> Icons.Default.Description
        else -> Icons.Default.InsertDriveFile
    }
}

@Composable
fun CloudVaultSyncAndVersioningView(
    syncSummary: CloudSyncSummary,
    fileVersionsMap: Map<String, List<CloudVaultFileVersion>>,
    localFiles: List<CachedFileEntity>,
    onTriggerSync: () -> Unit,
    onSelectPolicy: (ConflictResolutionPolicy) -> Unit,
    onCommitVersion: (CachedFileEntity) -> Unit,
    onRollback: (fileId: String, versionNumber: Int, localPath: String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Tarjeta de Control de Sincronización en Segundo Plano
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Motor Delta Sync en Segundo Plano", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                    if (syncSummary.isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1B5E20).copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, Color(0xFF66BB6A))
                        ) {
                            Text(
                                "Activo (60s)",
                                color = Color(0xFF66BB6A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Text(
                    syncSummary.statusMessage,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Archivos Sincronizados", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${syncSummary.filesSyncedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Conflictos Resueltos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${syncSummary.conflictsResolvedCount}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFA726))
                        }
                    }
                }

                Button(
                    onClick = onTriggerSync,
                    enabled = !syncSummary.isSyncing,
                    modifier = Modifier.fillMaxWidth().testTag("trigger_delta_sync_btn")
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ejecutar Sincronización Delta Inmediata")
                }

                // Selector de Política de Resolución
                Text("Política de Resolución de Conflictos:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ConflictResolutionPolicy.values().forEach { policy ->
                        val isSelected = (syncSummary.activePolicy == policy)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPolicy(policy) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(policy.label, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(policy.description, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sección: Crear Nuevo Commit de Versión para Archivo Local
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Versionar Archivo Local", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Text("Genera un nuevo commit inmutable con checksum SHA-256 e IV cifrado en Cloud Vault:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                localFiles.forEach { file ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.fileName, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${file.sizeBytes / 1024} KB", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            OutlinedButton(
                                onClick = { onCommitVersion(file) },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Commit vN+1", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        // Sección: Historial de Versiones Criptográficas
        Text("Linaje de Versiones Criptográficas (Audit Trail):", fontWeight = FontWeight.Bold, fontSize = 14.sp)

        if (fileVersionsMap.isEmpty()) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                    Text("No hay linajes de versiones registrados aún.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            fileVersionsMap.forEach { (fileId, versions) ->
                val firstVer = versions.firstOrNull()
                val fileName = firstVer?.fileName ?: fileId

                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(fileName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("(${versions.size} commits)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        versions.forEach { ver ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (ver.isCurrentVersion) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (ver.isCurrentVersion) MaterialTheme.colorScheme.primary else Color.Transparent),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (ver.isCurrentVersion) MaterialTheme.colorScheme.primary else Color.DarkGray
                                            ) {
                                                Text(
                                                    if (ver.isCurrentVersion) "v${ver.versionNumber} ACTUAL" else "v${ver.versionNumber}",
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Text(ver.authorCallsign, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        if (!ver.isCurrentVersion) {
                                            TextButton(
                                                onClick = {
                                                    val localMatch = localFiles.find { it.fileId == ver.fileId }
                                                    val path = localMatch?.localAbsolutePath ?: ""
                                                    onRollback(ver.fileId, ver.versionNumber, path)
                                                },
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Rollback", fontSize = 10.sp)
                                            }
                                        }
                                    }

                                    Text("\"${ver.commitNote}\"", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    Text("SHA-256: ${ver.sha256Checksum}", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CommitVersionDialog(
    file: CachedFileEntity,
    onDismiss: () -> Unit,
    onCommit: (commitNote: String) -> Unit
) {
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Nuevo Commit de Versión")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Archivo: ${file.fileName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text("Introduce una nota descriptiva para este punto de restauración inmutable en Cloud Vault:", fontSize = 11.sp)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ej: Actualización de coordenadas MGRS") },
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onCommit(if (note.isBlank()) "Commit de versión manual" else note)
                }
            ) {
                Text("Crear Commit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}


