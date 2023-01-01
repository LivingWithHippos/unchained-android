package com.github.livingwithhippos.unchained.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.local.RepositoryDataDao
import com.github.livingwithhippos.unchained.data.model.Repository
import com.github.livingwithhippos.unchained.utilities.PLUGINS_REPOSITORY_LINK
import com.github.livingwithhippos.unchained.utilities.TelemetryManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Entry point for the Dagger-Hilt injection.
 * Deletes incomplete credentials from the datastore on start
 */
@HiltAndroidApp
class UnchainedApplication : Application() {

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var activityCallback: ThemingCallback

    @Inject
    lateinit var protoStore: ProtoStore

    @Inject
    lateinit var pluginRepositoryDataDao: RepositoryDataDao

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(activityCallback)

        scope.launch {
            protoStore.deleteIncompleteCredentials()
            if (pluginRepositoryDataDao.getDefaultRepository().isEmpty())
                pluginRepositoryDataDao.insert(
                    Repository(
                        PLUGINS_REPOSITORY_LINK
                    )
                )
        }

        createNotificationChannels()

        TelemetryManager.onCreate(this)
    }

    private fun createNotificationChannels() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val torrentChannel = NotificationChannel(
                TORRENT_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.torrent_channel_description)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Register the channels with the system
            notificationManager.createNotificationChannel(torrentChannel)

            val downloadChannel = NotificationChannel(
                DOWNLOAD_CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.download_channel_description)
            }
            notificationManager.createNotificationChannel(downloadChannel)
        }
    }

    companion object {
        const val TORRENT_CHANNEL_ID = "unchained_torrent_channel"
        const val DOWNLOAD_CHANNEL_ID = "unchained_download_channel"
    }
}
