package com.github.livingwithhippos.unchained.utilities.download

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Taken from https://www.baeldung.com/java-okhttp-download-binary-file
 */
class Downloader(
    private val client: OkHttpClient,
    private val writer: FileWriter
) : AutoCloseable {

    @Throws(IOException::class)
    fun download(url: String): Long {
        val request: Request = Request.Builder().url(url).build()
        client.newCall(request).execute()
            .use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body
                    if (responseBody != null) {
                        val length: Double =
                            response.header("Content-Length", "1")?.toDouble() ?: 1.toDouble()

                        return writer.write(responseBody.byteStream(), length)
                    } else {
                        throw IllegalStateException("Response doesn't contain a file")
                    }
                } else {
                    throw IllegalStateException("Response not successful")
                }
            }
    }

    @Throws(Exception::class)
    override fun close() {
        writer.close()
    }
}
