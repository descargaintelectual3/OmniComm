package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.p2p.PacketPriority
import java.util.UUID

/**
 * Entidad persistente de Room para Paquetes / Bundles de Retardo y Disrupción Tolerante (DTN)
 * Almacena los mensajes y datos en custodia persistente (Custody Transfer) en disco cifrado con SQLCipher.
 */
@Entity(tableName = "dtn_bundles")
data class DtnBundleEntity(
    @PrimaryKey
    val bundleId: String = UUID.randomUUID().toString(),
    val sourceNodeId: String,
    val destinationNodeId: String, // UID específico o "BROADCAST_ALL"
    val priority: String = PacketPriority.HIGH_PRIORITY_TEXT.name,
    val payloadType: String, // "TEXT", "SOS", "PTT_AUDIO", "GIS_MARKER", "OPTICAL"
    val payloadData: String,
    val ttlHops: Int = 7,
    val currentHops: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + 86400000L, // 24h
    val visitedNodesCsv: String = sourceNodeId,
    val custodyAccepted: Boolean = true,
    val isDelivered: Boolean = false,
    val ackReceived: Boolean = false
) {
    fun getVisitedList(): List<String> = visitedNodesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
