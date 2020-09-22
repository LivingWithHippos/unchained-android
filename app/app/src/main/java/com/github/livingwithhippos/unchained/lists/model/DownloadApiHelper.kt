package com.github.livingwithhippos.unchained.lists.model

import com.github.livingwithhippos.unchained.downloadlists.model.DownloadItem
import retrofit2.Response

interface DownloadApiHelper {
    suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int
    ): Response<List<DownloadItem>>
}