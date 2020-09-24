package com.github.livingwithhippos.unchained.data.repositoy

import com.github.livingwithhippos.unchained.lists.model.DownloadApiHelper
import com.github.livingwithhippos.unchained.lists.model.DownloadItem
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

        return downloadResponse ?: emptyList<DownloadItem>()

    }

}