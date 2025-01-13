package com.github.livingwithhippos.unchained.di

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import com.github.livingwithhippos.unchained.data.service.ForegroundTorrentService
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module

val notificationModule = module {
    single<NotificationCompat.Builder>(named("TorrentNotification")) {
        NotificationCompat.Builder(androidContext(), UnchainedApplication.TORRENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_background)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setGroup(ForegroundTorrentService.GROUP_KEY_TORRENTS)
    }

    single<NotificationCompat.Builder>(named("TorrentSummaryNotification")) {
        NotificationCompat.Builder(androidContext(), UnchainedApplication.TORRENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.logo_no_background)
            .setContentTitle(androidContext().getString(R.string.monitor_torrents_download))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(ForegroundTorrentService.GROUP_KEY_TORRENTS)
            .setGroupSummary(true)
    }

    single {
        NotificationManagerCompat.from(androidContext())
    }
}