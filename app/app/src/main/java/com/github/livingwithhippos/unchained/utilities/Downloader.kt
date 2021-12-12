package com.github.livingwithhippos.unchained.utilities

import com.github.livingwithhippos.unchained.di.ClassicClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.File
import javax.inject.Inject


class Downloader @Inject constructor(@ClassicClient private val okHttpClient: OkHttpClient) {

    suspend fun downloadFileViaOKHTTP(url: String, destinationFile: File) = withContext(Dispatchers.IO) {
        val request: Request = Request.Builder().url(url).build()

        val response: Response = okHttpClient.newCall(request).execute()
        val responseBody = response.body
            ?: throw IllegalStateException("Response doesn't contain a file")

        val length: Double = response.header("Content-Length", "1")?.toDouble() ?: 1.toDouble()

        val customInputStream = FlowableInputStream(responseBody.byteStream())
        customInputStream.counter.collect {
            val progress = it * 100 / length
            Timber.d("progress: $progress")
        }

        customInputStream.use { input ->
            destinationFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }
}