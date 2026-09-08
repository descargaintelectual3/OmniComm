package com.example.domain.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val deviceId: String,
    val alias: String,
    val email: String = "",
    val avatarIcon: String = "🛡️",
    val photoUrl: String = "",
    val lastKnownLatitude: Double = 0.0,
    val lastKnownLongitude: Double = 0.0,
    val lastSeenTimestamp: Long = 0L,
    val isOnline: Boolean = false,
    val isTrustedNode: Boolean = false,
    val isPriority: Boolean = false,
    val accessCount: Int = 0,
    val batteryPercent: Int = 85,
    val publicKeyFingerprint: String = "",
    val rawPublicKeyBase64: String = "",
    val preferredMedium: String = "DUAL_RADIO",
    val encryptedPublicKey: String = "",
    val keyAlgorithm: String = "RSA-2048",
    val isKeyVerified: Boolean = true,
    val keyExchangeTimestamp: Long = 0L,
    val e2eeProtocolVersion: String = "v2.4"
)
