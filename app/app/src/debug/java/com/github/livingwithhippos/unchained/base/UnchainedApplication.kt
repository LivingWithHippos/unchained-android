package com.github.livingwithhippos.unchained.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.github.livingwithhippos.unchained.BuildConfig.COUNTLY_APP_KEY
import com.github.livingwithhippos.unchained.BuildConfig.COUNTLY_URL
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.local.ProtoStore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ly.count.android.sdk.Countly
import ly.count.android.sdk.CountlyConfig
import ly.count.android.sdk.DeviceId
import timber.log.Timber
import javax.inject.Inject

/**
 * Entry point for the Dagger-Hilt injection.
 * Deletes incomplete credentials from the datastore on start
 */
@HiltAndroidApp
class UnchainedApplication : Application() {

    /*************************************************
     * DUPLICATE CHANGES IN THE RELEASE FILE VERSION *
     *************************************************/

    @Inject
    lateinit var preferences: SharedPreferences

    @Inject
    lateinit var activityCallback: ThemingCallback

    @Inject
    lateinit var protoStore: ProtoStore

    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.Default + job)

    override fun onCreate() {
        super.onCreate()

        registerActivityLifecycleCallbacks(activityCallback)

        scope.launch {
            protoStore.deleteIncompleteCredentials()
        }

        createNotificationChannel()

        // remove these lines from the release file
        Timber.plant(Timber.DebugTree())

        val config: CountlyConfig = CountlyConfig(this, COUNTLY_APP_KEY, COUNTLY_URL)
            .setIdMode(DeviceId.Type.OPEN_UDID)
            .enableCrashReporting()
            // if true will print internal countly logs to the console
            .setLoggingEnabled(false)
        // .setParameterTamperingProtectionSalt("SampleSalt")

        Countly.sharedInstance().init(config)
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
