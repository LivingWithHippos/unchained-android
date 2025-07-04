package com.github.livingwithhippos.unchained.data.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.MainActivity
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repository.TorrentsRepository
import com.github.livingwithhippos.unchained.di.TorrentNotification
import com.github.livingwithhippos.unchained.di.TorrentSummaryNotification
import com.github.livingwithhippos.unchained.settings.view.SettingsFragment
import com.github.livingwithhippos.unchained.utilities.PreferenceKeys
import com.github.livingwithhippos.unchained.utilities.extension.getStatusTranslation
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

@AndroidEntryPoint
@SuppressLint("MissingPermission")
class ForegroundTorrentService : LifecycleService() {

    @Inject lateinit var torrentRepository: TorrentsRepository

    private val torrentBinder = TorrentBinder()

    private val torrentsLiveData = MutableLiveData<List<TorrentItem>>()

    @Inject @TorrentSummaryNotification lateinit var summaryBuilder: NotificationCompat.Builder

    @Inject @TorrentNotification lateinit var torrentBuilder: NotificationCompat.Builder

    @Inject lateinit var notificationManager: NotificationManagerCompat

    @Inject lateinit var preferences: SharedPreferences

    private var updateTiming = UPDATE_TIMING_SHORT

    private var serviceStart = System.currentTimeMillis()

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return torrentBinder
    }

    /**
     * Binder for the client. It can be used to retrieve this service and call its public methods.
     */
    inner class TorrentBinder : Binder() {
        internal val service: ForegroundTorrentService
            get() = this@ForegroundTorrentService
    }

    override fun onCreate() {
        super.onCreate()
        // here or in onStartCommand()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        torrentsLiveData.observe(this) { list ->
            // todo: manage removed torrents (right now they just stop updating)
            // the torrents we were observing
            val oldTorrentsIDs: Set<String> =
                preferences.getStringSet(KEY_OBSERVED_TORRENTS, emptySet()) as Set<String>
            // their updated status
            val newLoadingTorrents =
                list.filter { torrent -> loadingStatusList.contains(torrent.status) }
            // the torrent whose status is not a loading one anymore.
            val finishedTorrents =
                list
                    // They are in our old list
                    .filter { oldTorrentsIDs.contains(it.id) }
                    // They aren't in our new loading list
                    .filter { !newLoadingTorrents.map { newT -> newT.id }.contains(it.id) }
            /*
            // the new torrents to add to the notification system
            val unwatchedTorrents = newLoadingTorrents.filter { !oldTorrentsIDs.contains(it.id) }
            // the torrents not in our updated list anymore. These needs to be retrieved and analyzed singularly.
            // Shouldn't happen often since there is a limit on how many active torrents you can have in real debrid
            // and we retrieve the last 30 torrents every time
            val missingTorrents = oldTorrentsIDs.filter { id ->
                !list.map { it.id }.contains(id)
            }
             */

            val shouldVibrate =
                preferences.getBoolean(PreferenceKeys.DownloadManager.VIBRATE_ON_FINISH, false)

            // update the torrents id to observe
            val newIDs = mutableSetOf<String>()
            newIDs.addAll(newLoadingTorrents.map { it.id })
            with(preferences.edit()) {
                putStringSet(KEY_OBSERVED_TORRENTS, newIDs)
                apply()
            }
            updateTiming = if (newIDs.isEmpty()) UPDATE_TIMING_LONG else UPDATE_TIMING_SHORT

            // let's first operate as if all the needed torrents were always in the list

            // update the notifications for torrents in one of the loading statuses
            updateNotification(newLoadingTorrents)
            // update the notifications for torrents in one of the finished statuses
            finishedTorrents.forEach { torrent -> completeNotification(torrent) }
            if (shouldVibrate && finishedTorrents.isNotEmpty()) applicationContext.vibrate()
        }

        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                startForeground(SUMMARY_ID, summaryBuilder.build())
            } else {
                startForeground(
                    SUMMARY_ID,
                    summaryBuilder.build(),
                    FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            }
        } catch (ex: Exception) {
            Timber.e("Error starting foreground service: ${ex.message}")
        }

        preferences.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    private val preferenceListener =
        SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
            if (key == SettingsFragment.KEY_TORRENT_NOTIFICATIONS) {
                val enableTorrentNotifications = sharedPreferences?.getBoolean(key, false) ?: false
                if (!enableTorrentNotifications) stopTorrentService()
            }
        }

    private fun startMonitoring() {
        lifecycleScope.launch {
            while (true) {
                // right now on api >= 35 after 6 hours the service will crash
                // because of system imposed limits
                if (
                    Build.VERSION.SDK_INT >= 35 &&
                        System.currentTimeMillis() - serviceStart > 5 * 60 * 60 * 1000
                ) {
                    Timber.w("Service has been running for too long, stopping it.")
                    break
                }
                try {
                    torrentsLiveData.postValue(getTorrentList())
                } catch (ex: IllegalArgumentException) {
                    // no valid token ready, retry later
                }
                // update notifications every 5 seconds
                delay(updateTiming)
            }
            stopSelf()
        }
    }

    private suspend fun getTorrentList(max: Int = 30): List<TorrentItem> {
        return torrentRepository.getTorrentsList(limit = max)
    }

    private fun updateNotification(items: List<TorrentItem>) {

        val notifications: MutableMap<String, Notification> = mutableMapOf()

        items.forEach { torrent ->
            torrentBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(torrent.filename))

            if (torrent.status == "downloading") {
                val speedMBs = (torrent.speed ?: 0).toFloat().div(1000000)
                torrentBuilder
                    .setProgress(100, torrent.progress.toInt(), false)
                    .setContentTitle(
                        getString(R.string.torrent_in_progress_format, torrent.progress, speedMBs)
                    )
                    .setOngoing(true)
            } else {
                torrentBuilder
                    .setContentTitle(applicationContext.getStatusTranslation(torrent.status))
                    // note: this could be indeterminate = true since it's technically in a loading
                    // status
                    // which should change
                    .setProgress(0, 0, false)
                    .setOngoing(false)
            }

            val resultIntent =
                Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(KEY_TORRENT_ID, torrent.id)
                }

            val resultPendingIntent: PendingIntent? =
                TaskStackBuilder.create(this).run {
                    // Add the intent, which inflates the back stack
                    addNextIntentWithParentStack(resultIntent)
                    // Get the PendingIntent containing the entire back stack
                    getPendingIntent(
                        torrent.id.hashCode(),
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        else PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }

            torrentBuilder.setContentIntent(resultPendingIntent)

            notifications[torrent.id] = torrentBuilder.build()
        }
        // will open the app on the torrent details page
        summaryBuilder.setContentText(getString(R.string.downloading_torrent_format, items.size))

        notificationManager.apply {
            // todo: manage permission
            notifications.forEach { (id, notification) -> notify(id.hashCode(), notification) }
            notify(SUMMARY_ID, summaryBuilder.build())
        }
    }

    private fun completeNotification(item: TorrentItem) {

        val resultIntent =
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(KEY_TORRENT_ID, item.id)
            }

        val resultPendingIntent: PendingIntent? =
            TaskStackBuilder.create(this).run {
                // Add the intent, which inflates the back stack
                addNextIntentWithParentStack(resultIntent)
                // Get the PendingIntent containing the entire back stack
                getPendingIntent(
                    item.id.hashCode(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    else PendingIntent.FLAG_UPDATE_CURRENT,
                )
            }

        notificationManager.apply {
            torrentBuilder
                .setContentTitle(applicationContext.getStatusTranslation(item.status))
                // if the file is already downloaded the second row will not be set elsewhere
                .setStyle(NotificationCompat.BigTextStyle().bigText(item.filename))
                // remove the progressbar if present
                .setProgress(0, 0, false)
                // set click intent
                .setContentIntent(resultPendingIntent)
                // remove notification on tap
                .setAutoCancel(true)
                .setOngoing(false)
            notify(item.id.hashCode(), torrentBuilder.build())
        }
    }

    private fun stopTorrentService() {
        torrentsLiveData.value?.let {
            it.forEach { torrent -> notificationManager.cancel(torrent.id.hashCode()) }
        }
        stopSelf()
    }

    companion object {
        const val GROUP_KEY_TORRENTS: String = "group_key_torrent"
        const val KEY_OBSERVED_TORRENTS: String = "observed_torrents_key"
        const val UPDATE_TIMING_SHORT: Long = 5000
        const val UPDATE_TIMING_LONG: Long = 30000
        const val SUMMARY_ID: Int = 21
        const val KEY_TORRENT_ID = "torrent_id_key"
    }
}
