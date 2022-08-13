package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import com.github.livingwithhippos.unchained.data.service.ForegroundDownloadService.Companion.GROUP_KEY_DOWNLOADS
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
    @TorrentNotification
    fun provideTorrentNotificationBuilder(
        @ApplicationContext applicationContext: Context
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, UnchainedApplication.TORRENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setGroup(ForegroundTorrentService.GROUP_KEY_TORRENTS)


    @ServiceScoped
    @Provides
    @TorrentSummaryNotification
    fun provideTorrentSummaryNotificationBuilder(
        @ApplicationContext applicationContext: Context
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, UnchainedApplication.TORRENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_background)
            .setContentTitle(applicationContext.getString(R.string.monitor_downloads))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(ForegroundTorrentService.GROUP_KEY_TORRENTS)
            .setGroupSummary(true)

    @ServiceScoped
    @Provides
    @DownloadNotification
    fun provideDownloadNotificationBuilder(
        @ApplicationContext applicationContext: Context
    ): NotificationCompat.Builder =
        NotificationCompat.Builder(applicationContext, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_background)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            // ongoing means it can't be swiped away
            .setOngoing(true)
            .setGroup(GROUP_KEY_DOWNLOADS)
            .setGroupSummary(true)

    @ServiceScoped
    @Provides
    fun provideNotificationManager(
        @ApplicationContext applicationContext: Context
    ): NotificationManagerCompat = NotificationManagerCompat.from(applicationContext)
}
