package com.github.livingwithhippos.unchained.base.model.database

import android.content.Context
import androidx.room.Room
import com.github.livingwithhippos.unchained.base.model.dao.CredentialsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext appContext: Context): UnchaindeDB {
        return Room.databaseBuilder(
            appContext,
            UnchaindeDB::class.java,
            "unachained_db"
        ).build()
    }

    @Provides
    fun provideLogDao(database: UnchaindeDB): CredentialsDao {
        return database.credentialsDao()
    }
}