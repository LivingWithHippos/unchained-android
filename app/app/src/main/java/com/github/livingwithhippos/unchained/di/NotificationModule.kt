package com.github.livingwithhippos.unchained.di

import android.content.Context
import androidx.core.app.NotificationCompat
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton

@InstallIn(ApplicationComponent::class)
@Module
object NotificationModule {

    private const val GROUP_KEY_TORRENTS: String = "group_key_torrent"

    @Provides
    @Singleton
    fun provideNotificationBuilder(@ApplicationContext appContext: Context): NotificationCompat.Builder {
        // fixme: using the logo from R.mipmap.icon_launcher gives a painted icon, this one does not. Maybe because this is missing a background?
        return NotificationCompat.Builder(appContext, UnchainedApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_logo_no_bg)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setGroup(GROUP_KEY_TORRENTS)

    }

}