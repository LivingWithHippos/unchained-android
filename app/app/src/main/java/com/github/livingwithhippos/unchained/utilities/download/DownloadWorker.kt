package com.github.livingwithhippos.unchained.utilities.download

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.github.livingwithhippos.unchained.R
import com.github.livingwithhippos.unchained.base.UnchainedApplication
import com.github.livingwithhippos.unchained.start.viewmodel.MainActivityViewModel
import com.github.livingwithhippos.unchained.utilities.extension.showToast
import com.github.livingwithhippos.unchained.utilities.extension.vibrate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import timber.log.Timber
import java.net.URLConnection
import java.util.*

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private var job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    var shutdown = false

    override suspend fun doWork(): Result {
        val sourceUrl: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_SOURCE) ?: return Result.failure()
        val fileName: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_NAME) ?: return Result.failure()

        val folderUri: Uri =
            Uri.parse(inputData.getString(MainActivityViewModel.KEY_FOLDER_URI)!!)

        val newFile: DocumentFile? = try {
            getFileDocument(sourceUrl, folderUri, fileName)
        } catch (ex: SecurityException) {
            Timber.e("User has removed folder permissions")
            null
        }

        if (newFile == null) {
            Timber.e("Error getting download location file")
            showToast(R.string.pick_download_folder)
            return Result.failure()
        }

        val notificationID = newFile.hashCode()

        try {


            val outputStream = applicationContext.contentResolver.openOutputStream(newFile.uri)
            if (outputStream == null) {
                Timber.e("Error getting download uri")
                showToast(R.string.download_queued_error)
                return Result.failure()
            }

            // todo: use a single customized instance of this
            val client = OkHttpClient()
            val writer = FileWriter(
                outputStream
            )
            val downloader = Downloader(
                client,
                writer
            )

            var progressCounter = -1
            var lastNotificationTime = 0L

            scope.launch {
                writer.state.collect {
                    if (!isStopped) {
                        when (it) {
                            DownloadStatus.Completed -> {
                                // this is managed below
                            }
                            is DownloadStatus.Error -> {
                                setForeground(
                                    makeStatusNotification(
                                        id,
                                        notificationID,
                                        fileName,
                                        applicationContext.getString(R.string.error),
                                        applicationContext,
                                        false
                                    )
                                )

                            }
                            DownloadStatus.Paused -> {
                                setForeground(
                                    makeStatusNotification(
                                        id,
                                        notificationID,
                                        fileName,
                                        applicationContext.getString(R.string.paused),
                                        applicationContext
                                    )
                                )
                            }
                            DownloadStatus.Queued -> {
                                setForeground(
                                    makeStatusNotification(
                                        id,
                                        notificationID,
                                        fileName,
                                        applicationContext.getString(R.string.queued),
                                        applicationContext
                                    )
                                )
                            }
                            DownloadStatus.Stopped -> {
                                setForeground(
                                    makeStatusNotification(
                                        id,
                                        notificationID,
                                        fileName,
                                        applicationContext.getString(R.string.stopped),
                                        applicationContext,
                                        false
                                    )
                                )
                            }
                            is DownloadStatus.Running -> {
                                if (it.percent < 100 && it.percent != progressCounter && System.currentTimeMillis() - lastNotificationTime > 500) {
                                    lastNotificationTime = System.currentTimeMillis()
                                    progressCounter = it.percent


                                    setForeground(
                                        makeProgressStatusNotification(
                                            id,
                                            notificationID,
                                            fileName,
                                            it.percent,
                                            applicationContext
                                        )
                                    )

                                }
                            }
                        }
                    } else {
                        // stops the download only the first time I get a notification
                        if (!shutdown) {
                            shutdown = true
                            downloader.stop()
                        }
                    }
                }
            }

            showToast(R.string.download_queued)
            // this needs to be blocking, see https://developer.android.com/topic/libraries/architecture/workmanager/advanced/coroutineworker
            val downloadedSize: Long = downloader.download(sourceUrl)

            // todo: get whole size and check if it correspond
            return if (downloadedSize > 0) {
                setForeground(
                    makeStatusNotification(
                        id,
                        notificationID,
                        fileName,
                        applicationContext.getString(R.string.download_complete),
                        applicationContext,
                        false
                    )
                )
                applicationContext.vibrate()
                Result.success()
            } else
                Result.failure()
        } catch (e: android.accounts.NetworkErrorException) {
            e.printStackTrace()

            showToast(R.string.download_link_expired)
            Timber.e("Exception occurred while downloading, ${e.message}")
            return Result.failure()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()

            showToast(
                applicationContext.getString(
                    R.string.download_not_started_format,
                    fileName
                )
            )
            Timber.e("Exception occurred while downloading, ${e.message}")
            return Result.failure()
        }
    }

    private suspend fun showToast(stringId: Int) = withContext(Dispatchers.Main) {
        applicationContext.showToast(stringId)
    }

    private suspend fun showToast(message: String) = withContext(Dispatchers.Main) {
        applicationContext.showToast(message)
    }

    private fun getFileDocument(
        sourceUrl: String,
        destinationFolder: Uri,
        fileName: String
    ): DocumentFile? {

        val folderUri: DocumentFile? =
            DocumentFile.fromTreeUri(applicationContext, destinationFolder)
        if (folderUri != null) {
            val extension: String = MimeTypeMap.getFileExtensionFromUrl(sourceUrl)
            var mime: String? = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime == null) {
                mime = URLConnection.guessContentTypeFromName(sourceUrl)
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
}

const val GROUP_KEY_DOWNLOADS: String = "com.github.livingwithhippos.unchained.DOWNLOADS"


// see https://developer.android.com/topic/libraries/architecture/workmanager/advanced/long-running
fun makeStatusNotification(
    workerId: UUID,
    id: Int,
    filename: String,
    title: String,
    context: Context,
    onGoing: Boolean = true
): ForegroundInfo {

    // This PendingIntent can be used to cancel the worker
    val stopIntent = WorkManager.getInstance(context)
        .createCancelPendingIntent(workerId)
    // Create the notification
    val notification = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(GROUP_KEY_DOWNLOADS)
        // setting setGroupSummary(false) will prevent this from showing up after the makeProgressStatusNotification one
        .setGroupSummary(true)
        .setProgress(0, 0, false)
        .setOngoing(onGoing)
        .setContentTitle(title)
        // todo:check if .setContentText(progress) is the same
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))
        .addAction(R.drawable.icon_stop, context.getString(R.string.stop), stopIntent)
        .build()

    return ForegroundInfo(id, notification)
}

fun makeProgressStatusNotification(
    workerId: UUID,
    id: Int,
    filename: String,
    progress: Int,
    context: Context
): ForegroundInfo {
    val title = context.getString(
        R.string.download_in_progress_format,
        progress
    )
    // This PendingIntent can be used to cancel the worker
    val stopIntent = WorkManager.getInstance(context)
        .createCancelPendingIntent(workerId)
    // Create the notification
    val notification = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(GROUP_KEY_DOWNLOADS)
        .setGroupSummary(true)
        .setOngoing(true)
        .setProgress(100, progress, false)
        .setContentTitle(title)
        // todo:check if .setContentText(progress) is the same
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))
        .addAction(R.drawable.icon_stop, context.getString(R.string.stop), stopIntent)
        .build()

    return ForegroundInfo(id, notification)
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
