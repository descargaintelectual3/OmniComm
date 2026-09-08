package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete
import com.example.domain.local.entities.ContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY isPriority DESC, accessCount DESC, lastSeenTimestamp DESC")
    fun getAllContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts ORDER BY isPriority DESC, accessCount DESC, lastSeenTimestamp DESC")
    suspend fun getPriorityScanNodes(): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE deviceId = :id LIMIT 1")
    fun getContactById(id: String): Flow<ContactEntity?>

    @Query("SELECT * FROM contacts WHERE deviceId = :id LIMIT 1")
    suspend fun getContactDirect(id: String): ContactEntity?

    @Query("SELECT * FROM contacts WHERE isOnline = 1 ORDER BY isPriority DESC, alias ASC")
    fun getOnlineContacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE isPriority = 1")
    fun getPriorityContacts(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET isPriority = :isPriority WHERE deviceId = :deviceId")
    suspend fun setNodePriority(deviceId: String, isPriority: Boolean)

    @Query("UPDATE contacts SET accessCount = accessCount + 1, lastSeenTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun recordNodeAccess(deviceId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET batteryPercent = :battery, lastSeenTimestamp = :timestamp, isOnline = 1 WHERE deviceId = :deviceId")
    suspend fun updateNodeHeartbeat(deviceId: String, battery: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeenTimestamp = :lastSeen WHERE deviceId = :deviceId")
    suspend fun updatePresence(deviceId: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE contacts SET isOnline = :isOnline, lastSeenTimestamp = :lastSeen, batteryPercent = :battery WHERE deviceId = :deviceId")
    suspend fun updateFullPresence(deviceId: String, isOnline: Boolean, lastSeen: Long, battery: Int)

    @Query("SELECT * FROM contacts WHERE alias LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR deviceId LIKE '%' || :query || '%' ORDER BY isPriority DESC, isOnline DESC")
    fun searchContacts(query: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE rawPublicKeyBase64 != '' OR encryptedPublicKey != '' ORDER BY isPriority DESC, alias ASC")
    fun getContactsWithPublicKeys(): Flow<List<ContactEntity>>

    @Query("UPDATE contacts SET encryptedPublicKey = :encryptedKey, rawPublicKeyBase64 = :rawKey, publicKeyFingerprint = :fingerprint, keyAlgorithm = :algorithm, isKeyVerified = 1, keyExchangeTimestamp = :timestamp WHERE deviceId = :deviceId")
    suspend fun updateContactPublicKey(
        deviceId: String,
        rawKey: String,
        encryptedKey: String,
        fingerprint: String,
        algorithm: String,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("SELECT rawPublicKeyBase64 FROM contacts WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getPublicKeyForContact(deviceId: String): String?

    @Query("SELECT encryptedPublicKey FROM contacts WHERE deviceId = :deviceId LIMIT 1")
    suspend fun getEncryptedPublicKeyForContact(deviceId: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<ContactEntity>)

    @Update
    suspend fun updateContact(contact: ContactEntity)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE deviceId = :deviceId")
    suspend fun deleteContactById(deviceId: String)
}
