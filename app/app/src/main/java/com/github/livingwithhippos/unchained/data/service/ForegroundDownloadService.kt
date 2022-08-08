package com.github.livingwithhippos.unchained.data.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.di.DownloadNotification
import com.github.livingwithhippos.unchained.utilities.download.Downloader
import com.github.livingwithhippos.unchained.utilities.download.FileWriter
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.URLConnection
import javax.inject.Inject

@AndroidEntryPoint
class ForegroundDownloadService : LifecycleService() {

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
        startForegroundService()
        return super.onStartCommand(intent, flags, startId)
    }

    private fun startForegroundService() {
        startForeground(SUMMARY_ID, downloadBuilder.build())
    }

    fun queueDownload(source: String, destinationFolder: Uri, fileName: String) {

        // checking if I already have this download in the queue
        val replaceDownload = downloads.firstOrNull { it.source == source }
        if (replaceDownload != null) {
            // in these cases I can restart it eventually
            if (
                replaceDownload.status == CurrentDownloadStatus.Error
            ) {
                replaceDownload.destination = destinationFolder
                replaceDownload.status = CurrentDownloadStatus.Queued
                replaceDownload.progress = 0
                replaceDownload.speed = 0f
            } else {
                // not replacing anything for a running or stopped download because it could already be partially downloaded
            }

        } else {
            // new download!

            val folderUri = DocumentFile.fromTreeUri(applicationContext, destinationFolder)
            if (folderUri != null) {

                val extension: String = MimeTypeMap.getFileExtensionFromUrl(source)
                var mime: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime == null) {
                    mime = URLConnection.guessContentTypeFromName(source)
                    /*
                    if (mime == null) {
                        val connection: URLConnection = URL(link).openConnection()
                        mime= connection.contentType
                    }
                     */
                    if (mime == null) {
                        // todo: use other checks or a random mime type
                        mime = "*/*"
                    }
                }
                // todo: check if the extension needs to be removed as the docs say (it does not seem to)
                val newFile: DocumentFile? = folderUri.createFile(mime, fileName)
                if (newFile != null) {
                    val outputStream = applicationContext.contentResolver.openOutputStream(newFile.uri)
                    if (outputStream != null) {
                        val client = OkHttpClient()
                        val writer = FileWriter(
                            outputStream,
                            tempProgressListener
                        )
                        val downloader = Downloader(
                            client,
                            writer
                        )
                        downloader.download(link)
                    } else {
                        Timber.e("Outpustream nullo")
                    }
                } else {
                    Timber.e("newFile nullo")
                }
            } else {
                Timber.e("folderUri nullo")
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

    inner class CommandReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                when (it.action) {
                    PAUSE_DOWNLOAD -> {}
                    RESTART_DOWNLOAD -> {}
                    STOP_DOWNLOAD -> {}
                }
                Timber.d("Ricevuto intent $it")
            }
        }
    }

    /**
     * Only call this from the mutex lock. I use this to download a single file at once
     */
    private fun startDownloadIfAvailable() {

        // if I have no running downloads
        if (downloads.firstOrNull { it.status == CurrentDownloadStatus.Running } == null) {
            // Start the first queued download
            val currentDownload =
                downloads.firstOrNull { it.status == CurrentDownloadStatus.Queued }
            if (currentDownload != null) {

                val stopDownloadIntent = Intent(this, CommandReceiver::class.java).apply {
                    action = STOP_DOWNLOAD
                    putExtra(EXTRA_NOTIFICATION_ID, 0)
                    putExtra(CURRENT_DOWNLOAD_TITLE, currentDownload.title)
                }

                val stopDownloadPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        stopDownloadIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )


                currentDownload.status = CurrentDownloadStatus.Running
                val outputStream = contentResolver?.openOutputStream(currentDownload.destination)
                if (outputStream != null) {
                    lifecycleScope.launch {
                        // todo: move collection code to own function
                        // the collect must be run in another scope to avoid being blocked
                        launch {
                            var lastRegisteredTime = System.currentTimeMillis()
                            var lastRegisteredSize = 0.0
                            downloader.downloadInfo.collect {
                                val asd = 3
                                // todo: collect is not triggered if the data is equal to the previous one (maybe, check)
                                it.forEach { downloadStatus ->
                                    if (downloadStatus.key == outputStream.hashCode()) {
                                        when (downloadStatus.value) {
                                            is DownloadStatus.Running -> {
                                                val running =
                                                    downloadStatus.value as DownloadStatus.Running
                                                val currentTime = System.currentTimeMillis()
                                                // update speed every 2 seconds
                                                if (currentTime - lastRegisteredTime > 2000) {
                                                    currentDownload.speed =
                                                        ((running.downloadedSize - lastRegisteredSize) / (currentTime - lastRegisteredTime) / 1000).toFloat()
                                                    lastRegisteredTime = currentTime
                                                    lastRegisteredSize = running.downloadedSize
                                                }
                                                currentDownload.progress =
                                                    (running.downloadedSize * 100 / running.totalSize).toInt()
                                            }
                                            DownloadStatus.Completed -> {
                                                currentDownload.status =
                                                    CurrentDownloadStatus.Completed
                                                currentDownload.progress = 100
                                                applicationContext.vibrate()
                                            }
                                            is DownloadStatus.Error -> {
                                                currentDownload.status = CurrentDownloadStatus.Error
                                            }
                                            DownloadStatus.Paused -> {
                                                currentDownload.status =
                                                    CurrentDownloadStatus.Stopped
                                            }
                                            DownloadStatus.Queued -> TODO()
                                            DownloadStatus.Stopped -> TODO()
                                        }
                                    }
                                }
                                // update the notification
                                updateNotification(stopDownloadPendingIntent)
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

    private fun updateNotification(stopDownloadPendingIntent: PendingIntent) {

        val notifications: MutableMap<String, Notification> = mutableMapOf()

        downloads.forEach {
            when (it.status) {
                CurrentDownloadStatus.Running -> {
                    downloadBuilder.setProgress(100, it.progress, false)
                        .setContentTitle(
                            getString(
                                R.string.torrent_in_progress_format,
                                it.progress,
                                it.speed
                            )
                        )
                        .setStyle(NotificationCompat.BigTextStyle().bigText(it.title))

                    // todo: when a Running download notification becomes Completed, make the last Running notification cancellable

                    if (it.status == CurrentDownloadStatus.Running && it.progress < 100) {
                        downloadBuilder.setOngoing(true).addAction(
                            R.drawable.icon_stop,
                            getString(R.string.stop),
                            stopDownloadPendingIntent
                        )
                    } else
                        downloadBuilder.setOngoing(false)

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Completed -> {
                    downloadBuilder
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(it.title)
                        ).setOngoing(false)
                        .setProgress(0, 0, false)
                        .setContentTitle(getString(R.string.download_complete))

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Error -> {
                    downloadBuilder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(it.title)
                    )

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Queued -> {
                    downloadBuilder
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(it.title)
                        ).setOngoing(false)
                        .setProgress(0, 0, false)
                        .setContentTitle(getString(R.string.queued))

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Stopped -> {
                    downloadBuilder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(it.title)
                    )

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Paused -> {
                    downloadBuilder.setProgress(100, it.progress, false)
                        .setContentTitle(
                            getString(
                                R.string.torrent_in_progress_format,
                                it.progress,
                                it.speed
                            )
                        )
                        .setStyle(NotificationCompat.BigTextStyle().bigText(it.title))

                    downloadBuilder.setOngoing(true).addAction(
                        R.drawable.icon_stop,
                        getString(R.string.stop),
                        stopDownloadPendingIntent
                    )

                    notifications[it.source] = downloadBuilder.build()
                }
            }
        }

        notificationManager.apply {
            notifications.forEach { (id, notification) ->
                notify(id.hashCode(), notification)
            }
        }

        // stop serving completed download notifications
        downloads.removeAll {
            it.status == CurrentDownloadStatus.Completed ||
                    it.status == CurrentDownloadStatus.Error
        }

    }

    companion object {
        const val GROUP_KEY_DOWNLOADS: String = "com.github.livingwithhippos.unchained.DOWNLOADS"
        const val KEY_DOWNLOADS_ID = "downloads_id_key"
        const val SUMMARY_ID: Int = 308
        val downloads = mutableListOf<CustomDownload>()

        const val PAUSE_DOWNLOAD = "pause_download"
        const val RESTART_DOWNLOAD = "restart_download"
        const val STOP_DOWNLOAD = "stop_download"
        const val CURRENT_DOWNLOAD_TITLE = "current_download_id"
    }
}

data class CustomDownload(
    val source: String,
    var destination: Uri,
    var title: String,
    var status: CurrentDownloadStatus = CurrentDownloadStatus.Queued,
    var progress: Int = 0,
    var speed: Float = 0f
)

sealed class CurrentDownloadStatus {
    object Queued : CurrentDownloadStatus()
    object Stopped : CurrentDownloadStatus()
    object Paused : CurrentDownloadStatus()
    object Completed : CurrentDownloadStatus()
    object Running : CurrentDownloadStatus()
    object Error : CurrentDownloadStatus()
}

sealed class DownloadStatus {
    object Queued : DownloadStatus()
    object Stopped : DownloadStatus()
    object Paused : DownloadStatus()
    object Completed : DownloadStatus()
    data class Running(
        val totalSize: Double,
        val downloadedSize: Double,
    ) : DownloadStatus()

    data class Error(val type: DownloadErrorType) : DownloadStatus()
}

sealed class DownloadErrorType {
    object ResponseError : DownloadErrorType()
    object Interrupted : DownloadErrorType()
    object EmptyBody : DownloadErrorType()
    object ServerUnavailable : DownloadErrorType()
    object IPBanned : DownloadErrorType()
}