package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.newdownload.model.AvailableHost
import com.github.livingwithhippos.unchained.newdownload.model.TorrentApiHelper
import com.github.livingwithhippos.unchained.newdownload.model.UploadedTorrent
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject


class TorrentsRepository @Inject constructor(private val torrentApiHelper: TorrentApiHelper) :
    BaseRepository() {

    suspend fun getAvailableHosts(token: String): List<AvailableHost>? {
        val hostResponse: List<AvailableHost>? = safeApiCall(
            call = { torrentApiHelper.getAvailableHosts(token = "Bearer $token") },
            errorMessage = "Error Retrieving Available Hosts"
        )

        return hostResponse;
    }

    suspend fun addTorrent(
        token: String,
        binaryTorrent: ByteArray,
        host: String
    ): UploadedTorrent? {

        val requestBody: RequestBody = binaryTorrent.toRequestBody(
            "application/octet-stream".toMediaTypeOrNull(),
            0,
            binaryTorrent.size
        )

        val addTorrentResponse = safeApiCall(
            call = {
                torrentApiHelper.addTorrent(
                    token = "Bearer $token",
                    binaryTorrent = requestBody,
                    host = host
                )
            },
            errorMessage = "Error Uploading Torrent"
        )

        return addTorrentResponse;
    }
}