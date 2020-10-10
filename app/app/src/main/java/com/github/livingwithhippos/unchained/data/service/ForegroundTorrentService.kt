package com.github.livingwithhippos.unchained.data.service

import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.repositoy.CredentialsRepository
import com.github.livingwithhippos.unchained.data.repositoy.TorrentsRepository
import com.github.livingwithhippos.unchained.utilities.loadingStatusList
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundTorrentService : LifecycleService() {

    @Inject
    lateinit var torrentRepository: TorrentsRepository

    @Inject
    lateinit var credentialsRepository: CredentialsRepository

    private val torrentBinder = Binder()

    private val torrentsLiveData = MutableLiveData<List<TorrentItem>>()

    @Inject
    lateinit var builder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    @Inject
    lateinit var preferences: SharedPreferences


    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return torrentBinder
    }

    /**
     * Binder for the client. It can be used to retrieve this service and call its public methods.
     */
    inner class TorrentServiceBinder : Binder() {
        internal val service: ForegroundTorrentService
            get() = this@ForegroundTorrentService
    }

    override fun onCreate() {
        super.onCreate()

        torrentsLiveData.observe(this, { list ->

            // the torrents we were observing
            val oldTorrentsIDs: Set<String> =
                preferences.getStringSet(KEY_OBSERVED_TORRENTS, emptySet()) as Set<String>
            // their updated status
            val newLoadingTorrents =
                list.filter { torrent -> loadingStatusList.contains(torrent.status) }
            // the new torrents to add to the notification system
            val unwatchedTorrents = newLoadingTorrents.filter { !oldTorrentsIDs.contains(it.id) }
            // the torrent whose status is not a loading one anymore.
            //todo: vibrate once if this is not empty
            val finishedTorrents = list
                // They are in our old list
                .filter { oldTorrentsIDs.contains(it.id) }
                // They aren't in our new loading list
                .filter { !newLoadingTorrents.map { newT -> newT.id }.contains(it.id) }
            // the torrents not in our updated list anymore. These needs to be retrieved and analyzed singularly.
            // Should't happen often since there is a limit on how many active torrents you can have in real debrid
            // and we retrieve the last 30 torrents every time
            val missingTorrents = oldTorrentsIDs.filter { id ->
                !list.map { it.id }.contains(id)
            }

            // update the torrents id to observe
            val newIDs = mutableSetOf<String>()
            newIDs.addAll(newLoadingTorrents.map { it.id })
            with(preferences.edit()) {
                putStringSet(KEY_OBSERVED_TORRENTS, newIDs)
                apply()
            }

            // let's first operate as if all the needed torrents were always in the list

            // update the notifications for torrents in one of the loading statuses
            newLoadingTorrents.forEach { torrent ->
                updateNotification(torrent)
            }
            // update the notifications for torrents in one of the finished statuses
            finishedTorrents.forEach { torrent ->
                completeNotification(torrent)
            }

        })

        lifecycleScope.launch {

            while ((preferences.getStringSet(
                    KEY_OBSERVED_TORRENTS,
                    emptySet()
                ) as Set<String>).size > 0
            ) {
                // todo: manage token values
                val token = credentialsRepository.getToken()
                val torrents = torrentRepository.getTorrentsList(token, limit = 30)
                torrentsLiveData.postValue(torrents)
                // update notifications every 5 seconds
                delay(5000)
            }
        }
    }

    private fun updateNotification(item: TorrentItem) {
        notificationManager.apply {
            builder.setContentText(item.filename)
            if (item.status == "downloading") {
                builder.setProgress(100, item.progress, false)
                    .setContentTitle(getString(R.string.torrent_in_progress_format, item.progress))
            } else {
                builder.setContentTitle(item.status)
                    // note: this could be indeterminate = true since it's technically in a loading status which should change
                    .setProgress(0, 0, false)
            }
            notify(item.id.hashCode(), builder.build())
        }
    }

    private fun completeNotification(item: TorrentItem) {
        notificationManager.apply {
            builder.setContentTitle(item.status)
                // if the file is already downloaded the second row will not be set elsewhere
                .setContentText(item.filename)
                // remove the progressbar if present
                .setProgress(0, 0, false)
            notify(item.id.hashCode(), builder.build())
        }
    }

    companion object {
        const val GROUP_KEY_TORRENTS: String = "group_key_torrent"
        const val KEY_OBSERVED_TORRENTS: String = "observed_torrents_key"
    }
}