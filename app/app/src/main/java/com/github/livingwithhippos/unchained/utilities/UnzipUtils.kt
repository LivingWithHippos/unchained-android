package com.github.livingwithhippos.unchained.utilities

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

/**
 * UnzipUtils class extracts files and sub-directories of a standard zip file to a destination
 * directory. credits to https://gist.github.com/NitinPraksash9911/dea21ec4b8ae7df068f8f891187b6d1e
 * this file has been modified
 */
object UnzipUtils {
    /**
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    @Throws(IOException::class)
    fun unzip(zipFilePath: File, destDirectory: File) {

        destDirectory.run {
            if (!exists()) {
                mkdirs()
            }
        }

        ZipFile(zipFilePath).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                zip.getInputStream(entry).use { input ->
                    val filePath = File(destDirectory, entry.name)

                    if (!entry.isDirectory) {
                        // if the entry is a file, extracts it
                        extractFile(input, filePath)
                    } else {
                        // if the entry is a directory, make the directory
                        filePath.mkdir()
                    }
                }
            }
        }
    }

    /**
     * Extracts a zip entry (file entry)
     *
     * @param inputStream
     * @param destFilePath
     * @throws IOException
     */
    @Throws(IOException::class)
    private fun extractFile(inputStream: InputStream, destFilePath: File) {
        destFilePath.outputStream().use { inputStream.copyTo(it) }
    }
}
