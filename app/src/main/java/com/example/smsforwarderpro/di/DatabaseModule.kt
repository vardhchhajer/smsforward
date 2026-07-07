package com.example.smsforwarderpro.di

import android.content.Context
import androidx.room.Room
import com.example.smsforwarderpro.data.local.db.AppDatabase
import com.example.smsforwarderpro.data.local.db.ForwardAttemptDao
import com.example.smsforwarderpro.data.local.db.ForwardingRuleDao
import com.example.smsforwarderpro.data.local.db.MessageLogDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        prefs: com.example.smsforwarderpro.data.local.pref.EncryptedPreferencesManager
    ): AppDatabase {
        val dbName = "sms_forwarder_pro.db"
        val passphrase = prefs.databasePassphrase
        val factory = SupportFactory(passphrase)

        // Pre-check: Attempt to open the database. If it's an unencrypted DB, SQLCipher will throw.
        // We delete it so Room can recreate an encrypted one.
        val dbFile = context.getDatabasePath(dbName)
        if (dbFile.exists()) {
            try {
                SQLiteDatabase.openDatabase(
                    dbFile.absolutePath,
                    String(passphrase), // SQLiteDatabase uses string/chars for checking
                    null,
                    SQLiteDatabase.OPEN_READONLY
                ).close()
            } catch (e: Exception) {
                // If it fails to open, it's likely an older unencrypted DB or corrupted.
                // Since fallbackToDestructiveMigration() only works on version changes,
                // we manually delete the file here to force a recreation.
                dbFile.delete()
                File("${dbFile.absolutePath}-journal").delete()
                File("${dbFile.absolutePath}-wal").delete()
                File("${dbFile.absolutePath}-shm").delete()
            }
        }

        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            dbName
        )
        .openHelperFactory(factory)
        .fallbackToDestructiveMigration()
        .build()
    }

    @Provides
    fun provideMessageLogDao(db: AppDatabase): MessageLogDao {
        return db.messageLogDao()
    }

    @Provides
    fun provideForwardAttemptDao(db: AppDatabase): ForwardAttemptDao {
        return db.forwardAttemptDao()
    }

    @Provides
    fun provideForwardingRuleDao(db: AppDatabase): ForwardingRuleDao {
        return db.forwardingRuleDao()
    }
}
