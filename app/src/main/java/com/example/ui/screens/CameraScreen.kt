package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.ai.TacticalEvidenceAnalyzer
import kotlinx.coroutines.launch
import coil.compose.AsyncImage
import com.example.domain.storage.CloudPhotoItem
import com.example.domain.storage.UploadStatus
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import com.example.ui.viewmodels.CameraViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    onBack: () -> Unit,
    viewModel: CameraViewModel = viewModel()
) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    if (cameraPermissionState.status.isGranted) {
        TacticalCameraCaptureContent(onBack = onBack, viewModel = viewModel)
    } else {
        CameraPermissionFallback(
            onRequestPermission = { cameraPermissionState.launchPermissionRequest() },
            onBack = onBack
        )
    }
}

@Composable
private fun CameraPermissionFallback(
    onRequestPermission: () -> Unit,
    onBack: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070B0E))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(28.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = TacticalCyanPrimary.copy(alpha = 0.15f),
                    modifier = Modifier.size(72.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = TacticalCyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Sensor Óptico Requerido",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "OmniComm utiliza CameraX para capturar evidencia táctica y sincronizarla con Firebase Storage.",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("grant_camera_permission_button")
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Autorizar Acceso a Cámara", color = Color.Black, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onBack) {
                    Text("Regresar al Hub Táctico", color = Color.Gray)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TacticalCameraCaptureContent(
    onBack: () -> Unit,
    viewModel: CameraViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val lensFacing by viewModel.lensFacing.collectAsStateWithLifecycle()
    val flashMode by viewModel.flashMode.collectAsStateWithLifecycle()
    val isTorchEnabled by viewModel.isTorchEnabled.collectAsStateWithLifecycle()
    val zoomRatio by viewModel.zoomRatio.collectAsStateWithLifecycle()
    val isAutoUploadEnabled by viewModel.isAutoUploadEnabled.collectAsStateWithLifecycle()
    val tacticalTag by viewModel.tacticalTag.collectAsStateWithLifecycle()
    val isCapturing by viewModel.isCapturing.collectAsStateWithLifecycle()
    val uploadStatus by viewModel.uploadStatus.collectAsStateWithLifecycle()
    val recentPhotos by viewModel.recentPhotos.collectAsStateWithLifecycle()
    val selectedPhoto by viewModel.selectedPhotoForInspection.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val showGalleryDrawer by viewModel.showGalleryDrawer.collectAsStateWithLifecycle()

    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var showReticleGrid by remember { mutableStateOf(true) }
    var showTagDialog by remember { mutableStateOf(false) }
    var focusTapCoords by remember { mutableStateOf<Offset?>(null) }

    // Shutter flash effect
    var shutterFlashActive by remember { mutableStateOf(false) }

    val imageCapture = remember(flashMode) {
        ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
    }

    // Inicializar y vincular CameraX al ciclo de vida
    LaunchedEffect(lensFacing, flashMode) {
        try {
            val cameraProvider = context.getCameraProvider()
            cameraProvider.unbindAll()

            val preview = Preview.Builder().build()
            previewView?.let { pv ->
                preview.setSurfaceProvider(pv.surfaceProvider)
            }

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageCapture
            )
            boundCamera = camera

            // Restaurar zoom y antorcha
            camera.cameraControl.setZoomRatio(zoomRatio)
            camera.cameraControl.enableTorch(isTorchEnabled)
        } catch (e: Exception) {
            Log.e("CameraScreen", "Error vinculando casos de uso de CameraX: ${e.message}", e)
        }
    }

    // Manejar cambios de linterna/antorcha
    LaunchedEffect(isTorchEnabled) {
        boundCamera?.cameraControl?.enableTorch(isTorchEnabled)
    }

    // Manejar cambios de zoom
    LaunchedEffect(zoomRatio) {
        boundCamera?.cameraControl?.setZoomRatio(zoomRatio)
    }

    // Auto-limpiar mensajes de estado
    LaunchedEffect(statusBanner) {
        if (statusBanner != null) {
            delay(4000)
            viewModel.clearBannerMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_viewport_container")
    ) {
        // 1. Visor en Vivo de CameraX (AndroidView PreviewView)
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this

                    // Soporte para enfocar al pulsar en la pantalla
                    setOnTouchListener { v, event ->
                        if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                            focusTapCoords = Offset(event.x, event.y)
                            val factory = SurfaceOrientedMeteringPointFactory(v.width.toFloat(), v.height.toFloat())
                            val point = factory.createPoint(event.x, event.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            boundCamera?.cameraControl?.startFocusAndMetering(action)
                            v.performClick()
                        }
                        true
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Retícula Táctica y Líneas de Guía (Regla de Tercios)
        if (showReticleGrid) {
            TacticalCameraReticle()
        }

        // 3. Indicador animado de Foco al pulsar en la pantalla
        focusTapCoords?.let { coords ->
            FocusRingIndicator(
                position = coords,
                onAnimationEnd = { focusTapCoords = null }
            )
        }

        // 4. Efecto de Flash de Obturación
        if (shutterFlashActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.7f))
            )
            LaunchedEffect(Unit) {
                delay(80)
                shutterFlashActive = false
            }
        }

        // 5. HUD Superior Táctico con Controles Rápidos y Estado de Firebase Storage
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 42.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón Regresar
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .testTag("camera_back_btn")
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = TacticalCyanPrimary
                    )
                }

                // Píldora de Auto-Subida a Firebase Storage
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isAutoUploadEnabled) Color(0xFF064E3B).copy(alpha = 0.85f) else Color(0xFF1E293B).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, if (isAutoUploadEnabled) TacticalEmeraldSecondary else Color.Gray),
                    modifier = Modifier
                        .clickable { viewModel.toggleAutoUpload() }
                        .testTag("toggle_auto_upload_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isAutoUploadEnabled) Icons.Default.CloudUpload else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (isAutoUploadEnabled) TacticalEmeraldSecondary else Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isAutoUploadEnabled) "STORAGE AUTO-SYNC" else "SOLO LOCAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (isAutoUploadEnabled) Color.White else Color.LightGray
                        )
                    }
                }

                // Acciones Superiores de Cámara (Flash, Antorcha, Lente)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Flash
                    IconButton(
                        onClick = { viewModel.cycleFlashMode() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        val (icon, color) = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn to TacticalAmberTertiary
                            ImageCapture.FLASH_MODE_OFF -> Icons.Default.FlashOff to Color.White
                            else -> Icons.Default.FlashAuto to TacticalCyanPrimary
                        }
                        Icon(icon, contentDescription = "Flash", tint = color, modifier = Modifier.size(20.dp))
                    }

                    // Antorcha / Linterna
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            if (isTorchEnabled) Icons.Default.Highlight else Icons.Default.FlashlightOff,
                            contentDescription = "Torch",
                            tint = if (isTorchEnabled) TacticalAmberTertiary else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Alternar Cámara Frontal / Posterior
                    IconButton(
                        onClick = { viewModel.toggleLensFacing() },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.FlipCameraAndroid,
                            contentDescription = "Cambiar Lente",
                            tint = TacticalCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-HUD con Tag Táctico y Metadata de Sensor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de Tag Táctico
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f),
                    border = BorderStroke(0.8.dp, TacticalCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable { showTagDialog = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Label, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = tacticalTag,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(14.dp))
                    }
                }

                // Telemetría de Lente y Retícula
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.65f)
                ) {
                    val nowStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                    Text(
                        text = "1080p • ${if (lensFacing == CameraSelector.LENS_FACING_BACK) "BACK" else "FRONT"} • $nowStr",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 6. Barra de Progreso de Subida a Firebase Storage en Vivo
        AnimatedVisibility(
            visible = uploadStatus is UploadStatus.InProgress,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 130.dp, start = 20.dp, end = 20.dp)
        ) {
            if (uploadStatus is UploadStatus.InProgress) {
                val progress = uploadStatus as UploadStatus.InProgress
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A).copy(alpha = 0.95f),
                    border = BorderStroke(1.dp, TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    progress = { progress.progressPercent / 100f },
                                    modifier = Modifier.size(16.dp),
                                    color = TacticalCyanPrimary,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Subiendo a Firebase Storage...",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                "${progress.progressPercent}%",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress.progressPercent / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = TacticalCyanPrimary,
                            trackColor = Color.DarkGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val kbTransferred = progress.bytesTransferred / 1024
                        val kbTotal = progress.totalBytes / 1024
                        Text(
                            "${progress.fileName} • $kbTransferred KB / $kbTotal KB",
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.Gray
                        )
                    }
                }
            }
        }

        // 7. Notificación / Banner de Estado flotante
        if (statusBanner != null) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1E293B).copy(alpha = 0.95f),
                border = BorderStroke(1.dp, TacticalEmeraldSecondary),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 120.dp, start = 24.dp, end = 24.dp)
            ) {
                Text(
                    text = statusBanner!!,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 8. Selector de Nivel de Zoom (1x, 2x, 3x, 5x)
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 125.dp)
                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1.0f, 2.0f, 3.0f, 5.0f).forEach { zoomLevel ->
                val isSelected = (zoomRatio == zoomLevel)
                Surface(
                    shape = CircleShape,
                    color = if (isSelected) TacticalCyanPrimary else Color.Transparent,
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { viewModel.setZoomRatio(zoomLevel) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "${zoomLevel.toInt()}x",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // 9. Barra Inferior de Disparo, Miniatura de Bóveda y Galería Cloud
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 36.dp, start = 28.dp, end = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón / Miniatura para abrir la Galería de Firebase Storage y Bóveda
            val latestPhoto = recentPhotos.firstOrNull()
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                    .border(
                        2.dp,
                        if (latestPhoto?.isUploadedToCloud == true) TacticalEmeraldSecondary else Color.White.copy(alpha = 0.6f),
                        RoundedCornerShape(14.dp)
                    )
                    .clickable { viewModel.toggleGalleryDrawer() }
                    .testTag("open_camera_gallery_button"),
                contentAlignment = Alignment.Center
            ) {
                if (latestPhoto != null) {
                    if (latestPhoto.localPath != null && File(latestPhoto.localPath).exists()) {
                        val bitmap = remember(latestPhoto.localPath) {
                            BitmapFactory.decodeFile(latestPhoto.localPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Última Captura",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = latestPhoto.downloadUrl.ifBlank { latestPhoto.localPath },
                                contentDescription = "Captura",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else if (latestPhoto.downloadUrl.isNotBlank()) {
                        AsyncImage(
                            model = latestPhoto.downloadUrl,
                            contentDescription = "Cloud Captura",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White)
                    }

                    // Badge de Cloud si está en Firebase Storage
                    if (latestPhoto.isUploadedToCloud) {
                        Surface(
                            shape = CircleShape,
                            color = TacticalEmeraldSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                        ) {}
                    }
                } else {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Galería", tint = Color.Gray)
                }
            }

            // BOTÓN DE DISPARO PRINCIPAL (CameraX Shutter)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(84.dp)
            ) {
                // Anillo exterior
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(3.dp, if (isCapturing) Color.Gray else TacticalCyanPrimary),
                    modifier = Modifier.size(80.dp)
                ) {}

                // Botón central
                Surface(
                    shape = CircleShape,
                    color = if (isCapturing) Color.DarkGray else Color.White,
                    modifier = Modifier
                        .size(66.dp)
                        .clickable {
                            if (isCapturing) return@clickable
                            viewModel.setCapturing(true)
                            shutterFlashActive = true

                            val vaultDir = File(context.filesDir, "vault").apply { if (!exists()) mkdirs() }
                            val photoFileName = "tactical_${System.currentTimeMillis()}.jpg"
                            val photoFile = File(vaultDir, photoFileName)
                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            imageCapture.takePicture(
                                outputOptions,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                        viewModel.onPhotoCaptured(
                                            photoFile = photoFile,
                                            lensName = if (lensFacing == CameraSelector.LENS_FACING_BACK) "POSTERIOR" else "FRONTAL",
                                            resolution = "1920x1080"
                                        )
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e("CameraScreen", "Error capturando imagen: ${exception.message}", exception)
                                        viewModel.setCapturing(false)
                                        Toast.makeText(context, "Error en captura de cámara: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                        .testTag("camera_shutter_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isCapturing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TacticalCyanPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                Icons.Default.Camera,
                                contentDescription = "Disparar",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
            }

            // Botón de Lista / Galería Cloud
            IconButton(
                onClick = { viewModel.toggleGalleryDrawer() },
                modifier = Modifier
                    .size(54.dp)
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                    .border(1.dp, TacticalCyanPrimary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                    .testTag("open_cloud_drawer_button")
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Cloud, contentDescription = "Cloud Captures", tint = TacticalCyanPrimary, modifier = Modifier.size(20.dp))
                    Text("CLOUD", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Modal para Cambiar Tag Táctico
    if (showTagDialog) {
        TacticalTagSelectionDialog(
            currentTag = tacticalTag,
            onSelectTag = {
                viewModel.setTacticalTag(it)
                showTagDialog = false
            },
            onDismiss = { showTagDialog = false }
        )
    }

    // Bottom Sheet de Galería de Firebase Storage y Bóveda Local
    if (showGalleryDrawer) {
        TacticalPhotoGalleryBottomSheet(
            photos = recentPhotos,
            onDismiss = { viewModel.closeGalleryDrawer() },
            onSelectPhoto = { viewModel.selectPhotoForInspection(it) },
            onUploadPhoto = { viewModel.uploadExistingPhoto(it) },
            onDeletePhoto = { viewModel.deletePhoto(it) },
            onRefresh = { viewModel.refreshCaptures() }
        )
    }

    // Inspector Modal Detallado de Foto (Full Resolution & Firebase Storage URL Viewer)
    selectedPhoto?.let { photoItem ->
        PhotoDetailInspectionDialog(
            photo = photoItem,
            onDismiss = { viewModel.selectPhotoForInspection(null) },
            onUploadToCloud = { viewModel.uploadExistingPhoto(photoItem) },
            onDelete = { viewModel.deletePhoto(photoItem) }
        )
    }
}

/**
 * Retícula de líneas de guía (regla de los tercios y cruz central)
 */
@Composable
private fun TacticalCameraReticle() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val gridLineColor = Color(0x3300E5FF)
        val strokeWidth = 1.dp.toPx()

        // Líneas verticales (tercios)
        drawLine(
            color = gridLineColor,
            start = Offset(width / 3f, 0f),
            end = Offset(width / 3f, height),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridLineColor,
            start = Offset(width * 2f / 3f, 0f),
            end = Offset(width * 2f / 3f, height),
            strokeWidth = strokeWidth
        )

        // Líneas horizontales (tercios)
        drawLine(
            color = gridLineColor,
            start = Offset(0f, height / 3f),
            end = Offset(width, height / 3f),
            strokeWidth = strokeWidth
        )
        drawLine(
            color = gridLineColor,
            start = Offset(0f, height * 2f / 3f),
            end = Offset(width, height * 2f / 3f),
            strokeWidth = strokeWidth
        )

        // Cruz central táctica
        val centerX = width / 2f
        val centerY = height / 2f
        val crossSize = 16.dp.toPx()
        drawLine(
            color = Color(0x8800E5FF),
            start = Offset(centerX - crossSize, centerY),
            end = Offset(centerX + crossSize, centerY),
            strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = Color(0x8800E5FF),
            start = Offset(centerX, centerY - crossSize),
            end = Offset(centerX, centerY + crossSize),
            strokeWidth = 2.dp.toPx()
        )
    }
}

/**
 * Anillo animado de enfoque al tocar la pantalla
 */
@Composable
private fun FocusRingIndicator(
    position: Offset,
    onAnimationEnd: () -> Unit
) {
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(1.0f) }

    LaunchedEffect(position) {
        launch {
            scale.animateTo(
                targetValue = 0.9f,
                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
            )
        }
        launch {
            alpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 600, easing = LinearEasing)
            )
            onAnimationEnd()
        }
    }

    Box(
        modifier = Modifier
            .offset(x = (position.x - 30).dp, y = (position.y - 30).dp)
            .size(60.dp)
            .border(2.dp, TacticalCyanPrimary.copy(alpha = alpha.value), CircleShape)
    )
}

/**
 * Diálogo para seleccionar Tag de clasificación táctica
 */
@Composable
private fun TacticalTagSelectionDialog(
    currentTag: String,
    onSelectTag: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val availableTags = listOf(
        "EVIDENCIA_TACTICA" to "Evidencia general de misión",
        "RECONOCIMIENTO_AEREO" to "Fotografía de terreno o ruta",
        "INTEL_DOCUMENTO" to "Documentación o planos",
        "TOPOLOGIA_NODO" to "Inspección de equipo RF",
        "SENSOR_OPTICO_RAW" to "Captura directa sin procesar"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Label, contentDescription = null, tint = TacticalCyanPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clasificación de Evidencia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Selecciona el metadato con el cual se indexará la foto en Firebase Storage y Firestore:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                availableTags.forEach { (tag, desc) ->
                    val isSelected = (currentTag == tag)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color.Transparent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectTag(tag) }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(tag, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = if (isSelected) TacticalCyanPrimary else MaterialTheme.colorScheme.onSurface)
                            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )
}

/**
 * Bottom Sheet para ver capturas locales y de Firebase Storage
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TacticalPhotoGalleryBottomSheet(
    photos: List<CloudPhotoItem>,
    onDismiss: () -> Unit,
    onSelectPhoto: (CloudPhotoItem) -> Unit,
    onUploadPhoto: (CloudPhotoItem) -> Unit,
    onDeletePhoto: (CloudPhotoItem) -> Unit,
    onRefresh: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.testTag("tactical_gallery_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(20.dp))
                        Text("Bóveda & Firebase Storage", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Text(
                        "${photos.count { it.isUploadedToCloud }} en Nube • ${photos.count { !it.isUploadedToCloud }} solo local",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = TacticalCyanPrimary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Aún no se han capturado fotos", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(photos, key = { it.id }) { photo ->
                        GalleryPhotoThumbnailCard(
                            photo = photo,
                            onClick = { onSelectPhoto(photo) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryPhotoThumbnailCard(
    photo: CloudPhotoItem,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(
            1.dp,
            if (photo.isUploadedToCloud) TacticalEmeraldSecondary.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.4f)
        ),
        modifier = Modifier
            .aspectRatio(1f)
            .clickable { onClick() }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Imagen
            if (photo.localPath != null && File(photo.localPath).exists()) {
                val bitmap = remember(photo.localPath) {
                    BitmapFactory.decodeFile(photo.localPath)
                }
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = photo.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = photo.downloadUrl.ifBlank { photo.localPath },
                        contentDescription = photo.fileName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (photo.downloadUrl.isNotBlank()) {
                AsyncImage(
                    model = photo.downloadUrl,
                    contentDescription = photo.fileName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color.Gray)
                }
            }

            // Gradiente inferior con fecha y badge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(photo.capturedAt))
                    Text(dateStr, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color.White)
                    Icon(
                        if (photo.isUploadedToCloud) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (photo.isUploadedToCloud) TacticalEmeraldSecondary else Color.LightGray,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Inspector detallado de la foto capturada con datos de Firebase Storage y copia de enlace
 */
@Composable
private fun PhotoDetailInspectionDialog(
    photo: CloudPhotoItem,
    onDismiss: () -> Unit,
    onUploadToCloud: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Inspección de Evidencia", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        if (photo.isUploadedToCloud) "Sincronizado en Firebase Storage" else "Solo guardado en Bóveda Local",
                        fontSize = 11.sp,
                        color = if (photo.isUploadedToCloud) TacticalEmeraldSecondary else TacticalAmberTertiary
                    )
                }
                IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagen grande
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    if (photo.localPath != null && File(photo.localPath).exists()) {
                        val bitmap = remember(photo.localPath) {
                            BitmapFactory.decodeFile(photo.localPath)
                        }
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = photo.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AsyncImage(
                                model = photo.downloadUrl.ifBlank { photo.localPath },
                                contentDescription = photo.fileName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        AsyncImage(
                            model = photo.downloadUrl,
                            contentDescription = photo.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabla de Metadatos Tácticos & Firebase Storage
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MetadataRow("Archivo:", photo.fileName)
                        val sizeKb = photo.sizeBytes / 1024f
                        MetadataRow("Tamaño:", String.format(Locale.getDefault(), "%.1f KB", sizeKb))
                        MetadataRow("Tag Táctico:", photo.tacticalTag)
                        MetadataRow("Lente:", photo.lens)
                        val dateFormatted = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(photo.capturedAt))
                        MetadataRow("Captura:", dateFormatted)
                        if (photo.storagePath.isNotBlank()) {
                            MetadataRow("Storage Path:", photo.storagePath)
                        }
                    }
                }

                // Sección de Análisis Óptico AI (OCR y Clasificación Táctica)
                val aiAnalyzer = remember { TacticalEvidenceAnalyzer.getInstance() }
                val isAiAnalyzing by aiAnalyzer.isAnalyzing.collectAsStateWithLifecycle()
                val aiResult by aiAnalyzer.lastResult.collectAsStateWithLifecycle()
                val coroutineScope = rememberCoroutineScope()

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF161B22),
                    border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(16.dp))
                                Text("Análisis Óptico Gemini AI (OCR)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = TacticalCyanPrimary)
                            }
                            if (isAiAnalyzing) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = TacticalCyanPrimary)
                            }
                        }

                        if (aiResult != null) {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.Black, modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("AMENAZA: ${aiResult?.threatLevel}", color = TacticalAmberTertiary, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    Text("COORDENADAS: ${aiResult?.coordinatesFound ?: "N/A"}", color = TacticalEmeraldSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    Text(aiResult?.fullSummary ?: "", color = Color.White, fontSize = 10.sp)
                                }
                            }
                        } else {
                            Text(
                                "Extrae matrículas, números de serie, coordenadas y evaluación de amenazas con Gemini Vision.",
                                color = Color.LightGray,
                                fontSize = 10.sp
                            )
                            Button(
                                onClick = {
                                    if (photo.localPath != null) {
                                        coroutineScope.launch {
                                            aiAnalyzer.analyzeImageFile(File(photo.localPath))
                                        }
                                    }
                                },
                                enabled = !isAiAnalyzing && photo.localPath != null,
                                colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Ejecutar OCR & Análisis de Evidencia", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Sección de Enlace de Descarga de Firebase Storage
                if (photo.isUploadedToCloud && photo.downloadUrl.isNotBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF064E3B).copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "URL PÚBLICA DE DESCARGA (STORAGE)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TacticalEmeraldSecondary
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Firebase Storage Download URL", photo.downloadUrl)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "URL de Firebase Storage copiada al portapapeles", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copiar URL",
                                        tint = TacticalEmeraldSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = photo.downloadUrl,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (!photo.isUploadedToCloud) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = {
                            onUploadToCloud()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Subir a Firebase Storage Ahora", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Cerrar")
            }
        }
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("¿Eliminar Evidencia?") },
            text = { Text("Se eliminará esta foto permanentemente de Firebase Storage y de la bóveda local.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}
