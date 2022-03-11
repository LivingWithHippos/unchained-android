package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.di.ClassicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.OutputStream
import javax.inject.Inject


class Downloader @Inject constructor(@ClassicClient private val okHttpClient: OkHttpClient) {
    private val downloadList = mutableMapOf<Int, DownloadStatus>()

    private val _downloadInfo: MutableStateFlow<Map<Int, DownloadStatus>> =
        MutableStateFlow(downloadList)

    val downloadInfo: StateFlow<Map<Int, DownloadStatus>> get() = _downloadInfo

    var pauseDownload = false
    var endDownload = false

    suspend fun downloadFileViaOKHTTP(url: String, outputStream: OutputStream) =
        withContext(Dispatchers.IO) {
            pauseDownload = false
            endDownload = false
            val currentHash = outputStream.hashCode()
            downloadList[currentHash] = DownloadStatus.Queued

            val request: Request = Request.Builder().url(url).build()
            var responseObtained = false

            okHttpClient.newCall(request).execute()
                .use { response ->
                    responseObtained = true
                    if (response.isSuccessful) {
                        val responseBody = response.body
                        if (responseBody != null) {

                            val length: Double =
                                response.header("Content-Length", "1")?.toDouble() ?: 1.toDouble()
                            downloadList[currentHash] = DownloadStatus.Running(length, 0.0)
                            var bytesCopied: Long = 0

                            val customInputStream = FlowableInputStream(responseBody.byteStream())
                            launch {
                                customInputStream.counter.collect {
                                    // I may get some more data while not in the correct state (hypothetically). Ignore it.
                                    // todo: should probably only update if running
                                    if (downloadList[currentHash] is DownloadStatus.Running || downloadList[currentHash] is DownloadStatus.Paused || downloadList[currentHash] is DownloadStatus.Queued) {
                                        downloadList[currentHash] = DownloadStatus.Running(
                                            length,
                                            it,
                                        )
                                        _downloadInfo.emit(downloadList)
                                    }
                                }
                            }

                            customInputStream.use input@{ input ->
                                outputStream.use output@{ output ->
                                    // todo: do not copy stuff it the download is not running
                                    // taken from InputStream.copyTo
                                    val buffer = ByteArray(1024)
                                    var looping = true
                                    while (looping) {
                                        when (downloadList[currentHash]) {
                                            is DownloadStatus.Stopped -> {
                                                return@output bytesCopied
                                            }
                                            is DownloadStatus.Paused -> {
                                                delay(1000)
                                            }
                                            is DownloadStatus.Running -> {
                                                val bytes = input.read(buffer)
                                                if (bytes >= 0) {
                                                    output.write(buffer, 0, bytes)
                                                    bytesCopied += bytes
                                                } else {
                                                    looping = false
                                                }
                                            }
                                            DownloadStatus.Completed -> {
                                                // should not happen
                                                return@output bytesCopied
                                            }
                                            is DownloadStatus.Error -> {
                                                // should not happen
                                            }
                                            DownloadStatus.Queued -> {
                                                // do nothing
                                            }
                                            null -> {
                                                // should not happen
                                            }
                                        }
                                    }
                                    return@output bytesCopied
                                }
                                // code written below here will wait for the download to complete
                            }
                            if (downloadList[currentHash] is DownloadStatus.Running) {
                                if (bytesCopied >= length) {
                                    downloadList[currentHash] = DownloadStatus.Completed
                                    _downloadInfo.emit(downloadList)
                                } else {
                                    downloadList[currentHash] =
                                        DownloadStatus.Error(DownloadErrorType.Interrupted)
                                    _downloadInfo.emit(downloadList)
                                }
                            }

                        } else {
                            downloadList[currentHash] =
                                DownloadStatus.Error(DownloadErrorType.EmptyBody)
                            _downloadInfo.emit(downloadList)
                        }
                    } else {
                        downloadList[currentHash] =
                            DownloadStatus.Error(DownloadErrorType.ServerUnavailable)
                        _downloadInfo.emit(downloadList)
                    }
                }

            if (!responseObtained) {
                downloadList[currentHash] = DownloadStatus.Error(DownloadErrorType.ResponseError)
                _downloadInfo.emit(downloadList)
            }

            // at the end of the download I remove it whatever has happened
            downloadList.remove(currentHash)
        }
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