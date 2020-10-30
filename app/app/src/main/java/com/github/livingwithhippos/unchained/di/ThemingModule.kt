package com.github.livingwithhippos.unchained.di

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.base.ThemingCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import javax.inject.Singleton

/**
 * Provides the activity lifecycle callback to be injected with Dagger Hilt
 */
@InstallIn(ApplicationComponent::class)
@Module
object ThemingModule {

    @Provides
    @Singleton
    fun provideThemingCallback(preferences: SharedPreferences): ThemingCallback {
        return ThemingCallback(preferences)
    }
}