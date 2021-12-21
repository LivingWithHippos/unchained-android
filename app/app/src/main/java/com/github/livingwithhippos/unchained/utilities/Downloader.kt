package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.di.ClassicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.OutputStream
import javax.inject.Inject


class Downloader @Inject constructor(@ClassicClient private val okHttpClient: OkHttpClient) {

    private val _downloadInfo: MutableStateFlow<Pair<Double,Double>> = MutableStateFlow(Pair(0.0,0.0))
    val downloadInfo: StateFlow<Pair<Double, Double>> get() = _downloadInfo

    var pauseDownload = false
    var endDownload = false

    suspend fun downloadFileViaOKHTTP(url: String, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        // reset the variables values
        _downloadInfo.value = Pair(0.0,0.0)
        pauseDownload = false
        endDownload = false

        val request: Request = Request.Builder().url(url).build()

        val response: Response = okHttpClient.newCall(request).execute()
        val responseBody = response.body
            ?: throw IllegalStateException("Response doesn't contain a file")

        val length: Double = response.header("Content-Length", "1")?.toDouble() ?: 1.toDouble()

        val customInputStream = FlowableInputStream(responseBody.byteStream())
        launch {
            customInputStream.counter.collect {
                _downloadInfo.value = Pair(length, it)
            }
        }

        customInputStream.use input@{ input ->
            outputStream.use output@{ output ->
                // taken from InputStream.copyTo
                var bytesCopied: Long = 0
                val buffer = ByteArray(1024)
                var bytes = input.read(buffer)
                while (bytes >= 0) {
                    when {
                        endDownload -> {
                            return@output bytesCopied
                        }
                        pauseDownload -> {
                            delay(1000)
                        }
                        else -> {
                            output.write(buffer, 0, bytes)
                            bytesCopied += bytes
                            bytes = input.read(buffer)
                        }
                    }
                }
                return@output bytesCopied
            }
        }
        // code written below here will wait for the download to complete
    }
}