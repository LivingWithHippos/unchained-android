package com.github.livingwithhippos.unchained.utilities.download

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Taken from https://www.baeldung.com/java-okhttp-download-binary-file
 */
class FileWriter(private val outputStream: OutputStream, private val progressCallback: ProgressCallback) : AutoCloseable {

    @Throws(IOException::class)
    fun write(inputStream: InputStream, length: Double): Long {
        BufferedInputStream(inputStream).use { input ->
            val dataBuffer =
                ByteArray(CHUNK_SIZE)
            var readBytes: Int
            var totalBytes: Long = 0
            while (input.read(dataBuffer).also { readBytes = it } != -1) {
                totalBytes += readBytes.toLong()
                outputStream.write(dataBuffer, 0, readBytes)
                progressCallback.onProgress(totalBytes / length * 100.0)
            }
            return totalBytes
        }
    }

    @Throws(IOException::class)
    override fun close() {
        outputStream.close()
    }

    companion object {
        private const val CHUNK_SIZE = 1024
    }
}

interface ProgressCallback {
    fun onProgress(progress: Double)
}
