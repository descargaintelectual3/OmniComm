package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.config.FeatureManager
import com.example.domain.config.LiteProfile
import com.example.domain.config.ResourceWeight
import com.example.domain.config.TacticalModuleComponent
import com.example.ui.theme.TacticalAmberTertiary
import com.example.ui.theme.TacticalCyanPrimary
import com.example.ui.theme.TacticalEmeraldSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiteModeScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val featureManager = remember { FeatureManager.getInstance(context) }

    val isLiteActive by featureManager.isLiteModeActive.collectAsStateWithLifecycle()
    val currentProfile by featureManager.currentLiteProfile.collectAsStateWithLifecycle()

    // Estado reactivo de módulos para refrescar la lista al alternar
    var moduleRefreshTrigger by remember { mutableIntStateOf(0) }
    val allModules = remember(isLiteActive, currentProfile, moduleRefreshTrigger) {
        featureManager.getAllModuleComponents()
    }
    val metrics = remember(allModules) {
        featureManager.getEstimatedMetrics()
    }

    var selectedFilterTab by remember { mutableIntStateOf(0) } // 0: Todos, 1: Activos, 2: Alto Impacto
    var searchQuery by remember { mutableStateOf("") }

    val filteredModules = remember(allModules, selectedFilterTab, searchQuery) {
        allModules.filter { module ->
            val matchesTab = when (selectedFilterTab) {
                1 -> module.isEnabled
                2 -> module.resourceWeight == ResourceWeight.HEAVY
                else -> true
            }
            val matchesQuery = if (searchQuery.isBlank()) true else {
                module.name.contains(searchQuery, ignoreCase = true) ||
                module.sourceFilePath.contains(searchQuery, ignoreCase = true) ||
                module.layerCategory.contains(searchQuery, ignoreCase = true)
            }
            matchesTab && matchesQuery
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Versión Lite & Modular",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.2f) else Color.DarkGray
                            ) {
                                Text(
                                    text = if (isLiteActive) "LITE ACTIVO" else "FULL C4ISR",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLiteActive) TacticalEmeraldSecondary else Color.LightGray,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "Control granular de módulos, hilos y optimización de recursos",
                            fontSize = 11.sp,
                            color = TacticalCyanPrimary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_lite_mode")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            featureManager.applyLiteProfile(LiteProfile.FULL_TACTICAL)
                            featureManager.setLiteModeActive(false)
                            moduleRefreshTrigger++
                        },
                        modifier = Modifier.testTag("btn_reset_lite_mode")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = "Restablecer a Full", tint = TacticalAmberTertiary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0D1117))
            )
        },
        containerColor = Color(0xFF0B0F17)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // --- TARJETA MAESTRA: MASTER SWITCH MODO LITE ---
            item {
                MasterLiteSwitchCard(
                    isLiteActive = isLiteActive,
                    onToggle = { active ->
                        featureManager.setLiteModeActive(active)
                        moduleRefreshTrigger++
                    }
                )
            }

            // --- VELOCÍMETRO / TARJETAS DE IMPACTO EN RECURSOS ---
            item {
                PerformanceImpactDashboard(
                    activeModulesCount = metrics.first,
                    totalModulesCount = allModules.size,
                    ramUsageMb = metrics.second,
                    batterySavingPercent = metrics.third
                )
            }

            // --- SELECTOR DE PERFILES RÁPIDOS PREDEFINIDOS ---
            item {
                Text(
                    "PERFILES PREDEFINIDOS DE RENDIMIENTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TacticalCyanPrimary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiteProfile.values().forEach { profile ->
                        val isSelected = (currentProfile == profile && isLiteActive) || 
                                         (!isLiteActive && profile == LiteProfile.FULL_TACTICAL)
                        PresetProfileCard(
                            profile = profile,
                            isSelected = isSelected,
                            onSelect = {
                                if (profile == LiteProfile.FULL_TACTICAL) {
                                    featureManager.setLiteModeActive(false)
                                    featureManager.applyLiteProfile(LiteProfile.FULL_TACTICAL)
                                } else {
                                    featureManager.setLiteProfile(profile)
                                    featureManager.setLiteModeActive(true)
                                }
                                moduleRefreshTrigger++
                            }
                        )
                    }
                }
            }

            // --- FILTRO Y BUSCADOR DE COMPONENTES ---
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "COMPONENTES & CÓDIGO FUENTE (${filteredModules.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TacticalCyanPrimary,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pestañas de filtro rápido
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = (selectedFilterTab == 0),
                            onClick = { selectedFilterTab = 0 },
                            label = { Text("Todos (${allModules.size})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalCyanPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = TacticalCyanPrimary
                            )
                        )
                        FilterChip(
                            selected = (selectedFilterTab == 1),
                            onClick = { selectedFilterTab = 1 },
                            label = { Text("Activos (${metrics.first})", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalEmeraldSecondary.copy(alpha = 0.2f),
                                selectedLabelColor = TacticalEmeraldSecondary
                            )
                        )
                        FilterChip(
                            selected = (selectedFilterTab == 2),
                            onClick = { selectedFilterTab = 2 },
                            label = { Text("Alto Impacto", fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TacticalAmberTertiary.copy(alpha = 0.2f),
                                selectedLabelColor = TacticalAmberTertiary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Barra de búsqueda rápida
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_search_modules"),
                        placeholder = { Text("Buscar componente, archivo o capa...", fontSize = 12.sp, color = Color.Gray) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = TacticalCyanPrimary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = Color.Gray)
                                }
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TacticalCyanPrimary,
                            unfocusedBorderColor = Color(0xFF21262D),
                            focusedContainerColor = Color(0xFF0F141C),
                            unfocusedContainerColor = Color(0xFF0F141C)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // --- LISTA DE MÓDULOS Y ARCHIVOS DE CÓDIGO ---
            items(filteredModules, key = { it.id }) { module ->
                TacticalModuleItemCard(
                    module = module,
                    onToggle = { enabled ->
                        featureManager.toggleModuleById(module.id, enabled)
                        moduleRefreshTrigger++
                    }
                )
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MasterLiteSwitchCard(
    isLiteActive: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (isLiteActive) TacticalEmeraldSecondary.copy(alpha = 0.6f) else Color(0xFF21262D),
                shape = RoundedCornerShape(12.dp)
            ),
        color = if (isLiteActive) Color(0xFF0C1D16) else Color(0xFF161B22)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isLiteActive) Icons.Default.EnergySavingsLeaf else Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (isLiteActive) TacticalEmeraldSecondary else TacticalCyanPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isLiteActive) "MODO LITE ACTIVADO" else "MODO COMPLETO C4ISR",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = if (isLiteActive) TacticalEmeraldSecondary else Color.White
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLiteActive)
                        "La aplicación opera en modo ultra-ligero. Hilos de audio DSP, shaders pesados y sensores continuos están optimizados para máxima fluidez y ahorro de batería."
                    else
                        "Todos los 31 subsistemas tácticos, servidores de control y sensores 100Hz están listos para máxima capacidad operativa.",
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = isLiteActive,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("switch_master_lite_mode"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = TacticalEmeraldSecondary,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFF21262D)
                )
            )
        }
    }
}

