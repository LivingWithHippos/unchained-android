package com.github.livingwithhippos.unchained.utilities.download

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import androidx.work.WorkManager
import com.github.livingwithhippos.unchained.utilities.EMBEDDED_DOWNLOAD_WORK_TAG
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * Cancels active embedded (WorkManager/OkHttp) downloads whenever the default network changes,
 * since continuing a Real-Debrid download from a different IP can get an account flagged. Opt-in
 * via the [PreferenceKeys.DownloadManager.PAUSE_ON_NETWORK_CHANGE_KEY] setting.
 *
 * There is no download resume support anywhere in the app, so this cancels the transfer outright;
 * the user has to restart it manually afterward.
 *
 * Registered once from [com.github.livingwithhippos.unchained.base.UnchainedApplication] instead
 * of an Activity, since embedded downloads run in a foreground-service-backed WorkManager worker
 * that can keep going while the app is backgrounded.
 */
@Singleton
class NetworkChangeDownloadCanceller
@Inject
constructor(@ApplicationContext private val context: Context, private val preferences: SharedPreferences) {

    // Android hands out a new Network instance for the new connection on almost any network
    // switch, including reconnecting to the same wi-fi access point, so comparing instances is a
    // reasonably safe proxy for "the IP address may have changed"
    private var lastActiveNetwork: Network? = null

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val previousNetwork = lastActiveNetwork
                lastActiveNetwork = network
                if (previousNetwork != null && previousNetwork != network) {
                    onNetworkChanged()
                }
            }
        }

    fun start() {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    private fun onNetworkChanged() {
        val cancelOnNetworkChange =
            preferences.getBoolean(PreferenceKeys.DownloadManager.PAUSE_ON_NETWORK_CHANGE_KEY, false)
        if (!cancelOnNetworkChange) return

        Timber.d("Default network changed, cancelling active embedded downloads")
        WorkManager.getInstance(context).cancelAllWorkByTag(EMBEDDED_DOWNLOAD_WORK_TAG)
    }
}
