package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import okhttp3.RequestBody
import retrofit2.Response
import javax.inject.Inject

class TorrentApiHelperImpl @Inject constructor(private val torrentsApi: TorrentsApi) :
    TorrentApiHelper {
    override suspend fun getAvailableHosts(token: String): Response<List<AvailableHost>> =
        torrentsApi.getAvailableHosts(token)

    override suspend fun getTorrentInfo(token: String, id: String): Response<TorrentItem> =
        torrentsApi.getTorrentInfo(token, id)

    override suspend fun addTorrent(
        token: String,
        binaryTorrent: RequestBody,
        host: String
    ): Response<UploadedTorrent> = torrentsApi.addTorrent(token, binaryTorrent, host)

    override suspend fun addMagnet(
        token: String,
        magnet: String,
        host: String
    ): Response<UploadedTorrent> = torrentsApi.addMagnet(token, magnet, host)

    override suspend fun getTorrentsList(
        token: String,
        offset: Int?,
        page: Int?,
        limit: Int?,
        filter: String?
    ): Response<List<TorrentItem>> = torrentsApi.getTorrentsList(token, offset, page, limit, filter)

    override suspend fun selectFiles(token: String, id: String, files: String) =
        torrentsApi.selectFiles(token, id, files)

    override suspend fun deleteTorrent(token: String, id: String) =
        torrentsApi.deleteTorrent(token, id)
}
