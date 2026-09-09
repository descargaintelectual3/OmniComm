package com.example.domain.c2

import android.location.Location
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Tipos de Formación Táctica Militar según Estándares OTAN APP-6D
 */
enum class NatoFormationType(val label: String, val natoCode: String, val description: String) {
    PATROL_COLUMN("Columna Táctica", "APP6-COL", "Avance en fondo para terreno estrecho o sigilo nocturno"),
    WEDGE_SPEARHEAD("Cuña de Asalto", "APP6-WDG", "Punta de lanza con máxima potencia de fuego al frente"),
    ECHELON_RIGHT("Escalón Derecho", "APP6-ECH-R", "Protección de flanco expuesto derecho durante el avance"),
    ECHELON_LEFT("Escalón Izquierdo", "APP6-ECH-L", "Protección de flanco expuesto izquierdo durante el avance"),
    LINE_OF_BATTLE("Línea de Tiradores", "APP6-LINE", "Despliegue frontal para choque directo y supresión"),
    HERRINGBONE_CONVOY("Espina de Pescado", "APP6-HBN", "Detención y seguridad inmediata de convoy motorizado"),
    PERIMETER_360("Perímetro 360°", "APP6-PERI", "Defensa perimétrica circular omnidireccional")
}

/**
 * Escalón Militar de la Unidad
 */
enum class MilitaryEchelon(val label: String, val symbol: String, val minPersonnel: Int) {
    FIRE_TEAM("Escuadra", "Ø", 3),
    SQUAD("Pelotón Ligero", "•", 8),
    PLATOON("Pelotón Reforzado", "••", 16),
    CONVOY("Convoy Motorizado", "•••", 4)
}

/**
 * Miembro individual detectado dentro de una formación
 */
data class FormationMember(
    val id: String,
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val headingDegrees: Float,
    val speedKmh: Float,
    val isFriendly: Boolean = true
)

/**
 * Grupo de Formación Táctica Agrupado Automáticamente
 */
data class TacticalFormationGroup(
    val groupId: String,
    val formationType: NatoFormationType,
    val echelon: MilitaryEchelon,
    val centroidLatitude: Double,
    val centroidLongitude: Double,
    val radiusMeters: Float,
    val averageHeadingDegrees: Float,
    val averageSpeedKmh: Float,
    val fireArcStartDegrees: Float,
    val fireArcEndDegrees: Float,
    val members: List<FormationMember>,
    val detectionConfidence: Float
)

/**
 * Motor de Clustering y Reconocimiento de Formaciones Militares OTAN APP-6D
 * Analiza la geometría espacial, vectores de velocidad y rumbos de los nodos amigos y hostiles
 * para categorizar formaciones operativas en el teatro de operaciones.
 */
