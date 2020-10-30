package com.github.livingwithhippos.unchained.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject


/**
 * Entry point for the Dagger-Hilt injection.
 * Deletes incomplete credentials from the db on start
 */
@HiltAndroidApp
class UnchainedApplication : Application() {
    @Inject
    lateinit var credentialsRepository: CredentialsRepository

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var activityCallback: ThemingCallback

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(activityCallback)

        scope.launch {
            credentialsRepository.deleteIncompleteCredentials()
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = getString(R.string.torrent_channel_description)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "unchained_torrent_channel"
    }
}