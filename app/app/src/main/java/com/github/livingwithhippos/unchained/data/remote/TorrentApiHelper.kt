package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

interface TorrentApiHelper {

    suspend fun getAvailableHosts(token: String): Response<List<AvailableHost>>


    suspend fun getTorrentInfo(
        token: String,
        id: String
    ): Response<TorrentItem>

    suspend fun addTorrent(
        token: String,
        binaryTorrent: RequestBody,
        host: String
    ): Response<UploadedTorrent>

    suspend fun addMagnet(
        token: String,
        magnet: String,
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

    suspend fun deleteTorrent(
        token: String,
        id: String
    ): Call<ResponseBody>
}