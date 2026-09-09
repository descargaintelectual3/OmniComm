package com.example.domain.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.local.dao.CachedFileDao
import com.example.domain.local.dao.ChatDao
import com.example.domain.local.dao.ContactDao
import com.example.domain.local.dao.CryptographicKeyDao
import com.example.domain.local.dao.DeviceSystemStateDao
import com.example.domain.local.dao.DtnBundleDao
import com.example.domain.local.dao.SessionStateDao
import com.example.domain.local.dao.TacticalC2AuditDao
import com.example.domain.local.dao.ThreadCustomizationDao
import com.example.domain.local.entities.CachedFileEntity
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ContactEntity
import com.example.domain.local.entities.CryptographicKeyEntity
import com.example.domain.local.entities.DeviceSystemStateEntity
import com.example.domain.local.entities.DtnBundleEntity
import com.example.domain.local.entities.TacticalC2LogEntity
import com.example.domain.local.entities.TacticalDetectedTargetEntity
import com.example.domain.local.entities.TacticalDroneTelemetryEntity
import com.example.domain.local.entities.ThreadCustomizationEntity
import com.example.domain.local.entities.UnifiedSessionStateEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(
    entities = [
        ChatMessageEntity::class, 
        ContactEntity::class, 
        CachedFileEntity::class,
        DeviceSystemStateEntity::class,
        UnifiedSessionStateEntity::class,
        ThreadCustomizationEntity::class,
        CryptographicKeyEntity::class,
        DtnBundleEntity::class,
        TacticalC2LogEntity::class,
        TacticalDroneTelemetryEntity::class,
        TacticalDetectedTargetEntity::class
    ],
    version = 11,
    exportSchema = false
)
abstract class OmniDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun contactDao(): ContactDao
    abstract fun cachedFileDao(): CachedFileDao
    abstract fun deviceSystemStateDao(): DeviceSystemStateDao
    abstract fun sessionStateDao(): SessionStateDao
    abstract fun threadCustomizationDao(): ThreadCustomizationDao
    abstract fun cryptographicKeyDao(): CryptographicKeyDao
    abstract fun dtnBundleDao(): DtnBundleDao
    abstract fun tacticalC2AuditDao(): TacticalC2AuditDao

    companion object {
        @Volatile
        private var INSTANCE: OmniDatabase? = null

        private const val TAG = "OmniDatabase"

        // Llave maestra criptográfica de 256 bits para el motor SQLCipher
        private val PASSPHRASE = "omnicomm_e2ee_military_grade_sqlcipher_key_2026".toByteArray()

        private var isSqlCipherNativeLoaded = false

        init {
            loadSqlCipherNative()
        }

        fun loadSqlCipherNative(): Boolean {
            if (isSqlCipherNativeLoaded) return true
            return try {
                System.loadLibrary("sqlcipher")
                isSqlCipherNativeLoaded = true
                Log.i(TAG, "SQLCipher native library loaded successfully.")
                true
            } catch (t: Throwable) {
                isSqlCipherNativeLoaded = false
                Log.w(TAG, "Unable to load SQLCipher native library (${t.message}). Safe SQLite fallback active.")
                false
            }
        }
        
        fun getDatabase(context: Context): OmniDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let { return it }
                val appContext = context.applicationContext
                var dbInstance: OmniDatabase? = null

                if (loadSqlCipherNative()) {
                    try {
                        val factory = SupportOpenHelperFactory(PASSPHRASE)
                        val encDb = Room.databaseBuilder(
                            appContext,
                            OmniDatabase::class.java,
                            "omni_encrypted_database.db"
                        )
                        .openHelperFactory(factory)
                        .fallbackToDestructiveMigration()
                        .build()

                        // Test connection synchronously so any UnsatisfiedLinkError is caught here
                        encDb.openHelper.writableDatabase
                        dbInstance = encDb
                        Log.i(TAG, "OmniDatabase initialized with SQLCipher AES-256 encryption.")
                    } catch (t: Throwable) {
                        Log.w(TAG, "SQLCipher database open failed (${t.message}), switching to standard SQLite fallback.", t)
                        try {
                            dbInstance?.close()
                        } catch (_: Throwable) {}
                        dbInstance = null
                    }
                }

                if (dbInstance == null) {
                    try {
                        val standardDb = Room.databaseBuilder(
                            appContext,
                            OmniDatabase::class.java,
                            "omni_standard_database.db"
                        )
                        .fallbackToDestructiveMigration()
                        .build()

                        standardDb.openHelper.writableDatabase
                        dbInstance = standardDb
                        Log.i(TAG, "OmniDatabase initialized with standard SQLite.")
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error opening standard SQLite database: ${t.message}", t)
                        // In-memory fallback as ultimate safeguard
                        dbInstance = Room.inMemoryDatabaseBuilder(
                            appContext,
                            OmniDatabase::class.java
                        )
                        .fallbackToDestructiveMigration()
                        .build()
                    }
                }

                INSTANCE = dbInstance
                dbInstance
            }
        }
    }
}

