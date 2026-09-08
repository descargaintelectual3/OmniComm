package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad de Base de Datos Room para almacenamiento y gestión local de
 * pares de claves criptográficas y claves públicas para mensajería cifrada E2EE.
 */
@Entity(tableName = "cryptographic_keys")
data class CryptographicKeyEntity(
    @PrimaryKey 
    val keyId: String,                    // Identificador único (ej: "KEY-RSA-7A9B", "KEY-PQC-3F12")
    val alias: String,                    // Nombre táctico (ej: "Master E2EE Identity", "Terminal Recon-1")
    val algorithm: String,                // "RSA-2048", "RSA-4096", "EC-P256", "KYBER-768-PQC", "ED25519"
    val purpose: String,                  // "E2EE_MESSAGING", "DIGITAL_SIGNATURE", "POST_QUANTUM", "KEY_EXCHANGE"
    val publicKeyBase64: String,          // Clave pública codificada en Base64 / formato X.509
    val fingerprint: String,              // Huella SHA-256 formateada (ej: "E3:4B:91:A0:...")
    val keySizeBits: Int = 2048,          // Tamaño de clave en bits (2048, 4096, 256, 768)
    val isHardwareBacked: Boolean = true, // Si reside en AndroidKeyStore / TEE / StrongBox
    val isPrimary: Boolean = false,       // Si es la clave actualmente activa para cifrado saliente
    val isRevoked: Boolean = false,       // Si ha sido revocada por rotación de seguridad
    val createdAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = 0L,             // 0L = sin expiración configurada
    val usageCount: Int = 0,              // Conteo de mensajes cifrados / descifrados con esta clave
    val ownerNodeId: String = "LOCAL_NODE", // "LOCAL_NODE" para claves locales, o deviceId para pares
    val ownerDisplayName: String = "Terminal Local",
    val notes: String = ""                // Metadatos u observaciones tácticas
)
