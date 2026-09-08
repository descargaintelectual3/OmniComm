package com.example.domain.repository

import com.example.domain.local.dao.CryptographicKeyDao
import com.example.domain.local.entities.CryptographicKeyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repositorio para la abstracción y gestión del ciclo de vida de claves criptográficas en Room.
 * Cumple con el Repository Pattern establecido en la arquitectura de persistencia.
 */
class KeyManagementRepository(
    private val keyDao: CryptographicKeyDao
) {
    val allKeys: Flow<List<CryptographicKeyEntity>> = keyDao.getAllKeys()
    val localKeyPairs: Flow<List<CryptographicKeyEntity>> = keyDao.getLocalKeyPairs()
    val peerPublicKeys: Flow<List<CryptographicKeyEntity>> = keyDao.getPeerPublicKeys()
    val primaryActiveKey: Flow<CryptographicKeyEntity?> = keyDao.getPrimaryActiveKey()

    fun searchKeys(query: String): Flow<List<CryptographicKeyEntity>> = keyDao.searchKeys(query)

    suspend fun insertKey(key: CryptographicKeyEntity): Long = withContext(Dispatchers.IO) {
        if (key.isPrimary && key.ownerNodeId == "LOCAL_NODE") {
            keyDao.clearPrimaryFlags()
        }
        keyDao.insertKey(key)
    }

    suspend fun insertKeys(keys: List<CryptographicKeyEntity>) = withContext(Dispatchers.IO) {
        keyDao.insertKeys(keys)
    }

    suspend fun updateKey(key: CryptographicKeyEntity) = withContext(Dispatchers.IO) {
        keyDao.updateKey(key)
    }

    suspend fun deleteKey(key: CryptographicKeyEntity) = withContext(Dispatchers.IO) {
        keyDao.deleteKey(key)
    }

    suspend fun deleteKeyById(keyId: String) = withContext(Dispatchers.IO) {
        keyDao.deleteKeyById(keyId)
    }

    suspend fun setAsPrimary(keyId: String) = withContext(Dispatchers.IO) {
        keyDao.clearPrimaryFlags()
        keyDao.setPrimaryKey(keyId)
    }

    suspend fun revokeKey(keyId: String) = withContext(Dispatchers.IO) {
        keyDao.revokeKey(keyId)
    }

    suspend fun incrementUsage(keyId: String) = withContext(Dispatchers.IO) {
        keyDao.incrementUsage(keyId)
    }

    suspend fun getKeyById(keyId: String): CryptographicKeyEntity? = withContext(Dispatchers.IO) {
        keyDao.getKeyById(keyId)
    }

    suspend fun getKeyByFingerprint(fingerprint: String): CryptographicKeyEntity? = withContext(Dispatchers.IO) {
        keyDao.getKeyByFingerprint(fingerprint)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        keyDao.clearAllKeys()
    }
}
