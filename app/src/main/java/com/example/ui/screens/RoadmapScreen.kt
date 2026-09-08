package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.*
import com.example.domain.remote.TacticalExternalControlServer
import com.example.ui.components.ExternalApiServerDialog
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

enum class RoadmapTab(val title: String, val icon: ImageVector) {
    FORENSIC_TABLE("Plano Maestro", Icons.Default.TableView),
    LAYERS("Capas Arquitectura", Icons.Default.Layers),
    DATA_FLOWS("Flujos & Cableado", Icons.Default.AltRoute),
    CODE_INDEX("Ingeniería Inversa", Icons.Default.Code),
    CAPABILITIES("Matriz Capacidades", Icons.Default.Security),
    EXTERNAL_API("Control Externo API", Icons.Default.Sensors)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(RoadmapTab.FORENSIC_TABLE) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("TODAS") }
    var showApiDialog by remember { mutableStateOf(false) }

    val masterRecords = SystemArchitectureRegistry.masterRecords
    val categories = remember(masterRecords) {
        listOf("TODAS") + masterRecords.map { it.category }.distinct().sorted()
    }

    val filteredRecords = remember(masterRecords, searchQuery, selectedCategoryFilter) {
        masterRecords.filter { record ->
            val matchesCategory = (selectedCategoryFilter == "TODAS" || record.category.equals(selectedCategoryFilter, ignoreCase = true))
            val matchesSearch = searchQuery.isBlank() ||
                    record.title.contains(searchQuery, ignoreCase = true) ||
                    record.operationalScope.contains(searchQuery, ignoreCase = true) ||
                    record.domainModule.contains(searchQuery, ignoreCase = true) ||
                    record.implementationModes.contains(searchQuery, ignoreCase = true) ||
                    record.researchAndStandards.contains(searchQuery, ignoreCase = true) ||
                    record.sourceFiles.any { it.contains(searchQuery, ignoreCase = true) }
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                                text = "PLANO MAESTRO & INGENIERÍA INVERSA",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = TacticalEmeraldSecondary.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                            ) {
                                Text(
                                    text = "${masterRecords.size}/${masterRecords.size} FASES 100%",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TacticalEmeraldSecondary,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Registro Forense Integral, Cableado de Módulos y APIs de Control Remoto",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showApiDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = "Servidor API Externo",
                            tint = TacticalCyanPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B0F17)
                )
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Métricas Rápidas en Cabecera
            SystemExecutiveHeader(
                totalPhases = masterRecords.size,
                totalLayers = SystemArchitectureRegistry.layers.size,
                totalFlows = SystemArchitectureRegistry.dataFlows.size,
                totalModules = SystemArchitectureRegistry.codeIndex.size
            )

            // Selector de Pestañas
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color(0xFF161B22),
                contentColor = TacticalCyanPrimary,
                edgePadding = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                RoadmapTab.values().forEach { tab ->
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
                                    modifier = Modifier.size(14.dp),
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

            // Contenido de la pestaña activa
            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    RoadmapTab.FORENSIC_TABLE -> {
                        ForensicTableTabContent(
                            records = filteredRecords,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            categories = categories,
                            selectedCategory = selectedCategoryFilter,
                            onCategorySelect = { selectedCategoryFilter = it }
                        )
                    }
                    RoadmapTab.LAYERS -> {
                        LayersBlueprintTabContent()
                    }
                    RoadmapTab.DATA_FLOWS -> {
                        DataFlowsWiringTabContent()
                    }
                    RoadmapTab.CODE_INDEX -> {
                        CodeReverseEngineeringTabContent()
                    }
                    RoadmapTab.CAPABILITIES -> {
                        CapabilitiesMatrixTabContent()
                    }
                    RoadmapTab.EXTERNAL_API -> {
                        ExternalApiOverviewTabContent(onOpenDialog = { showApiDialog = true })
                    }
                }
            }
        }
    }

    if (showApiDialog) {
        ExternalApiServerDialog(onDismiss = { showApiDialog = false })
    }
}

@Composable
fun SystemExecutiveHeader(
    totalPhases: Int,
    totalLayers: Int,
    totalFlows: Int,
    totalModules: Int
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
            ExecutiveMetricBadge("FASES TÁCTICAS", "$totalPhases / $totalPhases", TacticalEmeraldSecondary)
            ExecutiveMetricBadge("CAPAS SISTEMA", "$totalLayers Capas", TacticalCyanPrimary)
            ExecutiveMetricBadge("FLUJOS CABLEADOS", "$totalFlows Pipelines", TacticalAmberTertiary)
            ExecutiveMetricBadge("MÓDULOS CÓDIGO", "$totalModules Clases", Color(0xFFB388FF))
        }
    }
}

