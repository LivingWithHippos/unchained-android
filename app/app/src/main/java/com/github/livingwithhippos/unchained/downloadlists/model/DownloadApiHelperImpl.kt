package com.github.livingwithhippos.unchained.downloadlists.model

import com.github.livingwithhippos.unchained.base.model.repositories.BaseRepository
import retrofit2.Response
import javax.inject.Inject

class DownloadApiHelperImpl @Inject constructor(private val downloadsApi: DownloadsApi) : BaseRepository(),
    DownloadApiHelper {

    override suspend fun getDownloads(
        token: String,
        offset: Int,
        page: Int,
        limit: Int
    ): Response<List<DownloadItem>> =
        downloadsApi.getDownloads(token, offset, page, limit)

    override suspend fun getDownloadsList(
        token: String,
        offset: Int,
        page: Int,
        limit: Int
    ): Response<DownloadList> =
        downloadsApi.getDownloadsList(token, offset, page, limit)

    override suspend fun getDownloadsDirectly(
        token: String,
        offset: Int,
        page: Int,
        limit: Int
    ): DownloadList {

        val downloadResponse = safeApiCall(
            call = { getDownloadsList("Bearer $token", offset, page, limit) },
            errorMessage = "Error Fetching User Info"
        )

        return downloadResponse ?: DownloadList(emptyList())
    }
}