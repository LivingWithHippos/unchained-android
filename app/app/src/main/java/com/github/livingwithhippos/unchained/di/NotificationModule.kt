package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped

@Module
@InstallIn(ServiceComponent::class)
object NotificationModule {

    @ServiceScoped
    @Provides
    fun provideNotificationBuilder(
        @ApplicationContext applicationContext: Context
    ): NotificationCompat.Builder = NotificationCompat.Builder(applicationContext, UnchainedApplication.CHANNEL_ID)
        .setSmallIcon(R.mipmap.icon_launcher)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setGroup(ForegroundTorrentService.GROUP_KEY_TORRENTS)

    @ServiceScoped
    @Provides
    fun provideNotificationManager(
        @ApplicationContext applicationContext: Context
    ): NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
}