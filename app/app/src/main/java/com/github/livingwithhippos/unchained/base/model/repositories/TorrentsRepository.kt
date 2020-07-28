package com.github.livingwithhippos.unchained.base.model.repositories

import com.github.livingwithhippos.unchained.newdownload.model.AvailableHost
import com.github.livingwithhippos.unchained.newdownload.model.TorrentApiHelper
import com.github.livingwithhippos.unchained.newdownload.model.TorrentItem
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

    suspend fun getTorrentInfo(
        token: String,
        id: String
    ): TorrentItem? {
        val torrentResponse: TorrentItem? = safeApiCall(
            call = { torrentApiHelper.getTorrentInfo(
                token = "Bearer $token",
                id = id
            ) },
            errorMessage = "Error Retrieving Torrent Info"
        )

        return torrentResponse;

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

    suspend fun addMagnet(
        token: String,
        magnet: String,
        host: String
    ): UploadedTorrent? {
        val torrentResponse = safeApiCall(
            call = { torrentApiHelper.addMagnet(
                token = "Bearer $token",
                magnet = magnet,
                host = host
            ) },
            errorMessage = "Error Uploading Torrent From Magnet"
        )

        return torrentResponse;

    }

    suspend fun getTorrentsList(
        token: String,
        offset: Int? = 0,
        page: Int? = null,
        limit: Int? = 5,
        filter: String?
    ): List<TorrentItem>? {
        val torrentsResponse: List<TorrentItem>? = safeApiCall(
            call = { torrentApiHelper.getTorrentsList(
                token = "Bearer $token",
                offset = offset,
                page = page,
                limit = limit,
                filter = filter
            ) },
            errorMessage = "Error Retrieving Torrent Info"
        )

        return torrentsResponse;

    }

    suspend fun selectFiles(
        token: String,
        id: String,
        files: String = "all"
    ) {

        val selectFilesResponse = safeApiCall(
            call = {
                torrentApiHelper.selectFiles(
                    token = "Bearer $token",
                    id = id,
                    files = files
                )
            },
            errorMessage = "Error Selecting Torrent Files"
        )

    }
}