@Composable
fun ExecutiveMetricBadge(label: String, value: String, accentColor: Color) {
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
fun ForensicTableTabContent(
    records: List<SystemMasterArchitectureRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    categories: List<String>,
    selectedCategory: String,
    onCategorySelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Barra de Búsqueda y Filtros
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            placeholder = { Text("Buscar fase, módulo, archivo, estándar o hardware...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(16.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalCyanPrimary,
                unfocusedBorderColor = Color(0xFF30363D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        // Chips de Categorías
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            categories.forEach { cat ->
                val isSelected = selectedCategory == cat
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.2f) else Color(0xFF161B22),
                    border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                    modifier = Modifier.clickable { onCategorySelect(cat) }
                ) {
                    Text(
                        text = cat,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TacticalCyanPrimary else Color.LightGray,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Text(
            text = "Mostrando ${records.size} registros forenses detallados:",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // Lista de Filas Forenses Exhaustivas
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(records) { record ->
                ForensicRecordCard(record = record)
            }
        }
    }
}

@Composable
fun ForensicRecordCard(record: SystemMasterArchitectureRecord) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
        border = BorderStroke(1.dp, if (isExpanded) TacticalCyanPrimary else Color(0xFF30363D)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(10.dp)
        ) {
            // Fila Principal: Fase, Bloque, Título y Estado
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
                        shape = RoundedCornerShape(4.dp),
                        color = TacticalCyanPrimary.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, TacticalCyanPrimary)
                    ) {
                        Text(
                            text = "FASE ${record.phaseNumber}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFF21262D)
                    ) {
                        Text(
                            text = "BLOQUE ${record.blockNumber}",
                            fontSize = 9.sp,
                            color = TacticalAmberTertiary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = record.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = if (isExpanded) 3 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TacticalEmeraldSecondary.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                ) {
                    Text(
                        text = record.status,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalEmeraldSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Resumen de Alcance
            Text(
                text = record.operationalScope,
                fontSize = 10.sp,
                color = Color.LightGray,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis
            )

            // Indicadores de etiquetas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF0D1117)) {
                        Text(
                            text = "Módulo: ${record.domainModule}",
                            fontSize = 9.sp,
                            color = TacticalCyanPrimary,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(3.dp), color = Color(0xFF0D1117)) {
                        Text(
                            text = record.category,
                            fontSize = 9.sp,
                            color = TacticalAmberTertiary,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Desglose Exhaustivo de Todas las Columnas
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Divider(color = Color(0xFF30363D))

                    // 1. Capas Arquitectónicas
                    ForensicDataSection("CAPAS ARQUITECTÓNICAS ASIGNADAS:", record.architectureLayers.joinToString(" • "), TacticalCyanPrimary)

                    // 2. Archivos Fuente y Módulos
                    ForensicDataListSection("ARCHIVOS FUENTE (CÓDIGO FUENTE REAL):", record.sourceFiles, Color(0xFFB388FF))

                    // 3. APIs Expuestas y Control Externo
                    ForensicDataListSection("APIs EXPUESTAS & ACCESO REMOTO:", record.exposedApis, TacticalEmeraldSecondary)

                    // 4. Modos de Implementación
                    ForensicDataSection("MODOS DE IMPLEMENTACIÓN & ALGORITMOS:", record.implementationModes, Color.White)

                    // 5. Estándares Militares & Normas
                    ForensicDataSection("ESTÁNDARES & REFERENCIAS DE INVESTIGACIÓN:", record.researchAndStandards, TacticalAmberTertiary)

                    // 6. Hardware, HAL y Sensores
                    ForensicDataListSection("HARDWARE, HAL & SENSORES REQUERIDOS:", record.hardwareAndSensors, Color(0xFFFF80AB))

                    // 7. Zero-Trust & Tolerancia a Fallos
                    ForensicDataSection("ZERO-TRUST, SIGILO & TOLERANCIA A FALLOS:", record.zeroTrustAndFaultTolerance, Color(0xFFFF5252))

                    // 8. Proyecciones Futuras & Roadmap
                    ForensicDataSection("PROYECCIONES FUTURAS & ROADMAP:", record.futureProjectionsAndRoadmap, Color(0xFF80D8FF))

                    // 9. Métodos de Verificación Forense
                    ForensicDataSection("MÉTODOS DE VERIFICACIÓN & TESTING:", record.verificationMethods, TacticalEmeraldSecondary)
                }
            }
        }
    }
}

@Composable
fun ForensicDataSection(title: String, content: String, titleColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            fontFamily = FontFamily.Monospace
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0D1117),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            Text(
                text = content,
                fontSize = 10.sp,
                color = Color(0xFFE6EDF3),
                lineHeight = 14.sp,
                modifier = Modifier.padding(6.dp)
            )
        }
    }
}

@Composable
fun ForensicDataListSection(title: String, items: List<String>, titleColor: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            fontFamily = FontFamily.Monospace
        )
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0D1117),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items.forEach { item ->
                    Text(
                        text = "• $item",
                        fontSize = 9.sp,
                        color = Color(0xFFE6EDF3),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun LayersBlueprintTabContent() {
    val allLayers = remember { SystemArchitectureRegistry.layers }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableIntStateOf(0) } // 0: Todas, 1: Capas 1-8, 2: Capas 9-16, 3: Capas 17-24

    val filteredLayers = remember(searchQuery, selectedCategoryFilter, allLayers) {
        allLayers.filter { layer ->
            val matchesCategory = when (selectedCategoryFilter) {
                1 -> layer.layerNumber in 1..8
                2 -> layer.layerNumber in 9..16
                3 -> layer.layerNumber in 17..24
                else -> true
            }
            val matchesSearch = if (searchQuery.isBlank()) true else {
                layer.name.contains(searchQuery, ignoreCase = true) ||
                    layer.tag.contains(searchQuery, ignoreCase = true) ||
                    layer.description.contains(searchQuery, ignoreCase = true) ||
                    layer.primaryResponsibility.contains(searchQuery, ignoreCase = true) ||
                    layer.components.any { it.name.contains(searchQuery, ignoreCase = true) || it.protocolsOrTech.contains(searchQuery, ignoreCase = true) }
            }
            matchesCategory && matchesSearch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Panel superior de métricas y buscador
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATÁLOGO DE CAPAS DEL SISTEMA (${allLayers.size} CAPAS)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyanPrimary,
                        letterSpacing = 1.sp
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = TacticalEmeraldSecondary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${filteredLayers.size} VISIBLES",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalEmeraldSecondary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                // Campo de búsqueda
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar capa, protocolo, sensor o componente...", fontSize = 11.sp, color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TacticalCyanPrimary,
                        unfocusedBorderColor = Color(0xFF30363D),
                        focusedContainerColor = Color(0xFF0D1117),
                        unfocusedContainerColor = Color(0xFF0D1117),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // Chips de filtro por bloque
                val filterOptions = listOf(
                    "Todas (${allLayers.size})",
                    "Fundación (Capas 1-8)",
                    "Tácticas & RF (Capas 9-16)",
                    "Autónomas & BLOS (Capas 17-24)"
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    filterOptions.forEachIndexed { index, optionTitle ->
                        val isSelected = selectedCategoryFilter == index
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) TacticalCyanPrimary.copy(alpha = 0.25f) else Color(0xFF161B22),
                            border = BorderStroke(1.dp, if (isSelected) TacticalCyanPrimary else Color(0xFF30363D)),
                            modifier = Modifier.clickable { selectedCategoryFilter = index }
                        ) {
                            Text(
                                text = optionTitle,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TacticalCyanPrimary else Color.LightGray,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        items(filteredLayers, key = { it.layerNumber }) { layer ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = layer.name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF21262D)
                        ) {
                            Text(
                                text = layer.tag,
                                fontSize = 9.sp,
                                color = TacticalAmberTertiary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(text = layer.description, fontSize = 10.sp, color = Color.LightGray)
                    Text(
                        text = "Responsabilidad Clave: ${layer.primaryResponsibility}",
                        fontSize = 10.sp,
                        color = TacticalEmeraldSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Divider(color = Color(0xFF30363D))

                    // Flujos de entrada y salida
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0D1117),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("ENTRADAS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                                layer.inboundFlows.forEach { inFlow ->
                                    Text("• $inFlow", fontSize = 8.sp, color = Color.LightGray)
                                }
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0D1117),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("SALIDAS:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)
                                layer.outboundFlows.forEach { outFlow ->
                                    Text("• $outFlow", fontSize = 8.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }

                    Text("Componentes Registrados (${layer.components.size}):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    layer.components.forEach { comp ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF0D1117),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(6.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("${comp.name} (${comp.role})", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalCyanPrimary)
                                Text("Ruta: ${comp.filePath}", fontSize = 9.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Text("Funciones: ${comp.keyFunctions.joinToString(", ")}", fontSize = 9.sp, color = Color.LightGray)
                                Text("Protocolos: ${comp.protocolsOrTech}", fontSize = 9.sp, color = TacticalAmberTertiary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataFlowsWiringTabContent() {
    val flows = SystemArchitectureRegistry.dataFlows
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(flows) { flow ->
            Card(
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = flow.title,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = TacticalEmeraldSecondary.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, TacticalEmeraldSecondary)
                        ) {
                            Text(
                                text = flow.securityLevel,
                                fontSize = 9.sp,
                                color = TacticalEmeraldSecondary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text("Disparador: ${flow.trigger}", fontSize = 10.sp, color = TacticalAmberTertiary)
                    Text(flow.description, fontSize = 10.sp, color = Color.LightGray)

                    Divider(color = Color(0xFF30363D))

                    Text("Secuencia de Pasos y Cableado:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    flow.stepSequence.forEach { step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TacticalCyanPrimary,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${step.stepNumber}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${step.moduleName} -> ${step.action}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                                Text(
                                    text = step.filePath,
                                    fontSize = 8.sp,
                                    color = Color.Gray,
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

@Composable
fun CodeReverseEngineeringTabContent() {
    val modules = SystemArchitectureRegistry.codeIndex
    var filterText by remember { mutableStateOf("") }

    val filtered = remember(modules, filterText) {
        if (filterText.isBlank()) modules
        else modules.filter {
            it.fileName.contains(filterText, ignoreCase = true) ||
                    it.primaryClass.contains(filterText, ignoreCase = true) ||
                    it.description.contains(filterText, ignoreCase = true) ||
                    it.packageGroup.contains(filterText, ignoreCase = true)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        OutlinedTextField(
            value = filterText,
            onValueChange = { filterText = it },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            placeholder = { Text("Filtrar clases, archivos, paquetes o consumidores...", fontSize = 11.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TacticalCyanPrimary, modifier = Modifier.size(16.dp)) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TacticalCyanPrimary,
                unfocusedBorderColor = Color(0xFF30363D),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text("Catálogo de ${filtered.size} módulos de código fuente:", fontSize = 10.sp, color = Color.Gray)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered) { mod ->
                Card(
                    shape = RoundedCornerShape(6.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                    border = BorderStroke(1.dp, Color(0xFF30363D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = mod.primaryClass,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalCyanPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = mod.tacticalDomain,
                                fontSize = 9.sp,
                                color = TacticalAmberTertiary
                            )
                        }
                        Text("Archivo: ${mod.relativePath}", fontSize = 8.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                        Text(mod.description, fontSize = 9.sp, color = Color.LightGray)
                        Text("Métodos Clave: ${mod.keyMethods.joinToString(", ")}", fontSize = 8.sp, color = TacticalEmeraldSecondary)
                        Text("Consumidores: ${mod.consumers.joinToString(", ")}", fontSize = 8.sp, color = Color(0xFF80D8FF))
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilitiesMatrixTabContent() {
    val caps = SystemArchitectureRegistry.tacticalCapabilities
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(caps) { cap ->
            Card(
                shape = RoundedCornerShape(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
                border = BorderStroke(1.dp, Color(0xFF30363D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = cap.title,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF21262D)
                        ) {
                            Text(
                                text = "FASE ${cap.activePhase}",
                                fontSize = 9.sp,
                                color = TacticalAmberTertiary,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(cap.operationalScope, fontSize = 9.sp, color = Color.LightGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (cap.isRealTime) CapabilityTag("TIEMPO REAL", TacticalEmeraldSecondary)
                        if (cap.isZeroTrust) CapabilityTag("ZERO-TRUST", Color(0xFFFF5252))
                        if (cap.isHardwareAccelerated) CapabilityTag("HARDWARE HAL", Color(0xFFB388FF))
                    }
                }
            }
        }
    }
}

@Composable
fun CapabilityTag(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(3.dp),
        color = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color)
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

@Composable
fun ExternalApiOverviewTabContent(onOpenDialog: () -> Unit) {
    val context = LocalContext.current
    val server = remember { TacticalExternalControlServer.getInstance(context) }
    val state by server.serverState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161B22)),
            border = BorderStroke(1.dp, TacticalCyanPrimary),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "SERVIDOR DE CONTROL EXTERNO REST & WEB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyanPrimary
                )
                Text(
                    text = "Permite a computadoras externas (vía Wi-Fi LAN, Hotspot o cable USB ADB) controlar todos los módulos de OmniComm Hub y su hardware.",
                    fontSize = 10.sp,
                    color = Color.LightGray
                )

                Divider(color = Color(0xFF30363D))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("ESTADO DEL SERVIDOR:", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = if (state.isRunning) "EN LÍNEA (ACTIVO)" else "DETENIDO",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.isRunning) TacticalEmeraldSecondary else Color.Gray
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("DIRECCIÓN IP & PUERTO:", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${state.localIp}:${state.port}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalAmberTertiary,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("PETICIONES ATENDIDAS:", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${state.totalRequests} requests",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Button(
                    onClick = onOpenDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Abrir Consola y Panel de Control API", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(6.dp),
            color = Color(0xFF0D1117),
            border = BorderStroke(1.dp, Color(0xFF30363D)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Comando de Redirección USB ADB:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TacticalAmberTertiary)
                Text(
                    text = "adb forward tcp:9090 tcp:9090\ncurl http://localhost:9090/api/v1/status\nhttp://localhost:9090/console",
                    fontSize = 9.sp,
                    color = TacticalCyanPrimary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
