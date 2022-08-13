package com.github.livingwithhippos.unchained.data.service

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Binder
import android.os.IBinder
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.EXTRA_NOTIFICATION_ID
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.LifecycleService
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.di.DownloadNotification
import com.github.livingwithhippos.unchained.utilities.download.Downloader
import com.github.livingwithhippos.unchained.utilities.download.FileWriter
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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

    private var job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private val notificationJob: Job = Job()
    private val notificationScope = CoroutineScope(Dispatchers.Default + notificationJob)

    var notificationStarted = false

    private val downloads = mutableMapOf<String, CustomDownload>()

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

    private fun getFileDocument(
        source: String,
        destinationFolder: Uri,
        fileName: String
    ): DocumentFile? {

        val folderUri: DocumentFile? =
            DocumentFile.fromTreeUri(applicationContext, destinationFolder)
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
            return folderUri.createFile(mime, fileName)
        } else {
            Timber.e("folderUri was null")
            return null
        }
    }

    fun queueDownload(source: String, destinationFolder: Uri, fileName: String) {

        val newFile = getFileDocument(source, destinationFolder, fileName)
        if (newFile == null) {
            Timber.e("Error getting download location file")
            applicationContext.showToast(R.string.download_queued_error)
            return
        } else {
            applicationContext.showToast(R.string.download_queued)
        }
        // checking if I already have this download in the queue
        val replaceDownload: CustomDownload? = downloads[source]
        if (replaceDownload != null) {
            // in these cases I can restart it eventually
            if (
                replaceDownload.status == CurrentDownloadStatus.Error
            ) {
                replaceDownload.status = CurrentDownloadStatus.Queued
                replaceDownload.destination = newFile.uri
                replaceDownload.progress = 0
                replaceDownload.downloadedSize = 0

                downloads[source] = replaceDownload
            } else {
                // todo: decide what to do, even nothing
                // not replacing anything for a running or stopped download because it could already be partially downloaded
                Timber.e("Requested download of already queued file in status ${replaceDownload.status}")
            }

        } else {
            // new download!
            downloads[source] = CustomDownload(
                source = source,
                destination = newFile.uri,
                title = fileName
            )
        }
        scope.launch {
            startDownloadIfAvailable()
        }
        scope.launch {
            startNotificationLoop()
        }
    }

    private suspend fun startNotificationLoop() {
        if (!notificationStarted) {
            notificationStarted = true
            notificationScope.launch {
                delay(1000)
                while (isActive) {
                    updateNotification()
                    scope.launch {
                        startDownloadIfAvailable()
                    }
                    delay(1000)
                }
            }
        }
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
    private suspend fun startDownloadIfAvailable() {

        // if I have no running downloads
        val hasRunningDownloads = downloads.values.firstOrNull {
            it.status is CurrentDownloadStatus.Running
        } != null
        if (!hasRunningDownloads) {
            // Start the first queued download
            val queuedDownload: CustomDownload? = downloads.values.firstOrNull {
                it.status == CurrentDownloadStatus.Queued
            }

            if (queuedDownload != null) {

                val stopDownloadIntent = Intent(this, CommandReceiver::class.java).apply {
                    action = STOP_DOWNLOAD
                    putExtra(EXTRA_NOTIFICATION_ID, 0)
                    putExtra(CURRENT_DOWNLOAD_TITLE, queuedDownload.title)
                }

                // todo: check PendingIntent.FLAG_IMMUTABLE on api 22
                val stopDownloadPendingIntent: PendingIntent =
                    PendingIntent.getBroadcast(
                        this,
                        0,
                        stopDownloadIntent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                queuedDownload.status = CurrentDownloadStatus.Running
                downloads[queuedDownload.source] = queuedDownload

                val outputStream = contentResolver?.openOutputStream(queuedDownload.destination)
                if (outputStream != null) {

                    val client = OkHttpClient()
                    val writer = FileWriter(
                        outputStream
                    )
                    val downloader = Downloader(
                        client,
                        writer
                    )

                    scope.launch {
                        writer.state.collect {
                            when (it) {
                                DownloadStatus.Completed -> {
                                    val currentDownload: CustomDownload? =
                                        downloads[queuedDownload.source]
                                    if (currentDownload != null) {
                                        currentDownload.status = CurrentDownloadStatus.Completed
                                        currentDownload.progress = 100
                                        downloads[queuedDownload.source] = currentDownload
                                        // todo: make optional
                                        applicationContext.vibrate()
                                    }
                                }
                                is DownloadStatus.Error -> {
                                    val currentDownload: CustomDownload? =
                                        downloads[queuedDownload.source]
                                    if (currentDownload != null) {
                                        currentDownload.status = CurrentDownloadStatus.Error
                                        // currentDownload.progress = "0"
                                        downloads[queuedDownload.source] = currentDownload
                                        // applicationContext.vibrate()
                                    }
                                }
                                DownloadStatus.Paused -> {
                                    // todo: implement
                                    val currentDownload: CustomDownload? =
                                        downloads[queuedDownload.source]
                                    if (currentDownload != null) {
                                        currentDownload.status = CurrentDownloadStatus.Paused
                                        downloads[queuedDownload.source] = currentDownload
                                        // applicationContext.vibrate()
                                    }
                                }
                                DownloadStatus.Queued -> {
                                    // do nothing?
                                }
                                DownloadStatus.Stopped -> {
                                    // todo: implement
                                    val currentDownload: CustomDownload? =
                                        downloads[queuedDownload.source]
                                    if (currentDownload != null) {
                                        currentDownload.status = CurrentDownloadStatus.Stopped
                                        downloads[queuedDownload.source] = currentDownload
                                        // applicationContext.vibrate()
                                    }
                                }
                                is DownloadStatus.Running -> {
                                    val currentDownload: CustomDownload? =
                                        downloads[queuedDownload.source]
                                    if (currentDownload != null) {
                                        currentDownload.status = CurrentDownloadStatus.Running
                                        currentDownload.progress = it.percent
                                        currentDownload.downloadedSize = it.downloadedSize

                                        downloads[queuedDownload.source] = currentDownload
                                    }
                                }
                            }
                        }
                    }


                    // todo: check if this is blocking
                    // todo: move to download queue manager
                    downloader.download(queuedDownload.source)
                } else {
                    Timber.e("Output stream is empty, check permissions etc")
                }
            } else {
                Timber.d("Download requested but no queued download available $downloads")
                // notifications are managed in updateNotification, just skip this one
            }
        } else {
            // Timber.d("Some downloads are already running")
        }
    }

    private fun updateNotification() {

        val notifications: MutableMap<String, Notification> = mutableMapOf()

        downloads.values.forEach {
            when (it.status) {
                CurrentDownloadStatus.Running -> {
                    downloadBuilder.setProgress(100, it.progress, false)
                        .setContentTitle(
                            getString(
                                R.string.download_in_progress_format,
                                it.progress
                            )
                        )
                        .setStyle(NotificationCompat.BigTextStyle().bigText(it.title))

                    // todo: when a Running download notification becomes Completed,
                    //  make the last Running notification cancellable

                    // todo: need custom stopDownloadPendingIntent
                    if (it.status == CurrentDownloadStatus.Running && it.progress < 100) {
                        // downloadBuilder.setOngoing(true).addAction(
                        //     R.drawable.icon_stop,
                        //     getString(R.string.stop),
                        //     stopDownloadPendingIntent
                        // )
                        downloadBuilder.setOngoing(true)
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
                    ).setOngoing(false)
                        .setContentTitle(getString(R.string.error))

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Queued -> {
                    downloadBuilder
                        .setStyle(
                            NotificationCompat.BigTextStyle()
                                .bigText(it.title)
                        ).setOngoing(true)
                        .setProgress(0, 0, false)
                        .setContentTitle(getString(R.string.queued))

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Stopped -> {
                    downloadBuilder.setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(it.title)
                    ).setOngoing(false)
                        .setContentTitle(getString(R.string.stopped))

                    notifications[it.source] = downloadBuilder.build()
                }
                CurrentDownloadStatus.Paused -> {
                    downloadBuilder.setProgress(100, it.progress, false)
                        .setContentTitle(
                            getString(
                                R.string.paused
                            )
                        ).setOngoing(true)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(it.title))

                    // downloadBuilder.addAction(
                    //     R.drawable.icon_play_outline,
                    //     getString(R.string.restart),
                    //     stopDownloadPendingIntent
                    // )

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
        downloads.filter {
            it.value.status is CurrentDownloadStatus.Completed || it.value.status is CurrentDownloadStatus.Error || it.value.status is CurrentDownloadStatus.Stopped
        }.forEach {
            downloads.remove(it.value.source)
            Timber.d("Removing from downloads ${it.value.title}")
        }

    }

    companion object {
        const val GROUP_KEY_DOWNLOADS: String = "com.github.livingwithhippos.unchained.DOWNLOADS"
        const val SUMMARY_ID: Int = 308

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
    var downloadedSize: Long = 0
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
        val downloadedSize: Long,
        val percent: Int,
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