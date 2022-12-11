package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.remote.CustomDownloadHelper
import com.github.livingwithhippos.unchained.plugins.model.Plugin
import com.github.livingwithhippos.unchained.repository.model.JsonPluginRepository
import com.github.livingwithhippos.unchained.utilities.EitherResult
import com.github.livingwithhippos.unchained.utilities.PLUGINS_REPOSITORY_LINK
import com.github.livingwithhippos.unchained.utilities.extension.isWebUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

class CustomDownloadRepository @Inject constructor(
    protoStore: ProtoStore,
    private val customDownloadHelper: CustomDownloadHelper
) :
    BaseRepository(protoStore) {

    fun downloadToCache(
        url: String,
        fileName: String,
        cacheDir: File,
        suffix: String? = null
    ): Flow<DownloadResult> = channelFlow {
        if (url.isWebUrl()) {
            // todo: use the FileWriter and Downloader helper classes
            val call = customDownloadHelper.getFile(url)
            if (call.isSuccessful) {
                val body = call.body()
                if (body != null) {
                    withContext(Dispatchers.IO) {

                        var inputStream: InputStream? = null
                        var outputStream: OutputStream? = null

                        var successfulDownload = false
                        var sentEnding = false

                        // todo: check cache size with getCacheQuotaBytes() before downloading
                        val file = File.createTempFile(fileName, suffix, cacheDir)
                        val tempFileName = file.name

                        try {
                            val fileSize: Long = body.contentLength()

                            var fileSizeDownloaded: Long = 0

                            val fileReader = ByteArray(4096)
                            inputStream = body.byteStream()
                            outputStream = FileOutputStream(file)
                            while (true) {
                                val read: Int = inputStream.read(fileReader)
                                if (read == -1) {
                                    break
                                }
                                outputStream.write(fileReader, 0, read)
                                fileSizeDownloaded += read.toLong()
                                send(DownloadResult.Progress((fileSizeDownloaded / fileSize * 100).toInt()))
                            }
                            // todo: add check for fileSizeDownloaded and fileSize difference
                            outputStream.flush()
                            successfulDownload = true
                        } catch (e: IOException) {
                            send(DownloadResult.Failure)
                            sentEnding = true
                        } finally {
                            inputStream?.close()
                            outputStream?.close()
                            // send ok or error if it hasn't already been sent
                            if (successfulDownload)
                                send(DownloadResult.End(tempFileName))
                            else if (!sentEnding)
                                send(DownloadResult.Failure)
                        }
                    }
                } else send(DownloadResult.Failure)
            } else send(DownloadResult.Failure)
        } else send(DownloadResult.WrongURL)
    }

    suspend fun downloadPluginRepository(link: String): EitherResult<UnchainedNetworkException, JsonPluginRepository> {

        return eitherApiResult(
            call = { customDownloadHelper.getPluginsRepository(link) },
            errorMessage = "Error Fetching plugins repository"
        )
    }

    suspend fun downloadPlugin(link: String): EitherResult<UnchainedNetworkException, Plugin> {
        return eitherApiResult(
            call = { customDownloadHelper.getPlugin(link) },
            errorMessage = "Error fetching plugin"
        )
    }
}

sealed class DownloadResult {
    data class Progress(val percent: Int) : DownloadResult()
    object WrongURL : DownloadResult()
    data class End(val fileName: String) : DownloadResult()
    object Failure : DownloadResult()
}
