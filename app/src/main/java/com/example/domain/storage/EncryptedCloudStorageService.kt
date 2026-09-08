package com.example.domain.storage

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Metadata for files stored in the Cloud Vault with end-to-end encryption (AES-256-GCM)
 */
data class CloudVaultFileMetadata(
    val fileId: String,
    val fileName: String,
    val originalSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val remoteStoragePath: String,
    val downloadUrl: String = "",
    val encryptionAlgorithm: String = "AES-256-GCM",
    val ivBase64: String,
    val sha256Checksum: String,
    val uploadedAtTimestamp: Long = System.currentTimeMillis(),
    val uploaderId: String = "",
    val isDecryptedLocally: Boolean = false,
    val localFilePath: String? = null
)

sealed class CloudVaultTransferState {
    object Idle : CloudVaultTransferState()
    data class Transferring(
        val fileName: String,
        val progressPercent: Int,
        val bytesTransferred: Long,
        val totalBytes: Long,
        val isUpload: Boolean
    ) : CloudVaultTransferState()
    data class Completed(
        val fileName: String,
        val isUpload: Boolean,
        val message: String
    ) : CloudVaultTransferState()
    data class Failed(
        val fileName: String,
        val error: String
    ) : CloudVaultTransferState()
}

/**
 * File storage service interface using Firebase Storage that supports encrypted
 * file uploads and downloads for the 'Cloud Vault' feature.
 */
interface EncryptedCloudStorageService {

    /**
     * Encrypts a local file using AES-256-GCM and uploads the ciphertext to Firebase Storage
     * in the Cloud Vault directory, returning metadata including the IV and checksum.
     */
    suspend fun uploadEncryptedFile(
        localFile: File,
        vaultFolder: String = "cloud_vault_files"
    ): Result<CloudVaultFileMetadata>

    /**
     * Downloads an encrypted file from Firebase Storage, decrypts it using AES-256-GCM,
     * verifies its integrity against the SHA-256 checksum, and writes it to the destination.
     */
    suspend fun downloadAndDecryptFile(
        metadata: CloudVaultFileMetadata,
        destinationFile: File
    ): Result<File>

    /**
     * Observes transfer progress for encrypted uploads
     */
    fun uploadEncryptedFileWithProgress(
        localFile: File,
        vaultFolder: String = "cloud_vault_files"
    ): Flow<CloudVaultTransferState>

    /**
     * Lists all encrypted files available in the user's Cloud Vault
     */
    suspend fun listVaultFiles(vaultFolder: String = "cloud_vault_files"): Result<List<CloudVaultFileMetadata>>

    /**
     * Deletes an encrypted file from Firebase Storage and associated metadata
     */
    suspend fun deleteVaultFile(remoteStoragePath: String): Result<Boolean>
}
