package com.github.livingwithhippos.unchained.utilities.download

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
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

class DownloadWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private var job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override suspend fun doWork(): Result {
        try {
            val sourceUrl: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_SOURCE)!!
            val folderUri: Uri =
                Uri.parse(inputData.getString(MainActivityViewModel.KEY_FOLDER_URI)!!)
            val fileName: String = inputData.getString(MainActivityViewModel.KEY_DOWNLOAD_NAME)!!

        try {
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

            val outputStream = applicationContext.contentResolver.openOutputStream(newFile.uri)
            if (outputStream == null) {
                Timber.e("Error getting download uri")
                showToast(R.string.download_queued_error)
                return Result.failure()
            }

            val notificationID = newFile.hashCode()

            // todo: customize this
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
                    // todo: manage progress

                    when (it) {
                        DownloadStatus.Completed -> {
                            // this is managed below
                        }
                        is DownloadStatus.Error -> {
                            makeStatusNotification(
                                notificationID,
                                fileName,
                                applicationContext.getString(R.string.error),
                                applicationContext
                            )
                        }
                        DownloadStatus.Paused -> {
                            // todo: add this, it also needs to be onGoing
                            makeStatusNotification(
                                notificationID,
                                fileName,
                                applicationContext.getString(R.string.paused),
                                applicationContext
                            )
                        }
                        DownloadStatus.Queued -> {
                            makeStatusNotification(
                                notificationID,
                                fileName,
                                applicationContext.getString(R.string.queued),
                                applicationContext
                            )
                        }
                        DownloadStatus.Stopped -> {
                            makeStatusNotification(
                                notificationID,
                                fileName,
                                applicationContext.getString(R.string.stopped),
                                applicationContext
                            )
                        }
                        is DownloadStatus.Running -> {
                            if (it.percent < 100 && it.percent != progressCounter && System.currentTimeMillis() - lastNotificationTime > 500) {
                                lastNotificationTime = System.currentTimeMillis()
                                Timber.e("DownloadStatus.Running progressCounter $progressCounter")
                                progressCounter = it.percent
                                makeProgressStatusNotification(
                                    notificationID,
                                    fileName,
                                    applicationContext.getString(
                                        R.string.download_in_progress_format,
                                        it.percent
                                    ),
                                    it.percent,
                                    applicationContext
                                )
                            }
                        }
                    }
                }
            }

            showToast(R.string.download_queued)
            // this needs to be blocking, see https://developer.android.com/topic/libraries/architecture/workmanager/advanced/coroutineworker
            val downloadedSize: Long = downloader.download(sourceUrl)

            // todo: get whole size and check if it correspond
            return if (downloadedSize > 0) {
                makeStatusNotification(
                    notificationID,
                    fileName,
                    applicationContext.getString(R.string.download_complete),
                    applicationContext
                )
                applicationContext.vibrate()
                Result.success()
            } else
                Result.failure()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Timber.e("Exception occurred while downloading, ${e.message}")
            return Result.failure()
        }
    }

    private suspend fun showToast(stringId: Int) = withContext(Dispatchers.Main) {
        applicationContext.showToast(stringId)
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

fun makeStatusNotification(
    id: Int,
    filename: String,
    title: String,
    context: Context
) {

    // Create the notification
    val builder = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(GROUP_KEY_DOWNLOADS)
        // setting setGroupSummary(false) will prevent this from showing up after the makeProgressStatusNotification one
        .setGroupSummary(true)
        .setProgress(0, 0, false)
        .setOngoing(false)
        .setContentTitle(title)
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))

    Timber.e("makeStatusNotification $title")
    // Show the notification
    NotificationManagerCompat.from(context).notify(id, builder.build())
}

fun makeProgressStatusNotification(
    id: Int,
    filename: String,
    title: String,
    progress: Int,
    context: Context
) {
    // Create the notification
    val builder = NotificationCompat.Builder(context, UnchainedApplication.DOWNLOAD_CHANNEL_ID)
        .setSmallIcon(R.drawable.logo_no_background)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setGroup(GROUP_KEY_DOWNLOADS)
        .setGroupSummary(true)
        .setOngoing(true)
        .setProgress(100, progress, false)
        .setContentTitle(title)
        .setStyle(NotificationCompat.BigTextStyle().bigText(filename))

    // Show the notification
    NotificationManagerCompat.from(context).notify(id, builder.build())
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
