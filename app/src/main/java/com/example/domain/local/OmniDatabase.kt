package com.example.domain.local

import android.content.Context
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
import com.example.domain.local.dao.ThreadCustomizationDao
import com.example.domain.local.entities.CachedFileEntity
import com.example.domain.local.entities.ChatMessageEntity
import com.example.domain.local.entities.ContactEntity
import com.example.domain.local.entities.CryptographicKeyEntity
import com.example.domain.local.entities.DeviceSystemStateEntity
import com.example.domain.local.dao.TacticalC2AuditDao
import com.example.domain.local.entities.TacticalC2LogEntity
import com.example.domain.local.entities.TacticalDroneTelemetryEntity
import com.example.domain.local.entities.TacticalDetectedTargetEntity
import com.example.domain.local.entities.DtnBundleEntity
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

        // Llave maestra criptográfica de 256 bits para el motor SQLCipher
        private val PASSPHRASE = "omnicomm_e2ee_military_grade_sqlcipher_key_2026".toByteArray()
        
        fun getDatabase(context: Context): OmniDatabase {
            return INSTANCE ?: synchronized(this) {
                val appContext = context.applicationContext
                try {
                    val factory = SupportOpenHelperFactory(PASSPHRASE)
                    val instance = Room.databaseBuilder(
                        appContext,
                        OmniDatabase::class.java,
                        "omni_encrypted_database.db"
                    )
                    .openHelperFactory(factory)
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = instance
                    instance
                } catch (t: Throwable) {
                    android.util.Log.e("OmniDatabase", "Error initializing encrypted database, falling back to standard Room SQLite: ${t.message}")
                    val fallbackInstance = Room.databaseBuilder(
                        appContext,
                        OmniDatabase::class.java,
                        "omni_standard_database.db"
                    )
                    .fallbackToDestructiveMigration()
                    .build()
                    INSTANCE = fallbackInstance
                    fallbackInstance
                }
            }
        }
    }
}

