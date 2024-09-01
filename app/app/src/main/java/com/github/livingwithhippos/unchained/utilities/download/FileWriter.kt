package com.github.livingwithhippos.unchained.utilities.download

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/** Taken from https://www.baeldung.com/java-okhttp-download-binary-file */
class FileWriter(private val outputStream: OutputStream) : AutoCloseable {

    private val _state: MutableStateFlow<DownloadStatus> = MutableStateFlow(DownloadStatus.Queued)
    val state: StateFlow<DownloadStatus> = _state

    var stopDownload = false

    @Throws(IOException::class)
    suspend fun write(inputStream: InputStream, length: Double): Long =
        withContext(Dispatchers.IO) {
            BufferedInputStream(inputStream).use { input ->
                val dataBuffer = ByteArray(CHUNK_SIZE)
                var readBytes: Int
                var totalBytes: Long = 0
                while (input.read(dataBuffer).also { readBytes = it } != -1 && !stopDownload) {
                    totalBytes += readBytes.toLong()
                    outputStream.write(dataBuffer, 0, readBytes)
                    _state.emit(
                        DownloadStatus.Running(
                            length,
                            totalBytes,
                            (totalBytes / length * 100).toInt(),
                        )
                    )
                }
                if (!stopDownload) _state.emit(DownloadStatus.Completed)
                else _state.emit(DownloadStatus.Stopped)
                return@withContext totalBytes
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
