package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.room.Room
import com.github.livingwithhippos.unchained.data.local.CredentialsDao
import com.github.livingwithhippos.unchained.data.local.UnchaineDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

/**
 * Provides the database injected with Dagger Hilt
 */
@InstallIn(ApplicationComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): UnchaineDB {
        return Room.databaseBuilder(
            appContext,
            UnchaineDB::class.java,
            "unchained_db"
        ).build()
    }

    @Provides
    fun provideCredentialsDao(database: UnchaineDB): CredentialsDao {
        return database.credentialsDao()
    }
}