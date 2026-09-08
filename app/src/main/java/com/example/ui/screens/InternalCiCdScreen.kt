package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.cicd.*
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CiCdTab(val title: String, val icon: ImageVector) {
    PIPELINES_LIVE("Pipelines & Ejecución", Icons.Default.PlayArrow),
    EXECUTION_LOGS("Logs & Consola", Icons.Default.List),
    ARTIFACTS_VAULT("Bóveda Artefactos & SBOM", Icons.Default.Info),
    DEPLOYMENTS_ROLLBACK("Despliegues & Rollback", Icons.Default.Share),
    YAML_SPEC("Pipeline YAML (.ci.yml)", Icons.Default.Code)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalCiCdScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val engine = remember { InternalCiCdEngine.getInstance(context) }

    val executionsHistory by engine.executionsHistory.collectAsState()
    val runningExecution by engine.currentRunningExecution.collectAsState()
    val artifacts by engine.artifactsRegistry.collectAsState()
    val deployments by engine.deploymentRecords.collectAsState()

    var selectedTab by remember { mutableStateOf(CiCdTab.PIPELINES_LIVE) }
    var selectedExecutionId by remember { mutableStateOf<String?>(null) }
    var showTriggerDialog by remember { mutableStateOf(false) }

    val activeOrSelectedExecution = runningExecution
        ?: executionsHistory.firstOrNull { it.executionId == selectedExecutionId }
        ?: executionsHistory.lastOrNull()

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_cicd")) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar",
                            tint = TacticalCyanPrimary
                        )
                    }
                },
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "CI/CD & DEVSECOPS INTERNO",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (runningExecution != null) TacticalAmberTertiary.copy(alpha = 0.2f) else TacticalEmeraldSecondary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, if (runningExecution != null) TacticalAmberTertiary else TacticalEmeraldSecondary)
                            ) {
                                Text(
                                    text = if (runningExecution != null) "PIPELINE RUNNING" else "MOTOR LISTO",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (runningExecution != null) TacticalAmberTertiary else TacticalEmeraldSecondary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Compilación Táctica • SAST • PQC Test Matrix • SBOM • OTA Mesh Deploy",
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showTriggerDialog = true },
                        modifier = Modifier.testTag("btn_trigger_pipeline")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Lanzar Pipeline",
                            tint = TacticalEmeraldSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0B0F17))
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Ejecutivo de Métricas de CI/CD
            CiCdExecutiveMetricsHeader(
                totalPipelines = executionsHistory.size,
                successRate = if (executionsHistory.isNotEmpty()) {
                    ((executionsHistory.count { it.status == PipelineStatus.SUCCESS }.toFloat() / executionsHistory.size) * 100).toInt()
                } else 100,
                artifactsCount = artifacts.size,
                deploymentsCount = deployments.size
            )

            // Selector de Pestañas
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF161B22),
                contentColor = TacticalCyanPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                CiCdTab.values().forEach { tab ->
                    Tab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.title,
                                    modifier = Modifier.size(13.dp),
                                    tint = if (selectedTab == tab) TacticalCyanPrimary else Color.Gray
                                )
                                Text(
                                    text = tab.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == tab) TacticalCyanPrimary else Color.LightGray
                                )
                            }
                        }
                    )
                }
            }

            // Contenido dinámico según la pestaña seleccionada
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    CiCdTab.PIPELINES_LIVE -> {
                        PipelinesLiveTabContent(
                            runningExecution = runningExecution,
                            history = executionsHistory,
                            selectedExecution = activeOrSelectedExecution,
                            onSelectExecution = {
                                selectedExecutionId = it.executionId
                            },
                            onTriggerFastPipeline = {
                                engine.triggerPipeline(
                                    pipelineName = "OmniComm Tactical Fast CI Run",
                                    source = PipelineTriggerSource.MANUAL_OPERATOR,
                                    branchOrTag = "main",
                                    targetEnv = DeploymentEnvironment.P2P_MESH_FLEET
                                )
                                Toast.makeText(context, "Pipeline lanzada en segundo plano", Toast.LENGTH_SHORT).show()
                            },
                            onCancelRunning = {
                                engine.cancelActivePipeline()
                                Toast.makeText(context, "Pipeline cancelada por el operador", Toast.LENGTH_SHORT).show()
                            },
                            onViewLogs = {
                                selectedExecutionId = it.executionId
                                selectedTab = CiCdTab.EXECUTION_LOGS
                            }
                        )
                    }

                    CiCdTab.EXECUTION_LOGS -> {
                        ExecutionLogsTabContent(
                            execution = activeOrSelectedExecution,
                            executions = executionsHistory,
                            onSelectExecution = { selectedExecutionId = it.executionId }
                        )
                    }

                    CiCdTab.ARTIFACTS_VAULT -> {
                        ArtifactsVaultTabContent(
                            artifacts = artifacts,
                            onInspectArtifact = { art ->
                                Toast.makeText(context, "Artefacto ${art.name} verificado (SHA-256 OK)", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    CiCdTab.DEPLOYMENTS_ROLLBACK -> {
                        DeploymentsRollbackTabContent(
                            deployments = deployments,
                            onRollback = { dep ->
                                val ok = engine.performRollback(dep.id, "Intervención desde panel UI")
                                Toast.makeText(context, if (ok) "Rollback de ${dep.version} ejecutado" else "Error al realizar rollback", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    CiCdTab.YAML_SPEC -> {
                        YamlSpecTabContent(
                            engine = engine
                        )
                    }
                }
            }
        }
    }

    if (showTriggerDialog) {
        TriggerPipelineDialog(
            onDismiss = { showTriggerDialog = false },
            onLaunch = { name, env, branch ->
                engine.triggerPipeline(
                    pipelineName = name,
                    source = PipelineTriggerSource.MANUAL_OPERATOR,
                    branchOrTag = branch,
                    targetEnv = env
                )
                showTriggerDialog = false
                selectedTab = CiCdTab.PIPELINES_LIVE
                Toast.makeText(context, "Pipeline '$name' despachada", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun CiCdExecutiveMetricsHeader(
    totalPipelines: Int,
    successRate: Int,
    artifactsCount: Int,
    deploymentsCount: Int
) {
    Surface(
        color = Color(0xFF0F141C),
        border = BorderStroke(1.dp, Color(0xFF21262D)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CiCdMetricBadge("PIPELINES", "$totalPipelines Ejecutadas", TacticalCyanPrimary)
            CiCdMetricBadge("SUCCESSRATE", "$successRate%", if (successRate >= 90) TacticalEmeraldSecondary else TacticalAmberTertiary)
            CiCdMetricBadge("ARTEFACTOS", "$artifactsCount Paquetes", TacticalAmberTertiary)
            CiCdMetricBadge("DESPLIEGUES", "$deploymentsCount Releases", Color(0xFF80D8FF))
        }
    }
}

@Composable
fun CiCdMetricBadge(label: String, value: String, accentColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            fontSize = 8.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun PipelinesLiveTabContent(
    runningExecution: PipelineExecution?,
    history: List<PipelineExecution>,
    selectedExecution: PipelineExecution?,
    onSelectExecution: (PipelineExecution) -> Unit,
    onTriggerFastPipeline: () -> Unit,
    onCancelRunning: () -> Unit,
    onViewLogs: (PipelineExecution) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Banner de Pipeline en Ejecución o Botón de Disparo
        if (runningExecution != null) {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, TacticalAmberTertiary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = TacticalAmberTertiary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = "PIPELINE EN EJECUCIÓN: ${runningExecution.executionId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalAmberTertiary,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Button(
                            onClick = onCancelRunning,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Cancelar", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    Text(
                        text = "${runningExecution.pipelineName} • Branch: ${runningExecution.branchOrTag} • Commit: ${runningExecution.commitHash}",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )

                    // Pipeline Stage Stepper Horizontal
                    PipelineStagesProgressRow(
                        stages = runningExecution.stages,
                        currentStageIndex = runningExecution.currentStageIndex
                    )
                }
            }
        } else {
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, TacticalCyanPrimary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("DevSecOps Pipeline Engine", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                        Text("Ejecuta análisis de vulnerabilidades, pruebas criptográficas PQC y compilación de release.", fontSize = 9.sp, color = Color.LightGray)
                    }

                    Button(
                        onClick = onTriggerFastPipeline,
                        colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lanzar Run", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Inspección de Etapas de la Pipeline Seleccionada
        selectedExecution?.let { exec ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF11161F)),
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "DETALLE DE EJECUCIÓN: ${exec.executionId}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${exec.pipelineName} • Disparado por: ${exec.initiatedBy}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = when (exec.status) {
                                PipelineStatus.SUCCESS -> TacticalEmeraldSecondary.copy(alpha = 0.2f)
                                PipelineStatus.RUNNING -> TacticalAmberTertiary.copy(alpha = 0.2f)
                                PipelineStatus.FAILED -> Color(0xFFFF5252).copy(alpha = 0.2f)
                                PipelineStatus.CANCELLED -> Color.Gray.copy(alpha = 0.2f)
                                else -> Color(0xFF21262D)
                            }
                        ) {
                            Text(
                                text = exec.status.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (exec.status) {
                                    PipelineStatus.SUCCESS -> TacticalEmeraldSecondary
                                    PipelineStatus.RUNNING -> TacticalAmberTertiary
                                    PipelineStatus.FAILED -> Color(0xFFFF5252)
                                    else -> Color.LightGray
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFF21262D))

                    // Lista de 6 Etapas con estados
                    exec.stages.forEachIndexed { idx, stage ->
                        StageRowItem(idx + 1, stage)
                    }

                    Button(
                        onClick = { onViewLogs(exec) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                        modifier = Modifier.fillMaxWidth().height(32.dp)
                    ) {
                        Icon(Icons.Default.List, contentDescription = null, modifier = Modifier.size(12.dp), tint = TacticalCyanPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ver Registro Completo de Salida / Logs", fontSize = 10.sp, color = TacticalCyanPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Historial Reciente de Ejecuciones
        Text(
            text = "Historial de Ejecuciones Recientes (${history.size}):",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        history.reversed().forEach { execItem ->
            val isSelected = execItem.executionId == selectedExecution?.executionId
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFF161B22) else Color(0xFF0F141C)),
                border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF21262D)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectExecution(execItem) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = when (execItem.status) {
                                    PipelineStatus.SUCCESS -> TacticalEmeraldSecondary
                                    PipelineStatus.RUNNING -> TacticalAmberTertiary
                                    PipelineStatus.FAILED -> Color(0xFFFF5252)
                                    else -> Color.Gray
                                },
                                modifier = Modifier.size(8.dp)
                            ) {}
                            Text(
                                text = execItem.executionId,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• ${execItem.branchOrTag}",
                                fontSize = 9.sp,
                                color = Color.Gray
                            )
                        }
                        Text(
                            text = "${execItem.pipelineName} • Duración: ${execItem.totalDurationMs}ms",
                            fontSize = 8.sp,
                            color = Color.LightGray
                        )
                    }

                    Text(
                        text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(execItem.startedAt)),
                        fontSize = 9.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PipelineStagesProgressRow(
    stages: List<StageExecutionRecord>,
    currentStageIndex: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        stages.forEachIndexed { index, stage ->
            val isCurrent = index == currentStageIndex
            val isCompleted = stage.status == PipelineStatus.SUCCESS

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when {
                    isCompleted -> TacticalEmeraldSecondary.copy(alpha = 0.2f)
                    isCurrent -> TacticalAmberTertiary.copy(alpha = 0.2f)
                    else -> Color(0xFF21262D)
                },
                border = BorderStroke(
                    1.dp,
                    when {
                        isCompleted -> TacticalEmeraldSecondary
                        isCurrent -> TacticalAmberTertiary
                        else -> Color(0xFF30363D)
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isCompleted) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = TacticalEmeraldSecondary, modifier = Modifier.size(10.dp))
                    } else if (isCurrent) {
                        CircularProgressIndicator(modifier = Modifier.size(8.dp), color = TacticalAmberTertiary, strokeWidth = 1.5.dp)
                    }
                    Text(
                        text = stage.stageType.displayName.substringBefore("."),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isCompleted -> TacticalEmeraldSecondary
                            isCurrent -> TacticalAmberTertiary
                            else -> Color.Gray
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun StageRowItem(number: Int, stage: StageExecutionRecord) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                shape = CircleShape,
                color = when (stage.status) {
                    PipelineStatus.SUCCESS -> TacticalEmeraldSecondary.copy(alpha = 0.2f)
                    PipelineStatus.RUNNING -> TacticalAmberTertiary.copy(alpha = 0.2f)
                    PipelineStatus.FAILED -> Color(0xFFFF5252).copy(alpha = 0.2f)
                    else -> Color(0xFF21262D)
                },
                border = BorderStroke(1.dp, when (stage.status) {
                    PipelineStatus.SUCCESS -> TacticalEmeraldSecondary
                    PipelineStatus.RUNNING -> TacticalAmberTertiary
                    PipelineStatus.FAILED -> Color(0xFFFF5252)
                    else -> Color.Gray
                }),
                modifier = Modifier.size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "$number", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column {
                Text(
                    text = stage.stageType.displayName,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = stage.stageType.description,
                    fontSize = 8.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = if (stage.durationMs > 0) "${stage.durationMs}ms" else if (stage.status == PipelineStatus.RUNNING) "En curso..." else "-",
            fontSize = 9.sp,
            color = if (stage.status == PipelineStatus.SUCCESS) TacticalEmeraldSecondary else Color.Gray,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ExecutionLogsTabContent(
    execution: PipelineExecution?,
    executions: List<PipelineExecution>,
    onSelectExecution: (PipelineExecution) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Selector horizontal de ejecución
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            executions.forEach { execItem ->
                val isSelected = execItem.executionId == execution?.executionId
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.2f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                    modifier = Modifier.clickable { onSelectExecution(execItem) }
                ) {
                    Text(
                        text = execItem.executionId,
                        fontSize = 9.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TacticalCyanPrimary else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Consola de Salida de Logs
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (execution == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("Selecciona una ejecución para ver sus logs.", color = Color.Gray, fontSize = 10.sp)
                }
            } else {
                val allLogs = execution.stages.flatMap { it.logLines }
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(allLogs) { logLine ->
                        Text(
                            text = logLine,
                            fontSize = 8.5.sp,
                            color = when {
                                logLine.contains("PASSED") || logLine.contains("completada") || logLine.contains("exitosamente") -> TacticalEmeraldSecondary
                                logLine.contains("INICIANDO") -> TacticalCyanPrimary
                                logLine.contains("advertencia") || logLine.contains("Scanning") -> TacticalAmberTertiary
                                logLine.contains("ERROR") || logLine.contains("FAILED") -> Color(0xFFFF5252)
                                else -> Color(0xFFE6EDF3)
                            },
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Botón para Copiar Logs
        Button(
            onClick = {
                execution?.let {
                    val fullText = it.stages.flatMap { s -> s.logLines }.joinToString("\n")
                    clipboardManager.setText(AnnotatedString(fullText))
                    Toast.makeText(context, "Logs copiados al portapapeles.", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copiar Logs de Ejecución", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun ArtifactsVaultTabContent(
    artifacts: List<CiCdArtifact>,
    onInspectArtifact: (CiCdArtifact) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalAmberTertiary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("BÓVEDA DE ARTEFACTOS & SBOM CRIPTOGRÁFICO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)
                Text(
                    text = "Binarios compilados firmados con esquema APK Signature v2/v3/v4 y Manifiestos de Componentes de Software (CycloneDX / SPDF) para Zero-Trust compliance.",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(artifacts) { art ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = when (art.type) {
                                        ArtifactType.RELEASE_APK -> TacticalEmeraldSecondary.copy(alpha = 0.2f)
                                        ArtifactType.SBOM_CYCLONEDX -> TacticalCyanPrimary.copy(alpha = 0.2f)
                                        ArtifactType.PQC_SIGNATURE_MANIFEST -> TacticalAmberTertiary.copy(alpha = 0.2f)
                                        else -> Color(0xFF21262D)
                                    }
                                ) {
                                    Text(
                                        text = art.type.name,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (art.type) {
                                            ArtifactType.RELEASE_APK -> TacticalEmeraldSecondary
                                            ArtifactType.SBOM_CYCLONEDX -> TacticalCyanPrimary
                                            ArtifactType.PQC_SIGNATURE_MANIFEST -> TacticalAmberTertiary
                                            else -> Color.LightGray
                                        },
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Text(
                                    text = art.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = art.formattedSize,
                                fontSize = 9.sp,
                                color = Color.Gray,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Text(
                            text = "SHA-256: ${art.sha256Checksum}",
                            fontSize = 7.5.sp,
                            color = TacticalCyanPrimary,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Versión: ${art.versionTag} • Generado: ${SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(art.createdAt))}",
                                fontSize = 8.sp,
                                color = Color.Gray
                            )

                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(art.sha256Checksum))
                                    Toast.makeText(context, "Checksum copiado", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF21262D)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Copiar Hash", fontSize = 8.sp, color = TacticalCyanPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeploymentsRollbackTabContent(
    deployments: List<DeploymentRecord>,
    onRollback: (DeploymentRecord) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalEmeraldSecondary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ENTORNOS DE DESPLIEGUE & MOTOR DE ROLLBACK", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalEmeraldSecondary)
                Text(
                    text = "Supervisión de versiones desplegadas en la malla P2P y servidores. Permite revertir versiones a estados herméticos anteriores ante cualquier degradación de telemetría.",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(deployments.reversed()) { dep ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, if (dep.status == PipelineStatus.ROLLED_BACK) TacticalAmberTertiary else Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = dep.environment.title,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (dep.status == PipelineStatus.ROLLED_BACK) TacticalAmberTertiary.copy(alpha = 0.2f) else TacticalEmeraldSecondary.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = dep.version,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (dep.status == PipelineStatus.ROLLED_BACK) TacticalAmberTertiary else TacticalEmeraldSecondary,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(text = "Artefacto: ${dep.artifactName}", fontSize = 9.sp, color = Color.LightGray)
                            Text(text = "Desplegado por: ${dep.deployedBy} • ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(dep.deployedAt))}", fontSize = 8.sp, color = Color.Gray)
                        }

                        if (dep.rollbackAvailable && dep.status != PipelineStatus.ROLLED_BACK) {
                            Button(
                                onClick = { onRollback(dep) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Rollback", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun YamlSpecTabContent(
    engine: InternalCiCdEngine
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val yamlText = remember { engine.generateYamlPipelineSpec() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("DEFINICIÓN DE PIPELINE .omnicomm-ci.yml", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                Text(
                    text = "Especificación declarativa estándar de las etapas de automatización, disparadores de red mesh y matrices de pruebas.",
                    fontSize = 9.sp,
                    color = Color.LightGray
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = yamlText,
                fontSize = 8.5.sp,
                color = Color(0xFFE6EDF3),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(8.dp)
            )
        }

        Button(
            onClick = {
                clipboardManager.setText(AnnotatedString(yamlText))
                Toast.makeText(context, "Archivo .omnicomm-ci.yml copiado al portapapeles.", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth().height(36.dp)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Copiar .omnicomm-ci.yml", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
        }
    }
}

@Composable
fun TriggerPipelineDialog(
    onDismiss: () -> Unit,
    onLaunch: (String, DeploymentEnvironment, String) -> Unit
) {
    var pipelineName by remember { mutableStateOf("OmniComm Tactical DevSecOps Pipeline") }
    var branch by remember { mutableStateOf("main") }
    var selectedEnv by remember { mutableStateOf(DeploymentEnvironment.P2P_MESH_FLEET) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Lanzar Pipeline de CI/CD", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pipelineName,
                    onValueChange = { pipelineName = it },
                    label = { Text("Nombre de Pipeline", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch / Tag Git", fontSize = 10.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Entorno de Despliegue Destino:", fontSize = 10.sp, color = Color.LightGray)
                DeploymentEnvironment.values().forEach { env ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedEnv = env }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = selectedEnv == env, onClick = { selectedEnv = env })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(env.title, fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onLaunch(pipelineName, selectedEnv, branch) },
                colors = ButtonDefaults.buttonColors(containerColor = TacticalEmeraldSecondary)
            ) {
                Text("Ejecutar Pipeline", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 10.sp)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar", fontSize = 10.sp)
            }
        },
        containerColor = Color(0xFF161B22)
    )
}