@Composable
private fun PerformanceImpactDashboard(
    activeModulesCount: Int,
    totalModulesCount: Int,
    ramUsageMb: Int,
    batterySavingPercent: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MetricTile(
            title = "RAM ESTIMADA",
            value = "$ramUsageMb MB",
            subtitle = if (ramUsageMb < 60) "Ultra Ligero" else "Normal",
            color = if (ramUsageMb < 60) TacticalEmeraldSecondary else TacticalCyanPrimary,
            icon = Icons.Default.Memory,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "AHORRO BATERÍA",
            value = "+$batterySavingPercent%",
            subtitle = "Eficiencia Energética",
            color = if (batterySavingPercent > 40) TacticalEmeraldSecondary else TacticalAmberTertiary,
            icon = Icons.Default.BatteryChargingFull,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "MÓDULOS ACTIVOS",
            value = "$activeModulesCount / $totalModulesCount",
            subtitle = "${totalModulesCount - activeModulesCount} Apagados",
            color = TacticalCyanPrimary,
            icon = Icons.Default.Extension,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
        color = Color(0xFF0F141C)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = color)
            Text(subtitle, fontSize = 10.sp, color = Color(0xFF8B949E))
        }
    }
}

@Composable
private fun PresetProfileCard(
    profile: LiteProfile,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onSelect() }
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) TacticalCyanPrimary else Color(0xFF21262D),
                shape = RoundedCornerShape(8.dp)
            ),
        color = if (isSelected) Color(0xFF101B2B) else Color(0xFF161B22)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onSelect,
                        colors = RadioButtonDefaults.colors(selectedColor = TacticalCyanPrimary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = profile.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else Color.LightGray
                    )
                }
                Text(
                    text = profile.description,
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                    modifier = Modifier.padding(start = 36.dp)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = TacticalCyanPrimary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = profile.ramEstimate,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TacticalCyanPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(profile.cpuReduction, fontSize = 9.sp, color = TacticalEmeraldSecondary)
            }
        }
    }
}

@Composable
private fun TacticalModuleItemCard(
    module: TacticalModuleComponent,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, Color(0xFF21262D), RoundedCornerShape(8.dp)),
        color = Color(0xFF161B22)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = Color(module.resourceWeight.badgeColorHex).copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = module.resourceWeight.label,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(module.resourceWeight.badgeColorHex),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    if (module.affectsBackgroundThreads) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = TacticalAmberTertiary.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "HILOS BG",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = TacticalAmberTertiary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = module.layerCategory,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = module.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (module.isEnabled) Color.White else Color.Gray
                )

                Text(
                    text = module.description,
                    fontSize = 11.sp,
                    color = Color(0xFF8B949E),
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.DarkGray,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = module.sourceFilePath,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF58A6FF)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Switch(
                checked = module.isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.testTag("switch_${module.id}"),
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = TacticalEmeraldSecondary,
                    uncheckedThumbColor = Color.LightGray,
                    uncheckedTrackColor = Color(0xFF21262D)
                )
            )
        }
    }
}