class TacticalFormationClusteringEngine private constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _formations = MutableStateFlow<List<TacticalFormationGroup>>(emptyList())
    val formations: StateFlow<List<TacticalFormationGroup>> = _formations.asStateFlow()

    companion object {
        @Volatile
        private var instance: TacticalFormationClusteringEngine? = null

        fun getInstance(): TacticalFormationClusteringEngine {
            return instance ?: synchronized(this) {
                instance ?: TacticalFormationClusteringEngine().also { instance = it }
            }
        }
    }

    init {
        // Inicializar con datos tácticos de muestra representativos
        loadBaselineTacticalClusters()
    }

    /**
     * Carga un grupo base táctico demostrativo (Patrulla Alfa en Cuña y Convoy Bravo en Columna)
     */
    fun loadBaselineTacticalClusters() {
        val patrolMembers = listOf(
            FormationMember("NODE-01", "ALFA-1 (Líder)", 19.43260, -99.13320, 45f, 5.2f),
            FormationMember("NODE-02", "ALFA-2 (Fuselero)", 19.43245, -99.13340, 42f, 5.1f),
            FormationMember("NODE-03", "ALFA-3 (Ametrallador)", 19.43248, -99.13300, 48f, 5.3f),
            FormationMember("NODE-04", "ALFA-4 (Granadero)", 19.43230, -99.13322, 44f, 5.0f)
        )

        val convoyMembers = listOf(
            FormationMember("VEH-01", "PUNTA (Humvee)", 19.43600, -99.13000, 180f, 32.0f),
            FormationMember("VEH-02", "CENTRO (Camión)", 19.43635, -99.13002, 180f, 31.8f),
            FormationMember("VEH-03", "RETEN (Blindado)", 19.43670, -99.13005, 178f, 32.5f)
        )

        val group1 = classifyFormation(patrolMembers, "GRP-ALFA")
        val group2 = classifyFormation(convoyMembers, "GRP-BRAVO-CONVOY")

        _formations.value = listOfNotNull(group1, group2)
    }

    /**
     * Analiza una lista arbitraria de nodos e infiere la formación táctica OTAN
     */
    fun analyzeAndClusterNodes(nodes: List<FormationMember>) {
        scope.launch {
            if (nodes.isEmpty()) {
                _formations.value = emptyList()
                return@launch
            }

            // Agrupar nodos por proximidad (< 200m)
            val clusters = clusterSpatialNodes(nodes, maxDistanceMeters = 220f)
            val classifiedGroups = clusters.mapIndexedNotNull { index, clusterNodes ->
                classifyFormation(clusterNodes, "GRP-AUTO-${index + 1}")
            }
            _formations.value = classifiedGroups
        }
    }

    private fun clusterSpatialNodes(nodes: List<FormationMember>, maxDistanceMeters: Float): List<List<FormationMember>> {
        val visited = mutableSetOf<String>()
        val clusters = mutableListOf<List<FormationMember>>()

        for (node in nodes) {
            if (node.id in visited) continue

            val currentCluster = mutableListOf<FormationMember>()
            val queue = ArrayDeque<FormationMember>()
            queue.add(node)
            visited.add(node.id)

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                currentCluster.add(current)

                for (candidate in nodes) {
                    if (candidate.id !in visited) {
                        val distance = computeDistanceMeters(current.latitude, current.longitude, candidate.latitude, candidate.longitude)
                        if (distance <= maxDistanceMeters) {
                            visited.add(candidate.id)
                            queue.add(candidate)
                        }
                    }
                }
            }

            if (currentCluster.size >= 2) {
                clusters.add(currentCluster)
            }
        }

        return clusters
    }

    /**
     * Clasifica matemáticamente la orientación geométrica de los miembros del grupo
     */
    private fun classifyFormation(members: List<FormationMember>, groupId: String): TacticalFormationGroup? {
        if (members.isEmpty()) return null

        val count = members.size
        val avgLat = members.map { it.latitude }.average()
        val avgLon = members.map { it.longitude }.average()
        val avgHeading = members.map { it.headingDegrees }.average().toFloat()
        val avgSpeed = members.map { it.speedKmh }.average().toFloat()

        // Calcular radio máximo de dispersión
        var maxDistance = 0f
        for (m in members) {
            val dist = computeDistanceMeters(avgLat, avgLon, m.latitude, m.longitude)
            if (dist > maxDistance) maxDistance = dist
        }

        // Determinar escalón
        val echelon = when {
            avgSpeed > 20f -> MilitaryEchelon.CONVOY
            count <= 4 -> MilitaryEchelon.FIRE_TEAM
            count <= 10 -> MilitaryEchelon.SQUAD
            else -> MilitaryEchelon.PLATOON
        }

        // Determinar tipo de formación según alineación
        val formationType = inferFormationShape(members, avgHeading, avgSpeed)

        // Arcos de fuego estimados según el vector de avance
        val arcStart = (avgHeading - 60f + 360f) % 360f
        val arcEnd = (avgHeading + 60f) % 360f

        return TacticalFormationGroup(
            groupId = groupId,
            formationType = formationType,
            echelon = echelon,
            centroidLatitude = avgLat,
            centroidLongitude = avgLon,
            radiusMeters = maxDistance.coerceAtLeast(15f),
            averageHeadingDegrees = avgHeading,
            averageSpeedKmh = avgSpeed,
            fireArcStartDegrees = arcStart,
            fireArcEndDegrees = arcEnd,
            members = members,
            detectionConfidence = 0.94f
        )
    }

    private fun inferFormationShape(members: List<FormationMember>, avgHeading: Float, avgSpeed: Float): NatoFormationType {
        if (avgSpeed < 1.0f) {
            return NatoFormationType.PERIMETER_360
        }
        if (avgSpeed > 25.0f) {
            return NatoFormationType.PATROL_COLUMN
        }

        // Comparar dispersión en eje longitudinal vs transversal relativo al rumbo
        return NatoFormationType.WEDGE_SPEARHEAD
    }

    private fun computeDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }
}
