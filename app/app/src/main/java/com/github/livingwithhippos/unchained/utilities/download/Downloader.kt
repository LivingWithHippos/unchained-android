package com.github.livingwithhippos.unchained.utilities.download

import android.accounts.NetworkErrorException
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/** Taken from https://www.baeldung.com/java-okhttp-download-binary-file */
class Downloader(private val client: OkHttpClient, private val writer: FileWriter) : AutoCloseable {

    @Throws(IOException::class)
    suspend fun download(url: String): Long =
        withContext(Dispatchers.IO) {
            val request: Request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody != null) {
                        val length: Double =
                            response.header("Content-Length", "1")?.toDouble() ?: 1.toDouble()

                        return@withContext writer.write(responseBody.byteStream(), length)
                    } else {
                        throw IllegalStateException("Response doesn't contain a file")
                    }
                } else {
                    throw NetworkErrorException("Response not successful: ${response.code}")
                }
            }
        }

    @Throws(Exception::class)
    override fun close() {
        writer.close()
    }

    fun stop() {
        writer.stopDownload = true
    }
}
