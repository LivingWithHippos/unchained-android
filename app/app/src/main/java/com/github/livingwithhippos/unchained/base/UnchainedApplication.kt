package com.github.livingwithhippos.unchained.base

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.acra.config.httpSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra
import org.acra.security.TLS
import org.acra.sender.HttpSender
import timber.log.Timber
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

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        // add error report for debug builds
        if (BuildConfig.DEBUG) {
            initAcra {
                //core configuration:
                buildConfigClass = BuildConfig::class.java
                reportFormat = StringFormat.JSON
                httpSender {
                    //required. Https recommended
                    uri = "https://acrarium.professiona.li/report"
                    //optional. Enables http basic auth
                    basicAuthLogin = BuildConfig.ACRA_LOGIN
                    //required if above set
                    basicAuthPassword = BuildConfig.ACRA_PASSWORD
                    // defaults to POST
                    httpMethod = HttpSender.Method.POST
                    //defaults to false. Recommended if your backend supports it
                    compress = true
                    //defaults to all
                    tlsProtocols = arrayOf(TLS.V1_3, TLS.V1_2)
                }
            }
        }
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
