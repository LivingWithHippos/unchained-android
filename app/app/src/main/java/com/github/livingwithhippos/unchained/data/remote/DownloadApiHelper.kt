package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Response

interface DownloadApiHelper {
    suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int
    ): Response<List<DownloadItem>>

    suspend fun deleteDownload(
        token: String,
        id: String,
    ): Response<Unit>
}