package com.github.livingwithhippos.unchained.downloadlists.model

import retrofit2.Response

interface DownloadApiHelper {
    suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int
    ): Response<List<DownloadItem>>
}