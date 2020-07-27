package com.github.livingwithhippos.unchained.newdownload.model

import okhttp3.RequestBody
import retrofit2.Response
import javax.inject.Inject

class TorrentApiHelperImpl @Inject constructor(private val torrentsApi: TorrentsApi) :
    TorrentApiHelper {
    override suspend fun getAvailableHosts(token: String): Response<List<AvailableHost>> =
        torrentsApi.getAvailableHosts(token)

    override suspend fun addTorrent(
        token: String,
        binaryTorrent: RequestBody,
        host: String
    ): Response<UploadedTorrent> = torrentsApi.addTorrent(token, binaryTorrent, host)
}