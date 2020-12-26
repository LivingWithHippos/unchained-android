package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.github.livingwithhippos.unchained.data.local.CredentialsDao
import com.github.livingwithhippos.unchained.data.local.HostRegexDao
import com.github.livingwithhippos.unchained.data.local.UnchaineDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the database injected with Dagger Hilt
 */
@InstallIn(SingletonComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): UnchaineDB {
        return Room.databaseBuilder(
            appContext,
            UnchaineDB::class.java,
            "unchained_db"
        )
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideCredentialsDao(database: UnchaineDB): CredentialsDao {
        return database.credentialsDao()
    }

    @Provides
    fun provideHostRegexDao(database: UnchaineDB): HostRegexDao {
        return database.hostRegexDao()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE `host_regex` (`regex` TEXT NOT NULL, " +
                        "PRIMARY KEY(`regex`))"
            )
        }
    }
}