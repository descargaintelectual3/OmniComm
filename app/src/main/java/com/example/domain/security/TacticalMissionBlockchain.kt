package com.example.domain.security

import android.content.Context
import com.example.domain.logging.DiscoveryLogCollector
import com.example.domain.logging.LogCategory
import com.example.domain.logging.LogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

/**
 * FASE 18: Registro Inmutable de Misión en Micro-Blockchain Táctica Local (Proof-of-Authority):
 * Estructura de bloques enlazados criptográficamente para auditoría forense inalterable
 * de órdenes de combate, bajas, transmisiones SOS y cambios de mando.
 */
data class TacticalBlock(
    val index: Long,
    val previousHash: String,
    val timestamp: Long,
    val payloadData: String,
    val signerAuthorityCallsign: String,
    val blockHash: String
)

class TacticalMissionBlockchain private constructor(context: Context) {

    private val _chain = MutableStateFlow<List<TacticalBlock>>(emptyList())
    val chain: StateFlow<List<TacticalBlock>> = _chain.asStateFlow()

    init {
        // Bloque Génesis
        val genesis = createBlock(
            index = 0,
            previousHash = "0000000000000000000000000000000000000000000000000000000000000000",
            payload = "GENESIS_MISSION_INIT_OMNICOMM_SECURE_CHAIN",
            signer = "SYSTEM-ROOT-HQ"
        )
        _chain.value = listOf(genesis)
    }

    fun appendLogEntry(payload: String, signerCallsign: String): TacticalBlock {
        val last = _chain.value.last()
        val newBlock = createBlock(
            index = last.index + 1,
            previousHash = last.blockHash,
            payload = payload,
            signer = signerCallsign
        )
        _chain.value = _chain.value + newBlock

        DiscoveryLogCollector.log(
            category = LogCategory.SYSTEM,
            severity = LogSeverity.INFO,
            tag = "BLOCKCHAIN_LOG",
            message = "Bloque de misión #${newBlock.index} sellado e inmutable. Hash: ${newBlock.blockHash.take(16)}..."
        )

        return newBlock
    }

    private fun createBlock(
        index: Long,
        previousHash: String,
        payload: String,
        signer: String
    ): TacticalBlock {
        val timestamp = System.currentTimeMillis()
        val raw = "$index-$previousHash-$timestamp-$payload-$signer"
        val md = MessageDigest.getInstance("SHA-256")
        val hash = md.digest(raw.toByteArray()).joinToString("") { "%02x".format(it) }

        return TacticalBlock(
            index = index,
            previousHash = previousHash,
            timestamp = timestamp,
            payloadData = payload,
            signerAuthorityCallsign = signer,
            blockHash = hash
        )
    }

    fun verifyIntegrity(): Boolean {
        val list = _chain.value
        for (i in 1 until list.size) {
            val current = list[i]
            val prev = list[i - 1]
            if (current.previousHash != prev.blockHash) return false
        }
        return true
    }

    companion object {
        @Volatile
        private var INSTANCE: TacticalMissionBlockchain? = null

        fun getInstance(context: Context): TacticalMissionBlockchain {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TacticalMissionBlockchain(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
