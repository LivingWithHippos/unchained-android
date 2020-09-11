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
}