package com.github.livingwithhippos.unchained.data.remote

import android.content.SharedPreferences
import com.github.livingwithhippos.unchained.data.model.AvailableHost
import com.github.livingwithhippos.unchained.data.model.TorrentItem
import com.github.livingwithhippos.unchained.data.model.UploadedTorrent
import com.github.livingwithhippos.unchained.utilities.DebridProvider
import com.github.livingwithhippos.unchained.utilities.getDebridProvider
import javax.inject.Inject
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

class TorrentApiHelperImpl
@Inject
constructor(
    private val preferences: SharedPreferences,
    private val torrentsApi: TorrentsApi,
    private val allDebridTorrentsApi: AllDebridTorrentsApi,
) : TorrentApiHelper {
    override suspend fun getAvailableHosts(token: String): Response<List<AvailableHost>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.getAvailableHosts(token)
            DebridProvider.AllDebrid -> Response.success(placeholderAllDebridHosts())
        }

    override suspend fun getTorrentInfo(token: String, id: String): Response<TorrentItem> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.getTorrentInfo(token, id)
            DebridProvider.AllDebrid -> {
                val statusResponse = allDebridTorrentsApi.getMagnetStatus(token = token, id = id)
                if (!statusResponse.isSuccessful) {
                    allDebridErrorResponse(
                        code = statusResponse.code(),
                        error = statusResponse.body()?.error,
                    )
                } else {
                    val body = statusResponse.body()
                    val magnet = body?.data?.magnets?.firstOrNull()
                    if (body?.status != "success" || magnet == null) {
                        allDebridErrorResponse(body?.error)
                    } else {
                        val filesResponse = allDebridTorrentsApi.getMagnetFiles(token, listOf(id))
                        val filesEntry =
                            if (filesResponse.isSuccessful) {
                                filesResponse.body()?.data?.magnets?.firstOrNull()
                            } else {
                                null
                            }
                        Response.success(magnet.toTorrentItem(filesEntry))
                    }
                }
            }
        }

    override suspend fun addTorrent(
        token: String,
        binaryTorrent: RequestBody,
        host: String,
    ): Response<UploadedTorrent> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.addTorrent(token, binaryTorrent, host)
            DebridProvider.AllDebrid -> {
                val filePart =
                    MultipartBody.Part.createFormData(
                        "files[0]",
                        "upload.torrent",
                        binaryTorrent,
                    )
                val response = allDebridTorrentsApi.addTorrent(token, filePart)
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val upload = body?.data?.files?.firstOrNull { it.id != null && it.error == null }
                    if (body?.status == "success" && upload != null) {
                        val torrent = upload.toUploadedTorrent()
                        if (torrent != null) Response.success(torrent) else allDebridErrorResponse(null)
                    } else allDebridErrorResponse(upload?.error ?: body?.error)
                }
            }
        }

    override suspend fun addMagnet(
        token: String,
        magnet: String,
        host: String,
    ): Response<UploadedTorrent> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.addMagnet(token, magnet, host)
            DebridProvider.AllDebrid -> {
                val response = allDebridTorrentsApi.addMagnet(token, listOf(magnet))
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    val upload =
                        body?.data?.magnets?.firstOrNull { it.id != null && it.error == null }
                    if (body?.status == "success" && upload != null) {
                        val torrent = upload.toUploadedTorrent()
                        if (torrent != null) Response.success(torrent) else allDebridErrorResponse(null)
                    } else allDebridErrorResponse(upload?.error ?: body?.error)
                }
            }
        }

    override suspend fun getTorrentsList(
        token: String,
        offset: Int?,
        page: Int?,
        limit: Int?,
        filter: String?,
    ): Response<List<TorrentItem>> =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid ->
                torrentsApi.getTorrentsList(token, offset, page, limit, filter)
            DebridProvider.AllDebrid -> {
                val statusResponse = allDebridTorrentsApi.getMagnetStatus(token = token, status = filter)
                if (!statusResponse.isSuccessful) {
                    allDebridErrorResponse(
                        code = statusResponse.code(),
                        error = statusResponse.body()?.error,
                    )
                } else {
                    val body = statusResponse.body()
                    val magnets = body?.data?.magnets
                    if (body?.status == "success" && magnets != null) {
                        Response.success(magnets.map { it.toTorrentItem() })
                    } else allDebridErrorResponse(body?.error)
                }
            }
        }

    override suspend fun selectFiles(token: String, id: String, files: String) =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.selectFiles(token, id, files)
            DebridProvider.AllDebrid -> Response.success(Unit)
        }

    override suspend fun deleteTorrent(token: String, id: String) =
        when (preferences.getDebridProvider()) {
            DebridProvider.RealDebrid -> torrentsApi.deleteTorrent(token, id)
            DebridProvider.AllDebrid -> {
                val response = allDebridTorrentsApi.deleteMagnet(token, id)
                if (!response.isSuccessful) {
                    allDebridErrorResponse(code = response.code(), error = response.body()?.error)
                } else {
                    val body = response.body()
                    if (body?.status == "success") Response.success(Unit)
                    else allDebridErrorResponse(body?.error)
                }
            }
        }
}
