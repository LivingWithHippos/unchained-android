package com.github.livingwithhippos.unchained.newdownload.model

import okhttp3.RequestBody
import retrofit2.Response

interface TorrentApiHelper {

    suspend fun getAvailableHosts(token: String): Response<List<AvailableHost>>

    suspend fun addTorrent(
        token: String,
        binaryTorrent: RequestBody,
        host: String
    ): Response<UploadedTorrent>

    suspend fun getTorrentsList(
        token: String,
        offset: Int?,
        page: Int?,
        limit: Int?,
        filter: String?
    ): Response<List<TorrentItem>>

    suspend fun selectFiles(
        token: String,
        id: String,
        files: String
    ): Response<Unit>
}