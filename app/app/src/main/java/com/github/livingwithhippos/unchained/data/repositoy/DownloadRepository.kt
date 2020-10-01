package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import com.github.livingwithhippos.unchained.data.remote.DownloadApiHelper
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class DownloadRepository @Inject constructor(private val downloadApiHelper: DownloadApiHelper) :
    BaseRepository() {
    suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int = 1,
        limit: Int = 30
    ): List<DownloadItem> {

        val downloadResponse = safeApiCall(
            call = { downloadApiHelper.getDownloads("Bearer $token", offset, page, limit) },
            errorMessage = "Error Fetching User Info"
        )

        return downloadResponse ?: emptyList()

    }

    suspend fun deleteTorrent(token: String, id: String): Unit? {

        val response = safeApiCall(
            call = {
                downloadApiHelper.deleteDownload(
                    token = "Bearer $token",
                    id = id
                )
            },
            errorMessage = "Error deleting download"
        )

        return response
    }

}