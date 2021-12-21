package com.github.livingwithhippos.unchained.data.service

import android.app.Notification
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.OpenableColumns
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.di.DownloadNotification
import com.github.livingwithhippos.unchained.utilities.Downloader
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
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
                replaceDownload.speed = 0f
            } else {
                // not replacing anything for a running or stopped download because it could already be partially downloaded
            }

        } else {
            // new download!
            var fileName: String = destination.path?.substringAfterLast("/") ?: "download"
            val cursor = contentResolver.query(
                destination,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )

            cursor.use {
                if (it != null && it.moveToFirst()) {
                    fileName = it.getString(0)
                }
            }

            downloads.add(
                CustomDownload(
                    source = source,
                    destination = destination,
                    title = fileName
                )
            )
        }
        startDownloadIfAvailable()
    }

    /**
     * Only call this from the mutex lock. I use this to download a single file at once
     */
    private fun startDownloadIfAvailable() {
        // if I have no running downloads
        if (downloads.firstOrNull { it.status == DownloadStatus.Running } == null) {
            // Start the first queued download
            val currentDownload = downloads.firstOrNull { it.status == DownloadStatus.Queued }
            if (currentDownload != null) {
                currentDownload.status = DownloadStatus.Running
                val outputStream = contentResolver?.openOutputStream(currentDownload.destination)
                if (outputStream != null) {
                    lifecycleScope.launch {
                        // the collect must be run in another scope to avoid being blocked
                        launch {
                            var lastRegisteredTime = System.currentTimeMillis()
                            var lastRegisteredSize = 0.0
                            downloader.downloadInfo.collect {
                                val currentTime = System.currentTimeMillis()
                                // update speed according to the last second
                                if (currentTime - lastRegisteredTime > 2000) {
                                    currentDownload.speed =
                                        ((it.second - lastRegisteredSize) / (currentTime - lastRegisteredTime) / 1000).toFloat()
                                    lastRegisteredTime = currentTime
                                    lastRegisteredSize = it.second
                                }
                                currentDownload.progress = (it.second * 100 / it.first).toInt()
                                // update the notification
                                updateNotification()
                            }
                        }
                        downloader.downloadFileViaOKHTTP(currentDownload.source, outputStream)
                    }
                } else {
                    Timber.e("Selected file had null output stream")
                }
            } else {
                // notifications are managed in updateNotification, just skip this one
            }
        }
    }

    private fun updateNotification() {

        val notifications: MutableMap<String, Notification> = mutableMapOf()

        downloads.firstOrNull { it.status == DownloadStatus.Running }?.let { currentDownload ->

            downloadBuilder.setProgress(100, currentDownload.progress, false)
                .setContentTitle(
                    getString(
                        R.string.torrent_in_progress_format,
                        currentDownload.progress,
                        currentDownload.speed
                    )
                )

            downloadBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(currentDownload.title)
            )

            // todo: finished the download, make the last notification cancellable
            if (currentDownload.progress >= 100) {
                currentDownload.status = DownloadStatus.Completed
                applicationContext.vibrate()
                startDownloadIfAvailable()
            }

            if (currentDownload.status == DownloadStatus.Running && currentDownload.progress < 100)
                downloadBuilder.setOngoing(true)
            else
                downloadBuilder.setOngoing(false)

            notifications[currentDownload.source] = downloadBuilder.build()
        }

        downloads.filter { it.status == DownloadStatus.Stopped }.forEach {
            downloadBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(it.title)
            )

            notifications[it.source] = downloadBuilder.build()
        }

        downloads.filter { it.status == DownloadStatus.Queued }.forEach {
            downloadBuilder
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(it.title)
                ).setOngoing(false)
                .setProgress(0, 0, false)
                .setContentTitle(getString(R.string.queued))

            notifications[it.source] = downloadBuilder.build()
        }

        downloads.filter { it.status == DownloadStatus.Completed }.forEach {
            downloadBuilder
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(it.title)
                ).setOngoing(false)
                .setProgress(0, 0, false)
                .setContentTitle(getString(R.string.download_complete))

            notifications[it.source] = downloadBuilder.build()
        }

        downloads.filter { it.status == DownloadStatus.Error }.forEach {
            downloadBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(it.title)
            )

            notifications[it.source] = downloadBuilder.build()
        }

        notificationManager.apply {
            notifications.forEach { (id, notification) ->
                notify(id.hashCode(), notification)
            }
        }

        // stop serving completed download notifications
        downloads.removeAll {
            it.status == DownloadStatus.Completed ||
                    it.status == DownloadStatus.Error
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
    var title: String,
    var status: DownloadStatus = DownloadStatus.Queued,
    var progress: Int = 0,
    var speed: Float = 0f
)

sealed class DownloadStatus {
    object Queued : DownloadStatus()
    object Stopped : DownloadStatus()
    object Completed : DownloadStatus()
    object Running : DownloadStatus()
    object Error : DownloadStatus()
}