package com.example.ui.screens

import android.app.Activity
import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.media.TacticalScreenshotCaptureService
import com.example.domain.media.TacticalScreenshotRecord
import com.example.ui.theme.StealthBackground
import com.example.ui.theme.StealthSurfaceContainer
import com.example.ui.theme.StealthSurfaceContainerHigh
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualTelemetryScreen(
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val coroutineScope = rememberCoroutineScope()
    val screenshotService = remember { TacticalScreenshotCaptureService.getInstance(context) }

    val screenshotsList by screenshotService.screenshotsList.collectAsStateWithLifecycle()
    val isCapturing by screenshotService.isCapturing.collectAsStateWithLifecycle()
    var selectedScreenshot by remember { mutableStateOf<TacticalScreenshotRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "TELEMETRÍA VISUAL Y DIAGNÓSTICO",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = TacticalCyanPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Capturas Internas & Evidencia de Renderizado",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_visual_telemetry")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar al Hub",
                            tint = TacticalCyanPrimary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            screenshotService.clearScreenshots()
                            selectedScreenshot = null
                            Toast.makeText(context, "Historial de capturas purgado", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("btn_clear_screenshots")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Limpiar capturas",
                            tint = Color.Red.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = StealthBackground)
            )
        },
        containerColor = StealthBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Panel de Control de Captura Instantánea
            Surface(
                color = StealthSurfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, TacticalCyanPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(TacticalCyanPrimary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Motor de Captura Interna",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    "PixelCopy Hardware API + Canvas Fallback",
                                    fontSize = 11.sp,
                                    color = TacticalEmeraldSecondary
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (activity != null) {
                                    coroutineScope.launch {
                                        val record = screenshotService.captureActivityScreen(
                                            activity = activity,
                                            label = "Captura Manual en Pantalla de Telemetría"
                                        )
                                        if (record != null) {
                                            selectedScreenshot = record
                                            Toast.makeText(context, "📸 Captura generada con éxito", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(context, "No se detectó actividad activa para captura", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = !isCapturing,
                            modifier = Modifier.testTag("btn_trigger_screenshot"),
                            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Capturar",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Visor de Imagen Seleccionada / Previa
            if (selectedScreenshot != null) {
                Surface(
                    color = StealthSurfaceContainer,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .testTag("preview_screenshot_card")
                ) {
                    val file = File(selectedScreenshot!!.filePath)
                    val bitmap = remember(selectedScreenshot) {
                        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "Captura de pantalla",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No se pudo cargar el bitmap del archivo", color = Color.Gray, fontSize = 12.sp)
                            }
                        }

                        // Overlay con datos de telemetría
                        Surface(
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        selectedScreenshot!!.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "${selectedScreenshot!!.resolutionLabel} • ${selectedScreenshot!!.fileSizeBytes / 1024} KB • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(selectedScreenshot!!.timestamp))}",
                                        fontSize = 10.sp,
                                        color = TacticalCyanPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                IconButton(
                                    onClick = { selectedScreenshot = null },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cerrar visor",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Lista de capturas registradas
            Text(
                "HISTORIAL DE CAPTURAS INTERNAS (${screenshotsList.size})",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.7f),
                letterSpacing = 1.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (screenshotsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Aún no se han generado capturas de pantalla.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                        Text(
                            "Presiona 'Capturar' o invoca /api/v1/screenshots desde la API externa.",
                            color = Color.DarkGray,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(screenshotsList, key = { it.id }) { shot ->
                        Surface(
                            color = StealthSurfaceContainer,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (selectedScreenshot?.id == shot.id) TacticalEmeraldSecondary else Color.White.copy(alpha = 0.1f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedScreenshot = shot }
                                .testTag("item_screenshot_${shot.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(TacticalEmeraldSecondary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = TacticalEmeraldSecondary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        shot.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${shot.resolutionLabel} | ${shot.fileSizeBytes / 1024} KB | ${SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(shot.timestamp))}",
                                        fontSize = 11.sp,
                                        color = TacticalCyanPrimary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Icon(
                                    imageVector = Icons.Default.Visibility,
                                    contentDescription = "Visualizar",
                                    tint = TacticalCyanPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
