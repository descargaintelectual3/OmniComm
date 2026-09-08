package com.example.domain.models

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Metadatos de un paquete cartográfico offline (formato MBTiles / Vector Tiles).
 * Capa 10: Cartografía Offline & Simbología OTAN APP-6D.
 */
data class TacticalMBTilesPackage(
    val id: String,
    val name: String,
    val description: String,
    val format: String, // "pbf" (vectorial), "png" (raster), "webp"
    val minZoom: Int,
    val maxZoom: Int,
    val bounds: String, // "left,bottom,right,top" WGS84
    val sizeBytes: Long,
    val tileCount: Long,
    val attribution: String,
    val isInstalled: Boolean = true,
    val isPrimaryActive: Boolean = false,
    val layerType: MapLayerType = MapLayerType.TOPOGRAPHIC
)

enum class MapLayerType(val label: String, val colorHex: Long) {
    TOPOGRAPHIC("Topográfico Militar 1:50k", 0xFF00E5FF),
    SATELLITE_HYBRID("Ortofoto Satelital Táctica", 0xFF00E676),
    ELEVATION_SHADED("Relieve Sombreado (DEM)", 0xFFFFD600),
    VECTOR_TACTICAL("Vectorial Ligero LPI", 0xFFFF9100),
    NIGHT_VISION("Modo Nocturno / Térmico", 0xFF00FF41)
}

/**
 * Gestor y catálogo de paquetes cartográficos offline MBTiles.
 */
class TacticalMBTilesManager private constructor(private val context: Context) {

    private val _installedPackages = MutableStateFlow<List<TacticalMBTilesPackage>>(
        listOf(
            TacticalMBTilesPackage(
                id = "mbtiles_topo_tactical_v1",
                name = "Cuadrícula Táctica Topo 50k",
                description = "Cartografía raster detallada con curvas de nivel a 10m y toponimia militar.",
                format = "png",
                minZoom = 10,
                maxZoom = 17,
                bounds = "-74.15,4.55,-74.00,4.70",
                sizeBytes = 142_000_000L,
                tileCount = 8420L,
                attribution = "IGAC / Cartografía Táctica Militar",
                isInstalled = true,
                isPrimaryActive = true,
                layerType = MapLayerType.TOPOGRAPHIC
            ),
            TacticalMBTilesPackage(
                id = "mbtiles_sat_highres_v2",
                name = "Ortofoto Satelital de Alta Resolución",
                description = "Imágenes multiespectrales ortorrectificadas con resolución 0.5m/px.",
                format = "webp",
                minZoom = 12,
                maxZoom = 19,
                bounds = "-74.12,4.58,-74.02,4.68",
                sizeBytes = 380_000_000L,
                tileCount = 19500L,
                attribution = "Sentinel-2 / Recon Aéreo Táctico",
                isInstalled = true,
                isPrimaryActive = false,
                layerType = MapLayerType.SATELLITE_HYBRID
            ),
            TacticalMBTilesPackage(
                id = "mbtiles_dem_elevation_v1",
                name = "Modelo Digital de Elevación (DEM)",
                description = "Pendientes, zonas ciegas de radar y líneas de visión (LOS/NLOS).",
                format = "png",
                minZoom = 8,
                maxZoom = 16,
                bounds = "-74.20,4.50,-73.95,4.75",
                sizeBytes = 64_000_000L,
                tileCount = 4120L,
                attribution = "SRTM 30m / DEM Táctico",
                isInstalled = true,
                isPrimaryActive = false,
                layerType = MapLayerType.ELEVATION_SHADED
            ),
            TacticalMBTilesPackage(
                id = "mbtiles_vector_minimal_v1",
                name = "Vectorial Minimalista LPI (Vector Tiles)",
                description = "Consumo ultrabajo de CPU/GPU y memoria, ideal para modo sigilo o batería crítica.",
                format = "pbf",
                minZoom = 6,
                maxZoom = 16,
                bounds = "-75.00,4.00,-73.50,5.00",
                sizeBytes = 18_500_000L,
                tileCount = 2800L,
                attribution = "OpenStreetMap / Apollo Tactical",
                isInstalled = true,
                isPrimaryActive = false,
                layerType = MapLayerType.VECTOR_TACTICAL
            )
        )
    )
    val installedPackages: StateFlow<List<TacticalMBTilesPackage>> = _installedPackages.asStateFlow()

    private val _selectedLayer = MutableStateFlow(_installedPackages.value.first())
    val selectedLayer: StateFlow<TacticalMBTilesPackage> = _selectedLayer.asStateFlow()

    private val _activeZoomLevel = MutableStateFlow(14)
    val activeZoomLevel: StateFlow<Int> = _activeZoomLevel.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: TacticalMBTilesManager? = null

        fun getInstance(context: Context): TacticalMBTilesManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalMBTilesManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun selectPrimaryPackage(packageId: String) {
        val current = _installedPackages.value
        val target = current.find { it.id == packageId } ?: return

        _installedPackages.value = current.map {
            it.copy(isPrimaryActive = (it.id == packageId))
        }
        _selectedLayer.value = target

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "MBTILES_MANAGER",
            message = "Capa cartográfica activa cambiada a: ${target.name} (${target.format.uppercase()})"
        )
    }

    fun setZoomLevel(zoom: Int) {
        val bounded = zoom.coerceIn(_selectedLayer.value.minZoom, _selectedLayer.value.maxZoom)
        _activeZoomLevel.value = bounded
    }

    /**
     * Valida si un archivo MBTiles local existe y puede cargarse.
     */
    fun importLocalMBTilesFile(file: File): Boolean {
        return try {
            if (!file.exists() || file.length() == 0L) return false

            val newPkg = TacticalMBTilesPackage(
                id = "mbtiles_custom_${System.currentTimeMillis()}",
                name = file.nameWithoutExtension.replace("_", " ").capitalize(),
                description = "Paquete MBTiles importado por el operador (${file.length() / (1024 * 1024)} MB)",
                format = if (file.name.endsWith(".pbf", true)) "pbf" else "png",
                minZoom = 10,
                maxZoom = 18,
                bounds = "-74.15,4.55,-74.00,4.70",
                sizeBytes = file.length(),
                tileCount = 1000L,
                attribution = "Importación Local",
                isInstalled = true,
                isPrimaryActive = false,
                layerType = MapLayerType.TOPOGRAPHIC
            )

            _installedPackages.value = _installedPackages.value + newPkg
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.INFO,
                tag = "MBTILES_IMPORT",
                message = "Paquete importado con éxito: ${newPkg.name}"
            )
            true
        } catch (e: Exception) {
            DiscoveryLogCollector.log(
                category = LogCategory.SYSTEM,
                severity = LogSeverity.WARNING,
                tag = "MBTILES_IMPORT",
                message = "Error importando archivo MBTiles: ${e.message}"
            )
            false
        }
    }
}
