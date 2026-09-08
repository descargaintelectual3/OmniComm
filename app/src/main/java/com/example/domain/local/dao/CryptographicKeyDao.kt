package com.example.domain.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.domain.local.entities.CryptographicKeyEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) de Room para interactuar con la tabla de claves criptográficas locales.
 */
@Dao
interface CryptographicKeyDao {

    @Query("SELECT * FROM cryptographic_keys ORDER BY isPrimary DESC, createdAt DESC")
    fun getAllKeys(): Flow<List<CryptographicKeyEntity>>

    @Query("SELECT * FROM cryptographic_keys WHERE ownerNodeId = 'LOCAL_NODE' ORDER BY isPrimary DESC, createdAt DESC")
    fun getLocalKeyPairs(): Flow<List<CryptographicKeyEntity>>

    @Query("SELECT * FROM cryptographic_keys WHERE ownerNodeId != 'LOCAL_NODE' ORDER BY createdAt DESC")
    fun getPeerPublicKeys(): Flow<List<CryptographicKeyEntity>>

    @Query("SELECT * FROM cryptographic_keys WHERE isPrimary = 1 AND isRevoked = 0 LIMIT 1")
    fun getPrimaryActiveKey(): Flow<CryptographicKeyEntity?>

    @Query("SELECT * FROM cryptographic_keys WHERE keyId = :keyId LIMIT 1")
    suspend fun getKeyById(keyId: String): CryptographicKeyEntity?

    @Query("SELECT * FROM cryptographic_keys WHERE fingerprint = :fingerprint LIMIT 1")
    suspend fun getKeyByFingerprint(fingerprint: String): CryptographicKeyEntity?

    @Query("SELECT * FROM cryptographic_keys WHERE alias LIKE '%' || :query || '%' OR fingerprint LIKE '%' || :query || '%' OR ownerDisplayName LIKE '%' || :query || '%' ORDER BY isPrimary DESC, createdAt DESC")
    fun searchKeys(query: String): Flow<List<CryptographicKeyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: CryptographicKeyEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKeys(keys: List<CryptographicKeyEntity>)

    @Update
    suspend fun updateKey(key: CryptographicKeyEntity)

    @Delete
    suspend fun deleteKey(key: CryptographicKeyEntity)

    @Query("DELETE FROM cryptographic_keys WHERE keyId = :keyId")
    suspend fun deleteKeyById(keyId: String)

    @Query("UPDATE cryptographic_keys SET isPrimary = 0 WHERE ownerNodeId = 'LOCAL_NODE'")
    suspend fun clearPrimaryFlags()

    @Query("UPDATE cryptographic_keys SET isPrimary = 1 WHERE keyId = :keyId")
    suspend fun setPrimaryKey(keyId: String)

    @Query("UPDATE cryptographic_keys SET isRevoked = 1, isPrimary = 0 WHERE keyId = :keyId")
    suspend fun revokeKey(keyId: String)

    @Query("UPDATE cryptographic_keys SET usageCount = usageCount + 1 WHERE keyId = :keyId")
    suspend fun incrementUsage(keyId: String)

    @Query("DELETE FROM cryptographic_keys")
    suspend fun clearAllKeys()
}
