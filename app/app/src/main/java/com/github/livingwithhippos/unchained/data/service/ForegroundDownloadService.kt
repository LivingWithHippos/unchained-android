package com.github.livingwithhippos.unchained.data.service

import android.app.Notification
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.di.DownloadNotification
import com.github.livingwithhippos.unchained.utilities.Downloader
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundDownloadService : LifecycleService() {

    @Inject
    lateinit var downloader: Downloader

    private val downloadBinder = DownloadBinder()

    @Inject
    @DownloadNotification
    lateinit var downloadBuilder: NotificationCompat.Builder

    @Inject
    lateinit var notificationManager: NotificationManagerCompat

    val mutex = Mutex()

    /**
     * Binder for the client. It can be used to retrieve this service and call its public methods.
     */
    inner class DownloadBinder : Binder() {
        internal val service: ForegroundDownloadService
            get() = this@ForegroundDownloadService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return downloadBinder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val input = intent?.getStringExtra("ASD")
        startForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        startForeground(SUMMARY_ID, downloadBuilder.build())
    }

    fun queueDownload(source: String, destination: Uri) {

        val fileName = destination.lastPathSegment
        Timber.e("filename $fileName")
        // checking if I already have this download in the queue
        val replaceDownload = downloads.firstOrNull { it.source == source }
        if (replaceDownload != null) {
            // in these cases I can restart it eventually
            if (
                replaceDownload.status == DownloadStatus.Queued ||
                replaceDownload.status == DownloadStatus.Error
            ) {
                replaceDownload.destination = destination
                replaceDownload.status = DownloadStatus.Queued
                replaceDownload.progress = 0
                replaceDownload.speed = 0
            } else {
                // not replacing anything for a running or stopped download because it could already be partially downloaded
            }

        } else {
            Timber.e("Nuovo download, filename " + destination.lastPathSegment)
            // new download!
            downloads.add(
                CustomDownload(
                    source = source,
                    destination = destination
                )
            )
        }
        startDownloadIfAvailable()
    }

    /**
     * Only call this from the mutex lock. I use this to download a single file at once
     */
    private fun startDownloadIfAvailable() {
        if (downloads.firstOrNull { it.status == DownloadStatus.Running } == null) {
            val currentDownload = downloads.firstOrNull { it.status == DownloadStatus.Queued }
            if (currentDownload != null) {
                currentDownload.status = DownloadStatus.Running
                val outputStream = contentResolver?.openOutputStream(currentDownload.destination)
                if (outputStream != null) {
                    lifecycleScope.launch {
                        // this must run in another scope to avoid being blocked
                        launch {
                            downloader.progress.collect {
                                currentDownload.progress = it
                                if (it == 100) {
                                    // finished the download, make the last notification cancellable
                                    currentDownload.status = DownloadStatus.Completed
                                    // start a new download from the queue
                                    startDownloadIfAvailable()
                                    updateNotification()
                                } else {
                                    // update the notification
                                    updateNotification()
                                }
                                Timber.d("progress $it")
                            }
                        }
                        downloader.downloadFileViaOKHTTP(currentDownload.source, outputStream)
                    }
                } else {
                    Timber.e("Selected file had null output stream")
                }
            } else {
                // todo: close notifications?
            }
        }
    }

    private fun updateNotification() {

        val notifications: MutableMap<String, Notification> = mutableMapOf()

        downloads.let { currentDownloads ->
            val currentDownload = currentDownloads.firstOrNull { it.status == DownloadStatus.Running }
            val stoppedDownloads = currentDownloads.filter { it.status == DownloadStatus.Stopped }
            val queuedDownload = currentDownloads.filter { it.status == DownloadStatus.Queued }
            val errorDownload = currentDownloads.filter { it.status == DownloadStatus.Error }

            if (currentDownload!=null) {
                downloadBuilder.setProgress(100, currentDownload.progress, false)
                    .setContentTitle(
                        getString(
                            R.string.torrent_in_progress_format,
                            currentDownload.progress,
                            currentDownload.speed.toFloat()
                        )
                    )

                downloadBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(currentDownload.source)
                )

                notifications[currentDownload.source] = downloadBuilder.build()

                if (currentDownload.progress >= 100)
                    currentDownload.status = DownloadStatus.Completed
            }

            stoppedDownloads.forEach {
                downloadBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(it.source)
                )

                notifications[it.source] = downloadBuilder.build()
            }

            queuedDownload.forEach {
                downloadBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(it.source)
                )

                notifications[it.source] = downloadBuilder.build()
            }

            errorDownload.forEach {
                downloadBuilder.setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(it.source)
                )

                notifications[it.source] = downloadBuilder.build()
            }


            notificationManager.apply {
                notifications.forEach { (id, notification) ->
                    notify(id.hashCode(), notification)
                }
            }
        }

    }

    companion object {
        const val GROUP_KEY_DOWNLOADS: String = "com.github.livingwithhippos.unchained.DOWNLOADS"
        const val KEY_DOWNLOADS_ID = "downloads_id_key"
        const val SUMMARY_ID: Int = 308
        val downloads = mutableListOf<CustomDownload>()
    }
}

data class CustomDownload(
    val source: String,
    var destination: Uri,
    var status: DownloadStatus = DownloadStatus.Queued,
    var progress: Int = 0,
    var speed: Int = 0
)

sealed class DownloadStatus {
    object Queued : DownloadStatus()
    object Stopped : DownloadStatus()
    object Completed : DownloadStatus()
    object Running : DownloadStatus()
    object Error : DownloadStatus()
}