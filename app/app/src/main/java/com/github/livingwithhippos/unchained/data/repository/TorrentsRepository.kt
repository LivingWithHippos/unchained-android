package com.github.livingwithhippos.unchained.data.repository

import com.github.livingwithhippos.unchained.data.local.ProtoStore
import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.data.model.cache.InstantAvailability
import com.github.livingwithhippos.unchained.data.remote.TorrentApiHelper
import com.github.livingwithhippos.unchained.utilities.EitherResult
import javax.inject.Inject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber

class TorrentsRepository
@Inject
constructor(protoStore: ProtoStore, private val torrentApiHelper: TorrentApiHelper) :
    BaseRepository(protoStore) {

    suspend fun getAvailableHosts(): List<AvailableHost>? {
        val token = getToken()
        val hostResponse: List<AvailableHost>? =
            safeApiCall(
                call = { torrentApiHelper.getAvailableHosts(token = "Bearer $token") },
                errorMessage = "Error Retrieving Available Hosts")

        return hostResponse
    }

    suspend fun getTorrentInfo(id: String): TorrentItem? {
        val token = getToken()
        val torrentResponse: TorrentItem? =
            safeApiCall(
                call = { torrentApiHelper.getTorrentInfo(token = "Bearer $token", id = id) },
                errorMessage = "Error Retrieving Torrent Info")

        return torrentResponse
    }

    suspend fun addTorrent(
        binaryTorrent: ByteArray,
        host: String
    ): EitherResult<UnchainedNetworkException, UploadedTorrent> {
        val token = getToken()

        val requestBody: RequestBody =
            binaryTorrent.toRequestBody(
                "application/octet-stream".toMediaTypeOrNull(), 0, binaryTorrent.size)

        val addTorrentResponse =
            eitherApiResult(
                call = {
                    torrentApiHelper.addTorrent(
                        token = "Bearer $token", binaryTorrent = requestBody, host = host)
                },
                errorMessage = "Error Uploading Torrent")

        return addTorrentResponse
    }

    suspend fun addMagnet(
        magnet: String,
        host: String
    ): EitherResult<UnchainedNetworkException, UploadedTorrent> {
        val token = getToken()
        val torrentResponse =
            eitherApiResult(
                call = {
                    torrentApiHelper.addMagnet(
                        token = "Bearer $token", magnet = magnet, host = host)
                },
                errorMessage = "Error Uploading Torrent From Magnet")

        return torrentResponse
    }

    suspend fun getTorrentsList(
        offset: Int? = null,
        page: Int? = 1,
        limit: Int? = 50,
        filter: String? = null
    ): List<TorrentItem> {
        val token = getToken()

        val torrentsResponse: List<TorrentItem>? =
            safeApiCall(
                call = {
                    torrentApiHelper.getTorrentsList(
                        token = "Bearer $token",
                        offset = offset,
                        page = page,
                        limit = limit,
                        filter = filter)
                },
                errorMessage = "Error retrieving the torrents List, or list empty")

        return torrentsResponse ?: emptyList()
    }

    suspend fun selectFiles(
        id: String,
        files: String = "all"
    ): EitherResult<UnchainedNetworkException, Unit> {
        val token = getToken()

        Timber.d("Selecting files for torrent: $id")
        // this call has no return type
        val response =
            eitherApiResult(
                call = {
                    torrentApiHelper.selectFiles(token = "Bearer $token", id = id, files = files)
                },
                errorMessage = "Error Selecting Torrent Files")

        return response
    }

    suspend fun deleteTorrent(id: String): EitherResult<UnchainedNetworkException, Unit> {
        val token = getToken()

        val response =
            eitherApiResult(
                call = { torrentApiHelper.deleteTorrent(token = "Bearer $token", id = id) },
                errorMessage = "Error deleting Torrent")

        return response
    }

    suspend fun getInstantAvailability(
        url: String
    ): EitherResult<UnchainedNetworkException, InstantAvailability> {
        val token = getToken()

        val response =
            eitherApiResult(
                call = {
                    torrentApiHelper.getInstantAvailability(token = "Bearer $token", url = url)
                },
                errorMessage = "Error getting cached torrent files")

        return response
    }
}
