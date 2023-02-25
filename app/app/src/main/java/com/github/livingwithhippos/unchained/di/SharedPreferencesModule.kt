package com.github.livingwithhippos.unchained.di

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Provides the shared preferences injected with Dagger Hilt */
@InstallIn(SingletonComponent::class)
@Module
object SharedPreferencesModule {

    @Provides
    @Singleton
    fun providePreferences(@ApplicationContext appContext: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
    }
}
